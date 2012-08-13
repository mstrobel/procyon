package com.strobel.compilerservices;

/**
 * @author Mike Strobel
 */
public final class CallerResolver extends SecurityManager {
    private static final CallerResolver CALLER_RESOLVER = new CallerResolver();
    private static final int CALL_CONTEXT_OFFSET = 3; // may need to change if this class is redesigned

    protected Class[] getClassContext() {
        return super.getClassContext();
    }

    /*
    * Indexes into the current method call context with a given
    * offset.
    */
    public static Class getCallerClass(final int callerOffset) {
        return CALLER_RESOLVER.getClassContext()[CALL_CONTEXT_OFFSET + callerOffset];
    }

    public static int getContextSize(final int callerOffset) {
        return CALLER_RESOLVER.getClassContext().length - callerOffset;
    }

    public static int getContextSize() {
        return getContextSize(CALL_CONTEXT_OFFSET);
    }
}
