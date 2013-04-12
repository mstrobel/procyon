/*
 * JavaDecompilerService.java
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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.strobel.core.StringUtilities;
import com.strobel.util.ContractUtils;

import java.util.Iterator;

public class JavaDecompilerService {
    private final static Key<JavaDecompiler> DECOMPILER_KEY = Key.create("JavaDecompiler");

    public JavaDecompilerService() {
    }

    public String decompile(final Project project, final VirtualFile virtualFile) {
        JavaDecompiler javaDecompiler = project.getUserData(DECOMPILER_KEY);

        if (javaDecompiler == null) {
            javaDecompiler = new JavaDecompiler(project);
            project.putUserData(DECOMPILER_KEY, javaDecompiler);
        }

        // for jars only
        final String filePath = virtualFile.getPath();
        final ProjectRootManager rootManager = ProjectRootManager.getInstance(project);
        final VirtualFile classRootForFile = rootManager.getFileIndex().getClassRootForFile(virtualFile);

        if (classRootForFile != null) {
            final String basePath = classRootForFile.getPresentableUrl();
            final int jarSeparatorIndex = filePath.indexOf('!');

            final String internalClassName;

            if (jarSeparatorIndex < 0) {
                internalClassName = filePath.substring(basePath.length(), filePath.length());
            }
            else {
                internalClassName = filePath.substring(jarSeparatorIndex + 2, filePath.length());
            }

            try {
                final String decompiled = javaDecompiler.decompile(
                    filePath,
                    basePath,
                    StringUtilities.removeRight(
                        StringUtilities.removeLeft(internalClassName, "/"),
                        ".class"
                    )
                );

                if (validContent(decompiled)) {
                    return StringUtil.convertLineSeparators(decompiled);
                }
            }
            catch (Throwable ignored) {
                ignored.printStackTrace(System.err);
            }
        }

        // for other files if possible
        for (final DecompilerPathArgs decompilerPathArgs : new DecompilerPathArgsFinder(virtualFile)) {
            final String decompiled = javaDecompiler.decompile(
                filePath,
                decompilerPathArgs.getBasePath(),
                decompilerPathArgs.getInternalClassName()
            );

            if (validContent(decompiled)) {
                return StringUtil.convertLineSeparators(decompiled);
            }
        }

        // fallback
        return ClsFileImpl.decompile(PsiManager.getInstance(project), virtualFile);
    }

    private boolean validContent(final String decompiled) {
        return decompiled != null && !decompiled.startsWith("!!!");
    }

    private static class DecompilerPathArgsFinder implements Iterable<DecompilerPathArgs> {
        private final VirtualFile virtualFile;

        public DecompilerPathArgsFinder(final VirtualFile virtualFile) {
            this.virtualFile = virtualFile;
        }

        @Override
        public Iterator<DecompilerPathArgs> iterator() {
            return new Iterator<DecompilerPathArgs>() {
                private VirtualFile classPathRoot = virtualFile.getParent();

                @Override
                public boolean hasNext() {
                    return classPathRootIsNotRootDirectory();
                }

                private boolean classPathRootIsNotRootDirectory() {
                    return classPathRoot != null;
                }

                @Override
                public DecompilerPathArgs next() {
                    try {
                        final String internalClassName = VfsUtil.getRelativePath(virtualFile, classPathRoot, '/');
                        return new DecompilerPathArgs(classPathRoot.getPresentableUrl(), internalClassName);
                    }
                    finally {
                        classPathRoot = classPathRoot.getParent();
                    }
                }

                @Override
                public void remove() {
                    throw ContractUtils.unsupported();
                }
            };
        }
    }

    /**
     * Java Decompiler path arguments. <p/> Composed of the <em>base name</em> and the <em>qualified name</em>.
     */
    private static class DecompilerPathArgs {
        private final String basePath;
        private final String internalClassName;

        public DecompilerPathArgs(final String basePath, final String internalClassName) {
            this.basePath = basePath;
            this.internalClassName = internalClassName;
        }

        public String getBasePath() {
            return basePath;
        }

        public String getInternalClassName() {
            return internalClassName;
        }
    }
}
