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
package io.inverno.mod.http.server.internal.http1x.ws;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketFrame;
import io.inverno.mod.http.base.internal.ws.GenericWebSocketMessage;
import io.inverno.mod.http.base.ws.BaseWebSocketExchange;
import io.inverno.mod.http.base.ws.WebSocketException;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.inverno.mod.http.base.ws.WebSocketStatus;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import io.inverno.mod.http.server.ws.WebSocketExchangeHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * <p>
 * Generic {@link WebSocketExchange} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWebSocketExchange extends BaseSubscriber<WebSocketFrame> implements WebSocketExchange<ExchangeContext> {

	private static final Logger LOGGER = LogManager.getLogger(WebSocketExchange.class);
	
	private final ChannelHandlerContext context;
	private final Exchange<ExchangeContext> exchange;
	private final String subProtocol;
	private final GenericWebSocketFrame.GenericFactory frameFactory;
	private final GenericWebSocketMessage.GenericFactory messageFactory;
	
	private final boolean closeOnOutboundComplete;
	private final long inboundCloseFrameTimeout;
	
	private final EventExecutor contextExecutor;
	
	private Optional<Sinks.Many<WebSocketFrame>> inboundFrames;
	private GenericInbound inbound;
	private GenericOutbound outbound;
	
	private Sinks.One<Publisher<WebSocketFrame>> outboundFramesSinks;
	private Publisher<WebSocketFrame> outboundFrames;

	private boolean started;
	private boolean outboundFramesSet;
	private boolean inboundSubscribed;
	
	private boolean inClosed;
	private boolean outClosed;
	private ScheduledFuture<?> inboundCloseMessageTimeoutFuture;
	
	/**
	 * <p>
	 * Creates a generic WebSocket exchange.
	 * </p>
	 *
	 * @param context        the channel handler context
	 * @param exchange       the original exchange
	 * @param subProtocol    the negotiated subprotocol
	 * @param frameFactory   the WebSocket frame factory
	 * @param messageFactory the WebSocket message factory
	 * @param closeOnOutboundComplete  true to close WebSocket when outbound publisher completes, false otherwise
	 * @param inboundCloseFrameTimeout the time to wait for a close frame before closing the WebSocket unilatterally
	 */
	public GenericWebSocketExchange(
			ChannelHandlerContext context, 
			Exchange<ExchangeContext> exchange, 
			String subProtocol, 
			GenericWebSocketFrame.GenericFactory frameFactory, 
			GenericWebSocketMessage.GenericFactory messageFactory,
			boolean closeOnOutboundComplete,
			long inboundCloseFrameTimeout
		) {
		this.context = context;
		this.exchange = exchange;
		this.subProtocol = subProtocol;
		this.frameFactory = frameFactory;
		this.messageFactory = messageFactory;
		
		this.closeOnOutboundComplete = closeOnOutboundComplete;
		this.inboundCloseFrameTimeout = inboundCloseFrameTimeout;
		
		this.contextExecutor = this.context.executor();
		
		this.inboundFrames = Optional.empty();
	}
	
	/**
	 * <p>
	 * Starts the WebSocket exchange processing by subscribing to the deferred handler and then to the outbound frames publisher to start receiving and sending frames to the client.
	 * </p>
	 * 
	 * @param handler the WebSocket handler
	 */
	public void start(WebSocketExchangeHandler<ExchangeContext, WebSocketExchange<ExchangeContext>> handler) {
		// No need to synchronize this code since we are in an EventLoop
		if(this.started) {
			throw new IllegalStateException("WebSocket Exchange already started");
		}
		if(this.outboundFrames == null) {
			this.outboundFramesSinks = Sinks.one();
			this.outboundFrames = Flux.switchOnNext(this.outboundFramesSinks.asMono());
		}
		
		Mono<Void> deferHandle;
		try {
			deferHandle = handler.defer(this);
		}
		catch(Throwable throwable) {
			this.hookOnError(throwable);
			return;
		}
		deferHandle.thenMany(this.outboundFrames)
			.doOnDiscard(GenericWebSocketFrame.class, frame -> frame.release())
			.subscribe(this);
		this.started = true;
	}
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 *
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event loop, otherwise it is scheduled in the event loop.
	 * </p>
	 *
	 * <p>
	 * After the execution of the task, one event is requested to the response data subscriber.
	 * </p>
	 *
	 * @param runnable the task to execute
	 */
	protected void executeInEventLoop(Runnable runnable) {
		this.executeInEventLoop(runnable, 1);
	}
	
	/**
	 * <p>
	 * Executes the specified task in the event loop.
	 * </p>
	 *
	 * <p>
	 * The tasks is executed immediately when the current thread is in the event loop, otherwise it is scheduled in the event loop.
	 * </p>
	 *
	 * <p>
	 * After the execution of the task, the specified number of events is requested to the response data subscriber.
	 * </p>
	 *
	 * @param runnable the task to execute
	 * @param request  the number of events to request to the response data subscriber after the task completes
	 */
	protected void executeInEventLoop(Runnable runnable, int request) {
		if(this.contextExecutor.inEventLoop()) {
			runnable.run();
			this.request(request);
		}
		else {
			this.contextExecutor.execute(() -> {
				try {
					runnable.run();
					this.request(request);
				}
				catch (Throwable throwable) {
					this.cancel();
					this.hookOnError(throwable);
				}
			});
		}
	}

	@Override
	protected final void hookOnSubscribe(Subscription subscription) {
		this.onStart(subscription);
	}
	
	/**
	 * <p>
	 * Invokes when the WebSocket exchange is started.
	 * </p>
	 *
	 * <p>
	 * The default implementation basically request an unbounded amount of events to the subscription.
	 * </p>
	 *
	 * @param subscription the subscription to the response data publisher
	 */
	protected void onStart(Subscription subscription) {
		subscription.request(Long.MAX_VALUE);
		LOGGER.debug("WebSocket exchange started");
	}
	
	@Override
	protected void hookOnNext(WebSocketFrame value) {
		// do write the frame
		if(value.getKind() == WebSocketFrame.Kind.CLOSE) {
			throw new WebSocketException("Invalid outbound frame type " + value.getKind() + ", use close() to close the WebSocket");
		}
		this.executeInEventLoop(() -> {
			LOGGER.trace("Write {} frame (size={}, final={})", value.getKind(), value.getBinaryData().readableBytes(), value.isFinal());
			this.context.writeAndFlush(this.frameFactory.toUnderlyingWebSocketFrame(value));
		});
	}

	@Override
	protected void hookOnCancel() {
		this.close(WebSocketStatus.ENDPOINT_UNAVAILABLE);
	}

	@Override
	protected void hookOnComplete() {
		if(this.outbound != null && this.outbound.closeOnComplete) {
			this.close();
		}
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		// Close the WebSocket with error
		LOGGER.error("WebSocketExchange processing error", throwable);
		this.close(WebSocketStatus.INTERNAL_SERVER_ERROR, throwable.getMessage());
	}
	
	@Override
	public void dispose() {
		this.dispose(null);
	}
	
	public void dispose(Throwable error) {
		super.dispose();
		this.inboundFrames.ifPresent(frameSink -> {
			if(error != null) {
				frameSink.tryEmitError(error);
			}
			else {
				frameSink.tryEmitComplete();
			}
			if(!this.inboundSubscribed) {
				this.inboundSubscribed = true;
				frameSink.asFlux().subscribe(
					frame -> {
						((GenericWebSocketFrame)frame).release();
					},
					ex -> {
						// TODO Should be ignored but can be logged as debug or trace log
					}
				);
			}
		});
		this.inboundFrames = Optional.empty();
	}
	
	/**
	 * <p>
	 * Returns the inbound frames sink used by the {@link WebSocketProtocolHandler} to emit the frame received by the server from the client.
	 * </p>
	 * 
	 * <p>
	 * An empty optional is returned if the handler does not consume the frames received from the client, in which case the {@link WebSocketProtocolHandler} simply releases the received frames and
	 * discards them.
	 * </p>
	 * 
	 * @return an optional returning the sink or an empty optional if the handler was not interested in receiving frames from the client
	 */
	public Optional<Sinks.Many<WebSocketFrame>> inboundFrames() {
		return inboundFrames;
	}
	
	/**
	 * <p>
	 * Sets the outboind frame publisher.
	 * </p>
	 * 
	 * @param frames the frames to send to the client
	 */
	protected void setOutboundFrames(Publisher<WebSocketFrame> frames) {
		if(this.started && this.outboundFramesSet) {
			throw new IllegalStateException("Outbound frames already set");
		}

		if(this.outboundFramesSinks != null) {
			this.outboundFramesSinks.tryEmitValue(frames);
		}
		else {
			this.outboundFrames = frames;
		}
		this.outboundFramesSet = true;
	}
	
	/**
	 * <p>
	 * Finalizes the exchange by completing the inbound sink (if present).
	 * </p>
	 * 
	 * @param finalPromise a promise that completes with the final exchange operation 
	 * 
	 * @return the promise
	 */
	public ChannelFuture finalizeExchange(ChannelPromise finalPromise) {
		finalPromise.addListener(future -> {
			this.inboundFrames.ifPresent(Sinks.Many::tryEmitComplete);
		});
		return finalPromise;
	}

	@Override
	public Request request() {
		return this.exchange.request();
	}

	@Override
	public ExchangeContext context() {
		return this.exchange.context();
	}
	
	@Override
	public String getSubProtocol() {
		return this.subProtocol;
	}

	@Override
	public BaseWebSocketExchange.Inbound inbound() {
		if(this.inbound == null) {
			Sinks.Many<WebSocketFrame> inboundFrameSink = Sinks.many().unicast().onBackpressureBuffer();
			this.inbound = new GenericInbound(inboundFrameSink.asFlux()
				.doOnSubscribe(ign -> this.inboundSubscribed = true)
				.doOnDiscard(GenericWebSocketFrame.class, frame -> frame.release())
				.doOnTerminate(() -> {
					this.inbound = null;
					this.inboundFrames = Optional.empty();
				})
			);
			this.inboundFrames = Optional.of(inboundFrameSink);
		}
		return this.inbound;
	}

	@Override
	public BaseWebSocketExchange.Outbound outbound() {
		if(this.outbound == null) {
			this.outbound = new GenericOutbound();
		}
		return this.outbound;
	}

	@Override
	public void close(short code, String reason) {
		if(!this.outClosed) {
			this.executeInEventLoop(() -> {
				if(!this.outClosed) {
					this.outClosed = true;
					String cleanReason = reason;
					// 125 bytes is the limit: code is encoded on 2 bytes, reason must be 123 bytes
					if(cleanReason != null && cleanReason.length() >= 123) {
						cleanReason = new StringBuilder(cleanReason.substring(0, 120)).append("...").toString();
					}
					this.context.writeAndFlush(new CloseWebSocketFrame(code, cleanReason));
					LOGGER.debug("WebSocket close frame sent ({}): {}", code, reason);
					
					if(!this.inClosed) {
						this.inboundCloseMessageTimeoutFuture = this.contextExecutor.schedule(
							() -> {
								this.dispose(new WebSocketException("Inbound close frame timeout"));
								
								// Then close the channel
								ChannelPromise closePromise = this.context.newPromise();
								closePromise.addListener(ign -> LOGGER.debug("WebSocket closed ({}): {}", code, reason));
								this.finalizeExchange(closePromise);
								this.context.close(closePromise);
							},
							this.inboundCloseFrameTimeout, 
							TimeUnit.MILLISECONDS
						);
					}
				}
			});
		}
	}

	/**
	 * <p>
	 * Invoked when a WebSocket close frame is received.
	 * </p>
	 * 
	 * @param code
	 * @param reason 
	 */
	public void onCloseReceived(short code, String reason) {
		if(!this.inClosed) {
			LOGGER.debug("WebSocket close frame received ({}): {}", code, reason);
		}
		this.inClosed = true;
		if(this.inboundCloseMessageTimeoutFuture != null) {
			this.inboundCloseMessageTimeoutFuture.cancel(false);
			this.inboundCloseMessageTimeoutFuture = null;
		}
		
		// TODO we currently cancel output and send back the close frame, we should maybe try to delay this until the outbound is in a proper shape (i.e. if there's an inflight fragmented message,
		// wait for the final frame)
		
		// Cancel output and send a close frame back
		this.dispose();
		this.close(code, reason);
		
		// Then close the channel
		ChannelPromise closePromise = this.context.newPromise();
		closePromise.addListener(ign -> LOGGER.debug("WebSocket closed ({}): {}", code, reason));
		this.finalizeExchange(closePromise);
		this.context.close(closePromise);
	}
	
	/**
	 * <p>
	 * Generic {@link WebSocketExchange.Inbound} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	protected class GenericInbound implements BaseWebSocketExchange.Inbound {

		private final Flux<WebSocketFrame> frames;

		// This is OK because frames is a unicast publisher!!!
		private WebSocketFrame.Kind currentFrameKind;
		
		/**
		 * <p>
		 * Creates a generic WebSocket exchange inbound part.
		 * </p>
		 * 
		 * @param frames the inbound frames publisher
		 */
		public GenericInbound(Flux<WebSocketFrame> frames) {
			this.frames = frames;
		}
		
		@Override
		public Publisher<WebSocketFrame> frames() {
			return this.frames;
		}

		@Override
		public Publisher<WebSocketMessage> messages() {
			return this.frames
				// Only consider TEXT or BINARY frames for messages
				.filter(frame -> {
					WebSocketFrame.Kind kind = frame.getKind();
					return kind == WebSocketFrame.Kind.TEXT || kind == WebSocketFrame.Kind.BINARY || kind == WebSocketFrame.Kind.CONTINUATION;
				})
				.windowUntil(frame -> {
					if(this.currentFrameKind == null && frame.getKind() == WebSocketFrame.Kind.CONTINUATION) {
						// We can't receive a continuation frame before a non-final TEXT frame
						GenericWebSocketExchange.this.close(WebSocketStatus.PROTOCOL_ERROR);
					}
					this.currentFrameKind = frame.isFinal() ? null : frame.getKind();
					return frame.isFinal();
				})
				.map(messageFrames -> {
					WebSocketMessage message; 
					if(null == this.currentFrameKind) {
						// Should never happen
						throw new IllegalStateException();
					}
					else switch(this.currentFrameKind) {
						case TEXT: {
							return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, messageFrames);
						}
						case BINARY: {
							return new GenericWebSocketMessage(WebSocketMessage.Kind.BINARY, messageFrames);
						}
						default: {
							// Should never happen
							throw new IllegalStateException();
						}
					}
				});
		}

		@Override
		public Publisher<WebSocketMessage> textMessages() {
			return this.frames
				// Only consider TEXT or BINARY frames for messages
				.filter(frame -> {
					WebSocketFrame.Kind kind = frame.getKind();
					return kind == WebSocketFrame.Kind.TEXT || kind == WebSocketFrame.Kind.CONTINUATION;
				})
				.windowUntil(frame -> {
					if(this.currentFrameKind == null && frame.getKind() == WebSocketFrame.Kind.CONTINUATION) {
						// We can't receive a continuation frame before a non-final TEXT frame
						GenericWebSocketExchange.this.close(WebSocketStatus.PROTOCOL_ERROR);
					}
					this.currentFrameKind = frame.isFinal() ? null : frame.getKind();
					return frame.isFinal();
				})
				.map(messageFrames -> {
					return new GenericWebSocketMessage(WebSocketMessage.Kind.TEXT, messageFrames);
				});
		}

		@Override
		public Publisher<WebSocketMessage> binaryMessages() {
			return this.frames
				// Only consider TEXT or BINARY frames for messages
				.filter(frame -> {
					WebSocketFrame.Kind kind = frame.getKind();
					return kind == WebSocketFrame.Kind.BINARY || kind == WebSocketFrame.Kind.CONTINUATION;
				})
				.windowUntil(frame -> {
					if(this.currentFrameKind == null && frame.getKind() == WebSocketFrame.Kind.CONTINUATION) {
						// We can't receive a continuation frame before a non-final TEXT frame
						GenericWebSocketExchange.this.close(WebSocketStatus.PROTOCOL_ERROR);
					}
					this.currentFrameKind = frame.isFinal() ? null : frame.getKind();
					return frame.isFinal();
				})
				.map(messageFrames -> {
					return new GenericWebSocketMessage(WebSocketMessage.Kind.BINARY, messageFrames);
				});
		}
	}
	
	/**
	 * <p>
	 * Generic {@link WebSocketExchange.Outbound} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	protected class GenericOutbound implements BaseWebSocketExchange.Outbound {

		protected boolean closeOnComplete = GenericWebSocketExchange.this.closeOnOutboundComplete;

		@Override
		public BaseWebSocketExchange.Outbound closeOnComplete(boolean closeOnComplete) {
			this.closeOnComplete = closeOnComplete;
			return this;
		}
		
		@Override
		public void frames(Function<WebSocketFrame.Factory, Publisher<WebSocketFrame>> frames) {
			GenericWebSocketExchange.this.setOutboundFrames(frames.apply(GenericWebSocketExchange.this.frameFactory));
		}

		@Override
		public void messages(Function<WebSocketMessage.Factory, Publisher<WebSocketMessage>> messages) {
			GenericWebSocketExchange.this.setOutboundFrames(Flux.from(messages.apply(GenericWebSocketExchange.this.messageFactory)).flatMap(WebSocketMessage::frames));
		}
	}
}