/*
 * SourceArtifactId.java
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

package com.strobel.decompiler.ide.intellij;

public final class SourceArtifactId implements Comparable<SourceArtifactId> {
    private final String _prefix;
    private final String _id;

    public SourceArtifactId(final String prefix, final String id) {
        _prefix = prefix;
        _id = id;
    }

    public String getPrefix() {
        return _prefix;
    }

    public String getId() {
        return _id;
    }

    public String toString() {
        return _prefix + "_" + _id;
    }

    public int compareTo(final SourceArtifactId o) {
        return getId().compareTo(o.getId());
    }
}