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
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.client.PreRequest;
import io.inverno.mod.http.client.Request;
import java.util.Optional;
import java.util.function.Consumer;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import io.netty.buffer.ByteBuf;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import io.inverno.mod.http.client.PreRequestBody;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class GenericPreRequest implements PreRequest, Request {

	private final ObjectConverter<String> parameterConverter;
	
	private final Method method;
	private final String path;
	private final URIBuilder primaryPathBuilder;
	private final GenericRequestHeaders requestHeaders;
	private final GenericPreRequestBody requestBody;
	
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	
	private String authority;

	public GenericPreRequest(HeaderService headerService, ObjectConverter<String> parameterConverter, Method method, String path, String authority, List<Map.Entry<String, String>> headers, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		this.parameterConverter = parameterConverter;
		this.method = method;
		this.path = path;
		this.primaryPathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED);
		this.requestHeaders = new GenericRequestHeaders(headerService, parameterConverter);
		this.requestBody = bodyConfigurer != null ? new GenericPreRequestBody() : null;
	}
	
	@Override
	public Method getMethod() {
		return this.method;
	}
	
	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public String getPathAbsolute() {
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.primaryPathBuilder.buildRawString();
		}
		return this.pathAbsolute;
	}

	@Override
	public URIBuilder getPathBuilder() {
		return this.primaryPathBuilder.clone();
	}

	@Override
	public String getQuery() {
		if(this.queryString == null) {
			this.queryString = this.primaryPathBuilder.buildRawQuery();
		}
		return this.queryString;
	}

	@Override
	public QueryParameters queryParameters() {
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.primaryPathBuilder.getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}

	@Override
	public String getAuthority() {
		return this.authority;
	}
	
	@Override
	public PreRequest authority(String authority) {
		this.authority = authority;
		return this;
	}
	
	@Override
	public InboundRequestHeaders headers() {
		return this.requestHeaders;
	}
	
	@Override
	public PreRequest headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		headersConfigurer.accept(this.requestHeaders);
		return this;
	}

	@Override
	public Optional<PreRequestBody> body() {
		return Optional.ofNullable(this.requestBody);
	}
	
	public Function<Publisher<ByteBuf>, Publisher<ByteBuf>> getBodyTransformer() {
		return this.requestBody != null ? this.requestBody.getTransformer() : null;
	}
	
	public List<Map.Entry<String, String>> getHeaders() {
		return this.requestHeaders.getAll();
	} 

	@Override
	public boolean isHeadersWritten() {
		return true;
	}

	@Override
	public HttpVersion getProtocol() {
		return HttpVersion.HTTP;
	}

	@Override
	public String getScheme() {
		return "http";
	}

	@Override
	public SocketAddress getLocalAddress() {
		return null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return null;
	}
}
