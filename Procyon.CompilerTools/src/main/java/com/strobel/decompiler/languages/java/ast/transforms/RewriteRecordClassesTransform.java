package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.annotations.NotNull;
import com.strobel.assembler.ir.attributes.AttributeNames;
import com.strobel.assembler.ir.attributes.RecordAttribute;
import com.strobel.assembler.ir.attributes.RecordComponentInfo;
import com.strobel.assembler.ir.attributes.SourceAttribute;
import com.strobel.assembler.metadata.DynamicCallSite;
import com.strobel.assembler.metadata.MetadataHelper;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.core.Predicate;
import com.strobel.core.StringUtilities;
import com.strobel.core.StrongBox;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.AnyNode;
import com.strobel.decompiler.patterns.Match;
import com.strobel.decompiler.patterns.NamedNode;
import com.strobel.decompiler.patterns.ParameterReferenceNode;
import com.strobel.decompiler.patterns.Pattern;
import com.strobel.decompiler.patterns.Repeat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.strobel.core.CollectionUtilities.*;

public class RewriteRecordClassesTransform extends ContextTrackingVisitor<Void> {
    protected final static Map<String, String> GENERATED_METHOD_SIGNATURES;

    protected final static BlockStatement INVOKE_DYNAMIC_BODY = new BlockStatement(
        new ReturnStatement(
            new NamedNode(
                "invocation",
                new InvocationExpression(
                    new IdentifierExpression(Expression.MYSTERY_OFFSET, "invokedynamic"),
                    new Repeat(new AnyNode()).toExpression()
                )
            ).toExpression()
        )
    );

    protected final static ExpressionStatement ASSIGNMENT_PATTERN =
        new ExpressionStatement(
            new AssignmentExpression(
                new NamedNode(
                    "assignment",
                    new MemberReferenceExpression(
                        new ThisReferenceExpression(Expression.MYSTERY_OFFSET),
                        Pattern.ANY_STRING
                    )
                ).toExpression(),
                AssignmentOperatorType.ASSIGN,
                new ParameterReferenceNode(-1, "parameter").toExpression()
            )
        );

    protected final static ExpressionStatement SUPER_CONSTRUCTOR_CALL =
        new ExpressionStatement(
            new InvocationExpression(
                new SuperReferenceExpression(Expression.MYSTERY_OFFSET)
            )
        );

    static {
        final HashMap<String, String> generatedMethodNames = new HashMap<>();

        generatedMethodNames.put("toString", "()Ljava/lang/String;");
        generatedMethodNames.put("hashCode", "()I");
        generatedMethodNames.put("equals", "(Ljava/lang/Object;)Z");

        GENERATED_METHOD_SIGNATURES = Collections.unmodifiableMap(generatedMethodNames);
    }

    private RecordState _currentRecord;

    public RewriteRecordClassesTransform(final DecompilerContext context) {
        super(context);
    }

    @Override
    protected Void visitTypeDeclarationOverride(final TypeDeclaration typeDeclaration, final Void p) {
        final RecordState oldRecord = _currentRecord;

        final TypeDefinition definition = typeDeclaration.getUserData(Keys.TYPE_DEFINITION);

        final RecordAttribute recordAttribute = definition != null && definition.isRecord()
                                                ? SourceAttribute.<RecordAttribute>find(AttributeNames.Record, definition.getSourceAttributes())
                                                : null;

        final RecordState recordState = recordAttribute != null ? new RecordState(definition, recordAttribute, typeDeclaration) : null;

        _currentRecord = recordState;

        try {
            super.visitTypeDeclarationOverride(typeDeclaration, p);

            if (recordState != null) {
                recordState.tryRewrite();
            }

            return null;
        }
        finally {
            _currentRecord = oldRecord;
        }
    }

