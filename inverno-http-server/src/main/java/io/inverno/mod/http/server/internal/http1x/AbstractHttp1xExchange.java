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
package io.inverno.mod.http.server.internal.http1x;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.netty.FlatFullHttpResponse;
import io.inverno.mod.http.base.internal.netty.LinkedHttpHeaders;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.HttpServerConfiguration;
import io.inverno.mod.http.server.HttpServerException;
import io.inverno.mod.http.server.ServerController;
import io.inverno.mod.http.server.internal.AbstractExchange;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * <p>
 * Base Http/1.x {@link Exchange} implementation.
 * </p>
 * 
 * <p>
 * HTTP pipelining is implemented with a link list: the exchange has a {@link #next} exchange which is started by the connection on completion.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
abstract class AbstractHttp1xExchange extends AbstractExchange<Http1xRequest, Http1xResponse, Http1xErrorExchange> {
	
	/**
	 * The Http/1.x connection.
	 */
	protected final Http1xConnection connection;

	/**
	 * The HTTP version.
	 */
	protected final io.netty.handler.codec.http.HttpVersion version;

	/**
	 * The next exchange to process in the request pipeline.
	 */
	Http1xExchange next;
	
	protected boolean reset;

	/**
	 * <p>
	 * Creates an Http/1.x exchange.
	 * </p>
	 * 
	 * @param configuration the server configuration
	 * @param controller    the server controller
	 * @param connection    the Http/1.x connection
	 */
	public AbstractHttp1xExchange(
			HttpServerConfiguration configuration, 
			ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> controller, 
			Http1xConnection connection,
			boolean head,
			io.netty.handler.codec.http.HttpVersion version
		) {
		super(configuration, controller, head);
		this.connection = connection;
		this.version = version;
	}

	/**
	 * <p>
	 * Creates an Http/1.x exchange from the specified parent exchange.
	 * </p>
	 * 
	 * <p>
	 * This is used by {@link Http1xErrorExchange}.
	 * </p>
	 * 
	 * @param parentExchange the parent exchange
	 */
	protected AbstractHttp1xExchange(Http1xExchange parentExchange) {
		super(parentExchange);
		this.connection = parentExchange.connection;
		this.version = parentExchange.version;
		this.next = parentExchange.next;
		this.reset = parentExchange.reset;
	}
	
	/**
	 * <p>
	 * Returns the originating exchange or this exchange if this is the originating exchange (i.e. {@link Http1xExchange}).
	 * </p>
	 * 
	 * @return the originating exchange
	 */
	abstract Http1xExchange unwrap();

	/**
	 * <p>
	 * Determines whether the connection should be kept alive after processing the exchange.
	 * </p>
	 *
	 * @return true when the connection must be kept alive, false otherwise
	 */
	abstract boolean isKeepAlive();

	@Override
	public final HttpVersion getProtocol() {
		return this.version == io.netty.handler.codec.http.HttpVersion.HTTP_1_0 ? io.inverno.mod.http.base.HttpVersion.HTTP_1_0 : io.inverno.mod.http.base.HttpVersion.HTTP_1_1;
	}

	@Override
	public final void reset(long code) {
		if(this.connection.executor().inEventLoop()) {
			if(!this.reset) {
				this.reset = true;
				this.dispose(new HttpServerException("Exchange was reset: " + code));
				if(this.response().headers().isWritten()) {
					this.connection.shutdown().subscribe();
				}
				else {
					LinkedHttpHeaders httpHeaders = new LinkedHttpHeaders();
					if(this.version == io.netty.handler.codec.http.HttpVersion.HTTP_1_0) {
						if(this.isKeepAlive()) {
							httpHeaders.set((CharSequence)Headers.NAME_CONNECTION, (CharSequence)Headers.VALUE_KEEP_ALIVE);
						}
					}
					else if(!this.isKeepAlive()) {
						httpHeaders.set((CharSequence)Headers.NAME_CONNECTION, (CharSequence)Headers.VALUE_CLOSE);
					}
					httpHeaders.set((CharSequence)Headers.NAME_CONTENT_LENGTH, (CharSequence)"0");
					this.connection.writeHttpObject(new FlatFullHttpResponse(this.version , HttpResponseStatus.valueOf(Status.CANCELLED_REQUEST.getCode()), httpHeaders, Unpooled.EMPTY_BUFFER, null));

					this.connection.onExchangeComplete();
				}
			}
		}
		else {
			this.connection.executor().execute(() -> this.reset(code));
		}
	}
}
