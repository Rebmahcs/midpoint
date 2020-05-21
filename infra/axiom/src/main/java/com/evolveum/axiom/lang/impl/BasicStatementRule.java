package com.evolveum.axiom.lang.impl;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Type;
import com.evolveum.axiom.lang.api.AxiomIdentifierDefinition;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;
import com.evolveum.axiom.lang.api.AxiomModel;
import com.evolveum.axiom.lang.api.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.IdentifierSpaceKey;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomStatement;
import com.evolveum.axiom.reactor.Dependency;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public enum BasicStatementRule implements AxiomStatementRule<AxiomIdentifier> {

    REQUIRE_REQUIRED_ITEMS(all(),all()) {
        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomTypeDefinition typeDef = rule.typeDefinition();
            for(AxiomItemDefinition required : typeDef.requiredItems()) {
                //rule.requireChild(required).unsatisfiedMessage(() -> rule.error("%s does not have required statement %s", rule.itemDefinition().name() ,required.name()));
                rule.apply((ctx) -> {});
            }
        }
    },

    COPY_ARGUMENT_VALUE(all(),all()) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomItemDefinition> argument = rule.typeDefinition().argument();
            if(argument.isPresent() && rule.optionalValue().isPresent()) {
               rule.apply(ctx -> ctx.createEffectiveChild(argument.get().name(), ctx.requireValue()));
            }
        }
    },

    EXPAND_TYPE_REFERENCE(items(Item.TYPE_REFERENCE), types(Type.TYPE_REFERENCE)) {
        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier type = rule.requireValue();
            Dependency.Search<AxiomStatement<?>> typeDef = rule.requireGlobalItem(AxiomTypeDefinition.IDENTIFIER_SPACE, AxiomTypeDefinition.identifier(type));
            typeDef.notFound(() ->  rule.error("type '%s' was not found.", type));
            typeDef.unsatisfiedMessage(() -> rule.error("Referenced type %s is not complete.", type));
            rule.apply(ctx -> {
                ctx.replace(typeDef);
            });
        }
    },
    MATERIALIZE_FROM_SUPERTYPE(items(Item.SUPERTYPE_REFERENCE),types(Type.TYPE_REFERENCE)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier type = rule.requireValue();
            Dependency.Search<AxiomStatement<?>> typeDef = rule.requireGlobalItem(AxiomTypeDefinition.IDENTIFIER_SPACE, AxiomTypeDefinition.identifier(type));
            typeDef.notFound(() ->  rule.error("type '%s' was not found.", type));
            typeDef.unsatisfiedMessage(() -> rule.error("Referenced type %s is not complete.", type));
            rule.apply(ctx -> {
                ctx.replace(typeDef);
                AxiomStatement<?> superType = typeDef.get();
                // Copy Identifiers
                ctx.parent().addChildren(superType.children(Item.IDENTIFIER_DEFINITION.name()));
                // Copy Items
                });
        }

    },

    REGISTER_TO_IDENTIFIER_SPACE(all(),all()) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            Collection<AxiomIdentifierDefinition> idDefs = rule.typeDefinition().identifierDefinitions();
            if(!idDefs.isEmpty()) {
                rule.apply(ctx -> {
                    for (AxiomIdentifierDefinition idDef : idDefs) {
                        IdentifierSpaceKey key = keyFrom(idDef, rule);
                        ctx.register(idDef.space(), idDef.scope(), key);
                    }

                });
            }
        }
    },
    IMPORT_DEFAULT_TYPES(all(), types(Type.MODEL)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            Dependency<NamespaceContext> req = rule.requireNamespace(Item.NAMESPACE.name(), namespaceId(AxiomModel.BUILTIN_TYPES));
            req.unsatisfiedMessage(() -> rule.error("Default types not found."));
            rule.apply((ctx) -> {
                ctx.parent().importIdentifierSpace(req.get());
            });
        }
    },
    EXPORT_GLOBALS_FROM_MODEL(all(), types(Type.MODEL)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            String namespace = rule.requiredChildValue(Item.NAMESPACE, String.class);
            rule.apply(ctx -> {
                ctx.exportIdentifierSpace(namespaceId(namespace));
            });
        }
    },
    IMPORT_MODEL(all(),types(Type.IMPORT_DEFINITION)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            String child = rule.requiredChildValue(Item.NAMESPACE, String.class);
            Dependency<NamespaceContext> req = rule.requireNamespace(Item.NAMESPACE.name(), namespaceId(child));
            req.unsatisfiedMessage(() -> rule.error("Namespace %s not found.", child));
            rule.apply((ctx) -> {
                ctx.parent().importIdentifierSpace(req.get());
            });
        }
    },
    APPLY_EXTENSION(all(), types(Type.EXTENSION_DEFINITION)) {

        @Override
        public void apply(Context<AxiomIdentifier> ctx) throws AxiomSemanticException {
            AxiomIdentifier targetName = ctx.requiredChildValue(Item.TARGET, AxiomIdentifier.class);
            Dependency<AxiomStatementContext<?>> targetRef = ctx.modify(AxiomTypeDefinition.IDENTIFIER_SPACE, AxiomTypeDefinition.identifier(targetName));

            ctx.apply(ext -> {
                Context<?> copyItems = ext.modify(targetRef.get(), "Extension: Adding items to target");
                Dependency<AxiomStatement<?>> extDef = copyItems.require(ext);
                copyItems.apply(target -> {
                    // Copy items to target
                    target.addEffectiveChildren(extDef.get().children(Item.ITEM_DEFINITION.name()));
                });
            });

            /*ctx.apply(ext -> {
                Context<AxiomIdentifier> targetAction = ext.newAction();
                targetAction.
            });*/
        }

    },
    /*
     * Not needed - registration is handled by identifier statement
    REGISTER_TYPE(items(Item.TYPE_DEFINITION), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            AxiomIdentifier typeName = rule.requireValue();
            rule.apply(ctx -> ctx.registerAsGlobalItem(typeName));
        }
    },
     */
    ;
