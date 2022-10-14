@io.inverno.core.annotation.Module
module io.inverno.mod.http.client {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	
	requires jdk.unsupported; // required by netty for low level API for accessing direct buffers
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.codec.http2;
	requires io.netty.handler;
	
	requires transitive io.inverno.mod.base;
	requires io.inverno.mod.configuration;
	requires transitive io.inverno.mod.http.base;
	
	requires org.apache.logging.log4j;
	
	exports io.inverno.mod.http.client;
}
