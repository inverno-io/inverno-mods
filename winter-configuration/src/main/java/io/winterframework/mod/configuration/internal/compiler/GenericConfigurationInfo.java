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
package io.winterframework.mod.configuration.internal.compiler;

import java.util.Collections;
import java.util.List;

import javax.lang.model.type.DeclaredType;

import io.winterframework.core.compiler.spi.BeanQualifiedName;
import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.mod.configuration.internal.compiler.spi.ConfigurationInfo;
import io.winterframework.mod.configuration.internal.compiler.spi.ConfigurationInfoVisitor;
import io.winterframework.mod.configuration.internal.compiler.spi.ConfigurationPropertyInfo;

/**
 * @author jkuhn
 *
 */
class GenericConfigurationInfo extends AbstractInfo<BeanQualifiedName> implements ConfigurationInfo {

	private List<? extends ConfigurationPropertyInfo> properties;
	
	private DeclaredType type;
	
	private boolean generateBean;
	
	private boolean overridable;
	
	public GenericConfigurationInfo(BeanQualifiedName name, ReporterInfo reporter, 
			DeclaredType type,
			List<? extends ConfigurationPropertyInfo> properties) {
		this(name, reporter, type, properties, false, false);
	}
	
	public GenericConfigurationInfo(BeanQualifiedName name, ReporterInfo reporter, 
			DeclaredType type,
			List<? extends ConfigurationPropertyInfo> properties,
			boolean generateBean,
			boolean overridable) {
			super(name, reporter);
		this.type = type;
		this.properties = properties != null ? Collections.unmodifiableList(properties) : List.of();
		this.generateBean = generateBean;
		this.overridable = overridable;
	}

	@Override
	public DeclaredType getType() {
		return this.type;
	}

	@Override
	public ConfigurationPropertyInfo[] getProperties() {
		return this.properties.stream().toArray(ConfigurationPropertyInfo[]::new);
	}
	
	@Override
	public boolean isGenerateBean() {
		return this.generateBean;
	}
	
	@Override
	public boolean isOverridable() {
		return this.overridable;
	}
	
	@Override
	public <R, P> R accept(ConfigurationInfoVisitor<R, P> visitor, P p) {
		return visitor.visit(this,  p);
	}
}
