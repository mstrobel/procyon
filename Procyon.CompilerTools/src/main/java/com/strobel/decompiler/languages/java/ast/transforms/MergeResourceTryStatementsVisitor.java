package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.Flags;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.AnyNode;
import com.strobel.decompiler.patterns.Choice;
import com.strobel.decompiler.patterns.DeclaredVariableBackReference;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.NamedNode;
import com.strobel.decompiler.patterns.Pattern;

import java.util.ArrayList;
import java.util.List;

import static com.strobel.core.CollectionUtilities.first;

public class MergeResourceTryStatementsVisitor extends ContextTrackingVisitor<Void> {
    private final BlockStatement _emptyResource;

    public MergeResourceTryStatementsVisitor(final DecompilerContext context) {
        super(context);

        final VariableDeclarationStatement rv = new VariableDeclarationStatement(
            new AnyNode().toType(),
            Pattern.ANY_STRING,
            new AnyNode().toExpression()
        );

        rv.addModifier(Flags.Flag.FINAL);

        final Expression rr = new DeclaredVariableBackReference("resourceDeclaration").toExpression();

        _emptyResource = new BlockStatement(
            new NamedNode("resourceDeclaration", rv).toStatement(),
            new NamedNode("resourceDisposal",
                          new Choice(new ExpressionStatement(rr.clone().invoke("close")),
                                     new IfElseStatement(Expression.MYSTERY_OFFSET,
                                                         new BinaryOperatorExpression(rr.clone(),
                                                                                      BinaryOperatorType.INEQUALITY,
                                                                                      new NullReferenceExpression()),
                                                         new BlockStatement(new ExpressionStatement(rr.clone().invoke("close")))))).toStatement()
        );
    }

    @Override
    public Void visitTryCatchStatement(final TryCatchStatement node, final Void data) {
        super.visitTryCatchStatement(node, data);

        if (node.getDeclaredResources().isEmpty() && node.getExternalResources().isEmpty()) {
            return null;
        }

        final List<AstNode> resources = new ArrayList<>();

        TryCatchStatement current = node;

        while (current.getCatchClauses().isEmpty() &&
               current.getFinallyBlock().isNull()) {

            final Match m = _emptyResource.match(current.getTryBlock());

            if (m.success()) {
                final VariableDeclarationStatement innerResource = first(m.<VariableDeclarationStatement>get("resourceDeclaration"));

                current.getTryBlock().getStatements().clear();
                current.getDeclaredResources().add(innerResource);
            }

            final AstNode parent = current.getParent();

            if (parent instanceof BlockStatement &&
                parent.getParent() instanceof TryCatchStatement) {

                final TryCatchStatement parentTry = (TryCatchStatement) parent.getParent();

                if (parentTry.getTryBlock().getStatements().hasSingleElement()) {
                    if (!current.getDeclaredResources().isEmpty()) {
                        resources.addAll(0, current.getDeclaredResources());
                    }
                    if (!current.getExternalResources().isEmpty()) {
                        resources.addAll(0, current.getExternalResources());
                    }

                    current = parentTry;
                    continue;
                }
            }

            break;
        }

        final BlockStatement tryContent = node.getTryBlock();

        if (current != node) {
            for (final AstNode resource : resources) {
                resource.remove();

                if (resource instanceof VariableDeclarationStatement) {
                    current.getDeclaredResources().add((VariableDeclarationStatement) resource);
                }
                else {
                    current.getExternalResources().add((IdentifierExpression) resource);
                }
            }

            tryContent.remove();
            current.setTryBlock(tryContent);

            final BlockStatement block = current.getFinallyBlock();

            if (!block.isNull() && block.getStatements().isEmpty()) {
                block.remove();
            }
        }

        return null;
    }
}
