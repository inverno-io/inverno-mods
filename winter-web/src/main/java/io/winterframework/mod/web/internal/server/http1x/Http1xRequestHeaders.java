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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslHandler;
import io.winterframework.mod.web.Header;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.RequestHeaders;
import io.winterframework.mod.web.internal.netty.LinkedHttpHeaders;

/**
 * @author jkuhn
 *
 */
public class Http1xRequestHeaders implements RequestHeaders {

	private final ChannelHandlerContext context;
	private final HeaderService headerService;
	private final HttpRequest httpRequest;
	private final LinkedHttpHeaders httpHeaders;
	
	private Method method;
	private String scheme;
	
	public Http1xRequestHeaders(ChannelHandlerContext context, HttpRequest httpRequest, HeaderService headerService) {
		this.context = context;
		this.httpRequest = httpRequest;
		this.httpHeaders = (LinkedHttpHeaders)httpRequest.headers();
		this.headerService = headerService;
	}
	
	@Override
	public String getAuthority() {
		return this.httpHeaders.get((CharSequence)Headers.HOST);
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
		return this.httpHeaders.get((CharSequence)Headers.CONTENT_TYPE);
	}

	@Override
	public Long getSize() {
		return this.httpHeaders.getLong((CharSequence)Headers.CONTENT_LENGTH);
	}

	@Override
	public Set<String> getNames() {
		return this.httpHeaders.names();
	}

	@Override
	public <T extends Header> Optional<T> getHeader(String name) {
		return Optional.ofNullable(this.httpHeaders.get((CharSequence)name)).map(value -> this.headerService.decode(name, value));
	}

	@Override
	public <T extends Header> List<T> getAllHeader(String name) {
		return this.httpHeaders.getAll((CharSequence)name).stream().map(value -> this.headerService.<T>decode(name, value)).collect(Collectors.toList());
	}

	@Override
	public Map<String, List<Header>> getAllHeader() {
		return this.httpHeaders.entries().stream().map(e -> (Header)this.headerService.decode(e.getKey(), e.getValue())).collect(Collectors.groupingBy(h -> h.getHeaderName()));
	}
	
	@Override
	public boolean contains(String name, String value) {
		return this.httpHeaders.contains(name, value, true);
	}
}
