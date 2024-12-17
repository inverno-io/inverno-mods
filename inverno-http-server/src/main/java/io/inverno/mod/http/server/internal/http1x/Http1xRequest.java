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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.internal.AbstractRequest;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.handler.codec.http.HttpRequest;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * <p>
 * Http/1.x {@link Request} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http1xRequest extends AbstractRequest<Http1xRequestHeaders, Http1xRequestBody> {

	private final Http1xConnection connection;
	private final HttpRequest request;
	
	private String scheme;
	private Method method;
	private String authority;

	/**
	 * <p>
	 * Creates an Http/1.x request.
	 * </p>
	 *
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param connection            the Http/1.x connection
	 * @param request               the originating HTTP request
	 */
	public Http1xRequest(
			HeaderService headerService,
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			Http1xConnection connection,
			HttpRequest request
		) {
		super(parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder, new Http1xRequestHeaders(headerService, parameterConverter, (LinkedHttpHeaders)request.headers()));
		this.connection = connection;
		this.request = request;
	}
	
	/**
	 * <p>
	 * Returns the originating request.
	 * </p>
	 * 
	 * @return the originating request
	 */
	HttpRequest unwrap() {
		return this.request;
	}
	
	@Override
	public String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.connection.isTls() ? "https" : "http";
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
		if(this.method == null) {
			try {
				this.method = Method.valueOf(this.request.method().name());
			}
			catch (IllegalArgumentException e) {
				this.method = Method.UNKNOWN;
			}
		}
		return method;
	}

	@Override
	public String getPath() {
		return this.request.uri();
	}
	
	@Override
	public String getAuthority() {
		if(this.authority == null) {
			this.authority = this.request.headers().get((CharSequence)Headers.NAME_HOST);
		}
		return this.authority;
	}
	
	@Override
	public Optional<Http1xRequestBody> body() {
		if(this.body == null) {
			switch(this.getMethod()) {
				case POST:
				case PUT:
				case PATCH:
				case DELETE: {
					this.body = new Http1xRequestBody(this.urlEncodedBodyDecoder, this.multipartBodyDecoder, this.headers);
					break;
				}
			}
		}
		return Optional.ofNullable(this.body);
	}
}