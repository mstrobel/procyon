package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.ClassFileReader;
import com.strobel.reflection.SimpleType;
import com.strobel.util.ContractUtils;

/**
 * @author Mike Strobel
 */
public final class BuiltinTypes {
    public final static TypeDefinition Boolean;
    public final static TypeDefinition Byte;
    public final static TypeDefinition Character;
    public final static TypeDefinition Short;
    public final static TypeDefinition Integer;
    public final static TypeDefinition Long;
    public final static TypeDefinition Float;
    public final static TypeDefinition Double;
    public final static TypeDefinition Void;
    public final static TypeDefinition Object;
    public final static TypeDefinition Bottom;

    static {
        Boolean = new PrimitiveType(SimpleType.Boolean);
        Byte = new PrimitiveType(SimpleType.Byte);
        Character = new PrimitiveType(SimpleType.Character);
        Short = new PrimitiveType(SimpleType.Short);
        Integer = new PrimitiveType(SimpleType.Integer);
        Long = new PrimitiveType(SimpleType.Long);
        Float = new PrimitiveType(SimpleType.Float);
        Double = new PrimitiveType(SimpleType.Double);
        Void = new PrimitiveType(SimpleType.Void);
        Bottom = null;

        final Buffer buffer = new Buffer();
        final ITypeLoader typeLoader = new ClasspathTypeLoader(System.getProperty("java.class.path"));

        if (!typeLoader.tryLoadType("java/lang/Object", buffer)) {
            throw Error.couldNotLoadObjectType();
        }

        final MetadataSystem metadataSystem = MetadataSystem.instance();
        final MutableTypeDefinition object = new MutableTypeDefinition(metadataSystem);

        Object = object;

        final ClassFileReader reader = ClassFileReader.readClass(metadataSystem, buffer);
        final TypeDefinitionBuilder builder = new TypeDefinitionBuilder(metadataSystem);

        reader.accept(object, builder);
    }

    public static TypeDefinition fromPrimitiveTypeCode(final int code) {
        switch (code) {
            case 4:
                return Boolean;
            case 8:
                return Byte;
            case 9:
                return Short;
            case 10:
                return Integer;
            case 11:
                return Long;
            case 5:
                return Character;
            case 6:
                return Float;
            case 7:
                return Double;
            default:
                throw ContractUtils.unreachable();
        }
    }
}
