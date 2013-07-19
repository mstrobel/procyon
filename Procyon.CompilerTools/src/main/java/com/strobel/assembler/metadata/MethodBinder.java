package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public final class MethodBinder {
    public static class BindResult {
        public final static BindResult FAILURE = new BindResult(false, null);
        public final static BindResult AMBIGUOUS = new BindResult(true, null);

        private final boolean _ambiguous;
        private final MethodReference _method;

        private BindResult(final boolean ambiguous, final MethodReference method) {
            _ambiguous = ambiguous;
            _method = method;
        }

        public final boolean isFailure() {
            return _method == null;
        }

        public final boolean isAmbiguous() {
            return _ambiguous;
        }

        public final MethodReference getMethod() {
            return _method;
        }
    }

    public static BindResult selectMethod(final List<? extends MethodReference> matches, final List<TypeReference> types) {
        VerifyArgument.notNull(matches, "matches");
        VerifyArgument.notNull(types, "types");

        if (types.isEmpty()) {
            return null;
        }

        final int argumentCount = types.size();
        final MethodReference[] candidates = matches.toArray(new MethodReference[matches.size()]);

        for (int i = 0; i < candidates.length; i++) {
            final MethodReference candidate = candidates[i];

            if (candidate.isGenericMethod()) {
                final Map<TypeReference, TypeReference> mappings = new HashMap<>();
                final List<ParameterDefinition> parameters = candidate.getParameters();

                for (int j = 0, n = Math.min(argumentCount, parameters.size()); j < n; j++) {
                    final ParameterDefinition p = parameters.get(j);
                    final TypeReference pType = p.getParameterType();

                    if (pType.containsGenericParameters()) {
                        new AddMappingsForArgumentVisitor(types.get(j)).visit(pType, mappings);
                    }
                }

                candidates[i] = TypeSubstitutionVisitor.instance().visitMethod(candidate, mappings);
            }
        }

        //
        // Find all the methods that can be described by the parameter types.
        // Remove all of them that cannot.
        //

        int stop;
        int currentIndex = 0;

        for (int i = 0, n = candidates.length; i < n; i++) {
            final MethodReference candidate = candidates[i];
            final MethodDefinition resolved = candidate.resolve();
            final List<ParameterDefinition> parameters = candidate.getParameters();
            final int parameterCount = parameters.size();
            final boolean isVarArgs = resolved != null && resolved.isVarArgs();

            if (parameterCount != types.size() && !isVarArgs) {
                continue;
            }

            for (stop = 0; stop < Math.min(parameterCount, types.size()); stop++) {
                final TypeReference parameterType = parameters.get(stop).getParameterType();

                if (MetadataResolver.areEquivalent(parameterType, types.get(stop), false) ||
                    MetadataResolver.areEquivalent(parameterType, BuiltinTypes.Object, false)) {

                    continue;
                }

                if (MetadataHelper.isAssignableFrom(parameterType, types.get(stop))) {
                    continue;
                }

                if (!isVarArgs || stop != parameterCount - 1) {
                    break;
                }

                if (!MetadataHelper.isAssignableFrom(parameterType.getElementType(), types.get(stop))) {
                    break;
                }
            }

            if (stop == parameterCount ||
                stop == parameterCount - 1 && isVarArgs) {

                candidates[currentIndex++] = candidate;
            }
        }

        if (currentIndex == 0) {
            return BindResult.FAILURE;
        }

        if (currentIndex == 1) {
            return new BindResult(false, candidates[0]);
        }

        // Walk all of the methods looking the most specific method to invoke
        int currentMin = 0;
        boolean ambiguous = false;

        final int[] parameterOrder = new int[types.size()];

        for (int i = 0, n = types.size(); i < n; i++) {
            parameterOrder[i] = i;
        }

        for (int i = 1; i < currentIndex; i++) {
            final MethodReference m1 = candidates[currentMin];
            final MethodReference m2 = candidates[i];

            final MethodDefinition r1 = m1.resolve();
            final MethodDefinition r2 = m2.resolve();

            final TypeReference varArgType1;
            final TypeReference varArgType2;

            if (r1 != null && r1.isVarArgs()) {
                final List<ParameterDefinition> p1 = m1.getParameters();
                varArgType1 = p1.get(p1.size() - 1).getParameterType().getElementType();
            }
            else {
                varArgType1 = null;
            }

            if (r2 != null && r2.isVarArgs()) {
                final List<ParameterDefinition> p2 = m2.getParameters();
                varArgType2 = p2.get(p2.size() - 1).getParameterType().getElementType();
            }
            else {
                varArgType2 = null;
            }

            final int newMin = findMostSpecificMethod(
                m1,
                parameterOrder,
                varArgType1,
                candidates[i],
                parameterOrder,
                varArgType2,
                types,
                null
            );

            if (newMin == 0) {
                ambiguous = true;
            }
            else {
                if (newMin == 2) {
                    ambiguous = false;
                    currentMin = i;
                }
            }
        }

        if (ambiguous) {
            return new BindResult(true, candidates[currentMin]);
        }

        return new BindResult(false, candidates[currentMin]);
    }

    private static int findMostSpecificMethod(
        final MethodReference m1,
        final int[] varArgOrder1,
        final TypeReference varArgArrayType1,
        final MethodReference m2,
        final int[] varArgOrder2,
        final TypeReference varArgArrayType2,
        final List<TypeReference> types,
        final Object[] args) {

        //
        // Find the most specific method based on the parameters.
        //
        final int result = findMostSpecific(
            m1.getParameters(),
            varArgOrder1,
            varArgArrayType1,
            m2.getParameters(),
            varArgOrder2,
            varArgArrayType2,
            types,
            args
        );

        //
        // If the match was not ambiguous then return the result.
        //
        if (result != 0) {
            return result;
        }

        //
        // Check to see if the methods have the exact same name and signature.
        //
        if (compareMethodSignatureAndName(m1, m2)) {
            //
            // Determine the depth of the declaring types for both methods.
            //
            final int hierarchyDepth1 = getHierarchyDepth(m1.getDeclaringType());
            final int hierarchyDepth2 = getHierarchyDepth(m2.getDeclaringType());

            //
            // The most derived method is the most specific one.
            //
            if (hierarchyDepth1 == hierarchyDepth2) {
                return 0;
            }
            else if (hierarchyDepth1 < hierarchyDepth2) {
                return 2;
            }
            else {
                return 1;
            }
        }

        //
        // The match is ambiguous.
        //
        return 0;
    }

    private static int findMostSpecific(
        final List<ParameterDefinition> p1,
        final int[] varArgOrder1,
        final TypeReference varArgArrayType1,
        final List<ParameterDefinition> p2,
        final int[] varArgOrder2,
        final TypeReference varArgArrayType2,
        final List<TypeReference> types,
        final Object[] args) {
        //
        // A method using varargs is always less specific than one not using varargs.
        //
        if (varArgArrayType1 != null && varArgArrayType2 == null) {
            return 2;
        }
        if (varArgArrayType2 != null && varArgArrayType1 == null) {
            return 1;
        }

        //
        // Now either p1 and p2 both use params or neither does.
        //

        boolean p1Less = false;
        boolean p2Less = false;

        for (int i = 0, n = types.size(); i < n; i++) {
            if (args != null) {
                continue;
            }

            final TypeReference c1;
            final TypeReference c2;

            //
            //  If a vararg array is present, then either
            //      the user re-ordered the parameters, in which case
            //          the argument to the vararg array is either an array
            //              in which case the params is conceptually ignored and so varArgArrayType1 == null
            //          or the argument to the vararg array is a single element
            //              in which case varArgOrder[i] == p1.Length - 1 for that element 
            //      or the user did not re-order the parameters in which case 
            //          the varArgOrder array could contain indexes larger than p.Length - 1 (see VSW 577286)
            //          so any index >= p.Length - 1 is being put in the vararg array 
            //

            if (varArgArrayType1 != null && varArgOrder1[i] >= p1.size() - 1) {
                c1 = varArgArrayType1;
            }
            else {
                c1 = p1.get(varArgOrder1[i]).getParameterType();
            }

            if (varArgArrayType2 != null && varArgOrder2[i] >= p2.size() - 1) {
                c2 = varArgArrayType2;
            }
            else {
                c2 = p2.get(varArgOrder2[i]).getParameterType();
            }

            if (c1 == c2) {
                continue;
            }

            switch (findMostSpecificType(c1, c2, types.get(i))) {
                case 0:
                    return 0;
                case 1:
                    p1Less = true;
                    break;
                case 2:
                    p2Less = true;
                    break;
            }
        }

        // Two way p1Less and p2Less can be equal.  All the arguments are the
        //  same they both equal false, otherwise there were things that both 
        //  were the most specific type on....
        if (p1Less == p2Less) {
            // if we cannot tell which is a better match based on parameter types (p1Less == p2Less),
            // let's see which one has the most matches without using the params array (the longer one wins). 
            if (!p1Less && args != null) {
                if (p1.size() > p2.size()) {
                    return 1;
                }
                else if (p2.size() > p1.size()) {
                    return 2;
                }
            }

            return 0;
        }
        else {
            return p1Less ? 1 : 2;
        }
    }

    private static int findMostSpecificType(final TypeReference c1, final TypeReference c2, final TypeReference t) {
        //
        // If the two types are exact move on...
        //
        if (MetadataResolver.areEquivalent(c1, c2, false)) {
            return 0;
        }

        if (MetadataResolver.areEquivalent(c1, t, false)) {
            return 1;
        }

        if (MetadataResolver.areEquivalent(c2, t, false)) {
            return 2;
        }

        final boolean c1FromC2 = MetadataHelper.isAssignableFrom(c1, c2);
        final boolean c2FromC1 = MetadataHelper.isAssignableFrom(c2, c1);

        if (c1FromC2 == c2FromC1) {
            if (!t.isPrimitive() && c1.isPrimitive() != c2.isPrimitive()) {
                return c1.isPrimitive() ? 2 : 1;
            }
            return 0;
        }

        return c1FromC2 ? 2 : 1;
    }

    private static boolean compareMethodSignatureAndName(final MethodReference m1, final MethodReference m2) {
        final List<ParameterDefinition> p1 = m1.getParameters();
        final List<ParameterDefinition> p2 = m2.getParameters();

        if (p1.size() != p2.size()) {
            return false;
        }

        for (int i = 0, n = p1.size(); i < n; i++) {
            if (!MetadataResolver.areEquivalent(p1.get(i).getParameterType(), p2.get(i).getParameterType(), false)) {
                return false;
            }
        }

        return true;
    }

    private static int getHierarchyDepth(final TypeReference t) {
        int depth = 0;

        TypeReference currentType = t;

        do {
            depth++;

            final TypeDefinition resolved = currentType.resolve();

            currentType = resolved != null ? resolved.getBaseType() : null;
        }
        while (currentType != null);

        return depth;
    }

    private final static class AddMappingsForArgumentVisitor extends DefaultTypeVisitor<Map<TypeReference, TypeReference>, Void> {
        private TypeReference argumentType;

        AddMappingsForArgumentVisitor(final TypeReference argumentType) {
            this.argumentType = VerifyArgument.notNull(argumentType, "argumentType");
        }

        public Void visit(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            final TypeReference a = argumentType;
            t.accept(this, map);
            argumentType = a;
            return null;
        }

        @Override
        public Void visitArrayType(final ArrayType t, final Map<TypeReference, TypeReference> map) {
            final TypeReference a = argumentType;

            if (a.isArray() && t.isArray()) {
                argumentType = a.getElementType();
                visit(t.getElementType(), map);
            }

            return null;
        }

        @Override
        public Void visitGenericParameter(final GenericParameter t, final Map<TypeReference, TypeReference> map) {
            if (MetadataResolver.areEquivalent(argumentType, t)) {
                return null;
            }

            final TypeReference existingMapping = map.get(t);

            if (existingMapping == null) {
                map.put(t, argumentType);
            }
            else {
                map.put(t, MetadataHelper.findCommonSuperType(existingMapping, argumentType));
            }

            return null;
        }

        @Override
        public Void visitWildcard(final WildcardType t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitCompoundType(final CompoundTypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitParameterizedType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            final TypeReference s = MetadataHelper.asSubType(argumentType, t.getUnderlyingType());

            if (s != null && s instanceof IGenericInstance) {
                final List<TypeReference> tArgs = ((IGenericInstance) t).getTypeArguments();
                final List<TypeReference> sArgs = ((IGenericInstance) s).getTypeArguments();

                if (tArgs.size() == sArgs.size()) {
                    for (int i = 0, n = tArgs.size(); i < n; i++) {
                        argumentType = sArgs.get(i);
                        visit(tArgs.get(i), map);
                    }
                }
            }

            return null;
        }

        @Override
        public Void visitPrimitiveType(final PrimitiveType t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitClassType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitNullType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitBottomType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }

        @Override
        public Void visitRawType(final TypeReference t, final Map<TypeReference, TypeReference> map) {
            return null;
        }
    }
}
