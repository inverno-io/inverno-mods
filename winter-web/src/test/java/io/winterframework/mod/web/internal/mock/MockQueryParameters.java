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
package io.winterframework.mod.web.internal.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.server.QueryParameters;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class MockQueryParameters implements QueryParameters {

	private Map<String, List<Parameter>> queryParameters;
	
	public MockQueryParameters(Map<String, List<String>> parameters) {
		this.queryParameters = new HashMap<>();
		for(Map.Entry<String, List<String>> e : parameters.entrySet()) {
			this.queryParameters.put(e.getKey(), e.getValue().stream().map(value -> new MockParameter(e.getKey(), value)).collect(Collectors.toList())); 
		}
	}
	
	@Override
	public boolean contains(String name) {
		return this.queryParameters.containsKey(name);
	}

	@Override
	public Set<String> getNames() {
		return this.queryParameters.keySet();
	}

	@Override
	public Optional<Parameter> get(String name) {
		return Optional.ofNullable(this.queryParameters.get(name)).map(l -> l.get(0));
	}

	@Override
	public List<Parameter> getAll(String name) {
		return this.queryParameters.containsKey(name) ? this.queryParameters.get(name) : List.of();
	}

	@Override
	public Map<String, List<Parameter>> getAll() {
		return this.queryParameters;
	}

}
