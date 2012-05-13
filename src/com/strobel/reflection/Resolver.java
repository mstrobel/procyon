package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.strobel.util.EmptyArrayCache;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.AbstractElementVisitor8;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("unchecked")
public final class Resolver extends AbstractElementVisitor8<Resolver.Frame, Resolver.Frame> {
    private final Context _context;

    public Resolver(final Context context) {
        _context = VerifyArgument.notNull(context, "context");
    }

    void resolveMembers(final ClassType<?> type) {
        final Frame frame = new Frame(type, null);
        final Symbol.ClassSymbol e = type.getTypeElement();

        for (final Element ee : e.getEnclosedElements()) {
            if (!(ee instanceof Symbol.ClassSymbol)) {
                this.visit(ee, frame);
            }
        }
    }

    public final class Frame {

        private final ClassType<?> _type;
        private final TypeElement _typeElement;
        private final Frame _previous;
        private final Map<Element, Type> _elementTypeMap;
        private final Stack<BaseMethod> _methods;

        private List<Type<?>> _typeArguments = com.sun.tools.javac.util.List.nil();

        public Frame(final TypeElement typeElement, final Frame previous) {
            _typeElement = VerifyArgument.notNull(typeElement, "typeElement");
            _previous = previous;
            _elementTypeMap = previous != null ? previous._elementTypeMap : new HashMap<Element, Type>();
            _methods = previous != null ? previous._methods : new Stack<BaseMethod>();
            _type = new ClassType<>(_context, (Symbol.ClassSymbol)typeElement);
            _elementTypeMap.put(typeElement, _type);

            final Frame ownerFrame = findFrame(typeElement.getEnclosingElement());

            if (ownerFrame != null) {
                ownerFrame._type.addNestedType(_type);
            }
        }

        Frame(final ClassType<?> type, final Frame previous) {
            _type = VerifyArgument.notNull(type, "type");
            _typeElement = type.getTypeElement();
            _previous = previous;
            _elementTypeMap = previous != null ? previous._elementTypeMap : new HashMap<Element, Type>();
            _methods = previous != null ? previous._methods : new Stack<BaseMethod>();
            _elementTypeMap.put(_typeElement, _type);

            final Frame ownerFrame = findFrame(_typeElement.getEnclosingElement());

            if (ownerFrame != null) {
                ownerFrame._type.addNestedType(_type);
            }
        }

        public Type<?> getResult() {
            return _type;
        }

        void pushMethod(final BaseMethod method) {
            _methods.push(method);
        }

        BaseMethod popMethod() {
            return _methods.pop();
        }

        BaseMethod currentMethod() {
            return _methods.peek();
        }

        ClassType<?> getCurrentType() {
            return _type;
        }

        TypeElement getCurrentTypeElement() {
            return _typeElement;
        }

        List<Type<?>> getTypeArguments() {
            return _typeArguments;
        }

        Type<?> findType(final Element e) {
            return _elementTypeMap.get(e);
        }

