package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.sun.tools.javac.util.List;
import sun.reflect.generics.factory.CoreReflectionFactory;
import sun.reflect.generics.factory.GenericsFactory;
import sun.reflect.generics.scope.ClassScope;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("unchecked")
public final class NewResolver {
    void resolveMembers(final ReflectedType<?> type) {
        final Frame frame = new Frame(type, null);

        this.visit(type, frame);
    }

    public final class Frame {

        private final ReflectedType<?> _type;
        private final java.lang.reflect.Type _typeElement;
        private final Frame _previous;
        private final Map<java.lang.reflect.Type, Type> _elementTypeMap;
        private final Stack<ReflectedMethod> _methods;
        private final GenericsFactory _reflectionFactory;

        private List<Type<?>> _typeArguments = com.sun.tools.javac.util.List.nil();

        public Frame(final java.lang.reflect.Type typeElement, final Frame previous) {
            GenericsFactory reflectionFactory = null;

            if (typeElement instanceof Class<?>) {
                reflectionFactory = CoreReflectionFactory.make(null, ClassScope.make((Class)typeElement));
            }
            else if (previous != null && previous._reflectionFactory != null) {
                reflectionFactory = previous._reflectionFactory;
            }

            _typeElement = VerifyArgument.notNull(typeElement, "typeElement");
            _previous = previous;
            _elementTypeMap = previous != null ? previous._elementTypeMap : new HashMap<java.lang.reflect.Type, Type>();
            _methods = previous != null ? previous._methods : new Stack<ReflectedMethod>();
            _type = new ReflectedType<>((Class<?>)typeElement);
            _elementTypeMap.put(typeElement, _type);

            if (reflectionFactory == null) {
                reflectionFactory = CoreReflectionFactory.make(null, null);
            }

            _reflectionFactory = reflectionFactory;
        }

        Frame(final ReflectedType<?> type, final Frame previous) {
            GenericsFactory reflectionFactory = null;

            _type = VerifyArgument.notNull(type, "type");
            _typeElement = type;
            _previous = previous;
            _elementTypeMap = previous != null ? previous._elementTypeMap : new HashMap<java.lang.reflect.Type, Type>();
            _methods = previous != null ? previous._methods : new Stack<ReflectedMethod>();
            _elementTypeMap.put(_typeElement, _type);

            final Class<?> classType = type.getErasedClass();
            final Class<?> enclosingClass = classType.getEnclosingClass();

            if (enclosingClass != null && enclosingClass != classType) {
                final Frame ownerFrame = findFrame(enclosingClass);

                if (ownerFrame != null) {
                    ownerFrame._type.addNestedType(_type);
                    reflectionFactory = ownerFrame._reflectionFactory;
                }
            }

            if (reflectionFactory == null) {
                reflectionFactory = CoreReflectionFactory.make(null, ClassScope.make(type.getErasedClass()));
            }

            _reflectionFactory = reflectionFactory;
        }

        public Type<?> getResult() {
            return _type;
        }

        void pushMethod(final ReflectedMethod method) {
            _methods.push(method);
        }

        ReflectedMethod popMethod() {
            return _methods.pop();
        }

        ReflectedMethod currentMethod() {
            return _methods.peek();
        }

        ReflectedType<?> getCurrentClass() {
            return _type;
        }

        java.lang.reflect.Type getCurrentType() {
            return _typeElement;
        }

        List<Type<?>> getTypeArguments() {
            return _typeArguments;
        }

        Type<?> findType(final java.lang.reflect.Type e) {
            Frame currentFrame = this;

            while (currentFrame != null) {
                Type result = currentFrame._elementTypeMap.get(e);

                if (result != null) {
                    return result;
                }

                if (e instanceof java.lang.reflect.TypeVariable) {
                    final TypeVariable typeVariable = (TypeVariable)e;

                    result = currentFrame._type.findGenericParameter(typeVariable);

                    if (result != null) {
                        return result;
                    }

                    if (!_typeArguments.isEmpty()) {
                        for (final Type<?> typeArgument : _typeArguments) {
                            if (!(typeArgument instanceof GenericParameter)) {
                                continue;
                            }
                            if (((GenericParameter)typeArgument).getRawTypeVariable() == typeVariable) {
                                return typeArgument;
                            }
                        }
                    }
                }

                currentFrame = currentFrame._previous;
            }

            return null;
        }

