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
package io.winterframework.mod.web.lab.router.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.buffer.Unpooled;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Wrapper;
import io.winterframework.core.annotation.Bean.Strategy;
import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.Charsets;
import io.winterframework.mod.web.lab.router.BaseContext;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author jkuhn
 *
 */
//@Bean(strategy = Strategy.SINGLETON)
//@Wrapper
public class TestRouter implements Supplier<RequestHandler<RequestBody, Void, ResponseBody>> {

	private HeaderService headerService;
	
	/**
	 * 
	 */
	public TestRouter(HeaderService headerService) {
		this.headerService = headerService;
	}

	@Override
	public RequestHandler<RequestBody, Void, ResponseBody> get() {
		System.out.println("=== create ===");
		return new GenericBaseRouter(this.headerService)
				.route().path("/toto", true).method(Method.GET).handler(this.simple().map(this::handlerAdapter))
				.route().path("/tata", true).method(Method.POST).handler(this.echo().map(this::handlerAdapter))
				.route().path("/json", true).method(Method.POST).handler(this.json().map(this::handlerAdapter))
				.route().path("/toto/{param1}/tata/{param2}", true).handler(this.a().map(this::handlerAdapter))
				.route().path("/toto/titi/tata/{param2}", true).handler(this.b().map(this::handlerAdapter))
				.route().path("/toto/{param1}/tata/titi", true).handler(this.c().map(this::handlerAdapter));
	}

	private RequestHandler<RequestBody, BaseContext, ResponseBody> handlerAdapter(RequestHandler<RequestBody, Void, ResponseBody> handler) {
		return handler.map(h -> (request, response) -> h.handle(request.map(Function.identity(), ign -> null), response));
	}
	
	private RequestHandler<RequestBody, Void, ResponseBody> simple() {
		return (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=\"UTF-8\"")
				.add("test", "1235")
			)
			.body().data().data(
				Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Server Response : Version - HTTP/2", Charsets.UTF_8)))
			);
	}
	
	private RequestHandler<RequestBody, Void, ResponseBody> echo() {
		return (request, response) -> {
			response
				.headers(headers -> headers.status(200).contentType("text/plain"))
				.body().data().data(request.body()
					.map(body -> body.data().data().doOnNext(chunk -> chunk.retain()))	
					.orElse(Flux.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("=== Empty ===", Charsets.UTF_8))))
				);
		};
	}
	
	public static class JsonRequest {
		private String field1;
		
		private String field2;

		public String getField1() {
			return field1;
		}

		public void setField1(String field1) {
			this.field1 = field1;
		}

		public String getField2() {
			return field2;
		}

		public void setField2(String field2) {
			this.field2 = field2;
		}
	}
	
	public static class JsonResponse {
		private String message;
		
		public String getMessage() {
			return message;
		}
		
		public void setMessage(String message) {
			this.message = message;
		}
	}
	
	public static class EntityResponseBody<T> {
		
		private Response<ResponseBody> response;
		
		private T entity;
		
		private EntityResponseBody(Response<ResponseBody> response) {
			this.response = response;
		}
		
		public Response<EntityResponseBody<T>> entity(T entity) {
			this.entity = entity;
			return this.response.map(ign -> this);
		}
		
		private T getEntity() {
			return entity;
		}
	}
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private RequestHandler<RequestBody, Void, ResponseBody> json() {
		
		Function<JsonRequest, JsonResponse> handler0 = request -> {
			JsonResponse response = new JsonResponse();
			response.setMessage("Received request with field1 " + request.field1 + " and field2 " + request.field2);
			return response;
		};
		
		RequestHandler<JsonRequest, Void, EntityResponseBody<JsonResponse>> handler1 = (request, response) -> {
			response.headers(headers -> headers.contentType("application/json")).body().entity(handler0.apply(request.body().get())); 
		};
		
		return handler1.map(handler -> 
			 (request, response) -> {
				 
				if(request.headers().<Headers.ContentType>get(Headers.CONTENT_TYPE).get().getMediaType().equals("application/json")) {
					// convert json
				}
				else if(request.headers().<Headers.ContentType>get(Headers.CONTENT_TYPE).get().getMediaType().equals("application/xml")) {
					// convert xml
				}
				 
				response.body().data().data(
					request.body().get().data().data()
						.reduce(new ByteArrayOutputStream(), (out, chunk) -> {
							try {
								chunk.getBytes(chunk.readerIndex(), out, chunk.readableBytes());
							} 
							catch (IOException e) {
								throw Exceptions.propagate(e);
							}
							return out;
						})
						.map(ByteArrayOutputStream::toByteArray)
						.map(bytes -> {
							try {
								return this.mapper.readValue(bytes, JsonRequest.class);
							}
							catch (IOException e) {
								throw Exceptions.propagate(e);
							}
						})
						.map(jsonRequest -> {
							Request<JsonRequest, Void> entityRequest = request.map(ign -> jsonRequest, Function.identity());
							Response<EntityResponseBody<JsonResponse>> entityResponse = response.map(body -> {
								return new EntityResponseBody<>(response);
							});
							handler.handle(entityRequest, entityResponse);
							
							// response entity can come in an asynchronous way so we must delegate the whole process to the other handler
							// if we want to chain things we need to use publishers
							// handler1 is actually synchronous since there are no publisher accessible in handler1
							
							return entityResponse.body().getEntity();
						})
						.flatMap(jsonResponse -> {
							try {
								return Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(this.mapper.writeValueAsBytes(jsonResponse))));
							} 
							catch (JsonProcessingException e) {
								throw Exceptions.propagate(e);
							}
						})
					);
		});
	}
	
	private RequestHandler<RequestBody, Void, ResponseBody> a() {
		return (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=\"UTF-8\"")
			)
			.body().data().data(
				Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("= A =", Charsets.UTF_8)))
			);
	}
	
	private RequestHandler<RequestBody, Void, ResponseBody> b() {
		return (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=\"UTF-8\"")
			)
			.body().data().data(
				Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("= B =", Charsets.UTF_8)))
			);
	}
	
	private RequestHandler<RequestBody, Void, ResponseBody> c() {
		return (request, response) -> response
			.headers(headers -> headers
				.status(200)
				.contentType("text/plain; charset=\"UTF-8\"")
			)
			.body().data().data(
				Mono.just(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("= C =", Charsets.UTF_8)))
			);
	}
}
