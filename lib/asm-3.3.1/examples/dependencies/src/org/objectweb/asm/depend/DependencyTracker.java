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
package org.objectweb.asm.depend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.objectweb.asm.ClassReader;

/**
 * DependencyTracker
 * 
 * @author Eugene Kuleshov
 * 
 * @see http://www.onjava.com/pub/a/onjava/2005/08/17/asm3.html
 */
public class DependencyTracker {

    private static final int CELL_PAD = 1;

    private static final int GRID_SIZE = 10;

    private static final int CELLS_SIZE = 8;

    private static final int LABEL_WIDTH = 200;

    private static final String LABEL_FONT = "Tahoma-9";

    public static void main(final String[] args) throws IOException {
        DependencyVisitor v = new DependencyVisitor();

        ZipFile f = new ZipFile(args[0]);

        long l1 = System.currentTimeMillis();
        Enumeration< ? extends ZipEntry> en = f.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            String name = e.getName();
            if (name.endsWith(".class")) {
                new ClassReader(f.getInputStream(e)).accept(v, 0);
            }
        }
        long l2 = System.currentTimeMillis();

        Map<String, Map<String, Integer>> globals = v.getGlobals();
        Set<String> jarPackages = globals.keySet();
        Set<String> classPackages = v.getPackages();
        int size = classPackages.size();
        System.err.println("time: " + (l2 - l1) / 1000f + "  " + size);

        String[] jarNames = jarPackages.toArray(new String[jarPackages.size()]);
        String[] classNames = classPackages.toArray(new String[classPackages.size()]);
        Arrays.sort(jarNames);
        Arrays.sort(classNames);

        buildDiagram(jarNames, classNames, globals);
    }

    public static void buildDiagram(
        final String[] jarNames,
        final String[] classNames,
        final Map<String, Map<String, Integer>> globals) throws IOException
    {
        // normalize
        int max = 0;
        for (int i = 0; i < classNames.length; i++) {
            Map<String, Integer> map = globals.get(classNames[i]);
            if (map == null) {
                continue;
            }
            Integer maxCount = Collections.max(map.values());
            if (maxCount > max) {
                max = maxCount;
            }
        }

        List<Color> colors = new ArrayList<Color>();
        for (int i = LABEL_WIDTH; i >= 0; i--) {
            colors.add(new Color(i, i, 255));
        }
        for (int i = 255; i >= 128; i--) {
            colors.add(new Color(0, 0, i));
        }
        int maxcolor = colors.size() - 1;

        int heigh = CELL_PAD + (CELLS_SIZE + CELL_PAD) * classNames.length;
        int width = CELL_PAD + (CELLS_SIZE + CELL_PAD) * jarNames.length;

        BufferedImage img = new BufferedImage(width + LABEL_WIDTH, heigh
                + LABEL_WIDTH, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width + LABEL_WIDTH, heigh + LABEL_WIDTH);

        // draw lines
        g.setColor(Color.LIGHT_GRAY);
        for (int y = GRID_SIZE; y < classNames.length; y += GRID_SIZE) {
            g.drawLine(0, y * (CELLS_SIZE + CELL_PAD), width, y
                    * (CELLS_SIZE + CELL_PAD));
        }
        for (int x = GRID_SIZE; x < jarNames.length; x += GRID_SIZE) {
            g.drawLine(x * (CELLS_SIZE + CELL_PAD), 0, x
                    * (CELLS_SIZE + CELL_PAD), heigh);
        }

        // draw diagram
        for (int y = 0; y < classNames.length; y++) {
            // System.err.println( y+" : "+classNames[ y]);

            for (int x = 0; x < jarNames.length; x++) {
                Map<String, Integer> map = globals.get(jarNames[x]);

                Integer count = map == null ? null : map.get(classNames[y]);
                if (count != null) {
                    int b = (int) ((float) count * maxcolor / max);

                    g.setColor(colors.get(b));
                    g.fillRect(CELL_PAD + x * (CELLS_SIZE + CELL_PAD),
                            CELL_PAD + y * (CELLS_SIZE + CELL_PAD),
                            CELLS_SIZE,
                            CELLS_SIZE);
                }

            }

        }

        // draw labels
        Font f = Font.decode(LABEL_FONT);
        g.setFont(f);
        // g.setColor( new Color( 70, 70, 255));
        g.setColor(Color.GRAY);

        for (int y = 0; y < classNames.length; y++) {
            AffineTransform trans = g.getTransform();
            g.transform(AffineTransform.getTranslateInstance(CELL_PAD * 2
                    + width, CELLS_SIZE + y * (CELLS_SIZE + CELL_PAD)));
            g.transform(AffineTransform.getRotateInstance(Math.PI / 12));
            g.drawString(classNames[y], 0, 0);
            g.setTransform(trans);
        }

        for (int x = 0; x < jarNames.length; x++) {
            AffineTransform trans = g.getTransform();
            g.transform(AffineTransform.getTranslateInstance(CELL_PAD * 2 + x
                    * (CELLS_SIZE + CELL_PAD), heigh + CELL_PAD * 2));
            g.transform(AffineTransform.getRotateInstance(Math.PI / 2.5));
            g.drawString(jarNames[x], 0, 0);
            g.setTransform(trans);
        }

        FileOutputStream fos = new FileOutputStream("test.png");
        ImageIO.write(img, "png", fos);
        fos.flush();
        fos.close();
    }
}
