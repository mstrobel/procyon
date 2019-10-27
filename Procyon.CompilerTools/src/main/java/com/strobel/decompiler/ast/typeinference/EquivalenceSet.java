package com.strobel.decompiler.ast.typeinference;

import com.strobel.assembler.metadata.*;
import com.strobel.core.CollectionUtilities;
import com.strobel.decompiler.ast.Expression;
import com.strobel.decompiler.ast.Variable;
import com.strobel.functions.Function;
import com.strobel.functions.Functions;

import java.util.*;

public class EquivalenceSet {
    public Set<TypeReference> types = new LinkedHashSet<>();
    public Set<Expression> expressions = new LinkedHashSet<>();
    public Set<Variable> variables = new LinkedHashSet<>();

    public Set<EquivalenceSet> supertypes = new LinkedHashSet<>();
    public Set<EquivalenceSet> subtypes = new LinkedHashSet<>();

    public TypeReference solution;

    public void add(final Object object) {
        if (object instanceof TypeReference) {
            types.add((TypeReference) object);
        } else if (object instanceof Expression) {
            expressions.add((Expression) object);
        } else if (object instanceof Variable) {
            variables.add((Variable) object);
        } else {
            throw new RuntimeException("Unknown type: " + object);
        }
    }

    public Collection<Object> getObjects() {
        final List<Object> objects = new ArrayList<>(types.size() + expressions.size() + variables.size());
        objects.addAll(types);
        objects.addAll(expressions);
        objects.addAll(variables);
        return objects;
    }

    public boolean findConstraints(final ConstraintSolver constraints) {
        boolean changed = false;

        // Create copies of these to avoid ConcurrentModificationException or change before next pass
        final Set<TypeReference> types = new LinkedHashSet<>(this.types);
        final Set<EquivalenceSet> supertypes = InferenceSettings.S_TRANSITIVITY ? getAllSupertypes() : new LinkedHashSet<>(this.supertypes);
        final Set<EquivalenceSet> subtypes = InferenceSettings.S_TRANSITIVITY ? getAllSubtypes() : new LinkedHashSet<>(this.subtypes);

        // Check for equalities between subtypes and supertypes (T <= U <= T implies T == U)
        if (InferenceSettings.S_CYCLE_EQUALITY_RULE) {
            final Set<TypeReference> commonTypes = getCommonSuperSubTypes(getAllSupertypes(), getAllSubtypes());
            for (final TypeReference type : commonTypes) {
                changed |= addType(constraints, type);
            }
        }

        // Solve equality constraints
        final GenericConstraintFinder equalityVisitor = new GenericConstraintFinder(constraints, GenericConstraintFinder.Mode.EQUALS);
        for (final TypeReference t : types) {
            for (final TypeReference u : types) {
                if (t != u) {
                    changed |= equalityVisitor.visit(t, u);
                }
            }
        }

        // Solve supertype and subtype constraints
        final GenericConstraintFinder supertypeVisitor = new GenericConstraintFinder(constraints, GenericConstraintFinder.Mode.EXTENDS);
        final GenericConstraintFinder subtypeVisitor = new GenericConstraintFinder(constraints, GenericConstraintFinder.Mode.SUPER);
        for (final TypeReference t : types) {
            for (final EquivalenceSet info : supertypes) {
                for (final TypeReference u : info.types) {
                    if (t != u) {
                        changed |= supertypeVisitor.visit(t, u);
                    }
                }
            }
        }

        for (final TypeReference t : types) {
            for (final EquivalenceSet info : subtypes) {
                for (final TypeReference u : info.types) {
                    if (t != u) {
                        changed |= subtypeVisitor.visit(t, u);
                    }
                }
            }
        }

        // Type variable solving assumes everything extends Object, and it would
        // be too hard to change that in a way that works with casts.
        //
        // If 'P <= T' where 'P' is a primitive type, then 'T <= P'. Transitivity
        // is ok in this case since primitives can't be cast (using 'checkcast')
        // and can't be generic arguments either.
        if (InferenceSettings.S_PRIMITIVE_RULE) {
            outer:
            for (final TypeReference subtype : getTypes(getAllSubtypes())) {
                if (subtype.isPrimitive()) {
                    for (final TypeReference type : types) {
                        if (type.isPrimitive()) {
                            break outer;
                        }
                    }
                    changed |= constraints.addExtends(getObjects().iterator().next(), subtype);
                }
            }
        }

        return changed;
    }

