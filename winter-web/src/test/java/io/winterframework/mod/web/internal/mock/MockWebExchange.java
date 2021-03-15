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
import java.util.Map;
import java.util.Optional;

import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.web.WebExchange;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class MockWebExchange implements WebExchange {

	private final MockWebRequest mockRequest;
	private final MockWebResponse mockResponse;
	
	private final Map<String, Object> attributes;
	
	public MockWebExchange(MockWebRequest mockRequest, MockWebResponse mockResponse) {
		this.mockRequest = mockRequest;
		this.mockResponse = mockResponse;
		this.attributes = new HashMap<>();
	}
	
	public static MockExchangeBuilder from(String path) {
		return new MockExchangeBuilder().path(path);
	}
	
	public static MockExchangeBuilder from(String path, Method method) {
		return new MockExchangeBuilder().method(method).path(path);
	}
	
	@Override
	public MockWebRequest request() {
		return this.mockRequest;
	}

	@Override
	public MockWebResponse response() {
		return this.mockResponse;
	}

	@Override
	public void setAttribute(String name, Object value) {
		this.attributes.put(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> getAttribute(String name) {
		return Optional.ofNullable((T)this.attributes.get(name));
	}

	@Override
	public Map<String, Object> getAttributes() {
		return this.attributes;
	}
}
