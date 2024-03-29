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
package io.inverno.mod.web.server.internal.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.web.server.internal.MutablePathParameters;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MockPathParameters implements MutablePathParameters {

	private Map<String, Parameter> parameters;
	
	public MockPathParameters() {
		this.parameters = new HashMap<>();
	}

	@Override
	public Set<String> getNames() {
		return this.parameters.keySet();
	}

	@Override
	public Optional<Parameter> get(String name) {
		return Optional.ofNullable(this.parameters.get(name));
	}

	@Override
	public Map<String, Parameter> getAll() {
		return this.parameters;
	}

	@Override
	public void put(String name, String value) {
		this.parameters.put(name,  new MockParameter(name, value));
	}

	@Override
	public void putAll(Map<String, String> parameters) {
		for(Map.Entry<String, String> e : parameters.entrySet()) {
			this.parameters.put(e.getKey(),  new MockParameter(e.getKey(), e.getValue()));
		}
	}

	@Override
	public String remove(String name) {
		Parameter removedParameter = this.parameters.remove(name);
		return removedParameter != null ? removedParameter.getValue() : null;
	}
}
