package com.strobel.assembler.metadata;

import com.strobel.core.VerifyArgument;
import sun.misc.Resource;
import sun.misc.URLClassPath;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;

/**
* @author Mike Strobel
*/
public final class ClasspathTypeLoader implements ITypeLoader {
    private final URLClassPath _classPath;

    public ClasspathTypeLoader() {
        this(System.getProperty("java.class.path"));
    }

    public ClasspathTypeLoader(final String classPath) {
        final String[] parts = VerifyArgument.notNull(classPath, "classPath").split(";");
        final URL[] urls = new URL[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                urls[i] = new File(parts[i]).toURI().toURL();
            }
            catch (MalformedURLException e) {
                throw new UndeclaredThrowableException(e);
            }
        }

        _classPath = new URLClassPath(urls);
    }

    @Override
    public boolean tryLoadType(final String internalName, final Buffer buffer) {
        final String path = internalName.concat(".class");
        final Resource resource = _classPath.getResource(path, false);

        if (resource == null) {
            return false;
        }

//        System.out.println("Loading " + internalName + "...");

        final byte[] data;

        try {
            data = resource.getBytes();
            assert data.length == resource.getContentLength();
        }
        catch (IOException e) {
            return false;
        }

        buffer.reset(data.length);
        System.arraycopy(data, 0, buffer.array(), 0, data.length);

        return true;
    }
}
