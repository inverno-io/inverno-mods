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
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.internal.AbstractRequest;
import io.inverno.mod.http.client.internal.EndpointRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Http/2 {@link Request} implementation
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public class Http2Request extends AbstractRequest<Http2RequestHeaders> {
	
	private final Http2ConnectionStream connectionStream;
	
	private String scheme;
	private final Http2RequestBody body;
	
	private Disposable disposable;
	
	
	/**
	 * <p>
	 * Creates an Http/2 request.
	 * </p>
	 * 
	 * @param headerService the header service
	 * @param parameterConverter the parameter converter
	 * @param connectionStream the Http/2 connection stream
	 * @param endpointRequest the endpoint request
	 * @param validateHeaders true to validate headers, false otherwise
	 */
	Http2Request(HeaderService headerService, ObjectConverter<String> parameterConverter, Http2ConnectionStream connectionStream, EndpointRequest endpointRequest, boolean validateHeaders) {
		super(
			parameterConverter, 
			endpointRequest,
			new Http2RequestHeaders(headerService, parameterConverter, endpointRequest.getHeaders(), validateHeaders), 
			endpointRequest.getAuthority() == null ? resolveAuthority(connectionStream.getRemoteAddress(), connectionStream.isTls()) : endpointRequest.getAuthority()
		);
		this.connectionStream = connectionStream;
		this.body = endpointRequest.getBody() != null ? new Http2RequestBody(endpointRequest.getBody()) : null;
		
		this.headers.unwrap()
			.method(endpointRequest.getMethod().name())
			.scheme(this.getScheme())
			.authority(this.authority)
			.path(endpointRequest.getPath());
	}

	@Override
	public void send() {
		if(this.connectionStream.executor().inEventLoop()) {
			if(this.body != null) {
				if(this.headers.contains(Headers.NAME_EXPECT, Headers.VALUE_100_CONTINUE)) {
					this.connectionStream.writeHeaders(this.headers.unwrap(), 0, false);
					this.headers.setWritten();
				}
				else {
					this.sendBody();
				}
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
	 * Sends the request body.
	 * </p>
	 */
	void sendBody() {
		Publisher<ByteBuf> data = this.body.getData();
		this.mono = data instanceof Mono;
		data.subscribe(this);
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

	private boolean mono;
	private ByteBuf singleChunk;
	private boolean many;

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
		else if(!this.connectionStream.isReset()) {
			this.many = true;
			if(!this.headers.isWritten()) {
				this.connectionStream.writeHeaders(this.headers.unwrap(), 0, false);
				this.headers.setWritten();
				this.connectionStream.writeData(this.singleChunk, 0, false);
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
			this.connectionStream.writeData(value, 0, false, this.connectionStream.newPromise().addListener(future -> {
				if(future.isSuccess()) {
					this.request(1);
				}
			}));
		}
	}

	@Override
	protected void hookOnComplete() {
		if(!this.connectionStream.isReset()) {
			if(this.mono || !this.many) {
				if(this.headers.isWritten()) {
					if(this.singleChunk == null) {
						this.connectionStream.writeData(Unpooled.EMPTY_BUFFER, 0, true);
					}
					else {
						this.connectionStream.writeData(this.singleChunk, 0, true);
						this.singleChunk = null;
					}
				}
				else {
					if(!this.headers.contains(Headers.NAME_CONTENT_LENGTH)) {
						this.headers.contentLength(this.transferredLength);
					}
					if(this.singleChunk == null) {
						this.connectionStream.writeHeaders(this.headers.unwrap(), 0, true);
						this.headers.setWritten();
					}
					else {
						this.connectionStream.writeHeaders(this.headers.unwrap(), 0, false);
						this.headers.setWritten();
						this.connectionStream.writeData(this.singleChunk, 0, true);
						this.singleChunk = null;
					}
				}
			}
			else {
				this.connectionStream.writeData(Unpooled.EMPTY_BUFFER, 0, true);
			}
		}
	}

	@Override
	protected void hookOnCancel() {
		if(this.singleChunk != null) {
			this.singleChunk.release();
		}
		if(this.many && !this.connectionStream.isReset()) {
			// Make sure the HTTP protocol flow is correct
			this.connectionStream.writeData(Unpooled.EMPTY_BUFFER, 0, true);
		}
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		if(!this.connectionStream.isReset()) {
			this.connectionStream.onRequestError(throwable);
		}
	}
}
