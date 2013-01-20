package com.strobel.assembler.ir;

import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.CodeAttribute;
import com.strobel.assembler.ir.attributes.SignatureAttribute;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.*;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.EmptyArrayCache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mike Strobel
 */
final class ClassFile implements ClassReader {
    final static long MAGIC = 0xCAFEBABEL;

    final long magic;
    final int majorVersion;
    final int minorVersion;
    final Buffer buffer;
    final ConstantPool constantPool;
    final int accessFlags;
    final ConstantPool.TypeInfoEntry thisClassEntry;
    final ConstantPool.TypeInfoEntry baseClassEntry;
    final ConstantPool.TypeInfoEntry[] interfaceEntries;
    final List<FieldInfo> fields;
    final List<MethodInfo> methods;
    final List<SourceAttribute> attributes;
    final String name;
    final String packageName;

    private final AtomicBoolean _completed;

    private TypeDefinition _thisType;

    private ClassFile(
        final long magic,
        final int majorVersion,
        final int minorVersion,
        final Buffer buffer,
        final ConstantPool constantPool,
        final int accessFlags,
        final ConstantPool.TypeInfoEntry thisClassEntry,
        final ConstantPool.TypeInfoEntry baseClassEntry,
        final ConstantPool.TypeInfoEntry[] interfaceEntries) {

        this.magic = magic;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.buffer = buffer;
        this.constantPool = constantPool;
        this.accessFlags = accessFlags;
        this.thisClassEntry = VerifyArgument.notNull(thisClassEntry, "thisClassEntry");
        this.baseClassEntry = baseClassEntry;
        this.interfaceEntries = VerifyArgument.notNull(interfaceEntries, "interfaceEntries");
        this.attributes = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();

        final String internalName = thisClassEntry.getName();

        final int delimiter = internalName.lastIndexOf('/');

        if (delimiter < 0) {
            this.packageName = StringUtilities.EMPTY;
            this.name = internalName;
        }
        else {
            this.packageName = internalName.substring(0, delimiter);
            this.name = internalName.substring(delimiter + 1);
        }

        _completed = new AtomicBoolean();
    }

    final synchronized void complete(final TypeDefinition thisType, final IMetadataResolver resolver) {
        if (_completed.getAndSet(true)) {
            return;
        }

        _thisType = thisType;

        final Scope scope = new Scope(resolver);

        final int fieldCount = buffer.readUnsignedShort();

        for (int i = 0; i < fieldCount; i++) {
            final int accessFlags = buffer.readUnsignedShort();

            final String name = constantPool.lookupUtf8Constant(buffer.readUnsignedShort());
            final String descriptor = constantPool.lookupUtf8Constant(buffer.readUnsignedShort());

            final SourceAttribute[] attributes;
            final int attributeCount = buffer.readUnsignedShort();

            if (attributeCount > 0) {
                attributes = new SourceAttribute[attributeCount];
                SourceAttribute.readAttributes(resolver, scope, buffer, attributes);
            }
            else {
                attributes = EmptyArrayCache.fromElementType(SourceAttribute.class);
            }

            final FieldInfo field = new FieldInfo(accessFlags, name, descriptor, attributes);

            fields.add(field);
        }

        final int methodCount = buffer.readUnsignedShort();

        for (int i = 0; i < methodCount; i++) {
            final int accessFlags = buffer.readUnsignedShort();

            final String name = constantPool.lookupUtf8Constant(buffer.readUnsignedShort());
            final String descriptor = constantPool.lookupUtf8Constant(buffer.readUnsignedShort());

            final SourceAttribute[] attributes;
            final int attributeCount = buffer.readUnsignedShort();

            if (attributeCount > 0) {
                attributes = new SourceAttribute[attributeCount];
                SourceAttribute.readAttributes(resolver, scope, buffer, attributes);
            }
            else {
                attributes = EmptyArrayCache.fromElementType(SourceAttribute.class);
            }

            final MethodInfo field = new MethodInfo(accessFlags, name, descriptor, attributes);

            methods.add(field);
        }

        buffer.reset(0);
    }

