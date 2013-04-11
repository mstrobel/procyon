/*
 * DecompilerIconProvider.java
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

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class JavaDecompilerIconProvider extends IconProvider {

    private static final String JD_ICON_URL = "/resources/icons/jd_16.png";

    public Icon getIcon(@NotNull final PsiElement psiElement, final int flags) {
        final PsiFile containingFile = psiElement.getContainingFile();
        if (containingFile != null && containingFile.getName().endsWith(".class")) {
            return IconLoader.getIcon(JD_ICON_URL);
        }
        return null;
    }
}
