package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.CompilerTarget;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MetadataParser;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.Annotation;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.ContextTrackingVisitor;
import com.strobel.decompiler.languages.java.ast.Keys;
import com.strobel.decompiler.languages.java.ast.MethodDeclaration;
import com.strobel.decompiler.languages.java.ast.SimpleType;

import java.util.List;

public final class InsertOverrideAnnotationsTransform extends ContextTrackingVisitor<Void> {
    private final static String OVERRIDE_ANNOTATION_NAME = "java/lang/Override";

    private final AstBuilder _astBuilder;

    public InsertOverrideAnnotationsTransform(final DecompilerContext context) {
        super(context);
        _astBuilder = context.getUserData(Keys.AST_BUILDER);
    }

    @Override
    public Void visitMethodDeclaration(final MethodDeclaration node, final Void _) {
        tryAddOverrideAnnotation(node);
        return super.visitMethodDeclaration(node, _);
    }

    private void tryAddOverrideAnnotation(final MethodDeclaration node) {
        boolean foundOverride = false;

        for (final Annotation annotation : node.getAnnotations()) {
            final TypeReference annotationType = annotation.getType().getUserData(Keys.TYPE_REFERENCE);

            if (StringUtilities.equals(annotationType.getInternalName(), OVERRIDE_ANNOTATION_NAME)) {
                foundOverride = true;
                break;
            }
        }

        if (foundOverride) {
            return;
        }

        final MethodDefinition method = node.getUserData(Keys.METHOD_DEFINITION);

        if (method.isStatic() || method.isConstructor() || method.isTypeInitializer()) {
            return;
        }

        final TypeDefinition declaringType = method.getDeclaringType();

        if (declaringType.getCompilerMajorVersion() < CompilerTarget.JDK1_6.majorVersion) {
            return;
        }

        final TypeReference annotationType = new MetadataParser(declaringType).parseTypeDescriptor(OVERRIDE_ANNOTATION_NAME);

        final List<MethodReference> candidates = MetadataHelper.findMethods(
            declaringType,
            new Predicate<MethodReference>() {
                @Override
                public boolean test(final MethodReference reference) {
                    return StringUtilities.equals(reference.getName(), method.getName());
                }
            },
            false,
            true
        );

        for (final MethodReference candidate : candidates) {
            if (MetadataHelper.isOverride(method, candidate)) {
                final Annotation annotation = new Annotation();

                if (_astBuilder != null) {
                    annotation.setType(_astBuilder.convertType(annotationType));
                }
                else {
                    annotation.setType(new SimpleType(annotationType.getSimpleName()));
                }

                node.getAnnotations().add(annotation);
                break;
            }
        }
    }
}
