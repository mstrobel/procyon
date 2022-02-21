package com.strobel.assembler.metadata;

import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;

public enum LanguageFeature {
    ENUM_CLASSES(CompilerTarget.JDK1_5),
    FOR_EACH_LOOPS(CompilerTarget.JDK1_5),
    TRY_WITH_RESOURCES(CompilerTarget.JDK1_7),
    DEFAULT_INTERFACE_METHODS(CompilerTarget.JDK1_8),
    STATIC_INTERFACE_METHODS(CompilerTarget.JDK1_8),
    LAMBDA_EXPRESSIONS(CompilerTarget.JDK1_8),
    TRY_EXPRESSION_RESOURCE(CompilerTarget.JDK9),
    PRIVATE_LOOKUP(CompilerTarget.JDK9),
    LOCAL_TYPE_INFERENCE(CompilerTarget.JDK10),
    SWITCH_EXPRESSIONS(CompilerTarget.JDK14, CompilerTarget.JDK13),
    TEXT_BLOCKS(CompilerTarget.JDK15, CompilerTarget.JDK13),
    SEALED_CLASSES(CompilerTarget.JDK15),
    RECORD_CLASSES(CompilerTarget.JDK15, CompilerTarget.JDK14),
    PATTERN_MATCHING(CompilerTarget.JDK15, CompilerTarget.JDK14);

    private final CompilerTarget _version;
    private final CompilerTarget _previewVersion;

    LanguageFeature(final CompilerTarget version) {
        this(version, version);
    }

    LanguageFeature(final CompilerTarget version, final CompilerTarget previewVersion) {
        _version = VerifyArgument.notNull(version, "version");
        _previewVersion = previewVersion != null ? previewVersion : version;
    }

    public final CompilerTarget version() {
        return _version;
    }

    public final CompilerTarget previewVersion() {
        return _previewVersion;
    }

    public final boolean isAvailable(final TypeDefinition definition) {
        return isAvailable(definition, false);
    }

    public final boolean isAvailable(final TypeDefinition definition, final boolean allowPreview) {
        return definition != null &&
               Comparer.compare(definition.getCompilerTarget(), allowPreview ? _previewVersion : _version) >= 0;
    }

    public final boolean isAvailable(final CompilerTarget targetVersion) {
        return isAvailable(targetVersion, false);
    }

    public final boolean isAvailable(final CompilerTarget targetVersion, final boolean allowPreview) {
        return targetVersion != null &&
               Comparer.compare(targetVersion, allowPreview ? _previewVersion : _version) >= 0;
    }
}