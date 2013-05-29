![](logo.png)

*Procyon* is a suite of Java metaprogramming tools focused on code generation and analysis.  It includes the following libraries:

  1. [Core Framework](#procyon-core)
  2. [Reflection Framework](#procyon-reflection)
  3. [Expressions Framework](#procyon-expressions)
  4. [Compiler Toolset (Experimental)](#procyon-compilertools)
  5. [Decompiler Front-End (Experimental)](#procyon-decompiler)

### <a id="#procyon-core"></a>Core Framework

The `procyon-core` framework contains common support classes used by the other Procyon APIs.  Its facilities include string manipulation, collection extensions, filesystem/path utilities, freezable objects and collections, attached data stores, and some runtime type helpers.

### <a id="#procyon-reflection"></a>Reflection Framework
The `procyon-reflection` framework provides a rich reflection and code generation API with full support for generics, wildcards, and other high-level Java type concepts.  It is based on .NET's `System.Reflection` and `System.Reflection.Emit` APIs and is meant to address many of the shortcomings of the core Java reflection API, which offers rather limited and cumbersome support for generic type inspection.  Its code generation facilities include a `TypeBuilder`, `MethodBuilder`, and a bytecode emitter.

#### Example

	:::java
    final Type<Map> map = Type.of(Map.class);
    final Type<?> rawMap = map.getErasedType();
    final Type<Map<String, Integer>> boundMap = map.makeGenericType(Types.String, Types.Integer);
    
    System.out.println(map.getDeclaredMethods().get(1));
    System.out.println(rawMap.getDeclaredMethods().get(1));
    System.out.println(boundMap.getDeclaredMethods().get(1));
    
    System.out.println(boundMap.getGenericTypeParameters());
    System.out.println(boundMap.getTypeArguments());

#### Output

    :::text
    public abstract V put(K, V)
    public abstract Object put(Object, Object)
    public abstract Integer put(String, Integer)
    [K, V]
    [java.lang.String, java.lang.Integer]

### <a id="#procyon-expressions"></a>Expressions Framework

The `procyon-expressions` framework provides a more natural form of code generation.
Rather than requiring bytecode to be emitted directly, as with `procyon-reflection`
and other popular libraries like ASM, `procyon-expressions` enables code composition
using declarative expression trees.  These expression trees may then be compiled directly
into callbacks or coupled with a `MethodBuilder`.  The `procyon-expressions` API is
almost a direct port of `System.Linq.Expressions` from .NET's Dynamic Language Runtime,
minus the dynamic callsite support (and with more relaxed rules regarding type conversions).

#### Example

    :::java
    final ParameterExpression item = variable(Types.String, "item");

    final LambdaExpression<Runnable> runnable = lambda(
        Type.of(Runnable.class),
        forEach(
            item,
            constant(new String[] { "one", "two", "three", "four", "five" }),
            call(
                field(null, Types.System.getField("out")),
                "printf",
                constant("Got item: %s\n"),
                item
            )
        )
    );

    final Runnable callback = runnable.compile();

    callback.run(); 

#### Output

    Got item: one
    Got item: two
    Got item: three
    Got item: four
    Got item: five

### <a id="#procyon-compilertools"></a>Compiler Toolset

The `procyon-compilertools` project is a work in progress that includes:

  1. Class metadata and bytecode inspection/manipulation facilities based on `Mono.Cecil`
  2. An optimization and decompiler framework based on `ILSpy`

The Compiler Toolset is still early in development and subject to change.

### <a id="#procyon-decompiler"></a>Decompiler Front-End

`procyon-decompiler` is a standalone front-end for the Java decompiler included in
`procyon-compilertools`.  All dependencies are embedded in the JAR for easy redistribution.
For more information about the decompiler, see the [Java Decompiler](
https://bitbucket.org/mstrobel/procyon/wiki/Java%20Decompiler) wiki page.