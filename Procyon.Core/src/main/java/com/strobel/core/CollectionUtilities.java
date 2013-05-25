/*
 * CollectionUtilities.java
 *
 * Copyright (c) 2012 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.core;

import com.strobel.util.ContractUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Strobel
 */
public final class CollectionUtilities {
    public static <T> List<T> toList(final Iterable<T> collection) {
        final ArrayList<T> list = new ArrayList<>();

        for (final T item : collection) {
            list.add(item);
        }

        return list;
    }

    public static <T> T first(final Iterable<T> collection) {
        final Iterator<T> it = VerifyArgument.notNull(collection, "collection").iterator();
        if (it.hasNext()) {
            return it.next();
        }
        throw Error.sequenceHasNoElements();
    }

    public static <T> T getOrDefault(final Iterable<T> collection, final int index) {
        int i = 0;

        for (final T item : collection) {
            if (i++ == index) {
                return item;
            }
        }

        return null;
    }

    public static <T> T getOrDefault(final List<T> collection, final int index) {
        if (index >= VerifyArgument.notNull(collection, "collection").size() || index < 0) {
            return null;
        }
        return collection.get(index);
    }

    public static <T> T firstOrDefault(final Iterable<T> collection) {
        final Iterator<T> it = VerifyArgument.notNull(collection, "collection").iterator();
        return it.hasNext() ? it.next() : null;
    }

    public static <T> T firstOrDefault(final Iterable<T> collection, final Predicate<T> predicate) {
        VerifyArgument.notNull(predicate, "predicate");

        for (final T item : VerifyArgument.notNull(collection, "collection")) {
            if (predicate.test(item)) {
                return item;
            }
        }

        return null;
    }

    public static <T> T lastOrDefault(final Iterable<T> collection) {
        VerifyArgument.notNull(collection, "collection");

        if (collection instanceof List<?>) {
            final List<T> list = (List<T>) collection;
            return list.isEmpty() ? null : list.get(list.size() - 1);
        }

        T last = null;

        for (final T item : collection) {
            last = item;
        }

        return last;
    }

    public static <T> T lastOrDefault(final Iterable<T> collection, final Predicate<T> predicate) {
        VerifyArgument.notNull(collection, "collection");
        VerifyArgument.notNull(predicate, "predicate");

        T lastMatch = null;

        for (final T item : VerifyArgument.notNull(collection, "collection")) {
            if (predicate.test(item)) {
                lastMatch = item;
            }
        }

        return lastMatch;
    }

