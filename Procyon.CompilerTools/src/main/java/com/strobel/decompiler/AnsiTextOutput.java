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
import com.strobel.assembler.metadata.*;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.io.Ansi;

import java.io.StringWriter;
import java.io.Writer;

public class AnsiTextOutput extends PlainTextOutput {
    private final static class Delimiters {
        final static String L = "L";
        final static String T = "T";
        final static String DOLLAR = "$";
        final static String DOT = ".";
        final static String SLASH = "/";
        final static String LEFT_BRACKET = "[";
        final static String SEMICOLON = ";";
    }

    private final Ansi _keyword;
    private final Ansi _instruction;
    private final Ansi _label;
    private final Ansi _type;
    private final Ansi _typeVariable;
    private final Ansi _package;
    private final Ansi _method;
    private final Ansi _field;
    private final Ansi _local;
    private final Ansi _literal;
    private final Ansi _textLiteral;
    private final Ansi _comment;
    private final Ansi _operator;
    private final Ansi _delimiter;
    private final Ansi _attribute;
    private final Ansi _error;

    public AnsiTextOutput() {
        this(new StringWriter(), ColorScheme.DARK);
    }

    public AnsiTextOutput(final ColorScheme colorScheme) {
        this(new StringWriter(), colorScheme);
    }

    public AnsiTextOutput(final Writer writer) {
        this(writer, ColorScheme.DARK);
    }

