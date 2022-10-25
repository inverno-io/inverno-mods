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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.client.Request;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public abstract class AbstractRequest implements Request {

	private final ChannelHandlerContext context;
	private final boolean tls;
	private final ObjectConverter<String> parameterConverter;
	private final HttpVersion protocol;
	private final Method method;
	private final String path;
	private final URIBuilder primaryPathBuilder;
	protected final InternalRequestHeaders requestHeaders;
	protected final Optional<GenericRequestBody> requestBody;
	
	private String scheme;
	private String authority;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	
	private GenericRequestCookies requestCookies;
	
	protected AbstractRequest(
			ChannelHandlerContext context, 
			boolean tls,
			ObjectConverter<String> parameterConverter, 
			HttpVersion protocol, 
			Method method, 
			String authority,
			String path, 
			InternalRequestHeaders requestHeaders, 
			GenericRequestBody requestBody) {
		this.context = context;
		this.tls = tls;
		this.parameterConverter = parameterConverter;
		this.protocol = protocol;
		this.method = method;
		this.path = path;
		// TODO make sure this is a path with no scheme or authority
		this.primaryPathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED);
		this.requestHeaders = requestHeaders;
		this.requestBody = Optional.ofNullable(requestBody);
	}

	@Override
	public InternalRequestHeaders headers() {
		return this.requestHeaders;
	}

	@Override
	public Request headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		headersConfigurer.accept(this.requestHeaders);
		return this;
	}

	@Override
	public Request authority(String authority) {
		if(this.isHeadersWritten()) {
			throw new IllegalStateException("Headers already written");
		}
		this.authority = authority;
		return this;
	}
	
	@Override
	public boolean isHeadersWritten() {
		return this.requestHeaders.isWritten();
	}

	@Override
	public HttpVersion getProtocol() {
		return this.protocol;
	}

	@Override
	public String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.tls ? "https" : "http";
		}
		return this.scheme;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.context.channel().localAddress();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.context.channel().remoteAddress();
	}
	
	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public String getAuthority() {
		if(this.authority == null) {
			SocketAddress remoteAddress = this.getRemoteAddress();
			if(remoteAddress == null) {
				return null;
			}
			else if(remoteAddress instanceof InetSocketAddress) {
				this.authority = ((InetSocketAddress)remoteAddress).getHostString();
				int port = ((InetSocketAddress)remoteAddress).getPort();
				if((this.tls && port != 443) || (this.tls && port != 80)) {
					this.authority += ":" + port;
				}
			}
			else {
				this.authority = remoteAddress.toString();
			}
		}
		return this.authority;
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
	public Optional<? extends GenericRequestBody> body() {
		return this.requestBody;
	}
}
