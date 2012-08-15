package com.strobel.expressions;

import com.strobel.core.StringUtilities;
import com.strobel.reflection.*;
import com.strobel.util.TypeUtils;

import java.util.HashMap;
import java.util.Set;

/**
 * @author Mike Strobel
 */
final class ExpressionStringBuilder extends ExpressionVisitor {
    private final static String lineSeparator = System.getProperty("line.separator");

    private final StringBuilder _out;
    private int     _indentLevel   = 0;
    private int     _blockDepth    = 0;
    private boolean _indentPending = false;
    private HashMap<Object, Integer> _ids;

    private ExpressionStringBuilder() {
        _out = new StringBuilder();
    }

    private int getParameterId(final ParameterExpression p) {
        if (_ids == null) {
            _ids = new HashMap<>();
            _ids.put(p, 0);
            return 0;
        }
        else if (_ids.containsKey(p)) {
            return _ids.get(p);
        }

        final int id = _ids.size();
        _ids.put(p, id);
        return id;
    }

    private void increaseIndent() {
        ++_indentLevel;
    }

    private void decreaseIndent() {
        --_indentLevel;
    }

    private void flush() {
        if (_indentPending) {
            return;
        }
        _out.append(lineSeparator);
        _indentPending = true;
    }

    private void applyIndent() {
        if (!_indentPending) {
            return;
        }
        for (int i = 0; i < _indentLevel; i++) {
            _out.append("    ");
        }
        _indentPending = false;
    }

    private void out(final String s) {
        applyIndent();
        _out.append(s);
    }

    private void out(final char c) {
        applyIndent();
        _out.append(c);
    }

    private void outMember(final Expression instance, final MemberInfo member) {
        if (instance != null) {
            visit(instance);
            out("." + member.getName());
        }
        else {
            // For static members, include the type name
            out(member.getDeclaringType().getName() + "." + member.getName());
        }
    }

    @Override
    public String toString() {
        return _out.toString();
    }

    static String expressionToString(final Expression node) {
        assert node != null;
        final ExpressionStringBuilder esb = new ExpressionStringBuilder();
        esb.visit(node);
        return esb.toString();
    }

    static String catchBlockToString(final CatchBlock node) {
        assert node != null;
        final ExpressionStringBuilder esb = new ExpressionStringBuilder();
        esb.visitCatchBlock(node);
        return esb.toString();
    }

    static String switchCaseToString(final SwitchCase node) {
        assert node != null;
        final ExpressionStringBuilder esb = new ExpressionStringBuilder();
        esb.visitSwitchCase(node);
        return esb.toString();
    }

    private <T extends Expression> void visitExpressions(final char open, final ExpressionList<T> expressions, final char close) {
        out(open);
        if (expressions != null) {
            boolean isFirst = true;
            for (final T e : expressions) {
                if (isFirst) {
                    isFirst = false;
                }
                else {
                    out(", ");
                }
                visit(e);
            }
        }
        out(close);
    }

    @Override
    public Expression visit(final Expression node) {
        return super.visit(node);
    }

    @Override
    protected Expression visitDefaultValue(final DefaultValueExpression node) {
        out("default(");
        out(node.getType().getFullName());
        out(')');
        return node;
    }

    @Override
    protected Expression visitExtension(final Expression node) {
        // Prefer a toString override, if available.
        final Set<BindingFlags> flags = BindingFlags.PublicInstanceExact;
        final MethodInfo toString = node.getType().getMethod("toString", flags, null, Type.EmptyTypes);

        if (toString.getDeclaringType() != Type.of(Expression.class)) {
            out(node.toString());
            return node;
        }

        out("[");

        if (node.getNodeType() == ExpressionType.Extension) {
            out(node.getType().getFullName());
        }
        else {
            out(node.getNodeType().toString());
        }

        out("]");
        return node;
    }

    @Override
    protected Expression visitMember(final MemberExpression node) {
        outMember(node.getTarget(), node.getMember());
        return node;
    }

    @Override
    protected Expression visitConstant(final ConstantExpression node) {
        final Object value = node.getValue();

        if (value == null) {
            out("null");
        }
        else {
            final String toString = value.toString();

            if (value instanceof String) {
                out('"');
                out(((String) value).replace("\r", "\\r").replace("\n", "\\n"));
                out('"');
            }
            else if (toString.equals(value.getClass().toString())) {
                out("value(");
                out(toString);
                out(')');
            }
            else {
                out(toString);
            }
        }

        return node;
    }

    @Override
    protected Expression visitParameter(final ParameterExpression node) {
        if (StringUtilities.isNullOrEmpty(node.getName())) {
            final int id = getParameterId(node);
            out("Param_" + id);
        }
        else {
            out(node.getName());
        }
        return node;
    }

