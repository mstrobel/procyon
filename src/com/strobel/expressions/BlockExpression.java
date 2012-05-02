package com.strobel.expressions;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author Mike Strobel
 */
public class BlockExpression extends Expression {

    public final List<Expression> getExpressions() {
        return getOrMakeExpressions();
    }

    public final List<ParameterExpression> getVariables() {
        return getOrMakeVariables();
    }

    public final Expression getResult() {
        final int expressionCount = getExpressionCount();
        assert expressionCount > 0;
        return getExpression(expressionCount - 1);
    }

    @Override
    public final ExpressionType getNodeType() {
        return ExpressionType.Block;
    }

    @Override
    public Class getType() {
        final int expressionCount = getExpressionCount();
        assert expressionCount > 0;
        return getExpression(expressionCount - 1).getType();
    }

    @Override
    protected Expression accept(final ExpressionVisitor visitor) {
        return visitor.visitBlock(this);
    }

    Expression getExpression(final int index) {
        throw ContractUtils.unreachable();
    }

    int getExpressionCount() {
        return 0;
    }

    List<Expression> getOrMakeExpressions() {
        throw ContractUtils.unreachable();
    }

    ParameterExpression getVariable(final int index) {
        throw ContractUtils.unreachable();
    }

    int getVariableCount() {
        return 0;
    }

    List<ParameterExpression> getOrMakeVariables() {
        return Collections.emptyList();
    }

    BlockExpression rewrite(final List<ParameterExpression> variables, final Expression[] args) {
        throw ContractUtils.unreachable();
    }

    @SuppressWarnings("unchecked")
    static List<Expression> returnReadOnlyExpressions(final BlockExpression provider, final Object expressionOrCollection) {
        if (expressionOrCollection instanceof Expression) {
            return Collections.unmodifiableList(
                new BlockExpressionList(provider, (Expression)expressionOrCollection)
            );
        }
        // Return what is not guaranteed to be a readonly expressionOrCollection
        return (List<Expression>)expressionOrCollection;
    }
}

final class Block2 extends BlockExpression {
    private Object _arg0;
    private final Expression _arg1;

    Block2(final Expression arg0, final Expression arg1) {
        _arg0 = arg0;
        _arg1 = arg1;
    }

