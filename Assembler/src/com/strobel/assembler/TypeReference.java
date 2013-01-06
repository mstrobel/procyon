package com.strobel.assembler;

import com.strobel.core.StringUtilities;
import com.strobel.reflection.SimpleType;
import com.strobel.util.ContractUtils;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:08 PM
 */
public abstract class TypeReference extends MemberReference {
    private SimpleType _simpleType;
    private String _packageName;
    private String _fullName;

    protected TypeReference(final String packageName, final String name, final SimpleType simpleType) {
        super(name);
        _packageName = packageName != null ? packageName : StringUtilities.EMPTY;
        _simpleType = simpleType != null ? simpleType : SimpleType.Object;
    }

    @Override
    public void setName(final String name) {
        super.setName(name);
        _fullName = null;
    }

    public SimpleType getSimpleType() {
        return _simpleType;
    }

    protected void setSimpleType(final SimpleType simpleType) {
        _simpleType = simpleType;
    }

    @Override
    public String getFullName() {
        if (_fullName != null) {
            return _fullName;
        }

        if (isNested()) {
            return _fullName = getDeclaringType().getFullName() + "$" + getName();
        }

        if (StringUtilities.isNullOrEmpty(_packageName)) {
            return _fullName = getName();
        }

        return _fullName = _packageName + "." + getName();
    }

    @Override
    public String getQualifiedMemberName() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType == null) {
            return getFullName();
        }

        return declaringType.getFullName() + "$" + getFullName();
    }

    public String getPackageName() {
        return _packageName;
    }

    public void setPackageName(final String packageName) {
        _packageName = packageName;
    }

    @Override
    public void setDeclaringType(final TypeReference declaringType) {
        super.setDeclaringType(declaringType);
        _fullName = null;
    }

    public boolean isNested() {
        return getDeclaringType() != null;
    }

    public boolean isArray() {
        return getSimpleType() == SimpleType.Array;
    }

    public boolean isPrimitive() {
        return getSimpleType().isPrimitive();
    }

    public boolean isSentinel() { return false; }

    public boolean isGenericParameter() { return false; }

    public boolean isGenericType() { return false; }

    public boolean isGenericTypeDefinition() { return false; }

    public TypeReference getElementType() { return this; }

    public TypeDefinition resolve() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType == null)
            throw ContractUtils.unsupported();

        return declaringType.resolve(this);
    }

    public FieldDefinition resolve(final FieldReference field) {
        throw ContractUtils.unsupported();
    }

    public MethodDefinition resolve(final MethodReference method) {
        throw ContractUtils.unsupported();
    }

    public TypeDefinition resolve(final TypeReference type) {
        throw ContractUtils.unsupported();
    }
}
