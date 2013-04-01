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

import com.strobel.assembler.metadata.BuiltinTypes;
import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.IntegerBox;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Block;
import com.strobel.decompiler.ast.Expression;
import com.strobel.decompiler.ast.Loop;
import com.strobel.decompiler.ast.PatternMatching;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.reflection.SimpleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.getOrDefault;

public class NameVariables {
    private final static char MAX_LOOP_VARIABLE_NAME = 'n';
    private final static String[] METHOD_PREFIXES = { "get", "is", "are", "to", "as" };
    private final static String[] METHOD_SUFFIXES = { "At", "For", "From" };
    private final static Map<String, String> BUILT_IN_TYPE_NAMES;

    static {
        final Map<String, String> builtInTypeNames = new LinkedHashMap<>();

        builtInTypeNames.put(BuiltinTypes.Boolean.getInternalName(), "b");
        builtInTypeNames.put("java/lang/Boolean", "b");
        builtInTypeNames.put(BuiltinTypes.Byte.getInternalName(), "b");
        builtInTypeNames.put("java/lang/Byte", "b");
        builtInTypeNames.put(BuiltinTypes.Short.getInternalName(), "num");
        builtInTypeNames.put("java/lang/Short", "num");
        builtInTypeNames.put(BuiltinTypes.Integer.getInternalName(), "num");
        builtInTypeNames.put("java/lang/Integer", "num");
        builtInTypeNames.put(BuiltinTypes.Long.getInternalName(), "num");
        builtInTypeNames.put("java/lang/Long", "num");
        builtInTypeNames.put(BuiltinTypes.Float.getInternalName(), "num");
        builtInTypeNames.put("java/lang/Float", "num");
        builtInTypeNames.put(BuiltinTypes.Double.getInternalName(), "num");
        builtInTypeNames.put("java/lang/Double", "num");
        builtInTypeNames.put(BuiltinTypes.Character.getInternalName(), "c");
        builtInTypeNames.put("java/lang/Character", "c");
        builtInTypeNames.put("java/lang/Object", "obj");
        builtInTypeNames.put("java/lang/String", "s");

        BUILT_IN_TYPE_NAMES = Collections.unmodifiableMap(builtInTypeNames);
    }

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
            final boolean generateName = StringUtilities.isNullOrEmpty(varDef.getName()) ||
                                         varDef.isGenerated() ||
                                         !varDef.isParameter() && !varDef.getOriginalVariable().isFromMetadata();