        Type<?> resolveType(final java.lang.reflect.Type t) {
            final Type<?> result = resolveTypeCore(t);

/*
            if (result != null && result.isGenericType()) {
                if (t instanceof Class<?> &&
                    ArrayUtilities.isNullOrEmpty(((Class)t).getTypeParameters())) {
                    return result.getErasedType();
                }
            }
*/

            if (result != null) {
                _elementTypeMap.put(t, result);
                Type.CACHE.add(result);
            }

            return result;
        }

        private Type<?> resolveTypeCore(final java.lang.reflect.Type t) {
            final Type<?> fromMap = findType(t);

            if (fromMap != null) {
                return fromMap;
            }

            if (t instanceof Class<?>) {
                final Class<?> classType = (Class<?>)t;

                if (classType.isPrimitive() || classType.isEnum() || classType == Void.TYPE) {
                    return Type.of(classType);
                }

                if (classType.isArray()) {
                    final Type<?> componentType = resolveType(classType.getComponentType());

                    if (componentType != null) {
                        return componentType.makeArrayType();
                    }

                    return null;
                }

                final Type cachedType = Type.CACHE.find((Class<?>)t);

                if (cachedType != null) {
                    return cachedType;
                }

                return visit(classType, this);
            }

            if (t instanceof GenericArrayType) {
                final java.lang.reflect.Type genericComponentType = ((GenericArrayType)t).getGenericComponentType();
                final Type<?> componentType = resolveType(genericComponentType);

                if (componentType != null) {
                    return componentType.makeArrayType();
                }

                return null;
            }

            if (t instanceof java.lang.reflect.WildcardType) {
                final java.lang.reflect.WildcardType w = (java.lang.reflect.WildcardType)t;
                final java.lang.reflect.Type[] upperBounds = w.getUpperBounds();
                final java.lang.reflect.Type[] lowerBounds = w.getLowerBounds();

                List<Type<?>> resolvedUpperBounds = List.nil();
                List<Type<?>> resolvedLowerBounds = List.nil();

                for (final java.lang.reflect.Type lowerBound : lowerBounds) {
                    final Type<?> resolvedLowerBound = resolveType(lowerBound);

                    if (resolvedLowerBound != null) {
                        resolvedLowerBounds = resolvedLowerBounds.append(resolvedLowerBound);
                    }
                }

                if (resolvedLowerBounds.size() != lowerBounds.length) {
                    return null;
                }

                for (final java.lang.reflect.Type upperBound : upperBounds) {
                    final Type<?> resolvedUpperBound = resolveType(upperBound);

                    if (resolvedUpperBound != null) {
                        resolvedUpperBounds = resolvedUpperBounds.append(resolvedUpperBound);
                    }
                }

                if (resolvedUpperBounds.size() != upperBounds.length) {
                    return null;
                }

                Type<?> upperBound = Type.Bottom;
                Type<?> lowerBound = Types.Object;

                if (!resolvedUpperBounds.isEmpty()) {
                    if (resolvedUpperBounds.size() == 1) {
                        upperBound = resolvedUpperBounds.get(0);
                    }
                    else {
                        upperBound = Type.makeCompoundType(Type.list(resolvedUpperBounds));
                    }
                }

                if (!resolvedLowerBounds.isEmpty()) {
                    if (resolvedLowerBounds.size() == 1) {
                        lowerBound = resolvedLowerBounds.get(0);
                    }
                    else {
                        lowerBound = Type.makeCompoundType(Type.list(resolvedLowerBounds));
                    }
                }

                return new WildcardType(lowerBound, upperBound);
            }

            // Now redundant to findType()...
            if (t instanceof java.lang.reflect.TypeVariable) {
                final java.lang.reflect.TypeVariable typeVariable = (java.lang.reflect.TypeVariable)t;

                Frame currentFrame = this;

                while (currentFrame != null) {
                    final GenericParameter genericParameter = currentFrame._type.findGenericParameter(typeVariable);

                    if (genericParameter != null) {
                        return genericParameter;
                    }

                    currentFrame = currentFrame._previous;
                }
            }

            if (t instanceof ParameterizedType) {
                final ParameterizedType type = (ParameterizedType)t;

                final Type<?> rawType = resolveType(type.getRawType());
                final java.lang.reflect.Type[] typeArguments = type.getActualTypeArguments();

                if (ArrayUtilities.isNullOrEmpty(typeArguments) || !rawType.isGenericType()) {
                    return rawType;
                }

                final Type<?>[] resolvedTypeArguments = new Type<?>[typeArguments.length];

                for (int i = 0, n = typeArguments.length; i < n; i++) {
                    final java.lang.reflect.Type typeArg = typeArguments[i];

                    if (typeArg instanceof java.lang.reflect.TypeVariable) {
                        final java.lang.reflect.TypeVariable typeVariable = (java.lang.reflect.TypeVariable)typeArg;
                        final GenericDeclaration genericDeclaration = typeVariable.getGenericDeclaration();
                        final Type<?> existingTypeArgument = findType(typeVariable);

                        if (existingTypeArgument != null) {
                            resolvedTypeArguments[i] = existingTypeArgument;
                            continue;
                        }

                        if (genericDeclaration instanceof Method) {
                            final ReflectedType<?> declaringType = (ReflectedType<?>)resolveType(((Method)genericDeclaration).getDeclaringClass());
                            final Method method = (Method)genericDeclaration;
                            final int position = ArrayUtilities.indexOf(method.getTypeParameters(), typeVariable);
                            final ReflectedMethod declaredMethod = declaringType.findMethod(method);

                            resolvedTypeArguments[i] = declaredMethod.getTypeArguments().get(position);
                        }
                        else {
                            final Class<?> declaringClass = (Class<?>)genericDeclaration;
                            final Type<?> declaringType = resolveType(declaringClass);
                            final int position = ArrayUtilities.indexOf(declaringClass.getTypeParameters(), typeVariable);

                            if (declaringType instanceof ReflectedType<?>) {
                                resolvedTypeArguments[i] = ((ReflectedType<?>)declaringType).getGenericParameters().get(position);
                            }
                            else {
                                resolvedTypeArguments[i] = declaringType.getTypeArguments().get(position);
                            }
                        }
                    }
                    else {
                        resolvedTypeArguments[i] = resolveType(typeArg);
                    }
                }

                final Type fromCache = Type.CACHE.find(
                    Type.CACHE.key(
                        rawType.getErasedClass(),
                        Type.list(resolvedTypeArguments)
                    )
                );

                if (fromCache != null) {
                    return fromCache;
                }

                for (final Type<?> resolvedTypeArgument : resolvedTypeArguments) {
                    if (!resolvedTypeArgument.isGenericParameter() || resolvedTypeArgument.getDeclaringType() != rawType) {
                        return Type.CACHE.getGenericType(rawType, Type.list(resolvedTypeArguments));
                    }
                }

                return rawType;
            }

            return null;
        }

