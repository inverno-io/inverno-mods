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
package io.inverno.mod.web.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRequest;
import io.inverno.mod.web.WebResponse;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebExchange} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRequest
 * @see WebResponse
 */
class GenericWebExchange implements WebExchange  {

	private final GenericWebRequest request;
	
	private final GenericWebResponse response;
	
	private final Supplier<Mono<Void>> finalizerSupplier;
	
	private Map<String, Object> attributes;
	
	/**
	 * <p>
	 * Creates a generic web exchange with the specified request and response.
	 * </p>
	 * 
	 * @param request           a web request
	 * @param response          a web response
	 * @param finalizerSupplier the deferred exchange finalizer
	 */
	public GenericWebExchange(GenericWebRequest request, GenericWebResponse response, Supplier<Mono<Void>> finalizerSupplier) {
		this.request = request;
		this.response = response;
		this.finalizerSupplier = finalizerSupplier;
	}

	@Override
	public GenericWebRequest request() {
		return this.request;
	}

	@Override
	public GenericWebResponse response() {
		return this.response;
	}
	
	@Override
	public Mono<Void> finalizer() {
		return this.finalizerSupplier.get();
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
