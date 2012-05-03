package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.StringEx;
import com.strobel.core.VerifyArgument;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.strobel.reflection.Flags.all;
import static com.strobel.reflection.Flags.any;

/**
 * @author Mike Strobel
 */
final class ReflectedType<T> extends Type {
    private final Class<T> _class;
    private final Type _baseType;
    private final FieldCollection _fields;
    private final MethodCollection _methods;
    private final ConstructorCollection _constructors;
    private final TypeCollection _genericParameters;
    private final TypeCollection _interfaces;

    ReflectedType(final Class<T> rawType, final TypeCollection genericParameters, final Type baseType, final TypeCollection interfaces) {
        _class = VerifyArgument.notNull(rawType, "rawType");
        _genericParameters = VerifyArgument.notNull(genericParameters, "genericParameters");

        if (baseType == null && rawType != java.lang.Object.class) {
            throw new IllegalArgumentException("Base type cannot be null.");
        }

        _baseType = baseType;
        _interfaces = VerifyArgument.notNull(interfaces, "interfaces");
        _fields = FieldCollection.empty();
        _methods = MethodCollection.empty();
        _constructors = ConstructorCollection.empty();
    }

    protected TypeCollection populateGenericParameters() {
        final TypeVariable<Class<T>>[] typeParameters = _class.getTypeParameters();
        final Type[] genericParameters = new Type[typeParameters.length];

        for (int i = 0, n = typeParameters.length; i < n; i++) {
            final TypeVariable<?> typeVariable = typeParameters[i];
            genericParameters[i] = new GenericParameterType(typeVariable, this, i);
        }

        return new TypeCollection(genericParameters);
    }

    @Override
    public Type makeGenericType(final Type... typeArguments) {
        return new GenericType(this, typeArguments);
    }

    public TypeContext getContext() {
        return TypeContext.SYSTEM;
    }

    @Override
    public Type getBaseType() {
        return _baseType;
    }

    @Override
    public TypeCollection getInterfaces() {
        return _interfaces;
    }

    @Override
    public Class<?> getErasedClass() {
        return _class;
    }

    @Override
    public boolean isGenericType() {
        return !_genericParameters.isEmpty();
    }

    @Override
    public TypeCollection getGenericParameters() {
        return _genericParameters;
    }

    @Override
    public Type getGenericTypeDefinition() {
        if (isGenericTypeDefinition()) {
            return this;
        }
        throw Error.notGenericType(this);
    }

    @Override
    public <A extends Annotation> A getAnnotation(final Class<A> annotationClass) {
        return _class.getAnnotation(annotationClass);
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return _class.isAnnotationPresent(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return _class.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return _class.getDeclaredAnnotations();
    }

    @Override
    public MemberCollection<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType... memberTypes) {
        VerifyArgument.notNull(name, "name");

        if (memberTypes == null || memberTypes.length == 0) {
            return MemberCollection.empty();
        }

        MethodInfo[] methods = EmptyMethods;
        ConstructorInfo[] constructors = EmptyConstructors;
        FieldInfo[] fields = EmptyFields;
        final Type[] nestedTypes = EmptyTypes;

        if (ArrayUtilities.contains(memberTypes, MemberType.Field)) {
            fields = getFieldCandidates(name, bindingFlags, true);
        }

        if (ArrayUtilities.contains(memberTypes, MemberType.Method)) {
            methods = getMethodBaseCandidates(_methods, name, bindingFlags, CallingConvention.Any, null, true);
        }

        if (ArrayUtilities.contains(memberTypes, MemberType.Constructor)) {
            constructors = getMethodBaseCandidates(_constructors, name, bindingFlags, CallingConvention.Any, null, true);
        }

        if (ArrayUtilities.contains(memberTypes, MemberType.NestedType)) {
        }

        if (memberTypes.length == 1) {
            switch (memberTypes[0]) {
                case Constructor:
                    if (constructors.length == 0) {
                        return ConstructorCollection.empty();
                    }
                    return new ConstructorCollection(constructors);

                case Field:
                    if (fields.length == 0) {
                        return FieldCollection.empty();
                    }
                    return new FieldCollection(fields);

                case Method:
                    if (methods.length == 0) {
                        return MethodCollection.empty();
                    }
                    return new MethodCollection(methods);

                case NestedType:
                    if (nestedTypes.length == 0) {
                        return TypeCollection.empty();
                    }
                    return new TypeCollection(nestedTypes);
            }
        }

        final ArrayList<MemberInfo> results = new ArrayList<>(
            fields.length +
                methods.length +
                constructors.length +
                nestedTypes.length);

        Collections.addAll(results, fields);
        Collections.addAll(results, methods);
        Collections.addAll(results, constructors);
        Collections.addAll(results, nestedTypes);

        final MemberInfo[] array = new MemberInfo[results.size()];

        results.toArray(array);

        return new MemberCollection<>(MemberInfo.class, array);
    }

