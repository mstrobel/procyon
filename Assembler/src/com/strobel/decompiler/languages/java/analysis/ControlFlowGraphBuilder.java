/*
 * ControlFlowGraphBuilder.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found : the License.html file at the root of this distribution.
 * By using this source code : any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.analysis;

import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.Statement;
import com.strobel.decompiler.semantics.ResolveResult;
import com.strobel.functions.Function;

import java.util.ArrayList;
import java.util.HashMap;

public class ControlFlowGraphBuilder {
    private Statement rootStatement;
    private Function<AstNode, ResolveResult> resolver;
    private ArrayList<ControlFlowNode> nodes;
    private HashMap<String, ControlFlowNode> labels;
    private ArrayList<ControlFlowNode> gotoStatements;

    protected ControlFlowNode createNode(
        final Statement previousStatement,
        final Statement nextStatement,
        final ControlFlowNodeType type) {

        return new ControlFlowNode(previousStatement, nextStatement, type);
    }

    protected ControlFlowEdge createEdge(
        final ControlFlowNode from,
        final ControlFlowNode to,
        final ControlFlowEdgeType type) {

        return new ControlFlowEdge(from, to, type);
    }

/*
    final ArrayList<ControlFlowNode> buildControlFlowGraph(
        final Statement statement,
        final Function<AstNode, ResolveResult> resolver) {

        final NodeCreationVisitor nodeCreationVisitor = new NodeCreationVisitor();

        nodeCreationVisitor.builder = this;

        try {
            this.nodes = new ArrayList<ControlFlowNode>();
            this.labels = new HashMap<String, ControlFlowNode>();
            this.gotoStatements = new ArrayList<ControlFlowNode>();
            this.rootStatement = statement;
            this.resolver = resolver;

            final ControlFlowNode entryPoint = createStartNode(statement);

            statement.acceptVisitor(nodeCreationVisitor, entryPoint);

            // Resolve goto statements:
            for (ControlFlowNode gotoStmt : gotoStatements) {
                final String label = ((GotoStatement) gotoStmt.NextStatement).getLabel();
                final ControlFlowNode labelNode = labels.get(label);

                if (labelNode != null) {
                    nodeCreationVisitor.connect(gotoStmt, labelNode, ControlFlowEdgeType.Jump);
                }
            }

            annotateLeaveEdgesWithTryFinallyBlocks();

            return nodes;
        }
        finally {
            this.nodes = null;
            this.labels = null;
            this.gotoStatements = null;
            this.rootStatement = null;
            this.resolver = null;
        }
    }
*/
}
