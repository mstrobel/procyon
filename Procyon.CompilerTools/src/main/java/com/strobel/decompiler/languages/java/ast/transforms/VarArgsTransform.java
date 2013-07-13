package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.MemberReference;
import com.strobel.assembler.metadata.MetadataFilters;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MethodBinder;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.semantics.ResolveResult;

import java.util.ArrayList;
import java.util.List;

import static com.strobel.core.CollectionUtilities.lastOrDefault;

public class VarArgsTransform extends ContextTrackingVisitor<Void> {
    private final JavaResolver _resolver;

    public VarArgsTransform(final DecompilerContext context) {
        super(context);
        _resolver = new JavaResolver(context);
    }

    @Override
    public Void visitInvocationExpression(final InvocationExpression node, final Void data) {
        super.visitInvocationExpression(node, data);

        final AstNodeCollection<Expression> arguments = node.getArguments();
        final Expression arrayArg = lastOrDefault(arguments);

        if (arrayArg == null ||
            arrayArg.isNull() ||
            !(arrayArg instanceof ArrayCreationExpression &&
              node.getTarget() instanceof MemberReferenceExpression)) {

            return null;
        }

        final ArrayCreationExpression newArray = (ArrayCreationExpression) arrayArg;
        final MemberReferenceExpression target = (MemberReferenceExpression) node.getTarget();

        if (!newArray.getAdditionalArraySpecifiers().hasSingleElement()) {
            return null;
        }

        final MethodReference method = (MethodReference) node.getUserData(Keys.MEMBER_REFERENCE);

        if (method == null) {
            return null;
        }

        final MethodDefinition resolved = method.resolve();

        if (resolved == null || !resolved.isVarArgs()) {
            return null;
        }

        final ResolveResult targetResult = _resolver.apply(target.getTarget());

        if (targetResult == null || targetResult.getType() == null) {
            return null;
        }

        final List<TypeReference> argTypes = new ArrayList<>();

        for (final Expression argument : arguments) {
            final ResolveResult argResult = _resolver.apply(argument);

            if (argResult == null || argResult.getType() == null) {
                return null;
            }

            argTypes.add(argResult.getType());
        }

        final List<MethodReference> candidates = MetadataHelper.findMethods(
            targetResult.getType(),
            MetadataFilters.matchName(resolved.getName())
        );

        final MethodBinder.BindResult c1 = MethodBinder.selectMethod(candidates, argTypes);

        if (c1.isFailure() || c1.isAmbiguous()) {
            return null;
        }

        argTypes.remove(argTypes.size() - 1);

        final ArrayInitializerExpression initializer = newArray.getInitializer();
        final boolean hasElements = !initializer.isNull() && !initializer.getElements().isEmpty();

        if (hasElements) {
            for (final Expression argument : initializer.getElements()) {
                final ResolveResult argResult = _resolver.apply(argument);

                if (argResult == null || argResult.getType() == null) {
                    return null;
                }

                argTypes.add(argResult.getType());
            }
        }

        final MethodBinder.BindResult c2 = MethodBinder.selectMethod(candidates, argTypes);

        if (c2.isFailure() ||
            c2.isAmbiguous() ||
            !StringUtilities.equals(c2.getMethod().getErasedSignature(), c1.getMethod().getErasedSignature())) {

            return null;
        }

        newArray.remove();

        if (!hasElements) {
            return null;
        }

        for (final Expression newArg : initializer.getElements()) {
            newArg.remove();
            arguments.add(newArg);
        }

        return null;
    }
}
