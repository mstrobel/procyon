/*
 * ConstantPoolPrinter.java
 *
 * Copyright (c) 2013 Mike Strobel
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

package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.ConstantPool;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;

import static java.lang.String.format;

public class ConstantPoolPrinter implements ConstantPool.Visitor {
    private final static int MAX_TAG_LENGTH;

    static {
        int maxTagLength = 0;

        for (final ConstantPool.Tag tag : ConstantPool.Tag.values()) {
            final int length = tag.name().length();

            if (length > maxTagLength) {
                maxTagLength = length;
            }
        }

        MAX_TAG_LENGTH = maxTagLength;
    }

    private final ITextOutput _printer;
    private boolean _isHeaderPrinted;

    public ConstantPoolPrinter(final ITextOutput printer) {
        _printer = VerifyArgument.notNull(printer, "printer");
    }

    protected void printTag(final ConstantPool.Tag tag) {
        _printer.write("%1$-" + MAX_TAG_LENGTH + "s  ", tag);
    }

    @Override
    public void visit(final ConstantPool.Entry entry) {
        VerifyArgument.notNull(entry, "entry");

        if (!_isHeaderPrinted) {
            _printer.write("Constant Pool:");
            _printer.writeLine();
            _isHeaderPrinted = true;
        }

        _printer.write("  %1$5d: ", entry.index);
        printTag(entry.getTag());
        entry.accept(this);
        _printer.writeLine();
    }

    @Override
    public void visitTypeInfo(final ConstantPool.TypeInfoEntry info) {
        _printer.write("#%1$-13d //  %2$s", info.nameIndex, info.getName());
    }

    @Override
    public void visitDoubleConstant(final ConstantPool.DoubleConstantEntry info) {
        _printer.write("%1$-13s", info.getConstantValue());
    }

    @Override
    public void visitFieldReference(final ConstantPool.FieldReferenceEntry info) {
        final ConstantPool.NameAndTypeDescriptorEntry nameAndTypeInfo = info.getNameAndTypeInfo();

        _printer.write(
            "%1$-14s //  %2$s.%3$s:%4$s",
            format(
                "#%1$d.#%2$d",
                info.typeInfoIndex,
                info.nameAndTypeDescriptorIndex
            ),
            info.getClassName(),
            nameAndTypeInfo.getName(),
            nameAndTypeInfo.getType()
        );
    }

    @Override
    public void visitFloatConstant(final ConstantPool.FloatConstantEntry info) {
        _printer.write("%1$-13s", info.getConstantValue());
    }

    @Override
    public void visitIntegerConstant(final ConstantPool.IntegerConstantEntry info) {
        _printer.write("%1$-13s", info.getConstantValue());
    }

    @Override
    public void visitInterfaceMethodReference(final ConstantPool.InterfaceMethodReferenceEntry info) {
        final ConstantPool.NameAndTypeDescriptorEntry nameAndTypeInfo = info.getNameAndTypeInfo();

        _printer.write(
            "%1$-14s //  %2$s.%3$s:%4$s",
            format(
                "#%1$d.#%2$d",
                info.typeInfoIndex,
                info.nameAndTypeDescriptorIndex
            ),
            info.getClassName(),
            nameAndTypeInfo.getName(),
            nameAndTypeInfo.getType()
        );
    }

    @Override
    public void visitInvokeDynamicInfo(final ConstantPool.InvokeDynamicInfoEntry info) {
        _printer.write("%1$-13s", info.bootstrapMethodAttributeIndex);
        info.getNameAndTypeDescriptor().accept(this);
    }

    @Override
    public void visitLongConstant(final ConstantPool.LongConstantEntry info) {
        _printer.write("%1$-13s", info.getConstantValue());
    }

    @Override
    public void visitNameAndTypeDescriptor(final ConstantPool.NameAndTypeDescriptorEntry info) {
        _printer.write(
            "%1$-14s //  %2$s:%3$s",
            format(
                "#%1$d.#%2$d",
                info.nameIndex,
                info.typeDescriptorIndex
            ),
            info.getName(),
            info.getType()
        );
    }

    @Override
    public void visitMethodReference(final ConstantPool.MethodReferenceEntry info) {
        final ConstantPool.NameAndTypeDescriptorEntry nameAndTypeInfo = info.getNameAndTypeInfo();

        _printer.write(
            "%1$-14s //  %2$s.%3$s:%4$s",
            format(
                "#%1$d.#%2$d",
                info.typeInfoIndex,
                info.nameAndTypeDescriptorIndex
            ),
            info.getClassName(),
            nameAndTypeInfo.getName(),
            nameAndTypeInfo.getType()
        );
    }

    @Override
    public void visitMethodHandle(final ConstantPool.MethodHandleEntry info) {
        _printer.write("%1$s ", info.referenceKind);
        info.getReference().accept(this);
    }

    @Override
    public void visitMethodType(final ConstantPool.MethodTypeEntry info) {
        _printer.write("%1$-13s", info.getType());
    }

    @Override
    public void visitStringConstant(final ConstantPool.StringConstantEntry info) {
        _printer.write(
            "#%1$-13s //  \"%2$s\"",
            info.stringIndex,
            JavaOutputVisitor.convertString(info.getValue())
        );
    }

    @Override
    public void visitUtf8StringConstant(final ConstantPool.Utf8StringConstantEntry info) {
        _printer.write(
            "\"%1$s\"",
            JavaOutputVisitor.convertString((String) info.getConstantValue())
        );
    }

    @Override
    public void visitEnd() {
    }
}
