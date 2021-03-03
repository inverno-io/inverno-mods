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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.internal.GenericParameter;
import io.winterframework.mod.http.server.QueryParameters;

/**
 * @author jkuhn
 *
 */
public class GenericQueryParameters implements QueryParameters {

	private final Map<String, List<String>> queryParameters;
	private final ObjectConverter<String> parameterConverter;
	
	public GenericQueryParameters(Map<String, List<String>> queryParameters, ObjectConverter<String> parameterConverter) {
		this.queryParameters = queryParameters;
		this.parameterConverter = parameterConverter;
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
		return Optional.ofNullable(this.queryParameters.get(name)).filter(p -> !p.isEmpty()).map(p -> new GenericParameter(this.parameterConverter, name, p.get(0)));
	}
	
	@Override
	public List<Parameter> getAll(String name) {
		List<String> parameters = queryParameters.get(name);
		if(parameters != null) {
			return parameters.stream().map(value -> new GenericParameter(this.parameterConverter, name, value)).collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	@Override
	public Map<String, List<Parameter>> getAll() {
		Map<String, List<Parameter>> result = new HashMap<>();
		for(Entry<String, List<String>> e : queryParameters.entrySet()) {
			result.put(e.getKey(), e.getValue().stream().map(value -> new GenericParameter(this.parameterConverter, e.getKey(), value)).collect(Collectors.toList()));
		}
		return Collections.unmodifiableMap(result);
	}
}
