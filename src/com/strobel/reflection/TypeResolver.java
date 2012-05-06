package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.ReadOnlyList;
import com.strobel.core.VerifyArgument;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author strobelm
 */
final class TypeResolver {

    private final TypeCache _resolvedTypes;

    TypeResolver(final TypeCache resolvedTypes) {
        _resolvedTypes = VerifyArgument.notNull(resolvedTypes, "resolvedTypes");
    }

    public Type resolve(final java.lang.reflect.Type jdkType, final TypeBindings typeBindings) {
        return _fromAny(null, jdkType, typeBindings);
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

        final java.lang.reflect.Type[] bounds = variable.getBounds();

        final TypeBindings newBindings;
        final Type genericParameter = typeBindings.findGenericParameter(name);

        if (genericParameter != null) {
            newBindings = typeBindings.withAdditionalBinding(
                genericParameter,
                Type.Object
            );
        }
        else {
            final int ordinal = ArrayUtilities.indexOf(variable.getGenericDeclaration().getTypeParameters(), variable);
            newBindings = typeBindings.withAdditionalParameter(
                new TypePlaceHolder(ordinal, variable.getName())
            );
        }

        return _fromAny(context, bounds[0], newBindings);
    }

    private Type _constructType(final ClassStack context, final Class<?> rawType, final TypeBindings typeBindings) {
        // Ok: no easy shortcut, let's figure out type of type...
        if (rawType.isArray()) {
            final Type elementType = _fromAny(context, rawType.getComponentType(), typeBindings);
            return new ArrayType(elementType);
        }

        final TypeVariable<? extends Class<?>>[] typeParameters = rawType.getTypeParameters();
        final RecursiveType selfReference;
        final TypeList genericParameterList;

        if (typeParameters.length != 0) {
            selfReference = new RecursiveType(rawType, typeBindings);
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
                typeBindings.bindingsFor(genericParameterList),
                Type.Object,
                _resolveSuperInterfaces(context, rawType, typeBindings)
            );
        }

