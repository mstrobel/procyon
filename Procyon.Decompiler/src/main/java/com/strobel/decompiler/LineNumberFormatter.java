/*
 * LineNumberFormatter.java
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

/**
 * A <code>LineNumberFormatter</code> is used to rewrite an existing .java file, introducing
 * line number information.  It can handle either, or both, of the following jobs:
 * 
 * <ul>
 * <li>Introduce line numbers as leading comments.
 *   <li>Stretch the file so that the line number comments match the physical lines.
 * </ul>
 */
public class LineNumberFormatter extends AbstractLineNumberFormatter {

    private final File _file;

    /**
     * Constructs an instance.
     * 
     * @param file                the file whose line numbers should be fixed
     * @param lineNumberPositions a recipe for how to fix the line numbers in 'file'.
     * @param options controls how 'this' represents line numbers in the resulting file
     */
    public LineNumberFormatter(File file,
            List<LineNumberPosition> lineNumberPositions,
            EnumSet<LineNumberOption> options) {
        super(lineNumberPositions, options);
        _file = file;
    }

    @Override
    protected BufferedReader createReader() throws IOException {
        return Files.newBufferedReader(_file.toPath());
    }
}
