package com.strobel.reflection;

import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;

import javax.lang.model.type.TypeKind;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

enum MemberListType {
    All,
    CaseSensitive,
    CaseInsensitive,
    HandleToInfo,
}

/**
 * @author strobelm
 */
final class RuntimeTypeCache<T> {
    private enum WhatsCached {
        Nothing,
        EnclosingType
    }

    private enum CacheType {
        Method,
        Constructor,
        Field,
        Property,
        Event,
        Interface,
        NestedType
    }

    private WhatsCached _whatsCached;
    private Class<T> _erasedClass;
    private Type<T> _runtimeType;
    private Type<?> _enclosingType;
    private TypeKind _typeKind;
    private String _name;
    private String _fullName;
    private String _signature;
    private String _erasedSignature;
    private String _description;
    private String _briefDescription;
    private String _simpleDescription;
    private String _erasedDescription;
    private Package _package;
    private MemberInfoCache<RuntimeMethodInfo> _methodCache;
    private MemberInfoCache<RuntimeConstructorInfo> _constructorCache;
    private MemberInfoCache<RuntimeFieldInfo> _fieldCache;
    private MemberInfoCache<Type<?>> _interfaceCache;
    private MemberInfoCache<Type<?>> _nestedTypeCache;

//    private static HashMap<RuntimeMethodInfo, RuntimeMethodInfo> _methodInstantiations;

    RuntimeTypeCache(final Type<T> runtimeType) {
        _typeKind = TypeKind.DECLARED;
        _runtimeType = runtimeType;
    }

    @SuppressWarnings("unchecked")
    Class<T> getErasedClass() {
        final String fullName = getFullName();

        if (_erasedClass == null) {
            try {
                _erasedClass = (Class<T>)Class.forName(fullName);
            }
            catch (ClassNotFoundException e) {
                throw Error.couldNotResolveType(fullName);
            }
        }

        return _erasedClass;
    }

    Package getPackage() {
        if (_package == null) {
            _package = getErasedClass().getPackage();
        }
        return _package;
    }

    TypeKind getTypeKind() {
        return _typeKind;
    }

    String getName() {
        if (_name == null) {
            _name = _runtimeType._appendClassName(new StringBuilder(), false, true).toString();
        }
        return _name;
    }

    String getFullName() {
        if (_fullName == null) {
            _fullName = _runtimeType._appendClassName(new StringBuilder(), true, true).toString();
        }
        return _fullName;
    }

    String getSignature() {
        if (_signature == null) {
            _signature = _runtimeType.appendSignature(new StringBuilder()).toString();
        }
        return _signature;
    }

    String getErasedSignature() {
        if (_erasedSignature == null) {
            _erasedSignature = _runtimeType.appendErasedSignature(new StringBuilder()).toString();
        }
        return _erasedSignature;
    }

    String getFullDescription() {
        if (_description == null) {
            _description = _runtimeType.appendFullDescription(new StringBuilder()).toString();
        }
        return _description;
    }

    String getErasedDescription() {
        if (_erasedDescription == null) {
            _erasedDescription = _runtimeType.appendErasedDescription(new StringBuilder()).toString();
        }
        return _erasedDescription;
    }

    String getBriefDescription() {
        if (_briefDescription == null) {
            _briefDescription = _runtimeType.appendBriefDescription(new StringBuilder()).toString();
        }
        return _briefDescription;
    }

    String getSimpleDescription() {
        if (_simpleDescription == null) {
            _simpleDescription = _runtimeType.appendSimpleDescription(new StringBuilder()).toString();
        }
        return _simpleDescription;
    }

    Type<T> getRuntimeType() {
        return _runtimeType;
    }

    Type<?> getEnclosingType() {
        if (_whatsCached != WhatsCached.EnclosingType) {
            _enclosingType = getRuntimeType().getDeclaringType();
            _whatsCached = WhatsCached.EnclosingType;
        }
        return _enclosingType;
    }

    private final static class Filter {
        private final String _name;
        private final MemberListType _listType;

        private Filter(final String name, final MemberListType listType) {
            this._name = name;
            this._listType = VerifyArgument.notNull(listType, "listType");
        }

