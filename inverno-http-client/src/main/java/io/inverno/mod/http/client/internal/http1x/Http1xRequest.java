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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.LastHttpContent;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Optional;
import org.reactivestreams.Publisher;
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
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
class Http1xRequest extends AbstractRequest<Http1xRequestHeaders> {
	
	private final Http1xConnection connection;
	
	private String scheme;
	private final Http1xRequestBody body;
	
	private Disposable disposable;

	/* Body data subscription */
	private boolean mono;
	private ByteBuf singleChunk;
	private boolean many;

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
			Publisher<ByteBuf> data = this.body.getData();
			this.mono = data instanceof Mono;
			data.subscribe(this);
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
	 * @param cause the error that caused the disposal or null if there was no error
	 */
	final void dispose(Throwable cause) {
		if(this.disposable != null) {
			this.disposable.dispose();
			this.disposable = null;
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

	private void sanitizeRequest() {
		if(!this.headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
			List<String> transferEncodings = this.headers.getAll(HttpHeaderNames.TRANSFER_ENCODING);
			if(!this.many && (transferEncodings.isEmpty() || !transferEncodings.getLast().endsWith(Headers.VALUE_CHUNKED))) {
				// set content length
				this.headers.add(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(this.transferredLength));
			}
			else if(transferEncodings.isEmpty()) {
				this.headers.add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
			}
		}
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		this.disposable = this;
		subscription.request(1);
	}

	@Override
	protected void hookOnNext(ByteBuf value) {
		this.transferredLength += value.readableBytes();
		if(this.mono || (!this.many && this.singleChunk == null)) {
			this.singleChunk = value;
			this.request(1);
		}
		else {
			this.many = true;
			if(!this.headers.isWritten()) {
				this.sanitizeRequest();
				this.connection.writeHttpObject(new FlatHttpRequest(this.connection.getVersion(), HttpMethod.valueOf(this.method.name()), this.path, this.headers.unwrap(), this.singleChunk));
				this.headers.setWritten();
				this.singleChunk = null;
			}

			/*
			 * In case of big request body, we can end up flooding the channel with WRITE operations, preventing READ to happen.
			 * This is problematic and will result in connection errors when the server sends a response and terminates the exchange before the request has been entirely sent
			 * (e.g. 413 REQUEST ENTITY TOO LARGE).
			 * To fix this, we simply wait for the write operation to succeed before requesting the next chunk.
			 *
			 * TODO this basically serializes the write operation and might lead to buffering which has an impact on performance
			 */
			this.connection.writeHttpObject(new DefaultHttpContent(value), this.connection.newPromise().addListener(future -> {
				if(future.isSuccess()) {
					this.request(1);
				}
			}));
		}
	}

	@Override
	protected void hookOnComplete() {
		if(this.mono || !this.many) {
			if(this.headers.isWritten()) {
				if(this.singleChunk == null) {
					this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
				}
				else {
					this.connection.writeHttpObject(new DefaultLastHttpContent(this.singleChunk));
					this.singleChunk = null;
				}
			}
			else {
				this.sanitizeRequest();
				this.connection.writeHttpObject(new FlatFullHttpRequest(this.connection.getVersion(), HttpMethod.valueOf(this.method.name()), this.path, this.headers.unwrap(), this.singleChunk != null ? this.singleChunk : Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE));
				this.headers.setWritten();
				this.singleChunk = null;
			}
		}
		else {
			this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
		}
		this.connection.onRequestSent();
	}

	@Override
	protected void hookOnCancel() {
		// Make sure the HTTP protocol flow is correct
		if(this.singleChunk != null) {
			this.singleChunk.release();
		}
		if(this.many) {
			this.connection.writeHttpObject(LastHttpContent.EMPTY_LAST_CONTENT);
		}
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		this.connection.onRequestError(throwable);
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
			Http1xRequest.this.transferredLength += value.count();
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