        Type<?> resolveType(final TypeMirror t) {
            if (t instanceof com.sun.tools.javac.code.Type.ArrayType) {
                final com.sun.tools.javac.code.Type componentTypeMirror = ((com.sun.tools.javac.code.Type.ArrayType)t).getComponentType();
                final Type<?> componentType = resolveType(componentTypeMirror);

                if (componentType != null) {
                    return componentType.makeArrayType();
                }

                return null;
            }

            if (t instanceof com.sun.tools.javac.code.Type) {
                final com.sun.tools.javac.code.Type type = (com.sun.tools.javac.code.Type)t;
                final Type<?> fromMap = _elementTypeMap.get(type.asElement());

                if (fromMap != null) {
                    return fromMap;
                }
            }

            if (t instanceof com.sun.tools.javac.code.Type.WildcardType) {
                final com.sun.tools.javac.code.Type.WildcardType w = (com.sun.tools.javac.code.Type.WildcardType)t;
                if (w.isUnbound()) {
                    return new WildcardType(Types.Object, Type.NoType);
                }
                if (w.isExtendsBound()) {
                    final Type<?> extendsBound = resolveType(w.getExtendsBound());
                    return new WildcardType(extendsBound, Type.NoType);
                }
                return new WildcardType(Types.Object, resolveType(w.getSuperBound()));
            }

            if (t instanceof com.sun.tools.javac.code.Type) {
                final com.sun.tools.javac.code.Type type = (com.sun.tools.javac.code.Type)t;

                final Type fromMap = _elementTypeMap.get(type.asElement());

                if (fromMap != null) {
                    return fromMap;
                }

                final Type<?> result = resolveType(type.asElement());
                final List<com.sun.tools.javac.code.Type> typeArguments = type.getTypeArguments();

                if (typeArguments.isEmpty() || !result.isGenericType()) {
                    return result;
                }

                final Type<?>[] resolvedTypeArguments = new Type<?>[typeArguments.size()];

                for (int i = 0, n = typeArguments.size(); i < n; i++) {
                    final com.sun.tools.javac.code.Type typeArg = typeArguments.get(i);
                    if (typeArg instanceof TypeVariable) {
                        final Symbol genericElement = typeArg.asElement().getGenericElement();

                        if (genericElement instanceof Symbol.MethodSymbol) {
                            final ClassType<?> declaringType = (ClassType<?>)resolveType((Symbol.TypeSymbol)genericElement.getEnclosingElement());
                            final Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol)genericElement;
                            final int position = methodSymbol.getTypeParameters().indexOf(typeArg.asElement());
                            final BaseMethod declaredMethod = declaringType.findMethod(methodSymbol);

                            resolvedTypeArguments[i] = declaredMethod.getTypeArguments().get(position);
                        }
                        else {
                            final Symbol.TypeSymbol typeSymbol = (Symbol.TypeSymbol)genericElement;
                            final Type<?> declaringType = resolveType(typeSymbol);
                            final int position = typeSymbol.getTypeParameters().indexOf(typeArg.asElement());

                            if (declaringType instanceof ClassType<?>) {
                                resolvedTypeArguments[i] = ((ClassType<?>)declaringType).getGenericParameters().get(position);
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
                        result.getErasedClass(),
                        Type.list(resolvedTypeArguments)
                    )
                );

                if (fromCache != null) {
                    return fromCache;
                }

                for (final Type<?> resolvedTypeArgument : resolvedTypeArguments) {
                    if (!resolvedTypeArgument.isGenericParameter() || resolvedTypeArgument.getDeclaringType() != result) {
                        final GenericType genericType = new GenericType(
                            result,
                            resolvedTypeArguments
                        );

                        Type.CACHE.add(genericType);

                        return genericType;
                    }
                }

                return result;
            }

            return null;
        }

        Type<?> resolveType(final Symbol.TypeSymbol e) {
            final com.sun.tools.javac.code.Type type = e.asType();

            if (type instanceof com.sun.tools.javac.code.Type.ArrayType) {
                final com.sun.tools.javac.code.Type componentType = ((com.sun.tools.javac.code.Type.ArrayType)type).getComponentType();
                final Type<?> elementType = resolveType(componentType.asElement());

                if (elementType != null) {
                    return elementType.makeArrayType();
                }

                return null;
            }

            final Type<?> t = findType(e);

            if (t != null) {
                return t;
            }

            final TypeKind kind = type.getKind();

            if (kind.isPrimitive() || kind == TypeKind.VOID) {
                return resolvePrimitive(kind);
            }

            if (e.getKind() == ElementKind.TYPE_PARAMETER) {
                Frame currentFrame = this;

                while (currentFrame != null) {
                    final TypeParameter tp = currentFrame._type.findGenericParameter(e);

                    if (tp != null) {
                        return tp;
                    }

                    currentFrame = currentFrame._previous;
                }
            }

            if (e.getKind() == ElementKind.ENUM) {
                try {
                    final Class<?> clazz = Class.forName(e.flatName().toString());
                    return Type.of(clazz);
                }
                catch (ClassNotFoundException ex) {
                    throw Error.couldNotResolveType(e.flatName());
                }
            }

/*
            if (e.getKind() == ElementKind.CLASS ||
                e.getKind() == ElementKind.INTERFACE) {

                final Class<?> clazz;

                try {
                    clazz = Class.forName(e.flatName().toString());
                }
                catch (ClassNotFoundException ex) {
                    throw Error.couldNotResolveType(e.flatName());
                }

                final Type fromCache = Type.CACHE.find(clazz);

                if (fromCache != null) {
                    return fromCache;
                }
            }
*/

            final Frame frame = visit(e, this);

            if (frame != null) {
                return frame._type;
            }

            return null;
        }

        private Type<?> resolvePrimitive(final TypeKind type) {
            switch (type) {
                case BOOLEAN:
                    return PrimitiveTypes.Boolean;
                case BYTE:
                    return PrimitiveTypes.Byte;
                case SHORT:
                    return PrimitiveTypes.Short;
                case INT:
                    return PrimitiveTypes.Integer;
                case LONG:
                    return PrimitiveTypes.Long;
                case CHAR:
                    return PrimitiveTypes.Character;
                case FLOAT:
                    return PrimitiveTypes.Float;
                case DOUBLE:
                    return PrimitiveTypes.Double;
                case VOID:
                    return PrimitiveTypes.Void;
                default:
                    return null;
            }
        }

        Frame findFrame(final Element e) {
            Frame current = this;

            while (current != null) {
                if (current._typeElement.equals(e)) {
                    return current;
                }

                current = current._previous;
            }

            return null;
        }

        void addClassType(final Element e, final ClassType<?> type) {
            _elementTypeMap.put(
                VerifyArgument.notNull(e, "e"),
                VerifyArgument.notNull(type, "type")
            );

            _type.addNestedType(type);
        }

        void addTypeArgument(final Element e, final TypeParameter type) {
            _elementTypeMap.put(
                VerifyArgument.notNull(e, "e"),
                VerifyArgument.notNull(type, "type")
            );

            _typeArguments = _typeArguments.append(type);
        }
    }