        Frame findFrame(final java.lang.reflect.Type e) {
            Frame current = this;

            while (current != null) {
                if (current._typeElement.equals(e)) {
                    return current;
                }

                current = current._previous;
            }

            return null;
        }

        void addTypeArgument(final GenericParameter genericParameter) {
            _typeArguments = _typeArguments.append(genericParameter);
        }
    }

    public Type<?> resolve(final java.lang.reflect.Type type) {
        return resolveExisting(null, type, true);
    }

    private Type<?> visit(final java.lang.reflect.Type type, final Frame frame) {
        if (type instanceof ParameterizedType) {
            return visitParameterizedType((ParameterizedType)type, frame);
        }
        else if (type instanceof java.lang.reflect.WildcardType) {
            return visitWildcardType((java.lang.reflect.WildcardType)type, frame);
        }
        else if (type instanceof GenericArrayType) {
            return visitGenericArrayType((GenericArrayType)type, frame);
        }
        else if (type instanceof java.lang.reflect.TypeVariable) {
            return visitTypeVariable((java.lang.reflect.TypeVariable)type, frame);
        }
        else {
            return visitClass((Class<?>)type, frame);
        }
    }

    private Type<?> visitClass(final Class<?> c, final Frame frame) {
        if (c.isLocalClass() || c.isAnonymousClass()) {
            return null;
        }

        final Frame currentFrame = new Frame(c, frame);
        final ReflectedType<?> currentType = currentFrame.getCurrentClass();

        final Class<?> enclosingClass = c.getEnclosingClass();

        if (enclosingClass != null) {
            final Type declaringType = currentFrame.resolveType(enclosingClass);
            currentFrame.getCurrentClass().setDeclaringType(declaringType);
        }

        final TypeVariable<?>[] typeParameters = c.getTypeParameters();

        if (!ArrayUtilities.isNullOrEmpty(typeParameters)) {
            for (int i = 0, n = typeParameters.length; i < n; i++) {
                final TypeVariable typeVariable = typeParameters[i];

                currentType.addGenericParameter(
                    new GenericParameter(
                        typeVariable.getName(),
                        currentType,
                        null,
                        i
                    )
                );
            }
        }

        final Type<?> baseType;

        java.lang.reflect.Type baseClass;

        if (!c.isInterface()) {
            baseClass = c.getGenericSuperclass();

            if (baseClass == null) {
                baseClass = c.getSuperclass();
            }

            if (baseClass != null && baseClass != c) {
                baseType = currentFrame.resolveType(baseClass);

                if (baseType != null) {
                    currentType.setBaseType(baseType);
                }
                else {
                    return null;
                }
            }
        }

        List<Type<?>> interfaceList = List.nil();

        java.lang.reflect.Type[] interfaces = c.getGenericInterfaces();

        if (ArrayUtilities.isNullOrEmpty(interfaces)) {
            interfaces = c.getInterfaces();
        }

        if (!ArrayUtilities.isNullOrEmpty(interfaces)) {
            for (final java.lang.reflect.Type t : interfaces) {
                final Type<?> interfaceType = currentFrame.resolveType(t);

                if (interfaceType == null) {
                    return null;
                }

                interfaceList = interfaceList.append(interfaceType);
            }
        }

        currentType.setInterfaces(Type.list(interfaceList));

        for (final Class<?> ee : c.getDeclaredClasses()) {
            this.visit(ee, currentFrame);
        }

        for (final GenericParameter genericParameter : currentFrame.getCurrentClass().getGenericParameters()) {
            final java.lang.reflect.Type[] bounds = genericParameter.getRawTypeVariable().getBounds();

            if (ArrayUtilities.isNullOrEmpty(bounds)) {
                continue;
            }

            final Type<?> boundType;

            if (bounds.length == 1) {
                boundType = currentFrame.resolveType(bounds[0]);
            }
            else {
                final Type<?>[] resolvedBounds = new Type<?>[bounds.length];

                for (int i = 0, n = bounds.length; i < n; i++) {
                    resolvedBounds[i] = currentFrame.resolveType(bounds[i]);
                }

                boundType = Type.makeCompoundType(Type.list(resolvedBounds));
            }

            genericParameter.setUpperBound(boundType);
        }

        currentType.complete();

        if (!java.lang.reflect.Modifier.isPrivate(currentType.getModifiers())) {
            Type.CACHE.add(currentType);
        }

        return currentType;
    }

