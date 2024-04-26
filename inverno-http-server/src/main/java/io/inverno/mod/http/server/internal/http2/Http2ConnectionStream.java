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
package io.inverno.mod.http.server.internal.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.concurrent.EventExecutor;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * <p>
 * An Http/2 connection stream.
 * </p>
 * 
 * <p>
 * This is used as a proxy between the exchange and the connection and abstracts the stream to the exchange.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
class Http2ConnectionStream {

	private final Http2Connection connection;
	private final ChannelHandlerContext channelContext;
	private final Http2Stream stream;
	
	/**
	 * The exchange associated to the stream which can be replaced by an {@link Http2ErrorExchange} in case of error while processing the exchange.
	 */
	AbstractHttp2Exchange exchange;

	/**
	 * <p>
	 * Creates an Http/2 connection stream.
	 * </p>
	 *
	 * @param connection     the Http/2 connection
	 * @param channelContext the channel handler context
	 * @param stream         the originating stream
	 */
	public Http2ConnectionStream(Http2Connection connection, ChannelHandlerContext channelContext, Http2Stream stream) {
		this.connection = connection;
		this.channelContext = channelContext;
		this.stream = stream;
	}
	
	/**
	 * <p>
	 * Returns the event loop associated to the connection.
	 * </p>
	 * 
	 * @return the connection event loop
	 */
	public EventExecutor executor() {
		return this.channelContext.executor();
	}
	
	/**
	 * <p>
	 * Returns a new channel promise.
	 * </p>
	 * 
	 * @return a new channel promise
	 */
	public ChannelPromise newPromise() {
		return this.channelContext.newPromise();
	}
	
	/**
	 * <p>
	 * Returns the void promise.
	 * </p>
	 * 
	 * @return the void promise
	 */
	public ChannelPromise voidPromise() {
		return this.channelContext.voidPromise();
	}
	
	/**
	 * <p>
	 * Returns the local socket address of the connection.
	 * </p>
	 * 
	 * @return a socket address
	 */
	public SocketAddress getLocalAddress() {
		return this.connection.getLocalAddress();
	}

	/**
	 * <p>
	 * Returns the certificates that were sent to the remote peer during handshaking.
	 * </p>
	 * 
	 * @return an optional returning the list of local certificates or an empty optional if no certificates were sent.
	 */
	public Optional<Certificate[]> getLocalCertificates() {
		return this.connection.getLocalCertificates();
	}

	/**
	 * <p>
	 * Returns the remote socket address of the client or last proxy that opened the connection.
	 * </p>
	 * 
	 * @return a socket address
	 */
	public SocketAddress getRemoteAddress() {
		return this.connection.getRemoteAddress();
	}

