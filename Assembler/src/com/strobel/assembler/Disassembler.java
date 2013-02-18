package com.strobel.assembler;

import com.strobel.assembler.ir.ClassFileReader;
import com.strobel.assembler.metadata.*;
import com.strobel.core.VerifyArgument;

/**
 * @author Mike Strobel
 */
public final class Disassembler {
    public static void disassemble(final String internalName, final CodePrinter printer) {
        VerifyArgument.notNull(internalName, "internalName");

        final ClasspathTypeLoader loader = new ClasspathTypeLoader();
        final Buffer b = new Buffer();

        if (!loader.tryLoadType(internalName, b)) {
            printer.printf("!!! ERROR: Failed to load class %s.", internalName);
            return;
        }

        final MetadataSystem metadataSystem = MetadataSystem.instance();

        final ClassFileReader reader = ClassFileReader.readClass(
            ClassFileReader.OPTION_PROCESS_CODE | ClassFileReader.OPTION_PROCESS_ANNOTATIONS,
            metadataSystem,
            b
        );

        final TypePrinter typePrinter = new TypePrinter(printer);

        reader.accept(typePrinter);
    }

    public static void main(final String[] args) {
        final CodePrinter printer = new CodePrinter(System.out);

        if (args.length == 0) {
            disassemble("com/strobel/assembler/Disassembler", printer);
        }
        else {
            disassemble(args[0], printer);
        }
    }
}
