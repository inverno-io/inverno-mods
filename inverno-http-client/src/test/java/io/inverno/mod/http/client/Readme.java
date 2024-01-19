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
package io.inverno.mod.http.client;

import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.SetCookieParameter;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.base.ws.WebSocketMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {

	private static final Logger LOGGER = LogManager.getLogger(Readme.class);
	
	public void doc() {
		HttpClient client = null;
		String path = "/";
		
		Endpoint endpoint = client.endpoint("example.org", 80).build();
		
		endpoint
			.request(Method.GET, path)
			.send()
			.flatMapMany(exchange -> exchange
				.response()
				.body()
				.string().stream()
			)
			.collect(Collectors.joining())
			.block();
		
		HttpClient.Request<ExchangeContext, Exchange<ExchangeContext>, InterceptableExchange<ExchangeContext>> request = client
			.request(Method.GET, "/")
			.headers(headers -> headers
				.contentType(MediaTypes.APPLICATION_JSON)
				.add("SomeHeader", "SomeValue")
				.cookies(cookies -> cookies
					.addCookie("SESSION", "12345")
				)
			);
		
		Endpoint endpoint1 = null;
		Endpoint endpoint2 = null;
		
		endpoint1.send(request);
		endpoint2.send(request);
		
		Map<String, ?> values = null;
		endpoint.request(
			Method.GET, 
			URIs.uri(
				"/some/path/{id}?p1={p1}", 
				URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED
			)
			.buildString(values)
		);
		
		endpoint
			.request(Method.GET, path)
			.body(body -> body.string(). stream(Flux.just("a", "b", "c")))
			.send();
		
		endpoint
			.request(Method.GET, path)
			.body(body -> body.raw().stream(
				Flux.just(
					Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)),
					Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(" world!", Charsets.DEFAULT))
				)
			))
			.send();
		
		endpoint
			.request(Method.GET, path)
			.body(body -> body.resource().value(new FileResource("/path/to/resource")))
			.send();
		
		
		endpoint
			.request(Method.GET, path)
			.body(body -> body.urlEncoded()
				.from((factory, data) -> data.stream(
					Flux.just(
						factory.create("param1", 1234), 
						factory.create("param2", "abc")
					)
				))
			)
			.send();
		
		endpoint
			.request(Method.POST, "/some/path")
			.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
				factory.string(part -> part
					.name("param1")
					.headers(headers -> headers
						.contentType(MediaTypes.TEXT_PLAIN)
					)
					.value("1234")
				),
				factory.string(part -> part
					.name("param2")
					.headers(headers -> headers
						.contentType(MediaTypes.APPLICATION_JSON)
					)
					.value("{\"value\":123}")
				),
				factory.resource(part -> part
					.name("file")
					.value(new FileResource("/path/to/resource"))
				)
			))))
			.send();
		
		endpoint
			.request(Method.GET, path)
			.send()
			.map(exchange -> {
				// Returns the value of the first occurence of 'some-header' as string or returns null
				String someHeaderValue = exchange.response().headers().get("some-header").orElse(null);
    
				// Returns all 'some-header' values as strings
				List<String> someHeaderValues = exchange.response().headers().getAll("some-header");

				// Returns all headers as strings
				List<Map.Entry<String, String>> allHeadersValues = exchange.response().headers().getAll();
				
				return Mono.empty();
			});
		
		endpoint
			.request(Method.GET, path)
			.send()
			.map(exchange -> {
				// Returns the value of the first occurence of 'some-header' as LocalDateTime or returns null
				LocalDateTime someHeaderValue = exchange.response().headers().getParameter("some-header").map(Parameter::asLocalDateTime).orElse(null);

				// Returns all 'some-header' values as LocalDateTime
				List<LocalDateTime> someHeaderValues = exchange.response().headers().getAllParameter("some-header").stream().map(Parameter::asLocalDateTime).collect(Collectors.toList());

				// Returns all headers as parameters
				List<Parameter> allHeadersParameters = exchange.response().headers().getAllParameter();
				
				return Mono.empty();
			});
		
		endpoint
			.request(Method.GET, path)
			.send()
			.map(exchange -> {
				// Returns the decoded 'content-type' header or null
				Headers.ContentType contenType = exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);

				String mediaType = contenType.getMediaType();
				Charset charset = contenType.getCharset();

				return Mono.empty();
			});
		
		endpoint
			.request(Method.GET, path)
			.send()
			.map(exchange -> {
				if(exchange.response().headers().getStatus().getCode() >= 400) {
					// Report Error
				}
				
				return Mono.empty();
			});
		
		endpoint
			.request(Method.GET, path)
			.send()
			.map(exchange -> {
				// Returns the value of the first occurence of 'some-cookie' as LocalDateTime or returns null
				LocalDateTime someCookieValue = exchange.response().headers().cookies().get("some-cookie").map(Parameter::asLocalDateTime).orElse(null);

				// Returns all 'some-cookie' values as LocalDateTime
				List<LocalDateTime> someCookieValues = exchange.response().headers().cookies().getAll("some-cookie").stream().map(Parameter::asLocalDateTime).collect(Collectors.toList());

				// Returns all cookies as set-cookie parameters
				Map<String, List<SetCookieParameter>> allCookieParameters = exchange.response().headers().cookies().getAll();
				
				return Mono.empty();
			});
		
		endpoint
			.request(Method.GET, path)
			.send()
			.map(exchange -> {
				SetCookieParameter someCookie = exchange.response().headers().cookies().get("some-cookie").orElse(null);
				
				ZonedDateTime expires = someCookie.getExpires();
				int maxAge = someCookie.getMaxAge();
				String domain = someCookie.getDomain();
				String path2 = someCookie.getPath();
				boolean isSecure = someCookie.isSecure();
				boolean isHttp = someCookie.isHttpOnly();
				Headers.SetCookie.SameSitePolicy sameSitePolicy = someCookie.getSameSite();
				
				return Mono.empty();
			});
		
		endpoint
			.request(Method.GET, path)
			.send()
			.flatMapMany(exchange -> exchange.response().body().string().stream())
			.subscribe((CharSequence chunk) -> {
				
			});
		
		endpoint
			.request(Method.GET, path)
			.send()
			.flatMapMany(exchange -> exchange.response().body().raw().stream())
			.subscribe((ByteBuf chunk) -> {
				
			});

		endpoint
			.request(Method.GET, path)
			.send()
			.flatMapMany(exchange -> exchange.response().body().string().stream())
			.collect(Collectors.joining())
			.subscribe(
				body -> {
				
				},
				error -> LOGGER.error("There was an error requesting the server", error)
			);
		
		endpoint
			.request(Method.GET, path)
			.send()
			.retry(5)
			.flatMapMany(exchange -> exchange.response().body().string().stream())
			.collect(Collectors.joining())
			.subscribe(
				body -> {
				
				},
				error -> LOGGER.error("There was an error requesting the server", error)
			);
		
		ExchangeContext context = null;
		
		/*endpoint
			.request(Method.GET, path, context)
			.intercept(exchange -> {
				exchange.request().headers(headers -> headers
					.add(Headers.NAME_AUTHORIZATION, "Bearer " + exchange.context().getToken())
				);
				
				return Mono.just(exchange);
			})
			.send();*/
		
		endpoint
			.request(Method.GET, path)
			.intercept(exchange -> {
				final StringBuilder requestBodyBuilder = new StringBuilder();
				exchange.request().body().ifPresent(body -> body.transform(data -> Flux.from(data)
					.doOnNext(buf -> requestBodyBuilder.append(buf.toString(Charsets.UTF_8)))
					.doOnComplete(() -> System.out.println("Request Body: \n" + requestBodyBuilder.toString()))
				));

				final StringBuilder responseBodyBuilder = new StringBuilder();
				exchange.response().body().transform(data -> Flux.from(data)
					.doOnNext(buf -> responseBodyBuilder.append(buf.toString(Charsets.UTF_8)))
					.doOnComplete(() -> System.out.println("Response Body: \n" + responseBodyBuilder.toString()))
				);
				
				return Mono.just(exchange);
			})
			.send();
		
		endpoint
			.request(Method.POST, path)                         // Creates the request 
			.intercept(exchange -> {
				final StringBuilder requestBodyBuilder = new StringBuilder();
				exchange.request().body().ifPresent(body -> body.transform(data -> Flux.from(data)
					.doOnNext(buf -> requestBodyBuilder.append(buf.toString(Charsets.UTF_8)))
					.doOnComplete(() -> System.out.println("Request Body: \n" + requestBodyBuilder.toString()))
				));

				final StringBuilder responseBodyBuilder = new StringBuilder();
				exchange.response().body().transform(data -> Flux.from(data)
					.doOnNext(buf -> responseBodyBuilder.append(buf.toString(Charsets.UTF_8)))
					.doOnComplete(() -> System.out.println("Response Body: \n" + responseBodyBuilder.toString()))
				);

				return Mono.just(exchange);
			})
			.body(body -> body.string().value("test"))
			.send()                                            // Sends the request
			.flatMapMany(exchange -> exchange                  // Streams the response
				.response()
				.body()
				.string().stream()
			)
			.collect(Collectors.joining())                     // Aggregates the response
			.block();
	
		endpoint
			.request(Method.GET, path)
			.intercept(exchange -> {
				exchange.response()
					.headers(headers -> headers
						.status(Status.OK)
					)
					.body()
						.string().value("You have been intercepted");
				
				return Mono.empty();
			})
			.send();
		
		endpoint
			.webSocketRequest("/some/path/ws")
			.headers(headers -> headers
				.add("some-header", "value")
				.cookies(cookies -> cookies.addCookie("some-cookie", 123))
			)
			.subProtocol("xml")
			.send()
			.subscribe(wsExchange -> {
			});
		
		endpoint
			.webSocketRequest("/some/path/ws")
			.send()
			.flatMapMany(wsExchange -> wsExchange.inbound().frames())
			.subscribe(frame -> {
				try {
					LOGGER.info("Received WebSocket frame: kind = " + frame.getKind() + ", final = " + frame.isFinal() + ", size = " + frame.getBinaryData().readableBytes());					
				}
				finally {
					frame.release();
				}
			});
		
		endpoint
			.webSocketRequest("/some/path/ws")
			.send()
			.flatMapMany(wsExchange -> wsExchange.inbound().messages())
			.flatMap(message -> {
				// The stream of frames composing the message
                Publisher<WebSocketFrame> frames = message.frames();
            
                // The message data as stream of ByteBuf
                Publisher<ByteBuf> binary = message.binary();
                
                // The message data as stream of String
                Publisher<String> text = message.text();
                
                // Aggregate all fragments into a single ByteBuf
                Mono<ByteBuf> reducedBinary = message.reducedBinary();
                
                // Aggregate all fragments into a single String
                Mono<String> reducedText = message.reducedText();
				
				return Mono.empty();
			});

		Sinks.Many<String> framesSink = Sinks.many().unicast().onBackpressureBuffer();
		endpoint
			.webSocketRequest("/some/path/ws")
			.send()
			.flatMapMany(wsExchange -> {
				wsExchange.outbound()
					.frames(factory -> framesSink.asFlux().map(factory::text));

				return wsExchange.inbound().frames();
			})
			.subscribe(frame -> {
				try {
					LOGGER.info("Received WebSocket frame: kind = " + frame.getKind() + ", final = " + frame.isFinal() + ", size = " + frame.getBinaryData().readableBytes());					
				}
				finally {
					frame.release();
				}
			});

		framesSink.tryEmitNext("test1");
		framesSink.tryEmitNext("test2");
		framesSink.tryEmitNext("test3");
		framesSink.tryEmitComplete();
		
		Sinks.Many<List<String>> messagesSink = Sinks.many().unicast().onBackpressureBuffer();
		endpoint
			.webSocketRequest("/some/path/ws")
			.send()
			.flatMapMany(wsExchange -> {
				wsExchange.outbound().closeOnComplete(true).messages(factory -> messagesSink.asFlux().map(Flux::fromIterable).map(factory::text));
				
				return wsExchange.inbound().messages();
			})
			.flatMap(WebSocketMessage::reducedText)
			.subscribe(message -> {
				LOGGER.info("Received WebSocket message: {}", message );
			});
		
		messagesSink.tryEmitNext(List.of("One frame"));
		messagesSink.tryEmitNext(List.of("Multiple ", "frames"));
		messagesSink.tryEmitComplete();
	}
}
