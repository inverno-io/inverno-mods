@io.winterframework.core.annotation.Module
module io.winterframework.mod.commons {
	requires io.winterframework.core;
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	
	exports io.winterframework.mod.commons;
	exports io.winterframework.mod.commons.resource;
}