    @Override
    public Frame visitPackage(final PackageElement e, final Frame frame) {
        return frame;
    }

    @Override
    public Frame visitUnknown(final Element e, final Frame frame) {
        if (e instanceof com.sun.tools.javac.code.Type.WildcardType) {
            return frame;
        }
        return frame;
    }

    @Override
    public Frame visitType(final TypeElement e, final Frame frame) {
        if (e.getNestingKind() == NestingKind.ANONYMOUS ||
            e.getNestingKind() == NestingKind.LOCAL) {

            return frame;
        }

        final Frame currentFrame = new Frame(e, frame);
        final Element enclosingElement = e.getEnclosingElement();

        if (enclosingElement instanceof Symbol.ClassSymbol) {
            final Type declaringType = currentFrame.resolveType((Symbol.ClassSymbol)enclosingElement);
            currentFrame.getCurrentType().setDeclaringType(declaringType);
        }

        for (final TypeParameterElement tpe : e.getTypeParameters()) {
            tpe.accept(this, currentFrame);
        }

        for (final Element ee : e.getEnclosedElements()) {
            if (ee instanceof Symbol.ClassSymbol) {
                this.visit(ee, currentFrame);
            }
        }

        for (final TypeParameter typeParameter : currentFrame.getCurrentType().getGenericParameters()) {
            final java.util.List<? extends TypeMirror> bounds = typeParameter.getElement().getBounds();

            for (final TypeMirror bound : bounds) {
                final Type<?> boundType = currentFrame.resolveType(bound);
                typeParameter.addBound(boundType);
            }
        }

        currentFrame.getCurrentType().complete();

        if (!Flags.asModifierSet(((Symbol)e).flags()).contains(Modifier.PRIVATE)) {
            Type.CACHE.add(currentFrame.getCurrentType());
        }

        return currentFrame;
    }

    @Override
    public Frame visitVariable(final VariableElement e, final Frame frame) {
        if (e.getKind() == ElementKind.PARAMETER) {
            return doVisitParameter(e, frame, frame.currentMethod());
        }

        if (e.getKind() == ElementKind.FIELD || e.getKind() == ElementKind.ENUM_CONSTANT) {
            try {
                final ClassField field = new ClassField(
                    (Symbol.VarSymbol)e,
                    frame.getCurrentType(),
                    frame.resolveType(e.asType())
                );

                frame.getCurrentType().addField(field);
            }
            catch (MemberResolutionException ignored) {
            }

            return frame;
        }

        return frame;
    }

    private Frame doVisitParameter(final VariableElement e, final Frame frame, final BaseMethod method) {
        final Type<?> parameterType = resolveType((com.sun.tools.javac.code.Type)e.asType(), frame);

        if (parameterType == null) {
            throw Error.couldNotResolveParameterType(e);
        }

        method.addParameter(
            new ParameterInfo(
                e.getSimpleName().toString(),
                parameterType
            )
        );

        return frame;
    }

    private Frame doVisitParameter(final VariableElement e, final Frame frame, final ClassConstructor constructor) {
        final Type<?> parameterType = frame.resolveType(e.asType());

        if (parameterType == null) {
            throw Error.couldNotResolveParameterType(e);
        }

        constructor.addParameter(
            new ParameterInfo(
                e.getSimpleName().toString(),
                parameterType
            )
        );

        return frame;
    }

    @Override
    public Frame visitExecutable(final ExecutableElement e, final Frame frame) {
        if (e.getKind() == ElementKind.METHOD) {
            return doVisitMethod(e, frame);
        }

        if (e.getKind() == ElementKind.CONSTRUCTOR) {
            return doVisitConstructor(e, frame);
        }

        return frame;
    }

