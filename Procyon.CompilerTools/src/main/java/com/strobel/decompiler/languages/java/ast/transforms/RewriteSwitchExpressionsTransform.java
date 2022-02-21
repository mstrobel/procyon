package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.DefaultMap;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.analysis.ControlFlowEdge;
import com.strobel.decompiler.languages.java.analysis.ControlFlowGraphBuilder;
import com.strobel.decompiler.languages.java.analysis.ControlFlowNode;
import com.strobel.decompiler.languages.java.analysis.ControlFlowNodeType;
import com.strobel.decompiler.languages.java.analysis.Correlator;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.*;
import com.strobel.functions.Supplier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.*;

public class RewriteSwitchExpressionsTransform extends ContextTrackingVisitor<Void> {
    private final ExpressionStatement resultAssignment;
    private final ExpressionStatement firstResultAssignment;
    private final VariableDeclarationStatement resultDeclaration;

    public RewriteSwitchExpressionsTransform(final DecompilerContext context) {
        super(context);

        final VariableDeclarationStatement vd = new VariableDeclarationStatement();

        vd.setType(new NamedNode("resultType", new AnyNode()).toType());
        vd.setAnyModifiers(true);

        vd.getVariables().add(
            new NamedNode("resultInitializer",
                          new VariableInitializer(Pattern.ANY_STRING,
                                                  new NamedNode("initValue",
                                                                new Choice(new TypedNode(PrimitiveExpression.class),
                                                                           new NullReferenceExpression())).toExpression())).toVariableInitializer());

        resultDeclaration = vd;

        firstResultAssignment = new ExpressionStatement(
            new AssignmentExpression(
                new NamedNode("resultInitializer", new IdentifierExpression(Pattern.ANY_STRING)).toExpression(),
                new NamedNode("yieldedValue", new TypedNode(Expression.class)).toExpression()
            )
        );

        resultAssignment = new ExpressionStatement(
            new AssignmentExpression(
                new IdentifierBackReference("resultInitializer").toExpression(),
                new NamedNode("yieldedValue", new TypedNode(Expression.class)).toExpression()
            )
        );
    }

    @Override
    public void run(final AstNode compilationUnit) {
        super.run(compilationUnit);
    }

