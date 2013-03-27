/*
 * JavaLanguage.java
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

package com.strobel.decompiler.languages.java;

import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.core.Predicate;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;

public class JavaLanguage extends Language {
    private final String _name;
    private final boolean _showAllMembers;
    private final Predicate<IAstTransform> _transformAbortCondition;

    public JavaLanguage() {
        this("Java", true, null);
    }

    private JavaLanguage(final String name, final boolean showAllMembers, final Predicate<IAstTransform> transformAbortCondition) {
        _name = name;
        _showAllMembers = showAllMembers;
        _transformAbortCondition = transformAbortCondition;
    }

    @Override
    public final String getName() {
        return _name;
    }

    @Override
    public final String getFileExtension() {
        return ".java";
    }

    @Override
    public void decompileType(final TypeDefinition type, final ITextOutput output, final DecompilationOptions options) {
        final AstBuilder builder = createAstBuilder(options, type, false);
        builder.addType(type);
        runTransformsAndGenerateCode(builder, output, options, null);
    }

    @SuppressWarnings("UnusedParameters")
    private AstBuilder createAstBuilder(
        final DecompilationOptions options,
        final TypeDefinition currentType,
        final boolean isSingleMember) {

        final DecompilerSettings settings = options.getSettings();
        final DecompilerContext context = new DecompilerContext();

        context.setCurrentType(currentType);
        context.setSettings(settings);

        return new AstBuilder(context);
    }

    private void runTransformsAndGenerateCode(
        final AstBuilder astBuilder,
        final ITextOutput output,
        final DecompilationOptions options,
        final IAstTransform additionalTransform)
    {
        astBuilder.runTransformations(_transformAbortCondition);

        if (additionalTransform != null) {
            additionalTransform.run(astBuilder.getCompilationUnit());
        }

        astBuilder.generateCode(output);
    }
}
