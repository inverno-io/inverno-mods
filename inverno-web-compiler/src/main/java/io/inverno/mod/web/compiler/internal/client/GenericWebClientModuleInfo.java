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
import io.inverno.mod.web.compiler.spi.client.WebClientModuleInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientModuleInfoVisitor;
import io.inverno.mod.web.compiler.spi.client.WebClientModuleQualifiedName;
import io.inverno.mod.web.compiler.spi.client.WebClientRouteInterceptorConfigurerInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientSocketInfo;
import io.inverno.mod.web.compiler.spi.client.WebClientStubInfo;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebClientModuleInfo} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class GenericWebClientModuleInfo implements WebClientModuleInfo {

	private final ModuleElement element;
	private final WebClientModuleQualifiedName name;

	private final List<? extends WebClientSocketInfo> webClientSockets;
	private final List<? extends WebClientRouteInterceptorConfigurerInfo> webRouteInterceptorConfigurers;
	private final List<? extends WebClientStubInfo> webClientStubs;

	private final Set<? extends TypeMirror> contextTypes;

	/**
	 * <p>
	 * Creates a generic Web client module info
	 * </p>
	 *
	 * @param element                        the module element
	 * @param name                           the module name
	 * @param webClientSockets               the Web client sockets
	 * @param webRouteInterceptorConfigurers the route interceptor configurers
	 * @param webClientStubs                 the Web client stubs
	 * @param contextTypes                   the context types
	 */
	public GenericWebClientModuleInfo(
			ModuleElement element,
			WebClientModuleQualifiedName name,
			List<? extends WebClientSocketInfo> webClientSockets,
			List<? extends WebClientRouteInterceptorConfigurerInfo> webRouteInterceptorConfigurers,
			List<? extends WebClientStubInfo> webClientStubs,
			Set<? extends TypeMirror> contextTypes
		) {
		this.element = element;
		this.name = name;

		this.webRouteInterceptorConfigurers = webRouteInterceptorConfigurers != null ? webRouteInterceptorConfigurers : List.of();
		this.webClientSockets = webClientSockets != null ? webClientSockets : List.of();
		this.webClientStubs = webClientStubs != null ? webClientStubs : List.of();
		this.contextTypes = contextTypes != null ? contextTypes : Set.of();
	}

	@Override
	public WebClientModuleQualifiedName getQualifiedName() {
		return this.name;
	}

	@Override
	public ModuleElement getElement() {
		return this.element;
	}

	@Override
	public WebClientSocketInfo[] getWebClientSockets() {
		return this.webClientSockets.toArray(WebClientSocketInfo[]::new);
	}

	@Override
	public WebClientRouteInterceptorConfigurerInfo[] getInterceptorConfigurers() {
		return this.webRouteInterceptorConfigurers.toArray(WebClientRouteInterceptorConfigurerInfo[]::new);
	}

	@Override
	public WebClientStubInfo[] getClientStubs() {
		return this.webClientStubs.toArray(WebClientStubInfo[]::new);
	}

	@Override
	public TypeMirror[] getContextTypes() {
		return this.contextTypes.toArray(TypeMirror[]::new);
	}

	@Override
	public <R, P> R accept(WebClientModuleInfoVisitor<R, P> visitor, P p) {
		return visitor.visit(this, p);
	}

	@Override
	public boolean hasError() {
		return this.webClientStubs.stream().anyMatch(ReporterInfo::hasError);
	}

	@Override
	public boolean hasWarning() {
		return this.webClientStubs.stream().anyMatch(ReporterInfo::hasWarning);
	}

	@Override
	public void error(String message) {

	}

	@Override
	public void warning(String message) {

	}
}
