package com.strobel.core;

import com.strobel.core.delegates.Func1;

import java.util.Iterator;

/**
 * @author Mike Strobel
 */
public final class CollectionUtilities {
    public static <T> T first(final Iterable<T> collection) {
        final Iterator<T> it = VerifyArgument.notNull(collection, "collection").iterator();
        if (it.hasNext()) {
            return it.next();
        }
        throw Error.sequenceHasNoElements();
    }

    public static <T> T firstOrDefault(final Iterable<T> collection) {
        final Iterator<T> it = VerifyArgument.notNull(collection, "collection").iterator();
        return it.hasNext() ? it.next() : null;
    }

    public static <T> boolean all(final Iterable<T> collection, final Func1<? super T, Boolean> predicate) {
        VerifyArgument.notNull(collection, "collection");
        VerifyArgument.notNull(predicate, "predicate");

        for (final T t : collection) {
            if (!predicate.apply(t)) {
                return false;
            }
        }

        return true;
    }
}