    private Set<TypeReference> getCommonSuperSubTypes(final Set<EquivalenceSet> supertypes, final Set<EquivalenceSet> subtypes) {
        final Set<TypeReference> commonTypes = new LinkedHashSet<>();
        for (final EquivalenceSet supertypeSet : supertypes) {
            for (final EquivalenceSet subtypeSet : subtypes) {
                if (subtypeSet != this && supertypeSet != this) {
                    for (final TypeReference supertype : supertypeSet.types) {
                        for (final TypeReference subtype : subtypeSet.types) {
                            if (MetadataHelper.isSameType(supertype, subtype)) {
                                commonTypes.add(supertype);
                            }
                        }
                    }
                }
            }
        }
        return commonTypes;
    }

    private Set<EquivalenceSet> getAllSupertypes() {
        final LinkedHashSet<EquivalenceSet> results = new LinkedHashSet<>();
        getAllSupertypes(this, results);
        return results;
    }

    private void getAllSupertypes(final EquivalenceSet current, final Set<EquivalenceSet> results) {
        for (final EquivalenceSet supertype : current.supertypes) {
            if (results.add(supertype)) {
                getAllSupertypes(supertype, results);
            }
        }
    }

    private Set<EquivalenceSet> getAllSubtypes() {
        final LinkedHashSet<EquivalenceSet> results = new LinkedHashSet<>();
        getAllSubtypes(this, results);
        return results;
    }

    private void getAllSubtypes(final EquivalenceSet current, final Set<EquivalenceSet> results) {
        for (final EquivalenceSet subtype : current.subtypes) {
            if (results.add(subtype)) {
                getAllSubtypes(subtype, results);
            }
        }
    }

    public boolean solveUnique(final ConstraintSolver constraints) {
        boolean found = false;

        // Look for solved type in types
        if (solution == null) {
            for (final TypeReference type : types) {
                if (!isUnsolved(type)) {
                    if (found) {
//                        throw new IllegalStateException("Set has two solutions: " + solution + ", " + type);
                        return true;
                        // TODO: hashCode and equals seems to be broken for UnresolvedGenericType, TypeDefinition, ParameterizedTypes
                    }
                    print("Found solution " + type + " in equivalence set " + this);
                    setSolution(constraints, type);
                    found = true;
                }
            }
        }

        return found;
    }

    private boolean isUnsolved(final TypeReference type) {
        if (!type.containsGenericParameters()) {
            return false;
        }

        if (type instanceof GenericParameter) {
            return ((GenericParameter) type).getOwner() == null;
        }

        if (type instanceof IGenericInstance) {
            for (final TypeReference typeArgument : ((IGenericInstance) type).getTypeArguments()) {
                if (isUnsolved(typeArgument)) {
                    return true;
                }
            }
            return false;
        }

        if (type.isGenericDefinition()) {
            for (final GenericParameter typeParameter : type.getGenericParameters()) {
                if (isUnsolved(typeParameter)) {
                    return true;
                }
            }
            return false;
        }

        if (type instanceof WildcardType) {
            if (type.isUnbounded()) {
                return false;
            } else if (type.hasExtendsBound()) {
                return isUnsolved(type.getExtendsBound());
            } else if (type.hasSuperBound()) {
                return isUnsolved(type.getSuperBound());
            }
        }

        if (type instanceof ArrayType) {
            return isUnsolved(type.getElementType());
        }

        return true;
    }

    private boolean addTypeVariablesInType(final TypeReference type, final ConstraintSolver constraints) {
        final Set<TypeReference> generics = new LinkedHashSet<>();
        getTypeVariablesInType(type, generics);
        boolean modified = false;
        for (final TypeReference generic : generics) {
            modified |= constraints.add(generic);
        }
        return modified;
    }

