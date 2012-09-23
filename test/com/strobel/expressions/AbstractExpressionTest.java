package com.strobel.expressions;

import com.strobel.reflection.Type;
import org.junit.After;
import org.junit.Before;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * @author Mike Strobel
 */
public abstract class AbstractExpressionTest {
    private final static ThreadLocal<Queue<Object>> OUTPUT_QUEUE = new ThreadLocal<Queue<Object>>() {
        @Override
        protected Queue<Object> initialValue() {
            return new ArrayDeque<>();
        }
    };

    private static final ThreadLocal<OutputInfo> THREAD_OUT = new ThreadLocal<OutputInfo>() {
        @Override
        protected OutputInfo initialValue() {
            return new OutputInfo(queue());
        }
    };

    public static void push(final Object value) {
        OUTPUT_QUEUE.get().add(value);
    }

    public static Object dequeue() {
        return OUTPUT_QUEUE.get().poll();
    }

    public static Object peek() {
        return OUTPUT_QUEUE.get().peek();
    }

    public static Queue<Object> queue() {
        return OUTPUT_QUEUE.get();
    }

    public static Expression makePush(final Expression value) {
        return Expression.call(
            Expression.call(
                Type.of(AbstractExpressionTest.class),
                "queue"
            ),
            "add",
            value
        );
    }

    public static Expression outExpression() {
        return Expression.field(
            null,
            Type.of(System.class).getField("out")
        );
    }
    public static OutputRecorder outRecorder() {
        return THREAD_OUT.get().recorder;
    }

    @Before
    public void setUp() throws Throwable {
        final OutputInfo outputInfo = THREAD_OUT.get();
        outputInfo.systemStream = System.out;
        System.setOut(outputInfo.recorderStream);
        System.setProperty("com.strobel.reflection.emit.TypeBuilder.DumpGeneratedClasses", "true");
    }

    @After
    public void tearDown() throws Throwable {
        final OutputInfo outputInfo = THREAD_OUT.get();
        outputInfo.recorder.reset();
        System.setOut(outputInfo.systemStream);
        outputInfo.systemStream = null;
        final Queue<Object> queue = OUTPUT_QUEUE.get();
        queue.clear();
    }

    @SuppressWarnings("PackageVisibleField")
    final static class OutputInfo {
        final Queue<Object> outputQueue;
        final OutputRecorder recorder;
        final PrintStream recorderStream;
        PrintStream systemStream;

        OutputInfo(final Queue<Object> outputQueue) {
            this.outputQueue = outputQueue;
            this.recorder = new OutputRecorder(System.out, outputQueue);
            this.recorderStream = new PrintStream(recorder);
        }
    }
}
