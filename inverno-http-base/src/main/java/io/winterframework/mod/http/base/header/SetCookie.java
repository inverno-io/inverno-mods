/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.base.header;

/**
 * <p>
 * Represents a HTTP set-cookie.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @see Cookie
 */
public interface SetCookie extends Cookie {
	
	/**
	 * <p>
	 * Returns cookie's max age.
	 * </p>
	 * 
	 * @return the max age or null
	 */
	Integer getMaxAge();
	
	/**
	 * <p>
	 * Returns cookie's dpmain.
	 * </p>
	 * 
	 * @return the domain or null
	 */
	String getDomain();
	
	/**
	 * <p>
	 * Returns cookie's path.
	 * </p>
	 * 
	 * @return the path or null
	 */
	String getPath();
	
	/**
	 * <p>
	 * Returns cookie's secure flag.
	 * </p>
	 * 
	 * @return the secure flag or null
	 */
	Boolean isSecure();
	
	/**
	 * <p>
	 * Returns cookie's http only flag.
	 * </p>
	 * 
	 * @return the http only flag or null
	 */
	Boolean isHttpOnly();

	/**
	 * <p>
	 * A configurator used to configure a set-cookie.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	public static interface Configurator {

		/**
		 * <p>
		 * Sets the cookie name.
		 * </p>
		 * 
		 * @param name a cookie name
		 * 
		 * @return the configurator
		 */
		Configurator name(String name);

		/**
		 * <p>
		 * Sets the cookie value.
		 * </p>
		 * 
		 * @param value a cookie value
		 * 
		 * @return the configurator
		 */
		Configurator value(String value);

		/**
		 * <p>
		 * sets the cookie max age in seconds.
		 * </p>
		 * 
		 * @param maxAge the cookie max age
		 * 
		 * @return the configurator
		 */
		Configurator maxAge(int maxAge);

		/**
		 * <p>
		 * Sets the cookie domain.
		 * </p>
		 * 
		 * @param domain the cookie domain
		 * 
		 * @return the configurator
		 */
		Configurator domain(String domain);

		/**
		 * <p>
		 * Sets the cookie path.
		 * </p>
		 * 
		 * @param path the cookie path
		 * 
		 * @return the configurator
		 */
		Configurator path(String path);

		/**
		 * <p>
		 * Sets the cookie secure flag.
		 * </p>
		 * 
		 * @param secure the cookie secure flag
		 * 
		 * @return the configurator
		 */
		Configurator secure(boolean secure);

		/**
		 * <p>
		 * Sets the cookie http only flag.
		 * </p>
		 * 
		 * @param httpOnly the cookie http only flag
		 * @return the configurator
		 */
		Configurator httpOnly(boolean httpOnly);
	}
}
