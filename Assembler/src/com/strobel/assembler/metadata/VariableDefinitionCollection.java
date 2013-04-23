/*
 * VariableDefinitionCollection.java
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

package com.strobel.assembler.metadata;

import com.strobel.assembler.Collection;
import com.strobel.assembler.ir.OpCode;
import com.strobel.core.StringUtilities;
import com.strobel.reflection.SimpleType;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public final class VariableDefinitionCollection extends Collection<VariableDefinition> {
    private final TypeReference _declaringType;

    public VariableDefinitionCollection(final TypeReference declaringType) {
        _declaringType = declaringType;
    }

    @Override
    protected void afterAdd(final int index, final VariableDefinition d, final boolean appended) {
        d.setDeclaringType(_declaringType);
    }

    @Override
    protected void beforeSet(final int index, final VariableDefinition d) {
        final VariableDefinition current = get(index);
        current.setDeclaringType(null);
        d.setDeclaringType(_declaringType);
    }

    @Override
    protected void afterRemove(final int index, final VariableDefinition d) {
        d.setDeclaringType(null);
    }

    @Override
    protected void beforeClear() {
        for (int i = 0; i < size(); i++) {
            get(i).setDeclaringType(null);
        }
    }

    public int slotCount() {
        int count = 0;

        for (int i = 0; i < size(); i++) {
            count += get(i).getSize();
        }

        return count;
    }

    public VariableDefinition tryFind(final int slot) {
        return find(slot, -1);
    }

    public VariableDefinition tryFind(final int slot, final int instructionOffset) {
        for (int i = 0; i < size(); i++) {
            final VariableDefinition variable = get(i);

            if (variable.getSlot() == slot &&
                (instructionOffset < 0 ||
                 variable.getScopeStart() <= instructionOffset &&
                 (variable.getScopeEnd() < 0 || variable.getScopeEnd() > instructionOffset))) {

                return variable;
            }
        }

        return null;
    }

    public VariableDefinition find(final int slot) {
        return find(slot, -1);
    }

    public VariableDefinition find(final int slot, final int instructionOffset) {
        final VariableDefinition variable = tryFind(slot, instructionOffset);

        if (variable != null) {
            return variable;
        }

        throw new NoSuchElementException(
            String.format(
                "Could not find varible at slot %d and offset %d.",
                slot,
                instructionOffset
            )
        );
    }

    public VariableDefinition ensure(final int slot, final OpCode op, final int instructionOffset) {
        final TypeReference variableType;

        switch (op) {
            case ISTORE:
            case ISTORE_0:
            case ISTORE_1:
            case ISTORE_2:
            case ISTORE_3:
            case ISTORE_W:
            case ILOAD:
            case ILOAD_0:
            case ILOAD_1:
            case ILOAD_2:
            case ILOAD_3:
            case ILOAD_W:
                variableType = BuiltinTypes.Integer;
                break;

            case LSTORE:
            case LSTORE_0:
            case LSTORE_1:
            case LSTORE_2:
            case LSTORE_3:
            case LSTORE_W:
            case LLOAD:
            case LLOAD_0:
            case LLOAD_1:
            case LLOAD_2:
            case LLOAD_3:
            case LLOAD_W:
                variableType = BuiltinTypes.Long;
                break;

            case FSTORE:
            case FSTORE_0:
            case FSTORE_1:
            case FSTORE_2:
            case FSTORE_3:
            case FSTORE_W:
            case FLOAD:
            case FLOAD_0:
            case FLOAD_1:
            case FLOAD_2:
            case FLOAD_3:
            case FLOAD_W:
                variableType = BuiltinTypes.Float;
                break;

            case DSTORE:
            case DSTORE_0:
            case DSTORE_1:
            case DSTORE_2:
            case DSTORE_3:
            case DSTORE_W:
            case DLOAD:
            case DLOAD_0:
            case DLOAD_1:
            case DLOAD_2:
            case DLOAD_3:
            case DLOAD_W:
                variableType = BuiltinTypes.Double;
                break;

            case IINC:
            case IINC_W:
                variableType = BuiltinTypes.Integer;
                break;

            default:
                variableType = BuiltinTypes.Object;
                break;
        }

        final int effectiveOffset;

        if (op.isStore()) {
            effectiveOffset = instructionOffset + op.getSize() + op.getOperandType().getBaseSize();
        }
        else {
            effectiveOffset = instructionOffset;
        }

        VariableDefinition variable = tryFind(slot, effectiveOffset);

        if (variable != null) {
            if (variable.isFromMetadata()) {
                return variable;
            }

            final TypeReference targetType = op.isStore() ? variable.getVariableType() : variableType;
            final TypeReference sourceType = op.isStore() ? variableType : variable.getVariableType();

            if (isTargetTypeCompatible(targetType, sourceType)) {
                return variable;
            }

            variable.setScopeEnd(effectiveOffset - 1);
        }

        variable = new VariableDefinition(
            slot,
            String.format("$%d_%d$", slot, effectiveOffset),
            variableType
        );

        variable.setDeclaringType(_declaringType);
        variable.setScopeStart(effectiveOffset);
        variable.setScopeEnd(-1);
        variable.setFromMetadata(false);

        if (variableType != BuiltinTypes.Object) {
            variable.setTypeKnown(true);
        }

        add(variable);

        return variable;
    }

    public void updateScopes(final int codeSize) {
        boolean modified;

        do {
            modified = false;

            for (int i = 0; i < size(); i++) {
                final VariableDefinition variable = get(i);

                for (int j = 0; j < size(); j++) {
                    if (i == j) {
                        continue;
                    }

                    final VariableDefinition other = get(j);

                    if (variable.getSlot() == other.getSlot() &&
                        variable.getScopeEnd() < 0 &&
                        variable.getScopeStart() < other.getScopeStart()) {

                        variable.setScopeEnd(other.getScopeStart());
                        modified = true;
                    }
                }
            }
        }
        while (modified);

        for (int i = 0; i < size(); i++) {
            final VariableDefinition variable = get(i);

            if (variable.getScopeEnd() < 0) {
                variable.setScopeEnd(codeSize);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public final void mergeVariables() {
        final ArrayList<VariableDefinition> slotSharers = new ArrayList<>();

    outer:
        for (int i = 0; i < size(); i++) {
            final VariableDefinition variable = get(i);
            final TypeReference variableType = variable.getVariableType();

            for (int j = 0; j < size(); j++) {
                final VariableDefinition other;

                if (i != j && variable.getSlot() == (other = get(j)).getSlot()) {
                    if (StringUtilities.equals(other.getName(), variable.getName())) {
                        slotSharers.add(other);
                    }
                    else {
                        continue outer;
                    }
                }
            }

            boolean merged = false;
            int minScopeStart = variable.getScopeStart();
            int maxScopeEnd = variable.getScopeEnd();

            for (int j = 0; j < slotSharers.size(); j++) {
                final VariableDefinition slotSharer = slotSharers.get(j);

                if (slotSharer.isFromMetadata() && !StringUtilities.equals(slotSharer.getName(), variable.getName())) {
                    continue;
                }

                final TypeReference slotSharerType = slotSharer.getVariableType();

                if (isTargetTypeCompatible(variableType, slotSharerType) ||
                    isTargetTypeCompatible(slotSharerType, variableType)) {

                    if (slotSharer.getScopeStart() < minScopeStart) {
                        merged = true;
                        minScopeStart = slotSharer.getScopeStart();
                    }

                    if (slotSharer.getScopeEnd() > maxScopeEnd) {
                        merged = true;
                        maxScopeEnd = slotSharer.getScopeEnd();
                    }

                    if (merged) {
                        remove(slotSharer);
                    }

                    if (variable.isFromMetadata()) {
                        continue;
                    }

                    if (!isTargetTypeCompatible(variableType, slotSharerType)) {
                        variable.setVariableType(slotSharerType);
                    }
                }
            }

            if (merged) {
                variable.setScopeStart(minScopeStart);
                variable.setScopeEnd(maxScopeEnd);
            }
        }
    }

    private boolean isTargetTypeCompatible(final TypeReference targetType, final TypeReference sourceType) {
        //noinspection SimplifiableIfStatement
        if (sourceType.isPrimitive() || targetType.isPrimitive()) {
            if (sourceType.isPrimitive() != targetType.isPrimitive()) {
                return false;
            }
        }

        if (MetadataHelper.isAssignableFrom(targetType, sourceType)) {
            return true;
        }

        final SimpleType simpleTarget = targetType.getSimpleType();
        final SimpleType simpleSource = sourceType.getSimpleType();

        if (simpleTarget.isIntegral()) {
            return simpleSource.isIntegral() &&
                   simpleSource.bitWidth() <= simpleTarget.bitWidth();
        }

        if (simpleTarget.isFloating()) {
            return simpleSource.isFloating() &&
                   simpleSource.bitWidth() <= simpleTarget.bitWidth();
        }

        return false;
    }

    /*
    @Override
    protected void afterAdd(final int index, final VariableDefinition v, final boolean appended) {
        v.setIndex(index);

        if (!appended) {
            for (int i = index + 1; i < size(); i++) {
                get(i).setIndex(i + 1);
            }
        }
    }

    @Override
    protected void beforeSet(final int index, final VariableDefinition v) {
        final VariableDefinition current = get(index);

        current.setIndex(-1);

        v.setIndex(index);
    }

    @Override
    protected void afterRemove(final int index, final VariableDefinition v) {
        v.setIndex(-1);

        for (int i = index; i < size(); i++) {
            get(i).setIndex(i);
        }
    }

    @Override
    protected void beforeClear() {
        super.beforeClear();
    }
*/
}
