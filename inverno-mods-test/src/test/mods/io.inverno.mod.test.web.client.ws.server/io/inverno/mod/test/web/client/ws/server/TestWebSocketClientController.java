/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.test.web.client.ws.server;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.test.web.client.ws.server.dto.Message;
import io.inverno.mod.web.base.annotation.CookieParam;
import io.inverno.mod.web.base.annotation.HeaderParam;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.base.annotation.QueryParam;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebSocketRoute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 *
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean( visibility = Bean.Visibility.PUBLIC )
@WebController
public class TestWebSocketClientController {

	@WebSocketRoute(path = "/out_publisher_raw")
	public Flux<String> out_publisher_raw(Publisher<Publisher<ByteBuf>> inbound) {
		return Flux.concat(Mono.just("out_publisher_raw"), Flux.from(inbound).flatMap(messageData -> Flux.from(messageData).reduceWith(Unpooled::buffer, ByteBuf::writeBytes)).map(messageData -> messageData.toString(Charsets.DEFAULT)));
	}

	@WebSocketRoute(path = "/out_publisher_raw_reduced")
	public Flux<String> out_publisher_raw_reduced(Publisher<ByteBuf> inbound) {
		return Flux.concat(Mono.just("out_publisher_raw_reduced"), Flux.from(inbound).map(messageData -> messageData.toString(Charsets.DEFAULT)));
	}

	@WebSocketRoute(path = "/out_publisher_string")
	public Flux<String> out_publisher_string(Publisher<Publisher<String>> inbound) {
		return Flux.concat(Mono.just("out_publisher_string"), Flux.from(inbound).flatMap(messageData -> Flux.from(messageData).collect(Collectors.joining())));
	}

	@WebSocketRoute(path = "/out_publisher_string_reduced")
	public Flux<String> out_publisher_string_reduced(Publisher<String> inbound) {
		return Flux.concat(Mono.just("out_publisher_string_reduced"), Flux.from(inbound));
	}

	@WebSocketRoute(path = "/out_publisher_encoded", subprotocol = "json")
	public Flux<Message> out_publisher_encoded(Publisher<Message> inbound) {
		return Flux.concat(Mono.just(new Message("out_publisher_encoded")), Flux.from(inbound));
	}

	@WebSocketRoute(path = "/out_publisher_void")
	public Flux<String> out_publisher_void(Publisher<Void> outbound) {
		return Flux.just("out_publisher_void");
	}

	@WebSocketRoute(path = "/out_flux_raw")
	public Flux<String> out_flux_raw(Flux<Flux<ByteBuf>> inbound) {
		return Flux.concat(Mono.just("out_flux_raw"), inbound.flatMap(messageData -> Flux.from(messageData).reduceWith(Unpooled::buffer, ByteBuf::writeBytes)).map(messageData -> messageData.toString(Charsets.DEFAULT)));
	}

	@WebSocketRoute(path = "/out_flux_raw_reduced")
	public Flux<String> out_flux_raw_reduced(Flux<ByteBuf> inbound) {
		return Flux.concat(Mono.just("out_flux_raw_reduced"), inbound.map(messageData -> messageData.toString(Charsets.DEFAULT)));
	}

	@WebSocketRoute(path = "/out_flux_string")
	public Flux<String> out_flux_string(Flux<Flux<String>> inbound) {
		return Flux.concat(Mono.just("out_flux_string"), inbound.flatMap(messageData -> messageData.collect(Collectors.joining())));
	}

	@WebSocketRoute(path = "/out_flux_string_reduced")
	public Flux<String> out_flux_string_reduced(Flux<String> inbound) {
		return Flux.concat(Mono.just("out_flux_string_reduced"), inbound);
	}

	@WebSocketRoute(path = "/out_flux_encoded", subprotocol = "json")
	public Flux<Message> out_flux_encoded(Flux<Message> inbound) {
		return Flux.concat(Mono.just(new Message("out_flux_encoded")), inbound);
	}

	@WebSocketRoute(path = "/out_flux_void")
	public Flux<String> out_flux_void(Flux<Void> inbound) {
		return inbound.thenMany(Flux.just("out_flux_void"));
	}

	@WebSocketRoute(path = "/out_mono_raw")
	public Flux<String> out_mono_raw(Mono<Mono<ByteBuf>> inbound) {
		return Flux.concat(Mono.just("out_mono_raw"), inbound.flatMap(Function.identity()).map(messageData -> messageData.toString(Charsets.DEFAULT)));
	}

	@WebSocketRoute(path = "/out_mono_raw_reduced")
	public Flux<String> out_mono_raw_reduced(Mono<ByteBuf> inbound) {
		return Flux.concat(Mono.just("out_mono_raw_reduced"), inbound.map(messageData -> messageData.toString(Charsets.DEFAULT)));
	}

