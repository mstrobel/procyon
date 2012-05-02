<?xml version="1.0" encoding="ASCII"?>
<!--
  ASM XML Adapter examples.
  Copyright (c) 2004, Eugene Kuleshov
  All rights reserved.
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:
  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
  3. Neither the name of the copyright holders nor the names of its
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
  THE POSSIBILITY OF SUCH DAMAGE.
-->

<!--
  XSL transformation for ASM XML document to add the code that will dump
  an execution time for each method.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" indent="yes" encoding="ASCII"/>


<xsl:template match="//method/code">
  <code>
    <!-- add attributes for code element -->
    <xsl:apply-templates select="@*"/>
  
    <!-- store start time into the max variable after method params -->
    <INVOKESTATIC desc="()J" name="currentTimeMillis" owner="java/lang/System"/>
    <LSTORE>
      <xsl:attribute name="var">
        <xsl:value-of select="./Max/@maxLocals"/>
      </xsl:attribute>
    </LSTORE>

    <!-- process child elements -->
    <xsl:apply-templates select="*"/>

  </code>

</xsl:template>


<!--
  Add print statement before return instructions
  IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN, 
  ATHROW ??
-->
<xsl:template match="//IRETURN | //LRETURN | //FRETURN | //DRETURN | //ARETURN | //RETURN">
  <GETSTATIC desc="Ljava/io/PrintStream;" name="err" owner="java/lang/System"/>

  <NEW desc="java/lang/StringBuffer"/>
  <DUP/>
  <INVOKESPECIAL owner="java/lang/StringBuffer" name="&lt;init&gt;" desc="()V"/>
  
  <INVOKESTATIC desc="()J" name="currentTimeMillis" owner="java/lang/System"/>
  <LLOAD>
    <xsl:attribute name="var">
      <xsl:value-of select="../Max/@maxLocals"/>
    </xsl:attribute>
  </LLOAD>
  <LSUB/>
  <INVOKEVIRTUAL owner="java/lang/StringBuffer" name="append" desc="(J)Ljava/lang/StringBuffer;"/>
  
  <LDC desc="Ljava/lang/String;">
    <xsl:attribute name="cst">
      <xsl:value-of select="concat( ' : ', ../../../@name, '.', ../../@name, ../../@desc)"/>
    </xsl:attribute>
  </LDC>
  <INVOKEVIRTUAL owner="java/lang/StringBuffer" name="append" desc="(Ljava/lang/String;)Ljava/lang/StringBuffer;"/>
  <INVOKEVIRTUAL owner="java/lang/StringBuffer" name="toString" desc="()Ljava/lang/String;"/>

  <INVOKEVIRTUAL desc="(Ljava/lang/String;)V" name="println" owner="java/io/PrintStream"/>

  <xsl:element name="{name()}"/>
</xsl:template>


<!-- copy everything -->
<xsl:template match="@*|*|text()|processing-instruction()">
  <xsl:copy><xsl:apply-templates select="@*|*|text()|processing-instruction()"/></xsl:copy>
</xsl:template>

</xsl:stylesheet>

