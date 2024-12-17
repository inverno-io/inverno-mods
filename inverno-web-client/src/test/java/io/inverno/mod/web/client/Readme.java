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
package io.inverno.mod.web.client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {

	private static final Logger LOGGER = LogManager.getLogger(Readme.class);

	private WebClient<? extends ExchangeContext> webClient;

	@Bean
	public static class TimeService {

		public record CurrentTime(
			int year,
			int month,
			int day,
			int hour,
			int minute,
			int seconds,
			int milliseconds,
			String dateTime,
			String date,
			String time,
			String timeZone,
			String dayOfWeek,
			boolean dstActive
		) {}

		public final WebClient<? extends ExchangeContext> webClient;

		public TimeService(WebClient<? extends ExchangeContext> webClient) {
			this.webClient = webClient;
		}

		public Mono<CurrentTime> getTime(String timeZone) {
			return this.webClient
				.exchange(URI.create("https://timeapi.io/api/time/current/zone?timeZone=" + timeZone))
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers
							.accept(MediaTypes.APPLICATION_JSON)
						);
					return exchange.response();
				})
				.flatMap(response -> response.body().decoder(CurrentTime.class).one());
		}
	}

	public void doc() {
		Mono<String> responseBody = webClient
			.exchange(URI.create("https://service/path/to/resource"))
			.flatMap(WebExchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining());

		webClient
			.exchange("https://service")
			.method(Method.GET)
			.path("/fruit/{name}")
			.pathParameter("name", "apple")
			.queryParameter("debug", true)
			.build();

		WebClient.WebExchangeBuilder<? extends ExchangeContext> getFruitExchangeBuilder = webClient
			.exchange("https://service")
			.method(Method.GET)
			.path("/fruit/{name}");

		Mono<? extends WebExchange<? extends ExchangeContext>> getAppleExchange = getFruitExchangeBuilder.clone().pathParameter("name", "apple").build();
		Mono<? extends WebExchange<? extends ExchangeContext>> getOrangeExchange = getFruitExchangeBuilder.clone().pathParameter("name", "orange").build();
		Mono<? extends WebExchange<? extends ExchangeContext>> getBananaExchange = getFruitExchangeBuilder.clone().pathParameter("name", "banana").build();

		WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
			.method(Method.GET)
			.uri(uri -> uri.scheme("https").host("{:*}").path("/fruit/**"))
			.interceptor(exchange -> {
				LOGGER.info("Exchange was intercepted!");
				return Mono.just(exchange);
			});

		Mono<? extends WebExchange<? extends ExchangeContext>> intercetpedGetAppleExchange = interceptedWebClient.exchange(URI.create("https://service/fruit/apple"));
		Mono<? extends WebExchange<? extends ExchangeContext>> getTomatoExchange = interceptedWebClient.exchange(URI.create("https://service/vegetable/tomato"));
	}

	public void pathParameters0() {
		String productUriFormat = "https://service/api/%s/%s";

		Mono<? extends WebExchange<? extends ExchangeContext>> getAppleExchange = webClient.exchange(URI.create(String.format(productUriFormat, "fruit", "apple")));
		Mono<? extends WebExchange<? extends ExchangeContext>> getLeekExchange = webClient.exchange(URI.create(String.format(productUriFormat, "vegetable", "leek")));
	}

	public void pathParameters1() {
		URIBuilder productURIBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN, URIs.Option.PARAMETERIZED)
			.scheme("https")
			.host("service")
			.path("/api/{productFamily}/{productName}");

		Mono<? extends WebExchange<? extends ExchangeContext>> getAppleExchange = webClient.exchange(productURIBuilder.build("fruit", "apple"));
		Mono<? extends WebExchange<? extends ExchangeContext>> getLeekExchange = webClient.exchange(productURIBuilder.build("vegetable", "leek"));
	}

	public void pathParameters2() {
		WebClient.WebExchangeBuilder<? extends ExchangeContext> getProductExchangeBuilder = webClient.exchange("https://service/api/{productFamily}/{productName}");

		Mono<? extends WebExchange<? extends ExchangeContext>> getAppleExchange = getProductExchangeBuilder.clone()
			.pathParameter("productFamily", "fruit")
			.pathParameter("productName", "apple")
			.build();

		Mono<? extends WebExchange<? extends ExchangeContext>> getLeekExchange = getProductExchangeBuilder.clone()
			.pathParameter("productFamily", "vegetable")
			.pathParameter("productName", "leek")
			.build();
	}

	public void queryParameters1() {
		URIBuilder productURIBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN, URIs.Option.PARAMETERIZED)
			.scheme("https")
			.host("service")
			.path("/api/get-time")
			.query("timezone={timezone}");

		Mono<? extends WebExchange<? extends ExchangeContext>> getParisTimeExchange = webClient.exchange(productURIBuilder.build("Europe/Paris"));
		Mono<? extends WebExchange<? extends ExchangeContext>> getESTTimeExchange = webClient.exchange(productURIBuilder.build("US/Eastern"));
	}

	public void queryParameters2() {
		WebClient.WebExchangeBuilder<? extends ExchangeContext> getProductExchangeBuilder = webClient.exchange("https://service/api/get-times");

		Mono<? extends WebExchange<? extends ExchangeContext>> getTimesExchange = getProductExchangeBuilder.clone()
			.queryParameter("timezone", List.of(ZoneId.of("Europe/Paris"), ZoneId.of("US/Eastern")), Types.type(List.class).type(ZoneId.class).and().build())
			.build();
	}

	public static abstract class Product {

		private String name;
		private String color;
		private String unit;
		private float price;
		private String currency;

		public Product() {
		}

		public Product(String name, String color, String unit, float price, String currency) {
			this.name = name;
			this.color = color;
			this.unit = unit;
			this.price = price;
			this.currency = currency;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getColor() {
			return color;
		}

		public void setColor(String color) {
			this.color = color;
		}

		public String getUnit() {
			return unit;
		}

		public void setUnit(String unit) {
			this.unit = unit;
		}

		public float getPrice() {
			return price;
		}

		public void setPrice(float price) {
			this.price = price;
		}

		public String getCurrency() {
			return currency;
		}

		public void setCurrency(String currency) {
			this.currency = currency;
		}
	}

	@Bean
	public static class FruitService {

		public static class Fruit extends Product {

			public Fruit() {
			}

			public Fruit(String name, String color, String unit, float price, String currency) {
				super(name, color, unit, price, currency);
			}
		}

		public final WebClient<? extends ExchangeContext> webClient;

		public FruitService(WebClient<? extends ExchangeContext> webClient) {
			this.webClient = webClient;
		}

		public Mono<Void> addFruit(Fruit fruit) {
			return this.webClient.exchange(Method.POST, URI.create("https://service/api/fruit"))
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
						.body().encoder().one(fruit);

					return exchange.response();
				})
				.then();
		}

		public Mono<Void> addFruits(Flux<Fruit> fruits) {
			return this.webClient.exchange(Method.POST, URI.create("https://service/api/fruit"))
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
						.body().encoder().many(fruits);

					return exchange.response();
				})
				.then();
		}

		public Mono<Void> addFruitsMultipart(Flux<Fruit> fruits) {
			return this.webClient.exchange(Method.POST, URI.create("https://service/api/fruit"))
				.flatMap(exchange -> {
					exchange.request()
						.headers(headers -> headers.contentType(MediaTypes.MULTIPART_FORM_DATA))
						.body().multipart().from((factory, data) -> data.stream(
							fruits.map(fruit -> factory.encoded(
								part -> part
									.name(fruit.getName())
									.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
									.value(fruit),
								Fruit.class
							)
						)));

					return exchange.response();
				})
				.then();
		}

		public Mono<Fruit> getFruit(String name) {
			return this.webClient.exchange(URI.create("https://service/api/fruit/" + name))
				.flatMap(exchange -> {
					exchange.request().headers(headers -> headers.accept(MediaTypes.APPLICATION_JSON));
					return exchange.response();
				})
				.flatMap(response -> response.body().decoder(Fruit.class).one());
		}

		public Flux<Fruit> listFruits() {
			return this.webClient.exchange(URI.create("https://service/api/fruit"))
				.flatMap(exchange -> {
					exchange.request().headers(headers -> headers.accept(MediaTypes.APPLICATION_JSON));
					return exchange.response();
				})
				.flatMapMany(response -> response.body().decoder(Fruit.class).many());
		}
	}

	@Bean
	public static class ChatClient {

		public record Message(
			String nickname,
			String message
		) {}

		public final WebClient<? extends ExchangeContext> webClient;

		public ChatClient(WebClient<? extends ExchangeContext> webClient) {
			this.webClient = webClient;
		}

		public Flux<Message> join(Flux<Message> inbound) {
			return this.webClient.exchange(URI.create("https://service/chat"))
				.flatMap(exchange -> exchange.webSocket("json"))
				.flatMapMany(wsExchange -> {
					wsExchange.outbound().encodeTextMessages(inbound, Message.class);
					return wsExchange.inbound().decodeTextMessages(Message.class);
				});
		}
	}

	public void interceptor() {
		WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
			.method(Method.POST)
			.produce(MediaTypes.APPLICATION_JSON)
			.interceptor(exchange -> {
				LOGGER.info("Intercepted!");
				return Mono.just(exchange);
			});

		Mono<? extends WebExchange<? extends ExchangeContext>> interceptedExchange = interceptedWebClient
			.exchange(Method.POST, URI.create("https://service/path/to/resource"))
			.doOnNext(exchange -> exchange.request()
				.headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
				.body().string().value("{\"message\":\"Hello world!\"}")
			);

		Mono<? extends WebExchange<? extends ExchangeContext>> notInterceptedExchange = interceptedWebClient
			.exchange(Method.GET, URI.create("https://service/path/to/resource"));

		WebClient.Intercepted<? extends ExchangeContext> childInterceptedWebClient = interceptedWebClient.intercept()
			.method(Method.GET)
			.interceptor(exchange -> {
				LOGGER.info("Intercepted GET request!");
				return Mono.just(exchange);
			});

		Mono<? extends WebExchange<? extends ExchangeContext>> interceptedGetExchange = childInterceptedWebClient
			.exchange(Method.GET, URI.create("https://service/path/to/resource"));

		Mono<? extends WebExchange<? extends ExchangeContext>> stillNotInterceptedGetExchange = interceptedWebClient
			.exchange(Method.GET, URI.create("https://service/path/to/resource"));
	}

	public void interceptors() {
		WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.configureInterceptors(interceptors -> interceptors
			.intercept()
				.method(Method.POST)
				.produce(MediaTypes.APPLICATION_JSON)
				.interceptor(exchange -> {
					LOGGER.info("Intercepted!");
					return Mono.just(exchange);
				})
			.intercept()
				.method(Method.GET)
				.interceptor(exchange -> {
					LOGGER.info("Intercepted GET request!");
					return Mono.just(exchange);
				})
		);

		((WebClient.Intercepted<ExchangeContext>)this.webClient).configureInterceptors(interceptors -> {
			WebRouteInterceptor<ExchangeContext> interceptor = interceptors
				.intercept()
				.interceptor(exchange -> {
					LOGGER.info("I'm included");
					return Mono.just(exchange);
				});

			interceptor.intercept()
				.method(Method.GET)
				.interceptor(exchange -> {
					LOGGER.info("I'm just ignored");
					return Mono.just(exchange);
				});

			return interceptor;
		});
	}

	public void uriInterceptor() {
		WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
			.uri(uri -> uri
				.scheme("https")
				.host("service")
				.path("/api/fruit/**")
			)
			.interceptor(exchange -> Mono.just(exchange));

		this.webClient.intercept()
			.uri(uri -> uri
				.scheme("{:(http|https)}")
				.host("service")
				.path("/api/fruit/**")
			)
			.interceptor(exchange -> Mono.just(exchange));

		this.webClient.intercept()
			.uri(uri -> uri
				.scheme("{:(http|https)}")
				.host("{:*}")
				.path("/api/fruit/**")
			)
			.uri(uri -> uri
				.scheme("{:(http|https)}")
				.host("{:*}")
				.port(443)
				.path("/api/fruit/**")
			)
			.interceptor(exchange -> Mono.just(exchange));


		this.webClient.intercept()
			.uri(uri -> uri
				.scheme("http")
				.host("localhost")
				.port(8080)
				.authority("127.0.0.1:8080") // overrides localhost and 8080
			)
			.interceptor(exchange -> Mono.just(exchange));
	}

	public void produceInterceptor() {
		WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
			.produce("application/json")
			.interceptor(exchange -> Mono.just(exchange));
	}

	public void frontOfficeWebClient() {
		Mono<String> responseBody = this.webClient.exchange(URI.create("conf://frontOffice/path/to/resource"))
			.flatMap(WebExchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining());
	}

	public void frontOfficeDiscoveryService() {
		HttpClient httpClient = null;
		HttpDiscoveryService discoveryService = null;

		URI requestURI = URI.create("conf://frontOffice/path/to/resource");
		Mono<String> responseBody = discoveryService.resolve(ServiceID.of(requestURI))
			.flatMap(service -> httpClient.exchange(ServiceID.getRequestTarget(requestURI))
					.flatMap(exchange -> service.getInstance(exchange)
						.map(serviceInstance -> serviceInstance.bind(exchange))
					)
					.flatMap(Exchange::response)
			)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining());
	}

	public void frontOfficeHttpClient() {
		HttpClient httpClient = null;

		Mono<String> responseBody = httpClient
			.endpoint("localhost", 8080).build()
			.exchange("/path/to/resource")
			.flatMap(Exchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining());
	}

	public void followRedirect() {
		Mono<String> responseBody = webClient.exchange(URI.create("http://service/path/to/resource"))
			.flatMap(WebExchange::response)
			.flatMap(response -> {
				if(response.headers().getStatus().getCategory() == Status.Category.REDIRECTION) {
					return response.headers().get(Headers.NAME_LOCATION)
						.map(location -> webClient.exchange(URI.create(location)).flatMap(WebExchange::response))
						.orElseThrow(() -> new IllegalStateException("Missing location in redirect"));
				}
				return Mono.just(response);
			})
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining());
	}

	public static class CustomException extends RuntimeException {}

	public void failOnErrorStatus() {
		String responseBody = webClient.exchange(URI.create("http://service/path/to/resource"))
			.doOnNext(exchange -> exchange.failOnErrorStatus(false)) // do not automatically fail on 4xx or 5xx
			.flatMap(WebExchange::response)
			.flatMapMany(response -> {
				switch(response.headers().getStatus().getCategory()) {
					case CLIENT_ERROR:
					case SERVER_ERROR: {
						throw new CustomException();
					}
					default: return response.body().string().stream();
				}
			})
			.collect(Collectors.joining())
			.block();
	}

	public void failOnErrorStatusCustomMapper() {
		WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient
			.intercept()
			.interceptor(exchange -> {
				exchange.failOnErrorStatus(response -> Mono.error(new CustomException()));
				return Mono.just(exchange);
			});

		String responseBody = interceptedWebClient
			.exchange(URI.create("http://service/error"))
			.flatMap(WebExchange::response)
			.flatMapMany(response -> response.body().string().stream())
			.collect(Collectors.joining())
			.block();
	}

	public void retryOnError() {
		Mono<String> responseBody = webClient.exchange(URI.create("http://service/path/to/resource"))
			.flatMap(WebExchange::response)
			.retry(2)                                           // retries 2 times when receiving errors
			.retryWhen(Retry.fixedDelay(10, Duration.ofMillis(500))) // retries 2 times with a fixed delay of 500ms between each attempt
			.retryWhen(Retry.backoff(10, Duration.ofMillis(100))) // retries 10 times using an exponential backoff strategy with a minimum duration of 100ms
			.flatMapMany(response ->  response.body().string().stream())
			.collect(Collectors.joining());
	}

	public void webRouteTranslation() {

		webClient.exchange(Method.POST, URI.create("http://service/api/v1/resource"))
			.doOnNext(exchange -> exchange.request()
				.headers(headers -> headers
					.contentType("application/json")
					.set(Headers.NAME_ACCEPT, "application/json")
					.set(Headers.NAME_ACCEPT_LANGUAGE, "en-US")
				)
			);
	}
}