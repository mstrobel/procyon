package com.strobel.reflection;

import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;

/**
 * @author strobelm
 */
final class TypeResolver {

    private final TypeCache _resolvedTypes;

    TypeResolver(final TypeCache resolvedTypes) {
        _resolvedTypes = VerifyArgument.notNull(resolvedTypes, "resolvedTypes");
    }

    public Type resolve(final Class<?> rawType) {
        // with erased class, no bindings:
        return _fromClass(null, rawType, TypeBindings.empty());
    }

    private Type _fromAny(final ClassStack context, final java.lang.reflect.Type mainType, final TypeBindings typeBindings) {
        if (mainType instanceof Class<?>) {
            return _fromClass(context, (Class<?>)mainType, typeBindings);
        }
        if (mainType instanceof ParameterizedType) {
            return _fromParamType(context, (ParameterizedType)mainType, typeBindings);
        }
        if (mainType instanceof GenericArrayType) {
            return _fromArrayType(context, (GenericArrayType)mainType, typeBindings);
        }
        if (mainType instanceof TypeVariable<?>) {
            return _fromVariable(context, (TypeVariable<?>)mainType, typeBindings);
        }
        if (mainType instanceof WildcardType) {
            return _fromWildcard(context, (WildcardType)mainType, typeBindings);
        }
        // should never getBoundType here...
        throw new IllegalArgumentException("Unrecognized type class: " + mainType.getClass().getName());
    }

    private Type _fromClass(final ClassStack context, final Class<?> rawType, final TypeBindings typeBindings) {
        ClassStack c = context;

        // First: a primitive type perhaps?
        if (rawType.isPrimitive()) {
            return Type.of(rawType);
        }

        // Second: recursive reference?
        if (c == null) {
            c = new ClassStack(rawType);
        }
        else {
            final ClassStack prev = c.find(rawType);
            if (prev != null) {
                // Self-reference: needs special handling, then...
                final RecursiveType selfRef = new RecursiveType(rawType, typeBindings);
                prev.addSelfReference(selfRef);
                return selfRef;
            }
            // no, can just add
            c = c.child(rawType);
        }

        // If not, already recently resolved?
        final TypeList typeParameters = typeBindings.getBoundTypes();
        final TypeCache.Key key = _resolvedTypes.key(rawType, typeParameters);

        Type type = _resolvedTypes.find(key);

        if (type == null) {
            type = _constructType(c, rawType, typeBindings);
            _resolvedTypes.put(key, type);
        }

        c.resolveSelfReferences(type);
        return type;
    }

    private Type _fromParamType(final ClassStack context, final ParameterizedType parameterizedType, final TypeBindings parentBindings) {
        /* First: what is the actual base type? One odd thing is that 'getRawType'
         * returns Type, not Class<?> as one might expect. But let's assume it is
         * always of type Class: if not, need to add more code to resolve it...
         */
        final Class<?> rawType = (Class<?>)parameterizedType.getRawType();
        final java.lang.reflect.Type[] params = parameterizedType.getActualTypeArguments();
        final int length = params.length;
        final Type[] types = new Type[length];

        for (int i = 0; i < length; ++i) {
            types[i] = _fromAny(context, params[i], parentBindings);
        }

        Type declaringType = _resolvedTypes.find(_resolvedTypes.key(rawType));

        if (declaringType == null) {
            declaringType = _fromAny(context, rawType, parentBindings);
        }

        // Ok: this gives us current bindings for this type:
/*
        TypeBindings newBindings = parentBindings;
        final TypeList genericTypeParameters = declaringType.getGenericTypeParameters();
        for (int i = 0, n = genericTypeParameters.size(); i < n; i++) {

            final Type binding = types[i];
            final Type typeParameter = genericTypeParameters.get(i);
            final Type existingParameter = parentBindings.findGenericParameter(typeParameter.getName());

            if (existingParameter != null) {
                newBindings = newBindings.withAdditionalBinding(typeParameter, existingParameter);

                if (!binding.isGenericParameter()) {
                    newBindings = newBindings.withAdditionalBinding(existingParameter, binding);
                }
            }
            else {
                newBindings = newBindings.withAdditionalBinding(typeParameter, binding);
            }
        }
*/
        final TypeBindings newBindings = TypeBindings.create(declaringType.getGenericTypeParameters(), types);
        return _fromClass(context, rawType, newBindings);
    }

