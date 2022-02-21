package com.strobel.assembler.metadata;

import com.strobel.assembler.Collection;
import com.strobel.core.VerifyArgument;

class RecordTypeDefinition extends TypeDefinition {
    final static TypeDefinition INSTANCE = new RecordTypeDefinition();

    private RecordTypeDefinition() {
        setBaseType(BuiltinTypes.Object);
        setCompilerVersion(CompilerTarget.JDK15.majorVersion, CompilerTarget.JDK15.minorVersion);
        setFlags((Flags.AccessFlags | Flags.ClassFlags) & (Flags.PUBLIC | Flags.SUPER | Flags.ABSTRACT));
        setName("Record");
        setPackageName("java.lang");
        setSimpleName("Record");

        final Collection<MethodDefinition> methodDefinitions = getDeclaredMethodsInternal();

        for (final MethodDefinition baseMethod : BuiltinTypes.Object.getDeclaredMethods()) {
            methodDefinitions.add(new RecordMethod(this, baseMethod));
        }

        setResolver(MetadataSystem.instance());
    }

    private final static class RecordMethod extends MethodDefinition {
        RecordMethod(final RecordTypeDefinition declaringType, final MethodDefinition baseMethod) {
            VerifyArgument.notNull(declaringType, "declaringType");
            VerifyArgument.notNull(baseMethod, "baseMethod");

            setName(baseMethod.getName());
            setReturnType(baseMethod.getReturnType());
            setDeclaringType(declaringType);
            setFlags((Flags.AccessFlags | Flags.MethodFlags) & (baseMethod.getFlags() | Flags.ABSTRACT));

            if (baseMethod.isConstructor()) {
                setFlags((getFlags() & (~Flags.PUBLIC)) | Flags.PROTECTED);
            }

            final ParameterDefinitionCollection ps = getParametersInternal();

            for (final ParameterDefinition bp : baseMethod.getParameters()) {
                ps.add(new ParameterDefinition(bp.getSlot(), bp.getName(), bp.getParameterType()));
            }
        }
    }
}
