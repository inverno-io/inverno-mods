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
package io.inverno.mod.web.client;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A Web client for invoking HTTP services.
 * </p>
 *
 * <p>
 * A Web client is backed by a caching composite {@link io.inverno.mod.discovery.DiscoveryService} for resolving HTTP services from service URIs.
 * </p>
 *
 * <p>
 * It allows to specify interceptors with a set of criteria applied to matching exchanges. Every time an interceptor is declared on a Web client, a new {@link Intercepted} Web client instance is
 * created containing the interceptor and any other interceptors declared on ancestor clients.
 * </p>
 *
 * <pre>{@code
 * WebClient<ExchangeContext> webClient = ...
 * WebClient.Intercepted<ExchangeContext> interceptedWebClient = webClient.intercept()
 *     .method(Method.GET) // intercept all GET requests
 *     .interceptor(inteceptableExchange -> ...)
 * }</pre>
 *
 * <p>
 * Interceptors can also be declared globally in a module in {@link WebRouteInterceptor.Configurer} beans which are injected in the module's Web client instance on initialization.
 * </p>
 *
 * <p>
 * An HTTP request can be sent to any destination that can be resolved by the set of discovery services injected in the Web client module:
 * </p>
 *
 * <pre>{@code
 * WebClient<ExchangeContext> webClient = ...
 *
 * webClient.exchange(URI.create("http://example.org"))
 *     .map(WebExchange::response)
 *     ...
 * }</pre>
 *
 * <p>
 * The Web client {@link Boot} bean is exposed in the module, it is used to create the root Web client instance, it requires to specify the factory used to create the {@link ExchangeContext} for any
 * exchange. There can be only one type for the exchange context per Web client module instance.
 * </p>
 *
 * <pre>{@code
 * WebClient.Boot boot = ...
 * WebClient<Context> rootWebClient = boot.webClient(Context::new)
 *     .configureInterceptors(...)
 *     .intercept()...
 * }</pre>
 *
 * <p>
 * The root {@code WebClient} thus obtained can then be used inside the module or injected in composed module. The Web compiler normally takes care of generating a wrapper bean for initializing this
 * root Web client.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public interface WebClient<A extends ExchangeContext> extends WebRouteInterceptor<A> {

	@Override
	WebRouteInterceptorManager<A, WebClient.Intercepted<A>> intercept();

	@Override
	default WebClient.Intercepted<A> intercept(Function<WebRouteInterceptorManager<A, ? extends WebRouteInterceptor<A>>, ? extends WebRouteInterceptor<A>> configurer) {
		return (WebClient.Intercepted<A>)configurer.apply(this.intercept());
	}

	@Override
	WebClient.Intercepted<A> configureInterceptors(Configurer<? super A> configurer);

	@Override
	WebClient.Intercepted<A> configureInterceptors(List<Configurer<? super A>> configurers);

	/**
	 * <p>
	 * Creates a Web exchange.
	 * </p>
	 *
	 * <p>
	 * This method is a shortcut for {@code exchange(Method.GET, uri)}.
	 * </p>
	 *
	 * @param uri the service URI
	 *
	 * @return a {@code Mono} emitting the Web exchange
	 *
	 * @throws IllegalArgumentException if the specified URI is not a valid service URI
	 */
	default Mono<WebExchange<A>> exchange(URI uri) throws IllegalArgumentException {
		return this.exchange(Method.GET, uri);
	}

	/**
	 * <p>
	 * Creates a Web exchange.
	 * </p>
	 *
	 * @param method the HTTP method
	 * @param uri    the service URI
	 *
	 * @return a {@code Mono} emitting the Web exchange
	 *
	 * @throws IllegalArgumentException if the specified URI is not a valid service URI
	 */
	Mono<WebExchange<A>> exchange(Method method, URI uri) throws IllegalArgumentException;

	/**
	 * <p>
	 * Creates a Web exchange builder.
	 * </p>
	 *
	 * @param uri a service URI
	 *
	 * @return a Web exchange builder
	 *
	 * @throws IllegalArgumentException if the specified URI is not a valid service URI
	 */
	WebExchangeBuilder<A> exchange(String uri) throws IllegalArgumentException;

	/**
	 * <p>
	 * Web client boot used that initializes the root Web client.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	interface Boot {

		/**
		 * <p>
		 * Initializes the root Web client with no context.
		 * </p>
		 *
		 * @return the root Web client
		 *
		 * @throws IllegalStateException if the root Web client has already been initialized
		 */
		default WebClient<ExchangeContext> webClient() throws IllegalStateException {
			return this.webClient(() -> null);
		}

		/**
		 * <p>
		 * Initializes the root Web client with the specified exchange context factory.
		 * </p>
		 *
		 * @param <T>            the exchange context type
		 * @param contextFactory the exchange context factory
		 *
		 * @return the root Web client
		 *
		 * @throws IllegalStateException if the root Web client has already been initialized
		 */
		<T extends ExchangeContext> WebClient<T> webClient(Supplier<T> contextFactory) throws IllegalStateException;
	}

	/**
	 * <p>
	 * An intercepted Web client.
	 * </p>
	 *
	 * <p>
	 * An intercepted Web client is created every time an interceptor is defined on a Web client, it contains all interceptors that were defined on its ancestors creating a chain of intercepted Web
	 * client with the root Web client at the top.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	interface Intercepted<A extends ExchangeContext> extends WebClient<A> {

		/**
		 * <p>
		 * Returns the originating Web client.
		 * </p>
		 *
		 * @return an intercepted Web client or the root Web client
		 */
		WebClient<A> unwrap();
	}

	/**
	 * <p>
	 * A Web exchange builder.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	interface WebExchangeBuilder<A extends ExchangeContext> extends Cloneable {

		/**
		 * <p>
		 * Specifies the request method.
		 * </p>
		 *
		 * @param method the HTTP method
		 *
		 * @return the Web exchange builder
		 */
		WebExchangeBuilder<A> method(Method method);

		/**
		 * <p>
		 * Specifies the request target.
		 * </p>
		 *
		 * @param path the request target
		 *
		 * @return the Web exchange builder
		 */
		WebExchangeBuilder<A> path(String path);

		/**
		 * <p>
		 * Specifies the value of a path parameter in the request path.
		 * </p>
		 *
		 * <p>
		 * The specified value is converted to a {@code String} using the module's parameter converter.
		 * </p>
		 *
		 * @param <T>   the parameter value type
		 * @param name  the parameter name
		 * @param value the parameter value
		 *
		 * @return the Web exchange builder
		 */
		<T> WebExchangeBuilder<A> pathParameter(String name, T value);

		/**
		 * <p>
		 * Specifies the value of a path parameter in the request path.
		 * </p>
		 *
		 * <p>
		 * The specified value is converted to a {@code String} using the module's parameter converter.
		 * </p>
		 *
		 * @param <T>   the parameter value type
		 * @param name  the parameter name
		 * @param value the parameter value
		 * @param type  the parameter value type
		 *
		 * @return the Web exchange builder
		 */
		default <T> WebExchangeBuilder<A> pathParameter(String name, T value, Class<T> type) {
			return this.pathParameter(name, value, (Type)type);
		}

		/**
		 * <p>
		 * Specifies the value of a path parameter in the request path.
		 * </p>
		 *
		 * <p>
		 * The specified value is converted to a {@code String} using the module's parameter converter.
		 * </p>
		 *
		 * @param <T>   the parameter value type
		 * @param name  the parameter name
		 * @param value the parameter value
		 * @param type  the parameter value type
		 *
		 * @return the Web exchange builder
		 */
		<T> WebExchangeBuilder<A> pathParameter(String name, T value, Type type);

		/**
		 * <p>
		 * Adds a query parameter.
		 * </p>
		 *
		 * <p>
		 * The specified value is converted to a {@code String} using the module's parameter converter.
		 * </p>
		 *
		 * @param <T>   the parameter value type
		 * @param name  the parameter name
		 * @param value the parameter value type
		 *
		 * @return the Web exchange builder
		 */
		<T> WebExchangeBuilder<A> queryParameter(String name, T value);

		/**
		 * <p>
		 * Adds a query parameter.
		 * </p>
		 *
		 * <p>
		 * The specified value is converted to a {@code String} using the module's parameter converter.
		 * </p>
		 *
		 * @param <T>   the parameter value type
		 * @param name  the parameter name
		 * @param value the parameter value
		 * @param type  the parameter value type
		 *
		 * @return the Web exchange builder
		 */
		default <T> WebExchangeBuilder<A> queryParameter(String name, T value, Class<T> type) {
			return this.queryParameter(name, value, (Type)type);
		}

		/**
		 * <p>
		 * Adds a query parameter.
		 * </p>
		 *
		 * <p>
		 * The specified value is converted to a {@code String} using the module's parameter converter.
		 * </p>
		 *
		 * @param <T>   the parameter value type
		 * @param name  the parameter name
		 * @param value the parameter value
		 * @param type  the parameter value type
		 *
		 * @return the Web exchange builder
		 */
		<T> WebExchangeBuilder<A> queryParameter(String name, T value, Type type);

		/**
		 * <p>
		 * Builds the Web exchange.
		 * </p>
		 *
		 * @return a {@code Mono} emitting the Web exchange
		 */
		Mono<WebExchange<A>> build();

		/**
		 * <p>
		 * Clones the Web exchange builder.
		 * </p>
		 *
		 * @return a clone of the Web exchange builder
		 */
		WebExchangeBuilder<A> clone();
	}
}
