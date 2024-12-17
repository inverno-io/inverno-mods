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
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.router.AbstractRouter;
import io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink;
import io.inverno.mod.http.base.router.link.ContentRoutingLink;
import io.inverno.mod.http.base.router.MethodRoute;
import io.inverno.mod.http.base.router.link.MethodRoutingLink;
import io.inverno.mod.http.base.router.link.OutboundAcceptContentRoutingLink;
import io.inverno.mod.http.base.router.link.PathRoutingLink;
import io.inverno.mod.http.base.router.RoutingLink;
import io.inverno.mod.http.base.router.WebSocketSubprotocolRoute;
import io.inverno.mod.http.base.router.link.WebSocketSubprotocolRoutingLink;
import io.inverno.mod.web.server.internal.GenericWebExchange;
import io.inverno.mod.web.server.internal.WebRouteHandler;
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
 * Internal Web router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public final class InternalWebRouter<A extends ExchangeContext> extends AbstractRouter<WebRouteHandler<A>, GenericWebExchange<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteManager<A>, InternalWebRouter<A>, InternalWebRouter.RouteExtractor<A>> {

	/**
	 * <p>
	 * Creates an internal Web router.
	 * </p>
	 */
	public InternalWebRouter() {
		super(RoutingLink
			.<WebRouteHandler<A>, GenericWebExchange<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteExtractor<A>>link(next -> new PathRoutingLink<>(next) {

				@Override
				protected String getNormalizedPath(GenericWebExchange<A> input) {
					return input.request().getPathAbsolute();
				}

				@Override
				protected void setPathParameters(GenericWebExchange<A> input, Map<String, String> parameters) {
					input.request().setPathParameters(parameters);
				}
			})
			.link(next -> new MethodRoutingLink<>(next) {

				@Override
				protected Method getMethod(GenericWebExchange<A> input) {
					return input.request().getMethod();
				}
			})
			.link(next -> new ContentRoutingLink<>(next) {

				@Override
				protected Headers.ContentType getContentTypeHeader(GenericWebExchange<A> input) {
					return input.request().headers().getContentTypeHeader();
				}
			})
			.link(next -> new OutboundAcceptContentRoutingLink<>(next) {

				@Override
				protected List<Headers.Accept> getAllAcceptHeaders(GenericWebExchange<A> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT);
				}
			})
			.link(next -> new AcceptLanguageRoutingLink<>(next) {

				@Override
				protected List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(GenericWebExchange<A> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT_LANGUAGE);
				}
			})
			.link(ign -> new WebSocketSubprotocolRoutingLink<>() {

				@Override
				protected List<String> getSubprotocols(GenericWebExchange<A> input) {
					return input.request().headers().getAll(Headers.NAME_SEC_WEBSOCKET_PROTOCOL);
				}
			})
		);
	}

	@Override
	protected InternalWebRouter.Route<A> createRoute(WebRouteHandler<A> resource, boolean disabled) {
		return new InternalWebRouter.Route<>(this, resource, disabled);
	}

	@Override
	protected InternalWebRouter.RouteManager<A> createRouteManager() {
		return new InternalWebRouter.RouteManager<>(this);
	}

	@Override
	protected InternalWebRouter.RouteExtractor<A> createRouteExtractor() {
		return new InternalWebRouter.RouteExtractor<>(this);
	}

	/**
	 * <p>
	 * Internal Web route.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class Route<A extends ExchangeContext>
		extends
			AbstractWebRoute<WebRouteHandler<A>, GenericWebExchange<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteManager<A>, InternalWebRouter<A>, InternalWebRouter.RouteExtractor<A>>
		implements
			MethodRoute<WebRouteHandler<A>>,
			WebSocketSubprotocolRoute<WebRouteHandler<A>> {

		private Method method;
		private String subprotocol;

		/**
		 * <p>
		 * Creates an internal Web route.
		 * </p>
		 *
		 * @param router   the internal Web router
		 * @param resource the Web route handler
		 * @param disabled true to create a disabled route, false otherwise
		 */
		private Route(InternalWebRouter<A> router, WebRouteHandler<A> resource, boolean disabled) {
			super(router, resource, disabled);
		}

		@Override
		public Method getMethod() {
			return this.method;
		}

		/**
		 * <p>
		 * Sets the route method.
		 * </p>
		 *
		 * @param method an HTTP method
		 */
		void setMethod(Method method) {
			this.method = method;
		}

		@Override
		public String getSubprotocol() {
			return this.subprotocol;
		}

		/**
		 * <p>
		 * Sets the route WebSocket subprotocol.
		 * </p>
		 *
		 * @param subprotocol a WebSocket subprotocol
		 */
		void setSubprotocol(String subprotocol) {
			this.subprotocol = subprotocol;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			InternalWebRouter.Route<?> route = (InternalWebRouter.Route<?>) o;
			return method == route.method && Objects.equals(subprotocol, route.subprotocol);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), method, subprotocol);
		}

		@Override
		public String toString() {
			StringBuilder routeStringBuilder = new StringBuilder();

			routeStringBuilder.append("{");
			routeStringBuilder.append("\"path\":\"").append(this.getPath() != null ? this.getPath() : this.getPathPattern()).append("\",");
			if(this.get().getWebSocketHandler() != null) {
				routeStringBuilder.append("\"method\":\"").append(this.getMethod()).append("\",");
				routeStringBuilder.append("\"consume\":\"").append(this.getContentType()).append("\",");
				routeStringBuilder.append("\"produce\":\"").append(this.getAccept()).append("\",");
				routeStringBuilder.append("\"language\":\"").append(this.getLanguage());
			}
			else {
				routeStringBuilder.append("\"language\":\"").append(this.getLanguage()).append("\",");
				routeStringBuilder.append("\"subprotocol\":\"").append(this.getSubprotocol());
			}
			routeStringBuilder.append("}");

			return routeStringBuilder.toString();
		}
	}

	/**
	 * <p>
	 * Internal Web route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class RouteManager<A extends ExchangeContext>
		extends
			AbstractWebRouteManager<WebRouteHandler<A>, GenericWebExchange<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteManager<A>, InternalWebRouter<A>, InternalWebRouter.RouteExtractor<A>>
		implements
			MethodRoute.Manager<WebRouteHandler<A>, GenericWebExchange<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteManager<A>, InternalWebRouter<A>>,
			WebSocketSubprotocolRoute.Manager<WebRouteHandler<A>, GenericWebExchange<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteManager<A>, InternalWebRouter<A>> {

		private Set<Method> methods;
		private Set<String> subprotocols;

		/**
		 * <p>
		 * Creates an internal Web route manager.
		 * </p>
		 *
		 * @param router the internal Web router
		 */
		private RouteManager(InternalWebRouter<A> router) {
			super(router);
		}

		@Override
		public InternalWebRouter.RouteManager<A> method(Method method) {
			Objects.requireNonNull(method);
			if(this.methods == null) {
				this.methods = new HashSet<>();
			}
			this.methods.add(method);
			return this;
		}

		@Override
		public InternalWebRouter.RouteManager<A> subprotocol(String subprotocol) {
			Objects.requireNonNull(subprotocol);
			if(this.subprotocols == null) {
				this.subprotocols = new HashSet<>();
			}
			this.subprotocols.add(subprotocol);
			return this;
		}

		/**
		 * <p>
		 * Determines whether the specified internal route method is matching the route manager method.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route method is matching, false otherwise
		 */
		private boolean matchesMethod(InternalWebRouter.Route<A> route) {
			if(this.methods != null && !this.methods.isEmpty()) {
				return route.getMethod() != null && this.methods.contains(route.getMethod());
			}
			return true;
		}

		/**
		 * <p>
		 * Determines whether the specified internal route WebSocket subProtocol is matching the route manager WebSocket subProtocol.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route WebSocket subProtocol is matching, false otherwise
		 */
		private boolean matchesSubProtocol(InternalWebRouter.Route<A> route) {
			if(this.subprotocols != null && !this.subprotocols.isEmpty()) {
				return route.getSubprotocol() != null && this.subprotocols.contains(route.getSubprotocol());
			}
			return true;
		}

		@Override
		protected Predicate<InternalWebRouter.Route<A>> routeMatcher() {
			return ((Predicate<InternalWebRouter.Route<A>>)this::matchesPath)
				.and(this::matchesMethod)
				.and(this::matchesContentType)
				.and(this::matchesAccept)
				.and(this::matchesLanguage)
				.and(this::matchesSubProtocol);
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
		private Consumer<InternalWebRouter.RouteExtractor<A>> methodRouteExtractor(Consumer<InternalWebRouter.RouteExtractor<A>> next) {
			return extractor -> {
				if(this.methods != null && !this.methods.isEmpty()) {
					for(Method method : this.methods) {
						next.accept(extractor.method(method));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		/**
		 * <p>
		 * Returns the WebSocket subProtocol route extractor.
		 * </p>
		 *
		 * @param next the next route extractor
		 *
		 * @return the WebSocket subProtocol route extractor
		 */
		private Consumer<InternalWebRouter.RouteExtractor<A>> subprotocolExtractor(Consumer<InternalWebRouter.RouteExtractor<A>> next) {
			return extractor -> {
				if(this.subprotocols != null && !this.subprotocols.isEmpty()) {
					for(String subprotocol : this.subprotocols) {
						next.accept(extractor.subprotocol(subprotocol));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		@Override
		protected Function<Consumer<InternalWebRouter.RouteExtractor<A>>, Consumer<InternalWebRouter.RouteExtractor<A>>> routeExtractor() {
			return ((Function<Consumer<InternalWebRouter.RouteExtractor<A>>, Consumer<InternalWebRouter.RouteExtractor<A>>>)this::pathRouteExtractor)
				.andThen(this::methodRouteExtractor)
				.andThen(this::contentTypeRouteExtractor)
				.andThen(this::acceptRouteExtractor)
				.andThen(this::languageRouteExtractor)
				.andThen(this::subprotocolExtractor);
		}
	}

	/**
	 * <p>
	 * Internal Web route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public final static class RouteExtractor<A extends ExchangeContext>
		extends
			AbstractWebRouteExtractor<WebRouteHandler<A>, GenericWebExchange<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteManager<A>, InternalWebRouter<A>, InternalWebRouter.RouteExtractor<A>>
		implements
			MethodRoute.Extractor<WebRouteHandler<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteExtractor<A>>,
			WebSocketSubprotocolRoute.Extractor<WebRouteHandler<A>, InternalWebRouter.Route<A>, InternalWebRouter.RouteExtractor<A>> {

		private Method method;
		private String subprotocol;

		/**
		 * <p>
		 * Creates an internal Web route extractor.
		 * </p>
		 *
		 * @param router the internal Web router
		 */
		private RouteExtractor(InternalWebRouter<A> router) {
			super(router);
		}

		/**
		 * <p>
		 * Creates an internal Web route extractor.
		 * </p>
		 *
		 * @param parent the parent Web route extractor
		 */
		private RouteExtractor(InternalWebRouter.RouteExtractor<A> parent) {
			super(parent);
		}

		@Override
		protected InternalWebRouter.RouteExtractor<A> createChildExtractor() {
			return new InternalWebRouter.RouteExtractor<>(this);
		}

		/**
		 * <p>
		 * Returns the extracted route method.
		 * </p>
		 *
		 * @return an HTTP method
		 */
		private Method getMethod() {
			if(this.method != null) {
				return this.method;
			}
			return this.parent != null ? this.parent.getMethod() : null;
		}

		@Override
		public InternalWebRouter.RouteExtractor<A> method(Method method) {
			InternalWebRouter.RouteExtractor<A> childExtractor = this.createChildExtractor();
			childExtractor.method = method;
			return childExtractor;
		}

		/**
		 * <p>
		 * Returns the extracted route WebSocket subProtocol.
		 * </p>
		 *
		 * @return a WebSocket subProtocol
		 */
		private String getSubprotocol() {
			if(this.subprotocol != null) {
				return this.subprotocol;
			}
			return this.parent != null ? this.parent.getSubprotocol() : null;
		}

		@Override
		public InternalWebRouter.RouteExtractor<A> subprotocol(String subprotocol) {
			InternalWebRouter.RouteExtractor<A> childExtractor = this.createChildExtractor();
			childExtractor.subprotocol = subprotocol;
			return childExtractor;
		}

		@Override
		protected void populateRoute(InternalWebRouter.Route<A> route) {
			super.populateRoute(route);
			route.setMethod(this.getMethod());
			route.setSubprotocol(this.getSubprotocol());

			route.get().setSubprotocol(this.getSubprotocol());
			route.get().setResponseContentType(route.getAccept());
		}
	}
}