    private Type _fromArrayType(final ClassStack context, final GenericArrayType arrayType, final TypeBindings typeBindings) {
        final Type elementType = _fromAny(context, arrayType.getGenericComponentType(), typeBindings);
        // Figuring out raw class for generic array is actually bit tricky...
        return new ArrayType(elementType);
    }

    private Type _fromWildcard(final ClassStack context, final WildcardType wildType, final TypeBindings typeBindings) {
        /* Similar to challenges with TypeVariable, we may have multiple upper bounds.
         * But it is also possible that if upper bound defaults to Object, we might want to
         * consider lower bounds instead?
         * For now, we won't try anything more advanced; above is just for future reference.
         */
        return _fromAny(context, wildType.getUpperBounds()[0], typeBindings);
    }

    private Type _fromVariable(final ClassStack context, final TypeVariable<?> variable, final TypeBindings typeBindings) {
        // ideally should find it via bindings:
        final String name = variable.getName();
        final Type type = typeBindings.findBoundType(name);

        if (type != null) {
            return type;
        }

/*
        final TypeBindings newBindings;
        final Type genericParameter = typeBindings.findGenericParameter(name);

        if (genericParameter != null) {
            newBindings = typeBindings.withAdditionalBinding(
                genericParameter,
                Type.Object
            );
        }
        else {
            newBindings = typeBindings;
        }
*/

        final java.lang.reflect.Type[] bounds = variable.getBounds();
        return _fromAny(context, bounds[0], typeBindings);
    }

    private Type _constructType(final ClassStack context, final Class<?> rawType, final TypeBindings typeBindings) {
        // Ok: no easy shortcut, let's figure out type of type...
        if (rawType.isArray()) {
            final Type elementType = _fromAny(context, rawType.getComponentType(), typeBindings);
            return new ArrayType(elementType);
        }

        final Type openType;

        final TypeVariable<? extends Class<?>>[] typeParameters = rawType.getTypeParameters();
        final RecursiveType selfReference;
        final TypeList genericParameterList;

        TypeBindings newBindings = typeBindings;

        if (typeParameters.length != 0) {
            selfReference = new RecursiveType(rawType, newBindings);
            context.addSelfReference(selfReference);
            final Type[] genericParameters = new Type[typeParameters.length];
            for (int i = 0, n = typeParameters.length; i < n; i++) {
                final TypeVariable<? extends Class<?>> typeParameter = typeParameters[i];

                genericParameters[i] = typeBindings.findGenericParameter(typeParameter.getName());

                if (genericParameters[i] == null) {
                    genericParameters[i] = new GenericParameterType(typeParameter, selfReference, i);
                }
            }

            genericParameterList = Type.list(genericParameters);
        }
        else {
            genericParameterList = TypeList.empty();
        }

        // For other types super interfaces are needed...
        if (rawType.isInterface()) {
            return new ReflectedType<>(
                rawType,
                newBindings.bindingsFor(genericParameterList),
                Type.Object,
                _resolveSuperInterfaces(context, rawType, newBindings)
            );
        }

        return new ReflectedType<>(
            rawType,
            newBindings.bindingsFor(genericParameterList),
            _resolveSuperClass(context, rawType, newBindings),
            _resolveSuperInterfaces(context, rawType, newBindings)
        );
    }

