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
package io.winterframework.mod.http.server.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.header.Cookie;
import io.winterframework.mod.http.base.header.CookieParameter;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.base.internal.header.GenericCookieParameter;
import io.winterframework.mod.http.server.RequestCookies;
import io.winterframework.mod.http.server.RequestHeaders;

/**
 * <p>
 * Generic {@link RequestCookies} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericRequestCookies implements RequestCookies {

	private Map<String, List<CookieParameter>> pairs; 
	
	/**
	 * <p>
	 * Creates request cookies with the specified request headers and parameter
	 * value converter.
	 * </p>
	 * 
	 * @param requestHeaders     the request headers
	 * @param parameterConverter an string object converter
	 */
	public GenericRequestCookies(RequestHeaders requestHeaders, ObjectConverter<String> parameterConverter) {
		this.pairs = requestHeaders.<Headers.Cookie>getAllHeader(Headers.NAME_COOKIE)
			.stream()
			.flatMap(cookieHeader -> cookieHeader.getPairs().values().stream().flatMap(List::stream))
			.map(cookie -> {
				if(cookie instanceof CookieParameter) {
					return (CookieParameter)cookie;
				}
				else {
					return new GenericCookieParameter(parameterConverter, cookie.getName(), cookie.getValue());
				}
				
			})
			.collect(Collectors.groupingBy(Cookie::getName));
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