    @Override
    public Void visitSwitchStatement(final SwitchStatement switchNode, final Void data) {
        super.visitSwitchStatement(switchNode, data);

        final ControlFlowGraphBuilder graphBuilder = new ControlFlowGraphBuilder();
        final List<ControlFlowNode> nodes = graphBuilder.buildControlFlowGraph(switchNode, new JavaResolver(context));
        final Set<String> nestedLabels = new HashSet<>();

        switchNode.acceptVisitor(
            new DepthFirstAstVisitor<Void, Void>() {
                @Override
                public Void visitLabelStatement(final LabelStatement node, final Void data) {
                    nestedLabels.add(node.getLabel());
                    return super.visitLabelStatement(node, data);
                }

                @Override
                public Void visitLabeledStatement(final LabeledStatement node, final Void data) {
                    nestedLabels.add(node.getLabel());
                    return super.visitLabeledStatement(node, data);
                }
            },
            null
        );

        final List<ControlFlowNode> endNodes = toList(
            where(nodes,
                  new Predicate<ControlFlowNode>() {
                      @Override
                      public boolean test(final ControlFlowNode node) {
                          final Statement ps = node.getPreviousStatement();
                          return node.getType() == ControlFlowNodeType.EndNode &&
                                 ps != switchNode &&
                                 ps != null &&
                                 !(ps instanceof BlockStatement) &&
                                 ps.getParent(SwitchStatement.class) == switchNode;
                      }
                  })
        );

        final SwitchInfo info = new SwitchInfo();
        final List<BreakStatement> breaks = new ArrayList<>();
        final List<ThrowStatement> throwStatements = new ArrayList<>();
        final List<ControlFlowNode> possibleFallThroughNodes = new ArrayList<>();

        outer:
        for (final ControlFlowNode node : endNodes) {
            final Statement ps = node.getPreviousStatement();

            if (ps instanceof BreakStatement) {
                final BreakStatement br = (BreakStatement) ps;

                if (!StringUtilities.isNullOrEmpty(br.getLabel()) && !nestedLabels.contains(br.getLabel())) {
                    // FAIL: Can't break out of the switch except via `yield` or `throw`.
                    return null;
                }

                breaks.add(br);
            }
            else if (ps instanceof ThrowStatement) {
                throwStatements.add((ThrowStatement) ps);
                continue;
            }
            else if (AstNode.isUnconditionalBranch(ps)) {
                // FAIL: Can't break out of the switch except via `yield` or `throw`.
                return null;
            }
            else {
                for (final ControlFlowEdge edge : node.getOutgoing()) {
                    if (edge.getTo() != null && edge.getTo().getOutgoing().size() != 0) {
                        continue outer;
                    }
                }
                possibleFallThroughNodes.add(node);
                info.needClassicStyle = true;
            }
        }

        if (breaks.isEmpty()) {
            return null;
        }

        IdentifierExpression firstAssignment = null;

        for (final BreakStatement br : breaks) {
            final Match m = Match.createNew();
            final Statement bp = br.getPreviousStatement();

            if (firstAssignment != null) {
                m.add("resultInitializer", firstAssignment);
                if (!resultAssignment.matches(bp, m)) {
                    // FAIL: No assignment prior to the break.
                    return null;
                }
            }
            else if (firstResultAssignment.matches(bp, m)) {
                firstAssignment = first(m.<IdentifierExpression>get("resultInitializer"));
                info.resultIdentifier = firstAssignment;
                info.resultVariable = firstAssignment.getUserData(Keys.VARIABLE);

                if (info.resultVariable == null) {
                    // FAIL: Need to be able to correlate reads on the variable.
                    return null;
                }
            }
            else {
                // FAIL: No assignment prior to the break.
                return null;
            }

            final SwitchSection ss = br.getParent(SwitchSection.class);

            if (ss == null || ss.isNull()) {
                // FAIL: This isn't supposed to be possible.
                return null;
            }

            final CaseInfo caseInfo = info.cases.get(ss);

            caseInfo.breaks.add(br);
            caseInfo.yieldedValues.put(br, first(m.<Expression>get("yieldedValue")));

            caseInfo.isDefault = ss.getCaseLabels().isEmpty() || ss.getCaseLabels().size() == 1 &&
                                                                 ss.getCaseLabels().firstOrNullObject().getExpression().isNull();
        }

        if (info.resultIdentifier == null) {
            return null;
        }

        for (final ThrowStatement ts : throwStatements) {
            final SwitchSection ss = ts.getParent(SwitchSection.class);

            if (ss == null || ss.isNull()) {
                // FAIL: This isn't supposed to be possible.
                return null;
            }

            info.cases.get(ss).hasThrows = true;
        }

        for (final ControlFlowNode fallThrough : possibleFallThroughNodes) {
            final Statement ps = fallThrough.getPreviousStatement();
            final SwitchSection ss = ps != null ? ps.getParent(SwitchSection.class) : null;
            final CaseInfo caseInfo;

            if (ss == null || !(caseInfo = info.cases.get(ss)).breaks.isEmpty()) {
                // FAIL: Somehow we're breaking out of the switch
                return null;
            }

            caseInfo.hasFallThrough = true;
        }

        boolean fallThroughNeeded = false;

        for (final SwitchSection section : switchNode.getSwitchSections()) {
            final boolean isEmpty = !info.cases.containsKey(section);
            final CaseInfo caseInfo = info.cases.get(section);

            if (isEmpty) {
                caseInfo.isEmpty = true;
            }

            if (isEmpty || caseInfo.hasFallThrough) {
                fallThroughNeeded = true;
            }
            else if (caseInfo.hasThrows || !caseInfo.yieldedValues.isEmpty()) {
                fallThroughNeeded = false;
            }
        }

        if (fallThroughNeeded) {
            // FAIL: Some section didn't produce a value, and none of its successors
            //       produced a value either.
            return null;
        }

        final boolean areCorrelated = Correlator.areCorrelated(info.resultIdentifier.clone(), switchNode);

        if (areCorrelated) {
            // FAIL: Our target variable gets read somewhere inside the switch.
            return null;
        }

        rewrite(switchNode, info);

        if (info.resultVariable.isGenerated()) {
            new SwitchExpressionInlining(info).tryInline();
        }

        return null;
    }

    private final class SwitchExpressionInlining extends ContextTrackingVisitor<Void> {
        private final List<Expression> references = new ArrayList<>();
        private final SwitchInfo info;
        private final MethodDefinition currentMethod;

        SwitchExpressionInlining(final SwitchInfo info) {
            super(new DecompilerContext(RewriteSwitchExpressionsTransform.this.context.getSettings()));
            this.info = info;
            this.currentMethod = RewriteSwitchExpressionsTransform.this.context.getCurrentMethod();
        }

        final void tryInline() {
            if (currentMethod == null || info.rewrittenExpression == null || (info.rewrittenAssignment == null && info.rewrittenDeclaration == null)) {
                return;
            }

            final AstNode parentMethod;

            if (currentMethod.isConstructor()) {
                parentMethod = info.rewrittenExpression.getParent(ConstructorDeclaration.class);
            }
            else {
                parentMethod = info.rewrittenExpression.getParent(MethodDeclaration.class);
            }

            if (parentMethod == null) {
                return;
            }

            parentMethod.acceptVisitor(this, null);

            if (references.size() == 1) {
                final Expression target = references.get(0);

                if (info.rewrittenAssignment != null) {
                    info.rewrittenAssignment.remove();
                }
                else if (info.rewrittenDeclaration.getVariables().size() == 1) {
                    info.rewrittenDeclaration.remove();
                }
                else {
                    final VariableInitializer vi = info.rewrittenDeclaration.getVariable(info.resultIdentifier.getIdentifier());
                    if (vi == null) {
                        return;
                    }
                    vi.remove();
                }
                info.rewrittenExpression.remove();
                target.replaceWith(info.rewrittenExpression);
            }
        }

