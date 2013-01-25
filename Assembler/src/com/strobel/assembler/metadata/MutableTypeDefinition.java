package com.strobel.assembler.metadata;

import com.strobel.assembler.Collection;
import com.strobel.assembler.metadata.annotations.CustomAnnotation;
import com.strobel.core.IFreezable;
import com.strobel.core.StringUtilities;

import java.util.List;

/**
 * @author Mike Strobel
 */
public final class MutableTypeDefinition extends TypeDefinition implements IFreezable {
    final Collection<FieldDefinition> declaredFields;
    final Collection<TypeDefinition> declaredTypes;
    final Collection<MethodDefinition> declaredMethods;
    final GenericParameterCollection genericParameters;
    final Collection<TypeReference> explicitInterfaces;
    final Collection<CustomAnnotation> customAnnotations;

    private int _flags;
    private String _name;
    private String _packageName = StringUtilities.EMPTY;
    private TypeReference _declaringType;
    private TypeReference _baseType = BuiltinTypes.Object;
    private boolean _isFrozen;
    private TypeReference _rawType;

    public MutableTypeDefinition() {
        this.declaredFields = new Collection<>();
        this.declaredTypes = new Collection<>();
        this.declaredMethods = new Collection<>();
        this.genericParameters = new GenericParameterCollection(this);
        this.explicitInterfaces = new Collection<>();
        this.customAnnotations = new Collection<>();
    }

    @Override
    public boolean isDefinition() {
        return true;
    }

    // <editor-fold defaultstate="collapsed" desc="Type Attributes">

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getPackageName() {
        return _packageName;
    }

    @Override
    public long getFlags() {
        return _flags;
    }

    @Override
    public TypeReference getDeclaringType() {
        return _declaringType;
    }

    @Override
    public TypeReference getBaseType() {
        return _baseType;
    }

    @Override
    public List<TypeReference> getExplicitInterfaces() {
        return explicitInterfaces;
    }

    @Override
    public boolean hasAnnotations() {
        return !customAnnotations.isEmpty();
    }

    @Override
    public List<CustomAnnotation> getAnnotations() {
        return customAnnotations;
    }

    public void setFlags(final int flags) {
        verifyNotFrozen();
        _flags = flags;
    }

    public void setName(final String name) {
        verifyNotFrozen();
        _name = name;
    }

    public void setPackageName(final String packageName) {
        verifyNotFrozen();
        _packageName = packageName;
    }

    public void setDeclaringType(final TypeReference declaringType) {
        verifyNotFrozen();
        _declaringType = declaringType;
    }

    public void setBaseType(final TypeReference baseType) {
        verifyNotFrozen();
        _baseType = baseType;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Generics">

    @Override
    public List<GenericParameter> getGenericParameters() {
        return genericParameters;
    }

    @Override
    public TypeReference getRawType() {
        if (_rawType == null) {
            synchronized (this) {
                if (_rawType == null) {
                    _rawType = isGenericDefinition() ? new RawType(this)
                                                     : this;
                }
            }
        }
        return _rawType;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type Members">

    @Override
    public List<FieldDefinition> getDeclaredFields() {
        return declaredFields;
    }

    @Override
    public List<MethodDefinition> getDeclaredMethods() {
        return declaredMethods;
    }

    @Override
    public List<TypeDefinition> getDeclaredTypes() {
        return declaredTypes;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="IFreezable Implementation">

    @Override
    public boolean canFreeze() {
        return !isFrozen();
    }

    @Override
    public final boolean isFrozen() {
        return _isFrozen;
    }

    @Override
    public final void freeze() throws IllegalStateException {
        if (!canFreeze()) {
            throw new IllegalStateException(
                "Type cannot be frozen.  Be sure to check canFreeze() before calling " +
                "freeze(), or use the tryFreeze() method instead."
            );
        }

        freezeCore();

        _isFrozen = true;
    }

    protected void freezeCore() {
        this.declaredTypes.freeze(false);
        this.declaredMethods.freeze();
        this.declaredFields.freeze();
        this.genericParameters.freeze();
        this.explicitInterfaces.freeze();
    }

    protected final void verifyNotFrozen() {
        if (isFrozen()) {
            throw new IllegalStateException("A frozen type definition cannot be modified.");
        }
    }

    protected final void verifyFrozen() {
        if (!isFrozen()) {
            throw new IllegalStateException(
                "Type must be frozen before performing this operation."
            );
        }
    }

    @Override
    public final boolean tryFreeze() {
        if (!canFreeze()) {
            return false;
        }

        try {
            freeze();
            return true;
        }
        catch (final Throwable t) {
            return false;
        }
    }

    @Override
    public final void freezeIfUnfrozen() throws IllegalStateException {
        if (isFrozen()) {
            return;
        }
        freeze();
    }

    // </editor-fold>
}