    @Override
    protected Void visitMethodDeclarationOverride(final MethodDeclaration node, final Void p) {
        final RecordState recordState = _currentRecord;
        final MethodDefinition method;

        super.visitMethodDeclarationOverride(node, p);

        if (recordState == null || (method = context.getCurrentMethod()) == null) {
            return null;
        }

        final Match indyMatch;
        final String expectedSignature = GENERATED_METHOD_SIGNATURES.get(method.getName());

        if (expectedSignature != null &&
            StringUtilities.equals(expectedSignature, method.getErasedSignature()) &&
            (indyMatch = INVOKE_DYNAMIC_BODY.match(node.getBody())).success()) {

            final DynamicCallSite callSite = first(indyMatch.<InvocationExpression>get("invocation")).getUserData(Keys.DYNAMIC_CALL_SITE);

            if (callSite != null && "java/lang/runtime/ObjectMethods".equals(callSite.getBootstrapMethod().getDeclaringType().getInternalName())) {
                recordState.removableMethods.add(node);
                return null;
            }
        }

        final RecordComponentInfo componentInfo = recordState.recordComponents.get(node.getName());

        if (componentInfo != null &&
            MetadataHelper.isSameType(componentInfo.getType(), node.getReturnType().toTypeReference())) {

            recordState.removableAccessors.put(componentInfo, node);
        }

        return null;
    }

    @Override
    public Void visitFieldDeclaration(final FieldDeclaration node, final Void data) {
        super.visitFieldDeclaration(node, data);

        final RecordState recordState = _currentRecord;

        if (recordState == null) {
            return null;
        }

        final RecordComponentInfo componentInfo = recordState.recordComponents.get(node.getName());

        if (componentInfo != null &&
            MetadataHelper.isSameType(componentInfo.getType(), node.getReturnType().toTypeReference())) {

            recordState.removableFields.put(componentInfo, node);
        }

        return null;
    }

    @Override
    public Void visitConstructorDeclaration(final ConstructorDeclaration node, final Void p) {
        final RecordState recordState = _currentRecord;
        final RecordState.Constructor oldConstructor = recordState != null ? recordState.currentConstructor : null;

        if (recordState != null) {
            RecordState.Constructor recordConstructor = recordState.constructors.get(node);

            if (recordConstructor == null) {
                recordState.constructors.put(node, recordConstructor = new RecordState.Constructor(node));
            }

            recordState.currentConstructor = recordConstructor;
        }

        try {
            return super.visitConstructorDeclaration(node, p);
        }
        finally {
            if (recordState != null) {
                recordState.currentConstructor = oldConstructor;
            }
        }
    }

    @Override
    public Void visitExpressionStatement(final ExpressionStatement node, final Void data) {
        super.visitExpressionStatement(node, data);

        final RecordState recordState = _currentRecord;
        final RecordState.Constructor recordConstructor = recordState != null ? recordState.currentConstructor : null;

        if (recordConstructor == null) {
            return null;
        }

        if (SUPER_CONSTRUCTOR_CALL.matches(node)) {
            recordConstructor.removableSuperCall.set(node);
            return null;
        }

        final Match match = ASSIGNMENT_PATTERN.match(node);

        if (!match.success()) {
            return null;
        }

        final MemberReferenceExpression f = first(match.<MemberReferenceExpression>get("assignment"));
        final IdentifierExpression p = first(match.<IdentifierExpression>get("parameter"));
        final RecordComponentInfo componentInfo = recordState.recordComponents.get(f.getMemberName());

        if (componentInfo != null) {
            recordConstructor.removableAssignments.put(componentInfo, node);

            final ConstructorDeclaration constructor = recordConstructor.constructor;

            final ParameterDeclaration parameter = firstOrDefault(
                constructor.getParameters(),
                new Predicate<ParameterDeclaration>() {
                    @Override
                    public boolean test(final ParameterDeclaration declaration) {
                        return StringUtilities.equals(declaration.getName(), p.getIdentifier());
                    }
                }
            );

            if (parameter != null) {
                recordConstructor.removableParameters.put(componentInfo, parameter);
            }
        }

        return null;
    }