    private void getTypeVariablesInType(final TypeReference type, final Set<? super GenericParameter> result) {
        if (!type.containsGenericParameters()) {
            return;
        }

        if (type instanceof GenericParameter) {
            if (((GenericParameter) type).getOwner() == null) {
                result.add((GenericParameter) type);
            }
        }

        if (type instanceof IGenericInstance) {
            for (final TypeReference typeArgument : ((IGenericInstance) type).getTypeArguments()) {
                getTypeVariablesInType(typeArgument, result);
            }
        }

        if (type.isGenericDefinition()) {
            for (final GenericParameter typeParameter : type.getGenericParameters()) {
                getTypeVariablesInType(typeParameter, result);
            }
        }

        if (type instanceof WildcardType) {
            if (type.hasExtendsBound()) {
                getTypeVariablesInType(type.getExtendsBound(), result);
            } else if (type.hasSuperBound()) {
                getTypeVariablesInType(type.getSuperBound(), result);
            }
        }

        if (type instanceof ArrayType) {
            getTypeVariablesInType(type.getElementType(), result);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public boolean solveMultiple(final ConstraintSolver constraints) {
        // Skip if the set already has a solution
        if (solution != null) {
            return false;
        }

        if (solveUnique(constraints)) { // TODO: should not be necessary
            return true;
        }

        boolean changed = false;

        // Check that the only types in the set are type variable. For example, if we have:
        //   - List<T> <= List<? extends Serializable>'
        //   - T <= String
        // we want to assign 'T = String' rather than 'T = ? extends Serializable'. Solving
        // type variables only will always be enough to completely solve a system and will
        // always give correct results as long as this step is not run before there are no
        // additional constraints on type variables.
        boolean canSolve = true;
        for (final TypeReference type : types) {
            if (!(type instanceof GenericParameter)) {
                if (isUnsolved(type)) {
                    // Add unsolved generics to constraint solver if missing
                    changed |= addTypeVariablesInType(type, constraints);
                    canSolve = false;
                }
            }
        }
        if (!canSolve) {
            return changed;
        }

        print("Solving single type variable: " + this);

        // Solve the type variable. If A_TRANSITIVITY is false, transitivity is not
        // used such that if 'T <= String <= U <= Object <= V', 'U' can be solved
        // before solving 'V' and 'T'. However, this may cause problems with casts?
        final Set<EquivalenceSet> subtypes = this.subtypes;
        final Set<EquivalenceSet> supertypes = this.supertypes;

        // Check if either the lower or upper bounds can be solved. Type variables in
        // constraints can't be used, so don't attempt to find common supertype until
        // all constraints on either side are known.
        //
        // Despite doing this, any system is guaranteed to be solvable if there are
        // no cycles in the graph (which are removed by the T <= U <= T implies T = U
        // rule).

        if (InferenceSettings.A_LOWER_BOUND) {
            boolean canSolveLowerBound = true;
            for (final EquivalenceSet subtypeSet : subtypes) {
                if (subtypeSet.types.isEmpty()) {
                    canSolveLowerBound = false;
                } else {
                    for (final TypeReference subtype : subtypeSet.types) {
                        if (isUnsolved(subtype)) {
                            changed |= addTypeVariablesInType(subtype, constraints);
                            canSolveLowerBound = false;
                        }
                    }
                }
            }

            if (canSolveLowerBound) {
                TypeReference leastUpperBound = null;
                for (final TypeReference subtype : getTypes(subtypes)) {
                    if (!isUnsolved(subtype)) {
                        leastUpperBound = leastUpperBound == null ? subtype : MetadataHelper.findCommonSuperType(leastUpperBound, subtype);
                    }
                }
                if (leastUpperBound != null) {
                    if (InferenceSettings.C_CASTS_ENABLED) {
                        for (final TypeReference supertype : getTypes(supertypes)) {
                            if (!isUnsolved(supertype) && !MetadataHelper.isSubType(leastUpperBound, supertype)) {
                                leastUpperBound = andType(leastUpperBound, supertype);
                                print("  Cast required, lower bound does not extend upper bound: " + this);
                                // TODO: just common supertype?
                            }
                        }
                    }

                    if (leastUpperBound instanceof WildcardType) {
                        if (leastUpperBound.hasSuperBound()) {
                            leastUpperBound = leastUpperBound.getSuperBound();
                            // Technically, least upper bound would be Bottom (skip to finding greatest
                            // lower bound of supertypes), but here it's not necessary
                        } else if (leastUpperBound.hasExtendsBound()) {
                            leastUpperBound = leastUpperBound.getExtendsBound();
                        } else if (leastUpperBound.isUnbounded()) {
                            leastUpperBound = CommonTypeReferences.Object;
                        }
                    }

                    print("  Assigned least upper bound of subtypes: " + leastUpperBound);
                    setSolution(constraints, leastUpperBound);
                    return true;
                }
                // An empty set of lower bounds can't be solved, solve greatest upper bound instead
            }
        }

        // Check if upper bound can be solved
        boolean canSolveUpperBound = true;
        for (final EquivalenceSet supertypeSet : supertypes) {
            if (supertypeSet.types.isEmpty()) {
                canSolveUpperBound = false;
            } else {
                for (final TypeReference supertype : supertypeSet.types) {
                    if (isUnsolved(supertype)) {
                        changed |= addTypeVariablesInType(supertype, constraints);
                        canSolveUpperBound = false;
                    }
                }
            }
        }

        if (canSolveUpperBound) {
            TypeReference greatestLowerBound = null;
            for (final TypeReference superType : getTypes(supertypes)) {
                if (greatestLowerBound == null || MetadataHelper.isSubType(superType, greatestLowerBound)) {
                    greatestLowerBound = superType;
                } else if (!MetadataHelper.isSubType(greatestLowerBound, superType)) {
                    // TODO: Implement 'MetadataHelper.findCommonSubType'
                    // Finding the least upper bound of the subtype constraints isn't always
                    // possible since there may be no subtype constraints. In that case, the
                    // supertype constraints must be respected by finding a common subtype for
                    // them.
                    TypeReference common = MetadataHelper.findCommonSubtype(greatestLowerBound, superType);

                    if (common == null) {
                        if (!InferenceSettings.C_CASTS_ENABLED) {
                            throw new IllegalStateException("Can't extend both " + greatestLowerBound + " and " + superType);
                        }

                        print("Cast required, " + greatestLowerBound + " and " + superType + " have no common subtype. Equivalence set: " + this);
                        common = andType(greatestLowerBound, superType);
                        // Technically, we could just keep the lower bound, but that may mean a cast through object may be needed.
                    }

                    greatestLowerBound = common;
                }
            }
            if (greatestLowerBound == null) {
                greatestLowerBound = CommonTypeReferences.Object;
            }
            if (greatestLowerBound == TypeInferer.INT_OR_BOOLEAN) {
                // We can't determine which was intended. Choosing int would be better because
                // it's less confusing if it's wrong ('if (someCondition == 1)' is better than
                // 'numberOfItems = true'), but that causes problems with some if/else transformation
                // for some reason...
                greatestLowerBound = BuiltinTypes.Boolean;
            } else if (greatestLowerBound == TypeInferer.NUMERIC) {
                // Type inference needs to be moved before any bytecode instruction set reduction
                greatestLowerBound = BuiltinTypes.Integer;
            }
            if (greatestLowerBound instanceof WildcardType) {
                if (greatestLowerBound.hasSuperBound()) {
                    // Technically, greatest lower bound would be Object, but here it's not necessary
                    greatestLowerBound = greatestLowerBound.getSuperBound();
                } else if (greatestLowerBound.hasExtendsBound()) {
                    greatestLowerBound = greatestLowerBound.getExtendsBound();
                } else if (greatestLowerBound.isUnbounded()) {
                    greatestLowerBound = CommonTypeReferences.Object;
                }
            }
            print("  Assigned greatest lower bound of supertypes: " + greatestLowerBound);
            setSolution(constraints, greatestLowerBound);
            return true;
        }

        // Check if the upper bound can be partially solved. For example, 'T <= ArrayList<U>, List<V>'
        // can be solved to 'T = ArrayList<E0>'. This is useful to make sure that something like
        // 'T <= List<U>, List<String>' can be reduced to 'T = List<E0>' and then 'T = List<String>',
        // rather than assigning 'U = Object'.
        boolean canPartiallySolveUpperBound = true;
        for (final EquivalenceSet supertypeSet : supertypes) {
            if (supertypeSet.types.isEmpty()) {
                canPartiallySolveUpperBound = false;
                break;
            }

            for (final TypeReference supertype : supertypeSet.types) {
                if (supertype instanceof GenericParameter) {
                    canPartiallySolveUpperBound = false;
                    break;
                }
            }
        }

        if (canPartiallySolveUpperBound) {
            final Set<TypeReference> newTypes = new LinkedHashSet<>();

            for (TypeReference type : getTypes(supertypes)) {
                if (type instanceof IGenericInstance) {
                    type = (TypeReference) ((IGenericInstance) type).getGenericDefinition();
                } else if (type instanceof ArrayType) {
                    int order = 0;
                    while (type instanceof ArrayType) {
                        type = type.getElementType();
                        order++;
                    }

                    if (type instanceof IGenericInstance) {
                        type = (TypeReference) ((IGenericInstance) type).getGenericDefinition();
                    } else if (type instanceof GenericParameter) {
                        type = new GenericParameter("ElementType");
                    } else {
                        throw new IllegalStateException();
                    }

                    while (order-- > 0) {
                        type = type.makeArrayType();
                    }
                }

                newTypes.add(type);
            }

            TypeReference greatestLowerBound = null;
            for (final TypeReference superType : newTypes) {
                if (greatestLowerBound == null || MetadataHelper.isSubType(superType, greatestLowerBound)) {
                    greatestLowerBound = superType;
                } else if (!MetadataHelper.isSubType(greatestLowerBound, superType)) {
                    final TypeReference common = MetadataHelper.findCommonSubtype(greatestLowerBound, superType);

                    if (common == null) {
                        throw new IllegalStateException("Can't extend both " + greatestLowerBound + " and " + superType); // TODO: casts
                    }

                    greatestLowerBound = common;
                }
            }

            final TypeReference renamedSolution = new TypeVariableRenamingVisitor().visit(greatestLowerBound);
            print("  Partially solved to " + renamedSolution);
            addType(constraints, renamedSolution);
            return true;
        }

        print("  Neither upper or lower bound can be assigned");
        return changed;
    }

    private TypeReference andType(final TypeReference a, final TypeReference b) {
        if (InferenceSettings.C_USE_AND_TYPES) {
            return new AndType(a, b);
        } else if (InferenceSettings.C_BOX_CASTABLE_TYPES) {
            return new BoxedType(MetadataHelper.findCommonSuperType(a, b));
        } else {
            return MetadataHelper.findCommonSuperType(a, b);
        }
    }

    private Set<TypeReference> getTypes(final Set<EquivalenceSet> typeSets) {
        final Set<TypeReference> supertypes = new LinkedHashSet<>();
        for (final EquivalenceSet supertypeSet : typeSets) {
            supertypes.addAll(supertypeSet.types);
        }
        return supertypes;
    }

    private void setSolution(final ConstraintSolver constraints, final TypeReference solution) {
        this.solution = solution;
        addType(constraints, solution);
    }

    private boolean addType(final ConstraintSolver constraints, final TypeReference type) {
        if (types.add(type)) {
//            print("Added type " + solution + " to " + this);
            for (final Object o : getObjects()) {
                if (o != type) {
                    constraints.addEquality(o, type);
                    return true;
                }
            }
            // TODO: what if the set is empty...? it still has to be merged with the set of the type!
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        if (solution != null) {
            final Collection<Object> objects = new ArrayList<>(getObjects());
            objects.remove(solution);
            return solution.toString() + " for " + CollectionUtilities.collectionToString(objects, Functions.objectToString());
        }

        return "[" +
               CollectionUtilities.collectionToString(subtypes, EquivalenceSet.TO_SHORT_STRING) +
               " <= " +
               toShortString() +
               " <= " +
               CollectionUtilities.collectionToString(supertypes, EquivalenceSet.TO_SHORT_STRING) +
               "]";
    }

    private String toShortString() {
        if (solution != null) {
            return solution.toString();
        }

        return "{" +
               (expressions.isEmpty() ? "" : "e = " + CollectionUtilities.collectionToString(expressions, Functions.objectToString()) + (variables.isEmpty() && types.isEmpty() ? "" : " | ")) +
               (variables.isEmpty() ? "" : "v = " + CollectionUtilities.collectionToString(variables, Functions.objectToString()) + (types.isEmpty() ? "" : " | ")) +
               (types.isEmpty() ? "" : "t = " + CollectionUtilities.collectionToString(types, Functions.objectToString())) +
               "}";
    }

    private static void print(final String s) {
        if (InferenceSettings.PRINT) {
            System.out.println(s);
        }
    }

    public final static Function<EquivalenceSet, String> TO_SHORT_STRING = new Function<EquivalenceSet, String>() {
        @Override
        public String apply(final EquivalenceSet input) {
            return input.toShortString();
        }
    };
}
