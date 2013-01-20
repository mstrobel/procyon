package com.strobel.assembler.metadata.annotations;

public enum AnnotationElementType {
    Constant,
    Enum,
    Array,
    Class,
    Annotation;

    public static AnnotationElementType forTag(final char tag) {
        switch (tag) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 's':
                return Constant;

            case 'e':
                return Enum;

            case '[':
                return Array;

            case 'c':
                return Class;

            case '@':
                return Annotation;

            default:
                throw new IllegalArgumentException("Invalid annotation element tag: " + tag);
        }
    }
}
