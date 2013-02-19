/*
 * InstructionVisitor.java
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

import com.strobel.assembler.metadata.*;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:15 PM
 */
public interface InstructionVisitor {
    void visit(final Instruction instruction);
    void visit(final OpCode opCode);

    void visitConstant(final OpCode opCode, final TypeReference value);
    void visitConstant(final OpCode opCode, final int value);
    void visitConstant(final OpCode opCode, final long value);
    void visitConstant(final OpCode opCode, final float value);
    void visitConstant(final OpCode opCode, final double value);
    void visitConstant(final OpCode opCode, final String value);
    
    void visitBranch(final OpCode opCode, final Instruction target);
    void visitVariable(final OpCode opCode, final VariableReference variable);
    void visitVariable(final OpCode opCode, final VariableReference variable, int operand);
    void visitType(final OpCode opCode, final TypeReference type);
    void visitMethod(final OpCode opCode, final MethodReference method);
    void visitField(final OpCode opCode, final FieldReference field);

    void visitLabel(final Label label);

    void visitSwitch(final OpCode opCode, final SwitchInfo switchInfo);
}
