package com.strobel.reflection.emit;

import com.strobel.core.ArrayUtilities;
import com.strobel.core.ReadOnlyList;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.reflection.*;
import com.strobel.util.TypeUtils;
import sun.misc.Unsafe;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author strobelm
 */
@SuppressWarnings({"unchecked", "PackageVisibleField"})
public final class TypeBuilder<T> extends Type<T> {
    private final static Pattern PACKAGE_DELIMITER = Pattern.compile("\\.");

    final ConstantPool constantPool;
    final ArrayList<ConstructorBuilder> constructorBuilders;
    final ArrayList<MethodBuilder> methodBuilders;
    final ArrayList<FieldBuilder> fieldBuilders;
    final ArrayList<GenericParameterBuilder<?>> genericParameterBuilders;

    private String _name;
    private String _fullName;
    private Package _package;
    private Type<?> _baseType;
    private ConstructorList _constructors;
    private MethodList _methods;
    private FieldList _fields;
    private TypeList _interfaces;
    private TypeBuilder _declaringType;
    private MethodBuilder _declaringMethod;
    private int _modifiers;
    private boolean _hasBeenCreated;
    private Class<T> _generatedClass;
    private Type<T> _generatedType;
    private Type<?> _extendsBound;

    private int _genericParameterPosition;
    private boolean _isGenericParameter;
    private boolean _isGenericTypeDefinition;
    private TypeBuilder _genericTypeDefinition;
    private TypeBindings _typeBindings;
    private ReadOnlyList<AnnotationBuilder> _annotations;
    private final ProtectionDomain _protectionDomain;

    // <editor-fold defaultstate="collapsed" desc="Constructors and Initializers">

    public TypeBuilder(
        final String name,
        final int modifiers,
        final Type<?> baseType,
        final TypeList interfaces) {

        this();

        initialize(
            name,
            modifiers,
            baseType,
            interfaces,
            null
        );
    }

    TypeBuilder() {
        this.constantPool = new ConstantPool();
        this.constructorBuilders = new ArrayList<>();
        this.methodBuilders = new ArrayList<>();
        this.fieldBuilders = new ArrayList<>();
        this.genericParameterBuilders = new ArrayList<>();

        _constructors = ConstructorList.empty();
        _methods = MethodList.empty();
        _fields = FieldList.empty();
        _typeBindings = TypeBindings.empty();
        _annotations = ReadOnlyList.emptyList();

        _protectionDomain = CallerResolver.getCallerClass(2).getProtectionDomain();
    }

    TypeBuilder(final String name, final int genericParameterPosition, final TypeBuilder declaringType) {
        this();

        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");

        initializeAsGenericParameter(
            VerifyArgument.notNull(name, "name"),
            VerifyArgument.isNonNegative(genericParameterPosition, "genericParameterPosition")
        );
    }

    TypeBuilder(final String name, final int genericParameterPosition, final MethodBuilder declaringMethod) {
        this();

        _declaringMethod = VerifyArgument.notNull(declaringMethod, "declaringMethod");
        _declaringType = _declaringMethod.getDeclaringType();

        initializeAsGenericParameter(
            VerifyArgument.notNull(name, "name"),
            VerifyArgument.isNonNegative(genericParameterPosition, "genericParameterPosition")
        );
    }

    TypeBuilder(final String name, final int modifiers, final Type<?> baseType, final TypeBuilder declaringType) {
        this();

        initialize(
            name,
            modifiers,
            baseType,
            TypeList.empty(),
            declaringType
        );
    }

    TypeBuilder(
        final String name,
        final int modifiers,
        final Type<?> baseType,
        final TypeList interfaces,
        final TypeBuilder declaringType) {

        this();

        initialize(
            name,
            modifiers,
            baseType,
            interfaces,
            declaringType
        );
    }

    private void initializeAsGenericParameter(final String name, final int position) {
        _name = name;
        _fullName = name;
        _genericParameterPosition = position;
        _isGenericParameter = true;
        _isGenericTypeDefinition = false;
        _interfaces = TypeList.empty();
    }

    private void initialize(
        final String fullName,
        final int modifiers,
        final Type<?> baseType,
        final TypeList interfaces,
        final TypeBuilder declaringType) {

        VerifyArgument.notNullOrWhitespace(fullName, "fullName");

        if (fullName.length() > 1023) {
            throw Error.typeNameTooLong();
        }

        _fullName = fullName;
        _isGenericTypeDefinition = false;
        _isGenericParameter = false;
        _hasBeenCreated = false;
        _declaringType = declaringType;

        final int lastDotIndex = fullName.lastIndexOf('.');

        if (lastDotIndex == -1 || lastDotIndex == 0) {
            _package = Package.getPackage(StringUtilities.EMPTY);
            _name = _fullName;
        }
        else {
            _package = Package.getPackage(fullName.substring(0, lastDotIndex));
            _name = fullName.substring(lastDotIndex + 1);
        }

        _modifiers = modifiers & Modifier.classModifiers();

        setBaseType(baseType);
        setInterfaces(interfaces);
    }

