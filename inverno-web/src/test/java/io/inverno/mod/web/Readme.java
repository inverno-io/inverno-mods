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
package io.inverno.mod.web;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {
	
	public static interface Book {
		
	}
	
	public static class Message {
		
		public Message(String message) {
			
		}
	}
	
	public static interface Result {
		String getMessage();
	}
	
	public static Result storeBook(Book book) {
		return null;
	}
	
	public static class SomeCustomException extends RuntimeException {
		
	}
			
	
	public void doc() {
		
		ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
			Mono<Result> storeBook = exchange.request().body().get()
				.decoder(Book.class)
				.one()
				.map(book -> storeBook(book));
			exchange.response().body()
				.string().stream(storeBook.map(result -> result.getMessage()));
		};
		
		WebRoutable<ExchangeContext, ?> routable = null;
		
		routable
			.webSocketRoute()
				.path("/ws")
				.subprotocol("json")
				.handler(webSocketExchange -> {
					webSocketExchange.outbound().messages(factory -> webSocketExchange.inbound().messages());
				});
		
		routable
			.webSocketRoute()
				.subprotocol("json")
				.findRoutes()
				.stream()
				.forEach(WebSocketRoute::disable);
		
		routable
			.route()
				.method(Method.GET)
				.findRoutes()
				.stream()
				.filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
				.forEach(WebRoute::disable);
		
		routable
			.route()
				.path("/ws")
				.findRoutes()
				.stream()
				.forEach(WebRoute::enable);
		
		WebInterceptable<ExchangeContext, ?> interceptable = null;
		
		interceptable
			.intercept()
				.path("/doc")
				.path("/document")
				.consumes(MediaTypes.APPLICATION_JSON)
				.consumes(MediaTypes.APPLICATION_XML)
				.interceptor(exchange -> {
					return Mono.just(exchange);
				});
		
		ResourceService resourceService = null;
		
		WebRouter<ExchangeContext> router = null;
		
		router
			.route()
				.path("/public")
				.handler(exchange -> {
				
				})
			.intercept()
				.interceptor(exchange -> {
					return Mono.just(exchange);
				})
			.route()
				.path("/private")
				.handler(exchange -> {
				
				})
			.getRouter()
			.route()
				.path("/static/**")
				.handler(new StaticHandler<>(resourceService.getResource(URI.create("file:/path/to/web-root/"))));
		
		routable
			.route()
				.path("/ws")
				.enable();
		
		ExchangeHandler<ExchangeContext, ErrorWebExchange<ExchangeContext>> errorHandler = errorExchange -> {
			errorExchange.response()
				.headers(headers -> headers.status(Status.INTERNAL_SERVER_ERROR))
				.body()
				.encoder(Message.class)
				.value(new Message(errorExchange.getError().getMessage()));
		};
		
		ErrorWebRoutable<ExchangeContext, ?> errorRoutable = null;
		
		errorRoutable
			.route()
				.error(SomeCustomException.class)
				.disable();
		
		ErrorWebInterceptable<ExchangeContext, ?> errorInterceptable = null;
		
		errorInterceptable
			.intercept()
				.path("/some_path")
				.error(SomeCustomException.class)
				.interceptor(errorExchange -> {
					return Mono.just(errorExchange);
				});
		
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = null;
		ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = null;
		
		interceptable
			.intercept()
				.path("/some_path")
				.interceptors(List.of(interceptor1, interceptor2));
		
		
		ErrorWebRoute<ExchangeContext> errorRoute = null;
		
		List<ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>> errorRouteInterceptors = new ArrayList<>(errorRoute.getInterceptors());
		errorRouteInterceptors.add(errorExchange -> {
			return Mono.just(errorExchange);
		});
		
		errorRoute.setInterceptors(errorRouteInterceptors);
		
		
		ErrorWebInterceptorsConfigurer<ExchangeContext> public_error_interceptors_configurer = errInterceptable -> {
			errInterceptable
				.intercept();
		};

		ErrorWebInterceptorsConfigurer<ExchangeContext> private_error_interceptors_configurer = errInterceptable -> {
			errInterceptable
				.intercept();
		};

		errorInterceptable
			.configureInterceptors(public_error_interceptors_configurer)
			.configureInterceptors(private_error_interceptors_configurer)
			.intercept();
	}
}
