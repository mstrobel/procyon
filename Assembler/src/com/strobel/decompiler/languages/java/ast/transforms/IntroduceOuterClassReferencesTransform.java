package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataResolver;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.Predicates;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.strobel.core.CollectionUtilities.*;

public class IntroduceOuterClassReferencesTransform extends ContextTrackingVisitor<Void> {
    private final List<AstNode> _nodesToRemove;
    private final Set<ParameterDefinition> _parametersToRemove;

    public IntroduceOuterClassReferencesTransform(final DecompilerContext context) {
        super(context);

        _nodesToRemove = new ArrayList<>();
        _parametersToRemove = new HashSet<>();
    }

    @Override
    public void run(final AstNode compilationUnit) {
        //
        // First run through and locate any outer class member access methods.
        //
        new PhaseOneVisitor().run(compilationUnit);

        super.run(compilationUnit);

        for (final AstNode node : _nodesToRemove) {
            node.remove();
        }
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression node, final Void data) {
        super.visitInvocationExpression(node, data);

        final Expression target = node.getTarget();
        final AstNodeCollection<Expression> arguments = node.getArguments();

        if (target instanceof MemberReferenceExpression && arguments.size() == 1) {
            final MemberReferenceExpression memberReference = (MemberReferenceExpression) target;

            MemberReference reference = memberReference.getUserData(Keys.MEMBER_REFERENCE);

            if (reference == null) {
                reference = node.getUserData(Keys.MEMBER_REFERENCE);
            }

            if (reference instanceof MethodReference) {
                final MethodReference method = (MethodReference) reference;

                if (method.isConstructor()) {
                    final MethodDefinition resolvedMethod = method.resolve();

                    if (resolvedMethod != null) {
                        final TypeDefinition declaringType = resolvedMethod.getDeclaringType();

                        if (declaringType.isInnerClass() || declaringType.isLocalClass()) {
                            for (final ParameterDefinition p : resolvedMethod.getParameters()) {
                                if (_parametersToRemove.contains(p)) {
                                    final int parameterIndex = p.getPosition();
                                    final Expression argumentToRemove = getOrDefault(arguments, parameterIndex);

                                    if (argumentToRemove != null) {
                                        _nodesToRemove.add(argumentToRemove);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public Void visitMemberReferenceExpression(final MemberReferenceExpression node, final Void data) {
        tryIntroduceOuterClassReference(node, node.getTarget() instanceof ThisReferenceExpression);
        return super.visitMemberReferenceExpression(node, data);
    }

    private boolean tryIntroduceOuterClassReference(final MemberReferenceExpression node, final boolean hasThisOnLeft) {
        if (!hasThisOnLeft || !context.getCurrentType().isInnerClass()) {
            return false;
        }

        if (node.getParent() instanceof AssignmentExpression &&
            node.getRole() == AssignmentExpression.LEFT_ROLE) {

            return false;
        }

        final String memberName = node.getMemberName();
        final MemberReference reference = node.getUserData(Keys.MEMBER_REFERENCE);

        if (context.getCurrentType().isStatic() ||
            node.getParent() instanceof AssignmentExpression && node.getRole() == AssignmentExpression.LEFT_ROLE ||
            !(reference instanceof FieldReference) ||
            !StringUtilities.startsWith(memberName, "this$")) {

            return tryInsertOuterClassReference(node, reference);
        }

        final FieldReference field = (FieldReference) reference;
        final FieldDefinition resolvedField = field.resolve();

        if (resolvedField == null || !resolvedField.isSynthetic()) {
            return false;
        }

        if (node.getParent() instanceof MemberReferenceExpression &&
            tryIntroduceOuterClassReference((MemberReferenceExpression) node.getParent(), hasThisOnLeft)) {

            return true;
        }

        final SimpleType outerType;

        final TypeReference outerTypeReference = field.getFieldType();
        final TypeDefinition resolvedOuterType = outerTypeReference.resolve();

        if (resolvedOuterType != null && resolvedOuterType.isAnonymous()) {
            if (resolvedOuterType.getExplicitInterfaces().isEmpty()) {
                outerType = new SimpleType(resolvedOuterType.getBaseType().getSimpleName());
            }
            else {
                outerType = new SimpleType(resolvedOuterType.getExplicitInterfaces().get(0).getSimpleName());
            }
        }
        else {
            if (resolvedOuterType != null) {
                outerType = new SimpleType(resolvedOuterType.getSimpleName());
            }
            else {
                outerType = new SimpleType(outerTypeReference.getSimpleName());
            }
        }

        outerType.putUserData(Keys.TYPE_REFERENCE, outerTypeReference);

        final ThisReferenceExpression replacement = new ThisReferenceExpression();

        replacement.setTarget(new TypeReferenceExpression(outerType));
        replacement.putUserData(Keys.TYPE_REFERENCE, outerTypeReference);

        node.replaceWith(replacement);

        return true;
    }

    private boolean tryInsertOuterClassReference(final MemberReferenceExpression node, final MemberReference reference) {
        if (node == null || reference == null || !(node.getTarget() instanceof ThisReferenceExpression)) {
            return false;
        }

        final ThisReferenceExpression left = (ThisReferenceExpression) node.getTarget();
        final TypeReference declaringType = reference.getDeclaringType();

        if (!left.getTarget().isNull() ||
            MetadataResolver.areEquivalent(context.getCurrentType(), declaringType) ||
            !isContextWithinTypeInstance(declaringType)) {

            return false;
        }

        final TypeDefinition resolvedType = declaringType.resolve();
        final TypeReference declaredType;

        if (resolvedType != null && resolvedType.isAnonymous()) {
            if (resolvedType.getExplicitInterfaces().isEmpty()) {
                declaredType = resolvedType.getBaseType();
            }
            else {
                declaredType = resolvedType.getExplicitInterfaces().get(0);
            }
        }
        else {
            declaredType = declaringType;
        }

        final SimpleType outerType = new SimpleType(declaredType.getSimpleName());

        outerType.putUserData(Keys.TYPE_REFERENCE, declaredType);
        left.setTarget(new TypeReferenceExpression(outerType));

        return true;
    }

    // <editor-fold defaultstate="collapsed" desc="PhaseOneVisitor Class">

    private class PhaseOneVisitor extends ContextTrackingVisitor<Void> {
        private PhaseOneVisitor() {
            super(IntroduceOuterClassReferencesTransform.this.context);
        }

        @Override
        public Void visitAssignmentExpression(final AssignmentExpression node, final Void _) {
            super.visitAssignmentExpression(node, _);

            if (context.getSettings().getShowSyntheticMembers() ||
                context.getCurrentMethod() == null ||
                !context.getCurrentMethod().isConstructor() ||
                !context.getCurrentType().isInnerClass() && !context.getCurrentType().isLocalClass()) {

                return null;
            }

            final Expression left = node.getLeft();
            final Expression right = node.getRight();

            if (left instanceof MemberReferenceExpression) {
                if (right instanceof IdentifierExpression) {
                    final Variable variable = right.getUserData(Keys.VARIABLE);

                    if (variable == null || !variable.isParameter()) {
                        return null;
                    }

                    final MemberReferenceExpression memberReference = (MemberReferenceExpression) left;
                    final MemberReference member = memberReference.getUserData(Keys.MEMBER_REFERENCE);

                    if (member instanceof FieldReference &&
                        memberReference.getTarget() instanceof ThisReferenceExpression) {

                        final FieldDefinition resolvedField = ((FieldReference) member).resolve();

                        if (resolvedField != null &&
                            resolvedField.isSynthetic() &&
                            resolvedField.getName().startsWith("this$")) {

                            final ParameterDefinition parameter = variable.getOriginalParameter();

                            _parametersToRemove.add(parameter);

                            final ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration) firstOrDefault(
                                node.getAncestorsAndSelf(),
                                Predicates.<AstNode>instanceOf(ConstructorDeclaration.class)
                            );

                            if (constructorDeclaration != null && !constructorDeclaration.isNull()) {
                                final ParameterDeclaration parameterToRemove = getOrDefault(
                                    constructorDeclaration.getParameters(),
                                    parameter.getPosition()
                                );

                                if (parameterToRemove != null) {
                                    _nodesToRemove.add(parameterToRemove);
                                }
                            }

                            if (node.getParent() instanceof ExpressionStatement) {
                                _nodesToRemove.add(node.getParent());
                            }
                            else {
                                final TypeReference fieldType = resolvedField.getFieldType();
                                final ThisReferenceExpression replacement = new ThisReferenceExpression();
                                final SimpleType type = new SimpleType(fieldType.getSimpleName());

                                type.putUserData(Keys.TYPE_REFERENCE, fieldType);
                                replacement.putUserData(Keys.TYPE_REFERENCE, fieldType);
                                replacement.setTarget(new TypeReferenceExpression(type));
                                right.replaceWith(replacement);
                            }
                        }
                    }
                }
            }

            return null;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Helper Methods">

    private static boolean isEnclosedBy(final TypeReference innerType, final TypeReference outerType) {
        if (innerType == null) {
            return false;
        }

        for (TypeReference current = innerType.getDeclaringType();
             current != null;
             current = current.getDeclaringType()) {

            if (MetadataResolver.areEquivalent(current, outerType)) {
                return true;
            }
        }

        final TypeDefinition resolvedInnerType = innerType.resolve();

        if (resolvedInnerType != null) {
            return isEnclosedBy(resolvedInnerType.getBaseType(), outerType);
        }

        return false;
    }

    private boolean isContextWithinTypeInstance(final TypeReference type) {
        final MethodReference method = context.getCurrentMethod();

        if (method != null) {
            final MethodDefinition resolvedMethod = method.resolve();

            if (resolvedMethod != null && resolvedMethod.isStatic()) {
                return false;
            }
        }

        final TypeReference scope = context.getCurrentType();

        for (TypeReference current = scope;
             current != null;
             current = current.getDeclaringType()) {

            if (MetadataResolver.areEquivalent(current, type)) {
                return true;
            }

            final TypeDefinition resolved = current.resolve();

            if (resolved != null && resolved.isLocalClass()) {
                final MethodReference declaringMethod = resolved.getDeclaringMethod();

                if (declaringMethod != null) {
                    final MethodDefinition resolvedDeclaringMethod = declaringMethod.resolve();

                    if (resolvedDeclaringMethod != null && resolvedDeclaringMethod.isStatic()) {
                        break;
                    }
                }
            }
        }

        return false;
    }

    // </editor-fold>
}
