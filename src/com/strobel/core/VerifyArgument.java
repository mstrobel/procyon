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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // GENERIC PRECONDITIONS                                                                                              //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static <T> T notNull(final T value, final String parameterName) {
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException(
            format("Argument '%s' cannot be null.", parameterName)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ARRAY AND COLLECTION PRECONDITIONS                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

            return collection;
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

    public static <T> T[] elementsOfType(final Class<?> elementType, final T[] values, final String parameterName) {
        VerifyArgument.notNull(elementType, "elementType");
        VerifyArgument.notNull(values, "values");

        for (final T value : values) {
            if (!elementType.isInstance(value)) {
                throw new IllegalArgumentException(
                    format(
                        "Argument '%s' must only contain elements of type '%s'.",
                        parameterName,
                        elementType
                    )
                );
            }
        }

        return values;
    }

    public static <T> T[] elementsOfTypeOrNull(final Class<T> elementType, final T[] values, final String parameterName) {
        VerifyArgument.notNull(elementType, "elementType");
        VerifyArgument.notNull(values, "values");

        for (final T value : values) {
            if (value != null && !elementType.isInstance(value)) {
                throw new IllegalArgumentException(
                    format(
                        "Argument '%s' must only contain elements of type '%s'.",
                        parameterName,
                        elementType
                    )
                );
            }
        }

        return values;
    }

    public static <T> void validElementRange(final int size, final int startInclusive, final int endExclusive) {
        if (startInclusive >= 0 && endExclusive <= size && endExclusive > startInclusive) {
            return;
        }
        throw new IllegalArgumentException("The specified element range is not valid.");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // STRING PRECONDITIONS                                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static String notNullOrEmpty(final String value, final String parameterName) {
        if (!StringUtilities.isNullOrEmpty(value)) {
            return value;
        }
        throw new IllegalArgumentException(
            format("Argument '%s' must be a non-null, non-empty string.", parameterName)
        );
    }

    public static String notNullOrWhitespace(final String value, final String parameterName) {
        if (!StringUtilities.isNullOrWhitespace(value)) {
            return value;
        }
        throw new IllegalArgumentException(
            format("Argument '%s' must be a non-null, non-empty string.", parameterName)
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // NUMERIC PRECONDITIONS                                                                                              //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static int isNonZero(final int value, final String parameterName) {
        if (value != 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be non-zero.", parameterName));
    }

    public static int isPositive(final int value, final String parameterName) {
        if (value > 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be positive.", parameterName));
    }

    public static int isNonNegative(final int value, final String parameterName) {
        if (value >= 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be non-negative.", parameterName));
    }

    public static int isNegative(final int value, final String parameterName) {
        if (value < 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be negative.", parameterName));
    }

    public static int inRange(final int minInclusive, final int maxInclusive, final int value, final String parameterName) {
        if (maxInclusive < minInclusive) {
            throw new IllegalArgumentException("The specified maximum value is less than the specified minimum value.");
        }

        if (value >= minInclusive && value <= maxInclusive) {
            return value;
        }

        throw new IllegalArgumentException(
            format(
                "Argument '%s' must be in the range [%s, %s].",
                parameterName,
                minInclusive,
                maxInclusive
            )
        );
    }

    public static double isNonZero(final double value, final String parameterName) {
        if (value != 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be non-zero.", parameterName));
    }

    public static double isPositive(final double value, final String parameterName) {
        if (value > 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be positive.", parameterName));
    }

    public static double isNonNegative(final double value, final String parameterName) {
        if (value >= 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be non-negative.", parameterName));
    }

    public static double isNegative(final double value, final String parameterName) {
        if (value < 0) {
            return value;
        }

        throw new IllegalArgumentException(format("Argument '%s' must be negative.", parameterName));
    }

    public static double inRange(
        final double minInclusive,
        final double maxInclusive,
        final double value,
        final String parameterName) {

        if (maxInclusive < minInclusive) {
            throw new IllegalArgumentException("The specified maximum value is less than the specified minimum value.");
        }

        if (value >= minInclusive && value <= maxInclusive) {
            return value;
        }

        throw new IllegalArgumentException(
            format(
                "Argument '%s' must be in the range [%s, %s].",
                parameterName,
                minInclusive,
                maxInclusive
            )
        );
    }
}
