/*
 * ExceptionBlock.java
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

package com.strobel.assembler.ir;

import com.strobel.core.VerifyArgument;

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

    public final boolean isWithin(final ExceptionBlock block) {
        return block != null &&
               block.getFirstInstruction().getOffset() >= getFirstInstruction().getOffset() &&
               block.getLastInstruction().getOffset() < getLastInstruction().getEndOffset();
    }
}
