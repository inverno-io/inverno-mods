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
package io.winterframework.mod.web.internal.server;

import java.util.List;
import java.util.function.Consumer;

import io.winterframework.mod.web.SetCookie;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.internal.header.SetCookieCodec;
import io.winterframework.mod.web.server.ResponseCookies;

/**
 * @author jkuhn
 *
 */
public class GenericResponseCookies implements ResponseCookies {

	private final HeaderService headerService;
	
	private final AbstractResponseHeaders responseHeaders;
	
	public GenericResponseCookies(HeaderService headerService, AbstractResponseHeaders responseHeaders) {
		this.headerService = headerService;
		this.responseHeaders = responseHeaders;
	}
	
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