    public static <T> boolean contains(final Iterable<? super T> collection, final T node) {
        if (collection instanceof Collection<?>) {
            return ((Collection<?>) collection).contains(node);
        }

        for (final Object item : collection) {
            if (Comparer.equals(item, node)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean any(final Iterable<T> collection) {
        if (collection instanceof Collection<?>) {
            return !((Collection) collection).isEmpty();
        }
        return collection != null && collection.iterator().hasNext();
    }

    public static <T> Iterable<T> skip(final Iterable<T> collection, final int count) {
        return new SkipIterator<>(collection, count);
    }
    
    public static <T> Iterable<T> skipWhile(final Iterable<T> collection, final Predicate<? super T> filter) {
        return new SkipIterator<>(collection, filter);
    }

    public static <T> Iterable<T> take(final Iterable<T> collection, final int count) {
        return new TakeIterator<>(collection, count);
    }
    
    public static <T> Iterable<T> takeWhile(final Iterable<T> collection, final Predicate<? super T> filter) {
        return new TakeIterator<>(collection, filter);
    }

    public static <T> boolean any(final Iterable<T> collection, final Predicate<? super T> predicate) {
        VerifyArgument.notNull(collection, "collection");
        VerifyArgument.notNull(predicate, "predicate");

        for (final T t : collection) {
            if (predicate.test(t)) {
                return true;
            }
        }

        return false;
    }

    public static <T> boolean all(final Iterable<T> collection, final Predicate<? super T> predicate) {
        VerifyArgument.notNull(collection, "collection");
        VerifyArgument.notNull(predicate, "predicate");

        for (final T t : collection) {
            if (!predicate.test(t)) {
                return false;
            }
        }

        return true;
    }

    public static int hashCode(final List<?> sequence) {
        VerifyArgument.notNull(sequence, "sequence");

        int hashCode = HashUtilities.NullHashCode;

        for (int i = 0; i < sequence.size(); i++) {
            final Object item = sequence.get(i);

            final int itemHashCode;

            if (item instanceof Iterable<?>) {
                itemHashCode = hashCode((Iterable<?>) item);
            }
            else {
                itemHashCode = item != null ? HashUtilities.hashCode(item)
                                            : HashUtilities.NullHashCode;
            }

            hashCode = HashUtilities.combineHashCodes(
                hashCode,
                itemHashCode
            );
        }

        return hashCode;
    }

    public static int hashCode(final Iterable<?> sequence) {
        if (sequence instanceof List<?>) {
            return hashCode((List<?>) sequence);
        }

        VerifyArgument.notNull(sequence, "sequence");

        int hashCode = HashUtilities.NullHashCode;

        for (final Object item : sequence) {
            final int itemHashCode;

            if (item instanceof Iterable<?>) {
                itemHashCode = hashCode((Iterable<?>) item);
            }
            else {
                itemHashCode = item != null ? HashUtilities.hashCode(item)
                                            : HashUtilities.NullHashCode;
            }

            hashCode = HashUtilities.combineHashCodes(
                hashCode,
                itemHashCode
            );
        }

        return hashCode;
    }

    public static <T> boolean sequenceEquals(final List<? extends T> first, final List<? extends T> second) {
        VerifyArgument.notNull(first, "first");
        VerifyArgument.notNull(second, "second");

        if (first == second) {
            return true;
        }

        if (first.size() != second.size()) {
            return false;
        }

        if (first.isEmpty()) {
            return true;
        }

        for (int i = 0, n = first.size(); i < n; i++) {
            if (!Comparer.equals(first.get(i), second.get(i))) {
                return false;
            }
        }

        return true;
    }

    public static <T> boolean sequenceEquals(final Iterable<? extends T> first, final Iterable<? extends T> second) {
        VerifyArgument.notNull(first, "first");
        VerifyArgument.notNull(second, "second");

        if (first == second) {
            return true;
        }

        if (first instanceof List<?> && second instanceof List<?>) {
            return sequenceDeepEquals((List<?>) first, (List<?>) second);
        }

        final Iterator<? extends T> firstIterator = first.iterator();
        final Iterator<? extends T> secondIterator = second.iterator();

        while (firstIterator.hasNext()) {
            if (!secondIterator.hasNext()) {
                return false;
            }

            if (!Comparer.equals(firstIterator.next(), secondIterator.next())) {
                return false;
            }
        }

        return !secondIterator.hasNext();
    }

    public static <T> boolean sequenceDeepEquals(final List<? extends T> first, final List<? extends T> second) {
        VerifyArgument.notNull(first, "first");
        VerifyArgument.notNull(second, "second");

        if (first == second) {
            return true;
        }

        if (first.size() != second.size()) {
            return false;
        }

        if (first.isEmpty()) {
            return true;
        }

        for (int i = 0, n = first.size(); i < n; i++) {
            if (!sequenceDeepEqualsCore(first.get(i), second.get(i))) {
                return false;
            }
        }

        return true;
    }

    public static <T> boolean sequenceDeepEquals(final Iterable<? extends T> first, final Iterable<? extends T> second) {
        VerifyArgument.notNull(first, "first");
        VerifyArgument.notNull(second, "second");

        if (first == second) {
            return true;
        }

        if (first instanceof List<?> && second instanceof List<?>) {
            return sequenceDeepEquals((List<?>) first, (List<?>) second);
        }

        final Iterator<? extends T> firstIterator = first.iterator();
        final Iterator<? extends T> secondIterator = second.iterator();

        while (firstIterator.hasNext()) {
            if (!secondIterator.hasNext()) {
                return false;
            }

            if (!sequenceDeepEqualsCore(firstIterator.next(), secondIterator.next())) {
                return false;
            }
        }

        return !secondIterator.hasNext();
    }

    private static boolean sequenceDeepEqualsCore(final Object first, final Object second) {
        if (first instanceof List<?>) {
            return second instanceof List<?> &&
                   sequenceDeepEquals((List<?>) first, (List<?>) second);
        }
        return Comparer.deepEquals(first, second);
    }

    private abstract static class AbstractIterator<T> implements Iterable<T>, Iterator<T> {
        final static int STATE_UNINITIALIZED = 0;
        final static int STATE_NEED_NEXT = 1;
        final static int STATE_HAS_NEXT = 2;
        final static int STATE_FINISHED = 3;

        long threadId;
        int state;
        T next;

        AbstractIterator() {
            super();
            threadId = Thread.currentThread().getId();
        }

        protected abstract AbstractIterator<T> clone();

        @Override
        public abstract boolean hasNext();

        @Override
        public T next() {
            if (!hasNext()) {
                throw new IllegalStateException();
            }
            state = STATE_NEED_NEXT;
            return next;
        }

        @Override
        public Iterator<T> iterator() {
            if (threadId == Thread.currentThread().getId() && state == STATE_UNINITIALIZED) {
                state = STATE_NEED_NEXT;
                return this;
            }
            final AbstractIterator<T> duplicate = clone();
            duplicate.state = STATE_NEED_NEXT;
            return duplicate;
        }

        @Override
        public final void remove() {
            throw ContractUtils.unsupported();
        }
    }

    private final static class SkipIterator<T> extends AbstractIterator<T> {
        private final static int STATE_NEED_SKIP = 4;

        final Iterable<T> source;
        final int skipCount;
        final Predicate<? super T> skipFilter;

        int skipsRemaining;
        Iterator<T> iterator;

        SkipIterator(final Iterable<T> source, final int skipCount) {
            this.source = VerifyArgument.notNull(source, "source");
            this.skipCount = skipCount;
            this.skipFilter = null;
            this.skipsRemaining = skipCount;
        }

        SkipIterator(final Iterable<T> source, final Predicate<? super T> skipFilter) {
            this.source = VerifyArgument.notNull(source, "source");
            this.skipCount = 0;
            this.skipFilter = VerifyArgument.notNull(skipFilter, "skipFilter");
        }

        @Override
        protected SkipIterator<T> clone() {
            if (skipFilter != null) {
                return new SkipIterator<>(source, skipFilter);
            }
            return new SkipIterator<>(source, skipCount);
        }

        @Override
        public boolean hasNext() {
            switch (state) {
                case STATE_NEED_SKIP:
                    iterator = source.iterator();
                    if (skipFilter != null) {
                        while (iterator.hasNext()) {
                            final T current = iterator.next();
                            if (!skipFilter.test(current)) {
                                state = STATE_HAS_NEXT;
                                next = current;
                                return true;
                            }
                        }
                    }
                    else {
                        while (iterator.hasNext() && skipsRemaining > 0) {
                            iterator.next();
                            --skipsRemaining;
                        }
                    }
                    state = STATE_NEED_NEXT;
                    // goto case STATE_NEED_NEXT

                case STATE_NEED_NEXT:
                    if (iterator.hasNext()) {
                        state = STATE_HAS_NEXT;
                        next = iterator.next();
                        return true;
                    }
                    state = STATE_FINISHED;
                    // goto case STATE_FINISHED

                case STATE_FINISHED:
                    return false;

                case STATE_HAS_NEXT:
                    return true;
            }

            return false;
        }

        @Override
        public Iterator<T> iterator() {
            if (threadId == Thread.currentThread().getId() && state == STATE_UNINITIALIZED) {
                state = STATE_NEED_SKIP;
                return this;
            }
            final SkipIterator<T> duplicate = clone();
            duplicate.state = STATE_NEED_SKIP;
            return duplicate;
        }
    }

    private final static class TakeIterator<T> extends AbstractIterator<T> {
        final Iterable<T> source;
        final int takeCount;
        final Predicate<? super T> takeFilter;

        Iterator<T> iterator;
        int takesRemaining;

        TakeIterator(final Iterable<T> source, final int takeCount) {
            this.source = VerifyArgument.notNull(source, "source");
            this.takeCount = takeCount;
            this.takeFilter = null;
            this.takesRemaining = takeCount;
        }

        TakeIterator(final Iterable<T> source, final Predicate<? super T> takeFilter) {
            this.source = VerifyArgument.notNull(source, "source");
            this.takeCount = Integer.MAX_VALUE;
            this.takeFilter = VerifyArgument.notNull(takeFilter, "takeFilter");
            this.takesRemaining = Integer.MAX_VALUE;
        }

        TakeIterator(final Iterable<T> source, final int takeCount, final Predicate<? super T> takeFilter) {
            this.source = VerifyArgument.notNull(source, "source");
            this.takeCount = takeCount;
            this.takeFilter = takeFilter;
            this.takesRemaining = takeCount;
        }

        @Override
        protected TakeIterator<T> clone() {
            return new TakeIterator<>(source, takeCount, takeFilter);
        }

        @Override
        public boolean hasNext() {
            switch (state) {
                case STATE_NEED_NEXT:
                    if (takesRemaining-- > 0) {
                        if (iterator == null) {
                            iterator = source.iterator();
                        }
                        if (iterator.hasNext()) {
                            final T current = iterator.next();
                            if (takeFilter == null || takeFilter.test(current)) {
                                state = STATE_HAS_NEXT;
                                next = current;
                                return true;
                            }
                        }
                    }
                    state = STATE_FINISHED;
                    // goto case STATE_FINISHED

                case STATE_FINISHED:
                    return false;

                case STATE_HAS_NEXT:
                    return true;
            }

            return false;
        }
    }
}
