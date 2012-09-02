package com.strobel.expressions;

import com.strobel.reflection.Types;
import org.junit.Test;

import static com.strobel.expressions.Expression.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Mike Strobel
 */
public class ConcatTests extends AbstractCompilerTest {
    @Test
    public void testConcat() throws Throwable {
        final LambdaExpression<Runnable> test = lambda(
            Types.Runnable,
            call(
                outExpression(),
                "println",
                concat(constant("a"), constant("b"), constant("c"))
            )
        );

        final Runnable runnable = test.compile();

        runnable.run();

        assertEquals("abc", dequeue());
    }

    @Test
    public void testNullConcat() throws Throwable {
        final LambdaExpression<Runnable> test = lambda(
            Types.Runnable,
            call(
                outExpression(),
                "println",
                concat(constant(1), defaultValue(Types.Object))
            )
        );

        final Runnable runnable = test.compile();

        runnable.run();

        assertEquals("1null", dequeue());
    }
}
