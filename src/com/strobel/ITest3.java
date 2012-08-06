package com.strobel;

import java.io.Serializable;

public interface ITest3<T extends Comparable<String> & Serializable, T2 extends T> {
    T2 test(final T t);
}
