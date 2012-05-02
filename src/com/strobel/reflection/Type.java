package com.strobel.reflection;

import com.strobel.util.ContractUtils;

import java.lang.reflect.Modifier;
import java.util.HashMap;

import static com.strobel.reflection.Flags.all;
import static com.strobel.reflection.Flags.any;

/**
 * @author Mike Strobel
 */
public abstract class Type extends MemberInfo implements java.lang.reflect.Type {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONSTANTS                                                                                                          //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final Binder DefaultBinder = null;
    public static final char Delimiter = '.';
    public static final Missing Value = new Missing();
    public static final Type[] EmptyTypes = new Type[0];

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

    Type() {}

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTES                                                                                                         //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean isNested() {
        return getDeclaringType() != null;
    }

    public boolean isVisible() {
        throw ContractUtils.unreachable();
    }

    public final boolean isNonPublic() {
        return !Modifier.isPublic(getModifiers());
    }

    public final boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public final boolean isPackagePrivate() {
        return (getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0;
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

    public final boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public boolean isArray() {
        return false;
    }

    public boolean isGenericType() {
        return false;
    }

    public boolean isGenericTypeDefinition() {
        return false;
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

    public Type getBaseType() {
        return Type.of(Object.class);
    }

    public Type getElementType() {
        throw Error.noElementType(this);
    }

    public int getGenericParameterPosition() {
        throw Error.notGenericParameter(this);
    }

    public TypeCollection getGenericArguments() {
        throw ContractUtils.unreachable();
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

        final TypeCollection genericArguments = getGenericArguments();

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < genericArguments.size(); i++) {
            if (genericArguments.get(i).containsGenericParameters()) {
                return true;
            }
        }

        return false;
    }

    public TypeCollection getGenericParameterConstraints() {
        throw Error.notGenericType(this);
    }

    public boolean isEquivalentTo(final Type other) {
        return other == this;
    }

    public boolean isSubclassOf(final Type type) {
        Type current = this;

        if (current == type) {
            return false;
        }

        while (current != null) {
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
    public boolean implementInterface(final Type interfaceType) {
        return false;
    }

    public boolean isAssignableFrom(final Type type) {
        if (type == null) {
            return false;
        }

        if (this == type) {
            return true;
        }

        // If type is a subclass of this class, then type can be cast to this type.
        if (type.isSubclassOf(this)) {
            return true;
        }

        if (this.isInterface()) {
            return type.implementInterface(this);
        }
        else if (isGenericParameter()) {
            final TypeCollection constraints = getGenericParameterConstraints();

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, constraintsSize = constraints.size(); i < constraintsSize; i++) {
                final Type constraint = constraints.get(i);
                if (!constraint.isAssignableFrom(type)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // MEMBER INFO                                                                                                        //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public MemberCollection<? extends MemberInfo> getMember(final String name, final MemberType... memberTypes) {
        return getMember(name, DefaultLookup, memberTypes);
    }

    public abstract MemberCollection<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType... memberTypes);

    public FieldInfo getField(final String name) {
        return getField(name, DefaultLookup);
    }

    public abstract FieldInfo getField(final String name, final int bindingFlags);

    public MethodInfo getMethod(final String name, final Type... parameterTypes) {
        return getMethod(name, DefaultLookup, parameterTypes);
    }

    public MethodInfo getMethod(final String name, final int bindingFlags, final Type... parameterTypes) {
        return getMethod(name, DefaultLookup, parameterTypes);
    }

    public abstract MethodInfo getMethod(final String name, final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes);

    public ConstructorInfo getConstructor(final Type... parameterTypes) {
        return getConstructor(DefaultLookup, parameterTypes);
    }

    public ConstructorInfo getConstructor(final int bindingFlags, final Type... parameterTypes) {
        return getConstructor(bindingFlags, CallingConvention.Any, parameterTypes);
    }

    public abstract ConstructorInfo getConstructor(final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes);

    public MemberCollection<? extends MemberInfo> getMembers() {
        return getMembers(DefaultLookup);
    }

    public abstract MemberCollection<? extends MemberInfo> getMembers(final int bindingFlags);

    public FieldCollection getFields() {
        return getFields(DefaultLookup);
    }

    public abstract FieldCollection getFields(final int bindingFlags);

    public MethodCollection getMethods() {
        return getMethods(DefaultLookup, CallingConvention.Any);
    }

    public MethodCollection getMethods(final int bindingFlags) {
        return getMethods(bindingFlags, CallingConvention.Any);
    }

    public abstract MethodCollection getMethods(final int bindingFlags, final CallingConvention callingConvention);

    public ConstructorCollection getConstructors() {
        return getConstructors(DefaultLookup);
    }

    public abstract ConstructorCollection getConstructors(final int bindingFlags);

    public TypeCollection getNestedTypes() {
        return getNestedTypes(DefaultLookup);
    }

    public abstract TypeCollection getNestedTypes(final int bindingFlags);

    public Object[] getEnumConstants() {
        throw Error.notEnumType(this);
    }

    public String[] getEnumNames() {
        throw Error.notEnumType(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TRANSFORMATION METHODS                                                                                             //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Type makeArrayType() {
        throw Error.notArrayType(this);
    }

    @SuppressWarnings("UnusedParameters")
    public Type makeGenericType(final Type... typeArguments) {
        throw Error.notGenericType(this);
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
            final ParameterCollection parameters = method.getParameters();

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
    // REFLECTED TYPE CACHE                                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final static HashMap<Class<?>, ReflectedType<?>> REFLECTED_TYPE_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized static <T> Type of(final Class<T> clazz) {
        final ReflectedType<T> reflectedType = (ReflectedType<T>)REFLECTED_TYPE_CACHE.get(clazz);

        if (reflectedType != null) {
            return reflectedType;
        }

        final ReflectedType<T> newReflectedType = new ReflectedType<>(clazz);

        REFLECTED_TYPE_CACHE.put(clazz, newReflectedType);

        return newReflectedType;
    }
}
