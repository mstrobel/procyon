package com.strobel.expressions;

import com.fasterxml.classmate.*;
import com.fasterxml.classmate.members.RawMethod;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.strobel.util.TypeUtils;

import java.lang.reflect.*;
import java.util.List;

/**
 * @author Mike Strobel
 */
public final class MethodBinder {
    private MethodBinder() {}

        public static Method findMethod(
            final Class type,
            final String methodName,
            final Class<?>[] typeArgs,
            final List<Expression> args,
            final int flags) {

            final TypeResolver typeResolver = new TypeResolver();
            final ResolvedType resolvedType = typeResolver.resolve(type);
            final ResolvedType[] resolvedTypeArgs = new ResolvedType[typeArgs != null ? typeArgs.length : 0];
            final ResolvedType[] argTypes = new ResolvedType[args.size()];

            for (int i = 0, n = resolvedTypeArgs.length; i < n; i++) {
                assert typeArgs != null;
                resolvedTypeArgs[i] = typeResolver.resolve(typeArgs[i]);
            }

            for (int i = 0, n = args.size(); i < n; i++) {
                argTypes[i] = typeResolver.resolve(args.get(i).getType());
            }

            final MemberResolver memberResolver = new MemberResolver(typeResolver);

            memberResolver.setMethodFilter(
                new Filter<RawMethod>() {
                    @Override
                    public boolean include(final RawMethod element) {
                        return element.getName().equalsIgnoreCase(methodName);
                    }
                }
            );

            final ResolvedTypeWithMembers resolvedTypeWithMembers = memberResolver.resolve(resolvedType, null, null);
            final ResolvedMethod[] candidates;

            if (Modifier.isStatic(flags)) {
                candidates = resolvedTypeWithMembers.getStaticMethods();
            }
            else {
                candidates = resolvedTypeWithMembers.getMemberMethods();
            }

            if (candidates == null || candidates.length == 0) {
                throw Error.methodDoesNotExistOnType(methodName, resolvedType.getErasedType());
            }

            final int bestMethod = findBestMethod(candidates, resolvedTypeArgs, argTypes);

            if (bestMethod == -1) {
                if (typeArgs != null && typeArgs.length > 0) {
                    throw Error.genericMethodWithArgsDoesNotExistOnType(methodName, type);
                }
                else {
                    throw Error.methodWithArgsDoesNotExistOnType(methodName, type);
                }
            }
            else if (bestMethod == -2) {
                throw Error.methodWithMoreThanOneMatch(methodName, type);
            }
            else {
                return candidates[bestMethod].getRawMember();
            }
        }
/*
    public static Method findMethod(
        final Class type,
        final String methodName,
        final Class<?>[] typeArgs,
        final List<Expression> args,
        final int flags) {

        final Class[] argTypes = new Class[args.size()];

        for (int i = 0, n = args.size(); i < n; i++) {
            argTypes[i] = args.get(i).getType();
        }

        return getMatchingAccessibleMethod(type, methodName, argTypes, Modifier.isStatic(flags));
    }
*/

    private static int findBestMethod(final ResolvedMethod[] candidates, final ResolvedType[] typeArgs, final ResolvedType[] args) {
        int count = 0;
        int method = -1;

        for (int i = 0, candidatesSize = candidates.length; i < candidatesSize; i++) {
            final ResolvedMethod candidate = candidates[i];

            if (candidate != null && isCompatible(candidate, typeArgs, args)) {
                // favor public over non-public methods
                if (method == -1 || (!candidates[method].isPublic() && candidate.isPublic())) {
                    method = i;
                    count = 1;
                }
                else if (candidates[method].isPublic() == candidate.isPublic()) {
                    // only count it as additional method if they both public or both non-public
                    count++;
                }
            }
        }

        if (count > 1) {
            return -2;
        }

        return method;
    }

