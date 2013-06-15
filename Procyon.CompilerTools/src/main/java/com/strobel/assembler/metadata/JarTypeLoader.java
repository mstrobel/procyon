package com.strobel.assembler.metadata;

import com.strobel.assembler.ir.ConstantPool;
import com.strobel.core.ExceptionUtilities;
import com.strobel.core.VerifyArgument;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarTypeLoader implements ITypeLoader {
    private final JarFile _jarFile;
    private final Map<String, String> _knownMappings;

    public JarTypeLoader(final JarFile jarFile) {
        _jarFile = VerifyArgument.notNull(jarFile, "jarFile");
        _knownMappings = new HashMap<>();
    }

    @Override
    public boolean tryLoadType(final String internalName, final Buffer buffer) {
        try {
            final JarEntry entry = _jarFile.getJarEntry(internalName + ".class");

            if (entry == null) {
                final String mappedName = _knownMappings.get(internalName);

                return mappedName != null &&
                       !mappedName.equals(internalName) && tryLoadType(mappedName, buffer);
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

            final String actualName = getInternalNameFromClassFile(buffer);

            if (actualName != null && !actualName.equals(internalName)) {
                _knownMappings.put(actualName, internalName);
            }

            return true;
        }
        catch (IOException e) {
            throw ExceptionUtilities.asRuntimeException(e);
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
