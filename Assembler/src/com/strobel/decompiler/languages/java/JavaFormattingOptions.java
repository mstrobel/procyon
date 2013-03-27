/*
 * JavaFormattingOptions.java
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
package com.strobel.decompiler.languages.java;

@SuppressWarnings("PublicField")
public class JavaFormattingOptions {
    public boolean IndentNamespaceBody;
    public boolean IndentClassBody;
    public boolean IndentInterfaceBody;
    public boolean IndentEnumBody;
    public boolean IndentMethodBody;
    public boolean IndentBlocks;
    public boolean IndentSwitchBody;
    public boolean IndentCaseBody;
    public boolean IndentBreakStatements;
    public boolean AlignEmbeddedUsingStatements;
    public boolean AlignEmbeddedIfStatements;
    public BraceStyle NamespaceBraceStyle;
    public BraceStyle ClassBraceStyle;
    public BraceStyle InterfaceBraceStyle;
    public BraceStyle EnumBraceStyle;
    public BraceStyle MethodBraceStyle;
    public BraceStyle ConstructorBraceStyle;
    public BraceStyle DestructorBraceStyle;
    public BraceStyle EventBraceStyle;
    public BraceStyle EventAddBraceStyle;
    public BraceStyle EventRemoveBraceStyle;
    public boolean AllowEventAddBlockInline;
    public boolean AllowEventRemoveBlockInline;
    public BraceStyle StatementBraceStyle;
    public boolean AllowIfBlockInline;
    public BraceEnforcement IfElseBraceEnforcement;
    public BraceEnforcement ForBraceEnforcement;
    public BraceEnforcement ForEachBraceEnforcement;
    public BraceEnforcement WhileBraceEnforcement;
    public BraceEnforcement UsingBraceEnforcement;
    public BraceEnforcement FixedBraceEnforcement;
    public boolean PlaceElseOnNewLine;
    public boolean PlaceElseIfOnNewLine;
    public boolean PlaceCatchOnNewLine;
    public boolean PlaceFinallyOnNewLine;
    public boolean PlaceWhileOnNewLine;
    public boolean SpaceBeforeMethodDeclarationParentheses;
    public boolean SpaceBetweenEmptyMethodDeclarationParentheses;
    public boolean SpaceBeforeMethodDeclarationParameterComma;
    public boolean SpaceAfterMethodDeclarationParameterComma;
    public boolean SpaceWithinMethodDeclarationParentheses;
    public boolean SpaceBeforeMethodCallParentheses;
    public boolean SpaceBetweenEmptyMethodCallParentheses;
    public boolean SpaceBeforeMethodCallParameterComma;
    public boolean SpaceAfterMethodCallParameterComma;
    public boolean SpaceWithinMethodCallParentheses;
    public boolean SpaceBeforeFieldDeclarationComma;
    public boolean SpaceAfterFieldDeclarationComma;
    public boolean SpaceBeforeLocalVariableDeclarationComma;
    public boolean SpaceAfterLocalVariableDeclarationComma;
    public boolean SpaceBeforeConstructorDeclarationParentheses;
    public boolean SpaceBetweenEmptyConstructorDeclarationParentheses;
    public boolean SpaceBeforeConstructorDeclarationParameterComma;
    public boolean SpaceAfterConstructorDeclarationParameterComma;
    public boolean SpaceWithinConstructorDeclarationParentheses;
    public boolean SpaceBeforeIndexerDeclarationBracket;
    public boolean SpaceWithinIndexerDeclarationBracket;
    public boolean SpaceBeforeIndexerDeclarationParameterComma;
    public boolean SpaceAfterIndexerDeclarationParameterComma;
    public boolean SpaceBeforeDelegateDeclarationParentheses;
    public boolean SpaceBetweenEmptyDelegateDeclarationParentheses;
    public boolean SpaceBeforeDelegateDeclarationParameterComma;
    public boolean SpaceAfterDelegateDeclarationParameterComma;
    public boolean SpaceWithinDelegateDeclarationParentheses;
    public boolean SpaceBeforeNewParentheses;
    public boolean SpaceBeforeIfParentheses;
    public boolean SpaceBeforeWhileParentheses;
    public boolean SpaceBeforeForParentheses;
    public boolean SpaceBeforeForeachParentheses;
    public boolean SpaceBeforeCatchParentheses;
    public boolean SpaceBeforeSwitchParentheses;
    public boolean SpaceBeforeLockParentheses;
    public boolean SpaceBeforeUsingParentheses;
    public boolean SpaceAroundAssignment;
    public boolean SpaceAroundLogicalOperator;
    public boolean SpaceAroundEqualityOperator;
    public boolean SpaceAroundRelationalOperator;
    public boolean SpaceAroundBitwiseOperator;
    public boolean SpaceAroundAdditiveOperator;
    public boolean SpaceAroundMultiplicativeOperator;
    public boolean SpaceAroundShiftOperator;
    public boolean SpaceAroundNullCoalescingOperator;
    public boolean SpacesWithinParentheses;
    public boolean SpacesWithinIfParentheses;
    public boolean SpacesWithinWhileParentheses;
    public boolean SpacesWithinForParentheses;
    public boolean SpacesWithinForeachParentheses;
    public boolean SpacesWithinCatchParentheses;
    public boolean SpacesWithinSwitchParentheses;
    public boolean SpacesWithinLockParentheses;
    public boolean SpacesWithinUsingParentheses;
    public boolean SpacesWithinCastParentheses;
    public boolean SpacesWithinNewParentheses;
    public boolean SpacesBetweenEmptyNewParentheses;
    public boolean SpaceBeforeNewParameterComma;
    public boolean SpaceAfterNewParameterComma;
    public boolean SpaceBeforeConditionalOperatorCondition;
    public boolean SpaceAfterConditionalOperatorCondition;
    public boolean SpaceBeforeConditionalOperatorSeparator;
    public boolean SpaceAfterConditionalOperatorSeparator;
    public boolean SpacesWithinBrackets;
    public boolean SpacesBeforeBrackets;
    public boolean SpaceBeforeBracketComma;
    public boolean SpaceAfterBracketComma;
    public boolean SpaceBeforeForSemicolon;
    public boolean SpaceAfterForSemicolon;
    public boolean SpaceAfterTypecast;
    public boolean SpaceBeforeArrayDeclarationBrackets;
    public boolean SpaceInNamedArgumentAfterDoubleColon;
    public int BlankLinesBeforeImports;
    public int BlankLinesAfterImports;
    public int BlankLinesBeforeFirstDeclaration;
    public int BlankLinesBetweenTypes;
    public int BlankLinesBetweenFields;
    public int BlankLinesBetweenEventFields;
    public int BlankLinesBetweenMembers;
    public boolean KeepCommentsAtFirstColumn;
    public Wrapping ArrayInitializerWrapping;
    public BraceStyle ArrayInitializerBraceStyle;
}