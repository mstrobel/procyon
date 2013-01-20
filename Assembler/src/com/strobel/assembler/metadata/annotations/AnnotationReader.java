package com.strobel.assembler.metadata.annotations;

import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.IMetadataScope;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.util.ContractUtils;

/**
 * @author Mike Strobel
 */
public final class AnnotationReader {
    public static CustomAnnotation read(final IMetadataScope scope, final Buffer input) {
        final int typeToken = input.readUnsignedShort();
        final int parameterCount = input.readUnsignedShort();
        
        final TypeReference annotationType = scope.lookupType(typeToken);
        final AnnotationParameter[] parameters = new AnnotationParameter[parameterCount];
        
        readParameters(parameters, scope, input, true);

        return new CustomAnnotation(annotationType, ArrayUtilities.asUnmodifiableList(parameters));
    }

    private static void readParameters(
        final AnnotationParameter[] parameters,
        final IMetadataScope scope,
        final Buffer input,
        final boolean namedParameter) {

        for (int i = 0; i < parameters.length; i++) {

            parameters[i] = new AnnotationParameter(
                namedParameter ? scope.<String>lookupConstant(input.readUnsignedShort())
                               : "value",
                readElement(scope, input)
            );
        }
    }

    private static AnnotationElement readElement(final IMetadataScope scope, final Buffer input) {
        final char tag = (char) input.readUnsignedByte();
        final AnnotationElementType elementType = AnnotationElementType.forTag(tag);

        switch (elementType) {
            case Constant: {
                return new ConstantAnnotationElement(scope.lookupConstant(input.readUnsignedShort()));
            }

            case Enum: {
                final TypeReference enumType = scope.lookupType(input.readUnsignedShort());
                final String constantName = scope.lookupConstant(input.readUnsignedShort());
                return new EnumAnnotationElement(enumType, constantName);
            }

            case Array: {
                final AnnotationElement[] elements = new AnnotationElement[input.readUnsignedShort()];

                for (int i = 0; i < elements.length; i++) {
                    elements[i] = readElement(scope, input);
                }

                return new ArrayAnnotationElement(elements);
            }

            case Class: {
                final TypeReference type = scope.lookupType(input.readUnsignedShort());
                return new ClassAnnotationElement(type);
            }

            case Annotation: {
                final CustomAnnotation annotation = read(scope, input);
                return new AnnotationAnnotationElement(annotation);
            }
        }

        throw ContractUtils.unreachable();
    }
}
