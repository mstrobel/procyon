/***
 * ASM tests
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
package org.objectweb.asm.attrs;

import java.util.Map;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.util.ASMifiable;
import org.objectweb.asm.util.Traceable;

/**
 * A non standard code attribute used for testing purposes.
 * 
 * @author Eric Bruneton
 */
public class CodeComment extends Attribute implements ASMifiable, Traceable {

    public CodeComment() {
        super("CodeComment");
    }

    public boolean isUnknown() {
        return false;
    }

    public boolean isCodeAttribute() {
        return true;
    }

    protected Attribute read(
        final ClassReader cr,
        final int off,
        final int len,
        final char[] buf,
        final int codeOff,
        final Label[] labels)
    {

        return new CodeComment();
    }

    protected ByteVector write(
        final ClassWriter cw,
        final byte[] code,
        final int len,
        final int maxStack,
        final int maxLocals)
    {
        return new ByteVector();
    }

    protected Label[] getLabels() {
        super.getLabels();
        return new Label[] { new Label() };
    }

    public void asmify(
        final StringBuffer buf,
        final String varName,
        final Map labelNames)
    {
        buf.append("Attribute ")
                .append(varName)
                .append(" = new org.objectweb.asm.attrs.CodeComment();");
    }

    public void trace(final StringBuffer buf, final Map labelNames) {
    }
}
