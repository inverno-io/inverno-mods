/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.test.web.webroute;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.test.web.webroute.dto.GenericMessage;
import io.inverno.mod.test.web.webroute.dto.IntegerMessage;
import io.inverno.mod.test.web.webroute.dto.Message;
import io.inverno.mod.test.web.webroute.dto.StringMessage;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.WebPart;
import io.inverno.mod.web.server.WebResponseBody;
import io.inverno.mod.web.server.annotation.Body;
import io.inverno.mod.web.server.annotation.CookieParam;
import io.inverno.mod.web.server.annotation.FormParam;
import io.inverno.mod.web.server.annotation.HeaderParam;
import io.inverno.mod.web.server.annotation.PathParam;
import io.inverno.mod.web.server.annotation.QueryParam;
import io.inverno.mod.web.server.annotation.SseEventFactory;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean(visibility = Visibility.PRIVATE)
@WebController
public class WebRouteController {

	private final ResourceService resourceService;
	
	public WebRouteController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}
	
	public boolean get_void;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_void'
	@WebRoute(path = "get_void", method = Method.GET)
	public void get_void() {
		this.get_void = true;
	}
	
	public boolean get_raw;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_raw'
	@WebRoute(path = "/get_raw", method = Method.GET)
	public ByteBuf get_raw() {
		this.get_raw = true;
		return Unpooled.copiedBuffer("get_raw", Charsets.DEFAULT);
	}
	
	public boolean get_raw_slash;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_raw/'
	@WebRoute(path = "/get_raw/", method = Method.GET)
	public ByteBuf get_raw_slash() {
		this.get_raw_slash = true;
		return Unpooled.copiedBuffer("get_raw_slash", Charsets.DEFAULT);
	}
	
	public boolean get_raw_pub;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_raw/pub'
	@WebRoute(path = "/get_raw/pub", method = Method.GET)
	public Publisher<ByteBuf> get_raw_pub() {
		this.get_raw_pub = true;
		return Mono.just(Unpooled.copiedBuffer("get_raw_pub", Charsets.DEFAULT));
	}
	
	public boolean get_raw_mono;

	// curl --insecure -iv 'http://127.0.0.1:8080/get_raw/mono'
	@WebRoute(path = "/get_raw/mono", method = Method.GET)
	public Mono<ByteBuf> get_raw_mono() {
		this.get_raw_mono = true;
		return Mono.just(Unpooled.copiedBuffer("get_raw_mono", Charsets.DEFAULT));
	}
	
	public boolean get_raw_flux;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_raw/flux'
	@WebRoute(path = "/get_raw/flux", method = Method.GET)
	public Flux<ByteBuf> get_raw_flux() {
		this.get_raw_flux = true;
		return Flux.just(Unpooled.copiedBuffer("get_ra", Charsets.DEFAULT), Unpooled.copiedBuffer("w_flux", Charsets.DEFAULT));
	}
	
	public boolean get_encoded;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded'
	@WebRoute(path = "/get_encoded", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded() {
		this.get_encoded = true;
		return "get_encoded";
	}
	
	public boolean get_encoded_no_produce;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/no_produce'
	@WebRoute(path = "/get_encoded/no_produce", method = Method.GET)
	public String get_encoded_no_produce() {
		this.get_encoded_no_produce = true;
		return "get_encoded_no_produce";
	}
	
	public boolean get_encoded_no_encoder;

	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/no_encoder'
	@WebRoute(path = "/get_encoded/no_encoder", method = Method.GET, produces = MediaTypes.TEXT_XML)
	public String get_encoded_no_encoder() {
		this.get_encoded_no_encoder = true;
		return "get_encoded_no_encoder";
	}
	
	public boolean get_encoded_collection;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/collection'
	@WebRoute(path = "/get_encoded/collection", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Collection<String> get_encoded_collection() {
		this.get_encoded_collection = true;
		return List.of("get", "encoded", "collection");
	}
	
	public boolean get_encoded_list;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/list'
	@WebRoute(path = "/get_encoded/list", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public List<String> get_encoded_list() {
		this.get_encoded_list = true;
		return List.of("get", "encoded", "list");
	}
	
	public boolean get_encoded_set;

	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/set'
	@WebRoute(path = "/get_encoded/set", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Set<String> get_encoded_set() {
		this.get_encoded_set = true;
		return Set.of("get", "encoded", "set");
	}
	
	public boolean get_encoded_array;

	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/array'
	@WebRoute(path = "/get_encoded/array", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String[] get_encoded_array() {
		this.get_encoded_array = true;
		return new String[] {"get", "encoded", "array"};
	}
	
	public boolean get_encoded_pub;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pub'
	@WebRoute(path = "/get_encoded/pub", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Publisher<String> get_encoded_pub() {
		this.get_encoded_pub = true;
		return Flux.just("get", "_encoded", "_pub");
	}
	
	public boolean get_encoded_mono;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/mono'
	@WebRoute(path = "/get_encoded/mono", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Mono<String> get_encoded_mono() {
		this.get_encoded_mono = true;
		return Mono.just("get_encoded_mono");
	}
	
	public boolean get_encoded_flux;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/flux'
	@WebRoute(path = "/get_encoded/flux", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Flux<String> get_encoded_flux() {
		this.get_encoded_flux = true;
		return Flux.just("get_","encoded_","flux");
	}
	
	public boolean get_encoded_queryParam;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam?queryParam=abc&queryParam=def'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam'
	@WebRoute(path = "/get_encoded/queryParam", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam(@QueryParam String queryParam) {
		this.get_encoded_queryParam = true;
		return "get_encoded_queryParam: " + queryParam;
	}
	
	public boolean get_encoded_queryParam_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/opt?queryParam=abc&queryParam=def'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/opt'
	@WebRoute(path = "/get_encoded/queryParam/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_opt(@QueryParam Optional<String> queryParam) {
		this.get_encoded_queryParam_opt = true;
		return "get_encoded_queryParam_opt: " + queryParam.orElse("empty");
	}
	
	public boolean get_encoded_queryParam_collection;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/collection?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/collection'
	@WebRoute(path = "/get_encoded/queryParam/collection", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_collection(@QueryParam Collection<String> queryParam) {
		this.get_encoded_queryParam_collection = true;
		return "get_encoded_queryParam_collection: " + queryParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_queryParam_collection_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/collection/opt'
	@WebRoute(path = "/get_encoded/queryParam/collection/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_collection_opt(@QueryParam Optional<Collection<String>> queryParam) {
		this.get_encoded_queryParam_collection_opt = true;
		return "get_encoded_queryParam_collection_opt: " + queryParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_queryParam_list;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/list?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/list'
	@WebRoute(path = "/get_encoded/queryParam/list", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_list(@QueryParam List<String> queryParam) {
		this.get_encoded_queryParam_list = true;
		return "get_encoded_queryParam_list: " + queryParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_queryParam_list_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/list/opt'
	@WebRoute(path = "/get_encoded/queryParam/list/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_list_opt(@QueryParam Optional<List<String>> queryParam) {
		this.get_encoded_queryParam_list_opt = true;
		return "get_encoded_queryParam_list_opt: " + queryParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_queryParam_set;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/set?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/set'
	@WebRoute(path = "/get_encoded/queryParam/set", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_set(@QueryParam Set<String> queryParam) {
		this.get_encoded_queryParam_set = true;
		return "get_encoded_queryParam_set: " + queryParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_queryParam_set_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/set/opt'
	@WebRoute(path = "/get_encoded/queryParam/set/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_set_opt(@QueryParam Optional<Set<String>> queryParam) {
		this.get_encoded_queryParam_set_opt = true;
		return "get_encoded_queryParam_set_opt: " + queryParam.orElse(Set.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_queryParam_array;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/array?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/array'
	@WebRoute(path = "/get_encoded/queryParam/array", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_array(@QueryParam String[] queryParam) {
		this.get_encoded_queryParam_array = true;
		return "get_encoded_queryParam_array: " + Arrays.stream(queryParam).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_queryParam_array_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt?queryParam=abc&queryParam=def,hij'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/queryParam/array/opt'
	@WebRoute(path = "/get_encoded/queryParam/array/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_queryParam_array_opt(@QueryParam Optional<String[]> queryParam) {
		this.get_encoded_queryParam_array_opt = true;
		return "get_encoded_queryParam_array_opt: " + queryParam.map(Arrays::stream).orElse(Stream.of()).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam'
	@WebRoute(path = "/get_encoded/cookieParam", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam(@CookieParam String cookieParam) {
		this.get_encoded_cookieParam = true;
		return "get_encoded_cookieParam: " + cookieParam;
	}
	
	public boolean get_encoded_cookieParam_opt;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/opt'
	@WebRoute(path = "/get_encoded/cookieParam/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_opt(@CookieParam Optional<String> cookieParam) {
		this.get_encoded_cookieParam_opt = true;
		return "get_encoded_cookieParam_opt: " + cookieParam.orElse("empty");
	}
	
	public boolean get_encoded_cookieParam_collection;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/collection'
	@WebRoute(path = "/get_encoded/cookieParam/collection", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_collection(@CookieParam Collection<String> cookieParam) {
		this.get_encoded_cookieParam_collection = true;
		return "get_encoded_cookieParam_collection: " + cookieParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam_collection_opt;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/collection/opt'
	@WebRoute(path = "/get_encoded/cookieParam/collection/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_collection_opt(@CookieParam Optional<Collection<String>> cookieParam) {
		this.get_encoded_cookieParam_collection_opt = true;
		return "get_encoded_cookieParam_collection_opt: " + cookieParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam_list;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/list'
	@WebRoute(path = "/get_encoded/cookieParam/list", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_list(@CookieParam List<String> cookieParam) {
		this.get_encoded_cookieParam_list = true;
		return "get_encoded_cookieParam_list: " + cookieParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam_list_opt;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/list/opt'
	@WebRoute(path = "/get_encoded/cookieParam/list/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_list_opt(@CookieParam Optional<List<String>> cookieParam) {
		this.get_encoded_cookieParam_list_opt = true;
		return "get_encoded_cookieParam_list_opt: " + cookieParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam_set;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/set'
	@WebRoute(path = "/get_encoded/cookieParam/set", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_set(@CookieParam Set<String> cookieParam) {
		this.get_encoded_cookieParam_set = true;
		return "get_encoded_cookieParam_set: " + cookieParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam_set_opt;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/set/opt'
	@WebRoute(path = "/get_encoded/cookieParam/set/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_set_opt(@CookieParam Optional<Set<String>> cookieParam) {
		this.get_encoded_cookieParam_set_opt = true;
		return "get_encoded_cookieParam_set_opt: " + cookieParam.orElse(Set.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam_array;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/array'
	@WebRoute(path = "/get_encoded/cookieParam/array", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_array(@CookieParam String[] cookieParam) {
		this.get_encoded_cookieParam_array = true;
		return "get_encoded_cookieParam_array: " + Arrays.stream(cookieParam).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_cookieParam_array_opt;
	
	// curl --insecure -iv -H 'cookie: cookieParam=abc' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc; cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
	// curl --insecure -iv -H 'cookie: cookieParam=abc' -H 'cookie: cookieParam=def,hij' 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/cookieParam/array/opt'
	@WebRoute(path = "/get_encoded/cookieParam/array/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_cookieParam_array_opt(@CookieParam Optional<String[]> cookieParam) {
		this.get_encoded_cookieParam_array_opt = true;
		return "get_encoded_cookieParam_array_opt: " + cookieParam.map(Arrays::stream).orElse(Stream.of()).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerParam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam'
	@WebRoute(path = "/get_encoded/headerParam", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam(@HeaderParam String headerparam) {
		this.get_encoded_headerParam = true;
		return "get_encoded_headerParam: " + headerparam;
	}
	
	public boolean get_encoded_headerParam_opt;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/opt'
	@WebRoute(path = "/get_encoded/headerParam/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_opt(@HeaderParam Optional<String> headerparam) {
		this.get_encoded_headerParam_opt = true;
		return "get_encoded_headerParam_opt: " + headerparam.orElse("empty");
	}
	
	public boolean get_encoded_headerParam_collection;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/collection'
	@WebRoute(path = "/get_encoded/headerParam/collection", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_collection(@HeaderParam Collection<String> headerparam) {
		this.get_encoded_headerParam_collection = true;
		return "get_encoded_headerParam_collection: " + headerparam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam_collection_opt;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/collection/opt'
	@WebRoute(path = "/get_encoded/headerParam/collection/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_collection_opt(@HeaderParam Optional<Collection<String>> headerparam) {
		this.get_encoded_headerParam_collection_opt = true;
		return "get_encoded_headerParam_collection_opt: " + headerparam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam_list;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/list'
	@WebRoute(path = "/get_encoded/headerParam/list", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_list(@HeaderParam List<String> headerparam) {
		this.get_encoded_headerParam_list = true;
		return "get_encoded_headerParam_list: " + headerparam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam_list_opt;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/list/opt'
	@WebRoute(path = "/get_encoded/headerParam/list/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_list_opt(@HeaderParam Optional<List<String>> headerparam) {
		this.get_encoded_headerParam_list_opt = true;
		return "get_encoded_headerParam_list_opt: " + headerparam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam_set;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/set'
	@WebRoute(path = "/get_encoded/headerParam/set", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_set(@HeaderParam Set<String> headerparam) {
		this.get_encoded_headerParam_set = true;
		return "get_encoded_headerParam_set: " + headerparam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam_set_opt;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/set/opt'
	@WebRoute(path = "/get_encoded/headerParam/set/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_set_opt(@HeaderParam Optional<Set<String>> headerparam) {
		this.get_encoded_headerParam_set_opt = true;
		return "get_encoded_headerParam_set_opt: " + headerparam.orElse(Set.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam_array;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/array'
	@WebRoute(path = "/get_encoded/headerParam/array", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_array(@HeaderParam String[] headerparam) {
		this.get_encoded_headerParam_array = true;
		return "get_encoded_headerParam_array: " + Arrays.stream(headerparam).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_headerParam_array_opt;
	
	// curl --insecure -iv -H 'headerparam: abc' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
	// curl --insecure -iv -H 'headerparam: abc,def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
	// curl --insecure -iv -H 'headerparam: abc' -H 'headerparam: def,hij' 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/headerParam/array/opt'
	@WebRoute(path = "/get_encoded/headerParam/array/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_headerParam_array_opt(@HeaderParam Optional<String[]> headerparam) {
		this.get_encoded_headerParam_array_opt = true;
		return "get_encoded_headerParam_array_opt: " + headerparam.map(Arrays::stream).orElse(Stream.of()).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c'
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam(@PathParam String pathParam) {
		this.get_encoded_pathParam = true;
		return "get_encoded_pathParam: " + pathParam;
	}
	
	public boolean get_encoded_pathParam_opt;
	
	// TODO pathParam is currently required no matter what
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/opt'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_opt(@PathParam Optional<String> pathParam) {
		this.get_encoded_pathParam_opt = true;
		return "get_encoded_pathParam_opt: " + pathParam.orElse("empty");
	}
	
	public boolean get_encoded_pathParam_collection;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/collection", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_collection(@PathParam Collection<String> pathParam) {
		this.get_encoded_pathParam_collection = true;
		return "get_encoded_pathParam_collection: " + pathParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam_collection_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/collection/opt'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/collection/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_collection_opt(@PathParam Optional<Collection<String>> pathParam) {
		this.get_encoded_pathParam_collection_opt = true;
		return "get_encoded_pathParam_collection_opt: " + pathParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam_list;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/list", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_list(@PathParam List<String> pathParam) {
		this.get_encoded_pathParam_list = true;
		return "get_encoded_pathParam_list: " + pathParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam_list_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/list/opt'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/list/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_list_opt(@PathParam Optional<List<String>> pathParam) {
		this.get_encoded_pathParam_list_opt = true;
		return "get_encoded_pathParam_list_opt: " + pathParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam_set;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/set", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_set(@PathParam Set<String> pathParam) {
		this.get_encoded_pathParam_set = true;
		return "get_encoded_pathParam_set: " + pathParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam_set_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/set/opt'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/set/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_set_opt(@PathParam Optional<Set<String>> pathParam) {
		this.get_encoded_pathParam_set_opt = true;
		return "get_encoded_pathParam_set_opt: " + pathParam.orElse(Set.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam_array;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/array", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_array(@PathParam String[] pathParam) {
		this.get_encoded_pathParam_array = true;
		return "get_encoded_pathParam_array: " + Arrays.stream(pathParam).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_pathParam_array_opt;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/pathParam/a,b,c/array/opt'
	@WebRoute(path = "/get_encoded/pathParam/{pathParam}/array/opt", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_encoded_pathParam_array_opt(@PathParam Optional<String[]> pathParam) {
		this.get_encoded_pathParam_array_opt = true;
		return "get_encoded_pathParam_array_opt: " + pathParam.map(Arrays::stream).orElse(Stream.of()).collect(Collectors.joining(", "));
	}
	
	public boolean get_encoded_json_dto;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/json/dto'
	@WebRoute(path = "/get_encoded/json/dto", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Message get_encoded_json_dto() {
		this.get_encoded_json_dto = true;
		return new Message("Hello, world!");
	}
	
	public boolean get_encoded_json_pub_dto;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/json/pub/dto'
	@WebRoute(path = "/get_encoded/json/pub/dto", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Publisher<Message> get_encoded_json_pub_dto() {
		this.get_encoded_json_dto = true;
		return Flux.just(new Message("Hello, world!"), new Message("Salut, monde!"), new Message("Hallo, welt!"));
	}
	
	public boolean get_encoded_json_dto_generic;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/json/dto/generic'
	@WebRoute(path = "/get_encoded/json/dto/generic", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public GenericMessage<?> get_encoded_json_dto_generic() {
		this.get_encoded_json_dto_generic = true;
		return new StringMessage(1, "Hello, world!");
	}
	
	public boolean get_encoded_json_pub_dto_generic;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/json/pub/dto/generic'
	@WebRoute(path = "/get_encoded/json/pub/dto/generic", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Publisher<GenericMessage<?>> get_encoded_json_pub_dto_generic() {
		this.get_encoded_json_pub_dto_generic = true;
		return Flux.just(new StringMessage(1, "Hello, world!"), new IntegerMessage(2, 123456));
	}
	
	public boolean get_encoded_json_map;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/json/map'
	@WebRoute(path = "/get_encoded/json/map", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Map<String, Integer> get_encoded_json_map() {
		this.get_encoded_json_map = true;
		return Map.of("a", 1, "b", 2);
	}
	
	public boolean get_encoded_json_pub_map;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_encoded/json/pub/map'
	@WebRoute(path = "/get_encoded/json/pub/map", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
	public Publisher<Map<String, Integer>> get_encoded_json_pub_map() {
		this.get_encoded_json_pub_map = true;
		return Flux.just(Map.of("a", 1, "b", 2), Map.of("c", 3, "d", 4));
	}
	
	public boolean post_formParam;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam'
	@WebRoute(path = "/post/formParam", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam(@FormParam String formParam) {
		this.post_formParam = true;
		return "post_formParam: " + formParam;
	}
	
	public boolean post_formParam_opt;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/opt'
	@WebRoute(path = "/post/formParam/opt", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_opt(@FormParam Optional<String> formParam) {
		this.post_formParam_opt = true;
		return "post_formParam_opt: " + formParam.orElse("empty");
	}
	
	public boolean post_formParam_collection;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection'
	@WebRoute(path = "/post/formParam/collection", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_collection(@FormParam Collection<String> formParam) {
		this.post_formParam_collection = true;
		return "post_formParam_collection: " + formParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_collection_opt;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/collection/opt'
	@WebRoute(path = "/post/formParam/collection/opt", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_collection_opt(@FormParam Optional<Collection<String>> formParam) {
		this.post_formParam_collection_opt = true;
		return "post_formParam_collection_opt: " + formParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_list;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list'
	@WebRoute(path = "/post/formParam/list", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_list(@FormParam List<String> formParam) {
		this.post_formParam_list = true;
		return "post_formParam_list: " + formParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_list_opt;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/list/opt'
	@WebRoute(path = "/post/formParam/list/opt", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_list_opt(@FormParam Optional<List<String>> formParam) {
		this.post_formParam_list_opt = true;
		return "post_formParam_list_opt: " + formParam.orElse(List.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_set;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set'
	@WebRoute(path = "/post/formParam/set", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_set(@FormParam Set<String> formParam) {
		this.post_formParam_set = true;
		return "post_formParam_set: " + formParam.stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_set_opt;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/set/opt'
	@WebRoute(path = "/post/formParam/set/opt", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_set_opt(@FormParam Optional<Set<String>> formParam) {
		this.post_formParam_set_opt = true;
		return "post_formParam_set_opt: " + formParam.orElse(Set.of()).stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_array;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array'
	@WebRoute(path = "/post/formParam/array", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_array(@FormParam String[] formParam) {
		this.post_formParam_array = true;
		return "post_formParam_array: " + Arrays.stream(formParam).collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_array_opt;
	
	// curl --insecure -iv -d 'formParam=a,b,c' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
	// curl --insecure -iv -d 'formParam=a,b,c&formParam=d,e,f' -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
	// curl --insecure -iv -H 'content-type: application/x-www-form-urlencoded' -X POST 'http://127.0.0.1:8080/post/formParam/array/opt'
	@WebRoute(path = "/post/formParam/array/opt", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public String post_formParam_array_opt(@FormParam Optional<String[]> formParam) {
		this.post_formParam_array_opt = true;
		return "post_formParam_array_opt: " + formParam.map(Arrays::stream).orElse(Stream.of()).collect(Collectors.joining(", "));
	}
	
	public boolean post_formParam_flux;
	
	// curl -iv -X POST -H 'content-type:application/x-www-form-urlencoded' -d 'a=1&b=2&c=3' 'http://127.0.0.1:8080/post/formParam/flux'
	@WebRoute(path = "/post/formParam/flux", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
	public Flux<String> post_formParam_flux(@Body Flux<Parameter> data) {
		this.post_formParam_flux = true;
		return Flux.concat(Mono.just("post_formParam_flux: "), data.map(p -> p.getName() + "=" + p.getValue() + ", "));
	}
	
	public boolean post_raw;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw'
	@WebRoute(path = "/post_raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public String post_raw(@Body ByteBuf data) {
		this.post_raw = true;
		return "post_raw: " + data.toString(Charsets.DEFAULT);
	}
	
	public boolean post_raw_raw;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_raw'
	@WebRoute(path = "/post_raw_raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public ByteBuf post_raw_raw(@Body ByteBuf data) {
		this.post_raw_raw = true;
		
		try {
			ByteBuf buffer = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buffer.writeCharSequence("post_raw_raw: ", Charsets.DEFAULT);
			buffer.writeBytes(data);
			
			return buffer;
		}
		finally {
			data.release();
		}
	}
	
	public boolean post_raw_pub_raw;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_pub_raw'
	@WebRoute(path = "/post_raw_pub_raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Publisher<ByteBuf> post_raw_pub_raw(@Body ByteBuf data) {
		this.post_raw_pub_raw = true;
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("post_raw_pub_raw: ", Charsets.DEFAULT))), Flux.just(data));
	}
	
	public boolean post_raw_mono_raw;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_mono_raw'
	@WebRoute(path = "/post_raw_mono_raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<ByteBuf> post_raw_mono_raw(@Body ByteBuf data) {
		this.post_raw_mono_raw = true;
		try {
			ByteBuf buffer = Unpooled.unreleasableBuffer(Unpooled.buffer());
			buffer.writeCharSequence("post_raw_mono_raw: ", Charsets.DEFAULT);
			buffer.writeBytes(data);
			
			return Mono.just(buffer);
		}
		finally {
			data.release();
		}
	}
	
	public boolean post_raw_flux_raw;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw_flux_raw'
	@WebRoute(path = "/post_raw_flux_raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Flux<ByteBuf> post_raw_flux_raw(@Body ByteBuf data) {
		this.post_raw_flux_raw = true;
		return Flux.concat(Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("post_raw_flux_raw: ", Charsets.DEFAULT))), Mono.just(data));
	}
	
	public boolean post_raw_pub;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/pub'
	@WebRoute(path = "/post_raw/pub", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_raw_pub(@Body Publisher<ByteBuf> data) {
		this.post_raw_pub = true;
		return Flux.from(data).reduceWith(
			() -> new StringBuilder("post_raw_pub: "), 
			(acc, chunk) -> {
				try {
					return acc.append(chunk.toString(Charsets.DEFAULT));
				}
				finally {
					chunk.release();
				}
			}
		).map(StringBuilder::toString);
	}
	
	public boolean post_raw_mono;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/mono'
	@WebRoute(path = "/post_raw/mono", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_raw_mono(@Body Mono<ByteBuf> data) {
		this.post_raw_mono = true;
		return data.map(chunk -> {
			try {
				return "post_raw_mono: " + chunk.toString(Charsets.DEFAULT);
			}
			finally {
				chunk.release();
			}
		});
	}
	
	public boolean post_raw_flux;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_raw/flux'
	@WebRoute(path = "/post_raw/flux", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_raw_flux(@Body Flux<ByteBuf> data) {
		this.post_raw_flux = true;
		return data.reduceWith(
			() -> new StringBuilder("post_raw_flux: "), 
			(acc, chunk) -> {
				try {
					return acc.append(chunk.toString(Charsets.DEFAULT));
				}
				finally {
					chunk.release();
				}
			}
		).map(StringBuilder::toString);
	}
	
	public boolean post_encoded;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded'
	@WebRoute(path = "/post_encoded", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public String post_encoded(@Body String data) {
		this.post_encoded = true;
		return "post_encoded: " + data;
	}
	
	public boolean post_encoded_no_consume;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: ' -X POST 'http://127.0.0.1:8080/post_encoded/no_consume'
	@WebRoute(path = "/post_encoded/no_consume", method = Method.POST, produces = MediaTypes.TEXT_PLAIN)
	public String post_encoded_no_consume(@Body String data) {
		this.post_encoded_no_consume = true;
		return "post_encoded_no_consume: " + data;
	}
	
	public boolean post_encoded_no_decoder;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/no_decoder'
	@WebRoute(path = "/post_encoded/no_decoder", method = Method.POST, produces = MediaTypes.TEXT_XML)
	public String post_encoded_no_decoder(@Body String data) {
		this.post_encoded_no_decoder = true;
		return "post_encoded_no_decoder: " + data;
	}
	
	public boolean post_encoded_collection;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/collection'
	@WebRoute(path = "/post_encoded/collection", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public String post_encoded_collection(@Body Collection<String> data) {
		this.post_encoded_collection = true;
		return "post_encoded_collection: " + data.stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_encoded_list;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/list'
	@WebRoute(path = "/post_encoded/list", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public String post_encoded_list(@Body List<String> data) {
		this.post_encoded_list = true;
		return "post_encoded_list: " + data.stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_encoded_set;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/set'
	@WebRoute(path = "/post_encoded/set", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public String post_encoded_set(@Body Set<String> data) {
		this.post_encoded_set = true;
		return "post_encoded_set: " + data.stream().collect(Collectors.joining(", "));
	}
	
	public boolean post_encoded_array;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/array'
	@WebRoute(path = "/post_encoded/array", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public String post_encoded_array(@Body String[] data) {
		this.post_encoded_array = true;
		return "post_encoded_array: " + Arrays.stream(data).collect(Collectors.joining(", "));
	}
	
	public boolean post_encoded_pub;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/pub'
	@WebRoute(path = "/post_encoded/pub", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_encoded_pub(@Body Publisher<String> data) {
		this.post_encoded_pub = true;
		return Flux.from(data).collectList().map(l -> "post_encoded_pub: " + l.stream().collect(Collectors.joining(", ")));
	}
	
	public boolean post_encoded_mono;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/mono'
	@WebRoute(path = "/post_encoded/mono", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_encoded_mono(@Body Mono<String> data) {
		this.post_encoded_mono = true;
		return data.map(s -> "post_encoded_mono: " + s);
	}
	
	public boolean post_encoded_flux;
	
	// curl --insecure -iv -d 'a,b,c' -H 'content-type: text/plain' -X POST 'http://127.0.0.1:8080/post_encoded/flux'
	@WebRoute(path = "/post_encoded/flux", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.TEXT_PLAIN)
	public Mono<String> post_encoded_flux(@Body Flux<String> data) {
		this.post_encoded_flux = true;
		return data.collectList().map(l -> "post_encoded_flux: " + l.stream().collect(Collectors.joining(", ")));
	}
	
	public boolean post_encoded_json_dto;
	
	// curl --insecure -iv -d '{"message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto'
	@WebRoute(path = "/post_encoded/json/dto", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_JSON)
	public String post_encoded_json_dto(@Body Message data) {
		this.post_encoded_json_dto = true;
		return "post_encoded_json_dto: " + data.getMessage();
	}
	
	public boolean post_encoded_json_pub_dto;
	
	// curl --insecure -iv -d '{"message":"Hello, world!"}{"message":"Hallo, welt!"}{"message":"Salut, monde!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto'
	@WebRoute(path = "/post_encoded/json/pub/dto", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_encoded_json_dto(@Body Publisher<Message> data) {
		this.post_encoded_json_pub_dto = true;
		return Flux.from(data).reduceWith(() -> new StringBuilder("post_encoded_json_pub_dto: "), (acc, message) -> acc.append(message.getMessage()).append(", ")).map(StringBuilder::toString);
	}
	
	public boolean post_encoded_json_dto_generic;
	
	// curl --insecure -iv -d '{"@type":"string", "message":"Hello, world!"}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/dto/generic'
	@WebRoute(path = "/post_encoded/json/dto/generic", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_JSON)
	public String post_encoded_json_dto_generic(@Body GenericMessage<?> data) {
		this.post_encoded_json_dto_generic = true;
		return "post_encoded_json_dto_generic: " + data.getMessage();
	}
	
	public boolean post_encoded_json_pub_dto_generic;
	
	// curl --insecure -iv -d '{"@type":"string","message":"Hello, world!"}{"@type":"integer","message":123456}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/dto/generic'
	@WebRoute(path = "/post_encoded/json/pub/dto/generic", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_encoded_json_pub_dto_generic(@Body Publisher<GenericMessage<?>> data) {
		this.post_encoded_json_pub_dto_generic = true;
		return Flux.from(data).reduceWith(() -> new StringBuilder("post_encoded_json_pub_dto_generic: "), (acc, message) -> acc.append(message.getMessage()).append(", ")).map(StringBuilder::toString);
	}
	
	public boolean post_encoded_json_map;
	
	// curl --insecure -iv -d '{"a":1, "b":2, "c":3}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/map'
	@WebRoute(path = "/post_encoded/json/map", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_JSON)
	public String post_encoded_json_map(@Body Map<String, Integer> data) {
		this.post_encoded_json_map = true;
		return "post_encoded_json_map: " + data;
	}
	
	public boolean post_encoded_json_pub_map;
	
	// curl --insecure -iv -d '{"a":1, "b":2, "c":3}{"d":4, "e":5, "f":6}{"g":7, "h":8, "i":9}' -H 'content-type: application/json' -X POST 'http://127.0.0.1:8080/post_encoded/json/pub/map'
	@WebRoute(path = "/post_encoded/json/pub/map", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.APPLICATION_JSON)
	public Mono<String> post_encoded_json_pub_map(@Body Publisher<Map<String, Integer>> data) {
		this.post_encoded_json_pub_map = true;
		return Flux.from(data).reduceWith(() -> new StringBuilder("post_encoded_json_pub_map: "), (acc, map) -> acc.append(map.toString()).append(", ")).map(StringBuilder::toString);
	}
	
	public boolean post_multipart_pub;
	
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub'
	@WebRoute(path = "/post_multipart_pub", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_pub(@Body Publisher<Part> data) {
		this.post_multipart_pub = true;
		return Flux.from(data).map(part -> part.getName()).reduceWith(() -> new StringBuilder("post_multipart_pub: "), (acc, name) -> acc.append(name).append(", ")).map(StringBuilder::toString);
	}
	
	public boolean post_multipart_pub_raw;
	
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/raw'
	@WebRoute(path = "/post_multipart_pub/raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_pub_raw(@Body Publisher<Part> data) {
		this.post_multipart_pub_raw = true;
		return Flux.from(data).flatMap(
				part -> Flux.concat(
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(part.getName() + " = ", Charsets.DEFAULT))), 
					part.raw().stream(), 
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", ", Charsets.DEFAULT)))
				)
			)
			.reduceWith(() -> Unpooled.unreleasableBuffer(Unpooled.buffer()), (acc, chunk) -> { try { return acc.writeBytes(chunk); } finally { chunk.release(); } })
			.map(buf -> "post_multipart_pub_raw: " + buf.toString(Charsets.DEFAULT));
	}
	
	public boolean post_multipart_pub_encoded;
	
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_pub/encoded'
	@WebRoute(path = "/post_multipart_pub/encoded", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_pub_encoded(@Body Publisher<WebPart> data) {
		this.post_multipart_pub_raw = true;
		return Flux.from(data).flatMap(part -> part.decoder(Integer.class).one().map(value -> part.getName() + " = " + value)).reduceWith(() -> new StringBuilder("post_multipart_pub_encoded: "), (acc, value) -> acc.append(value).append(", ")).map(StringBuilder::toString);
	}
	
	public boolean post_multipart_mono;

	// curl --insecure -iv --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono'
	@WebRoute(path = "/post_multipart_mono", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_mono(@Body Mono<Part> data) {
		this.post_multipart_mono = true;
		return data.map(part -> "post_multipart_mono: " + part.getName());
	}
	
	public boolean post_multipart_mono_raw;

	// curl --insecure -iv --form 'a=1' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/raw'
	@WebRoute(path = "/post_multipart_mono/raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_mono_raw(@Body Mono<Part> data) {
		this.post_multipart_mono_raw = true;
		return data.flatMapMany(
				part -> Flux.concat(
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(part.getName() + " = ", Charsets.DEFAULT))), 
					part.raw().stream(), 
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", ", Charsets.DEFAULT)))
				)
			)
			.reduceWith(() -> Unpooled.unreleasableBuffer(Unpooled.buffer()), (acc, chunk) -> { try { return acc.writeBytes(chunk); } finally { chunk.release(); } })
			.map(buf -> "post_multipart_mono_raw: " + buf.toString(Charsets.DEFAULT));
	}
	
	public boolean post_multipart_mono_encoded;
	
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_mono/encoded'
	@WebRoute(path = "/post_multipart_mono/encoded", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_mono_encoded(@Body Mono<WebPart> data) {
		this.post_multipart_mono_encoded = true;
		return data.flatMap(part -> part.decoder(Integer.class).one().map(value -> part.getName() + " = " + value)).map(value -> "post_multipart_mono_encoded: " + value);
	}
	
	public boolean post_multipart_flux;
	
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux'
	@WebRoute(path = "/post_multipart_flux", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_flux(@Body Flux<Part> data) {
		this.post_multipart_flux = true;
		return Flux.from(data).map(part -> part.getName()).reduceWith(() -> new StringBuilder("post_multipart_flux: "), (acc, name) -> acc.append(name).append(", ")).map(StringBuilder::toString);
	}
	
	public boolean post_multipart_flux_raw;
	
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/raw'
	@WebRoute(path = "/post_multipart_flux/raw", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_flux_raw(@Body Flux<Part> data) {
		this.post_multipart_flux_raw = true;
		return data.flatMap(
				part -> Flux.concat(
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(part.getName() + " = ", Charsets.DEFAULT))), 
					part.raw().stream(), 
					Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", ", Charsets.DEFAULT)))
				)
			)
			.reduceWith(() -> Unpooled.unreleasableBuffer(Unpooled.buffer()), (acc, chunk) -> { try { return acc.writeBytes(chunk); } finally { chunk.release(); } })
			.map(buf -> "post_multipart_pub_raw: " + buf.toString(Charsets.DEFAULT));
	}
	
	public boolean post_multipart_flux_encoded;
	
	// curl --insecure -iv --form 'a=1' --form 'b=2' --form 'c=3' -X POST 'http://127.0.0.1:8080/post_multipart_flux/encoded'
	@WebRoute(path = "/post_multipart_flux/encoded", method = Method.POST, produces = MediaTypes.TEXT_PLAIN, consumes = MediaTypes.MULTIPART_FORM_DATA)
	public Mono<String> post_multipart_flux_encoded(@Body Flux<WebPart> data) {
		this.post_multipart_flux_encoded = true;
		return data.flatMap(part -> part.decoder(Integer.class).one().map(value -> part.getName() + " = " + value)).reduceWith(() -> new StringBuilder("post_multipart_flux_encoded: "), (acc, value) -> acc.append(value).append(", ")).map(StringBuilder::toString);
	}
	
	public boolean get_sse_raw;

	// curl --insecure -iv 'http://127.0.0.1:8080/get_sse_raw'
	@WebRoute(path = "/get_sse_raw", method = Method.GET)
	public Publisher<ResponseBody.Sse.Event<ByteBuf>> get_sse_raw(@SseEventFactory ResponseBody.Sse.EventFactory<ByteBuf, ResponseBody.Sse.Event<ByteBuf>> events) {
		this.get_sse_raw = true;
		return Flux.interval(Duration.ofMillis(100))
			.take(5)
			.map(seq -> events.create(
					event -> event
						.id(Long.toString(seq))
						.event("get_sse_raw")
						.value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("SSE - " + seq + "\r\n", Charsets.UTF_8)))
				)
			)
			.doOnNext(evt -> System.out.println("Emit sse"));
	}
	
	public boolean get_sse_encoded;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_sse_encoded'
	@WebRoute(path = "/get_sse_encoded", method = Method.GET)
	public Publisher<WebResponseBody.SseEncoder.Event<String>> get_sse_encoded(@SseEventFactory WebResponseBody.SseEncoder.EventFactory<String> events) {
		this.get_sse_encoded = true;
		return Flux.interval(Duration.ofMillis(100))
			.take(5)
			.map(seq -> events.create(
					event -> event
						.id(Long.toString(seq))
						.event("get_sse_encoded_json")
						.value("SSE - " + seq + "\r\n")
				)
			)
			.doOnNext(evt -> System.out.println("Emit sse"));
	}
	
	public boolean get_sse_encoded_json;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_sse_encoded/json'
	@WebRoute(path = "/get_sse_encoded/json", method = Method.GET)
	public Publisher<WebResponseBody.SseEncoder.Event<Message>> get_sse_encoded_json(@SseEventFactory("application/json") WebResponseBody.SseEncoder.EventFactory<Message> events) {
		this.get_sse_encoded_json = true;
		return Flux.interval(Duration.ofMillis(100))
			.take(5)
			.map(seq -> events.create(
					event -> event
						.id(Long.toString(seq))
						.event("get_sse_encoded_json")
						.value(new Message("SSE - " + seq + "\r\n"))
				)
			)
			.doOnNext(evt -> System.out.println("Emit sse"));
	}
	
	public boolean get_sse_encoded_json_map;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_sse_encoded/json/map'
	@WebRoute(path = "/get_sse_encoded/json/map", method = Method.GET)
	public Publisher<WebResponseBody.SseEncoder.Event<Map<String, Integer>>> get_sse_encoded_json_map(@SseEventFactory("application/json") WebResponseBody.SseEncoder.EventFactory<Map<String, Integer>> events) {
		this.get_sse_encoded_json_map = true;
		
		// We must do that to have a fixed order
		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("a",1);
		map.put("b",2);
		map.put("c",3);
		
		return Flux.interval(Duration.ofMillis(100))
			.take(5)
			.map(seq -> events.create(
					event -> event
						.id(Long.toString(seq))
						.event("get_sse_encoded_json_map")
						.value(map)
				)
			)
			.doOnNext(evt -> System.out.println("Emit sse"));
	}
	
	public boolean get_resource;
	
	// curl --insecure -iv 'http://127.0.0.1:8080/get_resource'
	@WebRoute(path = "/get_resource", method = Method.GET)
	public Resource get_resource() {
		this.get_resource = true;
		return this.resourceService.getResource(new File("src/test/resources/get_resource.txt").toURI());
	}
	
	public boolean get_path_param_qmark;
	
	@WebRoute(path = "/get_path_param/qmark_?_", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_path_param_qmark(WebExchange<? extends ExchangeContext> exchange) {
		this.get_path_param_qmark = true;
		return exchange.request().getPathAbsolute();
	}
	
	public boolean get_path_param_wcard;
	
	@WebRoute(path = "/get_path_param/wcard_*_", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_path_param_wcard(WebExchange<? extends ExchangeContext> exchange) {
		this.get_path_param_wcard = true;
		return exchange.request().getPathAbsolute();
	}
	
	public boolean get_path_param_directories;
	
	@WebRoute(path = "/get_path_param/directories/**", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_path_param_directories(WebExchange<? extends ExchangeContext> exchange) {
		this.get_path_param_directories = true;
		return exchange.request().getPathAbsolute();
	}
	
	public boolean get_path_param_directories_jsp;
	
	@WebRoute(path = "/get_path_param/jsp/**/{jspfile:[^/]*\\.jsp}", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_path_param_directories_jsp(WebExchange<? extends ExchangeContext> exchange) {
		this.get_path_param_directories_jsp = true;
		return exchange.request().getPathAbsolute() + " - " + exchange.request().pathParameters().get("jspfile").get().getValue();
	}
	
	public boolean get_path_param_terminal;
	
	@WebRoute(path = "/get_path_param/terminal/**/*", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public String get_path_param_terminal(WebExchange<? extends ExchangeContext> exchange) {
		this.get_path_param_terminal = true;
		return exchange.request().getPathAbsolute();
	}
	
	@WebRoute(path = "/get_delay100", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
	public Mono<String> get_delay100() {
		return Mono.just("get_delay100").delayElement(Duration.ofMillis(100));
	}
}
