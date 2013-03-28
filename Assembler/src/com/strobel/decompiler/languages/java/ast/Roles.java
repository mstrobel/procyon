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
    
    public final static Role<AstType> TYPE = new Role<>("Type", AstType.class, AstType.NULL);
    public final static Role<AstType> BASE_TYPE = new Role<>("BaseType", AstType.class, AstType.NULL);
    public final static Role<AstType> TYPE_ARGUMENT = new Role<>("TypeArgument", AstType.class, AstType.NULL);
    public final static Role<TypeParameterDeclaration> TYPE_PARAMETER = new Role<>("TypeParameter", TypeParameterDeclaration.class);
    public final static Role<Expression> ARGUMENT = new Role<>("Argument", Expression.class, Expression.NULL);
    public final static Role<ParameterDeclaration> PARAMETER = new Role<>("Parameter", ParameterDeclaration.class);
    public final static Role<Expression> EXPRESSION = new Role<>("Expression", Expression.class, Expression.NULL);
    public final static Role<Expression> TARGET_EXPRESSION = new Role<>("Target", Expression.class, Expression.NULL);
    public final static Role<Expression> CONDITION = new Role<>("Condition", Expression.class, Expression.NULL);
    public final static Role<Comment> COMMENT = new Role<>("Comment", Comment.class);
    public final static Role<Identifier> IDENTIFIER = new Role<>("Identifier", Identifier.class, Identifier.NULL);
    public final static Role<Statement> EMBEDDED_STATEMENT = new Role<>("EmbeddedStatement", Statement.class, Statement.NULL);
    public final static Role<BlockStatement> BODY = new Role<>("Body", BlockStatement.class, BlockStatement.NULL);
    public final static Role<Annotation> ANNOTATION = new Role<>("Annotation", Annotation.class);
    public final static Role<VariableInitializer> VARIABLE = new Role<>("Variable", VariableInitializer.class, VariableInitializer.NULL);
    public final static Role<EntityDeclaration> TYPE_MEMBER = new Role<>("TypeMember", EntityDeclaration.class);
    public final static Role<PackageDeclaration> PACKAGE = new Role<>("Package", PackageDeclaration.class, PackageDeclaration.NULL);
    public final static Role<NewLineNode> NEW_LINE = new Role<>("NewLine", NewLineNode.class);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // COMMON TOKENS                                                                                                      //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public final static TokenRole LEFT_PARENTHESIS = new TokenRole("(");
    public final static TokenRole RIGHT_PARENTHESIS = new TokenRole(")");
    public final static TokenRole LEFT_BRACKET = new TokenRole("[");
    public final static TokenRole RIGHT_BRACKET = new TokenRole("]");
    public final static TokenRole LEFT_BRACE = new TokenRole("{");
    public final static TokenRole RIGHT_BRACE = new TokenRole("}");
    public final static TokenRole LEFT_CHEVRON = new TokenRole("<");
    public final static TokenRole RIGHT_CHEVRON = new TokenRole(">");
    public final static TokenRole COMMA = new TokenRole(",");
    public final static TokenRole DOT = new TokenRole(".");
    public final static TokenRole SEMICOLON = new TokenRole(";");
    public final static TokenRole COLON = new TokenRole(":");
    public final static TokenRole DOUBLE_COLON = new TokenRole("::");
    public final static TokenRole ASSIGN = new TokenRole("=");
    public final static TokenRole PIPE = new TokenRole("|");

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // KEYWORD TOKENS                                                                                                     //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public final static TokenRole PACKAGE_KEYWORD = new TokenRole("package");
    public final static TokenRole ENUM_KEYWORD = new TokenRole("enum");
    public final static TokenRole INTERFACE_KEYWORD = new TokenRole("interface");
    public final static TokenRole CLASS_KEYWORD = new TokenRole("class");
    public final static TokenRole ANNOTATION_KEYWORD = new TokenRole("@interface");
    public final static TokenRole EXTENDS_KEYWORD = new TokenRole("extends");
    public final static TokenRole IMPLEMENTS_KEYWORD = new TokenRole("implements");
}
