package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import javax.lang.model.element.Element;
import java.util.ArrayList;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("unchecked")
class ClassType<T> extends Type<T> {
    private final String _name;
    private final Context _context;
    private final Symbol.ClassSymbol _typeElement;
    private Type<?> _baseType;
    private TypeList _interfaces;
    private boolean _membersResolved;
    private boolean _completed;
    private Type<?> _declaringType;
    private Class<T> _erasedClass;
    private List<TypeParameter> _genericParameters = List.nil();
    private List<ClassType<?>> _nestedTypes = List.nil();
    private List<ClassMethod> _methods = List.nil();
    private List<ClassField> _fields = List.nil();
    private List<ClassConstructor> _constructors = List.nil();
    private TypeBindings _typeBindings;

    ClassType(final Context context, final Symbol.ClassSymbol typeElement) {
        _context = VerifyArgument.notNull(context, "context");
        _typeElement = VerifyArgument.notNull(typeElement, "typeElement");
        _name = typeElement.getQualifiedName().toString();
    }

    void setBaseType(final Type<?> baseType) {
        _baseType = baseType;
    }

    void setInterfaces(final TypeList interfaces) {
        _interfaces = VerifyArgument.notNull(interfaces, "interfaces");
    }

    Symbol.ClassSymbol getTypeElement() {
        return _typeElement;
    }

    List<TypeParameter> getGenericParameters() {
        return _genericParameters;
    }

    ClassMethod findMethod(final Symbol.MethodSymbol methodSymbol) {
        for (final ClassMethod method : _methods) {
            if (method.getElement() == methodSymbol) {
                return method;
            }
        }
        return null;
    }

    void setDeclaringType(final Type<?> declaringType) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
    }

    TypeParameter findGenericParameter(final Element element) {
        for (final TypeParameter genericParameter : _genericParameters) {
            if (genericParameter.getElement() == element) {
                return genericParameter;
            }
        }
        return null;
    }

    void addGenericParameter(final TypeParameter typeParameter) {
        VerifyArgument.notNull(typeParameter, "typeParameter");
        _genericParameters = _genericParameters.append(typeParameter);
    }

    void addNestedType(final ClassType<?> nestedType) {
        VerifyArgument.notNull(nestedType, "nestedType");
        _nestedTypes = _nestedTypes.append(nestedType);
        _membersResolved = false;
    }

    void addMethod(final ClassMethod method) {
        VerifyArgument.notNull(method, "method");
        _methods = _methods.append(method);
        _membersResolved = false;
    }

    void addConstructor(final ClassConstructor constructor) {
        VerifyArgument.notNull(constructor, "constructor");
        _constructors = _constructors.append(constructor);
        _membersResolved = false;
    }

    void addField(final ClassField field) {
        VerifyArgument.notNull(field, "field");
        _fields = _fields.append(field);
        _membersResolved = false;
    }

    private void completeIfNecessary() {
        if (_erasedClass == null) {
            synchronized (CACHE_LOCK) {
                if (_erasedClass == null) {
                    complete();
                }
            }
        }
    }

    private void ensureMembersResolved() {
        if (!_membersResolved) {
            synchronized (CACHE_LOCK) {
                if (!_membersResolved) {
                    new Resolver(_context).resolveMembers(this);

                    _membersResolved = true;

                    for (final ClassField field : _fields) {
                        field.complete();
                    }

                    for (final ClassConstructor constructor : _constructors) {
                        constructor.complete();
                    }

                    final ArrayList<ClassMethod> methods = new ArrayList<>(_methods);

                    for (int i = 0, n = methods.size(); i < n; i++) {
                        final ClassMethod method = methods.get(i);
                        try {
                            method.complete();
                        }
                        catch (MemberResolutionException e) {
                            --n;
                            methods.remove(i--);
                        }
                    }

                    for (final ClassType<?> nestedType : _nestedTypes) {
                        nestedType.complete();
                    }
                }
            }
        }
    }

    @Override
    ConstructorList getResolvedConstructors() {
        ensureMembersResolved();
        return new ConstructorList(_constructors);
    }

    @Override
    MethodList getResolvedInstanceMethods() {
        ensureMembersResolved();

        final ArrayList<MethodInfo> allMethods = new ArrayList<>(_methods.size());

        for (int i = 0, n = _methods.size(); i < n; i++) {
            final ClassMethod method = _methods.get(i);
            if (!method.isStatic()) {
                allMethods.add(method);
            }
        }

        Type<?> currentType = getBaseType();

        while (currentType != null) {
            List<MethodInfo> currentMethods = List.nil();

            for (final MethodInfo method : currentType.getResolvedInstanceMethods()) {
                if (method.getDeclaringType() != currentType || hasOverride(method, allMethods)) {
                    continue;
                }
                currentMethods = currentMethods.append(new InheritedMethod(method, this));
            }

            allMethods.addAll(currentMethods);
            currentType = currentType.getBaseType();
        }

        return new MethodList(allMethods);
    }

    private boolean hasOverride(final MethodInfo method, final ArrayList<MethodInfo> allMethods) {
        for (final MethodInfo m : allMethods) {
            if (Helper.overrides(m, method)) {
                return true;
            }
        }
        return false;
    }

    @Override
    MethodList getResolvedStaticMethods() {
        ensureMembersResolved();

        final ArrayList<MethodInfo> methods = new ArrayList<>(_methods.size());

        for (int i = 0, n = _methods.size(); i < n; i++) {
            final MethodInfo method = _methods.get(i);
            if (method.isStatic()) {
                methods.add(method);
            }
        }

        Type<?> currentType = getBaseType();

        while (currentType != null) {
            for (final MethodInfo method : currentType.getResolvedStaticMethods()) {
                if (method.getDeclaringType() == currentType) {
                    methods.add(new InheritedMethod(method, this));
                }
            }
            currentType = currentType.getBaseType();
        }

        return new MethodList(methods);
    }

    @Override
    FieldList getResolvedFields() {
        ensureMembersResolved();
        return new FieldList(_fields);
    }

    @Override
    TypeList getResolvedNestedTypes() {
        ensureMembersResolved();
        return new TypeList(_nestedTypes);
    }

    private Class<T> resolveErasedClass() {
        try {
            return (Class<T>)Class.forName(_typeElement.flatName().toString());
        }
        catch (ClassNotFoundException ignored) {
            throw Error.couldNotResolveType(_typeElement.flatName());
        }
    }

    void complete() {
        if (_completed) {
            return;
        }

        _completed = true;

        if (_baseType != null) {
            _baseType = Resolver.GenericPlaceholderResolver.visit(_baseType);
        }

        if (_interfaces == null) {
            _interfaces = TypeList.empty();
        }
        else {
            _interfaces = Resolver.GenericPlaceholderResolver.visit(_interfaces);
        }

        if (_erasedClass == null) {
            _erasedClass = resolveErasedClass();
        }

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
    public TypeList getInterfaces() {
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
        ensureMembersResolved();

        return new TypeBinder().visit(
            this,
            TypeBindings.create(getGenericTypeParameters(), typeArguments)
        );
    }

    @Override
    public Class<T> getErasedClass() {
        completeIfNecessary();
        return _erasedClass;
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
    int getModifiers() {
        return (int)(_typeElement.flags() & Flags.StandardFlags);
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitClassType(this, parameter);
    }

    @Override
    protected StringBuilder _appendClassName(final StringBuilder sb, final boolean dottedName) {
        if (dottedName) {
            return sb.append(_name);
        }
        return super._appendClassName(sb, dottedName);
    }
}
