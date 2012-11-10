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
