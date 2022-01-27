package com.strobel.reflection.emit;

import com.strobel.annotations.NotNull;
import com.strobel.core.VerifyArgument;

import java.lang.invoke.MethodHandles;
import java.util.ArrayDeque;

public final class LookupScope implements AutoCloseable {
    private final static LookupScope PUBLIC = create(MethodHandles.publicLookup());

    private final static ThreadLocal<ArrayDeque<LookupScope>> THREAD_SCOPE = new ThreadLocal<ArrayDeque<LookupScope>>() {
        @Override
        protected ArrayDeque<LookupScope> initialValue() {
            final ArrayDeque<LookupScope> stack = new ArrayDeque<>();
            stack.push(PUBLIC);
            return stack;
        }
    };

    private final MethodHandles.Lookup _lookup;

    private LookupScope(final @NotNull MethodHandles.Lookup lookup) {
        _lookup = lookup;
        VerifyArgument.notNull(lookup, "lookup");
    }

    public static LookupScope publicScope() {
        return PUBLIC;
    }

    public static LookupScope create(final @NotNull MethodHandles.Lookup lookup) {
        return new LookupScope(lookup);
    }

    public static LookupScope push(final @NotNull MethodHandles.Lookup lookup) {
        return new LookupScope(lookup).push();
    }

    public MethodHandles.Lookup lookup() {
        return _lookup;
    }

    public LookupScope push() {
        final ArrayDeque<LookupScope> stack = THREAD_SCOPE.get();
        stack.addFirst(this);
        return this;
    }

    public static LookupScope current() {
        final ArrayDeque<LookupScope> stack = THREAD_SCOPE.get();
        ensureScope0(stack);
        return stack.peekFirst();
    }

    @Override
    public void close() {
        pop();
    }

    public void pop() {
        final ArrayDeque<LookupScope> stack = THREAD_SCOPE.get();
        stack.removeFirst();
        ensureScope0(stack);
    }

    private static void ensureScope0(final ArrayDeque<LookupScope> stack) {
        if (stack.isEmpty()) {
            stack.addFirst(publicScope());
        }
    }
}