    @Override
    protected Expression visitUnary(final UnaryExpression node) {
        switch (node.getNodeType()) {
            case Not:
                out("Not(");
                break;
            case Negate:
                out("-");
                break;
            case UnaryPlus:
                out("+");
                break;
            case Quote:
                break;
            case Throw:
                out("throw(");
                break;
            case Increment:
                out("Increment(");
                break;
            case Decrement:
                out("Decrement(");
                break;
            case PreIncrementAssign:
                out("++");
                break;
            case PreDecrementAssign:
                out("--");
                break;
            case OnesComplement:
                out("~(");
                break;
            default:
                out(node.getNodeType().toString());
                out("(");
                break;
        }

        visit(node.getOperand());

        switch (node.getNodeType()) {
            case Negate:
            case UnaryPlus:
            case PreDecrementAssign:
            case PreIncrementAssign:
            case Quote:
                break;
            case PostIncrementAssign:
                out("++");
                break;
            case PostDecrementAssign:
                out("--");
                break;
            default:
                out(")");
                break;
        }

        return node;
    }

    @Override
    protected Expression visitBinary(final BinaryExpression node) {
        if (node.getNodeType() == ExpressionType.ArrayIndex) {
            visit(node.getLeft());
            out("[");
            visit(node.getRight());
            out("]");
        }
        else {
            final String op;

            switch (node.getNodeType()) {
                case Assign:
                    op = "=";
                    break;
                case Equal:
                    op = "==";
                    break;
                case NotEqual:
                    op = "!=";
                    break;
                case AndAlso:
                    op = "AndAlso";
                    break;
                case OrElse:
                    op = "OrElse";
                    break;
                case GreaterThan:
                    op = ">";
                    break;
                case LessThan:
                    op = "<";
                    break;
                case GreaterThanOrEqual:
                    op = ">=";
                    break;
                case LessThanOrEqual:
                    op = "<=";
                    break;
                case Add:
                    op = "+";
                    break;
                case AddAssign:
                    op = "+=";
                    break;
                case Subtract:
                    op = "-";
                    break;
                case SubtractAssign:
                    op = "-=";
                    break;
                case Divide:
                    op = "/";
                    break;
                case DivideAssign:
                    op = "/=";
                    break;
                case Modulo:
                    op = "%";
                    break;
                case ModuloAssign:
                    op = "%=";
                    break;
                case Multiply:
                    op = "*";
                    break;
                case MultiplyAssign:
                    op = "*=";
                    break;
                case LeftShift:
                    op = "<<";
                    break;
                case LeftShiftAssign:
                    op = "<<=";
                    break;
                case RightShift:
                    op = ">>";
                    break;
                case RightShiftAssign:
                    op = ">>=";
                    break;
                case UnsignedRightShift:
                    op = ">>>";
                    break;
                case UnsignedRightShiftAssign:
                    op = ">>>=";
                    break;
                case And:
                    if (TypeUtils.hasBuiltInEqualityOperator(node.getType(), PrimitiveTypes.Boolean)) {
                        op = "And";
                    }
                    else {
                        op = "&";
                    }
                    break;
                case AndAssign:
                    if (TypeUtils.hasBuiltInEqualityOperator(node.getType(), PrimitiveTypes.Boolean)) {
                        op = "&&=";
                    }
                    else {
                        op = "&=";
                    }
                    break;
                case Or:
                    if (TypeUtils.hasBuiltInEqualityOperator(node.getType(), PrimitiveTypes.Boolean)) {
                        op = "Or";
                    }
                    else {
                        op = "|";
                    }
                    break;
                case OrAssign:
                    if (TypeUtils.hasBuiltInEqualityOperator(node.getType(), PrimitiveTypes.Boolean)) {
                        op = "||=";
                    }
                    else {
                        op = "|=";
                    }
                    break;
                case ExclusiveOr:
                    op = "^";
                    break;
                case ExclusiveOrAssign:
                    op = "^=";
                    break;
                case Coalesce:
                    op = "??";
                    break;

                default:
                    throw new IllegalStateException();
            }

            out("(");
            visit(node.getLeft());
            out(' ');
            out(op);
            out(' ');
            visit(node.getRight());
            out(")");
        }

        return node;
    }

    @Override
    protected Expression visitGoto(final GotoExpression node) {
        switch (node.getKind()) {
            case Goto:
                out("goto ");
                break;
            case Return:
                out("return ");
                break;
            case Break:
                out("break ");
                break;
            case Continue:
                out("continue ");
                break;
        }

        final Expression value = node.getValue();

        if (value != null) {
            visit(value);
            out(' ');
        }

        final LabelTarget target = node.getTarget();

        if (target != null) {
            visitLabelTarget(target);
        }

        return node;
    }

