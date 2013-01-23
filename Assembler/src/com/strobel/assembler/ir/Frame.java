package com.strobel.assembler.ir;

import com.strobel.core.VerifyArgument;
import com.strobel.util.EmptyArrayCache;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:09 PM
 */
public final class Frame {
    public final static Frame SAME = new Frame(
        FrameType.Same,
        EmptyArrayCache.fromElementType(StackValue.class),
        EmptyArrayCache.fromElementType(StackValue.class)
    );

    private final FrameType _frameType;
    private final StackValue[] _localValues;
    private final StackValue[] _stackValues;

    public Frame(final FrameType frameType, final StackValue[] localValues, final StackValue[] stackValues) {
        _frameType = VerifyArgument.notNull(frameType, "frameType");
        _localValues = VerifyArgument.notNull(localValues, "localValues");
        _stackValues = VerifyArgument.notNull(stackValues, "stackValues");
    }

    public FrameType getFrameType() {
        return _frameType;
    }

    public StackValue[] getLocalValues() {
        return _localValues;
    }

    public StackValue[] getStackValues() {
        return _stackValues;
    }
}
