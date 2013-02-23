/*
 * InstructionCollection.java
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

import com.strobel.assembler.Collection;

import java.util.Collections;
import java.util.Comparator;

/**
 * @author Mike Strobel
 */
public final class InstructionCollection extends Collection<Instruction> {
    public Instruction atOffset(final int offset) {
        final int index = Collections.binarySearch(
            this,
            new Instruction(offset, OpCode.NOP),
            new Comparator<Instruction>() {
                @Override
                public int compare(final Instruction o1, final Instruction o2) {
                    return Integer.compare(o1.getOffset(), o2.getOffset());
                }
            }
        );

        if (index < 0) {
            throw new IndexOutOfBoundsException("No instruction found at offset " + offset + '.');
        }

        return get(index);
    }

    public Instruction tryGetAtOffset(final int offset) {
        final int index = Collections.binarySearch(
            this,
            new Instruction(offset, OpCode.NOP),
            new Comparator<Instruction>() {
                @Override
                public int compare(final Instruction o1, final Instruction o2) {
                    return Integer.compare(o1.getOffset(), o2.getOffset());
                }
            }
        );

        if (index < 0) {
            return null;
        }

        return get(index);
    }

    @Override
    protected void afterAdd(final int index, final Instruction item, final boolean appended) {
        final Instruction next = index < size() - 1 ? get(index + 1) : null;
        final Instruction previous = index > 0 ? get(index - 1) : null;

        if (previous != null) {
            previous.setNext(item);
        }

        if (next != null) {
            next.setPrevious(item);
        }

        item.setPrevious(previous);
        item.setNext(next);
    }

    @Override
    protected void beforeSet(final int index, final Instruction item) {
        final Instruction current = get(index);

        item.setPrevious(current.getPrevious());
        item.setNext(current.getNext());

        current.setPrevious(null);
        current.setNext(null);
    }

    @Override
    protected void afterRemove(final int index, final Instruction item) {
        final Instruction current = item.getNext();
        final Instruction previous = item.getPrevious();

        if (previous != null) {
            previous.setNext(current);
        }

        if (current != null) {
            current.setPrevious(previous);
        }

        item.setPrevious(null);
        item.setNext(null);
    }

    @Override
    protected void beforeClear() {
        for (int i = 0; i < size(); i++) {
            get(i).setNext(null);
            get(i).setPrevious(null);
        }
    }
}
