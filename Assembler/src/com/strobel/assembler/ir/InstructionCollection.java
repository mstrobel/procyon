package com.strobel.assembler.ir;

import com.strobel.assembler.Collection;

/**
 * @author Mike Strobel
 */
final class InstructionCollection extends Collection<Instruction> {
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
