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

import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.EmptyArrayCache;

import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:09 PM
 */
public final class Frame {
    public final static Frame SAME = new Frame(
        FrameType.Same,
        EmptyArrayCache.fromElementType(FrameValue.class),
        EmptyArrayCache.fromElementType(FrameValue.class)
    );

    private final FrameType _frameType;
    private final List<FrameValue> _localValues;
    private final List<FrameValue> _stackValues;

    public Frame(final FrameType frameType, final FrameValue[] localValues, final FrameValue[] stackValues) {
        _frameType = VerifyArgument.notNull(frameType, "frameType");
        _localValues = ArrayUtilities.asUnmodifiableList(VerifyArgument.notNull(localValues, "localValues").clone());
        _stackValues = ArrayUtilities.asUnmodifiableList(VerifyArgument.notNull(stackValues, "stackValues").clone());
    }

    public final FrameType getFrameType() {
        return _frameType;
    }

    public final List<FrameValue> getLocalValues() {
        return _localValues;
    }

    public final List<FrameValue> getStackValues() {
        return _stackValues;
    }
}
