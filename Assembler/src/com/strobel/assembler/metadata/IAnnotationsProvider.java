package com.strobel.assembler.metadata;

import com.strobel.assembler.metadata.annotations.CustomAnnotation;

import java.util.List;

/**
 * @author Mike Strobel
 */
public interface IAnnotationsProvider {
    boolean hasAnnotations();
    List<CustomAnnotation> getAnnotations();
}
