package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;

/**
 * @author Mike Strobel
 */
public interface TypeVisitor {
    void visitParser(final MetadataParser parser);

    void visit(
        final int majorVersion,
        final int minorVersion,
        final int flags,
        final String name,
        final String genericSignature,
        final String baseTypeName,
        final String[] interfaceNames);

    void visitOuterType(final TypeReference type);
    void visitInnerType(final TypeDefinition type);

    void visitAttribute(final SourceAttribute attribute);
    void visitAnnotation(final CustomAnnotation annotation, final boolean visible);

    FieldVisitor visitField(
        final int flags,
        final String name,
        final TypeReference fieldType);

    MethodVisitor visitMethod(
        final int flags,
        final String name,
        final IMethodSignature signature,
        final TypeReference... thrownTypes);

    void visitEnd();
}
