/*
 * JavaDecompilerClassFileProcessor.java
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

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileTypes.ContentBasedClassFileProcessor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.strobel.core.BooleanBox;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.UUID;

public class JavaDecompilerClassFileProcessor implements ContentBasedClassFileProcessor {

    private final JavaDecompilerService _javaDecompilerService;
//    private final JavaDecompilerComponent _jdPluginComponent = ApplicationManager.getApplication().getComponent(JavaDecompilerComponent.class);

    public JavaDecompilerClassFileProcessor() {
        _javaDecompilerService = ServiceManager.getService(JavaDecompilerService.class);
    }

    @Override
    public boolean isApplicable(final Project project, final VirtualFile virtualFile) {
        return virtualFile.getFileType() == StdFileTypes.CLASS;
    }

    @NotNull
    @Override
    public SyntaxHighlighter createHighlighter(final Project project, final VirtualFile vFile) {
        //noinspection ConstantConditions
        return SyntaxHighlighterFactory.getSyntaxHighlighter(StdFileTypes.JAVA, project, createSourceFile(vFile));
    }

    @NotNull
    @Override
    public String obtainFileText(final Project project, final VirtualFile virtualFile) {
        ServiceManager.getService(JavaDecompilerRefreshSupportService.class).markDecompiled(virtualFile);
        return _javaDecompilerService.decompile(project, virtualFile);
    }

    @Override
    public Language obtainLanguageForFile(final VirtualFile virtualFile) {
        if (virtualFile.getFileType() == StdFileTypes.CLASS) {
            return null;
        }
        else if (virtualFile.getFileType() == StdFileTypes.JAVA) {
            return Language.findLanguageByID("JAVA");
        }
        return Language.ANY;
    }

    private static VirtualFile createSourceFile(final VirtualFile originalFile) {
        try {
            final Reference<Document> documentRef = originalFile.getUserData(FileDocumentManagerImpl.DOCUMENT_KEY);

            if (documentRef == null) {
                return originalFile;
            }

            final Document document = documentRef.get();

            if (document == null) {
                return originalFile;
            }

            final VirtualFileSystem tempFs = VirtualFileManager.getInstance().getFileSystem("temp");

            if (tempFs == null) {
                return originalFile;
            }

            final VirtualFile root = tempFs.findFileByPath("/");

            if (root == null) {
                return originalFile;
            }

            final VirtualFile tempFile = root.createChildData(null, UUID.randomUUID().toString() + ".java");

//            tempFile.putUserData(FileDocumentManagerImpl.DOCUMENT_KEY, documentRef);
            tempFile.setCharset(Charset.defaultCharset());

            final ByteBuffer encodedText = Charset.defaultCharset().encode(document.getText());
            final byte[] contents = new byte[encodedText.remaining()];

            encodedText.get(contents);

            final BooleanBox succeeded = new BooleanBox();

            ApplicationManager.getApplication().runWriteAction(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tempFile.setBinaryContent(contents);
                        }
                        catch (Throwable t) {
                            succeeded.set(false);
                        }
                    }
                }
            );

            return succeeded.get() ? tempFile : originalFile;
        }
        catch (Throwable t) {
            return originalFile;
        }
    }
}

