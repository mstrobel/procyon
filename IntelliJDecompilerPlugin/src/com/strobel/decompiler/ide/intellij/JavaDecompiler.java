/*
 * JavaDecompiler.java
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
import com.intellij.openapi.roots.libraries.LibrariesHelper;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.ArrayUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class JavaDecompiler {
    private final Project _project;
    private final ITypeLoader _typeLoader;
    private MetadataSystem _metadataSystem;

    public JavaDecompiler(final Project project) {
        _project = project;
        _typeLoader = new TypeLoader();
        _metadataSystem = new MetadataSystem(_typeLoader);
    }

    public String decompile(final String basePath, final String internalName) {
        VerifyArgument.notNull(internalName, "internalName");

        final DecompilerSettings settings = new DecompilerSettings();
        final MetadataSystem metadataSystem = _metadataSystem;
        final TypeReference type = metadataSystem.lookupType(internalName);
        final PlainTextOutput output = new PlainTextOutput();

        final TypeDefinition resolvedType;

        if (type == null || (resolvedType = type.resolve()) == null) {
            return null;
        }

        final DecompilationOptions options = new DecompilationOptions();

        settings.setTypeLoader(_typeLoader);

        options.setSettings(settings);
        options.setFullDecompilation(true);

        if (settings.getFormattingOptions() == null) {
            settings.setFormattingOptions(JavaFormattingOptions.createDefault());
        }

        settings.getLanguage().decompileType(resolvedType, output, options);

        return output.toString();
    }

    private final class TypeLoader implements ITypeLoader {

        private final List<VirtualFile> _libraryRoots;

        TypeLoader() {
            _libraryRoots = ArrayUtilities.asUnmodifiableList(LibraryUtil.getLibraryRoots(_project));
        }

        @Override
        public final boolean tryLoadType(final String internalName, final Buffer buffer) {
            final String dottedName = internalName.replace('/', '.');
            final VirtualFile root = LibrariesHelper.getInstance().findRootByClass(_libraryRoots, dottedName);

            final InputStream stream;

            if (root == null) {
                stream = ClassLoader.getSystemClassLoader().getResourceAsStream(internalName + ".class");

                if (stream == null) {
                    return false;
                }
            }
            else {
                final VirtualFile classFile = root.findFileByRelativePath(internalName + ".class");

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
            }

            try {
                int remainingBytes = stream.available();

                buffer.position(0);
                buffer.reset(remainingBytes);

                while (remainingBytes > 0) {
                    final int bytesRead = stream.read(buffer.array(), buffer.position(), remainingBytes);

                    if (bytesRead < 0)
                        break;

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
