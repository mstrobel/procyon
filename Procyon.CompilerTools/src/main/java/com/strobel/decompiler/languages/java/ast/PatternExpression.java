package com.strobel.decompiler.languages.java.ast;

public abstract class PatternExpression extends Expression {
    protected PatternExpression(final int offset) {
        super(offset);
    }

    protected PatternExpression() {
        super(MYSTERY_OFFSET);
    }
}