    private Type<?> visitTypeVariable(final java.lang.reflect.TypeVariable type, final Frame frame) {
        return frame.findType(type);
    }

    private Type<?> visitGenericArrayType(final GenericArrayType type, final Frame frame) {
        final Type<?> elementType = frame.resolveType(type.getGenericComponentType());

        if (elementType != null) {
            return elementType.makeArrayType();
        }

        return null;
    }

    private Type<?> visitWildcardType(final java.lang.reflect.WildcardType w, final Frame frame) {
        final java.lang.reflect.Type[] upperBounds = w.getUpperBounds();
        final java.lang.reflect.Type[] lowerBounds = w.getLowerBounds();

        List<Type<?>> resolvedUpperBounds = List.nil();
        List<Type<?>> resolvedLowerBounds = List.nil();

        for (final java.lang.reflect.Type lowerBound : lowerBounds) {
            final Type<?> resolvedLowerBound = visit(lowerBound, frame);

            if (resolvedLowerBound != null) {
                resolvedLowerBounds = resolvedLowerBounds.append(resolvedLowerBound);
            }
        }

        if (resolvedLowerBounds.size() != lowerBounds.length) {
            return null;
        }

        for (final java.lang.reflect.Type upperBound : upperBounds) {
            final Type<?> resolvedUpperBound = visit(upperBound, frame);

            if (resolvedUpperBound != null) {
                resolvedUpperBounds = resolvedUpperBounds.append(resolvedUpperBound);
            }
        }

        if (resolvedUpperBounds.size() != upperBounds.length) {
            return null;
        }

        Type<?> upperBound = Type.Bottom;
        Type<?> lowerBound = Types.Object;

        if (!resolvedUpperBounds.isEmpty()) {
            if (resolvedUpperBounds.size() == 1) {
                upperBound = resolvedUpperBounds.get(0);
            }
            else {
                upperBound = Type.makeCompoundType(Type.list(resolvedUpperBounds));
            }
        }

        if (!resolvedLowerBounds.isEmpty()) {
            if (resolvedLowerBounds.size() == 1) {
                lowerBound = resolvedLowerBounds.get(0);
            }
            else {
                lowerBound = Type.makeCompoundType(Type.list(resolvedLowerBounds));
            }
        }

        return new WildcardType(lowerBound, upperBound);
    }

