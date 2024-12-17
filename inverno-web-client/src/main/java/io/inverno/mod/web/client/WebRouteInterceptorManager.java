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
import io.inverno.mod.http.client.ExchangeInterceptor;
import java.util.List;
import java.util.function.Consumer;

/**
 * <p>
 * Defines Web route interceptors and creates decorated Web clients intercepting Web exchanges.
 * </p>
 *
 * <p>
 * A Web route interceptor manager is obtained from {@link WebClient#intercept()} or more generally {@link WebRouteInterceptor#intercept()}, it allows to specify the criteria a Web exchange must match
 * to be intercepted by the exchange interceptors defined in {@link #interceptor(ExchangeInterceptor)} or {@link #interceptors(List)}. These methods return the intercepted Web client which shall be
 * used to process Web exchanges intercepted when they are matching the defined criteria.
 * </p>
 *
 * <p>
 * It is possible to specify multiple values for any given criteria resulting in multiple Web route interceptor definitions being created in the resulting Web route interceptor. For instance, the
 * following code will result in two definitions being created:
 * </p>
 *
 * <pre>{@code
 * webClient
 *     .intercept()
 *         .uri(uri -> uri
 *             .scheme("http")
 *             .host("example.org")
 *             .path("/**")
 *         )
 *         .method(Method.GET)
 *         .method(Method.POST)
 *         .interceptor(exchange -> {...})
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebRouteInterceptor
 * @see WebClient
 *
 * @param <A> the exchange context type
 * @param <B> the Web route interceptor type
 */
public interface WebRouteInterceptorManager<A extends ExchangeContext, B extends WebRouteInterceptor<A>> {

	/**
	 * <p>
	 * Specifies the URI that must be matched by a Web exchange to be intercepted.
	 * </p>
	 *
	 * @param uriConfigurer a URI configurer
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> uri(Consumer<UriConfigurator> uriConfigurer);

	/**
	 * <p>
	 * Specifies the HTTP method that must be matched by a Web route to be intercepted.
	 * </p>
	 *
	 * @param method an HTTP method
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> method(Method method);

	/**
	 * <p>
	 * Specifies the media range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> matching the content types consumed (i.e. accepted) by a
	 * Web exchange to be intercepted.
	 * </p>
	 *
	 * @param mediaRange a media range (e.g. {@code application/*})
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> consume(String mediaRange);

	/**
	 * <p>
	 * Specifies the media range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">RFC 7231 Section 5.3.2</a> defining the content types produced by a Web exchange to be
	 * intercepted.
	 * </p>
	 *
	 * @param mediaRange a media range (e.g. {@code application/*})
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> produce(String mediaRange);

	/**
	 * <p>
	 * Specifies the language range as defined by <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">RFC 7231 Section 5.3.5</a> matching the language consumed (i.e. accepted) by a Web exchange
	 * to be intercepted.
	 * </p>
	 *
	 * @param languageRange a language range (e.g. {@code *-FR})
	 *
	 * @return the interceptor manager
	 */
	WebRouteInterceptorManager<A, B> language(String languageRange);

	/**
	 * <p>
	 * Specifies the Web exchange interceptor to apply to a Web exchange matching the criteria specified in the route manager and returns a Web route interceptor containing the interceptor definitions
	 * specified in the interceptor manager.
	 * </p>
	 *
	 * <p>
	 * The Web route interceptor manager is usually obtained from {@link WebClient#intercept()} in which case the resulting Web route interceptor is a {@link WebClient.Intercepted} instance.
	 * </p>
	 *
	 * @param interceptor a Web exchange interceptor
	 *
	 * @return a new Web route interceptor containing one or more interceptor definitions
	 */
	B interceptor(ExchangeInterceptor<? super A, InterceptedWebExchange<A>> interceptor);

	/**
	 * <p>
	 * Specifies a list of Web exchange interceptors to apply to a Web exchange matching the criteria specified in the route manager and returns a Web route interceptor containing the interceptor
	 * definitions specified in the interceptor manager.
	 * </p>
	 *
	 * <p>
	 * The Web route interceptor manager is usually obtained from {@link WebClient#intercept()} in which case the resulting Web route interceptor is a {@link WebClient.Intercepted} instance.
	 * </p>
	 *
	 * @param interceptors a list of Web exchange interceptors
	 *
	 * @return a new Web route interceptor containing one or more interceptor definitions
	 */
	B interceptors(List<? extends ExchangeInterceptor<? super A, InterceptedWebExchange<A>>> interceptors);

