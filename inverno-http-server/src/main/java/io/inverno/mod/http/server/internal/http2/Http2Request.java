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
package io.inverno.mod.http.server.internal.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.internal.AbstractRequest;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.handler.codec.http2.Http2Headers;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * <p>
 * Http/2 {@link Request} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http2Request extends AbstractRequest<Http2RequestHeaders, Http2RequestBody> {

	private final Http2ConnectionStream connectionStream;
	
	private String scheme;
	private Method method;
	private String path;
	private String authority;
	
	/**
	 * <p>
	 * Creates an Http/2 request.
	 * </p>
	 * 
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param connectionStream      the connection stream
	 * @param headers               the originating headers
	 */
	public Http2Request(
			HeaderService headerService,
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder,
			Http2ConnectionStream connectionStream, 
			Http2Headers headers
		) {
		super(parameterConverter, urlEncodedBodyDecoder, multipartBodyDecoder, new Http2RequestHeaders(headerService, parameterConverter, headers));
		this.connectionStream = connectionStream;
	}
	
	@Override
	public String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.headers.get(Headers.NAME_PSEUDO_SCHEME).orElse(null);
		}
		return this.scheme;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.connectionStream.getLocalAddress();
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return this.connectionStream.getLocalCertificates();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.connectionStream.getRemoteAddress();
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.connectionStream.getRemoteCertificates();
	}
	
	@Override
	public Method getMethod() {
		if(this.method == null) {
			this.method = this.headers.get(Headers.NAME_PSEUDO_METHOD)
				.map(methodString -> {
					try {
						return Method.valueOf(methodString);
					}
					catch(IllegalArgumentException e) {
						return Method.UNKNOWN;
					}
				})
				.orElse(null);
		}
		return method;
	}
	
	@Override
	public String getPath() {
		if(this.path == null) {
			this.path = this.headers.get(Headers.NAME_PSEUDO_PATH).orElse(null);
		}
		return this.path;
	}
	
	@Override
	public String getAuthority() {
		if(this.authority == null) {
			this.authority = this.headers.get(Headers.NAME_PSEUDO_AUTHORITY).orElse(null);
		}
		return this.authority;
	}

	@Override
	public Optional<Http2RequestBody> body() {
		if(this.body == null) {
			switch(this.getMethod()) {
				case POST:
				case PUT:
				case PATCH:
				case DELETE: {
					this.body = new Http2RequestBody(this.urlEncodedBodyDecoder, this.multipartBodyDecoder, this.headers);
					break;
				}
			}
		}
		return Optional.ofNullable(this.body);
	}
}
