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

package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.CookieCodec;
import io.inverno.mod.http.client.RequestCookies;
import io.inverno.mod.http.client.RequestHeaders;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericRequestCookies implements RequestCookies {

	private final RequestHeaders requestHeaders;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final CookieCodec.Cookie.Builder cookieBuilder;
	
	public GenericRequestCookies(HeaderService headerService, RequestHeaders requestHeaders, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.requestHeaders = requestHeaders;
		this.parameterConverter = parameterConverter;
		this.cookieBuilder = new CookieCodec.Cookie.Builder(parameterConverter);
	}
	
	public void commit() {
		this.requestHeaders.set(Headers.NAME_COOKIE, this.headerService.encodeValue(this.cookieBuilder.build()));
	}
	
	@Override
	public <T> RequestCookies addCookie(String name, T value) {
		this.cookieBuilder.parameter(name, this.parameterConverter.encode(value));
		return this;
	}
}
