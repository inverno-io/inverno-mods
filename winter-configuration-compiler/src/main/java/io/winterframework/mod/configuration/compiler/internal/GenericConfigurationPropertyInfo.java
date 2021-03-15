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
import javax.lang.model.type.TypeMirror;

import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.core.compiler.spi.support.AbstractInfo;
import io.winterframework.mod.configuration.compiler.spi.ConfigurationPropertyInfo;
import io.winterframework.mod.configuration.compiler.spi.PropertyQualifiedName;

/**
 * <p>
 * Generic {@link ConfigurationPropertyInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ConfigurationPropertyInfo
 */
class GenericConfigurationPropertyInfo extends AbstractInfo<PropertyQualifiedName> implements ConfigurationPropertyInfo {

	private ExecutableElement accessor;
	
	public GenericConfigurationPropertyInfo(PropertyQualifiedName name, ReporterInfo reporter, ExecutableElement element) {
		super(name, reporter);
		this.accessor = element;
	}

	@Override
	public boolean isDefault() {
		return this.accessor.isDefault();
	}

	@Override
	public TypeMirror getType() {
		return this.accessor.getReturnType();
	}
}