    private Type<?> resolveType(final com.sun.tools.javac.code.Type type, final Frame frame) {
        if (type instanceof com.sun.tools.javac.code.Type.ArrayType) {
            final com.sun.tools.javac.code.Type.ArrayType arrayType = (com.sun.tools.javac.code.Type.ArrayType)type;
            return resolveType(arrayType.getComponentType(), frame).makeArrayType();
        }

        final Type<?> fromLookup = frame.findType(type.asElement());

        if (fromLookup != null) {
            final List<com.sun.tools.javac.code.Type> typeArguments = type.getTypeArguments();
            List<Type<?>> typeBindings = List.nil();

            for (final com.sun.tools.javac.code.Type typeArgument : typeArguments) {
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

    private Frame doVisitMethod(final ExecutableElement e, final Frame frame) {
        if (e.getKind() != ElementKind.METHOD) {
            return frame;
        }

        final Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol)e;
        final BaseMethod method = new BaseMethod(frame.getCurrentType(), methodSymbol);

        frame.getCurrentType().addMethod(method);
        frame.pushMethod(method);

        try {
            final java.util.List<? extends TypeParameterElement> typeParameters = e.getTypeParameters();

            for (final TypeParameterElement typeParameter : typeParameters) {
                visitTypeParameter(typeParameter, frame);
            }

            final List<com.sun.tools.javac.code.Type> thrownTypes = methodSymbol.getThrownTypes();

            for (final com.sun.tools.javac.code.Type thrownType : thrownTypes) {
                final Type<?> resolvedThrownType = resolveType(thrownType, frame);
                method.addThrownType(resolvedThrownType);
            }

            final com.sun.tools.javac.code.Type returnMirrorType = (com.sun.tools.javac.code.Type)e.getReturnType();
            final Type<?> returnType = resolveType(returnMirrorType, frame);

            method.setReturnType(returnType);

            final java.util.List<? extends VariableElement> parameters = e.getParameters();

            for (final VariableElement parameter : parameters) {
                doVisitParameter(parameter, frame, method);
            }

            method.allParametersAdded();
        }
        finally {
            frame.popMethod();
        }
        return frame;
    }

    private Frame doVisitConstructor(final ExecutableElement e, final Frame frame) {
        if (e.getKind() != ElementKind.CONSTRUCTOR) {
            return frame;
        }

        final Symbol.MethodSymbol constructorSymbol = (Symbol.MethodSymbol)e;
        final ClassConstructor constructor = new ClassConstructor(frame.getCurrentType(), constructorSymbol);
        final java.util.List<? extends VariableElement> parameters = e.getParameters();

        frame.getCurrentType().addConstructor(constructor);

        final List<com.sun.tools.javac.code.Type> thrownTypes = constructorSymbol.getThrownTypes();

        for (final com.sun.tools.javac.code.Type thrownType : thrownTypes) {
            final Type<?> resolvedThrownType = resolveType(thrownType, frame);
            constructor.addThrownType(resolvedThrownType);
        }

        if (frame.getCurrentType().getTypeElement().isInner()) {
            constructor.addParameter(
                new ParameterInfo(
                    "(outer)",
                    frame.getCurrentType().getDeclaringType()
                )
            );
        }

        for (final VariableElement parameter : parameters) {
            doVisitParameter(parameter, frame, constructor);
        }

        constructor.allParametersAdded();

        return frame;
    }

    @Override
    public Frame visitTypeParameter(final TypeParameterElement e, final Frame frame) {
        final TypeParameter typeParameter = new TypeParameter(frame._type, (Symbol.TypeSymbol)e);

        frame.addTypeArgument(e, typeParameter);

        if (e.getGenericElement() instanceof Symbol.MethodSymbol) {
            frame.currentMethod().addTypeParameter(typeParameter);
        }
        else {
            frame.getCurrentType().addGenericParameter(typeParameter);
        }

        return frame;
    }
}

@SuppressWarnings("unchecked")
class ClassType<T> extends Type<T> {
    private final String _name;
    private final Context _context;
    private final Symbol.ClassSymbol _typeElement;
    private boolean _membersResolved;
    private boolean _completed;
    private Type<?> _declaringType;
    private Class<T> _erasedClass;
    private List<TypeParameter> _genericParameters = List.nil();
    private List<ClassType<?>> _nestedTypes = List.nil();
    private List<BaseMethod> _methods = List.nil();
    private List<ClassField> _fields = List.nil();
    private List<ClassConstructor> _constructors = List.nil();
    private TypeBindings _typeBindings;
    private TypeList _resolvedTypeArguments;

    ClassType(final Context context, final Symbol.ClassSymbol typeElement) {
        _context = VerifyArgument.notNull(context, "context");
        _typeElement = VerifyArgument.notNull(typeElement, "typeElement");
        _name = typeElement.getQualifiedName().toString();
    }

    Symbol.ClassSymbol getTypeElement() {
        return _typeElement;
    }

    List<TypeParameter> getGenericParameters() {
        return _genericParameters;
    }

    BaseMethod findMethod(final Symbol.MethodSymbol methodSymbol) {
        for (final BaseMethod method : _methods) {
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

    void addMethod(final BaseMethod method) {
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

                    for (final BaseMethod method : _methods) {
                        method.complete();
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

        final ArrayList<BaseMethod> methods = new ArrayList<>(_methods.size());

        for (int i = 0, n = _methods.size(); i < n; i++) {
            final BaseMethod method = _methods.get(i);
            if (!method.isStatic()) {
                methods.add(method);
            }
        }

        return new MethodList(methods);
    }

    @Override
    MethodList getResolvedStaticMethods() {
        ensureMembersResolved();

        final ArrayList<BaseMethod> methods = new ArrayList<>(_methods.size());

        for (int i = 0, n = _methods.size(); i < n; i++) {
            final BaseMethod method = _methods.get(i);
            if (method.isStatic()) {
                methods.add(method);
            }
        }

        return new MethodList(methods);
    }

    @Override
    FieldList getResolvedFields() {
        ensureMembersResolved();
        return new FieldList(_fields);
    }

    void complete() {
        if (_completed) {
            return;
        }

        _completed = true;

        final Class<T> clazz;

        try {
            clazz = (Class<T>)Class.forName(_typeElement.flatName().toString());
        }
        catch (ClassNotFoundException ignored) {
            throw Error.couldNotResolveType(_typeElement.flatName());
        }

        _erasedClass = clazz;

        if (_genericParameters.isEmpty()) {
            _resolvedTypeArguments = TypeList.empty();
            _typeBindings = TypeBindings.empty();
        }
        else {
            for (final TypeParameter genericParameter : _genericParameters) {
                genericParameter.complete();
            }

            _resolvedTypeArguments = list(_genericParameters);
            _typeBindings = TypeBindings.createUnbound(_resolvedTypeArguments);
        }

        for (final ClassType<?> nestedType : _nestedTypes) {
            nestedType.complete();
        }

        for (final ClassField field : _fields) {
            field.complete();
        }

        for (final ClassConstructor constructor : _constructors) {
            constructor.complete();
        }

        final ArrayList<BaseMethod> methods = new ArrayList<>(_methods);

        for (int i = 0, n = methods.size(); i < n; i++) {
            final BaseMethod method = methods.get(i);
            try {
                method.complete();
            }
            catch (MemberResolutionException e) {
                --n;
                methods.remove(i--);
            }
        }
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
        return (int)(_typeElement.flags() & Flags.ModifierFlags);
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitType(this, parameter);
    }

    @Override
    protected StringBuilder _appendClassName(final StringBuilder sb, final boolean dottedName) {
        if (dottedName) {
            return sb.append(_name);
        }
        return super._appendClassName(sb, dottedName);
    }
}

final class GenericClassType<T> extends Type<T> {
    private final Context _context;
    private final ClassType<T> _genericTypeDefinition;

    private final TypeBindings _typeBindings;

    private boolean _completed;
    private List<ClassType<?>> _nestedTypes = List.nil();
    private List<BaseMethod> _methods = List.nil();
    private List<ClassField> _fields = List.nil();
    private List<ClassConstructor> _constructors = List.nil();

    GenericClassType(final Context context, final ClassType<T> genericTypeDefinition, final TypeBindings typeBindings) {
        _context = VerifyArgument.notNull(context, "context");
        _genericTypeDefinition = VerifyArgument.notNull(genericTypeDefinition, "genericTypeDefinition");
        _typeBindings = VerifyArgument.notNull(typeBindings, "typeBindings");
    }

    @Override
    ConstructorList getResolvedConstructors() {
        completeIfNecessary();
        return super.getResolvedConstructors();
    }

    private void completeIfNecessary() {
        if (_completed) {
            return;
        }

        synchronized (CACHE_LOCK) {
            if (!_completed) {
                complete();
            }
        }
    }

    void complete() {
        if (_completed) {
            return;
        }

        final JavacTypes jcTypes = JavacTypes.instance(_context);
        final JavaCompiler compiler = JavaCompiler.instance(_context);
        final com.sun.tools.javac.code.Types types = com.sun.tools.javac.code.Types.instance(_context);

        final TypeMirror[] typeArgMirrors = new TypeMirror[_typeBindings.size()];

        for (int i = 0, n = typeArgMirrors.length; i < n; i++) {
            final Type boundType = _typeBindings.getBoundType(i);
            if (boundType instanceof TypeParameter) {
                typeArgMirrors[i] = ((TypeParameter)boundType).getElement().asType();
            }
            else {
                typeArgMirrors[i] = compiler.resolveIdent(boundType.getName()).asType();
            }
        }

        final com.sun.tools.javac.code.Type.ClassType declaredType = (com.sun.tools.javac.code.Type.ClassType)jcTypes.getDeclaredType(
            (Symbol.ClassSymbol)_genericTypeDefinition.getTypeElement(),
            typeArgMirrors
        );

        final FieldList resolvedFields = _genericTypeDefinition.getResolvedFields();

        for (int i = 0, n = resolvedFields.size(); i < n; i++) {
            final ClassField originalFieldInfo = (ClassField)resolvedFields.get(i);
            final Type originalFieldType = originalFieldInfo.getFieldType();
            final Symbol.VarSymbol originalElement = originalFieldInfo.getElement();

            final com.sun.tools.javac.code.Type newFieldType = (com.sun.tools.javac.code.Type)
                jcTypes.asMemberOf(declaredType, originalElement);

            if (jcTypes.isSameType(newFieldType, originalElement.asType())) {
                _fields = _fields.append(originalFieldInfo);
                continue;
            }

            final Symbol.VarSymbol newFieldSymbol = (Symbol.VarSymbol)originalElement.asMemberOf(declaredType, types);

            if (_typeBindings.containsGenericParameter(originalFieldType)) {
                final Type<?> typeArgument = _typeBindings.getBoundType(originalFieldType);

                final ClassField newField = new ClassField(
                    newFieldSymbol,
                    this,
                    typeArgument
                );

                _fields = _fields.append(newField);
            }
            else {
                final ClassField newField = new ClassField(
                    newFieldSymbol,
                    this,
                    Type.of(newFieldType)
                );

                _fields = _fields.append(newField);
            }
        }

        _completed = true;
    }

    @Override
    MethodList getResolvedInstanceMethods() {
        return super.getResolvedInstanceMethods();
    }

    @Override
    MethodList getResolvedStaticMethods() {
        return super.getResolvedStaticMethods();
    }

    @Override
    FieldList getResolvedFields() {
        return super.getResolvedFields();
    }

    @Override
    public Class<T> getErasedClass() {
        return _genericTypeDefinition.getErasedClass();
    }

    @Override
    protected TypeBindings getTypeBindings() {
        return _typeBindings;
    }

    @Override
    public Type getGenericTypeDefinition() {
        return _genericTypeDefinition;
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public Type getDeclaringType() {
        return _genericTypeDefinition.getDeclaringType();
    }

    @Override
    int getModifiers() {
        return _genericTypeDefinition.getModifiers();
    }

    @Override
    public <P, R> R accept(final TypeVisitor<P, R> visitor, final P parameter) {
        return visitor.visitTypeParameter(this, parameter);
    }
}

class TypeParameter extends Type {

    private final String _name;
    private final ClassType<?> _declaringType;
    private final MethodInfo _declaringMethod;
    private final Symbol.TypeSymbol _element;
    private List<Type<?>> _boundsList = List.nil();
    private Class<?> _erasedClass;
    private TypeList _bounds;

    TypeParameter(final ClassType<?> declaringType, final Symbol.TypeSymbol element) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _declaringMethod = null;
        _element = VerifyArgument.notNull(element, "element");
        _name = _element.getSimpleName().toString();
    }

    TypeParameter(final MethodInfo declaringMethod, final Symbol.TypeSymbol element) {
        _declaringType = null;
        _declaringMethod = VerifyArgument.notNull(declaringMethod, "declaringMethod");
        _element = VerifyArgument.notNull(element, "element");
        _name = _element.getSimpleName().toString();
    }

    void addBound(final Type<?> bound) {
        VerifyArgument.notNull(bound, "bound");
        _boundsList = _boundsList.append(bound);
    }

    TypeParameterElement getElement() {
        return _element;
    }

    void complete() {
        if (_bounds != null) {
            return;
        }

        for (final Type<?> type : _boundsList) {
            if (type instanceof ClassType<?>) {
                ((ClassType)type).complete();
            }
        }

        _bounds = list(_boundsList);

        if (_boundsList.size() == 1) {
            _erasedClass = _boundsList.head.getErasedClass();
            return;
        }

        for (final Type<?> type : _boundsList) {
            if (!type.isInterface()) {
                _erasedClass = type.getErasedClass();
                return;
            }
        }

        final ArrayList<Type<?>> interfaceBounds = new ArrayList<>(_boundsList);

        outer:
        while (interfaceBounds.size() > 1) {
            final Type<?> a = interfaceBounds.get(0);

            for (int i = 1, n = interfaceBounds.size(); i < n; i++) {
                final Type<?> b = interfaceBounds.get(i);
                final Type moreSpecific = getMostSpecificType(a, b);

                if (moreSpecific == null) {
                    _erasedClass = Object.class;
                    return;
                }

                if (moreSpecific == a) {
                    interfaceBounds.remove(0);
                    continue outer;
                }

                interfaceBounds.remove(i--);
            }
        }

        if (interfaceBounds.size() == 1) {
            _erasedClass = interfaceBounds.get(0).getErasedClass();
            return;
        }

        _erasedClass = Object.class;
    }

    @Override
    public boolean isGenericParameter() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append(getName());
    }

    @Override
    public StringBuilder appendFullDescription(final StringBuilder sb) {
        return sb.append(getName());
    }

    @Override
    public Class getErasedClass() {
        if (_erasedClass == null) {
            synchronized (CACHE_LOCK) {
                if (_erasedClass == null) {
                    complete();
                }
            }
        }
        return _erasedClass;
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.TypeInfo;
    }

    @Override
    public TypeList getGenericParameterConstraints() {
        if (_bounds == null) {
            synchronized (CACHE_LOCK) {
                if (_bounds == null) {
                    complete();
                }
            }
        }
        return _bounds;
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public MethodInfo getDeclaringMethod() {
        return _declaringMethod;
    }

    @Override
    int getModifiers() {
        return (int)(Flags.ModifierFlags & _element.flags());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object accept(final TypeVisitor visitor, final Object parameter) {
        return visitor.visitTypeParameter(this, parameter);
    }
}

class BaseMethod extends MethodInfo {
    private final ClassType<?> _declaringType;
    private final Symbol.MethodSymbol _element;
    private List<Type> _thrownTypeList = List.nil();
    private List<ParameterInfo> _parameterList = List.nil();
    private List<TypeParameter> _typeParameterList = List.nil();
    private TypeBindings _typeBindings;
    private TypeList _typeParameters;
    private TypeList _thrownTypes;
    private ParameterList _parameters;
    private Type<?> _returnType;
    private Method _resolvedMethod;
    private final String _name;
    private CompletionState _completionState = CompletionState.AWAITING_PARAMETERS;

    BaseMethod(final ClassType<?> declaringType, final Symbol.MethodSymbol element) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _element = VerifyArgument.notNull(element, "element");
        _name = element.getSimpleName().toString();
    }

    Symbol.MethodSymbol getElement() {
        return _element;
    }

    void allParametersAdded() {
        final CompletionState oldCompletionState = _completionState;

        _completionState = CompletionState.CAN_COMPLETE;

        if (oldCompletionState == CompletionState.PENDING) {
            complete();
        }
    }

    void setReturnType(final Type<?> returnType) {
        _returnType = VerifyArgument.notNull(returnType, "returnType");
    }

    void addParameter(final ParameterInfo parameter) {
        _parameterList = _parameterList.append(VerifyArgument.notNull(parameter, "parameter"));
    }

    void addTypeParameter(final TypeParameter typeParameter) {
        _typeParameterList = _typeParameterList.append(typeParameter);
    }

    void addThrownType(final Type<?> type) {
        _thrownTypeList = _thrownTypeList.append(VerifyArgument.notNull(type, "type"));
    }

    void complete() {
        if (_completionState == CompletionState.AWAITING_PARAMETERS) {
            _completionState = CompletionState.PENDING;
            return;
        }

        if (_resolvedMethod != null || _completionState != CompletionState.CAN_COMPLETE) {
            return;
        }

        if (_parameters == null) {
            if (_parameterList.isEmpty()) {
                _parameters = ParameterList.empty();
            }
            else {
                _parameters = new ParameterList(_parameterList);
            }
        }

        if (_typeParameters == null) {
            if (_typeParameterList.isEmpty()) {
                _typeParameters = TypeList.empty();
                _typeBindings = TypeBindings.empty();
            }
            else {
                _typeParameters = Type.list(_typeParameterList);
                _typeBindings = TypeBindings.createUnbound(_typeParameters);
            }
        }

        final Class<?>[] parameterClasses;

        if (_parameters.isEmpty()) {
            parameterClasses = EmptyArrayCache.fromElementType(Class.class);
        }
        else {
            parameterClasses = new Class<?>[_parameters.size()];

            for (int i = 0, n = parameterClasses.length; i < n; i++) {
                parameterClasses[i] = _parameters.get(i).getParameterType().getErasedClass();
            }
        }

        if (_thrownTypeList.isEmpty()) {
            _thrownTypes = TypeList.empty();
        }
        else {
            _thrownTypes = new TypeList(_thrownTypeList);
        }

        try {
            _resolvedMethod = _declaringType.getErasedClass().getDeclaredMethod(
                _name,
                parameterClasses
            );
        }
        catch (final NoSuchMethodException e) {
            throw Error.couldNotResolveMember(this);
        }
    }

    @Override
    public String toString() {
        if (_resolvedMethod == null) {
            return _element.toString();
        }
        return super.toString();
    }

    @Override
    public boolean isStatic() {
        return _element.isStatic();
    }

    @Override
    public Type getReturnType() {
        return _returnType;
    }

    @Override
    protected TypeBindings getTypeBindings() {
        return _typeBindings;
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public TypeList getThrownTypes() {
        return _thrownTypes;
    }

    @Override
    public CallingConvention getCallingConvention() {
        if (_element.isVarArgs()) {
            return CallingConvention.VarArgs;
        }
        return CallingConvention.Standard;
    }

    @Override
    public Method getRawMethod() {
        if (_resolvedMethod == null) {
            complete();
        }
        return _resolvedMethod;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    int getModifiers() {
        return (int)(_element.flags() & Flags.ModifierFlags);
    }
}

class ClassConstructor extends ConstructorInfo {
    private final ClassType<?> _declaringType;
    private final Symbol.MethodSymbol _element;
    private List<ParameterInfo> _parameterList = List.nil();
    private List<Type<?>> _thrownTypeList = List.nil();
    private TypeList _thrownTypes;
    private ParameterList _parameters;
    private Constructor<?> _resolvedConstructor;
    private final String _name;
    private CompletionState _completionState = CompletionState.AWAITING_PARAMETERS;

    ClassConstructor(final ClassType<?> declaringType, final Symbol.MethodSymbol element) {
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _element = VerifyArgument.notNull(element, "element");
        _name = element.getSimpleName().toString();
    }

    Symbol.MethodSymbol getElement() {
        return _element;
    }

    void allParametersAdded() {
        final CompletionState oldCompletionState = _completionState;

        _completionState = CompletionState.CAN_COMPLETE;

        if (oldCompletionState == CompletionState.PENDING) {
            complete();
        }
    }

    void addParameter(final ParameterInfo parameter) {
        _parameterList = _parameterList.append(VerifyArgument.notNull(parameter, "parameter"));
    }

    void addThrownType(final Type<?> type) {
        _thrownTypeList = _thrownTypeList.append(VerifyArgument.notNull(type, "type"));
    }

    void complete() {
        if (_completionState == CompletionState.AWAITING_PARAMETERS) {
            _completionState = CompletionState.PENDING;
            return;
        }

        if (_resolvedConstructor != null || _completionState != CompletionState.CAN_COMPLETE) {
            return;
        }

        if (_parameters == null) {
            if (_parameterList.isEmpty()) {
                _parameters = ParameterList.empty();
            }
            else {
                _parameters = new ParameterList(_parameterList);
            }
        }

        final Class<?>[] parameterClasses;

        if (_parameters.isEmpty()) {
            parameterClasses = EmptyArrayCache.fromElementType(Class.class);
        }
        else {
            parameterClasses = new Class<?>[_parameters.size()];

            for (int i = 0, n = parameterClasses.length; i < n; i++) {
                parameterClasses[i] = _parameters.get(i).getParameterType().getErasedClass();
            }
        }

        if (_thrownTypeList.isEmpty()) {
            _thrownTypes = TypeList.empty();
        }
        else {
            _thrownTypes = new TypeList(_thrownTypeList);
        }

        try {
            _resolvedConstructor = _declaringType.getErasedClass().getDeclaredConstructor(
                parameterClasses
            );
        }
        catch (final NoSuchMethodException e) {
            throw Error.couldNotResolveMember(this);
        }
    }

    @Override
    public String toString() {
        if (_resolvedConstructor == null) {
            return _element.toString();
        }
        return super.toString();
    }

    @Override
    public Constructor<?> getRawConstructor() {
        if (_resolvedConstructor == null) {
            complete();
        }
        return _resolvedConstructor;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    public TypeList getThrownTypes() {
        return _thrownTypes;
    }

    @Override
    public ParameterList getParameters() {
        return _parameters;
    }

    @Override
    public CallingConvention getCallingConvention() {
        if (_element.isVarArgs()) {
            return CallingConvention.VarArgs;
        }
        return CallingConvention.Standard;
    }

    @Override
    int getModifiers() {
        return (int)(_element.flags() & Flags.ModifierFlags);
    }
}

class ClassField extends FieldInfo {
    private final String _name;
    private final Symbol.VarSymbol _element;
    private final Type<?> _declaringType;
    private final Type<?> _fieldType;
    private final Field _field;

    ClassField(final Symbol.VarSymbol element, final Type<?> declaringType, final Type<?> fieldType) {
        _element = VerifyArgument.notNull(element, "element");
        _declaringType = VerifyArgument.notNull(declaringType, "declaringType");
        _fieldType = VerifyArgument.notNull(fieldType, "fieldType");
        _name = _element.getSimpleName().toString();

        try {
            _field = getDeclaringType().getErasedClass().getDeclaredField(_name);
        }
        catch (NoSuchFieldException e) {
            throw Error.couldNotResolveMember(this);
        }
    }

    Symbol.VarSymbol getElement() {
        return _element;
    }

    void complete() {
        if (_fieldType instanceof ClassType<?>) {
            ((ClassType<?>)_fieldType).complete();
        }
    }

    @Override
    public Type getFieldType() {
        return _fieldType;
    }

    @Override
    public boolean isEnumConstant() {
        return _element.getKind() == ElementKind.ENUM_CONSTANT;
    }

    @Override
    public Field getRawField() {
        return _field;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public Type getDeclaringType() {
        return _declaringType;
    }

    @Override
    int getModifiers() {
        return (int)(_element.flags() & Flags.ModifierFlags);
    }

    @Override
    public String toString() {
        return getName();
    }
}

enum CompletionState {
    UNTRIED,
    AWAITING_PARAMETERS,
    CAN_COMPLETE,
    PENDING,
    COMPLETED
}

