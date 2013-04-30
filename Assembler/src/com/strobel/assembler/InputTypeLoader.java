/*
 * ClassFileResolver.java
 *
 * Copyright (c) 2013 Mike Strobel
 *
 * This source code is based on Mono.Cecil from Jb Evain, Copyright (c) Jb Evain;
 * and ILSpy/ICSharpCode from SharpDevelop, Copyright (c) AlphaSierraPapa.
 *
 * This source code is subject to terms and conditions of the Apache License, Version 2.0.
 * A copy of the license can be found in the License.html file at the root of this distribution.
 * By using this source code in any fashion, you are agreeing to be bound by the terms of the
 * Apache License, Version 2.0.
 *
 * You must not remove this notice, or any other, from this software.
 */

package com.strobel.assembler;

import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.io.PathHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InputTypeLoader implements ITypeLoader {
    private final ITypeLoader _defaultTypeLoader;
    private final Map<String, List<File>> _knownLocations;

    public InputTypeLoader() {
        this(new ClasspathTypeLoader());
    }

    public InputTypeLoader(final ITypeLoader defaultTypeLoader) {
        _defaultTypeLoader = VerifyArgument.notNull(defaultTypeLoader, "defaultTypeLoader");
        _knownLocations = new LinkedHashMap<>();
    }

    @Override
    public boolean tryLoadType(final String typeNameOrPath, final Buffer buffer) {
        VerifyArgument.notNull(typeNameOrPath, "typeNameOrPath");
        VerifyArgument.notNull(buffer, "buffer");

        final boolean hasExtension = StringUtilities.endsWithIgnoreCase(typeNameOrPath, ".class");

        final String internalName = (hasExtension ? typeNameOrPath.substring(0, typeNameOrPath.length() - 6)
                                                  : typeNameOrPath).replace('.', '/');

        if (hasExtension && tryLoadFile(null, typeNameOrPath, buffer)) {
            return true;
        }

        if (PathHelper.isPathRooted(typeNameOrPath)) {
            return false;
        }

        if (_defaultTypeLoader.tryLoadType(internalName, buffer)) {
            return true;
        }

        if (tryLoadFromKnownLocation(internalName, buffer)) {
            return true;
        }

        final String filePath = internalName.replace('/', File.separatorChar) + ".class";

        if (tryLoadFile(internalName, filePath, buffer)) {
            return true;
        }

        final int lastSeparatorIndex = filePath.lastIndexOf(File.separatorChar);

        return lastSeparatorIndex >= 0 &&
               tryLoadFile(internalName, filePath.substring(lastSeparatorIndex + 1), buffer);
    }

    private boolean tryLoadFromKnownLocation(final String internalName, final Buffer buffer) {
        final int packageEnd = internalName.lastIndexOf('/');

        final String className;
        final String packageName;

        if (packageEnd < 0 || packageEnd >= internalName.length()) {
            packageName = StringUtilities.EMPTY;
            className = internalName;
        }
        else {
            packageName = internalName.substring(0, packageEnd);
            className = internalName.substring(packageEnd + 1);
        }

        final List<File> directories = _knownLocations.get(packageName);

        if (directories == null) {
            return false;
        }

        for (final File directory : directories) {
            if (tryLoadFile(internalName, new File(directory, className + ".class").getAbsolutePath(), buffer)) {
                return true;
            }
        }

        return false;
    }

    private boolean tryLoadFile(final String internalName, final String typeNameOrPath, final Buffer buffer) {
        try {
            final File file = new File(typeNameOrPath);

            if (!file.exists() || file.isDirectory()) {
                return false;
            }

            try (final FileInputStream in = new FileInputStream(file)) {
                int remainingBytes = in.available();

                buffer.position(0);
                buffer.reset(remainingBytes);

                while (remainingBytes > 0) {
                    final int bytesRead = in.read(buffer.array(), buffer.position(), remainingBytes);

                    if (bytesRead < 0) {
                        break;
                    }

                    remainingBytes -= bytesRead;
                    buffer.advance(bytesRead);
                }

                buffer.position(0);

                final String name = internalName != null ? internalName : getInternalNameFromClassFile(buffer);

                final boolean result = internalName == null ||
                                       typeNameOrPath.endsWith(name.replace('/', File.separatorChar) + ".class") ||
                                       verifyClassName(buffer, name);

                if (result) {
                    final int packageEnd = name.lastIndexOf('/');
                    final String packageName;

                    if (packageEnd < 0 || packageEnd >= name.length()) {
                        packageName = StringUtilities.EMPTY;
                    }
                    else {
                        packageName = name.substring(0, packageEnd);
                    }

                    List<File> directories = _knownLocations.get(packageName);

                    if (directories == null) {
                        _knownLocations.put(packageName, directories = new ArrayList<>());
                    }

                    final File directory = file.getParentFile();

                    if (!directories.contains(directory)) {
                        directories.add(directory);
                    }
                }

                return result;
            }
        }
        catch (IOException e) {
            return false;
        }
    }

    private static boolean verifyClassName(final Buffer b, final String internalName) {
        try {
            return StringUtilities.equals(getInternalNameFromClassFile(b), internalName);
        }
        catch (Throwable t) {
            return false;
        }
    }

    private static String getInternalNameFromClassFile(final Buffer b) {
        final long magic = b.readInt() & 0xFFFFFFFFL;

        if (magic != 0xCAFEBABEL) {
            return null;
        }

        b.readUnsignedShort(); // minor version
        b.readUnsignedShort(); // major version

        final ConstantPool constantPool = ConstantPool.read(b);

        b.readUnsignedShort(); // access flags

        final ConstantPool.TypeInfoEntry thisClass = constantPool.getEntry(b.readUnsignedShort());

        b.position(0);

        return thisClass.getName();
    }
}
