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
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink;
import io.inverno.mod.http.base.router.link.ContentRoutingLink;
import io.inverno.mod.http.base.router.link.InboundAcceptContentRoutingLink;
import io.inverno.mod.http.base.router.link.MethodRoutingLink;
import io.inverno.mod.http.base.router.link.PathRoutingLink;
import java.util.ArrayList;
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
 * A client router is a particular {@link Router} implementation used to resolve the interceptors to apply when sending a request to a server using the {@link Router#resolveAll(Object)} method.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class ClientRouterTest {

	private static final HeaderCodec<? extends Headers.ContentType> CONTENT_TYPE_CODEC = new ContentTypeCodec();
	private static final HeaderCodec<? extends Headers.Accept> ACCEPT_CODEC = new AcceptCodec(true);
	private static final HeaderCodec<? extends Headers.AcceptLanguage> ACCEPT_LANGUAGE_CODEC = new AcceptLanguageCodec(true);

	@Test
	public void testResolveAll() {
		ClientRouter router = new ClientRouter();
		router
			.route().set("r0")
			.route().contentType("application/json").set("r1")
			.route().accept("text/html").set("r2")
			.route().method(Method.GET).set("r3")
			.route().language("fr-FR").set("r4")
			.route().method(Method.PUT).language("fr-FR").set("r5")
			.route().method(Method.PUT).language("*").set("r6")
			.route().path("/hello").set("r7")
			.route().path("/hello").contentType("application/json").set("r8")
			.route().path("/hello").method(Method.GET).contentType("application/json").set("r9")
			.route().path("/hello").accept("application/json").set("r10")
			.route().path("/hello").method(Method.POST).language("fr-FR").set("r11");

		// All routes matching the request are returned from the best matching r3 to the most general r0
		// Best matching is determined by the rule of precedence: path > method > contentType > accept > language
		Assertions.assertEquals(List.of("r3", "r1", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/test", Method.GET, Map.of("content-type", "application/json")))));
		Assertions.assertEquals(List.of("r1", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/test", Method.POST, Map.of("content-type", "application/json")))));
		Assertions.assertEquals(List.of("r1", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/test", Method.POST, Map.of("content-type", "application/json", "accept", "application/json")))));
		Assertions.assertEquals(List.of("r1", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/test", Method.POST, Map.of("content-type", "application/json", "accept", "application/json", "accept-language", "fr")))));
		Assertions.assertEquals(List.of("r1", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/test", Method.DELETE, Map.of("content-type", "application/json", "accept", "application/json", "accept-language", "en-US")))));
		Assertions.assertEquals(List.of("r5", "r6", "r1", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/test", Method.PUT, Map.of("content-type", "application/json", "accept", "application/json", "accept-language", "fr")))));
		Assertions.assertEquals(List.of("r6", "r1", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/test", Method.PUT, Map.of("content-type", "application/json", "accept", "application/json", "accept-language", "en-US")))));

		Assertions.assertEquals(List.of("r9", "r8", "r10", "r7", "r3", "r1", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.GET, Map.of()))));
		Assertions.assertEquals(List.of("r11", "r8", "r10", "r7", "r1", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.POST, Map.of()))));
		Assertions.assertEquals(List.of("r8", "r10", "r7", "r1", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.DELETE, Map.of()))));
		Assertions.assertEquals(List.of("r8", "r10", "r7", "r5", "r6", "r1", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.PUT, Map.of()))));

		Assertions.assertEquals(List.of("r9", "r8", "r10", "r7", "r3", "r1", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.GET, Map.of("content-type", "application/json")))));
		Assertions.assertEquals(List.of("r10", "r7", "r3", "r2", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.GET, Map.of("content-type", "application/xml")))));
		Assertions.assertEquals(List.of("r7", "r3", "r4", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.GET, Map.of("content-type", "application/xml", "accept", "application/xml")))));
		Assertions.assertEquals(List.of("r7", "r3", "r0"), new ArrayList<>(router.resolveAll(new ClientRequest("/hello", Method.GET, Map.of("content-type", "application/xml", "accept", "application/xml", "accept-language", "en-US")))));
	}

	@Test
	public void testGetRoutes() {
		ClientRouter router = new ClientRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		Set<ClientRoute> routes = router.getRoutes();
		Assertions.assertEquals(
			Set.of(
				ClientRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build(),
				ClientRoute.builder(router).contentType(MediaTypes.TEXT_HTML).build(),
				ClientRoute.builder(router).method(Method.GET).language("fr-FR").build(),
				ClientRoute.builder(router).method(Method.GET).language("en-US").build(),
				ClientRoute.builder(router).method(Method.POST).language("fr-FR").build(),
				ClientRoute.builder(router).method(Method.POST).language("en-US").build(),
				ClientRoute.builder(router).accept(MediaTypes.APPLICATION_JSON).build(),
				ClientRoute.builder(router).accept(MediaTypes.TEXT_HTML).build(),
				ClientRoute.builder(router).path("/hello").build(),
				ClientRoute.builder(router).path("/hello/").build(),
				ClientRoute.builder(router).pathPattern(pathPattern("/hello/{param1}", false)).build(),
				ClientRoute.builder(router).pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).build()
			),
			routes
		);
	}

	@Test
	public void testFindRoutes() {
		ClientRouter router = new ClientRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		ClientRoute r1 = ClientRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build();
		ClientRoute r2 = ClientRoute.builder(router).method(Method.GET).language("fr-FR").build();
		ClientRoute r3 = ClientRoute.builder(router).method(Method.POST).language("fr-FR").build();

		Set<ClientRoute> routes = router.route().language("fr-FR").findRoutes();
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
		ClientRouter router = new ClientRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		Set<ClientRoute> routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		Assertions.assertTrue(routes.contains(ClientRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build()));

		router.route().contentType(MediaTypes.APPLICATION_JSON).findRoutes().stream().forEach(ClientRoute::remove);

		routes = router.getRoutes();
		Assertions.assertEquals(11, routes.size());
		Assertions.assertFalse(routes.contains(ClientRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build()));
	}

	@Test
	public void testRouteEnableDisable() {
		ClientRouter router = new ClientRouter();
		router
			.route().contentType(MediaTypes.APPLICATION_JSON).contentType(MediaTypes.TEXT_HTML).set("r1")
			.route().method(Method.GET).method(Method.POST).language("fr-FR").language("en-US").set("r2")
			.route().accept(MediaTypes.APPLICATION_JSON).set("r3")
			.route().accept(MediaTypes.TEXT_HTML).set("r4")
			.route().path("/hello").path("/hello/").set("r5")
			.route().pathPattern(pathPattern("/hello/{param1}", false)).set("r6")
			.route().pathPattern(pathPattern("/hello/{param1}/{param2:[a-b]*}", false)).set("r7");

		Set<ClientRoute> routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());
		Assertions.assertTrue(routes.stream().noneMatch(ClientRoute::isDisabled));

		router.route().contentType(MediaTypes.APPLICATION_JSON).findRoutes().stream().forEach(ClientRoute::disable);

		routes = router.getRoutes();
		Assertions.assertEquals(12, routes.size());

		Set<ClientRoute> disabledRoutes = routes.stream().filter(ClientRoute::isDisabled).collect(Collectors.toSet());
		Assertions.assertEquals(Set.of(ClientRoute.builder(router).contentType(MediaTypes.APPLICATION_JSON).build()), disabledRoutes);
	}

	private static URIPattern pathPattern(String path, boolean matchTrailingSlash) {
		return URIs.uri(path, URIs.RequestTargetForm.PATH, false, URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED, URIs.Option.PATH_PATTERN).buildPathPattern(matchTrailingSlash);
	}

	private static class ClientRouter extends AbstractRouter<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter, ClientRouteExtractor> {

		public ClientRouter() {
			super(RoutingLink
				.<String, ClientRequest, ClientRoute, ClientRouteExtractor>link(next -> new PathRoutingLink<>(next) {

					@Override
					protected String getNormalizedPath(ClientRequest input) {
						return input.getPath();
					}

					@Override
					protected void setPathParameters(ClientRequest input, Map<String, String> parameters) {
						input.setPathParameters(parameters);
					}
				})
				.link(next -> new MethodRoutingLink<>(next) {

					@Override
					protected Method getMethod(ClientRequest input) {
						return input.getMethod();
					}
				})
				.link(next -> new ContentRoutingLink<>(next) {

					@Override
					protected Headers.ContentType getContentTypeHeader(ClientRequest input) {
						if(!input.getHeaders().containsKey(Headers.NAME_CONTENT_TYPE)) {
							return null;
						}
						return CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, input.getHeaders().get(Headers.NAME_CONTENT_TYPE));
					}
				})
				.link(next -> new InboundAcceptContentRoutingLink<>(next) {

					@Override
					protected List<Headers.Accept> getAllAcceptHeaders(ClientRequest input) {
						return List.of(ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, input.getHeaders().getOrDefault("accept", "*/*")));
					}
				})
				.link(ign -> new AcceptLanguageRoutingLink<>() {

					@Override
					protected List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(ClientRequest input) {
						return List.of(ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, input.getHeaders().getOrDefault("accept-language", "*")));
					}
				})
			);
		}

		@Override
		protected ClientRoute createRoute(String resource, boolean disabled) {
			return new ClientRoute(this, resource, disabled);
		}

		@Override
		protected ClientRouteManager createRouteManager() {
			return new ClientRouteManager(this);
		}

		@Override
		protected ClientRouteExtractor createRouteExtractor() {
			return new ClientRouteExtractor(this);
		}
	}

	private static class ClientRouteManager extends AbstractRouteManager<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter, ClientRouteExtractor> implements
		PathRoute.Manager<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter>,
		MethodRoute.Manager<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter>,
		ContentRoute.Manager<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter>,
		AcceptContentRoute.Manager<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter>,
		AcceptLanguageRoute.Manager<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter> {

		private Set<String> paths;
		private Set<URIPattern> pathPatterns;
		private Set<Method> methods;
		private Set<String> contentTypes;
		private Set<String> accepts;
		private Set<String> languages;

		public ClientRouteManager(ClientRouter router) {
			super(router);
		}

		@Override
		public ClientRouteManager path(String path) {
			Objects.requireNonNull(path);
			if(this.paths == null) {
				this.paths = new HashSet<>();
			}
			this.paths.add(path);
			return this;
		}

		@Override
		public ClientRouteManager pathPattern(URIPattern pathPattern) {
			Objects.requireNonNull(pathPattern);
			if(this.pathPatterns == null) {
				this.pathPatterns = new HashSet<>();
			}
			this.pathPatterns.add(pathPattern);
			return this;
		}

		@Override
		public ClientRouteManager method(Method method) {
			Objects.requireNonNull(method);
			if(this.methods == null) {
				this.methods = new HashSet<>();
			}
			this.methods.add(method);
			return this;
		}

		@Override
		public ClientRouteManager contentType(String contentType) {
			Objects.requireNonNull(contentType);
			if(this.contentTypes == null) {
				this.contentTypes = new HashSet<>();
			}
			this.contentTypes.add(contentType);
			return this;
		}

		@Override
		public ClientRouteManager accept(String accept) {
			Objects.requireNonNull(accept);
			if(this.accepts == null) {
				this.accepts = new HashSet<>();
			}
			this.accepts.add(accept);
			return this;
		}

		@Override
		public ClientRouteManager language(String language) {
			Objects.requireNonNull(language);
			if(this.languages == null) {
				this.languages = new HashSet<>();
			}
			this.languages.add(language);
			return this;
		}

		protected final boolean matchesPath(ClientRoute route) {
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

		protected final boolean matchesMethod(ClientRoute route) {
			if(this.methods != null && !this.methods.isEmpty()) {
				if(route.getMethod() == null || !this.methods.contains(route.getMethod())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesContentType(ClientRoute route) {
			if(this.contentTypes != null && !this.contentTypes.isEmpty()) {
				if(route.getContentType() == null || !this.contentTypes.contains(route.getContentType())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesAccept(ClientRoute route) {
			if(this.accepts != null && !this.accepts.isEmpty()) {
				if(route.getAccept() == null || !this.accepts.contains(route.getAccept())) {
					return false;
				}
			}
			return true;
		}

		protected final boolean matchesLanguage(ClientRoute route) {
			if(this.languages != null && !this.languages.isEmpty()) {
				if(route.getLanguage() == null || !this.languages.contains(route.getLanguage())) {
					return false;
				}
			}
			return true;
		}

		@Override
		protected Predicate<ClientRoute> routeMatcher() {
			return ((Predicate<ClientRoute>)this::matchesPath)
				.and(this::matchesMethod)
				.and(this::matchesContentType)
				.and(this::matchesAccept)
				.and(this::matchesLanguage);
		}

		protected final Consumer<ClientRouteExtractor> pathRouteExtractor(Consumer<ClientRouteExtractor> next) {
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

		protected final Consumer<ClientRouteExtractor> methodRouteExtractor(Consumer<ClientRouteExtractor> next) {
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

		protected final Consumer<ClientRouteExtractor> contentTypeRouteExtractor(Consumer<ClientRouteExtractor> next) {
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

		protected final Consumer<ClientRouteExtractor> acceptRouteExtractor(Consumer<ClientRouteExtractor> next) {
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

		protected final Consumer<ClientRouteExtractor> languageRouteExtractor(Consumer<ClientRouteExtractor> next) {
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
		protected Function<Consumer<ClientRouteExtractor>, Consumer<ClientRouteExtractor>> routeExtractor() {
			return ((Function<Consumer<ClientRouteExtractor>, Consumer<ClientRouteExtractor>>)this::pathRouteExtractor)
				.compose(this::methodRouteExtractor)
				.compose(this::contentTypeRouteExtractor)
				.compose(this::acceptRouteExtractor)
				.compose(this::languageRouteExtractor);
		}
	}

	private static class ClientRouteExtractor extends AbstractRouteExtractor<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter, ClientRouteExtractor> implements
		PathRoute.Extractor<String, ClientRoute, ClientRouteExtractor>,
		MethodRoute.Extractor<String, ClientRoute, ClientRouteExtractor>,
		ContentRoute.Extractor<String, ClientRoute, ClientRouteExtractor>,
		AcceptContentRoute.Extractor<String, ClientRoute, ClientRouteExtractor>,
		AcceptLanguageRoute.Extractor<String, ClientRoute, ClientRouteExtractor> {

		private String path;
		private URIPattern pathPattern;
		private Method method;
		private String contentType;
		private String accept;
		private String language;

		public ClientRouteExtractor(ClientRouter router) {
			super(router);
		}

		public ClientRouteExtractor(ClientRouteExtractor parent) {
			super(parent);
		}

		private String getPath() {
			if(this.path != null) {
				return this.path;
			}
			return this.parent != null ? this.parent.getPath() : null;
		}

		@Override
		public ClientRouteExtractor path(String path) {
			ClientRouteExtractor childExtractor = new ClientRouteExtractor(this);
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
		public ClientRouteExtractor pathPattern(URIPattern pathPattern) {
			ClientRouteExtractor childExtractor = new ClientRouteExtractor(this);
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
		public ClientRouteExtractor method(Method method) {
			ClientRouteExtractor childExtractor = new ClientRouteExtractor(this);
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
		public ClientRouteExtractor contentType(String contentType) {
			ClientRouteExtractor childExtractor = new ClientRouteExtractor(this);
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
		public ClientRouteExtractor accept(String accept) {
			ClientRouteExtractor childExtractor = new ClientRouteExtractor(this);
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
		public ClientRouteExtractor language(String language) {
			ClientRouteExtractor childExtractor = new ClientRouteExtractor(this);
			childExtractor.language = language;
			return childExtractor;
		}

		@Override
		protected void populateRoute(ClientRoute route) {
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

	private static class ClientRoute extends AbstractRoute<String, ClientRequest, ClientRoute, ClientRouteManager, ClientRouter, ClientRouteExtractor> implements PathRoute<String>, MethodRoute<String>, ContentRoute<String>, AcceptContentRoute<String>, AcceptLanguageRoute<String> {

		private String path;
		private URIPattern pathPattern;
		private Method method;
		private String contentType;
		private String accept;
		private String language;

		public ClientRoute(ClientRouter router, String resource, boolean disabled) {
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
			ClientRoute that = (ClientRoute) o;
			return Objects.equals(path, that.path) && Objects.equals(pathPattern, that.pathPattern) && method == that.method && Objects.equals(contentType, that.contentType) && Objects.equals(accept, that.accept) && Objects.equals(language, that.language);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), path, pathPattern, method, contentType, accept, language);
		}

		@Override
		public String toString() {
			return "ClientRoute{" +
				"path='" + path + '\'' +
				", pathPattern=" + pathPattern +
				", method=" + method +
				", contentType='" + contentType + '\'' +
				", accept='" + accept + '\'' +
				", language='" + language + '\'' +
				'}';
		}

		public static Builder builder(ClientRouter router) {
			return builder(router, null, false);
		}

		public static Builder builder(ClientRouter router, String resource, boolean disabled) {
			return new Builder(router, resource, disabled);
		}

		public static class Builder {

			private final ClientRouter router;
			private final String resource;
			private final boolean disabled;

			private String path;
			private URIPattern pathPattern;
			private Method method;
			private String contentType;
			private String accept;
			private String language;

			private Builder(ClientRouter router, String resource, boolean disabled) {
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

			public ClientRoute build() {
				ClientRoute route = new ClientRoute(this.router, this.resource, this.disabled);
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

	private static class ClientRequest {

		private final String path;
		private final Method method;
		private final Map<String, String> headers;

		private Map<String, String> pathParameters;

		public ClientRequest(String path, Method method, Map<String, String> headers) {
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
