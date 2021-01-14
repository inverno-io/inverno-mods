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
package io.winterframework.mod.web.internal.server.http1x;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslHandler;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.header.Header;
import io.winterframework.mod.web.header.HeaderService;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.internal.netty.LinkedHttpHeaders;
import io.winterframework.mod.web.server.RequestHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xRequestHeaders implements RequestHeaders {

	private final ChannelHandlerContext context;
	private final HeaderService headerService;
	private final HttpRequest httpRequest;
	private final LinkedHttpHeaders internalHeaders;
	
	private Method method;
	private String scheme;
	
	public Http1xRequestHeaders(ChannelHandlerContext context, HttpRequest httpRequest, HeaderService headerService) {
		this.context = context;
		this.httpRequest = httpRequest;
		this.internalHeaders = (LinkedHttpHeaders)httpRequest.headers();
		this.headerService = headerService;
	}
	
	LinkedHttpHeaders getInternalHeaders() {
		return this.internalHeaders;
	}
	
	@Override
	public String getAuthority() {
		return this.internalHeaders.get((CharSequence)Headers.NAME_HOST);
	}

	@Override
	public String getPath() {
		return this.httpRequest.uri();
	}

	@Override
	public Method getMethod() {
		if(this.method == null) {
			this.method = Method.valueOf(this.httpRequest.method().name());
		}
		return method;
	}

	@Override
	public String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.context.pipeline().get(SslHandler.class) != null ? "https" : "http";
		}
		return this.scheme;
	}

	@Override
	public String getContentType() {
		return this.internalHeaders.get((CharSequence)Headers.NAME_CONTENT_TYPE);
	}

	@Override
	public Long getContentLength() {
		return this.internalHeaders.getLong((CharSequence)Headers.NAME_CONTENT_LENGTH);
	}

	@Override
	public Set<String> getNames() {
		return this.internalHeaders.names();
	}

	@Override
	public Optional<String> get(CharSequence name) {
		return Optional.ofNullable(this.internalHeaders.get(name));
	}
	
	@Override
	public List<String> getAll(CharSequence name) {
		return this.internalHeaders.getAll(name);
	}
	
	@Override
	public List<Entry<String, String>> getAll() {
		return this.internalHeaders.entries();
	}
	
	@Override
	public <T extends Header> Optional<T> getHeader(CharSequence name) {
		return this.get(name).map(value -> this.headerService.decode(name.toString(), value));
	}

	@Override
	public <T extends Header> List<T> getAllHeader(CharSequence name) {
		return this.internalHeaders.getAll(name).stream().map(value -> this.headerService.<T>decode(name.toString(), value)).collect(Collectors.toList());
	}

	@Override
	public List<Header> getAllHeader() {
		return this.internalHeaders.entries().stream().map(e -> (Header)this.headerService.decode(e.getKey(), e.getValue())).collect(Collectors.toList());
	}
	
	@Override
	public boolean contains(CharSequence name, CharSequence value) {
		return this.internalHeaders.contains(name, value, true);
	}
}
