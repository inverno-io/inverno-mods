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
package io.winterframework.mod.web.compiler.spi;

import javax.lang.model.element.ModuleElement;

import io.winterframework.core.compiler.spi.Info;

/**
 * <p>
 * Describes the module's web router configurer to generate.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebRouterConfigurerInfo extends Info {

	@Override
	WebRouterConfigurerQualifiedName getQualifiedName();
	
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
	 * Returns the web router configurer provided in the module.
	 * </p>
	 * 
	 * <p>
	 * These can be defined as beans defined in the module or as beans exposed in
	 * component modules.
	 * </p>
	 * 
	 * @return an array of provided router configurer
	 */
	WebProvidedRouterConfigurerInfo[] getRouters();
	
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
	<R, P> R accept(WebRouterConfigurerInfoVisitor<R, P> visitor, P p);
}
