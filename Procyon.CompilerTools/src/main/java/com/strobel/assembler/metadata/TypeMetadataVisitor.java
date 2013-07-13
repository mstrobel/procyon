package com.strobel.assembler.metadata;

/**
 * @author Mike Strobel
 */
public interface TypeMetadataVisitor<P, R> {
    R visitArrayType(final ArrayType t, final P p);
    R visitGenericParameter(final GenericParameter t, final P p);
    R visitWildcard(final WildcardType t, final P p);
    R visitCompoundType(final CompoundTypeReference t, final P p);
    R visitParameterizedType(final TypeReference t, final P p);
    R visitPrimitiveType(final PrimitiveType t, final P p);
    R visitClassType(final TypeReference t, final P p);
    R visitNullType(final TypeReference t, final P p);
    R visitBottomType(final TypeReference t, final P p);
    R visitRawType(final TypeReference t, final P p);
}
