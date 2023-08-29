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
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.base.InboundSetCookies;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.SetCookie;
import io.inverno.mod.http.base.header.SetCookieParameter;
import io.inverno.mod.http.base.internal.header.GenericSetCookieParameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic response cookies implementation.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class GenericResponseCookies implements InboundSetCookies {

	private final Map<String, List<SetCookieParameter>> pairs;
	
	/**
	 * <p>
	 * Creates generic response cookies.
	 * </p>
	 * 
	 * @param responseHeaders    the response headers
	 * @param parameterConverter the parameter converter
	 */
	public GenericResponseCookies(InboundResponseHeaders responseHeaders, ObjectConverter<String> parameterConverter) {
		this.pairs = responseHeaders.<Headers.SetCookie>getAllHeader(Headers.NAME_SET_COOKIE)
			.stream()
			.map(setCookie -> new GenericSetCookieParameter(setCookie, parameterConverter))
			.collect(Collectors.groupingBy(SetCookie::getName));
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
	public Optional<SetCookieParameter> get(String name) {
		return Optional.ofNullable(this.getAll(name)).map(setCookies ->  {
			if(!setCookies.isEmpty()) {
				return setCookies.get(0);
			}
			return null;
		});
	}

	@Override
	public List<SetCookieParameter> getAll(String name) {
		List<SetCookieParameter> setCookies = this.pairs.get(name);
		return setCookies != null ? setCookies : List.of();
	}

	@Override
	public Map<String, List<SetCookieParameter>> getAll() {
		return this.pairs;
	}
}
