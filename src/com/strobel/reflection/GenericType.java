package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;

/**
 * @author strobelm
 */
class GenericType extends Type {

    private final Type _genericTypeDefinition;
    private final TypeCollection _typeArguments;

    GenericType(final Type genericTypeDefinition, final TypeCollection typeArguments) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");
        _typeArguments = VerifyArgument.notNull(typeArguments, "typeArguments");
    }

    GenericType(final Type genericTypeDefinition, final Type... typeArguments) {
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");
        
        VerifyArgument.notNull(typeArguments, "typeArguments");

        if (!genericTypeDefinition.isGenericTypeDefinition()) {
            throw Error.notGenericTypeDefinition(genericTypeDefinition);
        }

        final TypeCollection genericParameters = genericTypeDefinition.getGenericParameters();
        
        if (typeArguments.length != genericParameters.size()) {
            throw Error.incorrectNumberOfTypeArguments(genericTypeDefinition);
        }
        
        final Type[] actualArguments = new Type[genericParameters.size()];
        
        for (int i = 0, n = actualArguments.length; i < n; i++) {
            final Type providedArgument = typeArguments[i];
            if (providedArgument != null) {
                actualArguments[i] = providedArgument;
            }
            else {
                actualArguments[i] = genericParameters.get(i);
            }
        }
        
        _typeArguments = new TypeCollection(actualArguments);
    }

    @Override
    public Class<?> getErasedClass() {
        return _genericTypeDefinition.getErasedClass();
    }

    @Override
    public Type getGenericTypeDefinition() {
        return _genericTypeDefinition;
    }

    @Override
    public MemberCollection<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType... memberTypes) {
        return _genericTypeDefinition.getMember(name, bindingFlags, memberTypes);
    }

    @Override
    public FieldInfo getField(final String name, final int bindingFlags) {
        return _genericTypeDefinition.getField(name, bindingFlags);
    }

    @Override
    public MethodInfo getMethod(final String name, final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return _genericTypeDefinition.getMethod(name, bindingFlags, callingConvention, parameterTypes);
    }

    @Override
    public ConstructorInfo getConstructor(final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
        return _genericTypeDefinition.getConstructor(bindingFlags, callingConvention, parameterTypes);
    }

    @Override
    public MemberCollection<? extends MemberInfo> getMembers(final int bindingFlags) {
        return _genericTypeDefinition.getMembers(bindingFlags);
    }

    @Override
    public FieldCollection getFields(final int bindingFlags) {
        return _genericTypeDefinition.getFields(bindingFlags);
    }

    @Override
    public MethodCollection getMethods(final int bindingFlags, final CallingConvention callingConvention) {
        return _genericTypeDefinition.getMethods(bindingFlags, callingConvention);
    }

    @Override
    public ConstructorCollection getConstructors(final int bindingFlags) {
        return _genericTypeDefinition.getConstructors(bindingFlags);
    }

    @Override
    public TypeCollection getNestedTypes(final int bindingFlags) {
        return _genericTypeDefinition.getNestedTypes(bindingFlags);
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public Type getDeclaringType() {
        return _genericTypeDefinition.getDeclaringType();
    }

    @Override
    public final boolean isGenericType() {
        return true;
    }

    @Override
    public TypeCollection getGenericParameters() {
        return _typeArguments;
    }

    @Override
    int getModifiers() {
        return _genericTypeDefinition.getModifiers();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return _genericTypeDefinition.isAnnotationPresent(annotationClass);
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return _genericTypeDefinition.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return _genericTypeDefinition.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return _genericTypeDefinition.getDeclaredAnnotations();
    }
}