    private static boolean isCompatible(final ResolvedMethod candidate, final ResolvedType[] typeArgs, final ResolvedType[] argTypes) {
        final int argumentCount = candidate.getArgumentCount();
        final ResolvedType[] parameterTypes = new ResolvedType[argumentCount];

        for (int i = 0; i < argumentCount; i++) {
            parameterTypes[i] = candidate.getArgumentType(i);
        }

        if (parameterTypes.length != argTypes.length) {
            return false;
        }

        for (int i = 0; i < argumentCount; i++) {
            final ResolvedType argType = argTypes[i];
            final ResolvedType pType = parameterTypes[i];

            if (!areAssignmentCompatible(pType.getErasedType(), argType.getErasedType())) {
                return false;
            }
        }

        final Method rawMethod = candidate.getRawMember();
        final Type[] genericParameterTypes = rawMethod.getGenericParameterTypes();
        final TypeVariable<Method>[] typeParameters = rawMethod.getTypeParameters();
        final TypeResolver typeResolver = new TypeResolver();

        if (typeParameters.length != typeArgs.length) {
            return false;
        }

        final TypeBindings typeBindings = TypeBindings.emptyBindings();

        for (int i = 0, n = typeParameters.length; i < n; i++) {
            final TypeVariable<Method> t = typeParameters[i];
            typeBindings.withAdditionalBinding(t.getName(), typeArgs[i]);
        }

        for (int i = 0, n = genericParameterTypes.length; i < n; i++) {
            final Type t = genericParameterTypes[i];
            final ResolvedType callerTypeArg = argTypes[i];

            if (t instanceof Class<?>) {
                if (!areAssignmentCompatible((Class)t, callerTypeArg.getErasedType())) {
                    return false;
                }
            }
            else if (t instanceof ParameterizedType) {
                final ResolvedType resolvedParameterizedType = typeResolver.resolve(t, typeBindings);
                if (!testUpperBound(resolvedParameterizedType, callerTypeArg)) {
                    return false;
                }
            }
            else if (t instanceof GenericArrayType && callerTypeArg.isArray()) {
                final GenericArrayType arrayType = (GenericArrayType)t;
                final ResolvedType resolvedArrayType = typeResolver.resolve(arrayType.getGenericComponentType(), typeBindings);
                if (!testUpperBound(resolvedArrayType, callerTypeArg.getArrayElementType())) {
                    return false;
                }
            }
            else if (t instanceof TypeVariable<?>) {
                final Type[] upperBounds = ((TypeVariable)t).getBounds();

                for (final Type upperBound : upperBounds) {
                    if (!testUpperBound(typeResolver.resolve(upperBound, typeBindings), argTypes[i])) {
                        return false;
                    }
                }
            }
            else if (t instanceof WildcardType) {
                final WildcardType w = (WildcardType)t;
                final Type[] upperBounds = w.getUpperBounds();
                final Type[] lowerBounds = w.getUpperBounds();

                for (final Type upperBound : upperBounds) {
                    if (!testUpperBound(argTypes[i], typeResolver.resolve(upperBound, typeBindings))) {
                        return false;
                    }
                }

                for (final Type lowerBound : lowerBounds) {
                    if (!testLowerBond(argTypes[i], typeResolver.resolve(lowerBound, typeBindings))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static boolean areAssignmentCompatible(final Class destination, final Class source) {
        return TypeUtils.areReferenceAssignable(destination, source) ||
               TypeUtils.hasIdentityPrimitiveOrBoxingConversion(destination, source);
    }

    private static boolean testUpperBound(final ResolvedType boundType, final ResolvedType argType) {
        return areAssignmentCompatible(boundType.getErasedType(), argType.getErasedType());
    }

    private static boolean testLowerBond(final ResolvedType boundType, final ResolvedType argType) {
        return areAssignmentCompatible(argType.getErasedType(), boundType.getErasedType());
    }
}

interface IMethodFilter<T> {
    public static final IMethodFilter<String> FilterNameIgnoreCase = new IMethodFilter<String>() {
        @Override
        public boolean accept(final Method m, final String name) {
            return m.getName().equalsIgnoreCase(name);
        }
    };
    boolean accept(final Method m, final T filterCriteria);
}