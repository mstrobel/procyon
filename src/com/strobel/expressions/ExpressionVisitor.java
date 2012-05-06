package com.strobel.expressions;

import com.strobel.core.ReadOnlyList;
import com.strobel.reflection.Type;

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

    public Expression visitDefaultValue(final DefaultValueExpression node) {
        return node;
    }

    public Expression visitExtension(final Expression node) {
        return node.visitChildren(this);
    }

    public Expression visitLabel(final LabelExpression node) {
        return node.update(this.visitLabelTarget(node.getTarget()), visit(node.getDefaultValue()));
    }

    protected LabelTarget visitLabelTarget(final LabelTarget node) {
        return node;
    }

    public Expression visitGoto(final GotoExpression node) {
        return node.update(visitLabelTarget(node.getTarget()), visit(node.getValue()));
    }

    public Expression visitLoop(final LoopExpression node) {
        return node.update(
            visitLabelTarget(node.getBreakLabel()),
            visitLabelTarget(node.getContinueLabel()),
            visit(node.getBody())
        );
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

    public Expression visitTypeBinary(final TypeBinaryExpression node) {
        return node.update(visit(node.getOperand()));
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

        final ParameterExpressionList v = visitAndConvertList(
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

    public Expression visitInvocation(final InvocationExpression node) {
        final Expression e = visit(node.getExpression());
        final ExpressionList<? extends Expression> a = visitArguments(node);

        if (e == node.getExpression() && a == null) {
            return node;
        }

        return node.rewrite(e, a);
    }

    public Expression visitMethodCall(final MethodCallExpression node) {
        final Expression target = visit(node.getTarget());
        final ExpressionList<? extends Expression> arguments = visitArguments(node);

        if (target == node.getTarget() && arguments == null) {
            return node;
        }

        return node.rewrite(target, arguments);
    }

    public Expression visitNew(final NewExpression node) {
        return node.update(visit(node.getArguments()));
    }

    public Expression visitNewArray(final NewArrayExpression node) {
        return node.update(visit(node.getExpressions()));
    }

    public <T> LambdaExpression<T> visitLambda(final LambdaExpression<T> node) {
        return node.update(visit(node.getBody()), visitAndConvertList(node.getParameters(), "visitLambda"));
    }

    public Expression visitConditional(final ConditionalExpression node) {
        return node.update(visit(node.getTest()), visit(node.getIfTrue()), visit(node.getIfFalse()));
    }

    public Expression visitRuntimeVariables(final RuntimeVariablesExpression node) {
        return node.update(visitAndConvertList(node.getVariables(), "visitRuntimeVariables"));
    }

    public Expression visitTry(final TryExpression node) {
        return node.update(
            visit(node.getBody()),
            visit(
                node.getHandlers(),
                new ElementVisitor<CatchBlock>() {
                    @Override
                    public CatchBlock visit(final CatchBlock node) {
                        return visitCatchBlock(node);
                    }
                }
            ),
            visit(node.getFinallyBlock())
        );
    }

    public CatchBlock visitCatchBlock(final CatchBlock node) {
        return node.update(
            visitAndConvert(node.getVariable(), "visitCatchBlock"),
            visit(node.getFilter()),
            visit(node.getBody())
        );
    }

    public static <T> ReadOnlyList<T> visit(final ReadOnlyList<T> nodes, final ElementVisitor<T> elementVisitor) {
        T[] newNodes = null;

        for (int i = 0, n = nodes.size(); i < n; i++) {
            final T node = nodes.get(i);
            final T newNode = elementVisitor.visit(node);

            if (newNode != node) {
                if (newNodes == null) {
                    newNodes = nodes.toArray();
                }
                newNodes[i] = newNode;
            }
        }
        if (newNodes == null) {
            return nodes;
        }
        return new ReadOnlyList<>(newNodes);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // HELPER METHODS                                                                                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final ExpressionList<? extends Expression> visit(final ExpressionList<? extends Expression> nodes) {
        Expression[] newNodes = null;
        for (int i = 0, n = nodes.size(); i < n; i++) {
            final Expression node = nodes.get(i);
            final Expression newNode = visit(node);

            if (newNode != node) {
                if (newNodes == null) {
                    newNodes = nodes.toArray();
                }
                newNodes[i] = newNode;
            }
        }
        return new ExpressionList<>(newNodes);
    }

    final ExpressionList<? extends Expression> visitArguments(final IArgumentProvider nodes) {
        Expression[] newNodes = null;
        for (int i = 0, n = nodes.getArgumentCount(); i < n; i++) {
            final Expression node = nodes.getArgument(i);
            final Expression newNode = visit(node);

            if (newNodes != null) {
                newNodes[i] = newNode;
            }
            else if (newNode != node) {
                newNodes = new Expression[n];
                for (int j = 0; j < i; j++) {
                    newNodes[j] = nodes.getArgument(j);
                }
                newNodes[i] = newNode;
            }
        }
        return new ExpressionList<>(newNodes);
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
    public <T extends Expression> ExpressionList<T> visitAndConvertList(final ExpressionList<T> nodes, final String callerName) {
        T[] newNodes = null;

        for (int i = 0, n = nodes.size(); i < n; i++) {
            final T oldNode = nodes.get(i);
            final T node = (T)visit(oldNode);

            if (node == null) {
                throw Error.mustRewriteToSameNode(callerName, oldNode.getClass(), callerName);
            }

            if (newNodes == null) {
                newNodes = nodes.toArray();
            }

            newNodes[i] = node;
        }

        if (newNodes == null) {
            return nodes;
        }

        return new ExpressionList<>(newNodes);
    }

    @SuppressWarnings("unchecked")
    public ParameterExpressionList visitAndConvertList(final ParameterExpressionList nodes, final String callerName) {
        ParameterExpression[] newNodes = null;

        for (int i = 0, n = nodes.size(); i < n; i++) {
            final ParameterExpression oldNode = nodes.get(i);
            final ParameterExpression node = (ParameterExpression)visit(oldNode);

            if (node == null) {
                throw Error.mustRewriteToSameNode(callerName, oldNode.getClass(), callerName);
            }

            if (newNodes == null) {
                newNodes = nodes.toArray();
            }

            newNodes[i] = node;
        }

        if (newNodes == null) {
            return nodes;
        }

        return new ParameterExpressionList(newNodes);
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

    private static void validateChildType(final Type before, final Type after, final String methodName) {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DEPENDENT TYPES                                                                                                    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public interface ElementVisitor<T> {
        T visit(final T node);
    }
}
