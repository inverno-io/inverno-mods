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
package io.inverno.mod.web.compiler.internal.client;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.plugin.PluginContext;
import io.inverno.core.compiler.spi.plugin.PluginExecution;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.AbstractWebRouteParameterInfoFactory;
import io.inverno.mod.web.compiler.internal.GenericWebExchangeParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterQualifiedName;
import java.util.List;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

/**
 * <p>
 * Base Web client {@link AbstractWebRouteParameterInfoFactory} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public abstract class AbstractWebClientRouteParameterInfoFactory extends AbstractWebRouteParameterInfoFactory {

	/* Contextual */
	protected final TypeMirror webExchangeConfigurerType;
	protected final TypeMirror exchangeContextType;

	/**
	 * <p>
	 * Creates a Web client route parameter info factory.
	 * </p>
	 *
	 * @param pluginContext   the Web compiler plugin context
	 * @param pluginExecution the Web compiler plugin execution
	 */
	public AbstractWebClientRouteParameterInfoFactory(PluginContext pluginContext, PluginExecution pluginExecution) {
		super(pluginContext, pluginExecution);

		this.webExchangeConfigurerType = this.pluginContext.getTypeUtils().erasure(this.pluginContext.getElementUtils().getTypeElement(WebExchange.Configurer.class.getCanonicalName()).asType());
		this.exchangeContextType = this.pluginContext.getElementUtils().getTypeElement(ExchangeContext.class.getCanonicalName()).asType();
	}

	@Override
	protected AbstractWebParameterInfo createContextualParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement, TypeMirror parameterType) {
		if(this.pluginContext.getTypeUtils().isSameType(this.pluginContext.getTypeUtils().erasure(parameterType), this.webExchangeConfigurerType)) {
			return this.createExchangeParameter(reporter, parameterQName, parameterElement);
		}
		return null;
	}

	/**
	 * <p>
	 * Creates an exchange parameter info.
	 * </p>
	 *
	 * @param reporter         the parameter reporter
	 * @param parameterQName   the parameter qualified name
	 * @param parameterElement the parameter element
	 *
	 * @return a Web exchange parameter info
	 */
	private GenericWebExchangeParameterInfo createExchangeParameter(ReporterInfo reporter, WebParameterQualifiedName parameterQName, VariableElement parameterElement) {
		TypeMirror contextType = this.exchangeContextType;
		List<? extends TypeMirror> typeArguments = ((DeclaredType)parameterElement.asType()).getTypeArguments();
		if(!typeArguments.isEmpty()) {
			contextType = typeArguments.getFirst();
			if(contextType.getKind() == TypeKind.WILDCARD) {
				TypeMirror extendsBound = ((WildcardType)contextType).getExtendsBound();
				if(extendsBound != null) {
					contextType = extendsBound;
				}
				else {
					contextType = this.exchangeContextType;
				}
			}
			else if(contextType.getKind() == TypeKind.TYPEVAR) {
				contextType = ((TypeVariable)contextType).getUpperBound();
			}
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
		return new GenericWebExchangeParameterInfo(parameterQName, reporter, parameterElement, ((DeclaredType)parameterElement.asType()).getTypeArguments().getFirst(), contextType);
	}
}
