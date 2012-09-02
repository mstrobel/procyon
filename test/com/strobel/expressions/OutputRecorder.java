package com.strobel.expressions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Queue;

/**
 * @author Mike Strobel
 */
public final class OutputRecorder extends ByteArrayOutputStream {
    private final PrintStream stdOut;
    private final Queue<Object> output;
    private final char[] temp = new char[255];
    private final StringBuilder sb = new StringBuilder();
    private int readPosition;

    public OutputRecorder(final PrintStream stdOut, final Queue<Object> output) {
        this.stdOut = stdOut;
        this.output = output;
    }

    @Override
    public void write(final int b) {
        super.write(b);
        recordLines();
        if (stdOut != null) {
            stdOut.write(b);
        }
    }

    @Override
    public void write(final byte[] b) throws IOException {
        super.write(b);
        recordLines();
        if (stdOut != null) {
            stdOut.write(b);
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        super.write(b, off, len);
        recordLines();
        if (stdOut != null) {
            stdOut.write(b, off,  len);
        }
    }

    private void recordLines() {
        final byte[] buffer = super.buf;
        final int size = super.size();

        while (readPosition < size) {
            final int startPosition = readPosition;
            final int length = Math.min(size - readPosition, temp.length);

            if (length == 0) {
                return;
            }

            int i, lastSignificant = -1;

            for (i = 0; i < length; i++) {
                final char ch = (char)buffer[startPosition + i];
                temp[i] = ch;
                if (ch == '\r' || ch == '\n') {
                    if (lastSignificant != -1 || sb.length() != 0) {
                        sb.append(temp, 0, i);
                        lastSignificant = -1;
                        output.add(sb.toString());
                        sb.setLength(0);
                    }
                }
                else if (lastSignificant == -1) {
                    lastSignificant = i;
                }
                ++readPosition;
            }

            final int inBuffer = lastSignificant < 0 ? 0 : i - lastSignificant;

            if (inBuffer > 0) {
                sb.append(temp, lastSignificant, inBuffer);
            }
        }
    }

    public void reset() {
        super.reset();
        readPosition = 0;
        sb.setLength(0);
    }
}
