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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.router.MethodRoute;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.WebExchange;
import io.inverno.mod.web.server.internal.ExchangeInterceptorWrapper;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Internal Web route interceptor router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public final class InternalWebRouteInterceptorRouter<A extends ExchangeContext> extends AbstractWebRouteInterceptorRouter<
		A,
		WebExchange<A>,
		InternalWebRouter.Route<A>,
		InternalWebRouteInterceptorRouter.InterceptorRoute<A>,
		InternalWebRouteInterceptorRouter.InterceptorRouteManager<A>,
		InternalWebRouteInterceptorRouter<A>,
		InternalWebRouteInterceptorRouter.InterceptorRouteMatcher<A>
	> {

	/**
	 * <p>
	 * Creates an internal Web route interceptor router.
	 * </p>
	 */
	public InternalWebRouteInterceptorRouter() {
	}

	/**
	 * <p>
	 * Creates an internal Web route interceptor router.
	 * </p>
	 *
	 * @param parent the parent router
	 */
	private InternalWebRouteInterceptorRouter(InternalWebRouteInterceptorRouter<A> parent) {
		super(parent);
	}

	@Override
	protected InternalWebRouteInterceptorRouter.InterceptorRoute<A> createRoute(InternalWebRouteInterceptorRouter.InterceptorRoute<A> parentRoute) {
		return new InternalWebRouteInterceptorRouter.InterceptorRoute<>(parentRoute);
	}

	@Override
	protected InternalWebRouteInterceptorRouter.InterceptorRouteManager<A> createRouteManager() {
		return new InternalWebRouteInterceptorRouter.InterceptorRouteManager<>(new InternalWebRouteInterceptorRouter<>(this));
	}

	@Override
	protected InternalWebRouteInterceptorRouter.InterceptorRouteMatcher<A> createRouteMatcher(InternalWebRouteInterceptorRouter.InterceptorRoute<A> interceptorRoute, InternalWebRouter.Route<A> webRoute) {
		return new InternalWebRouteInterceptorRouter.InterceptorRouteMatcher<>(interceptorRoute, webRoute);
	}

	/**
	 * <p>
	 * Internal Web interceptor route.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class InterceptorRoute<A extends ExchangeContext>
		extends
		AbstractWebRouteInterceptorRoute<A, WebExchange<A>, InterceptorRoute<A>>
		implements
			MethodRoute<ExchangeInterceptor<A, WebExchange<A>>> {

		private Method method;

		/**
		 * <p>
		 * Creates an internal Web interceptor route.
		 * </p>
		 *
		 * @param parent the parent route
		 */
		private InterceptorRoute(InternalWebRouteInterceptorRouter.InterceptorRoute<A> parent) {
			super(parent);
		}

		@Override
		public Method getMethod() {
			if(this.method != null) {
				return this.method;
			}
			return this.parent != null ? this.parent.getMethod() : null;
		}

		/**
		 * <p>
		 * Sets the route method.
		 * </p>
		 *
		 * @param method a method
		 */
		public void setMethod(Method method) {
			this.method = method;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			InternalWebRouteInterceptorRouter.InterceptorRoute<?> that = (InternalWebRouteInterceptorRouter.InterceptorRoute<?>) o;
			return method == that.method;
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), method);
		}

		@Override
		public String toString() {
			StringBuilder routeStringBuilder = new StringBuilder();

			routeStringBuilder.append("{");
			routeStringBuilder.append("\"path\":\"").append(this.getPath() != null ? this.getPath() : this.getPathPattern()).append("\",");
			routeStringBuilder.append("\"method\":\"").append(this.getMethod()).append("\",");
			routeStringBuilder.append("\"consume\":\"").append(this.getContentType()).append("\",");
			routeStringBuilder.append("\"produce\":\"").append(this.getAccept()).append("\",");
			routeStringBuilder.append("\"language\":\"").append(this.getLanguage());
			routeStringBuilder.append("}");

			return routeStringBuilder.toString();
		}
	}

	/**
	 * <p>
	 * Internal Web interceptor route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class InterceptorRouteManager<A extends ExchangeContext>
		extends AbstractWebRouteInterceptorRouteManager<
					A,
					WebExchange<A>,
					InternalWebRouter.Route<A>,
					InterceptorRoute<A>,
					InterceptorRouteManager<A>,
				InternalWebRouteInterceptorRouter<A>,
					InterceptorRouteMatcher<A>
				>
		implements
			MethodRoute.Manager<
				ExchangeInterceptor<A, WebExchange<A>>,
				InternalWebRouter.Route<A>,
				InternalWebRouteInterceptorRouter.InterceptorRoute<A>,
				InternalWebRouteInterceptorRouter.InterceptorRouteManager<A>,
				InternalWebRouteInterceptorRouter<A>
			> {

		private Set<Method> methods;

		/**
		 * <p>
		 * Creates an internal Web interceptor route manager.
		 * </p>
		 *
		 * @param router the internal Web interceptor route manager
		 */
		private InterceptorRouteManager(InternalWebRouteInterceptorRouter<A> router) {
			super(router);
		}

		@Override
		public InternalWebRouteInterceptorRouter.InterceptorRouteManager<A> method(Method method) {
			Objects.requireNonNull(method);
			if(this.methods == null) {
				this.methods = new HashSet<>();
			}
			this.methods.add(method);
			return this;
		}

		/**
		 * <p>
		 * Returns the method route extractor.
		 * </p>
		 *
		 * @param next the next route extractor
		 *
		 * @return the method route extractor
		 */
		private Consumer<InternalWebRouteInterceptorRouter.InterceptorRoute<A>> methodRouteExtractor(Consumer<InternalWebRouteInterceptorRouter.InterceptorRoute<A>> next) {
			return route -> {
				if(this.methods != null && !this.methods.isEmpty()) {
					for (Method method : this.methods) {
						InternalWebRouteInterceptorRouter.InterceptorRoute<A> childRoute = this.router.createRoute(route);
						childRoute.setMethod(method);
						next.accept(childRoute);
					}
				}
				else {
					next.accept(route);
				}
			};
		}

		@Override
		protected Function<Consumer<InternalWebRouteInterceptorRouter.InterceptorRoute<A>>, Consumer<InternalWebRouteInterceptorRouter.InterceptorRoute<A>>> routeExtractor() {
			return ((Function<Consumer<InternalWebRouteInterceptorRouter.InterceptorRoute<A>>, Consumer<InternalWebRouteInterceptorRouter.InterceptorRoute<A>>>)this::pathRouteExtractor)
				.compose(this::methodRouteExtractor)
				.compose(this::contentTypeRouteExtractor)
				.compose(this::acceptRouteExtractor)
				.compose(this::languageRouteExtractor);
		}
	}

	/**
	 * <p>
	 * Internal Web interceptor route matcher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class InterceptorRouteMatcher<A extends ExchangeContext> extends AbstractWebRouteInterceptorRouteMatcher<A, WebExchange<A>, InternalWebRouter.Route<A>, InterceptorRoute<A>> {

		/**
		 * <p>
		 * Creates an internal Web interceptor route matcher.
		 * </p>
		 *
		 * @param interceptorRoute the internal Web interceptor route
		 * @param webRoute         the internal Web route
		 */
		private InterceptorRouteMatcher(InternalWebRouteInterceptorRouter.InterceptorRoute<A> interceptorRoute, InternalWebRouter.Route<A> webRoute) {
			super(interceptorRoute, webRoute);
		}

		/**
		 * <p>
		 * Returns the exchange interceptor when the route method matches the interceptor route method.
		 * </p>
		 *
		 * <p>
		 * The resulting exchange interceptor might be wrapped in an {@link ExchangeInterceptorWrapper} when the set defined by the route method is bigger than the set defined by the interceptor
		 * method.
		 * </p>
		 *
		 * @param interceptor an exchange interceptor
		 *
		 * @return an exchange interceptor or null if the route method doesn't match the interceptor route method
		 */
		private ExchangeInterceptor<A, WebExchange<A>> methodMatcher(ExchangeInterceptor<A, WebExchange<A>> interceptor) {
			if(interceptor == null) {
				return null;
			}
			Method interceptedMethod = this.interceptorRoute.getMethod();
			Method method = this.webRoute.getMethod();
			if(interceptedMethod != null) {
				if(method != null) {
					if(interceptedMethod == method) {
						// B\A == {}
						return interceptor;
					}
					return null;
				}
				else {
					// B\A != {}
					return new InternalWebRouteInterceptorRouter.FilteredMethodExchangeInterceptorWrapper<>(interceptedMethod, interceptor);
				}
			}
			else {
				// No restrictions
				return interceptor;
			}
		}

		@Override
		protected Function<ExchangeInterceptor<A, WebExchange<A>>, ExchangeInterceptor<A, WebExchange<A>>> matcher() {
			return ((Function<ExchangeInterceptor<A, WebExchange<A>>, ExchangeInterceptor<A, WebExchange<A>>>)this::pathMatcher)
				.andThen(this::methodMatcher)
				.andThen(this::contentTypeMatcher)
				.andThen(this::acceptMatcher)
				.andThen(this::languageMatcher);
		}
	}

	/**
	 * <p>
	 * Wraps an exchange interceptor to only intercept exchange whose method is matching a specific method.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	private static class FilteredMethodExchangeInterceptorWrapper<A extends ExchangeContext> extends ExchangeInterceptorWrapper<A, WebExchange<A>> {

		private final Method method;

		/**
		 * <p>
		 * Creates a filtered method exchange interceptor.
		 * </p>
		 *
		 * @param method      an HTTP method
		 * @param interceptor the exchange interceptor to filter
		 */
		public FilteredMethodExchangeInterceptorWrapper(Method method, ExchangeInterceptor<A, WebExchange<A>> interceptor) {
			super(interceptor);
			this.method = method;
		}

		@Override
		public Mono<? extends WebExchange<A>> intercept(WebExchange<A> exchange) {
			if(this.method == exchange.request().getMethod()) {
				// Interceptor method equals request method: the exchange must be intercepted
				return this.wrappedInterceptor.intercept(exchange);
			}
			// Interceptor method differs from request method: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}
}
