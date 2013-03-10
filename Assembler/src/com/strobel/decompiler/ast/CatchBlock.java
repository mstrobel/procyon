/*
 * CatchBlock.java
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

package com.strobel.decompiler.ast;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.ITextOutput;

public final class CatchBlock extends Block {
    private TypeReference _exceptionType;
    private Variable _exceptionVariable;

    public final TypeReference getExceptionType() {
        return _exceptionType;
    }

    public final void setExceptionType(final TypeReference exceptionType) {
        _exceptionType = exceptionType;
    }

    public final Variable getExceptionVariable() {
        return _exceptionVariable;
    }

    public final void setExceptionVariable(final Variable exceptionVariable) {
        _exceptionVariable = exceptionVariable;
    }

    @Override
    public final void writeTo(final ITextOutput output) {
        output.write("catch");

        if (_exceptionType != null) {
            output.write(" (");
            output.writeReference(_exceptionType.getFullName(), _exceptionType);

            if (_exceptionVariable != null) {
                output.write(" %s", _exceptionVariable.getName());
            }

            output.write(')');
        }

        output.writeLine(" {");
        output.indent();

        super.writeTo(output);

        output.unindent();
        output.writeLine("}");
    }
}
