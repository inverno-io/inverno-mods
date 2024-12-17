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
package io.inverno.mod.web.compiler.spi.server;

import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import java.util.Optional;

import javax.lang.model.element.ExecutableElement;

import io.inverno.mod.http.base.Method;
import javax.lang.model.type.ExecutableType;

/**
 * <p>
 * Describes a Web route.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public interface WebServerRouteInfo extends WebRouteInfo {

	@Override
	WebServerRouteQualifiedName getQualifiedName();

	/**
	 * <p>
	 * Returns the Web controller that defines the route.
	 * </p>
	 *
	 * @return an optional returning the Web controller or an empty optional if the route has been defined outside of a Web controller (eg. in a Web routes configurer)
	 */
	Optional<WebServerControllerInfo> getController();

	/**
	 * <p>
	 * Returns the executable element defining the Web route.
	 * </p>
	 *
	 * @return an optional returning the executable element or an empty optional if the route has not been defined with a method (eg. declared in a Web routes configurer)
	 */
	Optional<ExecutableElement> getElement();

	/**
	 * <p>
	 * Returns the Web server route executable type as a member of the Web server controller interface.
	 * </p>
	 *
	 * @return the Web route executable type
	 */
	Optional<ExecutableType> getType();
	
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
	 * @return true to match the trailing slash, false otherwise
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
	 * @return an array of Web parameter info
	 */
	WebParameterInfo[] getParameters();
	
	/**
	 * <p>
	 * Returns the response body specified in the route.
	 * </p>
	 * 
	 * @return a response body info
	 */
	WebServerResponseBodyInfo getResponseBody();
}
