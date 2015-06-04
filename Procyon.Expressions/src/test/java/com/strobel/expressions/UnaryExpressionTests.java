/*
 * UnaryExpressionTests.java
 *
 * Copyright (c) 2015 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.expressions;

import com.strobel.reflection.Type;
import org.junit.Test;

import static com.strobel.expressions.Expression.*;
import static org.junit.Assert.*;

public class UnaryExpressionTests extends AbstractExpressionTest {
    @Test
    public void testUnaryPlus() throws Throwable {
        assertResultEquals(unaryPlus(constant((byte) -5)), (byte) -5);
        assertResultEquals(unaryPlus(constant((short) -5)), (short) -5);
        assertResultEquals(unaryPlus(constant(-5)), -5);
        assertResultEquals(unaryPlus(constant(-5L)), -5L);
        assertResultEquals(unaryPlus(constant(-5f)), -5f);
        assertResultEquals(unaryPlus(constant(-5d)), -5d);

        try {
            lambda(unaryPlus(constant(new TestNumber(5)))).compileHandle().invoke();
            fail("Exception expected!");
        }
        catch (final IllegalStateException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            assertEquals(
                Error.unaryOperatorNotDefined(ExpressionType.UnaryPlus, TestNumber.TYPE).getMessage(),
                e.getMessage()
            );
        }
    }

    @Test
    public void testUnaryPlusWithMethod() throws Throwable {
        assertResultEquals(
            unaryPlus(constant(new TestNumber(-5)), TestNumber.TYPE.getMethod("abs")),
            new TestNumber(5)
        );
    }

    @Test
    public void testNegate() throws Throwable {
        assertResultEquals(negate(constant((byte) 5)), (byte) -5);
        assertResultEquals(negate(constant((short) 5)), (short) -5);
        assertResultEquals(negate(constant(5)), -5);
        assertResultEquals(negate(constant(5L)), -5L);
        assertResultEquals(negate(constant(5f)), -5f);
        assertResultEquals(negate(constant(5d)), -5d);
        assertResultEquals(negate(constant(new TestNumber(5))), new TestNumber(-5));

        assertResultEquals(negate(constant((byte) -5)), (byte) 5);
        assertResultEquals(negate(constant((short) -5)), (short) 5);
        assertResultEquals(negate(constant(-5)), 5);
        assertResultEquals(negate(constant(-5L)), 5L);
        assertResultEquals(negate(constant(-5f)), 5f);
        assertResultEquals(negate(constant(-5d)), 5d);
        assertResultEquals(negate(constant(new TestNumber(-5))), new TestNumber(5));
    }

    @Test
    public void testNegateWithMethod() throws Throwable {
        assertResultEquals(
            unaryPlus(constant(new TestNumber(5)), TestNumber.TYPE.getMethod("negate")),
            new TestNumber(-5)
        );
    }

    // <editor-fold defaultstate="collapsed" desc="TestNumber Class">

    private static final class TestNumber {
        private final static Type<TestNumber> TYPE = Type.of(TestNumber.class);
        private final int _value;

        public TestNumber(final int value) {
            _value = value;
        }

        public int value() {
            return _value;
        }

        public final TestNumber abs() {
            return new TestNumber(Math.abs(_value));
        }

        public final TestNumber negate() {
            return new TestNumber(-_value);
        }

        @Override
        public boolean equals(final Object o) {
            return this == o ||
                   (o instanceof TestNumber) && ((TestNumber) o)._value == _value;
        }

        @Override
        public int hashCode() {
            return _value;
        }
    }

    // </editor-fold>
}