    @Override
    final Expression getExpression(final int index) {
        switch (index) {
            case 0:
                return returnObject(Expression.class, _arg0);
            case 1:
                return _arg1;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    final int getExpressionCount() {
        return 2;
    }

    @SuppressWarnings("unchecked")
    @Override
    final List<Expression> getOrMakeExpressions() {
        return (List<Expression>)(_arg0 = returnReadOnlyExpressions(this, _arg0));
    }
}

final class Block3 extends BlockExpression {
    private Object _arg0;
    private final Expression _arg1;
    private final Expression _arg2;

    Block3(final Expression arg0, final Expression arg1, final Expression arg2) {
        _arg0 = arg0;
        _arg1 = arg1;
        _arg2 = arg2;
    }

    @Override
    final Expression getExpression(final int index) {
        switch (index) {
            case 0:
                return returnObject(Expression.class, _arg0);
            case 1:
                return _arg1;
            case 2:
                return _arg2;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    final int getExpressionCount() {
        return 3;
    }

    @SuppressWarnings("unchecked")
    @Override
    final List<Expression> getOrMakeExpressions() {
        return (List<Expression>)(_arg0 = returnReadOnlyExpressions(this, _arg0));
    }
}

final class Block4 extends BlockExpression {
    private Object _arg0;
    private final Expression _arg1;
    private final Expression _arg2;
    private final Expression _arg3;

    Block4(final Expression arg0, final Expression arg1, final Expression arg2, final Expression arg3) {
        _arg0 = arg0;
        _arg1 = arg1;
        _arg2 = arg2;
        _arg3 = arg3;
    }

    @Override
    final Expression getExpression(final int index) {
        switch (index) {
            case 0:
                return returnObject(Expression.class, _arg0);
            case 1:
                return _arg1;
            case 2:
                return _arg2;
            case 3:
                return _arg3;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    final int getExpressionCount() {
        return 4;
    }

    @SuppressWarnings("unchecked")
    @Override
    final List<Expression> getOrMakeExpressions() {
        return (List<Expression>)(_arg0 = returnReadOnlyExpressions(this, _arg0));
    }
}

final class Block5 extends BlockExpression {
    private Object _arg0;
    private final Expression _arg1;
    private final Expression _arg2;
    private final Expression _arg3;
    private final Expression _arg4;

    Block5(final Expression arg0, final Expression arg1, final Expression arg2, final Expression arg3, final Expression arg4) {
        _arg0 = arg0;
        _arg1 = arg1;
        _arg2 = arg2;
        _arg3 = arg3;
        _arg4 = arg4;
    }

    @Override
    final Expression getExpression(final int index) {
        switch (index) {
            case 0:
                return returnObject(Expression.class, _arg0);
            case 1:
                return _arg1;
            case 2:
                return _arg2;
            case 3:
                return _arg3;
            case 4:
                return _arg4;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    final int getExpressionCount() {
        return 5;
    }

    @SuppressWarnings("unchecked")
    @Override
    final List<Expression> getOrMakeExpressions() {
        return (List<Expression>)(_arg0 = returnReadOnlyExpressions(this, _arg0));
    }
}

final class BlockN extends BlockExpression {
    private final List<Expression> _expressions;

    BlockN(final List<Expression> expressions) {
        VerifyArgument.notEmpty(expressions, "expressions");
        _expressions = expressions;
    }

    @Override
    final int getExpressionCount() {
        return _expressions.size();
    }

    @Override
    final Expression getExpression(final int index) {
        return _expressions.get(index);
    }

    @Override
    final List<Expression> getOrMakeExpressions() {
        return ensureUnmodifiable(_expressions);
    }
}

class ScopeExpression extends BlockExpression {
    private List<ParameterExpression> _variables;

    ScopeExpression(final List<ParameterExpression> variables) {
        _variables = variables;
    }

    @Override
    final ParameterExpression getVariable(final int index) {
        return _variables.get(index);
    }

    @Override
    final int getVariableCount() {
        return _variables.size();
    }

    @Override
    final List<ParameterExpression> getOrMakeVariables() {
        return (_variables = ensureUnmodifiable(_variables));
    }

    // Used for rewrite of the nodes to either reuse existing set of variables if not rewritten.
    final List<ParameterExpression> reuseOrValidateVariables(final List<ParameterExpression> variables) {
        if (variables != null && variables != getVariables()) {
            // Need to validate the new variables (i.e. uniqueness)
            validateVariables(variables, "variables");
            return variables;
        }
        else {
            return getVariables();
        }
    }
}

final class Scope1 extends ScopeExpression {
    private Object _body;

    Scope1(final List<ParameterExpression> variables, final Expression body) {
        super(variables);
        _body = body;
    }

    @Override
    final Expression getExpression(final int index) {
        return returnObject(Expression.class, _body);
    }

    @Override
    final int getExpressionCount() {
        return 1;
    }

    @SuppressWarnings("unchecked")
    @Override
    final List<Expression> getOrMakeExpressions() {
        return (List<Expression>)(_body = returnReadOnlyExpressions(this, _body));
    }

    final BlockExpression rewrite(final List<ParameterExpression> variables, final Expression[] args) {
        assert args.length == 1;
        assert variables == null || variables.size() == getVariableCount();

        return new Scope1(reuseOrValidateVariables(variables), args[0]);
    }
}

class ScopeN extends ScopeExpression {
    private List<Expression> _body;

    ScopeN(final List<ParameterExpression> variables, final List<Expression> body) {
        super(variables);
        _body = body;
    }

    @Override
    final Expression getExpression(final int index) {
        return _body.get(index);
    }

    @Override
    final int getExpressionCount() {
        return _body.size();
    }

    @Override
    final List<Expression> getOrMakeExpressions() {
        return (_body = ensureUnmodifiable(_body));
    }

    BlockExpression rewrite(final List<ParameterExpression> variables, final Expression[] args) {
        assert args.length == getExpressionCount();
        assert variables == null || variables.size() == getVariableCount();

        return new ScopeN(reuseOrValidateVariables(variables), Arrays.asList(args));
    }
}

final class ScopeWithType extends ScopeN {
    private final Class _type;

    ScopeWithType(final List<ParameterExpression> variables, final List<Expression> expresions, final Class type) {
        super(variables, expresions);
        _type = type;
    }

    @Override
    public final Class getType() {
        return _type;
    }

    final BlockExpression rewrite(final List<ParameterExpression> variables, final Expression[] args) {
        assert args.length == getExpressionCount();
        assert variables == null || variables.size() == getVariableCount();

        return new ScopeWithType(reuseOrValidateVariables(variables), Arrays.asList(args), _type);
    }
}

final class BlockExpressionList implements List<Expression> {
    private final static Expression[] EMPTY_ARRAY = new Expression[0];

    private final BlockExpression _block;
    private final Expression _arg0;

    BlockExpressionList(final BlockExpression block, final Expression arg0) {
        _block = block;
        _arg0 = arg0;
    }

    @Override
    public int size() {
        return _block.getExpressionCount();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(final Object o) {
        return indexOf(o) != -1;
    }

    @Override
    public Iterator<Expression> iterator() {
        return new BlockExpressionListIterator();
    }

    @Override
    public Expression[] toArray() {
        final int size = size();

        if (size == 0) {
            return EMPTY_ARRAY;
        }

        final Expression[] array = new Expression[size];

        for (int i = 0; i < size; i++) {
            array[i] = get(i);
        }

        return array;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(final T[] a) {
        final int size = size();
        final T[] array = a != null && a.length >= size
                          ? a
                          : (T[])Array.newInstance(Expression.class, size);

        for (int i = 0; i < size; i++) {
            array[i] = (T)get(i);
        }

        return array;
    }

    @Override
    public boolean add(final Expression expression) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public boolean remove(final Object o) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends Expression> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends Expression> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public void clear() {
        throw Error.unmodifiableCollection();
    }

    @Override
    public Expression get(final int index) {
        if (index == 0) {
            return _arg0;
        }
        return _block.getExpression(index);
    }

    @Override
    public Expression set(final int index, final Expression element) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public void add(final int index, final Expression element) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public Expression remove(final int index) {
        throw Error.unmodifiableCollection();
    }

    @Override
    public int indexOf(final Object o) {
        if (o == _arg0) {
            return 0;
        }

        final int size = size();

        for (int i = 1; i < size; i++) {
            if (_block.getExpression(i) == o) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int lastIndexOf(final Object o) {
        final int size = size();
        for (int i = size - 1; i >= 0; i--) {
            if (o == get(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ListIterator<Expression> listIterator() {
        return new BlockExpressionListIterator();
    }

    @Override
    public ListIterator<Expression> listIterator(final int index) {
        return new BlockExpressionListIterator(index);
    }

    @Override
    public List<Expression> subList(final int fromIndex, final int toIndex) {
        if (fromIndex < 0 || toIndex > size() || toIndex <= fromIndex) {
            throw new IllegalArgumentException();
        }
        final List<Expression> subList = new ArrayList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            subList.set(i - fromIndex, get(i));
        }
        return subList;
    }

    private final class BlockExpressionListIterator implements ListIterator<Expression> {
        private int _position = -1;

        BlockExpressionListIterator() {}

        BlockExpressionListIterator(final int startPosition) {
            if (startPosition < -1 || startPosition >= size()) {
                throw new IllegalArgumentException();
            }
            _position = startPosition;
        }

        @Override
        public boolean hasNext() {
            return _position + 1 <= size();
        }

        @Override
        public Expression next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            return get(++_position);
        }

        @Override
        public boolean hasPrevious() {
            return _position > 0;
        }

        @Override
        public Expression previous() {
            if (!hasPrevious()) {
                throw new IllegalStateException();
            }
            return get(--_position);
        }

        @Override
        public int nextIndex() {
            return _position + 1;
        }

        @Override
        public int previousIndex() {
            return _position + 1;
        }

        @Override
        public void remove() {
            throw Error.unmodifiableCollection();
        }

        @Override
        public void set(final Expression expression) {
            throw Error.unmodifiableCollection();
        }

        @Override
        public void add(final Expression expression) {
            throw Error.unmodifiableCollection();
        }
    }
}
