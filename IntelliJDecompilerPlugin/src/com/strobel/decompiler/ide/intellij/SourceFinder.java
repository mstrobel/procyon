/*
 * SourceFinder.java
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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;

public abstract class SourceFinder {
    public static final String NAME = "com.intellij.sourceFinderDelegate";
    public static final ExtensionPointName<SourceFinder> EXTENSION_POINT_NAME;

    static {
        EXTENSION_POINT_NAME = new ExtensionPointName<SourceFinder>("com.intellij.sourceFinderDelegate");
    }

    public static final int ORDERED_FIRST = -2147483648;
    public static final int ORDERED_LAST = 2147483647;

    public abstract VirtualFile fetch(
        final SourceArtifactId sourceArtifactId,
        final VirtualFile binaryArtifact,
        final VirtualFile repositoryPath
    );

    public int getOrder() {
        return 0;
    }
}