    TypeList _resolveSuperInterfaces(final ClassStack context, final Class<?> rawType, final TypeBindings typeBindings) {
        final java.lang.reflect.Type[] types = rawType.getGenericInterfaces();

        if (types == null || types.length == 0) {
            return TypeList.empty();
        }

        final int length = types.length;
        final Type[] resolved = new Type[length];

        for (int i = 0; i < length; ++i) {
            resolved[i] = _fromAny(context, types[i], typeBindings);
        }

        return new TypeList(resolved);
    }

    Type _resolveSuperClass(final ClassStack context, final Class<?> rawType, final TypeBindings typeBindings) {
        final java.lang.reflect.Type parent = rawType.getGenericSuperclass();

        if (parent == null) {
            return null;
        }

        // can this ever be something other than class? (primitive, array)
        return _fromAny(context, parent, typeBindings);
    }

    private static final class RecursiveType extends Type {
        private Type _referencedType;
        private final Class<?> _rawType;
        private final TypeBindings _typeBindings;

        public RecursiveType(final Class<?> rawType, final TypeBindings typeBindings) {
            _rawType = VerifyArgument.notNull(rawType, "rawType");
            _typeBindings = VerifyArgument.notNull(typeBindings, "typeBindings");
        }

        public void setReference(final Type referencedType) {
            _referencedType = VerifyArgument.notNull(referencedType, "referencedType");
        }

        @Override
        public FieldInfo getField(final String name, final int bindingFlags) {
            return _referencedType.getField(name, bindingFlags);
        }

        @Override
        public MemberList<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType[] memberTypes) {
            return _referencedType.getMember(name, bindingFlags, memberTypes);
        }

        @Override
        public MethodInfo getMethod(final String name, final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
            return _referencedType.getMethod(name, bindingFlags, callingConvention, parameterTypes);
        }

        @Override
        public ConstructorInfo getConstructor(final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes) {
            return _referencedType.getConstructor(bindingFlags, callingConvention, parameterTypes);
        }

        @Override
        public MemberList<? extends MemberInfo> getMembers(final int bindingFlags) {
            return _referencedType.getMembers(bindingFlags);
        }

        @Override
        public FieldList getFields(final int bindingFlags) {
            return _referencedType.getFields(bindingFlags);
        }

        @Override
        public MethodList getMethods(final int bindingFlags, final CallingConvention callingConvention) {
            return _referencedType.getMethods(bindingFlags, callingConvention);
        }

        @Override
        public ConstructorList getConstructors(final int bindingFlags) {
            return _referencedType.getConstructors(bindingFlags);
        }

        @Override
        public TypeList getNestedTypes(final int bindingFlags) {
            return _referencedType.getNestedTypes(bindingFlags);
        }

        @Override
        public Class<?> getErasedClass() {
            return _referencedType.getErasedClass();
        }

        @Override
        public TypeBindings getTypeBindings() {
            return _referencedType.getTypeBindings();
        }

        @Override
        public Type getGenericTypeDefinition() {
            return _referencedType.getGenericTypeDefinition();
        }

        @Override
        public boolean containsGenericParameters() {
            return _referencedType.containsGenericParameters();
        }

        @Override
        public TypeList getGenericParameterConstraints() {
            return _referencedType.getGenericParameterConstraints();
        }

        @Override
        public boolean isEquivalentTo(final Type other) {
            return _referencedType.isEquivalentTo(other);
        }

        @Override
        public boolean isSubclassOf(final Type type) {
            return _referencedType.isSubclassOf(type);
        }

        @Override
        public boolean isInstance(final Object o) {
            return _referencedType.isInstance(o);
        }

        @Override
        public boolean implementsInterface(final Type interfaceType) {
            return _referencedType.implementsInterface(interfaceType);
        }

        @Override
        public Type getElementType() {
            return _referencedType.getElementType();
        }

        @Override
        public int getGenericParameterPosition() {
            return _referencedType.getGenericParameterPosition();
        }

        @Override
        public MethodInfo getDeclaringMethod() {
            return _referencedType.getDeclaringMethod();
        }

