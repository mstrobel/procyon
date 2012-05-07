package com.strobel;

import com.fasterxml.classmate.GenericType;
import com.strobel.expressions.ConditionalExpression;
import com.strobel.expressions.Expression;
import com.strobel.expressions.LambdaExpression;
import com.strobel.expressions.ParameterExpression;
import com.strobel.reflection.BindingFlags;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static com.strobel.expressions.Expression.*;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("UnusedDeclaration")
public class Test {
    public static void main(final String[] args) {
        expressionTest();
        testTypeResolution();
    }

    private static void testTypeResolution() {
        final GenericType<?> gt = new GenericType<HashMap<String, UUID>>() {
        };
        // ((ParameterizedTypeImpl) ((ParameterizedTypeImpl) ((ParameterizedType)gt.getClass().getGenericSuperclass())).actualTypeArguments[0]).rawType.getGenericInterfaces()
        final java.lang.reflect.Type[] genericInterfaces =
            ((ParameterizedTypeImpl) ((ParameterizedType) gt.getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getRawType().getGenericInterfaces();

        final Type hashMap = Type.of(HashMap.class);

        System.out.println(hashMap.getName());
        System.out.println(hashMap.getSignature());
        System.out.println(hashMap.getErasedSignature());
        System.out.println(hashMap.getBriefDescription());
        System.out.println(hashMap.getFullDescription());

        final Type boundHashMap = Type.of(HashMap.class)
                                      .makeGenericType(Type.of(String.class), Type.of(UUID.class))
                                      .makeArrayType();

        System.out.println(boundHashMap.getName());
        System.out.println(boundHashMap.getSignature());
        System.out.println(boundHashMap.getErasedSignature());
        System.out.println(boundHashMap.getBriefDescription());

        System.out.println(Arrays.toString(boundHashMap.getElementType().getMethods().toArray()));

        final MethodInfo[] genericMethodDefinitions = Type.of(GenericMethodTest.class)
                                                          .getMethods(
                                                              BindingFlags.DeclaredOnly |
                                                              BindingFlags.Public |
                                                              BindingFlags.NonPublic |
                                                              BindingFlags.Static)
                                                          .toArray();

        System.out.println(Arrays.toString(genericMethodDefinitions));

        final MethodInfo genericMethod = genericMethodDefinitions[0].makeGenericMethod(
            Type.list(Type.of(int[].class))
        );

        System.out.println(genericMethod.getSignature());

        printTypeTree(boundHashMap);
    }

    private static void expressionTest() {
        final ParameterExpression number = parameter(PrimitiveTypes.Integer, "number");

        final ConditionalExpression body = condition(
            equal(number, constant(0)),
            constant("zero"),
            condition(
                lessThan(number, constant(0)),
                constant("negative"),
                constant("positive")
            )
        );

        System.out.println(body);

        final LambdaExpression<ITest> lambda = Expression.lambda(
            Type.of(ITest.class),
            body,
            number
        );
    }

    private static void printTypeTree(final Type type) {
        final Type baseType = type.getBaseType();
        if (baseType != null && baseType != Type.Object) {
            printTypeTree(baseType);
        }
        System.out.println(type.getFullDescription());
        for (final Type interfaceType : type.getInterfaces()) {
            printTypeTree(interfaceType);
        }
        if (type.isArray()) {
            printTypeTree(type.getElementType());
        }
    }
}

interface ITest {
    String testNumber(final int number);
}

final class GenericMethodTest {
    static <T> T of(final Class<T> clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }
}