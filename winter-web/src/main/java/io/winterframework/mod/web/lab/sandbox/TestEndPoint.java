/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.lab.sandbox;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.Response;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
public class TestEndPoint {
/*
	// ? handler(Http2Request)
	public Response<String> someHandler1(Request<String> request) {
		return Response.<String>ok()
			.body(request.body().map(s -> "Response to " + s));
	}
	
	public Flux<String> someHandler2(Request<String> request) {
		return request.body().map(s -> "Response to " + s);
	}
	
	public Mono<String> someHandler3(Request<String> request) {
		return request.body().collect(Collectors.joining(", ")).map(s -> "Response to: " + s);
	}
	
	public String someHandler4(Request<String> request) {
		// At some point we need a result, if we don't do it here we'll do it right before sending the response to the connection
		// As such this is not really a blocking operation like an I/O or DB connection since this will happen eventually it only happens now
		// This is clearly the same method as someHandler3
		return request.body().collect(Collectors.joining(", ")).map(s -> "Response to: " + s).block();
	}
	
	// ? handler(Flux)
	public Response<String> someHandler5(Flux<String> request) {
		return Response.<String>ok()
			.headers(headers -> headers.setStatus(200))
			.body(request.map(s -> "Response to " + s));
	}
	
	public Flux<String> someHandler6(Flux<String> request) {
		return request.map(s -> "Response to " + s);
	}
	
	public Mono<String> someHandler7(Flux<String> request) {
		return request.collect(Collectors.joining(", ")).map(s -> "Response to: " + s);
	}
	
	public String someHandler8(Flux<String> request) {
		// same as someHandler8 
		// see someHandler4
		return request.collect(Collectors.joining(", ")).map(s -> "Response to: " + s).block();
	}
	
	// ? handler(Mono)
	public Response<String> someHandler9(Mono<String> request) {
		return Response.<String>ok()
			.headers(headers -> headers.setStatus(200))
			.body(request.map(s -> "Response to " + s));
	}
	
	public Flux<String> someHandler10(Mono<String> request) {
		return request.map(s -> "Response to " + s).flux();
	}
	
	public Mono<String> someHandler11(Mono<String> request) {
		return request.map(s -> "Response to: " + s);
	}
	
	public String someHandler12(Mono<String> request) {
		// same as someHandler13
		// see someHandler4
		return request.map(s -> "Response to: " + s).block();
	}
	
	// ? handler(A)
	public Response<String> someHandler13(String request) {
		return Response.<String>ok()
			.headers(headers -> headers.setStatus(200))
			.body(Mono.just("Response to: " + request));
	}
	
	public Flux<String> someHandler14(String request) {
		return Flux.fromStream(IntStream.range(0, request.length()).mapToObj(i -> Character.valueOf(request.charAt(i))).map(c -> "Response to: " + c));
	}
	
	public Mono<String> someHandler15(String request) {
		return Mono.just("Response to: " + request);
	}
	
	public String someHandler16(String request) {
		return "Response to: " + request;
	}
	*/
}
