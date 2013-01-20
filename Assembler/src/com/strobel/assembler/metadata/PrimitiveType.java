package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.SimpleType;

import java.util.Collections;
import java.util.List;

/**
 * @author Mike Strobel
 */
public final class PrimitiveType extends TypeDefinition {
    private final SimpleType _simpleType;

    PrimitiveType(final SimpleType simpleType) {
        _simpleType = VerifyArgument.notNull(simpleType, "simpleType");
    }

    @Override
    public String getName() {
        return _simpleType.getPrimitiveName();
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    protected String getRawFullName() {
        return _simpleType.getPrimitiveName();
    }

    @Override
    public String getFullName() {
        return _simpleType.getPrimitiveName();
    }

    @Override
    public String getInternalName() {
        return _simpleType.getDescriptorPrefix();
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    @Override
    public List<FieldDefinition> getDeclaredFields() {
        return Collections.emptyList();
    }

    @Override
    public List<MethodDefinition> getDeclaredMethods() {
        return Collections.emptyList();
    }

    @Override
    public List<TypeDefinition> getDeclaredTypes() {
        return Collections.emptyList();
    }

    @Override
    public TypeReference getDeclaringType() {
        return null;
    }

    @Override
    public long getFlags() {
        return Flags.PUBLIC;
    }

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendErasedDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendClassDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    protected StringBuilder appendErasedSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    protected StringBuilder appendClassSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    protected StringBuilder appendErasedClassSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    public StringBuilder appendGenericSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }
}
