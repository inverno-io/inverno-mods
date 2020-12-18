package io.winterframework.mod.web.internal.server.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Parameter;
import io.winterframework.mod.web.Part;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.RequestBodyDecoder;
import io.winterframework.mod.web.internal.server.AbstractExchange;

public class Http2ChannelHandler extends Http2ConnectionHandler implements Http2FrameListener, Http2Connection.Listener {

	private ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler; 
	private ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler; 
	private HeaderService headerService;
	private RequestBodyDecoder<Parameter> urlEncodedBodyDecoder;
	private RequestBodyDecoder<Part> multipartBodyDecoder;
	
	private IntObjectMap<Http2Exchange> serverStreams;
	
    public Http2ChannelHandler(
    		Http2ConnectionDecoder decoder, 
    		Http2ConnectionEncoder encoder, 
    		Http2Settings initialSettings, 
    		ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> rootHandler, 
			ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> errorHandler, 
			HeaderService headerService, 
			RequestBodyDecoder<Parameter> urlEncodedBodyDecoder, 
			RequestBodyDecoder<Part> multipartBodyDecoder) {
        super(decoder, encoder, initialSettings);
        
        this.rootHandler = rootHandler;
        this.errorHandler = errorHandler;
        this.headerService = headerService;
        this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
        this.multipartBodyDecoder = multipartBodyDecoder;
        
        this.serverStreams = new IntObjectHashMap<>();
        this.connection().addListener(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//    	System.out.println("error");
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        ctx.close();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    	super.channelInactive(ctx);
//    	System.out.println("Channel inactive");
    }
    
    @Override
    public void onError(ChannelHandlerContext ctx, boolean outbound, Throwable cause) {
    	super.onError(ctx, outbound, cause);
    	ctx.close();
    }
    
    /**
     * If receive a frame with end-of-stream set, send a pre-canned response.
     */
    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
//        System.out.println("onDataRead() " + streamId + " - "+ endOfStream);
        
        // TODO flow control?
        int processed = data.readableBytes() + padding;
        
        Http2Exchange serverStream = this.serverStreams.get(streamId);
    	if(serverStream != null) {
    		serverStream.request().data().ifPresent(sink -> sink.tryEmitNext(data));
            if(endOfStream) {
            	serverStream.request().data().ifPresent(sink -> sink.tryEmitComplete());
            }
    	}
    	else {
    		// TODO this should never happen?
    		throw new IllegalStateException("Unable to push data to unmanaged stream " + streamId);
    	}
        
        /*System.out.println("=================================");
        System.out.println(data.toString(CharsetUtil.UTF_8));
        System.out.println("=================================");*/
        
//        ByteBuf message = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Server Response B : Version - HTTP/2", CharsetUtil.UTF_8));
//        encoder().writeData(ctx,streamId, message, padding, endOfStream, ctx.newPromise());
        return processed;
    }
    
    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
//        System.out.println("onHeaderReads(2) " + streamId + " - " + endOfStream + " - " + this.hashCode());
    	Http2Exchange exchange = this.serverStreams.get(streamId);
    	if(exchange == null) {
			Http2Exchange streamExchange = new Http2Exchange(ctx, this.connection().stream(streamId), headers, this.encoder(), this.headerService, this.urlEncodedBodyDecoder, this.multipartBodyDecoder, this.rootHandler, this.errorHandler);
			this.serverStreams.put(streamId, streamExchange);
			if(endOfStream) {
				streamExchange.request().data().ifPresent(sink -> sink.tryEmitComplete());
            }
			streamExchange.start(new AbstractExchange.Handler() {
				@Override
				public void exchangeError(ChannelHandlerContext ctx, Throwable t) {
					Http2ChannelHandler.this.resetStream(ctx, streamId, Http2Error.INTERNAL_ERROR.code(), ctx.voidPromise());
				}
			});
    	}
    	else {
    		// Continuation frame
    		((Http2RequestHeaders)exchange.request().headers()).getHttpHeaders().add(headers);
    	}
    }
    
    @Override
    public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
//        System.out.println("onHeaderReads(1)");
        onHeadersRead(ctx, streamId, headers, padding, endOfStream);
    }

    @Override
    public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {
        //System.out.println("onPriorityRead()");
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
//        System.out.println("onRstStreamRead()");
    	Http2Exchange serverStream = this.serverStreams.remove(streamId);
    	if(serverStream != null) {
    		serverStream.dispose();
    	}
    	else {
    		// TODO this should never happen?
    		System.err.println("Unable to reset unmanaged stream " + streamId);
//    		throw new IllegalStateException("Unable to reset unmanaged stream " + streamId);
    	}
    }

    @Override
    public void onSettingsAckRead(ChannelHandlerContext ctx) throws Http2Exception {
//        System.out.println("onSettingsAckRead()");
    }

    @Override
    public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
//        System.out.println("onSettingsRead()");
    }

    @Override
    public void onPingRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
//        System.out.println("onPingRead()");
    }

    @Override
    public void onPingAckRead(ChannelHandlerContext ctx, long data) throws Http2Exception {
//        System.out.println("onPingAckRead()");
    }

    @Override
    public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception {
//        System.out.println("onPushPromiseRead()");
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception {
//        System.out.println("onGoAwayRead()");
    }

    @Override
    public void onWindowUpdateRead(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement) throws Http2Exception {
//        System.out.println("onWindowUpdateRead()");
    }

    @Override
    public void onUnknownFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload) throws Http2Exception {
//        System.out.println("onUnknownFrame()");
    }

	@Override
	public void onStreamAdded(Http2Stream stream) {
//		System.out.println("Stream added");		
	}

	@Override
	public void onStreamActive(Http2Stream stream) {
//		System.out.println("Stream active");		
	}

	@Override
	public void onStreamHalfClosed(Http2Stream stream) {
//		System.out.println("Stream half closed");		
	}

	@Override
	public void onStreamClosed(Http2Stream stream) {
//		System.out.println("Stream closed");
		Http2Exchange serverStream = this.serverStreams.remove(stream.id());
    	if(serverStream != null) {
    		serverStream.dispose();
    	}
    	else {
    		// TODO this should never happen?
    		System.err.println("Unable to reset unmanaged stream " + stream.id());
//    		throw new IllegalStateException("Unable to reset unmanaged stream " + stream.id());
    	}
	}

	@Override
	public void onStreamRemoved(Http2Stream stream) {
//		System.out.println("Stream removed");
	}

	@Override
	public void onGoAwaySent(int lastStreamId, long errorCode, ByteBuf debugData) {
//		System.out.println("Stream go away sent");		
	}

	@Override
	public void onGoAwayReceived(int lastStreamId, long errorCode, ByteBuf debugData) {
//		System.out.println("Stream go away received");		
	}
	
}