        private boolean match(final String name) {
            if (_listType == MemberListType.CaseSensitive) {
                return _name == null || _name.equals(name);
            }
            return _listType != MemberListType.CaseInsensitive ||
                   _name == null ||
                   _name.equalsIgnoreCase(name);
        }
    }

/*
    final MethodInfo getGenericMethodInfo(final RuntimeMethodInfo genericMethod) {
        if (_methodInstantiations == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_methodInstantiations == null) {
                    _methodInstantiations = new HashMap<>();
                }
            }
        }

        final Type<T> reflectedType = getRuntimeType();

        final ParameterList oldParameters = genericMethod.getParameters();
        final ParameterList newParameters = GenericType.GenericBinder.visitParameters(
            oldParameters,
            reflectedType.getTypeBindings()
        );

        final Type<?> oldReturnType = genericMethod.getReturnType();
        final Type<?> newReturnType = GenericType.GenericBinder.visit(
            oldReturnType,
            reflectedType.getTypeBindings()
        );

        final TypeList oldThrownTypes = genericMethod.getThrownTypes();
        final TypeList newThrownTypes = GenericType.GenericBinder.visit(
            oldThrownTypes,
            reflectedType.getTypeBindings()
        );

        final RuntimeMethodInfo runtimeMethod = new RuntimeMethodInfo(
            genericMethod.getRawMethod(),
            genericMethod.getDeclaringType(),
            this,
            genericMethod.getModifiers(),
            genericMethod.getBindingFlags(),
            newParameters,
            newReturnType,
            newThrownTypes
        );

        final RuntimeMethodInfo currentRuntimeMethod;

        synchronized (Type.CACHE_LOCK) {
            currentRuntimeMethod = _methodInstantiations.get(genericMethod);

            if (currentRuntimeMethod != null) {
                return currentRuntimeMethod;
            }

            _methodInstantiations.put(runtimeMethod, runtimeMethod);
        }

        return runtimeMethod;
    }
*/

