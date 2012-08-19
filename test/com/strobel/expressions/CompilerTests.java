package com.strobel.expressions;

import com.strobel.core.delegates.Action1;
import com.strobel.core.delegates.Func1;
import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import com.strobel.reflection.TypeList;
import com.strobel.reflection.Types;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.strobel.expressions.Expression.*;
import static junit.framework.Assert.*;

/**
 * @author Mike Strobel
 */
public class CompilerTests {

    private static final RuntimeException TestRuntimeException = new RuntimeException("More bad shit happened, yo.");

    interface IListRetriever<T> {
        List<T> getList();
    }

    interface INeedsBridgeMethod<T extends Comparable<String>> {
        T invoke(final T t);
    }

    @Test
    public void testStringEquals()
        throws Exception {

        final ParameterExpression p = parameter(Types.String, "s");
        final MemberExpression out = field(null, Type.of(System.class).getField("out"));

        final LambdaExpression<Action1<String>> lambda = lambda(
            Type.of(Action1.class).makeGenericType(Types.String),
            call(
                out,
                "println",
                equal(constant("one"), p)
            ),
            p
        );

        System.out.println();
        System.out.println(lambda);

        final Delegate delegate = lambda.compileDelegate();

        System.out.println();
        System.out.printf("\n[%s]\n", delegate.getInstance().getClass().getSimpleName());

        delegate.invokeDynamic("one");
        delegate.invokeDynamic("two");
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
    public void testSimpleLoop()
        throws Exception {
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
                            makeBreak(breakLabel)
                        ),
                        call(
                            out,
                            "printf",
                            constant("Loop iteration #%d\n"),
                            newArrayInit(
                                Types.Object,
                                convert(lcv, Types.Object)
                            )
                        ),
                        preIncrementAssign(lcv)
                    ),
                    breakLabel,
                    continueLabel
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
    public void testForEachWithArray()
        throws Exception {
        final Expression out = field(null, Type.of(System.class).getField("out"));
        final ParameterExpression item = variable(Types.String, "item");

        final ConstantExpression items = constant(
            new String[]{"one", "two", "three", "four", "five"}
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
    public void testForEachWithIterable()
        throws Exception {
        final Expression out = field(null, Type.of(System.class).getField("out"));
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

    @Test
    public void simpleLambdaTest()
        throws Exception {
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
    public void returnLabelTest()
        throws Exception {
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

    @Test
    public void testIntegerLookupSwitch()
        throws Exception {
        final ParameterExpression number = parameter(Types.Integer, "number");

        final LambdaExpression<Func1<Integer, String>> lambda = lambda(
            Type.of(Func1.class).makeGenericType(Types.Integer, Types.String),
            makeSwitch(
                unbox(number),
                SwitchOptions.PreferLookup,
                constant("something else"),
                switchCase(
                    constant("one or two"),
                    constant(1),
                    constant(2)
                ),
                switchCase(
                    constant("three"),
                    constant(3)
                ),
                switchCase(
                    constant("five"),
                    constant(5)
                )
            ),
            number
        );

        System.out.println();

        System.out.println(lambda);

        final Func1<Integer, String> delegate = lambda.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        delegate.apply(0);
        delegate.apply(1);
        delegate.apply(2);
        delegate.apply(3);
        delegate.apply(4);
        delegate.apply(5);
        delegate.apply(6);

        assertEquals("something else", delegate.apply(0));
        assertEquals("one or two", delegate.apply(1));
        assertEquals("one or two", delegate.apply(2));
        assertEquals("three", delegate.apply(3));
        assertEquals("something else", delegate.apply(4));
        assertEquals("five", delegate.apply(5));
        assertEquals("something else", delegate.apply(6));
    }

    @Test
    public void testIntegerTableSwitch()
        throws Exception {
        final ParameterExpression number = parameter(Types.Integer, "number");

        final LambdaExpression<Func1<Integer, String>> lambda = lambda(
            Type.of(Func1.class).makeGenericType(Types.Integer, Types.String),
            makeSwitch(
                unbox(number),
                SwitchOptions.PreferTable,
                constant("something else"),
                switchCase(
                    constant("one or two"),
                    constant(1),
                    constant(2)
                ),
                switchCase(
                    constant("three"),
                    constant(3)
                ),
                switchCase(
                    constant("five"),
                    constant(5)
                )
            ),
            number
        );

        System.out.println();

        System.out.println(lambda);

        final Func1<Integer, String> delegate = lambda.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        delegate.apply(0);
        delegate.apply(1);
        delegate.apply(2);
        delegate.apply(3);
        delegate.apply(4);
        delegate.apply(5);
        delegate.apply(6);

        assertEquals("something else", delegate.apply(0));
        assertEquals("one or two", delegate.apply(1));
        assertEquals("one or two", delegate.apply(2));
        assertEquals("three", delegate.apply(3));
        assertEquals("something else", delegate.apply(4));
        assertEquals("five", delegate.apply(5));
        assertEquals("something else", delegate.apply(6));
    }

    enum TestEnum {
        ZERO,
        ONE,
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX
    }

    @Test
    public void testEnumLookupSwitch()
        throws Exception {
        final Type<TestEnum> enumType = Type.of(TestEnum.class);
        final ParameterExpression enumValue = parameter(enumType, "e");

        final LambdaExpression<Func1<TestEnum, String>> lambda = lambda(
            Type.of(Func1.class).makeGenericType(enumType, Types.String),
            makeSwitch(
                enumValue,
                constant("something else"),
                switchCase(
                    constant("one or two"),
                    constant(TestEnum.ONE),
                    constant(TestEnum.TWO)
                ),
                switchCase(
                    constant("three"),
                    constant(TestEnum.THREE)
                ),
                switchCase(
                    constant("five"),
                    constant(TestEnum.FIVE)
                )
            ),
            enumValue
        );

        System.out.println();

        System.out.println(lambda);

        final Func1<TestEnum, String> delegate = lambda.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        delegate.apply(TestEnum.ONE);
        delegate.apply(TestEnum.TWO);
        delegate.apply(TestEnum.THREE);
        delegate.apply(TestEnum.FOUR);
        delegate.apply(TestEnum.FIVE);
        delegate.apply(TestEnum.SIX);

        assertEquals("something else", delegate.apply(TestEnum.ZERO));
        assertEquals("one or two", delegate.apply(TestEnum.ONE));
        assertEquals("one or two", delegate.apply(TestEnum.TWO));
        assertEquals("three", delegate.apply(TestEnum.THREE));
        assertEquals("something else", delegate.apply(TestEnum.FOUR));
        assertEquals("five", delegate.apply(TestEnum.FIVE));
        assertEquals("something else", delegate.apply(TestEnum.SIX));
    }

    @Test
    public void testStringTrieSwitch()
        throws Exception {
        final ParameterExpression stringValue = parameter(Types.String, "s");

        final LambdaExpression<Func1<String, String>> lambda = lambda(
            Type.of(Func1.class).makeGenericType(Types.String, Types.String),
            makeSwitch(
                stringValue,
                SwitchOptions.PreferTrie,
                constant("something else"),
                switchCase(
                    constant("one or two"),
                    constant("1"),
                    constant("2")
                ),
                switchCase(
                    constant("three"),
                    constant("3")
                ),
                switchCase(
                    constant("five"),
                    constant("5")
                )
            ),
            stringValue
        );

        System.out.println();

        System.out.println(lambda);

        final Func1<String, String> delegate = lambda.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        System.out.println(delegate.apply("0"));
        System.out.println(delegate.apply("1"));
        System.out.println(delegate.apply("2"));
        System.out.println(delegate.apply("3"));
        System.out.println(delegate.apply("4"));
        System.out.println(delegate.apply("5"));
        System.out.println(delegate.apply("6"));

        assertEquals("something else", delegate.apply("0"));
        assertEquals("one or two", delegate.apply("1"));
        assertEquals("one or two", delegate.apply("2"));
        assertEquals("three", delegate.apply("3"));
        assertEquals("something else", delegate.apply("4"));
        assertEquals("five", delegate.apply("5"));
        assertEquals("something else", delegate.apply("6"));
    }

    @Test
    public void testStringTableSwitch()
        throws Exception {
        final ParameterExpression stringValue = parameter(Types.String, "s");

        final LambdaExpression<Func1<String, String>> lambda = lambda(
            Type.of(Func1.class).makeGenericType(Types.String, Types.String),
            makeSwitch(
                stringValue,
                SwitchOptions.PreferTable,
                constant("something else"),
                switchCase(
                    constant("one or two"),
                    constant("1"),
                    constant("2")
                ),
                switchCase(
                    constant("three"),
                    constant("3")
                ),
                switchCase(
                    constant("five"),
                    constant("5")
                )
            ),
            stringValue
        );

        System.out.println();

        System.out.println(lambda);

        final Func1<String, String> delegate = lambda.compile();

        System.out.printf("\n[%s]\n", delegate.getClass().getSimpleName());

        System.out.println(delegate.apply("0"));
        System.out.println(delegate.apply("1"));
        System.out.println(delegate.apply("2"));
        System.out.println(delegate.apply("3"));
        System.out.println(delegate.apply("4"));
        System.out.println(delegate.apply("5"));
        System.out.println(delegate.apply("6"));

        assertEquals("something else", delegate.apply("0"));
        assertEquals("one or two", delegate.apply("1"));
        assertEquals("one or two", delegate.apply("2"));
        assertEquals("three", delegate.apply("3"));
        assertEquals("something else", delegate.apply("4"));
        assertEquals("five", delegate.apply("5"));
        assertEquals("something else", delegate.apply("6"));
    }

    @Test
    public void testTryCatchFinally()
        throws Exception {
        final Expression out = field(null, Type.of(System.class).getField("out"));
        final ParameterExpression tempException = variable(Types.RuntimeException, "$exception");

        final LambdaExpression<Runnable> lambda = lambda(
            Type.of(Runnable.class),
            block(
                new ParameterExpressionList(tempException),
                makeTry(
                    PrimitiveTypes.Void,
                    call(Type.of(CompilerTests.class), "holdMeThrillMeKissMeThrowMe1"),
                    call(out, "println", constant("In the finally block.")),
                    makeCatch(
                        Type.of(AssertionError.class),
                        call(out, "println", constant("In the AssertionError catch block."))
                    )
                )
            )
        );

        final Runnable delegate = lambda.compile();

        try {
            delegate.run();
        }
        catch (Throwable t) {
            fail("AssertionError should have been caught.");
        }
    }

    @Test
    public void testTryNestedCatchFinally()
        throws Exception {
        final Expression out = field(null, Type.of(System.class).getField("out"));
        final ParameterExpression tempException = variable(Types.RuntimeException, "$exception");

        final LambdaExpression<Runnable> lambda = lambda(
            Type.of(Runnable.class),
            block(
                new ParameterExpressionList(tempException),
                makeTry(
                    PrimitiveTypes.Void,
                    call(Type.of(CompilerTests.class), "holdMeThrillMeKissMeThrowMe1"),
                    call(out, "println", constant("In the finally block.")),
                    makeCatch(
                        Type.of(AssertionError.class),
                        makeTry(
                            PrimitiveTypes.Void,
                            block(
                                call(out, "println", constant("In the AssertionError catch block.")),
                                call(Type.of(CompilerTests.class), "holdMeThrillMeKissMeThrowMe2")
                            ),
                            makeCatch(
                                Types.RuntimeException,
                                tempException,
                                block(
                                    call(out, "println", constant("In the RuntimeException catch block.")),
                                    makeThrow(tempException)
                                )
                            )
                        )
                    )
                )
            )
        );

        final Runnable delegate = lambda.compile();

        try {
            delegate.run();
            fail("AssertionError should have been caught.");
        }
        catch (AssertionError e) {
            fail("AssertionError should have been caught.");
        }
        catch (Throwable e) {
            assertEquals(TestRuntimeException, e);
        }
    }

    static void holdMeThrillMeKissMeThrowMe1()
        throws AssertionError {
        throw new AssertionError("Bad shit happened, yo.");
    }

    static void holdMeThrillMeKissMeThrowMe2() {
        throw TestRuntimeException;
    }
}
