package com.strobel.assembler.metadata;

import com.strobel.util.ContractUtils;

public final class CommonClassReferences {
    public final static TypeReference OBJECT;
    public final static TypeReference STRING;
    public final static TypeReference SERIALIZABLE;

    public final static TypeReference BOOLEAN;
    public final static TypeReference CHARACTER;
    public final static TypeReference BYTE;
    public final static TypeReference SHORT;
    public final static TypeReference INTEGER;
    public final static TypeReference LONG;
    public final static TypeReference FLOAT;
    public final static TypeReference DOUBLE;

    static {
        final MetadataParser parser = new MetadataParser(IMetadataResolver.EMPTY);

        OBJECT = parser.parseTypeDescriptor("java/lang/Object");
        STRING = parser.parseTypeDescriptor("java/lang/String");
        SERIALIZABLE = parser.parseTypeDescriptor("java/lang/Serializable");

        BOOLEAN = parser.parseTypeDescriptor("java/lang/Boolean");
        CHARACTER = parser.parseTypeDescriptor("java/lang/Character");
        BYTE = parser.parseTypeDescriptor("java/lang/Byte");
        SHORT = parser.parseTypeDescriptor("java/lang/Short");
        INTEGER = parser.parseTypeDescriptor("java/lang/Integer");
        LONG = parser.parseTypeDescriptor("java/lang/Long");
        FLOAT = parser.parseTypeDescriptor("java/lang/Float");
        DOUBLE = parser.parseTypeDescriptor("java/lang/Double");
    }

    private CommonClassReferences() {
        throw ContractUtils.unreachable();
    }
}