	/**
	 * <p>
	 * Returns the certificates that were received from the remote peer during handshaking.
	 * </p>
	 * 
	 * @return an optional returning the list of remote certificates or an empty optional if no certificates were received.
	 */
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.connection.getRemoteCertificates();
	}

	/**
	 * <p>
	 * Resets the stream.
	 * </p>
	 * 
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param errorCode the error code indicating the nature of the failure
	 */
	public void resetStream(long errorCode) {
		this.resetStream(errorCode, this.channelContext.voidPromise());
    }
	
	/**
	 * <p>
	 * Resets the stream.
	 * </p>
	 * 
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param errorCode the error code indicating the nature of the failure
	 * @param promise   a promise
	 */
    public void resetStream(long errorCode, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.resetStream(this.channelContext, this.stream.id(), errorCode, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.resetStream(errorCode, promise));
		}
    }
	
	/**
	 * <p>
	 * Writes a {@code HEADERS} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param headers   the headers to be sent
	 * @param padding   additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive)
	 * @param endStream indicates if this is the last frame to be sent for the stream
	 */
	public void writeHeaders(Http2Headers headers, int padding, boolean endStream) {
		this.writeHeaders(headers, padding, endStream, this.channelContext.voidPromise());
	}
	
	/**
	 * <p>
	 * Writes a {@code HEADERS} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param headers   the headers to send
	 * @param padding   additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive)
	 * @param endStream indicates if this is the last frame to be sent for the stream
	 * @param promise   a promise
	 */
	public void writeHeaders(Http2Headers headers, int padding, boolean endStream, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			 this.connection.encoder().writeHeaders(this.channelContext, this.stream.id(), headers, padding, endStream, promise);
			 this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writeHeaders(headers, padding, endStream, promise));
		}
	}

	/**
	 * <p>
	 * Writes a {@code HEADERS} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param headers          the headers to send
	 * @param streamDependency the stream on which this stream should depend, or 0 if it should depend on the connection
	 * @param weight           the weight for this stream
	 * @param exclusive        whether this stream should be the exclusive dependant of its parent
	 * @param padding          additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive)
	 * @param endStream        indicates if this is the last frame to be sent for the stream
	 */
	public void writeHeaders(Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) {
		this.writeHeaders(headers, streamDependency, weight, exclusive, padding, endStream, this.channelContext.voidPromise());
	}
	
	/**
	 * <p>
	 * Writes a {@code HEADERS} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param headers          the headers to send
	 * @param streamDependency the stream on which this stream should depend, or 0 if it should depend on the connection
	 * @param weight           the weight for this stream
	 * @param exclusive        whether this stream should be the exclusive dependant of its parent
	 * @param padding          additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive)
	 * @param endStream        indicates if this is the last frame to be sent for the stream
	 * @param promise          a promise
	 */
	public void writeHeaders(Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.encoder().writeHeaders(this.channelContext, this.stream.id(), headers, streamDependency, weight, exclusive, padding, endStream, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writeHeaders(headers, streamDependency, weight, exclusive, padding, endStream, promise));
		}
	}
	
	/**
	 * <p>
	 * Writes a {@code DATA} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param data      the payload of the frame. This will be released by this method
	 * @param padding   additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive). A 1 byte padding is encoded as just the pad length field with
	 *                  value 0. A 256 byte padding is encoded as the pad length field with value 255 and 255 padding bytes appended to the end of the frame
	 * @param endStream indicates if this is the last frame to be sent for the stream
	 */
	public void writeData(ByteBuf data, int padding, boolean endStream) {
		this.writeData(data, padding, endStream, this.channelContext.voidPromise());
	}
	
	/**
	 * <p>
	 * Writes a {@code DATA} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param data      the payload of the frame. This will be released by this method
	 * @param padding   additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive). A 1 byte padding is encoded as just the pad length field with
	 *                  value 0. A 256 byte padding is encoded as the pad length field with value 255 and 255 padding bytes appended to the end of the frame
	 * @param endStream indicates if this is the last frame to be sent for the stream
	 * @param promise   a promise
	 */
	public void writeData(ByteBuf data, int padding, boolean endStream, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.encoder().writeData(this.channelContext, this.stream.id(), data, padding, endStream, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writeData(data, padding, endStream, promise));
		}
	}
	
	/**
	 * <p>
	 * Writes a {@code PRIORITY} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param streamDependency the stream on which this stream should depend, or 0 if it should depend on the connection
	 * @param weight           the weight for this stream
	 * @param exclusive        whether this stream should be the exclusive dependant of its parent
	 */
	public void writePriority(int streamDependency, short weight, boolean exclusive) {
		this.writePriority(streamDependency, weight, exclusive, this.channelContext.voidPromise());
	}

	/**
	 * <p>
	 * Writes a {@code PRIORITY} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param streamDependency the stream on which this stream should depend, or 0 if it should depend on the connection
	 * @param weight           the weight for this stream
	 * @param exclusive        whether this stream should be the exclusive dependant of its parent
	 * @param promise          a promise
	 */
	public void writePriority(int streamDependency, short weight, boolean exclusive, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.encoder().writePriority(this.channelContext, this.stream.id(), streamDependency, weight, exclusive, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writePriority(streamDependency, weight, exclusive, promise));
		}
	}

	/**
	 * <p>
	 * Writes a {@code RST_STREAM} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param errorCode the error code indicating the nature of the failure
	 */
	public void writeRstStream(long errorCode) {
		this.writeRstStream(errorCode, this.channelContext.voidPromise());
	}
	
	/**
	 * <p>
	 * Writes a {@code RST_STREAM} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param errorCode the error code indicating the nature of the failure
	 * @param promise   a promise
	 */
	public void writeRstStream(long errorCode, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.encoder().writeRstStream(this.channelContext, this.stream.id(), errorCode, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writeRstStream(errorCode, promise));
		}
	}
	
	/**
	 * <p>
	 * Writes a {@code PUSH_PROMISE} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param promisedStreamId the ID of the promised stream
	 * @param headers          the headers to send
	 * @param padding          additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive)
	 */
	public void writePushPromise(int promisedStreamId, Http2Headers headers, int padding) {
		this.writePushPromise(promisedStreamId, headers, padding, this.channelContext.voidPromise());
	}

	/**
	 * <p>
	 * Writes a {@code PUSH_PROMISE} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param promisedStreamId the ID of the promised stream
	 * @param headers          the headers to send
	 * @param padding          additional bytes that should be added to obscure the true content size. Must be between 0 and 256 (inclusive)
	 * @param promise          a promise
	 */
	public void writePushPromise(int promisedStreamId, Http2Headers headers, int padding, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.encoder().writePushPromise(this.channelContext, this.stream.id(), promisedStreamId, headers, padding, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writePushPromise(promisedStreamId, headers, padding, promise));
		}
	}
	
	/**
	 * <p>
	 * Writes a {@code WINDOW_UPDATE} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param windowSizeIncrement the number of bytes by which the local inbound flow control window is increasing
	 */
	public void writeWindowUpdate(int windowSizeIncrement) {
		this.writeWindowUpdate(windowSizeIncrement, this.channelContext.voidPromise());
	}

	/**
	 * <p>
	 * Writes a {@code WINDOW_UPDATE} frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 * 
	 * @param windowSizeIncrement the number of bytes by which the local inbound flow control window is increasing
	 * @param promise             a promise
	 */
	public void writeWindowUpdate(int windowSizeIncrement, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.encoder().writeWindowUpdate(this.channelContext, this.stream.id(), windowSizeIncrement, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writeWindowUpdate(windowSizeIncrement, promise));
		}
	}

	/**
	 * <p>
	 * Writes a frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param frameType the frame type identifier
	 * @param flags     the flags to write for this frame
	 * @param payload   the payload to write for this frame. This will be released by this method
	 */
	public void writeFrame(byte frameType, Http2Flags flags, ByteBuf payload) {
		this.writeFrame(frameType, flags, payload, this.channelContext.voidPromise());
	}
	
	/**
	 * <p>
	 * Writes a frame.
	 * </p>
	 *
	 * <p>
	 * The operation is always executed on the connection event loop.
	 * </p>
	 *
	 * @param frameType the frame type identifier
	 * @param flags     the flags to write for this frame
	 * @param payload   the payload to write for this frame. This will be released by this method
	 * @param promise   a promise
	 */
	public void writeFrame(byte frameType, Http2Flags flags, ByteBuf payload, ChannelPromise promise) {
		if(this.channelContext.executor().inEventLoop()) {
			this.connection.encoder().writeFrame(this.channelContext, frameType, this.stream.id(), flags, payload, promise);
			this.flush();
		}
		else {
			this.channelContext.executor().execute(() -> this.writeFrame(frameType, flags, payload, promise));
		}
	}
	
	/**
	 * <p>
	 * Flushed the channel when it can be flushed.
	 * </p>
	 */
	private void flush() {
		if(!this.connection.read) {
			this.channelContext.channel().flush();
		}
	}
	
	/**
	 * <p>
	 * Callback method that must be invoked when the processing of the current responding exchange completes.
	 * </p>
	 */
	public void onExchangeComplete() {
		// We must dispose the request body
		if(this.channelContext.executor().inEventLoop()) {
			this.exchange.request().dispose(null);
		}
		else {
			this.channelContext.executor().execute(this::onExchangeComplete);
		}
	}
	
	/**
	 * <p>
	 * Callback method that must be invoked when an error is raised during the processing of the current responding exchange.
	 * </p>
	 * 
	 * <p>
	 * This method executes on the connection event loop and delegates the error handling to the responding exchange (see {@link AbstractHttp2Exchange#handleError(java.lang.Throwable) }).
	 * </p>
	 * 
	 * @param throwable the error
	 */
	public void onExchangeError(Throwable error) {
		if(this.channelContext.executor().inEventLoop()) {
			this.exchange.handleError(error);
		}
		else {
			this.channelContext.executor().execute(() -> this.onExchangeError(error));
		}
	}
}
