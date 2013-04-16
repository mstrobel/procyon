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

import com.strobel.decompiler.languages.java.ast.*;

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

        final LabelStatement label = labelInfo.label;

        if (label == null || labelInfo.gotoStatements.isEmpty()) {
            return;
        }

        final List<Stack<AstNode>> paths = new ArrayList<>();

        for (final GotoStatement gotoStatement : labelInfo.gotoStatements) {
            paths.add(buildPath(gotoStatement));
        }

        paths.add(buildPath(label));

        final Statement commonAncestor = findLowestCommonAncestor(paths);

        if (commonAncestor instanceof SwitchStatement &&
            labelInfo.gotoStatements.size() == 1 &&
            label.getParent() instanceof BlockStatement &&
            label.getParent().getParent() instanceof SwitchSection &&
            label.getParent().getParent().getParent() == commonAncestor) {

            final GotoStatement s = labelInfo.gotoStatements.get(0);
            
            if (s.getParent() instanceof BlockStatement &&
                s.getParent().getParent() instanceof SwitchSection &&
                s.getParent().getParent().getParent() == commonAncestor) {

                //
                // We have a switch section that should fall through to another section.
                // make sure the fall through target is positioned after the section with
                // the goto, then remove the goto and the target label.
                //

                final SwitchStatement parentSwitch = (SwitchStatement) commonAncestor;

                final SwitchSection targetSection = (SwitchSection) label.getParent().getParent();
                final BlockStatement fallThroughBlock = (BlockStatement) s.getParent();
                final SwitchSection fallThroughSection = (SwitchSection) fallThroughBlock.getParent();

                if (fallThroughSection.getNextSibling() != targetSection) {
                    targetSection.remove();
                    parentSwitch.getSwitchSections().insertAfter(fallThroughSection, targetSection);
                }

                final BlockStatement parentBlock = (BlockStatement) label.getParent();

                s.remove();
                label.remove();

                if (fallThroughBlock.getStatements().isEmpty()) {
                    fallThroughBlock.remove();
                }

                if (parentBlock.getStatements().isEmpty()) {
                    parentBlock.remove();
                }

                return;
            }
        }


        final BlockStatement parent = findLowestCommonAncestorBlock(paths);

        if (parent == null) {
            return;
        }

        final Set<AstNode> remainingNodes = new LinkedHashSet<>();
        final LinkedList<AstNode> orderedNodes = new LinkedList<>();
        final AstNode startNode = paths.get(0).peek();

        assert startNode != null;

        for (final Stack<AstNode> path : paths) {
            if (path.isEmpty()) {
                return;
            }
            remainingNodes.add(path.peek());
        }

        AstNode current = startNode;

        while (lookAhead(current, remainingNodes)) {
            for (; current != null && !remainingNodes.isEmpty(); current = current.getNextSibling()) {
                if (current instanceof Statement) {
                    orderedNodes.addLast(current);
                }

                if (remainingNodes.remove(current)) {
                    break;
                }
            }
        }

        if (!remainingNodes.isEmpty()) {
            current = startNode.getPreviousSibling();

            while (lookBehind(current, remainingNodes)) {
                for (; current != null && !remainingNodes.isEmpty(); current = current.getPreviousSibling()) {
                    if (current instanceof Statement) {
                        orderedNodes.addFirst(current);
                    }

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

        label.remove();

        if (insertBefore != null) {
            parent.insertChildBefore(insertBefore, label, BlockStatement.STATEMENT_ROLE);
        }
        else if (insertAfter != null) {
            parent.insertChildAfter(insertAfter, label, BlockStatement.STATEMENT_ROLE);
        }
        else {
            parent.getStatements().add(label);
        }

        parent.insertChildAfter(label, newBlock, BlockStatement.STATEMENT_ROLE);

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

    private BlockStatement findLowestCommonAncestorBlock(final List<Stack<AstNode>> paths) {
        if (paths.isEmpty()) {
            return null;
        }

        AstNode current = null;
        BlockStatement match = null;

        final Stack<AstNode> sinceLastMatch = new Stack<>();

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
                sinceLastMatch.clear();
                match = (BlockStatement) current;
            }
            else {
                sinceLastMatch.push(current);
            }

            current = null;
        }

        while (!sinceLastMatch.isEmpty()) {
            for (int i = 0, n = paths.size(); i < n; i++) {
                paths.get(i).push(sinceLastMatch.peek());
            }
            sinceLastMatch.pop();
        }

        return match;
    }

    private Statement findLowestCommonAncestor(final List<Stack<AstNode>> paths) {
        if (paths.isEmpty()) {
            return null;
        }

        AstNode current = null;
        Statement match = null;

        final Stack<AstNode> sinceLastMatch = new Stack<>();

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

            if (current instanceof Statement) {
                sinceLastMatch.clear();
                match = (Statement) current;
            }
            else {
                sinceLastMatch.push(current);
            }

            current = null;
        }

        while (!sinceLastMatch.isEmpty()) {
            for (int i = 0, n = paths.size(); i < n; i++) {
                paths.get(i).push(sinceLastMatch.peek());
            }
            sinceLastMatch.pop();
        }

        return match;
    }

    private Stack<AstNode> buildPath(final AstNode node) {
        assert node != null;

        final Stack<AstNode> path = new Stack<>();

        path.push(node);

        for (AstNode current = node; current != null; current = current.getParent()) {
            path.push(current);

            if (current instanceof MethodDeclaration) {
                break;
            }
        }

        return path;
    }
}
