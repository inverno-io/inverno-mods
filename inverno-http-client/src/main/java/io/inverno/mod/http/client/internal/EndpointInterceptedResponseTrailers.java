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
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericParameter;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * An {@link OutboundHeaders} implementation used to specify response trailers in an {@link io.inverno.mod.http.client.ExchangeInterceptor}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointInterceptedResponseTrailers implements OutboundHeaders<EndpointInterceptedResponseTrailers> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	
	private final LinkedHttpHeaders underlyingHeaders;

	/**
	 * <p>
	 * Creates an intercepted response trailers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public EndpointInterceptedResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.underlyingHeaders = new LinkedHttpHeaders();
	}
	
	@Override
	public boolean isWritten() {
		return false;
	}
	
	@Override
	public EndpointInterceptedResponseTrailers add(CharSequence name, CharSequence value) {
		this.underlyingHeaders.addCharSequence(name, value);
		return this;
	}

	@Override
	public <T> EndpointInterceptedResponseTrailers addParameter(CharSequence name, T value) {
		return this.add(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> EndpointInterceptedResponseTrailers addParameter(CharSequence name, T value, Type type) {
		return this.add(name, this.parameterConverter.encode(value, type));
	}

	@Override
	public EndpointInterceptedResponseTrailers add(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.addCharSequence(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}
	
	@Override
	public EndpointInterceptedResponseTrailers set(CharSequence name, CharSequence value) {
		this.underlyingHeaders.setCharSequence(name, value);
		return this;
	}

	@Override
	public <T> EndpointInterceptedResponseTrailers setParameter(CharSequence name, T value) {
		return this.set(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> EndpointInterceptedResponseTrailers setParameter(CharSequence name, T value, Type type) {
		return this.set(name, this.parameterConverter.encode(value, type));
	}

	@Override
	public EndpointInterceptedResponseTrailers set(Header... headers) {
		for(Header header : headers) {
			this.underlyingHeaders.setCharSequence(header.getHeaderName(), header.getHeaderValue());
		}
		return this;
	}
	
	@Override
	public EndpointInterceptedResponseTrailers remove(CharSequence... names) {
		for(CharSequence name : names) {
			this.underlyingHeaders.remove(name);
		}
		return this;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.underlyingHeaders.contains(name);
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.underlyingHeaders.contains(name, value, true);
	}
	
	@Override
	public Set<String> getNames() {
		return this.underlyingHeaders.names();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.underlyingHeaders.get(name));
	}
	
	@Override
	public List<String> getAll(CharSequence name) {
		return this.underlyingHeaders.getAll(name);
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		return this.underlyingHeaders.entries();
	}
	
	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.decode(name.toString(), value));
	}
	
	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
	}
	
	@Override
	public List<Header> getAllHeader() {
		return this.getAll().stream().map(e -> this.headerService.<Header>decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}
	
	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.get(name).map(value -> new GenericParameter(name.toString(), value, this.parameterConverter));
	}
	
	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.underlyingHeaders.getAll(name).stream().map(value -> new GenericParameter(name.toString(), value, this.parameterConverter)).collect(Collectors.toList());
	}
	
	@Override
	public List<Parameter> getAllParameter() {
		return this.underlyingHeaders.entries().stream().map(e -> new GenericParameter(e.getKey(), e.getValue(), this.parameterConverter)).collect(Collectors.toList());
	}
}