        @Override
        public TypeList getInterfaces() {
            return _referencedType.getInterfaces();
        }

        @Override
        public Type getBaseType() {
            return _referencedType.getBaseType();
        }

        @Override
        public boolean hasElementType() {
            return _referencedType.hasElementType();
        }

        @Override
        public boolean isPrimitive() {
            return _referencedType.isPrimitive();
        }

        @Override
        public boolean isGenericParameter() {
            if (_referencedType == null) {
                return false;
            }
            return _referencedType.isGenericParameter();
        }

        @Override
        public boolean isGenericTypeDefinition() {
            return _referencedType.isGenericTypeDefinition();
        }

        @Override
        public boolean isGenericType() {
            return _referencedType.isGenericType();
        }

        @Override
        public boolean isArray() {
            return _referencedType.isArray();
        }

        @Override
        public boolean isVisible() {
            return _referencedType.isVisible();
        }

        @Override
        public boolean isNested() {
            return _referencedType.isNested();
        }

        @Override
        public boolean isAssignableFrom(final Type type) {
            return _referencedType.isAssignableFrom(type);
        }

        @Override
        public Object[] getEnumConstants() {
            return _referencedType.getEnumConstants();
        }

        @Override
        public String[] getEnumNames() {
            return _referencedType.getEnumNames();
        }

        @Override
        public Type makeGenericTypeCore(final TypeList typeArguments) {
            return _referencedType.makeGenericTypeCore(typeArguments);
        }

        @Override
        public Type makeArrayType() {
            return _referencedType.makeArrayType();
        }

        @Override
        public Type getRootElementType() {
            return _referencedType.getRootElementType();
        }

        @Override
        public StringBuilder _appendClassSignature(final StringBuilder sb) {
            return _referencedType._appendClassSignature(sb);
        }

        @Override
        public StringBuilder _appendClassDescription(final StringBuilder sb) {
            return _referencedType._appendClassDescription(sb);
        }

        @Override
        public StringBuilder _appendErasedClassSignature(final StringBuilder sb) {
            return _referencedType._appendErasedClassSignature(sb);
        }

        @Override
        public StringBuilder _appendClassName(final StringBuilder sb, final boolean dottedName) {
            return _referencedType._appendClassName(sb, dottedName);
        }

        @Override
        public MemberType getMemberType() {
            return _referencedType.getMemberType();
        }

        @Override
        public Type getDeclaringType() {
            return _referencedType.getDeclaringType();
        }

        @Override
        public int getModifiers() {
            return _referencedType.getModifiers();
        }

        @Override
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
            return _referencedType.isAnnotationPresent(annotationClass);
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
            return _referencedType.getAnnotation(annotationClass);
        }

        @Override
        public Annotation[] getAnnotations() {
            return _referencedType.getAnnotations();
        }

        @Override
        public Annotation[] getDeclaredAnnotations() {
            return _referencedType.getDeclaredAnnotations();
        }
    }

    final static class ClassStack {
        private final ClassStack _parent;
        private final Class<?> _current;

        private ArrayList<RecursiveType> _selfRefs;

        public ClassStack(final Class<?> rootType) {
            this(null, rootType);
        }

        private ClassStack(final ClassStack parent, final Class<?> curr) {
            _parent = parent;
            _current = curr;
        }

        public ClassStack child(final Class<?> cls) {
            return new ClassStack(this, cls);
        }

        public void addSelfReference(final RecursiveType ref) {
            if (_selfRefs == null) {
                _selfRefs = new ArrayList<>();
            }
            _selfRefs.add(ref);
        }

        public void resolveSelfReferences(final Type resolved) {
            if (_selfRefs != null) {
                for (final RecursiveType ref : _selfRefs) {
                    ref.setReference(resolved);
                }
            }
        }

        public ClassStack find(final Class<?> cls) {
            if (_current == cls) {
                return this;
            }
            if (_parent != null) {
                return _parent.find(cls);
            }
            return null;
        }
    }
}

