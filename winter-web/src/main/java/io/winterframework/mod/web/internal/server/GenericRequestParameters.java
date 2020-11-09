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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.RequestParameters;
import io.winterframework.mod.web.internal.GenericParameter;

/**
 * @author jkuhn
 *
 */
public class GenericRequestParameters implements RequestParameters {

	private String path;
	
	private QueryStringDecoder queryStringDecoder;
	
	public GenericRequestParameters(String path) {
		this.path = path;
	}

	private QueryStringDecoder getQueryStringDecoder() {
		if(this.queryStringDecoder == null) {
			this.queryStringDecoder = new QueryStringDecoder(path);
		}
		return this.queryStringDecoder;
	}
	
	@Override
	public Set<String> getNames() {
		return this.getQueryStringDecoder().parameters().keySet();
	}
	
	@Override
	public Optional<Parameter> get(String name) {
		Optional.ofNullable(this.getQueryStringDecoder().parameters().get(name)).map(p -> {
			if(!p.isEmpty()) {
				return new GenericParameter(name, p.get(0));
			}
			return p;
		});
		return null;
	}
	
	@Override
	public List<Parameter> getAll(String name) {
		return this.getQueryStringDecoder().parameters().get(name).stream().map(value -> new GenericParameter(name, value)).collect(Collectors.toList());
	}

	@Override
	public Map<String, List<Parameter>> getAll() {
		Map<String, List<Parameter>> result = new HashMap<>();
		for(Entry<String, List<String>> e : this.getQueryStringDecoder().parameters().entrySet()) {
			result.put(e.getKey(), e.getValue().stream().map(value -> new GenericParameter(e.getKey(), value)).collect(Collectors.toList()));
		}
		return Collections.unmodifiableMap(result);
	}
}
