package com.strobel.reflection;

public abstract class SimpleVisitor<P, R> extends DefaultTypeVisitor<P, R> {
    @Override
    public R visitCapturedType(final Type<?> t, final P s) {
        return visitTypeParameter(t, s);
    }

/*
    @Override
    public R visitForAll(ForAll t, P s) {
        return visit(t.qtype, s);
    }

    @Override
    public R visitUndetVar(UndetVar t, P s) {
        return visit(t.qtype, s);
    }
*/
}
