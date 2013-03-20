/*
 * InstructionHelper.java
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

package com.strobel.decompiler;

import com.strobel.assembler.ir.FlowControl;
import com.strobel.assembler.ir.Instruction;
import com.strobel.assembler.metadata.MethodBody;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.IMethodSignature;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.SimpleType;
import com.strobel.util.ContractUtils;

import java.util.List;

public final class InstructionHelper {
    public static int getPopDelta(final Instruction instruction, final MethodBody body) {
        VerifyArgument.notNull(instruction, "instruction");
        VerifyArgument.notNull(body, "body");

        final OpCode code = instruction.getOpCode();

        switch (code.getStackBehaviorPop()) {
            case Pop0:
                return 0;

            case Pop1: {
                if (code == OpCode.PUTSTATIC) {
                    final FieldReference field = instruction.getOperand(0);
                    if (field.getFieldType().getSimpleType().isDoubleWord()) {
                        return 2;
                    }
                }
                return 1;
            }

            case Pop2:
            case Pop1_Pop1:
                return 2;

            case Pop1_Pop2:
                return 3;

            case Pop1_PopA: {
                if (code == OpCode.PUTFIELD) {
                    final FieldReference field = instruction.getOperand(0);
                    if (field.getFieldType().getSimpleType().isDoubleWord()) {
                        return 2;
                    }
                }
                return 2;
            }

            case Pop2_Pop1:
                return 3;

            case Pop2_Pop2:
                return 4;

            case PopI4:
                return 1;

            case PopI8:
                return 2;

            case PopR4:
                return 1;

            case PopR8:
                return 2;

            case PopA:
                return 1;

            case PopI4_PopI4:
                return 2;

            case PopI4_PopI8:
                return 3;

            case PopI8_PopI8:
                return 4;

            case PopR4_PopR4:
                return 2;

            case PopR8_PopR8:
                return 4;

            case PopI4_PopA:
                return 2;

            case PopI4_PopI4_PopA:
                return 3;

            case PopI8_PopI4_PopA:
                return 4;

            case PopR4_PopI4_PopA:
                return 3;

            case PopR8_PopI4_PopA:
                return 4;

            case PopA_PopI4_PopA:
                return 3;

            case PopA_PopA:
                return 2;

            case VarPop: {
                if (code == OpCode.ATHROW) {
                    return -1;
                }

                if (code.getFlowControl() != FlowControl.Call) {
                    break;
                }

                final IMethodSignature signature = instruction.getOperand(0);
                final List<ParameterDefinition> parameters = signature.getParameters();

                int count = parameters.size();

                if (instruction.getOpCode() != OpCode.INVOKESTATIC) {
                    ++count;
                }

                for (int i = 0; i < parameters.size(); i++) {
                    if (parameters.get(i).getParameterType().getSimpleType().isDoubleWord()) {
                        ++count;
                    }
                }

                return count;
            }
        }

        throw ContractUtils.unsupported();
    }

    public static int getPushDelta(final Instruction instruction, final MethodBody body) {
        VerifyArgument.notNull(instruction, "instruction");
        VerifyArgument.notNull(body, "body");

        final OpCode code = instruction.getOpCode();

        switch (code.getStackBehaviorPush()) {
            case Push0:
                return 0;

            case Push1: {
                if (code == OpCode.GETFIELD || code == OpCode.GETSTATIC) {
                    final FieldReference field = instruction.getOperand(0);
                    if (field.getFieldType().getSimpleType().isDoubleWord()) {
                        return 2;
                    }
                }
                return 1;
            }

            case Push1_Push1:
                return 2;

            case Push1_Push1_Push1:
                return 3;

            case Push1_Push2_Push1:
                return 4;

            case Push2:
                return 2;

            case Push2_Push2:
                return 4;

            case Push2_Push1_Push2:
                return 5;

            case Push2_Push2_Push2:
                return 6;

            case PushI4:
                return 1;

            case PushI8:
                return 2;

            case PushR4:
                return 1;

            case PushR8:
                return 2;

            case PushA:
                return 1;

            case VarPush: {
                if (code.getFlowControl() != FlowControl.Call) {
                    break;
                }

                final IMethodSignature signature = instruction.getOperand(0);
                final TypeReference returnType = signature.getReturnType();
                final SimpleType simpleType = returnType.getSimpleType();

                if (simpleType == SimpleType.Void) {
                    return 0;
                }

                return simpleType.isDoubleWord() ? 2 : 1;
            }
        }

        throw ContractUtils.unsupported();
    }
}
