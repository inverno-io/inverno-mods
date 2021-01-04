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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Cookie;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.RequestCookies;
import io.winterframework.mod.web.RequestHeaders;

/**
 * @author jkuhn
 *
 */
public class GenericRequestCookies implements RequestCookies {

	private Map<String, List<Cookie>> pairs; 
	
	public GenericRequestCookies(RequestHeaders requestHeaders) {
		this.pairs = requestHeaders.<Headers.Cookie>getAllHeader(Headers.NAME_COOKIE)
			.stream()
			.flatMap(cookieHeader -> cookieHeader.getPairs().values().stream().flatMap(List::stream))
			.collect(Collectors.groupingBy(Cookie::getName));
	}

	@Override
	public Set<String> getNames() {
		return this.pairs.keySet();
	}
	
	@Override
	public Optional<Cookie> get(String name) {
		return Optional.ofNullable(this.getAll(name)).map(cookies ->  {
			if(!cookies.isEmpty()) {
				return cookies.get(0);
			}
			return null;
		});
	}
	
	@Override
	public List<Cookie> getAll(String name) {
		return this.pairs.get(name);
	}

	@Override
	public Map<String, List<Cookie>> getAll() {
		return this.pairs;
	}
}
