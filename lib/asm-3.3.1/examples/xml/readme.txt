
  This directory contains sample XSL transformation templates that
  demonstrate features of org.objectweb.asm.xml package.


  copy.xsl
    Copying source document into target without changes.

  strip.xsl
    Strips all attribute values from the target XML. 
    Result document can't be used to generate bytecode.

  annotate.xsl
    Adds comments for labels and variable instructions
    to the target XML document.

  linenumbers.xsl
    Adds code to dump source line numbers for the executing code to System.err
  
  profile.xsl
    Adds code to dump execution time for each method to System.err


  You can use the following command to transform

  java -jar asm-xml-<version>.jar <in format> <out format> 
       [-in <input jar>] [-out <output jar>] [-xslt <xslt file>]

    where <in format> and <out format> is one of code, xml or singlexml
    when -in or -out is omitted sysin and sysout would be used

