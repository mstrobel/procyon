package com.strobel.reflection.emit;

/**
 * @author Mike Strobel
 */
final class CallerResolver extends SecurityManager {
    private static final CallerResolver CALLER_RESOLVER = new CallerResolver();
    private static final int CALL_CONTEXT_OFFSET = 3; // may need to change if this class is redesigned

    protected Class[] getClassContext() {
        return super.getClassContext();
    }

    /*
    * Indexes into the current method call context with a given
    * offset.
    */
    static Class getCallerClass(final int callerOffset) {
        return CALLER_RESOLVER.getClassContext()[CALL_CONTEXT_OFFSET + callerOffset];
    }

    static int getContextSize(final int callerOffset) {
        return CALLER_RESOLVER.getClassContext().length - callerOffset;
    }

    static int getContextSize() {
        return getContextSize(CALL_CONTEXT_OFFSET);
    }
}
