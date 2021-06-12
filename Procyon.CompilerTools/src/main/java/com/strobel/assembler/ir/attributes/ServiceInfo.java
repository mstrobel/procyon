package com.strobel.assembler.ir.attributes;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;

import java.util.Collections;
import java.util.List;

public final class ServiceInfo {
    public final static ServiceInfo[] EMPTY = new ServiceInfo[0];

    private final TypeReference _interface;
    private final List<TypeReference> _implementations;

    public ServiceInfo(final TypeReference serviceInterface, final TypeReference[] implementations) {
        _interface = VerifyArgument.notNull(serviceInterface, "serviceInterface");

        _implementations = ArrayUtilities.isNullOrEmpty(implementations) ? Collections.<TypeReference>emptyList()
                                                                         : ArrayUtilities.asUnmodifiableList(implementations);
    }

    @NotNull
    public TypeReference getInterface() {
        return _interface;
    }

    @NotNull
    public List<TypeReference> getImplementations() {
        return _implementations;
    }
}