        @Override
        public Void visitIdentifierExpression(final IdentifierExpression node, final Void data) {
            super.visitIdentifierExpression(node, data);

            if (node.getRole() == AssignmentExpression.LEFT_ROLE && node.getParent(ExpressionStatement.class) == info.rewrittenAssignment) {
                return null;
            }

            if (node.matches(info.resultIdentifier)) {
                references.add(node);
            }

            return null;
        }
    }

    private void rewrite(final SwitchStatement node, final SwitchInfo info) {
        final AstNodeCollection<SwitchSection> sections = node.getSwitchSections();
        final SwitchExpression se = new SwitchExpression();

        for (final SwitchSection ss : sections) {
            final CaseInfo caseInfo = info.cases.get(ss);
            final AstNodeCollection<Statement> statements = ss.getStatements();
            final Statement first = statements.firstOrNullObject();
            final SwitchExpressionArm arm = new SwitchExpressionArm();
            final BlockStatement b;

            if (statements.size() == 1 && first instanceof BlockStatement) {
                b = (BlockStatement) first;
                b.remove();
            }
            else {
                b = new BlockStatement();

                for (final Statement statement : statements) {
                    statement.remove();
                    b.add(statement);
                }
            }

            for (final CaseLabel label : ss.getCaseLabels()) {
                final Expression value = label.getExpression();

                if (value.isNull()) {
                    arm.setDefaultCase(true);
                }
                else {
                    value.remove();
                    arm.getValues().add(value);
                }
            }

            for (final BreakStatement br : caseInfo.breaks) {
                final Expression value = caseInfo.yieldedValues.get(br);
                final Statement parentStatement = value.getParent(Statement.class);

                assert parentStatement != null;

                parentStatement.remove();
                value.remove();

                br.setYield(true);
                br.setValue(value);
            }

            final Statement newFirst;

            if (!info.needClassicStyle &&
                b.getStatements().size() == 1 &&
                !(newFirst = b.getStatements().firstOrNullObject()).isNull()) {

                if (newFirst instanceof BreakStatement) {
                    final Expression value = ((BreakStatement) newFirst).getValue();
                    value.remove();
                    arm.getStatements().add(new ExpressionStatement(value));
                }
                else if (newFirst instanceof ThrowStatement) {
                    newFirst.remove();
                    arm.getStatements().add(newFirst);
                }
                else {
                    arm.getStatements().add(b);
                }
            }
            else {
                arm.getStatements().add(b);
            }

            arm.setClassicStyle(info.needClassicStyle);
            se.getArms().add(arm);
        }

        final Expression governingExpression = node.getExpression();

        governingExpression.remove();
        se.setGoverningExpression(governingExpression);

        final Statement ps = node.getPreviousStatement();
        final Match m = ps != null ? resultDeclaration.match(ps) : Match.failure();

        final VariableInitializer vi;

        if (m.success() &&
            info.resultVariable == (vi = first(m.<VariableInitializer>get("resultInitializer"))).getUserData(Keys.VARIABLE)) {

            info.rewrittenDeclaration = vi.getParent(VariableDeclarationStatement.class);
            vi.getInitializer().replaceWith(se);
            node.remove();
        }
        else {
            info.rewrittenAssignment = new ExpressionStatement(new AssignmentExpression(info.resultIdentifier.clone(), se));
            node.replaceWith(info.rewrittenAssignment);
        }

        info.rewrittenExpression = se;
    }

    final static class CaseInfo {
        final List<BreakStatement> breaks = new ArrayList<>();
        final Map<BreakStatement, Expression> yieldedValues = new HashMap<>();

        boolean isEmpty;
        boolean isDefault;
        boolean hasThrows;
        boolean hasFallThrough;

        CaseInfo() {
        }
    }

    final static class SwitchInfo {
        IdentifierExpression resultIdentifier;
        Variable resultVariable;
        SwitchExpression rewrittenExpression;
        ExpressionStatement rewrittenAssignment;
        VariableDeclarationStatement rewrittenDeclaration;

        final Map<SwitchSection, CaseInfo> cases = new DefaultMap<>(
            new Supplier<CaseInfo>() {
                @Override
                public CaseInfo get() {
                    return new CaseInfo();
                }
            }
        );

        boolean needClassicStyle;
    }
}
