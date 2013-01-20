package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;

/**
 * @author Mike Strobel
 */
public interface ClassVisitor<P> {
    void visit(
        final P parameter,
        final int majorVersion,
        final int minorVersion,
        final int flags,
        final String name,
        final String genericSignature,
        final String baseTypeName,
        final String[] interfaceNames);

    void visitAttribute(final P parameter, final SourceAttribute attribute);
    void visitAnnotation(final P parameter, final CustomAnnotation annotation, final boolean visible);

    FieldVisitor<P> visitField(
        final P parameter,
        final int flags,
        final String name,
        final TypeReference fieldType);

    MethodVisitor<P> visitMethod(
        final P parameter,
        final int flags,
        final String name,
        final TypeReference returnType,
        final TypeReference[] genericParameterTypes,
        final TypeReference[] formalParameterTypes,
        final TypeReference[] thrownTypes);

    void visitEnd(final P parameter);
}
