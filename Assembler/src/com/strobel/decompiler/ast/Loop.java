/*
 * Loop.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.ast;

import com.strobel.core.ArrayUtilities;
import com.strobel.decompiler.ITextOutput;

import java.util.Collections;
import java.util.List;

public final class Loop extends Node {
    private Expression _condition;
    private Block _body;

    public final Expression getCondition() {
        return _condition;
    }

    public final void setCondition(final Expression condition) {
        _condition = condition;
    }

    public final Block getBody() {
        return _body;
    }

    public final void setBody(final Block body) {
        _body = body;
    }

    @Override
    public final List<Node> getChildren() {
        if (_condition == null) {
            if (_body == null) {
                return Collections.emptyList();
            }
            return Collections.<Node>singletonList(_body);
        }

        if (_body == null) {
            return Collections.<Node>singletonList(_condition);
        }

        return ArrayUtilities.asUnmodifiableList(_condition, _body);
    }

    @Override
    public final void writeTo(final ITextOutput output) {
        output.writeKeyword("loop");

        if (_condition != null) {
            output.write(" (");
            _condition.writeTo(output);
            output.write(')');
        }

        output.writeLine(" {");
        output.indent();

        if (_body != null) {
            _body.writeTo(output);
        }

        output.unindent();
        output.writeLine("}");
    }
}
