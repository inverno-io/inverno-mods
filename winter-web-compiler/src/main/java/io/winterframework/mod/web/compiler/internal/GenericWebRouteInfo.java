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
package io.winterframework.mod.web.compiler.internal;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;

import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.core.compiler.spi.support.AbstractInfo;
import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.web.compiler.spi.WebControllerInfo;
import io.winterframework.mod.web.compiler.spi.WebParameterInfo;
import io.winterframework.mod.web.compiler.spi.WebResponseBodyInfo;
import io.winterframework.mod.web.compiler.spi.WebRouteInfo;
import io.winterframework.mod.web.compiler.spi.WebRouteQualifiedName;

/**
 * <p>
 * Generic {@link WebRouteInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractInfo
 */
class GenericWebRouteInfo extends AbstractInfo<WebRouteQualifiedName> implements WebRouteInfo {

	private Optional<WebControllerInfo> controller = Optional.empty();
	
	private final String[] paths;
	
	private final boolean matchTrailingSlash;
	
	private final Method[] methods;
	
	private final String[] consumes;
	
	private final String[] produces;
	
	private final String[] languages;
	
	private final List<? extends AbstractWebParameterInfo> parameters;
	
	private final WebResponseBodyInfo responseBody;
	
	private final ExecutableElement element;
	
	/**
	 * <p>
	 * Creates a generic web route info.
	 * </p>
	 * 
	 * @param element            the executable element of the route
	 * @param name               the route qualified name
	 * @param reporter           the route reporter
	 * @param paths              the route paths
	 * @param matchTrailingSlash true to match trailing slash, false otherwise
	 * @param methods            the route methods
	 * @param consumes           the route consumed media ranges
	 * @param produces           the route produced media types
	 * @param languages          the route produced languages
	 * @param parameters         the route parameter info
	 * @param responseBody       the route response body info
	 */
	public GenericWebRouteInfo(
			ExecutableElement element,
			WebRouteQualifiedName name, 
			ReporterInfo reporter,
			Set<String> paths,
			boolean matchTrailingSlash,
			Set<Method> methods,
			Set<String> consumes,
			Set<String> produces,
			Set<String> languages,
			List<? extends AbstractWebParameterInfo> parameters,
			WebResponseBodyInfo responseBody) {
		super(name, reporter instanceof NoOpReporterInfo ? ((NoOpReporterInfo)reporter).getReporter() : reporter);
		this.element = element;
		this.paths = paths.stream().sorted().toArray(String[]::new);
		this.matchTrailingSlash = matchTrailingSlash;
		this.methods = methods.stream().sorted().toArray(Method[]::new);
		this.consumes = consumes.stream().sorted().toArray(String[]::new);
		this.produces = produces.stream().sorted().toArray(String[]::new);
		this.languages = languages.stream().sorted().toArray(String[]::new);
		this.parameters = parameters;
		this.responseBody = responseBody;
	}
	
	@Override
	public Optional<WebControllerInfo> getController() {
		return this.controller;
	}
	
	/**
	 * <p>
	 * Sets the web controller in which the route is defined.
	 * </p>
	 * 
	 * <p>
	 * A web route defined in a provided web router configurer has no controller.
	 * </p>
	 * 
	 * @param controller the route web controller
	 */
	public void setController(WebControllerInfo controller) {
		this.controller = Optional.ofNullable(controller);
	}
	
	@Override
	public boolean hasError() {
		return super.hasError() || this.parameters.stream().anyMatch(parameter -> parameter.hasError());
	}
	
	@Override
	public boolean hasWarning() {
		return super.hasWarning() || this.parameters.stream().anyMatch(parameter -> parameter.hasWarning());
	}
	
	@Override
	public String[] getPaths() {
		return this.paths;
	}
	
	@Override
	public boolean isMatchTrailingSlash() {
		return this.matchTrailingSlash;
	}

	@Override
	public Method[] getMethods() {
		return this.methods;
	}

	@Override
	public String[] getConsumes() {
		return this.consumes;
	}

	@Override
	public String[] getProduces() {
		return this.produces;
	}

	@Override
	public String[] getLanguages() {
		return this.languages;
	}

	@Override
	public Optional<ExecutableElement> getElement() {
		return Optional.ofNullable(this.element);
	}
	
	@Override
	public WebParameterInfo[] getParameters() {
		return this.parameters != null ? this.parameters.stream().toArray(WebParameterInfo[]::new) : null;
	}

	@Override
	public WebResponseBodyInfo getResponseBody() {
		return this.responseBody;
	}	
}
