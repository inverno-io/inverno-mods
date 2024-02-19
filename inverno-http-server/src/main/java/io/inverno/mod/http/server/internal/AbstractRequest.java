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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Base {@link Request} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public abstract class AbstractRequest implements Request {
	
	private static final Logger LOGGER = LogManager.getLogger(AbstractRequest.class);

	protected final ChannelHandlerContext context;
	protected final InboundRequestHeaders requestHeaders;
	protected final ObjectConverter<String> parameterConverter;
	protected final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	protected final MultipartDecoder<Part> multipartBodyDecoder;
	
	private GenericRequestBody requestBody;
	
	private String pathAbsolute;
	private String queryString;
	protected GenericQueryParameters queryParameters;
	
	/**
	 * <p>
	 * Creates a request with the specified channel handler context, request headers, parameter value converter, URL encoded body decoder and multipart body decoder.
	 * </p>
	 * 
	 * @param context               the channel handler context
	 * @param requestHeaders        the underlying request headers
	 * @param parameterConverter    a string object converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 */
	public AbstractRequest(
			ChannelHandlerContext context, 
			InboundRequestHeaders requestHeaders, 
			ObjectConverter<String> parameterConverter, 
			MultipartDecoder<Parameter> urlEncodedBodyDecoder, 
			MultipartDecoder<Part> multipartBodyDecoder) {
		this.context = context;
		this.requestHeaders = requestHeaders;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
	}

	/**
	 * <p>
	 * Returns or creates the primary path builder created from the request path.
	 * </p>
	 * 
	 * <p>
	 * The primary path builder is used to extract absolute path, query parameters and query string and to create the path builder returned by {@link #getPathBuilder() }.
	 * </p>
	 * 
	 * @return the path builder
	 */
	protected abstract URIBuilder getPrimaryPathBuilder();
	
	@Override
	public SocketAddress getLocalAddress() {
		return this.context.channel().localAddress();
	}
	
	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return Optional.ofNullable(this.context.pipeline().get(SslHandler.class))
			.map(handler -> handler.engine().getSession().getLocalCertificates())
			.filter(certificates -> certificates.length > 0);
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		return this.context.channel().remoteAddress();
	}
	
	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return Optional.ofNullable(this.context.pipeline().get(SslHandler.class))
			.map(handler -> {
				try {
					return handler.engine().getSession().getPeerCertificates();
				} 
				catch(SSLPeerUnverifiedException e) {
					LOGGER.debug("Could not verify identity of the client", e);
					return null;
				}
			})
			.filter(certificates -> certificates.length > 0);
	}
	
	@Override
	public String getPathAbsolute() {
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.getPrimaryPathBuilder().buildRawPath();
		}
		return this.pathAbsolute;
	}
	
	@Override
	public URIBuilder getPathBuilder() {
		return this.getPrimaryPathBuilder().clone();
	}
	
	@Override
	public String getQuery() {
		if(this.queryString == null) {
			this.queryString = this.getPrimaryPathBuilder().buildRawQuery();
		}
		return this.queryString;
	}
	
	@Override
	public QueryParameters queryParameters() {
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.getPrimaryPathBuilder().getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}
	
	@Override
	public InboundRequestHeaders headers() {
		return this.requestHeaders;
	}
	
	@Override
	@Deprecated
	public InboundCookies cookies() {
		return this.requestHeaders.cookies();
	}

	@Override
	public Optional<RequestBody> body() {
		if(this.requestBody == null) {
			Method method = this.getMethod();
			switch(method) {
				case POST:
				case PUT:
				case PATCH:
				case DELETE: {
					if(this.requestBody == null) {
						this.requestBody = new GenericRequestBody(
							this.headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE),
							this.urlEncodedBodyDecoder, 
							this.multipartBodyDecoder
						);
					}
					break;
				}
				default: {
					this.requestBody = null;
					break;
				}
			}
		}
		return Optional.ofNullable(this.requestBody);
	}

	/**
	 * <p>
	 * Returns the request payload data sink.
	 * </p>
	 *
	 * @return an optional returning the payload data sink or an empty optional if the request has no body
	 */
	public Optional<Sinks.Many<ByteBuf>> data() {
		return this.body().map(body -> ((GenericRequestBody)body).dataSink);
	}
	
	/**
	 * <p>
	 * Disposes the request.
	 * </p>
	 * 
	 * <p>
	 * This method delegates to {@link #dispose(java.lang.Throwable) } with a null error.
	 * </p>
	 */
	public void dispose() {
		this.dispose(null);
	}
	
	/**
	 * <p>
	 * Disposes the request with the specified error.
	 * </p>
	 * 
	 * <p>
	 * This method cleans up request outstanding resources, it especially drains received data if needed.
	 * </p>
	 * 
	 * <p>
	 * A non-null error indicates that the enclosing exchange did not complete successfully and that the error should be emitted when possible (e.g. in the request data publisher).
	 * </p>
	 * 
	 * @param error an error or null
	 * 
	 * @see GenericRequestBody#dispose(java.lang.Throwable) 
	 */
	public void dispose(Throwable error) {
		if(this.requestBody != null) {
			this.requestBody.dispose(error);
		}
	}
}
