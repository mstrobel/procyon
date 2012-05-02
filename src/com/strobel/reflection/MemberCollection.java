package com.strobel.reflection;

import com.strobel.core.ReadOnlyCollection;
import com.strobel.core.VerifyArgument;

import java.util.HashMap;

/**
 * @author Mike Strobel
 */
public class MemberCollection<T extends MemberInfo> extends ReadOnlyCollection<T> {

    private final HashMap<String, T> _nameLookup;

    @SuppressWarnings("unchecked")
    private final static MemberCollection<?> EMPTY = new MemberCollection(MemberInfo.class);
    private final Class<T> _memberType;

    @SuppressWarnings("unchecked")
    public static <T extends MemberInfo> MemberCollection<T> empty() {
        return (MemberCollection<T>) EMPTY;
    }

    @SafeVarargs
    MemberCollection(final Class<T> memberType, final T... members) {
        super(members);

        _memberType = VerifyArgument.notNull(memberType, "memberType");
        _nameLookup = new HashMap<>();

        for (final T member : members) {
            _nameLookup.put(member.getName(), member);
        }
    }

    MemberCollection(final Class<T> memberType, final T[] members, final int offset, final int length) {
        super(members, offset, length);

        _memberType = VerifyArgument.notNull(memberType, "memberType");
        _nameLookup = new HashMap<>();

        for (final T member : members) {
            _nameLookup.put(member.getName(), member);
        }
    }

    public final T get(final String name) {
        return _nameLookup.get(name);
    }

    Class<T> getMemberType() {
        return _memberType;
    }
}
