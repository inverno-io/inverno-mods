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
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.inverno.mod.http.client.internal.EndpointRequest;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * <p>
 * HTTP/1.x WebSocket {@link Request} implementation
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.11
 */
class Http1xWebSocketRequest extends AbstractRequest<Http1xRequestHeaders> {

	private final Http1xWebSocketConnection connection;
	
	private String scheme;

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
		super(
			parameterConverter, 
			endpointRequest, 
			new Http1xRequestHeaders(endpointRequest.getHeaders()), 
			endpointRequest.getAuthority() == null ? resolveAuthority(connection.getRemoteAddress(), connection.isTls()) : endpointRequest.getAuthority()
		);
		this.connection = connection;
		this.headers.setWritten();
	}

	@Override
	protected void send() {
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
}