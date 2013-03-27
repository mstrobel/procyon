/*
 * AstNode.java
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

package com.strobel.decompiler.languages.java.ast;

import com.strobel.core.CollectionUtilities;
import com.strobel.core.Freezable;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Region;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.patterns.BacktrackingInfo;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.Role;
import com.strobel.decompiler.utilities.TreeTraversal;
import com.strobel.functions.Function;
import com.strobel.util.ContractUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class AstNode extends Freezable implements INode {
    final static Role<AstNode> ROOT_ROLE = new Role<>("Root", AstNode.class);

    final static int ROLE_INDEX_MASK = (1 << Role.ROLE_INDEX_BITS) - 1;
    final static int FROZEN_BIT = 1 << Role.ROLE_INDEX_BITS;

    protected final static int AST_NODE_USED_FLAGS = Role.ROLE_INDEX_BITS + 1;

    @SuppressWarnings("ProtectedField")
    protected int flags = ROOT_ROLE.getIndex();

    private AstNode _parent;
    private AstNode _previousSibling;
    private AstNode _nextSibling;
    private AstNode _firstChild;
    private AstNode _lastChild;

    protected AstNode() {
        if (isNull()) {
            freeze();
        }
    }

    protected static boolean matchString(final String pattern, final String text) {
        return Pattern.matchString(pattern, text);
    }

    final void setRoleUnsafe(final Role<?> role) {
        flags = (flags & ~ROLE_INDEX_MASK) | role.getIndex();
    }

    public abstract <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data);

    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public AstNode clone() {
        try {
            final AstNode clone = (AstNode) super.clone();

            clone._parent = null;
            clone._firstChild = null;
            clone._lastChild = null;
            clone._previousSibling = null;
            clone._nextSibling = null;
            clone.flags &= ~FROZEN_BIT;

            for (AstNode current = _firstChild; current != null; current = current._nextSibling) {
                clone.addChildUnsafe(current.clone(), current.getRole());
            }

//            clone.cloneAnnotations();

            return clone;
        }
        catch (CloneNotSupportedException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Tree Structure">

    public final AstNode getParent() {
        return _parent;
    }

    public final AstNode getPreviousSibling() {
        return _previousSibling;
    }

    public final AstNode getLastChild() {
        return _lastChild;
    }

    @Override
    public final AstNode getFirstChild() {
        return _firstChild;
    }

    @Override
    public final AstNode getNextSibling() {
        return _nextSibling;
    }

    public final boolean hasChildren() {
        return _firstChild != null;
    }

    public final AstNode getNextNode() {
        final AstNode nextSibling = getNextSibling();

        if (nextSibling != null) {
            return nextSibling;
        }

        final AstNode parent = getParent();

        if (parent != null) {
            return parent.getNextNode();
        }

        return null;
    }

    public final AstNode getPreviousNode() {
        final AstNode previousSibling = getPreviousSibling();

        if (previousSibling != null) {
            return previousSibling;
        }

        final AstNode parent = getParent();

        if (parent != null) {
            return parent.getPreviousNode();
        }

        return null;
    }

    public final Iterable<AstNode> getChildren() {
        return new Iterable<AstNode>() {
            @Override
            public final Iterator<AstNode> iterator() {
                return new Iterator<AstNode>() {
                    AstNode next = _firstChild;

                    @Override
                    public final boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public final AstNode next() {
                        final AstNode result = next;

                        if (result == null) {
                            throw new NoSuchElementException();
                        }

                        next = result._nextSibling;

                        return result;
                    }

                    @Override
                    public final void remove() {
                        throw ContractUtils.unsupported();
                    }
                };
            }
        };
    }

    public final Iterable<AstNode> getAncestors() {
        return new Iterable<AstNode>() {
            @Override
            public final Iterator<AstNode> iterator() {
                return new Iterator<AstNode>() {
                    AstNode next = AstNode.this._parent;

                    @Override
                    public final boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public final AstNode next() {
                        final AstNode result = next;

                        if (result == null) {
                            throw new NoSuchElementException();
                        }

                        next = result._parent;

                        return result;
                    }

                    @Override
                    public final void remove() {
                        throw ContractUtils.unsupported();
                    }
                };
            }
        };
    }

    public final Iterable<AstNode> getAncestorsAndSelf() {
        return new Iterable<AstNode>() {
            @Override
            public final Iterator<AstNode> iterator() {
                return new Iterator<AstNode>() {
                    AstNode next = AstNode.this;

                    @Override
                    public final boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public final AstNode next() {
                        final AstNode result = next;

                        if (result == null) {
                            throw new NoSuchElementException();
                        }

                        next = result._parent;

                        return result;
                    }

                    @Override
                    public final void remove() {
                        throw ContractUtils.unsupported();
                    }
                };
            }
        };
    }

    public final Iterable<AstNode> getDescendants() {
        return TreeTraversal.preOrder(
            getChildren(),
            new Function<AstNode, Iterable<AstNode>>() {
                @Override
                public Iterable<AstNode> apply(final AstNode n) {
                    return n.getChildren();
                }
            }
        );
    }

    public final Iterable<AstNode> getDescendantsAndSelf() {
        return TreeTraversal.preOrder(
            this,
            new Function<AstNode, Iterable<AstNode>>() {
                @Override
                public Iterable<AstNode> apply(final AstNode n) {
                    return n.getChildren();
                }
            }
        );
    }

    @SuppressWarnings("unchecked")
    public final <T extends AstNode> T getChildByRole(final Role<T> role) {
        VerifyArgument.notNull(role, "role");

        final int roleIndex = role.getIndex();

        for (AstNode current = _firstChild; current != null; current = current._nextSibling) {
            if ((current.flags & ROLE_INDEX_MASK) == roleIndex) {
                return (T) current;
            }
        }

        return role.getNullObject();
    }

    public final <T extends AstNode> AstNodeCollection<T> getChildrenByRole(final Role<T> role) {
        return new AstNodeCollection<>(this, role);
    }

    protected final <T extends AstNode> void setChildByRole(final Role<T> role, final T newChild) {
        final T oldChild = getChildByRole(role);

        if (oldChild.isNull()) {
            addChild(newChild, role);
        }
        else {
            oldChild.replaceWith(newChild);
        }
    }

    @SuppressWarnings("unchecked")
    public final <T extends AstNode> T getParent(final Class<T> nodeType) {
        for (final AstNode node : getAncestors()) {
            if (nodeType.isInstance(node)) {
                return (T) node;
            }
        }

        return null;
    }

    public final <T extends AstNode> void addChild(final T child, final Role<T> role) {
        VerifyArgument.notNull(role, "role");

        if (child == null || child.isNull()) {
            return;
        }

        verifyNotFrozen();

        if (((AstNode)child)._parent != null) {
            throw new IllegalArgumentException("Node belongs to another tree.");
        }

        if (child.isFrozen()) {
            throw new IllegalArgumentException("Cannot add a frozen node.");
        }

        addChildUnsafe(child, role);
    }

    final void addChildUnsafe(final AstNode child, final Role<?> role) {
        child._parent = this;
        child.setRoleUnsafe(role);

        if (_firstChild == null) {
            _lastChild = _firstChild = child;
        }
        else {
            _lastChild._nextSibling = child;
            child._previousSibling = _lastChild;
            _lastChild = child;
        }
    }

    @SafeVarargs
    public final <T extends AstNode> void insertChildrenBefore(final AstNode nextSibling, final Role<T> role, final T... children) {
        VerifyArgument.notNull(children, "children");

        for (final T child : children) {
            insertChildBefore(nextSibling, child, role);
        }
    }

    public final <T extends AstNode> void insertChildBefore(final AstNode nextSibling, final T child, final Role<T> role) {
        VerifyArgument.notNull(role, "role");

        if (nextSibling == null || nextSibling.isNull()) {
            addChild(child, role);
            return;
        }

        if (child == null || child.isNull()) {
            return;
        }

        verifyNotFrozen();

        if (((AstNode)child)._parent != null) {
            throw new IllegalArgumentException("Node belongs to another tree.");
        }

        if (child.isFrozen()) {
            throw new IllegalArgumentException("Cannot add a frozen node.");
        }

        if (nextSibling._parent != this) {
            throw new IllegalArgumentException("Next sibling is not a child of this node.");
        }
    }

    @SafeVarargs
    public final <T extends AstNode> void insertChildrenAfter(final AstNode nextSibling, final Role<T> role, final T... children) {
        VerifyArgument.notNull(children, "children");

        for (final T child : children) {
            insertChildAfter(nextSibling, child, role);
        }
    }

    public final <T extends AstNode> void insertChildAfter(final AstNode previousSibling, final T child, final Role<T> role) {
        insertChildBefore(
            previousSibling == null || previousSibling.isNull() ? _firstChild : previousSibling._nextSibling,
            child,
            role
        );
    }

    final void insertChildBeforeUnsafe(final AstNode nextSibling, final AstNode child, final Role<?> role) {
        child._parent = this;
        child.setRole(role);
        child._nextSibling = nextSibling;
        child._previousSibling = nextSibling._previousSibling;

        if (nextSibling._previousSibling != null) {
            assert nextSibling._previousSibling._nextSibling == nextSibling;
            nextSibling._previousSibling._nextSibling = child;
        }
        else {
            assert _firstChild == nextSibling;
            _firstChild = child;
        }

        nextSibling._previousSibling = child;
    }

    public final void remove() {
        if (_parent == null) {
            return;
        }

        verifyNotFrozen();

        if (_previousSibling != null) {
            assert _previousSibling._nextSibling == this;
            _previousSibling._nextSibling = _nextSibling;
        }
        else {
            assert _parent._firstChild == this;
            _parent._firstChild = _nextSibling;
        }

        if (_nextSibling != null) {
            assert _nextSibling._previousSibling == this;
            _nextSibling._previousSibling = _previousSibling;
        }
        else {
            assert _parent._lastChild == this;
            _parent._lastChild = _previousSibling;
        }

        _parent = null;
        _previousSibling = null;
        _nextSibling = null;
    }

    public final void replaceWith(final AstNode newNode) {
        if (newNode == null || newNode.isNull()) {
            remove();
            return;
        }

        if (newNode == this) {
            return;
        }

        if (_parent == null) {
            throw new IllegalStateException(
                isNull() ? "Cannot replace null nodes."
                         : "Cannot replace the root node."
            );
        }

        verifyNotFrozen();

        final Role role = getRole();

        if (!role.isValid(newNode)) {
            throw new IllegalArgumentException(
                String.format(
                    "The new node '%s' is not valid for role '%s'.",
                    newNode.getClass().getName(),
                    role.toString()
                )
            );
        }

        if (newNode._parent != null) {
            if (CollectionUtilities.contains(newNode.getAncestors(), this)) {
                newNode.remove();
            }
            else {
                throw new IllegalArgumentException("Node belongs to another tree.");
            }
        }

        if (newNode.isFrozen()) {
            throw new IllegalArgumentException("Node belongs to another tree.");
        }

        newNode._parent = _parent;
        newNode.setRoleUnsafe(role);
        newNode._previousSibling = _previousSibling;
        newNode._nextSibling = _nextSibling;

        if (_parent != null) {
            if (_previousSibling != null) {
                assert _previousSibling._nextSibling == this;
                _previousSibling._nextSibling = newNode;
            }
            else {
                assert _parent._firstChild == this;
                _parent._firstChild = newNode;
            }

            if (_nextSibling != null) {
                assert _nextSibling._previousSibling == this;
                _nextSibling._previousSibling = newNode;
            }
            else {
                assert _parent._lastChild == this;
                _parent._lastChild = newNode;
            }

            _parent = null;
            _previousSibling = null;
            _nextSibling = null;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Freezable Implementation">

    @Override
    protected void freezeCore() {
        for (AstNode child = _firstChild; child != null; child = child._nextSibling) {
            child.freezeIfUnfrozen();
        }

        flags |= FROZEN_BIT;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Null Node">

    public final static AstNode NULL = new NullAstNode();

    private static final class NullAstNode extends AstNode {
        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
            return null;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.UNKNOWN;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Pattern Matching">

    public abstract NodeType getNodeType();

    @Override
    public boolean isNull() {
        return false;
    }

    @Override
    public final Role getRole() {
        return Role.get(flags & ROLE_INDEX_MASK);
    }

    public final void setRole(final Role<?> role) {
        VerifyArgument.notNull(role, "role");

        if (role.isValid(this)) {
            throw new IllegalArgumentException("This node is not valid for the specified role.");
        }

        verifyNotFrozen();
        setRoleUnsafe(role);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return false;
    }

    @Override
    public boolean matchesCollection(final Role role, final INode position, final Match match, final BacktrackingInfo backtrackingInfo) {
        return false;
    }

    public static AstNode forPattern(final Pattern pattern) {
        return new PatternPlaceholder(VerifyArgument.notNull(pattern, "pattern"));
    }

    private final static class PatternPlaceholder extends AstNode {
        final Pattern child;

        PatternPlaceholder(final Pattern child) {
            this.child = child;
        }

        @Override
        public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
            return visitor.visitPatternPlaceholder(this, child, data);
        }

        @Override
        public final NodeType getNodeType() {
            return NodeType.PATTERN;
        }

        @Override
        public boolean matches(final INode other, final Match match) {
            return child.matches(other, match);
        }

        @Override
        public boolean matchesCollection(final Role role, final INode position, final Match match, final BacktrackingInfo backtrackingInfo) {
            return child.matchesCollection(role, position, match, backtrackingInfo);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Text Attributes">

    public TextLocation getStartLocation() {
        final AstNode child = _firstChild;
        return child != null ? child.getStartLocation() : TextLocation.EMPTY;
    }

    public TextLocation getEndLocation() {
        final AstNode child = _lastChild;
        return child != null ? child.getEndLocation() : TextLocation.EMPTY;
    }

    public Region getRegion() {
        return new Region(getStartLocation(), getEndLocation());
    }

    public final boolean contains(final int line, final int column) {
        return contains(new TextLocation(line, column));
    }

    public final boolean contains(final TextLocation location) {
        if (location == null || location.isEmpty()) {
            return false;
        }

        final TextLocation startLocation = getStartLocation();
        final TextLocation endLocation = getEndLocation();

        return startLocation != null &&
               endLocation != null &&
               location.compareTo(startLocation) >= 0 &&
               location.compareTo(endLocation) < 0;
    }

    public final boolean isInside(final int line, final int column) {
        return isInside(new TextLocation(line, column));
    }

    public final boolean isInside(final TextLocation location) {
        if (location == null || location.isEmpty()) {
            return false;
        }

        final TextLocation startLocation = getStartLocation();
        final TextLocation endLocation = getEndLocation();

        return startLocation != null &&
               endLocation != null &&
               location.compareTo(startLocation) >= 0 &&
               location.compareTo(endLocation) <= 0;
    }

    public String getText() {
        return getText(null);
    }

    public String getText(final JavaFormattingOptions options) {
        if (isNull()) {
            return StringUtilities.EMPTY;
        }

        final ITextOutput output = new PlainTextOutput();
        final JavaOutputVisitor visitor = new JavaOutputVisitor(output);

        acceptVisitor(visitor, options);

        return output.toString();
    }

    String debugToString() {
        if (isNull()) {
            return "Null";
        }

        final String text = StringUtilities.trimRight(getText()).replace("\t", "").replace("\n", " ");

        return text.length() > 100 ? text.substring(0, 97) + "..." : text;
    }

    @Override
    public String toString() {
        return debugToString();
    }

    // </editor-fold>
}
