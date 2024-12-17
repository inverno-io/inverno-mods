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
package io.inverno.mod.test.web.client.server;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.CookieParam;
import io.inverno.mod.web.base.annotation.FormParam;
import io.inverno.mod.web.base.annotation.HeaderParam;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.base.annotation.QueryParam;
import io.inverno.mod.web.server.WebPart;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.inverno.mod.test.web.client.server.dto.Message;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean( visibility = Bean.Visibility.PUBLIC )
@WebController
public class TestClientController {

	public String create;

	@WebRoute(path = "/message", method = Method.POST, consumes = {MediaTypes.APPLICATION_JSON})
	public Mono<Void> create(@Body Message message) {
		this.create = message.getMessage();
		return Mono.empty();
	}

	public boolean list;

	@WebRoute(path = "/message", produces = {MediaTypes.APPLICATION_JSON})
	public Flux<Message> list() {
		this.list = true;
		return Flux.just(new Message("message1"), new Message("message2"));
	}

	public boolean get;

	@WebRoute(path = "/message/{id}", produces = {MediaTypes.APPLICATION_JSON})
	public Mono<Message> get(@PathParam String id) {
		this.get = true;
		return Mono.just(new Message("message " + id));
	}

	public String update;

	@WebRoute(path = "/message/{id}", method = Method.PUT, produces = {MediaTypes.APPLICATION_JSON}, consumes = {MediaTypes.APPLICATION_JSON})
	public Mono<Message> update(@PathParam String id, @Body Message message) {
		this.update = id;
		return Mono.just(message);
	}

	public String delete;

	@WebRoute(path = "/message/{id}", method = Method.DELETE)
	public Mono<Void> delete(@PathParam String id) {
		this.delete = id;
		return Mono.empty();
	}

	public boolean get_mono_web_exchange;

	@WebRoute(path = "/get_mono_web_exchange", method = Method.GET)
	public Mono<String> get_mono_web_exchange() {
		this.get_mono_web_exchange = true;
		return Mono.just("get_mono_web_exchange");
	}

	public boolean get_mono_web_exchange_generic;

	@WebRoute(path = "/get_mono_web_exchange_generic", method = Method.GET)
	public Mono<String> get_mono_web_exchange_generic() {
		this.get_mono_web_exchange_generic = true;
		return Mono.just("get_mono_web_exchange_generic");
	}

	public boolean post_mono_web_exchange_with_params_and_body;

	@WebRoute(path = "/post_mono_web_exchange_with_params_and_body", method = Method.POST)
	public Mono<String> post_mono_web_exchange_with_params_and_body(@HeaderParam String headerparam, @Body String body) {
		this.post_mono_web_exchange_with_params_and_body = true;
		return Mono.just(headerparam + ", " + body);
	}

	public boolean get_mono_web_response;

	@WebRoute(path = "/get_mono_web_response", method = Method.GET)
	public Mono<String> get_mono_web_response() {
		this.get_mono_web_response = true;
		return Mono.just("get_mono_web_response");
	}

	public boolean get_exchange;

	@WebRoute(path = "/get_exchange", method = Method.GET)
	public Mono<String> get_exchange() {
		this.get_exchange = true;
		return Mono.just("get_exchange");
	}

	public boolean get_exchange_generic;

	@WebRoute(path = "/get_exchange_generic", method = Method.GET)
	public Mono<String> get_exchange_generic() {
		this.get_exchange_generic = true;
		return Mono.just("get_exchange_generic");
	}

	public boolean get_publisher_void;

	@WebRoute(path = "/get_publisher_void", method = Method.GET)
	public Publisher<Void> get_publisher_void() {
		this.get_publisher_void = true;
		return Mono.empty();
	}

	public boolean get_publisher_raw;

	@WebRoute(path = "/get_publisher_raw", method = Method.GET)
	public Publisher<ByteBuf> get_publisher_raw() {
		this.get_publisher_raw = true;
		return Flux.just(Unpooled.copiedBuffer("get", Charsets.DEFAULT), Unpooled.copiedBuffer("_publisher", Charsets.DEFAULT), Unpooled.copiedBuffer("_raw", Charsets.DEFAULT));
	}

	public boolean get_publisher_string;

