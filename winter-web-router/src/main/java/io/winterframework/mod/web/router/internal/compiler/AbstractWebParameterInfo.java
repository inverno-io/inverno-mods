/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.web.router.internal.compiler;

import java.util.Objects;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.core.compiler.spi.support.AbstractInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebParameterInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebParameterQualifiedName;

/**
 * @author jkuhn
 *
 */
abstract class AbstractWebParameterInfo extends AbstractInfo<WebParameterQualifiedName> implements WebParameterInfo {

	private final VariableElement element;

	private final TypeMirror type;
	
	private final boolean required;
	
	public AbstractWebParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter, VariableElement element, TypeMirror type, boolean required) {
		super(name, reporter instanceof NoOpReporterInfo ? ((NoOpReporterInfo)reporter).getReporter() : reporter);
		this.element = Objects.requireNonNull(element);
		this.type = Objects.requireNonNull(type);
		this.required = required;
	}

	@Override
	public VariableElement getElement() {
		return this.element;
	}

	@Override
	public TypeMirror getType() {
		return this.type;
	}
	
	@Override
	public boolean isRequired() {
		return this.required;
	}
	
}