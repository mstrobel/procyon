package com.strobel.expressions;

import com.strobel.core.delegates.Func;
import com.strobel.reflection.Type;
import com.strobel.reflection.Types;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.strobel.expressions.Expression.*;
import static junit.framework.Assert.assertEquals;

/**
 * @author Mike Strobel
 */
public class CompilerTests {
    interface ListRetriever<T> {
        List<T> getList();
    }

    @Test
    public void testGenericMethodCall()
        throws Exception {

        final LambdaExpression listRetriever = lambda(
            Type.of(ListRetriever.class).makeGenericType(Types.String),
            call(
                Type.of(Collections.class),
                "emptyList",
                Type.list(Types.String)
            )
        );

        final Delegate delegate = listRetriever.compileDelegate();
        final Object result = delegate.invokeDynamic();

        assertEquals(Collections.emptyList(), result);
    }
}