    private Type<?> visitParameterizedType(final ParameterizedType type, final Frame frame) {
        final Type<?> rawType = frame.resolveType(type.getRawType());

        if (rawType == null) {
            return null;
        }

        final java.lang.reflect.Type[] typeArguments = type.getActualTypeArguments();
        final Type<?>[] resolvedTypeArguments = new Type<?>[typeArguments.length];

        for (int i = 0, n = typeArguments.length; i < n; i++) {
            resolvedTypeArguments[i] = frame.resolveType(typeArguments[i]);

            if (resolvedTypeArguments[i] == null) {
                return null;
            }
        }

        return rawType.makeGenericType(resolvedTypeArguments);
    }

    private Type<?> resolveExisting(final Frame frame, final java.lang.reflect.Type type, final boolean resolve) {
        final Type<?> result;

        Type<?> fromCacheOrFrame = Type.tryFind(type);

        if (fromCacheOrFrame != null) {
            result = fromCacheOrFrame;
        }
        else {
            if (frame != null) {
                fromCacheOrFrame = frame.resolveType(type);
            }

            if (!resolve) {
                return fromCacheOrFrame;
            }

            if (fromCacheOrFrame != null) {
                result = fromCacheOrFrame;
            }
            else if (type instanceof Class<?>) {
                result = visitClass((Class)type, frame);
            }
            else {
                result = visit(type, frame);
            }
        }

        return result;
    }

    private Type<?> visit(final ReflectedType<?> type, final Frame frame) {
        final Class<?> erasedClass = type.getErasedClass();

        for (final Field field : erasedClass.getDeclaredFields()) {
            visitField(field, frame);
        }

        for (final Method method : erasedClass.getDeclaredMethods()) {
            visitMethod(method, frame);
        }

        for (final Constructor<?> constructor : erasedClass.getDeclaredConstructors()) {
            visitConstructor(constructor, frame);
        }

        return type;
    }

    public Type<?> visitField(final Field field, final Frame frame) {
        final ReflectedType<?> declaringType = frame.getCurrentClass();

        final ReflectedField reflectedField = new ReflectedField(
            declaringType,
            field,
            frame.resolveType(field.getType())
        );

        declaringType.addField(reflectedField);

        return declaringType;
    }

    public Type<?> visitExecutable(final Executable e, final Frame frame) {
        if (e instanceof Method) {
            return visitMethod((Method)e, frame);
        }

        if (e instanceof Constructor<?>) {
            return visitConstructor((Constructor<?>)e, frame);
        }

        return frame.getCurrentClass();
    }

/*
    private Type<?> resolveType(final java.lang.reflect.Type type, final Frame frame) {
        if (type instanceof java.lang.reflect.Type.ArrayType) {
            final java.lang.reflect.Type.ArrayType arrayType = (java.lang.reflect.Type.ArrayType)type;
            return resolveType(arrayType.getComponentType(), frame).makeArrayType();
        }

        final Type<?> fromLookup = frame.findType(type.asElement());

        if (fromLookup != null) {
            final List<java.lang.reflect.Type> typeArguments = type.getTypeArguments();
            List<Type<?>> typeBindings = List.nil();

            for (final java.lang.reflect.Type typeArgument : typeArguments) {
                typeBindings = typeBindings.append(resolveType(typeArgument, frame));
            }

            if (typeBindings.isEmpty()) {
                return fromLookup;
            }

            final TypeList resolvedTypeArgs = Type.list(typeBindings);

            final Type fromCache = Type.CACHE.find(
                Type.CACHE.key(
                    fromLookup.getErasedClass(),
                    resolvedTypeArgs
                )
            );

            if (fromCache != null) {
                return fromCache;
            }

            final GenericType genericType = new GenericType(fromLookup, resolvedTypeArgs);

            Type.CACHE.add(genericType);

            return genericType;
        }

        return frame.resolveType(type);
    }
*/

