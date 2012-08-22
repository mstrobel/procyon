package com.strobel.core;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("unchecked")
public final class Selectors {
    private final static Selector<?,?> IdentitySelector = new Selector<Object, Object>() {
        @Override
        public Object select(final Object o) {
            return o;
        }
    };
    
    public static <T> Selector<T, T> identity() {
        return (Selector<T, T>) IdentitySelector;
    }
}
