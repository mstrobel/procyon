Overview
--------

What is `com.strobel`?  It's a suite of reflection and code generation capabilities based on some of my favorite .NET APIs.  Much of the work I do these days involves runtime code generation, both on the Microsoft .NET and Java platforms.  On the .NET side, APIs such as LINQ/DLR expression trees eliminate the need to concern oneself with individual IL instructions and instead focus on composing code in terms of higher-level constructs.  Unfortunately, the Java frameworks I have found relating to code generation are painfully cumbersome to use and generally require emitting low-level bytecode instructions.  This project is the culmination of many weekends spent trying to develop a better way to compose JVM code at runtime.  It's not finished yet, but it's far enough along that I'm ready to unveil it.

There are essentially three layers to this project:

  1. A comprehensive reflection API based on `System.Reflection`, providing full support for generics.

  2. A code generation API based on `System.Reflection.Emit`, enabling dynamic generation of types and methods.  Contains a low-level bytecode emitter.
  
  3. A high-level API based on `System.Linq.Expressions`, enabling runtime code composition in terms of declarative expression trees.  Can be used in combination with `TypeBuilder` to implement method bodies.

### com.strobel.reflection

I wanted my expression tree API to enforce the same level of type safety as LINQ/DLR trees.  Unfortunately, the Java reflection APIs fail to expose concepts such as generics in a way that is easy to digest.  For example, `java.lang.Class` cannot be used to represent a parameterized generic type.  Since the JDK's reflection APIs lack the richness of the Java language's type system, I had to build a better reflection API.  I decided to borrow the design from Microsoft's .NET Framework, since I am intimately familiar with it already, and has always worked well for me.

All types are exposed through a single class `Type` class.  `Type` can represent classes, interfaces, generic parameters, wildcards, and captures.  Obtaining a `Type` is very straightforward:  
  
    Type<String> string = Type.of(String.class);

For faster lookups, many common type definitions are exposed as constants in the `Types` and `PrimitiveTypes` classes.  And what about generic types?  How would one obtain the type of `List<T>`?  What about `List<String>`, or `List<? extends Comparable>`?  
  
    :::java
    // Unbound List<T> generic type definition:
    Type<List> list = Types.List;
    
    // List<String>:
    Type<?> list2 = list.makeGenericType(Types.String));
    
    // List<? extends Comparable>
    Type<?> list3 = list.makeGenericType(Type.makeExtendsWildcard(Types.Comparable)));

The `Type` class provides most of the facilities of `System.Type`, including those for inspection and invocation of declared and inherited fields, methods, and constructors.  In a break from Java tradition, **no methods in these APIs throw checked exceptions**.

### com.strobel.expressions

This is where the good stuff is.  It's almost an exact port of `System.Linq.Expressions`, but with more relaxed rules related to type compatibility.  The API consists of a comprehensive set of expression types and a set of factory methods for constructing them.  Consider the example below, which represents a for-each loop over an array of strings that prints a message for each string.  
  
    :::java
    final ParameterExpression item = variable(Types.String, "item");

    final ConstantExpression items = constant(
        new String[] { "one", "two", "three", "four", "five" }
    );

    final LambdaExpression<Runnable> runnable = lambda(
        Type.of(Runnable.class),
        forEach(
            item,
            items,
            call(
                field(null, Types.System.getField("out")),
                "printf",
                constant("Got item: %s\n"),
                item
            )
        )
    );

    final Runnable delegate = runnable.compile();

    delegate.run();

A couple things to note here.  `LambdaExpression` is the top-level expression type in this API.  It represents a function that can be invoked.  The type of a `LambdaExpression` is always a single-method interface.  It can be compiled directly to an instance of that interface.  Alternatively, it can be compiled to a `MethodHandle`, or as the body of a static or instance method of some other type by combining it with a `MethodBuilder`.

When compiled directly, a `LambdaExpression` may include complex constants that cannot normally be represented directly in bytecode.  In the example above, the generated `Runnable` internally retains a reference to the string array `items` by means of a closure.  Note that a `LambdaExpression` compiled to a `MethodBuilder` cannot utilize closures, so only primitive, class, and string constants may be referenced.

Back to our example: does this really work?  Let's see the output...
  
    Got item: one
    Got item: two
    Got item: three
    Got item: four
    Got item: five

Not bad.  And compare the lambda body above to the bytecode I would have otherwise had to emit manually:

     0: aload_0
     1: getfield      #12    // $__closure:Lcom/strobel/compilerservices/Closure;
     4: getfield      #18    // com/strobel/compilerservices/Closure.constants:[Ljava/lang/Object;
     7: iconst_0
     8: aaload
     9: checkcast     #20    // java/lang/String;
    12: astore_1
    13: aload_1
    14: arraylength
    15: istore_3
    16: iconst_0
    17: istore_2
    18: iload_2
    19: iload_3
    20: if_icmplt     24
    23: return
    24: aload_1
    25: iload_2
    26: aaload
    27: astore        4
    29: getstatic     #26    // java/lang/System.out:Ljava/io/PrintStream;
    32: ldc           #28    // "Got item: %s\n"
    34: iconst_1
    35: anewarray     #2     // java/lang/Object
    38: dup
    39: iconst_0
    40: aload         4
    42: checkcast     #2     // java/lang/Object
    45: aastore
    46: invokevirtual #34    // java/io/PrintStream.printf:...
    49: pop
    50: iinc          2, 1
    53: goto          18
    56: return

I think the difference speaks for itself.