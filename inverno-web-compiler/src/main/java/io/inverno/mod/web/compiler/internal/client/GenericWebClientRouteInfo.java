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
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.compiler.internal.AbstractWebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteReturnInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteQualifiedName;
import io.inverno.mod.web.compiler.spi.client.WebClientStubInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;

/**
 * <p>
 * Generic {@link WebClientRouteInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClientRouteInfo extends AbstractInfo<WebClientRouteQualifiedName> implements WebClientRouteInfo {

	private final ExecutableElement element;
	private final ExecutableType routeType;
	private final String path;
	private final Method method;
	private final String[] consumes;
	private final String produce;
	private final String[] languages;
	private final List<? extends AbstractWebParameterInfo> parameters;
	private final WebClientRouteReturnInfo routeReturn;

	private WebClientStubInfo clientStub;

	/**
	 * <p>
	 * Creates a generic Web client route info.
	 * </p>
	 *
	 * @param element     the element
	 * @param routeType   the route executable type
	 * @param name        the route qualified name
	 * @param reporter    the reporter info
	 * @param path        the path
	 * @param method      the method
	 * @param consumes    the accepted media types
	 * @param produce     the produce media type
	 * @param languages   the accepted language
	 * @param parameters  the route parameters
	 * @param routeReturn the route return info
	 */
	public GenericWebClientRouteInfo(
			ExecutableElement element,
			ExecutableType routeType,
			WebClientRouteQualifiedName name,
			ReporterInfo reporter,
			String path,
			Method method,
			Set<String> consumes,
			String produce,
			Set<String> languages,
			List<? extends AbstractWebParameterInfo> parameters,
			WebClientRouteReturnInfo routeReturn
		) {
		super(name, reporter);
		this.element = element;
		this.routeType = routeType;
		this.path = path;
		this.method = method;
		this.consumes = consumes.stream().sorted().toArray(String[]::new);
		this.produce = produce;
		this.languages = languages.stream().sorted().toArray(String[]::new);
		this.parameters = parameters;
		this.routeReturn = routeReturn;
	}

	@Override
	public boolean hasError() {
		return super.hasError() || this.parameters.stream().anyMatch(ReporterInfo::hasError);
	}

	@Override
	public boolean hasWarning() {
		return super.hasWarning() || this.parameters.stream().anyMatch(ReporterInfo::hasWarning);
	}

	@Override
	public WebClientStubInfo getClientStub() {
		return this.clientStub;
	}

	/**
	 * <p>
	 * Sets the Web client stub in which the route is defined.
	 * </p>
	 *
	 * @param clientStub the route Web client stub
	 */
	public void setClientStub(WebClientStubInfo clientStub) {
		this.clientStub = clientStub;
	}

	@Override
	public ExecutableType getType() {
		return this.routeType;
	}

	@Override
	public ExecutableElement getElement() {
		return this.element;
	}

	@Override
	public Optional<String> getPath() {
		return Optional.ofNullable(this.path);
	}

	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public String[] getConsumes() {
		return this.consumes;
	}

	@Override
	public Optional<String> getProduce() {
		return  Optional.ofNullable(this.produce);
	}

	@Override
	public String[] getLanguages() {
		return this.languages;
	}

	@Override
	public WebParameterInfo[] getParameters() {
		return this.parameters != null ? this.parameters.toArray(WebParameterInfo[]::new) : null;
	}

	@Override
	public WebClientRouteReturnInfo getReturn() {
		return this.routeReturn;
	}
}
