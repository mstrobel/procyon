package com.strobel;

import com.strobel.expressions.ExpressionList;
import com.strobel.expressions.LambdaExpression;
import com.strobel.expressions.ParameterExpression;
import com.strobel.reflection.*;
import com.strobel.reflection.emit.*;
import com.sun.source.tree.TreeVisitor;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

import static com.strobel.expressions.Expression.*;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("UnusedDeclaration")
public class Test {
    public static void main(final String[] args) {
        testTypeBuilder();
//        compilerToolsTest();
//        testGenericSignatures();
//        primitiveTest();
//        expressionTest();
//        genericMethodTest();
//        arrayTypeTest();
    }

    private static void testGenericSignatures() {
        System.out.println(Type.of(HashMap.class).getGenericSignature());
        System.out.println(Type.of(ExpressionList.class).getGenericSignature());
        System.out.println(Type.of(ITest2.class).getGenericSignature());
        System.out.println(Type.of(ITest3.class).getGenericSignature());
    }

    private static void compilerToolsTest() {
        final Context context = new Context();
        final Type<?> mapType = Type.of(HashMap.class);
        final Type<?> genericMapType = mapType.makeGenericType(Types.String, Types.Date);

        final Symbol symbol = JavaCompiler.instance(context).resolveIdent(ITest2.class.getName());

        final Class<? extends String> aClass = "".getClass();

        System.out.println(mapType);
        System.out.println(genericMapType);
        System.out.println(Type.of(ITest2.class));
        System.out.println(Arrays.toString(genericMapType.getMembers(BindingFlags.AllDeclared).toArray()));
        System.out.println(Arrays.toString(genericMapType.getMembers(BindingFlags.All).toArray()));
        System.out.println(Arrays.toString(Type.of(Type.class).getMethods(BindingFlags.AllStatic).toArray()));

        System.out.println(
            Type.of(Type.class)
                .makeGenericType(Types.BigInteger)
                .getMethods(
                    EnumSet.of(
                        BindingFlags.Public,
                        BindingFlags.NonPublic,
                        BindingFlags.Static,
                        BindingFlags.FlattenHierarchy
                    )
                )
                .get(3)
                .getDescription()
        );

        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getConstructors().get(3).getSignature());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getConstructors().get(3).getErasedSignature());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getMethods().get(4).getSignature());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getMethods().get(4).getErasedSignature());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getConstructors().get(3).getDescription());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getConstructors().get(3).getSimpleDescription());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getConstructors().get(3).getErasedDescription());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getMethods().get(4).getDescription());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getMethods().get(4).getSimpleDescription());
        System.out.println(Type.of(HashMap.class).makeGenericType(Types.String, Types.Date).getMethods().get(4).getErasedDescription());
        System.out.println(Arrays.toString(genericMapType.getNestedType("java.util.HashMap$ValueIterator", BindingFlags.All).getMethods().toArray()));
//        System.out.println(genericMapType.getMethods(BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic).get(30));
    }

    private static void primitiveTest() {
        System.out.println(PrimitiveTypes.Integer);
        System.out.println(Types.Integer);
        System.out.println(PrimitiveTypes.Integer == Types.Integer);
        System.out.println(PrimitiveTypes.Integer.getErasedClass() == Types.Integer.getErasedClass());
    }

    private static void genericMethodTest() {
        final Type map = Type.of(HashMap.class).makeGenericType(Types.String, Types.Date);
        final MethodInfo putMethod = map.getMethod("put");

        System.out.println("Full description: " + map.getFullDescription());
        System.out.println("Type signature: " + map.getSignature());
        System.out.println("Erased type signature: " + map.getErasedSignature());
        System.out.println("Method signature: " + putMethod.getSignature());
        System.out.println("Erased method signature: " + putMethod.getErasedSignature());
    }

    private static void arrayTypeTest() {
        final Type<String[]> stringArray = Types.String.makeArrayType();

        System.out.println("Full description: " + stringArray.getFullDescription());
        System.out.println("Type signature: " + stringArray.getSignature());
        System.out.println("Erased type signature: " + stringArray.getSignature());
        System.out.println("Element type signature: " + stringArray.getElementType().getSignature());
    }

    private static void expressionTest() {
        final ParameterExpression number = parameter(PrimitiveTypes.Integer, "number");

        final LambdaExpression<ITest> lambda = lambda(
            Type.of(ITest.class),
            condition(
                equal(number, constant(0)),
                constant("zero"),
                condition(
                    lessThan(number, constant(0)),
                    constant("negative"),
                    constant("positive")
                )
            ),
            number
        );

        System.out.println(lambda);
    }

    private static void exceptionTest() {
        System.out.println("lolwut");
        try {
            System.out.println(new File("").getCanonicalPath());
        }
        catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
        finally {
            System.out.print('c');
        }
    }

    private static class NullTree extends JCTree {

        @Override
        public Tag getTag() {
            return Tag.NO_TAG;
        }

        @Override
        public void accept(final Visitor v) {
        }

        @Override
        public <R, D> R accept(final TreeVisitor<R, D> v, final D d) {
            return null;
        }

        @Override
        public Kind getKind() {
            return Kind.NULL_LITERAL;
        }
    }

    private static void testTypeBuilder() {
        final TypeBuilder<ITest3<String, String>> t = new TypeBuilder<>(
            "com.strobel.MyTest2",
            Modifier.PUBLIC | Modifier.FINAL,
            Types.Object,
            Type.list(Type.of(ITest3.class).makeGenericType(Types.String, Types.String))
        );

        final ConstructorBuilder ctor = t.defineConstructor(
            Modifier.PUBLIC,
            TypeList.empty()
        );

        final MethodBuilder testMethod = t.defineMethod(
            "test",
            Modifier.PUBLIC,
            Types.String,
            Type.list(Types.String)
        );

        final MethodBuilder testBridge = t.defineMethod(
            "test",
            Modifier.PUBLIC | Modifier.VOLATILE | 0x00000040,
            Types.Comparable,
            Type.list(Types.Comparable)
        );

        BytecodeGenerator gen = ctor.getCodeGenerator();

        gen.emitThis();
        gen.call(Types.Object.getConstructor());
        gen.emit(OpCode.RETURN);

        gen = testMethod.getCodeGenerator();

        gen.getField(Type.of(System.class).getField("out"));
        gen.emitLoadArgument(0);
        gen.call(Type.of(PrintStream.class).getMethod("println", BindingFlags.PublicInstanceExact, Types.String));
        gen.emitLoadArgument(0);
        gen.emit(OpCode.ARETURN);

        gen = testBridge.getCodeGenerator();

        gen.emitThis();
        gen.emitLoadArgument(0);
        gen.emit(OpCode.CHECKCAST, Types.String);
        gen.call(testMethod);
        gen.emit(OpCode.ARETURN);

        final Type<ITest3<String, String>> generatedType = t.createType();
        final ITest3<String, String> instance = generatedType.newInstance();

        instance.test("HOLY FREAKIN' CRAP!");
    }
}

interface ITest {
    String testNumber(final int number);
}

interface ITest2<T extends String & Comparable<String> & Serializable, T2 extends T> {
    T2 test(final T t);
    <X, Y> Y doSomething(X o);
}

