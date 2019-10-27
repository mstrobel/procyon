package com.strobel.decompiler;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.*;
import com.strobel.decompiler.ast.typeinference.ConstraintSolver;
import com.strobel.decompiler.ast.typeinference.EquivalenceSet;

public class InfererTest {

    static {
        new MetadataSystem(new InputTypeLoader());
    }

    private static MetadataParser parser = new MetadataParser(MetadataSystem.instance());
    private static TypeReference t = new GenericParameter("T");
    private static TypeReference u = new GenericParameter("U");
    private static TypeReference v = new GenericParameter("V");
    private static TypeReference w = new GenericParameter("W");
    private static TypeReference x = new GenericParameter("X");
    private static TypeReference y = new GenericParameter("Y");
    private static TypeDefinition string = parser.parseTypeDescriptor("java/lang/String").resolve();
    private static TypeDefinition charSequence = parser.parseTypeDescriptor("java/lang/CharSequence").resolve();
    private static TypeDefinition integer = parser.parseTypeDescriptor("java/lang/Integer").resolve();
    private static TypeDefinition object = parser.parseTypeDescriptor("java/lang/Object").resolve();
    private static TypeDefinition list = parser.parseTypeDescriptor("java/util/List").resolve();
    private static TypeDefinition pair = parser.parseTypeDescriptor("com/strobel/core/Pair").resolve();

    public static void main(String... args) {
        testChain();
    }

    private static void testSolveOrder() {
        ConstraintSolver constraints = new ConstraintSolver();
        constraints.addExtends(list.makeGenericType(t), u);
        constraints.addExtends(u, v);
        constraints.addExtends(v, w);
        constraints.addExtends(w, list.makeGenericType(string));

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }

        System.out.println();
    }

    private static void testSolveOrder2() { // Also tests partial solving
        ConstraintSolver constraints = new ConstraintSolver();
        constraints.addExtends(t, list.makeGenericType(u));
        constraints.addExtends(t, list.makeGenericType(string));

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }

        System.out.println();
    }

    private static void testSolveOrder3() {
        ConstraintSolver constraints = new ConstraintSolver();
        constraints.addExtends(u, charSequence);
        constraints.addExtends(t, list.makeGenericType(string));
        constraints.addExtends(t, list.makeGenericType(u));

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }

        System.out.println();
    }

    private static void testUndeterminedCycle() {
        ConstraintSolver constraints = new ConstraintSolver();

        constraints.addExtends(v, w);
        constraints.addExtends(u, v);
        constraints.addExtends(w, t);
        constraints.addExtends(t, u);

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }

        System.out.println();
    }

    private static void testCasts() {
        ConstraintSolver constraints = new ConstraintSolver();

        constraints.addExtends(integer, t);
        constraints.addExtends(t, integer);
        constraints.addExtends(t, string);
        constraints.addExtends(string, t);

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }

        System.out.println();
    }

    private static void testCasts2() {
        ConstraintSolver constraints = new ConstraintSolver();

        // Integer <= T <= U <= String
        // Possible outcomes:
        //  1. T = Integer, U = Serializable
        //  2. T = Serializable, U = String
        // (depends on order)
        constraints.addExtends(integer, t);
        constraints.addExtends(t, u);
        constraints.addExtends(u, string);

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }

        System.out.println();
    }

    private static void testCasts3() {
        ConstraintSolver constraints = new ConstraintSolver();

        TypeReference listT = list.makeGenericType(t);
        constraints.addExtends(listT, list.makeGenericType(string));
        constraints.addExtends(listT, list.makeGenericType(integer));

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }

        // Result is technically correct... Why wouldn't:
        //
        // List<Serializable> a = new ArrayList<>();
        // List<Integer> b = (List<Integer>) a;
        // List<String> c = (List<String>) a;
        //
        // be legal if replacing 'Serializable' with '?' is?

        System.out.println();
    }

    private static void testPair() {
        ConstraintSolver constraints = new ConstraintSolver();

        TypeReference pairTU = pair.makeGenericType(t, u);
        constraints.addExtends(pairTU, x);
        constraints.addExtends(x, pair.makeGenericType(string, v));
        constraints.addExtends(x, pair.makeGenericType(w, integer));

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }
        System.out.println();
    }

    private static void testChain() {
        ConstraintSolver constraints = new ConstraintSolver();

        constraints.addExtends(w, string);
        constraints.addExtends(t, u);
        constraints.addExtends(u, v);
        constraints.addExtends(string, t);
        constraints.addExtends(v, w);

        for (EquivalenceSet s : constraints.solve()) {
            System.out.println(s);
        }
        System.out.println();
    }
}
