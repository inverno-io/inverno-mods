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
import io.inverno.mod.http.base.router.ErrorRoute;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.server.ErrorWebExchange;
import io.inverno.mod.web.server.internal.ExchangeInterceptorWrapper;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Internal error Web route interceptor router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public final class InternalErrorWebRouteInterceptorRouter<A extends ExchangeContext> extends AbstractWebRouteInterceptorRouter<
		A,
		ErrorWebExchange<A>,
		InternalErrorWebRouter.Route<A>,
		InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>,
		InternalErrorWebRouteInterceptorRouter.InterceptorRouteManager<A>,
		InternalErrorWebRouteInterceptorRouter<A>,
		InternalErrorWebRouteInterceptorRouter.InterceptorRouteMatcher<A>
	> {

	/**
	 * <p>
	 * Creates an internal error Web route interceptor router.
	 * </p>
	 */
	public InternalErrorWebRouteInterceptorRouter() {
	}

	/**
	 * <p>
	 * Creates an internal error Web route interceptor router.
	 * </p>
	 *
	 * @param parent the parent router
	 */
	private InternalErrorWebRouteInterceptorRouter(InternalErrorWebRouteInterceptorRouter<A> parent) {
		super(parent);
	}

	@Override
	protected InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A> createRoute(InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A> parentRoute) {
		return new InternalErrorWebRouteInterceptorRouter.InterceptorRoute<>(parentRoute);
	}

	@Override
	protected InternalErrorWebRouteInterceptorRouter.InterceptorRouteManager<A> createRouteManager() {
		return new InternalErrorWebRouteInterceptorRouter.InterceptorRouteManager<>(new InternalErrorWebRouteInterceptorRouter<>(this));
	}

	@Override
	protected InternalErrorWebRouteInterceptorRouter.InterceptorRouteMatcher<A> createRouteMatcher(InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A> interceptorRoute, InternalErrorWebRouter.Route<A> webRoute) {
		return new InternalErrorWebRouteInterceptorRouter.InterceptorRouteMatcher<>(interceptorRoute, webRoute);
	}

	/**
	 * <p>
	 * Internal error Web interceptor route.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class InterceptorRoute<A extends ExchangeContext>
		extends
			AbstractWebRouteInterceptorRoute<A, ErrorWebExchange<A>, InterceptorRoute<A>>
		implements
			ErrorRoute<ExchangeInterceptor<A, ErrorWebExchange<A>>> {

		private Class<? extends Throwable> errorType;

		/**
		 * <p>
		 * Creates an internal error Web interceptor route.
		 * </p>
		 *
		 * @param parent the parent route
		 */
		private InterceptorRoute(InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A> parent) {
			super(parent);
		}

		@Override
		public Class<? extends Throwable> getErrorType() {
			if(this.errorType != null) {
				return this.errorType;
			}
			return this.parent != null ? this.parent.getErrorType() : null;
		}

		/**
		 * <p>
		 * Sets the route error type.
		 * </p>
		 *
		 * @param errorType an error type
		 */
		public void setErrorType(Class<? extends Throwable> errorType) {
			this.errorType = errorType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			InternalErrorWebRouteInterceptorRouter.InterceptorRoute<?> that = (InternalErrorWebRouteInterceptorRouter.InterceptorRoute<?>) o;
			return Objects.equals(errorType, that.errorType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), errorType);
		}

		@Override
		public String toString() {
			StringBuilder routeStringBuilder = new StringBuilder();

			routeStringBuilder.append("{");
			routeStringBuilder.append("\"error\":\"").append(this.getErrorType()).append("\",");
			routeStringBuilder.append("\"path\":\"").append(this.getPath() != null ? this.getPath() : this.getPathPattern()).append("\",");
			routeStringBuilder.append("\"consume\":\"").append(this.getContentType()).append("\",");
			routeStringBuilder.append("\"produce\":\"").append(this.getAccept()).append("\",");
			routeStringBuilder.append("\"language\":\"").append(this.getLanguage());
			routeStringBuilder.append("}");

			return routeStringBuilder.toString();
		}
	}

	/**
	 * <p>
	 * Internal error Web interceptor route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class InterceptorRouteManager<A extends ExchangeContext>
		extends
			AbstractWebRouteInterceptorRouteManager<
				A,
				ErrorWebExchange<A>,
				InternalErrorWebRouter.Route<A>,
				InterceptorRoute<A>,
				InterceptorRouteManager<A>,
				InternalErrorWebRouteInterceptorRouter<A>,
				InterceptorRouteMatcher<A>
			>
		implements
			ErrorRoute.Manager<
				ExchangeInterceptor<A, ErrorWebExchange<A>>,
				InternalErrorWebRouter.Route<A>,
				InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>,
				InternalErrorWebRouteInterceptorRouter.InterceptorRouteManager<A>,
				InternalErrorWebRouteInterceptorRouter<A>
			> {

		private Set<Class<? extends Throwable>> errorTypes;

		/**
		 * <p>
		 * Creates an internal error Web interceptor route manager.
		 * </p>
		 *
		 * @param router the internal error Web interceptor route manager
		 */
		private InterceptorRouteManager(InternalErrorWebRouteInterceptorRouter<A> router) {
			super(router);
		}

		@Override
		public InternalErrorWebRouteInterceptorRouter.InterceptorRouteManager<A> errorType(Class<? extends Throwable> errorType) {
			Objects.requireNonNull(errorType);
			if(this.errorTypes == null) {
				this.errorTypes = new HashSet<>();
			}
			this.errorTypes.add(errorType);
			return this;
		}

		/**
		 * <p>
		 * Returns the error type route extractor.
		 * </p>
		 *
		 * @param next the next route extractor
		 *
		 * @return the error type route extractor
		 */
		private Consumer<InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>> errorTypeRouteExtractor(Consumer<InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>> next) {
			return route -> {
				if(this.errorTypes != null && !this.errorTypes.isEmpty()) {
					for (Class<? extends Throwable> errorType : this.errorTypes) {
						InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A> childRoute = this.router.createRoute(route);
						childRoute.setErrorType(errorType);
						next.accept(childRoute);
					}
				}
				else {
					next.accept(route);
				}
			};
		}

		@Override
		protected Function<Consumer<InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>>, Consumer<InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>>> routeExtractor() {
			return ((Function<Consumer<InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>>, Consumer<InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A>>>)this::errorTypeRouteExtractor).compose(super.routeExtractor());
		}
	}

	/**
	 * <p>
	 * Internal error Web interceptor route matcher.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class InterceptorRouteMatcher<A extends ExchangeContext> extends AbstractWebRouteInterceptorRouteMatcher<A, ErrorWebExchange<A>, InternalErrorWebRouter.Route<A>, InterceptorRoute<A>> {

		/**
		 * <p>
		 * Creates an internal error Web interceptor route matcher.
		 * </p>
		 *
		 * @param interceptorRoute the internal error Web interceptor route
		 * @param webRoute         the internal error Web route
		 */
		private InterceptorRouteMatcher(InternalErrorWebRouteInterceptorRouter.InterceptorRoute<A> interceptorRoute, InternalErrorWebRouter.Route<A> webRoute) {
			super(interceptorRoute, webRoute);
		}

		/**
		 * <p>
		 * Returns the exchange interceptor when the route error type matches the interceptor route error type.
		 * </p>
		 *
		 * <p>
		 * The resulting exchange interceptor might be wrapped in an {@link ExchangeInterceptorWrapper} when the set defined by the route error type is bigger than the set defined by the interceptor
		 * error type.
		 * </p>
		 *
		 * @param interceptor an exchange interceptor
		 *
		 * @return an exchange interceptor or null if the route error type doesn't match the interceptor route error type
		 */
		private ExchangeInterceptor<A, ErrorWebExchange<A>> errorTypeMatcher(ExchangeInterceptor<A, ErrorWebExchange<A>> interceptor) {
			if(interceptor == null) {
				return null;
			}
			Class<? extends Throwable> interceptedErrorType = this.interceptorRoute.getErrorType();
			Class<? extends Throwable> errorType = this.webRoute.getErrorType();
			if(interceptedErrorType != null) {
				if(errorType != null) {
					if(interceptedErrorType.isAssignableFrom(errorType)) {
						// B\A == {}
						return interceptor;
					}
					return null;
				}
				else {
					// B\A != {}
					return new InternalErrorWebRouteInterceptorRouter.FilteredErrorExchangeInterceptorWrapper<>(interceptedErrorType, interceptor);
				}
			}
			else {
				// No restrictions
				return interceptor;
			}
		}

		@Override
		protected Function<ExchangeInterceptor<A, ErrorWebExchange<A>>, ExchangeInterceptor<A, ErrorWebExchange<A>>> matcher() {
			return ((Function<ExchangeInterceptor<A, ErrorWebExchange<A>>, ExchangeInterceptor<A, ErrorWebExchange<A>>>)this::errorTypeMatcher)
				.andThen(super.matcher());
		}
	}

	/**
	 * <p>
	 * Wraps an error exchange interceptor to only intercept error exchange whose error is matching a specific error type.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	private static class FilteredErrorExchangeInterceptorWrapper<A extends ExchangeContext> extends ExchangeInterceptorWrapper<A, ErrorWebExchange<A>> {

		private final Class<? extends Throwable> errorType;

		/**
		 * <p>
		 * Creates a filtered content exchange interceptor.
		 * </p>
		 *
		 * @param errorType    an error type
		 * @param interceptor the exchange interceptor to filter
		 */
		public FilteredErrorExchangeInterceptorWrapper(Class<? extends Throwable> errorType, ExchangeInterceptor<A, ErrorWebExchange<A>> interceptor) {
			super(interceptor);
			this.errorType = errorType;
		}

		@Override
		public Mono<? extends ErrorWebExchange<A>> intercept(ErrorWebExchange<A> exchange) {
			if(this.errorType.isAssignableFrom(exchange.getError().getClass())) {
				// Interceptor error is assignable from the exchange error type: the exchange must be intercepted
				return this.wrappedInterceptor.intercept(exchange);
			}
			// Interceptor error is not assignable from the exchange error type: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}
}
