/*
 * AnsiTextOutput.java
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

package com.strobel.decompiler;

import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.IMethodSignature;
import com.strobel.assembler.metadata.Label;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.PackageReference;
import com.strobel.assembler.metadata.ParameterReference;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.assembler.metadata.VariableReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.io.Ansi;

import java.io.Writer;

public class AnsiTextOutput extends PlainTextOutput {
    private final static Ansi KEYWORD = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(33/*39*/), null);
    private final static Ansi INSTRUCTION = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(/*45*//*33*/141), null);
    private final static Ansi LABEL = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(249), null);
    private final static Ansi TYPE = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(45/*105*//*141*/), null);
    private final static Ansi PACKAGE = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(111), null);
    private final static Ansi METHOD = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(/*213*/212), null);
    private final static Ansi FIELD = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(222/*216*/), null);
    private final static Ansi LOCAL = new Ansi(Ansi.Attribute.NORMAL, /*new Ansi.AnsiColor(230)*/(Ansi.AnsiColor) null, null);
    private final static Ansi LITERAL = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(204), null);
    private final static Ansi TEXT_LITERAL = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(/*48*/42), null);
    private final static Ansi COMMENT = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(244), null);
    private final static Ansi OPERATOR = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(247), null);
    private final static Ansi DELIMITER = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(252), null);
    private final static Ansi ATTRIBUTE = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(214), null);
    private final static Ansi ERROR = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(196), null);

    private final static class Delimiters {
        final static String L = "L";
        final static String T = "T";
        final static String DOLLAR = "$";
        final static String DOT = ".";
        final static String SLASH = "/";
        final static String LEFT_BRACKET = "[";
        final static String SEMICOLON = ";";
    }

    public AnsiTextOutput() {
    }

    public AnsiTextOutput(final Writer writer) {
        super(writer);
    }

    @Override
    public void writeError(final String value) {
        writeAnsi(value, ERROR.colorize(value));
    }

    @Override
    public void writeLabel(final String value) {
        writeAnsi(value, LABEL.colorize(value));
    }

    protected final void writeAnsi(final String originalText, final String ansiText) {
        super.write(ansiText);

        if (originalText != null && ansiText != null) {
            super.column -= (ansiText.length() - originalText.length());
        }
    }

    @Override
    public void writeLiteral(final Object value) {
        final String literal = String.valueOf(value);
        writeAnsi(literal, LITERAL.colorize(literal));
    }

    @Override
    public void writeTextLiteral(final Object value) {
        final String literal = String.valueOf(value);
        writeAnsi(literal, TEXT_LITERAL.colorize(literal));
    }

    @Override
    public void writeComment(final String value) {
        writeAnsi(value, COMMENT.colorize(value));
    }

    @Override
    public void writeComment(final String format, final Object... args) {
        final String text = String.format(format, args);
        writeAnsi(text, COMMENT.colorize(text));
    }

    @Override
    public void writeDelimiter(final String text) {
        writeAnsi(text, DELIMITER.colorize(text));
    }

    @Override
    public void writeAttribute(final String text) {
        writeAnsi(text, ATTRIBUTE.colorize(text));
    }

    @Override
    public void writeOperator(final String text) {
        writeAnsi(text, OPERATOR.colorize(text));
    }

    @Override
    public void writeKeyword(final String text) {
        writeAnsi(text, KEYWORD.colorize(text));
    }

    @Override
    public void writeDefinition(final String text, final Object definition, final boolean isLocal) {
        final String escapedText = JavaOutputVisitor.escapeUnicode(text);

        if (escapedText == null) {
            super.write(escapedText);
            return;
        }

        final String colorizedText;

        if (definition instanceof Instruction ||
            definition instanceof OpCode ||
            definition instanceof AstCode) {

            colorizedText = INSTRUCTION.colorize(escapedText);
        }
        else if (definition instanceof TypeReference) {
            colorizedText = colorizeType(escapedText, (TypeReference) definition);
        }
        else if (definition instanceof MethodReference ||
                 definition instanceof IMethodSignature) {
            colorizedText = METHOD.colorize(escapedText);
        }
        else if (definition instanceof FieldReference) {
            colorizedText = FIELD.colorize(escapedText);
        }
        else if (definition instanceof VariableReference ||
                 definition instanceof ParameterReference ||
                 definition instanceof Variable) {

            colorizedText = LOCAL.colorize(escapedText);
        }
        else if (definition instanceof PackageReference) {
            colorizedText = colorizePackage(escapedText);
        }
        else if (definition instanceof Label ||
                 definition instanceof com.strobel.decompiler.ast.Label) {

            colorizedText = LABEL.colorize(escapedText);
        }
        else {
            colorizedText = escapedText;
        }

        writeAnsi(escapedText, colorizedText);
    }

    @Override
    public void writeReference(final String text, final Object reference, final boolean isLocal) {
        final String escapedText = JavaOutputVisitor.escapeUnicode(text);

        if (escapedText == null) {
            super.write(escapedText);
            return;
        }

        final String colorizedText;

        if (reference instanceof Instruction ||
            reference instanceof OpCode ||
            reference instanceof AstCode) {

            colorizedText = INSTRUCTION.colorize(escapedText);
        }
        else if (reference instanceof TypeReference) {
            colorizedText = colorizeType(escapedText, (TypeReference) reference);
        }
        else if (reference instanceof MethodReference ||
                 reference instanceof IMethodSignature) {
            colorizedText = METHOD.colorize(escapedText);
        }
        else if (reference instanceof FieldReference) {
            colorizedText = FIELD.colorize(escapedText);
        }
        else if (reference instanceof VariableReference ||
                 reference instanceof ParameterReference ||
                 reference instanceof Variable) {

            colorizedText = LOCAL.colorize(escapedText);
        }
        else if (reference instanceof PackageReference) {
            colorizedText = colorizePackage(escapedText);
        }
        else if (reference instanceof Label ||
                 reference instanceof com.strobel.decompiler.ast.Label) {

            colorizedText = LABEL.colorize(escapedText);
        }
        else {
            colorizedText = escapedText;
        }

        writeAnsi(escapedText, colorizedText);
    }

    @SuppressWarnings("ConstantConditions")
    private String colorizeType(final String text, final TypeReference type) {
        if (type.isPrimitive()) {
            return KEYWORD.colorize(text);
        }

        final String packageName = type.getPackageName();
        final TypeDefinition resolvedType = type.resolve();

        if (StringUtilities.isNullOrEmpty(packageName)) {
            if (resolvedType != null && resolvedType.isAnnotation()) {
                return ATTRIBUTE.colorize(text);
            }
            else {
                return TYPE.colorize(text);
            }
        }

        String s = text;
        char delimiter = '.';
        String packagePrefix = packageName + delimiter;

        int arrayDepth = 0;

        while (arrayDepth < s.length() && s.charAt(arrayDepth) == '[') {
            arrayDepth++;
        }

        if (arrayDepth > 0) {
            s = s.substring(arrayDepth);
        }

        final boolean isTypeVariable = s.startsWith("T") && s.endsWith(";");
        final boolean isSignature = isTypeVariable || s.startsWith("L") && s.endsWith(";");

        if (isSignature) {
            s = s.substring(1, s.length() - 1);
        }

        if (!StringUtilities.startsWith(s, packagePrefix)) {
            delimiter = '/';
            packagePrefix = packageName.replace('.', delimiter) + delimiter;
        }

        if (StringUtilities.startsWith(s, packagePrefix)) {
            final StringBuilder sb = new StringBuilder();
            final String[] packageParts = packageName.split("\\.");

            for (int i = 0; i < arrayDepth; i++) {
                sb.append(DELIMITER.colorize(Delimiters.LEFT_BRACKET));
            }

            if (isSignature) {
                sb.append(DELIMITER.colorize(isTypeVariable ? Delimiters.T : Delimiters.L));
            }

            for (int i = 0; i < packageParts.length; i++) {
                if (i != 0) {
                    sb.append(DELIMITER.colorize(String.valueOf(delimiter)));
                }

                sb.append(PACKAGE.colorize(packageParts[i]));
            }

            sb.append(DELIMITER.colorize(String.valueOf(delimiter)));

            final String typeName = s.substring(packagePrefix.length());
            final String[] typeParts = typeName.split("\\$|\\.");
            final Ansi typeColor = resolvedType != null && resolvedType.isAnnotation() ? ATTRIBUTE : TYPE;
            final boolean dollar = typeName.indexOf('$') >= 0;

            for (int i = 0; i < typeParts.length; i++) {
                if (i != 0) {
                    sb.append(DELIMITER.colorize(dollar ? Delimiters.DOLLAR : Delimiters.DOT));
                }

                sb.append(typeColor.colorize(typeParts[i]));
            }

            if (isSignature) {
                sb.append(DELIMITER.colorize(Delimiters.SEMICOLON));
            }

            return sb.toString();
        }

        if (resolvedType != null && resolvedType.isAnnotation()) {
            return ATTRIBUTE.colorize(text);
        }
        else {
            return TYPE.colorize(text);
        }
    }

    private String colorizePackage(final String text) {
        final String[] packageParts = text.split("\\.");
        final StringBuilder sb = new StringBuilder(text.length() * 2);

        for (int i = 0; i < packageParts.length; i++) {
            if (i != 0) {
                sb.append(DELIMITER.colorize("."));
            }

            final String packagePart = packageParts[i];

            if ("*".equals(packagePart)) {
                sb.append(packagePart);
            }
            else {
                sb.append(PACKAGE.colorize(packagePart));
            }
        }

        return sb.toString();
    }
}
