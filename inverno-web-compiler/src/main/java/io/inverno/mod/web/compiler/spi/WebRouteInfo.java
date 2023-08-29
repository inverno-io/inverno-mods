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

import java.util.Optional;

import javax.lang.model.element.ExecutableElement;

import io.inverno.core.compiler.spi.Info;
import io.inverno.mod.http.base.Method;

/**
 * <p>
 * Describes a web route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebRouteInfo extends Info {

	@Override
	WebRouteQualifiedName getQualifiedName();
	
	/**
	 * <p>
	 * Returns the web controller that defines the route.
	 * </p>
	 *
	 * @return an optional returning the web controller or an empty optional if the route has been defined outside of a web controller (eg. in a web router configurer)
	 */
	Optional<WebControllerInfo> getController();

	/**
	 * <p>
	 * Returns the executable element defining the web route.
	 * </p>
	 *
	 * @return an optional returning the executable element or an empty optional if the route has not been defined with a method (eg. declared in a web router configurer)
	 */
	Optional<ExecutableElement> getElement();
	
	/**
	 * <p>
	 * Returns the paths specified in the route.
	 * </p>
	 * 
	 * @return an array of paths
	 */
	String[] getPaths();
	
	/**
	 * <p>
	 * Determines whether the trailing slash should be matched.
	 * </p>
	 * 
	 * @return true to match the trailing slash, false otherwiser
	 */
	boolean isMatchTrailingSlash();
	
	/**
	 * <p>
	 * Returns the methods specified in the route.
	 * </p>
	 * 
	 * @return an array of methods
	 */
	Method[] getMethods();
	
	/**
	 * <p>
	 * Returns the consumed media ranges specified in the route.
	 * </p>
	 * 
	 * @return an array of media ranges
	 */
	String[] getConsumes();
	
	/**
	 * <p>
	 * Returns the produced media types specified in the route.
	 * </p>
	 * 
	 * @return an array of media types
	 */
	String[] getProduces();
	
	/**
	 * <p>
	 * Returns the produced languages specified in the route.
	 * </p>
	 * 
	 * @return an array of language tags
	 */
	String[] getLanguages();
	
	/**
	 * <p>
	 * Returns the parameters specified in the route.
	 * </p>
	 * 
	 * @return an array of web parameter info
	 */
	WebParameterInfo[] getParameters();
	
	/**
	 * <p>
	 * Returns the response body specified in the route.
	 * </p>
	 * 
	 * @return a response body info
	 */
	WebResponseBodyInfo getResponseBody();
}
