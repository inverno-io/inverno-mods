/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/module-info.java to edit this template
 */

@io.inverno.core.annotation.Module
module io.inverno.mod.redis.lettuce {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	requires io.inverno.mod.base;
	requires transitive io.inverno.mod.configuration;
	
	requires org.apache.commons.lang3;
	requires jdk.unsupported; // required by netty for low level API for accessing direct buffers
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	requires transitive lettuce.core;
	
	exports io.inverno.mod.redis.lettuce;
}
