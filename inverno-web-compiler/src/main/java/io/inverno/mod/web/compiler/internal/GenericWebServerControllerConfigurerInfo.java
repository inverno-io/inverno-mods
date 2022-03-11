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

import io.inverno.mod.web.compiler.spi.ErrorWebInterceptorsConfigurerInfo;
import io.inverno.mod.web.compiler.spi.ErrorWebRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.ErrorWebRoutesConfigurerInfo;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.mod.web.compiler.spi.WebControllerInfo;
import io.inverno.mod.web.compiler.spi.WebInterceptorsConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebServerControllerConfigurerQualifiedName;
import io.inverno.mod.web.compiler.spi.WebRoutesConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebServerControllerConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebServerControllerConfigurerInfoVisitor;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerInfo;

/**
 * <p>
 * Generic {@link WebServerControllerConfigurerInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebServerControllerConfigurerInfo implements WebServerControllerConfigurerInfo {

	private final ModuleElement element;
	
	private final WebServerControllerConfigurerQualifiedName name;
	
	private final List<? extends WebControllerInfo> webControllers;
	
	private final List<? extends WebRouterConfigurerInfo> webRouterConfigurers;
	private final List<? extends WebRoutesConfigurerInfo> webRoutesConfigurers;
	private final List<? extends WebInterceptorsConfigurerInfo> webInterceptorsConfigurers;
	
	private final List<? extends ErrorWebRouterConfigurerInfo> errorWebRouterConfigurers;
	private final List<? extends ErrorWebRoutesConfigurerInfo> errorWebRoutesConfigurers;
	private final List<? extends ErrorWebInterceptorsConfigurerInfo> errorWebInterceptorsConfigurers;
	
	private final Set<? extends TypeMirror> contextTypes;

	/**
	 * <p>
	 * Creates a generic web server controller configurer info.
	 * </p>
	 *
	 * @param element                         the element of the module targeted by the controller configurer
	 * @param name                            the server controller configurer qualified name
	 * @param webControllers                  the controllers aggregated in the controller configurer
	 * @param webInterceptorsConfigurers      the interceptors configurers aggregated in the controller configurer
	 * @param webRoutesConfigurers            the routes configurers aggregated in the controller configurer
	 * @param webRouterConfigurers            the router configurers aggregated in the controller configurer
	 * @param errorWebInterceptorsConfigurers the error interceptors configurers aggregated in the controller configurer
	 * @param errorWebRoutesConfigurers       the error routes configurers aggregated in the controller configurer
	 * @param errorWebRouterConfigurers       the error router configurers aggregated in the controller configurer
	 * @param contextTypes                    the set of context types required by the configured routes and interceptors
	 */
	public GenericWebServerControllerConfigurerInfo(
			ModuleElement element, 
			WebServerControllerConfigurerQualifiedName name, 
			List<? extends WebControllerInfo> webControllers,
			List<? extends WebInterceptorsConfigurerInfo> webInterceptorsConfigurers, 
			List<? extends WebRoutesConfigurerInfo> webRoutesConfigurers, 
			List<? extends WebRouterConfigurerInfo> webRouterConfigurers, 
			List<? extends ErrorWebInterceptorsConfigurerInfo> errorWebInterceptorsConfigurers, 
			List<? extends ErrorWebRoutesConfigurerInfo> errorWebRoutesConfigurers, 
			List<? extends ErrorWebRouterConfigurerInfo> errorWebRouterConfigurers,
			Set<? extends TypeMirror> contextTypes
		) {
		this.element = element;
		this.name = name;
		this.webControllers = webControllers != null ? webControllers : List.of();
		
		this.webRoutesConfigurers = webRoutesConfigurers != null ? webRoutesConfigurers : List.of();
		this.webInterceptorsConfigurers = webInterceptorsConfigurers != null ? webInterceptorsConfigurers : List.of();
		this.webRouterConfigurers = webRouterConfigurers != null ? webRouterConfigurers : List.of();
		
		this.errorWebRoutesConfigurers = errorWebRoutesConfigurers != null ? errorWebRoutesConfigurers : List.of();
		this.errorWebInterceptorsConfigurers = errorWebInterceptorsConfigurers != null ? errorWebInterceptorsConfigurers : List.of();
		this.errorWebRouterConfigurers = errorWebRouterConfigurers != null ? errorWebRouterConfigurers : List.of();
		
		this.contextTypes = contextTypes != null ? contextTypes : Set.of();
	}
	
	@Override
	public ModuleElement getElement() {
		return this.element;
	}
	
	@Override
	public WebServerControllerConfigurerQualifiedName getQualifiedName() {
		return this.name;
	}
	
	@Override
	public boolean hasError() {
		return this.webControllers.stream().anyMatch(controller -> controller.hasError());
	}
	
	@Override
	public boolean hasWarning() {
		return this.webControllers.stream().anyMatch(controller -> controller.hasWarning());
	}
	
	@Override
	public void error(String message) {
		
	}
	
	@Override
	public void warning(String message) {
		
	}

	@Override
	public WebControllerInfo[] getControllers() {
		return this.webControllers.toArray(WebControllerInfo[]::new);
	}

	@Override
	public WebInterceptorsConfigurerInfo[] getInterceptorsConfigurers() {
		return this.webInterceptorsConfigurers.toArray(WebInterceptorsConfigurerInfo[]::new);
	}
	
	@Override
	public WebRoutesConfigurerInfo[] getRoutesConfigurers() {
		return this.webRoutesConfigurers.toArray(WebRoutesConfigurerInfo[]::new);
	}

	@Override
	public WebRouterConfigurerInfo[] getRouterConfigurers() {
		return this.webRouterConfigurers.toArray(WebRouterConfigurerInfo[]::new);
	}
	
	@Override
	public ErrorWebInterceptorsConfigurerInfo[] getErrorInterceptorsConfigurers() {
		return this.errorWebInterceptorsConfigurers.toArray(ErrorWebInterceptorsConfigurerInfo[]::new);
	}
	
	@Override
	public ErrorWebRoutesConfigurerInfo[] getErrorRoutesConfigurers() {
		return this.errorWebRoutesConfigurers.toArray(ErrorWebRoutesConfigurerInfo[]::new);
	}

	@Override
	public ErrorWebRouterConfigurerInfo[] getErrorRouterConfigurers() {
		return this.errorWebRouterConfigurers.toArray(ErrorWebRouterConfigurerInfo[]::new);
	}

	@Override
	public TypeMirror[] getContextTypes() {
		return this.contextTypes.toArray(TypeMirror[]::new);
	}

	@Override
	public <R, P> R accept(WebServerControllerConfigurerInfoVisitor<R, P> visitor, P p) {
		return visitor.visit(this, p);
	}
}
