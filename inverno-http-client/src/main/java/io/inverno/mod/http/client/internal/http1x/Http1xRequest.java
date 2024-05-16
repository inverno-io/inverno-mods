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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.base.internal.netty.FlatFullHttpRequest;
import io.inverno.mod.http.base.internal.netty.FlatHttpRequest;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.inverno.mod.http.client.internal.HttpConnectionRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.LastHttpContent;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * <p>
 * Http/1.x {@link Request} implementation
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xRequest implements HttpConnectionRequest {
	
	private final ObjectConverter<String> parameterConverter;
	private final Http1xConnection connection;
	
	private final Method method;
	private final String path;
	private final URIBuilder pathBuilder;
	private final Http1xRequestHeaders headers;
	private final Http1xRequestBody body;
	
	private String scheme;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	private final String authority;
	
	private int transferedLength;
	private Disposable disposable;

	/**
	 * <p>
	 * Creates an Http/1.x request.
	 * </p>
	 *
	 * @param parameterConverter the parameter converter
	 * @param connection         the Http/1.x connection
	 * @param endpointRequest    the endpoint request
	 */
	public Http1xRequest(ObjectConverter<String> parameterConverter, Http1xConnection connection, EndpointRequest endpointRequest) {
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
		this.headers.set(Headers.NAME_HOST, this.authority);
		this.body = endpointRequest.getBody() != null ? new Http1xRequestBody(endpointRequest.getBody(), connection.supportsFileRegion()) : null;
	}
	
	/**
	 * <p>
	 * Sends the request.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop, it subscribes to the request body file region publisher when present and to the request body data publisher otherwise in order to generate 
	 * and send the request body.
	 * </p>
	 */
	public void send() {
		if(this.connection.executor().inEventLoop()) {
			if(this.body != null) {
				if(this.body.getFileRegionData() == null) {
					this.body.getData().subscribe(this.body.getData() instanceof Mono ? new MonoBodyDataSubscriber() : new BodyDataSubscriber());
				}
				else {
					Flux.concat(this.body.getFileRegionData(), Flux.from(this.body.getData()).cast(FileRegion.class)).subscribe(new FileRegionBodyDataSubscriber());
				}
			}
			else {
				this.connection.writeHttpObject(new FlatFullHttpRequest(this.connection.getVersion(), HttpMethod.valueOf(this.method.name()), this.path, this.headers.unwrap(), Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE));
				this.headers.setWritten();
				this.connection.onRequestSent();
			}
		}
		else {
			this.connection.executor().execute(this::send);
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
	public Http1xRequestHeaders headers() {
		return this.headers;
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
			Http1xRequest.this.disposable = this;
			subscription.request(1);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			Http1xRequest.this.transferedLength += value.readableBytes();
			this.data = value;
		}

		@Override
		protected void hookOnComplete() {
			if(!Http1xRequest.this.headers.contains((CharSequence)Headers.NAME_CONTENT_LENGTH)) {
				List<String> transferEncodings = Http1xRequest.this.headers.getAll((CharSequence)Headers.NAME_TRANSFER_ENCODING);
				if(transferEncodings.isEmpty() || !transferEncodings.getLast().endsWith(Headers.VALUE_CHUNKED)) {
					// set content length
					Http1xRequest.this.headers.add((CharSequence)Headers.NAME_CONTENT_LENGTH, "" + Http1xRequest.this.transferedLength);
				}
			}
			if(this.data != null) {
				Http1xRequest.this.connection.writeHttpObject(new FlatFullHttpRequest(Http1xRequest.this.connection.getVersion(), HttpMethod.valueOf(Http1xRequest.this.method.name()), Http1xRequest.this.path, Http1xRequest.this.headers.unwrap(), this.data, EmptyHttpHeaders.INSTANCE));
			}
			else {
				Http1xRequest.this.connection.writeHttpObject(new FlatFullHttpRequest(Http1xRequest.this.connection.getVersion(), HttpMethod.valueOf(Http1xRequest.this.method.name()), Http1xRequest.this.path, Http1xRequest.this.headers.unwrap(), Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE));
			}
			Http1xRequest.this.headers.setWritten();
			Http1xRequest.this.connection.onRequestSent();
		}
		
		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xRequest.this.connection.onRequestError(throwable);
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
		
		private void sanitizeResponse() {
			if(!Http1xRequest.this.headers.contains((CharSequence)Headers.NAME_CONTENT_LENGTH)) {
				List<String> transferEncodings = Http1xRequest.this.headers.getAll((CharSequence)Headers.NAME_TRANSFER_ENCODING);
				if(!this.many && (transferEncodings.isEmpty() || !transferEncodings.getLast().endsWith(Headers.VALUE_CHUNKED))) {
					// set content length
					Http1xRequest.this.headers.add((CharSequence)Headers.NAME_CONTENT_LENGTH, "" + Http1xRequest.this.transferedLength);
				}
				else if(transferEncodings.isEmpty()) {
					Http1xRequest.this.headers.add((CharSequence)Headers.NAME_TRANSFER_ENCODING, (CharSequence)Headers.VALUE_CHUNKED);
				}
			}
		}

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http1xRequest.this.disposable = this;
			subscription.request(1);
		}
		
		@Override
		protected void hookOnNext(ByteBuf value) {
			Http1xRequest.this.transferedLength += value.readableBytes();
			if(!this.many && this.singleChunk == null) {
				this.singleChunk = value;
				this.request(1);
			}
			else {
				this.many = true;
				if(!Http1xRequest.this.headers.isWritten()) {
					this.sanitizeResponse();
					Http1xRequest.this.connection.writeHttpObject(new FlatHttpRequest(Http1xRequest.this.connection.getVersion(), HttpMethod.valueOf(Http1xRequest.this.method.name()), Http1xRequest.this.path, Http1xRequest.this.headers.unwrap(), this.singleChunk));
					Http1xRequest.this.headers.setWritten();
					this.singleChunk = null;
				}
				
				/*
				 * In case of big request body, we can end up flooding the channel with WRITE operations, preventing READ to happen.
				 * This is problematic and will result in connection erros when the server sends a response and terminates the exchange before the request has been entirely sent 
				 * (e.g. 413 REQUEST ENTITY TOO LARGE).
				 * To fix this, we simply wait for the write operation to succeed before requesting the next chunk.
				 */
				Http1xRequest.this.connection.writeHttpObject(new DefaultHttpContent(value), Http1xRequest.this.connection.newPromise().addListener(future -> {
					if(future.isSuccess()) {
						this.request(1);
					}
				}));
			}
		}

		@Override
		protected void hookOnComplete() {
			if(this.many) {
				Http1xRequest.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
			}
			else {
				this.sanitizeResponse();
				if(this.singleChunk == null) {
					Http1xRequest.this.connection.writeHttpObject(new FlatFullHttpRequest(Http1xRequest.this.connection.getVersion(), HttpMethod.valueOf(Http1xRequest.this.method.name()), Http1xRequest.this.path, Http1xRequest.this.headers.unwrap(), Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE));
				}
				else {
					Http1xRequest.this.connection.writeHttpObject(new FlatFullHttpRequest(Http1xRequest.this.connection.getVersion(), HttpMethod.valueOf(Http1xRequest.this.method.name()), Http1xRequest.this.path, Http1xRequest.this.headers.unwrap(), this.singleChunk, EmptyHttpHeaders.INSTANCE));
					this.singleChunk = null;
				}
				Http1xRequest.this.headers.setWritten();
			}
			Http1xRequest.this.connection.onRequestSent();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xRequest.this.connection.onRequestError(throwable);
		}
	}
	
	private class FileRegionBodyDataSubscriber extends BaseSubscriber<FileRegion> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			Http1xRequest.this.connection.writeHttpObject(new FlatHttpRequest(Http1xRequest.this.connection.getVersion(), HttpMethod.valueOf(Http1xRequest.this.method.name()), Http1xRequest.this.path, Http1xRequest.this.headers.unwrap()));
			Http1xRequest.this.headers.setWritten();
			subscription.request(1);
		}

		@Override
		protected void hookOnNext(FileRegion value) {
			Http1xRequest.this.transferedLength += value.count();
			Http1xRequest.this.connection.writeFileRegion(value, Http1xRequest.this.connection.newPromise().addListener(future -> {
				if(future.isSuccess()) {
					this.request(1);
				}
				else {
					Http1xRequest.this.connection.onRequestError(future.cause());
				}
			}));
		}

		@Override
		protected void hookOnComplete() {
			Http1xRequest.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
			Http1xRequest.this.connection.onRequestSent();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xRequest.this.connection.onRequestError(throwable);
		}
	}
}
