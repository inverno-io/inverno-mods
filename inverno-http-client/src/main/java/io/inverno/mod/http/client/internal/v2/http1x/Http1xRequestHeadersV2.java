/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.v2.http1x;

import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.OutboundCookies;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.client.internal.EndpointRequestHeaders;
import io.inverno.mod.http.client.internal.HttpConnectionRequestHeaders;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * 
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 */
class Http1xRequestHeadersV2 implements HttpConnectionRequestHeaders {

	private final EndpointRequestHeaders endpointHeaders;
	
	private boolean written;

	public Http1xRequestHeadersV2(EndpointRequestHeaders endpointHeaders) {
		this.endpointHeaders = endpointHeaders;
	}
	
	/**
	 * <p>
	 * Returns the headers to send as part of the Http response.
	 * </p>
	 * 
	 * @return the wrapped headers
	 */
	LinkedHttpHeaders unwrap() {
		return this.endpointHeaders.getUnderlyingHeaders();
	}
	
	@Override
	public void setWritten(boolean written) {
		this.written = written;
	}
	
	@Override
	public final boolean isWritten() {
		return this.written;
	}
	
	@Override
	public Http1xRequestHeadersV2 contentType(String contentType) {
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
	public Http1xRequestHeadersV2 contentLength(long contentLength) {
		this.endpointHeaders.contentLength(contentLength);
		return this;
	}
	
	@Override
	public Long getContentLength() {
		return this.endpointHeaders.getContentLength();
	}
	
	@Override
	public Http1xRequestHeadersV2 cookies(Consumer<OutboundCookies> cookiesConfigurer) {
		this.endpointHeaders.cookies(cookiesConfigurer);
		return this;
	}

	@Override
	public InboundCookies cookies() {
		return this.endpointHeaders.cookies();
	}

	@Override
	public Http1xRequestHeadersV2 add(CharSequence name, CharSequence value) {
		this.endpointHeaders.add(name, value);
		return this;
	}

	@Override
	public Http1xRequestHeadersV2 add(Header... headers) {
		this.endpointHeaders.add(headers);
		return this;
	}

	@Override
	public Http1xRequestHeadersV2 set(CharSequence name, CharSequence value) {
		this.endpointHeaders.set(name, value);
		return this;
	}

	@Override
	public Http1xRequestHeadersV2 set(Header... headers) {
		this.endpointHeaders.set(headers);
		return this;
	}

	@Override
	public Http1xRequestHeadersV2 remove(CharSequence... names) {
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
	
	// TODO remove
	@Override
	public CharSequence getCharSequence(CharSequence name) {
		return this.endpointHeaders.getCharSequence(name);
	}

	// TODO remove
	@Override
	public List<CharSequence> getAllCharSequence(CharSequence name) {
		return this.endpointHeaders.getAllCharSequence(name);
	}

	// TODO remove
	@Override
	public List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence() {
		return this.endpointHeaders.getAllCharSequence();
	}
}
