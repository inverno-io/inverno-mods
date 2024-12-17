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

import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.header.Cookie;
import io.inverno.mod.http.base.header.CookieParameter;
import io.inverno.mod.http.base.header.Headers;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link InboundCookies} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericRequestCookies implements InboundCookies {

	private final Map<String, List<CookieParameter>> pairs; 
	
	/**
	 * <p>
	 * Creates request cookies with the specified request headers and parameter value converter.
	 * </p>
	 *
	 * @param requestHeaders     the request headers
	 */
	public GenericRequestCookies(InboundRequestHeaders requestHeaders) {
		this.pairs = requestHeaders.<Headers.Cookie>getAllHeader(Headers.NAME_COOKIE)
			.stream()
			.flatMap(cookieHeader -> cookieHeader.getPairs().values().stream().flatMap(List::stream))
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
				return cookies.getFirst();
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
