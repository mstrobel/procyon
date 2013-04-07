/*
 * PrimitiveExpression.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.core.Comparer;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.languages.TextLocation;
import com.strobel.decompiler.patterns.INode;
import com.strobel.decompiler.patterns.Match;

public class PrimitiveExpression extends Expression {
    public final static Object ANY_VALUE = new Object();

    private final TextLocation _startLocation;
    private TextLocation _endLocation;

    private String _literalValue;
    private Object _value;

    public PrimitiveExpression(final Object value) {
        _value = value;
        _startLocation = TextLocation.EMPTY;
        _literalValue = "";
    }

    public PrimitiveExpression(final Object value, final String literalValue) {
        _value = value;
        _startLocation = TextLocation.EMPTY;
        _literalValue = literalValue != null ? literalValue : StringUtilities.EMPTY;
    }

    public PrimitiveExpression(final Object value, final TextLocation startLocation, final String literalValue) {
        _value = value;
        _startLocation = startLocation;
        _literalValue = literalValue != null ? literalValue : StringUtilities.EMPTY;
    }

    @Override
    public TextLocation getStartLocation() {
        final TextLocation startLocation = _startLocation;
        return startLocation != null ? startLocation : TextLocation.EMPTY;
    }

    @Override
    public TextLocation getEndLocation() {
        if (_endLocation == null) {
            final TextLocation startLocation = getStartLocation();
            if (_literalValue == null) {
                return startLocation;
            }
            _endLocation = new TextLocation(_startLocation.line(), _startLocation.column() + _literalValue.length());
        }
        return _endLocation;
    }

    public final String getLiteralValue() {
        return _literalValue;
    }

    public final void setLiteralValue(final String literalValue) {
        verifyNotFrozen();
        _literalValue = literalValue;
        _endLocation = null;
    }

    public final Object getValue() {
        return _value;
    }

    public final void setValue(final Object value) {
        verifyNotFrozen();
        _value = value;
    }

    @Override
    public <T, R> R acceptVisitor(final IAstVisitor<? super T, ? extends R> visitor, final T data) {
        return visitor.visitPrimitiveExpression(this, data);
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof PrimitiveExpression) {
            return !other.isNull() &&
                   (_value == ANY_VALUE || Comparer.equals(_value, ((PrimitiveExpression) other)._value));
        }

        return false;
    }
}
