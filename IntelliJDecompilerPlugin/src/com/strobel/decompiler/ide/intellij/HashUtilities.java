/*
 * HashUtils.java
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.strobel.core.ExceptionUtilities;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.InputStream;

final class HashUtilities {
    public static String calculateSha1Hash(final VirtualFile binaryJarFile) {
        final VirtualFile physicalFile = SourceRootUtilities.getAsPhysical(binaryJarFile);

        InputStream inputStream = null;

        try {
            if (physicalFile.isDirectory()) {
                return DigestUtils.shaHex(physicalFile.getCanonicalPath());
            }

            inputStream = physicalFile.getInputStream();
            final String hash = DigestUtils.shaHex(StreamUtil.loadFromStream(inputStream));
            Logger.getInstance(HashUtilities.class).info(physicalFile + " -> " + hash);
            return hash;
        }
        catch (Throwable t) {
            throw ExceptionUtilities.asRuntimeException(t);
        }
        finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            }
            catch (Throwable ignored) {
            }
        }
    }
}