            if (generateName) {
                varDef.setName(nv.generateNameForVariable(varDef, methodBody));
            }
        }
    }

    static boolean isValidName(final String name) {
        if (StringUtilities.isNullOrEmpty(name)) {
            return false;
        }

        if (!Character.isJavaIdentifierPart(name.charAt(0))) {
            return false;
        }

        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
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

    @SuppressWarnings("ConstantConditions")
    private String generateNameForVariable(final Variable variable, final Block methodBody) {
        String proposedName = null;

        if (variable.getType().getSimpleType() == SimpleType.Integer) {
            boolean isLoopCounter = false;

        loopSearch:
            for (final Loop loop : methodBody.getSelfAndChildrenRecursive(Loop.class)) {
                Expression e = loop.getCondition();

                while (e != null && e.getCode() == AstCode.LogicalNot) {
                    e = e.getArguments().get(0);
                }

                if (e != null) {
                    switch (e.getCode()) {
                        case CmpEq:
                        case CmpNe:
                        case CmpLe:
                        case CmpGt:
                        case CmpGe:
                        case CmpLt: {
                            final StrongBox<Variable> loadVariable = new StrongBox<>();
                            if (PatternMatching.matchGetOperand(e.getArguments().get(0), AstCode.Load, loadVariable) &&
                                loadVariable.get() == variable) {

                                isLoopCounter = true;
                                break loopSearch;
                            }
                            break;
                        }
                    }
                }
            }

            if (isLoopCounter) {
                for (char c = 'i'; c < MAX_LOOP_VARIABLE_NAME; c++) {
                    final String name = String.valueOf(c);

                    if (!_typeNames.containsKey(name)) {
                        proposedName = name;
                        break;
                    }
                }
            }
        }

        if (StringUtilities.isNullOrEmpty(proposedName)) {
            String proposedNameForStore = null;

            for (final Expression e : methodBody.getSelfAndChildrenRecursive(Expression.class)) {
                if (e.getCode() == AstCode.Store && e.getOperand() == variable) {
                    final String name = getNameFromExpression(e.getArguments().get(0));

                    if (name != null/* && !_fieldNamesInCurrentType.contains(name)*/) {
                        if (proposedNameForStore != null) {
                            proposedNameForStore = null;
                            break;
                        }

                        proposedNameForStore = name;
                    }
                }
            }

            if (proposedNameForStore != null) {
                proposedName = proposedNameForStore;
            }
        }

        if (StringUtilities.isNullOrEmpty(proposedName)) {
            String proposedNameForLoad = null;

            for (final Expression e : methodBody.getSelfAndChildrenRecursive(Expression.class)) {
                final List<Expression> arguments = e.getArguments();

                for (int i = 0; i < arguments.size(); i++) {
                    final Expression a = arguments.get(i);
                    if (a.getCode() == AstCode.Load && a.getOperand() == variable) {
                        final String name = getNameForArgument(e, i);

                        if (name != null/* && !_fieldNamesInCurrentType.contains(name)*/) {
                            if (proposedNameForLoad != null) {
                                proposedNameForLoad = null;
                                break;
                            }

                            proposedNameForLoad = name;
                        }
                    }
                }
            }

            if (proposedNameForLoad != null) {
                proposedName = proposedNameForLoad;
            }
        }

        if (StringUtilities.isNullOrEmpty(proposedName)) {
            proposedName = getNameForType(variable.getType());
        }

        return this.getAlternativeName(proposedName);
/*
        while (true) {
            proposedName = this.getAlternativeName(proposedName);

            if (!_fieldNamesInCurrentType.contains(proposedName)) {
                return proposedName;
            }
        }
*/
    }

    private static String cleanUpVariableName(final String s) {
        if (s == null) {
            return null;
        }

        String name = s;

        if (name.length() > 2 && name.startsWith("m_")) {
            name = name.substring(2);
        }
        else if (name.length() > 1 && name.startsWith("_")) {
            name = name.substring(1);
        }

        if (name.length() == 0) {
            return "obj";
        }

        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

        if (JavaOutputVisitor.isKeyword(name)) {
            return name;
        }

        return name;
    }

    private static String getNameFromExpression(final Expression e) {
        switch (e.getCode()) {
            case GetField:
            case GetStatic: {
                return cleanUpVariableName(((FieldReference) e.getOperand()).getName());
            }

            case InvokeVirtual:
            case InvokeSpecial:
            case InvokeStatic:
            case InvokeInterface: {
                final MethodReference method = (MethodReference) e.getOperand();

                if (method != null) {
                    final String methodName = method.getName();

                    String name = methodName;

                    for (final String prefix : METHOD_PREFIXES) {
                        if (methodName.length() > prefix.length() &&
                            methodName.startsWith(prefix) &&
                            Character.isUpperCase(methodName.charAt(prefix.length()))) {

                            name = methodName.substring(prefix.length());
                            break;
                        }
                    }

                    for (final String suffix : METHOD_SUFFIXES) {
                        if (name.length() > suffix.length() &&
                            name.endsWith(suffix) &&
                            Character.isLowerCase(name.charAt(name.length() - suffix.length() - 1))) {

                            name = name.substring(0, name.length() - suffix.length());
                            break;
                        }
                    }

                    return cleanUpVariableName(name);
                }

                break;
            }
        }

        return null;
    }

    private static String getNameForArgument(final Expression parent, final int i) {
        switch (parent.getCode()) {
            case PutField:
            case PutStatic: {
                if (i == parent.getArguments().size() - 1) {
                    return cleanUpVariableName(((FieldReference) parent.getOperand()).getName());
                }
                break;
            }

            case InvokeVirtual:
            case InvokeSpecial:
            case InvokeStatic:
            case InvokeInterface:
            case InitObject: {
                final MethodReference method = (MethodReference) parent.getOperand();

                if (method != null) {
                    final String methodName = method.getName();
                    final List<ParameterDefinition> parameters = method.getParameters();

                    if (parameters.size() == 1 && i == parent.getArguments().size() - 1) {
                        if (methodName.length() > 3 &&
                            StringUtilities.startsWith(methodName, "set") &&
                            Character.isUpperCase(methodName.charAt(3))) {

                            return cleanUpVariableName(methodName.substring(3));
                        }
                    }

                    final MethodDefinition definition = method.resolve();

                    if (definition != null) {
                        final ParameterDefinition p = getOrDefault(
                            definition.getParameters(),
                            parent.getCode() != AstCode.InitObject && !definition.isStatic() ? i - 1 : i
                        );

                        if (p != null && p.hasName() && !StringUtilities.isNullOrEmpty(p.getName())) {
                            return cleanUpVariableName(p.getName());
                        }
                    }
                }

                break;
            }
        }

        return null;
    }

    private String getNameForType(final TypeReference type) {
        String name;

        if (type.isArray()) {
            name = "array";
        }
        else if (type.getName().endsWith("Exception")) {
            name = "ex";
        }
        else if (type.getName().endsWith("List")) {
            name = "list";
        }
        else if (type.getName().endsWith("Set")) {
            name = "set";
        }
        else if (type.getName().endsWith("Collection")) {
            name = "collection";
        }
        else {
            name = BUILT_IN_TYPE_NAMES.get(type.getInternalName());

            if (name != null) {
                return name;
            }

            name = type.getSimpleName();

            //
            // Remove leading 'I' for interfaces.
            //
            if (name.length() > 2 &&
                name.charAt(0) == 'I' &&
                Character.isUpperCase(name.charAt(1)) &&
                Character.isLowerCase(name.charAt(2))) {

                name = name.substring(1);
            }

            name = cleanUpVariableName(name);
        }

        return name;
    }
}
