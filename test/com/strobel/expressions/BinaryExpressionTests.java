package com.strobel.expressions;

import com.strobel.reflection.Types;
import org.junit.Test;

import java.math.BigInteger;
import java.util.concurrent.Callable;

import static com.strobel.expressions.Expression.*;
import static org.junit.Assert.*;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class BinaryExpressionTests extends AbstractExpressionTest {
    @Test
    public void testMethodBasedBinaryOperators() throws Throwable {
        final BigInteger big = new BigInteger("1234567890123456789012345678901234567890");
        final BigInteger big1 = new BigInteger("1");
        final BigInteger big2 = new BigInteger("2");
        final BigInteger notSoBig = new BigInteger("1234567890");
        final BigInteger big1337 = new BigInteger("1337");
        final BigInteger expectedAddResult = new BigInteger("2469135780246913578024691357802469135780");
        final BigInteger expectedMulResult = expectedAddResult;
        final BigInteger expectedDivResult = new BigInteger("617283945061728394506172839450617283945");
        final BigInteger expectedSubResult = new BigInteger("0");
        final BigInteger expectedModResult = new BigInteger("808");
        final BigInteger expectedShLResult = new BigInteger("633825300114114700748351602688");
        final BigInteger expectedShRResult = big1;

        final LambdaExpression<Callable<BigInteger[]>> lambda = lambda(
            Types.Callable.makeGenericType(Types.BigInteger.makeArrayType()),
            newArrayInit(
                Types.BigInteger,
                add(constant(big), constant(big)),
                subtract(constant(big), constant(big)),
                multiply(constant(big), constant(big2)),
                divide(constant(big), convert(constant(2), Types.BigInteger)),
                modulo(constant(notSoBig), constant(big1337)),
                leftShift(constant(big1), constant(99)),
                rightShift(constant(expectedShLResult), constant(99))
            )
        );

        final BigInteger[] result = lambda.compile().call();

        assertEquals(expectedAddResult, result[0]);
        assertEquals(expectedSubResult, result[1]);
        assertEquals(expectedMulResult, result[2]);
        assertEquals(expectedDivResult, result[3]);
        assertEquals(expectedModResult, result[4]);
        assertEquals(expectedShLResult, result[5]);
        assertEquals(expectedShRResult, result[6]);
    }

    @Test
    public void testComparisonOperators() throws Throwable {
        assertResultTrue(lessThanOrEqual(constant(1), constant(1)));
        assertResultTrue(lessThan(constant(2), constant(3L)));
        assertResultFalse(greaterThan(constant(2d), constant(3d)));
        assertResultTrue(lessThanOrEqual(constant(2f), constant((byte)2)));
    }
}