/*
    ADD_SUPERTYPE(items(), types(Type.TYPE_DEFINITION)) {

        @Override
        public void apply(Context<AxiomIdentifier> rule) throws AxiomSemanticException {
            Optional<AxiomIdentifier> superType = rule.optionalChildValue(Item.SUPERTYPE_REFERENCE, AxiomIdentifier.class);
            if(superType.isPresent()) {
                Requirement<AxiomStatement<?>> req = rule.requireGlobalItem(Item.TYPE_DEFINITION, superType.get());
                rule.apply((ctx) -> {
                    //ctx.builder().add(Item.SUPERTYPE_REFERENCE, req.get());
                });
                rule.errorMessage(() -> {
                    if(!req.isSatisfied()) {
                        return rule.error("Supertype %s is not defined", superType.get());
                    }
                    return null;
                });
            }
        }
    };*/

    private final Set<AxiomIdentifier> items;
    private final Set<AxiomIdentifier> types;


    private BasicStatementRule(Set<AxiomIdentifier> items, Set<AxiomIdentifier> types) {
        this.items = ImmutableSet.copyOf(items);
        this.types = ImmutableSet.copyOf(types);
    }

    static IdentifierSpaceKey keyFrom(AxiomIdentifierDefinition idDef, Context<AxiomIdentifier> ctx) {
        ImmutableMap.Builder<AxiomIdentifier, Object> components = ImmutableMap.builder();
        for(AxiomItemDefinition cmp : idDef.components()) {
            components.put(cmp.name(), ctx.requiredChildValue(cmp, Object.class));
        }
        return IdentifierSpaceKey.from(components.build());
    }

    @Override
    public boolean isApplicableTo(AxiomItemDefinition definition) {
        return (items.isEmpty() || items.contains(definition.name()))
                && (types.isEmpty() || types.contains(definition.type().name()));
    }

    private static ImmutableSet<AxiomIdentifier> types(AxiomTypeDefinition... types) {
        ImmutableSet.Builder<AxiomIdentifier> builder = ImmutableSet.builder();
        for (AxiomTypeDefinition item : types) {
            builder.add(item.name());
        }
        return builder.build();

    }

    private static ImmutableSet<AxiomIdentifier> items(AxiomItemDefinition... items) {
        ImmutableSet.Builder<AxiomIdentifier> builder = ImmutableSet.builder();
        for (AxiomItemDefinition item : items) {
            builder.add(item.name());
        }
        return builder.build();
    }

    private static ImmutableSet<AxiomIdentifier> all() {
        return ImmutableSet.of();
    }

    private static IdentifierSpaceKey namespaceId(String uri) {
        return IdentifierSpaceKey.of(Item.NAMESPACE.name(), uri);
    }
}
