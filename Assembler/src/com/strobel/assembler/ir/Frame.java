/*
 * Frame.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

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
