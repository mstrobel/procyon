package com.strobel.assembler.metadata;

import com.strobel.reflection.SimpleType;

/**
 * @author Mike Strobel
 */
public final class BuiltinTypes {
    public final static TypeDefinition Object = null;
    public final static TypeDefinition Bottom = null;
    public final static TypeDefinition Boolean = new PrimitiveType(SimpleType.Boolean);
    public final static TypeDefinition Byte = new PrimitiveType(SimpleType.Byte);
    public final static TypeDefinition Character = new PrimitiveType(SimpleType.Character);
    public final static TypeDefinition Short = new PrimitiveType(SimpleType.Short);
    public final static TypeDefinition Integer = new PrimitiveType(SimpleType.Integer);
    public final static TypeDefinition Long = new PrimitiveType(SimpleType.Long);
    public final static TypeDefinition Float = new PrimitiveType(SimpleType.Float);
    public final static TypeDefinition Double = new PrimitiveType(SimpleType.Double);
    public final static TypeDefinition Void = new PrimitiveType(SimpleType.Void);
}
