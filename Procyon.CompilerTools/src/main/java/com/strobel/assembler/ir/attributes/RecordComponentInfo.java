package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.metadata.IMetadataResolver;
import com.strobel.assembler.metadata.MetadataParser;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.VerifyArgument;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class RecordComponentInfo {
    private final String _name;
    private final String _descriptor;
    private final TypeReference _type;
    private final List<SourceAttribute> _attributes;
    private TypeReference _resolvedType;

    public RecordComponentInfo(final String name, final String descriptor, final TypeReference type, final List<SourceAttribute> attributes) {
        _name = VerifyArgument.notNull(name, "name");
        _descriptor = VerifyArgument.notNull(descriptor, "descriptor");
        _type = VerifyArgument.notNull(type, "type");
        _attributes = VerifyArgument.notNull(attributes, "attributes");
    }

    @NotNull
    public String getName() {
        return _name;
    }

    @NotNull
    public String getDescriptor() {
        return _descriptor;
    }

    @NotNull
    public TypeReference getType() {
        return _type;
    }

    @NotNull
    public TypeReference getResolvedType() {
        final TypeReference type = _resolvedType;
        return type != null ? type : _type;
    }

    @NotNull
    @SuppressWarnings("UnusedReturnValue")
    public TypeReference resolveType(final TypeReference recordType) {
        requireNonNull(recordType, "A record type is required.");

        TypeReference r = _resolvedType;

        if (r != null) {
            return r;
        }

        final SignatureAttribute signature = SourceAttribute.find(AttributeNames.Signature, _attributes);

        if (signature == null) {
            return _type;
        }

        final TypeDefinition rr = recordType.resolve();

        if (rr != null) {
            r = new MetadataParser(rr).parseTypeSignature(signature.getSignature());
        }

        _resolvedType = r;
        return r != null ? r : _type;
    }

    @NotNull
    public List<SourceAttribute> getAttributes() {
        return _attributes;
    }
}
