@io.winterframework.core.annotation.Module
module io.winterframework.mod.commons {
	requires io.winterframework.core;
	requires io.winterframework.mod.configuration;
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	
	requires transitive io.netty.buffer;
	requires transitive io.netty.transport;
	requires io.netty.common;
	
	requires static io.netty.transport.epoll;
	requires static io.netty.transport.unix.common;
	requires static io.netty.transport.kqueue;
	
	exports io.winterframework.mod.commons;
	exports io.winterframework.mod.commons.net;
	exports io.winterframework.mod.commons.resource;
}