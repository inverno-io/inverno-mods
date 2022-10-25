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

import io.inverno.mod.http.base.header.CookieParameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface InboundCookies {

		/**
	 * <p>
	 * Determines whether a cookie with the specified name is present.
	 * </p>
	 * 
	 * @param name a cookie name
	 * 
	 * @return true if a cookie is present, false otherwise
	 */
	boolean contains(String name);
	
	/**
	 * <p>
	 * Returns the names of the cookies sent in the request.
	 * </p>
	 * 
	 * @return a list of cookie names
	 */
	Set<String> getNames();
	
	/**
	 * <p>
	 * Returns the cookie with the specified name.
	 * </p>
	 *
	 * <p>
	 * If there are multiple cookies with the same name, this method returns the first one.
	 * </p>
	 *
	 * @param name a cookie name
	 *
	 * @return an optional returning the cookie parameter or an empty optional if there's no cookie with the specified name
	 */
	Optional<CookieParameter> get(String name);
	
	/**
	 * <p>
	 * Returns all cookies with the specified name.
	 * </p>
	 *
	 * @param name a cookie name
	 *
	 * @return a list of cookie parameters or an empty list if there's no cookie with the specified name
	 */
	List<CookieParameter> getAll(String name);
	
	/**
	 * <p>
	 * Returns all cookies sent in the request.
	 * </p>
	 * 
	 * @return the cookies grouped by name
	 */
	Map<String, List<CookieParameter>> getAll();
}
