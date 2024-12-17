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
package io.inverno.mod.test.web.client.ws;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.test.web.client.ws.dto.Message;
import io.inverno.mod.web.base.annotation.CookieParam;
import io.inverno.mod.web.base.annotation.HeaderParam;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.base.annotation.QueryParam;
import io.inverno.mod.web.base.ws.BaseWeb2SocketExchange;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.annotation.WebClient;
import io.inverno.mod.web.client.annotation.WebSocketRoute;
import io.inverno.mod.web.client.ws.Web2SocketExchange;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@WebClient(uri = "conf://testWebSocketService")
public interface TestWebSocketClient {

	@WebSocketRoute(path = "/out_publisher_raw", closeOnComplete = false)
	Flux<String> out_publisher_raw(Publisher<Publisher<ByteBuf>> outbound);

	@WebSocketRoute(path = "/out_publisher_raw_reduced", closeOnComplete = false)
	Flux<String> out_publisher_raw_reduced(Publisher<ByteBuf> outbound);

	@WebSocketRoute(path = "/out_publisher_string", closeOnComplete = false)
	Flux<String> out_publisher_string(Publisher<Publisher<String>> outbound);

	@WebSocketRoute(path = "/out_publisher_string_reduced", closeOnComplete = false)
	Flux<String> out_publisher_string_reduced(Publisher<String> outbound);

	@WebSocketRoute(path = "/out_publisher_encoded", subprotocol = "json", closeOnComplete = false)
	Flux<Message> out_publisher_encoded(Publisher<Message> outbound);

	@WebSocketRoute(path = "/out_publisher_void", closeOnComplete = false)
	Flux<String> out_publisher_void(Publisher<Void> outbound);

	@WebSocketRoute(path = "/out_flux_raw", closeOnComplete = false)
	Flux<String> out_flux_raw(Flux<Flux<ByteBuf>> outbound);

	@WebSocketRoute(path = "/out_flux_raw_reduced", closeOnComplete = false)
	Flux<String> out_flux_raw_reduced(Flux<ByteBuf> outbound);

	@WebSocketRoute(path = "/out_flux_string", closeOnComplete = false)
	Flux<String> out_flux_string(Flux<Flux<String>> outbound);

	@WebSocketRoute(path = "/out_flux_string_reduced", closeOnComplete = false)
	Flux<String> out_flux_string_reduced(Flux<String> outbound);

	@WebSocketRoute(path = "/out_flux_encoded", subprotocol = "json", closeOnComplete = false)
	Flux<Message> out_flux_encoded(Flux<Message> outbound);

	@WebSocketRoute(path = "/out_flux_void", closeOnComplete = false)
	Flux<String> out_flux_void(Flux<Void> outbound);

	@WebSocketRoute(path = "/out_mono_raw", closeOnComplete = false)
	Flux<String> out_mono_raw(Mono<Mono<ByteBuf>> outbound);

	@WebSocketRoute(path = "/out_mono_raw_reduced", closeOnComplete = false)
	Flux<String> out_mono_raw_reduced(Mono<ByteBuf> outbound);

	@WebSocketRoute(path = "/out_mono_string", closeOnComplete = false)
	Flux<String> out_mono_string(Mono<Mono<String>> outbound);

	@WebSocketRoute(path = "/out_mono_string_reduced", closeOnComplete = false)
	Flux<String> out_mono_string_reduced(Mono<String> outbound);

	@WebSocketRoute(path = "/out_mono_encoded", subprotocol = "json", closeOnComplete = false)
	Flux<Message> out_mono_encoded(Mono<Message> outbound);

	@WebSocketRoute(path = "/out_mono_void", closeOnComplete = false)
	Flux<String> out_mono_void(Mono<Void> outbound);

	@WebSocketRoute(path = "/in_publisher_raw", closeOnComplete = false)
	Publisher<Publisher<ByteBuf>> in_publisher_raw(Flux<String> outbound);

	@WebSocketRoute(path = "/in_publisher_raw_reduced", closeOnComplete = false)
	Publisher<ByteBuf> in_publisher_raw_reduced(Flux<String> outbound);

	@WebSocketRoute(path = "/in_publisher_string", closeOnComplete = false)
	Publisher<Publisher<String>> in_publisher_string(Flux<String> outbound);

	@WebSocketRoute(path = "/in_publisher_string_reduced", closeOnComplete = false)
	Publisher<String> in_publisher_string_reduced(Flux<String> outbound);

