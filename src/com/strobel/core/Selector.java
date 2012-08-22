package com.strobel.core;

/**
 * @author Mike Strobel
 */
public interface Selector<TSource, TResult> {
    TResult select(final TSource source);
}