	/**
	 * <p>
	 * A URI configurator used to configure URI criteria in a Web route interceptor manager.
	 * </p>
	 *
	 * <p>
	 * This especially allows to define path patterns to match range of URIs.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @see io.inverno.mod.base.net.URIs.Option#PATH_PATTERN
	 */
	interface UriConfigurator {

		/**
		 * <p>
		 * Specifies the URI scheme that must be matched by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * The value can be parameterized and include patterns like {@code ?} or {@code *} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this is only meant to filter exchanges
		 * and as a result named parameters have no use. For instance, any value can be matched by specifying `{:*}`.
		 * </p>
		 *
		 * @param scheme a scheme (e.g. {@code http})
		 *
		 * @return the URI configurator
		 */
		UriConfigurator scheme(String scheme);

		/**
		 * <p>
		 * Specifies the URI authority that must be matched by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * The value can be parameterized and support patterns like {@code ?} or {@code *} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this is only meant to filter exchanges
		 * and as a result named parameters have no use. For instance, any value can be matched by specifying `{:*}`.
		 * </p>
		 *
		 * <p>
		 * This basically overrides {@link #userInfo(String)}, {@link #host(String)} and {@link #port(String)}.
		 * </p>
		 *
		 * @param authority an authority (e.g. {@code example.org:80})
		 *
		 * @return the URI configurator
		 */
		UriConfigurator authority(String authority);

		/**
		 * <p>
		 * Specifies the user info that must be matched by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * The value can be parameterized and include patterns like {@code ?} or {@code *} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this is only meant to filter exchanges
		 * and as a result named parameters have no use. For instance, any value can be matched by specifying `{:*}`.
		 * </p>
		 *
		 * @param userInfo a user info (e.g. {@code user:password})
		 *
		 * @return the URI configurator
		 */
		UriConfigurator userInfo(String userInfo);

		/**
		 * <p>
		 * Specifies the host that must be matched by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * The value can be parameterized and include patterns like {@code ?} or {@code *} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this is only meant to filter exchanges
		 * and as a result named parameters have no use. For instance, any value can be matched by specifying `{:*}`.
		 * </p>
		 *
		 * @param host a user info (e.g. {@code example.org})
		 *
		 * @return the URI configurator
		 */
		UriConfigurator host(String host);

		/**
		 * <p>
		 * Specifies the port that must be matched by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * Port matching is exact, in order to match, a URI must define the port explicitly when a port is specified and omit it otherwise.
		 * </p>
		 *
		 * @param port a port (e.g. {@code 80})
		 *
		 * @return the URI configurator
		 */
		default UriConfigurator port(Integer port) {
			return this.port(port != null ? port.toString() : null);
		}

		/**
		 * <p>
		 * Specifies the port that must be matched by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * Port matching is exact, in order to match, a URI must define the port explicitly when a port is specified and omit it otherwise.
		 * </p>
		 *
		 * <p>
		 * The value can be parameterized and include patterns like {@code ?} or {@code *} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this is only meant to filter exchanges
		 * and as a result named path parameters have no use. For instance, any value can be matched by specifying `{:*}`.
		 * </p>
		 *
		 * @param port a port (e.g. {@code 80})
		 *
		 * @return the URI configurator
		 */
		UriConfigurator port(String port);

		/**
		 * <p>
		 * Specifies the absolute path that must be matched by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * The path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *} or {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this
		 * path is only meant to filter routes and as a result path parameters have no use. For instance, any path can be matched by specifying `/**`.
		 * </p>
		 *
		 * @param path a path
		 *
		 * @return the URI configurator
		 *
		 * @throws IllegalArgumentException if the specified path is not absolute
		 */
		default UriConfigurator path(String path) throws IllegalArgumentException {
			return this.path(path, false);
		}

		/**
		 * <p>
		 * Specifies the absolute path that must be matched with or without trailing slash by a Web exchange URI to be intercepted.
		 * </p>
		 *
		 * <p>
		 * The path can be specified as a parameterized path and include path pattern like {@code ?}, {@code *}, {@code **} as defined by {@link io.inverno.mod.base.net.URIBuilder}. Note that this
		 * path is only meant to filter routes and as a result path parameters have no use.
		 * </p>
		 *
		 * @param path a path
		 * @param matchTrailingSlash true to match path with or without trailing slash, false otherwise
		 *
		 * @return the URI configurator
		 *
		 * @throws IllegalArgumentException if the specified path is not absolute
		 */
		UriConfigurator path(String path, boolean matchTrailingSlash) throws IllegalArgumentException;
	}
}
