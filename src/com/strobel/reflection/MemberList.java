package com.strobel.reflection;

import com.strobel.core.ReadOnlyList;
import com.strobel.core.VerifyArgument;

import java.util.List;

/**
 * @author Mike Strobel
 */
public class MemberList<T extends MemberInfo> extends ReadOnlyList<T> {
    @SuppressWarnings("unchecked")
    private final static MemberList<?> EMPTY = new MemberList(MemberInfo.class);
    private final Class<T> _memberType;

    @SuppressWarnings("unchecked")
    public static <T extends MemberInfo> MemberList<T> empty() {
        return (MemberList<T>) EMPTY;
    }

    @SafeVarargs
    MemberList(final Class<T> memberType, final T... members) {
        super(members);
        _memberType = VerifyArgument.notNull(memberType, "memberType");
    }

    MemberList(final Class<T> memberType, final List<? extends T> members) {
        super(memberType, VerifyArgument.noNullElements(members, "members"));
        _memberType = memberType;
    }

    MemberList(final Class<T> memberType, final T[] members, final int offset, final int length) {
        super(members, offset, length);
        _memberType = VerifyArgument.notNull(memberType, "memberType");
    }

    Class<T> getMemberType() {
        return _memberType;
    }
}
