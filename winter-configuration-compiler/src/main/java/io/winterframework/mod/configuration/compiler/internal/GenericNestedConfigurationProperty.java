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

import javax.lang.model.element.ExecutableElement;

import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.mod.configuration.compiler.spi.ConfigurationInfo;
import io.winterframework.mod.configuration.compiler.spi.NestedConfigurationPropertyInfo;
import io.winterframework.mod.configuration.compiler.spi.PropertyQualifiedName;

/**
 * <p>
 * Generic {@link NestedConfigurationPropertyInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see NestedConfigurationPropertyInfo
 */
class GenericNestedConfigurationProperty extends GenericConfigurationPropertyInfo implements NestedConfigurationPropertyInfo {

	private ConfigurationInfo configuration;
	
	public GenericNestedConfigurationProperty(PropertyQualifiedName name, ReporterInfo reporter, 
			ExecutableElement element,
			ConfigurationInfo nestedConfiguration) {
		super(name, reporter, element);
		
		this.configuration = nestedConfiguration;
	}

	@Override
	public ConfigurationInfo getConfiguration() {
		return this.configuration;
	}
}
