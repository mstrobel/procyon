/*
 * DecompilerComponent.java
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

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class JavaDecompilerComponent implements ApplicationComponent {
    public static final String DECOMPILER_INTELLIJ_ID = "decompiler-intellij";

    @Override
    public void initComponent() {
    } // nop

    @Override
    public void disposeComponent() {
    } // nop

    @NotNull
    @Override
    public String getComponentName() {
        return "Java Decompiler Plugin";
    }
/*

    @Nls
    @Override
    public String getDisplayName() {
        return "Java Decompiler";
    }

    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("main/resources/icons/jd_64.png");
    }

    @Override
    public String getHelpTopic() {
        return null;
    } // nop

    @Override
    public JComponent createComponent() {
        if (configPane == null) {
            configPane = new JDPluginConfigurationPane();
        }
        return configPane.getRootPane();
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @Override
    public void reset() {
    }

    @Override
    public void disposeUIResources() {
    }
*/
}