        return new ReflectedType<>(
            rawType,
            typeBindings.bindingsFor(genericParameterList),
            _resolveSuperClass(context, rawType, typeBindings),
            _resolveSuperInterfaces(context, rawType, typeBindings)
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
        public MemberList getMember(final String name, final int bindingFlags, final MemberType[] memberTypes) {
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
        public MemberList getMembers(final int bindingFlags) {
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
            return _rawType;
        }

        @Override
        public TypeBindings getTypeBindings() {
            return _typeBindings;
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
            return _referencedType != null &&
                   _referencedType.isGenericParameter();
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

final class ClassKey implements Comparable<ClassKey>, Serializable {
    private final String _className;
    private final Class<?> _class;
    private final int _hashCode;

    public ClassKey(final Class<?> clazz) {
        _class = clazz;
        _className = clazz.getName();
        _hashCode = _className.hashCode();
    }

    public int compareTo(final ClassKey other) {
        return _className.compareTo(other._className);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass() != getClass()) {
            return false;
        }
        final ClassKey other = (ClassKey)o;
        return other._class == _class;
    }

    @Override
    public int hashCode() { return _hashCode; }

    @Override
    public String toString() { return _className; }
}

abstract class RawMember {
    private final Type _declaringType;

    protected RawMember(final Type context) {
        _declaringType = context;
    }

    public final Type getDeclaringType() {
        return _declaringType;
    }

    public abstract Member getRawMember();

    public String getName() {
        return getRawMember().getName();
    }

    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    public boolean isProtected() {
        return Modifier.isProtected(getModifiers());
    }

    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public String toString() {
        return getName();
    }

    protected final int getModifiers() { return getRawMember().getModifiers(); }
}

final class RawField extends RawMember {
    private final Field _field;
    private final int _hashCode;

    public RawField(final Type context, final Field field) {
        super(context);
        _field = field;
        _hashCode = (_field == null ? 0 : _field.hashCode());
    }

    public Field getRawMember() {
        return _field;
    }

    public boolean isTransient() {
        return Modifier.isTransient(getModifiers());
    }

    public boolean isVolatile() {
        return Modifier.isVolatile(getModifiers());
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        final RawField other = (RawField)o;
        return (other._field == _field);
    }

    @Override
    public int hashCode() {
        return _hashCode;
    }
}

final class RawConstructor extends RawMember {
    private final Constructor<?> _constructor;
    private final int _hashCode;

    public RawConstructor(final Type context, final Constructor<?> constructor) {
        super(context);
        _constructor = constructor;
        _hashCode = (_constructor == null ? 0 : _constructor.hashCode());
    }

    public MethodKey createKey() {
        final String name = "<init>";
        final Class<?>[] argTypes = _constructor.getParameterTypes();
        return new MethodKey(name, argTypes);
    }

    @Override
    public Constructor<?> getRawMember() {
        return _constructor;
    }

    @Override
    public int hashCode() {
        return _hashCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        final RawConstructor other = (RawConstructor)o;
        return (other._constructor == _constructor);
    }
}

final class RawMethod extends RawMember {
    private final Method _method;
    private final int _hashCode;

    public RawMethod(final Type context, final Method method) {
        super(context);
        _method = method;
        _hashCode = (_method == null ? 0 : _method.hashCode());
    }

    public Method getRawMember() {
        return _method;
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public boolean isStrict() {
        return Modifier.isStrict(getModifiers());
    }

    public boolean isNative() {
        return Modifier.isNative(getModifiers());
    }

    public boolean isSynchronized() {
        return Modifier.isSynchronized(getModifiers());
    }

    public MethodKey createKey() {
        final String name = _method.getName();
        final Class<?>[] argTypes = _method.getParameterTypes();
        return new MethodKey(name, argTypes);
    }

    @Override
    public int hashCode() {
        return _hashCode;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        final RawMethod other = (RawMethod)o;
        return (other._method == _method);
    }
}

@SuppressWarnings("serial")
final class MethodKey implements Serializable {
    private static final Class<?>[] NO_CLASSES = new Class[0];
    private final String _name;
    private final Class<?>[] _argumentTypes;
    private final int _hashCode;

    public MethodKey(final String name) {
        _name = name;
        _argumentTypes = NO_CLASSES;
        _hashCode = name.hashCode();
    }

    public MethodKey(final String name, final Class<?>[] argTypes) {
        _name = name;
        _argumentTypes = argTypes;
        _hashCode = name.hashCode() + argTypes.length;
    }

    /**
     * Equality means name is the same and argument type erasures as well.
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != getClass()) {
            return false;
        }
        final MethodKey other = (MethodKey)o;
        final Class<?>[] otherArgs = other._argumentTypes;
        final int argCount = _argumentTypes.length;
        if (otherArgs.length != argCount) {
            return false;
        }
        for (int i = 0; i < argCount; ++i) {
            if (otherArgs[i] != _argumentTypes[i]) {
                return false;
            }
        }
        return _name.equals(other._name);
    }

    @Override
    public int hashCode() { return _hashCode; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(_name);
        sb.append('(');
        for (int i = 0, len = _argumentTypes.length; i < len; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(_argumentTypes[i].getName());
        }
        sb.append(')');
        return sb.toString();
    }
}

@SuppressWarnings("ALL")
final class MemberResolver {
    final class Result {
        final HierarchicType mainType;
        final ReadOnlyList<HierarchicType> types;

        Result(final HierarchicType mainType, final ReadOnlyList<HierarchicType> types) {
            this.mainType = mainType;
            this.types = types;
        }
    }

    final Result resolve(final Type mainType) {
        // First: flatten basic type hierarchy (highest to lowest precedence)
        final HashSet<ClassKey> seenTypes = new HashSet<>();
        final ArrayList<Type> types = new ArrayList<>();

        _gatherTypes(mainType, seenTypes, types);

        // Second step: inject mix-ins (keeping order from highest to lowest)
        final HierarchicType[] hTypes;
        HierarchicType mainHierarchicType = null;

        // Third step: add mix-ins (if any), reverse order (lowest to highest precedence)
        final int len = types.size();
        hTypes = new HierarchicType[len];
        for (int i = 0; i < len; ++i) {
            // false -> not a mix-in
            hTypes[i] = new HierarchicType(types.get(i), false, i);
        }
        mainHierarchicType = hTypes[0];

        // And that's about all we need to do; rest computed lazily
        return new Result(mainHierarchicType, new ReadOnlyList<>(hTypes));
    }

    private void _gatherTypes(final Type currentType, final Set<ClassKey> seenTypes, final List<Type> types) {
        // may get called with null if no parent type
        if (currentType == null) {
            return;
        }

        final Class<?> raw = currentType.getErasedClass();

        // Finally, only include first instance of an interface, so:
        final ClassKey key = new ClassKey(raw);

        if (seenTypes.contains(key)) {
            return;
        }

        // If all good so far, append
        seenTypes.add(key);
        types.add(currentType);

        /* And check superclasses; starting with interfaces. Why interfaces?
         * So that "highest" interfaces get priority; otherwise we'd recurse
         * super-class stack and actually start with the bottom. Usually makes
         * little difference, but in cases where it does this seems like the
         * correct order.
         */
        for (final Type t : currentType.getInterfaces()) {
            _gatherTypes(t, seenTypes, types);
        }

        // and then superclass
        _gatherTypes(currentType.getBaseType(), seenTypes, types);
    }
}

final class TypePlaceHolder extends Type {
    private final int _ordinal;
    private final String _name;
    private Type _actualType;

    public TypePlaceHolder(final int ordinal, final String name) {
        _ordinal = ordinal;
        _name = name;
    }

    @Override
    public String getName() {
        return _name;
    }

    public Type getActualType() {
        return _actualType;
    }

    public void setActualType(final Type actualType) {
        _actualType = actualType;
    }

    @Override
    public boolean isGenericType() {
        return false;
    }

    @Override
    public boolean isGenericTypeDefinition() {
        return false;
    }

    @Override
    public boolean isGenericParameter() {
        return true;
    }

    @Override
    public boolean hasElementType() {
        return false;
    }

    @Override
    public Type getBaseType() {
        return Object;
    }

    @Override
    public TypeList getInterfaces() {
        return TypeList.empty();
    }

    @Override
    public MethodInfo getDeclaringMethod() {
        return null;
    }

    @Override
    public Type getElementType() {
        return null;
    }

    @Override
    public int getGenericParameterPosition() {
        return _ordinal;
    }

    @Override
    public boolean isArray() { return false; }

    @Override
    public boolean isPrimitive() { return false; }

    @Override
    public Class getErasedClass() {
        return java.lang.Object.class;
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        return _appendClassSignature(sb);
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return _appendErasedClassSignature(sb);
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        sb.append('<').append(_ordinal).append('>');
        return sb;
    }

    @Override
    public StringBuilder appendFullDescription(final StringBuilder sb) {
        return appendBriefDescription(sb);
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public Type getDeclaringType() {
        return null;
    }

    @Override
    int getModifiers() {
        return 0;
    }
}
