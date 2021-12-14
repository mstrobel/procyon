#Added InputStreamTypeReader for reading file a input stream
##Purpose:
I have a use case where, application gets the file references from jar scanning.
If we are not able to load the class file with classloader due some missing dependencies then we decompile the code and analyze the code.
That said application will pass empty string for filename or file location.
Below is the sample code how the Decompiler will be invoked.
```java

@Slf4j
public class CustomDecompiler {

    @SneakyThrows
    public static boolean readClassContent(FileInputStream fileInputStream, List<String> annotations, String fileName){
        final DecompilerSettings settings = DecompilerSettings.javaDefaults();
        boolean annotationFound = false;
        InputStreamTypeReader inputStreamTypeReader = new InputStreamTypeReader();
        inputStreamTypeReader.setFileInputStream(fileInputStream);
        settings.setTypeLoader(inputStreamTypeReader);

        try (final StringWriter writer = new StringWriter()) {
            PlainTextOutput plainTextOutput = new PlainTextOutput(writer);
            Decompiler.decompile("", plainTextOutput,settings);
            String fileContent = writer.toString();
            for(String annotation : annotations){
                annotationFound = annotationFound || StringUtils.containsIgnoreCase(fileContent, annotation);
            }
        }
        catch (final Throwable e) {
            log.debug("Error occurred while decompiling file content of {}", fileName, e);
        }
        return annotationFound;
    }
}

```