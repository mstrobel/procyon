package com.strobel.decompiler.patterns;

import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.ast.IdentifierExpression;
import com.strobel.decompiler.languages.java.ast.Keys;

public final class ParameterReferenceNode extends Pattern {
    private final int _parameterPosition;
    private final String _groupName;

    public ParameterReferenceNode(final int parameterPosition) {
        _parameterPosition = parameterPosition;
        _groupName = null;
    }

    public ParameterReferenceNode(final int parameterPosition, final String groupName) {
        _parameterPosition = parameterPosition;
        _groupName = groupName;
    }

    public final String getGroupName() {
        return _groupName;
    }

    public final int getParameterPosition() {
        return _parameterPosition;
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof IdentifierExpression) {
            final IdentifierExpression identifier = (IdentifierExpression) other;
            final Variable variable = identifier.getUserData(Keys.VARIABLE);

            if (variable != null &&
                variable.isParameter() &&
                variable.getOriginalParameter().getPosition() == _parameterPosition) {

                if (_groupName != null) {
                    match.add(_groupName, identifier);
                }

                return true;
            }
        }
        return false;
    }
}
