/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.compiler.spi.client;

import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.compiler.spi.WebParameterInfo;
import io.inverno.mod.web.compiler.spi.WebRouteInfo;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;

/**
 * <p>
 * Describes a Web client stub route.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public interface WebClientRouteInfo extends WebRouteInfo {

	@Override
	WebClientRouteQualifiedName getQualifiedName();

	/**
	 * <p>
	 * Returns the Web client stub that defines the route.
	 * </p>
	 *
	 * @return the Web client stub
	 */
	WebClientStubInfo getClientStub();

	/**
	 * <p>
	 * Returns the executable element defining the Web route.
	 * </p>
	 *
	 * @return the executable element
	 */
	ExecutableElement getElement();

	/**
	 * <p>
	 * Returns the Web client route executable type as a member of the Web client stub interface.
	 * </p>
	 *
	 * @return the Web route executable type
	 */
	ExecutableType getType();

	/**
	 * <p>
	 * Returns the path specified in the route.
	 * </p>
	 *
	 * @return the path
	 */
	Optional<String> getPath();

	/**
	 * <p>
	 * Returns the method specified in the route.
	 * </p>
	 *
	 * @return a method
	 */
	Method getMethod();

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
	 * Returns the produced media type specified in the route.
	 * </p>
	 *
	 * @return a media type
	 */
	Optional<String> getProduce();

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
	 * Returns the returned value specified in the route.
	 * </p>
	 *
	 * <p>
	 * A Web Client route can either return a response body or the Web client exchange.
	 * </p>
	 *
	 * @return a return info
	 */
	WebClientRouteReturnInfo getReturn();
}
