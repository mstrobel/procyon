package com.strobel.core;

/**
 * @author Mike Strobel
 */
final class Error {
    private Error() {}

    public static IllegalStateException unmodifiableCollection() {
        return new IllegalStateException("Collection is read only.");
    }

    public static IllegalArgumentException sequenceHasNoElements() {
        return new IllegalArgumentException("Sequence has no elements.");
    }
}
