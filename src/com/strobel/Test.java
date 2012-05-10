package com.strobel;

import com.strobel.expressions.LambdaExpression;
import com.strobel.expressions.ParameterExpression;
import com.strobel.reflection.MethodInfo;
import com.strobel.reflection.PrimitiveTypes;
import com.strobel.reflection.Type;
import com.strobel.reflection.Types;
import com.sun.tools.apt.mirror.type.TypeMaker;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.nio.charset.Charset;
import java.util.HashMap;

import static com.strobel.expressions.Expression.*;

/**
 * @author Mike Strobel
 */
@SuppressWarnings("UnusedDeclaration")
public class Test {
    public static void main(final String[] args) {
        primitiveTest();
        expressionTest();
        genericMethodTest();
        arrayTypeTest();
        compilerToolsTest();
    }

    private static void compilerToolsTest() {
        final Context context = new Context();
        final JavacFileManager fileManager = new JavacFileManager(context, true, Charset.defaultCharset());
        final com.sun.tools.javac.code.Types types = com.sun.tools.javac.code.Types.instance(context);
        final TypeMaker typeMaker = TypeMaker.instance(context);
        final Resolve resolve = Resolve.instance(context);
        final Names names = Names.instance(context);
        final JavaCompiler compiler = JavaCompiler.instance(context);
        final com.sun.tools.javac.code.Type mapType = compiler.resolveIdent("java.util.HashMap").asType();
        final java.util.List<Symbol> elements = mapType.getTypeArguments().get(0).asElement().getEnclosingElement().getEnclosedElements();
        final JavacTypes javacTypes = JavacTypes.instance(context);

        final com.sun.tools.javac.code.Type.ClassType declaredType = (com.sun.tools.javac.code.Type.ClassType)
            javacTypes.getDeclaredType(
                (TypeElement)mapType.asElement(),
                compiler.resolveIdent("java.lang.String").asType(),
                compiler.resolveIdent("java.util.Date").asType()
            );

        final Symbol.MethodSymbol methodDefinition = (Symbol.MethodSymbol)declaredType.asElement().getEnclosedElements().get(49);

        final com.sun.tools.javac.code.Type.MethodType genericMethod = (com.sun.tools.javac.code.Type.MethodType)
            javacTypes.asMemberOf(
                declaredType,
                methodDefinition
            );

        final DeclaredType entryType = javacTypes.getDeclaredType(
            (TypeElement)declaredType.asElement().getEnclosedElements().get(0),
            (TypeMirror[])declaredType.getTypeArguments().toArray(new TypeMirror[declaredType.getTypeArguments().size()])
        );

        final Symbol put = resolve.resolveInternalMethod(
            null,
            new Env<>(null, new AttrContext()),
            declaredType,
            names.fromString("put"),
            List.of(
                compiler.resolveIdent("java.lang.String").asType(),
                compiler.resolveIdent("java.sql.Timestamp").asType()
            ),
            List.<com.sun.tools.javac.code.Type>nil()
        );

        final MethodType putWithTypeArgs = ((com.sun.tools.javac.code.Type)javacTypes
            .asMemberOf(declaredType, put))
            .asMethodType();

        System.out.println(declaredType);
        System.out.println(putWithTypeArgs.asMethodType());
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
}

interface ITest {
    String testNumber(final int number);
}