	@WebSocketRoute(path = "/out_mono_string")
	public Flux<String> out_mono_string(Mono<Mono<String>> inbound) {
		return Flux.concat(Mono.just("out_mono_string"), inbound.flatMap(Function.identity()));
	}

	@WebSocketRoute(path = "/out_mono_string_reduced")
	public Flux<String> out_mono_string_reduced(Mono<String> inbound) {
		return Flux.concat(Mono.just("out_mono_string_reduced"), inbound);
	}

	@WebSocketRoute(path = "/out_mono_encoded", subprotocol = "json")
	public Flux<Message> out_mono_encoded(Mono<Message> inbound) {
		return Flux.concat(Mono.just(new Message("out_mono_encoded")), inbound);
	}

	@WebSocketRoute(path = "/out_mono_void")
	public Flux<String> out_mono_void(Mono<Void> inbound) {
		return inbound.thenMany(Flux.just("out_mono_void"));
	}

	@WebSocketRoute(path = "/in_publisher_raw")
	public Publisher<Publisher<ByteBuf>> in_publisher_raw(Flux<String> inbound) {
		return Flux.concat(Mono.just(Mono.just(Unpooled.copiedBuffer("in_publisher_raw", Charsets.DEFAULT))), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9)).map(chunk -> Unpooled.copiedBuffer(chunk, Charsets.DEFAULT))));
	}

	@WebSocketRoute(path = "/in_publisher_raw_reduced")
	public Publisher<Publisher<ByteBuf>> in_publisher_raw_reduced(Flux<String> inbound) {
		return Flux.concat(Mono.just(Mono.just(Unpooled.copiedBuffer("in_publisher_raw_reduced", Charsets.DEFAULT))), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9)).map(chunk -> Unpooled.copiedBuffer(chunk, Charsets.DEFAULT))));
	}

	@WebSocketRoute(path = "/in_publisher_string")
	public Publisher<Publisher<String>> in_publisher_string(Flux<String> inbound) {
		return Flux.concat(Mono.just(Mono.just("in_publisher_string")), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9))));
	}

	@WebSocketRoute(path = "/in_publisher_string_reduced")
	public Publisher<Publisher<String>> in_publisher_string_reduced(Flux<String> inbound) {
		return Flux.concat(Mono.just(Mono.just("in_publisher_string_reduced")), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9))));
	}

	@WebSocketRoute(path = "/in_publisher_encoded", subprotocol = "json")
	public Publisher<Message> in_publisher_encoded(Flux<Message> inbound) {
		return Flux.concat(Mono.just(new Message("in_publisher_encoded")), inbound);
	}

	public List<String> in_publisher_void = new ArrayList<>();

	@WebSocketRoute(path = "/in_publisher_void", closeOnComplete = true)
	public Publisher<Void> in_publisher_void(Flux<String> inbound) {
		return inbound.take(3).doOnNext(in_publisher_void::add).then();
	}

	@WebSocketRoute(path = "/in_flux_raw")
	public Flux<Flux<ByteBuf>> in_flux_raw(Flux<String> inbound) {
		return Flux.concat(Mono.just(Flux.just(Unpooled.copiedBuffer("in_flux_raw", Charsets.DEFAULT))), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9)).map(chunk -> Unpooled.copiedBuffer(chunk, Charsets.DEFAULT))));
	}

	@WebSocketRoute(path = "/in_flux_raw_reduced")
	public Flux<Flux<ByteBuf>> in_flux_raw_reduced(Flux<String> inbound) {
		return Flux.concat(Mono.just(Flux.just(Unpooled.copiedBuffer("in_flux_raw_reduced", Charsets.DEFAULT))), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9)).map(chunk -> Unpooled.copiedBuffer(chunk, Charsets.DEFAULT))));
	}

	@WebSocketRoute(path = "/in_flux_string")
	public Flux<Flux<String>> in_flux_string(Flux<String> inbound) {
		return Flux.concat(Mono.just(Flux.just("in_flux_string")), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9))));
	}

	@WebSocketRoute(path = "/in_flux_string_reduced")
	public Flux<Flux<String>> in_flux_string_reduced(Flux<String> inbound) {
		return Flux.concat(Mono.just(Flux.just("in_flux_string_reduced")), inbound.map(messageData -> Flux.just(messageData.substring(0, 7), messageData.substring(7, 9))));
	}

	@WebSocketRoute(path = "/in_flux_encoded", subprotocol = "json")
	public Flux<Message> in_flux_encoded(Flux<Message> inbound) {
		return Flux.concat(Mono.just(new Message("in_flux_encoded")), inbound);
	}

	public List<String> in_flux_void = new ArrayList<>();

	@WebSocketRoute(path = "/in_flux_void", closeOnComplete = true)
	public Flux<Void> in_flux_void(Flux<String> inbound) {
		return inbound.take(3).doOnNext(in_flux_void::add).then().flux();
	}

	@WebSocketRoute(path = "/in_mono_raw")
	public Mono<Flux<ByteBuf>> in_mono_raw(Flux<String> inbound) {
		return Mono.just(Flux.concat(Mono.just(Unpooled.copiedBuffer("in_mono_raw: ", Charsets.DEFAULT)), inbound.take(3).map(chunk -> Unpooled.copiedBuffer(chunk + ", ", Charsets.DEFAULT))));
	}

	@WebSocketRoute(path = "/in_mono_raw_reduced")
	public Mono<Flux<ByteBuf>> in_mono_raw_reduced(Flux<String> inbound) {
		return Mono.just(Flux.concat(Mono.just(Unpooled.copiedBuffer("in_mono_raw_reduced: ", Charsets.DEFAULT)), inbound.take(3).map(chunk -> Unpooled.copiedBuffer(chunk + ", ", Charsets.DEFAULT))));
	}

	@WebSocketRoute(path = "/in_mono_string")
	public Mono<Flux<String>> in_mono_string(Flux<String> inbound) {
		return Mono.just(Flux.concat(Mono.just("in_mono_string: "), inbound.take(3).map(chunk -> chunk + ", ")));
	}

	@WebSocketRoute(path = "/in_mono_string_reduced")
	public Mono<Flux<String>> in_mono_string_reduced(Flux<String> inbound) {
		return Mono.just(Flux.concat(Mono.just("in_mono_string_reduced: "), inbound.take(3).map(chunk -> chunk + ", ")));
	}

	@WebSocketRoute(path = "/in_mono_encoded", subprotocol = "json")
	public Mono<Message> in_mono_encoded(Flux<Message> inbound) {
		return inbound.take(3).map(Message::getMessage).collect(Collectors.joining(", ")).map(messages -> new Message("in_mono_encoded: " + messages));
	}

	public List<String> in_mono_void = new ArrayList<>();

	@WebSocketRoute(path = "/in_mono_void", closeOnComplete = true)
	public Mono<Void> in_mono_void(Flux<String> inbound) {
		return inbound.take(3).doOnNext(in_mono_void::add).then();
	}

	@WebSocketRoute(path = "/out_none", closeOnComplete = true)
	public Flux<String> in_mono_void() {
		return Flux.just("out_none", "message 1", "message 2", "message 3");
	}

	@WebSocketRoute(path = "/web_exchange_configurer", closeOnComplete = true)
	public Flux<String> web_exchange_configurer() {
		return Flux.just("web_exchange_configurer", "message 1", "message 2", "message 3");
	}

	@WebSocketRoute(path = "/web_exchange_configurer_generic", closeOnComplete = true)
	public Flux<String> web_exchange_configurer_generic() {
		return Flux.just("web_exchange_configurer_generic", "message 1", "message 2", "message 3");
	}

	@WebSocketRoute(path = "/web_socket_exchange_configurer", closeOnComplete = true)
	public Flux<String> web_socket_exchange_configurer() {
		return Flux.just("web_socket_exchange_configurer", "message 1", "message 2", "message 3");
	}

	@WebSocketRoute(path = "/web_socket_exchange_configurer_generic", closeOnComplete = true)
	public Flux<String> web_socket_exchange_configurer_generic() {
		return Flux.just("web_socket_exchange_configurer_generic", "message 1", "message 2", "message 3");
	}

	@WebSocketRoute(path = "/outbound_configurer")
	public Flux<String> outbound_configurer(Flux<String> inbound) {
		return Flux.concat(Mono.just("outbound_configurer"), inbound);
	}

	@WebSocketRoute(path = "/return_inbound")
	public Flux<String> return_inbound(Flux<String> inbound) {
		return Flux.concat(Mono.just("return_inbound"), inbound);
	}

	@WebSocketRoute(path = "/return_web_socket_exchange")
	public Flux<String> return_web_socket_exchange(Flux<String> inbound) {
		return Flux.concat(Mono.just("return_web_socket_exchange"), inbound);
	}

	@WebSocketRoute(path = "/return_web_socket_exchange_generic")
	public Flux<String> return_web_socket_exchange_generic(Flux<String> inbound) {
		return Flux.concat(Mono.just("return_web_socket_exchange_generic"), inbound);
	}

	@WebSocketRoute(path = "/mixed_parameters/{pathparam}", subprotocol = "json")
	public Flux<Message> mixed_parameters(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, Flux<Message> outbound) {
		return Flux.concat(Flux.just("mixed_parameters", pathparam, queryparam, headerparam, cookieparam).map(Message::new), outbound);
	}
}
