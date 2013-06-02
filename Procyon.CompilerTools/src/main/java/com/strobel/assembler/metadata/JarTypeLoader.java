package com.strobel.assembler.metadata;

import com.strobel.core.ExceptionUtilities;
import com.strobel.core.VerifyArgument;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarTypeLoader implements ITypeLoader {
    private final JarFile _jarFile;

    public JarTypeLoader(final JarFile jarFile) {
        _jarFile = VerifyArgument.notNull(jarFile, "jarFile");
    }

    @Override
    public boolean tryLoadType(final String internalName, final Buffer buffer) {
        try {
            final JarEntry entry = _jarFile.getJarEntry(internalName + ".class");

            if (entry == null) {
                return false;
            }

            final InputStream inputStream = _jarFile.getInputStream(entry);

            int remainingBytes = inputStream.available();

            buffer.reset(remainingBytes);

            while (remainingBytes > 0) {
                final int bytesRead = inputStream.read(buffer.array(), buffer.position(), remainingBytes);

                if (bytesRead < 0) {
                    break;
                }

                buffer.position(buffer.position() + bytesRead);
                remainingBytes -= bytesRead;
            }

            buffer.position(0);
            return true;
        }
        catch (IOException e) {
            throw ExceptionUtilities.asRuntimeException(e);
        }
    }
}
