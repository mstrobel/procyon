/*
 * RefreshResult.java
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

public class RefreshResult {
    private int addedFiles;
    private int removedFiles;

    public void incrementAddedFiles() {
        this.addedFiles += 1;
    }

    public void incrementRemovedFiles() {
        this.removedFiles += 1;
    }

    public int getAddedFiles() {
        return this.addedFiles;
    }

    public int getRemovedFiles() {
        return this.removedFiles;
    }
}