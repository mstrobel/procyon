package com.strobel.reflection;

import static java.lang.String.format;

/**
 * @author Mike Strobel
 */
public class MemberResolutionException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Failed to resolve member.";
    private static final String DEFAULT_MESSAGE_FORMAT = "Failed to resolve member '%s'.";

    public MemberResolutionException() {
        this(DEFAULT_MESSAGE);
    }

    public MemberResolutionException(final MemberInfo member, final Throwable cause) {
        super(format(DEFAULT_MESSAGE_FORMAT, member), cause);
    }

    public MemberResolutionException(final MemberInfo member) {
        super(format(DEFAULT_MESSAGE_FORMAT, member));
    }

    public MemberResolutionException(final String message) {
        super(message);
    }

    public MemberResolutionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public MemberResolutionException(final Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

    public MemberResolutionException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
