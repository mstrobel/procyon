package com.strobel.expressions;

/**
 * @author Mike Strobel
 */
public class ParameterExpressionList extends ExpressionList<ParameterExpression> {

    private final static ParameterExpressionList EMPTY = new ParameterExpressionList();

    @SuppressWarnings("unchecked")
    public static ParameterExpressionList empty() {
        return EMPTY;
    }

    public ParameterExpressionList(final ParameterExpression... expressions) {
        super(expressions);
    }

    @Override
    protected ParameterExpressionList newInstance(final ParameterExpression[] expressions) {
        return new ParameterExpressionList(expressions);
    }

    @Override
    public ParameterExpressionList add(final ParameterExpression expression) {
        return (ParameterExpressionList)super.add(expression);
    }

    @Override
    public ParameterExpressionList remove(final ParameterExpression expression) {
        return (ParameterExpressionList)super.remove(expression);
    }

    @Override
    public ParameterExpressionList addAll(final int index, final ExpressionList<ParameterExpression> c) {
        return (ParameterExpressionList)super.addAll(index, c);
    }

    public ParameterExpressionList removeAll(final ParameterExpressionList c) {
        return (ParameterExpressionList)super.removeAll(c);
    }

    public ParameterExpressionList retainAll(final ParameterExpressionList c) {
        return (ParameterExpressionList)super.retainAll(c);
    }

    @Override
    public ParameterExpressionList addAll(final ExpressionList<ParameterExpression> c) {
        return (ParameterExpressionList)super.addAll(c);
    }

    @Override
    public ParameterExpressionList replace(final int index, final ParameterExpression expression) {
        return (ParameterExpressionList)super.replace(index, expression);
    }

    @Override
    public ParameterExpressionList add(final int index, final ParameterExpression expression) {
        return (ParameterExpressionList)super.add(index, expression);
    }

    @Override
    public ParameterExpressionList remove(final int index) {
        return (ParameterExpressionList)super.remove(index);
    }

    @Override
    public ParameterExpressionList getRange(final int fromIndex, final int toIndex) {
        return (ParameterExpressionList)super.getRange(fromIndex, toIndex);
    }
}
