/*
 * Copyright 2024 Jeremy KUHN
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
 * The Inverno framework Web base module defines API and provides common services to implement Web enabled HTTP1.x and HTTP/2 clients and servers.
 * </p>
 *
 * <p>
 * It defines the following sockets:
 * </p>
 *
 * <dl>
 * <dt><b>mediaTypeConverters (required)</b></dt>
 * <dd>The list of media type converters used to encode and decode content based on their media type.</dd>
 * </dl>
 *
 * <p>
 * It exposes the following beans:
 * </p>
 *
 * <dl>
 * <dt><b>dataConversionService</b></dt>
 * <dd>The data conversion service used to encode and decode content based on a media type or a WebSocket subprotocol.</dd>
 * </dl>
 *
 * <pre>{@code
 * List<MediaTypeConverter<ByteBuf>> converters = ...;
 *
 * Base base = new Base.Builder(converters).build();
 * try {
 *     base.start();
 *
 *     OutboundData<ByteBuf> messageOutbound = ...;
 *     OutboundDataEncoder<Message> messageEncoder = base.dataConversionService().createEncoder(messageOutbound, MediaTypes.APPLICATION_JSON, Message.class);
 *     messageEncoder.many(Flux.just(new Message("Hello John"), new Message("Hello Bob"), new Message("Hello Alice")));
 *
 *     InboundData<ByteBuf> messageInbound = ...;
 *     InboundDataDecoder<Message> messageDecoder = base.dataConversionService().createDecoder(messageInbound, MediaTypes.APPLICATION_JSON, Message.class);
 *     Flux<Message> decodedInbound = messageDecoder.many();
 * }
 * finally {
 *     base.stop();
 * }
 * }</pre>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@io.inverno.core.annotation.Module
module io.inverno.mod.web.base {
	requires io.inverno.core;
	requires static io.inverno.core.annotation; // for javadoc...

	requires transitive io.inverno.mod.base;
	requires transitive io.inverno.mod.http.base;

	requires org.apache.logging.log4j;
	requires org.reactivestreams;
	requires reactor.core;
	
	exports io.inverno.mod.web.base;
	exports io.inverno.mod.web.base.annotation;
	exports io.inverno.mod.web.base.ws;
}
