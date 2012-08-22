package com.strobel.core;

/**
 * @author Mike Strobel
 */
public interface Aggregator<TSource, TAccumulate, TResult> {
    TResult aggregate(
        final TSource source,
        final TAccumulate seed,
        final Accumulator<TSource, TAccumulate> accumulator,
        final Selector<TAccumulate, TResult> resultSelector);
}
