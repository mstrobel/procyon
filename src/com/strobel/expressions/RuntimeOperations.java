package com.strobel.expressions;

import com.strobel.core.StrongBox;
import com.strobel.util.ContractUtils;

/**
 * @author Mike Strobel
 */
public final class RuntimeOperations {
    private RuntimeOperations() {
        throw ContractUtils.unreachable();
    }

    public static IRuntimeVariables createRuntimeVariables(final Object[] data, final long[] indexes) {
        return new RuntimeVariableList(data, indexes);
    }

    private final static class RuntimeVariableList implements IRuntimeVariables {
        //
        // The top level environment. It contains pointers to parent 
        // environments, which are always in the first element.
        //
        private final Object[] _data;

        //
        // An array of (int, int) pairs, each representing how to find a
        // variable in the environment data structure.
        //
        // The first integer indicates the number of times to go up in the
        // closure chain, the second integer indicates the index into that
        // closure chain.
        //
        private final long[] _indexes;

        RuntimeVariableList(final Object[] data, final long[] indexes) {
            assert (data != null);
            assert (indexes != null);

            _data = data;
            _indexes = indexes;
        }

        public int size() {
            return _indexes.length;
        }

        public Object get(final int index) {
            return getStrongBox(index).value;
        }

        public void set(final int index, final Object value) {
            getStrongBox(index).value = value;
        }

        private StrongBox getStrongBox(final int index) {
            //
            // We lookup the closure using two integers:
            //   1) The high dword is the number of parents to go up
            //   2) The low dword is the index into that array
            //
            final long closureKey = _indexes[index];

            //
            // Walk up the parent chain to find the real environment.
            //
            Object[] result = _data;

            for (int parents = (int)(closureKey >> 32); parents > 0; parents--) {
                result = HoistedLocals.getParent(result);
            }

            //
            // Return the variable storage.
            //
            return (StrongBox)result[(int)closureKey];
        }
    }
}
