/*
 * AstBuilder.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.ast;

import com.strobel.assembler.ir.ExceptionBlock;
import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionCollection;
import com.strobel.assembler.ir.MethodBody;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.metadata.SwitchInfo;
import com.strobel.assembler.metadata.VariableDefinition;
import com.strobel.assembler.metadata.VariableDefinitionCollection;
import com.strobel.assembler.metadata.VariableReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.InstructionHelper;

import java.util.*;

import static java.lang.String.format;

public final class AstBuilder {
    private final static AstCode[] CODES = AstCode.values();
    private final static StackSlot[] EMPTY_STACK = new StackSlot[0];

    private final Map<ExceptionHandler, ByteCode> _loadExceptions = new LinkedHashMap<>();
    private MethodBody _body;
    private boolean _optimize;
    private DecompilerContext _context;

    public static List<Node> build(final MethodBody body, final boolean optimize, final DecompilerContext context) {
        final AstBuilder builder = new AstBuilder();

        builder._body = VerifyArgument.notNull(body, "body");
        builder._optimize = optimize;
        builder._context = VerifyArgument.notNull(context, "context");

        if (body.getInstructions().isEmpty()) {
            return Collections.emptyList();
        }

        final List<ByteCode> byteCode = builder.performStackAnalysis();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final List<Node> ast = builder.convertToAst(byteCode, new HashSet<>(body.getExceptionHandlers()));

        return ast;
    }

    @SuppressWarnings("ConstantConditions")
    private List<ByteCode> performStackAnalysis() {
        final Map<Instruction, ByteCode> byteCodeMap = new IdentityHashMap<>();
        final InstructionCollection instructions = _body.getInstructions();
        final List<ByteCode> body = new ArrayList<>(instructions.size());

        final StrongBox<AstCode> codeBox = new StrongBox<>();
        final StrongBox<Object> operandBox = new StrongBox<>();

        for (final Instruction instruction : instructions) {
            final OpCode opCode = instruction.getOpCode();

            AstCode code = CODES[opCode.ordinal()];
            Object operand = instruction.hasOperand() ? instruction.getOperand(0) : null;
            Object secondOperand = instruction.getOperandCount() > 1 ? instruction.getOperand(1) : null;

            codeBox.set(code);
            operandBox.set(operand);

            if (AstCode.expandMacro(codeBox, operandBox, _body)) {
                code = codeBox.get();
                operand = operandBox.get();
            }

            final ByteCode byteCode = new ByteCode();

            byteCode.offset = instruction.getOffset();
            byteCode.endOffset = instruction.getEndOffset();
            byteCode.code = code;
            byteCode.operand = operand;
            byteCode.secondOperand = secondOperand;
            byteCode.popCount = InstructionHelper.getPopDelta(instruction, _body);
            byteCode.pushCount = InstructionHelper.getPushDelta(instruction, _body);

            byteCodeMap.put(instruction, byteCode);
            body.add(byteCode);
        }

        for (int i = 0, n = body.size() - 1; i < n; i++) {
            body.get(i).next = body.get(i + 1);
        }

        final Stack<ByteCode> agenda = new Stack<>();
        final VariableDefinitionCollection variables = _body.getVariables();
        final List<ExceptionHandler> exceptionHandlers = _body.getExceptionHandlers();

        final int variableCount = variables.size();
        final Set<ByteCode> exceptionHandlerStarts = new HashSet<>(exceptionHandlers.size());
        final VariableSlot[] unknownVariables = VariableSlot.makeUnknownState(variableCount);

        for (final ExceptionHandler handler : exceptionHandlers) {
            final ByteCode handlerStart = byteCodeMap.get(handler.getHandlerBlock().getFirstInstruction());

            handlerStart.stackBefore = EMPTY_STACK;
            handlerStart.variablesBefore = unknownVariables;

            if (handler.isCatch()) {
                final ByteCode loadException = new ByteCode();

                loadException.code = AstCode.LoadException;
                loadException.operand = handler.getCatchType();
                loadException.popCount = 0;
                loadException.pushCount = 1;

                _loadExceptions.put(handler, loadException);

                handlerStart.stackBefore = new StackSlot[] { new StackSlot(new ByteCode[] { loadException }) };
            }

            exceptionHandlerStarts.add(handlerStart);
            agenda.push(handlerStart);
        }

        body.get(0).stackBefore = EMPTY_STACK;
        body.get(0).variablesBefore = unknownVariables;

        agenda.push(body.get(0));

        //
        // Process agenda.
        //
        while (!agenda.isEmpty()) {
            final ByteCode byteCode = agenda.pop();

            //
            // Calculate new stack.
            //
            final StackSlot[] newStack = StackSlot.modifyStack(
                byteCode.stackBefore,
                byteCode.popCount != -1 ? byteCode.popCount : byteCode.stackBefore.length,
                byteCode.pushCount,
                byteCode
            );

            //
            // Calculate new variable state.
            //
            VariableSlot[] newVariableState = VariableSlot.cloneVariableState(byteCode.variablesBefore);

            if (byteCode.isVariableDefinition()) {
                newVariableState[((VariableReference) byteCode.operand).getIndex()] = new VariableSlot(
                    new ByteCode[] { byteCode },
                    false
                );
            }

            if (byteCode.code == AstCode.Goto ||
                byteCode.code == AstCode.__GotoW) {

                //
                // After GOTO, finally block might have touched the variables.
                //
                newVariableState = VariableSlot.makeUnknownState(variableCount);
            }

            //
            // Find all successors.
            //
            final ArrayList<ByteCode> branchTargets = new ArrayList<>();

            if (!byteCode.code.isUnconditionalControlFlow()) {
                if (exceptionHandlerStarts.contains(byteCode.next)) {
                    //
                    // Do not fall through down to exception handler.  It's invalid bytecode,
                    // but a non-compliant compiler could produce it.
                    //
                }
                else {
                    branchTargets.add(byteCode.next);
                }
            }

            if (byteCode.operand instanceof Instruction[]) {
                for (final Instruction instruction : (Instruction[]) byteCode.operand) {
                    final ByteCode target = byteCodeMap.get(instruction);

                    branchTargets.add(target);

                    //
                    // Branch target must have a label.
                    //
                    if (target.label == null) {
                        target.label = new Label();
                        target.label.setName(target.name());
                    }
                }
            }
            else if (byteCode.operand instanceof Instruction) {
                final Instruction instruction = (Instruction) byteCode.operand;
                final ByteCode target = byteCodeMap.get(instruction);

                branchTargets.add(target);

                //
                // Branch target must have a label.
                //
                if (target.label == null) {
                    target.label = new Label();
                    target.label.setName(target.name());
                }
            }
            else if (byteCode.operand instanceof SwitchInfo) {
                final SwitchInfo switchInfo = (SwitchInfo) byteCode.operand;
                final ByteCode defaultTarget = byteCodeMap.get(switchInfo.getDefaultTarget());

                branchTargets.add(defaultTarget);

                //
                // Branch target must have a label.
                //
                if (defaultTarget.label == null) {
                    defaultTarget.label = new Label();
                    defaultTarget.label.setName(defaultTarget.name());
                }

                for (final Instruction instruction : switchInfo.getTargets()) {
                    final ByteCode target = byteCodeMap.get(instruction);

                    branchTargets.add(target);

                    //
                    // Branch target must have a label.
                    //
                    if (target.label == null) {
                        target.label = new Label();
                        target.label.setName(target.name());
                    }
                }
            }

            //
            // Apply the state to successors.
            //
            for (final ByteCode branchTarget : branchTargets) {
                if (branchTarget.stackBefore == null && branchTarget.variablesBefore == null) {
                    if (branchTargets.size() == 1) {
                        branchTarget.stackBefore = newStack;
                        branchTarget.variablesBefore = newVariableState;
                    }
                    else {
                        //
                        // Do not share data for several bytecodes.
                        //
                        branchTarget.stackBefore = StackSlot.modifyStack(newStack, 0, 0, null);
                        branchTarget.variablesBefore = VariableSlot.cloneVariableState(newVariableState);
                    }

                    agenda.push(branchTarget);
                }
                else {
                    if (branchTarget.stackBefore.length != newStack.length) {
                        throw new IllegalStateException("Inconsistent stack size at " + byteCode.name() + ".");
                    }

                    //
                    // Be careful not to change our new data; it might be reused for several branch targets.
                    // In general, be careful that two bytecodes never share data structures.
                    //

                    boolean modified = false;

                    //
                    // Merge stacks; modify the target.
                    //
                    for (int i = 0; i < newStack.length; i++) {
                        final ByteCode[] oldDefinitions = branchTarget.stackBefore[i].definitions;
                        final ByteCode[] newDefinitions = ArrayUtilities.union(oldDefinitions, newStack[i].definitions);

                        if (newDefinitions.length > oldDefinitions.length) {
                            branchTarget.stackBefore[i] = new StackSlot(newDefinitions);
                            modified = true;
                        }
                    }

                    //
                    // Merge variables; modify the target;
                    //
                    for (int i = 0; i < newVariableState.length; i++) {
                        final VariableSlot oldSlot = branchTarget.variablesBefore[i];
                        final VariableSlot newSlot = newVariableState[i];

                        if (!oldSlot.unknownDefinition) {
                            if (newSlot.unknownDefinition) {
                                branchTarget.variablesBefore[i] = newSlot;
                                modified = true;
                            }
                            else {
                                final ByteCode[] oldDefinitions = oldSlot.definitions;
                                final ByteCode[] newDefinitions = ArrayUtilities.union(oldSlot.definitions, newSlot.definitions);

                                if (newDefinitions.length > oldDefinitions.length) {
                                    branchTarget.variablesBefore[i] = new VariableSlot(newDefinitions);
                                    modified = true;
                                }
                            }
                        }
                    }

                    if (modified) {
                        agenda.push(branchTarget);
                    }
                }
            }
        }

        //
        // Occasionally, compilers or obfuscators may generate unreachable code (which might be intentionally invalid).
        // It should be safe to simply remove it.
        //

        ArrayList<ByteCode> unreachable = null;

        for (final ByteCode byteCode : body) {
            if (byteCode.stackBefore == null) {
                if (unreachable == null) {
                    unreachable = new ArrayList<>();
                }

                unreachable.add(byteCode);
            }
        }

        if (unreachable != null) {
            body.removeAll(unreachable);
        }

        //
        // Generate temporary variables to replace stack values.
        //
        for (final ByteCode byteCode : body) {
            final int popCount = byteCode.popCount != -1 ? byteCode.popCount : byteCode.stackBefore.length;

            int argumentIndex = 0;

            for (int i = byteCode.stackBefore.length - popCount; i < byteCode.stackBefore.length; i++) {
                final Variable tempVariable = new Variable();

                tempVariable.setName(format("stack_%1$02X_%2$d", byteCode.offset, argumentIndex));
                tempVariable.setGenerated(true);

                byteCode.stackBefore[i] = new StackSlot(byteCode.stackBefore[i].definitions, tempVariable);

                for (final ByteCode pushedBy : byteCode.stackBefore[i].definitions) {
                    if (pushedBy.storeTo == null) {
                        pushedBy.storeTo = new ArrayList<>();
                    }

                    pushedBy.storeTo.add(tempVariable);
                }

                argumentIndex++;
            }
        }

        //
        // Try to use a single temporary variable instead of several, if possible (especially useful for DUP).
        // This has to be done after all temporary variables are assigned so we know about all loads.
        //
        for (final ByteCode byteCode : body) {
            if (byteCode.storeTo != null && byteCode.storeTo.size() > 1) {
                final List<Variable> localVariables = byteCode.storeTo;

                //
                // For each of the variables, find the location where it is loaded; there should be exactly one.
                //
                List<StackSlot> loadedBy = null;

                for (final Variable local : localVariables) {
                inner:
                    for (final ByteCode bc : body) {
                        for (final StackSlot s : bc.stackBefore) {
                            if (s.loadFrom == local) {
                                if (loadedBy == null) {
                                    loadedBy = new ArrayList<>();
                                }

                                loadedBy.add(s);
                                break inner;
                            }
                        }
                    }
                }

                if (loadedBy == null) {
                    continue;
                }

                //
                // We know that all the temp variables have a single load; now make sure they have a single store. 
                //
                boolean singleStore = true;

                for (final StackSlot slot : loadedBy) {
                    if (slot.definitions.length != 1 || slot.definitions[0] != byteCode) {
                        singleStore = false;
                        break;
                    }
                }

                if (!singleStore) {
                    continue;
                }

                //
                // We can now reduce everything into a single variable.
                //
                final Variable tempVariable = new Variable();

                tempVariable.setName(format("expr_%1$02X", byteCode.offset));
                tempVariable.setGenerated(true);

                byteCode.storeTo = Collections.singletonList(tempVariable);

                for (final ByteCode bc : body) {
                    for (int i = 0; i < bc.stackBefore.length; i++) {
                        //
                        // Is it one of the variables we merged?
                        //
                        if (localVariables.contains(bc.stackBefore[i].loadFrom)) {
                            //
                            // Replace with the new temp variable.
                            //
                            bc.stackBefore[i] = new StackSlot(bc.stackBefore[i].definitions, tempVariable);
                        }
                    }
                }
            }
        }

        //
        // Split and convert the normal local variables.
        //
        convertLocalVariables(body);

        //
        // Convert branch targets to labels.
        //
        for (final ByteCode byteCode : body) {
            if (byteCode.operand instanceof Instruction[]) {
                final Instruction[] branchTargets = (Instruction[]) byteCode.operand;
                final Label[] newOperand = new Label[branchTargets.length];

                for (int i = 0; i < branchTargets.length; i++) {
                    newOperand[i] = byteCodeMap.get(branchTargets[i]).label;
                }

                byteCode.operand = newOperand;
            }
            else if (byteCode.operand instanceof Instruction) {
                byteCode.operand = byteCodeMap.get(byteCode.operand).label;
            }
            else if (byteCode.operand instanceof SwitchInfo) {
                final SwitchInfo switchInfo = (SwitchInfo) byteCode.operand;
                final Instruction[] branchTargets = ArrayUtilities.prepend(switchInfo.getTargets(), switchInfo.getDefaultTarget());
                final Label[] newOperand = new Label[branchTargets.length];

                for (int i = 0; i < branchTargets.length; i++) {
                    newOperand[i] = byteCodeMap.get(branchTargets[i]).label;
                }

                byteCode.operand = newOperand;
            }
        }

        //
        // Convert parameters to Variables.
        //
//        convertParameters(body);

        return body;
    }

    private final static class VariableInfo {

        final Variable variable;
        final List<ByteCode> definitions;
        final List<ByteCode> references;

        VariableInfo(final Variable variable, final List<ByteCode> definitions, final List<ByteCode> references) {
            this.variable = variable;
            this.definitions = definitions;
            this.references = references;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void convertLocalVariables(final List<ByteCode> body) {
        for (final VariableDefinition variableDefinition : _body.getVariables()) {
            //
            // Find all definitions of and references to this variable.
            //

            final List<ByteCode> definitions = new ArrayList<>();
            final List<ByteCode> references = new ArrayList<>();

            for (final ByteCode b : body) {
                if (b.operand == variableDefinition) {
                    if (b.isVariableDefinition()) {
                        definitions.add(b);
                    }
                    else {
                        references.add(b);
                    }
                }
            }

            final List<VariableInfo> newVariables;
            boolean fromUnknownDefinition = false;

            if (_optimize) {
                for (final ByteCode b : references) {
                    if (b.variablesBefore[variableDefinition.getIndex()].unknownDefinition) {
                        fromUnknownDefinition = true;
                        break;
                    }
                }
            }

            if (!_optimize || fromUnknownDefinition) {
                final Variable variable = new Variable();

                variable.setName(
                    StringUtilities.isNullOrEmpty(variableDefinition.getName()) ? "var_" + variableDefinition.getIndex()
                                                                                : variableDefinition.getName()
                );

                variable.setType(variableDefinition.getVariableType());
                variable.setOriginalVariable(variableDefinition);

                final VariableInfo variableInfo = new VariableInfo(variable, definitions, references);

                newVariables = Collections.singletonList(variableInfo);
            }
            else {
                newVariables = new ArrayList<>();

                for (final ByteCode b : definitions) {
                    final Variable variable = new Variable();

                    variable.setName(
                        format(
                            "%1$s_%2$02X",
                            StringUtilities.isNullOrEmpty(variableDefinition.getName()) ? "var_" + variableDefinition.getIndex()
                                                                                        : variableDefinition.getName(),
                            b.offset
                        )
                    );

                    variable.setType(variableDefinition.getVariableType());
                    variable.setOriginalVariable(variableDefinition);

                    final VariableInfo variableInfo = new VariableInfo(
                        variable,
                        new ArrayList<ByteCode>(),
                        new ArrayList<ByteCode>()
                    );

                    variableInfo.definitions.add(b);
                    newVariables.add(variableInfo);
                }

                //
                // Add loads to the data structure; merge variables if necessary.
                //
                for (final ByteCode ref : references) {
                    final ByteCode[] refDefinitions = ref.variablesBefore[variableDefinition.getIndex()].definitions;

                    if (refDefinitions.length == 1) {
                        VariableInfo newVariable = null;

                        for (final VariableInfo v : newVariables) {
                            if (v.definitions.contains(refDefinitions[0])) {
                                newVariable = v;
                                break;
                            }
                        }

                        assert newVariable != null;

                        newVariable.references.add(ref);
                    }
                    else {
                        final ArrayList<VariableInfo> mergeVariables = new ArrayList<>();

                        for (final VariableInfo v : newVariables) {
                            boolean hasIntersection = false;

                        outer:
                            for (final ByteCode b1 : v.definitions) {
                                for (final ByteCode b2 : refDefinitions) {
                                    if (b1 == b2) {
                                        hasIntersection = true;
                                        break outer;
                                    }
                                }
                            }

                            if (hasIntersection) {
                                mergeVariables.add(v);
                            }
                        }

                        final ArrayList<ByteCode> mergedDefinitions = new ArrayList<>();
                        final ArrayList<ByteCode> mergedReferences = new ArrayList<>();

                        for (final VariableInfo v : mergeVariables) {
                            mergedDefinitions.addAll(v.definitions);
                            mergedReferences.addAll(v.references);
                        }

                        final VariableInfo mergedVariable = new VariableInfo(
                            mergeVariables.get(0).variable,
                            mergedDefinitions,
                            mergedReferences
                        );

                        mergedVariable.references.add(ref);
                        newVariables.removeAll(mergeVariables);
                        newVariables.add(mergedVariable);
                    }
                }
            }

            //
            // Set bytecode operands.
            //
            for (final VariableInfo newVariable : newVariables) {
                for (final ByteCode definition : newVariable.definitions) {
                    definition.operand = newVariable.variable;
                }
                for (final ByteCode reference : newVariable.references) {
                    reference.operand = newVariable.variable;
                }
            }
        }
    }

/*
    final List<Variable> parameters = new ArrayList<>();

    private void convertParameters(final List<ByteCode> body) {
        Variable thisParameter = null;

        if (_body.hasThis()) {
            final TypeReference type = _methodDefinition.getDeclaringType();

            thisParameter = new Variable();
            thisParameter.setName("this");
            thisParameter.setType(type);
            thisParameter.setOriginalParameter(_body.getThisParameter());
        }

        for (final ParameterDefinition parameter : _methodDefinition.getParameters()) {
            final Variable variable = new Variable();

            variable.setName(parameter.getName());
            variable.setType(parameter.getParameterType());
            variable.setOriginalParameter(parameter);

            this.parameters.add(variable);
        }

        for (final ByteCode byteCode : body) {
            ParameterDefinition p;
            
            switch (byteCode.code) {
                case __ILoad:
                case __LLoad:
                case __FLoad:
                case __DLoad:
            }
        }
    }
*/

    @SuppressWarnings("ConstantConditions")
    private List<Node> convertToAst(final List<ByteCode> body, final HashSet<ExceptionHandler> exceptionHandlers) {
        final ArrayList<Node> ast = new ArrayList<>();

        while (!exceptionHandlers.isEmpty()) {
            final TryCatchBlock tryCatchBlock = new TryCatchBlock();

            //
            // Find the first and widest scope;
            //

            int tryStart = Integer.MAX_VALUE;
            int tryEnd = -1;

            for (final ExceptionHandler handler : exceptionHandlers) {
                final int start = handler.getTryBlock().getFirstInstruction().getOffset();

                if (start < tryStart) {
                    tryStart = start;
                }
            }

            for (final ExceptionHandler handler : exceptionHandlers) {
                final int start = handler.getTryBlock().getFirstInstruction().getOffset();

                if (start == tryStart) {
                    final int end = handler.getTryBlock().getLastInstruction().getEndOffset();

                    if (end > tryEnd) {
                        tryEnd = end;
                    }
                }
            }

            final ArrayList<ExceptionHandler> handlers = new ArrayList<>();

            for (final ExceptionHandler handler : exceptionHandlers) {
                final int start = handler.getTryBlock().getFirstInstruction().getOffset();
                final int end = handler.getTryBlock().getLastInstruction().getEndOffset();

                if (start == tryStart && end == tryEnd) {
                    handlers.add(handler);
                }
            }

            Collections.sort(
                handlers,
                new Comparator<ExceptionHandler>() {
                    @Override
                    public int compare(final ExceptionHandler o1, final ExceptionHandler o2) {
                        return Integer.compare(
                            o1.getTryBlock().getFirstInstruction().getOffset(),
                            o2.getTryBlock().getFirstInstruction().getOffset()
                        );
                    }
                }
            );

            //
            // Remember that any part of the body might have been removed due to unreachability.
            //

            //
            // Cut all instructions up to the try block.
            //
            int tryStartIndex = 0;

            while (tryStartIndex < body.size() &&
                   body.get(tryStartIndex).offset < tryStart) {

                tryStartIndex++;
            }

            ast.addAll(convertToAst(body.subList(0, tryStartIndex)));

            for (int i = 0; i < tryStartIndex; i++) {
                body.remove(0);
            }

            //
            // Cut the try block.
            //
            {
                final HashSet<ExceptionHandler> nestedHandlers = new HashSet<>();

                for (final ExceptionHandler eh : exceptionHandlers) {
                    final int ts = eh.getTryBlock().getFirstInstruction().getOffset();
                    final int te = eh.getTryBlock().getLastInstruction().getEndOffset();

                    if (tryStart <= ts && te < tryEnd ||
                        tryStart < ts && te <= tryEnd) {

                        nestedHandlers.add(eh);
                    }
                }

                exceptionHandlers.removeAll(nestedHandlers);

                int tryEndIndex = 0;

                while (tryEndIndex < body.size() && body.get(tryEndIndex).offset < tryEnd) {
                    tryEndIndex++;
                }

                final Block tryBlock = new Block();
                final ArrayList<ByteCode> tryBody = new ArrayList<>(body.subList(0, tryEndIndex));

                for (int i = 0; i < tryEndIndex; i++) {
                    body.remove(0);
                }

                tryBlock.getBody().addAll(convertToAst(tryBody, nestedHandlers));
                tryCatchBlock.setTryBlock(tryBlock);
            }

            //
            // Cut all handlers.
            //
            for (final ExceptionHandler eh : handlers) {
                final ExceptionBlock handlerBlock = eh.getHandlerBlock();

                final int handlerStart = handlerBlock.getFirstInstruction().getOffset();

                final int handlerEnd = handlerBlock.getLastInstruction() != null
                                       ? handlerBlock.getLastInstruction().getEndOffset()
                                       : _body.getCodeSize();

                int startIndex = 0;

                while (startIndex < body.size() &&
                       body.get(startIndex).offset < handlerStart) {

                    startIndex++;
                }

                int endIndex = startIndex;

                while (endIndex < body.size() &&
                       body.get(endIndex).offset < handlerEnd) {

                    endIndex++;
                }

                final HashSet<ExceptionHandler> nestedHandlers = new HashSet<>();

                for (final ExceptionHandler e : exceptionHandlers) {
                    final int ts = eh.getTryBlock().getFirstInstruction().getOffset();
                    final int te = eh.getTryBlock().getLastInstruction().getEndOffset();

                    if (handlerStart <= ts && te < handlerEnd ||
                        handlerStart < ts && te <= handlerEnd) {

                        nestedHandlers.add(e);
                    }
                }

                exceptionHandlers.removeAll(nestedHandlers);

                final ArrayList<ByteCode> handlerCode = new ArrayList<>(body.subList(startIndex, endIndex));

                for (int i = startIndex; i < endIndex; i++) {
                    body.remove(startIndex);
                }

                final List<Node> handlerAst = convertToAst(handlerCode, nestedHandlers);

                if (eh.isCatch()) {
                    final CatchBlock catchBlock = new CatchBlock();

                    catchBlock.setExceptionType(eh.getCatchType());
                    catchBlock.getBody().addAll(handlerAst);

                    final ByteCode loadException = _loadExceptions.get(eh);

                    if (loadException.storeTo == null || loadException.storeTo.isEmpty()) {
                        //
                        // Exception is not used.
                        //
                        catchBlock.setExceptionVariable(null);
                    }
                    else if (loadException.storeTo.size() == 1) {
                        if (catchBlock.getBody().get(0) instanceof Expression) {
                            final Expression first = (Expression) catchBlock.getBody().get(0);
                            final AstCode firstCode = first.getCode();
                            final Expression firstArgument = first.getArguments().get(0);

                            if (firstCode == AstCode.Pop &&
                                firstArgument.getCode() == AstCode.Load &&
                                firstArgument.getOperand() == loadException.storeTo.get(0)) {

                                //
                                // The exception is just popped; optimize it away.
                                //
                                if (_context.getSettings().getAlwaysGenerateExceptionVariableForCatchBlocks()) {
                                    final Variable exceptionVariable = new Variable();

                                    exceptionVariable.setName(format("ex_%1$02X", handlerStart));
                                    exceptionVariable.setGenerated(true);

                                    catchBlock.setExceptionVariable(exceptionVariable);
                                }
                                else {
                                    catchBlock.setExceptionVariable(null);
                                }
                            }
                            else {
                                catchBlock.setExceptionVariable(loadException.storeTo.get(0));
                            }
                        }
                    }
                    else {
                        final Variable exceptionTemp = new Variable();

                        exceptionTemp.setName(format("ex_%1$02X", handlerStart));
                        exceptionTemp.setGenerated(true);

                        catchBlock.setExceptionVariable(exceptionTemp);

                        for (final Variable storeTo : loadException.storeTo) {
                            catchBlock.getBody().add(
                                0,
                                new Expression(AstCode.Store, storeTo, new Expression(AstCode.Load, exceptionTemp))
                            );
                        }
                    }

                    tryCatchBlock.getCatchBlocks().add(catchBlock);
                }
                else if (eh.isFinally()) {
                    final Block finallyBlock = new Block();

                    finallyBlock.getBody().addAll(handlerAst);
                    tryCatchBlock.setFinallyBlock(finallyBlock);
                }
            }

            exceptionHandlers.removeAll(handlers);
            ast.add(tryCatchBlock);
        }

        ast.addAll(convertToAst(body));

        return ast;
    }

    private List<Node> convertToAst(final List<ByteCode> body) {
        final ArrayList<Node> ast = new ArrayList<>();

        //
        // Convert stack-based bytecode to bytecode AST.
        //
        for (final ByteCode byteCode : body) {
            final Range codeRange = new Range(byteCode.offset, byteCode.endOffset);

            if (byteCode.stackBefore == null) {
                //
                // Unreachable code.
                //
                continue;
            }

            final Expression expression = new Expression(byteCode.code, byteCode.operand);

            if (byteCode.secondOperand != null) {
                expression.getArguments().add(new Expression(AstCode.LdC, byteCode.secondOperand));
            }

            expression.getRanges().add(codeRange);

            //
            // Include the instruction's label, if it has one.
            //
            if (byteCode.label != null) {
                ast.add(byteCode.label);
            }

            //
            // Reference arguments using temporary variables.
            //

            final int popCount = byteCode.popCount != -1 ? byteCode.popCount
                                                         : byteCode.stackBefore.length;

            for (int i = byteCode.stackBefore.length - popCount; i < byteCode.stackBefore.length; i++) {
                final StackSlot slot = byteCode.stackBefore[i];
                expression.getArguments().add(new Expression(AstCode.Load, slot.loadFrom));
            }

            //
            // Store the result to temporary variables, if needed.
            //
            if (byteCode.storeTo == null || byteCode.storeTo.isEmpty()) {
                ast.add(expression);
            }
            else if (byteCode.storeTo.size() == 1) {
                ast.add(new Expression(AstCode.Store, byteCode.storeTo.get(0), expression));
            }
            else {
                final Variable tempVariable = new Variable();

                tempVariable.setName(format("expr_%1$02X", byteCode.offset));
                tempVariable.setGenerated(true);

                ast.add(new Expression(AstCode.Store, tempVariable, expression));

                for (int i = byteCode.storeTo.size(); i >= 0; i--) {
                    ast.add(
                        new Expression(
                            AstCode.Store,
                            byteCode.storeTo.get(i),
                            new Expression(AstCode.Load, tempVariable)
                        )
                    );
                }
            }
        }

        return ast;
    }

    // <editor-fold defaultstate="collapsed" desc="StackSlot Class">

    private final static class StackSlot {
        final ByteCode[] definitions;
        final Variable loadFrom;

        public StackSlot(final ByteCode[] definitions) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.loadFrom = null;
        }

        public StackSlot(final ByteCode[] definitions, final Variable loadFrom) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.loadFrom = loadFrom;
        }

        public static StackSlot[] modifyStack(
            final StackSlot[] stack,
            final int popCount,
            final int pushCount,
            final ByteCode pushDefinition) {

            VerifyArgument.notNull(stack, "stack");
            VerifyArgument.isNonNegative(popCount, "popCount");
            VerifyArgument.isNonNegative(pushCount, "pushCount");

            final StackSlot[] newStack = new StackSlot[stack.length - popCount + pushCount];

            System.arraycopy(stack, 0, newStack, 0, stack.length - popCount);

            for (int i = stack.length - popCount; i < newStack.length; i++) {
                newStack[i] = new StackSlot(new ByteCode[] { pushDefinition });
            }

            return newStack;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="VariableSlot Class">

    private final static class VariableSlot {
        final static VariableSlot UNKNOWN_INSTANCE = new VariableSlot(new ByteCode[0], true);
        final ByteCode[] definitions;
        final boolean unknownDefinition;

        public VariableSlot(final ByteCode[] definitions) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.unknownDefinition = false;
        }

        public VariableSlot(final ByteCode[] definitions, final boolean unknownDefinition) {
            this.definitions = VerifyArgument.notNull(definitions, "definitions");
            this.unknownDefinition = unknownDefinition;
        }

        public static VariableSlot[] cloneVariableState(final VariableSlot[] state) {
            return VerifyArgument.notNull(state, "state").clone();
        }

        public static VariableSlot[] makeUnknownState(final int variableCount) {
            final VariableSlot[] unknownVariableState = new VariableSlot[variableCount];

            for (int i = 0; i < variableCount; i++) {
                unknownVariableState[i] = UNKNOWN_INSTANCE;
            }

            return unknownVariableState;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="ByteCode Class">

    private final static class ByteCode {
        Label label;
        int offset;
        int endOffset;
        AstCode code;
        Object operand;
        Object secondOperand;
        int popCount = -1;
        int pushCount;
        ByteCode next;
        StackSlot[] stackBefore;
        VariableSlot[] variablesBefore;
        List<Variable> storeTo;

        public final String name() {
            return format("#%1$04d", offset);
        }

        public final boolean isVariableDefinition() {
            return code == AstCode.Store;
        }

        @Override
        public final String toString() {
            final StringBuilder sb = new StringBuilder();

            //
            // Label
            //
            sb.append(name()).append(':');

            if (label != null) {
                sb.append('*');
            }

            //
            // Name
            //
            sb.append(' ');
            sb.append(code.getName());

            if (operand != null) {
                sb.append(' ');

                if (operand instanceof Instruction) {
                    sb.append(format("#%1$04d", ((Instruction) operand).getOffset()));
                }
                else if (operand instanceof Instruction[]) {
                    for (final Instruction instruction : (Instruction[]) operand) {
                        sb.append(format("#%1$04d", instruction.getOffset()));
                        sb.append(' ');
                    }
                }
                else if (operand instanceof Label) {
                    sb.append(((Label) operand).getName());
                }
                else if (operand instanceof Label[]) {
                    for (final Label l : (Label[]) operand) {
                        sb.append(l.getName());
                        sb.append(' ');
                    }
                }
                else {
                    sb.append(operand);
                }
            }

            if (stackBefore != null) {
                sb.append(" StackBefore={");

                for (int i = 0; i < stackBefore.length; i++) {
                    if (i != 0) {
                        sb.append(',');
                    }

                    final StackSlot slot = stackBefore[i];
                    final ByteCode[] definitions = slot.definitions;

                    for (int j = 0; j < definitions.length; j++) {
                        if (j != 0) {
                            sb.append('|');
                        }
                        sb.append(format("#%1$04d", definitions[j].offset));
                    }
                }

                sb.append('}');
            }

            if (storeTo != null && !storeTo.isEmpty()) {
                sb.append(" StoreTo={");

                for (int i = 0; i < storeTo.size(); i++) {
                    if (i != 0) {
                        sb.append(',');
                    }
                    sb.append(storeTo.get(i).getName());
                }

                sb.append('}');
            }

            if (variablesBefore != null) {
                sb.append(" VariablesBefore={");

                for (int i = 0; i < variablesBefore.length; i++) {
                    if (i != 0) {
                        sb.append(',');
                    }

                    final VariableSlot slot = variablesBefore[i];

                    if (slot.unknownDefinition) {
                        sb.append('?');
                    }
                    else {
                        final ByteCode[] definitions = slot.definitions;
                        for (int j = 0; j < definitions.length; j++) {
                            if (j != 0) {
                                sb.append('|');
                            }
                            sb.append(format("#%1$04d", definitions[j].offset));
                        }
                    }
                }

                sb.append('}');
            }

            return sb.toString();
        }
    }

    // </editor-fold>
}
