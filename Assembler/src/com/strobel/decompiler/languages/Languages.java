/*
 * Languages.java
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

package com.strobel.decompiler.languages;

import com.strobel.core.ArrayUtilities;
import com.strobel.decompiler.languages.java.JavaLanguage;

import java.util.List;

public final class Languages {
    private final static List<Language> ALL_LANGUAGES;
    private final static List<Language> DEBUG_LANGUAGES;
    private final static Language JAVA;
    private final static Language BYTECODE_AST_UNOPTIMIZED;
    private final static Language BYTECODE_AST;

    static {
        final List<BytecodeAstLanguage> bytecodeAstLanguages = BytecodeAstLanguage.getDebugLanguages();

        JAVA = new JavaLanguage();
        BYTECODE_AST_UNOPTIMIZED = bytecodeAstLanguages.get(0);
        BYTECODE_AST = new BytecodeAstLanguage();

        final Language[] languages = new Language[bytecodeAstLanguages.size() + 1];

        languages[0] = JAVA;

        for (int i = 1; i < languages.length; i++) {
            languages[i] = bytecodeAstLanguages.get(i - 1);
        }

        ALL_LANGUAGES = ArrayUtilities.asUnmodifiableList(JAVA, BYTECODE_AST, BYTECODE_AST_UNOPTIMIZED);
        DEBUG_LANGUAGES = ArrayUtilities.asUnmodifiableList(languages);
    }

    public static List<Language> all() {
        return ALL_LANGUAGES;
    }

    public static List<Language> debug() {
        return DEBUG_LANGUAGES;
    }

    public static Language java() {
        return JAVA;
    }

    public static Language bytecodeAst() {
        return BYTECODE_AST;
    }

    public static Language bytecodeAstUnoptimized() {
        return BYTECODE_AST_UNOPTIMIZED;
    }
}
