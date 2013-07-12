package com.strobel.assembler.metadata;

import com.strobel.core.Predicate;
import com.strobel.core.Predicates;
import com.strobel.core.StringUtilities;
import com.strobel.util.ContractUtils;

public final class MetadataFilters {
    private MetadataFilters() {
        throw ContractUtils.unreachable();
    }

    public static <T extends MemberReference> Predicate<T> matchName(final String name) {
        return new Predicate<T>() {
            @Override
            public final boolean test(final T t) {
                return StringUtilities.equals(t.getName(), name);
            }
        };
    }

    public static <T extends MemberReference> Predicate<T> matchDescriptor(final String descriptor) {
        return new Predicate<T>() {
            @Override
            public final boolean test(final T t) {
                return StringUtilities.equals(t.getErasedSignature(), descriptor);
            }
        };
    }

    public static <T extends MemberReference> Predicate<T> matchSignature(final String signature) {
        return new Predicate<T>() {
            @Override
            public final boolean test(final T t) {
                return StringUtilities.equals(t.getSignature(), signature);
            }
        };
    }

    public static <T extends MemberReference> Predicate<T> matchNameAndDescriptor(final String name, final String descriptor) {
        return Predicates.and(MetadataFilters.<T>matchName(name), matchDescriptor(descriptor));
    }

    public static <T extends MemberReference> Predicate<T> matchNameAndSignature(final String name, final String signature) {
        return Predicates.and(MetadataFilters.<T>matchName(name), matchSignature(signature));
    }
}
