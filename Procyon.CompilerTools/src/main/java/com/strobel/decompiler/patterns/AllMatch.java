package com.strobel.decompiler.patterns;

import com.strobel.core.VerifyArgument;

public final class AllMatch extends Pattern {
    private final INode[] _patterns;

    public AllMatch(final INode... patterns) {
        _patterns = VerifyArgument.noNullElementsAndNotEmpty(patterns, "patterns");
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        for (final INode pattern : _patterns) {
            if (pattern.matches(other, match)) {
                continue;
            }
            return false;
        }
        return true;
    }
}
