package com.strobel.core;

/**
* @author Mike Strobel
*/
public interface Accumulator<TSource, TAccumulate> {
    TAccumulate accumulate(final TAccumulate accumulate, final TSource item);
}
