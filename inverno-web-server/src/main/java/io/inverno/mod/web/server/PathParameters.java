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
package io.inverno.mod.web.server;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Represents the path parameters extracted from the absolute path of a request following the {@link URIPattern} used to define the route to the targeted resource.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see URIPattern
 */
public interface PathParameters {

	/**
	 * <p>
	 * Returns the names of the path parameters.
	 * </p>
	 * 
	 * @return a set of parameter names
	 */
	Set<String> getNames();
	
	/**
	 * <p>
	 * Returns the path parameter with the specified name.
	 * </p>
	 *
	 * @param name a parameter name
	 *
	 * @return an optional returning the parameter or an empty optional if there's no parameter with that name
	 */
	Optional<Parameter> get(String name);
	
	/**
	 * <p>
	 * Returns all path parameters.
	 * </p>
	 * 
	 * @return the parameters
	 */
	Map<String, Parameter> getAll();
}
