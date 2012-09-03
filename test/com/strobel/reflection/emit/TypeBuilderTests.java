package com.strobel.reflection.emit;

import com.strobel.core.ReadOnlyList;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.Type;
import com.strobel.reflection.TypeList;
import com.strobel.reflection.Types;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * @author Mike Strobel
 */
public final class TypeBuilderTests {
    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnotation {
        int value();
    }

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

        final Type<TestAnnotation> annotationType = Type.of(TestAnnotation.class);

        final AnnotationBuilder<TestAnnotation> annotation = AnnotationBuilder.create(
            annotationType,
            annotationType.getMethods(),
            new ReadOnlyList<Object>(42)
        );

        typeBuilder.addCustomAnnotation(annotation);

        final CodeGenerator code = method.getCodeGenerator();

        code.emitLoadArgument(0);
        code.emitReturn(gp[0]);

        final Type<?> createdType = typeBuilder.createType();
        final Type<?> boundType = createdType.makeGenericType(Types.String);
        final MethodInfo boundMethod = boundType.getMethod("test");
        final Object instance = createdType.newInstance();
        final String parameter = "test";

        final Object result = boundMethod.invoke(instance, parameter);

        assertSame(parameter, result);

        final TestAnnotation createdTypeAnnotation = createdType.getErasedClass().getAnnotation(TestAnnotation.class);

        assertNotNull(createdTypeAnnotation);
        assertEquals(createdTypeAnnotation.value(), 42);
    }
}
