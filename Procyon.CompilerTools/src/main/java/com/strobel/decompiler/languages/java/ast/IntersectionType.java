package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.CompoundTypeReference;
import com.strobel.assembler.metadata.ICompoundType;
import com.strobel.assembler.metadata.IMetadataResolver;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IntersectionType extends AstType {
    public final static TokenRole INTERSECTION_TOKEN = new TokenRole("&", TokenRole.FLAG_OPERATOR);

    public IntersectionType(final AstType baseType, final AstType... interfaceTypes) {
        setBaseType(baseType);

        for (final AstType ifType : interfaceTypes) {
            addChild(ifType, Roles.IMPLEMENTED_INTERFACE);
        }
    }

    public final AstType getBaseType() {
        return getChildByRole(Roles.BASE_TYPE);
    }

    public final void setBaseType(final AstType value) {
        setChildByRole(Roles.BASE_TYPE, value);
    }

    public final AstNodeCollection<AstType> getInterfaces() {
        return getChildrenByRole(Roles.IMPLEMENTED_INTERFACE);
    }

    @Override
    public TypeReference toTypeReference() {
        final TypeReference baseType = getBaseType().toTypeReference();
        final AstNodeCollection<AstType> interfaces = getInterfaces();
        final List<TypeReference> interfaceTypes = interfaces.isEmpty() ? Collections.<TypeReference>emptyList() : new ArrayList<TypeReference>();

        for (final AstType ifType : interfaces) {
            interfaceTypes.add(ifType.toTypeReference());
        }

        IMetadataResolver resolver = null;

        final TypeDefinition resolved;
        final TypeReference t = getUserData(Keys.TYPE_REFERENCE);

        if (t != null) {
            if (t instanceof ICompoundType) {
                resolver = ((ICompoundType) t).getResolver();
            }
            if (resolver == null && (resolved = t.resolve()) != null) {
                resolver = resolved.getResolver();
            }
        }

        return new CompoundTypeReference(baseType, interfaceTypes, resolver);
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitIntersectionType(this, data);
    }

    @Override
    public String toString() {
        final AstNodeCollection<AstType> ifTypes = getInterfaces();
        final StringBuilder sb = new StringBuilder();

        final AstType baseType = getBaseType();

        boolean needToken = true;

        if (baseType.isNull()) {
            needToken = false;
        }
        else {
            sb.append(baseType);
        }

        for (final AstType ifType : ifTypes) {
            if (needToken) {
                sb.append(" & ");
            }
            sb.append(ifType);
            needToken = true;
        }

        return sb.toString();
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        return other instanceof IntersectionType &&
               getInterfaces().matches(((IntersectionType) other).getInterfaces(), match);
    }
}
