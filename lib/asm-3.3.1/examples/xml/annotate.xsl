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
  XSL transformation for ASM XML document to add annotational comments to the local
  variables (name, type and visibility) and labels (line number, try/catch info, 
  number of incoming calls)
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" indent="yes" encoding="ASCII" />

<!--
  Annotate opcodes that are working with local variables
-->
<xsl:template match="//ILOAD | //LLOAD | //FLOAD | //DLOAD | //ALOAD | //ISTORE | //LSTORE | //FSTORE | //DSTORE | //ASTORE | //RET">
  <xsl:variable name="n"><xsl:value-of select="@var"/></xsl:variable>

  <xsl:apply-templates select="../LocalVar[@var=$n]" mode="localVar"/>
  
  <xsl:element name="{name()}">
    <xsl:attribute name="var"><xsl:value-of select="$n"/></xsl:attribute>
  </xsl:element>
</xsl:template>


<xsl:template match="*" mode="localVar">
  <xsl:comment><xsl:text> </xsl:text><xsl:value-of select="concat( @name, ' ', @desc, '  ', @start, ' - ', @end)"/><xsl:text> </xsl:text></xsl:comment>
</xsl:template>


<!--
  Annotate labels
-->
<xsl:template match="//method/code/Label">
  <xsl:variable name="n"><xsl:value-of select="@name"/></xsl:variable>

  <xsl:apply-templates select="../LineNumber[@start=$n]" mode="lineNumber"/>
  <xsl:apply-templates select="../TryCatch[@start=$n]" mode="tryStart"/>
  <xsl:apply-templates select="../TryCatch[@end=$n]" mode="tryEnd"/>
  <xsl:apply-templates select="../TryCatch[@handler=$n]" mode="catch"/>

  <xsl:variable name="c">
    <!-- this is the slowest part -->
    <xsl:value-of select="count(../IFEQ[@label=$n] | ../IFNE[@label=$n] | ../IFLT[@label=$n] | ../IFGE[@label=$n] | ../IFGT[@label=$n] | ../IFLE[@label=$n] | ../IF_ICMPEQ[@label=$n] | ../IF_ICMPNE[@label=$n] | ../IF_ICMPLT[@label=$n] | ../IF_ICMPGE[@label=$n] | ../IF_ICMPGT[@label=$n] | ../IF_ICMPLE[@label=$n] | ../IF_ACMPEQ[@label=$n] | ../IF_ACMPNE[@label=$n] | ../GOTO[@label=$n] | ../JSR[@label=$n] | ../IFNULL[@label=$n] | ../IFNONNULL[@label=$n] | ../TABLESWITCHINSN[@dflt=$n] | ../TABLESWITCHINSN/label[@name=$n] | ../LOOKUPSWITCH[@dflt=$n] | ../LOOKUPSWITCH/label[@name=$n])"/>
  </xsl:variable>
  <xsl:if test="$c>0">
    <xsl:comment><xsl:text> Incoming calls: </xsl:text><xsl:value-of select="$c"/><xsl:text> </xsl:text></xsl:comment>
  </xsl:if>

  <label><xsl:apply-templates select="@*"/></label>

</xsl:template>


<xsl:template match="*" mode="lineNumber">
  <xsl:comment><xsl:text> Line: </xsl:text><xsl:value-of select="@line"/><xsl:text> </xsl:text></xsl:comment>
</xsl:template>

<xsl:template match="*" mode="tryStart">
  <xsl:comment><xsl:text> try start </xsl:text></xsl:comment>
</xsl:template>

<xsl:template match="*" mode="tryEnd">
  <xsl:comment><xsl:text> try end </xsl:text></xsl:comment>
</xsl:template>

<xsl:template match="*" mode="catch">
  <xsl:comment>
    <xsl:text> catch </xsl:text>
    <xsl:if test="string-length(@type)>0">
      <xsl:text>(</xsl:text><xsl:value-of select="@type"/><xsl:text>)</xsl:text>
    </xsl:if>
  </xsl:comment>
</xsl:template>


<!-- copy everything -->
<xsl:template match="@*|*|text()|processing-instruction()">
  <xsl:copy><xsl:apply-templates select="@*|*|text()|processing-instruction()"/></xsl:copy>
</xsl:template>

</xsl:stylesheet>

