import java.util.concurrent.ExecutorService;

import io.winterframework.mod.base.converter.CompoundDecoder;
import io.winterframework.mod.base.converter.CompoundEncoder;
import io.winterframework.mod.base.net.NetService;
import io.winterframework.mod.base.resource.MediaTypeService;
import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.base.resource.ResourceService;

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
 * The Winter framework boot module provides basic services.
 * </p>
 * 
 * <p>It defines the following sockets:</p>
 * 
 * <dl>
 * <dt>bootConfiguration</dt>
 * <dd>the boot module configuration</dd>
 * <dt>compoundDecoders</dt>
 * <dd>extend the parameter converter decoding capabilities with a list of {@link CompoundDecoder}</dd>
 * <dt>compoundEncoders</dt>
 * <dd>extend the parameter converter encoding capabilities with a list of {@link CompoundEncoder}</dd>
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
 * <dt>bootConfiguration</dt>
 * <dd>the boot module configuration</dd>
 * <dt>netService</dt>
 * <dd>a {@link NetService} used to create optimized event loop group as well as
 * network clients and servers</dd>
 * <dt>mediaTypeService</dt>
 * <dd>a {@link MediaTypeService} used to determine the media type of a resource based on its URI</dd>
 * <dt>resourceService</dt>
 * <dd>a {@link ResourceService} used to access {@link Resource}</dd>
 * <dt>parameterConverter</dt>
 * <dd>a parameter converter used to convert parameter values to primitive and common types as well as custom types when custom compound decoders and/or encoders are injected</dd>
 * <dt>jsonByteBufConverter</dt>
 * <dd>A JSON to ByteBuf converter</dd>
 * <dt>jsonMediaTypeConverter</dt>
 * <dd>An application/json media type converter</dd>
 * <dt>ndjsonMediaTypeConverter</dt>
 * <dd>An application/x-ndjson media type converter</dd>
 * <dt>textPlainMediaTypeConverter</dt>
 * <dd>An text/plain media type converter</dd>
 * <dt>workerPool</dt>
 * <dd>a worker thread pool used whenever there's a need for an {@link ExecutorService} to execute tasks asynchronously.
 * <dt>objectMapper</dt>
 * <dd>A JSON reader/writer</dd>
 * </dl>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@io.winterframework.core.annotation.Module

@io.winterframework.core.annotation.Wire(beans="io.winterframework.mod.boot:jsonByteBufConverter", into="io.winterframework.mod.boot:jsonMediaTypeConverter:jsonByteBufConverter")
@io.winterframework.core.annotation.Wire(beans="io.winterframework.mod.boot:jsonByteBufConverter", into="io.winterframework.mod.boot:ndjsonMediaTypeConverter:jsonByteBufConverter")
module io.winterframework.mod.boot {
	requires transitive io.winterframework.core;
	requires static io.winterframework.core.annotation; // for javadoc...
	
	requires transitive io.winterframework.mod.base;
	requires transitive io.winterframework.mod.configuration;
	
	requires transitive reactor.core;
	requires transitive org.reactivestreams;
	
	requires transitive io.netty.buffer;
	requires io.netty.common;
	requires transitive io.netty.transport;
	requires static io.netty.transport.epoll;
	requires static io.netty.transport.unix.common;
	requires static io.netty.transport.kqueue;
	
	requires transitive com.fasterxml.jackson.databind;
	
	exports io.winterframework.mod.boot;
}