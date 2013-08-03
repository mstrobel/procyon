/*
 * ExceptionBlock.java
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

package com.strobel.assembler.ir;

import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ast.Range;

public final class ExceptionBlock {
    private final Instruction _firstInstruction;
    private final Instruction _lastInstruction;

    public ExceptionBlock(final Instruction firstInstruction, final Instruction lastInstruction) {
        _firstInstruction = VerifyArgument.notNull(firstInstruction, "firstInstruction");
        _lastInstruction = lastInstruction;
    }

    public final Instruction getFirstInstruction() {
        return _firstInstruction;
    }

    public final Instruction getLastInstruction() {
        return _lastInstruction;
    }

    public final boolean contains(final ExceptionBlock block) {
        return block != null &&
               block.getFirstInstruction().getOffset() >= getFirstInstruction().getOffset() &&
               block.getLastInstruction().getOffset() <= getLastInstruction().getOffset();
    }

    public final boolean contains(final Range range) {
        return range != null &&
               range.getStart() >= getFirstInstruction().getOffset() &&
               range.getEnd() <= getLastInstruction().getEndOffset();
    }

    public final boolean intersects(final ExceptionBlock block) {
        return block != null &&
               block.getFirstInstruction().getOffset() <= getLastInstruction().getOffset() &&
               block.getLastInstruction().getOffset() >= getFirstInstruction().getOffset();
    }

    public final boolean intersects(final Range range) {
        return range != null &&
               range.getStart() <= getLastInstruction().getOffset() &&
               range.getEnd() >= getFirstInstruction().getOffset();
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof ExceptionBlock) {
            final ExceptionBlock block = (ExceptionBlock) o;

            return Comparer.equals(_firstInstruction, block._firstInstruction) &&
                   Comparer.equals(_lastInstruction, block._lastInstruction);
        }

        return false;
    }

    @Override
    public final int hashCode() {
        int result = _firstInstruction != null ? _firstInstruction.hashCode() : 0;
        result = 31 * result + (_lastInstruction != null ? _lastInstruction.hashCode() : 0);
        return result;
    }
}
