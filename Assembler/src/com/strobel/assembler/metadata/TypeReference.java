package com.strobel.assembler.metadata;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.SimpleType;
import com.strobel.util.ContractUtils;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:08 PM
 */
public abstract class TypeReference extends MemberReference implements IGenericParameterProvider {
    private ArrayType _arrayType;

    public TypeReference() {}

    @Override
    public boolean containsGenericParameters() {
        if (this instanceof IGenericInstance) {
            final List<TypeReference> typeArguments = ((IGenericInstance) this).getTypeArguments();

            for (int i = 0, n = typeArguments.size(); i < n; i++) {
                if (typeArguments.get(i).containsGenericParameters()) {
                    return true;
                }
            }
        }

        return super.containsGenericParameters();
    }

    public String getPackageName() {
        return StringUtilities.EMPTY;
    }

    protected String getRawFullName() {
        final StringBuilder name = new StringBuilder();
        appendName(name, true, true);
        return name.toString();
    }

    public String getFullName() {
        final StringBuilder name = new StringBuilder();
        appendName(name, true, true);
        return name.toString();
    }

    public String getInternalName() {
        final StringBuilder name = new StringBuilder();
        appendName(name, true, false);
        return name.toString();
    }

    public TypeReference getUnderlyingType() {
        return this;
    }

    @Override
    public int hashCode() {
        return getErasedSignature().hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof TypeReference &&
               MetadataResolver.areEquivalent(this, (TypeReference) obj);
    }

    // <editor-fold defaultstate="collapsed" desc="Specification Factories">

    public TypeSpecification makeArrayType() {
        if (_arrayType == null) {
            synchronized (this) {
                if (_arrayType == null) {
                    _arrayType = ArrayType.create(this);
                }
            }
        }
        return _arrayType;
    }

