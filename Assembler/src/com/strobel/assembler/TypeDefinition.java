package com.strobel.assembler;

import com.strobel.reflection.Types;

import java.util.Collections;
import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:51 PM
 */
public abstract class TypeDefinition extends TypeReference {
    public TypeReference getBaseType() {
        if (this == BuiltinTypes.Object) {
            return null;
        }
        return BuiltinTypes.Object;
    }

    public List<TypeReference> getExplicitInterfaces() {
        return Collections.emptyList();
    }

    // <editor-fold defaultstate="collapsed" desc="Members">

    public abstract List<FieldDefinition> getDeclaredFields();
    public abstract List<MethodDefinition> getDeclaredMethods();
    public abstract List<TypeDefinition> getDeclaredTypes();

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = super.appendGenericSignature(sb);

        final TypeReference baseType = getBaseType();

        if (baseType != null) {
            s.append(" extends ");
            s = baseType.appendBriefDescription(s);
        }

        final List<TypeReference> interfaces = getExplicitInterfaces();
        final int interfaceCount = interfaces.size();

        if (interfaceCount > 0) {
            s.append(" implements ");
            for (int i = 0; i < interfaceCount; ++i) {
                if (i != 0) {
                    s.append(",");
                }
                s = interfaces.get(i).appendBriefDescription(s);
            }
        }

        return s;
    }

    @Override
    public StringBuilder appendGenericSignature(final StringBuilder sb) {
        StringBuilder s = super.appendGenericSignature(sb);

        final TypeReference baseType = getBaseType();
        final List<TypeReference> interfaces = getExplicitInterfaces();

        if (baseType == null) {
            if (interfaces.isEmpty()) {
                s = Types.Object.appendSignature(s);
            }
        }
        else {
            s = baseType.appendSignature(s);
        }

        for (final TypeReference interfaceType : interfaces) {
            s = interfaceType.appendSignature(s);
        }

        return s;
    }

    // </editor-fold>

    @Override
    public TypeDefinition resolve() {
        return this;
    }
}
