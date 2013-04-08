/*
 * MethodPrinter.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.metadata;

import com.strobel.assembler.DisassemblerOptions;
import com.strobel.assembler.ir.ErrorOperand;
import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.Frame;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionCollection;
import com.strobel.assembler.ir.InstructionVisitor;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.ir.OpCodeHelpers;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.ExceptionsAttribute;
import com.strobel.assembler.ir.attributes.LocalVariableTableAttribute;
import com.strobel.assembler.ir.attributes.LocalVariableTableEntry;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ITextOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class MethodPrinter implements MethodVisitor {
    private final ITextOutput _output;
    private final long _flags;
    private final String _name;
    private final IMethodSignature _signature;
    private final TypeReference[] _thrownTypes;
    private final DisassemblerOptions _options;

    private MethodBody _body;
    private int[] _lineNumbers;

    public MethodPrinter(
        final ITextOutput printer,
        final DisassemblerOptions options,
        final long flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        _output = VerifyArgument.notNull(printer, "printer");
        _options = options;
        _flags = flags;
        _name = VerifyArgument.notNull(name, "name");
        _signature = signature;
        _thrownTypes = thrownTypes;

        printDescription();
    }

    private void printDescription() {
        final List<String> flagStrings = new ArrayList<>();

        if ("<clinit>".equals(_name)) {
            _output.write("static {}");
        }
        else {
            final EnumSet<Flags.Flag> flagSet = Flags.asFlagSet(_flags & Flags.MethodFlags);

            for (final Flags.Flag flag : flagSet) {
                flagStrings.add(flag.toString());
            }

            if (flagSet.size() > 0) {
                _output.write(StringUtilities.join(" ", flagStrings));
                _output.write(' ');
            }

            final List<GenericParameter> genericParameters = _signature.getGenericParameters();

            if (!genericParameters.isEmpty()) {
                _output.write('<');

                for (int i = 0; i < genericParameters.size(); i++) {
                    if (i != 0) {
                        _output.write(", ");
                    }

                    _output.write(genericParameters.get(i).getBriefDescription());
                }

                _output.write('>');
            }

            _output.write(_signature.getReturnType().getBriefDescription());
            _output.write(' ');
            _output.write(_name);
            _output.write('(');

            final List<ParameterDefinition> parameters = _signature.getParameters();

            for (int i = 0; i < parameters.size(); i++) {
                if (i != 0) {
                    _output.write(", ");
                }

                final ParameterDefinition parameter = parameters.get(i);

                if (Flags.testAny(_flags, Flags.ACC_VARARGS) && i == parameters.size() - 1) {
                    _output.write(parameter.getParameterType().getUnderlyingType().getBriefDescription());
                    _output.write("...");
                }
                else {
                    _output.write(parameter.getParameterType().getBriefDescription());
                }

                _output.write(' ');

                final String parameterName = parameter.getName();

                if (StringUtilities.isNullOrEmpty(parameterName)) {
                    _output.write("p%d", i);
                }
                else {
                    _output.write(parameterName);
                }
            }

            _output.write(')');

            if (_thrownTypes != null && _thrownTypes.length > 0) {
                _output.write(" throws ");

                for (int i = 0; i < _thrownTypes.length; i++) {
                    if (i != 0) {
                        _output.write(", ");
                    }

                    _output.write(_thrownTypes[i].getBriefDescription());
                }
            }
        }
        _output.writeLine(";");

        flagStrings.clear();

        for (final Flags.Flag flag : Flags.asFlagSet(_flags & (Flags.MethodFlags | ~Flags.StandardFlags))) {
            flagStrings.add(flag.name());
        }

        if (flagStrings.isEmpty()) {
            return;
        }

        _output.write("  Flags: %s", StringUtilities.join(", ", flagStrings));
        _output.writeLine();
    }

    @Override
    public boolean canVisitBody() {
        return true;
    }

    @Override
    public InstructionVisitor visitBody(final MethodBody body) {
        _body = body;
        _output.writeLine("  Code:");

        _output.write(
            "    stack=%d, locals=%d, arguments=%d\n",
            body.getMaxStackSize(),
            body.getMaxLocals(),
            _signature.getParameters().size()
        );

        final InstructionCollection instructions = body.getInstructions();

        if (!instructions.isEmpty()) {
            _lineNumbers = new int[body.getCodeSize()];
            Arrays.fill(_lineNumbers, -1);
        }

        return new InstructionPrinter();
    }

    @Override
    public void visitEnd() {
        if (_body == null) {
            return;
        }

        final List<ExceptionHandler> handlers = _body.getExceptionHandlers();

        if (!handlers.isEmpty()) {
            int longestType = "Type".length();

            for (final ExceptionHandler handler : handlers) {
                final TypeReference catchType = handler.getCatchType();

                if (catchType != null) {
                    final String signature = catchType.getSignature();

                    if (signature.length() > longestType) {
                        longestType = signature.length();
                    }
                }
            }

            _output.write("  Exceptions:");
            _output.writeLine();
            _output.write("    Try           Handler");
            _output.writeLine();
            _output.write("    Start  End    Start  End    %1$-" + longestType + "s", "Type");
            _output.writeLine();

            _output.write(
                "    -----  -----  -----  -----  %1$-" + longestType + "s",
                StringUtilities.repeat('-', longestType)
            );

            _output.writeLine();

            for (final ExceptionHandler handler : handlers) {
                final String signature;
                final TypeReference catchType = handler.getCatchType();

                if (catchType != null) {
                    signature = catchType.getSignature();
                }
                else {
                    signature = "Any";
                }

                _output.write(
                    "    %1$-5d  %2$-5d  %3$-5d  %4$-5d  %5$-" + longestType + "s",
                    handler.getTryBlock().getFirstInstruction().getOffset(),
                    handler.getTryBlock().getLastInstruction().getEndOffset(),
                    handler.getHandlerBlock().getFirstInstruction().getOffset(),
                    handler.getHandlerBlock().getLastInstruction().getEndOffset(),
                    signature
                );

                _output.writeLine();
            }
        }
    }

    @Override
    public void visitFrame(final Frame frame) {
    }

    @Override
    public void visitLineNumber(final Instruction instruction, final int lineNumber) {
        if (_options != null &&
            _options.getPrintLineNumbers() &&
            _lineNumbers != null &&
            lineNumber >= 0) {

            _lineNumbers[instruction.getOffset()] = lineNumber;
        }
    }

    @Override
    public void visitAttribute(final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.Exceptions: {
                final ExceptionsAttribute exceptionsAttribute = (ExceptionsAttribute) attribute;
                final List<TypeReference> exceptionTypes = exceptionsAttribute.getExceptionTypes();

                if (!exceptionTypes.isEmpty()) {
                    _output.writeLine("  Exceptions:");
                    for (final TypeReference exceptionType : exceptionTypes) {
                        _output.write("    throws %s", exceptionType.getBriefDescription());
                        _output.writeLine();
                    }
                }

                break;
            }

            case AttributeNames.LocalVariableTable:
            case AttributeNames.LocalVariableTypeTable: {
                final LocalVariableTableAttribute localVariables = (LocalVariableTableAttribute) attribute;
                final List<LocalVariableTableEntry> entries = localVariables.getEntries();

                int longestName = "Name".length();
                int longestSignature = "Signature".length();

                for (final LocalVariableTableEntry entry : entries) {
                    final String name = entry.getName();
                    final String signature;
                    final TypeReference type = entry.getType();

                    if (type != null) {
                        if (attribute.getName().equals(AttributeNames.LocalVariableTypeTable)) {
                            signature = type.getSignature();
                        }
                        else {
                            signature = type.getErasedSignature();
                        }

                        if (signature.length() > longestSignature) {
                            longestSignature = signature.length();
                        }
                    }

                    if (name != null && name.length() > longestName) {
                        longestName = name.length();
                    }
                }

                _output.write("  %s:", attribute.getName());
                _output.writeLine();
                _output.write("    Start  Length  Slot  %1$-" + longestName + "s  Signature", "Name");
                _output.writeLine();

                _output.write(
                    "    -----  ------  ----  %1$-" + longestName + "s  %2$-" + longestSignature + "s",
                    StringUtilities.repeat('-', longestName),
                    StringUtilities.repeat('-', longestSignature)
                );

                _output.writeLine();

                for (final LocalVariableTableEntry entry : entries) {
                    final String signature;

                    if (attribute.getName().equals(AttributeNames.LocalVariableTypeTable)) {
                        signature = entry.getType().getSignature();
                    }
                    else {
                        signature = entry.getType().getErasedSignature();
                    }

                    _output.write(
                        "    %1$-5d  %2$-6d  %3$-4d  %4$-" + longestName + "s  %5$s",
                        entry.getScopeOffset(),
                        entry.getScopeLength(),
                        entry.getIndex(),
                        entry.getName(),
                        signature
                    );

                    _output.writeLine();
                }

                break;
            }

            case AttributeNames.Signature: {
                _output.writeLine("  Signature:");
                _output.write("    %s", ((SignatureAttribute) attribute).getSignature());
                _output.writeLine();
                break;
            }
        }
    }

    @Override
    public void visitAnnotation(final CustomAnnotation annotation, final boolean visible) {
    }

    // <editor-fold defaultstate="collapsed" desc="InstructionPrinter Class">

    private static final int MAX_OPCODE_LENGTH;
    private static final String[] OPCODE_NAMES;
    private static final String LINE_NUMBER_CODE = "linenumber";

    static {
        int maxLength = LINE_NUMBER_CODE.length();

        final OpCode[] values = OpCode.values();
        final String[] names = new String[values.length];

        for (int i = 0; i < values.length; i++) {
            final OpCode op = values[i];
            final int length = op.name().length();

            if (length > maxLength) {
                maxLength = length;
            }

            names[i] = op.name().toLowerCase();
        }

        MAX_OPCODE_LENGTH = maxLength;
        OPCODE_NAMES = names;
    }

    private final class InstructionPrinter implements InstructionVisitor {
        private void printOpCode(final OpCode opCode) {
            switch (opCode) {
                case TABLESWITCH:
                case LOOKUPSWITCH:
                    _output.writeReference(OPCODE_NAMES[opCode.ordinal()], opCode);
                    break;

                default:
                    _output.writeReference(String.format("%1$-" + MAX_OPCODE_LENGTH + "s", OPCODE_NAMES[opCode.ordinal()]), opCode);
                    break;
            }
        }

        @Override
        public void visit(final Instruction instruction) {
            VerifyArgument.notNull(instruction, "instruction");

            if (_lineNumbers != null) {
                final int lineNumber = _lineNumbers[instruction.getOffset()];

                if (lineNumber >= 0) {
                    _output.write(
                        "          %1$-" + MAX_OPCODE_LENGTH + "s %2$d",
                        LINE_NUMBER_CODE,
                        lineNumber
                    );
                    endLine();
                }
            }

            try {
                _output.writeLabel(String.format("%1$8d", instruction.getOffset()));
                _output.write(": ");
                instruction.accept(this);
            }
            catch (Throwable t) {
                printOpCode(instruction.getOpCode());

                boolean foundError = false;

                for (int i = 0; i < instruction.getOperandCount(); i++) {
                    final Object operand = instruction.getOperand(i);

                    if (operand instanceof ErrorOperand) {
                        _output.write(String.valueOf(operand));
                        foundError = true;
                        break;
                    }
                }

                if (!foundError) {
                    _output.write("!!! ERROR");
                }

                endLine();
            }
        }

        @Override
        public void visit(final OpCode op) {
            printOpCode(op);

            final int argumentIndex = OpCodeHelpers.getLoadStoreMacroArgumentIndex(op);

            if (argumentIndex >= 0) {
                final VariableDefinitionCollection variables = _body.getVariables();

                if (argumentIndex < variables.size()) {
                    final VariableDefinition variable = variables.get(argumentIndex);

                    if (variable.hasName()) {
                        _output.writeComment(" /* %s */", variable.getName());
                    }
                }
            }

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final TypeReference value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeReference(value.getErasedSignature(), value);
            _output.write(".class");

            endLine();
        }

        private void endLine() {
            _output.writeLine();
        }

        @Override
        public void visitConstant(final OpCode op, final int value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final long value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final float value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final double value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final String value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(StringUtilities.escape(value, true));

            endLine();
        }

        @Override
        public void visitBranch(final OpCode op, final Instruction target) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLabel(String.valueOf(target.getOffset()));

            endLine();
        }

        @Override
        public void visitVariable(final OpCode op, final VariableReference variable) {
            printOpCode(op);

            _output.write(' ');
            _output.writeReference(variable.getName(), variable);

            endLine();
        }

        @Override
        public void visitVariable(final OpCode op, final VariableReference variable, final int operand) {
            printOpCode(op);
            _output.write(' ');

            if (StringUtilities.isNullOrEmpty(variable.getName())) {
                _output.writeReference("$" + variable.getSlot(), variable);
            }
            else {
                _output.writeReference(variable.getName(), variable);
            }

            _output.write(", ");
            _output.writeLiteral(String.valueOf(operand));

            endLine();
        }

        @Override
        public void visitType(final OpCode op, final TypeReference type) {
            printOpCode(op);

            _output.write(' ');
            _output.writeReference(type.getSignature(), type);

            endLine();
        }

        @Override
        public void visitMethod(final OpCode op, final MethodReference method) {
            printOpCode(op);

            _output.write(' ');
            _output.writeReference(method.getDeclaringType().getInternalName(), method.getDeclaringType());
            _output.write('.');
            _output.writeReference(method.getName(), method);
            _output.write(':');
            _output.write(method.getErasedSignature());

            endLine();
        }

        @Override
        public void visitField(final OpCode op, final FieldReference field) {
            printOpCode(op);

            _output.write(' ');
            _output.writeReference(field.getDeclaringType().getInternalName(), field.getDeclaringType());
            _output.write('.');
            _output.writeReference(field.getName(), field);
            _output.write(':');
            _output.write(field.getErasedSignature());

            endLine();
        }

        @Override
        public void visitLabel(final Label label) {
        }

        @Override
        public void visitSwitch(final OpCode op, final SwitchInfo switchInfo) {
            printOpCode(op);
            _output.write(" {");
            endLine();

            switch (op) {
                case TABLESWITCH: {
                    final Instruction[] targets = switchInfo.getTargets();

                    int caseValue = switchInfo.getLowValue();

                    for (final Instruction target : targets) {
                        _output.writeLiteral(switchInfo.getLowValue() + caseValue++);
                        _output.write(": ");
                        _output.writeLabel(String.valueOf(target.getOffset()));
                        _output.writeLine();
                    }

                    _output.writeKeyword("default");
                    _output.write(": ");
                    _output.writeLabel(String.valueOf(switchInfo.getDefaultTarget().getOffset()));
                    _output.writeLine();

                    break;
                }

                case LOOKUPSWITCH: {
                    final int[] keys = switchInfo.getKeys();
                    final Instruction[] targets = switchInfo.getTargets();

                    for (int i = 0; i < keys.length; i++) {
                        final int key = keys[i];
                        final Instruction target = targets[i];

                        _output.writeLiteral(key);
                        _output.write(": ");
                        _output.writeLabel(String.valueOf(target.getOffset()));
                        _output.writeLine();
                    }

                    _output.writeKeyword("default");
                    _output.write(": ");
                    _output.writeLabel(String.valueOf(switchInfo.getDefaultTarget().getOffset()));
                    _output.writeLine();

                    break;
                }
            }

            _output.write("          }");
            endLine();
        }

        @Override
        public void visitEnd() {
        }
    }

    // </editor-fold>
}

