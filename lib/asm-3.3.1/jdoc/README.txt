This directory contains ant files to build the javadocs of the product.
The following rules describe the convention to write such files:

- An ant file must build only one javadoc.

- As there may exist several javadocs, all javadocs must be produced
  in a sub dir of ${out.dist.jdoc}. For example the user javadoc could be 
  produced into the ${out.dist.jdoc}/user directory

- The name of the ant file must be the name of the destination directory of the 
  javadoc it builds.

- Only the default task is called on an xml file.

Sample ant file:

<project name="FOO" default="dist.jdoc">

  <property name="jdoc.name" value="user"/>
  <property name="jdoc.dir" value="${out.dist.jdoc}/${jdoc.name}"/>

  <target name="dist.jdoc">
    <uptodate property="jdoc.required" targetfile="${jdoc.dir}/index.html">
      <srcfiles dir="${src}" includes="**/*.java"/>
    </uptodate>
    <antcall target="dist.jdoc.${jdoc.name}"/>
  </target>

  <target name="dist.jdoc.user" unless="jdoc.required">
    <mkdir dir="${jdoc.dir}"/>
    <javadoc destdir="${jdoc.dir}"
             windowtitle="FOO User Documentation"
             doctitle="FOO User Documentation">

       ...
       
    </javadoc>
  </target>
</project>
