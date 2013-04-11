/*
 * JavaDecompilerRefreshSupportService.java
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

import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class JavaDecompilerRefreshSupportService {
    private final ConcurrentHashMap<WeakReference<VirtualFile>, WeakReference<VirtualFile>> _decompiledFiles;

    public JavaDecompilerRefreshSupportService() {
        _decompiledFiles = new ConcurrentHashMap<WeakReference<VirtualFile>, WeakReference<VirtualFile>>();
    }

    public void markDecompiled(final VirtualFile virtualFile) {
        final WeakReference<VirtualFile> weakRef = new WeakReference<VirtualFile>(virtualFile);
        _decompiledFiles.put(weakRef, weakRef);
    }

    public void refreshDecompiledFiles() {
        LaterInvocator.invokeLater(new RefreshDecompiledFilesTask());
    }

    private class RefreshDecompiledFilesTask implements Runnable {
        @Override
        public void run() {
            final FileDocumentManager documentManager = FileDocumentManager.getInstance();
            final VirtualFileListener listener = (VirtualFileListener) documentManager;

            // need lock ?
            final HashSet<WeakReference<VirtualFile>> weakReferences = new HashSet<WeakReference<VirtualFile>>(_decompiledFiles.keySet());

            _decompiledFiles.clear();

            for (final WeakReference<VirtualFile> virtualFileWeakReference : weakReferences) {
                final VirtualFile virtualFile = virtualFileWeakReference.get();

                if (virtualFile != null) {
                    listener.contentsChanged(
                        new VirtualFileEvent(
                            null,
                            virtualFile,
                            virtualFile.getName(),
                            virtualFile.getParent()
                        )
                    );
                }
            }
        }
    }
}
