/*
 * Pattern.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.patterns;

import com.strobel.core.StringUtilities;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.Expression;

import java.util.Stack;

public abstract class Pattern implements INode {
    public final static String ANY_STRING = "$any$";

    public static boolean matchString(final String pattern, final String text) {
        return ANY_STRING.equals(pattern) || StringUtilities.equals(pattern, text);
    }

    public final AstNode toNode() {
        return AstNode.forPattern(this);
    }

    public final Expression toExpression() {
        return Expression.forPattern(this);
    }

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public Role getRole() {
        return null;
    }

    @Override
    public INode getFirstChild() {
        return null;
    }

    @Override
    public INode getNextSibling() {
        return null;
    }

    @Override
    public abstract boolean matches(final INode other, final Match match);

    @Override
    public boolean matchesCollection(final Role role, final INode position, final Match match, final BacktrackingInfo backtrackingInfo) {
        return matches(position, match);
    }

    public static boolean matchesCollection(
        final Role<?> role,
        final INode firstPatternChild,
        final INode firstOtherChild,
        final Match match) {

        final BacktrackingInfo backtrackingInfo = new BacktrackingInfo();
        final Stack<INode> patternStack = new Stack<>();
        final Stack<PossibleMatch> stack = new Stack<>();

        patternStack.push(firstPatternChild);
        stack.push(new PossibleMatch(firstOtherChild, match.getCheckpoint()));

        while (!stack.isEmpty()) {
            INode current1 = patternStack.pop();
            INode current2 = stack.peek().nextOther;

            match.restoreCheckpoint(stack.pop().checkpoint);

            boolean success = true;

            while (current1 != null && success) {
                while (current1 != null && current1.getRole() != role) {
                    current1 = current1.getNextSibling();
                }

                while (current2 != null && current2.getRole() != role) {
                    current2 = current2.getNextSibling();
                }

                if (current1 == null) {
                    break;
                }

                assert stack.size() == patternStack.size();
                success = current1.matchesCollection(role, current2, match, backtrackingInfo);
                assert stack.size() >= patternStack.size();

                while (stack.size() > patternStack.size()) {
                    patternStack.push(current1.getNextSibling());
                }

                current1 = current1.getNextSibling();

                if (current2 != null) {
                    current2 = current2.getNextSibling();
                }
            }

            while (current2 != null && current2.getRole() != role) {
                current2 = current2.getNextSibling();
            }

            if (success && current2 == null) {
                return true;
            }
        }

        return false;
    }
}
