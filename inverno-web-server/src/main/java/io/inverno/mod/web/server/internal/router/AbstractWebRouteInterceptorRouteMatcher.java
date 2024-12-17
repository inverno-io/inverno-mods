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
package io.inverno.mod.web.server.internal.router;

import io.inverno.mod.base.net.URIMatcher;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.internal.ExchangeInterceptorWrapper;
import java.util.Objects;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Base internal interceptor route matcher.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 * @param <C> the internal route type
 * @param <D> the internal interceptor route type
 */
abstract class AbstractWebRouteInterceptorRouteMatcher<A extends ExchangeContext, B extends Exchange<A>, C extends AbstractWebRoute<?, ?, ?, ?, ?, ?>, D extends AbstractWebRouteInterceptorRoute<A, B, ?>> {

	private static final Logger LOGGER = LogManager.getLogger(AbstractWebRouteInterceptorRouteMatcher.class);

	/**
	 * The internal interceptor route.
	 */
	protected final D interceptorRoute;
	/**
	 * The internal route.
	 */
	protected final C webRoute;

	/**
	 * <p>
	 * Creates an internal interceptor route matcher.
	 * </p>
	 *
	 * @param interceptorRoute the internal interceptor route
	 * @param webRoute         the internal route
	 */
	protected AbstractWebRouteInterceptorRouteMatcher(D interceptorRoute, C webRoute) {
		this.interceptorRoute = interceptorRoute;
		this.webRoute = webRoute;
	}

	/**
	 * <p>
	 * Returns the exchange interceptor when the route path matches the interceptor route path.
	 * </p>
	 *
	 * <p>
	 * The resulting exchange interceptor might be wrapped in an {@link ExchangeInterceptorWrapper} when the set defined by the route path is bigger than the set defined by the interceptor route path.
	 * </p>
	 *
	 * @param interceptor an exchange interceptor
	 *
	 * @return an exchange interceptor or null if the route path doesn't match the interceptor route path
	 */
	protected final ExchangeInterceptor<A, B> pathMatcher(ExchangeInterceptor<A, B> interceptor) {
		if(interceptor == null) {
			return null;
		}
		String interceptedPath = this.interceptorRoute.getPath();
		URIPattern interceptedPathPattern = this.interceptorRoute.getPathPattern();
		String path = this.webRoute.getPath();
		URIPattern pathPattern = this.webRoute.getPathPattern();
		if(interceptedPath != null) {
			if(path != null) {
				if(interceptedPath.equals(path)) {
					// B\A == {}
					return interceptor;
				}
				return null;
			}
			else if(pathPattern != null) {
				if(pathPattern.matcher(interceptedPath).matches()) {
					// B\A != {}
					return new AbstractWebRouteInterceptorRouteMatcher.FilteredPathExchangeInterceptorWrapper<>(interceptedPath, interceptor);
				}
				return null;
			}
			else {
				// B\A != {}
				return new AbstractWebRouteInterceptorRouteMatcher.FilteredPathExchangeInterceptorWrapper<>(interceptedPath, interceptor);
			}
		}
		else if(interceptedPathPattern != null) {
			if(path != null) {
				if(interceptedPathPattern.matcher(path).matches()) {
					// B\A == {}
					return interceptor;
				}
				return null;
			}
			else if(pathPattern != null) {
				// This is the trickiest
				// We need to determine whether the path pattern of the interceptor includes the path pattern of the route
				URIPattern.Inclusion includes = interceptedPathPattern.includes(pathPattern);

				switch(includes) {
					case INCLUDED: return interceptor;
					case INDETERMINATE: return new AbstractWebRouteInterceptorRouteMatcher.FilteredPathPatternExchangeInterceptorWrapper<>(interceptedPathPattern, interceptor);
					default: return null;
				}
			}
			else {
				// B\A != {}
				return new AbstractWebRouteInterceptorRouteMatcher.FilteredPathPatternExchangeInterceptorWrapper<>(interceptedPathPattern, interceptor);
			}
		}
		else {
			// No restrictions
			return interceptor;
		}
	}

