package com.strobel.expressions;

import com.strobel.reflection.*;
import com.strobel.reflection.emit.CodeGenerator;
import com.strobel.reflection.emit.MethodBuilder;
import com.strobel.reflection.emit.OpCode;
import com.strobel.reflection.emit.TypeBuilder;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

/**
 * @author strobelm
 */
public class OldTests {
    public static void main(final String[] args) {
        testTypeBuilder();
        compilerToolsTest();
        primitiveTest();
        testGenericSignatures();
        genericMethodTest();
        arrayTypeTest();
    }

    private static void testGenericSignatures() {
        System.out.println(Type.of(HashMap.class).getGenericSignature());
        System.out.println(Type.of(ExpressionList.class).getGenericSignature());
        System.out.println(Type.of(ITest2.class).getGenericSignature());
        System.out.println(Type.of(ITest3.class).getGenericSignature());
    }

    private static void compilerToolsTest() {
        final Type<?> mapType = Type.of(HashMap.class);
        final Type<?> genericMapType = mapType.makeGenericType(Types.String, Types.Date);

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

        System.out.println("Full description: " + map.getDescription());
        System.out.println("Type signature: " + map.getSignature());
        System.out.println("Erased type signature: " + map.getErasedSignature());
        System.out.println("Method signature: " + putMethod.getSignature());
        System.out.println("Erased method signature: " + putMethod.getErasedSignature());
    }

    private static void arrayTypeTest() {
        final Type<String[]> stringArray = Types.String.makeArrayType();

        System.out.println("Full description: " + stringArray.getDescription());
        System.out.println("Type signature: " + stringArray.getSignature());
        System.out.println("Erased type signature: " + stringArray.getSignature());
        System.out.println("Element type signature: " + stringArray.getElementType().getSignature());
    }

    private static void testTypeBuilder() {
        final TypeBuilder<ITest3<String, String>> t = new TypeBuilder<>(
            /* name: */         "MyTest",
            /* modifiers: */    Modifier.PUBLIC | Modifier.FINAL,
            /* baseType: */     Types.Object,
            /* interfaces: */   Type.list(Type.of(ITest3.class).makeGenericType(Types.String, Types.String))
        );

        final MethodBuilder testMethod = t.defineMethod(
            "test",
            Modifier.PUBLIC,
            Types.String,
            Type.list(Types.String)
        );

        CodeGenerator gen = testMethod.getCodeGenerator();

        final MethodInfo println = Type.of(PrintStream.class)
                                       .getMethod(
                                           "println",
                                           BindingFlags.PublicInstanceExact,
                                           Types.String
                                       );

        gen.getField(Type.of(System.class).getField("out"));
        gen.emitLoadArgument(0);
        gen.call(println);
        gen.emitLoadArgument(0);
        gen.emit(OpCode.ARETURN);

        final MethodBuilder testBridge = t.defineMethod(
            "test",
            Modifier.PUBLIC | 0x00000040,
            Types.Comparable,
            Type.list(Types.Comparable)
        );

        gen = testBridge.getCodeGenerator();

        gen.emitThis();
        gen.emitLoadArgument(0);
        gen.emitConversion(Types.Comparable, Types.String);
        gen.call(testMethod);
        gen.emit(OpCode.ARETURN);

        t.createType().newInstance().test("HOLY FREAKIN' CRAP!");
    }
}

interface ITest {
    String testNumber(final int number) throws NumberFormatException;
}

interface ITest2<T extends String & Comparable<String> & Serializable, T2 extends T> {
    T2 test(final T t);
    <X, Y> Y doSomething(X o);
}

interface ITest3<T extends Comparable<String> & Serializable, T2 extends T> {
    T2 test(final T t);
}