package com.strobel.assembler.ir;

import com.strobel.assembler.metadata.*;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.strobel.util.EmptyArrayCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;

/**
 * @author Mike Strobel
 */
public final class ClassTypeDefinition extends TypeDefinition {
    private final TypeReference _baseType;
    private final List<TypeReference> _interfaceTypes;
    private final ClassFile _classFile;
    private final FieldDefinition[] _fields;
    private final MethodDefinition[] _methods;
    private final TypeDefinition[] _nestedTypes;
    private final List<FieldDefinition> _fieldsView;
    private final List<MethodDefinition> _methodsView;
    private final List<TypeDefinition> _nestedTypesView;

    private ClassTypeDefinition(final ClassFile classFile, final TypeSystem typeSystem) {
        VerifyArgument.notNull(classFile, "classFile");
        VerifyArgument.notNull(typeSystem, "typeSystem");

        _classFile = classFile;
        _classFile.complete(this, typeSystem);
        _fields = new FieldDefinition[_classFile.fields.size()];
        _methods = new MethodDefinition[_classFile.methods.size()];
        _nestedTypes = EmptyArrayCache.fromElementType(TypeDefinition.class);
        _fieldsView = ArrayUtilities.asUnmodifiableList(_fields);
        _methodsView = ArrayUtilities.asUnmodifiableList(_methods);
        _nestedTypesView = ArrayUtilities.asUnmodifiableList(_nestedTypes);

        complete(typeSystem);

        _baseType = typeSystem.lookupType(_classFile.baseClassEntry.getName());

        final ConstantPool.TypeInfoEntry[] interfaceEntries = _classFile.interfaceEntries;

        if (interfaceEntries.length == 0) {
            _interfaceTypes = Collections.emptyList();
        }
        else {
            final TypeReference[] interfaceTypes = new TypeDefinition[interfaceEntries.length];

            for (int i = 0; i < interfaceTypes.length; i++) {
                interfaceTypes[i] = typeSystem.lookupType(interfaceEntries[i].getName());
            }

            _interfaceTypes = ArrayUtilities.asUnmodifiableList(interfaceTypes);
        }
    }

    @Override
    public TypeReference getBaseType() {
        return _baseType;
    }

    @Override
    public List<TypeReference> getExplicitInterfaces() {
        return _interfaceTypes;
    }

    @Override
    public List<FieldDefinition> getDeclaredFields() {
        return _fieldsView;
    }

    @Override
    public List<MethodDefinition> getDeclaredMethods() {
        return _methodsView;
    }

    @Override
    public List<TypeDefinition> getDeclaredTypes() {
        return _nestedTypesView;
    }

    @Override
    public TypeReference getDeclaringType() {
        throw ContractUtils.unreachable();
    }

    @Override
    public long getFlags() {
        return _classFile.accessFlags & Flags.ClassFlags;
    }

    // <editor-fold defaultstate="collapsed" desc="Resolution">

    private void complete(final TypeSystem typeSystem) {
        throw ContractUtils.unreachable();
    }

    public static ClassTypeDefinition load(final TypeSystem typeSystem, final File file) {
        VerifyArgument.notNull(typeSystem, "typeSystem");
        VerifyArgument.notNull(file, "file");

        final ClassTypeDefinition typeDefinition;

        try (final InputStream inputStream = new FileInputStream(file)) {
            final Buffer input;
            final ClassFile classFile;

            final byte[] data;

            data = new byte[inputStream.available()];
            inputStream.read(data);
            input = new Buffer(data);
            classFile = ClassFile.readClass(file, input);
            typeDefinition = new ClassTypeDefinition(classFile, typeSystem);
            typeSystem.registerType(typeDefinition);
        }
        catch (IOException e) {
            throw new UndeclaredThrowableException(e);
        }

        return typeDefinition;
    }

    // </editor-fold>
}
