package com.strobel.assembler.ir;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.CodeAttribute;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.core.VerifyArgument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mike Strobel
 */
final class ClassFile implements ClassReader<IMetadataScope> {
    final static long MAGIC = 0xCAFEBABEL;

    final long magic;
    final int majorVersion;
    final int minorVersion;
    final File file;
    final Buffer buffer;
    final ConstantPool constantPool;
    final int accessFlags;
    final ConstantPool.TypeInfoEntry thisClassEntry;
    final ConstantPool.TypeInfoEntry baseClassEntry;
    final ConstantPool.TypeInfoEntry[] interfaceEntries;
    final List<FieldInfo> fields;
    final List<MethodInfo> methods;
    final List<SourceAttribute> attributes;

    private final AtomicBoolean _completed;
    private TypeSystem _typeSystem;

    private ClassFile(
        final long magic,
        final int majorVersion,
        final int minorVersion,
        final File file,
        final Buffer buffer,
        final ConstantPool constantPool,
        final int accessFlags,
        final ConstantPool.TypeInfoEntry thisClassEntry,
        final ConstantPool.TypeInfoEntry baseClassEntry,
        final ConstantPool.TypeInfoEntry[] interfaceEntries) {

        this.magic = magic;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.file = file;
        this.buffer = buffer;
        this.constantPool = constantPool;
        this.accessFlags = accessFlags;
        this.thisClassEntry = VerifyArgument.notNull(thisClassEntry, "thisClassEntry");
        this.baseClassEntry = VerifyArgument.notNull(baseClassEntry, "baseClassEntry");
        this.interfaceEntries = VerifyArgument.notNull(interfaceEntries, "interfaceEntries");
        this.attributes = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();

        _completed = new AtomicBoolean();
    }

    final synchronized void complete(final TypeDefinition typeDefinition, final TypeSystem typeSystem) {
/*
        if (_completed.getAndSet(true)) {
            return;
        }

        _typeSystem = typeSystem;

        final int fieldCount = buffer.readUnsignedShort();

        for (int i = 0; i < fieldCount; i++) {
            final int accessFlags = buffer.readUnsignedShort();

            final ConstantPool.Utf8StringConstantEntry name = (ConstantPool.Utf8StringConstantEntry) constantPool.get(
                buffer.readUnsignedShort(),
                ConstantPool.Tag.Utf8StringConstant
            );

            final ConstantPool.Utf8StringConstantEntry descriptor = (ConstantPool.Utf8StringConstantEntry) constantPool.get(
                buffer.readUnsignedShort(),
                ConstantPool.Tag.Utf8StringConstant
            );

            final SourceAttribute[] attributes;
            final int attributeCount = buffer.readUnsignedShort();

            if (attributeCount > 0) {
                attributes = new SourceAttribute[attributeCount];
                readAttributes(scope, buffer, attributes);
            }
            else {
                attributes = EmptyArrayCache.fromElementType(SourceAttribute.class);
            }
        }

        buffer.reset(0);
*/
    }

    static ClassFile load(final File file) {
        try (final InputStream inputStream = new FileInputStream(file)) {
            final Buffer input;
            final ClassFile classFile;

            final byte[] data;

            try {
                data = new byte[inputStream.available()];
                inputStream.read(data);
                input = new Buffer(data);
                classFile = readClass(file, input);
                inputStream.close();
            }
            catch (IOException e) {
                throw new UndeclaredThrowableException(e);
            }

            return classFile;
        }
        catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    static ClassFile readClass(final File file, final Buffer b) throws IOException {
        final long magic = b.readInt() & 0xFFFFFFFFL;

        if (magic != MAGIC) {
            throw new IllegalStateException("Wrong magic number: " + magic);
        }

        final int minorVersion = b.readUnsignedShort();
        final int majorVersion = b.readUnsignedShort();

        final ConstantPool constantPool = ConstantPool.read(b);

        final int accessFlags = b.readUnsignedShort();

        final ConstantPool.TypeInfoEntry thisClass = (ConstantPool.TypeInfoEntry) constantPool.get(b.readUnsignedShort(), ConstantPool.Tag.TypeInfo);
        final ConstantPool.TypeInfoEntry baseClass = (ConstantPool.TypeInfoEntry) constantPool.get(b.readUnsignedShort(), ConstantPool.Tag.TypeInfo);
        final ConstantPool.TypeInfoEntry interfaces[] = new ConstantPool.TypeInfoEntry[b.readUnsignedShort()];

        for (int i = 0; i < interfaces.length; i++) {
            interfaces[i] = (ConstantPool.TypeInfoEntry) constantPool.get(b.readUnsignedShort(), ConstantPool.Tag.TypeInfo);
        }

        return new ClassFile(
            magic,
            majorVersion,
            minorVersion,
            file,
            b,
            constantPool,
            accessFlags,
            thisClass,
            baseClass,
            interfaces
        );
    }

    // <editor-fold defaultstate="collapsed" desc="ClassReader Implementation">

    @Override
    public void accept(final IMetadataScope scope, final ClassVisitor<IMetadataScope> visitor) {
        visitHeader(scope, visitor);
    }

    private void visitHeader(final IMetadataScope scope, final ClassVisitor<IMetadataScope> visitor) {
        final SignatureAttribute signature = SourceAttribute.find(AttributeNames.Signature, attributes);
        final String[] interfaceNames = new String[interfaceEntries.length];

        for (int i = 0; i < interfaceEntries.length; i++) {
            interfaceNames[i] = interfaceEntries[i].getName();
        }

        visitor.visit(
            scope,
            majorVersion,
            minorVersion,
            accessFlags,
            thisClassEntry.getName(),
            signature != null ? signature.getSignature() : null,
            baseClassEntry.getName(),
            interfaceNames
        );
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="FieldInfo Class">

    final class FieldInfo {
        final int accessFlags;
        final String name;
        final String descriptor;
        final SourceAttribute[] attributes;

        FieldInfo(final int accessFlags, final String name, final String descriptor, final SourceAttribute[] attributes) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.descriptor = descriptor;
            this.attributes = attributes;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="MethodInfo Class">

    final class MethodInfo {
        final int accessFlags;
        final String name;
        final String descriptor;
        final SourceAttribute[] attributes;
        final CodeAttribute codeAttribute;

        MethodInfo(final int accessFlags, final String name, final String descriptor, final SourceAttribute[] attributes) {
            this.accessFlags = accessFlags;
            this.name = name;
            this.descriptor = descriptor;
            this.attributes = attributes;
            this.codeAttribute = (CodeAttribute) SourceAttribute.find(AttributeNames.Code, attributes);
        }
    }

    // </editor-fold>
}
