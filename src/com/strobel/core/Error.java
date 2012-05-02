package com.strobel.core;

/**
 * @author Mike Strobel
 */
final class Error {
    private Error() {}

    public static RuntimeException unmodifiableCollection() {
        return new RuntimeException("Collection is read only.");
    }
}
