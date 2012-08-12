package com.strobel.expressions;

import com.strobel.core.Comparer;
import com.strobel.core.MutableInteger;
import com.strobel.core.delegates.Func1;

import java.util.HashSet;
import java.util.Map;

/**
 * @author Mike Strobel
 */
final class Helpers {
    static <T> T commonNode(final T first, final T second, final Func1<T, T> parent) {
        if (Comparer.equals(first, second)) {
            return first;
        }
        final HashSet<T> set = new HashSet<>();

        for (T t = first; t != null; t = parent.apply(t)) {
            set.add(t);
        }

        for (T t = second; t != null; t = parent.apply(t)) {
            if (set.contains(t)) {
                return t;
            }
        }

        return null;
    }

    static <T> void incrementCount(final T key, final Map<T, MutableInteger> dict) {
        MutableInteger count = dict.get(key);

        if (count == null) {
            dict.put(key, count = new MutableInteger());
        }

        count.increment();
    }
}