    protected final static class RecordState {
        final @NotNull TypeDefinition recordDefinition;
        final @NotNull RecordAttribute recordAttribute;
        final @NotNull TypeDeclaration recordDeclaration;
        final @NotNull Map<ConstructorDeclaration, Constructor> constructors;
        final @NotNull List<MethodDeclaration> removableMethods;
        final @NotNull Map<RecordComponentInfo, MethodDeclaration> removableAccessors;
        final @NotNull Map<RecordComponentInfo, FieldDeclaration> removableFields;
        final @NotNull Map<String, RecordComponentInfo> recordComponents;

        Constructor currentConstructor;

        public RecordState(final TypeDefinition recordDefinition, final RecordAttribute recordAttribute, final TypeDeclaration recordDeclaration) {
            this.recordDefinition = recordDefinition;
            this.recordAttribute = recordAttribute;
            this.recordDeclaration = recordDeclaration;
            this.constructors = new HashMap<>();
            this.removableAccessors = new HashMap<>();
            this.removableFields = new HashMap<>();
            this.removableMethods = new ArrayList<>();

            final Map<String, RecordComponentInfo> recordComponents = new HashMap<>();

            for (final RecordComponentInfo component : recordAttribute.getComponents()) {
                recordComponents.put(component.getName(), component);
            }

            this.recordComponents = Collections.unmodifiableMap(recordComponents);
        }

        @SuppressWarnings("UnusedReturnValue")
        public final boolean tryRewrite() {
            if (canRewrite()) {
                rewrite0();
                return true;
            }
            return false;
        }

        public final boolean canRewrite() {
            final List<RecordComponentInfo> components = recordAttribute.getComponents();
            final int componentCount = components.size();

            final Constructor constructor;

            if (removableAccessors.size() != componentCount ||
                removableFields.size() != componentCount ||
                constructors.size() != 1 ||
                (constructor = single(constructors.values())).removableParameters.size() != componentCount ||
                constructor.removableAssignments.size() != componentCount ||
                constructor.removableSuperCall.get() == null) {

                return false;
            }

            for (final RecordComponentInfo component : components) {
                if (!removableAccessors.containsKey(component) ||
                    !constructor.removableAssignments.containsKey(component) ||
                    !constructor.removableParameters.containsKey(component)) {

                    return false;
                }
            }

            return true;
        }

        private void rewrite0() {
            recordDeclaration.getBaseType().remove();
            recordDeclaration.setClassType(ClassType.RECORD);
            recordDeclaration.getModifiers().clear();

            for (final MethodDeclaration accessor : removableAccessors.values()) {
                accessor.remove();
            }

            final RecordState.Constructor constructor = single(constructors.values());
            final ExpressionStatement superCall = constructor.removableSuperCall.get();

            if (superCall != null) {
                superCall.remove();
            }

            for (final ExpressionStatement assignment : constructor.removableAssignments.values()) {
                assignment.remove();
            }

            for (final ParameterDeclaration p : constructor.removableParameters.values()) {
                p.remove();
                p.getModifiers().clear();
                recordDeclaration.addChild(p, EntityDeclaration.RECORD_COMPONENT);
            }

            for (final MethodDeclaration method : removableMethods) {
                method.remove();
            }

            for (final MethodDeclaration accessor : removableAccessors.values()) {
                accessor.remove();
            }

            for (final FieldDeclaration field : removableFields.values()) {
                field.remove();
            }

            final ConstructorDeclaration constructorDeclaration = single(constructors.keySet());

            if (constructorDeclaration.getBody().getStatements().isEmpty()) {
                constructorDeclaration.remove();
            }
        }

        public final static class Constructor {
            final @NotNull ConstructorDeclaration constructor;
            final @NotNull Map<RecordComponentInfo, ParameterDeclaration> removableParameters;
            final @NotNull Map<RecordComponentInfo, ExpressionStatement> removableAssignments;
            final @NotNull StrongBox<ExpressionStatement> removableSuperCall;

            Constructor(final ConstructorDeclaration constructor) {
                this.constructor = VerifyArgument.notNull(constructor, "constructor");
                this.removableParameters = new HashMap<>();
                this.removableAssignments = new HashMap<>();
                this.removableSuperCall = new StrongBox<>();
            }
        }
    }
}