	@WebSocketRoute(path = "/in_publisher_encoded", subprotocol = "json", closeOnComplete = false)
	Publisher<Message> in_publisher_encoded(Flux<Message> outbound);

	@WebSocketRoute(path = "/in_publisher_void", closeOnComplete = false)
	Publisher<Void> in_publisher_void(Flux<String> outbound);

	@WebSocketRoute(path = "/in_flux_raw", closeOnComplete = false)
	Flux<Flux<ByteBuf>> in_flux_raw(Flux<String> outbound);

	@WebSocketRoute(path = "/in_flux_raw_reduced", closeOnComplete = false)
	Flux<ByteBuf> in_flux_raw_reduced(Flux<String> outbound);

	@WebSocketRoute(path = "/in_flux_string", closeOnComplete = false)
	Flux<Flux<String>> in_flux_string(Flux<String> outbound);

	@WebSocketRoute(path = "/in_flux_string_reduced", closeOnComplete = false)
	Flux<String> in_flux_string_reduced(Flux<String> outbound);

	@WebSocketRoute(path = "/in_flux_encoded", subprotocol = "json", closeOnComplete = false)
	Flux<Message> in_flux_encoded(Flux<Message> outbound);

	@WebSocketRoute(path = "/in_flux_void", closeOnComplete = false)
	Flux<Void> in_flux_void(Flux<String> outbound);

	@WebSocketRoute(path = "/in_mono_raw", closeOnComplete = false)
	Mono<Mono<ByteBuf>> in_mono_raw(Flux<String> outbound);

	@WebSocketRoute(path = "/in_mono_raw_reduced", closeOnComplete = false)
	Mono<ByteBuf> in_mono_raw_reduced(Flux<String> outbound);

	@WebSocketRoute(path = "/in_mono_string", closeOnComplete = false)
	Mono<Mono<String>> in_mono_string(Flux<String> outbound);

	@WebSocketRoute(path = "/in_mono_string_reduced", closeOnComplete = false)
	Mono<String> in_mono_string_reduced(Flux<String> outbound);

	@WebSocketRoute(path = "/in_mono_encoded", subprotocol = "json", closeOnComplete = false)
	Mono<Message> in_mono_encoded(Flux<Message> outbound);

	@WebSocketRoute(path = "/in_mono_void", closeOnComplete = false)
	Mono<Void> in_mono_void(Flux<String> outbound);

	@WebSocketRoute(path = "/out_none")
	Flux<String> out_none();

	@WebSocketRoute(path = "/web_exchange_configurer")
	Flux<String> web_exchange_configurer(WebExchange.Configurer<ExchangeContext> configurer);

	@WebSocketRoute(path = "/web_exchange_configurer_generic")
	<T extends ExchangeContext> Flux<String> web_exchange_configurer_generic(WebExchange.Configurer<T> configurer);

	@WebSocketRoute(path = "/web_socket_exchange_configurer")
	Flux<String> web_socket_exchange_configurer(Web2SocketExchange.Configurer<ExchangeContext> configurer);

	@WebSocketRoute(path = "/web_socket_exchange_configurer_generic")
	<T extends ExchangeContext> Flux<String> web_socket_exchange_configurer_generic(Web2SocketExchange.Configurer<T> configurer);

	@WebSocketRoute(path = "/outbound_configurer", closeOnComplete = false)
	Flux<String> outbound_configurer(Consumer<BaseWeb2SocketExchange.Outbound> configurer);

	@WebSocketRoute(path = "/return_inbound", closeOnComplete = false)
	Mono<BaseWeb2SocketExchange.Inbound> return_inbound(Flux<String> outbound);

	@WebSocketRoute(path = "/return_web_socket_exchange", closeOnComplete = false)
	Mono<Web2SocketExchange<? extends ExchangeContext>> return_web_socket_exchange(Flux<String> outbound);

	@WebSocketRoute(path = "/return_web_socket_exchange_generic", closeOnComplete = false)
	<T extends ExchangeContext> Mono<Web2SocketExchange<T>> return_web_socket_exchange_generic(Flux<String> outbound);

	@WebSocketRoute(path = "/mixed_parameters/{pathparam}", subprotocol = "json", closeOnComplete = false)
	Flux<Message> mixed_parameters(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, WebExchange.Configurer<ExchangeContext> webExchangeConfigurer, Flux<Message> outbound);
}
