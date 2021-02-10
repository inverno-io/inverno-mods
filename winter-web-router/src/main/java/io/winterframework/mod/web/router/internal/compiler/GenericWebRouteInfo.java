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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.ExecutableElement;

import io.winterframework.core.compiler.spi.ReporterInfo;
import io.winterframework.core.compiler.spi.support.AbstractInfo;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.router.internal.compiler.spi.WebParameterInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebResponseBodyInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebRouteInfo;
import io.winterframework.mod.web.router.internal.compiler.spi.WebRouteQualifiedName;

/**
 * @author jkuhn
 *
 */
public class GenericWebRouteInfo extends AbstractInfo<WebRouteQualifiedName> implements WebRouteInfo {

	private final String[] paths;
	
	private final boolean matchTrailingSlash;
	
	private final Method[] methods;
	
	private final String[] consumes;
	
	private final String[] produces;
	
	private final String[] languages;
	
	private final List<? extends AbstractWebParameterInfo> parameters;
	
	private final WebResponseBodyInfo responseBody;
	
	private final ExecutableElement routeElement;
	
	public GenericWebRouteInfo(
			WebRouteQualifiedName name, 
			ReporterInfo reporter,
			Set<String> paths,
			boolean matchTrailingSlash,
			Set<Method> methods,
			Set<String> consumes,
			Set<String> produces,
			Set<String> languages,
			ExecutableElement routeElement,
			List<? extends AbstractWebParameterInfo> parameters,
			WebResponseBodyInfo responseBody) {
		super(name, reporter instanceof NoOpReporterInfo ? ((NoOpReporterInfo)reporter).getReporter() : reporter);
		
		this.paths = paths.stream().sorted().toArray(String[]::new);
		this.matchTrailingSlash = matchTrailingSlash;
		this.methods = methods.stream().sorted().toArray(Method[]::new);
		this.consumes = consumes.stream().sorted().toArray(String[]::new);
		this.produces = produces.stream().sorted().toArray(String[]::new);
		this.languages = languages.stream().sorted().toArray(String[]::new);
		this.parameters = parameters;
		this.responseBody = responseBody;
		this.routeElement = routeElement;
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
	public Optional<ExecutableElement> getRouteElement() {
		return Optional.ofNullable(this.routeElement);
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
