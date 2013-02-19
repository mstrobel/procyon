/*
 * Node.java
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

import com.strobel.core.Predicate;
import com.strobel.decompiler.ITextOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Node {
    public abstract void writeTo(final ITextOutput output);

    // <editor-fold defaultstate="collapsed" desc="Enumeration Methods">

    public List<Node> getChildren() {
        return Collections.emptyList();
    }

    public final List<Node> getSelfAndChildrenRecursive() {
        final ArrayList<Node> results = new ArrayList<>();
        accumulateSelfAndChildrenRecursive(results, Node.class, null);
        return results;
    }

    public final List<Node> getSelfAndChildrenRecursive(final Predicate<Node> predicate) {
        final ArrayList<Node> results = new ArrayList<>();
        accumulateSelfAndChildrenRecursive(results, Node.class, predicate);
        return results;
    }

    public final <T extends Node> List<T> getSelfAndChildrenRecursive(final Class<T> type) {
        final ArrayList<T> results = new ArrayList<>();
        accumulateSelfAndChildrenRecursive(results, type, null);
        return results;
    }

    public final <T extends Node> List<T> getSelfAndChildrenRecursive(final Class<T> type, final Predicate<T> predicate) {
        final ArrayList<T> results = new ArrayList<>();
        accumulateSelfAndChildrenRecursive(results, type, predicate);
        return results;
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> void accumulateSelfAndChildrenRecursive(
        final List<T> list,
        final Class<T> type,
        final Predicate<T> predicate) {

        if (type.isInstance(this) && (predicate == null || predicate.test((T)this))) {
            list.add((T)this);
        }

        for (final Node child : getChildren()) {
            child.accumulateSelfAndChildrenRecursive(list, type, predicate);
        }
    }

    // </editor-fold>
}
