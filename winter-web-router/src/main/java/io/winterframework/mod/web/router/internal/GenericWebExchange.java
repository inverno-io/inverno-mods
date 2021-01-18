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
package io.winterframework.mod.web.router.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.winterframework.mod.web.router.WebExchange;
import io.winterframework.mod.web.router.WebRequest;
import io.winterframework.mod.web.router.WebResponse;

/**
 * @author jkuhn
 *
 */
public class GenericWebExchange implements WebExchange  {

	private WebRequest request;
	
	private WebResponse response;
	
	private Map<String, Object> attributes;
	
	private Map<String, String> pathParameters;
	
	public GenericWebExchange(WebRequest request, WebResponse response) {
		this.request = request;
		this.response = response;
	}

	@Override
	public WebRequest request() {
		return this.request;
	}

	@Override
	public WebResponse response() {
		return this.response;
	}

	@Override
	public Map<String, String> getPathParameters() {
		return this.pathParameters != null ? this.pathParameters : Map.of();
	}

	public void setPathParameters(Map<String, String> pathParameters) {
		this.pathParameters = pathParameters != null ? Collections.unmodifiableMap(pathParameters) : Map.of();
	}
	
	@Override
	public void setAttribute(String name, Object value) {
		if(this.attributes == null) {
			this.attributes = new HashMap<>();
		}
		this.attributes.put(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		if(this.attributes != null) {
			this.attributes.remove(name);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> getAttribute(String name) {
		if(this.attributes != null) {
			return Optional.ofNullable((T)this.attributes.get(name));
		}
		return Optional.empty();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(this.attributes);
	}
}