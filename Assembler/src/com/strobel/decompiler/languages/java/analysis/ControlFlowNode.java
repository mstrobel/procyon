/*
 * ControlFlowNode.java
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

package com.strobel.decompiler.languages.java.analysis;

import com.strobel.decompiler.languages.java.ast.Statement;

import java.util.ArrayList;
import java.util.List;

/// <summary>
/// Represents a node in the control flow graph of a C# method.
/// </summary>
public class ControlFlowNode {
    public final Statement PreviousStatement;
    public final Statement NextStatement;

    public final ControlFlowNodeType Type;

    public final List<ControlFlowEdge> Outgoing = new ArrayList<>();
    public final List<ControlFlowEdge> Incoming = new ArrayList<>();

    public ControlFlowNode(final Statement previousStatement, final Statement nextStatement, final ControlFlowNodeType type) {
        if (previousStatement == null && nextStatement == null) {
            throw new IllegalArgumentException("previousStatement and nextStatement must not be both null");
        }

        this.PreviousStatement = previousStatement;
        this.NextStatement = nextStatement;
        this.Type = type;
    }
}

