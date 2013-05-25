/*
 * ExceptionHandler.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.ir;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerHelpers;
import com.strobel.decompiler.PlainTextOutput;

public final class ExceptionHandler {
    private final ExceptionBlock _tryBlock;
    private final ExceptionBlock _handlerBlock;
    private final ExceptionHandlerType _handlerType;
    private final TypeReference _catchType;

    private ExceptionHandler(
        final ExceptionBlock tryBlock,
        final ExceptionBlock handlerBlock,
        final ExceptionHandlerType handlerType,
        final TypeReference catchType) {

        _tryBlock = tryBlock;
        _handlerBlock = handlerBlock;
        _handlerType = handlerType;
        _catchType = catchType;
    }

    public static ExceptionHandler createCatch(
        final ExceptionBlock tryBlock,
        final ExceptionBlock handlerBlock,
        final TypeReference catchType) {

        VerifyArgument.notNull(tryBlock, "tryBlock");
        VerifyArgument.notNull(handlerBlock, "handlerBlock");
        VerifyArgument.notNull(catchType, "catchType");

        return new ExceptionHandler(
            tryBlock,
            handlerBlock,
            ExceptionHandlerType.Catch,
            catchType
        );
    }

    public static ExceptionHandler createFinally(
        final ExceptionBlock tryBlock,
        final ExceptionBlock handlerBlock) {

        VerifyArgument.notNull(tryBlock, "tryBlock");
        VerifyArgument.notNull(handlerBlock, "handlerBlock");

        return new ExceptionHandler(
            tryBlock,
            handlerBlock,
            ExceptionHandlerType.Finally,
            null
        );
    }

    public final boolean isFinally() {
        return _handlerType == ExceptionHandlerType.Finally;
    }

    public final boolean isCatch() {
        return _handlerType == ExceptionHandlerType.Catch;
    }

    public final ExceptionBlock getTryBlock() {
        return _tryBlock;
    }

    public final ExceptionBlock getHandlerBlock() {
        return _handlerBlock;
    }

    public final ExceptionHandlerType getHandlerType() {
        return _handlerType;
    }

    public final TypeReference getCatchType() {
        return _catchType;
    }

    @Override
    public final String toString() {
        final PlainTextOutput output = new PlainTextOutput();
        DecompilerHelpers.writeExceptionHandler(output, this);
        return output.toString();
    }
}

