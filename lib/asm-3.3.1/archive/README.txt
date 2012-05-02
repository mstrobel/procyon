This directory contains ant files to build the jars of the product.
The following rules describe the convention to write such files:

- An ant file must build only one jar file.

- The name of the ant file must be the name of the jar it builds:
  org-foo-bar.xml must build org-foo-bar.jar.

- Among the elements which are included into a jar, you must specify
  a manifest. It is adviced to store the manifest file in this directory.
  The manifest file can be shared by several jars. The name of the manifest
  file must be similar to the name of the jar file.

- Only the default task is called on each ant file.

- The jar file must be produced into the ${dist.lib} directory.

Sample ant file:

<project name="foo" default="dist">
  <target name="dist">
    <jar jarfile="${out.dist.lib}/foo.jar"
         basedir="${out.build}"
         manifest="${archive}/foo.mf">
         
      ...
      
    </jar>
  </target>
</project>
