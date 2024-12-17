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
package io.inverno.mod.http.base.router;

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink;
import io.inverno.mod.http.base.router.link.ContentRoutingLink;
import io.inverno.mod.http.base.router.link.MethodRoutingLink;
import io.inverno.mod.http.base.router.link.OutboundAcceptContentRoutingLink;
import io.inverno.mod.http.base.router.link.PathRoutingLink;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * <p>
 * A server router is a particular {@link Router} implementation where requests received from a client are routed to the best matching handler using the {@link Router#resolve(Object)} method.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ServerRouterTest {

	private static final HeaderCodec<? extends Headers.ContentType> CONTENT_TYPE_CODEC = new ContentTypeCodec();
	private static final HeaderCodec<? extends Headers.Accept> ACCEPT_CODEC = new AcceptCodec(true);
	private static final HeaderCodec<? extends Headers.AcceptLanguage> ACCEPT_LANGUAGE_CODEC = new AcceptLanguageCodec(true);

	/**
	 * Tests requests routing and provides use cases that help understanding the behaviour of the router:
	 * - routes are hierarchical which means a request is matched from top to bottom (path -> method -> contentType -> accept -> language)
	 *   - once a request has match a link, it can't switch to a parallel branch of the graph
	 *   - in practice if we define two routes one that matches the GET method and one that matches the application/json content type, when routing a GET request the GET route will be chosen even if
	 *   it results in a NotAcceptableException being thrown
	 *
	 * In a real-life web router, path and methods are usually made mandatory which makes things simpler
	 */
	@Test
	public void testResolve() {
		ServerRouter router = new ServerRouter();
		router
			.route().method(Method.POST).contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.DELETE).language("fr-FR").language("en-US").set("r2")
			.route().method(Method.POST).accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().method(Method.GET).accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		// The first request is routed to r2 because GET method takes precedence over application/json content type
		// next requests are properly routed to r1 because no route is defined for PUT or DELETE methods
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.GET, Map.of("content-type", MediaTypes.APPLICATION_JSON))));
		Assertions.assertEquals("r1", router.resolve(new ServerRequest("/test", Method.POST, Map.of("content-type", MediaTypes.APPLICATION_JSON))));
		Assertions.assertEquals("r1", router.resolve(new ServerRequest("/test", Method.POST, Map.of("content-type", MediaTypes.TEXT_HTML))));

		// Routes are defined for application/json and text/html but not for text/plain, request content media type is not supported
		Assertions.assertThrows(UnsupportedMediaTypeException.class , () -> router.resolve(new ServerRequest("/test", Method.POST, Map.of("content-type", MediaTypes.TEXT_PLAIN))));

		// Routes are defined to produce application/json and text/html, they are matched when request these media types are accepted by the request and when request path and method do not match other routes
		Assertions.assertEquals(Set.of(Method.GET, Method.DELETE, Method.POST), Assertions.assertThrows(MethodNotAllowedException.class, () -> router.resolve(new ServerRequest("/test", Method.PUT, Map.of("accept", MediaTypes.APPLICATION_JSON)))).getAllowedMethods());
		Assertions.assertEquals("r3", router.resolve(new ServerRequest("/test", Method.POST, Map.of("accept", MediaTypes.APPLICATION_JSON))));
		Assertions.assertEquals("r4", router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", MediaTypes.TEXT_HTML))));

		// Every request to /hello are routed to r5 regardless of the other request parameters
		Assertions.assertEquals("r5", router.resolve(new ServerRequest("/hello", Method.GET, Map.of("content-type", MediaTypes.APPLICATION_JSON))));
		Assertions.assertEquals("r5", router.resolve(new ServerRequest("/hello", Method.POST, Map.of("accept", MediaTypes.TEXT_HTML))));
		Assertions.assertEquals("r5", router.resolve(new ServerRequest("/hello", Method.DELETE, Map.of("language", "fr-FR"))));

		// Requests to /hello with a trailing '/' are also mapped to r5 note that /hello and /hello/ are two different routes matching is static here (i.e. string equals)
		Assertions.assertEquals("r5", router.resolve(new ServerRequest("/hello/", Method.GET, Map.of())));
		Assertions.assertEquals("r5", router.resolve(new ServerRequest("/hello/", Method.POST, Map.of())));
		Assertions.assertEquals("r5", router.resolve(new ServerRequest("/hello/", Method.DELETE, Map.of())));

		// path pattern matching with one path parameter, trailing slash is not matched and we fall back to other routes after path matching
		ServerRequest paramRequest = new ServerRequest("/hello/p1", Method.GET, Map.of());
		Assertions.assertEquals("r6", router.resolve(paramRequest));
		Assertions.assertEquals(Map.of("param1", "p1"), paramRequest.getPathParameters());
		paramRequest = new ServerRequest("/hello/p2", Method.POST, Map.of());
		Assertions.assertEquals("r6", router.resolve(paramRequest));
		Assertions.assertEquals(Map.of("param1", "p2"), paramRequest.getPathParameters());
		paramRequest = new ServerRequest("/hello/p3", Method.DELETE, Map.of());
		Assertions.assertEquals("r6", router.resolve(paramRequest));
		Assertions.assertEquals(Map.of("param1", "p3"), paramRequest.getPathParameters());

		// path pattern matching with two path parameters
		paramRequest = new ServerRequest("/hello/p1/a", Method.GET, Map.of());
		Assertions.assertEquals("r7", router.resolve(paramRequest));
		Assertions.assertEquals(Map.of("param1", "p1", "param2", "a"), paramRequest.getPathParameters());
		paramRequest = new ServerRequest("/hello/p2/ab", Method.POST, Map.of());
		Assertions.assertEquals("r7", router.resolve(paramRequest));
		Assertions.assertEquals(Map.of("param1", "p2", "param2", "ab"), paramRequest.getPathParameters());

		// The request path, the method are not matching defined routes, request has no content-type so content type link is ignored and the request is eventually routed to the first accept link (application/json)
		Assertions.assertEquals("r3", router.resolve(new ServerRequest("/hello/p3/56", Method.POST, Map.of())));

		// Trailing slash is not matched and we fall back to other routes after path matching if request path has a trailing slash, in that case r2 because POST method is matching r2 route
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/hello/p2/ab/", Method.DELETE, Map.of())));
	}

	@Test
	public void testResolve_method() {
		ServerRouter router = new ServerRouter();
		router
			.route().method(Method.GET).set("r1")
			.route().method(Method.POST).set("r2");

		Assertions.assertEquals("r1", router.resolve(new ServerRequest("/test", Method.GET, Map.of())));
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.POST, Map.of())));
		Assertions.assertEquals(Set.of(Method.GET, Method.POST), Assertions.assertThrows(MethodNotAllowedException.class, () -> router.resolve(new ServerRequest("/test", Method.DELETE, Map.of()))).getAllowedMethods());

		router
			.route().set("r3");

		Assertions.assertEquals("r1", router.resolve(new ServerRequest("/test", Method.GET, Map.of())));
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.POST, Map.of())));
		Assertions.assertEquals("r3", router.resolve(new ServerRequest("/test", Method.DELETE, Map.of())));
	}

	@Test
	public void testGetRoutes() {
		ServerRouter router = new ServerRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		Set<ServerRoute> routes = router.getRoutes();
		Assertions.assertEquals(
			Set.of(
				ServerRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build(),
				ServerRoute.builder(router).contentType(MediaTypes.TEXT_HTML).build(),
				ServerRoute.builder(router).method(Method.GET).language("fr-FR").build(),
				ServerRoute.builder(router).method(Method.GET).language("en-US").build(),
				ServerRoute.builder(router).method(Method.POST).language("fr-FR").build(),
				ServerRoute.builder(router).method(Method.POST).language("en-US").build(),
				ServerRoute.builder(router).accept(MediaTypes.APPLICATION_JSON).build(),
				ServerRoute.builder(router).accept(MediaTypes.TEXT_HTML).build(),
				ServerRoute.builder(router).path("/hello").build(),
				ServerRoute.builder(router).path("/hello/").build(),
				ServerRoute.builder(router).pathPattern(pathPattern("/hello/{param1}", false)).build(),
				ServerRoute.builder(router).pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).build()
			),
			routes
		);
	}

	@Test
	public void testFindRoutes() {
		ServerRouter router = new ServerRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		ServerRoute r1 = ServerRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build();
		ServerRoute r2 = ServerRoute.builder(router).method(Method.GET).language("fr-FR").build();
		ServerRoute r3 = ServerRoute.builder(router).method(Method.POST).language("fr-FR").build();

		Set<ServerRoute> routes = router.route().language("fr-FR").findRoutes();
		Assertions.assertEquals(Set.of(r2, r3), routes);

		routes = router.route().method(Method.GET).language("fr-FR").findRoutes();
		Assertions.assertEquals(Set.of(r2), routes);

		routes = router.route().method(Method.PUT).language("fr-FR").findRoutes();
		Assertions.assertEquals(Set.of(), routes);

		routes = router.route().contentType(MediaTypes.APPLICATION_JSON).findRoutes();
		Assertions.assertEquals(Set.of(r1), routes);
	}

	@Test
	public void testRouteRemove() {
		ServerRouter router = new ServerRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		Set<ServerRoute> routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		Assertions.assertTrue(routes.contains(ServerRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build()));

		router.route().contentType(MediaTypes.APPLICATION_JSON).findRoutes().stream().forEach(ServerRoute::remove);

		routes = router.getRoutes();
		Assertions.assertEquals(11, routes.size());
		Assertions.assertFalse(routes.contains(ServerRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build()));
	}

	@Test
	public void testRouteEnableDisable() {
		ServerRouter router = new ServerRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		Set<ServerRoute> routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		Assertions.assertTrue(routes.stream().noneMatch(ServerRoute::isDisabled));

		router.route().contentType(MediaTypes.APPLICATION_JSON).findRoutes().stream().forEach(ServerRoute::disable);

		routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());

		Set<ServerRoute> disabledRoutes = routes.stream().filter(ServerRoute::isDisabled).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(ServerRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build()), disabledRoutes);
	}

	@Test
	public void testBestMatchingResolutionDefaulting() {
		ServerRouter router = new ServerRouter();
		router
			.route().path("/test").set("r1")
			.route().path("/test").accept("application/json").set("r2");

		Assertions.assertEquals("r1", router.resolve(new ServerRequest("/test", Method.GET, Map.of())));
		Assertions.assertEquals(Set.of("application/json"), Assertions.assertThrows(NotAcceptableException.class, () -> router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "text/plain")))).getAcceptableMediaTypes());
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "application/json"))));

		ServerRoute defaultRoute = router.getRoutes().stream().filter(route -> route.getAccept() == null).findFirst().get();

		defaultRoute.disable();
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.GET, Map.of())));
		Assertions.assertEquals(Set.of("application/json"), Assertions.assertThrows(NotAcceptableException.class, () -> router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "text/plain")))).getAcceptableMediaTypes());
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "application/json"))));

		defaultRoute.enable();
		Assertions.assertEquals("r1", router.resolve(new ServerRequest("/test", Method.GET, Map.of())));
		Assertions.assertEquals(Set.of("application/json"), Assertions.assertThrows(NotAcceptableException.class, () -> router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "text/plain")))).getAcceptableMediaTypes());
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "application/json"))));

		defaultRoute.remove();
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.GET, Map.of())));
		Assertions.assertEquals(Set.of("application/json"), Assertions.assertThrows(NotAcceptableException.class, () -> router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "text/plain")))).getAcceptableMediaTypes());
		Assertions.assertEquals("r2", router.resolve(new ServerRequest("/test", Method.GET, Map.of("accept", "application/json"))));
	}

	private static URIPattern pathPattern(String path, boolean matchTrailingSlash) {
		return URIs.uri(path, URIs.RequestTargetForm.PATH, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern(matchTrailingSlash);
	}

	private static class ServerRouter extends AbstractRouter<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter, ServerRouteExtractor> {

		protected ServerRouter() {
			super(RoutingLink
				.<String, ServerRequest, ServerRoute, ServerRouteExtractor>link(next -> new PathRoutingLink<>(next) {

					@Override
					protected String getNormalizedPath(ServerRequest input) {
						return input.getPath();
					}

					@Override
					protected void setPathParameters(ServerRequest input, Map<String, String> parameters) {
						input.setPathParameters(parameters);
					}
				})
				.link(next -> new MethodRoutingLink<>(next) {

					@Override
					protected Method getMethod(ServerRequest input) {
						return input.getMethod();
					}
				})
				.link(next -> new ContentRoutingLink<>(next) {

					@Override
					protected Headers.ContentType getContentTypeHeader(ServerRequest input) {
						if(!input.getHeaders().containsKey(Headers.NAME_CONTENT_TYPE)) {
							return null;
						}
						return CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, input.getHeaders().get(Headers.NAME_CONTENT_TYPE));
					}
				})
				.link(next -> new OutboundAcceptContentRoutingLink<>(next) {

					@Override
					protected List<Headers.Accept> getAllAcceptHeaders(ServerRequest input) {
						return List.of(ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, input.getHeaders().getOrDefault("accept", "*/*")));
					}
				})
				.link(ign -> new AcceptLanguageRoutingLink<>() {

					@Override
					protected List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(ServerRequest input) {
						return List.of(ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, input.getHeaders().getOrDefault("accept-language", "*")));
					}
				})
			);
		}

		@Override
		protected ServerRoute createRoute(String resource, boolean disabled) {
			return new ServerRoute(this, resource, disabled);
		}

		@Override
		protected ServerRouteManager createRouteManager() {
			return new ServerRouteManager(this);
		}

		@Override
		protected ServerRouteExtractor createRouteExtractor() {
			return new ServerRouteExtractor(this);
		}
	}

	private static class ServerRouteManager extends AbstractRouteManager<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter, ServerRouteExtractor> implements
		PathRoute.Manager<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter>,
		MethodRoute.Manager<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter>,
		ContentRoute.Manager<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter>,
		AcceptContentRoute.Manager<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter>,
		AcceptLanguageRoute.Manager<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter> {

		private Set<String> paths;
		private Set<URIPattern> pathPatterns;
		private Set<Method> methods;
		private Set<String> contentTypes;
		private Set<String> accepts;
		private Set<String> languages;

		public ServerRouteManager(ServerRouter router) {
			super(router);
		}

		@Override
		public ServerRouteManager path(String path) {
			Objects.requireNonNull(path);
			if(this.paths == null) {
				this.paths = new HashSet<>();
			}
			this.paths.add(path);
			return this;
		}

		@Override
		public ServerRouteManager pathPattern(URIPattern pathPattern) {
			Objects.requireNonNull(pathPattern);
			if(this.pathPatterns == null) {
				this.pathPatterns = new HashSet<>();
			}
			this.pathPatterns.add(pathPattern);
			return this;
		}

		@Override
		public ServerRouteManager method(Method method) {
			Objects.requireNonNull(method);
			if(this.methods == null) {
				this.methods = new HashSet<>();
			}
			this.methods.add(method);
			return this;
		}

		@Override
		public ServerRouteManager contentType(String contentType) {
			Objects.requireNonNull(contentType);
			if(this.contentTypes == null) {
				this.contentTypes = new HashSet<>();
			}
			this.contentTypes.add(contentType);
			return this;
		}

		@Override
		public ServerRouteManager accept(String accept) {
			Objects.requireNonNull(accept);
			if(this.accepts == null) {
				this.accepts = new HashSet<>();
			}
			this.accepts.add(accept);
			return this;
		}

		@Override
		public ServerRouteManager language(String language) {
			Objects.requireNonNull(language);
			if(this.languages == null) {
				this.languages = new HashSet<>();
			}
			this.languages.add(language);
			return this;
		}

		protected final boolean matchesPath(ServerRoute route) {
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
					if(this.pathPatterns.stream().noneMatch(pattern -> pattern.matcher(route.getPath()).matches())) {
						return false;
					}
				}
				else if(route.getPathPattern() != null) {
					if(this.pathPatterns.stream().noneMatch(pattern -> pattern.includes(route.getPathPattern()) != URIPattern.Inclusion.DISJOINT)) {
						return false;
					}
				}
				else {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesMethod(ServerRoute route) {
			if(this.methods != null && !this.methods.isEmpty()) {
				if(route.getMethod() == null || !this.methods.contains(route.getMethod())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesContentType(ServerRoute route) {
			if(this.contentTypes != null && !this.contentTypes.isEmpty()) {
				if(route.getContentType() == null || !this.contentTypes.contains(route.getContentType())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesAccept(ServerRoute route) {
			if(this.accepts != null && !this.accepts.isEmpty()) {
				if(route.getAccept() == null || !this.accepts.contains(route.getAccept())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesLanguage(ServerRoute route) {
			if(this.languages != null && !this.languages.isEmpty()) {
				if(route.getLanguage() == null || !this.languages.contains(route.getLanguage())) {
					return false;
				}
			}
			return true;
		}

		@Override
		protected Predicate<ServerRoute> routeMatcher() {
			return ((Predicate<ServerRoute>)this::matchesPath)
				.and(this::matchesMethod)
				.and(this::matchesContentType)
				.and(this::matchesAccept)
				.and(this::matchesLanguage);
		}

		protected final Consumer<ServerRouteExtractor> pathRouteExtractor(Consumer<ServerRouteExtractor> next) {
			return extractor -> {
				if(this.paths != null && !this.paths.isEmpty() || this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
					if(this.paths != null) {
						for(String path : this.paths) {
							next.accept(extractor.path(path));
						}
					}
					if(this.pathPatterns != null) {
						for(URIPattern pathPattern : this.pathPatterns) {
							next.accept(extractor.pathPattern(pathPattern));
						}
					}
				}
				else {
					next.accept(extractor);
				}
			};
		}

		protected final Consumer<ServerRouteExtractor> methodRouteExtractor(Consumer<ServerRouteExtractor> next) {
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

		protected final Consumer<ServerRouteExtractor> contentRouteExtractor(Consumer<ServerRouteExtractor> next) {
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

		protected final Consumer<ServerRouteExtractor> acceptRouteExtractor(Consumer<ServerRouteExtractor> next) {
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

		protected final Consumer<ServerRouteExtractor> languageRouteExtractor(Consumer<ServerRouteExtractor> next) {
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
		protected Function<Consumer<ServerRouteExtractor>, Consumer<ServerRouteExtractor>> routeExtractor() {
			return ((Function<Consumer<ServerRouteExtractor>, Consumer<ServerRouteExtractor>>)this::pathRouteExtractor)
				.compose(this::methodRouteExtractor)
				.compose(this::contentRouteExtractor)
				.compose(this::acceptRouteExtractor)
				.compose(this::languageRouteExtractor);
		}
	}

	private static class ServerRouteExtractor extends AbstractRouteExtractor<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter, ServerRouteExtractor> implements
		PathRoute.Extractor<String, ServerRoute, ServerRouteExtractor>,
		MethodRoute.Extractor<String, ServerRoute, ServerRouteExtractor>,
		ContentRoute.Extractor<String, ServerRoute, ServerRouteExtractor>,
		AcceptContentRoute.Extractor<String, ServerRoute, ServerRouteExtractor>,
		AcceptLanguageRoute.Extractor<String, ServerRoute, ServerRouteExtractor> {

		private String path;
		private URIPattern pathPattern;
		private Method method;
		private String contentType;
		private String accept;
		private String language;

		public ServerRouteExtractor(ServerRouter router) {
			super(router);
		}

		public ServerRouteExtractor(ServerRouteExtractor parent) {
			super(parent);
		}

		private String getPath() {
			if(this.path != null) {
				return this.path;
			}
			return this.parent != null ? this.parent.getPath() : null;
		}

		@Override
		public ServerRouteExtractor path(String path) {
			ServerRouteExtractor childExtractor = new ServerRouteExtractor(this);
			childExtractor.path = path;
			return childExtractor;
		}

		private URIPattern getPathPattern() {
			if(this.pathPattern != null) {
				return this.pathPattern;
			}
			return this.parent != null ? this.parent.getPathPattern() : null;
		}

		@Override
		public ServerRouteExtractor pathPattern(URIPattern pathPattern) {
			ServerRouteExtractor childExtractor = new ServerRouteExtractor(this);
			childExtractor.pathPattern = pathPattern;
			return childExtractor;
		}

		private Method getMethod() {
			if(this.method != null) {
				return this.method;
			}
			return this.parent != null ? this.parent.getMethod() : null;
		}

		@Override
		public ServerRouteExtractor method(Method method) {
			ServerRouteExtractor childExtractor = new ServerRouteExtractor(this);
			childExtractor.method = method;
			return childExtractor;
		}

		private String getContentType() {
			if(this.contentType != null) {
				return this.contentType;
			}
			return this.parent != null ? this.parent.getContentType() : null;
		}

		@Override
		public ServerRouteExtractor contentType(String contentType) {
			ServerRouteExtractor childExtractor = new ServerRouteExtractor(this);
			childExtractor.contentType = contentType;
			return childExtractor;
		}

		private String getAccept() {
			if(this.accept != null) {
				return this.accept;
			}
			return this.parent != null ? this.parent.getAccept() : null;
		}

		@Override
		public ServerRouteExtractor accept(String accept) {
			ServerRouteExtractor childExtractor = new ServerRouteExtractor(this);
			childExtractor.accept = accept;
			return childExtractor;
		}

		private String getLanguage() {
			if(this.language != null) {
				return this.language;
			}
			return this.parent != null ? this.parent.getLanguage() : null;
		}

		@Override
		public ServerRouteExtractor language(String language) {
			ServerRouteExtractor childExtractor = new ServerRouteExtractor(this);
			childExtractor.language = language;
			return childExtractor;
		}

		@Override
		protected void populateRoute(ServerRoute route) {
			if(this.getPath() != null) {
				route.setPath(this.getPath());
			}
			if(this.getPathPattern() != null) {
				route.setPathPattern(this.getPathPattern());
			}
			route.setMethod(this.getMethod());
			route.setContentType(this.getContentType());
			route.setAccept(this.getAccept());
			route.setLanguage(this.getLanguage());
		}
	}

	private static class ServerRoute extends AbstractRoute<String, ServerRequest, ServerRoute, ServerRouteManager, ServerRouter, ServerRouteExtractor> implements PathRoute<String>, MethodRoute<String>, ContentRoute<String>, AcceptContentRoute<String>, AcceptLanguageRoute<String> {

		private String path;
		private URIPattern pathPattern;
		private Method method;
		private String contentType;
		private String accept;
		private String language;

		public ServerRoute(ServerRouter router, String resource, boolean disabled) {
			super(router, resource, disabled);
		}

		public void setPath(String path) {
			this.path = path;
			this.pathPattern = null;
		}

		@Override
		public String getPath() {
			return this.path;
		}

		public void setPathPattern(URIPattern pathPattern) {
			this.path = null;
			this.pathPattern = pathPattern;
		}

		@Override
		public URIPattern getPathPattern() {
			return this.pathPattern;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		public void setMethod(Method method) {
			this.method = method;
		}

		@Override
		public Method getMethod() {
			return this.method;
		}

		public void setAccept(String accept) {
			this.accept = accept;
		}

		@Override
		public String getAccept() {
			return this.accept;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		@Override
		public String getLanguage() {
			return this.language;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			if (!super.equals(o)) return false;
			ServerRoute that = (ServerRoute) o;
			return Objects.equals(path, that.path) && Objects.equals(pathPattern, that.pathPattern) && method == that.method && Objects.equals(contentType, that.contentType) && Objects.equals(accept, that.accept) && Objects.equals(language, that.language);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), path, pathPattern, method, contentType, accept, language);
		}

		@Override
		public String toString() {
			return "ServerRoute{" +
				"path='" + path + '\'' +
				", pathPattern=" + pathPattern +
				", method=" + method +
				", contentType='" + contentType + '\'' +
				", accept='" + accept + '\'' +
				", language='" + language + '\'' +
				'}';
		}

		public static Builder builder(ServerRouter router) {
			return builder(router, null, false);
		}

		public static Builder builder(ServerRouter router, String resource, boolean disabled) {
			return new Builder(router, resource, disabled);
		}

		public static class Builder {

			private final ServerRouter router;
			private final String resource;
			private final boolean disabled;

			private String path;
			private URIPattern pathPattern;
			private Method method;
			private String contentType;
			private String accept;
			private String language;

			private Builder(ServerRouter router, String resource, boolean disabled) {
				this.router = router;
				this.resource = resource;
				this.disabled = disabled;
			}

			public Builder path(String path) {
				this.path = path;
				this.pathPattern = null;
				return this;
			}

			public Builder pathPattern(URIPattern pathPattern) {
				this.path = null;
				this.pathPattern = pathPattern;
				return this;
			}

			public Builder method(Method method) {
				this.method = method;
				return this;
			}

			public Builder contentType(String contentType) {
				this.contentType = contentType;
				return this;
			}

			public Builder accept(String accept) {
				this.accept = accept;
				return this;
			}

			public Builder language(String language) {
				this.language = language;
				return this;
			}

			public ServerRoute build() {
				ServerRoute route = new ServerRoute(this.router, this.resource, this.disabled);
				if(this.path != null) {
					route.setPath(this.path);
				}
				if(this.pathPattern != null) {
					route.setPathPattern(this.pathPattern);
				}
				route.setMethod(this.method);
				route.setContentType(this.contentType);
				route.setAccept(this.accept);
				route.setLanguage(this.language);

				return route;
			}
		}
	}

	private static class ServerRequest {

		private final String path;
		private final Method method;
		private final Map<String, String> headers;

		private Map<String, String> pathParameters;

		public ServerRequest(String path, Method method, Map<String, String> headers) {
			this.path = path;
			this.method = method;
			this.headers = headers;
		}

		public String getPath() {
			return path;
		}

		public Method getMethod() {
			return method;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public Map<String, String> getPathParameters() {
			return pathParameters;
		}

		public void setPathParameters(Map<String, String> pathParameters) {
			this.pathParameters = pathParameters;
		}
	}
}
