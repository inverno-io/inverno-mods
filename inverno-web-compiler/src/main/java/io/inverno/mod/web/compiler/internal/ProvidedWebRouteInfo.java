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

import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;

import io.inverno.core.compiler.spi.ReporterInfo;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.annotation.WebRoutes;
import io.inverno.mod.web.compiler.spi.WebControllerInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebResponseBodyInfo;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebRouteQualifiedName;

/**
 * <p>
 * Provided {@link WebRouteInfo} implementation used to describes routes specified in a {@link WebRoutes} annotation on a web routes configurer or a web router.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractInfo
 */
class ProvidedWebRouteInfo extends AbstractInfo<WebRouteQualifiedName> implements WebRouteInfo {

	private final String[] paths;
	
	private final boolean matchTrailingSlash;
	
	private final Method[] methods;
	
	private final String[] consumes;
	
	private final String[] produces;
	
	private final String[] languages;
	
	/**
	 * <p>
	 * Creates a provided web route info.
	 * </p>
	 * 
	 * @param name               the route qualified name
	 * @param reporter           the route reporter
	 * @param paths              the route paths
	 * @param matchTrailingSlash true to match trailing slash, false otherwise
	 * @param methods            the route methods
	 * @param consumes           the route consumed media ranges
	 * @param produces           the route produced media types
	 * @param languages          the route produced languages
	 */
	public ProvidedWebRouteInfo(
			WebRouteQualifiedName name, 
			ReporterInfo reporter,
			Set<String> paths,
			boolean matchTrailingSlash,
			Set<Method> methods,
			Set<String> consumes,
			Set<String> produces,
			Set<String> languages) {
		super(name, reporter instanceof NoOpReporterInfo ? ((NoOpReporterInfo)reporter).getReporter() : reporter);
		
		this.paths = paths.stream().sorted().toArray(String[]::new);
		this.matchTrailingSlash = matchTrailingSlash;
		this.methods = methods.stream().sorted().toArray(Method[]::new);
		this.consumes = consumes.stream().sorted().toArray(String[]::new);
		this.produces = produces.stream().sorted().toArray(String[]::new);
		this.languages = languages.stream().sorted().toArray(String[]::new);
	}
	
	@Override
	public Optional<WebControllerInfo> getController() {
		return Optional.empty();
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
		return Optional.empty();
	}
	
	@Override
	public WebParameterInfo[] getParameters() {
		return null;
	}

	@Override
	public WebResponseBodyInfo getResponseBody() {
		return null;
	}
}
