/*
 * Copyright 2020 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.configuration.compiler.internal;

import io.inverno.core.compiler.spi.ModuleQualifiedName;
import io.inverno.core.compiler.spi.support.AbstractSourceGenerationContext;
import io.inverno.mod.configuration.ConfigurationLoader;
import io.inverno.mod.configuration.compiler.spi.ConfigurationInfo;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * <p>
 * Represents a generation context used by the {@link ConfigurationLoaderClassGenerator} during the generation of a {@link ConfigurationLoader}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class ConfigurationLoaderClassGenerationContext extends AbstractSourceGenerationContext<ConfigurationLoaderClassGenerationContext, ConfigurationLoaderClassGenerationContext.GenerationMode> {

	public static final String CONFIGURATION_LOADER_CLASS_SUFFIX = "Loader";
	
	public static final String CONFIGURATION_INNER_CLASS = "Configuration";
	
	public static final String CONFIGURATOR_INNER_CLASS = "Configurator";
	
	public static final String CONFIGURATION_BEAN_INNER_CLASS = "Bean";
	
	public enum GenerationMode {
		CONFIGURATION_LOADER_CLASS,
		CONFIGURATION_PROPERTIES,
		CONFIGURATION_CONFIGURER,
		CONFIGURATION_PROPERTY_NAME,
		CONFIGURATION_PROPERTY_CONFIGURER,
		CONFIGURATION_IMPL_CLASS,
		CONFIGURATION_IMPL_PROPERTY_FIELD,
		CONFIGURATION_IMPL_PROPERTY_PARAMETER,
		CONFIGURATION_IMPL_PROPERTY_ASSIGNMENT,
		CONFIGURATION_IMPL_PROPERTY_ACCESSOR,
		CONFIGURATION_CONFIGURATOR_CLASS,
		CONFIGURATION_CONFIGURATOR_PROPERTY_FIELD,
		CONFIGURATION_CONFIGURATOR_PROPERTY_INJECTOR,
		CONFIGURATION_BEAN_CLASS
	}
	
	private final TypeMirror collectionType;
	private final TypeMirror listType;
	private final TypeMirror setType;
	private final TypeMirror consumerType;
	private final TypeMirror optionalType;
	private final TypeMirror objectsType;
	
	private ConfigurationInfo configuration;
	
	private final AtomicInteger resultIndex;

	public ConfigurationLoaderClassGenerationContext(Types typeUtils, Elements elementUtils, GenerationMode mode) {
		super(typeUtils, elementUtils, mode);
		this.resultIndex = new AtomicInteger();
		this.collectionType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Collection.class.getCanonicalName()).asType());
		this.setType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Set.class.getCanonicalName()).asType());
		this.listType = this.typeUtils.erasure(this.elementUtils.getTypeElement(List.class.getCanonicalName()).asType());
		this.consumerType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Consumer.class.getCanonicalName()).asType());
		this.optionalType = this.typeUtils.erasure(this.elementUtils.getTypeElement(Optional.class.getCanonicalName()).asType());
		this.objectsType = this.elementUtils.getTypeElement(Objects.class.getCanonicalName()).asType();
	}
	
	private ConfigurationLoaderClassGenerationContext(ConfigurationLoaderClassGenerationContext parentGeneration) {
		super(parentGeneration);
		this.configuration = parentGeneration.configuration;
		this.resultIndex = parentGeneration.resultIndex;

		this.collectionType = parentGeneration.collectionType;
		this.listType = parentGeneration.listType;
		this.setType = parentGeneration.setType;
		this.consumerType = parentGeneration.consumerType;
		this.optionalType = parentGeneration.optionalType;
		this.objectsType = parentGeneration.objectsType;
	}
	
	@Override
	public ConfigurationLoaderClassGenerationContext withMode(GenerationMode mode) {
		ConfigurationLoaderClassGenerationContext context = new ConfigurationLoaderClassGenerationContext(this);
		context.mode = mode;
		return context;
	}

	@Override
	public ConfigurationLoaderClassGenerationContext withIndentDepth(int indentDepth) {
		ConfigurationLoaderClassGenerationContext context = new ConfigurationLoaderClassGenerationContext(this);
		context.indentDepth = indentDepth;
		return context;
	}
	
	@Override
	public ConfigurationLoaderClassGenerationContext withModule(ModuleQualifiedName moduleQualifiedName) {
		ConfigurationLoaderClassGenerationContext context = new ConfigurationLoaderClassGenerationContext(this);
		context.moduleQualifiedName = moduleQualifiedName;
		return context;
	}
	
	public ConfigurationLoaderClassGenerationContext withConfiguration(ConfigurationInfo configuration) {
		ConfigurationLoaderClassGenerationContext context = new ConfigurationLoaderClassGenerationContext(this);
		context.configuration = configuration;
		return context;
	}

	public ConfigurationInfo getConfiguration() {
		return configuration;
	}
	
	public AtomicInteger getResultIndex() {
		return resultIndex;
	}
	
	public TypeMirror getCollectionType() {
		return this.collectionType;
	}
	
	public TypeMirror getListType() {
		return this.listType;
	}
	
	public TypeMirror getSetType() {
		return this.setType;
	}
	
	public TypeMirror getConsumerType() {
		return this.consumerType;
	}
	
	public TypeMirror getOptionalType() {
		return this.optionalType;
	}
	
	public TypeMirror getObjectsType() {
		return this.objectsType;
	}
	
	public String getConfigurationLoaderTypeName(ConfigurationInfo configurationInfo) {
		return this.getTypeName(configurationInfo.getType().toString() + CONFIGURATION_LOADER_CLASS_SUFFIX);
	}
}
