/*
 * Flags.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.metadata;

import com.strobel.util.ContractUtils;

import javax.lang.model.element.Modifier;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("PointlessBitwiseExpression")
public class Flags {
    private Flags() {
        throw ContractUtils.unreachable();
    }

    public static String toString(final long flags) {
        final StringBuilder buf = new StringBuilder();
        String sep = "";
        for (final Flag s : asFlagSet(flags)) {
            buf.append(sep);
            buf.append(s.name());
            sep = ", ";
        }
        return buf.toString();
    }

    public static String toString(final long flags, final Kind kind) {
        final StringBuilder buf = new StringBuilder();
        String sep = "";
        for (final Flag s : asFlagSet(flags, kind)) {
            buf.append(sep);
            buf.append(s.name());
            sep = ", ";
        }
        return buf.toString();
    }

    @SuppressWarnings("ConstantConditions")
    public static EnumSet<Flag> asFlagSet(final long mask) {
        final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

        if ((mask & PUBLIC) != 0) {
            flags.add(Flag.PUBLIC);
        }
        if ((mask & PRIVATE) != 0) {
            flags.add(Flag.PRIVATE);
        }
        if ((mask & PROTECTED) != 0) {
            flags.add(Flag.PROTECTED);
        }
        if ((mask & STATIC) != 0) {
            flags.add(Flag.STATIC);
        }
        if ((mask & ABSTRACT) != 0) {
            flags.add(Flag.ABSTRACT);
        }
        if ((mask & FINAL) != 0) {
            flags.add(Flag.FINAL);
        }
        if ((mask & SEALED) != 0) {
            flags.add(Flag.SEALED);
        }
        if ((mask & NON_SEALED) != 0) {
            flags.add(Flag.NON_SEALED);
        }
        if ((mask & SYNCHRONIZED) != 0) {
            flags.add(Flag.SYNCHRONIZED);
        }
        if ((mask & VOLATILE) != 0) {
            flags.add(Flag.VOLATILE);
        }
        if ((mask & TRANSIENT) != 0) {
            flags.add(Flag.TRANSIENT);
        }
        if ((mask & NATIVE) != 0) {
            flags.add(Flag.NATIVE);
        }
        if ((mask & INTERFACE) != 0) {
            flags.add(Flag.INTERFACE);
        }
        if ((mask & DEFAULT) != 0) {
            flags.add(Flag.DEFAULT);
        }
        if ((mask & DEFAULT) != 0) {
            flags.add(Flag.DEFAULT);
        }
        if ((mask & STRICTFP) != 0) {
            flags.add(Flag.STRICTFP);
        }
        if ((mask & SUPER) != 0) {
            flags.add(Flag.SUPER);
        }
        if ((mask & BRIDGE) != 0) {
            flags.add(Flag.BRIDGE);
        }
        if ((mask & SYNTHETIC) != 0) {
            flags.add(Flag.SYNTHETIC);
        }
        if ((mask & DEPRECATED) != 0) {
            flags.add(Flag.DEPRECATED);
        }
        if ((mask & HASINIT) != 0) {
            flags.add(Flag.HASINIT);
        }
        if ((mask & ENUM) != 0) {
            flags.add(Flag.ENUM);
        }
        if ((mask & MANDATED) != 0) {
            flags.add(Flag.MANDATED);
        }
        if ((mask & IPROXY) != 0) {
            flags.add(Flag.IPROXY);
        }
        if ((mask & NOOUTERTHIS) != 0) {
            flags.add(Flag.NOOUTERTHIS);
        }
        if ((mask & EXISTS) != 0) {
            flags.add(Flag.EXISTS);
        }
        if ((mask & COMPOUND) != 0) {
            flags.add(Flag.COMPOUND);
        }
        if ((mask & CLASS_SEEN) != 0) {
            flags.add(Flag.CLASS_SEEN);
        }
        if ((mask & SOURCE_SEEN) != 0) {
            flags.add(Flag.SOURCE_SEEN);
        }
        if ((mask & LOCKED) != 0) {
            flags.add(Flag.LOCKED);
        }
        if ((mask & UNATTRIBUTED) != 0) {
            flags.add(Flag.UNATTRIBUTED);
        }
        if ((mask & ANONCONSTR) != 0) {
            flags.add(Flag.ANONCONSTR);
        }
        if ((mask & ACYCLIC) != 0) {
            flags.add(Flag.ACYCLIC);
        }
        if ((mask & PARAMETER) != 0) {
            flags.add(Flag.PARAMETER);
        }
        if ((mask & VARARGS) != 0) {
            flags.add(Flag.VARARGS);
        }

        return flags;
    }

    @SuppressWarnings("ConstantConditions")
    public static EnumSet<Flag> asFlagSet(final long mask, final Kind kind) {
        final EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);

        if (kind == Kind.Module) {
            if ((mask & ACC_OPEN) != 0) {
                flags.add(Flag.OPEN);
            }
            if ((mask & ACC_SYNTHETIC) != 0) {
                flags.add(Flag.SYNTHETIC);
            }
            if ((mask & ACC_MANDATED) != 0) {
                flags.add(Flag.MANDATED);
            }
            return flags;
        }

        if (kind == Kind.Requires) {
            if ((mask & ACC_TRANSITIVE) != 0) {
                flags.add(Flag.TRANSITIVE);
            }
            if ((mask & ACC_STATIC_PHASE) != 0) {
                flags.add(Flag.STATIC_PHASE);
            }
            if ((mask & ACC_SYNTHETIC) != 0) {
                flags.add(Flag.SYNTHETIC);
            }
            if ((mask & ACC_MANDATED) != 0) {
                flags.add(Flag.MANDATED);
            }
            return flags;
        }

        if (kind == Kind.ExportsOpens) {
            if ((mask & ACC_SYNTHETIC) != 0) {
                flags.add(Flag.SYNTHETIC);
            }
            if ((mask & ACC_MANDATED) != 0) {
                flags.add(Flag.MANDATED);
            }
            return flags;
        }

        if ((mask & PUBLIC) != 0) {
            flags.add(Flag.PUBLIC);
        }
        if ((mask & PRIVATE) != 0) {
            flags.add(Flag.PRIVATE);
        }
        if ((mask & PROTECTED) != 0) {
            flags.add(Flag.PROTECTED);
        }
        if ((mask & STATIC) != 0) {
            flags.add(Flag.STATIC);
        }
        if ((mask & FINAL) != 0) {
            flags.add(Flag.FINAL);
        }
        if ((mask & SEALED) != 0) {
            flags.add(Flag.SEALED);
        }
        if ((mask & NON_SEALED) != 0) {
            flags.add(Flag.NON_SEALED);
        }
        if ((mask & SYNCHRONIZED) != 0) {
            flags.add(kind == Kind.Class || kind == Kind.InnerClass ? Flag.SUPER : Flag.SYNCHRONIZED);
        }
        if ((mask & VOLATILE) != 0) {
            flags.add(kind == Kind.Method ? Flag.BRIDGE : Flag.VOLATILE);
        }
        if ((mask & TRANSIENT) != 0) {
            flags.add(kind == Kind.Method ? Flag.VARARGS : Flag.TRANSIENT);
        }
        if ((mask & NATIVE) != 0) {
            flags.add(Flag.NATIVE);
        }
        if ((mask & INTERFACE) != 0) {
            flags.add(Flag.INTERFACE);
        }
        if ((mask & ABSTRACT) != 0) {
            flags.add(Flag.ABSTRACT);
        }
        if ((mask & DEFAULT) != 0) {
            flags.add(Flag.DEFAULT);
        }
        if ((mask & STRICTFP) != 0) {
            flags.add(Flag.STRICTFP);
        }
        if ((mask & SUPER) != 0) {
            flags.add(Flag.SUPER);
        }
        if ((mask & BRIDGE) != 0) {
            flags.add(Flag.BRIDGE);
        }
        if ((mask & SYNTHETIC) != 0) {
            flags.add(Flag.SYNTHETIC);
        }
        if ((mask & DEPRECATED) != 0) {
            flags.add(Flag.DEPRECATED);
        }
        if ((mask & HASINIT) != 0) {
            flags.add(Flag.HASINIT);
        }
        if ((mask & ENUM) != 0) {
            flags.add(Flag.ENUM);
        }
        if ((mask & IPROXY) != 0) {
            flags.add(Flag.IPROXY);
        }
        if ((mask & NOOUTERTHIS) != 0) {
            flags.add(Flag.NOOUTERTHIS);
        }
        if ((mask & EXISTS) != 0) {
            flags.add(Flag.EXISTS);
        }
        if ((mask & COMPOUND) != 0) {
            flags.add(Flag.COMPOUND);
        }
        if ((mask & CLASS_SEEN) != 0) {
            flags.add(Flag.CLASS_SEEN);
        }
        if ((mask & SOURCE_SEEN) != 0) {
            flags.add(Flag.SOURCE_SEEN);
        }
        if ((mask & LOCKED) != 0) {
            flags.add(Flag.LOCKED);
        }
        if ((mask & UNATTRIBUTED) != 0) {
            flags.add(Flag.UNATTRIBUTED);
        }
        if ((mask & ANONCONSTR) != 0) {
            flags.add(Flag.ANONCONSTR);
        }
        if ((mask & ACYCLIC) != 0) {
            flags.add(Flag.ACYCLIC);
        }
        if ((mask & PARAMETER) != 0) {
            flags.add(Flag.PARAMETER);
        }
        if ((mask & VARARGS) != 0) {
            flags.add(Flag.VARARGS);
        }

        return flags;
    }

    public static enum Kind {
        Class,
        InnerClass,
        Field,
        Method,
        Module,
        Requires,
        ExportsOpens
    }

    /*
     * Standard Java flags.
     */
    public static final int PUBLIC       = 1 << 0;
    public static final int PRIVATE      = 1 << 1;
    public static final int PROTECTED    = 1 << 2;
    public static final int STATIC       = 1 << 3;
    public static final int FINAL        = 1 << 4;
    public static final int SYNCHRONIZED = 1 << 5;
    public static final int VOLATILE     = 1 << 6;
    public static final int TRANSIENT    = 1 << 7;
    public static final int NATIVE       = 1 << 8;
    public static final int INTERFACE    = 1 << 9;
    public static final int ABSTRACT     = 1 << 10;
    public static final int STRICTFP     = 1 << 11;

    /* Flag that marks a symbol synthetic, added in classfile v49.0. */
    public static final int SYNTHETIC = 1 << 12;

    /**
     * Flag that marks attribute interfaces, added in classfile v49.0.
     */
    public static final int ANNOTATION = 1 << 13;

    /**
     * An enumeration type or an enumeration constant, added in
     * classfile v49.0.
     */
    public static final int ENUM = 1 << 14;

    /**
     * Added in SE8, represents constructs implicitly declared in source.
     */
    public static final int MANDATED = 1 << 15;

    public static final int StandardFlags = 0x0fff;

    // Because the following access flags are overloaded with other
    // bit positions, we translate them when reading and writing class
    // files into unique bits positions: ACC_SYNTHETIC <-> SYNTHETIC,
    // for example.
    public static final int ACC_FINAL        = 0x0010;
    public static final int ACC_SUPER        = 0x0020;
    public static final int ACC_BRIDGE       = 0x0040;
    public static final int ACC_VARARGS      = 0x0080;
    public static final int ACC_MODULE       = 0x8000;

    public static final int ACC_OPEN         = 0x0020;
    public static final int ACC_SYNTHETIC    = 0x1000;
    public static final int ACC_MANDATED     = 0x8000;

    public static final int ACC_TRANSITIVE   = 0x0020;
    public static final int ACC_STATIC_PHASE = 0x0040;


    /*****************************************
     * Internal compiler flags (no bits in the lower 16).
     *****************************************/

    /**
     * Flag is set if symbol is deprecated.
     */
    public static final int DEPRECATED = 1 << 17;

    /**
     * Flag is set for a variable symbol if the variable's definition
     * has an initializer part.
     */
    public static final int HASINIT = 1 << 18;

    /**
     * Flag is set for compiler-generated anonymous method symbols
     * that `own' an initializer block.
     */
    public static final int BLOCK = 1 << 20;

    /**
     * Flag is set for compiler-generated abstract methods that implement
     * an interface method (Miranda methods).
     */
    public static final int IPROXY = 1 << 21;

    /**
     * Flag is set for nested classes that do not access instance members
     * or `this' of an outer class and therefore don't need to be passed
     * a this$n reference.  This flag is currently set only for anonymous
     * classes in superclass constructor calls and only for pre 1.4 targets.
     * todo: use this flag for optimizing away this$n parameters in
     * other cases.
     */
    public static final int NOOUTERTHIS = 1 << 22;

    /**
     * Flag is set for package symbols if a package has a member or
     * directory and therefore exists.
     */
    public static final int EXISTS = 1 << 23;

    /**
     * Flag is set for compiler-generated compound classes
     * representing multiple variable bounds
     */
    public static final int COMPOUND = 1 << 24;

    /**
     * Flag is set for class symbols if a class file was found for this class.
     */
    public static final int CLASS_SEEN = 1 << 25;

    /**
     * Flag is set for class symbols if a source file was found for this
     * class.
     */
    public static final int SOURCE_SEEN = 1 << 26;

    /* State flags (are reset during compilation).
     */

    /**
     * Flag for class symbols is set and later re-set as a lock in
     * Enter to detect cycles in the superclass/superinterface
     * relations.  Similarly for constructor call cycle detection in
     * Attr.
     */
    public static final int LOCKED = 1 << 27;

    /**
     * Flag for class symbols is set and later re-set to indicate that a class
     * has been entered but has not yet been attributed.
     */
    public static final int UNATTRIBUTED = 1 << 28;

    /**
     * Flag for synthesized default constructors of anonymous classes.
     */
    public static final int ANONCONSTR = 1 << 29;

    /**
     * Flag for class symbols to indicate it has been checked and found
     * acyclic.
     */
    public static final int ACYCLIC = 1 << 30;

    /**
     * Flag that marks bridge methods.
     */
    public static final long BRIDGE = 1L << 31;

    /**
     * Flag that marks formal parameters.
     */
    public static final long PARAMETER = 1L << 33;

    /**
     * Flag that marks varargs methods.
     */
    public static final long VARARGS = 1L << 34;

    /**
     * Flag for annotation type symbols to indicate it has been
     * checked and found acyclic.
     */
    public static final long ACYCLIC_ANN = 1L << 35;

    /**
     * Flag that marks a generated default constructor.
     */
    public static final long GENERATEDCONSTR = 1L << 36;

    /**
     * Flag that marks a hypothetical method that need not really be
     * generated in the binary, but is present in the symbol table to
     * simplify checking for erasure clashes - also used for 292 poly sig methods.
     */
    public static final long HYPOTHETICAL = 1L << 37;

    /**
     * Flag that marks an internal proprietary class.
     */
    public static final long PROPRIETARY = 1L << 38;

    /**
     * Flag that marks a a multi-catch parameter
     */
    public static final long UNION = 1L << 39;

    /**
     * Flag that marks a special kind of bridge methods (the ones that
     * come from restricted supertype bounds)
     */
    public static final long OVERRIDE_BRIDGE = 1L << 40;

    /**
     * Flag that marks an 'effectively final' local variable
     */
    public static final long EFFECTIVELY_FINAL = 1L << 41;

    /**
     * Flag that marks non-override equivalent methods with the same signature
     */
    public static final long CLASH = 1L << 42;

    /**
     * Flag that marks either a default method or an interface containing default methods.
     */
    public static final long DEFAULT = 1L<<43;

    /**
     * Flag that marks anonymous inner classes.
     */
    public static final long ANONYMOUS = 1L << 44;

    /**
     * Mirror of ACC_SUPER.
     */
    public static final long SUPER = 1L << 45;

    /**
     * Indicates whether an unsuccessful attempt has been made to load a method's body.
     */
    public static final long LOAD_BODY_FAILED = 1L << 46;

    /**
     * Indicates a class has been run through the deobfuscating preprocessor.
     */
    public static final long DEOBFUSCATED = 1L << 47;

    /**
     * Flag to indicate class symbol is for module-info
     */
    public static final long MODULE = 1L<<51;

    /**
     * Flag to indicate that a class is a record. The flag is also used to mark fields that are
     * part of the state vector of a record and to mark the canonical constructor
     */
    public static final long RECORD = 1L << 61; // ClassSymbols, MethodSymbols and VarSymbols

    /**
     * Flag to mark a record constructor as a compact one
     */
    public static final long COMPACT_RECORD_CONSTRUCTOR = 1L << 51; // MethodSymbols only

    /**
     * Flag to mark a record field that was not initialized in the compact constructor
     */
    public static final long UNINITIALIZED_FIELD = 1L << 51; // VarSymbols only

    /**
     * Flag is set for compiler-generated record members, it could be applied to
     * accessors and fields
     */
    public static final int GENERATED_MEMBER = 1 << 24; // MethodSymbols and VarSymbols

    /**
     * Flag to indicate sealed class/interface declaration.
     */
    public static final long SEALED = 1L << 62; // ClassSymbols

    /**
     * Flag to indicate that the class/interface was declared with the non-sealed modifier.
     */
    public static final long NON_SEALED = 1L << 63; // ClassSymbols

    /**
     * Modifier masks.
     */
    public static final int
        ModuleFlags                       = ACC_OPEN | ACC_SYNTHETIC | ACC_MANDATED,
        RequiresFlags                     = ACC_TRANSITIVE | ACC_STATIC_PHASE | ACC_SYNTHETIC | ACC_MANDATED,
        ExportsOpensFlags                 = ACC_SYNTHETIC | ACC_MANDATED,
        AccessFlags                       = PUBLIC | PROTECTED | PRIVATE,
        LocalClassFlags                   = FINAL | ABSTRACT | STRICTFP | ENUM | SYNTHETIC,
        StaticLocalFlags                  = LocalClassFlags | STATIC | INTERFACE,
        MemberClassFlags                  = LocalClassFlags | INTERFACE | AccessFlags,
        MemberStaticClassFlags            = MemberClassFlags | STATIC,
        ClassFlags                        = LocalClassFlags | INTERFACE | PUBLIC | ANNOTATION,
        InterfaceVarFlags                 = FINAL | STATIC | PUBLIC,
        VarFlags                          = AccessFlags | FINAL | STATIC |
                                            VOLATILE | TRANSIENT | ENUM,
        ConstructorFlags                  = AccessFlags,
        InterfaceMethodFlags              = ABSTRACT | PUBLIC,
        MethodFlags                       = AccessFlags | ABSTRACT | STATIC | NATIVE |
                                            SYNCHRONIZED | FINAL | STRICTFP,
        RecordMethodFlags                 = AccessFlags | ABSTRACT | STATIC |
                                            SYNCHRONIZED | FINAL | STRICTFP;
    public static final long
        ExtendedInterfaceFlags            = SEALED | NON_SEALED,
        ExtendedStandardFlags             = (long)StandardFlags | DEFAULT | SEALED | NON_SEALED,
        ExtendedMemberClassFlags          = (long)MemberClassFlags | SEALED | NON_SEALED,
        ExtendedMemberStaticClassFlags    = (long)MemberStaticClassFlags | SEALED | NON_SEALED,
        ExtendedClassFlags                = (long)ClassFlags | SEALED | NON_SEALED,
        ModifierFlags                     = ((long)StandardFlags & ~INTERFACE) | DEFAULT | SEALED | NON_SEALED,
        InterfaceMethodMask               = ABSTRACT | PRIVATE | STATIC | PUBLIC | STRICTFP | DEFAULT,
        AnnotationTypeElementMask         = ABSTRACT | PUBLIC,
        LocalVarFlags                     = FINAL | PARAMETER,
        ReceiverParamFlags                = PARAMETER;


    public static Set<Modifier> asModifierSet(final long flags) {
        Set<Modifier> modifiers = modifierSets.get(flags);

        if (modifiers == null) {
            modifiers = java.util.EnumSet.noneOf(Modifier.class);

            if (0 != (flags & PUBLIC)) {
                modifiers.add(Modifier.PUBLIC);
            }
            if (0 != (flags & PROTECTED)) {
                modifiers.add(Modifier.PROTECTED);
            }
            if (0 != (flags & PRIVATE)) {
                modifiers.add(Modifier.PRIVATE);
            }
            if (0 != (flags & ABSTRACT)) {
                modifiers.add(Modifier.ABSTRACT);
            }
            if (0 != (flags & STATIC)) {
                modifiers.add(Modifier.STATIC);
            }
            if (0 != (flags & FINAL)) {
                modifiers.add(Modifier.FINAL);
            }
            if (0 != (flags & TRANSIENT)) {
                modifiers.add(Modifier.TRANSIENT);
            }
            if (0 != (flags & VOLATILE)) {
                modifiers.add(Modifier.VOLATILE);
            }
            if (0 != (flags & SYNCHRONIZED)) {
                modifiers.add(Modifier.SYNCHRONIZED);
            }
            if (0 != (flags & NATIVE)) {
                modifiers.add(Modifier.NATIVE);
            }
            if (0 != (flags & STRICTFP)) {
                modifiers.add(Modifier.STRICTFP);
            }

            modifiers = Collections.unmodifiableSet(modifiers);
            modifierSets.put(flags, modifiers);
        }

        return modifiers;
    }

    public static int toModifiers(final long flags) {
        int modifiers = 0;

        if ((flags & PUBLIC) != 0) {
            modifiers |= java.lang.reflect.Modifier.PUBLIC;
        }
        if ((flags & PROTECTED) != 0) {
            modifiers |= java.lang.reflect.Modifier.PROTECTED;
        }
        if ((flags & PRIVATE) != 0) {
            modifiers |= java.lang.reflect.Modifier.PRIVATE;
        }
        if ((flags & ABSTRACT) != 0) {
            modifiers |= java.lang.reflect.Modifier.ABSTRACT;
        }
        if ((flags & STATIC) != 0) {
            modifiers |= java.lang.reflect.Modifier.STATIC;
        }
        if ((flags & FINAL) != 0) {
            modifiers |= java.lang.reflect.Modifier.FINAL;
        }
        if ((flags & TRANSIENT) != 0) {
            modifiers |= java.lang.reflect.Modifier.TRANSIENT;
        }
        if ((flags & VOLATILE) != 0) {
            modifiers |= java.lang.reflect.Modifier.VOLATILE;
        }
        if ((flags & SYNCHRONIZED) != 0) {
            modifiers |= java.lang.reflect.Modifier.SYNCHRONIZED;
        }
        if ((flags & NATIVE) != 0) {
            modifiers |= java.lang.reflect.Modifier.NATIVE;
        }
        if ((flags & STRICTFP) != 0) {
            modifiers |= java.lang.reflect.Modifier.STRICT;
        }

        return modifiers;
    }

    public static boolean testAny(final int value, final int flags) {
        return (value & flags) != 0;
    }

    public static boolean testAll(final int value, final int flags) {
        return (value & flags) == flags;
    }

    public static boolean testAny(final long value, final long flags) {
        return (value & flags) != 0;
    }

    public static boolean testAll(final long value, final long flags) {
        return (value & flags) == flags;
    }

    // Cache of modifier sets.
    private static final Map<Long, Set<Modifier>> modifierSets =
        new java.util.concurrent.ConcurrentHashMap<>(64);

    public static boolean isEnum(final TypeDefinition symbol) {
        return (symbol.getModifiers() & ENUM) != 0;
    }

    public static long fromStandardFlags(final long accessFlags, final Kind kind) {
        long flags = accessFlags;

        if (testAny(accessFlags, ACC_SUPER)) {
            flags |= (kind == Kind.Class || kind == Kind.InnerClass ? SUPER : SYNCHRONIZED);
        }

        if (testAny(accessFlags, ACC_BRIDGE)) {
            flags |= (kind == Kind.Field ? VOLATILE : BRIDGE);
        }

        if (testAny(accessFlags, ACC_VARARGS)) {
            flags |= (kind == Kind.Field ? TRANSIENT : VARARGS);
        }

        return flags;
    }

    public enum Flag {
        PUBLIC("public"),
        PROTECTED("protected"),
        PRIVATE("private"),
        ABSTRACT("abstract"),
        DEFAULT("default"),
        STATIC("static"),
        SEALED("sealed"),
        NON_SEALED("non-sealed"),
        FINAL("final"),
        TRANSIENT("transient"),
        VOLATILE("volatile"),
        SYNCHRONIZED("synchronized"),
        NATIVE("native"),
        STRICTFP("strictfp"),
        INTERFACE("interface"),
        SUPER("super"),
        BRIDGE("bridge"),
        SYNTHETIC("synthetic"),
        DEPRECATED("deprecated"),
        HASINIT("hasinit"),
        ENUM("enum"),
        MANDATED("mandated"),
        IPROXY("iproxy"),
        NOOUTERTHIS("noouterthis"),
        EXISTS("exists"),
        COMPOUND("compound"),
        CLASS_SEEN("class_seen"),
        SOURCE_SEEN("source_seen"),
        LOCKED("locked"),
        UNATTRIBUTED("unattributed"),
        ANONCONSTR("anonconstr"),
        ACYCLIC("acyclic"),
        PARAMETER("parameter"),
        VARARGS("varargs"),
        PACKAGE("package"),
        OPEN("open"),
        TRANSITIVE("transitive"),
        STATIC_PHASE("static");

        public final String name;

        Flag(final String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
