/*
 * Copyright 2021 Jeremy KUHN
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

/**
 * <p>
 * The Inverno framework boot module provides foundational services.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt><b>compoundDecoders</b></dt>
 * <dd>extend the parameter converter decoding capabilities with a list of {@link io.inverno.mod.base.converter.CompoundDecoder}</dd>
 * <dt><b>compoundEncoders</b></dt>
 * <dd>extend the parameter converter encoding capabilities with a list of {@link io.inverno.mod.base.converter.CompoundEncoder}</dd>
 * <dt><b>configuration</b></dt>
 * <dd>the boot module configuration</dd>
 * <dt><b>mediaTypeService</b></dt>
 * <dd>override the media type service</dd>
 * <dt><b>objectMapper</b></dt>
 * <dd>override the JSON object mapper</dd>
 * <dt><b>resourceProviders</b></dt>
 * <dd>extend the resource service with custom resource providers</dd>
 * <dt><b>workerPool</b></dt>
 * <dd>override the worker thread pool</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt><b>configuration</b></dt>
 * <dd>the boot module configuration</dd>
 * <dt><b>jsonByteBufConverter</b></dt>
 * <dd>A JSON to {@code ByteBuf} converter</dd>
 * <dt><b>jsonByteBufMediaTypeConverter</b></dt>
 * <dd>A JSON to {@code ByteBuf} {@code application/json} media type converter</dd>
 * <dt><b>jsonStringConverter</b></dt>
 * <dd>A JSON to {@code String} converter</dd>
 * <dt><b>jsonStringMediaTypeConverter</b></dt>
 * <dd>A JSON to {@code String} {@code application/json} media type converter</dd>
 * <dt><b>ndJsonByteBufMediaTypeConverter</b></dt>
 * <dd>A JSON to {@code ByteBuf} {@code application/x-ndjson} media type converter</dd>
 * <dt><b>ndJsonByteBufMediaTypeConverter</b></dt>
 * <dd>A JSON to {@code String} {@code application/x-ndjson} media type converter</dd>
 * <dt><b>mediaTypeService</b></dt>
 * <dd>a {@link io.inverno.mod.base.resource.MediaTypeService} used to determine the media type of a resource based on its URI</dd>
 * <dt><b>netService</b></dt>
 * <dd>a {@link io.inverno.mod.base.net.NetService} used to create optimized network clients and servers</dd>
 * <dt><b>objectMapper</b></dt>
 * <dd>a JSON object mapper configured with common modules (JDK8, time...)</dd>
 * <dt><b>parameterConverter</b></dt>
 * <dd>a parameter converter used to convert parameter values to primitive and common types as well as custom types when custom compound decoders and/or encoders are injected</dd>
 * <dt><b>reactor</b></dt>
 * <dd>the {@link io.inverno.mod.base.concurrent.Reactor} used to create optimized threading model based on event loop group</dd>
 * <dt><b>resourceService</b></dt>
 * <dd>a {@link io.inverno.mod.base.resource.ResourceService} used to access various types of {@link io.inverno.mod.base.resource.Resource}</dd>
 * <dt><b>textByteBufMediaTypeConverter</b></dt>
 * <dd>A text to {@code ByteBuf} {@code plain/text} media type converter</dd>
 * <dt><b>textStringMediaTypeConverter</b></dt>
 * <dd>A text to {@code String} {@code plain/text} media type converter</dd>
 * <dt><b>workerPool</b></dt>
 * <dd>a worker thread pool used whenever there's a need for an {@link java.util.concurrent.ExecutorService} to execute tasks asynchronously.</dd>
 * </dl>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@io.inverno.core.annotation.Module

@io.inverno.core.annotation.Wire(beans="io.inverno.mod.boot:jsonByteBufConverter", into="io.inverno.mod.boot:jsonByteBufMediaTypeConverter:jsonByteBufConverter")
@io.inverno.core.annotation.Wire(beans="io.inverno.mod.boot:jsonByteBufConverter", into="io.inverno.mod.boot:ndJsonByteBufMediaTypeConverter:jsonByteBufConverter")
@io.inverno.core.annotation.Wire(beans="io.inverno.mod.boot:jsonStringConverter", into="io.inverno.mod.boot:jsonStringMediaTypeConverter:jsonStringConverter")
@io.inverno.core.annotation.Wire(beans="io.inverno.mod.boot:jsonStringConverter", into="io.inverno.mod.boot:ndJsonStringMediaTypeConverter:jsonStringConverter")
module io.inverno.mod.boot {
	requires transitive io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...
	requires transitive io.inverno.mod.base;
	requires transitive io.inverno.mod.configuration;
	
	requires transitive com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.datatype.jdk8;
	requires com.fasterxml.jackson.datatype.jsr310;
	requires com.fasterxml.jackson.module.afterburner;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.resolver;
	requires io.netty.resolver.dns;
	requires transitive io.netty.transport;
	requires static io.netty.transport.classes.epoll;
	requires static io.netty.transport.classes.kqueue;
	requires static io.netty.incubator.transport.classes.io_uring;
	requires static io.netty.transport.unix.common;
	requires static io.vertx.core;
	requires org.apache.logging.log4j;
	requires transitive org.reactivestreams;
	requires transitive reactor.core;

	exports io.inverno.mod.boot;
	exports io.inverno.mod.boot.converter;
	exports io.inverno.mod.boot.json;

	provides com.fasterxml.jackson.databind.Module with io.inverno.mod.boot.json.InvernoBaseModule;
}