package com.strobel.decompiler.ast.typeinference;

import com.strobel.assembler.metadata.GenericParameter;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.TypeReference;

import java.util.*;

@SuppressWarnings("UnusedReturnValue")
public class ConstraintSolver {
    private final Map<Object, EquivalenceSet> equivalences = new LinkedHashMap<>();
    private final Map<TypeReference, TypeReference> genericSubstitutions = new HashMap<>();
    private final Set<TypeReference> types = new HashSet<>();

    public boolean addExtends(Object e1, Object e2) { // TODO: allow e1 and e2 to be constraint sets
        e1 = processObject(e1);
        e2 = processObject(e2);
        if (e1.equals(e2)) return false;
        print("type(" + e1 + ") <= type(" + e2 + ")");

        final EquivalenceSet s1 = getEquivalenceSet(e1);
        final EquivalenceSet s2 = getEquivalenceSet(e2);

        return s1.supertypes.add(s2) | s2.subtypes.add(s1);
    }

    public boolean addEquality(Object e1, Object e2) { // TODO: allow e1 and e2 to be constraint sets
        e1 = processObject(e1);
        e2 = processObject(e2);
        if (e1.equals(e2)) return false;
        print("type(" + e1 + ") = type(" + e2 + ")");

        // Handle generic parameter equality (substitute its use everywhere)
        if (e1 instanceof TypeReference && e2 instanceof TypeReference) {
            if (e1 instanceof GenericParameter && ((GenericParameter) e1).getOwner() == null) {
                return addGenericSubstitution((GenericParameter) e1, (TypeReference) e2);
            } else if (e2 instanceof GenericParameter && ((GenericParameter) e2).getOwner() == null) {
                return addGenericSubstitution((GenericParameter) e2, (TypeReference) e1);
            }
        }

        // Handle normal equality
        final EquivalenceSet s1 = equivalences.get(e1);
        final EquivalenceSet s2 = equivalences.get(e2);

        if (s1 == null && s2 == null) {
            final EquivalenceSet info = new EquivalenceSet();
            info.add(e1);
            info.add(e2);
            equivalences.put(e1, info);
            equivalences.put(e2, info);
        } else if (s1 == s2) {
            return false;
        } else if (s2 == null) {
            s1.add(e2);
            equivalences.put(e2, s1);
        } else if (s1 == null) {
            s2.add(e1);
            equivalences.put(e1, s2);
        } else {
            mergeSets(s1, s2);
        }

        return true;
    }

    private EquivalenceSet mergeSets(final EquivalenceSet into, final EquivalenceSet from) {
        for (final Object o : from.getObjects()) {
            into.add(o);
            equivalences.put(o, into);
        }

        // Merge supertypes and subtypes
        into.supertypes.addAll(from.supertypes);
        into.supertypes.remove(from);
        into.subtypes.addAll(from.subtypes);
        into.subtypes.remove(from);

        // Prevent sets extending themselves
        into.supertypes.remove(into);
        into.subtypes.remove(into);

        for (final EquivalenceSet set : getEquivalenceSets()) {
            if (set.supertypes.remove(from)) {
                set.supertypes.add(into);
            }
            if (set.subtypes.remove(from)) {
                set.subtypes.add(into);
            }
        }

        return into;
    }

