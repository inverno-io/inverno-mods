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
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.netty.FlatFullHttpRequest;
import io.inverno.mod.http.base.internal.netty.FlatHttpRequest;
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.LastHttpContent;
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
class Http1xRequest extends AbstractRequest<Http1xRequestHeaders> {
	
	private final Http1xConnection connection;
	
	private String scheme;
	private final Http1xRequestBody body;
	
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
		super(
			parameterConverter, 
			endpointRequest, 
			new Http1xRequestHeaders(endpointRequest.getHeaders()), 
			endpointRequest.getAuthority() == null ? resolveAuthority(connection.getRemoteAddress(), connection.isTls()) : endpointRequest.getAuthority()
		);
		this.connection = connection;
		this.body = endpointRequest.getBody() != null ? new Http1xRequestBody(endpointRequest.getBody(), connection.supportsFileRegion()) : null;
		
		this.headers.set(Headers.NAME_HOST, this.authority);
	}
	
	/**
	 * {@inheritDoc }
	 * 
	 * <p>
	 * The request body file region publisher is subscribed when present superseding the request body data publisher.
	 * </p>
	 */
	@Override
	public void send() {
		if(this.connection.executor().inEventLoop()) {
			if(this.body != null) {
				if(this.headers.contains(Headers.NAME_EXPECT, Headers.VALUE_100_CONTINUE)) {
					this.connection.writeHttpObject(new FlatHttpRequest(this.connection.getVersion(), HttpMethod.valueOf(this.method.name()), this.path, this.headers.unwrap()));
					this.headers.setWritten();
				}
				else {
					this.sendBody();
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
	 * Sends the request body.
	 * </p>
	 */
	void sendBody() {
		if(this.body.getFileRegionData() == null) {
			this.body.getData().subscribe(this.body.getData() instanceof Mono ? new MonoBodyDataSubscriber() : new BodyDataSubscriber());
		}
		else {
			Flux.concat(this.body.getFileRegionData(), Flux.from(this.body.getData()).cast(FileRegion.class)).subscribe(new FileRegionBodyDataSubscriber());
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
			if(Http1xRequest.this.headers.isWritten()) {
				if(this.data != null) {
					Http1xRequest.this.connection.writeHttpObject(new DefaultLastHttpContent(this.data));
				}
				else {
					Http1xRequest.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
				}
			}
			else {
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
			}
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
				if(Http1xRequest.this.headers.isWritten()) {
					if(this.singleChunk == null) {
						Http1xRequest.this.connection.writeHttpObject(new DefaultLastHttpContent(this.singleChunk));
					}
					else {
						Http1xRequest.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
					}
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
			}
			Http1xRequest.this.connection.onRequestSent();
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			Http1xRequest.this.connection.onRequestError(throwable);
		}

		@Override
		protected void hookOnCancel() {
			// Make sure the Http protocol flow is correct
			if(this.many) {
				Http1xRequest.this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
			}
		}
	}
	
	private class FileRegionBodyDataSubscriber extends BaseSubscriber<FileRegion> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			if(!Http1xRequest.this.headers.isWritten()) {
				Http1xRequest.this.connection.writeHttpObject(new FlatHttpRequest(Http1xRequest.this.connection.getVersion(), HttpMethod.valueOf(Http1xRequest.this.method.name()), Http1xRequest.this.path, Http1xRequest.this.headers.unwrap()));
				Http1xRequest.this.headers.setWritten();
			}
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
