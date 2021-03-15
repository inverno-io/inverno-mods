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
package io.winterframework.mod.configuration.compiler.internal;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import io.winterframework.core.compiler.spi.ModuleQualifiedName;
import io.winterframework.core.compiler.spi.support.AbstractSourceGenerationContext;
import io.winterframework.mod.configuration.ConfigurationLoader;
import io.winterframework.mod.configuration.compiler.spi.ConfigurationInfo;

/**
 * <p>
 * Represents a generation context used by the
 * {@link ConfigurationLoaderClassGenerator} during the generation of a {@link ConfigurationLoader}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class ConfigurationLoaderClassGenerationContext extends AbstractSourceGenerationContext<ConfigurationLoaderClassGenerationContext, ConfigurationLoaderClassGenerationContext.GenerationMode> {

	public static enum GenerationMode {
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
	
	private TypeMirror collectionType;
	private TypeMirror listType;
	private TypeMirror setType;
	
	private ConfigurationInfo configuration;
	
	private AtomicInteger resultIndex;

	public ConfigurationLoaderClassGenerationContext(Types typeUtils, Elements elementUtils, GenerationMode mode) {
		super(typeUtils, elementUtils, mode);
		this.resultIndex = new AtomicInteger();
		this.collectionType = this.getTypeUtils().erasure(this.getElementUtils().getTypeElement(Collection.class.getCanonicalName()).asType());
		this.setType = this.getTypeUtils().erasure(this.getElementUtils().getTypeElement(Set.class.getCanonicalName()).asType());
		this.listType = this.getTypeUtils().erasure(this.getElementUtils().getTypeElement(List.class.getCanonicalName()).asType());
	}
	
	private ConfigurationLoaderClassGenerationContext(ConfigurationLoaderClassGenerationContext parentGeneration) {
		super(parentGeneration);
		this.configuration = parentGeneration.configuration;
		this.resultIndex = parentGeneration.resultIndex;
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
		return this.collectionType != null ? this.collectionType : this.parentGeneration.getCollectionType();
	}
	
	public TypeMirror getListType() {
		return this.listType != null ? this.listType : this.parentGeneration.getListType();
	}
	
	public TypeMirror getSetType() {
		return this.setType != null ? this.setType : this.parentGeneration.getSetType();
	}
}
