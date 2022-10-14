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
package io.inverno.mod.base.net;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * A URI component that supports parameterization.
 * </p>
 * 
 * <p>
 * the raw value of parameterized component can contain parameters of the form
 * <code>{{@literal <name>[:<pattern>]}}</code> which can be replaced by actual
 * values when building a URI (eg.
 * {scheme}://{userinfo}@{host}:{port}/a/{segment}?parameter={parameter}#{fragment}).
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see URIComponent
 */
interface ParameterizedURIComponent extends URIComponent {
	
	/**
	 * <p>
	 * Returns the list of parameters present in the component.
	 * </p>
	 * 
	 * @return a list of parameters or an empty list if the component have no
	 *         parameter
	 */
	List<URIParameter> getParameters();
	
	/**
	 * <p>
	 * Returns the value of the component after replacing the parameters with the
	 * string representation of the specified values.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values a list of values to replace the component's parameters
	 * 
	 * @return the component value
	 * @throws IllegalArgumentException if there's not enough values to replace all
	 *                                  parameters
	 */
	String getValue(List<Object> values) throws IllegalArgumentException;
	
	/**
	 * <p>
	 * Returns the value of the component after replacing the parameters with the
	 * string representation of the specified values.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @param values a map of values to replace the component's parameters
	 * 
	 * @return the component value
	 * @throws IllegalArgumentException if there are missing values
	 */
	String getValue(Map<String, ?> values);
}
