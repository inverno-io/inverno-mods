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

import java.util.List;
import java.util.Set;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.mod.web.compiler.spi.WebControllerInfo;
import io.inverno.mod.web.compiler.spi.WebInterceptorsConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerInfo;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerInfoVisitor;
import io.inverno.mod.web.compiler.spi.WebRouterConfigurerQualifiedName;
import io.inverno.mod.web.compiler.spi.WebRoutesConfigurerInfo;

/**
 * <p>
 * Generic {@link WebRouterConfigurerInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRouterConfigurerInfo implements WebRouterConfigurerInfo {

	private final ModuleElement element;
	
	private final WebRouterConfigurerQualifiedName name;
	
	private final List<? extends WebControllerInfo> webControllers;
	private final List<? extends WebProvidedRouterConfigurerInfo> webRouterConfigurers;
	private final List<? extends WebRoutesConfigurerInfo> webRoutesConfigurers;
	private final List<? extends WebInterceptorsConfigurerInfo> webInterceptorsConfigurers;
	private final Set<? extends TypeMirror> contextTypes;

	/**
	 * <p>
	 * Creates a generic web router configurer info.
	 * </p>
	 * 
	 * @param element            the element of the module targeted by the router
	 *                           configurer
	 * @param name               the router configurer qualified name
	 * @param webControllers     the controllers aggregated in the router controller
	 * @param webProvidedRouters the provided router configurers aggresgated in the
	 *                           router controller
	 * @param contextTypes       the set of context types required by the configured
	 *                           routes
	 */
	public GenericWebRouterConfigurerInfo(ModuleElement element, WebRouterConfigurerQualifiedName name, List<? extends WebControllerInfo> webControllers, List<? extends WebInterceptorsConfigurerInfo> webInterceptorsConfigurers, List<? extends WebRoutesConfigurerInfo> webRoutesConfigurers, List<? extends WebProvidedRouterConfigurerInfo> webRouterConfigurers, Set<? extends TypeMirror> contextTypes) {
		this.element = element;
		this.name = name;
		this.webControllers = webControllers != null ? webControllers : List.of();
		this.webRoutesConfigurers = webRoutesConfigurers != null ? webRoutesConfigurers : List.of();
		this.webInterceptorsConfigurers = webInterceptorsConfigurers != null ? webInterceptorsConfigurers : List.of();
		this.webRouterConfigurers = webRouterConfigurers != null ? webRouterConfigurers : List.of();
		this.contextTypes = contextTypes != null ? contextTypes : Set.of();
	}
	
	@Override
	public ModuleElement getElement() {
		return this.element;
	}
	
	@Override
	public WebRouterConfigurerQualifiedName getQualifiedName() {
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
	public WebProvidedRouterConfigurerInfo[] getRouterConfigurers() {
		return this.webRouterConfigurers.toArray(WebProvidedRouterConfigurerInfo[]::new);
	}

	@Override
	public WebRoutesConfigurerInfo[] getRoutesConfigurers() {
		return this.webRoutesConfigurers.toArray(WebRoutesConfigurerInfo[]::new);
	}

	@Override
	public WebInterceptorsConfigurerInfo[] getInterceptorsConfigurers() {
		return this.webInterceptorsConfigurers.toArray(WebInterceptorsConfigurerInfo[]::new);
	}
	
	@Override
	public TypeMirror[] getContextTypes() {
		return this.contextTypes.toArray(TypeMirror[]::new);
	}

	@Override
	public <R, P> R accept(WebRouterConfigurerInfoVisitor<R, P> visitor, P p) {
		return visitor.visit(this, p);
	}
}
