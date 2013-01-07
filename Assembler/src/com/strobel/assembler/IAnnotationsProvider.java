package com.strobel.assembler;

import java.util.List;

/**
 * @author Mike Strobel
 */
public interface IAnnotationsProvider {
    boolean hasAnnotations();
    List<ICustomAnnotation> getAnnotations();
}
