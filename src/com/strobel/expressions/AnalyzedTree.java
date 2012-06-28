package com.strobel.expressions;

import com.strobel.compilerservices.DebugInfoGenerator;

import java.util.HashMap;
import java.util.Map;

final class AnalyzedTree {
    private DebugInfoGenerator _debugInfoGenerator;

    final Map<Object, CompilerScope> scopes = new HashMap<>();
    final Map<LambdaExpression, BoundConstants> constants = new HashMap<>();

    DebugInfoGenerator getDebugInfoGenerator() {
        if (_debugInfoGenerator == null) {
            return DebugInfoGenerator.empty();
        }
        return _debugInfoGenerator;
    }

    void setDebugInfoGenerator(final DebugInfoGenerator debugInfoGenerator) {
        _debugInfoGenerator = debugInfoGenerator;
    }
}