    private boolean addGenericSubstitution(final GenericParameter from, final TypeReference to) {
        if (from == to) {
            throw new IllegalArgumentException("Can't replace generic parameter with itself");
        }

        final TypeReference oldValue = genericSubstitutions.put(from, to);
        if (oldValue == to) {
            return false;
        } else if (oldValue != null) {
            throw new IllegalStateException("Tried to replace existing generic substitution");
        }

        final Map<TypeReference, TypeReference> mappedTypes = new HashMap<>();
        for (EquivalenceSet equivalenceSet : getEquivalenceSets()) { // TODO: Optimize by keeping a 'GenericParameter -> referring types' map updated from processObject
            final Set<TypeReference> newTypes = new LinkedHashSet<>();
            EquivalenceSet newSet = null;
            boolean changed = false;
            for (final TypeReference type : equivalenceSet.types) {
                TypeReference newType = mappedTypes.get(type);
                if (newType == null) {
                    mappedTypes.put(type, newType = MetadataHelper.substituteGenericArguments(type, genericSubstitutions));
                }
                newTypes.add(newType);

                if (!newType.equals(type)) {
                    // Old type no longer exists
                    equivalences.remove(type);
                    changed = true;
                }

                // Substituting the type may cause the set to now be equal to another set
                if (newSet == null || newSet == equivalenceSet) {
                    newSet = equivalences.get(newType);
                }
            }
            if (!changed) {
                continue;
            }
            equivalenceSet.types = newTypes;

            // Combine sets if generic substitution resulted in an equality
            if (newSet != null && newSet != equivalenceSet) {
                equivalenceSet = mergeSets(newSet, equivalenceSet);
            }

            // Update object to equivalence set map. This is done inside the for
            // loop such that equivalences.get(newType) will correctly return this
            // set if a generic substitution causes two sets to merge into a new set
            // (ex. Map<String, T> and Map<T, String> merge when T -> String).
            for (final Object o : equivalenceSet.getObjects()) {
                equivalences.put(o, equivalenceSet);
            }
        }

        return true;
    }

    private EquivalenceSet getEquivalenceSet(final Object e) {
        EquivalenceSet set = equivalences.get(e);

        if (set != null) {
            return set;
        }

        set = new EquivalenceSet();
        set.add(e);
        equivalences.put(e, set);
        return set;
    }

    public boolean add(final Object e) {
        if (equivalences.get(e) == null) {
            getEquivalenceSet(e);
            return true;
        } else {
            return false;
        }
    }

    private Object processObject(final Object o) {
        if (o instanceof TypeReference) {
            TypeReference type = (TypeReference) o;
            type = MetadataHelper.substituteGenericArguments(type, genericSubstitutions);

            if (types.add(type)) {
                if (type.hasExtendsBound()) {
                    addExtends(type, type.getExtendsBound());
                }

                if (type.hasSuperBound()) {
                    addExtends(type.getSuperBound(), type);
                }

                // TODO: composite types
            }

            return type;
        }

        return o;
    }

    public Set<EquivalenceSet> solve() {
        boolean needsPass = true;
        while (needsPass) {
            findAllConstraints();
            solveAllUnique();
            needsPass = solveAllSingleTypeVariables();
        }

        return getEquivalenceSets();
    }

    private Set<EquivalenceSet> getEquivalenceSets() {
        return new LinkedHashSet<>(equivalences.values());
    }

    private boolean findAllConstraints() {
        boolean changed = false;
        boolean changedThisPass;
        do {
            print("***Finding constraints***");
            changedThisPass = false;
            for (final EquivalenceSet type : getEquivalenceSets()) {
                try {
                    changedThisPass |= type.findConstraints(this);
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
            }
            changed |= changedThisPass;
        } while (changedThisPass);
        return changed;
    }

    private boolean solveAllUnique() {
        boolean changed = false;
        boolean changedThisPass;
        do {
            print("***Solving unique***");
            changedThisPass = false;
            for (final EquivalenceSet type : getEquivalenceSets()) {
                try {
                    changedThisPass |= type.solveUnique(this);
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
            }
            changed |= changedThisPass;
        } while (changedThisPass);
        return changed;
    }

    private boolean solveAllSingleTypeVariables() {
        boolean changed = false;
        boolean changedThisPass;
        do {
            print("***Solving multiple***");
            changedThisPass = false;
            for (final EquivalenceSet type : getEquivalenceSets()) {
                try {
                    changedThisPass |= type.solveMultiple(this);
                } catch (final Throwable t) {
                    t.printStackTrace();
                }
            }
            changed |= changedThisPass;

            if (changedThisPass) {
                findAllConstraints();
                solveAllUnique();
            }
        } while (changedThisPass);
        return changed;
    }

    private static void print(final String s) {
        if (InferenceSettings.PRINT) {
            System.out.println(s);
        }
    }
}
