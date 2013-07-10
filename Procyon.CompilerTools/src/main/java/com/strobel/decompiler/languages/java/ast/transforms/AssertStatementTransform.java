package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MetadataResolver;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.AnyNode;
import com.strobel.decompiler.patterns.Choice;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.NamedNode;
import com.strobel.decompiler.patterns.OptionalNode;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.TypedNode;

import static com.strobel.core.CollectionUtilities.first;

public class AssertStatementTransform extends ContextTrackingVisitor<Void> {
    public AssertStatementTransform(final DecompilerContext context) {
        super(context);
    }

    private final static IfElseStatement ASSERT_PATTERN;
    private final static AssignmentExpression ASSERTIONS_DISABLED_PATTERN;

    static {
        ASSERT_PATTERN = new IfElseStatement(
            new UnaryOperatorExpression(
                UnaryOperatorType.NOT,
                new BinaryOperatorExpression(
                    new TypeReferenceExpression(new SimpleType(Pattern.ANY_STRING)).member("$assertionsDisabled"),
                    BinaryOperatorType.LOGICAL_OR,
                    new AnyNode("condition").toExpression()
                )
            ),
            new BlockStatement(
                new ThrowStatement(
                    new ObjectCreationExpression(
                        new SimpleType("AssertionError"),
                        new OptionalNode(
                            new NamedNode(
                                "message",
                                new PrimitiveExpression(PrimitiveExpression.ANY_STRING)
                            )
                        ).toExpression()
                    )
                )
            )
        );

        ASSERTIONS_DISABLED_PATTERN = new AssignmentExpression(
            new NamedNode(
                "$assertionsDisabled",
                new Choice(
                    new IdentifierExpression("$assertionsDisabled"),
                    new TypedNode(TypeReferenceExpression.class).toExpression().member("$assertionsDisabled")
                )
            ).toExpression(),
            new UnaryOperatorExpression(
                UnaryOperatorType.NOT,
                new InvocationExpression(
                    new MemberReferenceExpression(
                        new NamedNode("type", new ClassOfExpression(new SimpleType(Pattern.ANY_STRING))).toExpression(),
                        "desiredAssertionStatus"
                    )
                )
            )
        );
    }

    @Override
    public Void visitIfElseStatement(final IfElseStatement node, final Void data) {
        super.visitIfElseStatement(node, data);

        transformAssert(node);

        return null;
    }

    @Override
    public Void visitAssignmentExpression(final AssignmentExpression node, final Void data) {
        super.visitAssignmentExpression(node, data);

        removeAssertionsDisabledAssignment(node);

        return null;
    }

    private void removeAssertionsDisabledAssignment(final AssignmentExpression node) {
        if (context.getSettings().getShowSyntheticMembers()) {
            return;
        }

        final Match m = ASSERTIONS_DISABLED_PATTERN.match(node);

        if (!m.success()) {
            return;
        }

        final AstNode parent = node.getParent();

        if (!(parent instanceof ExpressionStatement &&
              parent.getParent() instanceof BlockStatement &&
              parent.getParent().getParent() instanceof MethodDeclaration)) {

            return;
        }

        final MethodDeclaration staticInitializer = (MethodDeclaration) parent.getParent().getParent();
        final MethodDefinition methodDefinition = staticInitializer.getUserData(Keys.METHOD_DEFINITION);

        if (methodDefinition == null || !methodDefinition.isTypeInitializer()) {
            return;
        }

        final Expression field = first(m.<IdentifierExpression>get("$assertionsDisabled"));
        final ClassOfExpression type = m.<ClassOfExpression>get("type").iterator().next();
        final MemberReference reference = field.getUserData(Keys.MEMBER_REFERENCE);

        if (!(reference instanceof FieldReference)) {
            return;
        }

        final FieldDefinition resolvedField = ((FieldReference) reference).resolve();

        if (!resolvedField.isSynthetic()) {
            return;
        }

        final TypeReference typeReference = type.getType().getUserData(Keys.TYPE_REFERENCE);

        if (typeReference != null &&
            (MetadataResolver.areEquivalent(context.getCurrentType(), typeReference) ||
             MetadataHelper.isEnclosedBy(context.getCurrentType(), typeReference))) {

            parent.remove();

            if (staticInitializer.getBody().getStatements().isEmpty()) {
                staticInitializer.remove();
            }
        }
    }

    private AssertStatement transformAssert(final IfElseStatement ifElse) {
        final Match m = ASSERT_PATTERN.match(ifElse);

        if (!m.success()) {
            return null;
        }

        final Expression condition = m.<Expression>get("condition").iterator().next();
        final AssertStatement assertStatement = new AssertStatement();

        condition.remove();
        assertStatement.setCondition(condition);

        if (m.has("message")) {
            final PrimitiveExpression message = m.<PrimitiveExpression>get("message").iterator().next();
            assertStatement.setMessage((String) message.getValue());
        }

        ifElse.replaceWith(assertStatement);

        return assertStatement;
    }
}
