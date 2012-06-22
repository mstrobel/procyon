package com.strobel.reflection;

import com.strobel.core.VerifyArgument;
import com.strobel.reflection.emit.ModuleBuilder;
import net.sf.cglib.core.ClassEmitter;
import org.objectweb.asm.ClassWriter;

import java.lang.reflect.Modifier;

/**
 * @author Mike Strobel
 */
public abstract class ClassBuilder extends Type {
    private final String _name;
    private final ModuleBuilder _module;
    private final ClassBuilder _enclosingType;
    private int _modifiers;
    private final Type _parent;
    private boolean _isHiddenGlobalType;
    private ClassEmitter _classEmitter;

    ClassBuilder(
        final String name,
        final int modifiers,
        final Type parent,
        final ModuleBuilder module,
        final ClassBuilder enclosingType) {

        _name = name;
        _modifiers = modifiers;
        _parent = parent;
        _module = VerifyArgument.notNull(module, "module");
        _enclosingType = enclosingType;

        if (enclosingType == null) {
            throw new IllegalStateException("ClassBuilder does not currently support nested types.");
        }

        if (Modifier.isInterface(modifiers)) {
            throw new IllegalStateException("ClassBuilder does not currently support interfaces.");
        }
    }

    public ModuleBuilder getModule() {
        return _module;
    }

    ClassEmitter getClassEmitter() {
        if (_classEmitter == null) {
            _classEmitter = new ClassEmitter(
                new ClassWriter(
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS
                )
            );
        }
        return _classEmitter;
    }

/*
    private MethodBuilder defineMethodNoLock(
        final String name,
        final int modifiers,
        final Type returnType,
        final Type[] parameterTypes)  {
        
        VerifyArgument.notNullOrWhitespace(name, "name");

        checkContext(returnType);
        checkContext(parameterTypes);

        throwIfGeneric();
        throwIfCreated();

        if (!_isHiddenGlobalType)
        {
            (Modifier.isInterface(_modifiers) && Modifier.interfaceModifiers())
            if (((_modifiers & TypeAttributes.ClassSemanticsMask) == TypeAttributes.Interface) &&
                (modifiers & MethodAttributes.Abstract) == 0 && (modifiers & MethodAttributes.Static) == 0) {
                throw new ArgumentException(Environment.GetResourceString("Argument_BadAttributeOnInterfaceMethod"));
            }
        }

        // pass in Method modifiers
        MethodBuilder method = new MethodBuilder(
            name, modifiers, callingConvention,
            returnType, returnTypeRequiredCustomModifiers, returnTypeOptionalCustomModifiers,
            parameterTypes, parameterTypeRequiredCustomModifiers, parameterTypeOptionalCustomModifiers,
            m_module, this, false);

        if (!m_isHiddenGlobalType)
        {
            //If this method is declared to be a constructor, increment our constructor count.
            if ((method.Attributes & MethodAttributes.SpecialName) != 0 && method.Name.equals(ConstructorInfo.ConstructorName))
            {
                m_constructorCount++;
            }
        }

        m_listMethods.Add(method);

        return method;
    }
*/

}
