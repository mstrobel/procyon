package com.strobel.assembler.metadata;

import com.strobel.assembler.CodePrinter;
import com.strobel.assembler.ir.*;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * @author Mike Strobel
 */
public class MethodPrinter implements MethodVisitor<MutableTypeDefinition> {
    private final CodePrinter _printer;
    private final int _flags;
    private final String _name;
    private final IMethodSignature _signature;
    private final TypeReference[] _thrownTypes;

    public MethodPrinter(
        final CodePrinter printer,
        final int flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes) {

        _printer = VerifyArgument.notNull(printer, "printer");
        _flags = flags;
        _name = VerifyArgument.notNull(name, "name");
        _signature = signature;
        _thrownTypes = thrownTypes;

        printDescription();
    }

    private void printDescription() {
        final EnumSet<Flags.Flag> flagSet = Flags.asFlagSet(_flags & Flags.VarFlags & ~Flags.ENUM);
        final List<String> flagStrings = new ArrayList<>();

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

            _printer.print(parameter.getParameterType().getBriefDescription());
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

        _printer.println(";");
    }

    @Override
    public boolean canVisitBody(final MutableTypeDefinition _) {
        return true;
    }

    @Override
    public InstructionVisitor<MutableTypeDefinition> visitBody(final MutableTypeDefinition _, final int maxStack, final int maxLocals) {
        _printer.println("  Code:");
        _printer.printf("    stack=%d, locals=%d, arguments=%d\n", maxStack, maxLocals, _signature.getParameters().size());
        return new InstructionPrinter();
    }

    @Override
    public void visitEnd(final MutableTypeDefinition _) {
    }

    @Override
    public void visitFrame(final MutableTypeDefinition _, final Frame frame) {
    }

    @Override
    public void visitLineNumber(final MutableTypeDefinition _, final Instruction instruction, final int lineNumber) {
    }

    @Override
    public void visitAttribute(final MutableTypeDefinition _, final SourceAttribute attribute) {
    }

    @Override
    public void visitAnnotation(final MutableTypeDefinition _, final CustomAnnotation annotation, final boolean visible) {
    }

    // <editor-fold defaultstate="collapsed" desc="InstructionPrinter Class">

    private static final int MAX_OPCODE_LENGTH;
    private static final String[] OPCODE_NAMES;

    static {
        int maxLength = 0;

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

    private final class InstructionPrinter implements InstructionVisitor<MutableTypeDefinition> {

        private boolean _hasLabel;

        private void printOpCode(final OpCode opCode) {
            if (!_hasLabel) {
                _printer.printf("       ");
            }
            _printer.printf("%1$-" + MAX_OPCODE_LENGTH + "s", OPCODE_NAMES[opCode.ordinal()]);
        }

        @Override
        public void visit(final MutableTypeDefinition _, final Instruction instruction) {
            VerifyArgument.notNull(instruction, "instruction");

            try {
                instruction.accept(this, _);
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
        public void visit(final MutableTypeDefinition _, final OpCode op) {
            printOpCode(op);
            endLine();
        }

        private void endLine() {
            _hasLabel = false;
            _printer.println();
            _printer.flush();
        }

        @Override
        public void visit(final MutableTypeDefinition _, final OpCode op, final int value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visit(final MutableTypeDefinition _, final OpCode op, final long value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visit(final MutableTypeDefinition _, final OpCode op, final float value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visit(final MutableTypeDefinition _, final OpCode op, final double value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(value);

            endLine();
        }

        @Override
        public void visit(final MutableTypeDefinition _, final OpCode op, final String value) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(StringUtilities.escape(value, true));

            endLine();
        }

        @Override
        public void visitBranch(final MutableTypeDefinition _, final OpCode op, final Instruction target) {
            printOpCode(op);
            _printer.print(' ');

            if (target.hasLabel()) {
                _printer.printf("L%d", target.getLabel().index);
            }
            else {
                _printer.printf("#%d", target.getOffset());
            }

            endLine();
        }

        @Override
        public void visitVariable(final MutableTypeDefinition _, final OpCode op, final VariableReference variable) {
            printOpCode(op);
            _printer.print(' ');

            if (StringUtilities.isNullOrEmpty(variable.getName())) {
                _printer.print("$" + variable.getIndex());
            }
            else {
                _printer.print(variable.getName());
            }

            endLine();
        }

        @Override
        public void visitVariable(final MutableTypeDefinition _, final OpCode op, final VariableReference variable, final int operand) {
            printOpCode(op);
            _printer.print(' ');

            if (StringUtilities.isNullOrEmpty(variable.getName())) {
                _printer.print("$" + variable.getIndex());
            }
            else {
                _printer.print(variable.getName());
            }

            _printer.print(' ');
            _printer.print(operand);

            endLine();
        }

        @Override
        public void visitType(final MutableTypeDefinition _, final OpCode op, final TypeReference type) {
            printOpCode(op);

            _printer.print(' ');
            _printer.print(type.getSignature());

            endLine();
        }

        @Override
        public void visitMethod(final MutableTypeDefinition _, final OpCode op, final MethodReference method) {
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
        public void visitField(final MutableTypeDefinition _, final OpCode op, final FieldReference field) {
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
        public void visitLabel(final MutableTypeDefinition _, final Label label) {
            _printer.printf("%1$5s: ", "L" + label.index);
            _hasLabel = true;
        }

        @Override
        public void visitSwitch(final MutableTypeDefinition _, final OpCode op, final SwitchInfo switchInfo) {
            printOpCode(op);
            endLine();
        }
    }

    // </editor-fold>
}

