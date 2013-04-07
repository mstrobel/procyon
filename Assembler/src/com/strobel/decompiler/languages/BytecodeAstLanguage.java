/*
 * BytecodeAstLanguage.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClassFileReader;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeDefinitionBuilder;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.AnsiTextOutput;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerHelpers;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.NameSyntax;
import com.strobel.decompiler.ast.AstBuilder;
import com.strobel.decompiler.ast.AstOptimizationStep;
import com.strobel.decompiler.ast.AstOptimizer;
import com.strobel.decompiler.ast.Block;
import com.strobel.decompiler.ast.Expression;
import com.strobel.decompiler.ast.Variable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BytecodeAstLanguage extends Language {
    private final String _name;
    private final boolean _inlineVariables;
    private final AstOptimizationStep _abortBeforeStep;

    public BytecodeAstLanguage() {
        this("Bytecode AST", true, AstOptimizationStep.None);
    }

    private BytecodeAstLanguage(final String name, final boolean inlineVariables, final AstOptimizationStep abortBeforeStep) {
        _name = name;
        _inlineVariables = inlineVariables;
        _abortBeforeStep = abortBeforeStep;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getFileExtension() {
        return ".jvm";
    }

    @Override
    public void decompileType(final TypeDefinition type, final ITextOutput output, final DecompilationOptions options) {
        final ClasspathTypeLoader loader = new ClasspathTypeLoader();
        final Buffer buffer = new Buffer(0);

        if (!loader.tryLoadType(type.getInternalName(), buffer)) {
            output.writeLine("!!! ERROR: Failed to load class %s.", type.getInternalName());
            return;
        }

        final ClassFileReader reader = ClassFileReader.readClass(
            ClassFileReader.OPTION_PROCESS_CODE |
            ClassFileReader.OPTION_PROCESS_ANNOTATIONS,
            type.getResolver(),
            buffer
        );

        final TypeDefinitionBuilder typeBuilder = new TypeDefinitionBuilder();

        reader.accept(typeBuilder);

        final TypeDefinition typeWithCode = typeBuilder.getTypeDefinition();

        boolean first = true;

        for (final MethodDefinition method : typeWithCode.getDeclaredMethods()) {
            if (!first) {
                output.writeLine();
            }
            else {
                first = false;
            }

            decompileMethod(method, output, options);
        }
    }

    @Override
    public void decompileMethod(final MethodDefinition method, final ITextOutput output, final DecompilationOptions options) {
        VerifyArgument.notNull(method, "method");
        VerifyArgument.notNull(output, "output");
        VerifyArgument.notNull(options, "options");

        if (!method.hasBody()) {
            return;
        }

        final DecompilerContext context = new DecompilerContext();

        context.setCurrentMethod(method);
        context.setCurrentType(method.getDeclaringType());

        final Block methodAst = new Block();

        methodAst.getBody().addAll(AstBuilder.build(method.getBody(), _inlineVariables, context));

        if (_abortBeforeStep != null) {
            AstOptimizer.optimize(context, methodAst, _abortBeforeStep);
        }

        final Set<Variable> allVariables = new LinkedHashSet<>();

        for (final Expression e : methodAst.getSelfAndChildrenRecursive(Expression.class)) {
            final Object operand = e.getOperand();

            if (operand instanceof Variable && !((Variable) operand).isParameter()) {
                allVariables.add((Variable) operand);
            }
        }

        output.writeLine("%s {", method.isTypeInitializer() ? "static" : method.getDescription());
        output.indent();


        if (!allVariables.isEmpty()) {
            for (final Variable variable : allVariables) {
                output.writeDefinition(variable.getName(), variable);

                final TypeReference type = variable.getType();

                if (type != null) {
                    output.write(" : ");
                    DecompilerHelpers.writeType(output, type, NameSyntax.SHORT_TYPE_NAME);
                }

                if (variable.isGenerated()) {
                    output.write(" [generated]");
                }

                output.writeLine();
            }

            output.writeLine();
        }

        methodAst.writeTo(output);

        output.unindent();
        output.writeLine("}");
    }

    @Override
    public String typeToString(final TypeReference type, final boolean includePackage) {
        final ITextOutput output = new AnsiTextOutput();
        DecompilerHelpers.writeType(output, type, includePackage ? NameSyntax.TYPE_NAME : NameSyntax.SHORT_TYPE_NAME);
        return output.toString();
    }

    public static List<BytecodeAstLanguage> getDebugLanguages() {
        final AstOptimizationStep[] steps = AstOptimizationStep.values();
        final BytecodeAstLanguage[] languages = new BytecodeAstLanguage[steps.length];

        languages[0] = new BytecodeAstLanguage("Bytecode AST (Unoptimized)", false, steps[0]);

        String nextName = "Bytecode AST (Variable Splitting)";

        for (int i = 1; i < languages.length; i++) {
            languages[i] = new BytecodeAstLanguage(nextName, true, steps[i - 1]);
            nextName = "Bytecode AST (After " + steps[i - 1].name() + ")";
        }

        return ArrayUtilities.asUnmodifiableList(languages);
    }
}
