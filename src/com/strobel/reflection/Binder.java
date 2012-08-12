package com.strobel.reflection;

import com.strobel.core.ArrayUtilities;
import com.strobel.util.ContractUtils;
import com.strobel.util.TypeUtils;

import java.util.Set;

/**
 * @author Mike Strobel
 */
public abstract class Binder {
    static boolean compareMethodSignatureAndName(final MethodBase m1, final MethodBase m2) {
        final ParameterList p1 = m1.getParameters();
        final ParameterList p2 = m2.getParameters();

        if (p1.size() != p2.size()) {
            return false;
        }

        for (int i = 0, n = p1.size(); i < n; i++) {
            if (!TypeUtils.areEquivalent(p1.get(i).getParameterType(), p2.get(i).getParameterType())) {
                return false;
            }
        }

        return true;
    }

    static MethodBase findMostDerivedNewSlotMethod(final MethodBase[] match, final int cMatches) {
        throw ContractUtils.unreachable();
    }

    public abstract MethodBase selectMethod(final Set<BindingFlags> bindingFlags, final MethodBase[] matches, final Type[] parameterTypes);
}

final class DefaultBinder extends Binder {

    @Override
    public MethodBase selectMethod(final Set<BindingFlags> bindingFlags, final MethodBase[] matches, final Type[] parameterTypes) {
        if (ArrayUtilities.isNullOrEmpty(matches)) {
            return null;
        }
        if (matches.length == 1) {
            return matches[0];
        }
        throw Error.ambiguousMatch();
    }

/*
    private MethodBase findMethod(
        final MethodBase callSite,
        final String name,
        final TypeList argumentTypes,
        final TypeList typeArguments,
        final boolean allowBoxing,
        final boolean useVarArgs,
        final boolean operator) {

        return findMethod(
            callSite,
            name,
            argumentTypes,
            typeArguments,
            callSite.getDeclaringType(),
            true,
            null,
            allowBoxing,
            useVarArgs,
            operator,
            new HashSet<>()
        );
    }

    private MethodBase findMethod(
        final MethodBase callSite,
        final String name,
        final TypeList argumentTypes,
        final TypeList typeArguments,
        final Type<?> inType,
        boolean abstractOk,
        MethodBase bestSoFar,
        final boolean allowBoxing,
        final boolean useVarArgs,
        final boolean operator,
        final Set<Type<?>> seen) {

        for (Type<?> ct = inType; ct.isClass() || ct.isGenericParameter(); ct = ct.getBaseType()) {
            while (ct.isGenericParameter()) {
                ct = ct.getExtendsBound();
            }

            if (!seen.add(ct)) {
                return bestSoFar;
            }

            if ((ct.getModifiers() & (Modifier.ABSTRACT | Modifier.INTERFACE | Type.ENUM_MODIFIER)) == 0) {
                abstractOk = false;
            }

            for (final Object o : ct.getMember(name, MemberType.Method)) {
                final MethodBase method = (MethodBase)o;

                bestSoFar = selectBest(
                    callSite,
                    argumentTypes,
                    typeArguments,
                    method,
                    bestSoFar,
                    allowBoxing,
                    useVarArgs,
                    operator
                );
            }

            if ("<init>".equals(name)) {
                break;
            }

            if (abstractOk) {
                MethodInfo concrete = null;

                if (bestSoFar != null && !Modifier.isAbstract(bestSoFar.getModifiers())) {
                    concrete = (MethodInfo)bestSoFar;
                }

                for (final Type interfaceType : ct.getInterfaces()) {
                    bestSoFar = findMethod(
                        callSite,
                        name,
                        argumentTypes,
                        typeArguments,
                        interfaceType,
                        abstractOk,
                        bestSoFar,
                        allowBoxing,
                        useVarArgs,
                        operator,
                        seen
                    );
                }

                if (concrete != bestSoFar &&
                    concrete != null &&
                    bestSoFar != null &&
                    Helper.isSubSignature(concrete, (MethodInfo)bestSoFar)) {

                    bestSoFar = concrete;
                }
            }
        }

        return bestSoFar;
    }

    private MethodBase selectBest(
        final MethodBase callSite,
        final TypeList argumentTypes,
        final TypeList typeArguments,
        final MethodBase candidate,
        final MethodBase bestSoFar,
        final boolean allowBoxing,
        final boolean useVarArgs,
        final boolean operator) {

        if (candidate == null) {
            return bestSoFar;
        }

        if (!Helper.isInheritedIn(callSite.getDeclaringType(), candidate)) {
            return bestSoFar;
        }

        final ResolutionContext context = currentResolutionContext.get();

        try {
            final MethodBase m = rawInstantiate(
                callSite,
                candidate,
                argumentTypes,
                typeArguments,
                allowBoxing,
                useVarArgs
            );

            if (!operator) {
                context.addApplicableCandidate(candidate, m);
            }
        }
        catch (InapplicableMethodException e) {
            if (!operator) {
                context.addInapplicableCandidate(candidate);
            }
            return bestSoFar;
        }

        if (!isAccessible(callSite, candidate)) {
            return bestSoFar;
        }

        return mostSpecific(
            candidate,
            bestSoFar,
            callSite,
            allowBoxing && operator,
            useVarArgs
        );
    }

    private static Type<?> outermostType(final Type<?> t) {
        Type<?> outermost = t;
        while (outermost.isNested()) {
            outermost = outermost.getDeclaringType();
        }
        return outermost;
    }

    private final static int AccessFlags = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;


    public boolean isAccessible(final Type<?> site, final Type<?> type, final boolean checkInner) {
        if (type.isArray())
            return isAccessible(site, type.getElementType(), checkInner);

        boolean isAccessible = false;

        switch (type.getModifiers() & AccessFlags) {
            case Modifier.PRIVATE:
                isAccessible = TypeUtils.areEquivalent(outermostType(site), outermostType(type));
                break;

            case 0:
                isAccessible = Helper.inSamePackage(site, type) ||
                               site.getDeclaringMethod() != null &&
                               // check for synthesized anonymous inner class ctor:
                               (site.getDeclaringMethod().getModifiers() & (1 << 29)) != 0;
                break;

            case Modifier.PUBLIC:
                isAccessible = true;
                break;

            case Modifier.PROTECTED:
                isAccessible = Helper.inSamePackage(site, type) ||
                               isInnerSubtype(site, type);
                break;
        }

        final Type declaringType = type.getDeclaringType();

        if (!checkInner ||
            declaringType == null ||
            declaringType == Type.Bottom ||
            declaringType == Type.NullType) {

            return isAccessible;
        }

        return isAccessible && isAccessible(site, type.getDeclaringType(), checkInner);
    }

    private boolean isInnerSubtype(Type<?> c, final Type<?> base) {
        while (c != null && !Helper.isSubtype(c, base)) {
            c = c.getDeclaringType();
        }
        return c != null;
    }

    public boolean isAccessible(final Type<?> site, final MemberInfo member) {
        return isAccessible(site, member, false);
    }

    public boolean isAccessible(final Type<?> site, final MemberInfo member, final boolean checkInner) {
        if (member == null)
            return false;

        if (member instanceof ConstructorInfo && member.getDeclaringType() != site)
            return false;

        if (member.isPrivate()) {
            return TypeUtils.areEquivalent(outermostType(site), outermostType(member.getDeclaringType())) &&
                   Helper.isInheritedIn(site, member);
        }

        if (member.isPublic()) {
            return isAccessible(site, )
        }
    }

    private MethodBase rawInstantiate(
        final MethodBase callSite,
        final MethodBase candidate,
        final TypeList argumentTypes,
        final TypeList typeArguments,
        final boolean allowBoxing,
        final boolean useVarArgs) throws InapplicableMethodException{

        if (useVarArgs && candidate.getCallingConvention() == CallingConvention.VarArgs)
            throw inapplicableMethodException;

*/
/*
        MethodBase boundMethod = (MethodBase)Helper.asMemberOf(callSite.getDeclaringType(), candidate);

        if ()
*//*

        if (candidate instanceof MethodInfo){
            final MethodInfo method = (MethodInfo)candidate;
            if (method.isGenericMethod()) {
                return method.getGenericMethodDefinition().makeGenericMethod(typeArguments);
            }
        }

        return candidate;
    }

    private final static InapplicableMethodException inapplicableMethodException = new InapplicableMethodException();

    private final static ThreadLocal<ResolutionContext> currentResolutionContext = new ThreadLocal<ResolutionContext>() {
        @Override
        protected ResolutionContext initialValue() {
            return new ResolutionContext();
        }
    };

    private final static MethodResolutionPhase[] methodResolutionSteps = {
        MethodResolutionPhase.BASIC,
        MethodResolutionPhase.BOX,
        MethodResolutionPhase.VARARITY
    };

    private final static class InapplicableMethodException extends Exception {}

    @SuppressWarnings("PackageVisibleField")
    private final static class ResolutionContext {
        private final ArrayList<Candidate> _candidates = new ArrayList<>();
        private Map<MethodResolutionPhase, MethodBase> _resolutionCache = new EnumMap<>(MethodResolutionPhase.class);
        private MethodResolutionPhase _step = null;
        private boolean _internalResolution = false;

        private MethodResolutionPhase firstErroneousResolutionPhase() {
            final MethodResolutionPhase[] steps = methodResolutionSteps;

            MethodResolutionPhase bestSoFar = MethodResolutionPhase.BASIC;
            MethodBase method = null;

            for (int i = 0, n = steps.length; i < n && steps[i].isApplicable(true, true); i++) {
                final MethodResolutionPhase step = steps[i];
                method = _resolutionCache.get(step);
                bestSoFar = step;
            }

            return bestSoFar;
        }

        void addInapplicableCandidate(final MethodBase method) {
            final ResolutionContext context = currentResolutionContext.get();
            final Candidate c = new Candidate(context._step, method, null);

            if (!_candidates.contains(c)) {
                _candidates.add(c);
            }
        }

        void addApplicableCandidate(final MethodBase method, final MethodBase boundMethod) {
            final ResolutionContext context = currentResolutionContext.get();
            final Candidate c = new Candidate(context._step, method, boundMethod);

            _candidates.add(c);
        }

        final static class Candidate {
            final MethodResolutionPhase step;
            final MethodBase method;
            final MethodBase boundMethod;

            Candidate(final MethodResolutionPhase step, final MethodBase method, final MethodBase boundMethod) {
                this.step = step;
                this.method = method;
                this.boundMethod = boundMethod;
            }

            @Override
            public boolean equals(final Object o) {
                if (o instanceof Candidate) {
                    final MethodBase m1 = this.method;
                    final MethodBase m2 = ((Candidate)o).method;
                    if ((m1 != m2 && (Helper.overrides(m1, m2, false) || Helper.overrides(m2, m1, false))) ||
                        ((m1 instanceof ConstructorInfo || m2 instanceof ConstructorInfo) &&
                         m1.getDeclaringType() != m2.getDeclaringType())) {

                        return true;
                    }
                }
                return false;
            }

            boolean isApplicable() {
                return boundMethod != null;
            }
        }
    }

    @SuppressWarnings("PackageVisibleField")
    private enum MethodResolutionPhase {
        BASIC(false, false),
        BOX(true, false),
        VARARITY(true, true);

        boolean isBoxingRequired;
        boolean isVarargsRequired;

        MethodResolutionPhase(final boolean isBoxingRequired, final boolean isVarargsRequired) {
            this.isBoxingRequired = isBoxingRequired;
            this.isVarargsRequired = isVarargsRequired;
        }

        public boolean isBoxingRequired() {
            return isBoxingRequired;
        }

        public boolean isVarargsRequired() {
            return isVarargsRequired;
        }

        public boolean isApplicable(final boolean boxingEnabled, final boolean varargsEnabled) {
            return (varargsEnabled || !isVarargsRequired) &&
                   (boxingEnabled || !isBoxingRequired);
        }
    }
*/
}