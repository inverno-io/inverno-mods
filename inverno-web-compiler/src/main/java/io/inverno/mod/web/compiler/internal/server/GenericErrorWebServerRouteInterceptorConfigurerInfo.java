/*
 * Copyright 2022 Jeremy KUHN
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

import io.inverno.core.compiler.spi.BeanInfo;
import io.inverno.core.compiler.spi.BeanQualifiedName;
import io.inverno.core.compiler.spi.support.AbstractInfo;
import io.inverno.mod.web.compiler.spi.server.ErrorWebServerRouteInterceptorConfigurerInfo;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * Generic {@link ErrorWebServerRouteInterceptorConfigurerInfo} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see AbstractInfo
 */
public class GenericErrorWebServerRouteInterceptorConfigurerInfo extends AbstractInfo<BeanQualifiedName> implements ErrorWebServerRouteInterceptorConfigurerInfo {

	private final TypeElement element;
	
	private final DeclaredType type;
	
	private final TypeMirror contextType;
	
	/**
	 * <p>
	 * Creates a generic error Web server route interceptor configurer info.
	 * </p>
	 *
	 * @param element     the type element of the interceptor configurer
	 * @param name        the configurer qualified name
	 * @param bean        the configurer bean info
	 * @param contextType the exchange context type required by the configured interceptors
	 */
	public GenericErrorWebServerRouteInterceptorConfigurerInfo(TypeElement element, BeanQualifiedName name, BeanInfo bean, TypeMirror contextType) {
		super(name, bean);
		this.element = element;
		this.type = (DeclaredType)bean.getType();
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
	public TypeMirror getContextType() {
		return this.contextType;
	}
}
