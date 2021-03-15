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

import javax.lang.model.element.ModuleElement;

import io.winterframework.mod.web.compiler.spi.WebControllerInfo;
import io.winterframework.mod.web.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.winterframework.mod.web.compiler.spi.WebRouterConfigurerInfo;
import io.winterframework.mod.web.compiler.spi.WebRouterConfigurerInfoVisitor;
import io.winterframework.mod.web.compiler.spi.WebRouterConfigurerQualifiedName;

/**
 * <p>
 * Generic {@link WebRouterConfigurerInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericWebRouterConfigurerInfo implements WebRouterConfigurerInfo {

	private final ModuleElement element;
	
	private final WebRouterConfigurerQualifiedName name;
	
	private final List<? extends WebControllerInfo> webControllers; 
	private final List<? extends WebProvidedRouterConfigurerInfo> webProvidedRouters;

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
	 */
	public GenericWebRouterConfigurerInfo(ModuleElement element, WebRouterConfigurerQualifiedName name, List<? extends WebControllerInfo> webControllers, List<? extends WebProvidedRouterConfigurerInfo> webProvidedRouters) {
		this.element = element;
		this.name = name;
		this.webControllers = webControllers != null ? webControllers : List.of();
		this.webProvidedRouters = webProvidedRouters != null ? webProvidedRouters : List.of();
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
		return this.webControllers.stream().toArray(WebControllerInfo[]::new);
	}

	@Override
	public WebProvidedRouterConfigurerInfo[] getRouters() {
		return this.webProvidedRouters.stream().toArray(WebProvidedRouterConfigurerInfo[]::new);
	}

	@Override
	public <R, P> R accept(WebRouterConfigurerInfoVisitor<R, P> visitor, P p) {
		return visitor.visit(this, p);
	}
}
