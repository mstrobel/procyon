/*
 * SourceRootUtilities.java
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

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.strobel.util.ContractUtils;

final class SourceRootUtilities {
    private SourceRootUtilities() {
        throw ContractUtils.unreachable();
    }

    public static VirtualFile getAsSourceRoot(final VirtualFile sourceRoot) {
        if (sourceRoot.isDirectory()) {
            return sourceRoot;
        }
        if (sourceRoot.getFileType() == StdFileTypes.ARCHIVE) {
            return JarFileSystem.getInstance().getJarRootForLocalFile(sourceRoot);
        }
        return sourceRoot;
    }

    public static VirtualFile getAsPhysical(final VirtualFile sourceRoot) {
        return PathUtil.getLocalFile(sourceRoot);
    }
}