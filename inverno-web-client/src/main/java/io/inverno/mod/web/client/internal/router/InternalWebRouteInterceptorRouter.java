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
package io.inverno.mod.web.client.internal.router;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.router.AbstractRoute;
import io.inverno.mod.http.base.router.AbstractRouteExtractor;
import io.inverno.mod.http.base.router.AbstractRouteManager;
import io.inverno.mod.http.base.router.AbstractRouter;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.MethodRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import io.inverno.mod.http.base.router.URIRoute;
import io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink;
import io.inverno.mod.http.base.router.link.ContentRoutingLink;
import io.inverno.mod.http.base.router.link.InboundAcceptContentRoutingLink;
import io.inverno.mod.http.base.router.link.MethodRoutingLink;
import io.inverno.mod.http.base.router.link.URIRoutingLink;
import io.inverno.mod.web.client.WebExchange;
import io.inverno.mod.web.client.internal.WebRouteInterceptors;
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
 * Internal Web exchange interceptor router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public class InternalWebRouteInterceptorRouter<A extends ExchangeContext> extends AbstractRouter<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>> {

	/**
	 * <p>
	 * Creates an internal Web exchange interceptor router.
	 * </p>
	 */
	public InternalWebRouteInterceptorRouter() {
		super(RoutingLink
			.<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>link(next -> new URIRoutingLink<>(next) {

				@Override
				protected String getNormalizedURI(WebExchange<A> input) {
					// TODO this is probably a normalized URI already
					return input.request().getUri().normalize().toString();
				}

				@Override
				protected void setURIParameters(WebExchange<A> input, Map<String, String> parameters) {
					// Noop
				}
			})
			.link(next -> new MethodRoutingLink<>(next) {

				@Override
				protected Method getMethod(WebExchange<A> input) {
					return input.request().getMethod();
				}
			})
			.link(next -> new InboundAcceptContentRoutingLink<>(next) {

				@Override
				protected List<Headers.Accept> getAllAcceptHeaders(WebExchange<A> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT);
				}
			})
			.link(next -> new ContentRoutingLink<>(next) {

				@Override
				protected Headers.ContentType getContentTypeHeader(WebExchange<A> input) {
					return input.request().headers().getContentTypeHeader();
				}
			})
			.link(ign -> new AcceptLanguageRoutingLink<>() {

				@Override
				protected List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(WebExchange<A> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT_LANGUAGE);
				}
			})
		);
	}

	@Override
	protected Route<A> createRoute(WebRouteInterceptors<A> resource, boolean disabled) {
		return new InternalWebRouteInterceptorRouter.Route<>(this, resource, disabled);
	}

	@Override
	protected RouteManager<A> createRouteManager() {
		return new InternalWebRouteInterceptorRouter.RouteManager<>(this);
	}

	@Override
	protected RouteExtractor<A> createRouteExtractor() {
		return new InternalWebRouteInterceptorRouter.RouteExtractor<>(this);
	}

	/**
	 * <p>
	 * Internal Web exchange interceptor route.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public static final class Route<A extends ExchangeContext>
		extends
			AbstractRoute<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>
		implements
			URIRoute<WebRouteInterceptors<A>>,
			MethodRoute<WebRouteInterceptors<A>>,
			AcceptContentRoute<WebRouteInterceptors<A>>,
			ContentRoute<WebRouteInterceptors<A>>,
			AcceptLanguageRoute<WebRouteInterceptors<A>> {

		private String uri;
		private URIPattern uriPattern;
		private Method method;
		private String accept;
		private String contentType;
		private String language;

		/**
		 * <p>
		 * Creates a route.
		 * </p>
		 *
		 * @param router   the router
		 * @param resource the resource
		 * @param disabled true to create a disabled route, false otherwise
		 */
		private Route(InternalWebRouteInterceptorRouter<A> router, WebRouteInterceptors<A> resource, boolean disabled) {
			super(router, resource, disabled);
		}

		/**
		 * <p>
		 * Merges new interceptors into previously defined Web route interceptors when defined.
		 * </p>
		 *
		 * @return the route Web route interceptors or the previous Web route interceptors populated with the new interceptors
		 */
		@Override
		public WebRouteInterceptors<A> get(WebRouteInterceptors<A> previous) {
			if(previous == null) {
				return this.get();
			}
			previous.addAll(this.get().getInterceptors());
			return previous;
		}

		@Override
		public String getURI() {
			return this.uri;
		}

		/**
		 * <p>
		 * Sets the route URI.
		 * </p>
		 *
		 * @param uri a URI
		 */
		void setURI(String uri) {
			this.uri = uri;
		}

		@Override
		public URIPattern getURIPattern() {
			return this.uriPattern;
		}

		/**
		 * <p>
		 * Sets the route URI pattern.
		 * </p>
		 *
		 * @param uriPattern a URI pattern
		 */
		void setURIPattern(URIPattern uriPattern) {
			this.uriPattern = uriPattern;
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
		public String getAccept() {
			return this.accept;
		}

		/**
		 * <p>
		 * Sets the route accepted media range.
		 * </p>
		 *
		 * @param accept a media range
		 */
		void setAccept(String accept) {
			this.accept = accept;
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		/**
		 * <p>
		 * Sets the route content type.
		 * </p>
		 *
		 * @param contentType a media type
		 */
		void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Override
		public String getLanguage() {
			return this.language;
		}

		/**
		 * <p>
		 * Sets the route accepted language range.
		 * </p>
		 *
		 * @param language a language range
		 */
		void setLanguage(String language) {
			this.language = language;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			Route<?> route = (Route<?>) o;
			return Objects.equals(uri, route.uri) && Objects.equals(uriPattern, route.uriPattern) && method == route.method && Objects.equals(accept, route.accept) && Objects.equals(contentType, route.contentType) && Objects.equals(language, route.language);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), uri, uriPattern, method, accept, contentType, language);
		}

		@Override
		public String toString() {
			StringBuilder routeStringBuilder = new StringBuilder();

			routeStringBuilder.append("{");
			routeStringBuilder.append("\"uri\":\"").append(this.getURI() != null ? this.getURI() : this.getURIPattern()).append("\",");
			routeStringBuilder.append("\"method\":\"").append(this.getMethod()).append("\",");
			routeStringBuilder.append("\"produce\":\"").append(this.getAccept()).append("\",");
			routeStringBuilder.append("\"consume\":\"").append(this.getContentType()).append("\",");
			routeStringBuilder.append("\"language\":\"").append(this.getLanguage());
			routeStringBuilder.append("}");

			return routeStringBuilder.toString();
		}
	}

	/**
	 * <p>
	 * Internal Web exchange interceptor route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public static final class RouteManager<A extends ExchangeContext>
		extends
			AbstractRouteManager<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>
		implements
			URIRoute.Manager<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>>,
			MethodRoute.Manager<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>>,
			AcceptContentRoute.Manager<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>>,
			ContentRoute.Manager<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>>,
			AcceptLanguageRoute.Manager<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>> {


		private Set<String> uris;
		private Set<URIPattern> uriPatterns;
		private Set<Method> methods;
		private Set<String> accepts;
		private Set<String> contentTypes;
		private Set<String> languages;

		/**
		 * <p>
		 * Creates a route manager.
		 * </p>
		 *
		 * @param router the router
		 */
		private RouteManager(InternalWebRouteInterceptorRouter<A> router) {
			super(router);
		}

		@Override
		public RouteManager<A> uri(String uri) {
			Objects.requireNonNull(uri);
			if(this.uris == null) {
				this.uris = new HashSet<>();
			}
			this.uris.add(uri);
			return this;
		}

		@Override
		public RouteManager<A> uriPattern(URIPattern uriPattern) {
			Objects.requireNonNull(uriPattern);
			if(this.uriPatterns == null) {
				this.uriPatterns = new HashSet<>();
			}
			this.uriPatterns.add(uriPattern);
			return this;
		}

		@Override
		public RouteManager<A> method(Method method) {
			Objects.requireNonNull(method);
			if(this.methods == null) {
				this.methods = new HashSet<>();
			}
			this.methods.add(method);
			return this;
		}

		@Override
		public RouteManager<A> accept(String accept) {
			Objects.requireNonNull(accept);
			if(this.accepts == null) {
				this.accepts = new HashSet<>();
			}
			this.accepts.add(accept);
			return this;
		}

		@Override
		public RouteManager<A> contentType(String contentType) {
			Objects.requireNonNull(contentType);
			if(this.contentTypes == null) {
				this.contentTypes = new HashSet<>();
			}
			this.contentTypes.add(contentType);
			return this;
		}

		@Override
		public RouteManager<A> language(String language) {
			Objects.requireNonNull(language);
			if(this.languages == null) {
				this.languages = new HashSet<>();
			}
			this.languages.add(language);
			return this;
		}

		/**
		 * <p>
		 * Determines whether the specified route URI is matching the route manager URI.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route URI is matching, false otherwise
		 */
		private boolean matchesURI(InternalWebRouteInterceptorRouter.Route<A> route) {
			if(this.uris != null) {
				if(route.getURI() != null) {
					if(!this.uris.contains(route.getURI())) {
						return false;
					}
				}
				else if(route.getURIPattern() != null) {
					if(this.uris.stream().noneMatch(path -> route.getURIPattern().matcher(path).matches())) {
						return false;
					}
				}
				else {
					return false;
				}
			}
			if(this.uriPatterns != null) {
				if(route.getURI() != null) {
					return this.uriPatterns.stream().anyMatch(pattern -> pattern.matcher(route.getURI()).matches());
				}
				else if(route.getURIPattern() != null) {
					return this.uriPatterns.stream().anyMatch(pattern -> pattern.includes(route.getURIPattern()) != URIPattern.Inclusion.DISJOINT);
				}
				else {
					return false;
				}
			}
			return true;
		}

		/**
		 * <p>
		 * Determines whether the specified route method matching the route manager URI.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route method is matching, false otherwise
		 */
		private boolean matchesMethod(InternalWebRouteInterceptorRouter.Route<A> route) {
			if(this.methods != null && !this.methods.isEmpty()) {
				return route.getMethod() != null && this.methods.contains(route.getMethod());
			}
			return true;
		}

		/**
		 * <p>
		 * Determines whether the specified route accept matching the route manager URI.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route accept is matching, false otherwise
		 */
		private boolean matchesAccept(InternalWebRouteInterceptorRouter.Route<A> route) {
			if(this.accepts != null && !this.accepts.isEmpty()) {
				return route.getAccept() != null && this.accepts.contains(route.getAccept());
			}
			return true;
		}

		/**
		 * <p>
		 * Determines whether the specified route content type matching the route manager URI.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route content type is matching, false otherwise
		 */
		private boolean matchesContentType(InternalWebRouteInterceptorRouter.Route<A> route) {
			if(this.contentTypes != null && !this.contentTypes.isEmpty()) {
				return route.getContentType() != null && this.contentTypes.contains(route.getContentType());
			}
			return true;
		}
		/**
		 * <p>
		 * Determines whether the specified route language matching the route manager URI.
		 * </p>
		 *
		 * @param route an internal route
		 *
		 * @return true if the route language is matching, false otherwise
		 */
		private boolean matchesLanguage(InternalWebRouteInterceptorRouter.Route<A> route) {
			if(this.languages != null && !this.languages.isEmpty()) {
				return route.getLanguage() != null && this.languages.contains(route.getLanguage());
			}
			return true;
		}

		@Override
		protected Predicate<Route<A>> routeMatcher() {
			return ((Predicate<InternalWebRouteInterceptorRouter.Route<A>>)this::matchesURI)
				.and(this::matchesMethod)
				.and(this::matchesAccept)
				.and(this::matchesContentType)
				.and(this::matchesLanguage);
		}

		/**
		 * <p>
		 * Returns the URI route extractor.
		 * </p>
		 *
		 * @param next the next route extractor
		 *
		 * @return the URI route extractor
		 */
		private Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> uriRouteExtractor(Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> next) {
			return extractor -> {
				if(this.uris != null && !this.uris.isEmpty() || this.uriPatterns != null && !this.uriPatterns.isEmpty()) {
					if(this.uris != null) {
						for(String uri : this.uris) {
							next.accept(extractor.uri(uri));
						}
					}
					if(this.uriPatterns != null) {
						for(URIPattern uriPattern : this.uriPatterns) {
							next.accept(extractor.uriPattern(uriPattern));
						}
					}
				}
				else {
					next.accept(extractor);
				}
			};
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
		private Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> methodRouteExtractor(Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> next) {
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
		 * Returns the accept route extractor.
		 * </p>
		 *
		 * @param next the next route extractor
		 *
		 * @return the accept route extractor
		 */
		private Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> acceptRouteExtractor(Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> next) {
			return extractor -> {
				if(this.accepts != null && !this.accepts.isEmpty()) {
					for(String accept : this.accepts) {
						next.accept(extractor.accept(accept));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		/**
		 * <p>
		 * Returns the content type route extractor.
		 * </p>
		 *
		 * @param next the next route extractor
		 *
		 * @return the content type route extractor
		 */
		private Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> contentTypeRouteExtractor(Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> next) {
			return extractor -> {
				if(this.contentTypes != null && !this.contentTypes.isEmpty()) {
					for(String contentType : this.contentTypes) {
						next.accept(extractor.contentType(contentType));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		/**
		 * <p>
		 * Returns the language route extractor.
		 * </p>
		 *
		 * @param next the next route extractor
		 *
		 * @return the language route extractor
		 */
		private Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> languageRouteExtractor(Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>> next) {
			return extractor -> {
				if(this.languages != null && !this.languages.isEmpty()) {
					for(String language : this.languages) {
						next.accept(extractor.language(language));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		@Override
		protected Function<Consumer<RouteExtractor<A>>, Consumer<RouteExtractor<A>>> routeExtractor() {
			return ((Function<Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>>, Consumer<InternalWebRouteInterceptorRouter.RouteExtractor<A>>>)this::uriRouteExtractor)
				.andThen(this::methodRouteExtractor)
				.andThen(this::acceptRouteExtractor)
				.andThen(this::contentTypeRouteExtractor)
				.andThen(this::languageRouteExtractor);
		}
	}

	/**
	 * <p>
	 * Internal Web exchange interceptor route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the exchange context type
	 */
	public static final class RouteExtractor<A extends ExchangeContext>
		extends
			AbstractRouteExtractor<WebRouteInterceptors<A>, WebExchange<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteManager<A>, InternalWebRouteInterceptorRouter<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>
		implements
			URIRoute.Extractor<WebRouteInterceptors<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>,
			MethodRoute.Extractor<WebRouteInterceptors<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>,
			AcceptContentRoute.Extractor<WebRouteInterceptors<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>,
			ContentRoute.Extractor<WebRouteInterceptors<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>>,
			AcceptLanguageRoute.Extractor<WebRouteInterceptors<A>, InternalWebRouteInterceptorRouter.Route<A>, InternalWebRouteInterceptorRouter.RouteExtractor<A>> {

		private String uri;
		private URIPattern uriPattern;
		private Method method;
		private String accept;
		private String contentType;
		private String language;

		/**
		 * <p>
		 * Creates an internal Web exchange interceptor route extractor.
		 * </p>
		 *
		 * @param router the internal router
		 */
		private RouteExtractor(InternalWebRouteInterceptorRouter<A> router) {
			super(router);
		}

		/**
		 * <p>
		 * Creates an internal Web exchange interceptor route extractor.
		 * </p>
		 *
		 * @param parent the parent extractor
		 */
		private RouteExtractor(InternalWebRouteInterceptorRouter.RouteExtractor<A> parent) {
			super(parent);
		}

		/**
		 * <p>
		 * Returns the extracted route URI.
		 * </p>
		 *
		 * @return a URI
		 */
		public String getURI() {
			if(this.uri != null) {
				return this.uri;
			}
			return this.parent != null ? this.parent.getURI() : null;
		}

		@Override
		public RouteExtractor<A> uri(String uri) {
			InternalWebRouteInterceptorRouter.RouteExtractor<A> childExtractor = new InternalWebRouteInterceptorRouter.RouteExtractor<>(this);
			childExtractor.uri = uri;
			return childExtractor;
		}

		/**
		 * <p>
		 * Returns the extracted route URI pattern.
		 * </p>
		 *
		 * @return a URI pattern
		 */
		public URIPattern getURIPattern() {
			if(this.uriPattern != null) {
				return this.uriPattern;
			}
			return this.parent != null ? this.parent.getURIPattern() : null;
		}

		@Override
		public RouteExtractor<A> uriPattern(URIPattern uriPattern) {
			InternalWebRouteInterceptorRouter.RouteExtractor<A> childExtractor = new InternalWebRouteInterceptorRouter.RouteExtractor<>(this);
			childExtractor.uriPattern = uriPattern;
			return childExtractor;
		}

		/**
		 * <p>
		 * Returns the extracted route method.
		 * </p>
		 *
		 * @return an HTTP method
		 */
		public Method getMethod() {
			if(this.method != null) {
				return this.method;
			}
			return this.parent != null ? this.parent.getMethod() : null;
		}

		@Override
		public RouteExtractor<A> method(Method method) {
			InternalWebRouteInterceptorRouter.RouteExtractor<A> childExtractor = new InternalWebRouteInterceptorRouter.RouteExtractor<>(this);
			childExtractor.method = method;
			return childExtractor;
		}

		/**
		 * <p>
		 * Returns the extracted media range accepted by the route.
		 * </p>
		 *
		 * @return a media range
		 */
		public String getAccept() {
			if(this.accept != null) {
				return this.accept;
			}
			return this.parent != null ? this.parent.getAccept() : null;
		}

		@Override
		public RouteExtractor<A> accept(String accept) {
			InternalWebRouteInterceptorRouter.RouteExtractor<A> childExtractor = new InternalWebRouteInterceptorRouter.RouteExtractor<>(this);
			childExtractor.accept = accept;
			return childExtractor;
		}

		/**
		 * <p>
		 * Returns the extracted route content type.
		 * </p>
		 *
		 * @return a media type
		 */
		public String getContentType() {
			if(this.contentType != null) {
				return this.contentType;
			}
			return this.parent != null ? this.parent.getContentType() : null;
		}

		@Override
		public RouteExtractor<A> contentType(String contentType) {
			InternalWebRouteInterceptorRouter.RouteExtractor<A> childExtractor = new InternalWebRouteInterceptorRouter.RouteExtractor<>(this);
			childExtractor.contentType = contentType;
			return childExtractor;
		}

		/**
		 * <p>
		 * Returns the extracted language range accepted by the route.
		 * </p>
		 *
		 * @return a language range
		 */
		public String getLanguage() {
			if(this.language != null) {
				return this.language;
			}
			return this.parent != null ? this.parent.getLanguage() : null;
		}

		@Override
		public RouteExtractor<A> language(String language) {
			InternalWebRouteInterceptorRouter.RouteExtractor<A> childExtractor = new InternalWebRouteInterceptorRouter.RouteExtractor<>(this);
			childExtractor.language = language;
			return childExtractor;
		}

		@Override
		protected void populateRoute(Route<A> route) {
			if(this.getURI() != null) {
				route.setURI(this.getURI());
			}
			else if(this.getURIPattern() != null) {
				route.setURIPattern(this.getURIPattern());
			}
			route.setMethod(this.getMethod());
			route.setAccept(this.getAccept());
			route.setContentType(this.getContentType());
			route.setLanguage(this.getLanguage());
		}
	}
}
