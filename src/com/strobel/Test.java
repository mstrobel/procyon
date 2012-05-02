package com.strobel;

import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.strobel.core.VerifyArgument;
import com.strobel.expressions.Expression;
import com.strobel.expressions.MethodBinder;
import com.strobel.util.TypeUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Mike Strobel
 */
public class Test {
    public interface IMethodFilter<T> {
        public static final IMethodFilter<String> FilterNameIgnoreCase = new IMethodFilter<String>() {
            @Override
            public boolean accept(final Method m, final String name) {
                return m.getName().equalsIgnoreCase(name);
            }
        };
        boolean accept(final Method m, final T filterCriteria);
    }

    public static <TFilterArg> List<Method> findMethods(
        final Class declaringType,
        final IMethodFilter<? super TFilterArg> filter,
        final TFilterArg filterParameter,
        final int flags) {

        List<Method> candidates = null;

        final Method[] methods = declaringType.getMethods();

        for (final Method candidate : methods) {
            if (filter.accept(candidate, filterParameter)) {
                if (candidates == null) {
                    candidates = new ArrayList<>();
                }
                candidates.add(candidate);
            }
        }

        if (candidates != null) {
            return candidates;
        }

        return Collections.emptyList();
    }

    public static void main(final String[] args) {
        try {
            final List<Expression> argList = new ArrayList<>();

            argList.add(Expression.constant(Test.class));
            argList.add(
                Expression.constant(
                    new IMethodFilter<Expression>() {
                        @Override
                        public boolean accept(final Method m, final Expression filterCriteria) {
                            return true;
                        }
                    }
                )
            );
            argList.add(Expression.constant("findMethods"));
            argList.add(Expression.constant(0));

            final TypeResolver typeResolver = new TypeResolver();
            final ResolvedType resolvedType = typeResolver.resolve(Test.class);
            final MemberResolver memberResolver = new MemberResolver(typeResolver);
            final ResolvedTypeWithMembers typeWithMembers = memberResolver.resolve(resolvedType, null, null);

            final ResolvedType[] typeArgs = new ResolvedType[] { typeResolver.resolve(String.class) };
            final ResolvedMethod m = typeWithMembers.getStaticMethods()[1];
/*
            findMethods(
                Test.class,
                new IMethodFilter<Expression>() {
                    @Override
                    public boolean accept(final Method m, final Expression filterCriteria) {
                        return true;
                    }
                },
                "findMethods",
                0
            );
*/

            final Method method = MethodBinder.findMethod(
                Test.class,
                "findMethods",
                new Class[] { String.class },
                argList,
                Modifier.STATIC
            );

            if (method == null) {
                System.err.println("No match found!");
                System.exit(-1);
            }

            System.out.println(method.toString());
            System.out.println(method.toGenericString());
            System.out.println("================================================================================");
        }
        catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }

    private static boolean isCompatible(final Method candidate, final List<Expression> args) {
        final Class<?>[] parameterTypes = candidate.getParameterTypes();

        if (parameterTypes.length != args.size()) {
            return false;
        }

        for (int i = 0, n = parameterTypes.length; i < n; i++) {
            final Expression arg = args.get(i);

            VerifyArgument.notNull(arg, "argument");

            final Class argType = arg.getType();
            final Class<?> pType = parameterTypes[i];

            if (!TypeUtils.areReferenceAssignable(pType, argType) &&
                !TypeUtils.hasIdentityPrimitiveOrBoxingConversion(pType, argType)) {

                return false;
            }
        }

        return true;
    }
}
