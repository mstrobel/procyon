package com.strobel.reflection;

import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import javax.lang.model.element.TypeParameterElement;
import java.util.ArrayList;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("unchecked")
class JavacType<T> extends Type<T> {
    private final String _name;
    private final String _simpleName;
    private final Context _context;
    private final Symbol.ClassSymbol _typeElement;
    private Type<?> _baseType;
    private TypeList _interfaces;
    private boolean _membersResolved;
    private boolean _completed;
    private Type<?> _declaringType;
    private Class<T> _erasedClass;
    private List<JavacGenericParameter> _genericParameters = List.nil();
    private List<JavacType<?>> _nestedTypes = List.nil();
    private List<JavacMethod> _methods = List.nil();
    private List<ClassField> _fields = List.nil();
    private List<ClassConstructor> _constructors = List.nil();
    private TypeBindings _typeBindings;

    JavacType(final Context context, final Symbol.ClassSymbol typeElement) {
        _context = VerifyArgument.notNull(context, "context");
        _typeElement = VerifyArgument.notNull(typeElement, "typeElement");
        _name = typeElement.getQualifiedName().toString();
        _simpleName = typeElement.getSimpleName().toString();
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

    List<JavacGenericParameter> getGenericParameters() {
        return _genericParameters;
    }

    JavacMethod findMethod(final Symbol.MethodSymbol methodSymbol) {
        for (final JavacMethod method : _methods) {
            if (Comparer.equals(method.getElement(), methodSymbol)) {
                return method;
            }
        }
        return null;
    }

    JavacGenericParameter findGenericParameter(final TypeParameterElement symbol) {
        for (final JavacGenericParameter genericParameter : _genericParameters) {
            if (Comparer.equals(genericParameter.getElement(), symbol)) {
                return genericParameter;
            }
        }
        return null;
    }

    void setDeclaringType(final Type<?> declaringType) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
    }

    void addGenericParameter(final JavacGenericParameter genericParameter) {
        VerifyArgument.notNull(genericParameter, "typeParameter");
        _genericParameters = _genericParameters.append(genericParameter);
    }

    void addNestedType(final JavacType<?> nestedType) {
        VerifyArgument.notNull(nestedType, "nestedType");
        _nestedTypes = _nestedTypes.append(nestedType);
        _membersResolved = false;
    }

    void addMethod(final JavacMethod method) {
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

                    final ArrayList<JavacMethod> methods = new ArrayList<>(_methods);

                    for (int i = 0, n = methods.size(); i < n; i++) {
                        final JavacMethod method = methods.get(i);
                        try {
                            method.complete();
                        }
                        catch (MemberResolutionException e) {
                            --n;
                            methods.remove(i--);
                        }
                    }

                    for (final JavacType<?> nestedType : _nestedTypes) {
                        nestedType.complete();
                    }
                }
            }
        }
    }

    @Override
    public ConstructorList getDeclaredConstructors() {
        ensureMembersResolved();
        return new ConstructorList(_constructors);
    }
    
    @Override
    public MethodList getDeclaredMethods() {
        ensureMembersResolved();
        return new MethodList(_methods);
    }

    @Override
    public FieldList getDeclaredFields() {
        ensureMembersResolved();
        return new FieldList(_fields);
    }

    @Override
    public TypeList getDeclaredTypes() {
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
    protected StringBuilder _appendClassName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        if (!fullName) {
            return sb.append(_simpleName);
        }
        if (dottedName) {
            return sb.append(_name);
        }
        return super._appendClassName(sb, fullName, dottedName);
    }
}