    ArrayList<RuntimeMethodInfo> getMethodList(final MemberListType listType, final String name) {
        if (_methodCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_methodCache == null) {
                    _methodCache = new MemberInfoCache<>(this);
                }
            }
        }

        return _methodCache.getMemberList(listType, name, CacheType.Method);
    }

    ArrayList<RuntimeConstructorInfo> getConstructorList(final MemberListType listType, final String name) {
        if (_constructorCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_constructorCache == null) {
                    _constructorCache = new MemberInfoCache<>(this);
                }
            }
        }

        return _constructorCache.getMemberList(listType, name, CacheType.Constructor);
    }

    ArrayList<RuntimeFieldInfo> getFieldList(final MemberListType listType, final String name) {
        if (_fieldCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_fieldCache == null) {
                    _fieldCache = new MemberInfoCache<>(this);
                }
            }
        }

        return _fieldCache.getMemberList(listType, name, CacheType.Field);
    }

    ArrayList<Type<?>> getInterfaceList(final MemberListType listType, final String name) {
        if (_interfaceCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_interfaceCache == null) {
                    _interfaceCache = new MemberInfoCache<>(this);
                }
            }
        }

        return _interfaceCache.getMemberList(listType, name, CacheType.Interface);
    }

    ArrayList<Type<?>> getNestedTypeList(final MemberListType listType, final String name) {
        if (_nestedTypeCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_nestedTypeCache == null) {
                    _nestedTypeCache = new MemberInfoCache<>(this);
                }
            }
        }

        return _nestedTypeCache.getMemberList(listType, name, CacheType.NestedType);
    }

    MethodBase getMethod(final Type<? super T> declaringType, final MethodInfo method) {
        if (_methodCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_methodCache == null) {
                    _methodCache = new MemberInfoCache<>(this);
                }
            }
        }

        return _methodCache.addMethod(declaringType, method, CacheType.Method);
    }

    MethodBase getConstructor(final Type<? super T> declaringType, final MethodInfo constructor) {
        if (_constructorCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_constructorCache == null) {
                    _constructorCache = new MemberInfoCache<>(this);
                }
            }
        }

        return _constructorCache.addMethod(declaringType, constructor, CacheType.Constructor);
    }

    FieldInfo getField(final FieldInfo field) {
        if (_fieldCache == null) {
            synchronized (Type.CACHE_LOCK) {
                if (_fieldCache == null) {
                    _fieldCache = new MemberInfoCache<>(this);
                }
            }
        }
        return _fieldCache.addField(field);
    }

    @SuppressWarnings("unchecked")
    final static class MemberInfoCache<T extends MemberInfo> {
        private HashMap<String, ArrayList<T>> _caseSensitiveMembers;
        private HashMap<String, ArrayList<T>> _caseInsensitiveMembers;
        private ArrayList<T> _root;
        private boolean _cacheComplete;

        // This is the strong reference back to the cache
        private RuntimeTypeCache<?> _typeCache;

        private MemberInfoCache(final RuntimeTypeCache<?> typeCache) {
            _typeCache = VerifyArgument.notNull(typeCache, "typeCache");
            _cacheComplete = false;
        }

        Type<?> getReflectedType() {
            return _typeCache.getRuntimeType();
        }

        private void mergeWithGlobalList(final ArrayList<T> list) {
            final int cachedCount = _root.size();

            for (int i = 0, n = list.size(); i < n; i++) {
                final T newMemberInfo = list.get(i);

                T cachedMemberInfo = null;

                for (int j = 0; j < cachedCount; j++) {
                    cachedMemberInfo = _root.get(j);

                    if (newMemberInfo.equals(cachedMemberInfo)) {
                        list.set(i, cachedMemberInfo);
                        break;
                    }
                }

                if (list.get(i) != cachedMemberInfo) {
                    _root.add(newMemberInfo);
                }
            }
        }

        final ArrayList<T> getMemberList(final MemberListType listType, final String name, final CacheType cacheType) {
            final ArrayList<T> list;

            switch (listType) {
                case CaseSensitive:
                    if (_caseSensitiveMembers == null) {
                        return populate(name, listType, cacheType);
                    }

                    list = _caseSensitiveMembers.get(name);

                    if (list == null) {
                        return populate(name, listType, cacheType);
                    }

                    return list;

                case All:
                    if (_cacheComplete) {
                        return _root;
                    }

                    return populate(null, listType, cacheType);

                default:
                    if (_caseInsensitiveMembers == null) {
                        return populate(name, listType, cacheType);
                    }

                    list = _caseInsensitiveMembers.get(name);

                    if (list == null) {
                        return populate(name, listType, cacheType);
                    }

                    return list;
            }
        }

        final ArrayList<T> insert(final ArrayList<T> list, final String name, final MemberListType listType) {
            boolean preallocationComplete = false;

            ArrayList<T> result = list;

            synchronized (this) {
                try {

                    if (listType == MemberListType.CaseSensitive) {
                        if (_caseSensitiveMembers == null) {
                            _caseSensitiveMembers = new HashMap<>(1);
                        }
                    }
                    else if (listType == MemberListType.CaseInsensitive) {
                        if (_caseInsensitiveMembers == null) {
                            _caseInsensitiveMembers = new HashMap<>(1);
                        }
                    }

                    if (_root == null) {
                        _root = new ArrayList<>(list.size());
                    }

                    preallocationComplete = true;
                }
                finally {
                    if (preallocationComplete) {
                        if (listType == MemberListType.CaseSensitive) {
                            // Ensure we always return a list that has been merged with the global list.
                            final ArrayList<T> cachedList = _caseSensitiveMembers.get(name);
                            if (cachedList == null) {
                                mergeWithGlobalList(list);
                                _caseSensitiveMembers.put(name, list);
                            }
                            else {
                                result = cachedList;
                            }
                        }
                        else if (listType == MemberListType.CaseInsensitive) {
                            // Ensure we always return a list that has been merged with the global list.
                            final ArrayList<T> cachedList = _caseInsensitiveMembers.get(name);
                            if (cachedList == null) {
                                mergeWithGlobalList(list);
                                _caseInsensitiveMembers.put(name, list);
                            }
                            else {
                                result = cachedList;
                            }
                        }
                        else {
                            mergeWithGlobalList(list);
                        }

                        if (listType == MemberListType.All) {
                            _cacheComplete = true;
                        }
                    }
                }
            }

            return result;
        }

        final MethodBase addMethod(final Type<?> declaringType, final MethodBase method, final CacheType cacheType) {
            final ArrayList<T> list;

            final int modifiers = VerifyArgument.notNull(method, "method").getModifiers();
            final boolean isPublic = Modifier.isPublic(modifiers);
            final boolean isStatic = Modifier.isStatic(modifiers);
            final boolean isInherited = !Comparer.equals(declaringType, getReflectedType());
            final Set<BindingFlags> bindingFlags = Type.filterPreCalculate(isPublic, isInherited, isStatic);

            switch (cacheType) {
                case Method:
                    final ArrayList<MethodInfo> methodList = new ArrayList<>(1);
                    final MethodInfo sourceMethod = (MethodInfo)method;

                    methodList.add(
                        new RuntimeMethodInfo(
                            sourceMethod.getRawMethod(),
                            declaringType,
                            _typeCache,
                            modifiers,
                            bindingFlags,
                            method.getParameters(),
                            sourceMethod.getReturnType(),
                            method.getThrownTypes(),
                            sourceMethod.getTypeBindings()
                        )
                    );

                    list = (ArrayList<T>)methodList;
                    break;

                case Constructor:
                    final ArrayList<RuntimeConstructorInfo> constructorList = new ArrayList<>(1);
                    constructorList.add(
                        new RuntimeConstructorInfo(
                            ((ConstructorInfo)method).getRawConstructor(),
                            _typeCache,
                            modifiers,
                            bindingFlags,
                            method.getParameters()
                        )
                    );
                    list = (ArrayList<T>)constructorList;
                    break;

                default:
                    throw ContractUtils.unreachable();
            }

            return (MethodBase)insert(list, null, MemberListType.HandleToInfo).get(0);
        }

        final FieldInfo addField(final FieldInfo field) {
            final ArrayList<T> list = new ArrayList<>(1);
            final int modifiers = VerifyArgument.notNull(field, "field").getModifiers();
            final boolean isPublic = Modifier.isPublic(modifiers);
            final boolean isStatic = Modifier.isStatic(modifiers);
            final Type declaringType = field.getDeclaringType();
            final boolean isInherited = !Comparer.equals(declaringType, getReflectedType());
            final Set<BindingFlags> bindingFlags = Type.filterPreCalculate(isPublic, isInherited, isStatic);

            list.add(
                (T)new RuntimeFieldInfo(
                    field.getRawField(),
                    declaringType,
                    _typeCache,
                    modifiers,
                    bindingFlags,
                    field.getFieldType()
                )
            );

            return (FieldInfo)insert(list, null, MemberListType.HandleToInfo).get(0);
        }

        private void populateRuntimeFields(
            final Filter filter,
            final FieldList declaredFields,
            final Type<?> declaringType,
            final ArrayList<RuntimeFieldInfo> list) {
            final Type<?> reflectedType = getReflectedType();

            assert declaringType != Type.NullType;
            assert reflectedType != Type.NullType;

            final boolean isInherited = !declaringType.equals(reflectedType);

            for (int i = 0, n = declaredFields.size(); i < n; i++) {
                final FieldInfo declaredField = declaredFields.get(i);

                if (!filter.match(declaredField.getName())) {
                    continue;
                }

                assert declaredField.getFieldType() != Type.NullType;

                final int fieldModifiers = declaredField.getModifiers();

                if (isInherited && Modifier.isPrivate(fieldModifiers)) {
                    continue;
                }

                final boolean isPublic = Modifier.isPublic(fieldModifiers);
                final boolean isStatic = Modifier.isStatic(fieldModifiers);
                final Set<BindingFlags> bindingFlags = Type.filterPreCalculate(isPublic, isInherited, isStatic);

                final RuntimeFieldInfo runtimeFieldInfo =
                    new RuntimeFieldInfo(
                        declaredField.getRawField(),
                        declaringType,
                        _typeCache,
                        fieldModifiers,
                        bindingFlags,
                        declaredField.getFieldType()
                    );

                list.add(runtimeFieldInfo);
            }
        }

        private ArrayList<RuntimeFieldInfo> populateFields(final Filter filter) {
            final ArrayList<RuntimeFieldInfo> list = new ArrayList<>();
            final Type<?> reflectedType = getReflectedType();

            Type<?> declaringType = reflectedType;

            while (declaringType.isGenericParameter()) {
                declaringType = declaringType.getUpperBound();
            }

            while (declaringType != null && declaringType != Type.NullType) {
                populateRuntimeFields(filter, declaringType.getDeclaredFields(), declaringType, list);
                declaringType = declaringType.getBaseType();
            }

            final TypeList interfaces = reflectedType.isGenericParameter()
                                        ? reflectedType.getUpperBound().getExplicitInterfaces()
                                        : reflectedType.getExplicitInterfaces();

            for (int i = 0, n = interfaces.size(); i < n; i++) {
                // Populate literal fields defined on any of the interfaces implemented by the declaring type 
                final Type<?> interfaceType = interfaces.get(i);
                populateRuntimeFields(filter, interfaceType.getDeclaredFields(), interfaceType, list);
            }

            return list;
        }

        private ArrayList<RuntimeMethodInfo> populateMethods(final Filter filter) {
            final HashMap<String, ArrayList<RuntimeMethodInfo>> nameLookup = new HashMap<>();
            final ArrayList<RuntimeMethodInfo> list = new ArrayList<>();
            final Type<?> reflectedType = getReflectedType();

            Type<?> declaringType = reflectedType;

            final boolean isInterface = declaringType.isInterface();

            if (isInterface) {
                for (final MethodInfo method : declaringType.getDeclaredMethods()) {
                    final String name = method.getName();
                    final int methodModifiers = method.getModifiers();

                    if (!filter.match(name)) {
                        continue;
                    }

                    assert (method.getReturnType() != Type.NullType);

                    assert Modifier.isAbstract(methodModifiers);
                    assert !Modifier.isFinal(methodModifiers);

                    final boolean isPublic = Modifier.isPublic(methodModifiers);
                    final boolean isStatic = Modifier.isStatic(methodModifiers);
                    final boolean isInherited = false;
                    final Set<BindingFlags> bindingFlags = Type.filterPreCalculate(isPublic, isInherited, isStatic);

                    final RuntimeMethodInfo runtimeMethod = new RuntimeMethodInfo(
                        method.getRawMethod(),
                        declaringType,
                        _typeCache,
                        methodModifiers,
                        bindingFlags,
                        method.getParameters(),
                        method.getReturnType(),
                        method.getThrownTypes(),
                        method.getTypeBindings()
                    );

                    list.add(runtimeMethod);
                }

                return list;
            }

            while (declaringType.isGenericParameter()) {
                declaringType = declaringType.getUpperBound();
            }

            while (declaringType != null && declaringType != Type.NullType) {

                for (final MethodInfo method : declaringType.getDeclaredMethods()) {
                    final String name = method.getName();

                    if (!filter.match(name)) {
                        continue;
                    }

                    assert (method.getReturnType() != Type.NullType);

                    final int methodModifiers = method.getModifiers();

                    final boolean isVirtual = !Modifier.isFinal(methodModifiers);
                    final boolean isPrivate = Modifier.isPrivate(methodModifiers);
                    final boolean isInherited = !declaringType.equals(reflectedType);

                    if (isInherited && isPrivate) {
                        continue;
                    }

                    ArrayList<RuntimeMethodInfo> nameCollisions = nameLookup.get(name);

                    if (overrideExists(method, nameCollisions)) {
                        continue;
                    }

                    if (!isVirtual) {
                        assert !Modifier.isAbstract(methodModifiers);
                    }

                    final boolean isPublic = Modifier.isPublic(methodModifiers);
                    final boolean isStatic = Modifier.isStatic(methodModifiers);
                    final Set<BindingFlags> bindingFlags = Type.filterPreCalculate(isPublic, isInherited, isStatic);

                    final RuntimeMethodInfo runtimeMethod = new RuntimeMethodInfo(
                        method.getRawMethod(),
                        declaringType,
                        _typeCache,
                        methodModifiers,
                        bindingFlags,
                        method.getParameters(),
                        method.getReturnType(),
                        method.getThrownTypes(),
                        method.getTypeBindings()
                    );

                    if (nameCollisions == null) {
                        nameCollisions = new ArrayList<>(1);
                        nameLookup.put(name, nameCollisions);
                    }

                    nameCollisions.add(runtimeMethod);
                    list.add(runtimeMethod);
                }

                declaringType = declaringType.getBaseType();
            }

            return list;
        }

        private static boolean overrideExists(final MethodInfo method, final ArrayList<? extends MethodInfo> methods) {
            if (methods == null) {
                return false;
            }
            for (int i = 0, n = methods.size(); i < n; i++) {
                final MethodInfo otherMethod = methods.get(i);
                if (Helper.overrides(otherMethod, method)) {
                    return true;
                }
            }
            return false;
        }

        private ArrayList<RuntimeConstructorInfo> populateConstructors(final Filter filter) {
            final Type<?> reflectedType = getReflectedType();
            final ArrayList<RuntimeConstructorInfo> list = new ArrayList<>();

            if (reflectedType.isGenericParameter()) {
                return list;
            }

            for (final ConstructorInfo constructor : reflectedType.getDeclaredConstructors()) {
                final String name = constructor.getName();

                if (!filter.match(name)) {
                    continue;
                }

                final int modifiers = constructor.getModifiers();

                assert constructor.getDeclaringType() != Type.NullType;

                final boolean isPublic = Modifier.isPublic(modifiers);
                final boolean isStatic = false;
                final boolean isInherited = false;
                final Set<BindingFlags> bindingFlags = Type.filterPreCalculate(isPublic, isInherited, isStatic);

                final RuntimeConstructorInfo runtimeConstructorInfo = new RuntimeConstructorInfo(
                    constructor.getRawConstructor(),
                    _typeCache,
                    modifiers,
                    bindingFlags,
                    constructor.getParameters()
                );

                list.add(runtimeConstructorInfo);
            }

            return list;
        }

        private ArrayList<Type<?>> populateInterfaces(final Filter filter) {
            final ArrayList<Type<?>> list = new ArrayList<>();

            final Type<?> reflectedType = getReflectedType();

            final HashSet<Type> set = new HashSet<>();
            final com.sun.tools.javac.util.List<Type> interfaceList = Helper.interfaces(reflectedType);

            for (final Type interfaceType : interfaceList) {
                final String name = interfaceType.getFullName();

                if (filter.match(name) && set.add(interfaceType)) {
                    list.add(interfaceType);
                }
            }

            return list;
        }

        private ArrayList<Type<?>> populateNestedClasses(final Filter filter) {
            final ArrayList<Type<?>> list = new ArrayList<>();

            Type<?> declaringType = getReflectedType();

            if (declaringType.isGenericParameter()) {
                while (declaringType.isGenericParameter()) {
                    declaringType = declaringType.getUpperBound();
                }
            }

            if (declaringType == Type.NullType) {
                return list;
            }

            final TypeList declaredTypes = declaringType.getDeclaredTypes();

            for (int i = 0, n = declaredTypes.size(); i < n; i++) {
                final Type<?> nestedType = declaredTypes.get(i);

                if (!filter.match(nestedType.getName())) {
                    continue;
                }

                list.add(nestedType);
            }

            return list;
        }

        private ArrayList<T> populate(final String name, final MemberListType listType, final CacheType cacheType) {
            final Filter filter;

            if (name == null || name.length() == 0 ||
                (cacheType == CacheType.Constructor && name.charAt(0) != '.' && name.charAt(0) != '*')) {
                filter = new Filter(null, listType);
            }
            else {
                filter = new Filter(name, listType);
            }

            final ArrayList<T> list;

            switch (cacheType) {
                case Method:
                    list = (ArrayList<T>)populateMethods(filter);
                    break;
                case Field:
                    list = (ArrayList<T>)populateFields(filter);
                    break;
                case Constructor:
                    list = (ArrayList<T>)populateConstructors(filter);
                    break;
                case NestedType:
                    list = (ArrayList<T>)populateNestedClasses(filter);
                    break;
                case Interface:
                    list = (ArrayList<T>)populateInterfaces(filter);
                    break;
                default:
                    throw ContractUtils.unreachable();
            }

            return insert(list, name, listType);
        }
    }
}

