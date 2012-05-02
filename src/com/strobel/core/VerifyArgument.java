package com.strobel.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import static java.lang.String.format;

/**
 * @author Mike Strobel
 */
public final class VerifyArgument {
    private VerifyArgument() {}

    public static <T> T notNull(final T value, final String parameterName) {
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException(
            format("Argument '%s' cannot be null.", parameterName)
        );
    }

    public static String notNullOrEmpty(final String value, final String parameterName) {
        if (!StringEx.isNullOrEmpty(value)) {
            return value;
        }
        throw new IllegalArgumentException(
            format("Argument '%s' must be a non-null, non-empty string.", parameterName)
        );
    }

    public static String notNullOrWhitespace(final String value, final String parameterName) {
        if (!StringEx.isNullOrWhitespace(value)) {
            return value;
        }
        throw new IllegalArgumentException(
            format("Argument '%s' must be a non-null, non-empty string.", parameterName)
        );
    }

    public static <T> T[] notEmpty(final T[] array, final String parameterName) {
        notNull(array, parameterName);

        if (array.length == 0) {
            throw new IllegalArgumentException(
                format("Argument '%s' must be a non-empty collection.", parameterName)
            );
        }

        return array;
    }

    public static <T extends Iterable<?>> T notEmpty(final T collection, final String parameterName) {
        notNull(collection, parameterName);

        if (collection instanceof Collection<?>) {
            if (!((Collection<?>)collection).isEmpty()) {
                return collection;
            }
        }
        else {
            final Iterator<?> iterator = collection.iterator();
            if (iterator.hasNext()) {
                return collection;
            }
        }

        throw new IllegalArgumentException(
            format("Argument '%s' must be a non-empty collection.", parameterName)
        );
    }

    public static <T> T[] noNullElements(final T[] array, final String parameterName) {
        notNull(array, parameterName);

        for (final T item : array) {
            if (item == null) {
                throw new IllegalArgumentException(
                    format("Argument '%s' must not have any null elements.", parameterName)
                );
            }
        }

        return array;
    }

    public static <T extends Iterable<?>> T noNullElements(final T collection, final String parameterName) {
        notNull(collection, parameterName);

        if (collection instanceof List && collection instanceof RandomAccess) {
            final List<?> list = (List<?>)collection;
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, n = list.size(); i < n; i++) {
                if (list.get(i) == null) {
                    throw new IllegalArgumentException(
                        format("Argument '%s' must not have any null elements.", parameterName)
                    );
                }
            }
        }

        for (final Object item : collection) {
            if (item == null) {
                throw new IllegalArgumentException(
                    format("Argument '%s' must not have any null elements.", parameterName)
                );
            }
        }

        return collection;
    }
}
