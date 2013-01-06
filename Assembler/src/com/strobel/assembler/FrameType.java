package com.strobel.assembler;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:05 PM
 */
public enum FrameType {
    /**
     * Represents a compressed frame where locals are the same as the locals in the previous frame,
     * except that additional 1-3 locals are defined, and with an empty stack.
     */
    Append,

    /**
     * Represents a compressed frame where locals are the same as the locals in the previous frame,
     * except that the last 1-3 locals are absent and with an empty stack.
     */
    Chop,

    /**
     * Represents a compressed frame with complete frame data.
     */
    Full,

    /**
     * Represents an expanded frame.
     */
    New,

    /**
     * Represents a compressed frame with exactly the same locals as the previous frame and with
     * an empty stack.
     */
    Same,

    /**
     * Represents a compressed frame with exactly the same locals as the previous frame and with
     * a single value on the stack.
     */
    Same1
}
