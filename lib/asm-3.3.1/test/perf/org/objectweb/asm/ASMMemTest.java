/***
 * ASM performance test: measures the performances of asm package
 * Copyright (c) 2002-2005 France Telecom
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
package org.objectweb.asm;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

/*
 * Created on Nov 30, 2004 as part of ASMPerf by treffer
 */

/**
 * Memory performances tests for tree package.
 * 
 * @author treffer
 */
public class ASMMemTest {

    public static void main(final String[] args) {
        if (args.length < 2) {
            System.out.println("java ASMMemTest <jar-file> <number-of-classes>");
            System.exit(1);
        }

        Runtime runtime = Runtime.getRuntime();
        memDown(runtime);
        System.out.println("Initial memory load: ".concat(memFormat(getUsedMem(runtime))));

        LinkedList fileData = new LinkedList();
        int limit = Integer.parseInt(args[1]);
        try {
            long totalSize = 0;
            JarInputStream jar = new JarInputStream(new FileInputStream(args[0]));
            JarEntry entry = jar.getNextJarEntry();
            while (fileData.size() < limit && entry != null) {
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    if (entry.getSize() != -1) {
                        int len = (int) entry.getSize();
                        byte[] data = new byte[len];
                        jar.read(data);
                        fileData.add(data);
                        totalSize += data.length;
                    } else {
                        System.err.println("No jar-entry size given... Unimplemented, jar file not supported");
                    }
                }
                entry = jar.getNextJarEntry();
            }
            System.out.println(memFormat(totalSize) + " class data, ~"
                    + memFormat(totalSize / limit) + " per class.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList result = new ArrayList(fileData.size());
        long startmem;

        for (int i = 0; i < 10; i++) {
            System.out.println("\n> Run ".concat(Integer.toString(i + 1)));
            Iterator files = fileData.iterator();
            result.clear();
            memDown(runtime);
            System.out.println("Empty memory load: ".concat(memFormat(startmem = getUsedMem(runtime))));

            long time = -System.currentTimeMillis();
            while (files.hasNext()) {
                byte data[] = (byte[]) files.next();
                ClassReader reader = new ClassReader(data);
                ClassNode clazz = new ClassNode();
                reader.accept(clazz, 0);
                result.add(clazz);
            }
            time += System.currentTimeMillis();

            memDown(runtime);
            System.out.println("Time: ".concat(timeFormat(time)));
            System.out.println("Final memory load: ".concat(memFormat(getUsedMem(runtime))));
            System.out.println("ASM memory load: ".concat(memFormat(getUsedMem(runtime)
                    - startmem)));
            for (int j = 0; j < limit; j++) {
                ClassNode clazz = (ClassNode) result.get(j);
                List l = clazz.methods;
                for (int k = 0, lim = l.size(); k < lim; k++) {
                    MethodNode m = (MethodNode) l.get(k);
                    InsnList insn = m.instructions;
                    if (insn != null) {
                        insn.clear();
                    }
                }
            }
            memDown(runtime);
            System.out.println("ASM memory load (removed method code): ".concat(memFormat(getUsedMem(runtime)
                    - startmem)));
        }

    }

    public final static long getUsedMem(final Runtime r) {
        return r.totalMemory() - r.freeMemory();
    }

    public final static String timeFormat(final long time) {
        int min = (int) (time / (60 * 1000));
        int sec = (int) ((time / 1000) % 60);
        int msec = (int) (time % 1000);
        StringBuffer sbuf = new StringBuffer(30);
        if (min > 0) {
            sbuf.append(min);
            sbuf.append("min ");
        }
        if (sec > 0 || min > 0) {
            sbuf.append(sec);
            sbuf.append("s ");
        }
        if (msec > 0 || sec > 0 || min > 0) {
            sbuf.append(msec);
            sbuf.append("ms ");
        }
        sbuf.append('(');
        sbuf.append(time);
        sbuf.append("ms)");
        return sbuf.toString();
    }

    public final static String memFormat(final long mem) {
        int gb = (int) ((mem >> 30) & 0x3FF);
        int mb = (int) ((mem >> 20) & 0x3FF);
        int kb = (int) ((mem >> 10) & 0x3FF);
        int bytes = (int) (mem & 0x3FF);
        StringBuffer sbuf = new StringBuffer(30);
        if (gb > 0) {
            sbuf.append(gb);
            sbuf.append("GB ");
        }
        if (mb > 0 || gb > 0) {
            sbuf.append(mb);
            sbuf.append("MB ");
        }
        if (kb > 0 || mb > 0 || gb > 0) {
            sbuf.append(kb);
            sbuf.append("KB ");
        }
        if (bytes > 0 || kb > 0 || mb > 0 || gb > 0) {
            sbuf.append(bytes);
            sbuf.append("bytes ");
        }
        sbuf.append('(');
        sbuf.append(mem);
        sbuf.append("bytes)");
        return sbuf.toString();
    }

    public final static void memDown(final Runtime r) {
        long oldmem;
        do {
            oldmem = getUsedMem(r);
            for (int i = 0; i < 10; i++) {
                // Calling System.gc once is very unsafe
                System.gc();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {
                }
            }
        } while (getUsedMem(r) < oldmem);
    }
}