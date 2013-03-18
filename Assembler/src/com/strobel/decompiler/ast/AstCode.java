/*
 * AstCode.java
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

import com.strobel.assembler.ir.MethodBody;
import com.strobel.assembler.ir.OpCode;
import com.strobel.core.Pair;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;

public enum AstCode {
    Nop,
    AConstNull,
    __IConstM1,
    __IConst0,
    __IConst1,
    __IConst2,
    __IConst3,
    __IConst4,
    __IConst5,
    __LConst0,
    __LConst1,
    __FConst0,
    __FConst1,
    __FConst2,
    __DConst0,
    __DConst1,
    BIPush,
    SIPush,
    LdC,
    __LdCW,
    __LdC2W,
    __ILoad,
    __LLoad,
    __FLoad,
    __DLoad,
    __ALoad,
    __ILoad0,
    __ILoad1,
    __ILoad2,
    __ILoad3,
    __LLoad0,
    __LLoad1,
    __LLoad2,
    __LLoad3,
    __FLoad0,
    __FLoad1,
    __FLoad2,
    __FLoad3,
    __DLoad0,
    __DLoad1,
    __DLoad2,
    __DLoad3,
    __ALoad0,
    __ALoad1,
    __ALoad2,
    __ALoad3,
    __IALoad,
    __LALoad,
    __FALoad,
    __DALoad,
    __AALoad,
    __BALoad,
    __CALoad,
    __SALoad,
    __IStore,
    __LStore,
    __FStore,
    __DStore,
    __AStore,
    __IStore0,
    __IStore1,
    __IStore2,
    __IStore3,
    __LStore0,
    __LStore1,
    __LStore2,
    __LStore3,
    __FStore0,
    __FStore1,
    __FStore2,
    __FStore3,
    __DStore0,
    __DStore1,
    __DStore2,
    __DStore3,
    __AStore0,
    __AStore1,
    __AStore2,
    __AStore3,
    __IAStore,
    __LAStore,
    __FAStore,
    __DAStore,
    __AAStore,
    __BAStore,
    __CAStore,
    __SAStore,
    Pop,
    Pop2,
    Dup,
    DupX1,
    DupX2,
    Dup2,
    Dup2X1,
    Dup2X2,
    Swap,
    __IAdd,
    __LAdd,
    __FAdd,
    __DAdd,
    __ISub,
    __LSub,
    __FSub,
    __DSub,
    __IMul,
    __LMul,
    __FMul,
    __DMul,
    __IDiv,
    __LDiv,
    __FDiv,
    __DDiv,
    __IRem,
    __LRem,
    __FRem,
    __DRem,
    __INeg,
    __LNeg,
    __FNeg,
    __DNeg,
    __IShl,
    __LShl,
    __IShr,
    __LShr,
    __IUShr,
    __LUShr,
    __IAnd,
    __LAnd,
    __IOr,
    __LOr,
    __IXor,
    __LXor,
    __IInc,
    I2L,
    I2F,
    I2D,
    L2I,
    L2F,
    L2D,
    F2I,
    F2L,
    F2D,
    D2I,
    D2L,
    D2F,
    I2B,
    I2C,
    I2S,
    __LCmp,
    __FCmpL,
    __FCmpG,
    __DCmpL,
    __DCmpG,
    __IfEq,
    __IfNe,
    __IfLt,
    __IfGe,
    __IfGt,
    __IfLe,
    __IfICmpEq,
    __IfICmpNe,
    __IfICmpLt,
    __IfICmpGe,
    __IfICmpGt,
    __IfICmpLe,
    __IfACmpEq,
    __IfACmpNe,
    Goto,
    Jsr,
    Ret,
    TableSwitch,
    LookupSwitch,
    __IReturn,
    __LReturn,
    __FReturn,
    __DReturn,
    __AReturn,
    __Return,
    GetStatic,
    PutStatic,
    GetField,
    PutField,
    InvokeVirtual,
    InvokeSpecial,
    InvokeStatic,
    InvokeInterface,
    InvokeDynamic,
    New,
    __NewArray,
    __ANewArray,
    ArrayLength,
    AThrow,
    CheckCast,
    InstanceOf,
    MonitorEnter,
    MonitorExit,
    MultiANewArray,
    __IfNull,
    __IfNonNull,
    __GotoW,
    __JsrW,
    Breakpoint,
    __ILoadW,
    __LLoadW,
    __FLoadW,
    __DLoadW,
    __ALoadW,
    __IStoreW,
    __LStoreW,
    __FStoreW,
    __DStoreW,
    __AStoreW,
    __IIncW,
    __RetW,

    //
    // Virtual codes, defined for convenience.
    //
    Load,
    Store,
    LoadElement,
    StoreElement,
    Add,
    Sub,
    Mul,
    Div,
    Rem,
    Neg,
    Shl,
    Shr,
    UShr,
    And,
    Or,
    Not,
    Xor,
    Inc,
    CmpEq,
    CmpNe,
    CmpLt,
    CmpGe,
    CmpGt,
    CmpLe,
    IfTrue,
    Return,
    NewArray,
    LoadException,
    LogicalNot,
    LogicalAnd,
    LogicalOr,
    InitArray,

    /**
     * Defines a barrier between the parent expression and the argument expression that prevents combining them.
     */
    Wrap,

    TernaryOp,
    LoopOrSwitchBreak,
    LoopContinue,

    /**
     * <p> Expression with a single binary operator child.  Indicates that the binary operator will also assign the new value to its left-hand side. </p> <p>
     * {@link #CompoundAssignment} must not be used for local variables, as inlining and other optimizations don't know that it modifies the variable. </p>
     */
    CompoundAssignment,

    PostIncrement,

    /**
     * Simulates creation of a boxed type from its corresponding primitive type.
     */
    Box,

    /**
     * Simulates extraction of a primitive type from its corresponding boxed type.
     */
    Unbox;

    private final static OpCode[] STANDARD_CODES = OpCode.values();

    public final String getName() {
        return StringUtilities.trimAndRemoveLeft(name().toLowerCase(), "_");
    }

    public final boolean isConditionalControlFlow() {
        final int ordinal = ordinal();

        if (ordinal < STANDARD_CODES.length) {
            final OpCode standardCode = STANDARD_CODES[ordinal];
            return standardCode.isBranch() && !standardCode.isUnconditionalBranch();
        }

        switch (this) {
            case CmpEq:
            case CmpNe:
            case CmpLt:
            case CmpGe:
            case CmpGt:
            case CmpLe:
                return true;

            default:
                return false;
        }
    }

    public final boolean isUnconditionalControlFlow() {
        final int ordinal = ordinal();

        if (ordinal < STANDARD_CODES.length) {
            final OpCode standardCode = STANDARD_CODES[ordinal];
            return standardCode.isUnconditionalBranch();
        }

        switch (this) {
            case LoopContinue:
            case LoopOrSwitchBreak:
                return true;

            default:
                return false;
        }
    }

    public static Pair<AstCode, Object> expandMacro(final AstCode code, final Object operand, final MethodBody body) {
        switch (code) {
            case __IConstM1:
                return Pair.<AstCode, Object>create(LdC, -1);
            case __IConst0:
                return Pair.<AstCode, Object>create(LdC, 0);
            case __IConst1:
                return Pair.<AstCode, Object>create(LdC, 1);
            case __IConst2:
                return Pair.<AstCode, Object>create(LdC, 2);
            case __IConst3:
                return Pair.<AstCode, Object>create(LdC, 3);
            case __IConst4:
                return Pair.<AstCode, Object>create(LdC, 4);
            case __IConst5:
                return Pair.<AstCode, Object>create(LdC, 5);
            case __LConst0:
                return Pair.<AstCode, Object>create(LdC, 0L);
            case __LConst1:
                return Pair.<AstCode, Object>create(LdC, 1L);
            case __FConst0:
                return Pair.<AstCode, Object>create(LdC, 0f);
            case __FConst1:
                return Pair.<AstCode, Object>create(LdC, 1f);
            case __FConst2:
                return Pair.<AstCode, Object>create(LdC, 2f);
            case __DConst0:
                return Pair.<AstCode, Object>create(LdC, 0d);
            case __DConst1:
                return Pair.<AstCode, Object>create(LdC, 1d);

            case __GotoW:
                return Pair.create(Goto, operand);

            case __ILoad:
            case __LLoad:
            case __FLoad:
            case __DLoad:
            case __ALoad:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(0));
            case __ILoad0:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(0));
            case __ILoad1:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(1));
            case __ILoad2:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(2));
            case __ILoad3:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(3));
            case __LLoad0:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(0));
            case __LLoad1:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(1));
            case __LLoad2:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(2));
            case __LLoad3:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(3));
            case __FLoad0:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(0));
            case __FLoad1:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(1));
            case __FLoad2:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(2));
            case __FLoad3:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(3));
            case __DLoad0:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(0));
            case __DLoad1:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(1));
            case __DLoad2:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(2));
            case __DLoad3:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(3));
            case __ALoad0:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(0));
            case __ALoad1:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(1));
            case __ALoad2:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(2));
            case __ALoad3:
                return Pair.<AstCode, Object>create(Load, body.getVariables().get(3));

            case __IALoad:
            case __LALoad:
            case __FALoad:
            case __DALoad:
            case __AALoad:
            case __BALoad:
            case __CALoad:
            case __SALoad:
                return Pair.create(LoadElement, operand);
            
            case __IStore:
            case __LStore:
            case __FStore:
            case __DStore:
            case __AStore:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(0));
            case __IStore0:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(0));
            case __IStore1:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(1));
            case __IStore2:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(2));
            case __IStore3:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(3));
            case __LStore0:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(0));
            case __LStore1:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(1));
            case __LStore2:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(2));
            case __LStore3:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(3));
            case __FStore0:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(0));
            case __FStore1:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(1));
            case __FStore2:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(2));
            case __FStore3:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(3));
            case __DStore0:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(0));
            case __DStore1:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(1));
            case __DStore2:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(2));
            case __DStore3:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(3));
            case __AStore0:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(0));
            case __AStore1:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(1));
            case __AStore2:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(2));
            case __AStore3:
                return Pair.<AstCode, Object>create(Store, body.getVariables().get(3));

            case __IAStore:
            case __LAStore:
            case __FAStore:
            case __DAStore:
            case __AAStore:
            case __BAStore:
            case __CAStore:
            case __SAStore:
                return Pair.create(StoreElement, operand);

            default:
                return null;
        }
    }

    public static boolean expandMacro(final StrongBox<AstCode> code, final StrongBox<Object> operand, final MethodBody body) {
        switch (code.get()) {
            case __IConstM1:
                code.set(LdC);
                operand.set(-1);
                return true;
            case __IConst0:
                code.set(LdC);
                operand.set(0);
                return true;
            case __IConst1:
                code.set(LdC);
                operand.set(1);
                return true;
            case __IConst2:
                code.set(LdC);
                operand.set(2);
                return true;
            case __IConst3:
                code.set(LdC);
                operand.set(3);
                return true;
            case __IConst4:
                code.set(LdC);
                operand.set(4);
                return true;
            case __IConst5:
                code.set(LdC);
                operand.set(5);
                return true;
            case __LConst0:
                code.set(LdC);
                operand.set(0L);
                return true;
            case __LConst1:
                code.set(LdC);
                operand.set(1L);
                return true;
            case __FConst0:
                code.set(LdC);
                operand.set(0f);
                return true;
            case __FConst1:
                code.set(LdC);
                operand.set(1f);
                return true;
            case __FConst2:
                code.set(LdC);
                operand.set(2f);
                return true;
            case __DConst0:
                code.set(LdC);
                operand.set(0d);
                return true;
            case __DConst1:
                code.set(LdC);
                operand.set(1d);
                return true;

            case __ILoad:
            case __LLoad:
            case __FLoad:
            case __DLoad:
            case __ALoad:
                code.set(Load);
                return true;
            case __ILoad0:
                code.set(Load);
                operand.set(body.getVariables().get(0));
                return true;
            case __ILoad1:
                code.set(Load);
                operand.set(body.getVariables().get(1));
                return true;
            case __ILoad2:
                code.set(Load);
                operand.set(body.getVariables().get(2));
                return true;
            case __ILoad3:
                code.set(Load);
                operand.set(body.getVariables().get(3));
                return true;
            case __LLoad0:
                code.set(Load);
                operand.set(body.getVariables().get(0));
                return true;
            case __LLoad1:
                code.set(Load);
                operand.set(body.getVariables().get(1));
                return true;
            case __LLoad2:
                code.set(Load);
                operand.set(body.getVariables().get(2));
                return true;
            case __LLoad3:
                code.set(Load);
                operand.set(body.getVariables().get(3));
                return true;
            case __FLoad0:
                code.set(Load);
                operand.set(body.getVariables().get(0));
                return true;
            case __FLoad1:
                code.set(Load);
                operand.set(body.getVariables().get(1));
                return true;
            case __FLoad2:
                code.set(Load);
                operand.set(body.getVariables().get(2));
                return true;
            case __FLoad3:
                code.set(Load);
                operand.set(body.getVariables().get(3));
                return true;
            case __DLoad0:
                code.set(Load);
                operand.set(body.getVariables().get(0));
                return true;
            case __DLoad1:
                code.set(Load);
                operand.set(body.getVariables().get(1));
                return true;
            case __DLoad2:
                code.set(Load);
                operand.set(body.getVariables().get(2));
                return true;
            case __DLoad3:
                code.set(Load);
                operand.set(body.getVariables().get(3));
                return true;
            case __ALoad0:
                code.set(Load);
                operand.set(body.getVariables().get(0));
                return true;
            case __ALoad1:
                code.set(Load);
                operand.set(body.getVariables().get(1));
                return true;
            case __ALoad2:
                code.set(Load);
                operand.set(body.getVariables().get(2));
                return true;
            case __ALoad3:
                code.set(Load);
                operand.set(body.getVariables().get(3));
                return true;

            case __IALoad:
            case __LALoad:
            case __FALoad:
            case __DALoad:
            case __AALoad:
            case __BALoad:
            case __CALoad:
            case __SALoad:
                code.set(LoadElement);
                return true;
            
            case __GotoW:
                code.set(Goto);
                return true;

            case __IStore:
            case __LStore:
            case __FStore:
            case __DStore:
            case __AStore:
                code.set(Store);
                return true;
            case __IStore0:
                code.set(Store);
                operand.set(body.getVariables().get(0));
                return true;
            case __IStore1:
                code.set(Store);
                operand.set(body.getVariables().get(1));
                return true;
            case __IStore2:
                code.set(Store);
                operand.set(body.getVariables().get(2));
                return true;
            case __IStore3:
                code.set(Store);
                operand.set(body.getVariables().get(3));
                return true;
            case __LStore0:
                code.set(Store);
                operand.set(body.getVariables().get(0));
                return true;
            case __LStore1:
                code.set(Store);
                operand.set(body.getVariables().get(1));
                return true;
            case __LStore2:
                code.set(Store);
                operand.set(body.getVariables().get(2));
                return true;
            case __LStore3:
                code.set(Store);
                operand.set(body.getVariables().get(3));
                return true;
            case __FStore0:
                code.set(Store);
                operand.set(body.getVariables().get(0));
                return true;
            case __FStore1:
                code.set(Store);
                operand.set(body.getVariables().get(1));
                return true;
            case __FStore2:
                code.set(Store);
                operand.set(body.getVariables().get(2));
                return true;
            case __FStore3:
                code.set(Store);
                operand.set(body.getVariables().get(3));
                return true;
            case __DStore0:
                code.set(Store);
                operand.set(body.getVariables().get(0));
                return true;
            case __DStore1:
                code.set(Store);
                operand.set(body.getVariables().get(1));
                return true;
            case __DStore2:
                code.set(Store);
                operand.set(body.getVariables().get(2));
                return true;
            case __DStore3:
                code.set(Store);
                operand.set(body.getVariables().get(3));
                return true;
            case __AStore0:
                code.set(Store);
                operand.set(body.getVariables().get(0));
                return true;
            case __AStore1:
                code.set(Store);
                operand.set(body.getVariables().get(1));
                return true;
            case __AStore2:
                code.set(Store);
                operand.set(body.getVariables().get(2));
                return true;
            case __AStore3:
                code.set(Store);
                operand.set(body.getVariables().get(3));
                return true;

            case __IAStore:
            case __LAStore:
            case __FAStore:
            case __DAStore:
            case __AAStore:
            case __BAStore:
            case __CAStore:
            case __SAStore:
                code.set(StoreElement);
                return true;
            
            default:
                return false;
        }
    }
}
