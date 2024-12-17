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
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.router.AbstractRouter;
import io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink;
import io.inverno.mod.http.base.router.link.ContentRoutingLink;
import io.inverno.mod.http.base.router.ErrorRoute;
import io.inverno.mod.http.base.router.link.ErrorRoutingLink;
import io.inverno.mod.http.base.router.link.OutboundAcceptContentRoutingLink;
import io.inverno.mod.http.base.router.link.PathRoutingLink;
import io.inverno.mod.http.base.router.RoutingLink;
import io.inverno.mod.web.server.internal.ErrorWebRouteHandler;
import io.inverno.mod.web.server.internal.GenericErrorWebExchange;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>
 * Internal error Web router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public final class InternalErrorWebRouter<A extends ExchangeContext> extends AbstractRouter<ErrorWebRouteHandler<A>, GenericErrorWebExchange<A>, InternalErrorWebRouter.Route<A>, InternalErrorWebRouter.RouteManager<A>, InternalErrorWebRouter<A>, InternalErrorWebRouter.RouteExtractor<A>> {

	/**
	 * <p>
	 * Creates an internal error Web router.
	 * </p>
	 */
	public InternalErrorWebRouter() {
		super(RoutingLink
			.<ErrorWebRouteHandler<A>, GenericErrorWebExchange<A>, InternalErrorWebRouter.Route<A>, InternalErrorWebRouter.RouteExtractor<A>>link(next -> new ErrorRoutingLink<>(next) {

				@Override
				protected Throwable getError(GenericErrorWebExchange<A> input) {
					return input.getError();
				}
			})
			.link(next -> new PathRoutingLink<>(next) {

				@Override
				protected String getNormalizedPath(GenericErrorWebExchange<A> input) {
					return input.request().getPathAbsolute();
				}

				@Override
				protected void setPathParameters(GenericErrorWebExchange<A> input, Map<String, String> parameters) {
					input.request().setPathParameters(parameters);
				}
			})
			.link(next -> new ContentRoutingLink<>(next) {

				@Override
				protected Headers.ContentType getContentTypeHeader(GenericErrorWebExchange<A> input) {
					return input.request().headers().getContentTypeHeader();
				}
			})
			.link(next -> new OutboundAcceptContentRoutingLink<>(next) {

				@Override
				protected List<Headers.Accept> getAllAcceptHeaders(GenericErrorWebExchange<A> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT);
				}
			})
			.link(ign -> new AcceptLanguageRoutingLink<>() {

				@Override
				protected List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(GenericErrorWebExchange<A> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT_LANGUAGE);
				}
			})
		);
	}

	@Override
	protected InternalErrorWebRouter.Route<A> createRoute(ErrorWebRouteHandler<A> resource, boolean disabled) {
		return new InternalErrorWebRouter.Route<>(this, resource, disabled);
	}

	@Override
	protected InternalErrorWebRouter.RouteManager<A> createRouteManager() {
		return new InternalErrorWebRouter.RouteManager<>(this);
	}

	@Override
	protected InternalErrorWebRouter.RouteExtractor<A> createRouteExtractor() {
		return new InternalErrorWebRouter.RouteExtractor<>(this);
	}

	/**
	 * <p>
	 * Internal error Web route.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class Route<A extends ExchangeContext>
		extends
			AbstractWebRoute<ErrorWebRouteHandler<A>, GenericErrorWebExchange<A>, InternalErrorWebRouter.Route<A>, InternalErrorWebRouter.RouteManager<A>, InternalErrorWebRouter<A>, InternalErrorWebRouter.RouteExtractor<A>>
		implements
			ErrorRoute<ErrorWebRouteHandler<A>> {

		private Class<? extends Throwable> errorType;

		/**
		 * <p>
		 * Creates an internal error Web route.
		 * </p>
		 *
		 * @param router   the internal error Web router
		 * @param resource the error Web route handler
		 * @param disabled true to create a disabled route, false otherwise
		 */
		private Route(InternalErrorWebRouter<A> router, ErrorWebRouteHandler<A> resource, boolean disabled) {
			super(router, resource, disabled);
		}

		@Override
		public Class<? extends Throwable> getErrorType() {
			return this.errorType;
		}

		/**
		 * <p>
		 * Sets the route error type.
		 * </p>
		 *
		 * @param errorType an error type
		 */
		void setErrorType(Class<? extends Throwable> errorType) {
			this.errorType = errorType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			InternalErrorWebRouter.Route<?> route = (InternalErrorWebRouter.Route<?>) o;
			return Objects.equals(errorType, route.errorType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), errorType);
		}

		@Override
		public String toString() {
			StringBuilder routeStringBuilder = new StringBuilder();

			routeStringBuilder.append("{");
			routeStringBuilder.append("\"path\":\"").append(this.getPath() != null ? this.getPath() : this.getPathPattern()).append("\",");
			routeStringBuilder.append("\"error\":\"").append(this.getErrorType() != null ? this.getErrorType() : null).append("\",");
			routeStringBuilder.append("\"consume\":\"").append(this.getContentType()).append("\",");
			routeStringBuilder.append("\"produce\":\"").append(this.getAccept()).append("\",");
			routeStringBuilder.append("\"language\":\"").append(this.getLanguage());
			routeStringBuilder.append("}");

			return routeStringBuilder.toString();
		}
	}

	/**
	 * <p>
	 * Internal error Web route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class RouteManager<A extends ExchangeContext>
		extends
			AbstractWebRouteManager<ErrorWebRouteHandler<A>, GenericErrorWebExchange<A>, InternalErrorWebRouter.Route<A>, InternalErrorWebRouter.RouteManager<A>, InternalErrorWebRouter<A>, InternalErrorWebRouter.RouteExtractor<A>>
		implements
			ErrorRoute.Manager<ErrorWebRouteHandler<A>, GenericErrorWebExchange<A>, InternalErrorWebRouter.Route<A>, InternalErrorWebRouter.RouteManager<A>, InternalErrorWebRouter<A>> {

		private Set<Class<? extends Throwable>> errorTypes;

		/**
		 * <p>
		 * Creates an internal error Web route manager.
		 * </p>
		 *
		 * @param router the internal error Web router
		 */
		private RouteManager(InternalErrorWebRouter<A> router) {
			super(router);
		}

		@Override
		public InternalErrorWebRouter.RouteManager<A> errorType(Class<? extends Throwable> errorType) {
			Objects.requireNonNull(errorType);
			if(this.errorTypes == null) {
				this.errorTypes = new HashSet<>();
			}
			this.errorTypes.add(errorType);
			return this;
		}

		/**
		 * <p>
		 * Determines whether the specified internal route error type is matching the route manager error type.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route error type is matching, false otherwise
		 */
		private boolean matchesErrorType(InternalErrorWebRouter.Route<A> route) {
			if(this.errorTypes != null && !this.errorTypes.isEmpty()) {
				return route.getErrorType() != null && this.errorTypes.contains(route.getErrorType());
			}
			return true;
		}

		@Override
		protected Predicate<InternalErrorWebRouter.Route<A>> routeMatcher() {
			return ((Predicate<InternalErrorWebRouter.Route<A>>)this::matchesErrorType).and(super.routeMatcher());
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
		private Consumer<InternalErrorWebRouter.RouteExtractor<A>> errorRouteExtractor(Consumer<InternalErrorWebRouter.RouteExtractor<A>> next) {
			return extractor -> {
				if(this.errorTypes != null && !this.errorTypes.isEmpty()) {
					for(Class<? extends Throwable> error : this.errorTypes) {
						next.accept(extractor.errorType(error));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		@Override
		protected Function<Consumer<InternalErrorWebRouter.RouteExtractor<A>>, Consumer<InternalErrorWebRouter.RouteExtractor<A>>> routeExtractor() {
			return ((Function<Consumer<InternalErrorWebRouter.RouteExtractor<A>>, Consumer<InternalErrorWebRouter.RouteExtractor<A>>>)this::errorRouteExtractor).andThen(super.routeExtractor());
		}
	}

	/**
	 * <p>
	 * Internal error Web route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class RouteExtractor<A extends ExchangeContext>
		extends
			AbstractWebRouteExtractor<ErrorWebRouteHandler<A>, GenericErrorWebExchange<A>, InternalErrorWebRouter.Route<A>, InternalErrorWebRouter.RouteManager<A>, InternalErrorWebRouter<A>, InternalErrorWebRouter.RouteExtractor<A>>
		implements
			ErrorRoute.Extractor<ErrorWebRouteHandler<A>, InternalErrorWebRouter.Route<A>, InternalErrorWebRouter.RouteExtractor<A>> {

		private Class<? extends Throwable> errorType;

		/**
		 * <p>
		 * Creates an internal error Web route extractor.
		 * </p>
		 *
		 * @param router the internal error Web router
		 */
		private RouteExtractor(InternalErrorWebRouter<A> router) {
			super(router);
		}

		/**
		 * <p>
		 * Creates an internal error Web route extractor.
		 * </p>
		 *
		 * @param parent the parent error Web route extractor
		 */
		private RouteExtractor(InternalErrorWebRouter.RouteExtractor<A> parent) {
			super(parent);
		}

		@Override
		protected InternalErrorWebRouter.RouteExtractor<A> createChildExtractor() {
			return new InternalErrorWebRouter.RouteExtractor<>(this);
		}

		/**
		 * <p>
		 * Returns the extracted route error type.
		 * </p>
		 *
		 * @return an error type
		 */
		public Class<? extends Throwable> getErrorType() {
			if(this.errorType != null) {
				return this.errorType;
			}
			return this.parent != null ? this.parent.getErrorType() : null;
		}

		@Override
		public InternalErrorWebRouter.RouteExtractor<A> errorType(Class<? extends Throwable> errorType) {
			InternalErrorWebRouter.RouteExtractor<A> childExtractor = this.createChildExtractor();
			childExtractor.errorType = errorType;
			return childExtractor;
		}

		@Override
		protected void populateRoute(InternalErrorWebRouter.Route<A> route) {
			route.setErrorType(this.getErrorType());
			super.populateRoute(route);
			route.get().setResponseContentType(route.getAccept());
		}
	}
}
