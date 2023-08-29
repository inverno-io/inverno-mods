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
import io.inverno.mod.http.base.OutboundCookies;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.header.CookieParameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.CookieCodec;
import io.inverno.mod.http.base.internal.header.GenericCookieParameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Generic request cookies implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class GenericRequestCookies implements OutboundCookies {

	private final OutboundRequestHeaders requestHeaders;
	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final Map<String, List<CookieParameter>> pairs;
	
	/**
	 * <p>
	 * Creates generic request cookies.
	 * </p>
	 * 
	 * @param requestHeaders the request headers
	 * @param headerService the header service
	 * @param parameterConverter the parameter converter
	 */
	public GenericRequestCookies(OutboundRequestHeaders requestHeaders, HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.requestHeaders = requestHeaders;
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		
		this.pairs = new HashMap<>();
	}
	
	/**
	 * <p>
	 * Loads the cookie pairs from {@code cookie} request header.
	 * </p>
	 * 
	 * <p>
	 * This shall be invoked before read or write access to cookies.
	 * </p>
	 * 
	 * @see GenericRequestHeaders#cookies() 
	 * @see GenericRequestHeaders#cookies(java.util.function.Consumer) 
	 */
	public void load() {
		this.pairs.clear();
		this.requestHeaders.<Headers.Cookie>getAllHeader(Headers.NAME_COOKIE)
			.stream()
			.flatMap(cookieHeader -> cookieHeader.getPairs().values().stream().flatMap(List::stream))
			.forEach(cookie -> this.pairs.computeIfAbsent(cookie.getName(), ign -> new ArrayList<>()).add(cookie));
	}
	
	/**
	 * <p>
	 * Commits cookie pairs into {@code cookie} request header.
	 * </p>
	 * 
	 * <p>
	 * This shall be invoked after write access to cookies.
	 * </p>
	 * 
	 * @see GenericRequestHeaders#cookies(java.util.function.Consumer) 
	 */
	public void commit() {
		this.requestHeaders.set(Headers.NAME_COOKIE, this.headerService.encodeValue(new CookieCodec.Cookie(this.pairs)));
	}
	
	@Override
	public <T> GenericRequestCookies addCookie(String name, T value) {
		this.pairs.computeIfAbsent(name, ign -> new ArrayList<>()).add(new GenericCookieParameter(this.parameterConverter, name, this.parameterConverter.encode(value)));
		return this;
	}

	@Override
	public boolean contains(String name) {
		return this.pairs.containsKey(name);
	}
	
	@Override
	public Set<String> getNames() {
		return this.pairs.keySet();
	}
	
	@Override
	public Optional<CookieParameter> get(String name) {
		return Optional.ofNullable(this.getAll(name)).map(cookies ->  {
			if(!cookies.isEmpty()) {
				return cookies.get(0);
			}
			return null;
		});
	}
	
	@Override
	public List<CookieParameter> getAll(String name) {
		List<CookieParameter> cookiePairs = this.pairs.get(name);
		return cookiePairs != null ? cookiePairs : List.of();
	}

	@Override
	public Map<String, List<CookieParameter>> getAll() {
		return this.pairs;
	}
}
