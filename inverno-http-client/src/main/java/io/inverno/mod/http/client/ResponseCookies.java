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

package io.inverno.mod.http.client;

import io.inverno.mod.http.base.header.SetCookieParameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface ResponseCookies {

	/**
	 * <p>
	 * Determines whether a set cookie with the specified name is present.
	 * </p>
	 * 
	 * @param name a set cookie name
	 * 
	 * @return true if a set cookie is present, false otherwise
	 */
	boolean contains(String name);
	
	/**
	 * <p>
	 * Returns the names of the set cookies sent in the request.
	 * </p>
	 * 
	 * @return a list of set cookie names
	 */
	Set<String> getNames();
	
	/**
	 * <p>
	 * Returns the set cookie with the specified name.
	 * </p>
	 *
	 * <p>
	 * If there are multiple set cookies with the same name, this method returns the first one.
	 * </p>
	 *
	 * @param name a cookie name
	 *
	 * @return an optional returning the set cookie parameter or an empty optional if there's no set cookie with the specified name
	 */
	Optional<SetCookieParameter> get(String name);
	
	/**
	 * <p>
	 * Returns all set cookies with the specified name.
	 * </p>
	 *
	 * @param name a cookie name
	 *
	 * @return a list of set cookie parameters or an empty list if there's no set cookie with the specified name
	 */
	List<SetCookieParameter> getAll(String name);
	
	/**
	 * <p>
	 * Returns all set cookies sent in the request.
	 * </p>
	 * 
	 * @return the set cookies grouped by name
	 */
	Map<String, List<SetCookieParameter>> getAll();
}
