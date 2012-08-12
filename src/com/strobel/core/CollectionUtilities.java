package com.strobel.core;

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
}
