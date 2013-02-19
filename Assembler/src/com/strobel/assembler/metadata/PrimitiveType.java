/*
 * PrimitiveType.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.SimpleType;

/**
 * @author Mike Strobel
 */
public final class PrimitiveType extends TypeDefinition {
    private final SimpleType _simpleType;

    PrimitiveType(final SimpleType simpleType) {
        super(MetadataSystem.instance());
        _simpleType = VerifyArgument.notNull(simpleType, "simpleType");
        setFlags(Flags.PUBLIC);
        setName(_simpleType.getPrimitiveName());
    }

    @Override
    public String getInternalName() {
        return _simpleType.getDescriptorPrefix();
    }

    @Override
    public String getSimpleName() {
        return _simpleType.getPrimitiveName();
    }

    @Override
    public String getFullName() {
        return _simpleType.getDescriptorPrefix();
    }

    @Override
    public final boolean isPrimitive() {
        return true;
    }

    @Override
    public final SimpleType getSimpleType() {
        return _simpleType;
    }

    @Override
    protected StringBuilder appendName(final StringBuilder sb, final boolean fullName, final boolean dottedName) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendBriefDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendSimpleDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendErasedDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendClassDescription(final StringBuilder sb) {
        return sb.append(_simpleType.getPrimitiveName());
    }

    @Override
    protected StringBuilder appendSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    protected StringBuilder appendErasedSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    protected StringBuilder appendClassSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    protected StringBuilder appendErasedClassSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }

    @Override
    public StringBuilder appendGenericSignature(final StringBuilder sb) {
        return sb.append(_simpleType.getDescriptorPrefix());
    }
}
