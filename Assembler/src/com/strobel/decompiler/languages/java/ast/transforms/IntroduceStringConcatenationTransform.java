package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;

import java.util.ArrayList;
import java.util.List;

public class IntroduceStringConcatenationTransform extends ContextTrackingVisitor<Void> {
    public IntroduceStringConcatenationTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    public Void visitObjectCreationExpression(final ObjectCreationExpression node, final Void data) {
        if (node.getArguments().isEmpty()) {
            final TypeReference typeReference = node.getType().toTypeReference();

            if (typeReference != null &&
                StringUtilities.equals(typeReference.getInternalName(), "java/lang/StringBuilder")) {

                convertStringBuilderToConcatenation(node);
            }
        }

        return super.visitObjectCreationExpression(node, data);
    }

    private void convertStringBuilderToConcatenation(final ObjectCreationExpression node) {
        if (node.getParent() == null || node.getParent().getParent() == null) {
            return;
        }

        final ArrayList<Expression> operands = new ArrayList<>();

        AstNode current;
        AstNode parent;

        boolean atLeastOneStringArgument = false;

        for (current = node.getParent(), parent = current.getParent();
             current instanceof MemberReferenceExpression && parent instanceof InvocationExpression && parent.getParent() != null;
             current = parent.getParent(), parent = current.getParent()) {

            final String memberName = ((MemberReferenceExpression) current).getMemberName();
            final AstNodeCollection<Expression> arguments = ((InvocationExpression) parent).getArguments();

            if (StringUtilities.equals(memberName, "append") &&
                arguments.size() == 1) {

                operands.add(arguments.firstOrNullObject());

                final MemberReference member = parent.getUserData(Keys.MEMBER_REFERENCE);

                if (member instanceof MethodReference) {
                    final List<ParameterDefinition> p = ((MethodReference) member).getParameters();

                    if (p.size() == 1 &&
                        StringUtilities.equals(p.get(0).getParameterType().getInternalName(), "java/lang/String")) {

                        atLeastOneStringArgument = true;
                    }
                }
            }
            else {
                break;
            }
        }

        if (atLeastOneStringArgument &&
            operands.size() > 1 &&
            current instanceof MemberReferenceExpression &&
            parent instanceof InvocationExpression &&
            StringUtilities.equals(((MemberReferenceExpression) current).getMemberName(), "toString") &&
            ((InvocationExpression) parent).getArguments().isEmpty()) {

            for (final Expression operand : operands) {
                operand.remove();
            }

            Expression concatenation = new BinaryOperatorExpression(operands.get(0), BinaryOperatorType.ADD, operands.get(1));

            for (int i = 2; i < operands.size(); i++) {
                concatenation = new BinaryOperatorExpression(concatenation, BinaryOperatorType.ADD, operands.get(i));
            }

            parent.replaceWith(concatenation);
        }
    }
}
