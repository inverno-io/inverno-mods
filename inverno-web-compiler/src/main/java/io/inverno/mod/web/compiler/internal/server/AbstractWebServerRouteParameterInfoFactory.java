/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.AbstractWebRouteParameterInfoFactory;
import io.inverno.mod.web.compiler.internal.GenericWebExchangeContextParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import java.util.List;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

/**
 * <p>
 * Base Web client {@link AbstractWebRouteParameterInfoFactory} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public abstract class AbstractWebServerRouteParameterInfoFactory extends AbstractWebRouteParameterInfoFactory {

	/* Contextual */
	protected final TypeMirror exchangeContextType;

	/**
	 * <p>
	 * Creates a Web server route parameter info factory.
	 * </p>
	 *
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public AbstractWebServerRouteParameterInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		super(pluginContext, pluginExecution);

		this.exchangeContextType = this.pluginContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType();
	}

	@Override
	protected AbstractWebParameterInfo createContextualParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType) {
		if(this.pluginContext.getTypeUtils().isAssignable(parameterElement.asType(), this.exchangeContextType)) {
			return this.createExchangeContextParameter(reporter, parameterQName, parameterElement);
		}
		return null;
	}

	/**
	 * <p>
	 * Creates an exchange context parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 *
	 * @return a Web exchange context parameter info
	 */
	private GenericWebExchangeContextParameterInfo createExchangeContextParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		TypeMirror contextType = parameterElement.asType();
		if(contextType.getKind() == TypeKind.TYPEVAR) {
			contextType = ((TypeVariable)contextType).getUpperBound();
		}

		List<? extends TypeMirror> actualTypes;
		if(contextType.getKind() == TypeKind.INTERSECTION) {
			actualTypes = ((IntersectionType)contextType).getBounds();
		}
		else {
			actualTypes = List.of(contextType);
		}

		if(actualTypes.stream().anyMatch(type -> this.pluginContext.getTypeUtils().asElement(type).getKind() != ElementKind.INTERFACE)) {
			reporter.error("Web exchange context must be an interface");
		}
		return new GenericWebExchangeContextParameterInfo(parameterQName, reporter, parameterElement, parameterElement.asType(), contextType);
	}
}
