package com.strobel.decompiler.ast.typeinference;

import com.strobel.assembler.metadata.*;
import com.strobel.util.ContractUtils;

import java.util.List;

public final class GenericConstraintFinder extends DefaultTypeVisitor<TypeReference, Boolean> {
    public enum Mode { // relationship between t and u
        EQUALS,
        EXTENDS,
        SUPER
    }

    private final ConstraintSolver constraints;
    private final Mode mode;

    public GenericConstraintFinder(ConstraintSolver constraints, Mode mode) {
        this.constraints = constraints;
        this.mode = mode;
    }

    @Override
    public Boolean visit(TypeReference t, TypeReference u) {
        return t.accept(this, u);
    }

    @Override
    public Boolean visitArrayType(ArrayType t, TypeReference u) {
        if (mode == Mode.EXTENDS && u.equals(CommonTypeReferences.Object)) { // Arrays extend object
            return false;
        }

        if (u.isGenericParameter()) {
            return false;
        }

        boolean changed = false;

        if (mode == Mode.EQUALS) {
            changed |= constraints.addEquality(t.getElementType(), u.getElementType());
        } else if (mode == Mode.EXTENDS) {
            changed |= constraints.addExtends(t.getElementType(), u.getElementType());
        } else if (mode == Mode.SUPER) {
            changed |= constraints.addExtends(u.getElementType(), t.getElementType());
        }

        return changed;
    }

    @Override
    public Boolean visitGenericParameter(GenericParameter t, TypeReference u) {
        if (mode == Mode.EQUALS && !t.equals(u)) {
            return constraints.addEquality(t, u);
        }
        return false;
    }

    @Override
    public Boolean visitWildcard(WildcardType t, TypeReference u) {
        if (mode == Mode.EQUALS) {
            if (!(u instanceof WildcardType)) {
                //throw new IllegalStateException("Wildcard type " + t + " can't equal non-wildcard type " + u);
                return false; // Casts break everything
            }

            if (t.hasExtendsBound() && u.hasExtendsBound()) {
                return constraints.addEquality(t.getExtendsBound(), u.getExtendsBound());
            } else if (t.hasSuperBound() && u.hasSuperBound()) {
                return constraints.addEquality(t.getSuperBound(), u.getSuperBound());
            } else {
                throw new IllegalStateException("Wildcards " + t + " and " + u + " can't be equal");
            }
        } else if (mode == Mode.EXTENDS) {
            if (t.hasSuperBound()) {
                return constraints.addExtends(t.getSuperBound(), u);
            }
        } else if (mode == Mode.SUPER) {
            if (t.hasExtendsBound()) {
                return constraints.addExtends(u, t.getExtendsBound());
            }
        }

        return false;
    }

    @Override
    public Boolean visitCompoundType(CompoundTypeReference t, TypeReference superType) {
        return false; // TODO
    }

    @Override
    public Boolean visitAndType(AndType t, TypeReference superType) {
//        boolean changed = false;
//        for (TypeReference u : t.getTypes()) {
//            changed |= visit(u, superType);
//        }
//        return changed;
        return false; // nothing to infer
    }

//    @Override
//    public Boolean visitParameterizedType(TypeReference t, TypeReference u) {
//        if (!(u instanceof IGenericInstance || u instanceof TypeDefinition)) {
//            return false;
////            throw new RuntimeException("Raw types not implemented, yet found relation between raw and parameterized type");
//        }
//
//        if (!u.hasGenericParameters()) {
//            return false;
//        }
//
//        final List<? extends TypeReference> tArgs = ((IGenericInstance) t).getTypeArguments();
//        final List<? extends TypeReference> uArgs = u instanceof IGenericInstance ? ((IGenericInstance) u).getTypeArguments() : u.getGenericParameters();
//
//        boolean changed = false;
//        for (int i = 0; i < tArgs.size(); i++) {
//            TypeReference tArg = tArgs.get(i);
//            TypeReference uArg = uArgs.get(i);
//            if (mode == Mode.EQUALS) {
//                if (!tArg.equals(uArg)) {
//                    changed |= constraints.addEquality(tArg, uArg);
//                }
//            } else if (mode == Mode.EXTENDS) {
//                changed |= reduceCompatibilityConstraint(tArg, uArg);
//            } else if (mode == Mode.SUPER) {
//                changed |= reduceCompatibilityConstraint(uArg, tArg);
//            }
//        }
//
//        return changed;
//    }

