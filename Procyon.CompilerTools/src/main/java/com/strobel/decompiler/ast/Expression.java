/*
 * Expression.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.decompiler.ast;

import com.strobel.annotations.NotNull;
import com.strobel.annotations.Nullable;
import com.strobel.assembler.Collection;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.componentmodel.Key;
import com.strobel.componentmodel.UserDataStore;
import com.strobel.componentmodel.UserDataStoreBase;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.Comparer;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerHelpers;
import com.strobel.decompiler.ITextOutput;
import com.strobel.decompiler.NameSyntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.strobel.decompiler.DecompilerHelpers.writeType;

public final class Expression extends Node implements Cloneable, UserDataStore {
    public static final Object ANY_OPERAND = new Object();

    private final Collection<Expression> _arguments = new Collection<>();

    private final Collection<Range> _ranges = new Collection<Range>() {
        @Override
        public void add(final int index, final Range element) {
            if (contains(element)) {
                return;
            }
            super.add(index, element);
        }
    };

    private AstCode _code;
    private Object _operand;
    private TypeReference _expectedType;
    private TypeReference _inferredType;
    private UserDataStoreBase _userData;

    public Expression(final AstCode code, final Object operand, final List<Expression> arguments) {
        _code = VerifyArgument.notNull(code, "code");
        _operand = VerifyArgument.verifyNotInstanceOf(Expression.class, operand, "operand");

        if (arguments != null) {
            _arguments.addAll(arguments);
        }
    }

    public Expression(final AstCode code, final Object operand, final Expression... arguments) {
        _code = VerifyArgument.notNull(code, "code");
        _operand = VerifyArgument.verifyNotInstanceOf(Expression.class, operand, "operand");

        if (arguments != null) {
            Collections.addAll(_arguments, arguments);
        }
    }

    public final List<Expression> getArguments() {
        return _arguments;
    }

    public final AstCode getCode() {
        return _code;
    }

    public final void setCode(final AstCode code) {
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

    public final Collection<Range> getRanges() {
        return _ranges;
    }

    @Override
    public final List<Node> getChildren() {
        final ArrayList<Node> childrenCopy = new ArrayList<>();

        childrenCopy.addAll(_arguments);

        if (_operand instanceof Lambda) {
            childrenCopy.add((Node) _operand);
        }

        return childrenCopy;
    }

    public final boolean containsReferenceTo(final Variable variable) {
        if (_operand == variable) {
            return true;
        }

        for (int i = 0; i < _arguments.size(); i++) {
            if (_arguments.get(i).containsReferenceTo(variable)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public final void writeTo(final ITextOutput output) {
        final AstCode code = _code;
        final Object operand = _operand;
        final TypeReference inferredType = _inferredType;
        final TypeReference expectedType = _expectedType;

        if (operand instanceof Variable /*&&
            ((Variable) operand).isGenerated()*/) {

            if (AstCodeHelpers.isLocalStore(code)) {
                output.write(((Variable) operand).getName());
                output.write(" = ");
                getArguments().get(0).writeTo(output);
                return;
            }

            if (AstCodeHelpers.isLocalLoad(code)) {
                output.write(((Variable) operand).getName());

                if (inferredType != null) {
                    output.write(':');
                    writeType(output, inferredType, NameSyntax.SHORT_TYPE_NAME);

                    if (expectedType != null &&
                        !Comparer.equals(expectedType.getInternalName(), inferredType.getInternalName())) {

                        output.write("[expected:");
                        writeType(output, expectedType, NameSyntax.SHORT_TYPE_NAME);
                        output.write(']');
                    }
                }

                return;
            }
        }

        output.writeReference(code.name().toLowerCase(), code);

        if (inferredType != null) {
            output.write(':');
            writeType(output, inferredType, NameSyntax.SHORT_TYPE_NAME);

            if (expectedType != null &&
                !Comparer.equals(expectedType.getInternalName(), inferredType.getInternalName())) {

                output.write("[expected:");
                writeType(output, expectedType, NameSyntax.SHORT_TYPE_NAME);
                output.write(']');
            }
        }
        else if (expectedType != null) {
            output.write("[expected:");
            writeType(output, expectedType, NameSyntax.SHORT_TYPE_NAME);
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
                    if (i != 0) {
                        output.write(", ");
                    }

                    output.writeReference(labels[i].getName(), labels[i]);
                }
            }
            else if (operand instanceof MethodReference ||
                     operand instanceof FieldReference) {

                final MemberReference member = (MemberReference) operand;
                final TypeReference declaringType = member.getDeclaringType();

                if (declaringType != null) {
                    writeType(output, declaringType, NameSyntax.SHORT_TYPE_NAME);
                    output.write("::");
                }

                output.writeReference(member.getName(), member);
            }
            else {
                DecompilerHelpers.writeOperand(output, operand);
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

    @SuppressWarnings("CloneDoesntCallSuperClone")
    public final Expression clone() {
        final Expression clone = new Expression(_code, _operand);

        clone._code = _code;
        clone._expectedType = _expectedType;
        clone._inferredType = _inferredType;
        clone._operand = _operand;
        clone._userData = _userData != null ? _userData.clone() : null;

        for (final Expression argument : _arguments) {
            clone._arguments.add(argument.clone());
        }

        return clone;
    }

    @Override
    public <T> T getUserData(@NotNull final Key<T> key) {
        if (_userData == null) {
            return null;
        }
        return _userData.getUserData(key);
    }

    @Override
    public <T> void putUserData(@NotNull final Key<T> key, @Nullable final T value) {
        if (_userData == null) {
            _userData = new UserDataStoreBase();
        }
        _userData.putUserData(key, value);
    }

    @Override
    public <T> T putUserDataIfAbsent(@NotNull final Key<T> key, @Nullable final T value) {
        if (_userData == null) {
            _userData = new UserDataStoreBase();
        }
        return _userData.putUserDataIfAbsent(key, value);
    }

    @Override
    public <T> boolean replace(@NotNull final Key<T> key, @Nullable final T oldValue, @Nullable final T newValue) {
        if (_userData == null) {
            _userData = new UserDataStoreBase();
        }
        return _userData.replace(key, oldValue, newValue);
    }
}
