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
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import reactor.core.publisher.BaseSubscriber;

/**
 * <p>
 * Base {@link HttpConnectionRequest} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @param <A> the HTTP request headers type
 */
public abstract class AbstractRequest<A extends OutboundRequestHeaders> extends BaseSubscriber<ByteBuf> implements HttpConnectionRequest {
	
	private final ObjectConverter<String> parameterConverter;
	
	protected final Method method;
	protected final String path;
	private final URIBuilder pathBuilder;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	protected final String authority;
	protected final A headers;
	
	protected int transferredLength;

	/**
	 * <p>
	 * Create an HTTP request.
	 * </p>
	 *
	 * @param parameterConverter the parameter converter
	 * @param endpointRequest    the endpoint request
	 * @param headers            the HTTP headers
	 * @param authority          the request authority
	 */
	public AbstractRequest(
			ObjectConverter<String> parameterConverter, 
			EndpointRequest endpointRequest, 
			A headers, 
			String authority
		) {
		this.parameterConverter = parameterConverter;
		this.method = endpointRequest.getMethod();
		this.path = endpointRequest.getPath();
		this.pathBuilder = endpointRequest.getPathBuilder();
		this.headers = headers;
		this.authority = authority;
	}
	
	/**
	 * <p>
	 * Resolves the request authority.
	 * </p>
	 * 
	 * @param remoteAddress the remote address
	 * @param tls true if the connection is secured, false otherwise
	 * 
	 * @return the authority
	 */
	protected static String resolveAuthority(SocketAddress remoteAddress, boolean tls) {
		if(remoteAddress == null) {
			throw new IllegalStateException("Can't resolve authority");
		}
		else if(remoteAddress instanceof InetSocketAddress) {
			int port = ((InetSocketAddress)remoteAddress).getPort();
			if((tls && port != 443) || (!tls && port != 80)) {
				return ((InetSocketAddress)remoteAddress).getHostString() + ":" + port;
			}
			else {
				return ((InetSocketAddress)remoteAddress).getHostString();
			}
		}
		else {
			return remoteAddress.toString();
		}
	}
	
	/**
	 * <p>
	 * Sends the request.
	 * </p>
	 * 
	 * <p>
	 * This method must execute on the connection event loop and subscribe to the request body data publisher in order to generate and send the request body.
	 * </p>
	 */
	protected abstract void send();
	
	@Override
	public boolean isHeadersWritten() {
		return this.headers.isWritten();
	}

	@Override
	public A headers() {
		return this.headers;
	}
	
	@Override
	public Method getMethod() {
		return this.method;
	}

	@Override
	public String getAuthority() {
		return this.authority;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	@Override
	public String getPathAbsolute() {
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.pathBuilder.buildRawPath();
		}
		return this.pathAbsolute;
	}

	@Override
	public URIBuilder getPathBuilder() {
		return this.pathBuilder.clone();
	}

	@Override
	public String getQuery() {
		if(this.queryString == null) {
			this.queryString = this.pathBuilder.buildRawQuery();
		}
		return this.queryString;
	}

	@Override
	public GenericQueryParameters queryParameters() {
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.pathBuilder.getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}
}
