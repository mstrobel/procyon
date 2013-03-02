/*
 * DecompilerHelpers.java
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

package com.strobel.decompiler;

import com.strobel.assembler.ir.ExceptionBlock;
import com.strobel.assembler.ir.ExceptionHandler;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.assembler.metadata.VariableReference;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.java.JavaOutputVisitor;
import com.strobel.util.ContractUtils;

import static java.lang.String.format;

public final class DecompilerHelpers {
    public static void writeType(final ITextOutput writer, final TypeReference type) {
        writeType(writer, type, NameSyntax.SIGNATURE);
    }

    public static void writeType(final ITextOutput writer, final TypeReference type, final NameSyntax syntax) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(writer, "writer");
        VerifyArgument.notNull(syntax, "syntax");

        switch (syntax) {
            case SIGNATURE:
                writer.write(type.getSignature());
                break;

            case SIGNATURE_NO_NAMED_TYPE_VARIABLES:
                writer.write(type.getSignature());
                break;

            case TYPE_NAME:
                writer.write(type.getBriefDescription());
                break;

            case SHORT_TYPE_NAME:
                writer.write(type.getSimpleDescription());
                break;
        }
    }

    public static void writeMethod(final ITextOutput writer, final MethodReference method) {
        VerifyArgument.notNull(method, "method");
        VerifyArgument.notNull(writer, "writer");

        writer.write(method.getFullName());
        writer.write(':');
        writer.write(method.getSignature());
    }

    public static void writeField(final ITextOutput writer, final FieldReference field) {
        VerifyArgument.notNull(field, "field");
        VerifyArgument.notNull(writer, "writer");

        writer.write(field.getFullName());
        writer.write(':');
        writer.write(field.getSignature());
    }

    public static void writeOperand(final ITextOutput writer, final Object operand) {
        VerifyArgument.notNull(writer, "writer");
        VerifyArgument.notNull(operand, "operand");

        if (operand instanceof Instruction) {
            final Instruction targetInstruction = (Instruction) operand;
            writeOffsetReference(writer, targetInstruction);
            return;
        }

        if (operand instanceof Instruction[]) {
            final Instruction[] targetInstructions = (Instruction[]) operand;
            writeLabelList(writer, targetInstructions);
            return;
        }

        if (operand instanceof VariableReference) {
            final VariableReference variable = (VariableReference) operand;

            if (variable.hasName()) {
                writer.writeReference(escapeIdentifier(variable.getName()), variable);
            }
            else {
                writer.writeReference(String.valueOf(variable.getIndex()), variable);
            }

            return;
        }

        if (operand instanceof ParameterReference) {
            final ParameterReference parameter = (ParameterReference) operand;
            final String parameterName = parameter.getName();

            if (StringUtilities.isNullOrEmpty(parameterName)) {
                writer.writeReference(String.valueOf(parameter.getPosition()), parameter);
            }
            else {
                writer.writeReference(escapeIdentifier(parameterName), parameter);
            }

            return;
        }

        if (operand instanceof MethodReference) {
            writeMethod(writer, (MethodReference) operand);
            return;
        }

        if (operand instanceof TypeReference) {
            writeType(writer, (TypeReference) operand, NameSyntax.TYPE_NAME);
            return;
        }

        if (operand instanceof FieldReference) {
            writeField(writer, (FieldReference) operand);
            return;
        }

        if (operand instanceof String) {
            writer.write(JavaOutputVisitor.convertString((String) operand, true));
            return;
        }

        if (operand instanceof Character) {
            writer.write(String.valueOf((int) ((Character) operand).charValue()));
            return;
        }

        if (operand instanceof Boolean) {
            writer.write(Boolean.TRUE.equals(operand) ? "true" : "false");
            return;
        }

        writer.write(String.valueOf(operand));
    }

    public static String offsetToString(final int offset) {
        return format("#%1$04x", offset);
    }

    public static void writeExceptionHandler(final ITextOutput output, final ExceptionHandler handler) {
        VerifyArgument.notNull(output, "output");
        VerifyArgument.notNull(handler, "handler");

        output.write("Try ");
        writeOffsetReference(output, handler.getTryBlock().getFirstInstruction());
        output.write(" - ");
        writeEndOffsetReference(output, handler.getTryBlock().getLastInstruction());
        output.write(' ');
        output.write(String.valueOf(handler.getHandlerType()));

        final TypeReference catchType = handler.getCatchType();

        if (catchType != null) {
            output.write(' ');
            writeType(output, catchType);
        }

        final ExceptionBlock handlerBlock = handler.getHandlerBlock();

        output.write(' ');
        writeOffsetReference(output, handlerBlock.getFirstInstruction());

        if (handlerBlock.getLastInstruction() != null) {
            output.write(" - ");
            writeEndOffsetReference(output, handlerBlock.getLastInstruction());
        }
    }

    public static void writeInstruction(final ITextOutput writer, final Instruction instruction) {
        VerifyArgument.notNull(writer, "writer");
        VerifyArgument.notNull(instruction, "instruction");

        writer.writeDefinition(offsetToString(instruction.getOffset()), instruction);
        writer.write(": ");
        writer.writeReference(instruction.getOpCode().name(), instruction.getOpCode());

        if (instruction.hasOperand()) {
            writer.write(' ');
            writeOperandList(writer, instruction);
        }
    }

    public static void writeOffsetReference(final ITextOutput writer, final Instruction instruction) {
        VerifyArgument.notNull(writer, "writer");

        writer.writeReference(offsetToString(instruction.getOffset()), instruction);
    }

    public static void writeEndOffsetReference(final ITextOutput writer, final Instruction instruction) {
        VerifyArgument.notNull(writer, "writer");

        writer.writeReference(offsetToString(instruction.getEndOffset()), instruction);
    }

    public static String escapeIdentifier(final String name) {
        VerifyArgument.notNull(name, "name");

        StringBuilder sb = null;

        for (int i = 0, n = name.length(); i < n; i++) {
            final char ch = name.charAt(i);

            if (i == 0) {
                if (Character.isJavaIdentifierStart(ch)) {
                    continue;
                }
                sb = new StringBuilder(name.length() * 2);
                sb.append(format("\\u%1$04x", (int) ch));
            }
            else if (Character.isJavaIdentifierPart(ch)) {
                if (sb != null) {
                    sb.append(ch);
                }
            }
            else {
                if (sb == null) {
                    sb = new StringBuilder(name.length() * 2);
                }
                sb.append(format("\\u%1$04x", (int) ch));
            }
        }

        if (sb != null) {
            return sb.toString();
        }

        return name;
    }

    private static void writeLabelList(final ITextOutput writer, final Instruction[] instructions) {
        writer.write('(');

        for (int i = 0; i < instructions.length; i++) {
            if (i != 0) {
                writer.write(", ");
            }
            writeOffsetReference(writer, instructions[i]);
        }

        writer.write(')');
    }

    private static void writeOperandList(final ITextOutput writer, final Instruction instruction) {
        for (int i = 0, n = instruction.getOperandCount(); i < n; i++) {
            if (i != 0) {
                writer.write(", ");
            }
            writeOperand(writer, instruction.getOperand(i));
        }
    }
}
