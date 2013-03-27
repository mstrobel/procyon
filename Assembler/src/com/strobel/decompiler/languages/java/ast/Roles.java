/*
 * Roles.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.decompiler.patterns.Role;

public final class Roles {
    public final static Role<AstNode> Root = AstNode.ROOT_ROLE;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Common Roles                                                                                                       //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public final static Role<AstType> Type = new Role<>("Type", AstType.class, AstType.NULL);
    public final static Role<AstType> TypeArgument = new Role<>("TypeArgument", AstType.class, AstType.NULL);
    public final static Role<Expression> Argument = new Role<>("Argument", Expression.class, Expression.NULL);
    public final static Role<Expression> TargetExpression = new Role<>("Target", Expression.class, Expression.NULL);
    public final static Role<Expression> Condition = new Role<>("Condition", Expression.class, Expression.NULL);
    public final static Role<Comment> Comment = new Role<>("Comment", Comment.class);
    public final static Role<Identifier> Identifier = new Role<>("Identifier", Identifier.class, com.strobel.decompiler.languages.java.ast.Identifier.NULL);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // COMMON TOKENS                                                                                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public final static TokenRole LeftParenthesis = new TokenRole("("); 
    public final static TokenRole RightParenthesis = new TokenRole(")");
    public final static TokenRole LeftBracket = new TokenRole("[");
    public final static TokenRole RightBracket = new TokenRole("]");
    public final static TokenRole LeftBrace = new TokenRole("{");
    public final static TokenRole RightBrace = new TokenRole("}");
    public final static TokenRole LeftChevron = new TokenRole("<");
    public final static TokenRole RightChevron = new TokenRole(">");
    public final static TokenRole Comma = new TokenRole(",");
    public final static TokenRole Dot = new TokenRole(".");
    public final static TokenRole Semicolon = new TokenRole(";");
    public final static TokenRole Colon = new TokenRole(":");
    public final static TokenRole DoubleColon = new TokenRole("::");
    public final static TokenRole Assign = new TokenRole("=");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // KEYWORD TOKENS                                                                                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public final static TokenRole PackageKeyword = new TokenRole("package");
    public final static TokenRole EnumKeyword = new TokenRole("enum");
    public final static TokenRole InterfaceKeyword = new TokenRole("interface");
    public final static TokenRole ClassKeyword = new TokenRole("class");
    public final static TokenRole AnnotationKeyword = new TokenRole("@interface");
}