    @Override
    public FieldInfo getField(final String name, final int bindingFlags) {

        final FieldInfo[] candidates = getFieldCandidates(
            null,
            bindingFlags,
            false
        );

        if (candidates.length == 0) {
            return null;
        }

        FieldInfo match = null;
        boolean multipleStaticFieldMatches = false;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = candidates.length; i < n; i++) {
            final FieldInfo candidate = candidates[i];
            final Type candidateDeclaringType = candidate.getDeclaringType();

            if (match != null) {
                final Type matchDeclaringType = match.getDeclaringType();

                if (candidateDeclaringType == matchDeclaringType) {
                    throw Error.ambiguousMatch();
                }

                if (matchDeclaringType.isInterface() && candidateDeclaringType.isInterface()) {
                    multipleStaticFieldMatches = true;
                }
            }

            if (match == null || candidateDeclaringType.isSubclassOf(match.getDeclaringType()) || match.getDeclaringType().isInterface()) {
                match = candidate;
            }
        }

        if (multipleStaticFieldMatches && match.getDeclaringType().isInterface()) {
            throw Error.ambiguousMatch();
        }

        return match;
    }

    @Override
    public MethodInfo getMethod(
        final String name,
        final int bindingFlags,
        final CallingConvention callingConvention,
        final Type... parameterTypes) {

        final MethodInfo[] candidates = getMethodBaseCandidates(
            _methods,
            name,
            bindingFlags,
            callingConvention,
            parameterTypes,
            false
        );

        if (candidates.length == 0) {
            return null;
        }

        if (parameterTypes == null || parameterTypes.length == 0) {
            if (candidates.length == 1) {
                return candidates[0];
            }
            else if (parameterTypes == null) {
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0, n = candidates.length; i < n; i++) {
                    final MethodInfo method = candidates[i];
                    if (!Binder.compareMethodSignatureAndName(method, candidates[0])) {
                        throw Error.ambiguousMatch();
                    }
                }

                // All the methods have the exact same name and sig so return the most derived one.
                return (MethodInfo) Binder.findMostDerivedNewSlotMethod(candidates, candidates.length);
            }
        }

        return (MethodInfo) DefaultBinder.selectMethod(bindingFlags, candidates, parameterTypes);
    }

    @Override
    public ConstructorInfo getConstructor(
        final int bindingFlags,
        final CallingConvention callingConvention,
        final Type... parameterTypes) {

        final ConstructorInfo[] candidates = getMethodBaseCandidates(
            _constructors,
            null,
            bindingFlags,
            callingConvention,
            parameterTypes,
            false
        );

        if (candidates.length == 0) {
            return null;
        }

        if (parameterTypes == null || parameterTypes.length == 0) {
            if (candidates.length == 1) {
                return candidates[0];
            }
            else if (parameterTypes == null) {
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0, n = candidates.length; i < n; i++) {
                    final ConstructorInfo constructor = candidates[i];
                    if (!Binder.compareMethodSignatureAndName(constructor, candidates[0])) {
                        throw Error.ambiguousMatch();
                    }
                }

                // All the methods have the exact same name and sig so return the most derived one.
                return (ConstructorInfo) Binder.findMostDerivedNewSlotMethod(candidates, candidates.length);
            }
        }

        return (ConstructorInfo) DefaultBinder.selectMethod(bindingFlags, candidates, parameterTypes);
    }

    @Override
    public MemberCollection<? extends MemberInfo> getMembers(final int bindingFlags) {
        return MemberCollection.empty();
    }

    @Override
    public FieldCollection getFields(final int bindingFlags) {
        final FieldInfo[] candidates = getFieldCandidates(null, bindingFlags, false);

        if (candidates == null || candidates.length == 0) {
            return FieldCollection.empty();
        }

        return new FieldCollection(candidates);
    }

    @Override
    public MethodCollection getMethods(final int bindingFlags, final CallingConvention callingConvention) {
        final MethodInfo[] candidates = getMethodBaseCandidates(
            _methods,
            null,
            bindingFlags,
            callingConvention,
            null,
            false);

        if (candidates == null || candidates.length == 0) {
            return MethodCollection.empty();
        }

        return new MethodCollection(candidates);
    }

    @Override
    public ConstructorCollection getConstructors(final int bindingFlags) {
        final ConstructorInfo[] candidates = getMethodBaseCandidates(
            _constructors,
            null,
            bindingFlags,
            CallingConvention.Any,
            null,
            false);

        if (candidates == null || candidates.length == 0) {
            return ConstructorCollection.empty();
        }

        return new ConstructorCollection(candidates);
    }

    @Override
    public TypeCollection getNestedTypes(final int bindingFlags) {
        return TypeCollection.empty();
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public String getName() {
        return _class.getName();
    }

    @Override
    public Type getDeclaringType() {
        return of(_class.getDeclaringClass());
    }

    @Override
    int getModifiers() {
        return _class.getModifiers();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MEMBER LOOKUP                                                                                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("PackageVisibleField")
    private final static class FilterOptions {
        final String name;
        final boolean prefixLookup;
        final boolean ignoreCase;
        final MemberListOptions listOptions;

        FilterOptions(final String name, final boolean prefixLookup, final boolean ignoreCase, final MemberListOptions listOptions) {
            this.name = name;
            this.prefixLookup = prefixLookup;
            this.ignoreCase = ignoreCase;
            this.listOptions = listOptions;
        }
    }

    private static FilterOptions getFilterOptions(final String name, final int bindingFlags, final boolean allowPrefixLookup) {
        String filterName = name;
        boolean prefixLookup = false;
        boolean ignoreCase = false;
        MemberListOptions listOptions = MemberListOptions.All;

        if (name != null) {
            if (any(bindingFlags, BindingFlags.IgnoreCase)) {
                filterName = name.toLowerCase();
                ignoreCase = true;
                listOptions = MemberListOptions.CaseInsensitive;
            }
            else {
                listOptions = MemberListOptions.CaseSensitive;
            }

            if (allowPrefixLookup && name.endsWith("*")) {
                filterName = name.substring(0, name.length() - 1);
                prefixLookup = true;
                listOptions = MemberListOptions.All;
            }
        }

        return new FilterOptions(filterName, prefixLookup, ignoreCase, listOptions);
    }

    @SuppressWarnings("unchecked")
    private <T extends MethodBase> T[] getMethodBaseCandidates(
        final MemberCollection<T> source,
        final String name,
        final int bindingFlags,
        final CallingConvention callingConvention,
        final Type[] parameterTypes,
        final boolean allowPrefixLookup) {

        final FilterOptions filterOptions = getFilterOptions(name, bindingFlags, allowPrefixLookup);

        List<MethodBase> candidates = null;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = source.size(); i < n; i++) {
            final MethodBase method = source.get(i);

            final boolean passesFilter = filterMethodBase(
                method,
                BindingFlags.fromMember(method),
                bindingFlags,
                callingConvention,
                parameterTypes
            );

            if (!passesFilter) {
                continue;
            }

            if (filterOptions.prefixLookup) {
                if (!filterApplyPrefixLookup(method, filterOptions.name, filterOptions.ignoreCase)) {
                    continue;
                }
            }
            else if (name != null) {
                final String methodName = method.getName();
                if (filterOptions.ignoreCase ? !name.equalsIgnoreCase(methodName) : !name.equals(methodName)) {
                    continue;
                }
            }

            if (candidates == null) {
                candidates = new ArrayList<>(n);
            }

            candidates.add(method);
        }

        if (candidates == null) {
            return null;
        }

        final T[] results = (T[]) Array.newInstance(source.getMemberType(), candidates.size());

        candidates.toArray((Object[]) results);

        return results;
    }

    @SuppressWarnings("unchecked")
    private final FieldInfo[] getFieldCandidates(
        final String name,
        final int bindingFlags,
        final boolean allowPrefixLookup) {

        final FieldCollection fields = _fields;
        final int flags = bindingFlags ^ BindingFlags.DeclaredOnly;
        final FilterOptions filterOptions = getFilterOptions(name, flags, allowPrefixLookup);


        List<FieldInfo> candidates = null;

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, n = fields.size(); i < n; i++) {
            final FieldInfo field = fields.get(i);
            final int fieldFlags = BindingFlags.fromMember(field);

            if (!all(flags, fieldFlags)) {
                continue;
            }

            if (filterOptions.prefixLookup) {
                if (!filterApplyPrefixLookup(field, filterOptions.name, filterOptions.ignoreCase)) {
                    continue;
                }
            }
            else if (name != null) {
                final String methodName = field.getName();
                if (filterOptions.ignoreCase ? !name.equalsIgnoreCase(methodName) : !name.equals(methodName)) {
                    continue;
                }
            }

            if (candidates == null) {
                candidates = new ArrayList<>(n);
            }

            candidates.add(field);
        }

        if (candidates == null) {
            return null;
        }

        final FieldInfo[] results = new FieldInfo[candidates.size()];

        return candidates.toArray(results);
    }

    private Type[] getNestedTypeCandidates(final String fullName, final int bindingFlags, final boolean allowPrefixLookup) {
        final int flags = bindingFlags & ~BindingFlags.Static;
        final FilterOptions filterOptions = getFilterOptions(fullName, flags, allowPrefixLookup);

/*
        SplitName(fullname, out name, out ns);
        RuntimeType.FilterHelper(bindingAttr, ref name, allowPrefixLookup, out prefixLookup, out ignoreCase, out listType);

        CerArrayList<RuntimeType> cache = Cache.GetNestedTypeList(listType, name);

        List<Type> candidates = new List<Type>(cache.Count);
        for (int i = 0; i < cache.Count; i++)
        {
            RuntimeType nestedClass = cache[i];
            if (RuntimeType.FilterApplyType(nestedClass, bindingAttr, name, prefixLookup, ns))
            {
                candidates.Add(nestedClass);
            }
        }

        return candidates.ToArray();
*/
        return EmptyTypes;
    }


    private boolean filterApplyPrefixLookup(final MemberInfo method, final String name, final boolean ignoreCase) {
        final String methodName = method.getName();
        if (ignoreCase) {
            if (!StringEx.startsWithIgnoreCase(methodName.toLowerCase(), name)) {
                return false;
            }
        }
        else {
            if (!methodName.toLowerCase().startsWith(name)) {
                return false;
            }
        }
        return true;
    }
}