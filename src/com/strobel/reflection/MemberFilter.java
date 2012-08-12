package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
public interface MemberFilter {
    boolean apply(final MemberInfo m, final Object filterCriteria);
}
