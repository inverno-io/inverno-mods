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
package io.winterframework.mod.http.server;

import java.util.function.Consumer;

import io.winterframework.mod.http.base.header.SetCookie;

/**
 * <p>
 * Represents the cookies of a server response in a server exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Response
 */
public interface ResponseCookies {
	
	/**
	 * <p>
	 * Adds a cookie with the specified name and value.
	 * </p>
	 * 
	 * @param name  a cookie name
	 * @param value a cookie value
	 * 
	 * @return the response cookies
	 */
	default ResponseCookies addCookie(String name, String value) {
		return this.addCookie(configurator -> {
			configurator.name(name);
			configurator.value(value);
		});
	}

	/**
	 * <p>
	 * Adds a cookie created with the specified configurer.
	 * </p>
	 * 
	 * @param configurer a cookie configurer
	 * 
	 * @return the response cookies
	 */
	ResponseCookies addCookie(Consumer<SetCookie.Configurator> configurer);
}
