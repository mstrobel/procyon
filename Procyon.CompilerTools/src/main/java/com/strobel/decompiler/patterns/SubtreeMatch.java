package com.strobel.decompiler.patterns;

import com.strobel.annotations.NotNull;
import com.strobel.core.Predicate;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.utilities.TreeTraversal;
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
            TreeTraversal.preOrder(other, INode.CHILD_ITERATOR),
            new Predicate<INode>() {
                @Override
                public boolean test(final INode n) {
                    return _target.matches(n, match);
                }
            }
        );
    }
}
