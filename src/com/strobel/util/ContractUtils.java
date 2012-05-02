package com.strobel.util;

/**
 * @author Mike Strobel
 */
public final class ContractUtils {
    private ContractUtils() {}

    public static RuntimeException unreachable() {
        return new IllegalStateException("Code supposed to be unreachable");
    }
}
