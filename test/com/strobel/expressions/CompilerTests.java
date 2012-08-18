package com.strobel.expressions;

import com.strobel.compilerservices.Closure;
import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import com.strobel.reflection.TypeList;
import com.strobel.reflection.Types;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static com.strobel.expressions.Expression.*;
import static junit.framework.Assert.*;

/**
 * @author Mike Strobel
 */
public class CompilerTests {
    interface IListRetriever<T> {
        List<T> getList();
    }

    interface INeedsBridgeMethod<T extends Comparable<String>> {
        T invoke(final T t);
    }

    @Test
    public void testGenericMethodCall()
        throws Exception {

        final LambdaExpression listRetriever = lambda(
            Type.of(IListRetriever.class).makeGenericType(Types.String),
            call(
                Type.of(Collections.class),
                "emptyList",
                Type.list(Types.String)
            )
        );

        System.out.println();
        System.out.println(listRetriever);

        final Delegate delegate = listRetriever.compileDelegate();
        final Object result = delegate.invokeDynamic();

        System.out.println();
        System.out.printf("\n[%s]\n", delegate.getInstance().getClass().getSimpleName());
        System.out.println(result);

        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testBridgeMethodGeneration()
        throws Exception {

        final ParameterExpression arg = parameter(Types.String);

        final LambdaExpression listRetriever = lambda(
            Type.of(INeedsBridgeMethod.class).makeGenericType(Types.String),
            arg,
            arg
        );

        final String input = "zomg";
        final Delegate delegate = listRetriever.compileDelegate();
        final Object result = delegate.invokeDynamic(input);

        assertSame(input, result);
    }

    @Test
    public void testSimpleLoop() throws Exception {
        final ParameterExpression lcv = variable(PrimitiveTypes.Integer, "i");

        final LabelTarget breakLabel = label();
        final LabelTarget continueLabel = label();
        final MemberExpression out = field(null, Type.of(System.class).getField("out"));

        final LambdaExpression<Runnable> runnable = lambda(
            Type.of(Runnable.class),
            block(
                new ParameterExpressionList(lcv),
                assign(lcv, constant(0)),
                call(out, "println", constant("Starting the loop...")),
                loop(
                    block(
                        PrimitiveTypes.Void,
                        ifThen(
                            greaterThanOrEqual(lcv, constant(5)),
                            makeBreak(breakLabel)),
                        call(
                            out,
                            "printf",
                            constant("Loop iteration #%d\n"),
                            newArrayInit(
                                Types.Object,
                                convert(lcv, Types.Object)
                            )),
                        preIncrementAssign(lcv)),
                    breakLabel,
                    continueLabel),
                call(out, "println", constant("Finished the loop!"))
            )
        );

        System.out.println();
        System.out.println(runnable);

        final Runnable delegate = runnable.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        delegate.run();
    }

    @Test
    public void testForEachWithArray() throws Exception {
        final MemberExpression out = field(null, Type.of(System.class).getField("out"));
        final ParameterExpression item = variable(Types.String, "item");

        final ConstantExpression items = constant(
            new String[] { "one", "two", "three", "four", "five" }
        );

        final LambdaExpression<Runnable> runnable = lambda(
            Type.of(Runnable.class),
            block(
                call(out, "println", constant("Starting the 'for each' loop...")),
                forEach(
                    item,
                    items,
                    call(
                        out,
                        "printf",
                        constant("Got item: %s\n"),
                        newArrayInit(
                            Types.Object,
                            convert(item, Types.Object)
                        )
                    )
                ),
                call(out, "println", constant("Finished the loop!"))
            )
        );

        System.out.println();
        System.out.println(runnable);

        final Runnable delegate = runnable.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        delegate.run();
    }

    @Test
    public void testForEachWithIterable() throws Exception {
        final MemberExpression out = field(null, Type.of(System.class).getField("out"));
        final ParameterExpression item = variable(Types.String, "item");

        final ConstantExpression items = constant(
            Arrays.asList("one", "two", "three", "four", "five"),
            Types.Iterable.makeGenericType(Types.String)
        );

        final LambdaExpression<Runnable> runnable = lambda(
            Type.of(Runnable.class),
            block(
                call(out, "println", constant("Starting the 'for each' loop...")),
                forEach(
                    item,
                    items,
                    call(
                        out,
                        "printf",
                        constant("Got item: %s\n"),
                        newArrayInit(
                            Types.Object,
                            convert(item, Types.Object)
                        )
                    )
                ),
                call(out, "println", constant("Finished the loop!"))
            )
        );

        System.out.println();
        System.out.println(runnable);

        final Runnable delegate = runnable.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        delegate.run();
    }

    private final static class CompileIt implements Runnable {
        private final Closure __closure;

        private CompileIt(final Closure closure) {
            __closure = closure;
        }

        @Override
        public void run() {
            System.out.println("Starting the 'for each' loop...");
            String __local1;
            for (final Iterator<String> __local0 = ((Iterable<String>)__closure.constants[0]).iterator();
                 __local0.hasNext(); System.out.printf("Got item: %s\n", new Object[]{(Object)__local1})) {

                __local1 = ((String)(__local0.next()));
            }

            System.out.println("Finished the loop!");
        }
    }

    @Test
    public void simpleLambdaTest() throws Exception {
        final ParameterExpression number = parameter(PrimitiveTypes.Integer, "number");

        final LambdaExpression<ITest> lambda = lambda(
            Type.of(ITest.class),
            call(
                condition(
                    equal(
                        number,
                        call(
                            Types.Integer,
                            "parseInt",
                            TypeList.empty(),
                            constant("0")
                        )
                    ),
                    constant("zero"),
                    condition(
                        lessThan(number, constant(0)),
                        constant("negative"),
                        constant("positive")
                    )
                ),
                "toUpperCase",
                TypeList.empty(),
                constant(Locale.getDefault())
            ),
            number
        );

        System.out.println();
        System.out.println(lambda);

        final ITest delegate = lambda.compile();

        assertEquals("NEGATIVE", delegate.testNumber(-15));
        assertEquals("ZERO", delegate.testNumber(0));
        assertEquals("POSITIVE", delegate.testNumber(99));

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        System.out.println(delegate.testNumber(-15));
        System.out.println(delegate.testNumber(0));
        System.out.println(delegate.testNumber(99));
    }

    @Test
    public void returnLabelTest() throws Exception {
        final ParameterExpression number = parameter(PrimitiveTypes.Integer, "number");
        final LabelTarget returnLabel = label(Types.String);

        final LambdaExpression<ITest> lambda = lambda(
            Type.of(ITest.class),
            block(
                ifThenElse(
                    equal(number, constant(0)),
                    makeReturn(returnLabel, constant("zero")),
                    ifThenElse(
                        lessThan(number, constant(0)),
                        makeReturn(returnLabel, constant("negative")),
                        makeReturn(returnLabel, constant("positive"))
                    )
                ),
                label(returnLabel)
            ),
            number
        );

        System.out.println();

        System.out.println(lambda);

        final ITest delegate = lambda.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        assertEquals("negative", delegate.testNumber(-15));
        assertEquals("zero", delegate.testNumber(0));
        assertEquals("positive", delegate.testNumber(99));

        System.out.println(delegate.testNumber(-15));
        System.out.println(delegate.testNumber(0));
        System.out.println(delegate.testNumber(99));
    }
}
