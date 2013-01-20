package com.strobel.assembler.ir.attributes;

import com.strobel.util.ContractUtils;

/**
 * @author Mike Strobel
 */
public final class AttributeNames {
    public static final String SourceFile = "SourceFile";
    public static final String ConstantValue = "ConstantValue";
    public static final String Code = "Code";
    public static final String Exceptions = "Exceptions";
    public static final String LineNumberTable = "LineNumberTable";
    public static final String LocalVariableTable = "LocalVariableTable";
    public static final String InnerClasses = "InnerClasses";
    public static final String Synthetic = "Synthetic";
    public static final String BootstrapMethods = "BootstrapMethods";
    public static final String Signature = "Signature";
    public static final String Deprecated = "Deprecated";
    public static final String EnclosingMethod = "EnclosingMethod";
    public static final String LocalVariableTypeTable = "LocalVariableTypeTable";
    public static final String RuntimeVisibleAnnotations = "RuntimeVisibleAnnotations";
    public static final String RuntimeInvisibleAnnotations = "RuntimeInvisibleAnnotations";
    public static final String RuntimeVisibleParameterAnnotations = "RuntimeVisibleParameterAnnotations";
    public static final String RuntimeInvisibleParameterAnnotations = "RuntimeInvisibleParameterAnnotations";
    public static final String AnnotationDefault = "AnnotationDefault";

    private AttributeNames() {
        throw ContractUtils.unreachable();
    }
}
