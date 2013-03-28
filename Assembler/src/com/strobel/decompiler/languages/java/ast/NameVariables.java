/*
 * NameVariables.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.languages.java.ast;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.core.IntegerBox;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Block;
import com.strobel.decompiler.ast.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author strobelm
 */
public class NameVariables {
    private final static char MAX_LOOP_VARIABLE_NAME = 'n';

    private final ArrayList<String> _fieldNamesInCurrentType;
    private final Map<String, Integer> _typeNames = new HashMap<>();

    public NameVariables(final DecompilerContext context) {
        _fieldNamesInCurrentType = new ArrayList<>();

        for (final FieldDefinition field : context.getCurrentType().getDeclaredFields()) {
            _fieldNamesInCurrentType.add(field.getName());
        }
    }

    public final void addExistingName(final String name) {
        if (StringUtilities.isNullOrEmpty(name)) {
            return;
        }

        final IntegerBox number = new IntegerBox();
        final String nameWithoutDigits = splitName(name, number);
        final Integer existingNumber = _typeNames.get(nameWithoutDigits);

        if (existingNumber != null) {
            _typeNames.put(nameWithoutDigits, Math.max(number.value, existingNumber));
        }
        else {
            _typeNames.put(nameWithoutDigits, number.value);
        }
    }

    final String splitName(final String name, final IntegerBox number) {
        int position = name.length();

        while (position > 0 && name.charAt(position - 1) >= '0' && name.charAt(position - 1) <= '9') {
            position--;
        }

        if (position < name.length()) {
            number.value = Integer.parseInt(name.substring(position));
            return name.substring(0, position);
        }

        number.value = 1;
        return name;
    }

    public static void assignNamesToVariables(
        final DecompilerContext context,
        final Iterable<Variable> parameters,
        final Iterable<Variable> variables,
        final Block methodBody) {

        final NameVariables nv = new NameVariables(context);

        for (final String name : context.getReservedVariableNames()) {
            nv.addExistingName(name);
        }

        for (final Variable p : parameters) {
            nv.addExistingName(p.getName());
        }

        for (final Variable v : variables) {
            if (v.isGenerated()) {
                nv.addExistingName(v.getName());
            }
            else if (v.getOriginalVariable() != null) {
                final String varName = v.getOriginalVariable().getName();

                if (StringUtilities.isNullOrEmpty(varName) || varName.startsWith("V_") || !isValidName(varName)) {
                    v.setName(null);
                }
                else {
                    v.setName(nv.getAlternativeName(varName));
                }
            }
            else {
                v.setName(null);
            }
        }

        for (final Variable p : parameters) {
            if (StringUtilities.isNullOrEmpty(p.getName())) {
                p.setName(nv.generateNameForVariable(p, methodBody));
            }
        }

        for (final Variable varDef : variables) {
            if (StringUtilities.isNullOrEmpty(varDef.getName())) {
                varDef.setName(nv.generateNameForVariable(varDef, methodBody));
            }
        }
    }

    @SuppressWarnings("UnusedParameters")
    private String generateNameForVariable(final Variable variable, final Block methodBody) {
        while (true) {
            final String name = this.getAlternativeName(variable.isParameter() ? "p" : "v");

            if (!_fieldNamesInCurrentType.contains(name)) {
                return name;
            }
        }
    }

    static boolean isValidName(final String name) {
        if (StringUtilities.isNullOrEmpty(name)) {
            return false;
        }

        if (!Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
            return false;
        }

        for (int i = 1; i < name.length(); i++) {
            if (!(Character.isLetterOrDigit(name.charAt(i)) || name.charAt(i) == '_')) {
                return false;
            }
        }

        return true;
    }

    public String getAlternativeName(final String oldVariableName) {
        if (oldVariableName.length() == 1 &&
            oldVariableName.charAt(0) >= 'i' &&
            oldVariableName.charAt(0) <= MAX_LOOP_VARIABLE_NAME) {

            for (char c = 'i'; c <= MAX_LOOP_VARIABLE_NAME; c++) {
                final String cs = String.valueOf(c);

                if (!_typeNames.containsKey(cs)) {
                    _typeNames.put(cs, 1);
                    return cs;
                }
            }
        }

        final IntegerBox number = new IntegerBox();
        final String nameWithoutDigits = splitName(oldVariableName, number);

        if (!_typeNames.containsKey(nameWithoutDigits)) {
            _typeNames.put(nameWithoutDigits, number.value - 1);
        }

        final int count = _typeNames.get(nameWithoutDigits) + 1;

        _typeNames.put(nameWithoutDigits, count);

        if (count != 1) {
            return nameWithoutDigits + count;
        }
        else {
            return nameWithoutDigits;
        }
    }
}
