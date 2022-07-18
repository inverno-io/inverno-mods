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
package io.inverno.mod.http.server;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.Headers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {
	
	private static final Logger LOGGER = LogManager.getLogger(Readme.class);
	
	public void doc() {
		
		ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
			exchange.response().body().string().value("Hello, world!");
		};
		
		handler = exchange -> {
			exchange.response()
				.headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
				.body()
				.raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello, world!", Charsets.DEFAULT)));
		};

		handler = exchange -> {
			Flux<ByteBuf> dataStream = Flux.just(
				Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)),
				Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", world!", Charsets.DEFAULT))
			);

			exchange.response()
				.body().raw().stream(dataStream);
		};

		handler = exchange -> {
			exchange.response()
				.body().string().stream(Flux.just("Hello", ", world!"));
		};
		
		handler = exchange -> {
			exchange.response()
				.body().resource().value(new FileResource("/path/to/resource"));
		};
		
		handler = exchange -> {
			exchange.response().body().sse().from(
				(events, data) -> data.stream(Flux.interval(Duration.ofSeconds(1))
					.map(seq -> events.create(event -> event
					.id(Long.toString(seq))
					.event("seq")
					.comment("Some comment")
					.value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Event #" + seq, Charsets.DEFAULT))))
					)
				)
			);
		};
		
		handler = exchange -> {
			exchange.response()
				.body().raw().stream(exchange.request().body()
					.map(body -> Flux.from(body.raw().stream())
						.map(chunk -> {
							try {
								return Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(chunk.readableBytes()));
							}
							finally {
								chunk.release();
							}
						})
					)
					.orElse(Flux.just(Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(0))))
				);
		};
		
		handler = exchange -> {
			exchange.response()
				.body().string().stream(exchange.request().body()
					.map(body -> Flux.from(body.string().stream()).map(s -> Integer.toString(s.length())))
					.orElse(Flux.just("0"))
				);
		};
		
		handler = exchange -> {
			exchange.response()
				.body().raw().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
					.map(parameter -> Unpooled.copiedBuffer(Unpooled.copiedBuffer("Received parameter " + parameter.getName() + " with value " + parameter.getValue(), Charsets.DEFAULT)))
				);
		};
		
		handler = exchange -> {
			exchange.response()
				.body().string().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
					.map(parameter -> "Received parameter " + parameter.getName() + " with value " + parameter.getValue())
				);
		};
		
		ExchangeHandler<ExchangeContext, ErrorExchange<ExchangeContext>> errorHandler = errorExchange -> {
			if (errorExchange.getError() instanceof BadRequestException) {
				errorExchange.response().body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("client sent an invalid request", Charsets.DEFAULT)));
			} 
			else {
				errorExchange.response().body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Unknown server error", Charsets.DEFAULT)));
			}
		};
		
		ExchangeInterceptor<ExchangeContext, Exchange<ExchangeContext>> interceptor = exchange -> {
			LOGGER.info("Path: " + exchange.request().getPath());

			// exchange is returned unchanged and will be processed by the handler
			return Mono.just(exchange);
		};
		
		ReactiveExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> interceptedHandler = handler.intercept(interceptor);
		
		handler = exchange -> {
			// Returns the value of the first occurence of 'some-header' as string or returns null
			String someHeaderValue = exchange.request().headers().get("some-header").orElse(null);
			
			// Returns all 'some-header' values as strings
			List<String> someHeaderValues = exchange.request().headers().getAll("some-header");
			
			// Returns all headers as strings
			List<Map.Entry<String, String>> allHeadersValues = exchange.request().headers().getAll();
		};
		
		handler = exchange -> {
			// Returns the value of the first occurence of 'some-header' as LocalDateTime or returns null
			LocalDateTime someHeaderValue = exchange.request().headers().getParameter("some-header").map(Parameter::asLocalDateTime).orElse(null);
			
			// Returns all 'some-header' values as LocalDateTime
			List<LocalDateTime> someHeaderValues = exchange.request().headers().getAllParameter("some-header").stream().map(Parameter::asLocalDateTime).collect(Collectors.toList());
			
			// Returns all headers as parameters
			List<Parameter> allHeadersParameters = exchange.request().headers().getAllParameter();
		};
		
		handler = exchange -> {
			// Returns the decoded 'content-type' header or null
			Headers.ContentType contenType = exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
			
			String mediaType = contenType.getMediaType();
			Charset charset = contenType.getCharset();
		};
		
		handler = exchange -> {
			exchange.response()
				.trailers(headers -> headers
					.add("some-trailer", "abc")
				)
				.body().empty();
		};
		
		handler = exchange -> {
			exchange.response()
				.headers(headers -> headers
					.contentType(MediaTypes.TEXT_PLAIN)
					.set(Headers.NAME_SERVER, "inverno")
					.add("custom-header", "abc")
				)
				.body().empty();
		};
		
		handler = exchange -> {
			exchange.response()
				.cookies(cookies -> cookies
					.addCookie(cookie -> cookie.name("cookie1")
						.httpOnly(true)
						.secure(true)
						.maxAge(3600)
						.value("abc")
					)
					.addCookie(cookie -> cookie.name("cookie2")
						.httpOnly(true)
						.secure(true)
						.maxAge(3600)
						.value("def")
					)
				)
				.body().empty();
		};
		
		handler = exchange -> {
			exchange.webSocket()
				.orElseThrow(() -> new InternalServerErrorException("WebSocket not supported"))
				.handler(webSocketExchange -> {
					webSocketExchange.outbound().frames(factory -> webSocketExchange.inbound().frames());
					webSocketExchange.close((short)1000, "Goodbye!");
				})
				.or(() -> {
					throw new BadRequestException("Web socket handshake failed");
				});
		};
	}
}

