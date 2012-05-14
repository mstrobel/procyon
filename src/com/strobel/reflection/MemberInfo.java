package com.strobel.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;

/**
 * @author Mike Strobel
 */
public abstract class MemberInfo implements java.lang.reflect.AnnotatedElement {
    final static Annotation[] EMPTY_ANNOTATIONS = new Annotation[0]
        ;
    final static int ENUM_MODIFIER = 0x00004000;
    final static int VARARGS_MODIFIER = 0x00000080;

    MemberInfo() {}

    public abstract MemberType getMemberType();
    public abstract String getName();
    public abstract Type getDeclaringType();

    public Type getReflectedType() {
        // TODO: Implement this correctly
        return getDeclaringType();
    }

    public boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public final boolean isNonPublic() {
        return !Modifier.isPublic(getModifiers());
    }

    public final boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    public final boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public final boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public final boolean isPackagePrivate() {
        return (getModifiers() & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0;
    }

    abstract int getModifiers();

    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationClass) {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return null;
    }

    @Override
    public Annotation[] getAnnotations() {
        return EMPTY_ANNOTATIONS;
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return EMPTY_ANNOTATIONS;
    }
}
