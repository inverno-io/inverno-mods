/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.internal.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.inverno.mod.http.base.header.CookieParameter;
import io.inverno.mod.http.server.RequestCookies;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MockRequestCookies implements RequestCookies {

	private Map<String, List<CookieParameter>> cookieParameters;
	
	public MockRequestCookies(Map<String, List<String>> cookies) {
		this.cookieParameters = new HashMap<>();
		for(Map.Entry<String, List<String>> e : cookies.entrySet()) {
			this.cookieParameters.put(e.getKey(), e.getValue().stream().map(value -> new MockCookieParameter(e.getKey(), value)).collect(Collectors.toList())); 
		}
	}
	
	@Override
	public boolean contains(String name) {
		return this.cookieParameters.containsKey(name);
	}

	@Override
	public Set<String> getNames() {
		return this.cookieParameters.keySet();
	}

	@Override
	public Optional<CookieParameter> get(String name) {
		return Optional.ofNullable(this.cookieParameters.get(name)).map(l -> l.get(0));
	}

	@Override
	public List<CookieParameter> getAll(String name) {
		return this.cookieParameters.containsKey(name) ? this.cookieParameters.get(name) : List.of();
	}

	@Override
	public Map<String, List<CookieParameter>> getAll() {
		return this.cookieParameters;
	}
	
	private static class MockCookieParameter extends MockParameter implements CookieParameter {

		public MockCookieParameter(String name, String value) {
			super(name, value);
		}
	}
}
