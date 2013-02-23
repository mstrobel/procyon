/*
 * MethodPrinter.java
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

package com.strobel.assembler.metadata;

import com.strobel.assembler.CodePrinter;
import com.strobel.assembler.DisassemblerOptions;
import com.strobel.assembler.ir.*;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.ExceptionsAttribute;
import com.strobel.assembler.ir.attributes.LocalVariableTableAttribute;
import com.strobel.assembler.ir.attributes.LocalVariableTableEntry;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class MethodPrinter implements MethodVisitor {
    private final CodePrinter _printer;
    private final long _flags;
    private final String _name;
    private final IMethodSignature _signature;
    private final TypeReference[] _thrownTypes;
    private final DisassemblerOptions _options;

    private MethodBody _body;
    private int[] _lineNumbers;

    public MethodPrinter(
        final CodePrinter printer,
        final DisassemblerOptions options,
        final long flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        _printer = VerifyArgument.notNull(printer, "printer");
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
            _printer.print("static {}");
        }
        else {
            final EnumSet<Flags.Flag> flagSet = Flags.asFlagSet(_flags & Flags.MethodFlags);

            for (final Flags.Flag flag : flagSet) {
                flagStrings.add(flag.toString());
            }

            if (flagSet.size() > 0) {
                _printer.printf(StringUtilities.join(" ", flagStrings));
                _printer.print(' ');
            }

            final List<GenericParameter> genericParameters = _signature.getGenericParameters();

            if (!genericParameters.isEmpty()) {
                _printer.print('<');

                for (int i = 0; i < genericParameters.size(); i++) {
                    if (i != 0) {
                        _printer.print(", ");
                    }

                    _printer.print(genericParameters.get(i).getBriefDescription());
                }

                _printer.print('>');
            }

            _printer.printf(_signature.getReturnType().getBriefDescription());
            _printer.print(' ');
            _printer.printf(_name);
            _printer.print('(');

            final List<ParameterDefinition> parameters = _signature.getParameters();

            for (int i = 0; i < parameters.size(); i++) {
                if (i != 0) {
                    _printer.print(", ");
                }

                final ParameterDefinition parameter = parameters.get(i);

                if (Flags.testAny(_flags, Flags.ACC_VARARGS) && i == parameters.size() - 1) {
                    _printer.print(parameter.getParameterType().getUnderlyingType().getBriefDescription());
                    _printer.print("...");
                }
                else {
                    _printer.print(parameter.getParameterType().getBriefDescription());
                }

                _printer.print(' ');

                final String parameterName = parameter.getName();

                if (StringUtilities.isNullOrEmpty(parameterName)) {
                    _printer.printf("p%d", i);
                }
                else {
                    _printer.print(parameterName);
                }
            }

            _printer.print(')');

            if (_thrownTypes != null && _thrownTypes.length > 0) {
                _printer.print(" throws ");

                for (int i = 0; i < _thrownTypes.length; i++) {
                    if (i != 0) {
                        _printer.print(", ");
                    }

                    _printer.print(_thrownTypes[i].getBriefDescription());
                }
            }
        }
        _printer.println(";");

        flagStrings.clear();

        for (final Flags.Flag flag : Flags.asFlagSet(_flags & (Flags.MethodFlags | ~Flags.StandardFlags))) {
            flagStrings.add(flag.name());
        }

//        if (Flags.testAny(_flags, Flags.PUBLIC)) {
//            flagStrings.add("ACC_PUBLIC");
//        }
//
//        if (Flags.testAny(_flags, Flags.PROTECTED)) {
//            flagStrings.add("ACC_PROTECTED");
//        }
//
//        if (Flags.testAny(_flags, Flags.PRIVATE)) {
//            flagStrings.add("ACC_PRIVATE");
//        }
//
//        if (Flags.testAny(_flags, Flags.ACC_SUPER)) {
//            flagStrings.add("ACC_SUPER");
//        }
//
//        if (Flags.testAny(_flags, Flags.ACC_BRIDGE)) {
//            flagStrings.add("ACC_BRIDGE");
//        }
//
//        if (Flags.testAny(_flags, Flags.ACC_VARARGS)) {
//            flagStrings.add("ACC_VARARGS");
//        }
//
//        if (Flags.testAny(_flags, Flags.ACC_SYNTHETIC)) {
//            flagStrings.add("ACC_SYNTHETIC");
//        }

        if (flagStrings.isEmpty()) {
            return;
        }

        _printer.printf("  Flags: %s", StringUtilities.join(", ", flagStrings));
        _printer.println();
    }

    @Override
    public boolean canVisitBody() {
        return true;
    }

    @Override
    public InstructionVisitor visitBody(final MethodBody body) {
        _body = body;
        _printer.println("  Code:");

        _printer.printf(
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
                    _printer.println("  Exceptions:");
                    for (final TypeReference exceptionType : exceptionTypes) {
                        _printer.printf("    throws %s", exceptionType.getBriefDescription());
                        _printer.println();
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

                _printer.printf("  %s:", attribute.getName());
                _printer.println();
                _printer.printf("    Start  Length  Slot  %1$-" + longestName + "s  Signature", "Name");
                _printer.println();

                _printer.printf(
                    "    -----  ------  ----  %1$-" + longestName + "s  %2$-" + longestSignature + "s",
                    StringUtilities.repeat('-', longestName),
                    StringUtilities.repeat('-', longestSignature)
                );

                _printer.println();

                for (final LocalVariableTableEntry entry : entries) {
                    final String signature;

                    if (attribute.getName().equals(AttributeNames.LocalVariableTypeTable)) {
                        signature = entry.getType().getSignature();
                    }
                    else {
                        signature = entry.getType().getErasedSignature();
                    }

                    _printer.printf(
                        "    %1$-5d  %2$-6d  %3$-4d  %4$-" + longestName + "s  %5$s",
                        entry.getScopeOffset(),
                        entry.getScopeLength(),
                        entry.getIndex(),
                        entry.getName(),
                        signature
                    );

                    _printer.println();
                }

                break;
            }

            case AttributeNames.Signature: {
                _printer.println("  Signature:");
                _printer.printf("    %s", ((SignatureAttribute) attribute).getSignature());
                _printer.println();
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
                    _printer.print(OPCODE_NAMES[opCode.ordinal()]);
                    break;

                default:
                    _printer.printf("%1$-" + MAX_OPCODE_LENGTH + "s", OPCODE_NAMES[opCode.ordinal()]);
                    break;
            }
        }

        @Override
        public void visit(final Instruction instruction) {
            VerifyArgument.notNull(instruction, "instruction");

            if (_lineNumbers != null) {
                final int lineNumber = _lineNumbers[instruction.getOffset()];

                if (lineNumber >= 0) {
                    _printer.printf(
                        "          %1$-" + MAX_OPCODE_LENGTH + "s %2$d",
                        LINE_NUMBER_CODE,
                        lineNumber
                    );
                    endLine();
                }
            }

            try {
                _printer.printf("%1$8d: ", instruction.getOffset());
                instruction.accept(this);
            }
            catch (Throwable t) {
                printOpCode(instruction.getOpCode());

                boolean foundError = false;

                for (int i = 0; i < instruction.getOperandCount(); i++) {
                    final Object operand = instruction.getOperand(i);

                    if (operand instanceof ErrorOperand) {
                        _printer.print(operand);
                        foundError = true;
                        break;
                    }
                }

                if (!foundError) {
                    _printer.print("!!! ERROR");
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
                        _printer.printf(" /* %s */", variable.getName());
                    }
                }
            }

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final TypeReference value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value.getErasedSignature());
            _printer.print(".class");

            endLine();
        }

        private void endLine() {
            _printer.println();
            _printer.flush();
        }

        @Override
        public void visitConstant(final OpCode op, final int value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final long value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final float value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final double value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visitConstant(final OpCode op, final String value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(StringUtilities.escape(value, true));

            endLine();
        }

        @Override
        public void visitBranch(final OpCode op, final Instruction target) {
            printOpCode(op);

            _printer.print(' ');
            _printer.printf("%d", target.getOffset());

            endLine();
        }

        @Override
        public void visitVariable(final OpCode op, final VariableReference variable) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(variable.getName());

            endLine();
        }

        @Override
        public void visitVariable(final OpCode op, final VariableReference variable, final int operand) {
            printOpCode(op);
            _printer.print(' ');

            if (StringUtilities.isNullOrEmpty(variable.getName())) {
                _printer.print("$" + variable.getIndex());
            }
            else {
                _printer.print(variable.getName());
            }

            _printer.print(", ");
            _printer.print(operand);

            endLine();
        }

        @Override
        public void visitType(final OpCode op, final TypeReference type) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(type.getSignature());

            endLine();
        }

        @Override
        public void visitMethod(final OpCode op, final MethodReference method) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(method.getDeclaringType().getInternalName());
            _printer.print('.');
            _printer.print(method.getName());
            _printer.print(':');
            _printer.print(method.getErasedSignature());

            endLine();
        }

        @Override
        public void visitField(final OpCode op, final FieldReference field) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(field.getDeclaringType().getInternalName());
            _printer.print('.');
            _printer.print(field.getName());
            _printer.print(':');
            _printer.print(field.getErasedSignature());

            endLine();
        }

        @Override
        public void visitLabel(final Label label) {
        }

        @Override
        public void visitSwitch(final OpCode op, final SwitchInfo switchInfo) {
            printOpCode(op);
            _printer.print(" {");
            endLine();

            switch (op) {
                case TABLESWITCH: {
                    final Instruction[] targets = switchInfo.getTargets();

                    int caseValue = switchInfo.getLowValue();

                    for (final Instruction target : targets) {
                        _printer.printf(
                            "%1$21d: %2$d",
                            caseValue++,
                            target.getOffset()
                        );
                        endLine();
                    }

                    _printer.printf(
                        "%1$21s: %2$d",
                        "default",
                        switchInfo.getDefaultTarget().getOffset()
                    );

                    endLine();

                    break;
                }

                case LOOKUPSWITCH: {
                    final int[] keys = switchInfo.getKeys();
                    final Instruction[] targets = switchInfo.getTargets();

                    for (int i = 0; i < keys.length; i++) {
                        final int key = keys[i];
                        final Instruction target = targets[i];

                        _printer.printf(
                            "%1$21d: %2$d",
                            key,
                            target.getOffset()
                        );

                        endLine();
                    }

                    _printer.printf(
                        "%1$21s: %2$d",
                        "default",
                        switchInfo.getDefaultTarget().getOffset()
                    );

                    endLine();

                    break;
                }
            }

            _printer.print("          }");
            endLine();
        }

        @Override
        public void visitEnd() {
        }
    }

    // </editor-fold>
}

