package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.TryCatchStatement;
import com.strobel.decompiler.languages.java.ast.VariableDeclarationStatement;

import java.util.ArrayList;
import java.util.List;

public class MergeResourceTryStatementsVisitor extends ContextTrackingVisitor<Void> {
    public MergeResourceTryStatementsVisitor(final DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitTryCatchStatement(final TryCatchStatement node, final Void data) {
        super.visitTryCatchStatement(node, data);

        if (node.getResources().isEmpty()) {
            return null;
        }

        final List<VariableDeclarationStatement> resources = new ArrayList<>();

        TryCatchStatement current = node;

        while (current.getCatchClauses().isEmpty() &&
               current.getFinallyBlock().isNull()) {

            final AstNode parent = current.getParent();

            if (parent instanceof BlockStatement &&
                parent.getParent() instanceof TryCatchStatement) {

                final TryCatchStatement parentTry = (TryCatchStatement) parent.getParent();

                if (parentTry.getTryBlock().getStatements().hasSingleElement()) {
                    if (!current.getResources().isEmpty()) {
                        resources.addAll(0, current.getResources());
                    }

                    current = parentTry;
                    continue;
                }
            }

            break;
        }

        final BlockStatement tryContent = node.getTryBlock();

        if (current != node) {
            for (final VariableDeclarationStatement resource : resources) {
                resource.remove();
                current.getResources().add(resource);
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
