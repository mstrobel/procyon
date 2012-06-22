package com.strobel.reflection;

import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;

/**
 * @author Mike Strobel
 */
class GenericParameter<T> extends Type<T> {
    private final String _name;
    private final int _position;
    private final Type _upperBound;
    private final Type _lowerBound;
    private MethodInfo _declaringMethod;
    private Type _declaringType;
    private Class<T> _erasedClass;
    private TypeVariable<?> _typeVariable;

    GenericParameter(final String name, final Type declaringType, final Type upperBound, final int position) {
        _name = VerifyArgument.notNull(name, "name");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _upperBound = upperBound != null ? upperBound : Types.Object;
        _lowerBound = NoType;
        _position = position;
    }

    GenericParameter(final String name, final MethodInfo declaringMethod, final Type upperBound, final int position) {
        _name = VerifyArgument.notNull(name, "name");
        _declaringType = null;
        _declaringMethod = VerifyArgument.notNull(declaringMethod, "declaringMethod");
        _upperBound = upperBound != null ? upperBound : Types.Object;
        _lowerBound = NoType;
        _position = position;
    }

    protected GenericParameter(final String name, final Type declaringType, final Type upperBound, final Type lowerBound, final int position) {
        _name = VerifyArgument.notNull(name, "name");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _upperBound = upperBound != null ? upperBound : Types.Object;
        _lowerBound = lowerBound != null ? lowerBound : Type.NoType;
        _position = position;
    }

    protected GenericParameter(final String name, final MethodInfo declaringMethod, final Type upperBound, final Type lowerBound, final int position) {
        _name = VerifyArgument.notNull(name, "name");
        _declaringType = null;
        _declaringMethod = VerifyArgument.notNull(declaringMethod, "declaringMethod");
        _upperBound = upperBound != null ? upperBound : Types.Object;
        _lowerBound = lowerBound != null ? lowerBound : Type.NoType;
        _position = position;
    }

    @Override
    public TypeList getExplicitInterfaces() {
        return TypeList.empty();
    }

    private TypeVariable<?> resolveTypeVariable() {
        for (final TypeVariable typeVariable : _declaringType.getErasedClass().getTypeParameters()) {
            if (_name.equals(typeVariable.getName())) {
                return typeVariable;
            }
        }
        throw Error.couldNotResolveType(_name);
    }

    private Class<?> resolveErasedClass() {
        if (_upperBound != Types.Object) {
            return _upperBound.getErasedClass();
        }

        return Object.class;
    }

    public TypeVariable<?> getRawTypeVariable() {
        if (_typeVariable == null) {
            synchronized (CACHE_LOCK) {
                if (_typeVariable == null) {
                    _typeVariable = resolveTypeVariable();
                }
            }
        }
        return _typeVariable;
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public String getFullName() {
        return _name;
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        sb.append(getFullName());

        final Type<?> upperBound = getUpperBound();

        if (upperBound != null && upperBound != Types.Object) {
            sb.append(" extends ");
            if (upperBound.isGenericParameter()) {
                return sb.append(upperBound.getFullName());
            }
            return upperBound.appendBriefDescription(sb);
        }

        return sb;
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        sb.append(getFullName());

        final Type<?> upperBound = getUpperBound();

        if (upperBound != null && upperBound != Types.Object) {
            sb.append(" extends ");
            if (upperBound.isGenericParameter()) {
                return sb.append(upperBound.getName());
            }
            return upperBound.appendSimpleDescription(sb);
        }

        return sb;
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        return getUpperBound().appendErasedDescription(sb);
    }

    @Override
    public StringBuilder appendFullDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    @Override
    protected final StringBuilder _appendClassName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return sb.append(_name);
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public MethodInfo getDeclaringMethod() {
        return _declaringMethod;
    }

    public void setDeclaringMethod(final MethodInfo declaringMethod) {
        _declaringMethod = declaringMethod;
    }

    public void setDeclaringType(final Type declaringType) {
        _declaringType = declaringType;
    }

    @Override
    public boolean isGenericParameter() {
        return true;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }

    @Override
    public Type<?> getLowerBound() {
        return _lowerBound;
    }

    @Override
    public Type<?> getUpperBound() {
        return _upperBound;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> getErasedClass() {
        if (_erasedClass == null) {
            synchronized (CACHE_LOCK) {
                if (_erasedClass == null) {
                    _erasedClass = (Class<T>)resolveErasedClass();
                }
            }
        }
        return _erasedClass;
    }

    @Override
    public int getGenericParameterPosition() {
        return _position;
    }

    @Override
    public int getModifiers() {
        return 0;
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return new Annotation[0];
    }

    @Override
    public String toString() {
        return getFullName();
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitTypeParameter(this, parameter);
    }

    @Override
    public int hashCode() {
        return getGenericParameterPosition();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (obj == null || !(obj instanceof Type<?>)) {
            return false;
        }
        
        final Type<?> other = (Type<?>) obj;

        if (!other.isGenericParameter() ||
            other.getGenericParameterPosition() != this.getGenericParameterPosition()) {
            
            return false;
        }
        
        if (_declaringMethod != null) {
            final MethodInfo otherDeclaringMethod = other.getDeclaringMethod();
            return otherDeclaringMethod != null &&
                   Comparer.equals(_declaringMethod.getRawMethod(), otherDeclaringMethod.getRawMethod());
                   
        }

        final Type<?> otherDeclaringType = other.getDeclaringType();
        
        return otherDeclaringType != null &&
               otherDeclaringType.isEquivalentTo(_declaringType);
    }
}
