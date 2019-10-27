package com.strobel.decompiler.ast.typeinference;

public class InferenceSettings {
    public static final boolean PRINT = false;

    // Cast options
    public static final boolean C_CASTS_ENABLED = true;
    public static final boolean C_USE_AND_TYPES = false;
    public static final boolean C_BOX_CASTABLE_TYPES = false;

    // Useful information - Disable when testing inference!
    public static final boolean I_USE_CHECKCAST_TARGET = false;
    public static final boolean I_USE_DEBUG_INFO = true;

    // Constraint-finding options
    public static final boolean S_TRANSITIVITY = true; // !C_CASTS_ENABLED
    public static final boolean S_CYCLE_EQUALITY_RULE = true; // !C_CASTS_ENABLED
    public static final boolean S_PRIMITIVE_RULE = true;

    // Type variable assignment options
    public static final boolean A_LOWER_BOUND = false;
    public static final boolean A_PREFER_LOWER_BOUND = false;
    public static final boolean A_TRANSITIVITY = false;
}
