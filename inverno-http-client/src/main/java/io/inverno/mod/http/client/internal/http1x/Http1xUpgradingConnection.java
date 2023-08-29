/*
 * Copyright 2022 Jeremy KUHN
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
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientException;
import io.inverno.mod.http.client.HttpClientUpgradeException;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.EndpointChannelConfigurer;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoSink;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
class Http1xUpgradingConnection extends Http1xConnection {

	private static final int MAX_MESSAGE_BUFFER_SIZE = 65536;
	
	private enum UpgradeState {
		STARTED,
		RECEIVED,
		FULLY_RECEIVED,
		PREPARED,
		COMPLETED;
	}
	
	private final EndpointChannelConfigurer configurer;
	
	private Http1xUpgradingExchange upgradingExchange;
	
	private UpgradeState state;
	private boolean requestComplete;
	
	private Deque<Object> messageBuffer;
	private int messageBufferSize;
	
	public Http1xUpgradingConnection(
			HttpClientConfiguration configuration, 
			HttpVersion httpVersion, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder, 
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory,
			EndpointChannelConfigurer configurer
		) {
		super(configuration, httpVersion, headerService, parameterConverter, urlEncodedBodyEncoder, multipartBodyEncoder, partFactory);
		this.configurer = configurer;
	}

	@Override
	public Long getMaxConcurrentRequests() {
		// Before we have determined whether the connection can be upgraded we can only accept one request which will be used for the upgrade
		if(this.state == UpgradeState.COMPLETED) {
			return super.getMaxConcurrentRequests();
		}
		else {
			return 1l;
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		switch(this.state) {
			case STARTED: {
				if (msg instanceof HttpResponse) {
					HttpResponse response = (HttpResponse)msg;
					if(!HttpResponseStatus.SWITCHING_PROTOCOLS.equals(response.status())) {
						// upgrade rejected by server
						super.channelRead(ctx, msg);
						this.rejectUpgrade();
					}
					else {
						CharSequence upgradeHeader = response.headers().get(HttpHeaderNames.UPGRADE);
						if (upgradeHeader != null && !AsciiString.contentEqualsIgnoreCase(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, upgradeHeader)) {
							throw new HttpClientUpgradeException("Invalid HTTP/2 upgrade protocol: " + upgradeHeader);
						}
						this.state = UpgradeState.RECEIVED;
					}
				}
				else {
					// This should not be a valid state let's fail fast
					throw new IllegalStateException();
				}
				break;
			}
			case RECEIVED: {
				if(msg instanceof LastHttpContent) {
					LastHttpContent content = (LastHttpContent)msg;
					if(content.content().isReadable()) {
						throw new HttpClientUpgradeException("HTTP/2 upgrade protocol error");
					}
					if(this.requestComplete) {
						this.acceptUpgrade();
					}
					else {
						this.state = UpgradeState.FULLY_RECEIVED;
					}
				}
				else {
					throw new HttpClientUpgradeException("HTTP/2 upgrade protocol error");
				}
				break;
			}
			case FULLY_RECEIVED: {
				// we must buffer until we have sent the request body
				if(this.requestComplete) {
					this.acceptUpgrade();
					this.context.fireChannelRead(msg);
				}
				else {
					this.bufferMessage(msg);
				}
				break;
			}
			case PREPARED: {
				this.bufferMessage(msg);
				break;
			}
			case COMPLETED: {
				super.channelRead(ctx, msg);
				break;
			}
		}
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		super.handlerRemoved(ctx);
		
		if(this.messageBuffer != null) {
			Object current;
			while( (current = this.messageBuffer.poll()) != null) {
				ReferenceCountUtil.release(current);
			}
			this.messageBuffer = null;
		}
	}
	
	private void bufferMessage(Object msg) {
		if(this.messageBuffer == null) {
			this.messageBuffer = new ArrayDeque<>();
		}
		
		if(msg instanceof ByteBufHolder) {
			this.messageBufferSize += ((ByteBufHolder)msg).content().readableBytes();
		}
		else if(msg instanceof ByteBuf) {
			this.messageBufferSize += ((ByteBuf)msg).readableBytes();
		}
		this.messageBuffer.add(msg);
		
		if(this.messageBufferSize >= MAX_MESSAGE_BUFFER_SIZE) {
			throw new TooLongFrameException("Message buffer overflow: >" + MAX_MESSAGE_BUFFER_SIZE);
		}
	}
	
	private void rejectUpgrade() {
		this.upgradingExchange.getUpgradedExchangeSink().success(this.upgradingExchange);
		this.state = UpgradeState.COMPLETED;
		// Make sure the capacity is updated
		if(this.handler != null) {
			this.handler.onUpgrade(this);
		}
		this.upgradingExchange = null;
	}

	private void acceptUpgrade() {
		this.state = UpgradeState.PREPARED;
		this.configurer.completeHttp2Upgrade(this.context.pipeline(), configuration, this.upgradingExchange, this.messageBuffer);
		this.messageBuffer = null;
		this.messageBufferSize = 0;
		this.state = UpgradeState.COMPLETED;
		if(this.handler != null) {
			this.handler.onUpgrade(this.upgradingExchange.getUpgradedConnection());
		}
		this.upgradingExchange = null;
	}

	@Override
	protected Http1xExchange createExchange(ChannelHandlerContext context, MonoSink<Exchange<ExchangeContext>> exchangeSink, ExchangeContext exchangeContext, Http1xRequest request, Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer, Http1xConnectionEncoder encoder) throws HttpClientException {
		if(this.state == UpgradeState.COMPLETED) {
			return super.createExchange(context, exchangeSink, exchangeContext, request, responseBodyTransformer, encoder);
		}
		else if(this.upgradingExchange == null) {
			this.state = UpgradeState.STARTED;
			this.upgradingExchange = new Http1xUpgradingExchange(context, exchangeSink, exchangeContext, this.httpVersion, request, responseBodyTransformer, this);
			this.configurer.startHttp2Upgrade(context.pipeline(), this.configuration, this.upgradingExchange);
			
			return this.upgradingExchange;
		}
		else {
			throw new HttpClientUpgradeException("HTTP/2 upgrade already in progress");
		}
	}
	
	@Override
	public void requestComplete(Http1xExchange exchange) {
		super.requestComplete(exchange);
		this.requestComplete = true;
		if(this.state == UpgradeState.FULLY_RECEIVED) {
			this.acceptUpgrade();
		}
	}
	
	@Override
	public void exchangeError(Http1xExchange exchange, Throwable t) {
		super.exchangeError(exchange, t);
		if(this.state != UpgradeState.COMPLETED) {
			// Let's be conservative here, if there was an error in the upgrading exchange let's just close the connection even we could recover
			this.close().subscribe();
		}
	}
}
