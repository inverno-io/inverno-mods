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
package io.inverno.mod.web.compiler.spi;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.type.TypeMirror;

import io.inverno.core.compiler.spi.Info;

/**
 * <p>
 * Describes the module's web controller configurer to generate.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebServerControllerConfigurerInfo extends Info {

	@Override
	WebServerControllerConfigurerQualifiedName getQualifiedName();
	
	/**
	 * <p>
	 * Returns the module element for which a web router configurer is generated.
	 * </p>
	 * 
	 * @return a module element
	 */
	ModuleElement getElement();
	
	/**
	 * <p>
	 * Returns the web controllers defined in the module.
	 * </p>
	 * 
	 * @return an array of web controllers
	 */
	WebControllerInfo[] getControllers();
	
	/**
	 * <p>
	 * Returns the web router configurers provided in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 *
	 * @return an array of provided router configurer
	 */
	WebRouterConfigurerInfo[] getRouterConfigurers();
	
	/**
	 * <p>
	 * Returns the web routes configurers provided in the module.
	 * </p>
	 * 
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 * 
	 * @return an array of routes configurer
	 */
	WebRoutesConfigurerInfo[] getRoutesConfigurers();
	
	/**
	 * <p>
	 * Returns the web interceptors configurers provided in the module.
	 * </p>
	 * 
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 * 
	 * @return an array of interceptors configurer
	 */
	WebInterceptorsConfigurerInfo[] getInterceptorsConfigurers();
	
	/**
	 * <p>
	 * Returns the error web router configurers provided in the module.
	 * </p>
	 *
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 *
	 * @return an array of error router configurer
	 */
	ErrorWebRouterConfigurerInfo[] getErrorRouterConfigurers();
	
	/**
	 * <p>
	 * Returns the error web routes configurers provided in the module.
	 * </p>
	 * 
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 * 
	 * @return an array of error routes configurer
	 */
	ErrorWebRoutesConfigurerInfo[] getErrorRoutesConfigurers();
	
	/**
	 * <p>
	 * Returns the error web interceptors configurers provided in the module.
	 * </p>
	 * 
	 * <p>
	 * These can be defined as beans in the module or in component modules.
	 * </p>
	 * 
	 * @return an array of error interceptors configurer
	 */
	ErrorWebInterceptorsConfigurerInfo[] getErrorInterceptorsConfigurers();
	
	/**
	 * <p>
	 * Returns the list of exchange context types required by the routes defined by
	 * the Web router configurer.
	 * </p>
	 * 
	 * @return a list of context types
	 */
	TypeMirror[] getContextTypes();
	
	/**
	 * <p>
	 * Accepts the specified web router configurer info visitor.
	 * </p>
	 * 
	 * @param <R>     the type of the visitor result
	 * @param <P>     the type of the visitor parameter
	 * @param visitor the visitor to invoke
	 * @param p       the parameter
	 * 
	 * @return the visitor result
	 */
	<R, P> R accept(WebServerControllerConfigurerInfoVisitor<R, P> visitor, P p);
}
