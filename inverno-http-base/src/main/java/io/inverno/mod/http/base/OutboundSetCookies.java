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

import io.inverno.mod.http.base.header.SetCookie;
import java.util.function.Consumer;

/**
 * <p>
 * Represents mutable outbound HTTP set-cookies.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see SetCookie
 * @see OutboundResponseHeaders
 */
public interface OutboundSetCookies extends InboundSetCookies {

	/**
	 * <p>
	 * Adds a set-cookie with the specified name and value.
	 * </p>
	 * 
	 * @param name  a cookie name
	 * @param value a cookie value
	 * 
	 * @return the outbound set-cookies
	 */
	default OutboundSetCookies addCookie(String name, String value) {
		return this.addCookie(configurator -> {
			configurator.name(name);
			configurator.value(value);
		});
	}

	/**
	 * <p>
	 * Adds a set-cookie.
	 * </p>
	 * 
	 * @param configurer a set-cookie configurer
	 * 
	 * @return the outbound set-cookies
	 */
	OutboundSetCookies addCookie(Consumer<SetCookie.Configurator> configurer);
}
