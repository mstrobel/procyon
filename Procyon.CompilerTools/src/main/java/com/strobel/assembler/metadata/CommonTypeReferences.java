package com.strobel.assembler.metadata;

import com.strobel.util.ContractUtils;

public final class CommonTypeReferences {
    public final static TypeReference Object;
    public final static TypeReference String;
    public final static TypeReference Serializable;

    public final static TypeReference Boolean;
    public final static TypeReference Character;
    public final static TypeReference Byte;
    public final static TypeReference Short;
    public final static TypeReference Integer;
    public final static TypeReference Long;
    public final static TypeReference Float;
    public final static TypeReference Double;

    static {
        final MetadataParser parser = new MetadataParser(IMetadataResolver.EMPTY);

        Object = parser.parseTypeDescriptor("java/lang/Object");
        String = parser.parseTypeDescriptor("java/lang/String");
        Serializable = parser.parseTypeDescriptor("java/lang/Serializable");

        Boolean = parser.parseTypeDescriptor("java/lang/Boolean");
        Character = parser.parseTypeDescriptor("java/lang/Character");
        Byte = parser.parseTypeDescriptor("java/lang/Byte");
        Short = parser.parseTypeDescriptor("java/lang/Short");
        Integer = parser.parseTypeDescriptor("java/lang/Integer");
        Long = parser.parseTypeDescriptor("java/lang/Long");
        Float = parser.parseTypeDescriptor("java/lang/Float");
        Double = parser.parseTypeDescriptor("java/lang/Double");
    }

    private CommonTypeReferences() {
        throw ContractUtils.unreachable();
    }
}
