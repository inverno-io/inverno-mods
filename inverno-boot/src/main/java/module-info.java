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

import java.util.concurrent.ExecutorService;

import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.CompoundDecoder;
import io.inverno.mod.base.converter.CompoundEncoder;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.base.resource.MediaTypeService;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;

/**
 * <p>
 * The Inverno framework boot module provides basic services.
 * </p>
 * 
 * <p>
 * It defines the following sockets:
 * </p>
 * 
 * <dl>
 * <dt>configuration</dt>
 * <dd>the boot module configuration</dd>
 * <dt>compoundDecoders</dt>
 * <dd>extend the parameter converter decoding capabilities with a list of
 * {@link CompoundDecoder}</dd>
 * <dt>compoundEncoders</dt>
 * <dd>extend the parameter converter encoding capabilities with a list of
 * {@link CompoundEncoder}</dd>
 * <dt>objectMapper</dt>
 * <dd>override the JSON reader/writer</dd>
 * <dt>workerPool</dt>
 * <dd>override the worker thread pool</dd>
 * </dl>
 * 
 * <p>
 * It exposes the following beans:
 * </p>
 * 
 * <dl>
 * <dt>configuration</dt>
 * <dd>the boot module configuration</dd>
 * <dt>reactor</dt>
 * <dd>the {@link Reactor} used to create optimized threading model based on
 * event loop group</dd>
 * <dt>netService</dt>
 * <dd>a {@link NetService} used to create optimized network clients and
 * servers</dd>
 * <dt>mediaTypeService</dt>
 * <dd>a {@link MediaTypeService} used to determine the media type of a resource
 * based on its URI</dd>
 * <dt>resourceService</dt>
 * <dd>a {@link ResourceService} used to access {@link Resource}</dd>
 * <dt>parameterConverter</dt>
 * <dd>a parameter converter used to convert parameter values to primitive and
 * common types as well as custom types when custom compound decoders and/or
 * encoders are injected</dd>
 * <dt>jsonByteBufConverter</dt>
 * <dd>A JSON to ByteBuf converter</dd>
 * <dt>jsonMediaTypeConverter</dt>
 * <dd>An application/json media type converter</dd>
 * <dt>ndjsonMediaTypeConverter</dt>
 * <dd>An application/x-ndjson media type converter</dd>
 * <dt>textPlainMediaTypeConverter</dt>
 * <dd>An text/plain media type converter</dd>
 * <dt>workerPool</dt>
 * <dd>a worker thread pool used whenever there's a need for an
 * {@link ExecutorService} to execute tasks asynchronously.
 * <dt>objectMapper</dt>
 * <dd>A JSON reader/writer</dd>
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
	requires com.fasterxml.jackson.datatype.jsr310;
	requires com.fasterxml.jackson.module.afterburner;
	requires org.apache.logging.log4j;
	requires transitive org.reactivestreams;
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires transitive io.netty.transport;
	requires static io.netty.transport.unix.common;
	requires static io.netty.transport.classes.epoll;
	requires static io.netty.transport.classes.kqueue;
	requires static io.netty.incubator.transport.classes.io_uring;
	requires static io.vertx.core;
	requires transitive reactor.core;
	
	exports io.inverno.mod.boot;
	exports io.inverno.mod.boot.converter;
}