/*
 * IntelliSpyIntelliSpySourceFinderComponent.java
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.intellij.util.SystemProperties;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class IntelliSpyComponent extends AbstractProjectComponent {
    public static final String REPOSITORY_PATH = new File(SystemProperties.getUserHome(), ".lib-decompiled-src").getPath();
    public static final VirtualFile[] EMPTY_VIRTUAL_FILE_ARRAY = new VirtualFile[0];

    private final Project _project;

    public IntelliSpyComponent(final Project project) {
        super(project);
        this._project = project;
    }

    public static IntelliSpyComponent getInstance(final Project project) {
        return project.getComponent(IntelliSpyComponent.class);
    }

    public VirtualFile[] getRepositoryContents() {
        final VirtualFile repository = LocalFileSystem.getInstance().findFileByPath(REPOSITORY_PATH);

        return repository != null ? repository.getChildren()
                                  : EMPTY_VIRTUAL_FILE_ARRAY;
    }

    @Nullable
    public VirtualFile getSourceRoot(final SourceArtifactId id) {
        for (final VirtualFile virtualFile : getRepositoryContents()) {
            if (isSourceRootFor(id, virtualFile)) {
                return virtualFile;
            }
        }

        return null;
    }

    private boolean isSourceRootFor(final SourceArtifactId id, final VirtualFile virtualFile) {
        final String nameWithoutExtension = virtualFile.getNameWithoutExtension();

        final String hash = nameWithoutExtension.contains("_")
                            ? StringUtils.substringAfterLast(nameWithoutExtension, "_")
                            : nameWithoutExtension;

        return id.getId().equals(hash);
    }

    @Nullable
    public VirtualFile getSourceRoot(final VirtualFile binaryArtifact) {
        return getSourceRoot(calculateId(binaryArtifact));
    }

    @Nullable
    VirtualFile getOrFindSourceRoot(final VirtualFile binaryArtifact) {
        final SourceArtifactId id = calculateId(binaryArtifact);
        final VirtualFile sourceRoot = getSourceRoot(id);

        if (sourceRoot != null) {
            return sourceRoot;
        }

        return findSourceRoot(id, binaryArtifact);
    }

    @Nullable
    public VirtualFile findSourceRoot(final @Nullable SourceArtifactId id, final VirtualFile binaryArtifact) {
        final List<SourceFinder> sourceFinders = Arrays.asList(Extensions.getExtensions(SourceFinder.EXTENSION_POINT_NAME));
        final SourceArtifactId actualId = id != null ? id : calculateId(binaryArtifact);

        sortSourceFinders(sourceFinders);

        final VirtualFile repositoryPath = getOrCreateRepositoryDirectory();

        for (final SourceFinder sourceFinder : sourceFinders) {
            final VirtualFile result = sourceFinder.fetch(actualId, binaryArtifact, repositoryPath);

            if (result != null) {
                return getAsSourceRoot(result);
            }
        }

        return null;
    }

    private static void sortSourceFinders(final List<SourceFinder> sourceFinders) {
        Collections.sort(
            sourceFinders,
            new Comparator<SourceFinder>() {
                public int compare(final SourceFinder o1, final SourceFinder o2) {
                    return Integer.compare(o1.getOrder(), o2.getOrder());
                }
            }
        );
    }

    private VirtualFile getOrCreateRepositoryDirectory() {
        try {
            final VirtualFile repositoryPath = LocalFileSystem.getInstance().findFileByPath(REPOSITORY_PATH);

            if (repositoryPath != null) {
                return repositoryPath;
            }

            final WriteAction<VirtualFile> writeAction = new WriteAction<VirtualFile>() {
                protected void run(final Result<VirtualFile> result) throws Throwable {
                    result.setResult(
                        VfsUtil.createDirectoryIfMissing(IntelliSpyComponent.REPOSITORY_PATH)
                    );
                }
            };

            return writeAction.execute()
                              .throwException()
                              .getResultObject();
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private SourceArtifactId calculateId(final VirtualFile binaryArtifact) {
        return new SourceArtifactId(
            binaryArtifact.getNameWithoutExtension(),
            HashUtilities.calculateSha1Hash(binaryArtifact)
        );
    }

    public VirtualFile getAsSourceRoot(final VirtualFile virtualFile) {
        return SourceRootUtilities.getAsSourceRoot(virtualFile);
    }

    public boolean doAttachSources(final VirtualFile binaryArtifact) {
        VirtualFile sourceRoot = getSourceRoot(binaryArtifact);

        if (sourceRoot != null) {
            attachSources(sourceRoot, binaryArtifact);
            return true;
        }

        sourceRoot = getOrFindSourceRoot(binaryArtifact);

        if (sourceRoot != null) {
            attachSources(sourceRoot, binaryArtifact);
            return true;
        }

        return false;
    }

    public void attachSources(final VirtualFile sourceCodeFile, final VirtualFile binaryFile) {
        final ArrayList<Library.ModifiableModel> modelsToCommit = new ArrayList<Library.ModifiableModel>();
        final List<LibraryOrderEntry> orderEntries = findOrderEntriesContainingFile(binaryFile);

        if (orderEntries == null || orderEntries.isEmpty()) {
            return;
        }

        for (final LibraryOrderEntry libraryOrderEntry : orderEntries) {
            final Library library = libraryOrderEntry.getLibrary();

            if (library != null) {
                final Library.ModifiableModel modifiableModel = library.getModifiableModel();

                modifiableModel.addRoot(sourceCodeFile, OrderRootType.SOURCES);
                modelsToCommit.add(modifiableModel);
            }
        }

        commitModels(modelsToCommit);
    }

    private void commitModels(final List<Library.ModifiableModel> modelsToCommit) {
        ApplicationManager.getApplication().invokeLater(
            new Runnable() {
                public void run() {
                    ApplicationManager.getApplication().runWriteAction(
                        new Runnable() {
                            public void run() {
                                for (final Library.ModifiableModel modifiableModel : modelsToCommit) {
                                    modifiableModel.commit();
                                }
                            }
                        }
                    );
                }
            },
            ModalityState.NON_MODAL
        );
    }

    @Nullable
    private List<LibraryOrderEntry> findOrderEntriesContainingFile(@NotNull final VirtualFile binaryFile) {
        final VirtualFile binaryFileFS = JarFileSystem.getInstance()
                                                      .findFileByPath(binaryFile.getPath() + "!/");

        if (binaryFileFS == null) {
            return Collections.emptyList();
        }

        final List<LibraryOrderEntry> libraries = new ArrayList<LibraryOrderEntry>();

        final List<OrderEntry> entries = ProjectRootManager.getInstance(this._project)
                                                           .getFileIndex()
                                                           .getOrderEntriesForFile(binaryFileFS);

        for (final OrderEntry entry : entries) {
            if (entry instanceof LibraryOrderEntry) {
                libraries.add((LibraryOrderEntry) entry);
            }
        }

        return libraries.isEmpty() ? null : libraries;
    }

    public RefreshResult refreshAttachedSources() {
        final RefreshResult refreshResult = new RefreshResult();
        final List<Library.ModifiableModel> modelsToCommit = new ArrayList<Library.ModifiableModel>();
        final Map<SourceArtifactId, VirtualFile> idToSourceFile = calculateIdsMapForSourceRoots(getRepositoryContents());

        final RefreshAttachedSourcesAction refreshAttachedSourcesAction = new RefreshAttachedSourcesAction(
            refreshResult,
            idToSourceFile,
            modelsToCommit
        );

        ApplicationManager.getApplication().runReadAction(refreshAttachedSourcesAction);
        commitModels(modelsToCommit);

        return refreshResult;
    }

    private SourceArtifactId getIdForSourceRootFile(final VirtualFile sourceRoot) {
        final String name = sourceRoot.getNameWithoutExtension();
        return new SourceArtifactId(StringUtils.substringBeforeLast(name, "_"), StringUtils.substringAfterLast(name, "_"));
    }

    private Map<SourceArtifactId, VirtualFile> calculateIdsMap(final VirtualFile[] files) {
        final Map<SourceArtifactId, VirtualFile> result = new TreeMap<SourceArtifactId, VirtualFile>();

        for (final VirtualFile file : files) {
            if (!file.isDirectory()) {
                result.put(calculateId(file), file);
            }
        }

        return result;
    }

    private Map<SourceArtifactId, VirtualFile> calculateIdsMapForSourceRoots(final VirtualFile[] files) {
        final Map<SourceArtifactId, VirtualFile> result = new TreeMap<SourceArtifactId, VirtualFile>();

        for (final VirtualFile file : files) {
            result.put(getIdForSourceRootFile(file), file);
        }

        return result;
    }

    public void removeSourceRoot(final VirtualFile binaryFile) {
    }

    private final class RefreshAttachedSourcesAction implements Runnable {
        private final RefreshResult _refreshResult;
        private final Map<SourceArtifactId, VirtualFile> _idToSourceFile;
        private final List<Library.ModifiableModel> _modelsToCommit;

        public RefreshAttachedSourcesAction(
            final RefreshResult refreshResult,
            final Map<SourceArtifactId, VirtualFile> idToSourceFile,
            final List<Library.ModifiableModel> modelsToCommit) {

            _refreshResult = refreshResult;
            _idToSourceFile = idToSourceFile;
            _modelsToCommit = modelsToCommit;
        }

        public void run() {
            ProjectRootManager.getInstance(_project)
                              .orderEntries()
                              .librariesOnly()
                              .forEachLibrary(new LibraryProcessor());
        }

        private final class LibraryProcessor implements Processor<Library> {
            public boolean process(final Library library) {
                final Library.ModifiableModel modifiableModel = library.getModifiableModel();
                final Map<SourceArtifactId, VirtualFile> idToBinaryFile = new TreeMap<SourceArtifactId, VirtualFile>();

                idToBinaryFile.putAll(calculateIdsMap(library.getFiles(OrderRootType.CLASSES)));

                final VirtualFile[] sourceRoots = library.getFiles(OrderRootType.SOURCES);
                final List<VirtualFile> remainingSourceRoots = new ArrayList<VirtualFile>();

                for (final VirtualFile sourceRoot : sourceRoots) {
                    final VirtualFile sourceRootFile = SourceRootUtilities.getAsPhysical(sourceRoot);

                    if (!sourceRoot.isValid() ||
                        (VfsUtil.isAncestor(getOrCreateRepositoryDirectory(), sourceRootFile, true) &&
                         !idToBinaryFile.containsKey(getIdForSourceRootFile(sourceRoot)))) {

                        modifiableModel.removeRoot(sourceRoot.getUrl(), OrderRootType.SOURCES);
                        _refreshResult.incrementRemovedFiles();
                    }
                    else {
                        remainingSourceRoots.add(sourceRootFile);
                    }
                }

                for (final Map.Entry<SourceArtifactId, VirtualFile> entry : _idToSourceFile.entrySet()) {
                    if (!remainingSourceRoots.contains(SourceRootUtilities.getAsPhysical(entry.getValue()))) {
                        if (idToBinaryFile.containsKey(entry.getKey())) {
                            modifiableModel.addRoot(
                                getAsSourceRoot(entry.getValue()),
                                OrderRootType.SOURCES
                            );

                            _refreshResult.incrementAddedFiles();
                        }
                    }
                }

                _modelsToCommit.add(modifiableModel);
                return true;
            }
        }
    }
}