    @Override
    protected Expression visitLabel(final LabelExpression node) {
        final LabelTarget target = node.getTarget();

        visitLabelTarget(target);

        if (target.getName() != null) {
            out(":");
        }

        return node;
    }

    @Override
    protected LabelTarget visitLabelTarget(final LabelTarget node) {
        final String name = node.getName();

        if (name != null) {
            out(name);
        }

        return node;
    }

    @Override
    public <T> LambdaExpression<T> visitLambda(final LambdaExpression<T> node) {
        final ParameterExpressionList parameters = node.getParameters();

        if (parameters.size() == 1) {
            // p => body
            visit(parameters.get(0));
        }
        else {
            // (p1, p2, ..., pn) => body
            visitExpressions('(', parameters, ')');
        }

        out(" => ");
        visit(node.getBody());
        return node;
    }

    @Override
    protected Expression visitLoop(final LoopExpression node) {
        out("loop ");
        return super.visitLoop(node);
    }

    @Override
    protected Expression visitBlock(final BlockExpression node) {
        ++_blockDepth;
        out('{');
        increaseIndent();
        flush();
        for (final Expression v : node.getVariables()) {
            out(v.getType().getName());
            out(' ');
            visit(v);
            out(";");
            flush();
        }
        for (int i = 0, n = node.getExpressionCount(); i < n; i++) {
            visit(node.getExpression(i));
            flush();
        }
        decreaseIndent();
        out('}');
        --_blockDepth;
        return node;
    }

    @Override
    protected Expression visitInvocation(final InvocationExpression node) {
        final ExpressionList arguments = node.getArguments();

        out("Invoke(");
        visit(node.getExpression());

        for (int i = 0, n = arguments.size(); i < n; i++) {
            out(", ");
            visit(arguments.get(i));
        }

        out(")");
        return node;
    }

    @Override
    protected Expression visitMethodCall(final MethodCallExpression node) {
        final Expression target = node.getTarget();

        if (target != null) {
            visit(target);
            out(".");
        }
        else {
            out(node.getMethod().getDeclaringType().getName());
            out(".");
        }

        out(node.getMethod().getName());
        out("(");

        final ExpressionList<? extends Expression> arguments = node.getArguments();

        for (int i = 0, n = arguments.size(); i < n; i++) {
            if (i > 0) {
                out(", ");
            }
            visit(arguments.get(i));
        }

        out(")");
        return node;
    }

    @Override
    protected Expression visitConditional(final ConditionalExpression node) {
        if (node.getType() != PrimitiveTypes.Void) {
            out('(');
            visit(node.getTest());
            out(" ? ");
            visit(node.getIfTrue());
            out(" : ");
            visit(node.getIfFalse());
            out(')');
            return node;
        }

        if (node.getIfFalse() instanceof DefaultValueExpression &&
            node.getIfFalse().getType() == PrimitiveTypes.Void) {

            out("if (");
            visit(node.getTest());
            out(") ");
            if (_blockDepth > 0) {
                flush();
                increaseIndent();
            }
            visit(node.getIfTrue());
            if (_blockDepth > 0) {
                decreaseIndent();
            }
            return node;
        }

        out("if (");
        visit(node.getTest());
        out(") ");
        if (_blockDepth > 0) {
            flush();
            increaseIndent();
        }
        visit(node.getIfTrue());
        if (_blockDepth > 0) {
            flush();
            decreaseIndent();
        }
        out(" else ");
        if (_blockDepth > 0) {
            flush();
            increaseIndent();
        }
        visit(node.getIfFalse());
        if (_blockDepth > 0) {
            decreaseIndent();
        }
        return node;
    }

    @Override
    public CatchBlock visitCatchBlock(final CatchBlock node) {
        out("catch (" + node.getTest().getFullName());
        if (node.getVariable() != null) {
            final String variableName = node.getVariable().getName();
            out(variableName != null ? variableName : "");
        }
        out(") { ... }");
        return node;
    }

    @Override
    public SwitchCase visitSwitchCase(final SwitchCase node) {
        out("case ");
        visitExpressions('(', node.getTestValues(), ')');
        out(": ...");
        return node;
    }

    @Override
    protected Expression visitSwitch(final SwitchExpression node) {
        out("switch ");
        out("(");
        visit(node.getSwitchValue());
        out(") { ... }");
        return node;
    }

    @Override
    public <T extends Expression> T visitAndConvert(final T node, final String callerName) {
        return super.visitAndConvert(node, callerName);
    }

    @Override
    public <T extends Expression> ExpressionList<T> visitAndConvertList(final ExpressionList<T> nodes, final String callerName) {
        return super.visitAndConvertList(nodes, callerName);
    }

    @Override
    public ParameterExpressionList visitAndConvertList(final ParameterExpressionList nodes, final String callerName) {
        return super.visitAndConvertList(nodes, callerName);
    }
}