    private Type<?> visitMethod(final Method m, final Frame frame) {
        java.lang.reflect.Type[] parameterTypes = m.getGenericParameterTypes();
        java.lang.reflect.Type[] thrownTypes = m.getGenericExceptionTypes();
        java.lang.reflect.Type returnType = m.getGenericReturnType();

        if (parameterTypes == null) {
            parameterTypes = m.getParameterTypes();
        }

        if (thrownTypes == null) {
            thrownTypes = m.getExceptionTypes();
        }

        if (returnType == null) {
            returnType = m.getReturnType();
        }

        final Type<?>[] resolvedThrownTypes = new Type<?>[thrownTypes.length];
        final ParameterInfo[] parameters = new ParameterInfo[parameterTypes.length];
        final TypeVariable<Method>[] typeParameters = m.getTypeParameters();
        final TypeBindings genericParameters;

        if (ArrayUtilities.isNullOrEmpty(typeParameters)) {
            genericParameters = TypeBindings.empty();
        }
        else {
            final Type<?>[] resolvedTypeParameters = new Type<?>[typeParameters.length];

            for (int i = 0; i < typeParameters.length; i++) {
                final TypeVariable<Method> typeVariable = typeParameters[i];
                final GenericParameter genericParameter = new GenericParameter(typeVariable.getName(), typeVariable, i);

                resolvedTypeParameters[i] = genericParameter;
                frame.addTypeArgument(genericParameter);
            }

            genericParameters = TypeBindings.createUnbound(Type.list(resolvedTypeParameters));
        }

        for (int i = 0; i < parameterTypes.length; i++) {
            final Type<?> resolvedParameterType = frame.resolveType(parameterTypes[i]);

            if (resolvedParameterType == null) {
                return null;
            }

            parameters[i] = new ParameterInfo("p" + i, i, resolvedParameterType);
        }

        for (int i = 0; i < thrownTypes.length; i++) {
            final Type<?> resolvedThrownType = frame.resolveType(thrownTypes[i]);

            if (resolvedThrownType == null) {
                return null;
            }

            resolvedThrownTypes[i] = resolvedThrownType;
        }

        final Type<?> resolvedReturnType = frame.resolveType(returnType);

        if (resolvedReturnType == null) {
            return null;
        }

        final ReflectedType<?> declaringType = frame.getCurrentClass();

        final ReflectedMethod method = new ReflectedMethod(
            declaringType,
            m,
            new ParameterList(parameters),
            resolvedReturnType,
            Type.list(resolvedThrownTypes),
            genericParameters
        );

        if (!genericParameters.isEmpty()) {
            for (final Type type : genericParameters.getGenericParameters()) {
                final GenericParameter genericParameter = (GenericParameter)type;

                genericParameter.setDeclaringMethod(method);

                final java.lang.reflect.Type[] bounds = genericParameter.getRawTypeVariable().getBounds();

                if (ArrayUtilities.isNullOrEmpty(bounds)) {
                    continue;
                }

                final Type<?> boundType;

                if (bounds.length == 1) {
                    boundType = frame.resolveType(bounds[0]);
                }
                else {
                    final Type<?>[] resolvedBounds = new Type<?>[bounds.length];

                    for (int i = 0, n = bounds.length; i < n; i++) {
                        resolvedBounds[i] = frame.resolveType(bounds[i]);
                    }

                    boundType = Type.makeCompoundType(Type.list(resolvedBounds));
                }

                genericParameter.setUpperBound(boundType);

            }
        }

        declaringType.addMethod(method);

        return declaringType;
    }

    private Type<?> visitConstructor(final Constructor<?> c, final Frame frame) {
        java.lang.reflect.Type[] parameterTypes = c.getGenericParameterTypes();
        java.lang.reflect.Type[] thrownTypes = c.getGenericExceptionTypes();

        if (parameterTypes == null) {
            parameterTypes = c.getParameterTypes();
        }

        if (thrownTypes == null) {
            thrownTypes = c.getExceptionTypes();
        }

        final Type<?>[] resolvedThrownTypes = new Type<?>[thrownTypes.length];
        final ParameterInfo[] parameters = new ParameterInfo[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            final Type<?> resolvedParameterType = frame.resolveType(parameterTypes[i]);

            if (resolvedParameterType == null) {
                return null;
            }

            parameters[i] = new ParameterInfo("p" + i, i, resolvedParameterType);
        }

        for (int i = 0; i < thrownTypes.length; i++) {
            final Type<?> resolvedThrownType = frame.resolveType(thrownTypes[i]);

            if (resolvedThrownType == null) {
                return null;
            }

            resolvedThrownTypes[i] = resolvedThrownType;
        }

        final ReflectedType<?> declaringType = frame.getCurrentClass();

        final ReflectedConstructor constructor = new ReflectedConstructor(
            declaringType,
            c,
            new ParameterList(parameters),
            Type.list(resolvedThrownTypes)
        );

        declaringType.addConstructor(constructor);

        return declaringType;
    }
}