    public TypeReference makeGenericType(final TypeReference... typeArguments) {
        VerifyArgument.notEmpty(typeArguments, "typeArguments");
        VerifyArgument.noNullElements(typeArguments, "typeArguments");

        if (isGenericDefinition()) {
            return new ParameterizedType(
                this,
                ArrayUtilities.asUnmodifiableList(typeArguments)
            );
        }

        throw Error.notGenericType(this);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type Bounds">

    public boolean isWildcardType() {
        return false;
    }

    public boolean isCompoundType() {
        return (getFlags() & Flags.COMPOUND) == Flags.COMPOUND;
    }

    public boolean isBoundedType() {
        return this.isGenericParameter() ||
               this.isWildcardType() ||
               this instanceof ICapturedType;
    }

    public boolean isUnbound() {
        return isWildcardType() &&
               BuiltinTypes.Bottom.equals(getSuperBound()) &&
               BuiltinTypes.Object.equals(getExtendsBound());
    }

    public boolean hasExtendsBound() {
        return isGenericParameter() ||
               isWildcardType() && BuiltinTypes.Bottom.equals(getSuperBound());
    }

    public boolean hasSuperBound() {
        return isWildcardType() && !BuiltinTypes.Bottom.equals(getSuperBound());
    }

    public TypeReference getExtendsBound() {
        throw ContractUtils.unsupported();
    }

    public TypeReference getSuperBound() {
        throw ContractUtils.unsupported();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type Attributes">

    public SimpleType getSimpleType() {
        return SimpleType.Object;
    }

    public final boolean isAnnotation() {
        return isInterface() &&
               (getModifiers() & MODIFIER_ANNOTATION) != 0;
    }

    public final boolean isSynthetic() {
        return (getModifiers() & MODIFIER_SYNTHETIC) != 0;
    }

    public final boolean isClass() {
        return !isPrimitive() && !isInterface() && !isEnum();
    }

    public final boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    public final boolean isEnum() {
        return (getModifiers() & MODIFIER_ENUM) != 0;
    }

    public boolean isNested() {
        return getDeclaringType() != null;
    }

    public boolean isArray() {
        return getSimpleType() == SimpleType.Array;
    }

    public boolean isPrimitive() {
        return false;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Generics">

    @Override
    public boolean hasGenericParameters() {
        return !getGenericParameters().isEmpty();
    }

    @Override
    public boolean isGenericDefinition() {
        return hasGenericParameters() &&
               isDefinition();
    }

    @Override
    public List<GenericParameter> getGenericParameters() {
        return Collections.emptyList();
    }

    public boolean isGenericParameter() {
        return getSimpleType() == SimpleType.TypeVariable;
    }

    public boolean isGenericType() {
        return hasGenericParameters();
    }

    public TypeReference getRawType() {
        if (isGenericType()) {
            throw ContractUtils.unreachable();
        }
        throw ContractUtils.unsupported();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Name and Signature Formatting">

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        final String name = getName();

        if (!fullName) {
            return sb.append(name);
        }

        final String packageName = getPackageName();

        if (StringUtilities.isNullOrEmpty(packageName)) {
            return sb.append(name);
        }

        if (dottedName) {
            return sb.append(packageName)
                     .append('.')
                     .append(name);
        }

        final int packageEnd = packageName.length();

        for (int i = 0; i < packageEnd; i++) {
            final char c = packageName.charAt(i);
            sb.append(c == '.' ? '/' : c);
        }

        sb.append('/');

        return sb.append(name);
    }

    @Override
    protected StringBuilder appendBriefDescription(final StringBuilder sb) {
        StringBuilder s = appendName(sb, true, true);

        if (this instanceof IGenericInstance) {
            final List<TypeReference> typeArguments = ((IGenericInstance) this).getTypeArguments();
            final int count = typeArguments.size();

            if (count > 0) {
                s.append('<');
                for (int i = 0; i < count; ++i) {
                    if (i != 0) {
                        s.append(", ");
                    }
                    s = typeArguments.get(i).appendBriefDescription(s);
                }
                s.append('>');
            }
        }

        return s;
    }

    @Override
    protected StringBuilder appendSimpleDescription(final StringBuilder sb) {
        StringBuilder s = appendName(sb, false, false);

        if (isGenericType()) {
            final List<? extends TypeReference> typeArguments;

            if (this instanceof IGenericInstance) {
                typeArguments = ((IGenericInstance) this).getTypeArguments();
            }
            else {
                typeArguments = getGenericParameters();
            }

            final int count = typeArguments.size();

            if (count > 0) {
                s.append('<');
                for (int i = 0; i < count; ++i) {
                    if (i != 0) {
                        s.append(", ");
                    }
                    s = typeArguments.get(i).appendSimpleDescription(s);
                }
                s.append('>');
            }

        }
        if (this instanceof IGenericInstance) {
        }

        return s;
    }

    @Override
    protected StringBuilder appendErasedDescription(final StringBuilder sb) {
        return appendName(sb, true, true);
    }

    @Override
    protected StringBuilder appendDescription(final StringBuilder sb) {
        StringBuilder s = appendName(sb, false, false);

        if (this instanceof IGenericInstance) {
            final List<TypeReference> typeArguments = ((IGenericInstance) this).getTypeArguments();
            final int count = typeArguments.size();

            if (count > 0) {
                s.append('<');
                for (int i = 0; i < count; ++i) {
                    if (i != 0) {
                        s.append(", ");
                    }
                    s = typeArguments.get(i).appendBriefDescription(s);
                }
                s.append('>');
            }
        }

        return s;
    }

    @Override
    protected StringBuilder appendSignature(final StringBuilder sb) {
        if (isGenericParameter()) {
            sb.append('T');
            sb.append(getName());
            sb.append(';');
            return sb;
        }

        return appendClassSignature(sb);
    }

    @Override
    protected StringBuilder appendErasedSignature(final StringBuilder sb) {
        if (isGenericType() && !isGenericDefinition()) {
            return getUnderlyingType().appendErasedSignature(sb);
        }
        return appendErasedClassSignature(sb);
    }

    protected StringBuilder appendGenericSignature(final StringBuilder sb) {
        StringBuilder s = sb;

        if (isGenericParameter()) {
            final TypeReference extendsBound = getExtendsBound();

            s.append(getName());

            if (extendsBound.isInterface()) {
                s.append(':');
            }

            s.append(':');
            s = extendsBound.appendSignature(s);

            return s;
        }

        if (this instanceof IGenericInstance) {
            final List<TypeReference> typeArguments = ((IGenericInstance) this).getTypeArguments();
            final int count = typeArguments.size();

            if (count > 0) {
                s.append('<');
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < count; ++i) {
                    s = typeArguments.get(i).appendGenericSignature(s);
                }
                s.append('>');
            }
        }

        return s;
    }

    protected StringBuilder appendClassSignature(final StringBuilder sb) {
        StringBuilder s = sb;

        s.append('L');
        s = appendName(s, true, false);

        if (this instanceof IGenericInstance) {
            final List<TypeReference> typeArguments = ((IGenericInstance) this).getTypeArguments();
            final int count = typeArguments.size();

            if (count > 0) {
                s.append('<');
                for (int i = 0; i < count; ++i) {
                    final TypeReference type = typeArguments.get(i);
                    if (type.isGenericDefinition()) {
                        s = type.appendErasedSignature(s);
                    }
                    else {
                        s = type.appendSignature(s);
                    }
                }
                s.append('>');
            }
        }

        s.append(';');
        return s;
    }

    protected StringBuilder appendErasedClassSignature(StringBuilder sb) {
        sb.append('L');
        sb = appendName(sb, true, false);
        sb.append(';');
        return sb;
    }

    protected StringBuilder appendClassDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        appendName(sb, true, true);

        if (this instanceof IGenericInstance) {
            final List<TypeReference> typeArguments = ((IGenericInstance) this).getTypeArguments();
            final int count = typeArguments.size();

            if (count > 0) {
                s.append('<');
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < count; ++i) {
                    s = typeArguments.get(i).appendErasedClassSignature(s);
                }
                s.append('>');
            }
        }

        return s;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Member Resolution">

    public TypeDefinition resolve() {
        final TypeReference declaringType = getDeclaringType();

        if (declaringType == null) {
            throw ContractUtils.unsupported();
        }

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

    // </editor-fold>
}
