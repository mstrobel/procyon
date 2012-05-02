package com.strobel.reflection.emit;

import com.strobel.reflection.TypeContext;
import com.strobel.util.ContractUtils;

/**
 * @author Mike Strobel
 */
public final class ModuleBuilder extends TypeContext {

    void checkContext(ClassBuilder[]... types) {
        if (types == null) {
            return;
        }

        for (final ClassBuilder[] t : types) {
            if (t != null) {
                checkContext(t);
            }
        }
    }

    void checkContext(final ClassBuilder... types) {
        if (types == null) {
            return;
        }

        for (final ClassBuilder type : types) {
            if (type == null) {
                continue;
            }

            if (type.getModule() == null) {
                throw new IllegalArgumentException(
                    "ClassBuilder is not valid in the context of this ModuleBuilder."
                );
            }
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        throw ContractUtils.unreachable();
    }
}