	@WebRoute(path = "/get_publisher_string", method = Method.GET)
	public Publisher<String> get_publisher_string() {
		this.get_publisher_string = true;
		return Flux.just("get", "_publisher", "_string");
	}

	public boolean get_publisher_string_encoded;

	@WebRoute(path = "/get_publisher_string_encoded", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Publisher<String> get_publisher_string_encoded() {
		this.get_publisher_string_encoded = true;
		return Flux.just("get", "_publisher", "_string_encoded");
	}

	public boolean get_publisher_encoded;

	@WebRoute(path = "/get_publisher_encoded", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Publisher<Message> get_publisher_encoded() {
		this.get_publisher_encoded = true;
		return Flux.just(new Message("get_publisher"), new Message("_encoded"));
	}

	public boolean post_publisher_raw;

	@WebRoute(path = "/post_publisher_raw", method = Method.POST)
	public Mono<String> post_publisher_raw(@Body Publisher<ByteBuf> body) {
		this.post_publisher_raw = true;
		return Flux.from(body)
			.map(chunk -> {
				try {
					return chunk.toString(Charsets.DEFAULT);
				}
				finally {
					chunk.release();
				}
			}).collect(Collectors.joining(", ")).map(s -> "post_publisher_raw: " + s);
	}

	public boolean post_publisher_string;

	@WebRoute(path = "/post_publisher_string", method = Method.POST)
	public Mono<String> post_publisher_string(@Body Publisher<String> body) {
		this.post_publisher_string = true;
		return Flux.from(body).collect(Collectors.joining(", ")).map(s -> "post_publisher_string: " + s);
	}

	public boolean post_publisher_string_encoded;

	@WebRoute(path = "/post_publisher_string_encoded", method = Method.POST, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_publisher_string_encoded(@Body Publisher<String> body) {
		this.post_publisher_string_encoded = true;
		return Flux.from(body).collect(Collectors.joining(", ")).map(s -> "post_publisher_string_encoded: " + s);
	}

	public boolean post_publisher_encoded;

	@WebRoute(path = "/post_publisher_encoded", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_publisher_encoded(@Body Publisher<Message> body) {
		this.post_publisher_encoded = true;
		return Flux.from(body).map(Message::getMessage).collect(Collectors.joining(", ")).map(s -> "post_publisher_encoded: " + s);
	}

	public boolean get_flux_void;

	@WebRoute(path = "/get_flux_void", method = Method.GET)
	public Flux<Void> get_flux_void() {
		this.get_flux_void = true;
		return Flux.empty();
	}

	public boolean get_flux_raw;

	@WebRoute(path = "/get_flux_raw", method = Method.GET)
	public Flux<ByteBuf> get_flux_raw() {
		this.get_flux_raw = true;
		return Flux.just(Unpooled.copiedBuffer("get_flux", Charsets.DEFAULT), Unpooled.copiedBuffer("_raw", Charsets.DEFAULT));
	}

	public boolean get_flux_string;

	@WebRoute(path = "/get_flux_string", method = Method.GET)
	public Flux<String> get_flux_string() {
		this.get_flux_string = true;
		return Flux.just("get_flux", "_string");
	}

	public boolean get_flux_string_encoded;

	@WebRoute(path = "/get_flux_string_encoded", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Flux<String> get_flux_string_encoded() {
		this.get_flux_string_encoded = true;
		return Flux.just("get_flux_", "string_encoded");
	}

	public boolean get_flux_encoded;

	@WebRoute(path = "/get_flux_encoded", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Flux<Message> get_flux_encoded() {
		this.get_flux_encoded = true;
		return Flux.just(new Message("get_flux"), new Message("_encoded"));
	}

	public boolean post_flux_raw;

	@WebRoute(path = "/post_flux_raw", method = Method.POST)
	public Mono<String> post_flux_raw(@Body Flux<ByteBuf> body) {
		this.post_flux_raw = true;
		return body
			.map(chunk -> {
				try {
					return chunk.toString(Charsets.DEFAULT);
				}
				finally {
					chunk.release();
				}
			}).collect(Collectors.joining(", ")).map(s -> "post_flux_raw: " + s);
	}

	public boolean post_flux_string;

	@WebRoute(path = "/post_flux_string", method = Method.POST)
	public Mono<String> post_flux_string(@Body Flux<String> body) {
		this.post_flux_string = true;
		return Flux.from(body).collect(Collectors.joining(", ")).map(s -> "post_flux_string: " + s);
	}

	public boolean post_flux_string_encoded;

	@WebRoute(path = "/post_flux_string_encoded", method = Method.POST, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_flux_string_encoded(@Body Flux<String> body) {
		this.post_flux_string_encoded = true;
		return Flux.from(body).collect(Collectors.joining(", ")).map(s -> "post_flux_string_encoded: " + s);
	}

	public boolean post_flux_encoded;

	@WebRoute(path = "/post_flux_encoded", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_flux_encoded(@Body Flux<Message> body) {
		this.post_flux_encoded = true;
		return Flux.from(body).map(Message::getMessage).collect(Collectors.joining(", ")).map(s -> "post_flux_encoded: " + s);
	}

	public boolean get_mono_void;

	@WebRoute(path = "/get_mono_void", method = Method.GET)
	public Mono<Void> get_mono_void() {
		this.get_mono_void = true;
		return Mono.empty();
	}

	public boolean get_mono_raw;

	@WebRoute(path = "/get_mono_raw", method = Method.GET)
	public Mono<ByteBuf> get_mono_raw() {
		this.get_mono_raw = true;
		return Mono.just(Unpooled.copiedBuffer("get_mono_raw", Charsets.DEFAULT));
	}

	public boolean get_mono_string;

	@WebRoute(path = "/get_mono_string", method = Method.GET)
	public Mono<String> get_mono_string() {
		this.get_mono_string = true;
		return Mono.just("get_mono_string");
	}

	public boolean get_mono_string_encoded;

	@WebRoute(path = "/get_mono_string_encoded", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Mono<String> get_mono_string_encoded() {
		this.get_mono_string_encoded = true;
		return Mono.just("get_mono_string_encoded");
	}

	public boolean get_mono_encoded;

	@WebRoute(path = "/get_mono_encoded", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Mono<Message> get_mono_encoded() {
		this.get_mono_encoded = true;
		return Mono.just(new Message("get_mono_encoded"));
	}

	public boolean post_mono_raw;

	@WebRoute(path = "/post_mono_raw", method = Method.POST)
	public Mono<String> post_mono_raw(@Body Mono<ByteBuf> body) {
		this.post_mono_raw = true;
		return body.map(chunk -> {
			try {
				return "post_mono_raw: " + chunk.toString(Charsets.DEFAULT);
			}
			finally {
				chunk.release();
			}
		});
	}

	public boolean post_mono_string;

	@WebRoute(path = "/post_mono_string", method = Method.POST)
	public Mono<String> post_mono_string(@Body Mono<String> body) {
		this.post_mono_string = true;
		return body.map(s -> "post_mono_string: " + s);
	}

	public boolean post_mono_string_encoded;

	@WebRoute(path = "/post_mono_string_encoded", method = Method.POST, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_mono_string_encoded(@Body Mono<String> body) {
		this.post_mono_string_encoded = true;
		return body.map(s -> "post_mono_string_encoded: " + s);
	}

	public boolean post_mono_encoded;

	@WebRoute(path = "/post_mono_encoded", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_mono_encoded(@Body Mono<Message> body) {
		this.post_mono_encoded = true;
		return body.map(message -> "post_mono_encoded: " + message.getMessage());
	}

	public boolean get_header_param;

	@WebRoute(path = "/get_header_param", method = Method.GET)
	public Mono<String> get_header_param(@HeaderParam String param) {
		this.get_header_param = true;
		return Mono.just("get_header_param: " + param);
	}

	public boolean get_header_param_collection;

	@WebRoute(path = "/get_header_param_collection", method = Method.GET)
	public Mono<String> get_header_param_collection(@HeaderParam Collection<String> param) {
		this.get_header_param_collection = true;
		return Mono.just( "get_header_param_collection: " + String.join(", ", param));
	}

	public boolean get_header_param_list;

	@WebRoute(path = "/get_header_param_list", method = Method.GET)
	public Mono<String> get_header_param_list(@HeaderParam List<String> param) {
		this.get_header_param_list = true;
		return Mono.just( "get_header_param_list: " + String.join(", ", param));
	}

	public boolean get_header_param_set;

	@WebRoute(path = "/get_header_param_set", method = Method.GET)
	public Mono<String> get_header_param_set(@HeaderParam Set<String> param) {
		this.get_header_param_set = true;
		return Mono.just( "get_header_param_set: " + String.join(", ", new TreeSet<>(param)));
	}

	public boolean get_header_param_array;

	@WebRoute(path = "/get_header_param_array", method = Method.GET)
	public Mono<String> get_header_param_array(@HeaderParam String[] param) {
		this.get_header_param_array = true;
		return Mono.just( "get_header_param_array: " + String.join(", ", param));
	}

	public boolean get_cookie_param;

	@WebRoute(path = "/get_cookie_param", method = Method.GET)
	public Mono<String> get_cookie_param(@CookieParam String param) {
		this.get_cookie_param = true;
		return Mono.just( "get_cookie_param: " + param);
	}

	public boolean get_cookie_param_collection;

	@WebRoute(path = "/get_cookie_param_collection", method = Method.GET)
	public Mono<String> get_cookie_param_collection(@CookieParam Collection<String> param) {
		this.get_cookie_param_collection = true;
		return Mono.just( "get_cookie_param_collection: " + String.join(", ", param));
	}

	public boolean get_cookie_param_list;

	@WebRoute(path = "/get_cookie_param_list", method = Method.GET)
	public Mono<String> get_cookie_param_list(@CookieParam List<String> param) {
		this.get_cookie_param_list = true;
		return Mono.just( "get_cookie_param_list: " + String.join(", ", param));
	}

	public boolean get_cookie_param_set;

	@WebRoute(path = "/get_cookie_param_set", method = Method.GET)
	public Mono<String> get_cookie_param_set(@CookieParam Set<String> param) {
		this.get_cookie_param_set = true;
		return Mono.just( "get_cookie_param_set: " + String.join(", ", new TreeSet<>(param)));
	}

	public boolean get_cookie_param_array;

	@WebRoute(path = "/get_cookie_param_array", method = Method.GET)
	public Mono<String> get_cookie_param_array(@CookieParam String[] param) {
		this.get_cookie_param_array = true;
		return Mono.just( "get_cookie_param_array: " + String.join(", ", param));
	}

	public boolean get_query_param;

	@WebRoute(path = "/get_query_param", method = Method.GET)
	public Mono<String> get_query_param(@QueryParam String param) {
		this.get_query_param = true;
		return Mono.just( "get_query_param: " + param);
	}

	public boolean get_query_param_collection;

	@WebRoute(path = "/get_query_param_collection", method = Method.GET)
	public Mono<String> get_query_param_collection(@QueryParam Collection<String> param) {
		this.get_query_param_collection = true;
		return Mono.just( "get_query_param_collection: " + String.join(", ", param));
	}

	public boolean get_query_param_list;

	@WebRoute(path = "/get_query_param_list", method = Method.GET)
	public Mono<String> get_query_param_list(@QueryParam List<String> param) {
		this.get_query_param_list = true;
		return Mono.just( "get_query_param_list: " + String.join(", ", param));
	}

	public boolean get_query_param_set;

	@WebRoute(path = "/get_query_param_set", method = Method.GET)
	public Mono<String> get_query_param_set(@QueryParam Set<String> param) {
		this.get_query_param_set = true;
		return Mono.just( "get_query_param_set: " + String.join(", ", new TreeSet<>(param)));
	}

	public boolean get_query_param_array;

	@WebRoute(path = "/get_query_param_array", method = Method.GET)
	public Mono<String> get_query_param_array(@QueryParam String[] param) {
		this.get_query_param_array = true;
		return Mono.just( "get_query_param_array: " + String.join(", ", param));
	}

	public boolean get_path_param;

	@WebRoute(path = "/get_path_param/{param}", method = Method.GET)
	public Mono<String> get_path_param(@PathParam String param) {
		this.get_path_param = true;
		return Mono.just( "get_path_param: " + param);
	}

	public boolean get_path_param_collection;

	@WebRoute(path = "/get_path_param_collection/{param}", method = Method.GET)
	public Mono<String> get_path_param_collection(@PathParam Collection<String> param) {
		this.get_path_param_collection = true;
		return Mono.just( "get_path_param_collection: " + String.join(", ", param));
	}

	public boolean get_path_param_list;

	@WebRoute(path = "/get_path_param_list/{param}", method = Method.GET)
	public Mono<String> get_path_param_list(@PathParam List<String> param) {
		this.get_path_param_list = true;
		return Mono.just( "get_path_param_list: " + String.join(", ", param));
	}

	public boolean get_path_param_set;

	@WebRoute(path = "/get_path_param_set/{param}", method = Method.GET)
	public Mono<String> get_path_param_set(@PathParam Set<String> param) {
		this.get_path_param_set = true;
		return Mono.just( "get_path_param_set: " + String.join(", ", new TreeSet<>(param)));
	}

	public boolean get_path_param_array;

	@WebRoute(path = "/get_path_param_array/{param}", method = Method.GET)
	public Mono<String> get_path_param_array(@PathParam String[] param) {
		this.get_path_param_array = true;
		return Mono.just( "get_path_param_array: " + String.join(", ", param));
	}

	public boolean post_form_param;

	@WebRoute(path = "/post_form_param", method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public Mono<String> post_form_param(@FormParam String param) {
		this.post_form_param = true;
		return Mono.just( "post_form_param: " +  param);
	}

	public boolean post_form_param_collection;

	@WebRoute(path = "/post_form_param_collection", method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public Mono<String> post_form_param_collection(@FormParam Collection<String> param) {
		this.post_form_param_collection = true;
		return Mono.just("post_form_param_collection: " +  String.join(", ", param));
	}

	public boolean post_form_param_list;

	@WebRoute(path = "/post_form_param_list", method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public Mono<String> post_form_param_list(@FormParam List<String> param) {
		this.post_form_param_list = true;
		return Mono.just("post_form_param_list: " +  String.join(", ", param));
	}

	public boolean post_form_param_set;

	@WebRoute(path = "/post_form_param_set", method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public Mono<String> post_form_param_set(@FormParam Set<String> param) {
		this.post_form_param_set = true;
		return Mono.just("post_form_param_set: " +  String.join(", ", new TreeSet<>(param)));
	}

	public boolean post_form_param_array;

	@WebRoute(path = "/post_form_param_array", method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public Mono<String> post_form_param_array(@FormParam String[] param) {
		this.post_form_param_array = true;
		return Mono.just("post_form_param_array: " +  String.join(", ", param));
	}

	public boolean post_raw;

	@WebRoute(path = "/post_raw", method = Method.POST)
	public Mono<String> post_raw(@Body ByteBuf body) {
		this.post_raw = true;
		return Mono.just("post_raw: " + body.toString(Charsets.DEFAULT));
	}

	public boolean post_string;

	@WebRoute(path = "/post_string", method = Method.POST)
	public Mono<String> post_string(@Body String body) {
		this.post_string = true;
		return Mono.just("post_string: " + body);
	}

	public boolean post_encoded;

	@WebRoute(path = "/post_encoded", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_encoded(@Body Message message) {
		this.post_encoded = true;
		return Mono.just("post_encoded: " + message.getMessage());
	}

	public boolean post_encoded_collection;

	@WebRoute(path = "/post_encoded_collection", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	Mono<String> post_encoded_collection(@Body Collection<Message> messages) {
		this.post_encoded_collection = true;
		return Mono.just("post_encoded_collection: " + messages.stream().map(Message::getMessage).collect(Collectors.joining(", ")));
	}

	public boolean post_encoded_list;

	@WebRoute(path = "/post_encoded_list", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_encoded_list(@Body List<Message> messages) {
		this.post_encoded_list = true;
		return Mono.just("post_encoded_list: " + messages.stream().map(Message::getMessage).collect(Collectors.joining(", ")));
	}

	public boolean post_encoded_set;

	@WebRoute(path = "/post_encoded_set", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_encoded_set(@Body Set<Message> messages) {
		this.post_encoded_set = true;
		return Mono.just("post_encoded_set: " + messages.stream().map(Message::getMessage).sorted().collect(Collectors.joining(", ")));
	}

	public boolean post_encoded_array;

	@WebRoute(path = "/post_encoded_array", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_encoded_array(@Body Message[] messages) {
		this.post_encoded_array = true;
		return Mono.just("post_encoded_array: " + Arrays.stream(messages).map(Message::getMessage).collect(Collectors.joining(", ")));
	}

	public boolean post_multipart;

	@WebRoute(path = "/post_multipart", method = Method.POST, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart(@Body Flux<WebPart> parts) {
		this.post_multipart = true;
		return parts.flatMap(part -> Flux.concat(Mono.just("[" + part.getName()+ ", " + part.getFilename().orElse(null) + ", " + part.headers().getContentType() + "]"), part.string().stream()).collect(Collectors.joining("="))).collect(Collectors.joining(", ")).map(s -> "post_multipart: " + s);
	}

	public boolean post_multipart_flux_encoded;

	@WebRoute(path = "/post_multipart_flux_encoded", method = Method.POST, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_flux_encoded(@Body Flux<WebPart> parts) {
		this.post_multipart_flux_encoded = true;
		return parts.flatMap(part -> Flux.concat(Mono.just("[" + part.getName()+ ", " + part.getFilename().orElse(null) + ", " + part.headers().getContentType() + "]"), part.string().stream()).collect(Collectors.joining("="))).collect(Collectors.joining(", ")).map(s -> "post_multipart_flux_encoded: " + s);
	}

	public boolean post_multipart_mono_encoded;

	@WebRoute(path = "/post_multipart_mono_encoded", method = Method.POST, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_mono_encoded(@Body Flux<WebPart> parts) {
		this.post_multipart_mono_encoded = true;
		return parts.flatMap(part -> Flux.concat(Mono.just("[" + part.getName()+ ", " + part.getFilename().orElse(null) + ", " + part.headers().getContentType() + "]"), part.string().stream()).collect(Collectors.joining("="))).collect(Collectors.joining(", ")).map(s -> "post_multipart_mono_encoded: " + s);
	}

	public boolean post_resource;

	@WebRoute(path = "/post_resource", method = Method.POST)
	public Mono<String> post_resource(@Body Mono<String> resource) {
		this.post_resource = true;
		return resource.map(s -> "post_resource: " + s);
	}

	public boolean get_not_found;

	@WebRoute(path = "/get_not_found", method = Method.GET)
	public Mono<Void> get_not_found() {
		this.get_not_found = true;
		throw new NotFoundException();
	}

	public boolean get_internal_server_error;

	@WebRoute(path = "/get_internal_server_error", method = Method.GET)
	public Mono<String> get_internal_server_error() {
		this.get_internal_server_error = true;
		throw new InternalServerErrorException();
	}

	public boolean post_mixed_parameters_body;

	@WebRoute(path = "/post_mixed_parameters_body/{pathparam}", method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_mixed_parameters_body(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, @Body Message message) {
		this.post_mixed_parameters_body = true;
		return Mono.just("post_mixed_parameters_body: " + pathparam + ", " + queryparam + ", " + headerparam + ", " + cookieparam + ", " + message.getMessage());
	}

	public boolean post_mixed_parameters_form;

	@WebRoute(path = "/post_mixed_parameters_form/{pathparam}", method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public Mono<String> post_mixed_parameters_body(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, @FormParam String formparam) {
		this.post_mixed_parameters_form = true;
		return Mono.just("post_mixed_parameters_form: " + pathparam + ", " + queryparam + ", " + headerparam + ", " + cookieparam + ", " + formparam);
	}

	public boolean post_mixed_parameters_multipart;

	@WebRoute(path = "/post_mixed_parameters_multipart/{pathparam}", method = Method.POST, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_mixed_parameters_multipart(@PathParam String pathparam, @QueryParam String queryparam, @HeaderParam String headerparam, @CookieParam String cookieparam, @Body Flux<WebPart> parts) {
		this.post_mixed_parameters_multipart = true;
		return parts.filter(part -> part.getName().equals("partparam"))
			.flatMap(part -> part.string().stream()).collect(Collectors.joining())
			.map(partparam -> "post_mixed_parameters_multipart: " + pathparam + ", " + queryparam + ", " + headerparam + ", " + cookieparam + ", " + partparam);
	}
}
