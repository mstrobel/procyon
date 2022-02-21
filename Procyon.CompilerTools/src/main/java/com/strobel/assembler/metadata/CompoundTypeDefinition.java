package com.strobel.assembler.metadata;

import com.strobel.annotations.Nullable;
import com.strobel.core.VerifyArgument;

import java.util.List;

import static com.strobel.assembler.metadata.CompoundTypeReference.append0;

@SuppressWarnings("DuplicatedCode")
public final class CompoundTypeDefinition extends TypeDefinition implements ICompoundType {
    private @Nullable final TypeReference _underlyingType;
    private final TypeReference _baseType;
    private final List<TypeReference> _interfaces;

    CompoundTypeDefinition(final TypeReference baseType, final List<TypeReference> interfaces, final IMetadataResolver resolver) {
        super(VerifyArgument.notNull(resolver, "resolver"));

        VerifyArgument.noNullElementsAndNotEmpty(interfaces, "interfaces");

        setBaseType(baseType);
        getExplicitInterfacesInternal().addAll(interfaces);

        _baseType = baseType;
        _interfaces = interfaces;
        _underlyingType = baseType != null ? baseType : interfaces.get(0);
    }

    public final List<TypeReference> getInterfaces() {
        return _interfaces;
    }

    @Override
    public boolean isCompoundType() {
        return true;
    }

    @Override
    public String getSimpleName() {
        return _underlyingType.getSimpleName();
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean containsGenericParameters() {
        final TypeReference baseType = getBaseType();

        if (baseType != null && baseType.containsGenericParameters()) {
            return true;
        }

        for (final TypeReference t : _interfaces) {
            if (t.containsGenericParameters()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return _underlyingType.getName();
    }

    @Override
    public String getFullName() {
        return _underlyingType.getFullName();
    }

    @Override
    public String getInternalName() {
        return _underlyingType.getInternalName();
    }

    @Override
    public TypeReference getUnderlyingType() {
        return _underlyingType;
    }

    @Override
    public final <R, P> R accept(final TypeMetadataVisitor<P, R> visitor, final P parameter) {
        return visitor.visitCompoundType(this, parameter);
    }

    @Override
    public StringBuilder appendBriefDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_BRIEF_DESCRIPTION);
    }

    @Override
    public StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_SIMPLE_DESCRIPTION);
    }

    @Override
    public StringBuilder appendErasedDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_ERASED_DESCRIPTION);
    }

    @Override
    public StringBuilder appendDescription(final StringBuilder sb) {
        return append0(this, sb, " & ", TypeFunctions.APPEND_DESCRIPTION);
    }

    @Override
    public StringBuilder appendSignature(final StringBuilder sb) {
        final int position = sb.length();
        final StringBuilder s = append0(this, sb, ":", TypeFunctions.APPEND_SIGNATURE);

        if (_baseType == null) {
            s.insert(position, ':');
        }

        return s;
    }

    @Override
    public StringBuilder appendErasedSignature(final StringBuilder sb) {
        return _underlyingType.appendErasedSignature(sb);
    }

    @Override
    protected StringBuilder appendErasedClassSignature(final StringBuilder sb) {
        return _underlyingType.appendErasedClassSignature(sb);
    }
}
