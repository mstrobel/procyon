package com.strobel.decompiler.patterns;

import com.strobel.assembler.metadata.MemberReference;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.java.ast.AstNode;
import com.strobel.decompiler.languages.java.ast.Keys;

public final class MemberReferenceTypeNode extends Pattern {
    private final String _groupName;
    private final INode _target;
    private final Class<? extends MemberReference> _referenceType;

    public MemberReferenceTypeNode(final INode target, final Class<? extends MemberReference> referenceType) {
        _groupName = null;
        _target = VerifyArgument.notNull(target, "target");
        _referenceType = VerifyArgument.notNull(referenceType, "referenceType");
    }

    public MemberReferenceTypeNode(final String groupName, final INode target, final Class<? extends MemberReference> referenceType) {
        _groupName = groupName;
        _target = VerifyArgument.notNull(target, "target");
        _referenceType = VerifyArgument.notNull(referenceType, "referenceType");
    }

    public final String getGroupName() {
        return _groupName;
    }

    public final Class<? extends MemberReference> getReferenceType() {
        return _referenceType;
    }

    public final INode getTarget() {
        return _target;
    }

    @Override
    public boolean matches(final INode other, final Match match) {
        if (other instanceof AstNode) {
            final AstNode reference = (AstNode) other;
            final MemberReference memberReference = reference.getUserData(Keys.MEMBER_REFERENCE);
            
            if (_target.matches(reference, match) &&
                _referenceType.isInstance(memberReference)) {

                match.add(_groupName, reference);
                return true;
            }
        }

        return false;
    }
}