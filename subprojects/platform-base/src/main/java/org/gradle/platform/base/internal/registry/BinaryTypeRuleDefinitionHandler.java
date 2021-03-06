/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.internal.registry;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.reflect.JavaReflectionUtil;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.internal.core.ModelMutator;
import org.gradle.model.internal.inspect.MethodRuleDefinition;
import org.gradle.model.internal.inspect.RuleSourceDependencies;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.model.internal.type.ModelType;
import org.gradle.platform.base.BinaryContainer;
import org.gradle.platform.base.BinarySpec;
import org.gradle.platform.base.BinaryType;
import org.gradle.platform.base.BinaryTypeBuilder;
import org.gradle.platform.base.binary.BaseBinarySpec;
import org.gradle.platform.base.internal.builder.TypeBuilderInternal;

public class BinaryTypeRuleDefinitionHandler extends TypeRuleDefinitionHandler<BinaryType, BinarySpec, BaseBinarySpec> {

    private Instantiator instantiator;

    public BinaryTypeRuleDefinitionHandler(final Instantiator instantiator) {
        super("binary", BinarySpec.class, BaseBinarySpec.class, BinaryTypeBuilder.class, JavaReflectionUtil.factory(new DirectInstantiator(), DefaultBinaryTypeBuilder.class));
        this.instantiator = instantiator;
    }

    @Override
    <R> void doRegister(MethodRuleDefinition<R> ruleDefinition, ModelRegistry modelRegistry, RuleSourceDependencies dependencies, ModelType<? extends BinarySpec> type, TypeBuilderInternal<BinarySpec> builder) {
        ModelType<? extends BaseBinarySpec> implementation = determineImplementationType(type, builder);
        dependencies.add(ComponentModelBasePlugin.class);
        if (implementation != null) {
            ModelMutator<?> mutator = new RegisterTypeRule<BinarySpec, BaseBinarySpec>(type, implementation, ruleDefinition.getDescriptor(), new RegistrationAction(instantiator));
            modelRegistry.mutate(mutator);
        }
    }

    public static class DefaultBinaryTypeBuilder extends AbstractTypeBuilder<BinarySpec> implements BinaryTypeBuilder<BinarySpec> {
        public DefaultBinaryTypeBuilder() {
            super(BinaryType.class);
        }
    }

    private static class RegistrationAction implements Action<RegistrationContext<BinarySpec, BaseBinarySpec>> {
        private final Instantiator instantiator;

        public RegistrationAction(Instantiator instantiator) {
            this.instantiator = instantiator;
        }

        public void execute(RegistrationContext<BinarySpec, BaseBinarySpec> context) {
            BinaryContainer binaries = context.getExtensions().getByType(BinaryContainer.class);
            doRegister(binaries, context.getType(), context.getImplementation());
        }

        private <T extends BinarySpec, U extends BaseBinarySpec> void doRegister(BinaryContainer binaries, ModelType<T> type, final ModelType<U> implementation) {
            binaries.registerFactory(type.getConcreteClass(), new NamedDomainObjectFactory<T>() {
                public T create(String name) {
                    // safe because we implicitly know that U extends V, but can't express this in the type system
                    @SuppressWarnings("unchecked")
                    T created = (T) BaseBinarySpec.create(implementation.getConcreteClass(), name, instantiator);
                    return created;
                }
            });
        }
    }
}

