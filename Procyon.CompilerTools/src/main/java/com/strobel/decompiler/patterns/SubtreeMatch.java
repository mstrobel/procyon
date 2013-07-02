package com.strobel.decompiler.patterns;

import com.strobel.annotations.NotNull;
import com.strobel.core.Predicate;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.strobel.core.CollectionUtilities.any;

public final class SubtreeMatch extends Pattern {
    private final INode _target;

    public SubtreeMatch(final INode target) {
        _target = VerifyArgument.notNull(target, "target");
    }

    public final INode getTarget() {
        return _target;
    }

    @Override
    public final boolean matches(final INode other, final Match match) {
        return any(
            traverseNodes(other),
            new Predicate<INode>() {
                @Override
                public boolean test(final INode n) {
                    return _target.matches(n, match);
                }
            }
        );
    }

    private static Iterable<INode> traverseNodes(final INode node) {
        return new Iterable<INode>() {
            @NotNull
            @Override
            public final Iterator<INode> iterator() {
                return new Iterator<INode>() {
                    INode position = node.getFirstChild();
                    INode next;

                    @SuppressWarnings("unchecked")
                    private INode selectNext() {
                        if (next != null) {
                            return next;
                        }

                        if (position == null) {
                            return null;
                        }

                        next = position;
                        position = position.getNextSibling();

                        return next;
                    }

                    @Override
                    public boolean hasNext() {
                        return selectNext() != null;
                    }

                    @Override
                    public INode next() {
                        final INode next = selectNext();

                        if (next == null) {
                            throw new NoSuchElementException();
                        }

                        this.next = null;
                        return next;
                    }

                    @Override
                    public void remove() {
                        throw ContractUtils.unsupported();
                    }
                };
            }
        };
    }
}
