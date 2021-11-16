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

import io.inverno.core.compiler.spi.BeanInfo;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.web.compiler.spi.WebConfigurerQualifiedName;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import io.inverno.mod.web.compiler.spi.WebRoutesConfigurerInfo;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link WebRoutesConfigurerInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 * 
 * @see AbstractInfo
 */
public class GenericWebRoutesConfigurerInfo extends AbstractInfo<WebConfigurerQualifiedName> implements WebRoutesConfigurerInfo {
	
	private final TypeElement element;
	
	private final DeclaredType type;
	
	private final List<? extends WebRouteInfo> routes;
	
	private final TypeMirror contextType;
	
	/**
	 * <p>
	 * Creates a generic web routes configurer info.
	 * </p>
	 *
	 * @param element     the type element of the router configurer
	 * @param name        the configurer qualified name
	 * @param bean        the configurer bean info
	 * @param routes      the routes defined in the router configuer
	 * @param contextType the exchange context type required by the configured routes
	 */
	public GenericWebRoutesConfigurerInfo(TypeElement element, WebConfigurerQualifiedName name, BeanInfo bean, List<? extends WebRouteInfo> routes, TypeMirror contextType) {
		super(name, bean);
		this.element = element;
		this.type = (DeclaredType)bean.getType();
		this.routes = routes != null ? routes : List.of();
		this.contextType = contextType;
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
		return this.routes.toArray(WebRouteInfo[]::new);
	}
	
	@Override
	public TypeMirror getContextType() {
		return this.contextType;
	}
}
