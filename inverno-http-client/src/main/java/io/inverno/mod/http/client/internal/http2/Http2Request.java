/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.http.client.internal.http2;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/**
 * <p>
 * HTTP/2 {@link Request} implementation
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2Request implements HttpConnectionRequest {
	
	private final ObjectConverter<String> parameterConverter;
	private final Http2ConnectionStream connectionStream;
	
	private final Method method;
	private final String path;
	private final URIBuilder pathBuilder;
	private final Http2RequestHeaders headers;
	private final Http2RequestBody body;
	
	private String scheme;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	private final String authority;
	
	private int transferedLength;
	private Disposable disposable;
	
	/**
	 * <p>
	 * Creates an HTTP/2 request.
	 * </p>
	 * 
	 * @param headerService the header service
	 * @param parameterConverter the parameter converter
	 * @param connectionStream the Http/2 connection stream
	 * @param endpointRequest the endpoint request
	 * @param validateHeaders true to validate headers, false otherwise
	 */
	public Http2Request(
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			Http2ConnectionStream connectionStream, 
			EndpointRequest endpointRequest, 
			boolean validateHeaders
		) {
		this.parameterConverter = parameterConverter;
		this.connectionStream = connectionStream;
		
		this.method = endpointRequest.getMethod();
		this.path = endpointRequest.getPath();
		this.pathBuilder = endpointRequest.getPathBuilder();
		if(endpointRequest.getAuthority() == null) {
			SocketAddress remoteAddress = connectionStream.getRemoteAddress();
			if(remoteAddress == null) {
				throw new IllegalStateException("Can't resolve authority");
			}
			else if(remoteAddress instanceof InetSocketAddress) {
				int port = ((InetSocketAddress)remoteAddress).getPort();
				if((connectionStream.isTls() && port != 443) || (!connectionStream.isTls() && port != 80)) {
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
		
		this.headers = new Http2RequestHeaders(headerService, parameterConverter, endpointRequest.getHeaders(), validateHeaders);
		this.headers.unwrap()
			.method(endpointRequest.getMethod().name())
			.scheme(this.getScheme())
			.authority(this.authority)
			.path(endpointRequest.getPath());
		this.body = endpointRequest.getBody() != null ? new Http2RequestBody(endpointRequest.getBody()) : null;
	}

	/**
	 * <p>
	 * Sends the request.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop, it subscribes to the request body data publisher in order to generate and send the request body.
	 * </p>
	 */
	public void send() {
		if(this.connectionStream.executor().inEventLoop()) {
			if(this.body != null) {
				this.body.getData().subscribe(this.body.getData() instanceof Mono ? new Http2Request.MonoBodyDataSubscriber() : new Http2Request.BodyDataSubscriber());
			}
			else {
				this.connectionStream.writeHeaders(this.headers.unwrap(), 0, true);
				this.headers.setWritten();
			}
		}
		else {
			this.connectionStream.executor().execute(this::send);
		}
	}
	
	/**
	 * <p>
	 * Disposes the request.
	 * </p>
	 * 
	 * <p>
	 * This method simply cancels any active subscription.
	 * </p>
	 * 
	 * @param cause 
	 */
	final void dispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
		}
	}
	
	@Override
	public boolean isHeadersWritten() {
		return this.headers.isWritten();
	}

	@Override
	public Http2RequestHeaders headers() {
		return this.headers;
	}

	@Override
	public final String getScheme() {
		if(this.scheme == null) {
			this.scheme = this.connectionStream.isTls() ? "https" : "http";
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
	
	/**
	 * <p>
	 * The request body data publisher optimized for {@link Mono} publisher that writes a single request object to the connection.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.11
	 */
	private class MonoBodyDataSubscriber extends BaseSubscriber<ByteBuf> {
		private ByteBuf data;
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http2Request.this.disposable = this;
			subscription.request(1);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			Http2Request.this.transferedLength += value.readableBytes();
			this.data = value;
		}

		@Override
		protected void hookOnComplete() {
			if(!Http2Request.this.headers.contains(Headers.NAME_CONTENT_LENGTH)) {
				Http2Request.this.headers.contentLength(Http2Request.this.transferedLength);
			}

			if(this.data == null) {
				Http2Request.this.connectionStream.writeHeaders(Http2Request.this.headers.unwrap(), 0, true);
				Http2Request.this.headers.setWritten();
			}
			else {
				Http2Request.this.connectionStream.writeHeaders(Http2Request.this.headers.unwrap(), 0, false);
				Http2Request.this.headers.setWritten();
				Http2Request.this.connectionStream.writeData(this.data, 0, true);
			}
		}
		
		@Override
		protected void hookOnError(Throwable throwable) {
			Http2Request.this.connectionStream.onRequestError(throwable);
		}
	}
	
	/**
	 * <p>
	 * The request body data subscriber that writes request objects to the connection.
	 * </p>
	 * 
	 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.11
	 */
	private class BodyDataSubscriber extends BaseSubscriber<ByteBuf> {
		
		private ByteBuf singleChunk;
		private boolean many;
		
		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http2Request.this.disposable = this;
			subscription.request(1);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			Http2Request.this.transferedLength += value.readableBytes();
			if(!this.many && this.singleChunk == null) {
				this.singleChunk = value;
				this.request(1);
			}
			else {
				this.many = true;
				if(!Http2Request.this.headers.isWritten()) {
					Http2Request.this.connectionStream.writeHeaders(Http2Request.this.headers.unwrap(), 0, false);
					Http2Request.this.headers.setWritten();
					Http2Request.this.connectionStream.writeData(this.singleChunk, 0, false);
					this.singleChunk = null;
				}
				
				/*
				 * In case of big request body, we can end up flooding the channel with WRITE operations, preventing READ to happen.
				 * This is problematic and will result in connection erros when the server sends a response and terminates the exchange before the request has been entirely sent 
				 * (e.g. 413 REQUEST ENTITY TOO LARGE).
				 * To fix this, we simply wait for the write operation to succeed before requesting the next chunk.
				 */
				Http2Request.this.connectionStream.writeData(value, 0, false, Http2Request.this.connectionStream.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						this.request(1);
					}
					else {
						Http2Request.this.connectionStream.onRequestError(future.cause());
					}
				}));
			}
		}

		@Override
		protected void hookOnComplete() {
			if(this.many) {
				Http2Request.this.connectionStream.writeData(Unpooled.EMPTY_BUFFER, 0, true);
			}
			else {
				if(!Http2Request.this.headers.contains(Headers.NAME_CONTENT_LENGTH)) {
					Http2Request.this.headers.contentLength(Http2Request.this.transferedLength);
				}
				if(this.singleChunk == null) {
					Http2Request.this.connectionStream.writeHeaders(Http2Request.this.headers.unwrap(), 0, true);
					Http2Request.this.headers.setWritten();
				}
				else {
					Http2Request.this.connectionStream.writeHeaders(Http2Request.this.headers.unwrap(), 0, false);
					Http2Request.this.headers.setWritten();
					Http2Request.this.connectionStream.writeData(this.singleChunk, 0, true);
					this.singleChunk = null;
				}
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http2Request.this.connectionStream.onRequestError(throwable);
		}
	}
}
