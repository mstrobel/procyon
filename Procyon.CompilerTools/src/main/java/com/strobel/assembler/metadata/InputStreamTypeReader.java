package com.strobel.assembler.metadata;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.ir.ConstantPool;
import com.strobel.core.StringComparison;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.io.PathHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InputStreamTypeReader implements ITypeLoader {
    private static final Logger LOG = Logger.getLogger(InputTypeLoader.class.getSimpleName());
    private final ITypeLoader _defaultTypeLoader;
    private final Map<String, LinkedHashSet<File>> _packageLocations;
    private final Map<String, File> _knownFiles;
    private FileInputStream fileInputStream;
    private byte bytes[] ;

    public InputStreamTypeReader() {
        this(new ClasspathTypeLoader());
    }

    public InputStreamTypeReader(ITypeLoader defaultTypeLoader) {
        this._defaultTypeLoader = (ITypeLoader) VerifyArgument.notNull(defaultTypeLoader, "defaultTypeLoader");
        this._packageLocations = new LinkedHashMap();
        this._knownFiles = new LinkedHashMap();
    }

    public void setFileInputStream(FileInputStream fileInputStream){
        this.fileInputStream = fileInputStream;
    }

    public boolean tryLoadType(String typeNameOrPath, Buffer buffer) {

        VerifyArgument.notNull(typeNameOrPath, "typeNameOrPath");
        VerifyArgument.notNull(buffer, "buffer");
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Attempting to load type: " + typeNameOrPath + "...");
        }

        boolean hasExtension = StringUtilities.endsWithIgnoreCase(typeNameOrPath, ".class");
        if (hasExtension && this.tryLoadFile((String)null, typeNameOrPath, buffer, true)) {
            return true;
        } else if (PathHelper.isPathRooted(typeNameOrPath)) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("Failed to load type: " + typeNameOrPath + ".");
            }

            return false;
        } else {
            String internalName = hasExtension ? typeNameOrPath.substring(0, typeNameOrPath.length() - 6) : typeNameOrPath.replace('.', '/');
            if (this.tryLoadTypeFromName(internalName, buffer)) {
                return true;
            } else if (hasExtension) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Failed to load type: " + typeNameOrPath + ".");
                }

                return false;
            } else {
                for(int lastDelimiter = internalName.lastIndexOf(47); lastDelimiter != -1; lastDelimiter = internalName.lastIndexOf(47)) {
                    internalName = internalName.substring(0, lastDelimiter) + "$" + internalName.substring(lastDelimiter + 1);
                    if (this.tryLoadTypeFromName(internalName, buffer)) {
                        return true;
                    }
                }

                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Failed to load type: " + typeNameOrPath + ".");
                }

                return false;
            }
        }
    }

    private boolean tryLoadTypeFromName(String internalName, Buffer buffer) {
        if (this.tryLoadFile(buffer)) {
            return true;
        } else if (this._defaultTypeLoader.tryLoadType(internalName, buffer)) {
            return true;
        } else {
            return false;
        }
    }


    private boolean tryLoadFile(Buffer buffer) {

        if (fileInputStream != null) {

            try {
                if(this.bytes != null && this.bytes.length > 0){
                    buffer.position(0);
                    return false;
                }
                FileInputStream in = fileInputStream;
                Throwable var4 = null;

                try {
                    int remainingBytes = in.available();
                    buffer.position(0);
                    buffer.reset(remainingBytes);

                    while(true) {
                        if (remainingBytes > 0) {
                            int bytesRead = in.read(buffer.array(), buffer.position(), remainingBytes);
                            if (bytesRead >= 0) {
                                remainingBytes -= bytesRead;
                                buffer.advance(bytesRead);
                                continue;
                            }
                        }

                        buffer.position(0);
                        boolean var19 = true;
                        bytes = Arrays.copyOf(buffer.array(), buffer.array().length);
                        return var19;
                    }

                } catch (Throwable var16) {
                    var4 = var16;
                    throw var16;
                } finally {
                    if (in != null) {
                        if (var4 != null) {
                            try {
                                in.close();
                            } catch (Throwable var15) {
                                var4.addSuppressed(var15);
                            }
                        } else {
                            in.close();
                        }
                    }

                }
            } catch (IOException var18) {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean tryLoadFile(String internalName, String typeNameOrPath, Buffer buffer, boolean trustName) {
        File file = new File(typeNameOrPath);
        if (!this.tryLoadFile(buffer)) {
            return false;
        } else {
            String actualName = getInternalNameFromClassFile(buffer);
            String name = trustName ? (internalName != null ? internalName : actualName) : actualName;
            if (name == null) {
                return false;
            } else {
                boolean nameMatches = StringUtilities.equals(actualName, internalName);
                boolean pathMatchesName = typeNameOrPath.endsWith(name.replace('/', File.separatorChar) + ".class");
                boolean result = internalName == null || pathMatchesName || nameMatches;
                if (result) {
                    int packageEnd = name.lastIndexOf(47);
                    String packageName;
                    if (packageEnd >= 0 && packageEnd < name.length()) {
                        packageName = name.substring(0, packageEnd);
                    } else {
                        packageName = "";
                    }

                    this.registerKnownPath(packageName, file.getParentFile(), pathMatchesName);
                    this._knownFiles.put(actualName, file);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Type loaded from " + file.getAbsolutePath() + ".");
                    }
                } else {
                    buffer.reset(0);
                }

                return result;
            }
        }
    }

    private void registerKnownPath(String packageName, File directory, boolean recursive) {
        if (directory != null && directory.exists()) {
            LinkedHashSet<File> directories = (LinkedHashSet)this._packageLocations.get(packageName);
            if (directories == null) {
                this._packageLocations.put(packageName, directories = new LinkedHashSet());
            }

            if (directories.add(directory) && recursive) {
                try {
                    String directoryPath = StringUtilities.removeRight(directory.getCanonicalPath(), new char[]{PathHelper.DirectorySeparator, PathHelper.AlternateDirectorySeparator}).replace('\\', '/');
                    String currentPackage = packageName;
                    File currentDirectory = new File(directoryPath);

                    int delimiterIndex;
                    while((delimiterIndex = currentPackage.lastIndexOf(47)) >= 0 && currentDirectory.exists() && delimiterIndex < currentPackage.length() - 1) {
                        String segmentName = currentPackage.substring(delimiterIndex + 1);
                        if (!StringUtilities.equals(currentDirectory.getName(), segmentName, StringComparison.OrdinalIgnoreCase)) {
                            break;
                        }

                        currentPackage = currentPackage.substring(0, delimiterIndex);
                        currentDirectory = currentDirectory.getParentFile();
                        directories = (LinkedHashSet)this._packageLocations.get(currentPackage);
                        if (directories == null) {
                            this._packageLocations.put(currentPackage, directories = new LinkedHashSet());
                        }

                        if (!directories.add(currentDirectory)) {
                            break;
                        }
                    }
                } catch (IOException var10) {
                }

            }
        }
    }

    private static String getInternalNameFromClassFile(Buffer b) {
        long magic = (long)b.readInt() & 4294967295L;
        if (magic != 3405691582L) {
            return null;
        } else {
            b.readUnsignedShort();
            b.readUnsignedShort();
            ConstantPool constantPool = ConstantPool.read(b);
            b.readUnsignedShort();
            ConstantPool.TypeInfoEntry thisClass = (ConstantPool.TypeInfoEntry)constantPool.getEntry(b.readUnsignedShort());
            b.position(0);
            return thisClass.getName();
        }
    }
}
