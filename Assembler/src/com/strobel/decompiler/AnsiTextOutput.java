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
import com.strobel.assembler.metadata.Label;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.assembler.metadata.VariableReference;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Variable;
import com.strobel.io.Ansi;

import java.io.Writer;

public class AnsiTextOutput extends PlainTextOutput {
    private final static Ansi KEYWORD = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(33/*39*/), null);
    private final static Ansi INSTRUCTION = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(/*45*//*33*/141), null);
    private final static Ansi LABEL = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(249), null);
    private final static Ansi TYPE = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(45/*105*//*141*/), null);
    private final static Ansi METHOD = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(/*213*/212), null);
    private final static Ansi FIELD = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(222/*216*/), null);
    private final static Ansi LOCAL = new Ansi(Ansi.Attribute.NORMAL, (Ansi.Color) null, null);
    private final static Ansi LITERAL = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(204), null);
    private final static Ansi TEXT_LITERAL = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(/*48*/42), null);
    private final static Ansi COMMENT = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(244), null);
    private final static Ansi OPERATOR = new Ansi(Ansi.Attribute.NORMAL, new Ansi.AnsiColor(247), null);

    public AnsiTextOutput() {
    }

    public AnsiTextOutput(final Writer writer) {
        super(writer);
    }

    @Override
    public void writeLabel(final String value) {
        write(LABEL.colorize(value));
    }

    @Override
    public void writeLiteral(final Object value) {
        write(LITERAL.colorize(String.valueOf(value)));
    }

    @Override
    public void writeTextLiteral(final Object value) {
        write(TEXT_LITERAL.colorize(String.valueOf(value)));
    }

    @Override
    public void writeComment(final String value) {
        write(COMMENT.colorize(value));
    }

    @Override
    public void writeComment(final String format, final Object... args) {
        write(COMMENT.colorize(String.format(format, args)));
    }

    @Override
    public void writeOperator(final String text) {
        write(OPERATOR.colorize(text));
    }

    @Override
    public void writeKeyword(final String text) {
        write(KEYWORD.colorize(text));
    }

    @Override
    public void writeDefinition(final String text, final Object definition, final boolean isLocal) {
        final String colorizedText;

        if (definition instanceof Instruction ||
            definition instanceof OpCode ||
            definition instanceof AstCode) {

            colorizedText = INSTRUCTION.colorize(text);
        }
        else if (definition instanceof TypeReference) {
            if (((TypeReference) definition).isPrimitive()) {
                colorizedText = KEYWORD.colorize(text);
            }
            else {
                colorizedText = TYPE.colorize(text);
            }
        }
        else if (definition instanceof MethodReference) {
            colorizedText = METHOD.colorize(text);
        }
        else if (definition instanceof FieldReference) {
            colorizedText = FIELD.colorize(text);
        }
        else if (definition instanceof VariableReference ||
                 definition instanceof ParameterReference ||
                 definition instanceof Variable) {

            colorizedText = LOCAL.colorize(text);
        }
        else if (definition instanceof Label ||
                 definition instanceof com.strobel.decompiler.ast.Label) {

            colorizedText = LABEL.colorize(text);
        }
        else {
            colorizedText = text;
        }

        super.writeDefinition(colorizedText, definition, isLocal);
    }

    @Override
    public void writeReference(final String text, final Object reference, final boolean isLocal) {
        final String colorizedText;

        if (reference instanceof Instruction ||
            reference instanceof OpCode ||
            reference instanceof AstCode) {

            colorizedText = INSTRUCTION.colorize(text);
        }
        else if (reference instanceof TypeReference) {
            if (((TypeReference) reference).isPrimitive()) {
                colorizedText = KEYWORD.colorize(text);
            }
            else {
                colorizedText = TYPE.colorize(text);
            }
        }
        else if (reference instanceof MethodReference) {
            colorizedText = METHOD.colorize(text);
        }
        else if (reference instanceof FieldReference) {
            colorizedText = FIELD.colorize(text);
        }
        else if (reference instanceof VariableReference ||
                 reference instanceof ParameterReference ||
                 reference instanceof Variable) {

            colorizedText = LOCAL.colorize(text);
        }
        else if (reference instanceof Label ||
                 reference instanceof com.strobel.decompiler.ast.Label) {

            colorizedText = LABEL.colorize(text);
        }
        else {
            colorizedText = text;
        }

        super.writeReference(colorizedText, reference, isLocal);
    }
}
