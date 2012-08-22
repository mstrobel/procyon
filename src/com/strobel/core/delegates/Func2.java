package com.strobel.core.delegates;

/**
 * @author Mike Strobel
 */
public interface Func2<T1, T2, R> {
    R apply(final T1 t1, final T2 t2);
}
