package com.strobel.assembler;

import java.io.*;

/**
 * @author Mike Strobel
 */
public class CodePrinter extends PrintWriter {
    public CodePrinter(final Writer out) {
        super(out);
    }

    public CodePrinter(final Writer out, final boolean autoFlush) {
        super(out, autoFlush);
    }

    public CodePrinter(final OutputStream out) {
        super(out);
    }

    public CodePrinter(final OutputStream out, final boolean autoFlush) {
        super(out, autoFlush);
    }

    public CodePrinter(final String fileName) throws FileNotFoundException {
        super(fileName);
    }

    public CodePrinter(final String fileName, final String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(fileName, csn);
    }

    public CodePrinter(final File file) throws FileNotFoundException {
        super(file);
    }

    public CodePrinter(final File file, final String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(file, csn);
    }

    public void increaseIndent() {}

    public void decreaseIndent() {}
}
