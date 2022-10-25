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
package io.inverno.mod.http.base;

import io.inverno.mod.http.base.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Represents the query parameters sent of a client request in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Request
 */
public interface QueryParameters {

	/**
	 * <p>
	 * Determines whether a parameter with the specified name is present.
	 * </p>
	 * 
	 * @param name a query parameter name
	 * 
	 * @return true if a parameter is present, false otherwise
	 */
	boolean contains(String name);
	
	/**
	 * <p>
	 * Returns the names of the query parameters sent in the request.
	 * </p>
	 * 
	 * @return a list of header names
	 */
	Set<String> getNames();
	
	/**
	 * <p>
	 * Returns the query parameter with the specified name.
	 * </p>
	 *
	 * <p>
	 * If there are multiple parameters with the same name, this method returns the first one.
	 * </p>
	 *
	 * @param name a query parameter name
	 *
	 * @return an optional returning the parameter or an empty optional if there's no parameter with the specified name
	 */
	Optional<Parameter> get(String name);
	
	/**
	 * <p>
	 * Returns all query parameters with the specified name.
	 * </p>
	 *
	 * @param name a query parameter name
	 *
	 * @return a list of parameters or an empty list if there's no parameter with the specified name
	 */
	List<Parameter> getAll(String name);
	
	/**
	 * <p>
	 * Returns all query parameters sent in the request.
	 * </p>
	 * 
	 * @return the parameters grouped by name
	 */
	Map<String, List<Parameter>> getAll();
}