    static ClassFile readClass(final Buffer b) {
        final long magic = b.readInt() & 0xFFFFFFFFL;

        if (magic != MAGIC) {
            throw new IllegalStateException("Wrong magic number: " + magic);
        }

        final int minorVersion = b.readUnsignedShort();
        final int majorVersion = b.readUnsignedShort();

        final ConstantPool constantPool = ConstantPool.read(b);

        final int accessFlags = b.readUnsignedShort();

        final ConstantPool.TypeInfoEntry thisClass = (ConstantPool.TypeInfoEntry) constantPool.get(b.readUnsignedShort(), ConstantPool.Tag.TypeInfo);
        final ConstantPool.TypeInfoEntry baseClass;

        final int baseClassToken = b.readUnsignedShort();

        if (baseClassToken == 0) {
            baseClass = null;
        }
        else {
            baseClass = constantPool.getEntry(baseClassToken);
        }

        final ConstantPool.TypeInfoEntry interfaces[] = new ConstantPool.TypeInfoEntry[b.readUnsignedShort()];

        for (int i = 0; i < interfaces.length; i++) {
            interfaces[i] = (ConstantPool.TypeInfoEntry) constantPool.get(b.readUnsignedShort(), ConstantPool.Tag.TypeInfo);
        }

        return new ClassFile(
            magic,
            majorVersion,
            minorVersion,
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
    public void accept(final IMetadataResolver resolver, final ClassVisitor<IMetadataScope> visitor) {
        final IMetadataScope scope = new Scope(resolver);

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

    // <editor-fold defaultstate="collapsed" desc="Metadata Scope">

    private class Scope implements IMetadataScope {
        private final MetadataParser _parser;

        Scope(final IMetadataResolver resolver) {
            _parser = new MetadataParser(VerifyArgument.notNull(resolver, "resolver"));
        }

        @Override
        public TypeReference lookupType(final int token) {
            final ConstantPool.Entry entry = constantPool.get(token);

            if (entry instanceof ConstantPool.TypeInfoEntry) {
                final ConstantPool.TypeInfoEntry typeInfo = (ConstantPool.TypeInfoEntry) entry;

                if (typeInfo == thisClassEntry || thisClassEntry.getName().equals(typeInfo.getName())) {
                    return _thisType;
                }

                return _parser.parseTypeDescriptor(typeInfo.getName());
            }

            final String typeName = constantPool.lookupConstant(token);

            if (thisClassEntry.getName().equals(typeName)) {
                return _thisType;
            }

            return _parser.parseTypeDescriptor(typeName);
        }

        @Override
        public FieldReference lookupField(final int token) {
            final ConstantPool.FieldReferenceEntry entry = constantPool.getEntry(token);
            return lookupField(entry.typeInfoIndex, entry.nameAndTypeDescriptorIndex);
        }

        @Override
        public MethodReference lookupMethod(final int token) {
            final ConstantPool.FieldReferenceEntry entry = constantPool.getEntry(token);
            return lookupMethod(entry.typeInfoIndex, entry.nameAndTypeDescriptorIndex);
        }

        @Override
        public FieldReference lookupField(final int typeToken, final int nameAndTypeToken) {
            final ConstantPool.NameAndTypeDescriptorEntry nameAndDescriptor = constantPool.getEntry(nameAndTypeToken);

            return _parser.parseField(
                lookupType(typeToken),
                nameAndDescriptor.getName(),
                nameAndDescriptor.getType()
            );
        }

        @Override
        public MethodReference lookupMethod(final int typeToken, final int nameAndTypeToken) {
            final ConstantPool.NameAndTypeDescriptorEntry nameAndDescriptor = constantPool.getEntry(nameAndTypeToken);

            return _parser.parseMethod(
                lookupType(typeToken),
                nameAndDescriptor.getName(),
                nameAndDescriptor.getType()
            );
        }

        @Override
        public <T> T lookupConstant(final int token) {
            return constantPool.lookupConstant(token);
        }
    }

    // </editor-fold>
}
