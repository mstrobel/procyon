/*
 * Switch.java
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

import com.strobel.assembler.Collection;
import com.strobel.core.ArrayUtilities;
import com.strobel.decompiler.ITextOutput;

import java.util.List;

public final class Switch extends Node {
    private final List<CaseBlock> _caseBlocks = new Collection<>();
    private Expression _condition;

    public final Expression getCondition() {
        return _condition;
    }

    public final void setCondition(final Expression condition) {
        _condition = condition;
    }

    public final List<CaseBlock> getCaseBlocks() {
        return _caseBlocks;
    }

    @Override
    public final List<Node> getChildren() {
        final int size = _caseBlocks.size() + (_condition != null ? 1 : 0);
        final Node[] children = new Node[size];

        int i = 0;

        if (_condition != null) {
            children[i++] = _condition;
        }

        for (final CaseBlock caseBlock : _caseBlocks) {
            children[i++] = caseBlock;
        }

        return ArrayUtilities.asUnmodifiableList(children);
    }

    @Override
    public final void writeTo(final ITextOutput output) {
        output.write("switch (");

        if (_condition != null) {
            _condition.writeTo(output);
        }
        else {
            output.write("...");
        }

        output.writeLine(") {");
        output.indent();

        for (final CaseBlock caseBlock : _caseBlocks) {
            caseBlock.writeTo(output);
        }

        output.unindent();
        output.writeLine("}");
    }
}
