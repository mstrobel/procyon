/*
 * TransformationPipeline.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.core.Predicate;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.AstNode;

public final class TransformationPipeline {
    @SuppressWarnings("UnusedParameters")
    public static IAstTransform[] createPipeline(final DecompilerContext context) {
        return new IAstTransform[] {
            new EnumRewriterTransform(context),
            new EnumSwitchRewriterTransform(context),
            new PatternStatementTransform(context),
            new BreakTargetRelocation(),
            new DeclareVariablesTransform(context),
            new CollapseImportsTransform(context),
            new PushNegationTransform(context),
            new FlattenSwitchBlocksTransform(context),
            new LambdaTransform(context),
            new RemoveHiddenMembersTransform(context),
            new RemoveImplicitBoxingTransform(context),
            new IntroduceStringConcatenationTransform(context)
        };
    }

    public static void runTransformationsUntil(
        final AstNode node,
        final Predicate<IAstTransform> abortCondition,
        final DecompilerContext context) {

        if (node == null) {
            return;
        }

        for (final IAstTransform transform : createPipeline(context)) {
            if (abortCondition != null && abortCondition.test(transform)) {
                return;
            }

            transform.run(node);
        }
    }
}
