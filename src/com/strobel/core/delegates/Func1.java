package com.strobel.core.delegates;

/**
 * @author Mike Strobel
 */
public interface Func1<T, R> {
    R apply(final T t);
}