@SuppressWarnings("unchecked")
class ReflectedType<T> extends Type<T> {
    private final String _name;
    private final String _simpleName;
    private final Class<T> _rawClass;
    private Type<?> _baseType;
    private TypeList _interfaces;
    private boolean _membersResolved;
    private boolean _completed;
    private Type<?> _declaringType;
    private List<GenericParameter> _genericParameters = List.nil();
    private List<ReflectedType<?>> _nestedTypes = List.nil();
    private List<ReflectedMethod> _methods = List.nil();
    private List<ReflectedField> _fields = List.nil();
    private List<ReflectedConstructor> _constructors = List.nil();
    private TypeBindings _typeBindings;

    ReflectedType(final Class<T> rawClass) {
        _rawClass = VerifyArgument.notNull(rawClass, "rawClass");
        _name = rawClass.getCanonicalName();
        _simpleName = rawClass.getSimpleName();
    }

    @Override
    public String getFullName() {
        return _name;
    }

    void setBaseType(final Type<?> baseType) {
        _baseType = baseType;
    }

    void setInterfaces(final TypeList interfaces) {
        _interfaces = VerifyArgument.notNull(interfaces, "interfaces");
    }

    List<GenericParameter> getGenericParameters() {
        return _genericParameters;
    }

    ReflectedMethod findMethod(final Method rawMethod) {
        for (final ReflectedMethod method : _methods) {
            if (Comparer.equals(method.getRawMethod(), rawMethod)) {
                return method;
            }
        }
        return null;
    }

    GenericParameter findGenericParameter(final java.lang.reflect.TypeVariable typeVariable) {
        final GenericParameter result = GenericParameterFinder.visit(this, typeVariable);

        if (result != null) {
            return result;
        }

        final int position = ArrayUtilities.indexOf(typeVariable.getGenericDeclaration().getTypeParameters(), typeVariable);

        for (final GenericParameter genericParameter : _genericParameters) {
            final Type declaringType = genericParameter.getDeclaringType();

            if (genericParameter.getGenericParameterPosition() != position) {
                continue;
            }

            if (declaringType != null &&
                declaringType.getErasedClass() == typeVariable.getGenericDeclaration()) {
                return genericParameter;
            }

            final MethodInfo method = genericParameter.getDeclaringMethod();

            if (method != null && method.getRawMethod() == typeVariable.getGenericDeclaration()) {
                return genericParameter;
            }
        }

        return null;
    }

