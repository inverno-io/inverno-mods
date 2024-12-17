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
import io.inverno.mod.web.compiler.spi.server.ErrorWebServerRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.ErrorWebServerRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerControllerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerConfigurerInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerModuleInfo;
import io.inverno.mod.web.compiler.spi.server.WebServerModuleInfoVisitor;
import io.inverno.mod.web.compiler.spi.server.WebServerModuleQualifiedName;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebServerModuleInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebServerModuleInfo implements WebServerModuleInfo {

	private final ModuleElement element;
	private final WebServerModuleQualifiedName name;
	private final WebServerRouteInterceptorConfigurerInfo[] webRouteInterceptorConfigurers;
	private final WebServerRouterConfigurerInfo[] webRouterConfigurers;
	private final ErrorWebServerRouteInterceptorConfigurerInfo[] errorWebRouteInterceptorConfigurers;
	private final ErrorWebServerRouterConfigurerInfo[] errorWebRouterConfigurers;
	private final WebServerConfigurerInfo[] serverConfigurers;
	private final WebServerControllerInfo[] webControllers;
	private final TypeMirror[] contextTypes;
	private final TypeMirror[] typesRegistry;

	/**
	 * <p>
	 * Creates a generic Web server info.
	 * </p>
	 *
	 * @param element                             the server module element
	 * @param name                                the server qualified name
	 * @param webRouteInterceptorConfigurers      the list of interceptors configurers
	 * @param webRouterConfigurers                the list of routes configurer
	 * @param errorWebRouteInterceptorConfigurers the list of error interceptors configurers
	 * @param errorWebRouterConfigurers           the list of error routes configurers
	 * @param serverConfigurers                   the list of server configurers
	 * @param webControllers                      the list of Web controllers
	 * @param contextTypes                        the list of context types required by the interceptors and routes configured in the server
	 * @param typesRegistry                       the types registry
	 */
	public GenericWebServerModuleInfo(
			ModuleElement element,
			WebServerModuleQualifiedName name,
			List<? extends WebServerRouteInterceptorConfigurerInfo> webRouteInterceptorConfigurers,
			List<? extends WebServerRouterConfigurerInfo> webRouterConfigurers,
			List<? extends ErrorWebServerRouteInterceptorConfigurerInfo> errorWebRouteInterceptorConfigurers,
			List<? extends ErrorWebServerRouterConfigurerInfo> errorWebRouterConfigurers,
			List<? extends WebServerConfigurerInfo> serverConfigurers,
			List<? extends WebServerControllerInfo> webControllers,
			Set<TypeMirror> contextTypes,
			Set<TypeMirror> typesRegistry
		) {
		this.element = element;
		this.name = name;

		this.webRouteInterceptorConfigurers = webRouteInterceptorConfigurers != null ? webRouteInterceptorConfigurers.toArray(WebServerRouteInterceptorConfigurerInfo[]::new) : new WebServerRouteInterceptorConfigurerInfo[0];
		this.webRouterConfigurers = webRouterConfigurers != null ? webRouterConfigurers.toArray(WebServerRouterConfigurerInfo[]::new) : new WebServerRouterConfigurerInfo[0];
		this.errorWebRouteInterceptorConfigurers = errorWebRouteInterceptorConfigurers != null ? errorWebRouteInterceptorConfigurers.toArray(ErrorWebServerRouteInterceptorConfigurerInfo[]::new) : new ErrorWebServerRouteInterceptorConfigurerInfo[0];
		this.errorWebRouterConfigurers = errorWebRouterConfigurers != null ? errorWebRouterConfigurers.toArray(ErrorWebServerRouterConfigurerInfo[]::new) : new ErrorWebServerRouterConfigurerInfo[0];
		this.serverConfigurers = serverConfigurers != null ? serverConfigurers.toArray(WebServerConfigurerInfo[]::new) : new WebServerConfigurerInfo[0];
		this.webControllers = webControllers != null ? webControllers.toArray(WebServerControllerInfo[]::new) : new WebServerControllerInfo[0];
		this.contextTypes = contextTypes != null ? contextTypes.toArray(TypeMirror[]::new) : new TypeMirror[0];
		this.typesRegistry = typesRegistry != null ? typesRegistry.toArray(TypeMirror[]::new) : new TypeMirror[0];
	}

	@Override
	public WebServerModuleQualifiedName getQualifiedName() {
		return this.name;
	}

	@Override
	public ModuleElement getElement() {
		return this.element;
	}

	@Override
	public WebServerRouteInterceptorConfigurerInfo[] getInterceptorConfigurers() {
		return this.webRouteInterceptorConfigurers;
	}

	@Override
	public WebServerRouterConfigurerInfo[] getRouterConfigurers() {
		return this.webRouterConfigurers;
	}

	@Override
	public ErrorWebServerRouteInterceptorConfigurerInfo[] getErrorInterceptorConfigurers() {
		return this.errorWebRouteInterceptorConfigurers;
	}

	@Override
	public ErrorWebServerRouterConfigurerInfo[] getErrorRouterConfigurers() {
		return this.errorWebRouterConfigurers;
	}

	@Override
	public WebServerConfigurerInfo[] getServerConfigurers() {
		return this.serverConfigurers;
	}

	@Override
	public WebServerControllerInfo[] getControllers() {
		return this.webControllers;
	}

	@Override
	public TypeMirror[] getContextTypes() {
		return this.contextTypes;
	}

	@Override
	public TypeMirror[] getTypesRegistry() {
		return this.typesRegistry;
	}

	@Override
	public boolean hasError() {
		return Arrays.stream(this.webControllers).anyMatch(ReporterInfo::hasError);
	}

	@Override
	public boolean hasWarning() {
		return Arrays.stream(this.webControllers).anyMatch(ReporterInfo::hasWarning);
	}

	@Override
	public void error(String message) {

	}

	@Override
	public void warning(String message) {

	}

	@Override
	public <R, P> R accept(WebServerModuleInfoVisitor<R, P> visitor, P p) {
		return visitor.visit(this, p);
	}
}
