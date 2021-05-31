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
package io.inverno.mod.http.server.internal;

import java.util.List;
import java.util.function.Consumer;

import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.SetCookie;
import io.inverno.mod.http.base.internal.header.SetCookieCodec;
import io.inverno.mod.http.server.ResponseCookies;

/**
 * <p>
 * Generic {@link ResponseCookies} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericResponseCookies implements ResponseCookies {

	private final HeaderService headerService;
	
	private final AbstractResponseHeaders responseHeaders;
	
	/**
	 * <p>
	 * Creates response cookies with the specified header service and response
	 * headers.
	 * </p>
	 * 
	 * @param headerService   the header service
	 * @param responseHeaders the response headers
	 */
	public GenericResponseCookies(HeaderService headerService, AbstractResponseHeaders responseHeaders) {
		this.headerService = headerService;
		this.responseHeaders = responseHeaders;
	}
	
	/**
	 * <p>
	 * Returns all response cookies.
	 * </p>
	 * 
	 * @return a list of set-cookie headers
	 */
	public List<Headers.SetCookie> getAll() {
		return this.responseHeaders.getAllHeader(Headers.NAME_SET_COOKIE);
	}
	
	@Override
	public ResponseCookies addCookie(Consumer<SetCookie.Configurator> configurer) {
		SetCookieCodec.SetCookie setCookie = new SetCookieCodec.SetCookie();
		configurer.accept(setCookie);
		setCookie.setHeaderValue(this.headerService.encodeValue(setCookie));
		this.responseHeaders.add(setCookie);
		return this;
	}
}
