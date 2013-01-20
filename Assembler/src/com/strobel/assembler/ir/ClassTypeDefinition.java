package com.strobel.assembler.ir;

import com.strobel.assembler.metadata.*;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.util.ContractUtils;
import com.strobel.util.EmptyArrayCache;

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

    private ClassTypeDefinition(final ClassFile classFile, final MetadataSystem metadataSystem) {
        VerifyArgument.notNull(classFile, "classFile");
        VerifyArgument.notNull(metadataSystem, "metadataSystem");

        _classFile = classFile;

        metadataSystem.addTypeDefinition(this);

        _classFile.complete(this, metadataSystem);
        _fields = new FieldDefinition[_classFile.fields.size()];
        _methods = new MethodDefinition[_classFile.methods.size()];
        _nestedTypes = EmptyArrayCache.fromElementType(TypeDefinition.class);
        _fieldsView = ArrayUtilities.asUnmodifiableList(_fields);
        _methodsView = ArrayUtilities.asUnmodifiableList(_methods);
        _nestedTypesView = ArrayUtilities.asUnmodifiableList(_nestedTypes);

        complete(metadataSystem);

        final ConstantPool.TypeInfoEntry baseClassEntry = _classFile.baseClassEntry;

        if (baseClassEntry != null) {
            _baseType = metadataSystem.lookupType(baseClassEntry.getName());
        }
        else {
            _baseType = null;
        }

        final ConstantPool.TypeInfoEntry[] interfaceEntries = _classFile.interfaceEntries;

        if (interfaceEntries.length == 0) {
            _interfaceTypes = Collections.emptyList();
        }
        else {
            final TypeReference[] interfaceTypes = new TypeDefinition[interfaceEntries.length];

            for (int i = 0; i < interfaceTypes.length; i++) {
                interfaceTypes[i] = metadataSystem.lookupType(interfaceEntries[i].getName());
            }

            _interfaceTypes = ArrayUtilities.asUnmodifiableList(interfaceTypes);
        }
    }

    @Override
    public String getName() {
        return _classFile.name;
    }

    @Override
    public String getPackageName() {
        return _classFile.packageName;
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

    private void complete(final MetadataSystem metadataSystem) {
//        throw ContractUtils.unreachable();
    }

    public static ClassTypeDefinition load(final MetadataSystem metadataSystem, final Buffer data) {
        VerifyArgument.notNull(metadataSystem, "metadataSystem");
        VerifyArgument.notNull(data, "data");

        final ClassFile classFile = ClassFile.readClass(data);
        final ClassTypeDefinition typeDefinition = new ClassTypeDefinition(classFile, metadataSystem);

        return typeDefinition;
    }

    // </editor-fold>
}