	/**
	 * <p>
	 * Returns the exchange interceptor when the route content type matches the interceptor route content type.
	 * </p>
	 *
	 * <p>
	 * The resulting exchange interceptor might be wrapped in an {@link ExchangeInterceptorWrapper} when the set defined by the route content type is bigger than the set defined by the interceptor
	 * route content type.
	 * </p>
	 *
	 * @param interceptor an exchange interceptor
	 *
	 * @return an exchange interceptor or null if the route content type doesn't match the interceptor route content type
	 */
	protected final ExchangeInterceptor<A, B> contentTypeMatcher(ExchangeInterceptor<A, B> interceptor) {
		if(interceptor == null) {
			return null;
		}
		String interceptedContentType = this.interceptorRoute.getContentType();
		Headers.Accept.MediaRange interceptedContentTypeRange = this.interceptorRoute.getContentTypeRange();
		String contentType = this.webRoute.getContentType();
		if(interceptedContentType != null) {
			if(contentType != null) {
				// interceptor: consume is a media range: A/B
				// route: consume is a media range: C/D
				Headers.Accept.MediaRange contentTypeRange = AbstractWebRouteInterceptorRoute.CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, contentType).toMediaRange();

				// if parameters do not match there's no match
				if(interceptedContentTypeRange.getParameters().isEmpty() || interceptedContentTypeRange.getParameters().equals(contentTypeRange.getParameters())) {
					// from there we have to compare types and subtypes to determine what to do

					String interceptorType = interceptedContentTypeRange.getType();
					String interceptorSubType = interceptedContentTypeRange.getSubType();
					String routeType = contentTypeRange.getType();
					String routeSubType = contentTypeRange.getSubType();

					if(interceptorType.equals("*")) {
						if(interceptorSubType.equals("*")) {
							// interceptor */*
							// route ?/? => B/A == {}
							return interceptor;
						}
						else {
							// interceptor */x
							if(routeType.equals("*")) {
								if(routeSubType.equals("*")) {
									// route */* => B/A != {} && B/A != A
									return new AbstractWebRouteInterceptorRouteMatcher.FilteredContentExchangeInterceptorWrapper<>(interceptedContentTypeRange, interceptor);
								}
								else {
									// route */x
									if(interceptorSubType.equals(routeSubType)) {
										return interceptor;
									}
									return null;
								}
							}
							else {
								if(routeSubType.equals("*")) {
									// route x/* => B/A != {} && B/A != A
									return new AbstractWebRouteInterceptorRouteMatcher.FilteredContentExchangeInterceptorWrapper<>(interceptedContentTypeRange, interceptor);
								}
								else {
									// route x/x
									if(interceptorSubType.equals(routeSubType)) {
										return interceptor;
									}
									return null;
								}
							}
						}
					}
					else {
						if(interceptorSubType.equals("*")) {
							// interceptor x/*
							if(routeType.equals("*")) {
								// route */? => B/A != {}
								return new AbstractWebRouteInterceptorRouteMatcher.FilteredContentExchangeInterceptorWrapper<>(interceptedContentTypeRange, interceptor);
							}
							else {
								// route x/?
								if(interceptorType.equals(routeType)) {
									return interceptor;
								}
								return null;
							}
						}
						else {
							// interceptor x/x
							if(routeType.equals("*")) {
								if(routeSubType.equals("*") || interceptorSubType.equals(routeSubType)) {
									// route */*|*/x => B/A != {}
									return new AbstractWebRouteInterceptorRouteMatcher.FilteredContentExchangeInterceptorWrapper<>(interceptedContentTypeRange, interceptor);
								}
								return null;
							}
							else {
								if(interceptorType.equals(routeType)) {
									if(routeSubType.equals("*")) {
										// route x/* => B/A != {}
										return new AbstractWebRouteInterceptorRouteMatcher.FilteredContentExchangeInterceptorWrapper<>(interceptedContentTypeRange, interceptor);
									}
									else {
										// route x/x
										if(interceptorSubType.equals(routeSubType)) {
											return interceptor;
										}
										return null;
									}
								}
								return null;
							}
						}
					}
				}
				return null;
			}
			else {
				// B\A != {}
				return new AbstractWebRouteInterceptorRouteMatcher.FilteredContentExchangeInterceptorWrapper<>(interceptedContentTypeRange, interceptor);
			}
		}
		else {
			// No restrictions
			return interceptor;
		}
	}

	/**
	 * <p>
	 * Returns the exchange interceptor when the route accept matches the interceptor route accept.
	 * </p>
	 *
	 * <p>
	 * The resulting exchange interceptor might be wrapped in an {@link ExchangeInterceptorWrapper} when the set defined by the route accept is bigger than the set defined by the interceptor route
	 * accept.
	 * </p>
	 *
	 * @param interceptor an exchange interceptor
	 *
	 * @return an exchange interceptor or null if the route accept doesn't match the interceptor route accept
	 */
	protected final ExchangeInterceptor<A, B> acceptMatcher(ExchangeInterceptor<A, B> interceptor) {
		if(interceptor == null) {
			return null;
		}
		String interceptedAccept = this.interceptorRoute.getAccept();
		Headers.Accept.MediaRange interceptedAcceptRange = this.interceptorRoute.getAcceptRange();
		String accept = this.webRoute.getAccept();
		if(interceptedAccept != null) {
			if(accept != null) {
				Headers.ContentType routeContentType = AbstractWebRouteInterceptorRoute.CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, accept);
				if(interceptedAcceptRange.matches(routeContentType)) {
					return interceptor;
				}
				return null;
			}
			else {
				// B\A != {}
				/*
				 * Here we actually don't know the content type before the route handler is actually executed, so what to do?
				 * - very defensive: we can break here and say we detected a bad defined route
				 * - defensive: we can decide not to apply the interceptor and log a warning
				 * - offensive: we can apply the interceptor and log a warning saying that the route is incomplete
				 * we should go for the defensive option: if someone wants to apply interceptors on routes producing a particular content, it is fair to assume these routes are defined with such information
				 */
				LOGGER.warn(() -> "Ignoring interceptor " + this + " on route " + this.webRoute + ": content type is missing");
				return null;
			}
		}
		else {
			// No restrictions
			return interceptor;
		}
	}

	/**
	 * <p>
	 * Returns the exchange interceptor when the route language matches the interceptor route language.
	 * </p>
	 *
	 * <p>
	 * The resulting exchange interceptor might be wrapped in an {@link ExchangeInterceptorWrapper} when the set defined by the route language is bigger than the set defined by the interceptor route
	 * language.
	 * </p>
	 *
	 * @param interceptor an exchange interceptor
	 *
	 * @return an exchange interceptor or null if the route language doesn't match the interceptor route language
	 */
	protected final ExchangeInterceptor<A, B> languageMatcher(ExchangeInterceptor<A, B> interceptor) {
		if (interceptor == null) {
			return null;
		}
		String interceptedLanguage = this.interceptorRoute.getLanguage();
		Headers.AcceptLanguage.LanguageRange interceptedLanguageRange = this.interceptorRoute.getLanguageRange();
		String language = this.webRoute.getLanguage();
		if(interceptedLanguage != null) {
			if(language != null) {
				Headers.AcceptLanguage.LanguageRange routeLanguageRange = AbstractWebRouteInterceptorRoute.ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().getFirst();
				if(interceptedLanguageRange.matches(routeLanguageRange)) {
					return interceptor;
				}
				return null;
			}
			else {
				// B\A != {}
				/*
				 * Here we actually don't know the language tag before the route handler is actually executed, so what to do?
				 * - very defensive: we can break here and say we detected a bad defined route
				 * - defensive: we can decide not to apply the interceptor and log a warning
				 * - offensive: we can apply the interceptor and log a warning saying that the route is incomplete
				 * we should go for the defensive option: if someone wants to apply interceptors on routes producing a particular language, it is fair to assume these routes are defined with such information
				 */
				LOGGER.warn("Ignoring interceptor " + this + " on route " + this.webRoute + ": language is missing");
				return null;
			}
		}
		else {
			// No restrictions
			return interceptor;
		}
	}

	/**
	 * <p>
	 * Matches the route against the interceptor route and returns the corresponding exchange interceptor.
	 * </p>
	 *
	 * @return an exchange interceptor or null if the route doesn't match the interceptor route
	 */
	public final ExchangeInterceptor<A, B> matches() {
		return this.matcher().apply(interceptorRoute.get());
	}

	/**
	 * <p>
	 * Returns the interceptor route matcher.
	 * </p>
	 *
	 * @return the interceptor route matcher
	 */
	protected Function<ExchangeInterceptor<A, B>, ExchangeInterceptor<A, B>> matcher() {
		return ((Function<ExchangeInterceptor<A, B>, ExchangeInterceptor<A, B>>)this::pathMatcher)
			.andThen(this::contentTypeMatcher)
			.andThen(this::acceptMatcher)
			.andThen(this::languageMatcher);
	}

	/**
	 * <p>
	 * Wraps an exchange interceptor to only intercept exchange whose request content type is matching a specific media range.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 * @param <B> the exchange type
	 */
	private static class FilteredContentExchangeInterceptorWrapper<A extends ExchangeContext, B extends Exchange<A>> extends ExchangeInterceptorWrapper<A, B> {

		private final Headers.Accept.MediaRange contentTypeRange;

		/**
		 * <p>
		 * Creates a filtered content exchange interceptor.
		 * </p>
		 *
		 * @param contentTypeRange a media range
		 * @param interceptor      the exchange interceptor to filter
		 */
		public FilteredContentExchangeInterceptorWrapper(Headers.Accept.MediaRange contentTypeRange, ExchangeInterceptor<A, B> interceptor) {
			super(interceptor);
			this.contentTypeRange = contentTypeRange;
		}

		@Override
		public Mono<? extends B> intercept(B exchange) {
			if(exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(this.contentTypeRange::matches).orElse(false)) {
				// Interceptor consume media range matches request content type: the exchange must be intercepted
				return this.wrappedInterceptor.intercept(exchange);
			}
			// Interceptor consume media range does not match request content type: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}

	/**
	 * <p>
	 * Wraps an exchange interceptor to only intercept exchange whose request path is matching a specific path.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 * @param <B> the exchange type
	 */
	private static class FilteredPathExchangeInterceptorWrapper<A extends ExchangeContext, B extends Exchange<A>> extends ExchangeInterceptorWrapper<A, B> {

		private final String path;

		/**
		 * <p>
		 * Creates a filtered path exchange interceptor.
		 * </p>
		 *
		 * @param path        an absolute path
		 * @param interceptor the exchange interceptor to filter
		 */
		public FilteredPathExchangeInterceptorWrapper(String path, ExchangeInterceptor<A, B> interceptor) {
			super(interceptor);
			this.path = Objects.requireNonNull(path);
		}

		@Override
		public Mono<? extends B> intercept(B exchange) {
			String normalizedPath = exchange.request().getPathAbsolute();
			if(this.path.equals(normalizedPath)) {
				// Interceptor path is equals to request path: the exchange must be intercepted
				return this.wrappedInterceptor.intercept(exchange);
			}
			// Interceptor path is not equals to request path: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}

	/**
	 * <p>
	 * Wraps an exchange interceptor to only intercept exchange whose request path is matching a specific path pattern.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 * @param <B> the exchange type
	 */
	private static class FilteredPathPatternExchangeInterceptorWrapper<A extends ExchangeContext, B extends Exchange<A>> extends ExchangeInterceptorWrapper<A, B> {

		private final URIPattern pathPattern;

		/**
		 * <p>
		 * Creates a filtered path exchange interceptor.
		 * </p>
		 *
		 * @param pathPattern an absolute path pattern
		 * @param interceptor the exchange interceptor to filter
		 */
		public FilteredPathPatternExchangeInterceptorWrapper(URIPattern pathPattern, ExchangeInterceptor<A, B> interceptor) {
			super(interceptor);
			this.pathPattern = Objects.requireNonNull(pathPattern);
		}

		@Override
		public Mono<? extends B> intercept(B exchange) {
			String normalizedPath = exchange.request().getPathAbsolute();
			URIMatcher matcher = this.pathPattern.matcher(normalizedPath);
			if(matcher.matches()) {
				// Interceptor path pattern matches request path: the exchange must be intercepted
				return this.wrappedInterceptor.intercept(exchange);
			}
			// Interceptor path pattern matches request path: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}
}
