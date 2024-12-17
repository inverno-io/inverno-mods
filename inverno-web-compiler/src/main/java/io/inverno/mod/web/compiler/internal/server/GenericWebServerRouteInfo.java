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
package io.inverno.mod.web.compiler.internal.server;

import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.internal.NoOpReporterInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.compiler.spi.server.WebServerControllerInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteQualifiedName;
import javax.lang.model.type.ExecutableType;

/**
 * <p>
 * Generic {@link WebServerRouteInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractInfo
 */
public class GenericWebServerRouteInfo extends AbstractInfo<WebServerRouteQualifiedName> implements WebServerRouteInfo {

	private final ExecutableElement element;
	private final ExecutableType routeType;
	private final String[] paths;
	private final boolean matchTrailingSlash;
	private final Method[] methods;
	private final String[] consumes;
	private final String[] produces;
	private final String[] languages;
	private final List<? extends AbstractWebParameterInfo> parameters;
	private final WebServerResponseBodyInfo responseBody;

	private WebServerControllerInfo controller;
	
	/**
	 * <p>
	 * Creates a generic Web server route info.
	 * </p>
	 * 
	 * @param element            the executable element of the route
	 * @param routeType          the executable type of the route as member of the Web controller interface
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
	public GenericWebServerRouteInfo(
			ExecutableElement element,
			ExecutableType routeType,
			WebServerRouteQualifiedName name,
			ReporterInfo reporter,
			Set<String> paths,
			boolean matchTrailingSlash,
			Set<Method> methods,
			Set<String> consumes,
			Set<String> produces,
			Set<String> languages,
			List<? extends AbstractWebParameterInfo> parameters,
			WebServerResponseBodyInfo responseBody) {
		super(name, reporter instanceof NoOpReporterInfo ? ((NoOpReporterInfo)reporter).getReporter() : reporter);
		this.element = element;
		this.routeType = routeType;
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
	public boolean hasError() {
		return super.hasError() || this.parameters.stream().anyMatch(AbstractInfo::hasError);
	}

	@Override
	public boolean hasWarning() {
		return super.hasWarning() || this.parameters.stream().anyMatch(AbstractInfo::hasWarning);
	}
	
	@Override
	public Optional<WebServerControllerInfo> getController() {
		return Optional.ofNullable(this.controller);
	}
	
	/**
	 * <p>
	 * Sets the Web controller in which the route is defined.
	 * </p>
	 * 
	 * <p>
	 * A Web route defined in a provided Web router configurer has no controller.
	 * </p>
	 * 
	 * @param controller the route Web controller
	 */
	public void setController(WebServerControllerInfo controller) {
		this.controller = controller;
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
	public Optional<ExecutableType> getType() {
		return Optional.of(this.routeType);
	}

	@Override
	public WebParameterInfo[] getParameters() {
		return this.parameters != null ? this.parameters.toArray(WebParameterInfo[]::new) : null;
	}

	@Override
	public WebServerResponseBodyInfo getResponseBody() {
		return this.responseBody;
	}
}
