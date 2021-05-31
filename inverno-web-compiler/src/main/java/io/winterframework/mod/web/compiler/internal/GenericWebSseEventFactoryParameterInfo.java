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
package io.inverno.mod.web.compiler.internal;

import java.util.Objects;
import java.util.Optional;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import io.inverno.mod.web.compiler.spi.WebSseEventFactoryParameterInfo;

/**
 * <p>
 * Generic {@link WebSseEventFactoryParameterInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractWebParameterInfo
 */
class GenericWebSseEventFactoryParameterInfo extends AbstractWebParameterInfo implements WebSseEventFactoryParameterInfo {

	private final SseEventFactoryKind eventFactoryKind;
	
	private final Optional<String> dataMediaType;
	
	/**
	 * <p>
	 * Create a generic server-sent event factory parameter.
	 * </p>
	 * 
	 * @param name             the parameter qualified name
	 * @param reporter         the parameter reporter
	 * @param element          the parameter element
	 * @param eventDataType    the type of the server-sent event data
	 * @param eventFactoryKind the event factory kind
	 */
	public GenericWebSseEventFactoryParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter, VariableElement element, TypeMirror eventDataType, SseEventFactoryKind eventFactoryKind) {
		this(name, reporter, element, eventDataType, eventFactoryKind, null);
	}
	
	/**
	 * <p>
	 * Create a generic server-sent event factory parameter.
	 * </p>
	 * 
	 * @param name             the parameter qualified name
	 * @param reporter         the parameter reporter
	 * @param element          the parameter element
	 * @param eventDataType    the type of the server-sent event data
	 * @param eventFactoryKind the event factory kind
	 * @param dataMediaType    the media type to use to encode server-sent event
	 *                         data
	 */
	public GenericWebSseEventFactoryParameterInfo(WebParameterQualifiedName name, ReporterInfo reporter, VariableElement element, TypeMirror eventDataType, SseEventFactoryKind eventFactoryKind, String dataMediaType) {
		super(name, reporter, element, eventDataType, true);
		this.eventFactoryKind = Objects.requireNonNull(eventFactoryKind);
		this.dataMediaType = Optional.ofNullable(dataMediaType);
	}

	@Override
	public SseEventFactoryKind getEventFactoryKind() {
		return this.eventFactoryKind;
	}
	
	@Override
	public Optional<String> getDataMediaType() {
		return this.dataMediaType;
	}
}
