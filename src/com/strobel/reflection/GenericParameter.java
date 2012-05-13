package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

/**
 * @author Mike Strobel
 */
final class GenericParameter<T> extends Type<T> {
    private final String _name;
    private final int _position;
    private final TypeList _constraints;
    private MethodInfo _declaringMethod;
    private Type _declaringType;
    private Class<T> _erasedClass;
    private TypeVariable<?> _typeVariable;

    GenericParameter(final String name, final Type declaringType, final TypeList constraints, final int position) {
        _name = VerifyArgument.notNull(name, "name");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _constraints = VerifyArgument.notNull(constraints, "constraints");
        _position = position;
    }

    GenericParameter(final String name, final MethodInfo declaringMethod, final TypeList constraints, final int position) {
        _name = VerifyArgument.notNull(name, "name");
        _declaringType = null;
        _declaringMethod = VerifyArgument.notNull(declaringMethod, "declaringMethod");
        _constraints = VerifyArgument.notNull(constraints, "constraints");
        _position = position;
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
        if (_constraints.size() == 1) {
            return _constraints.get(0).getErasedClass();
        }

        for (final Type<?> type : _constraints) {
            if (!type.isInterface()) {
                return type.getErasedClass();
            }
        }

        final ArrayList<Type> interfaceBounds = new ArrayList<>(_constraints);

        outer:
        while (interfaceBounds.size() > 1) {
            final Type<?> a = interfaceBounds.get(0);

            for (int i = 1, n = interfaceBounds.size(); i < n; i++) {
                final Type<?> b = interfaceBounds.get(i);
                final Type moreSpecific = getMostSpecificType(a, b);

                if (moreSpecific == null) {
                    return Object.class;
                }

                if (moreSpecific == a) {
                    interfaceBounds.remove(0);
                    continue outer;
                }

                interfaceBounds.remove(i--);
            }
        }

        if (interfaceBounds.size() == 1) {
            return interfaceBounds.get(0).getErasedClass();
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
    public MemberList<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType[] memberTypes) {
        return MemberList.empty();
    }

    @Override
    public FieldInfo getField(final String name, final int bindingFlags) {
        return null;
    }

    @Override
    public MethodInfo getMethod(final String name, final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return null;
    }

    @Override
    public ConstructorInfo getConstructor(final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return null;
    }

    @Override
    public MemberList<? extends MemberInfo> getMembers(final int bindingFlags) {
        return MemberList.empty();
    }

    @Override
    public FieldList getFields(final int bindingFlags) {
        return FieldList.empty();
    }

    @Override
    public MethodList getMethods(final int bindingFlags, final CallingConvention callingConvention) {
        return MethodList.empty();
    }

    @Override
    public ConstructorList getConstructors(final int bindingFlags) {
        return ConstructorList.empty();
    }

    @Override
    public TypeList getNestedTypes(final int bindingFlags) {
        return TypeList.empty();
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public String getName() {
        return _typeVariable.getName();
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append(getName());
    }

    @Override
    public StringBuilder appendFullDescription(final StringBuilder sb) {
        return sb.append(getName());
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
    int getModifiers() {
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
        return getName();
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitTypeParameter(this, parameter);
    }
}
