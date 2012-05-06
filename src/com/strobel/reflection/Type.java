package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.strobel.util.TypeUtils;

import java.lang.reflect.Modifier;

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

    protected static final Object[] EmptyObjects = new Object[0];
    protected static final String[] EmptyStrings = new String[0];
    protected static final MethodInfo[] EmptyMethods = new MethodInfo[0];
    protected static final ConstructorInfo[] EmptyConstructors = new ConstructorInfo[0];
    protected static final FieldInfo[] EmptyFields = new FieldInfo[0];
    protected static final MemberInfo[] EmptyMembers = new MemberInfo[0];

    protected static final int DefaultLookup = BindingFlags.Instance | BindingFlags.Static | BindingFlags.Public;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // STANDARD SYSTEM TYPES                                                                                              //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final Type Object = new ReflectedType(java.lang.Object.class, TypeBindings.empty(), null, TypeList.empty());

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS                                                                                                       //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    protected Type() {
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

    public boolean isArray() {
        return false;
    }

    public boolean isGenericType() {
        return false;
    }

    public boolean isGenericTypeDefinition() {
        if (!isGenericType()) {
            return false;
        }

        final TypeBindings typeArguments = getTypeBindings();

        return !typeArguments.isEmpty() &&
               !typeArguments.hasConcreteParameters();
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

        return typeArguments.hasUnboundParameters();
    }

    public TypeList getGenericParameterConstraints() {
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
    public boolean implementsInterface(final Type interfaceType) {
        Type t = this;

        while (t != null) {
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
        if (type == null) {
            return false;
        }

        if (TypeUtils.hasIdentityPrimitiveOrBoxingConversion(this, type)) {
            return true;
        }

        if (type.isSubclassOf(this)) {
            return true;
        }

        if (this.isInterface()) {
            return type.implementsInterface(this);
        }

        else if (isGenericParameter()) {
            final TypeList constraints = getGenericParameterConstraints();

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

    public final MemberList<? extends MemberInfo> getMember(final String name, final MemberType... memberTypes) {
        return getMember(name, DefaultLookup, memberTypes);
    }

    public abstract MemberList<? extends MemberInfo> getMember(final String name, final int bindingFlags, final MemberType[] memberTypes);

    public FieldInfo getField(final String name) {
        return getField(name, DefaultLookup);
    }

    public abstract FieldInfo getField(final String name, final int bindingFlags);

    public MethodInfo getMethod(final String name, final Type... parameterTypes) {
        return getMethod(name, DefaultLookup, parameterTypes);
    }

    public MethodInfo getMethod(final String name, final int bindingFlags, final Type... parameterTypes) {
        return getMethod(name, bindingFlags, CallingConvention.Any, parameterTypes);
    }

    public abstract MethodInfo getMethod(final String name, final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes);

    public ConstructorInfo getConstructor(final Type... parameterTypes) {
        return getConstructor(DefaultLookup, parameterTypes);
    }

    public ConstructorInfo getConstructor(final int bindingFlags, final Type... parameterTypes) {
        return getConstructor(bindingFlags, CallingConvention.Any, parameterTypes);
    }

    public abstract ConstructorInfo getConstructor(final int bindingFlags, final CallingConvention callingConvention, final Type... parameterTypes);

    public MemberList<? extends MemberInfo> getMembers() {
        return getMembers(DefaultLookup);
    }

    public abstract MemberList<? extends MemberInfo> getMembers(final int bindingFlags);

    public FieldList getFields() {
        return getFields(DefaultLookup);
    }

    public abstract FieldList getFields(final int bindingFlags);

    public MethodList getMethods() {
        return getMethods(DefaultLookup, CallingConvention.Any);
    }

    public MethodList getMethods(final int bindingFlags) {
        return getMethods(bindingFlags, CallingConvention.Any);
    }

    public abstract MethodList getMethods(final int bindingFlags, final CallingConvention callingConvention);

    public ConstructorList getConstructors() {
        return getConstructors(DefaultLookup);
    }

    public abstract ConstructorList getConstructors(final int bindingFlags);

    public TypeList getNestedTypes() {
        return getNestedTypes(DefaultLookup);
    }

    public abstract TypeList getNestedTypes(final int bindingFlags);

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
    public String getName() {
        return _appendClassName(new StringBuilder(), true).toString();
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
        return _appendClassDescription(sb);
    }

    public StringBuilder appendFullDescription(final StringBuilder sb) {
        StringBuilder s = _appendClassDescription(sb);

        final Type baseType = getBaseType();

        if (baseType != null && baseType != Object) {
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

    final static TypeCache CACHE;
    final static TypeResolver TYPE_RESOLVER;

    static {
        CACHE = new TypeCache();
        TYPE_RESOLVER = new TypeResolver(CACHE);
    }

    @SuppressWarnings("unchecked")
    public synchronized static <T extends Class<?>> Type<T> of(final T clazz) {
        final ReflectedType<T> reflectedType = (ReflectedType<T>)CACHE.find(clazz);

        if (reflectedType != null) {
            return reflectedType;
        }

        return TYPE_RESOLVER.resolve(clazz);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIST FACTORY METHODS                                                                                               //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static TypeList list(final Type... types) {
        return new TypeList(types);
    }
}
