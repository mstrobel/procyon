package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.ReadOnlyList;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.nio.JavacPathFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

import javax.lang.model.type.TypeKind;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.util.*;

import static com.strobel.reflection.Flags.all;
import static com.strobel.reflection.Flags.any;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("unchecked")
public abstract class Type<T> extends MemberInfo implements java.lang.reflect.Type {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONSTANTS                                                                                                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final Binder DefaultBinder = null;
    public static final char Delimiter = '.';
    public static final Missing Value = new Missing();
    public static final Type[] EmptyTypes = new Type[0];

    public static final Type NoType = new NoType();
    public static final Type NullType = new NullType();

    protected static final Object[] EmptyObjects = new Object[0];
    protected static final String[] EmptyStrings = new String[0];
    protected static final MethodInfo[] EmptyMethods = new MethodInfo[0];
    protected static final ConstructorInfo[] EmptyConstructors = new ConstructorInfo[0];
    protected static final FieldInfo[] EmptyFields = new FieldInfo[0];
    protected static final MemberInfo[] EmptyMembers = new MemberInfo[0];

    protected static final int DefaultLookup = BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS                                                                                                       //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected Type() {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTES                                                                                                         //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isNested() {
        return getDeclaringType() != null;
    }

    public boolean isVisible() {
        throw ContractUtils.unreachable();
    }

    public final boolean isClass() {
        return (getModifiers() & (Modifier.INTERFACE | ENUM_MODIFIER)) == 0;
    }

    public final boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    public final boolean isEnum() {
        return (getModifiers() & ENUM_MODIFIER) != 0;
    }

    public final boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public boolean isArray() {
        return false;
    }

    public boolean isGenericType() {
        return !getTypeBindings().isEmpty();
    }

    public boolean isGenericTypeDefinition() {
        if (!isGenericType()) {
            return false;
        }

        final TypeBindings typeArguments = getTypeBindings();

        return !typeArguments.isEmpty() &&
               !typeArguments.hasBoundParameters();
    }

    public boolean isGenericParameter() {
        return false;
    }

    public boolean isPrimitive() {
        return false;
    }

    public boolean hasElementType() {
        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // REFLECTION METHODS                                                                                                 //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public TypeKind getKind() {
        return TypeKind.DECLARED;
    }

    public Type getBaseType() {
        return Type.of(Object.class);
    }

    public TypeList getInterfaces() {
        return TypeList.empty();
    }

    public abstract Class<T> getErasedClass();

    public MethodInfo getDeclaringMethod() {
        return null;
    }

    public Type getElementType() {
        throw Error.noElementType(this);
    }

    public int getGenericParameterPosition() {
        throw Error.notGenericParameter(this);
    }

    protected TypeBindings getTypeBindings() {
        return TypeBindings.empty();
    }

    public TypeList getTypeArguments() {
        return getTypeBindings().getBoundTypes();
    }

    public TypeList getGenericTypeParameters() {
        return getTypeBindings().getGenericParameters();
    }

    public Type getGenericTypeDefinition() {
        throw ContractUtils.unreachable();
    }

    public boolean containsGenericParameters() {
        if (hasElementType()) {
            return getRootElementType().containsGenericParameters();
        }

        if (isGenericParameter()) {
            return true;
        }

        if (!isGenericType()) {
            return false;
        }

        final TypeBindings typeArguments = getTypeBindings();

        for (int i = 0, n = typeArguments.size(); i < n; i++) {
            if (typeArguments.getBoundType(i).containsGenericParameters()) {
                return true;
            }
        }

        return false;
    }

    public boolean isUnbound() {
        return isWildcardType() &&
               getLowerBound() == NoType &&
               getUpperBound() == Types.Object;
    }

    public boolean isExtendsBound() {
        return isGenericParameter() ||
               isWildcardType() && getLowerBound() == NoType;
    }

    public boolean isSuperBound() {
        return isWildcardType() &&
               (getLowerBound() != NoType || getUpperBound() == Types.Object);
    }

    public Type<?> getUpperBound() {
        throw Error.notGenericParameter(this);
    }

    public Type<?> getLowerBound() {
        return NoType;
    }

    public boolean isEquivalentTo(final Type other) {
        return other == this;
    }

    public boolean isSubType(final Type type) {
        Type current = this;

        if (current == type) {
            return false;
        }

        while (current != null && current != Type.NoType) {
            if (current.equals(type)) {
                return true;
            }
            current = current.getBaseType();
        }

        return false;
    }

    public boolean isInstance(final Object o) {
        return o != null &&
               isAssignableFrom(of(o.getClass()));
    }

    @SuppressWarnings("UnusedParameters")
    public boolean implementsInterface(final Type interfaceType) {
        Type t = this;

        while (t != null && t != Type.NoType) {
            final TypeList interfaces = t.getInterfaces();

            for (int i = 0, n = interfaces.size(); i < n; i++) {
                final Type type = interfaces.get(i);
                if (type.isEquivalentTo(interfaceType) || type.implementsInterface(interfaceType)) {
                    return true;
                }
            }

            t = t.getBaseType();
        }

        return false;
    }

    public boolean isAssignableFrom(final Type type) {
/*
        if (type == null) {
            return false;
        }

        if (type == NoType) {
            return true;
        }

        if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(this, type)) {
            return true;
        }

        if (type.isSubType(this)) {
            return true;
        }

        if (this.isInterface()) {
            return type.implementsInterface(this);
        }
        else if (isGenericParameter()) {
            return getUpperBound().isAssignableFrom(type) &&
                   type.isAssignableFrom(getLowerBound());
        }

        return false;
*/
        return Helper.isAssignable(this, type);
    }

    public Package getPackage() {
        return getErasedClass().getPackage();
    }

    public boolean isCompoundType() {
        return false;
    }

    public boolean isWildcardType() {
        return false;
    }

    public boolean isSynthetic() {
        return false;
    }

    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitType(this, parameter);
    }

    @Override
    public int hashCode() {
        return Helper.hashCode(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MEMBER INFO                                                                                                        //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public final MemberList getMember(final String name) {
        return getMember(name, DefaultLookup, EnumSet.noneOf(MemberType.class));
    }

    public final MemberList getMember(final String name, final MemberType memberType, final MemberType... memberTypes) {
        return getMember(name, DefaultLookup, EnumSet.of(memberType, memberTypes));
    }

    public MemberList getMember(final String name, final int bindingFlags, final Set<MemberType> memberTypes) {
        VerifyArgument.notNull(name, "name");

        if (memberTypes == null || memberTypes.isEmpty()) {
            return MemberList.empty();
        }

        MethodInfo[] methods = EmptyMethods;
        ConstructorInfo[] constructors = EmptyConstructors;
        FieldInfo[] fields = EmptyFields;
        Type[] nestedTypes = EmptyTypes;

        if (memberTypes.contains(MemberType.Field)) {
            fields = getFieldCandidates(name, bindingFlags, true);
        }

        if (memberTypes.contains(MemberType.Method)) {
            if (all(bindingFlags, BindingFlags.Instance)) {
                methods = getMethodBaseCandidates(getResolvedInstanceMethods(), name, bindingFlags, CallingConvention.Any, null, true);
            }
            if (all(bindingFlags, BindingFlags.Static)) {
                methods = ArrayUtilities.append(
                    methods,
                    getMethodBaseCandidates(getResolvedStaticMethods(), name, bindingFlags, CallingConvention.Any, null, true)
                );
            }
        }

        if (memberTypes.contains(MemberType.Constructor)) {
            constructors = getMethodBaseCandidates(getResolvedConstructors(), name, bindingFlags, CallingConvention.Any, null, true);
        }

        if (memberTypes.contains(MemberType.NestedType)) {
            nestedTypes = getNestedTypeCandidates(name, bindingFlags, true);
        }

        if (fields == null) {
            fields = EmptyFields;
        }

        if (methods == null) {
            methods = EmptyMethods;
        }

        if (constructors == null) {
            constructors = EmptyConstructors;
        }

        if (nestedTypes == null) {
            nestedTypes = EmptyTypes;
        }

        if (memberTypes.size() == 1) {
            final MemberType memberType = memberTypes.iterator().next();
            switch (memberType) {
                case Constructor:
                    if (constructors.length == 0) {
                        return ConstructorList.empty();
                    }
                    return new ConstructorList(constructors);

                case Field:
                    if (fields.length == 0) {
                        return FieldList.empty();
                    }
                    return new FieldList(fields);

                case Method:
                    if (methods.length == 0) {
                        return MethodList.empty();
                    }
                    return new MethodList(methods);

                case NestedType:
                    if (nestedTypes.length == 0) {
                        return TypeList.empty();
                    }
                    return new TypeList(nestedTypes);
            }
        }

        final ArrayList<MemberInfo> results = new ArrayList<>(
            fields.length +
            methods.length +
            constructors.length +
            nestedTypes.length
        );

        Collections.addAll(results, fields);
        Collections.addAll(results, methods);
        Collections.addAll(results, constructors);
        Collections.addAll(results, nestedTypes);

        final MemberInfo[] array = new MemberInfo[results.size()];

        results.toArray(array);

        return new MemberList<>(MemberInfo.class, array);
    }

    public FieldInfo getField(final String name) {
        return getField(name, DefaultLookup);
    }

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

            if (match == null || candidateDeclaringType.isSubType(match.getDeclaringType()) || match.getDeclaringType().isInterface()) {
                match = candidate;
            }
        }

        if (multipleStaticFieldMatches && match.getDeclaringType().isInterface()) {
            throw Error.ambiguousMatch();
        }

        return match;
    }

    public MethodInfo getMethod(final Method rawMethod) {
        getResolvedInstanceMethods();
        getResolvedStaticMethods();
        return _rawMethodMap.get(rawMethod);
    }

    public MethodInfo getMethod(final String name, final Type... parameterTypes) {
        return getMethod(name, DefaultLookup, parameterTypes);
    }

    public MethodInfo getMethod(final String name, final int bindingFlags, final Type... parameterTypes) {
        return getMethod(name, bindingFlags, CallingConvention.Any, parameterTypes);
    }

    public MethodInfo getMethod(
        final String name,
        final int bindingFlags,
        final CallingConvention callingConvention,
        final Type... parameterTypes) {

        MethodInfo[] candidates = EmptyMethods;

        if (all(bindingFlags, BindingFlags.Instance)) {
            candidates = getMethodBaseCandidates(
                getResolvedInstanceMethods(),
                name,
                bindingFlags,
                callingConvention,
                parameterTypes,
                false
            );
        }

        if (all(bindingFlags, BindingFlags.Static)) {
            candidates = ArrayUtilities.append(
                candidates,
                getMethodBaseCandidates(
                    getResolvedStaticMethods(),
                    name,
                    bindingFlags,
                    callingConvention,
                    parameterTypes,
                    false
                )
            );
        }

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
                return (MethodInfo)Binder.findMostDerivedNewSlotMethod(candidates, candidates.length);
            }
        }

        return (MethodInfo)DefaultBinder.selectMethod(bindingFlags, candidates, parameterTypes);
    }

    public ConstructorInfo getConstructor(final Type... parameterTypes) {
        return getConstructor(DefaultLookup, parameterTypes);
    }

    public ConstructorInfo getConstructor(final int bindingFlags, final Type... parameterTypes) {
        return getConstructor(bindingFlags, CallingConvention.Any, parameterTypes);
    }

    public ConstructorInfo getConstructor(
        final int bindingFlags,
        final CallingConvention callingConvention,
        final Type... parameterTypes) {

        final ConstructorInfo[] candidates = getMethodBaseCandidates(
            getResolvedConstructors(),
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
                return (ConstructorInfo)Binder.findMostDerivedNewSlotMethod(candidates, candidates.length);
            }
        }

        return (ConstructorInfo)DefaultBinder.selectMethod(bindingFlags, candidates, parameterTypes);
    }

    public final MemberList getMembers() {
        return getMembers(DefaultLookup, EnumSet.allOf(MemberType.class));
    }

    public final MemberList getMembers(final Set<MemberType> memberTypes) {
        return getMembers(DefaultLookup, memberTypes);
    }

    public final MemberList getMembers(final MemberType memberType, final MemberType... memberTypes) {
        return getMembers(DefaultLookup, EnumSet.of(memberType, memberTypes));
    }

    public final MemberList getMembers(final int bindingFlags) {
        return getMembers(bindingFlags, EnumSet.allOf(MemberType.class));
    }

    public final MemberList getMembers(final int bindingFlags, final MemberType memberType, final MemberType... memberTypes) {
        return getMembers(bindingFlags, EnumSet.of(memberType, memberTypes));
    }

    public MemberList getMembers(final int bindingFlags, final Set<MemberType> memberTypes) {
        MethodInfo[] methods = EmptyMethods;
        ConstructorInfo[] constructors = EmptyConstructors;
        FieldInfo[] fields = EmptyFields;
        Type[] nestedTypes = EmptyTypes;

        if (memberTypes.contains(MemberType.Field)) {
            fields = getFieldCandidates(null, bindingFlags, false);
        }

        if (memberTypes.contains(MemberType.Method)) {
            if (all(bindingFlags, BindingFlags.Instance)) {
                methods = getMethodBaseCandidates(getResolvedInstanceMethods(), null, bindingFlags, CallingConvention.Any, null, false);
            }
            if (all(bindingFlags, BindingFlags.Static)) {
                methods = ArrayUtilities.append(
                    methods,
                    getMethodBaseCandidates(getResolvedStaticMethods(), null, bindingFlags, CallingConvention.Any, null, false)
                );
            }
        }

        if (memberTypes.contains(MemberType.Constructor)) {
            constructors = getMethodBaseCandidates(getResolvedConstructors(), null, bindingFlags, CallingConvention.Any, null, false);
        }

        if (memberTypes.contains(MemberType.NestedType)) {
            nestedTypes = getNestedTypeCandidates(null, bindingFlags, false);
        }

        if (fields == null) {
            fields = EmptyFields;
        }

        if (methods == null) {
            methods = EmptyMethods;
        }

        if (constructors == null) {
            constructors = EmptyConstructors;
        }

        if (nestedTypes == null) {
            nestedTypes = EmptyTypes;
        }

        if (memberTypes.size() == 1) {
            final MemberType memberType = memberTypes.iterator().next();
            switch (memberType) {
                case Constructor:
                    if (constructors.length == 0) {
                        return ConstructorList.empty();
                    }
                    return new ConstructorList(constructors);

                case Field:
                    if (fields.length == 0) {
                        return FieldList.empty();
                    }
                    return new FieldList(fields);

                case Method:
                    if (methods.length == 0) {
                        return MethodList.empty();
                    }
                    return new MethodList(methods);

                case NestedType:
                    if (nestedTypes.length == 0) {
                        return TypeList.empty();
                    }
                    return new TypeList(nestedTypes);
            }
        }

        final ArrayList<MemberInfo> results = new ArrayList<>(
            fields.length +
            methods.length +
            constructors.length +
            nestedTypes.length
        );

        Collections.addAll(results, fields);
        Collections.addAll(results, methods);
        Collections.addAll(results, constructors);
        Collections.addAll(results, nestedTypes);

        final MemberInfo[] array = new MemberInfo[results.size()];

        results.toArray(array);

        return new MemberList<>(MemberInfo.class, array);
    }

    public FieldList getFields() {
        return getFields(DefaultLookup);
    }

    public FieldList getFields(final int bindingFlags) {
        final FieldInfo[] candidates = getFieldCandidates(null, bindingFlags, false);

        if (candidates == null || candidates.length == 0) {
            return FieldList.empty();
        }

        return new FieldList(candidates);
    }

    public MethodList getMethods() {
        return getMethods(DefaultLookup, CallingConvention.Any);
    }

    public MethodList getMethods(final int bindingFlags) {
        return getMethods(bindingFlags, CallingConvention.Any);
    }

    public MethodList getMethods(final int bindingFlags, final CallingConvention callingConvention) {
        MethodInfo[] candidates = EmptyMethods;

        if (all(bindingFlags, BindingFlags.Instance)) {
            candidates = getMethodBaseCandidates(
                getResolvedInstanceMethods(),
                null,
                bindingFlags,
                callingConvention,
                null,
                false
            );
        }

        if (all(bindingFlags, BindingFlags.Static)) {
            candidates = ArrayUtilities.append(
                candidates,
                getMethodBaseCandidates(
                    getResolvedStaticMethods(),
                    null,
                    bindingFlags,
                    callingConvention,
                    null,
                    false
                )
            );
        }

        if (candidates == null || candidates.length == 0) {
            return MethodList.empty();
        }

        return new MethodList(candidates);
    }

    public ConstructorList getConstructors() {
        return getConstructors(DefaultLookup);
    }

    public ConstructorList getConstructors(final int bindingFlags) {
        final ConstructorInfo[] candidates = getMethodBaseCandidates(
            getResolvedConstructors(),
            null,
            bindingFlags,
            CallingConvention.Any,
            null,
            false
        );

        if (candidates == null || candidates.length == 0) {
            return ConstructorList.empty();
        }

        return new ConstructorList(candidates);
    }

    public TypeList getNestedTypes() {
        return getNestedTypes(DefaultLookup);
    }

    public TypeList getNestedTypes(final int bindingFlags) {
        final Type[] candidates = getNestedTypeCandidates(null, bindingFlags, false);

        if (ArrayUtilities.isNullOrEmpty(candidates)) {
            return TypeList.empty();
        }

        return list(candidates);
    }

    public Type<?> getNestedType(final String fullName) {
        return getNestedType(fullName, DefaultLookup);
    }

    public Type<?> getNestedType(final String fullName, final int bindingFlags) {
        VerifyArgument.notNull(fullName, "fullName");

        final String name;

        if (fullName != null) {
            final String ownerName = getName();

            final boolean isLongName = all(bindingFlags, BindingFlags.IgnoreCase)
                                       ? StringUtilities.startsWithIgnoreCase(fullName, ownerName)
                                       : fullName.startsWith(ownerName);
            if (isLongName) {
                if (fullName.length() == ownerName.length()) {
                    return null;
                }
                name = fullName.substring(ownerName.length() + 1);
            }
            else {
                name = fullName;
            }
        }
        else {
            name = null;
        }

        final int flags = bindingFlags & ~BindingFlags.Static;
        final FilterOptions filterOptions = getFilterOptions(name, flags, false);

        final TypeList nestedTypes = getResolvedNestedTypes();

        Type<?> match = null;

        for (int i = 0, n = nestedTypes.size(); i < n; i++) {
            final Type<?> nestedType = nestedTypes.get(i);
            if (filterApplyType(nestedType, bindingFlags, name, filterOptions.prefixLookup)) {
                if (match != null) {
                    throw Error.ambiguousMatch();
                }
                match = nestedType;
            }
        }

        return match;
    }

    public Object[] getEnumConstants() {
        throw Error.notEnumType(this);
    }

    public String[] getEnumNames() {
        throw Error.notEnumType(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TRANSFORMATION METHODS                                                                                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Type<T[]> makeArrayType() {
        return CACHE.getArrayType(this);
    }

    public final Type makeGenericType(final TypeList typeArguments) {
        VerifyArgument.noNullElements(typeArguments, "typeArguments");
        return makeGenericTypeCore(typeArguments);
    }

    public final Type makeGenericType(final Type... typeArguments) {
        return makeGenericTypeCore(list(VerifyArgument.noNullElements(typeArguments, "typeArguments")));
    }

    @SuppressWarnings("UnusedParameters")
    protected Type makeGenericTypeCore(final TypeList typeArguments) {
        throw Error.notGenericType(this);
    }

    public static <T> WildcardType<T> makeExtendsWildcard(final Type<T> bound) {
        return new WildcardType<>(
            VerifyArgument.notNull(bound, "bound"),
            NoType
        );
    }

    public static WildcardType<?> makeSuperWildcard(final Type<?> bound) {
        return new WildcardType<>(
            Types.Object,
            bound
        );
    }

    public static WildcardType<?> makeWildcard() {
        return new WildcardType<>(
            Types.Object,
            NoType
        );
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // INTERNAL METHODS                                                                                                   //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Type getRootElementType() {
        Type rootElementType = this;

        while (rootElementType.hasElementType()) {
            rootElementType = rootElementType.getElementType();
        }

        return rootElementType;
    }

    Type getMostSpecificType(final Type t1, final Type t2) {
        if (t1.isSubType(t2)) {
            return t1;
        }
        if (t2.isSubType(t1)) {
            return t2;
        }
        return null;
    }

    boolean filterMethodBase(
        final MethodBase method,
        final int methodFlags,
        final int bindingFlags,
        final CallingConvention callingConventions,
        final Type... argumentTypes) {

        final int flags = bindingFlags ^ BindingFlags.DeclaredOnly;

        if ((flags & methodFlags) != methodFlags) {
            return false;
        }

        if (callingConventions != null &&
            callingConventions != CallingConvention.Any &&
            callingConventions != method.getCallingConvention()) {

            return false;
        }

        if (argumentTypes != null) {
            final ParameterList parameters = method.getParameters();

            final int definedParameterCount = parameters.size();
            final int suppliedArgumentCount = argumentTypes.length;

            if (suppliedArgumentCount != definedParameterCount) {

                // If the number of supplied arguments differs than the number in the signature AND
                // we are not filtering for a dynamic call, i.e., InvokeMethod or CreateInstance,
                // then filter out the method.

                if (any(flags, BindingFlags.InvokeMethod | BindingFlags.CreateInstance)) {
                    return false;
                }

                if (method.getCallingConvention() == CallingConvention.VarArgs) {
                    if (definedParameterCount == 0) {
                        return false;
                    }

                    // If we're short by more than one argument, we can't bind to the VarArgs parameter.

                    if (suppliedArgumentCount < definedParameterCount - 1) {
                        return false;
                    }

                    final ParameterInfo lastParameter = parameters.get(definedParameterCount - 1);
                    final Type lastParameterType = lastParameter.getParameterType();

                    if (!lastParameterType.isArray()) {
                        return false;
                    }
                }
            }
            else if (all(flags, BindingFlags.ExactBinding) && !any(flags, BindingFlags.InvokeMethod)) {
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < definedParameterCount; i++) {
                    final Type parameterType = parameters.get(i).getParameterType();
                    if (argumentTypes[i] != null && !parameterType.isEquivalentTo(argumentTypes[i])) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // NAME AND SIGNATURE FORMATTING                                                                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        return getBriefDescription();
    }

    @Override
    public String getName() {
        return getBriefDescription();
    }

    /**
     * Method that returns full generic signature of the type; suitable
     * as signature for things like ASM package.
     */
    public String getSignature() {
        return appendSignature(new StringBuilder()).toString();
    }

    /**
     * Method that returns type erased signature of the type; suitable
     * as non-generic signature some packages need
     */
    public String getErasedSignature() {
        return appendErasedSignature(new StringBuilder()).toString();
    }

    /**
     * Human-readable full description of type, which includes specification
     * of super types (in brief format)
     */
    public String getFullDescription() {
        return appendFullDescription(new StringBuilder()).toString();
    }

    /**
     * Human-readable brief description of type, which does not include
     * information about super types.
     */
    public String getBriefDescription() {
        return appendBriefDescription(new StringBuilder()).toString();
    }

    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        s.append(getErasedClass().getName());

        if (isGenericType()) {
            final TypeList typeArguments = getTypeBindings().getBoundTypes();
            final int count = typeArguments.size();
            if (count > 0) {
                s.append('<');
                //noinspection ForLoopReplaceableByForEach
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

    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        return sb.append(getErasedClass().getName());
    }

    public StringBuilder appendFullDescription(final StringBuilder sb) {
        StringBuilder s = appendBriefDescription(sb);

        final Type baseType = getBaseType();

        if (baseType != null && baseType != Types.Object) {
            s.append(" extends ");
            s = baseType.appendBriefDescription(s);
        }

        final TypeList interfaces = getInterfaces();
        final int interfaceCount = interfaces.size();

        if (interfaceCount > 0) {
            s.append(" implements ");
            for (int i = 0; i < interfaceCount; ++i) {
                if (i != 0) {
                    s.append(",");
                }
                s = interfaces.get(i).appendBriefDescription(s);
            }
        }

        return s;
    }

    public StringBuilder appendSignature(final StringBuilder sb) {
        return _appendClassSignature(sb);
    }

    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return _appendErasedClassSignature(sb);
    }

    protected StringBuilder _appendClassSignature(final StringBuilder sb) {
        StringBuilder s = sb;

        s.append('L');
        s = _appendClassName(s, false);

        if (isGenericType()) {
            final TypeList genericParameters = getTypeBindings().getBoundTypes();
            final int count = genericParameters.size();

            if (count > 0) {
                s.append('<');
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < count; ++i) {
                    s = genericParameters.get(i).appendErasedSignature(s);
                }
                s.append('>');
            }
        }

        s.append(';');
        return s;
    }

    protected StringBuilder _appendErasedClassSignature(StringBuilder sb) {
        sb.append('L');
        sb = _appendClassName(sb, false);
        sb.append(';');
        return sb;
    }

    protected StringBuilder _appendClassDescription(final StringBuilder sb) {
        StringBuilder s = sb;

        s.append(getErasedClass().getName());

        if (isGenericType()) {
            final TypeList typeArguments = getTypeBindings().getBoundTypes();
            final int count = typeArguments.size();
            if (count > 0) {
                s.append('<');
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < count; ++i) {
                    s = typeArguments.get(i)._appendErasedClassSignature(s);
                }
                s.append('>');
            }
        }

        return s;
    }

    protected StringBuilder _appendClassName(final StringBuilder sb, final boolean dottedName) {

        final Class<?> erasedClass = getErasedClass();
        final Package classPackage = erasedClass.getPackage();
        final String name = erasedClass.getName();

        if (dottedName) {
            return sb.append(name);
        }

        final int start;

        if (classPackage != null) {
            final String packageName = classPackage.getName();
            if (packageName.length() != 0) {
                for (int i = 0, n = packageName.length(); i < n; i++) {
                    char c = name.charAt(i);
                    if (c == '.') {
                        c = '/';
                    }
                    sb.append(c);
                }
                sb.append('/');
                start = packageName.length() + 1;
            }
            else {
                start = 0;
            }
        }
        else {
            start = 0;
        }

        sb.append(name, start, name.length());

        return sb;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // REFLECTED TYPE CACHE                                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    final static Object CACHE_LOCK = new Object();
    final static TypeCache CACHE;
    final static TypeResolver TYPE_RESOLVER;
    final static MemberResolver MEMBER_RESOLVER;
    final static Context CONTEXT;
    final static Resolver RESOLVER;
    final static JavaCompiler COMPILER;
    final static Type<?>[] PRIMITIVE_TYPES;

    static {
        synchronized (CACHE_LOCK) {
            CACHE = new TypeCache();
            TYPE_RESOLVER = new TypeResolver(CACHE);
            MEMBER_RESOLVER = new MemberResolver();

            final Context context = new Context();

            new JavacPathFileManager(context, true, Charset.defaultCharset());
            com.sun.tools.javac.code.Types.instance(context);
            Resolve.instance(context);
            Names.instance(context);

            COMPILER = JavaCompiler.instance(context);
            CONTEXT = context;
            RESOLVER = new Resolver(context);
            PRIMITIVE_TYPES = new PrimitiveType<?>[TypeKind.values().length];

            PrimitiveTypes.ensureRegistered();

            PRIMITIVE_TYPES[TypeKind.VOID.ordinal()] = PrimitiveTypes.Void;
            PRIMITIVE_TYPES[TypeKind.BOOLEAN.ordinal()] = PrimitiveTypes.Boolean;
            PRIMITIVE_TYPES[TypeKind.BYTE.ordinal()] = PrimitiveTypes.Byte;
            PRIMITIVE_TYPES[TypeKind.CHAR.ordinal()] = PrimitiveTypes.Character;
            PRIMITIVE_TYPES[TypeKind.SHORT.ordinal()] = PrimitiveTypes.Short;
            PRIMITIVE_TYPES[TypeKind.INT.ordinal()] = PrimitiveTypes.Integer;
            PRIMITIVE_TYPES[TypeKind.LONG.ordinal()] = PrimitiveTypes.Long;
            PRIMITIVE_TYPES[TypeKind.FLOAT.ordinal()] = PrimitiveTypes.Float;
            PRIMITIVE_TYPES[TypeKind.DOUBLE.ordinal()] = PrimitiveTypes.Double;

            Types.ensureRegistered();
        }
    }

    public static <T> Type<T> of(final Class<T> clazz) {
        synchronized (CACHE_LOCK) {
            final Type<T> reflectedType = (Type<T>)CACHE.find(clazz);

            if (reflectedType != null) {
                return reflectedType;
            }

            final Symbol.ClassSymbol symbol = (Symbol.ClassSymbol)COMPILER.resolveIdent(clazz.getName());

            loadAncestors(symbol);

            final Type<T> resolvedType = (Type<T>)RESOLVER.visit(symbol, null);

            if (resolvedType != null) {
                return resolvedType;
            }

            throw Error.couldNotResolveType(clazz);
        }
    }

    private static void loadAncestors(final Symbol.ClassSymbol symbol) {
        final com.sun.tools.javac.code.Type superclass = symbol.getSuperclass();

        if (superclass != null && superclass.getKind() != TypeKind.NONE && superclass.asElement() != symbol) {
            of(superclass);
        }

        for (final com.sun.tools.javac.code.Type type : symbol.getInterfaces()) {
            if (type != null && type.getKind() != TypeKind.NONE) {
                of(type);
            }
        }
    }

    static <T> Type<T> of(final com.sun.tools.javac.code.Type type) {
        if (type instanceof com.sun.tools.javac.code.Type.ArrayType) {
            return (Type<T>)of(((com.sun.tools.javac.code.Type.ArrayType)type).getComponentType()).makeArrayType();
        }

        final TypeKind typeKind = type.getKind();

        if (typeKind == TypeKind.VOID || typeKind.isPrimitive()) {
            return (Type<T>)PRIMITIVE_TYPES[typeKind.ordinal()];
        }

        synchronized (CACHE_LOCK) {
            Type<?> resultType = tryFind(type);

            if (resultType != null) {
                return (Type<T>)resultType;
            }

            loadAncestors((Symbol.ClassSymbol)type.asElement());

            resultType = RESOLVER.visit(type.asElement(), null);

            if (resultType != null) {
                return (Type<T>)resultType;
            }

            throw Error.couldNotResolveType(type);
        }
    }

    static Type<?> tryFind(final com.sun.tools.javac.code.Type type) {
        final TypeKind typeKind = type.getKind();

        if (typeKind == TypeKind.VOID || typeKind.isPrimitive()) {
            return PRIMITIVE_TYPES[typeKind.ordinal()];
        }

        if (typeKind != TypeKind.DECLARED) {
            return null;
        }

        synchronized (CACHE_LOCK) {
            final Class<?> clazz;
            final String className = type.asElement().flatName().toString();

            try {
                clazz = Class.forName(className);
            }
            catch (ClassNotFoundException e) {
                throw Error.couldNotResolveType(className);
            }

            return (Type<?>)CACHE.find(clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public static Type resolve(final java.lang.reflect.Type jdkType, final TypeBindings bindings) {
        VerifyArgument.notNull(jdkType, "jdkType");

        if (jdkType instanceof Class && bindings.isEmpty()) {
            return of((Class<?>)jdkType);
        }

        synchronized (CACHE_LOCK) {
            return TYPE_RESOLVER.resolve(jdkType, bindings != null ? bindings : TypeBindings.empty());
        }
    }

    public static CompoundType<?> makeCompoundType(final TypeList bounds) {
        VerifyArgument.notEmpty(bounds, "bounds");
        VerifyArgument.noNullElements(bounds, "bounds");

        final Type<?> baseType;
        final TypeList interfaces;

        if (!bounds.get(0).isInterface()) {
            baseType = bounds.get(0);
            interfaces = bounds.subList(1, bounds.size());
        }
        else {
            baseType = Types.Object;
            interfaces = bounds;
        }

        return makeCompoundType(baseType, interfaces);
    }

    public static CompoundType<?> makeCompoundType(final Type<?> baseType, final TypeList interfaces) {
        VerifyArgument.notNull(baseType, "baseType");
        VerifyArgument.noNullElements(interfaces, "interfaces");

        return makeCompoundTypeCore(baseType, interfaces);
    }

    private static <T> CompoundType<T> makeCompoundTypeCore(final Type<T> baseType, final TypeList interfaces) {
        if (baseType.isGenericParameter()) {
            throw Error.compoundTypeMayNotHaveGenericParameterBound();
        }

        for (int i = 0, n = interfaces.size(); i < n; i++) {
            final Type type = interfaces.get(i);

            if (type.isGenericParameter()) {
                throw Error.compoundTypeMayNotHaveGenericParameterBound();
            }

            if (!type.isInterface()) {
                throw Error.compoundTypeMayOnlyHaveOneClassBound();
            }
        }

        return new CompoundType<>(interfaces, baseType);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIST FACTORY METHODS                                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static TypeList list(final Type... types) {
        return new TypeList(types);
    }

    public static TypeList list(final List<? extends Type> types) {
        return new TypeList(types);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TYPE HIERARCHY AND MEMBER RESOLUTION INFO                                                                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Map<Method, MethodInfo> _rawMethodMap = new HashMap<>();

    private HierarchicType _mainType;

    private ReadOnlyList<HierarchicType> _typeHierarchy;
    private TypeList _resolvedNestedTypes;
    private ConstructorList _resolvedConstructors;
    private MethodList _resolvedInstanceMethods;
    private MethodList _resolvedStaticMethods;
    private FieldList _resolvedFields;

    ConstructorList getResolvedConstructors() {
        if (_resolvedConstructors == null) {
            synchronized (CACHE_LOCK) {
                if (_resolvedConstructors == null) {
                    _resolvedConstructors = resolveConstructors();
                }
            }
        }
        return _resolvedConstructors;
    }

    MethodList getResolvedInstanceMethods() {
        if (_resolvedInstanceMethods == null) {
            synchronized (CACHE_LOCK) {
                if (_resolvedInstanceMethods == null) {
                    _resolvedInstanceMethods = resolveInstanceMethods();
                    for (int i = 0, n = _resolvedInstanceMethods.size(); i < n; i++) {
                        final MethodInfo method = _resolvedInstanceMethods.get(i);
                        _rawMethodMap.put(method.getRawMethod(), method);
                    }
                }
            }
        }
        return _resolvedInstanceMethods;
    }

    MethodList getResolvedStaticMethods() {
        if (_resolvedStaticMethods == null) {
            synchronized (CACHE_LOCK) {
                if (_resolvedStaticMethods == null) {
                    _resolvedStaticMethods = resolveStaticMethods();
                }
            }
        }
        return _resolvedStaticMethods;
    }

    FieldList getResolvedFields() {
        if (_resolvedFields == null) {
            synchronized (CACHE_LOCK) {
                if (_resolvedFields == null) {
                    _resolvedFields = resolveFields();
                }
            }
        }
        return _resolvedFields;
    }

    TypeList getResolvedNestedTypes() {
        if (_resolvedNestedTypes == null) {
            synchronized (CACHE_LOCK) {
                if (_resolvedNestedTypes == null) {
                    _resolvedNestedTypes = resolveNestedTypes();
                }
            }
        }
        return _resolvedNestedTypes;
    }

    HierarchicType getMainType() {
        if (_mainType != null) {
            return _mainType;
        }

        synchronized (CACHE_LOCK) {
            if (_mainType == null) {
                final MemberResolver.Result result = MEMBER_RESOLVER.resolve(this);
                _mainType = result.mainType;
                _typeHierarchy = result.types;
            }
        }

        return _mainType;
    }

    ReadOnlyList<HierarchicType> getTypes() {
        if (_typeHierarchy != null) {
            return _typeHierarchy;
        }

        synchronized (CACHE_LOCK) {
            if (_typeHierarchy == null) {
                final MemberResolver.Result result = MEMBER_RESOLVER.resolve(this);
                _mainType = result.mainType;
                _typeHierarchy = result.types;
            }
        }

        return _typeHierarchy;
    }

    ReadOnlyList<HierarchicType> getMainTypeAndOverrides() {
        ReadOnlyList<HierarchicType> l = getTypes();
        final int end = getMainType().getPriority() + 1;
        if (end < l.size()) {
            l = l.subList(0, end);
        }
        return l;
    }

    ReadOnlyList<HierarchicType> getOverridesOnly() {
        final int index = getMainType().getPriority();
        if (index == 0) {
            return ReadOnlyList.emptyList();
        }
        return getTypes().subList(0, index);
    }

    protected List<Constructor<?>> getRawConstructors() {
        return Arrays.asList(getErasedClass().getDeclaredConstructors());
    }

    protected List<Method> getRawMethods() {
        return Arrays.asList(getErasedClass().getDeclaredMethods());
    }

    protected List<Field> getRawFields() {
        return Arrays.asList(getErasedClass().getDeclaredFields());
    }

    protected TypeList resolveNestedTypes() {
        return TypeList.empty();
    }

    protected ConstructorList resolveConstructors() {
        final LinkedHashMap<MethodKey, ConstructorInfo> constructors = new LinkedHashMap<>();
        final Type mainType = getMainType().getType();

        for (final Constructor constructor : getRawConstructors()) {
            final RawConstructor raw = new RawConstructor(mainType, constructor);
            constructors.put(raw.createKey(), resolveConstructor(raw));
        }

        if (constructors.size() == 0) {
            return ConstructorList.empty();
        }

        final ConstructorInfo[] constructorArray = new ConstructorInfo[constructors.size()];
        constructors.values().toArray(constructorArray);
        return new ConstructorList(constructorArray);
    }

    protected FieldList resolveFields() {
        final LinkedHashMap<String, FieldInfo> fields = new LinkedHashMap<>();

        for (int typeIndex = getMainTypeAndOverrides().size(); --typeIndex >= 0; ) {
            final HierarchicType thisType = getMainTypeAndOverrides().get(typeIndex);
            for (final Field jField : thisType.getType().getRawFields()) {
                final RawField raw = new RawField(thisType.getType(), jField);
                fields.put(raw.getName(), resolveField(raw));
            }
        }

        // and that's it?
        if (fields.size() == 0) {
            return FieldList.empty();
        }

        final FieldInfo[] fieldArray = new FieldInfo[fields.size()];
        fields.values().toArray(fieldArray);
        return new FieldList(fieldArray);
    }

    protected MethodList resolveStaticMethods() {
        final LinkedHashMap<MethodKey, MethodInfo> methods = new LinkedHashMap<>();

        for (final Method method : getMainType().getType().getRawMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                final RawMethod raw = new RawMethod(getMainType().getType(), method);
                methods.put(raw.createKey(), resolveMethod(raw));
            }
        }

        if (methods.size() == 0) {
            return MethodList.empty();
        }

        final MethodInfo[] methodArray = new MethodInfo[methods.size()];
        methods.values().toArray(methodArray);
        return new MethodList(methodArray);
    }

    protected MethodList resolveInstanceMethods() {
        final LinkedHashMap<MethodKey, MethodInfo> methods = new LinkedHashMap<>();

        for (final HierarchicType type : getMainTypeAndOverrides()) {
            for (final Method method : type.getType().getRawMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }

                final RawMethod raw = new RawMethod(type.getType(), method);

                final MethodKey key = raw.createKey();
                final MethodInfo old = methods.get(key);

                if (old == null) {
                    final MethodInfo newMethod = resolveMethod(raw);
                    methods.put(key, newMethod);
                }
            }
        }

        if (methods.size() == 0) {
            return MethodList.empty();
        }

        final MethodInfo[] methodArray = new MethodInfo[methods.size()];
        methods.values().toArray(methodArray);
        return new MethodList(methodArray);
    }

    protected ConstructorInfo resolveConstructor(final RawConstructor raw) {
        final Type context = raw.getDeclaringType();
        final TypeBindings bindings = context.getTypeBindings();
        final Constructor<?> ctor = raw.getRawMember();
        final java.lang.reflect.Type[] rawTypes = ctor.getGenericParameterTypes();
        final ParameterList parameterList;
        if (rawTypes == null || rawTypes.length == 0) {
            parameterList = ParameterList.empty();
        }
        else {
            final ParameterInfo[] parameters = new ParameterInfo[rawTypes.length];
            for (int i = 0, len = rawTypes.length; i < len; ++i) {
                parameters[i] = new ParameterInfo("p" + i, TYPE_RESOLVER.resolve(rawTypes[i], bindings));
            }
            parameterList = new ParameterList(parameters);
        }
        return new ReflectedConstructor(context, ctor, parameterList, TypeList.empty());
    }

    protected FieldInfo resolveField(final RawField raw) {
        final Type context = raw.getDeclaringType();
        final Field field = raw.getRawMember();
        final Type type = TYPE_RESOLVER.resolve(field.getGenericType(), context.getTypeBindings());

        return new ReflectedField(context, field, type);
    }

    protected MethodInfo resolveMethod(final RawMethod raw) {
        final Type context = raw.getDeclaringType();
        final TypeBindings bindings = context.getTypeBindings();
        final Method m = raw.getRawMember();
        final java.lang.reflect.Type rawType = m.getGenericReturnType();
        final java.lang.reflect.Type[] rawTypes = m.getGenericParameterTypes();
        final TypeVariable<Method>[] typeVariables = m.getTypeParameters();
        final ParameterList parameterList;

        TypeBindings methodTypeBindings = TypeBindings.empty();
        TypeBindings resolveBindings = bindings;

        if (typeVariables != null && typeVariables.length != 0) {
            final GenericParameterType[] genericParameters = new GenericParameterType[typeVariables.length];

            for (int i = 0; i < typeVariables.length; i++) {
                final TypeVariable<Method> typeVariable = typeVariables[i];
                genericParameters[i] = new GenericParameterType(typeVariable, i);
            }

            methodTypeBindings = TypeBindings.createUnbound(list(genericParameters));
            resolveBindings = methodTypeBindings.withAdditionalBindings(resolveBindings);
        }

        if (rawTypes == null || rawTypes.length == 0) {
            parameterList = ParameterList.empty();
        }
        else {
            final ParameterInfo[] parameters = new ParameterInfo[rawTypes.length];
            for (int i = 0, len = rawTypes.length; i < len; ++i) {
                parameters[i] = new ParameterInfo("p" + i, resolve(rawTypes[i], resolveBindings));
            }
            parameterList = new ParameterList(parameters);
        }

        final Type rt = (rawType == Void.TYPE) ? PrimitiveTypes.Void : resolve(rawType, resolveBindings);

        return new ReflectedMethod(context, m, parameterList, rt, TypeList.empty(), methodTypeBindings);
    }

    @SuppressWarnings("unchecked")
    private <T extends MethodBase> T[] getMethodBaseCandidates(
        final MemberList<T> source,
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
            if (source instanceof ConstructorList) {
                return (T[])EmptyConstructors;
            }
            return (T[])EmptyMethods;
        }

        final T[] results = (T[])Array.newInstance(source.getMemberType(), candidates.size());

        candidates.toArray((Object[])results);

        return results;
    }

    @SuppressWarnings("unchecked")
    private FieldInfo[] getFieldCandidates(
        final String name,
        final int bindingFlags,
        final boolean allowPrefixLookup) {

        final FieldList fields = getResolvedFields();
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

        final String name;

        if (fullName != null) {
            final String ownerName = getName();

            final boolean isLongName = all(bindingFlags, BindingFlags.IgnoreCase)
                                       ? StringUtilities.startsWithIgnoreCase(fullName, ownerName)
                                       : fullName.startsWith(ownerName);
            if (isLongName) {
                if (fullName.length() == ownerName.length()) {
                    return EmptyTypes;
                }
                name = fullName.substring(ownerName.length() + 1);
            }
            else {
                name = fullName;
            }
        }
        else {
            name = null;
        }

        final int flags = bindingFlags & ~BindingFlags.Static;
        final FilterOptions filterOptions = getFilterOptions(name, flags, allowPrefixLookup);

        final TypeList nestedTypes = getResolvedNestedTypes();
        final ListBuffer<Type<?>> candidates = new ListBuffer<>();

        for (int i = 0, n = nestedTypes.size(); i < n; i++) {
            final Type<?> nestedType = nestedTypes.get(i);
            if (filterApplyType(nestedType, flags, name, filterOptions.prefixLookup)) {
                candidates.add(nestedType);
            }
        }

        if (candidates.isEmpty()) {
            return EmptyTypes;
        }

        return candidates.toArray(new Type[candidates.size()]);
    }

    private boolean filterApplyType(
        final Type<?> type,
        final int bindingFlags,
        final String name,
        final boolean prefixLookup) {
        VerifyArgument.notNull(type, "type");

        final boolean isPublic = type.isPublic();
        final boolean isStatic = false;

        return filterApplyCore(
            type,
            bindingFlags,
            isPublic,
            type.isNested() && type.isPackagePrivate(),
            isStatic,
            name,
            prefixLookup
        );
    }

    private boolean filterApplyCore(
        final MemberInfo member,
        final int bindingFlags,
        final boolean isPublic,
        final boolean isPackagePrivate,
        final boolean isStatic,
        final String name,
        final boolean prefixLookup) {
        if (isPublic) {
            if (!any(bindingFlags, BindingFlags.Public)) {
                return false;
            }
        }
        else {
            if (!any(bindingFlags, BindingFlags.NonPublic)) {
                return false;
            }
        }

        final boolean isInherited = member.getDeclaringType() != this;

        if (isInherited && any(bindingFlags, BindingFlags.DeclaredOnly)) {
            return false;
        }

        if (member.getMemberType() != MemberType.TypeInfo &&
            member.getMemberType() != MemberType.NestedType) {
            if (isStatic) {
                if ((bindingFlags & BindingFlags.FlattenHierarchy) == 0 && isInherited) {
                    return false;
                }

                if ((bindingFlags & BindingFlags.Static) == 0) {
                    return false;
                }
            }
            else {
                if ((bindingFlags & BindingFlags.Instance) == 0) {
                    return false;
                }
            }
        }

        if (prefixLookup) {
            if (!filterApplyPrefixLookup(member, name, any(bindingFlags, BindingFlags.IgnoreCase))) {
                return false;
            }
        }

        /*
            Asymmetry:

             Package-private, inherited, instance, non-protected, non-virtual, non-abstract members returned iff
             BindingFlags !DeclaredOnly, Instance and Public are present except for fields
        */

        //noinspection SimplifiableIfStatement
        if (!any(bindingFlags, BindingFlags.DeclaredOnly) &&    // DeclaredOnly not present
            isInherited &&                                      // Is inherited Member

            isPackagePrivate &&                                 // Is package-private member
            any(bindingFlags, BindingFlags.NonPublic) &&        // BindingFlag.NonPublic present

            !isStatic &&                                        // Is instance member
            any(bindingFlags, BindingFlags.Instance))           // BindingFlag.Instance present
        {
            return member instanceof MethodInfo &&
                   !member.isFinal();
        }

        return true;
    }

    private boolean filterApplyPrefixLookup(final MemberInfo method, final String name, final boolean ignoreCase) {
        final String methodName = method.getName();
        if (ignoreCase) {
            if (!StringUtilities.startsWithIgnoreCase(methodName.toLowerCase(), name)) {
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
}
