package com.strobel.assembler;

import com.strobel.core.StringUtilities;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 4:38 PM
 */
public abstract class MemberReference {
    private String _name;
    private TypeReference _declaringType;

    protected MemberReference() {
    }

    protected MemberReference(final String name) {
        _name = name != null ? name : StringUtilities.EMPTY;
    }

    public String getName() {
        return _name;
    }

    public void setName(final String name) {
        _name = name;
    }

    public boolean isDefinition() {
        return false;
    }

    protected boolean containsGenericParameters() {
        return _declaringType != null &&
               _declaringType.containsGenericParameters();
    }

    public TypeReference getDeclaringType() {
        return _declaringType;
    }

    public void setDeclaringType(final TypeReference declaringType) {
        _declaringType = declaringType;
    }

    public abstract String getFullName();

    public String getQualifiedMemberName() {
        if (_declaringType == null) {
            return getFullName();
        }

        return _declaringType.getFullName() + "." + getFullName();
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
