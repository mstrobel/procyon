package com.strobel.reflection.emit;

import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.Type;
import com.strobel.reflection.TypeList;
import com.strobel.reflection.Types;
import org.junit.Test;

import java.lang.reflect.Modifier;

import static org.junit.Assert.assertSame;

/**
 * @author Mike Strobel
 */
public final class TypeBuilderTests {
    @Test
    public void testGenericTypeBuilder() throws Throwable {
        final TypeBuilder typeBuilder = new TypeBuilder(
            TypeBuilderTests.class.getPackage().getName() + ".TestGenericType",
            Modifier.PUBLIC | Modifier.FINAL,
            Types.Object,
            TypeList.empty()
        );

        typeBuilder.defineDefaultConstructor();

        final GenericParameterBuilder[] gp = typeBuilder.defineGenericParameters("T");

        final MethodBuilder method = typeBuilder.defineMethod(
            "test",
            Modifier.PUBLIC | Modifier.FINAL,
            gp[0],
            Type.list(gp[0])
        );

        final CodeGenerator code = method.getCodeGenerator();

        code.emitLoadArgument(0);
        code.emitReturn(gp[0]);

        final Type createdType = typeBuilder.createType();
        final Type boundType = createdType.makeGenericType(Types.String);
        final MethodInfo boundMethod = boundType.getMethod("test");
        final Object instance = createdType.newInstance();
        final String parameter = "test";

        final Object result = boundMethod.invoke(instance, parameter);

        assertSame(parameter, result);
    }
}