    public AnsiTextOutput(final Writer writer, final ColorScheme colorScheme) {
        super(writer);

        final boolean light = colorScheme == ColorScheme.LIGHT;

        _keyword = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 21 : 33), null);
        _instruction = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 91 : 141), null);
        _label = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 249 : 249), null);
        _type = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 25 : 45), null);
        _typeVariable = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 29 : 79), null);
        _package = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 32 : 111), null);
        _method = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 162 : 212), null);
        _field = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 136 : 222), null);
        _local = new Ansi(Ansi.Attribute.NORMAL, (Ansi.AnsiColor) null, null);
        _literal = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 197 : 204), null);
        _textLiteral = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 28 : 42), null);
        _comment = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 244 : 244), null);
        _operator = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 242 : 247), null);
        _delimiter = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 242 : 252), null);
        _attribute = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 166 : 214), null);
        _error = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(light ? 196 : 196), null);
    }

    @Override
    public void writeError(final String value) {
        writeAnsi(value, _error.colorize(value));
    }

    @Override
    public void writeLabel(final String value) {
        writeAnsi(value, _label.colorize(value));
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
        writeAnsi(literal, _literal.colorize(literal));
    }

    @Override
    public void writeTextLiteral(final Object value) {
        final String literal = String.valueOf(value);
        writeAnsi(literal, _textLiteral.colorize(literal));
    }

    @Override
    public void writeComment(final String value) {
        writeAnsi(value, _comment.colorize(value));
    }

    @Override
    public void writeComment(final String format, final Object... args) {
        final String text = String.format(format, args);
        writeAnsi(text, _comment.colorize(text));
    }

    @Override
    public void writeDelimiter(final String text) {
        writeAnsi(text, _delimiter.colorize(text));
    }

    @Override
    public void writeAttribute(final String text) {
        writeAnsi(text, _attribute.colorize(text));
    }

    @Override
    public void writeOperator(final String text) {
        writeAnsi(text, _operator.colorize(text));
    }

    @Override
    public void writeKeyword(final String text) {
        writeAnsi(text, _keyword.colorize(text));
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

            colorizedText = _instruction.colorize(escapedText);
        }
        else if (definition instanceof TypeReference) {
            colorizedText = colorizeType(escapedText, (TypeReference) definition);
        }
        else if (definition instanceof MethodReference ||
                 definition instanceof IMethodSignature) {
            colorizedText = _method.colorize(escapedText);
        }
        else if (definition instanceof FieldReference) {
            colorizedText = _field.colorize(escapedText);
        }
        else if (definition instanceof VariableReference ||
                 definition instanceof ParameterReference ||
                 definition instanceof Variable) {

            colorizedText = _local.colorize(escapedText);
        }
        else if (definition instanceof PackageReference) {
            colorizedText = colorizePackage(escapedText);
        }
        else if (definition instanceof Label ||
                 definition instanceof com.strobel.decompiler.ast.Label) {

            colorizedText = _label.colorize(escapedText);
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

            colorizedText = _instruction.colorize(escapedText);
        }
        else if (reference instanceof TypeReference) {
            colorizedText = colorizeType(escapedText, (TypeReference) reference);
        }
        else if (reference instanceof MethodReference ||
                 reference instanceof IMethodSignature) {
            colorizedText = _method.colorize(escapedText);
        }
        else if (reference instanceof FieldReference) {
            colorizedText = _field.colorize(escapedText);
        }
        else if (reference instanceof VariableReference ||
                 reference instanceof ParameterReference ||
                 reference instanceof Variable) {

            colorizedText = _local.colorize(escapedText);
        }
        else if (reference instanceof PackageReference) {
            colorizedText = colorizePackage(escapedText);
        }
        else if (reference instanceof Label ||
                 reference instanceof com.strobel.decompiler.ast.Label) {

            colorizedText = _label.colorize(escapedText);
        }
        else {
            colorizedText = escapedText;
        }

        writeAnsi(escapedText, colorizedText);
    }

    @SuppressWarnings("ConstantConditions")
    private String colorizeType(final String text, final TypeReference type) {
        if (type.isPrimitive()) {
            return _keyword.colorize(text);
        }

        final String packageName = type.getPackageName();
        final TypeDefinition resolvedType = type.resolve();

        Ansi typeColor = type.isGenericParameter() ? _typeVariable : _type;

        if (StringUtilities.isNullOrEmpty(packageName)) {
            if (resolvedType != null && resolvedType.isAnnotation()) {
                return _attribute.colorize(text);
            }
            else {
                return typeColor.colorize(text);
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

        final String typeName;
        final StringBuilder sb = new StringBuilder();

        if (StringUtilities.startsWith(s, packagePrefix)) {
            final String[] packageParts = packageName.split("\\.");

            for (int i = 0; i < arrayDepth; i++) {
                sb.append(_delimiter.colorize(Delimiters.LEFT_BRACKET));
            }

            if (isSignature) {
                sb.append(_delimiter.colorize(isTypeVariable ? Delimiters.T : Delimiters.L));
            }

            for (int i = 0; i < packageParts.length; i++) {
                if (i != 0) {
                    sb.append(_delimiter.colorize(String.valueOf(delimiter)));
                }

                sb.append(_package.colorize(packageParts[i]));
            }

            sb.append(_delimiter.colorize(String.valueOf(delimiter)));

            typeName = s.substring(packagePrefix.length());
        }
        else {
            typeName = text;
        }

        final String[] typeParts = typeName.split("\\$|\\.");
        final boolean dollar = typeName.indexOf('$') >= 0;

        typeColor = resolvedType != null && resolvedType.isAnnotation() ? _attribute : typeColor;

        for (int i = 0; i < typeParts.length; i++) {
            if (i != 0) {
                sb.append(_delimiter.colorize(dollar ? Delimiters.DOLLAR : Delimiters.DOT));
            }

            sb.append(typeColor.colorize(typeParts[i]));
        }

        if (isSignature) {
            sb.append(_delimiter.colorize(Delimiters.SEMICOLON));
        }

        return sb.toString();
    }

    private String colorizePackage(final String text) {
        final String[] packageParts = text.split("\\.");
        final StringBuilder sb = new StringBuilder(text.length() * 2);

        for (int i = 0; i < packageParts.length; i++) {
            if (i != 0) {
                sb.append(_delimiter.colorize("."));
            }

            final String packagePart = packageParts[i];

            if ("*".equals(packagePart)) {
                sb.append(packagePart);
            }
            else {
                sb.append(_package.colorize(packagePart));
            }
        }

        return sb.toString();
    }

    public enum ColorScheme {
        DARK,
        LIGHT
    }
}
