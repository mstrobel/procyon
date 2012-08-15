package com.strobel.reflection;

/**
 * @author strobelm
 */
public class TargetInvocationException extends RuntimeException {
    private final static String DefaultMessage = "Exception has been thrown by the target of an invocation.";

    public TargetInvocationException() {
        super(DefaultMessage);
    }

    public TargetInvocationException(final String message) {
        super(message);
    }

    public TargetInvocationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TargetInvocationException(final Throwable cause) {
        super(DefaultMessage, cause);
    }

    public TargetInvocationException(final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(DefaultMessage, cause, enableSuppression, writableStackTrace);
    }

    public TargetInvocationException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