    void setDeclaringType(final Type<?> declaringType) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
    }

    void addGenericParameter(final GenericParameter genericParameter) {
        VerifyArgument.notNull(genericParameter, "typeParameter");
        _completed = false;
        _genericParameters = _genericParameters.append(genericParameter);
    }

    void addNestedType(final ReflectedType<?> nestedType) {
        VerifyArgument.notNull(nestedType, "nestedType");
        _nestedTypes = _nestedTypes.append(nestedType);
        _membersResolved = false;
    }

    void addMethod(final ReflectedMethod method) {
        VerifyArgument.notNull(method, "method");
        _methods = _methods.append(method);
        _membersResolved = false;
    }

    void addConstructor(final ReflectedConstructor constructor) {
        VerifyArgument.notNull(constructor, "constructor");
        _constructors = _constructors.append(constructor);
        _membersResolved = false;
    }

    void addField(final ReflectedField field) {
        VerifyArgument.notNull(field, "field");
        _fields = _fields.append(field);
        _membersResolved = false;
    }

    private void completeIfNecessary() {
        if (!_completed) {
            synchronized (CACHE_LOCK) {
                if (!_completed) {
                    complete();
                }
            }
        }
    }

    private void ensureMembersResolved() {
        if (!_membersResolved) {
            synchronized (CACHE_LOCK) {
                if (!_membersResolved) {
                    new NewResolver().resolveMembers(this);
                    _membersResolved = true;
                    for (final ReflectedType<?> nestedType : _nestedTypes) {
                        nestedType.complete();
                    }
                }
            }
        }
    }

    @Override
    protected ConstructorList getDeclaredConstructors() {
        ensureMembersResolved();
        return new ConstructorList(_constructors);
    }

    @Override
    protected MethodList getDeclaredMethods() {
        ensureMembersResolved();
        return new MethodList(_methods);
    }

    @Override
    protected FieldList getDeclaredFields() {
        ensureMembersResolved();
        return new FieldList(_fields);
    }

    @Override
    protected TypeList getDeclaredTypes() {
        ensureMembersResolved();
        return new TypeList(_nestedTypes);
    }

    void complete() {
        if (_completed) {
            return;
        }

        _completed = true;

        if (_genericParameters.isEmpty()) {
            _typeBindings = TypeBindings.empty();
        }
        else {
            _typeBindings = TypeBindings.createUnbound(list(_genericParameters));
        }
    }

    @Override
    public Type getBaseType() {
        return _baseType;
    }

    @Override
    public TypeList getExplicitInterfaces() {
        return _interfaces;
    }

    @Override
    protected TypeBindings getTypeBindings() {
        completeIfNecessary();
        return _typeBindings;
    }

    @Override
    public Type getGenericTypeDefinition() {
        if (!isGenericType()) {
            throw Error.notGenericType(this);
        }
        if (!getTypeBindings().hasBoundParameters()) {
            return this;
        }
        throw ContractUtils.unreachable();
    }

    @Override
    protected Type makeGenericTypeCore(final TypeList typeArguments) {
        synchronized (CACHE_LOCK) {
            return CACHE.getGenericType(getGenericTypeDefinition(), typeArguments);
        }
    }

    @Override
    public Class<T> getErasedClass() {
        completeIfNecessary();
        return _rawClass;
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public int getModifiers() {
        return _rawClass.getModifiers();
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitClassType(this, parameter);
    }

    @Override
    protected StringBuilder _appendClassName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        if (!fullName) {
            return sb.append(_simpleName);
        }
        if (dottedName) {
            return sb.append(_name);
        }
        return super._appendClassName(sb, fullName, dottedName);
    }

    private final static SimpleVisitor<java.lang.reflect.Type, GenericParameter> GenericParameterFinder =
        new SimpleVisitor<java.lang.reflect.Type, GenericParameter>() {
            public GenericParameter visit(final TypeList types, final java.lang.reflect.Type s) {
                for (final Type type : types) {
                    final GenericParameter result = visit(type, s);
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }

            @Override
            public GenericParameter visitCapturedType(final Type<?> t, final java.lang.reflect.Type s) {
                return null;
            }

            @Override
            public GenericParameter visitClassType(final Type<?> type, final java.lang.reflect.Type parameter) {
                GenericParameter result;

                if (type.isGenericType()) {
                    result = visit(type.getGenericTypeParameters(), parameter);

                    if (result != null) {
                        return result;
                    }
                }

                if (type instanceof ReflectedType) {
                    final ReflectedType javacType = (ReflectedType)type;
                    for (final Object o : javacType._methods) {
                        final ReflectedMethod method = (ReflectedMethod)o;
                        if (method.isGenericMethod()) {
                            result = visit(method.getGenericMethodParameters(), parameter);

                            if (result != null) {
                                return result;
                            }
                        }
                    }
                }

                final Type declaringType = type.getDeclaringType();

                if (declaringType != null && declaringType != NullType) {
                    return visitClassType(declaringType, parameter);
                }

                return null;
            }

            @Override
            public GenericParameter visitPrimitiveType(final Type<?> type, final java.lang.reflect.Type parameter) {
                return super.visitPrimitiveType(type, parameter);
            }

            public GenericParameter visitTypeParameter(final Type<?> type, final TypeVariable typeVariable) {
                if (type instanceof GenericParameter) {
                    final GenericParameter genericParameter = (GenericParameter)type;
                    final Type declaringType = genericParameter.getDeclaringType();

                    if (declaringType != null && declaringType.getErasedClass() == typeVariable.getGenericDeclaration()) {
                        return genericParameter;
                    }

                    final MethodInfo method = genericParameter.getDeclaringMethod();

                    if (method != null && method.getRawMethod() == typeVariable.getGenericDeclaration()) {
                        return genericParameter;
                    }
                }
                return null;
            }

            @Override
            public GenericParameter visitWildcardType(final Type<?> type, final java.lang.reflect.Type parameter) {
                return null;
            }

            @Override
            public GenericParameter visitArrayType(final Type<?> type, final java.lang.reflect.Type parameter) {
                return null;
            }
        };
}
