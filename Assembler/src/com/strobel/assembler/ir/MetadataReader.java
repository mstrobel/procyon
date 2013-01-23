package com.strobel.assembler.ir;

import com.strobel.assembler.ir.attributes.*;
import com.strobel.assembler.metadata.*;
import com.strobel.assembler.metadata.annotations.AnnotationElement;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public abstract class MetadataReader {
    private final IMetadataResolver _resolver;
    private final MetadataParser _parser;

    protected MetadataReader(final IMetadataResolver resolver) {
        _resolver = VerifyArgument.notNull(resolver, "resolver");
        _parser = new MetadataParser(resolver);
    }

    protected abstract IMetadataScope getScope();

    public void readAttributes(final Buffer input, final SourceAttribute[] attributes) {
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = readAttribute(input);
        }
    }

    public SourceAttribute readAttribute(final Buffer buffer) {
        final int nameIndex = buffer.readUnsignedShort();
        final int length = buffer.readInt();
        final IMetadataScope scope = getScope();
        final String name = scope.lookupConstant(nameIndex);

        if (length == 0) {
            return SourceAttribute.create(name);
        }

        switch (name) {
            case AttributeNames.SourceFile: {
                final int token = buffer.readUnsignedShort();
                final String sourceFile = scope.lookupConstant(token);
                return new SourceFileAttribute(sourceFile);
            }

            case AttributeNames.ConstantValue: {
                final int token = buffer.readUnsignedShort();
                final Object constantValue = scope.lookupConstant(token);
                return new ConstantValueAttribute(constantValue);
            }

            case AttributeNames.Code: {
                final int maxStack = buffer.readUnsignedShort();
                final int maxLocals = buffer.readUnsignedShort();
                final int codeOffset = buffer.position();
                final int codeLength = buffer.readInt();
                final byte[] code = new byte[codeLength];

                buffer.read(code, 0, codeLength);

                final int exceptionTableLength = buffer.readUnsignedShort();
                final ExceptionTableEntry[] exceptionTable = new ExceptionTableEntry[exceptionTableLength];

                for (int k = 0; k < exceptionTableLength; k++) {
                    final int startOffset = buffer.readUnsignedShort();
                    final int endOffset = buffer.readUnsignedShort();
                    final int handlerOffset = buffer.readUnsignedShort();
                    final int catchTypeToken = buffer.readUnsignedShort();
                    final TypeReference catchType;

                    if (catchTypeToken == 0) {
                        catchType = null;
                    }
                    else {
                        catchType = _resolver.lookupType(scope.<String>lookupConstant(catchTypeToken));
                    }

                    exceptionTable[k] = new ExceptionTableEntry(
                        startOffset,
                        endOffset,
                        handlerOffset,
                        catchType
                    );
                }

                final int attributeCount = buffer.readUnsignedShort();
                final SourceAttribute[] attributes = new SourceAttribute[attributeCount];

                readAttributes(buffer, attributes);

                return new CodeAttribute(
                    length,
                    maxStack,
                    maxLocals,
                    codeOffset,
                    codeLength,
                    exceptionTable,
                    attributes
                );
            }

            case AttributeNames.Exceptions: {
                final int exceptionCount = buffer.readUnsignedShort();
                final TypeReference[] exceptionTypes = new TypeReference[exceptionCount];

                for (int i = 0; i < exceptionTypes.length; i++) {
                    exceptionTypes[i] = scope.lookupType(buffer.readUnsignedShort());
                }

                return new ExceptionsAttribute(exceptionTypes);
            }

            case AttributeNames.LineNumberTable: {
                final int entryCount = buffer.readUnsignedShort();
                final LineNumberTableEntry[] entries = new LineNumberTableEntry[entryCount];

                for (int i = 0; i < entries.length; i++) {
                    entries[i] = new LineNumberTableEntry(
                        buffer.readUnsignedShort(),
                        buffer.readUnsignedShort()
                    );
                }

                return new LineNumberTableAttribute(entries);
            }

            case AttributeNames.LocalVariableTable:
            case AttributeNames.LocalVariableTypeTable: {
                final int entryCount = buffer.readUnsignedShort();
                final LocalVariableTableEntry[] entries = new LocalVariableTableEntry[entryCount];

                for (int i = 0; i < entries.length; i++) {
                    final int scopeOffset = buffer.readUnsignedShort();
                    final int scopeLength = buffer.readUnsignedShort();
                    final String variableName = scope.lookupConstant(buffer.readUnsignedShort());
                    final String descriptor = scope.lookupConstant(buffer.readUnsignedShort());
                    final int variableIndex = buffer.readUnsignedShort();

                    entries[i] = new LocalVariableTableEntry(
                        variableIndex,
                        variableName,
                        _parser.parseTypeSignature(descriptor),
                        scopeOffset,
                        scopeLength
                    );
                }

                return new LocalVariableTableAttribute(name, entries);
            }

            case AttributeNames.EnclosingMethod: {
                return new EnclosingMethodAttribute(
                    scope.lookupMethod(
                        buffer.readUnsignedShort(),
                        buffer.readUnsignedShort()
                    )
                );
            }

/*
            case AttributeNames.InnerClasses: {
                throw ContractUtils.unreachable();
            }
*/

            case AttributeNames.RuntimeVisibleAnnotations:
            case AttributeNames.RuntimeInvisibleAnnotations: {
                final CustomAnnotation[] annotations = new CustomAnnotation[buffer.readUnsignedShort()];

                for (int i = 0; i < annotations.length; i++) {
                    annotations[i] = AnnotationReader.read(scope, buffer);
                }

                return new AnnotationsAttribute(name, length, annotations);
            }

            case AttributeNames.RuntimeVisibleParameterAnnotations:
            case AttributeNames.RuntimeInvisibleParameterAnnotations: {
                final CustomAnnotation[][] annotations = new CustomAnnotation[buffer.readUnsignedShort()][];

                for (int i = 0; i < annotations.length; i++) {
                    final CustomAnnotation[] parameterAnnotations = new CustomAnnotation[buffer.readUnsignedShort()];

                    for (int j = 0; j < parameterAnnotations.length; j++) {
                        parameterAnnotations[j] = AnnotationReader.read(scope, buffer);
                    }

                    annotations[i] = parameterAnnotations;
                }

                return new ParameterAnnotationsAttribute(name, length, annotations);
            }

            case AttributeNames.AnnotationDefault: {
                final AnnotationElement defaultValue = AnnotationReader.readElement(scope, buffer);
                return new AnnotationDefaultAttribute(length, defaultValue);
            }

            case AttributeNames.Signature: {
                final int token = buffer.readUnsignedShort();
                final String signature = scope.lookupConstant(token);
                return new SignatureAttribute(signature);
            }

            default: {
                final byte[] blob = new byte[length];
                buffer.read(blob, 0, blob.length);
                return new BlobAttribute(name, blob);
            }
        }
    }
}
