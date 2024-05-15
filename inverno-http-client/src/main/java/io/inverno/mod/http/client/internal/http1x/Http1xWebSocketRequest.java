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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * <p>
 * HTTP/1.x WebSocket {@link Request} implementation
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.1
 */
class Http1xWebSocketRequest implements HttpConnectionRequest {

	private final ObjectConverter<String> parameterConverter;
	private final Http1xWebSocketConnection connection;
	
	private final Method method;
	private final String path;
	private final URIBuilder pathBuilder;
	private final Http1xRequestHeaders headers;
	
	private String scheme;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	private final String authority;

	/**
	 * <p>
	 * Creates an HTTP/1.x request.
	 * </p>
	 * 
	 * @param parameterConverter the parameter converter
	 * @param connection         the WebSocket connection
	 * @param endpointRequest    the endpoint request
	 */
	public Http1xWebSocketRequest(ObjectConverter<String> parameterConverter, Http1xWebSocketConnection connection, EndpointRequest endpointRequest) {
		this.parameterConverter = parameterConverter;
		this.connection = connection;
		
		this.method = endpointRequest.getMethod();
		this.path = endpointRequest.getPath();
		this.pathBuilder = endpointRequest.getPathBuilder();
		if(endpointRequest.getAuthority() == null) {
			SocketAddress remoteAddress = connection.getRemoteAddress();
			if(remoteAddress == null) {
				throw new IllegalStateException("Can't resolve authority");
			}
			else if(remoteAddress instanceof InetSocketAddress) {
				int port = ((InetSocketAddress)remoteAddress).getPort();
				if((connection.isTls() && port != 443) || (!connection.isTls() && port != 80)) {
					this.authority = ((InetSocketAddress)remoteAddress).getHostString() + ":" + port;
				}
				else {
					this.authority = ((InetSocketAddress)remoteAddress).getHostString();
				}
			}
			else {
				this.authority = remoteAddress.toString();
			}
		}
		else {
			this.authority = endpointRequest.getAuthority();
		}
		
		this.headers = new Http1xRequestHeaders(endpointRequest.getHeaders());
	}
	
	@Override
	public boolean isHeadersWritten() {
		return true;
	}

	@Override
	public Http1xRequestHeaders headers() {
		return this.headers;
	}

	@Override
	public String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.connection.isTls() ? "wss" : "ws";
		}
		return this.scheme;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.connection.getLocalAddress();
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return this.connection.getLocalCertificates();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.connection.getRemoteAddress();
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.connection.getRemoteCertificates();
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
