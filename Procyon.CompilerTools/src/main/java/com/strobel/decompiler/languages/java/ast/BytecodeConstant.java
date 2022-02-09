package com.strobel.decompiler.languages.java.ast;

import com.strobel.core.Comparer;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Pattern;

public class BytecodeConstant extends Expression {
    public static final BytecodeConstant NULL = new NullMetadata();

    private final Object _constant;

    public BytecodeConstant(final Object constant) {
        super(MYSTERY_OFFSET);
        _constant = constant;
    }

    public final Object getConstantValue() {
        return _constant;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        if (_constant instanceof AstNode) {
            return ((AstNode) _constant).acceptVisitor(visitor, data);
        }
        else {
            return visitor.visitBytecodeConstant(this, data);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.UNKNOWN;
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof Pattern) {
            return other.matches(this, match);
        }
        return other instanceof BytecodeConstant &&
               Comparer.equals(_constant, ((BytecodeConstant) other)._constant);
    }

    private static final class NullMetadata extends BytecodeConstant {
        public NullMetadata() {
            super(null);
        }

        @Override
        public final boolean isNull() {
            return true;
        }

        @Override
        public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
            return null;
        }

        @Override
        public boolean matches(final INode other, final Match match) {
            return other == null || other.isNull();
        }
    }
}
