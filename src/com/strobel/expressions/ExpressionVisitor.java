package com.strobel.expressions;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Mike Strobel
 */
public abstract class ExpressionVisitor {

    public Expression visit(final Expression node) {
        if (node != null) {
            return node.accept(this);
        }
        return null;
    }

    public Expression visitExtension(final Expression node) {
        return node;
    }

    public Expression visitMember(final MemberExpression node) {
        return node.update(this.visit(node.getTarget()));
    }

    public Expression visitConstant(final ConstantExpression node) {
        return node;
    }

    public Expression visitParameter(final ParameterExpression node) {
        return node;
    }

    public Expression visitUnary(final UnaryExpression node) {
        return validateUnary(node, node.update(visit(node.getOperand())));
    }

    protected Expression visitBinary(final BinaryExpression node) {
        // Walk children in evaluation order: left, conversion, right
        return validateBinary(
            node,
            node.update(
                visit(node.getLeft()),
                visitAndConvert(node.getConversion(), "visitBinary"),
                visit(node.getRight())
            )
        );
    }

    public Expression visitBlock(final BlockExpression node) {
        final int count = node.getExpressionCount();

        Expression[] nodes = null;

        for (int i = 0; i < count; i++) {
            final Expression oldNode = node.getExpression(i);
            final Expression newNode = visit(oldNode);

            if (newNode != oldNode) {
                if (nodes == null) {
                    nodes = new Expression[count];
                }
                nodes[i] = newNode;
            }
        }

        final List<ParameterExpression> v = visitAndConvertList(
            ParameterExpression.class,
            node.getVariables(),
            "visitBlock"
        );

        if (v == node.getVariables() && nodes == null) {
            return node;
        }
        else if (nodes != null) {
            for (int i = 0; i < count; i++) {
                if (nodes[i] == null) {
                    nodes[i] = node.getExpression(i);
                }
            }
        }

        return node.rewrite(v, nodes);
    }

    public final Expression visitInvocation(final InvocationExpression node) {
        final Expression e = visit(node.getExpression());
        final Expression[] a = visitArguments(node);

        if (e == node.getExpression() && a == null) {
            return node;
        }

        return node.rewrite(e, a);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // HELPER METHODS                                                                                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final Expression[] visitArguments(final IArgumentProvider nodes) {
        Expression[] newNodes = null;
        for (int i = 0, n = nodes.getArgumentCount(); i < n; i++) {
            final Expression curNode = nodes.getArgument(i);
            final Expression node = visit(curNode);

            if (newNodes != null) {
                newNodes[i] = node;
            }
            else if (node != curNode) {
                newNodes = new Expression[n];
                for (int j = 0; j < i; j++) {
                    newNodes[j] = nodes.getArgument(j);
                }
                newNodes[i] = node;
            }
        }
        return newNodes;
    }

    @SuppressWarnings("unchecked")
    public <T extends Expression> T visitAndConvert(final T node, final String callerName) {
        if (node == null) {
            return null;
        }

        final Expression newNode = visit(node);

        if (!node.getClass().isInstance(newNode)) {
            throw Error.mustRewriteToSameNode(callerName, node.getClass(), callerName);
        }

        return (T)newNode;
    }

    @SuppressWarnings("unchecked")
    public <T extends Expression> List<T> visitAndConvertList(final Class<T> type, final List<T> nodes, final String callerName) {
        T[] newNodes = null;

        for (int i = 0, n = nodes.size(); i < n; i++) {
            final T oldNode = nodes.get(i);

            final T node = (T) visit(oldNode);

            if (node == null) {
                throw Error.mustRewriteToSameNode(callerName, type, callerName);
            }

            if (newNodes != null) {
                newNodes[i] = node;
            }
            else if (node != oldNode) {
                newNodes = (T[])Array.newInstance(type);
                for (int j = 0; j < i; j++) {
                    newNodes[j] = nodes.get(j);
                }
                newNodes[i] = node;
            }
        }

        if (newNodes == null) {
            return nodes;
        }

        return Collections.unmodifiableList(Arrays.asList(newNodes));
    }

    private static UnaryExpression validateUnary(final UnaryExpression before, final UnaryExpression after) {
        if (before != after && before.getMethod() == null) {
            if (after.getMethod() != null) {
                throw Error.mustRewriteWithoutMethod(after.getMethod(), "visitUnary");
            }

            // rethrow has null operand
            if (before.getOperand() != null && after.getOperand() != null) {
                validateChildType(before.getOperand().getType(), after.getOperand().getType(), "visitUnary");
            }
        }
        return after;
    }

    private static BinaryExpression validateBinary(final BinaryExpression before, final BinaryExpression after) {
        if (before != after && before.getMethod() == null) {
            if (after.getMethod() != null) {
                throw Error.mustRewriteWithoutMethod(after.getMethod(), "VisitBinary");
            }

            validateChildType(before.getLeft().getType(), after.getLeft().getType(), "VisitBinary");
            validateChildType(before.getRight().getType(), after.getRight().getType(), "VisitBinary");
        }
        return after;
    }

    private static void validateChildType(final Class before, final Class after, final String methodName) {
        if (before.isPrimitive()) {
            if (before == after) {
                // types are the same value type
                return;
            }
        }
        else if (!after.isPrimitive()) {
            // both are reference types
            return;
        }

        // Otherwise, it's an invalid type change.
        throw Error.mustRewriteChildToSameType(before, after, methodName);
    }
}
