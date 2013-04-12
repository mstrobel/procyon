/*
 * JavaDecompilerSourceFinder.java
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

import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.FilteringProcessor;
import com.intellij.util.Processor;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.CompositeTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class JavaDecompilerSourceFinder extends SourceFinder {
    private final static Logger _logger = Logger.getInstance(JavaDecompilerSourceFinder.class);

    @Override
    public VirtualFile fetch(
        final SourceArtifactId sourceArtifactId,
        final VirtualFile binaryArtifact,
        final VirtualFile repositoryPath) {

        final VirtualFile artifactRoot;

        if (binaryArtifact.isDirectory()) {
            artifactRoot = binaryArtifact;
        }
        else {
            artifactRoot = JarFileSystem.getInstance().getJarRootForLocalFile(binaryArtifact);
        }

        if (artifactRoot == null) {
            return null;
        }

        final WriteAction<VirtualFile> action = new WriteAction<VirtualFile>() {
            protected void run(final Result<VirtualFile> result) throws Throwable {
                final ITypeLoader typeLoader = new CompositeTypeLoader(
                    new VirtualFileTypeLoader(artifactRoot),
                    new ClasspathTypeLoader()
                );

                final DecompilerSettings settings = new DecompilerSettings();
                final MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
                final VirtualFile repositoryRoot = repositoryPath.createChildDirectory(null, sourceArtifactId.getId());

                VfsUtil.processFilesRecursively(
                    artifactRoot,
                    new FilteringProcessor<VirtualFile>(
                        new Condition<VirtualFile>() {
                            @Override
                            public boolean value(final VirtualFile virtualFile) {
                                return true;
                            }
                        },
                        new Processor<VirtualFile>() {
                            @Override
                            public boolean process(final VirtualFile virtualFile) {
                                try {
                                    if (!"class".equals(virtualFile.getExtension())) {
                                        return true;
                                    }

                                    final String internalName = StringUtilities.removeRight(
                                        StringUtilities.removeLeft(
                                            VfsUtil.getRelativePath(virtualFile, artifactRoot, '/'),
                                            "/"
                                        ),
                                        ".class"
                                    );

                                    final TypeReference type = metadataSystem.lookupType(internalName);

                                    if (type == null) {
                                        return true;
                                    }

                                    final PlainTextOutput output = new PlainTextOutput();

                                    final TypeDefinition resolvedType;

                                    if (type == null || (resolvedType = type.resolve()) == null) {
                                        return false;
                                    }

                                    final DecompilationOptions options = new DecompilationOptions();

                                    settings.setTypeLoader(typeLoader);

                                    options.setSettings(settings);
                                    options.setFullDecompilation(true);

                                    if (settings.getFormattingOptions() == null) {
                                        settings.setFormattingOptions(JavaFormattingOptions.createDefault());
                                    }

                                    settings.setOutputFileHeaderText(
                                        "\nOriginal source code not available.  Decompiled sources generated by IntelliSpy.\n" +
                                        virtualFile.getPath() + "\n"
                                    );

                                    settings.getLanguage().decompileType(resolvedType, output, options);

                                    final VirtualFile directory = VfsUtil.createDirectoryIfMissing(
                                        repositoryRoot,
                                        VfsUtil.getRelativePath(virtualFile.getParent(), artifactRoot, '/')
                                    );

                                    final VirtualFile outputFile = directory.createChildData(
                                        null,
                                        virtualFile.getNameWithoutExtension() + ".java"
                                    );

                                    outputFile.setCharset(Charset.defaultCharset());

                                    final ByteBuffer encodedText = Charset.defaultCharset().encode(output.toString());
                                    final byte[] contents = new byte[encodedText.remaining()];

                                    encodedText.get(contents);
                                    outputFile.setBinaryContent(contents);

                                    return true;
                                }
                                catch (Throwable t) {
                                    _logger.error("Uncaught exception", t);
                                }

                                return true;
                            }
                        }
                    )
                );

                if (ArrayUtilities.isNullOrEmpty(repositoryRoot.getChildren())) {
                    try {
                        repositoryRoot.delete(null);
                    }
                    catch (Throwable ignored) {
                    }

                    result.setResult(null);

                    return;
                }

                result.setResult(repositoryRoot);
            }
        };

        return action.execute()
                     .throwException()
                     .getResultObject();
    }

    private final static class VirtualFileTypeLoader implements ITypeLoader {
        private final VirtualFile _root;

        VirtualFileTypeLoader(final VirtualFile root) {
            _root = root;
        }

        @Override
        public final boolean tryLoadType(final String internalName, final Buffer buffer) {
            final InputStream stream;
            final VirtualFile classFile = _root.findFileByRelativePath(internalName + ".class");

            if (classFile == null) {
                stream = ClassLoader.getSystemClassLoader().getResourceAsStream(internalName + ".class");

                if (stream == null) {
                    return false;
                }
            }
            else {
                try {
                    stream = classFile.getInputStream();
                }
                catch (Throwable t) {
                    t.printStackTrace(System.err);
                    return false;
                }
            }

            try {
                int remainingBytes = stream.available();

                buffer.position(0);
                buffer.reset(remainingBytes);

                while (remainingBytes > 0) {
                    final int bytesRead = stream.read(buffer.array(), buffer.position(), remainingBytes);

                    if (bytesRead < 0) {
                        break;
                    }

                    remainingBytes -= bytesRead;
                    buffer.advance(bytesRead);
                }

                buffer.position(0);

                return true;
            }
            catch (IOException e) {
                e.printStackTrace(System.err);
                return false;
            }
            finally {
                try {
                    stream.close();
                }
                catch (IOException ignored) {
                }
            }
        }
    }
}
