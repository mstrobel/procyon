/*
 * JavaDecompilerAttachSourcesProvider.java
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

import com.intellij.codeInsight.AttachSourcesProvider;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.RunBackgroundable;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.strobel.core.ExceptionUtilities;
import com.strobel.core.VerifyArgument;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class JavaDecompilerAttachSourcesProvider implements AttachSourcesProvider {
    @NotNull
    @Override
    public Collection<AttachSourcesAction> getActions(final List<LibraryOrderEntry> libraryOrderEntries, final PsiFile psiFile) {
        return Collections.<AttachSourcesAction>singletonList(new DecompileAction(psiFile));
    }

    private final static class DecompileAction implements AttachSourcesAction {
        private final PsiFile _psiFile;

        private DecompileAction(final PsiFile psiFile) {
            _psiFile = psiFile;
        }

        @Override
        public String getName() {
            return "Decompile";
        }

        @Override
        public String getBusyText() {
            return "Decompiling...";
        }

        @Override
        public ActionCallback perform(final List<LibraryOrderEntry> libraryOrderEntries) {
            final ActionCallback actionCallback = new ActionCallback();
            final Project project = _psiFile.getProject();
            final IntelliSpyComponent component = IntelliSpyComponent.getInstance(project);

            RunBackgroundable.run(
                new Task.Backgroundable(project, "Decompiling sources...") {
                    @Override
                    public void run(@NotNull final ProgressIndicator progressIndicator) {
                        VerifyArgument.notNull(progressIndicator, "progressIndicator");

                        try {
                            final VirtualFile binaryArtifact = findRoot(libraryOrderEntries, _psiFile.getVirtualFile());

                            if (component.doAttachSources(binaryArtifact)) {
                                actionCallback.setDone();
                            }
                            else {
                                displayFailureMessage(new RuntimeException("An unknown error occurred."));
                            }
                        }
                        catch (Throwable t) {
                            Logger.getInstance(this.getClass()).error("Uncaught exception", t);

                            displayFailureMessage(t);
                        }
                        finally {
                            if (!actionCallback.isProcessed()) {
                                actionCallback.setRejected();
                            }
                        }
                    }
                }
            );

            return actionCallback;
        }

        private void displayFailureMessage(final Throwable t) {
            SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        final String text = format(
                            "<html>" +
                            "<p>Maybe you'd like to <a href=\"openBrowser\" target=\"_top\">file a bug report</a>?</p>" +
                            "<p>Include this information in the report:</p>" +
                            "<pre>%s</pre>" +
                            "</html>",
                            ExceptionUtilities.getStackTraceString(t));

                        Notifications.Bus.notify(
                            new Notification(
                                "IntelliSpy",
                                "Unable to decompile sources.",
                                text,
                                NotificationType.WARNING,
                                new NotificationListener() {
                                    public void hyperlinkUpdate(final Notification notification, final HyperlinkEvent event) {
                                        if (notification == null || event == null) {
                                            return;
                                        }

                                        if (event.getDescription().equals("openBrowser")) {
                                            BrowserUtil.launchBrowser("https://bitbucket.org/mstrobel/com.strobel/issues");
                                        }
                                    }
                                }
                            ), _psiFile.getProject()
                        );
                    }
                }
            );
        }
    }

    private static VirtualFile findRoot(final List<LibraryOrderEntry> libraryOrderEntries, final VirtualFile file) {
        for (final LibraryOrderEntry libraryOrderEntry : libraryOrderEntries) {
            for (final VirtualFile classesRoot : libraryOrderEntry.getFiles(OrderRootType.CLASSES)) {
                if (VfsUtil.isAncestor(classesRoot, file, true)) {
                    return SourceRootUtilities.getAsPhysical(classesRoot);
                }
            }
        }
        return null;
    }
}
