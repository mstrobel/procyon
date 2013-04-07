/*
 * BreakTargetRelocation.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.AstNodeCollection;
import com.strobel.decompiler.languages.java.ast.BlockStatement;
import com.strobel.decompiler.languages.java.ast.BreakStatement;
import com.strobel.decompiler.languages.java.ast.GotoStatement;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.LabelStatement;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.Statement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public final class BreakTargetRelocation implements IAstTransform {
    private final static class LabelInfo {
        final String name;
        final List<GotoStatement> gotoStatements = new ArrayList<>();

        boolean labelIsLast;
        LabelStatement label;
        AstNode labelTarget;

        LabelInfo(final String name) {
            this.name = name;
        }

        LabelInfo(final LabelStatement label) {
            this.label = label;
            this.labelTarget = label.getNextSibling();
            this.name = label.getLabel();
        }
    }

    @Override
    public void run(final AstNode compilationUnit) {
        final Map<String, LabelInfo> labels = new LinkedHashMap<>();

        for (final AstNode node : compilationUnit.getDescendantsAndSelf()) {
            if (node instanceof LabelStatement) {
                final LabelStatement label = (LabelStatement) node;
                final LabelInfo labelInfo = labels.get(label.getLabel());

                if (labelInfo == null) {
                    labels.put(label.getLabel(), new LabelInfo(label));
                }
                else {
                    labelInfo.label = label;
                    labelInfo.labelTarget = label.getNextSibling();
                    labelInfo.labelIsLast = true;
                }
            }
            else if (node instanceof GotoStatement) {
                final GotoStatement gotoStatement = (GotoStatement) node;

                LabelInfo labelInfo = labels.get(gotoStatement.getLabel());

                if (labelInfo == null) {
                    labels.put(gotoStatement.getLabel(), labelInfo = new LabelInfo(gotoStatement.getLabel()));
                }
                else {
                    labelInfo.labelIsLast = false;
                }

                labelInfo.gotoStatements.add(gotoStatement);
            }
        }

        for (final LabelInfo labelInfo : labels.values()) {
            run(labelInfo);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void run(final LabelInfo labelInfo) {
        assert labelInfo != null;

        if (labelInfo.label == null || labelInfo.gotoStatements.isEmpty()) {
            return;
        }

        final List<Stack<AstNode>> paths = new ArrayList<>();

        for (final GotoStatement gotoStatement : labelInfo.gotoStatements) {
            paths.add(builtPath(gotoStatement));
        }

        paths.add(builtPath(labelInfo.label));

        final BlockStatement parent = findLowestCommonAncestor(paths);

        if (parent == null) {
            return;
        }

        final Set<AstNode> remainingNodes = new LinkedHashSet<>();
        final LinkedList<AstNode> orderedNodes = new LinkedList<>();
        final AstNode startNode = paths.get(0).peek();

        assert startNode != null;

        for (final Stack<AstNode> path : paths) {
            remainingNodes.add(path.peek());
        }

        AstNode current = startNode;

        while (lookAhead(current, remainingNodes)) {
            for (; current != null && !remainingNodes.isEmpty(); current = current.getNextSibling()) {
                orderedNodes.addLast(current);

                if (remainingNodes.remove(current)) {
                    break;
                }
            }
        }

        if (!remainingNodes.isEmpty()) {
            current = startNode.getPreviousSibling();

            while (lookBehind(current, remainingNodes)) {
                for (; current != null && !remainingNodes.isEmpty(); current = current.getPreviousSibling()) {
                    orderedNodes.addFirst(current);

                    if (remainingNodes.remove(current)) {
                        break;
                    }
                }
            }
        }

        if (!remainingNodes.isEmpty()) {
            return;
        }

        final AstNode insertBefore = orderedNodes.getLast().getNextSibling();
        final AstNode insertAfter = orderedNodes.getFirst().getPreviousSibling();

        final BlockStatement newBlock = new BlockStatement();
        final AstNodeCollection<Statement> blockStatements = newBlock.getStatements();

        for (final AstNode node : orderedNodes) {
            node.remove();
            blockStatements.add((Statement) node);
        }

        labelInfo.label.remove();

        if (insertBefore != null) {
            parent.insertChildBefore(insertBefore, labelInfo.label, BlockStatement.STATEMENT_ROLE);
        }
        else if (insertAfter != null) {
            parent.insertChildAfter(insertAfter, labelInfo.label, BlockStatement.STATEMENT_ROLE);
        }
        else {
            parent.getStatements().add(labelInfo.label);
        }

        parent.insertChildAfter(labelInfo.label, newBlock, BlockStatement.STATEMENT_ROLE);

        for (final GotoStatement gotoStatement : labelInfo.gotoStatements) {
            final BreakStatement breakStatement = new BreakStatement();

            breakStatement.putUserData(Keys.VARIABLE, gotoStatement.getUserData(Keys.VARIABLE));
            breakStatement.putUserData(Keys.MEMBER_REFERENCE, gotoStatement.getUserData(Keys.MEMBER_REFERENCE));
            breakStatement.setLabel(gotoStatement.getLabel());

            gotoStatement.replaceWith(breakStatement);
        }
    }

    private static boolean lookAhead(final AstNode start, final Set<AstNode> targets) {
        for (AstNode current = start;
             current != null && !targets.isEmpty();
             current = current.getNextSibling()) {

            if (targets.contains(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean lookBehind(final AstNode start, final Set<AstNode> targets) {
        for (AstNode current = start;
             current != null && !targets.isEmpty();
             current = current.getPreviousSibling()) {

            if (targets.contains(current)) {
                return true;
            }
        }
        return false;
    }

    private BlockStatement findLowestCommonAncestor(final List<Stack<AstNode>> paths) {
        if (paths.isEmpty()) {
            return null;
        }

        AstNode current = null;
        BlockStatement match = null;

    outer:
        while (true) {
            for (final Stack<AstNode> path : paths) {
                if (path.isEmpty()) {
                    break outer;
                }

                if (current == null) {
                    current = path.peek();
                }
                else if (path.peek() != current) {
                    break outer;
                }
            }

            for (final Stack<AstNode> path : paths) {
                path.pop();
            }

            if (current instanceof BlockStatement) {
                match = (BlockStatement) current;
            }

            current = null;
        }

        return match;
    }

    private Stack<AstNode> builtPath(final AstNode node) {
        assert node != null;

        final Stack<AstNode> path = new Stack<>();

        for (AstNode current = node; current != null; current = current.getParent()) {
            path.push(current);

            if (current instanceof MethodDeclaration) {
                break;
            }
        }

        return path;
    }
}
