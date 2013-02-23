/*
 * Expression.java
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

package com.strobel.decompiler.ast;

import com.strobel.assembler.Collection;
import com.strobel.assembler.ir.OpCode;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.Comparer;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.NameSyntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.strobel.decompiler.DecompilerHelpers.writeType;
import static com.strobel.assembler.ir.OpCodeHelpers.*;

public class Expression extends Node {
    public static final Object ANY_OPERAND = new Object();

    private final Collection<Expression> _arguments = new Collection<>();

    private OpCode _code;
    private Object _operand;
    private TypeReference _expectedType;
    private TypeReference _inferredType;

    public final List<Expression> getArguments() {
        return _arguments;
    }

    public final OpCode getCode() {
        return _code;
    }

    public final void setCode(final OpCode code) {
        _code = code;
    }

    public final Object getOperand() {
        return _operand;
    }

    public final void setOperand(final Object operand) {
        _operand = operand;
    }

    public final TypeReference getExpectedType() {
        return _expectedType;
    }

    public final void setExpectedType(final TypeReference expectedType) {
        _expectedType = expectedType;
    }

    public final TypeReference getInferredType() {
        return _inferredType;
    }

    public final void setInferredType(final TypeReference inferredType) {
        _inferredType = inferredType;
    }

    public final boolean isBranch() {
        return _operand instanceof Label ||
               _operand instanceof Label[];
    }

    public final List<Label> getBranchTargets() {
        if (_operand instanceof Label) {
            return Collections.singletonList((Label) _operand);
        }

        if (_operand instanceof Label[]) {
            return ArrayUtilities.asUnmodifiableList((Label[]) _operand);
        }

        return Collections.emptyList();
    }

    @Override
    public List<Node> getChildren() {
        final ArrayList<Node> childrenCopy = new ArrayList<>();
        childrenCopy.addAll(_arguments);
        return childrenCopy;
    }

    @Override
    public void writeTo(final ITextOutput output) {
        final OpCode code = _code;
        final Object operand = _operand;
        final TypeReference inferredType = _inferredType;
        final TypeReference expectedType = _expectedType;

        if (operand instanceof Variable &&
            ((Variable) operand).isGenerated()) {

            if (isLocalStore(code)) {
                output.write(((Variable) operand).getName());
                output.write(" = ");
                getArguments().get(0).writeTo(output);
                return;
            }

            if (isLocalLoad(code)) {
                output.write(((Variable) operand).getName());

                if (inferredType != null) {
                    output.write(':');
                    writeType(inferredType, output, NameSyntax.SHORT_TYPE_NAME);

                    if (expectedType != null &&
                        !Comparer.equals(expectedType.getInternalName(), inferredType.getInternalName())) {

                        output.write("[expected:");
                        writeType(expectedType, output, NameSyntax.SHORT_TYPE_NAME);
                        output.write(']');
                    }
                }

                return;
            }
        }

        output.write(code.name().toLowerCase());

        if (inferredType != null) {
            output.write(':');
            writeType(inferredType, output, NameSyntax.SHORT_TYPE_NAME);

            if (expectedType != null &&
                !Comparer.equals(expectedType.getInternalName(), inferredType.getInternalName())) {

                output.write("[expected:");
                writeType(expectedType, output, NameSyntax.SHORT_TYPE_NAME);
                output.write(']');
            }
        }
        else if (expectedType != null) {
            output.write("[expected:");
            writeType(expectedType, output, NameSyntax.SHORT_TYPE_NAME);
            output.write(']');
        }

        output.write('(');

        boolean first = true;

        if (operand != null) {
            if (operand instanceof Label) {
                output.writeReference(((Label) operand).getName(), operand);
            }
            else if (operand instanceof Label[]) {
                final Label[] labels = (Label[]) operand;

                for (int i = 0; i < (labels).length; i++) {
                    if (i != 0)
                        output.write(", ");

                    output.writeReference(labels[i].getName(), labels[i]);
                }
            }
            else if (operand instanceof MethodReference ||
                     operand instanceof FieldReference) {

                final MemberReference method = (MemberReference) operand;
                final TypeReference declaringType = method.getDeclaringType();

                if (declaringType != null) {
                    writeType(declaringType, output, NameSyntax.SHORT_TYPE_NAME);
                    output.write("::");
                }

                output.writeReference(method.getName(), method);
            }
            else {

            }

            first = false;
        }

        for (final Expression argument : getArguments()) {
            if (!first) {
                output.write(", ");
            }

            argument.writeTo(output);
            first = false;
        }

        output.write(')');
    }
}