    final void setInterfaces(final TypeList interfaces) {
        verifyNotCreated();
        _interfaces = interfaces != null ? interfaces : TypeList.empty();
        updateExtendsBound();
    }

    final void setBaseType(final Type<?> baseType) {
        verifyNotGeneric();
        verifyNotCreated();

        if (baseType != null) {
            if (baseType.isInterface()) {
                throw Error.baseTypeCannotBeInterface();
            }
            _baseType = baseType;
            updateExtendsBound();
            return;
        }

        if (Modifier.isInterface(_modifiers)) {
            _baseType = null;
        }
        else {
            _baseType = Types.Object;
        }

        updateExtendsBound();
    }

    private void updateExtendsBound() {
        if (!isGenericParameter()) {
            return;
        }

        _extendsBound = _baseType == Types.Object ? null : _baseType;

        if (_interfaces.isEmpty()) {
            return;
        }

        if (_interfaces.size() == 1 && _extendsBound == null) {
            _extendsBound = _interfaces.get(0);
            return;
        }

        _extendsBound = Type.makeCompoundType(_baseType, _interfaces);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Assertions">

    final void verifyNotCreated() {
        if (isCreated()) {
            throw Error.typeHasBeenCreated();
        }
    }

    final void verifyCreated() {
        if (!isCreated()) {
            throw Error.typeHasNotBeenCreated();
        }
    }

    final void verifyNotGeneric() {
        if (isGenericType() && !isGenericTypeDefinition()) {
            throw new IllegalStateException();
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type Information Overrides">

    @Override
    public Package getPackage() {
        return _package;
    }

    @Override
    public Type getReflectedType() {
        return _declaringType;
    }

    @Override
    public MethodInfo getDeclaringMethod() {
        return _declaringMethod;
    }

    @Override
    protected String getClassFullName() {
        return _fullName;
    }

    @Override
    protected String getClassSimpleName() {
        return _name;
    }

    @Override
    public Type getBaseType() {
        return _baseType;
    }

    @Override
    public TypeList getInterfaces() {
        return _interfaces;
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public int getModifiers() {
        return _modifiers;
    }

    @Override
    public boolean isEquivalentTo(final Type<?> other) {
        if (other == this) {
            return true;
        }

        if (other == null) {
            return false;
        }

        final Type<?> runtimeType = other instanceof TypeBuilder<?>
                                    ? ((TypeBuilder)other)._generatedType
                                    : other;

        return _generatedType != null &&
               runtimeType != null &&
               _generatedType.isEquivalentTo(runtimeType);
    }

    @Override
    public boolean isGenericParameter() {
        return _isGenericParameter;
    }

    @Override
    public boolean isGenericType() {
        return _genericTypeDefinition != null;
    }

    @Override
    public boolean isGenericTypeDefinition() {
        return _isGenericTypeDefinition;
    }

    @Override
    public int getGenericParameterPosition() {
        if (isGenericParameter()) {
            return _genericParameterPosition;
        }
        return super.getGenericParameterPosition();
    }

    @Override
    public Type getGenericTypeDefinition() {
        if (isGenericType()) {
            return _genericTypeDefinition;
        }
        return super.getGenericTypeDefinition();
    }

    @Override
    protected TypeBindings getTypeBindings() {
        return _typeBindings;
    }

    @Override
    public Type<?> getExtendsBound() {
        if (_extendsBound != null) {
            return _extendsBound;
        }
        return super.getExtendsBound();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Member Information Overrides">

    @Override
    public ConstructorInfo getConstructor(
        final Set<BindingFlags> bindingFlags,
        final CallingConvention callingConvention,
        final Type... parameterTypes) {

        verifyCreated();
        return _generatedType.getConstructor(bindingFlags, callingConvention, parameterTypes);
    }

    @Override
    public ConstructorList getConstructors(final Set<BindingFlags> bindingFlags) {
        verifyNotCreated();
        return _generatedType.getConstructors(bindingFlags);
    }

    @Override
    public MemberList getMembers(final Set<BindingFlags> bindingFlags, final Set<MemberType> memberTypes) {
        verifyNotCreated();
        return _generatedType.getMembers(bindingFlags, memberTypes);
    }

    @Override
    public MemberList getMember(final String name, final Set<BindingFlags> bindingFlags, final Set<MemberType> memberTypes) {
        verifyNotCreated();
        return _generatedType.getMember(name, bindingFlags, memberTypes);
    }

    @Override
    public MethodInfo getMethod(
        final String name,
        final Set<BindingFlags> bindingFlags,
        final CallingConvention callingConvention,
        final Type... parameterTypes) {

        verifyNotCreated();
        return _generatedType.getMethod(name, bindingFlags, callingConvention, parameterTypes);
    }

    @Override
    public MethodList getMethods(final Set<BindingFlags> bindingFlags, final CallingConvention callingConvention) {
        verifyNotCreated();
        return _generatedType.getMethods(bindingFlags, callingConvention);
    }

    @Override
    public Type<?> getNestedType(final String fullName, final Set<BindingFlags> bindingFlags) {
        verifyNotCreated();
        return _generatedType.getNestedType(fullName, bindingFlags);
    }

    @Override
    public TypeList getNestedTypes(final Set<BindingFlags> bindingFlags) {
        verifyCreated();
        return _generatedType.getNestedTypes(bindingFlags);
    }

    @Override
    public FieldList getFields(final Set<BindingFlags> bindingFlags) {
        verifyNotCreated();
        return _generatedType.getFields(bindingFlags);
    }

    @Override
    public FieldInfo getField(final String name, final Set<BindingFlags> bindingFlags) {
        verifyNotCreated();
        return _generatedType.getField(name, bindingFlags);
    }

    @Override
    public Class<T> getErasedClass() {
        verifyCreated();
        return _generatedClass;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Annotations">

    public void addCustomAnnotation(final AnnotationBuilder annotation) {
        VerifyArgument.notNull(annotation, "annotation");
        final AnnotationBuilder[] newAnnotations = new AnnotationBuilder[this._annotations.size() + 1];
        _annotations.toArray(newAnnotations);
        newAnnotations[this._annotations.size()] = annotation;
        _annotations = new ReadOnlyList<>(newAnnotations);
    }

    public ReadOnlyList<AnnotationBuilder> getCustomAnnotations() {
        return _annotations;
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        verifyCreated();
        return _generatedType.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        verifyCreated();
        return _generatedType.getAnnotations();
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        verifyCreated();
        return _generatedType.getDeclaredAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        verifyCreated();
        return _generatedType.isAnnotationPresent(annotationClass);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type Manipulation">

    public boolean isCreated() {
        return _hasBeenCreated;
    }

    public synchronized Type<T> createType() {
        return createTypeNoLock();
    }

    public ConstructorBuilder defineConstructor(
        final int modifiers,
        final TypeList parameterTypes) {

        verifyNotGeneric();
        verifyNotCreated();

        final ConstructorBuilder constructor = new ConstructorBuilder(
            modifiers & Modifier.constructorModifiers(),
            parameterTypes,
            this
        );

        constructorBuilders.add(constructor);
        _constructors = new ConstructorList(ArrayUtilities.append(_constructors.toArray(), constructor));

        return constructor;
    }

    final void addMethodToList(final MethodBuilder methodBuilder) {
        methodBuilders.add(methodBuilder);
    }

    public MethodBuilder defineMethod(
        final String name,
        final int modifiers,
        final Type<?> returnType,
        final TypeList parameterTypes) {

        VerifyArgument.notNullOrWhitespace(name, "name");

        verifyNotGeneric();
        verifyNotCreated();

        final MethodBuilder method = new MethodBuilder(
            name,
            modifiers & Modifier.methodModifiers(),
            returnType,
            parameterTypes,
            this
        );

        methodBuilders.add(method);
        _methods = new MethodList(ArrayUtilities.append(_methods.toArray(), method));

        return method;
    }

    public FieldBuilder defineConstant(
        final String name,
        final Type<?> type,
        final int modifiers,
        final Object constantValue) {

        VerifyArgument.notNullOrWhitespace(name, "name");
        VerifyArgument.notNull(constantValue, "constantValue");

        verifyNotGeneric();
        verifyNotCreated();

        return defineFieldCore(name, type, modifiers, constantValue);
    }

    public FieldBuilder defineField(
        final String name,
        final Type<?> type,
        final int modifiers) {

        return defineFieldCore(name, type, modifiers, null);
    }

    private FieldBuilder defineFieldCore(
        final String name,
        final Type<?> type,
        final int modifiers,
        final Object constantValue) {

        VerifyArgument.notNullOrWhitespace(name, "name");

        verifyNotGeneric();
        verifyNotCreated();

        if (constantValue != null &&
            !TypeUtils.isAutoUnboxed(Type.of(constantValue.getClass()))) {

            throw Error.valueMustBeConstant();
        }

        final FieldBuilder field = new FieldBuilder(
            this,
            name,
            type,
            modifiers & Modifier.fieldModifiers(),
            constantValue
        );

        fieldBuilders.add(field);
        _fields = new FieldList(ArrayUtilities.append(_fields.toArray(), field));

        return field;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Constant Pool">

    short getTypeToken(final Type<?> type) {
        VerifyArgument.notNull(type, "type");
        return (short)(constantPool.getTypeInfo(type).index & 0xFFFF);
    }

    short getMethodToken(final MethodBase method) {
        VerifyArgument.notNull(method, "method");
        return (short)(constantPool.getMethodReference(method).index & 0xFFFF);
    }

    short getFieldToken(final FieldInfo field) {
        VerifyArgument.notNull(field, "field");
        return (short)(constantPool.getFieldReference(field).index & 0xFFFF);
    }

    short getConstantToken(final int value) {
        return (short)(constantPool.getIntegerConstant(value).index & 0xFFFF);
    }

    short getConstantToken(final long value) {
        return (short)(constantPool.getLongConstant(value).index & 0xFFFF);
    }

    short getConstantToken(final float value) {
        return (short)(constantPool.getFloatConstant(value).index & 0xFFFF);
    }

    short getConstantToken(final double value) {
        return (short)(constantPool.getDoubleConstant(value).index & 0xFFFF);
    }

    short getStringToken(final String value) {
        return (short)(constantPool.getStringConstant(value).index & 0xFFFF);
    }

    short getUtf8StringToken(final String value) {
        return (short)(constantPool.getUtf8StringConstant(value).index & 0xFFFF);
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type Generation Methods">

    private Type<T> createTypeNoLock() {
        if (isCreated()) {
            return _generatedType;
        }

        verifyNotGeneric();
        verifyNotCreated();

        if (isGenericParameter()) {
            _hasBeenCreated = true;
            return this;
        }
        else if (!genericParameterBuilders.isEmpty()) {
            for (int i = 0, n = genericParameterBuilders.size(); i < n; i++) {
                genericParameterBuilders.get(i).typeBuilder.createType();
            }
        }

        byte[] body;

        final int methodCount = methodBuilders.size();

        for (int i = 0; i < methodCount; i++) {
            final MethodBuilder method = methodBuilders.get(i);

            if (method.isAbstract() && !this.isAbstract()) {
                throw Error.abstractMethodDeclaredOnNonAbstractType();
            }

            body = method.getBody();

            if (method.isAbstract()) {
                if (body != null) {
                    throw Error.abstractMethodCannotHaveBody();
                }
            }
            else {
                if (method.generator != null) {
                    method.createMethodBodyHelper(method.getCodeGenerator());
                    body = method.getBody();
                }

                if (body == null || body.length == 0) {
                    throw Error.methodHasEmptyBody(method);
                }
            }
        }

        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024)) {
            new ClassWriter(this).writeClass(outputStream);

            final String fullName = getClassFullName();
            final byte[] classBytes = outputStream.toByteArray();

            _hasBeenCreated = true;

            _generatedClass = (Class<T>)getUnsafeInstance().defineClass(
                fullName,
                classBytes,
                0,
                classBytes.length,
                ClassLoader.getSystemClassLoader(),
                _protectionDomain
            );

            _generatedType = Type.of(_generatedClass);
        }
        catch (Throwable e) {
            throw Error.classGenerationFailed(this, e);
        }
        finally {
            if (_generatedType != null) {
                updateMembersWithGeneratedReferences();
            }

            for (int i = 0; i < methodCount; i++) {
                methodBuilders.get(i).releaseBakedStructures();
            }
        }

        return _generatedType;
    }

    private void updateMembersWithGeneratedReferences() {
        final FieldList generatedFields = _generatedType.getFields(BindingFlags.AllDeclared);
        final MethodList generatedMethods = _generatedType.getMethods(BindingFlags.AllDeclared);
        final ConstructorList generatedConstructors = _generatedType.getConstructors(BindingFlags.AllDeclared);

        for (int i = 0, n = fieldBuilders.size(); i < n; i++) {
            fieldBuilders.get(i).generatedField = generatedFields.get(i);
        }

        for (int i = 0, n = methodBuilders.size(); i < n; i++) {
            final MethodBuilder method = methodBuilders.get(i);
            method.generatedMethod = generatedMethods.get(i);
        }

        for (int i = 0, n = constructorBuilders.size(); i < n; i++) {
            constructorBuilders.get(i).generatedConstructor = generatedConstructors.get(i);
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Unsafe Access">

    private static Unsafe _unsafe;

    private static Unsafe getUnsafeInstance() {

        if (_unsafe != null) {
            return _unsafe;
        }

        try {
            _unsafe = Unsafe.getUnsafe();
        }
        catch (Throwable ignored) {
        }

        try {
            final Field instanceField = Unsafe.class.getDeclaredField("theUnsafe");
            instanceField.setAccessible(true);
            _unsafe = (Unsafe)instanceField.get(Unsafe.class);
        }
        catch (Throwable t) {
            throw Error.couldNotLoadUnsafeClassInstance();
        }

        return _unsafe;
    }

    // </editor-fold>
}
