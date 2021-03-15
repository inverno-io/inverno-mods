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

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import io.winterframework.core.compiler.spi.BeanInfo;
import io.winterframework.core.compiler.spi.support.AbstractInfo;
import io.winterframework.mod.web.compiler.spi.WebProvidedRouterConfigurerInfo;
import io.winterframework.mod.web.compiler.spi.WebRouteInfo;
import io.winterframework.mod.web.compiler.spi.WebRouterConfigurerQualifiedName;

/**
 * <p>
 * Generic {@link WebProvidedRouterConfigurerInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see AbstractInfo
 */
class GenericWebProvidedRouterConfigurerInfo extends AbstractInfo<WebRouterConfigurerQualifiedName> implements WebProvidedRouterConfigurerInfo {

	private TypeElement element;
	
	private final DeclaredType type;
	
	private final List<? extends WebRouteInfo> routes;
	
	/**
	 * <p>
	 * Creates a generic web provided router configurer info.
	 * </p>
	 * 
	 * @param element the type element of the router configurer
	 * @param name    the router qualified name
	 * @param bean    the router configurer bean info
	 * @param routes  the routes defined in the router configuer
	 */
	public GenericWebProvidedRouterConfigurerInfo(TypeElement element, WebRouterConfigurerQualifiedName name, BeanInfo bean, List<? extends WebRouteInfo> routes) {
		super(name, bean);
		this.element = element;
		this.type = (DeclaredType)bean.getType();
		this.routes = routes != null ? routes : List.of();
	}

	@Override
	public TypeElement getElement() {
		return this.element;
	}
	
	@Override
	public DeclaredType getType() {
		return this.type;
	}
	
	@Override
	public WebRouteInfo[] getRoutes() {
		return this.routes.stream().toArray(WebRouteInfo[]::new);
	}
}
