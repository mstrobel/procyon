package com.strobel.assembler;

import java.util.List;

/**
 * User: Mike Strobel
 * Date: 1/6/13
 * Time: 5:40 PM
 */
public interface IMethodSignature {
    boolean hasParameters();
    List<ParameterDefinition> getParameters();
    TypeReference getReturnType();
}
