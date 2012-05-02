/***
 * ASM examples: examples showing how ASM can be used
 * Copyright (c) 2000-2007 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Eric Bruneton
 */
public class ArraySet {

    private int[] values = new int[3];

    private int size;

    public boolean contains(final int v) {
        for (int i = 0; i < size; ++i) {
            if (values[i] == v) {
                return true;
            }
        }
        return false;
    }

    public void add(final int v) {
        if (!contains(v)) {
            if (size == values.length) {
                System.err.println("[enlarge]");
                int[] newValues = new int[values.length + 3];
                System.arraycopy(values, 0, newValues, 0, size);
                values = newValues;
            }
            values[size++] = v;
        }
    }

    public void remove(final int v) {
        int i = 0;
        int j = 0;
        while (i < size) {
            int u = values[i];
            if (u != v) {
                values[j++] = u;
            }
            ++i;
        }
        size = j;
    }

    // test method

    public static void main(final String[] args) {
        ArraySet s = new ArraySet();
        System.err.println("add 1");
        s.add(1);
        System.err.println("add 1");
        s.add(1);
        System.err.println("add 2");
        s.add(2);
        System.err.println("add 4");
        s.add(4);
        System.err.println("add 8");
        s.add(8);
        System.err.println("contains 3 = " + s.contains(3));
        System.err.println("contains 1 = " + s.contains(1));
        System.err.println("remove 1");
        s.remove(1);
        System.err.println("contains 1 = " + s.contains(1));
    }
}
