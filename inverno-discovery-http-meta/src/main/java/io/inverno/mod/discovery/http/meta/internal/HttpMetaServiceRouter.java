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
package io.inverno.mod.discovery.http.meta.internal;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.router.AbstractRoute;
import io.inverno.mod.http.base.router.AbstractRouteExtractor;
import io.inverno.mod.http.base.router.AbstractRouteManager;
import io.inverno.mod.http.base.router.AbstractRouter;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.AuthorityRoute;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.HeadersRoute;
import io.inverno.mod.http.base.router.MethodRoute;
import io.inverno.mod.http.base.router.PathRoute;
import io.inverno.mod.http.base.router.QueryParametersRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink;
import io.inverno.mod.http.base.router.link.AuthorityRoutingLink;
import io.inverno.mod.http.base.router.link.ContentRoutingLink;
import io.inverno.mod.http.base.router.link.HeadersRoutingLink;
import io.inverno.mod.http.base.router.link.MethodRoutingLink;
import io.inverno.mod.http.base.router.link.OutboundAcceptContentRoutingLink;
import io.inverno.mod.http.base.router.link.PathRoutingLink;
import io.inverno.mod.http.base.router.link.QueryParametersRoutingLink;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * <p>
 * An HTTP meta service router.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public final class HttpMetaServiceRouter extends AbstractRouter<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, HttpMetaServiceRouter.Route, HttpMetaServiceRouter.RouteManager, HttpMetaServiceRouter, HttpMetaServiceRouter.RouteExtractor> {

	/**
	 * <p>
	 * Creates an HTTP meta service router.
	 * </p>
	 */
	public HttpMetaServiceRouter() {
		super(RoutingLink
			.<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteExtractor>link(next -> new AuthorityRoutingLink<>(next) {

				@Override
				protected String getAuthority(UnboundExchange<?> input) {
					return input.request().getAuthority();
				}
			})
			.link(next -> new PathRoutingLink<>(next) {

				@Override
				protected String getNormalizedPath(UnboundExchange<?> input) {
					return input.request().getPathAbsolute();
				}

				@Override
				protected void setPathParameters(UnboundExchange<?> input, Map<String, String> parameters) {
					// Noop?
				}
			})
			.link(next -> new MethodRoutingLink<>(next) {

				@Override
				protected Method getMethod(UnboundExchange<?> input) {
					return input.request().getMethod();
				}
			})
			.link(next -> new ContentRoutingLink<>(next) {

				@Override
				protected Headers.ContentType getContentTypeHeader(UnboundExchange<?> input) {
					return input.request().headers().getContentTypeHeader();
				}
			})
			.link(next -> new OutboundAcceptContentRoutingLink<>(next) {

				@Override
				protected List<Headers.Accept> getAllAcceptHeaders(UnboundExchange<?> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT);
				}
			})
			.link(next -> new AcceptLanguageRoutingLink<>(next) {

				@Override
				protected List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(UnboundExchange<?> input) {
					return input.request().headers().getAllHeader(Headers.NAME_ACCEPT_LANGUAGE);
				}
			})
			.link(next -> new HeadersRoutingLink<>(next) {

				@Override
				protected InboundHeaders getHeaders(UnboundExchange<?> input) {
					return input.request().headers();
				}
			})
			.link(ign -> new QueryParametersRoutingLink<>() {

				@Override
				protected QueryParameters getQueryParameters(UnboundExchange<?> input) {
					return null;
				}
			})
		);
	}

	@Override
	protected Route createRoute(AbstractHttpMetaServiceRoute<?, ?> resource, boolean disabled) {
		return new Route(this, resource, disabled);
	}

	@Override
	protected RouteManager createRouteManager() {
		return new RouteManager(this);
	}

	@Override
	protected RouteExtractor createRouteExtractor() {
		return new RouteExtractor(this);
	}

	/**
	 * <p>
	 * Internal route.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public static final class Route
		extends
			AbstractRoute<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter, RouteExtractor>
		implements
			AuthorityRoute<AbstractHttpMetaServiceRoute<?, ?>>,
			PathRoute<AbstractHttpMetaServiceRoute<?, ?>>,
			MethodRoute<AbstractHttpMetaServiceRoute<?, ?>>,
			ContentRoute<AbstractHttpMetaServiceRoute<?, ?>>,
			AcceptContentRoute<AbstractHttpMetaServiceRoute<?, ?>>,
			AcceptLanguageRoute<AbstractHttpMetaServiceRoute<?, ?>>,
			HeadersRoute<AbstractHttpMetaServiceRoute<?, ?>>,
			QueryParametersRoute<AbstractHttpMetaServiceRoute<?, ?>> {

		private String authority;
		private Pattern authorityPattern;
		private String path;
		private URIPattern pathPattern;
		private Method method;
		private String contentType;
		private String accept;
		private String language;
		private Map<String, HeaderMatcher> headersMatchers;
		private Map<String, ParameterMatcher> queryParametersMatchers;

		/**
		 * <p>
		 * Creates a route.
		 * </p>
		 *
		 * @param router   the router
		 * @param resource the resource
		 * @param disabled true to create a disabled route, false otherwise
		 */
		private Route(HttpMetaServiceRouter router, AbstractHttpMetaServiceRoute<?, ?> resource, boolean disabled) {
			super(router, resource, disabled);
		}

		@Override
		public String getAuthority() {
			return this.authority;
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}

		@Override
		public Pattern getAuthorityPattern() {
			return this.authorityPattern;
		}

		public void setAuthorityPattern(Pattern authorityPattern) {
			this.authorityPattern = authorityPattern;
		}

		@Override
		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		@Override
		public URIPattern getPathPattern() {
			return this.pathPattern;
		}

		public void setPathPattern(URIPattern pathPattern) {
			this.pathPattern = pathPattern;
		}

		@Override
		public Method getMethod() {
			return this.method;
		}

		public void setMethod(Method method) {
			this.method = method;
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Override
		public String getAccept() {
			return this.accept;
		}

		public void setAccept(String accept) {
			this.accept = accept;
		}

		@Override
		public String getLanguage() {
			return this.language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		@Override
		public Map<String, HeaderMatcher> getHeadersMatchers() {
			return this.headersMatchers;
		}

		public void setHeadersMatchers(Map<String, HeaderMatcher> headersMatchers) {
			this.headersMatchers = headersMatchers;
		}

		@Override
		public Map<String, ParameterMatcher> getQueryParameterMatchers() {
			return this.queryParametersMatchers;
		}

		public void setQueryParametersMatchers(Map<String, ParameterMatcher> queryParametersMatchers) {
			this.queryParametersMatchers = queryParametersMatchers;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			Route route = (Route) o;
			return Objects.equals(authority, route.authority) && Objects.equals(authorityPattern, route.authorityPattern) && Objects.equals(path, route.path) && Objects.equals(pathPattern, route.pathPattern) && method == route.method && Objects.equals(contentType, route.contentType) && Objects.equals(accept, route.accept) && Objects.equals(language, route.language) && Objects.equals(headersMatchers, route.headersMatchers) && Objects.equals(queryParametersMatchers, route.queryParametersMatchers);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), authority, authorityPattern, path, pathPattern, method, contentType, accept, language, headersMatchers, queryParametersMatchers);
		}
	}

	/**
	 * <p>
	 * Internal route manager.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public static final class RouteManager 
		extends
			AbstractRouteManager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter, RouteExtractor>
		implements
			AuthorityRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter>,
			PathRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter>,
			MethodRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter>,
			ContentRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter>,
			AcceptContentRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter>,
			AcceptLanguageRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter>,
			HeadersRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter>,
			QueryParametersRoute.Manager<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter> {

		private Set<String> authorities;
		private Set<Pattern> authorityPatterns;
		private Set<String> paths;
		private Set<URIPattern> pathPatterns;
		private Set<Method> methods;
		private Set<String> contentTypes;
		private Set<String> accepts;
		private Set<String> languages;
		private Set<Map<String, HeadersRoute.HeaderMatcher>> headersMatcherss;
		private Set<Map<String, QueryParametersRoute.ParameterMatcher>> queryParametersMatcherss;

		/**
		 * <p>
		 * Creates a route manager.
		 * </p>
		 *
		 * @param router the router
		 */
		private RouteManager(HttpMetaServiceRouter router) {
			super(router);
		}

		@Override
		public RouteManager authority(String authority) {
			Objects.requireNonNull(authority);
			if(this.authorities == null) {
				this.authorities = new HashSet<>();
			}
			this.authorities.add(authority);
			return this;
		}

		@Override
		public RouteManager authorityPattern(Pattern authorityPattern) {
			Objects.requireNonNull(authorityPattern);
			if(this.authorityPatterns == null) {
				this.authorityPatterns = new HashSet<>();
			}
			this.authorityPatterns.add(authorityPattern);
			return this;
		}

		@Override
		public RouteManager path(String path) {
			Objects.requireNonNull(path);
			if(this.paths == null) {
				this.paths = new HashSet<>();
			}
			this.paths.add(path);
			return this;
		}

		@Override
		public RouteManager pathPattern(URIPattern pathPattern) {
			Objects.requireNonNull(pathPattern);
			if(this.pathPatterns == null) {
				this.pathPatterns = new HashSet<>();
			}
			this.pathPatterns.add(pathPattern);
			return this;
		}

		@Override
		public RouteManager method(Method method) {
			Objects.requireNonNull(method);
			if(this.methods == null) {
				this.methods = new HashSet<>();
			}
			this.methods.add(method);
			return this;
		}

		@Override
		public RouteManager contentType(String mediaRange) {
			Objects.requireNonNull(mediaRange);
			if(this.contentTypes == null) {
				this.contentTypes = new HashSet<>();
			}
			this.contentTypes.add(mediaRange);
			return this;
		}

		@Override
		public RouteManager accept(String accept) {
			Objects.requireNonNull(accept);
			if(this.accepts == null) {
				this.accepts = new HashSet<>();
			}
			this.accepts.add(accept);
			return this;
		}

		@Override
		public RouteManager language(String language) {
			Objects.requireNonNull(language);
			if(this.languages == null) {
				this.languages = new HashSet<>();
			}
			this.languages.add(language);
			return this;
		}

		@Override
		public RouteManager headersMatchers(Map<String, HeadersRoute.HeaderMatcher> headersMatchers) {
			Objects.requireNonNull(headersMatchers);
			if(this.headersMatcherss == null) {
				this.headersMatcherss = new HashSet<>();
			}
			this.headersMatcherss.add(headersMatchers);
			return this;
		}

		@Override
		public RouteManager queryParametersMatchers(Map<String, QueryParametersRoute.ParameterMatcher> queryParametersMatchers) {
			Objects.requireNonNull(queryParametersMatchers);
			if(this.queryParametersMatcherss == null) {
				this.queryParametersMatcherss = new HashSet<>();
			}
			this.queryParametersMatcherss.add(queryParametersMatchers);
			return this;
		}

		private boolean matchesAuthority(Route route) {
			if(this.authorities != null) {
				if(route.getAuthority() != null) {
					if(!this.authorities.contains(route.getAuthority())) {
						return false;
					}
				}
				else if(route.getAuthorityPattern() != null) {
					if(this.authorities.stream().noneMatch(path -> route.getAuthorityPattern().matcher(path).matches())) {
						return false;
					}
				}
				else {
					return false;
				}
			}
			if(this.authorityPatterns != null) {
				if(route.getAuthority() != null) {
					return this.authorityPatterns.stream().anyMatch(pattern -> pattern.matcher(route.getAuthority()).matches());
				}
				else if(route.getPathPattern() != null) {
					return this.authorityPatterns.stream().anyMatch(pattern -> pattern.pattern().equals(route.getAuthorityPattern().pattern()));
				}
				else {
					return false;
				}
			}
			return true;
		}

		private boolean matchesPath(Route route) {
			if(this.paths != null) {
				if(route.getPath() != null) {
					if(!this.paths.contains(route.getPath())) {
						return false;
					}
				}
				else if(route.getPathPattern() != null) {
					if(this.paths.stream().noneMatch(path -> route.getPathPattern().matcher(path).matches())) {
						return false;
					}
				}
				else {
					return false;
				}
			}
			if(this.pathPatterns != null) {
				if(route.getPath() != null) {
					return this.pathPatterns.stream().anyMatch(pattern -> pattern.matcher(route.getPath()).matches());
				}
				else if(route.getPathPattern() != null) {
					return this.pathPatterns.stream().anyMatch(pattern -> pattern.includes(route.getPathPattern()) != URIPattern.Inclusion.DISJOINT);
				}
				else {
					return false;
				}
			}
			return true;
		}

		private boolean matchesMethod(Route route) {
			if(this.methods != null && !this.methods.isEmpty()) {
				return route.getMethod() != null && this.methods.contains(route.getMethod());
			}
			return true;
		}

		private boolean matchesContentType(Route route) {
			if(this.contentTypes != null && !this.contentTypes.isEmpty()) {
				return route.getContentType() != null && this.contentTypes.contains(route.getContentType());
			}
			return true;
		}

		private boolean matchesAccept(Route route) {
			if(this.accepts != null && !this.accepts.isEmpty()) {
				return route.getAccept() != null && this.accepts.contains(route.getAccept());
			}
			return true;
		}

		private boolean matchesLanguage(Route route) {
			if(this.languages != null && !this.languages.isEmpty()) {
				return route.getLanguage() != null && this.languages.contains(route.getLanguage());
			}
			return true;
		}

		private boolean matchesHeadersMatchers(Route route) {
			if(this.headersMatcherss != null && !this.headersMatcherss.isEmpty()) {
				return route.getHeadersMatchers() != null && this.headersMatcherss.contains(route.getHeadersMatchers());
			}
			return true;
		}

		private boolean matchesQueryParametersMatchers(Route route) {
			if(this.queryParametersMatcherss != null && !this.queryParametersMatcherss.isEmpty()) {
				return route.getQueryParameterMatchers() != null && this.queryParametersMatcherss.contains(route.getQueryParameterMatchers());
			}
			return true;
		}

		@Override
		protected Predicate<Route> routeMatcher() {
			return ((Predicate<Route>)this::matchesAuthority)
				.and(this::matchesPath)
				.and(this::matchesMethod)
				.and(this::matchesContentType)
				.and(this::matchesAccept)
				.and(this::matchesLanguage)
				.and(this::matchesHeadersMatchers)
				.and(this::matchesQueryParametersMatchers);
		}

		private Consumer<RouteExtractor> authorityRouteExtractor(Consumer<RouteExtractor> next) {
			return extractor -> {
				if(this.authorities != null && !this.authorities.isEmpty() || this.authorityPatterns != null && !this.authorityPatterns.isEmpty()) {
					if(this.authorities != null) {
						for(String authority : this.authorities) {
							next.accept(extractor.authority(authority));
						}
					}
					if(this.authorityPatterns != null) {
						for(Pattern authorityPattern : this.authorityPatterns) {
							next.accept(extractor.authorityPattern(authorityPattern));
						}
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		private Consumer<RouteExtractor> pathRouteExtractor(Consumer<RouteExtractor> next) {
			return extractor -> {
				if(this.paths != null && !this.paths.isEmpty() || this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
					if(this.paths != null) {
						for(String path : this.paths) {
							next.accept(extractor.path(path));
						}
					}
					if(this.pathPatterns != null) {
						for(URIPattern uriPattern : this.pathPatterns) {
							next.accept(extractor.pathPattern(uriPattern));
						}
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		private Consumer<RouteExtractor> methodRouteExtractor(Consumer<RouteExtractor> next) {
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

		private Consumer<RouteExtractor> contentTypeRouteExtractor(Consumer<RouteExtractor> next) {
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

		private Consumer<RouteExtractor> acceptRouteExtractor(Consumer<RouteExtractor> next) {
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

		private Consumer<RouteExtractor> languageRouteExtractor(Consumer<RouteExtractor> next) {
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

		private Consumer<RouteExtractor> headersMatchersRouteExtractor(Consumer<RouteExtractor> next) {
			return extractor -> {
				if(this.headersMatcherss != null && !this.headersMatcherss.isEmpty()) {
					for(Map<String, HeadersRoute.HeaderMatcher> headersMatchers : this.headersMatcherss) {
						next.accept(extractor.headersMatchers(headersMatchers));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		private Consumer<RouteExtractor> queryParametersMatchersRouteExtractor(Consumer<RouteExtractor> next) {
			return extractor -> {
				if(this.queryParametersMatcherss != null && !this.queryParametersMatcherss.isEmpty()) {
					for(Map<String, QueryParametersRoute.ParameterMatcher> queryParametersMatchers : this.queryParametersMatcherss) {
						next.accept(extractor.queryParametersMatchers(queryParametersMatchers));
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		@Override
		protected Function<Consumer<RouteExtractor>, Consumer<RouteExtractor>> routeExtractor() {
			return ((Function<Consumer<RouteExtractor>, Consumer<RouteExtractor>>)this::authorityRouteExtractor)
				.andThen(this::pathRouteExtractor)
				.andThen(this::methodRouteExtractor)
				.andThen(this::contentTypeRouteExtractor)
				.andThen(this::acceptRouteExtractor)
				.andThen(this::languageRouteExtractor)
				.andThen(this::headersMatchersRouteExtractor)
				.andThen(this::queryParametersMatchersRouteExtractor);
		}
	}

	/**
	 * <p>
	 * Internal route extractor.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	public static final class RouteExtractor
		extends
			AbstractRouteExtractor<AbstractHttpMetaServiceRoute<?, ?>, UnboundExchange<?>, Route, RouteManager, HttpMetaServiceRouter, RouteExtractor>
		implements
			AuthorityRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor>,
			PathRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor>,
			MethodRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor>,
			ContentRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor>,
			AcceptContentRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor>,
			AcceptLanguageRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor>,
			HeadersRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor>,
			QueryParametersRoute.Extractor<AbstractHttpMetaServiceRoute<?, ?>, Route, RouteExtractor> {

		private String authority;
		private Pattern authorityPattern;
		private String path;
		private URIPattern pathPattern;
		private Method method;
		private String contentType;
		private String accept;
		private String language;
		private Map<String, HeadersRoute.HeaderMatcher> headersMatchers;
		private Map<String, QueryParametersRoute.ParameterMatcher> queryParametersMatchers;

		/**
		 * <p>
		 * Creates a route extractor.
		 * </p>
		 *
		 * @param router the router
		 */
		private RouteExtractor(HttpMetaServiceRouter router) {
			super(router);
		}

		private RouteExtractor(RouteExtractor parent) {
			super(parent);
		}

		public String getAuthority() {
			if(this.authority != null) {
				return this.authority;
			}
			return this.parent != null ? this.parent.getAuthority() : null;
		}

		@Override
		public RouteExtractor authority(String authority) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.authority = authority;
			return childExtractor;
		}

		public Pattern getAuthorityPattern() {
			if(this.authorityPattern != null) {
				return this.authorityPattern;
			}
			return this.parent != null ? this.parent.getAuthorityPattern() : null;
		}

		@Override
		public RouteExtractor authorityPattern(Pattern authorityPattern) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.authorityPattern = authorityPattern;
			return childExtractor;
		}

		public String getPath() {
			if(this.path != null) {
				return this.path;
			}
			return this.parent != null ? this.parent.getPath() : null;
		}

		@Override
		public RouteExtractor path(String path) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.path = path;
			return childExtractor;
		}

		public URIPattern getPathPattern() {
			if(this.pathPattern != null) {
				return this.pathPattern;
			}
			return this.parent != null ? this.parent.getPathPattern() : null;
		}

		@Override
		public RouteExtractor pathPattern(URIPattern pathPattern) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.pathPattern = pathPattern;
			return childExtractor;
		}

		public Method getMethod() {
			if(this.method != null) {
				return this.method;
			}
			return this.parent != null ? this.parent.getMethod() : null;
		}

		@Override
		public RouteExtractor method(Method method) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.method = method;
			return childExtractor;
		}

		public String getContentType() {
			if(this.contentType != null) {
				return this.contentType;
			}
			return this.parent != null ? this.parent.getContentType() : null;
		}

		@Override
		public RouteExtractor contentType(String mediaRange) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.contentType = mediaRange;
			return childExtractor;
		}

		public String getAccept() {
			if(this.accept != null) {
				return this.accept;
			}
			return this.parent != null ? this.parent.getAccept() : null;
		}

		@Override
		public RouteExtractor accept(String accept) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.accept = accept;
			return childExtractor;
		}

		public String getLanguage() {
			if(this.language != null) {
				return this.language;
			}
			return this.parent != null ? this.parent.getLanguage() : null;
		}

		@Override
		public RouteExtractor language(String language) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.language = language;
			return childExtractor;
		}

		public Map<String, HeadersRoute.HeaderMatcher> getHeadersMatchers() {
			if(this.headersMatchers != null) {
				return this.headersMatchers;
			}
			return this.parent != null ? this.parent.getHeadersMatchers() : null;
		}

		@Override
		public RouteExtractor headersMatchers(Map<String, HeadersRoute.HeaderMatcher> headersMatchers) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.headersMatchers = headersMatchers;
			return childExtractor;
		}

		public Map<String, QueryParametersRoute.ParameterMatcher> getQueryParametersMatchers() {
			if(this.queryParametersMatchers != null) {
				return this.queryParametersMatchers;
			}
			return this.parent != null ? this.parent.getQueryParametersMatchers() : null;
		}

		@Override
		public RouteExtractor queryParametersMatchers(Map<String, QueryParametersRoute.ParameterMatcher> queryParametersMatchers) {
			RouteExtractor childExtractor = new RouteExtractor(this);
			childExtractor.queryParametersMatchers = queryParametersMatchers;
			return childExtractor;
		}

		@Override
		protected void populateRoute(Route route) {
			if(this.getAuthority() != null) {
				route.setAuthority(this.getAuthority());
			}
			else if(this.getAuthorityPattern() != null) {
				route.setAuthorityPattern(this.getAuthorityPattern());
			}

			if(this.getPath() != null) {
				route.setPath(this.getPath());
			}
			else if(this.getPathPattern() != null) {
				route.setPathPattern(this.getPathPattern());
			}
			route.setMethod(this.getMethod());
			route.setContentType(this.getContentType());
			route.setAccept(this.getAccept());
			route.setLanguage(this.getLanguage());
			route.setHeadersMatchers(this.getHeadersMatchers());
			route.setQueryParametersMatchers(this.getQueryParametersMatchers());
		}
	}
}