    @Override
    public Boolean visitClassType(TypeReference t, TypeReference u) {
        if (u instanceof AndType) {
            return false; // TODO: really nothing to do?
        }

        if (t instanceof GenericParameter || u instanceof GenericParameter ||
            t.equals(BuiltinTypes.Object) || u.equals(BuiltinTypes.Object) ||
            t instanceof WildcardType || u instanceof WildcardType ||
            MetadataHelper.isSubType(t, u)) {
            return false;
        }

        if (mode == Mode.EQUALS) {
            boolean changed = false;
            final List<? extends TypeReference> tArgs = t instanceof IGenericInstance ? ((IGenericInstance) t).getTypeArguments() : t.getGenericParameters();
            final List<? extends TypeReference> uArgs = u instanceof IGenericInstance ? ((IGenericInstance) u).getTypeArguments() : u.getGenericParameters();
            for (int i = 0; i < tArgs.size(); i++) {
                TypeReference tArg = tArgs.get(i);
                TypeReference uArg = uArgs.get(i);
                changed |= constraints.addEquality(tArg, uArg);
            }
            return changed;
        } else if (mode == Mode.EXTENDS) {
            if (t.resolve().equals(u.resolve())) {
                boolean changed = false;
                final List<? extends TypeReference> tArgs = t instanceof IGenericInstance ? ((IGenericInstance) t).getTypeArguments() : t.getGenericParameters();
                final List<? extends TypeReference> uArgs = u instanceof IGenericInstance ? ((IGenericInstance) u).getTypeArguments() : u.getGenericParameters();
                for (int i = 0; i < tArgs.size(); i++) {
                    TypeReference tArg = tArgs.get(i);
                    TypeReference uArg = uArgs.get(i);
                    changed |= reduceCompatibilityConstraint(tArg, uArg);
                }
                return changed;
            } else {
                boolean found = false;
                boolean changed = false;

                TypeReference tSuper = MetadataHelper.getSuperType(t);
                if (tSuper != null && MetadataHelper.isSubType(tSuper.resolve(), u.resolve())) {
                    changed |= visit(tSuper, u);
                    found = true;
                }

                for (TypeReference tInterface : MetadataHelper.getInterfaces(t)) {
                    if (MetadataHelper.isSubType(tInterface.resolve(), u.resolve())) {
                        changed |= visit(tInterface, u);
                        found = true;
                    }
                }

                if (!found) {
                    if (!InferenceSettings.C_CASTS_ENABLED) {
                        throw new IllegalStateException("Illegal extends: " + t + " <= " + u);
                    }

                    print("Cast required: " + t + " <= " + u);
                    return false;
                }
                return changed;
            }
        } else if (mode == Mode.SUPER) { // TODO: is this necessary? we'll eventually get to it anyway...
            return new GenericConstraintFinder(constraints, Mode.EXTENDS).visit(u, t);
        } else {
            throw ContractUtils.unreachable();
        }
    }

    private boolean reduceCompatibilityConstraint(TypeReference t, TypeReference u) {
        // https://docs.oracle.com/javase/specs/jls/se8/html/jls-18.html#jls-18.2.2
        if (t instanceof WildcardType && u instanceof WildcardType) {
            if (t.hasExtendsBound() && u.hasExtendsBound()) {
                return constraints.addExtends(t.getExtendsBound(), u.getExtendsBound());
            } else if (t.hasExtendsBound() && u.isUnbounded()) { // Always true
                return false;
            } else if (t.hasSuperBound() && u.hasSuperBound()) {
                return constraints.addExtends(u.getSuperBound(), t.getSuperBound());
            } else if (t.hasSuperBound() && u.isUnbounded()) { // Always true
                return false;
            } else if (t.isUnbounded() && u.isUnbounded()) { // Always true
                return false;
            } else {
                throw new IllegalStateException("Type " + t + " cannot be compatible with " + u);
            }
        } else if (u instanceof WildcardType) {
            if (u.hasExtendsBound()) {
                return constraints.addExtends(t, u.getExtendsBound());
            } else if (u.hasSuperBound()) {
                return constraints.addExtends(u.getSuperBound(), t);
            } else { // Always true
                return false;
            }
        } else {
            return addSoftEquality(t, u);
        }
    }

    private boolean addSoftEquality(TypeReference t, TypeReference u) {
        if (!InferenceSettings.C_CASTS_ENABLED) {
            return constraints.addEquality(t, u);
        }

        print("type(" + t + ") ~ type(" + u + ")");
        return constraints.addExtends(t, u) | constraints.addExtends(u, t);
    }

    @Override
    public Boolean visitPrimitiveType(PrimitiveType t, TypeReference superType) {
        return false;
    }

    @Override
    public Boolean visitNullType(final TypeReference t, TypeReference superType) {
        return false;
    }

    @Override
    public Boolean visitBottomType(final TypeReference t, TypeReference superType) {
        return false;
    }

    @Override
    public Boolean visitRawType(final RawType t, TypeReference superType) {
        return false;
    }

    private static void print(String s) {
        if (InferenceSettings.PRINT) {
            System.out.println(s);
        }
    }
}
