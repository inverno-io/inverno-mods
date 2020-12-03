@io.winterframework.core.annotation.Module
module io.winterframework.mod.web {
	requires io.winterframework.core;
	requires io.winterframework.mod.commons;
	requires io.winterframework.mod.configuration;
	
	requires org.apache.logging.log4j;
	requires com.fasterxml.jackson.databind;
	
	requires jdk.unsupported;
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.transport;
	requires io.netty.transport.epoll;
	requires io.netty.codec;
	requires io.netty.codec.http;
	requires io.netty.codec.http2;
	requires io.netty.handler;
	
	exports io.winterframework.mod.web;
	exports io.winterframework.mod.web.app;
	exports io.winterframework.mod.web.router;
	
	// Test
//	exports io.winterframework.mod.web.internal.server;
}