final class RuntimeConstructorInfo extends ConstructorInfo {

    private final Constructor<?> _rawConstructor;
    private final RuntimeTypeCache<?> _reflectedTypeCache;
    private final Set<BindingFlags> _bindingFlags;
    private final int _modifiers;
    private final ParameterList _parameters;

    private String _signature;
    private String _erasedSignature;
    private String _description;
    private String _simpleDescription;
    private String _erasedDescription;

    RuntimeConstructorInfo(
        final Constructor<?> rawConstructor,
        final RuntimeTypeCache<?> reflectedTypeCache,
        final int modifiers,
        final Set<BindingFlags> bindingFlags,
        final ParameterList parameters) {

        _rawConstructor = VerifyArgument.notNull(rawConstructor, "rawConstructor");
        _reflectedTypeCache = VerifyArgument.notNull(reflectedTypeCache, "reflectedTypeCache");
        _bindingFlags = VerifyArgument.notNull(bindingFlags, "bindingFlags");
        _modifiers = modifiers;
        _parameters = VerifyArgument.notNull(parameters, "parameters");
    }

    Set<BindingFlags> getBindingFlags() {
        return _bindingFlags;
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public Constructor<?> getRawConstructor() {
        return _rawConstructor;
    }

/*
    @Override
    public String getName() {
        return _rawConstructor.getName();
    }
*/

    @Override
    public Type getDeclaringType() {
        return _reflectedTypeCache.getRuntimeType();
    }

    @Override
    public Type getReflectedType() {
        return _reflectedTypeCache.getRuntimeType();
    }

    @Override
    int getModifiers() {
        return _modifiers;
    }

    @Override
    public String getSignature() {
        if (_signature == null) {
            _signature = super.getSignature();
        }
        return _signature;
    }

    @Override
    public String getErasedSignature() {
        if (_erasedSignature == null) {
            _erasedSignature = super.getErasedSignature();
        }
        return _erasedSignature;
    }

    @Override
    public String getDescription() {
        if (_description == null) {
            _description = super.getDescription();
        }
        return _description;
    }

    @Override
    public String getSimpleDescription() {
        if (_simpleDescription == null) {
            _simpleDescription = super.getSimpleDescription();
        }
        return _simpleDescription;
    }

    @Override
    public String getErasedDescription() {
        if (_erasedDescription == null) {
            _erasedDescription = super.getErasedDescription();
        }
        return _erasedDescription;
    }
}

final class RuntimeMethodInfo extends MethodInfo {
    private final static Method GET_CLASS_METHOD;

