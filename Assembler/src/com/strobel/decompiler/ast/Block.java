/*
 * Block.java
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
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ITextOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Block extends Node {
    private final Collection<Node> _body;

    private Expression _entryGoto;

    public Block() {
        _body = new Collection<>();
    }

    public Block(final Iterable<Node> body) {
        this();

        for (final Node node : VerifyArgument.notNull(body, "body")) {
            _body.add(node);
        }
    }

    public Block(final Node... body) {
        this();
        Collections.addAll(_body, VerifyArgument.notNull(body, "body"));
    }

    public final Expression getEntryGoto() {
        return _entryGoto;
    }

    public final void setEntryGoto(final Expression entryGoto) {
        _entryGoto = entryGoto;
    }

    public final List<Node> getBody() {
        return _body;
    }

    @Override
    public final List<Node> getChildren() {
        final ArrayList<Node> childrenCopy = new ArrayList<>();

        if (_entryGoto != null) {
            childrenCopy.add(_entryGoto);
        }

        childrenCopy.addAll(_body);

        return childrenCopy;
    }

    @Override
    public void writeTo(final ITextOutput output) {
        final List<Node> children = getChildren();

        for (int i = 0, childrenSize = children.size(); i < childrenSize; i++) {
            final Node child = children.get(i);
            final boolean isSimpleNode = child instanceof Expression || child instanceof Label;

            if (i != 0 && !isSimpleNode && !(children.get(i - 1) instanceof Label)) {
                output.writeLine();
            }

            child.writeTo(output);

            if (isSimpleNode) {
                output.writeLine();
            }
        }
    }
}
