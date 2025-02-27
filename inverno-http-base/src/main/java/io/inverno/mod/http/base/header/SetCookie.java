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

import java.time.ZonedDateTime;

/**
 * <p>
 * Represents an HTTP set-cookie as defined by <a href="https://tools.ietf.org/html/rfc6265#section-4.1">RFC 6265 Section 4.1</a>
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
	 * Returns cookie's expires attribute.
	 * </p>
	 * 
	 * @return the expires or null
	 */
	ZonedDateTime getExpires();
	
	/**
	 * <p>
	 * Returns cookie's max age attribute.
	 * </p>
	 * 
	 * @return the max age or null
	 */
	Integer getMaxAge();
	
	/**
	 * <p>
	 * Returns cookie's domain attribute.
	 * </p>
	 * 
	 * @return the domain or null
	 */
	String getDomain();
	
	/**
	 * <p>
	 * Returns cookie's path attribute.
	 * </p>
	 * 
	 * @return the path or null
	 */
	String getPath();
	
	/**
	 * <p>
	 * Returns cookie's secure flag attribute.
	 * </p>
	 * 
	 * @return the secure flag or null
	 */
	Boolean isSecure();
	
	/**
	 * <p>
	 * Returns cookie's HTTP only flag attribute.
	 * </p>
	 * 
	 * @return the HTTP only flag or null
	 */
	Boolean isHttpOnly();

	/**
	 * <p>
	 * Returns cookie's partitioned flag attribute.
	 * </p>
	 *
	 * @return the partitioned flag or null
	 */
	Boolean isPartitioned();
	
	/**
	 * <p>
	 * Returns cookie's same site attribute.
	 * </p>
	 * 
	 * @return the same site attribute or null
	 */
	Headers.SetCookie.SameSitePolicy getSameSite();

	/**
	 * <p>
	 * A configurator used to configure a set-cookie.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.0
	 */
	interface Configurator {

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
		 * Sets the cookie expires attribute.
		 * </p>
		 * 
		 * <p>
		 * Note that the resulting date time will be serialized at GMT time zone.
		 * </p>
		 * 
		 * @param expires the expires date time
		 * 
		 * @return the configurator
		 */
		Configurator expires(ZonedDateTime expires);
		
		/**
		 * <p>
		 * Sets the cookie max age attribute in seconds.
		 * </p>
		 * 
		 * @param maxAge the cookie max age
		 * 
		 * @return the configurator
		 */
		Configurator maxAge(int maxAge);

		/**
		 * <p>
		 * Sets the cookie domain attribute.
		 * </p>
		 * 
		 * @param domain the cookie domain
		 * 
		 * @return the configurator
		 */
		Configurator domain(String domain);

		/**
		 * <p>
		 * Sets the cookie path attribute.
		 * </p>
		 * 
		 * @param path the cookie path
		 * 
		 * @return the configurator
		 */
		Configurator path(String path);

		/**
		 * <p>
		 * Sets the cookie secure flag attribute.
		 * </p>
		 * 
		 * @param secure the cookie secure flag
		 * 
		 * @return the configurator
		 */
		Configurator secure(boolean secure);

		/**
		 * <p>
		 * Sets the cookie HTTP only flag attribute.
		 * </p>
		 * 
		 * @param httpOnly the cookie HTTP only flag
		 * 
		 * @return the configurator
		 */
		Configurator httpOnly(boolean httpOnly);

		/**
		 * <p>
		 * Sets the cookie partitioned flag attribute.
		 * </p>
		 *
		 * @param partitioned the cookie partitioned flag
		 *
		 * @return the configurator
		 */
		Configurator partitioned(boolean partitioned);
		
		/**
		 * <p>
		 * Sets the cookie same site flag attribute.
		 * </p>
		 * 
		 * @param sameSite a same site policy
		 * 
		 * @return the configurator
		 */
		Configurator sameSite(Headers.SetCookie.SameSitePolicy sameSite);
	}
}
