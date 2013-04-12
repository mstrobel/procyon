/*
 * java
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

import com.intellij.concurrency.JobScheduler;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class IntelliSpyProjectInitializer extends AbstractProjectComponent {
    private final Object _refreshLock = new Object();
    private final Project _project;
    private VirtualFileAdapter _listener;
    private ScheduledFuture<?> _task;

    public IntelliSpyProjectInitializer(final Project project) {
        super(project);
        _project = project;
    }

    public void projectOpened() {
        _listener = new Listener();
        VirtualFileManager.getInstance().addVirtualFileListener(_listener);

        refreshAndDisplayNotification();
    }

    private void refreshAndDisplayNotification() {
        final RefreshResult result = IntelliSpyComponent.getInstance(_project).refreshAttachedSources();

        final String text = String.format(
            "Added %d roots, removed %d roots.",
            result.getAddedFiles(),
            result.getRemovedFiles()
        );

        Notifications.Bus.notify(
            new Notification(
                "IntelliSpy",
                "Source roots refresh completed.",
                text,
                NotificationType.INFORMATION
            ),
            _project
        );
    }

    public void projectClosed() {
        VirtualFileManager.getInstance().removeVirtualFileListener(_listener);
    }

    private class Listener extends VirtualFileAdapter {
        public void fileCreated(final VirtualFileEvent event) {
            if (!applicable(event.getFile())) {
                return;
            }

            fireFileStateChanged(event);
        }

        public void fileDeleted(final VirtualFileEvent event) {
            if (!applicable(event.getFile())) {
                return;
            }

            fireFileStateChanged(event);
        }

        @SuppressWarnings("UnusedParameters")
        private void fireFileStateChanged(final VirtualFileEvent event) {
            synchronized (_refreshLock) {
                if (_task != null) {
                    _task.cancel(false);
                }

                _task = JobScheduler.getScheduler().scheduleWithFixedDelay(
                    new Runnable() {
                        public void run() {
                            refreshAndDisplayNotification();
                        }
                    },
                    1L,
                    1800L,
                    TimeUnit.SECONDS
                );
            }
        }

        private boolean applicable(final VirtualFile file) {
            return file.getFileType() == StdFileTypes.ARCHIVE;
        }
    }
}
