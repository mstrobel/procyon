package com.strobel.decompiler.languages.java.ast.transforms;

import com.strobel.assembler.metadata.Flags;
import com.strobel.assembler.metadata.LanguageFeature;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.ast.Variable;
import com.strobel.decompiler.languages.java.analysis.Correlator;
import com.strobel.decompiler.languages.java.ast.*;
import com.strobel.decompiler.patterns.*;

import java.util.List;

import static com.strobel.core.CollectionUtilities.*;

public class IntroducePatternMatchingTransform extends ContextTrackingVisitor<Void> {

    private final IfElseStatement simplePattern;

    public IntroducePatternMatchingTransform(final DecompilerContext context) {
        super(context);

        final VariableDeclarationStatement v = new VariableDeclarationStatement(
            new BackReference("type").toType(),
            Pattern.ANY_STRING,
            new CastExpression(new BackReference("type").toType(), new BackReference("expression").toExpression())
        );

        v.setAnyModifiers(true);

        simplePattern = new IfElseStatement(
            new NamedNode("instanceOf",
                          new InstanceOfExpression(new NamedNode("expression", new AnyNode()).toExpression(),
                                                   new NamedNode("type", new AnyNode()).toType())).toExpression(),
            new BlockStatement(
                new Choice(new NamedNode("variableDeclaration", v),
                           new SubtreeMatch(new CastExpression(new BackReference("type").toType(),
                                                               new BackReference("expression").toExpression()),
                                            "usageViaCast",
                                            true)).toStatement(),
                new Repeat(new AnyNode()).toStatement()
            ),
            new OptionalNode(new AnyNode()).toStatement()
        );
    }

    @Override
    public void run(final AstNode compilationUnit) {
        if (context.isSupported(LanguageFeature.PATTERN_MATCHING)) {
            super.run(compilationUnit);
        }
    }

    @Override
    public Void visitIfElseStatement(final IfElseStatement node, final Void data) {
        super.visitIfElseStatement(node, data);

        //noinspection IfStatementWithIdenticalBranches
        if (trySimplePatternMatch(node)) {
            return null;
        }

        // TODO: Handle more complex cases, like the `instanceof` test appearing before/after an &&, ||, etc.

        return null;
    }

    private boolean trySimplePatternMatch(final IfElseStatement node) {
        final Match m = simplePattern.match(node);

        if (!m.success()) {
            return false;
        }

        final InstanceOfExpression io = first(m.<InstanceOfExpression>get("instanceOf"));

        final Identifier nameToken;

        if (m.has("variableDeclaration")) {
            final VariableDeclarationStatement v = first(m.<VariableDeclarationStatement>get("variableDeclaration"));
            final VariableInitializer vi = first(v.getVariables());
            nameToken = vi.getNameToken();

            v.remove();
            nameToken.remove();
            io.setModifiers(v.getModifiers());

            final Variable variable = vi.getUserData(Keys.VARIABLE);

            if (variable != null) {
                io.putUserData(Keys.VARIABLE, variable);
            }
        }
        else if (m.has("usageViaCast")) {
            final NameVariables nv;
            final MethodDeclaration md = node.getParent(MethodDeclaration.class);

            if (md == null || (nv = md.getUserData(Keys.NAME_VARIABLES)) == null) {
                return false;
            }

            final AstType astType = first(m.<AstType>get("type"));
            final TypeReference type = astType.toTypeReference();
            final List<CastExpression> casts = toList(m.<CastExpression>get("usageViaCast"));

            if (type == null) {
                return false;
            }

            nameToken = Identifier.create(nv.getNameForType(type));

            if (casts.size() > 1 && io.getExpression() instanceof IdentifierExpression) {
                //
                // Replace additional casts only if (1) the test expression is a simple identifier; and
                // (2) the target identifier is not reassigned anywhere in the block.
                //

                final String id = ((IdentifierExpression) io.getExpression()).getIdentifier();
                final Statement parentStatement = casts.get(0).getParent(Statement.class);

                if (parentStatement == null || parentStatement.getParent() == null) {
                    return false;
                }

                final BlockStatement placeholder = new BlockStatement();

                parentStatement.replaceWith(placeholder);

                final boolean correlated = Correlator.areCorrelated(new IdentifierExpression(id), node.getTrueStatement());

                placeholder.replaceWith(parentStatement);

                if (!correlated) {
                    for (int i = 1; i < casts.size(); i++) {
                        casts.get(i).replaceWith(new IdentifierExpression(casts.get(i).getOffset(), nameToken.clone()));
                    }
                }
            }

            casts.get(0).replaceWith(new IdentifierExpression(casts.get(0).getOffset(), nameToken.clone()));
            io.addModifier(Flags.Flag.FINAL);
        }
        else {
            return false;
        }

        io.setIdentifier(nameToken);

        return true;
    }
}
