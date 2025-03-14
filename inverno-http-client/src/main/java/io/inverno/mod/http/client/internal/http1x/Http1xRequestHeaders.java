/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.OutboundCookies;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.client.internal.AbstractRequestHeaders;
import io.inverno.mod.http.client.internal.EndpointRequestHeaders;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Http/1.x {@link OutboundRequestHeaders} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xRequestHeaders extends AbstractRequestHeaders<LinkedHttpHeaders> {

	private final EndpointRequestHeaders endpointHeaders;
	
	/**
	 * <p>
	 * Creates Http/1.x request headers.
	 * </p>
	 *
	 * @param endpointHeaders the endpoint request headers
	 */
	public Http1xRequestHeaders(EndpointRequestHeaders endpointHeaders) {
		this.endpointHeaders = endpointHeaders;
	}
	
	@Override
	protected LinkedHttpHeaders unwrap() {
		return this.endpointHeaders.getUnderlyingHeaders();
	}
	
	@Override
	public Http1xRequestHeaders contentType(String contentType) {
		this.endpointHeaders.contentType(contentType);
		return this;
	}

	@Override
	public String getContentType() {
		return this.endpointHeaders.getContentType();
	}

	@Override
	public Headers.ContentType getContentTypeHeader() {
		return this.endpointHeaders.getContentTypeHeader();
	}

	@Override
	public OutboundRequestHeaders accept(String accept) {
		this.endpointHeaders.accept(accept);
		return this;
	}

	@Override
	public String getAccept() {
		return this.endpointHeaders.getAccept();
	}

	@Override
	public Headers.Accept getAcceptHeader() {
		return this.endpointHeaders.getAcceptHeader();
	}

	@Override
	public Http1xRequestHeaders contentLength(long contentLength) {
		this.endpointHeaders.contentLength(contentLength);
		return this;
	}
	
	@Override
	public Long getContentLength() {
		return this.endpointHeaders.getContentLength();
	}
	
	@Override
	public Http1xRequestHeaders cookies(Consumer<OutboundCookies> cookiesConfigurer) {
		this.endpointHeaders.cookies(cookiesConfigurer);
		return this;
	}

	@Override
	public InboundCookies cookies() {
		return this.endpointHeaders.cookies();
	}

	@Override
	public Http1xRequestHeaders add(CharSequence name, CharSequence value) {
		this.endpointHeaders.add(name, value);
		return this;
	}

	@Override
	public <T> Http1xRequestHeaders addParameter(CharSequence name, T value) {
		this.endpointHeaders.addParameter(name, value);
		return this;
	}

	@Override
	public <T> OutboundRequestHeaders addParameter(CharSequence name, T value, Type type) {
		this.endpointHeaders.addParameter(name, value, type);
		return this;
	}

	@Override
	public Http1xRequestHeaders add(List<? extends Header> headers) {
		this.endpointHeaders.add(headers);
		return this;
	}

	@Override
	public Http1xRequestHeaders set(CharSequence name, CharSequence value) {
		this.endpointHeaders.set(name, value);
		return this;
	}

	@Override
	public <T> Http1xRequestHeaders setParameter(CharSequence name, T value) {
		this.endpointHeaders.setParameter(name, value);
		return this;
	}

	@Override
	public <T> OutboundRequestHeaders setParameter(CharSequence name, T value, Type type) {
		this.endpointHeaders.setParameter(name, value, type);
		return this;
	}

	@Override
	public Http1xRequestHeaders set(List<? extends Header> headers) {
		this.endpointHeaders.set(headers);
		return this;
	}

	@Override
	public Http1xRequestHeaders remove(Set<? extends CharSequence> names) {
		this.endpointHeaders.remove(names);
		return this;
	}
	
	@Override
	public boolean contains(CharSequence name) {
		return this.endpointHeaders.contains(name);
	}

	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.endpointHeaders.contains(name, value);
	}

	@Override
	public Set<String> getNames() {
		return this.endpointHeaders.getNames();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return this.endpointHeaders.get(name);
	}

	@Override
	public List<String> getAll(CharSequence name) {
		return this.endpointHeaders.getAll(name);
	}

	@Override
	public List<Map.Entry<String, String>> getAll() {
		return this.endpointHeaders.getAll();
	}

	@Override
	public Optional<Parameter> getParameter(CharSequence name) {
		return this.endpointHeaders.getParameter(name);
	}

	@Override
	public List<Parameter> getAllParameter(CharSequence name) {
		return this.endpointHeaders.getAllParameter(name);
	}

	@Override
	public List<Parameter> getAllParameter() {
		return this.endpointHeaders.getAllParameter();
	}

	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.endpointHeaders.getHeader(name);
	}

	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.endpointHeaders.getAllHeader(name);
	}

	@Override
	public List<Header> getAllHeader() {
		return this.endpointHeaders.getAllHeader();
	}
}
