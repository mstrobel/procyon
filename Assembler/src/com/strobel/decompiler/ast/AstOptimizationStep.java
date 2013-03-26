/*
 * AstOptimizatonStep.java
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

public enum AstOptimizationStep {
    RemoveRedundantCode,
    ReduceBranchInstructionSet,
    InlineVariables,
    CopyPropagation,
    SplitToMovableBlocks,
    TypeInference,
    SimplifyShortCircuit,
    SimplifyTernaryOperator,
    JoinBasicBlocks,
    SimplifyLogicalNot,
    SimplifyShiftOperations,
    SimplifyLoadAndStore,
    TransformObjectInitializers,
    TransformArrayInitializers,
    MakeAssignmentExpression,
    IntroducePostIncrement,
    InlineVariables2,
    FindLoops,
    FindConditions,
    FlattenNestedMovableBlocks,
//    RemoveEndFinally,
    RemoveRedundantCode2,
    GotoRemoval,
    DuplicateReturns,
    GotoRemoval2,
    ReduceIfNesting,
    InlineVariables3,
    ReduceComparisonInstructionSet,
    RecombineVariables,
    TypeInference2,
    RemoveRedundantCode3,
    None
}
