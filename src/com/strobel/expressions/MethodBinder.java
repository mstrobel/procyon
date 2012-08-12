package com.strobel.expressions;

import java.lang.reflect.Method;

interface IMethodFilter<T> {
    public static final IMethodFilter<String> FilterNameIgnoreCase = new IMethodFilter<String>() {
        @Override
        public boolean accept(final Method m, final String name) {
            return m.getName().equalsIgnoreCase(name);
        }
    };
    boolean accept(final Method m, final T filterCriteria);
}