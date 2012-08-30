package com.strobel.core;

import static java.lang.String.format;

/**
 * @author Mike Strobel
 */
final class Error {
    private Error() {}

    static IllegalStateException unmodifiableCollection() {
        return new IllegalStateException("Collection is read only.");
    }

    static IllegalArgumentException sequenceHasNoElements() {
        return new IllegalArgumentException("Sequence has no elements.");
    }

    static IllegalArgumentException couldNotConvertFromNull() {
        return new IllegalArgumentException("Could not convert from 'null'.");
    }

    static IllegalArgumentException couldNotConvertFromType(final Class<?> sourceType) {
        return new IllegalArgumentException(
            format("Could not convert from type '%s'.", sourceType.getName())
        );
    }

    static IllegalArgumentException couldNotConvertNullValue(final Class<?> targetType) {
        return new IllegalArgumentException(
            format(
                "Could not convert 'null' to an instance of '%s'.",
                targetType.getName()
            )
        );
    }

    static IllegalArgumentException couldNotConvertValue(final Class<?> sourceType, final Class<?> targetType) {
        return new IllegalArgumentException(
            format(
                "Could not convert a value of type '%s' to an instance of '%s'.",
                sourceType.getName(),
                targetType.getName()
            )
        );
    }
}
