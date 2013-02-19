/*
 * VariableDefinition.java
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

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 2:11 PM
 */
public final class VariableDefinition extends VariableReference {
    public VariableDefinition(final TypeReference variableType) {
        super(variableType);
    }

    public VariableDefinition(final String name, final TypeReference variableType) {
        super(name, variableType);
    }

    @Override
    public VariableDefinition resolve() {
        return this;
    }
}