    static {
        Method getClassMethod;

        try {
            getClassMethod = Object.class.getMethod("getClass");
        }
        catch (NoSuchMethodException ignored) {
            getClassMethod = null;
        }

        GET_CLASS_METHOD = getClassMethod;
    }

    private final Method _rawMethod;
    private final Type<?> _declaringType;
    private final RuntimeTypeCache<?> _reflectedTypeCache;
    private final int _modifiers;
    private final Set<BindingFlags> _bindingFlags;
    private final ParameterList _parameters;
    private final Type<?> _returnType;
    private final TypeList _thrownTypes;
    private final TypeBindings _typeBindings;

    private String _signature;
    private String _erasedSignature;
    private String _description;
    private String _simpleDescription;
    private String _erasedDescription;

    RuntimeMethodInfo(
        final Method rawMethod,
        final Type<?> declaringType,
        final RuntimeTypeCache<?> reflectedTypeCache,
        final int modifiers,
        final Set<BindingFlags> bindingFlags,
        final ParameterList parameters,
        final Type<?> returnType,
        final TypeList thrownTypes,
        final TypeBindings typeBindings) {

        _rawMethod = VerifyArgument.notNull(rawMethod, "rawConstructor");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _reflectedTypeCache = VerifyArgument.notNull(reflectedTypeCache, "reflectedTypeCache");
        _bindingFlags = VerifyArgument.notNull(bindingFlags, "bindingFlags");
        _modifiers = modifiers;
        _parameters = VerifyArgument.notNull(parameters, "parameters");

        if (GET_CLASS_METHOD.equals(rawMethod)) {
            _returnType = Type.of(Class.class)
                              .makeGenericType(
                                  Type.makeExtendsWildcard(reflectedTypeCache.getRuntimeType())
                              );
        }
        else {
            _returnType = VerifyArgument.notNull(returnType, "returnType");
        }

        _thrownTypes = VerifyArgument.notNull(thrownTypes, "thrownTypes");
        _typeBindings = VerifyArgument.notNull(typeBindings, "typeBindings");
    }

