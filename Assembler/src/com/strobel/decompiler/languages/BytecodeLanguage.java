/*
 * BytecodeLanguage.java
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

package com.strobel.decompiler.languages;

import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.ir.ErrorOperand;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.InstructionCollection;
import com.strobel.assembler.ir.InstructionVisitor;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.ir.OpCodeHelpers;
import com.strobel.assembler.ir.attributes.*;
import com.strobel.assembler.metadata.*;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerHelpers;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.NameSyntax;
import com.strobel.decompiler.PlainTextOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static java.lang.String.format;

public class BytecodeLanguage extends Language {
    @Override
    public String getName() {
        return "Bytecode";
    }

    @Override
    public String getFileExtension() {
        return ".class";
    }

    @Override
    public void decompileType(final TypeDefinition type, final ITextOutput output, final DecompilationOptions options) {
        VerifyArgument.notNull(type, "type");
        VerifyArgument.notNull(output, "output");
        VerifyArgument.notNull(options, "options");

        if (type.isInterface()) {
            if (type.isAnnotation()){
                output.writeKeyword("@interface");
            }
            else {
                output.writeKeyword("interface");
            }
        }
        else if (type.isEnum()) {
            output.writeKeyword("enum");
        }
        else {
            output.writeKeyword("class");
        }

        output.write(' ');
        DecompilerHelpers.writeType(output, type, NameSyntax.TYPE_NAME, true);
        output.writeLine();
        output.indent();

        try {
            writeTypeHeader(output, type);

            for (final SourceAttribute attribute : type.getSourceAttributes()) {
                writeTypeAttribute(output, type, attribute);
            }

            final ConstantPool constantPool = type.getConstantPool();

            if (constantPool != null) {
                constantPool.accept(new ConstantPoolPrinter(output));
            }

            for (final FieldDefinition field : type.getDeclaredFields()) {
                output.writeLine();
                decompileField(field, output, options);
            }

            for (final MethodDefinition method : type.getDeclaredMethods()) {
                output.writeLine();
                decompileMethod(method, output, options);
            }
        }
        finally {
            output.unindent();
        }

        if (options.getSettings().getShowNestedTypes()) {
            for (final TypeDefinition innerType : type.getDeclaredTypes()) {
                output.writeLine();
                decompileType(innerType, output, options);
            }
        }
    }

    private void writeTypeAttribute(final ITextOutput output, final TypeDefinition type, final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.SourceFile: {
                output.writeAttribute("SourceFile");
                output.write(": ");
                output.writeTextLiteral(((SourceFileAttribute) attribute).getSourceFile());
                output.writeLine();
                break;
            }

            case AttributeNames.Deprecated: {
                output.writeAttribute("Deprecated");
                output.writeLine();
                break;
            }

            case AttributeNames.EnclosingMethod: {
                final TypeReference enclosingType = ((EnclosingMethodAttribute) attribute).getEnclosingType();
                final MethodReference enclosingMethod = ((EnclosingMethodAttribute) attribute).getEnclosingMethod();

                if (enclosingType != null) {
                    output.writeAttribute("EnclosingType");
                    output.write(": ");
                    output.writeReference(enclosingType.getInternalName(), enclosingType);
                    output.writeLine();
                }

                if (enclosingMethod != null) {
                    final TypeReference declaringType = enclosingMethod.getDeclaringType();

                    output.writeAttribute("EnclosingMethod");
                    output.write(": ");
                    output.writeReference(declaringType.getInternalName(), declaringType);
                    output.writeDelimiter(".");
                    output.writeReference(enclosingMethod.getName(), enclosingMethod);
                    output.writeDelimiter(":");
                    DecompilerHelpers.writeMethodSignature(output, enclosingMethod);
                    output.writeLine();
                }

                break;
            }

            case AttributeNames.Signature: {
                if (SourceAttribute.find(AttributeNames.Signature, type.getSourceAttributes()) != null) {
                    output.writeAttribute("Signature");
                    output.write(": ");
                    DecompilerHelpers.writeType(output, type, NameSyntax.SIGNATURE, true);
                    output.writeLine();
                }
            }
        }
    }

    private void writeTypeHeader(final ITextOutput output, final TypeDefinition type) {
        output.writeAttribute("Minor version");
        output.write(": ");
        output.writeLiteral(type.getCompilerMinorVersion());
        output.writeLine();

        output.writeAttribute("Major version");
        output.write(": ");
        output.writeLiteral(type.getCompilerMajorVersion());
        output.writeLine();

        final long flags = type.getFlags();
        final List<String> flagStrings = new ArrayList<>();
        final EnumSet<Flags.Flag> flagsSet = Flags.asFlagSet(flags & (Flags.ClassFlags | ~Flags.StandardFlags));

        for (final Flags.Flag flag : flagsSet) {
            flagStrings.add(flag.name());
        }

        if (!flagStrings.isEmpty()) {
            output.writeAttribute("Flags");
            output.write(": ");

            for (int i = 0; i < flagStrings.size(); i++) {
                if (i != 0) {
                    output.write(", ");
                }

                output.writeLiteral(flagStrings.get(i));
            }

            output.writeLine();
        }
    }

    @Override
    public void decompileField(final FieldDefinition field, final ITextOutput output, final DecompilationOptions options) {
        final long flags = field.getFlags();
        final EnumSet<Flags.Flag> flagSet = Flags.asFlagSet(flags & Flags.VarFlags & ~Flags.ENUM);
        final List<String> flagStrings = new ArrayList<>();

        for (final Flags.Flag flag : flagSet) {
            flagStrings.add(flag.toString());
        }

        if (flagSet.size() > 0) {
            for (int i = 0; i < flagStrings.size(); i++) {
                output.writeKeyword(flagStrings.get(i));
                output.write(' ');
            }
        }

        DecompilerHelpers.writeType(output, field.getFieldType(), NameSyntax.TYPE_NAME);

        output.write(' ');
        output.writeDefinition(field.getName(), field);
        output.writeDelimiter(";");
        output.writeLine();

        flagStrings.clear();

        for (final Flags.Flag flag : Flags.asFlagSet(flags & (Flags.VarFlags | ~Flags.StandardFlags))) {
            flagStrings.add(flag.name());
        }

        if (flagStrings.isEmpty()) {
            return;
        }

        output.indent();

        try {
            output.writeAttribute("Flags");
            output.write(": ");

            for (int i = 0; i < flagStrings.size(); i++) {
                if (i != 0) {
                    output.write(", ");
                }

                output.writeLiteral(flagStrings.get(i));
            }

            output.writeLine();
        }
        finally {
            output.unindent();
        }

        for (final SourceAttribute attribute : field.getSourceAttributes()) {
            writeFieldAttribute(output, field, attribute);
        }
    }

    private void writeFieldAttribute(final ITextOutput output, final FieldDefinition field, final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.ConstantValue: {
                final Object constantValue = ((ConstantValueAttribute) attribute).getValue();
                output.indent();
                output.writeAttribute("ConstantValue");
                output.write(": ");
                DecompilerHelpers.writeOperand(output, constantValue);
                output.writeLine();
                output.unindent();
                break;
            }

            case AttributeNames.Signature: {
                output.writeAttribute("Signature");
                output.write(": ");
                DecompilerHelpers.writeType(output, field.getFieldType(), NameSyntax.SIGNATURE, false);
                output.writeLine();
                break;
            }
        }
    }

    @Override
    public void decompileMethod(final MethodDefinition method, final ITextOutput output, final DecompilationOptions options) {
        writeMethodHeader(output, method);
        writeMethodBody(output, method);

        for (final SourceAttribute attribute : method.getSourceAttributes()) {
            writeMethodAttribute(output, method, attribute);
        }
    }

    private void writeMethodHeader(final ITextOutput output, final MethodDefinition method) {
        final String name = method.getName();
        final long flags = method.getFlags();
        final List<String> flagStrings = new ArrayList<>();

        if ("<clinit>".equals(name)) {
            output.writeKeyword("static");
            output.write(" {}");
        }
        else {
            final EnumSet<Flags.Flag> flagSet = Flags.asFlagSet(flags & Flags.MethodFlags);

            for (final Flags.Flag flag : flagSet) {
                flagStrings.add(flag.toString());
            }

            if (flagSet.size() > 0) {
                for (int i = 0; i < flagStrings.size(); i++) {
                    output.writeKeyword(flagStrings.get(i));
                    output.write(' ');
                }
            }

            final List<GenericParameter> genericParameters = method.getGenericParameters();

            if (!genericParameters.isEmpty()) {
                output.writeDelimiter("<");

                for (int i = 0; i < genericParameters.size(); i++) {
                    if (i != 0) {
                        output.writeDelimiter(", ");
                    }

                    DecompilerHelpers.writeType(output, genericParameters.get(i), NameSyntax.TYPE_NAME, true);
                }

                output.writeDelimiter(">");
                output.write(' ');
            }

            DecompilerHelpers.writeType(output, method.getReturnType(), NameSyntax.TYPE_NAME, false);

            output.write(' ');
            output.writeDefinition(name, method);
            output.writeDelimiter("(");

            final List<ParameterDefinition> parameters = method.getParameters();

            for (int i = 0; i < parameters.size(); i++) {
                if (i != 0) {
                    output.writeDelimiter(", ");
                }

                final ParameterDefinition parameter = parameters.get(i);

                if (Flags.testAny(flags, Flags.ACC_VARARGS) && i == parameters.size() - 1) {
                    DecompilerHelpers.writeType(output, parameter.getParameterType().getUnderlyingType(), NameSyntax.TYPE_NAME, false);
                    output.writeDelimiter("...");
                }
                else {
                    DecompilerHelpers.writeType(output, parameter.getParameterType(), NameSyntax.TYPE_NAME, false);
                }

                output.write(' ');

                final String parameterName = parameter.getName();

                if (StringUtilities.isNullOrEmpty(parameterName)) {
                    output.write("p%d", i);
                }
                else {
                    output.write(parameterName);
                }
            }

            output.writeDelimiter(")");

            final List<TypeReference> thrownTypes = method.getThrownTypes();

            if (!thrownTypes.isEmpty()) {
                output.writeKeyword(" throws ");

                for (int i = 0; i < thrownTypes.size(); i++) {
                    if (i != 0) {
                        output.writeDelimiter(", ");
                    }

                    DecompilerHelpers.writeType(output, thrownTypes.get(i), NameSyntax.TYPE_NAME, false);
                }
            }
        }

        output.writeDelimiter(";");
        output.writeLine();

        flagStrings.clear();

        for (final Flags.Flag flag : Flags.asFlagSet(flags & (Flags.MethodFlags | ~Flags.StandardFlags))) {
            flagStrings.add(flag.name());
        }

        if (flagStrings.isEmpty()) {
            return;
        }

        output.indent();

        try {
            output.writeAttribute("Flags");
            output.write(": ");

            for (int i = 0; i < flagStrings.size(); i++) {
                if (i != 0) {
                    output.write(", ");
                }

                output.writeLiteral(flagStrings.get(i));
            }

            output.writeLine();
        }
        finally {
            output.unindent();
        }
    }

    private void writeMethodAttribute(final ITextOutput output, final MethodDefinition method, final SourceAttribute attribute) {
        switch (attribute.getName()) {
            case AttributeNames.Exceptions: {
                final ExceptionsAttribute exceptionsAttribute = (ExceptionsAttribute) attribute;
                final List<TypeReference> exceptionTypes = exceptionsAttribute.getExceptionTypes();

                if (!exceptionTypes.isEmpty()) {
                    output.indent();

                    try {
                        output.writeAttribute("Exceptions");
                        output.writeLine(":");

                        output.indent();

                        try {
                            for (final TypeReference exceptionType : exceptionTypes) {
                                output.writeKeyword("throws");
                                output.write(' ');
                                DecompilerHelpers.writeType(output, exceptionType, NameSyntax.TYPE_NAME);
                                output.writeLine();
                            }
                        }
                        finally {
                            output.unindent();
                        }
                    }
                    finally {
                        output.unindent();
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

                output.indent();

                try {
                    output.writeAttribute(attribute.getName());
                    output.writeLine(":");

                    output.indent();

                    try {
                        output.write("Start  Length  Slot  %1$-" + longestName + "s  Signature", "Name");
                        output.writeLine();

                        output.write(
                            "-----  ------  ----  %1$-" + longestName + "s  %2$-" + longestSignature + "s",
                            StringUtilities.repeat('-', longestName),
                            StringUtilities.repeat('-', longestSignature)
                        );

                        output.writeLine();

                        final MethodBody body = method.getBody();

                        for (final LocalVariableTableEntry entry : entries) {
                            final NameSyntax nameSyntax;
                            final VariableDefinitionCollection variables = body != null ? body.getVariables() : null;

                            if (attribute.getName().equals(AttributeNames.LocalVariableTypeTable)) {
                                nameSyntax = NameSyntax.SIGNATURE;
                            }
                            else {
                                nameSyntax = NameSyntax.ERASED_SIGNATURE;
                            }

                            output.writeLiteral(format("%1$-5d", entry.getScopeOffset()));
                            output.write("  ");
                            output.writeLiteral(format("%1$-6d", entry.getScopeLength()));
                            output.write("  ");
                            output.writeLiteral(format("%1$-4d", entry.getIndex()));

                            output.writeReference(
                                String.format("  %1$-" + longestName + "s  ", entry.getName()),
                                variables != null ? variables.tryFind(entry.getIndex(), entry.getScopeOffset()) : null
                            );

                            DecompilerHelpers.writeType(output, entry.getType(), nameSyntax);

                            output.writeLine();
                        }
                    }
                    finally {
                        output.unindent();
                    }
                }
                finally {
                    output.unindent();
                }

                break;
            }

            case AttributeNames.Signature: {
                output.indent();

                try {
                    final String signature = ((SignatureAttribute) attribute).getSignature();

                    output.writeAttribute(attribute.getName());
                    output.writeLine(":");
                    output.indent();

                    final PlainTextOutput temp = new PlainTextOutput();

                    DecompilerHelpers.writeMethodSignature(temp, method);

                    if (StringUtilities.equals(temp.toString(), signature)) {
                        DecompilerHelpers.writeMethodSignature(output, method);
                    }
                    else {
                        DecompilerHelpers.writeMethodSignature(output, method);
                        output.writeLine();
                        output.writeTextLiteral(signature);
                    }

                    output.writeLine();
                    output.unindent();
                }
                finally {
                    output.unindent();
                }

                break;
            }
        }
    }

    private void writeMethodBody(final ITextOutput output, final MethodDefinition method) {
        final MethodBody body = method.getBody();

        if (body == null) {
            return;
        }

        output.indent();

        try {
            output.writeAttribute("Code");
            output.writeLine(":");

            output.indent();

            try {
                output.write("stack=");
                output.writeLiteral(body.getMaxStackSize());
                output.write(", locals=");
                output.writeLiteral(body.getMaxLocals());
                output.write(", arguments=");
                output.writeLiteral(method.getParameters().size());
                output.writeLine();
            }
            finally {
                output.unindent();
            }

            final InstructionCollection instructions = body.getInstructions();

            if (!instructions.isEmpty()) {
                final LineNumberTableAttribute lineNumbersAttribute = SourceAttribute.find(
                    AttributeNames.LineNumberTable,
                    method.getSourceAttributes()
                );

                final int[] lineNumbers;

                if (lineNumbersAttribute != null) {
                    lineNumbers = new int[body.getCodeSize()];

                    Arrays.fill(lineNumbers, -1);

                    for (final LineNumberTableEntry entry : lineNumbersAttribute.getEntries()) {
                        lineNumbers[entry.getOffset()] = entry.getLineNumber();
                    }
                }
                else {
                    lineNumbers = null;
                }

                final InstructionPrinter printer = new InstructionPrinter(output, method, lineNumbers);

                for (final Instruction instruction : instructions) {
                    printer.visit(instruction);
                }
            }
        }
        finally {
            output.unindent();
        }
    }

    private final static class InstructionPrinter implements InstructionVisitor {
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

        private final ITextOutput _output;
        private final MethodBody _body;
        private final int[] _lineNumbers;

        private int _currentOffset = -1;

        private InstructionPrinter(final ITextOutput output, final MethodDefinition method, final int[] lineNumbers) {
            _output = VerifyArgument.notNull(output, "output");
            _body = VerifyArgument.notNull(method, "method").getBody();
            _lineNumbers = lineNumbers;
        }

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
                    _output.write("          ");
                    _output.write("%1$-" + MAX_OPCODE_LENGTH + "s", LINE_NUMBER_CODE);
                    _output.write(' ');
                    _output.writeLiteral(lineNumber);
                    _output.writeLine();
                }
            }

            _currentOffset = instruction.getOffset();

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

                _output.writeLine();
            }
            finally {
                _currentOffset = -1;
            }
        }

        @Override
        public void visit(final OpCode op) {
            printOpCode(op);

            final int slot = OpCodeHelpers.getLoadStoreMacroArgumentIndex(op);

            if (slot >= 0) {
                final VariableDefinitionCollection variables = _body.getVariables();

                if (slot < variables.size()) {
                    final VariableDefinition variable = findVariable(op, slot, _currentOffset);

                    assert variable != null;

                    if (variable.hasName() && variable.isFromMetadata()) {
                        _output.writeComment(" /* %s */", variable.getName());
                    }
                }
            }

            _output.writeLine();
        }

        private VariableDefinition findVariable(final OpCode op, final int slot, final int offset) {
            VariableDefinition variable = _body.getVariables().tryFind(slot, offset);

            if (variable == null && op.isStore()) {
                variable = _body.getVariables().tryFind(slot, offset + op.getSize() + op.getOperandType().getBaseSize());
            }

            return variable;
        }

        @Override
        public void visitConstant(final OpCode op, final TypeReference value) {
            printOpCode(op);

            _output.write(' ');
            DecompilerHelpers.writeType(_output, value, NameSyntax.ERASED_SIGNATURE);
            _output.write(".class");

            _output.writeLine();
        }

        @Override
        public void visitConstant(final OpCode op, final int value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            _output.writeLine();
        }

        @Override
        public void visitConstant(final OpCode op, final long value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            _output.writeLine();
        }

        @Override
        public void visitConstant(final OpCode op, final float value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            _output.writeLine();
        }

        @Override
        public void visitConstant(final OpCode op, final double value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLiteral(value);

            _output.writeLine();
        }

        @Override
        public void visitConstant(final OpCode op, final String value) {
            printOpCode(op);

            _output.write(' ');
            _output.writeTextLiteral(StringUtilities.escape(value, true));

            _output.writeLine();
        }

        @Override
        public void visitBranch(final OpCode op, final Instruction target) {
            printOpCode(op);

            _output.write(' ');
            _output.writeLabel(String.valueOf(target.getOffset()));

            _output.writeLine();
        }

        @Override
        public void visitVariable(final OpCode op, final VariableReference variable) {
            printOpCode(op);

            _output.write(' ');

            final VariableDefinition definition = findVariable(op, variable.getSlot(), _currentOffset);

            if (definition != null && definition.hasName() && definition.isFromMetadata()) {
                _output.writeReference(variable.getName(), variable);
            }
            else {
                _output.writeLiteral(variable.getSlot());
            }

            _output.writeLine();
        }

        @Override
        public void visitVariable(final OpCode op, final VariableReference variable, final int operand) {
            printOpCode(op);
            _output.write(' ');

            final VariableDefinition definition;

            if (variable instanceof VariableDefinition) {
                definition = (VariableDefinition) variable;
            }
            else {
                definition = findVariable(op, variable.getSlot(), _currentOffset);
            }

            if (definition != null && definition.hasName() && definition.isFromMetadata()) {
                _output.writeReference(variable.getName(), variable);
            }
            else {
                _output.writeLiteral(variable.getSlot());
            }

            _output.write(", ");
            _output.writeLiteral(String.valueOf(operand));

            _output.writeLine();
        }

        @Override
        public void visitType(final OpCode op, final TypeReference type) {
            printOpCode(op);

            _output.write(' ');

            DecompilerHelpers.writeType(_output, type, NameSyntax.SIGNATURE);

            _output.writeLine();
        }

        @Override
        public void visitMethod(final OpCode op, final MethodReference method) {
            printOpCode(op);

            _output.write(' ');

            DecompilerHelpers.writeMethod(_output, method);

            _output.writeLine();
        }

        @Override
        public void visitDynamicCallSite(final OpCode op, final DynamicCallSite callSite) {
            printOpCode(op);

            _output.write(' ');

            _output.writeReference(callSite.getMethodName(), callSite.getMethodType());
            _output.writeDelimiter(":");

            DecompilerHelpers.writeMethodSignature(_output, callSite.getMethodType());

            _output.writeLine();
        }

        @Override
        public void visitField(final OpCode op, final FieldReference field) {
            printOpCode(op);

            _output.write(' ');

            DecompilerHelpers.writeField(_output, field);

            _output.writeLine();
        }

        @Override
        public void visitLabel(final Label label) {
        }

        @Override
        public void visitSwitch(final OpCode op, final SwitchInfo switchInfo) {
            printOpCode(op);
            _output.write(" {");
            _output.writeLine();

            switch (op) {
                case TABLESWITCH: {
                    final Instruction[] targets = switchInfo.getTargets();

                    int caseValue = switchInfo.getLowValue();

                    for (final Instruction target : targets) {
                        _output.write("            ");
                        _output.writeLiteral(format("%1$7d", switchInfo.getLowValue() + caseValue++));
                        _output.write(": ");
                        _output.writeLabel(String.valueOf(target.getOffset()));
                        _output.writeLine();
                    }

                    _output.write("            ");
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

                        _output.write("            ");
                        _output.writeLiteral(format("%1$7d", key));
                        _output.write(": ");
                        _output.writeLabel(String.valueOf(target.getOffset()));
                        _output.writeLine();
                    }

                    _output.write("            ");
                    _output.writeKeyword("default");
                    _output.write(": ");
                    _output.writeLabel(String.valueOf(switchInfo.getDefaultTarget().getOffset()));
                    _output.writeLine();

                    break;
                }
            }

            _output.write("          }");
            _output.writeLine();
        }

        @Override
        public void visitEnd() {
        }
    }
}
