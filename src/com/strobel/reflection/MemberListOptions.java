package com.strobel.reflection;

/**
 * @author Mike Strobel
 */
enum MemberListOptions {
    CaseSensitive(0x01),
    CaseInsensitive(0x02),
    All(CaseSensitive.mask | CaseInsensitive.mask);

    private final int mask;

    private MemberListOptions(int mask) {
        this.mask = mask;
    }
}