    Set<BindingFlags> getBindingFlags() {
        return _bindingFlags;
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public Type getReturnType() {
        return _returnType;
    }

    @Override
    public Method getRawMethod() {
        return _rawMethod;
    }

    @Override
    public String getName() {
        return _rawMethod.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public Type getReflectedType() {
        return _reflectedTypeCache.getRuntimeType();
    }

    @Override
    public TypeList getThrownTypes() {
        return _thrownTypes;
    }

    @Override
    protected TypeBindings getTypeBindings() {
        return _typeBindings;
    }

    @Override
    int getModifiers() {
        return _modifiers;
    }

    @Override
    public String getSignature() {
        if (_signature == null) {
            _signature = super.getSignature();
        }
        return _signature;
    }

    @Override
    public String getErasedSignature() {
        if (_erasedSignature == null) {
            _erasedSignature = super.getErasedSignature();
        }
        return _erasedSignature;
    }

    @Override
    public String getDescription() {
        if (_description == null) {
            _description = super.getDescription();
        }
        return _description;
    }

    @Override
    public String getSimpleDescription() {
        if (_simpleDescription == null) {
            _simpleDescription = super.getSimpleDescription();
        }
        return _simpleDescription;
    }

    @Override
    public String getErasedDescription() {
        if (_erasedDescription == null) {
            _erasedDescription = super.getErasedDescription();
        }
        return _erasedDescription;
    }
}

final class RuntimeFieldInfo extends FieldInfo {
    private final Field _rawField;
    private final Type<?> _declaringType;
    private final RuntimeTypeCache<?> _reflectedTypeCache;
    private final int _modifiers;
    private final Set<BindingFlags> _bindingFlags;
    private final Type<?> _fieldType;

    private String _signature;
    private String _erasedSignature;
    private String _description;
    private String _erasedDescription;

    RuntimeFieldInfo(
        final Field rawField,
        final Type<?> declaringType,
        final RuntimeTypeCache<?> reflectedTypeCache,
        final int modifiers,
        final Set<BindingFlags> bindingFlags,
        final Type<?> fieldType) {

        _rawField = VerifyArgument.notNull(rawField, "rawConstructor");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _reflectedTypeCache = VerifyArgument.notNull(reflectedTypeCache, "reflectedTypeCache");
        _bindingFlags = VerifyArgument.notNull(bindingFlags, "bindingFlags");
        _modifiers = modifiers;
        _fieldType = VerifyArgument.notNull(fieldType, "fieldType");
    }

    Set<BindingFlags> getBindingFlags() {
        return _bindingFlags;
    }

    @Override
    public Type getFieldType() {
        return _fieldType;
    }

    @Override
    public boolean isEnumConstant() {
        return false;
    }

    @Override
    public Field getRawField() {
        return _rawField;
    }

    @Override
    public String getName() {
        return _rawField.getName();
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public Type getReflectedType() {
        return _reflectedTypeCache.getRuntimeType();
    }

    @Override
    int getModifiers() {
        return _modifiers;
    }

    @Override
    public String getSignature() {
        if (_signature == null) {
            _signature = super.getSignature();
        }
        return _signature;
    }

    @Override
    public String getErasedSignature() {
        if (_erasedSignature == null) {
            _erasedSignature = super.getErasedSignature();
        }
        return _erasedSignature;
    }

    @Override
    public String getDescription() {
        if (_description == null) {
            _description = super.getDescription();
        }
        return _description;
    }

    @Override
    public String getErasedDescription() {
        if (_erasedDescription == null) {
            _erasedDescription = super.getErasedDescription();
        }
        return _erasedDescription;
    }
}
