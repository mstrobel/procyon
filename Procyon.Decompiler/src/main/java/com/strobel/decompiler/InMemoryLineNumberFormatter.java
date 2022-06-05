/*
 * InMemoryLineNumberFormatter.java
 *
 * Copyright (c) 2013-2022 Mike Strobel and other contributors
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler;

import com.strobel.decompiler.languages.LineNumberPosition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;
import java.util.List;

/**
 * An <code>InMemoryLineNumberFormatter</code> is used to rewrite an existing .java source, introducing
 * line number information.  It can handle either, or both, of the following jobs:
 * 
 * <ul>
 * <li>Introduce line numbers as leading comments.
 *   <li>Stretch the source so that the line number comments match the physical lines.
 * </ul>
 */
public class InMemoryLineNumberFormatter extends AbstractLineNumberFormatter {

    private final String _source;

    /**
     * Constructs an instance.
     * 
     * @param source                the source whose line numbers should be fixed
     * @param lineNumberPositions a recipe for how to fix the line numbers in source.
     * @param options controls how 'this' represents line numbers in the resulting source
     */
    public InMemoryLineNumberFormatter(String source,
            List<LineNumberPosition> lineNumberPositions,
            EnumSet<LineNumberOption> options) {
        super(lineNumberPositions, options);
        _source = source;
    }

    @Override
    protected BufferedReader createReader() throws IOException {
        return new BufferedReader(new StringReader(_source));
    }
}
