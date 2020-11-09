package io.winterframework.mod.web.internal.server.http2;

import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Flags;
import io.netty.handler.codec.http2.Http2FrameListener;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.CharsetUtil;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Strategy;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.core.annotation.Lazy;
import io.winterframework.core.annotation.Wrapper;

public class Http2ChannelHandler extends Http2ConnectionHandler implements Http2FrameListener, Http2Connection.Listener {

	private Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier;
	
	private IntObjectMap<Http2ServerStream<?>> serverStreams;
	
    public Http2ChannelHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings, Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier) {
        super(decoder, encoder, initialSettings);
        this.http2ServerStreamBuilderSupplier = http2ServerStreamBuilderSupplier;
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
    
    /**
     * If receive a frame with end-of-stream set, send a pre-canned response.
     */
    @Override
    public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
//        System.out.println("onDataRead() " + streamId + " - "+ endOfStream);
        
        // TODO flow control?
        int processed = data.readableBytes() + padding;
        
        Http2ServerStream<?> serverStream = this.serverStreams.get(streamId);
    	if(serverStream != null) {
    		serverStream.request().data().ifPresent(emitter -> emitter.next(data));
            if(endOfStream) {
            	serverStream.request().data().ifPresent(emitter -> emitter.complete());
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
        if(headers.path().toString().equalsIgnoreCase("/favicon.ico")){
            ByteBuf message = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("NoImages", CharsetUtil.UTF_8));
            encoder().writeData(ctx,streamId, message, 0, endOfStream, ctx.newPromise());
        }
        else {
        	if(!this.serverStreams.containsKey(streamId)) {
        		this.http2ServerStreamBuilderSupplier.get()
    				.stream(this.connection().stream(streamId))
    				.headers(headers)
    				.encoder(this.encoder())
    				.build(ctx)
    				.flatMap(serverStream -> {
    					return serverStream.init()
    						.doOnSubscribe(subscription -> {
    							this.serverStreams.put(streamId, serverStream);
    							if(endOfStream) {
       			                	serverStream.request().data().ifPresent(emitter -> emitter.complete());
       			                }
    						});
    				}).subscribe();
        	}
        	else {
        		// TODO HTTP trailers...
        	}
        }
    	
    	/*Http2Headers responseHeaders = new DefaultHttp2Headers();
    	responseHeaders.status("200");
    	encoder().writeHeaders(ctx, streamId, responseHeaders, padding, true, ctx.newPromise());
    	
    	ByteBuf message = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Server Response : Version - HTTP/2", CharsetUtil.UTF_8));
        encoder().writeData(ctx, streamId, message, padding, endOfStream, ctx.newPromise());
        ctx.channel().flush();*/
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
    	Http2ServerStream<?> serverStream = this.serverStreams.remove(streamId);
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
		Http2ServerStream<?> serverStream = this.serverStreams.remove(stream.id());
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
	
	@Bean(strategy = Strategy.PROTOTYPE, visibility = Visibility.PRIVATE)
	@Wrapper
	public static class Htt2ChannelHandlerWrapper implements Supplier<Http2ChannelHandler> {

		private Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier;
		
		public Htt2ChannelHandlerWrapper(@Lazy Supplier<Http2ServerStreamBuilder> http2ServerStreamBuilderSupplier) {
			this.http2ServerStreamBuilderSupplier = http2ServerStreamBuilderSupplier;
		}
		
		@Override
		public Http2ChannelHandler get() {
			return new Http2ChannelHandlerBuilder(this.http2ServerStreamBuilderSupplier).build();
		}
		
	}
}