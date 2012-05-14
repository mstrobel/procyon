package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import javax.lang.model.type.TypeKind;
import java.lang.annotation.Annotation;
import java.lang.reflect.TypeVariable;
import java.util.Set;

/**
 * @author Mike Strobel
 */
final class GenericParameterType<T> extends Type<T> {
    private final int _position;
    private final TypeVariable<?> _typeVariable;
    private Class<T> _erasedClass;
    private MethodInfo _declaringMethod;
    private Type<?> _declaringType;

    GenericParameterType(final TypeVariable<?> typeVariable, final int position) {
        _typeVariable = VerifyArgument.notNull(typeVariable, "typeVariable");
        _position = position;
    }
    
    GenericParameterType(final TypeVariable<?> typeVariable, final Type declaringType, final int position) {
        _typeVariable = VerifyArgument.notNull(typeVariable, "typeVariable");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _declaringMethod = null;
        _position = position;
    }

    GenericParameterType(final TypeVariable<?> typeVariable, final MethodInfo declaringMethod, final int position ) {
        _typeVariable = VerifyArgument.notNull(typeVariable, "typeVariable");
        _declaringType = null;
        _declaringMethod = VerifyArgument.notNull(declaringMethod, "declaringMethod");
        _position = position;
    }

    @SuppressWarnings("unchecked")
    private Class<T> resolveErasedClass() {
        final TypeBindings bindings;
        
        if (_declaringMethod != null) {
            if (_declaringType != null) {
                bindings = _declaringMethod.getTypeBindings().withAdditionalBindings(_declaringType.getTypeBindings());
            }
            else {
                bindings = _declaringMethod.getTypeBindings();
            }
        }
        else {
            if (_declaringType != null) {
                bindings = _declaringType.getTypeBindings();
            }
            else {
                bindings = TypeBindings.empty();
            }
        }
        
        Class classConstraint = null;
        final java.lang.reflect.Type[] bounds = _typeVariable.getBounds();
        
        if (bounds.length == 0) {
            return (Class<T>)Object.class;
        }
        
        if (bounds.length == 1) {
            return TYPE_RESOLVER.resolve(bounds[0], bindings).getErasedClass(); 
        }
        
        for (final java.lang.reflect.Type bound : bounds) {
            final Class erasedClass = TYPE_RESOLVER.resolve(bound, bindings).getErasedClass();
            
            if (erasedClass.isInterface()) {
                continue;
            }
            
            if (classConstraint == null || classConstraint.isAssignableFrom(erasedClass)) {
                classConstraint = erasedClass; 
            }
        }
        
        if (classConstraint == null) {
            return classConstraint;
        }

        return (Class<T>) java.lang.Object.class;
    }

    public TypeVariable<?> getRawTypeVariable() {
        return _typeVariable;
    }

    @Override
    public MemberList getMember(final String name, final int bindingFlags, final Set<MemberType> memberTypes) {
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
    public MemberList getMembers(final int bindingFlags, final Set<MemberType> memberTypes) {
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
    public TypeKind getKind() {
        return TypeKind.TYPEVAR;
    }

    @Override
    public Class<T> getErasedClass() {
        if (_erasedClass == null) {
            synchronized (CACHE_LOCK) {
                if (_erasedClass == null) {
                    _erasedClass = resolveErasedClass();
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
}

