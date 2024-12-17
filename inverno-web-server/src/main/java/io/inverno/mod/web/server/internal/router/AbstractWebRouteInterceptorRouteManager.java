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

import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.PathRoute;
import io.inverno.mod.http.base.router.RouteManager;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * Base internal interceptor route manager.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 * @param <C> the internal route type
 * @param <D> the internal interceptor route type
 * @param <E> the internal interceptor route manager type
 * @param <F> the internal interceptor router type
 * @param <G> the internal interceptor route matcher
 */
abstract class AbstractWebRouteInterceptorRouteManager<
		A extends ExchangeContext,
		B extends Exchange<A>,
		C extends AbstractWebRoute<?, ?, ?, ?, ?, ?>,
		D extends AbstractWebRouteInterceptorRoute<A, B, ?>,
		E extends AbstractWebRouteInterceptorRouteManager<A, B, C, D, E, F, G>,
		F extends AbstractWebRouteInterceptorRouter<A, B, C, D, E, F, G>,
		G extends AbstractWebRouteInterceptorRouteMatcher<A, B, C, D>
	>
	implements
		RouteManager<ExchangeInterceptor<A, B>, C, D, E, F>,
		PathRoute.Manager<ExchangeInterceptor<A, B>, C, D, E, F>,
		ContentRoute.Manager<ExchangeInterceptor<A, B>, C, D, E, F>,
		AcceptContentRoute.Manager<ExchangeInterceptor<A, B>, C, D, E, F>,
		AcceptLanguageRoute.Manager<ExchangeInterceptor<A, B>, C, D, E, F> {

	/**
	 * The internal interceptor router.
	 */
	protected final F router;

	private Set<String> paths;
	private Set<URIPattern> pathPatterns;
	private Set<String> contentTypes;
	private Set<String> accepts;
	private Set<String> languages;

	/**
	 * <p>
	 * Creates an internal interceptor route manager.
	 * </p>
	 *
	 * @param router an internal interceptor router
	 */
	protected AbstractWebRouteInterceptorRouteManager(F router) {
		this.router = router;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E path(String path) {
		Objects.requireNonNull(path);
		if(this.paths == null) {
			this.paths = new HashSet<>();
		}
		this.paths.add(path);
		return (E)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E pathPattern(URIPattern pathPattern) {
		Objects.requireNonNull(pathPattern);
		if(this.pathPatterns == null) {
			this.pathPatterns = new HashSet<>();
		}
		this.pathPatterns.add(pathPattern);
		return (E)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E contentType(String contentType) {
		Objects.requireNonNull(contentType);
		if(this.contentTypes == null) {
			this.contentTypes = new HashSet<>();
		}
		this.contentTypes.add(contentType);
		return (E)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E accept(String accept) {
		Objects.requireNonNull(accept);
		if(this.accepts == null) {
			this.accepts = new HashSet<>();
		}
		this.accepts.add(accept);
		return (E)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public E language(String language) {
		Objects.requireNonNull(language);
		if(this.languages == null) {
			this.languages = new HashSet<>();
		}
		this.languages.add(language);
		return (E)this;
	}

	@Override
	public F set(ExchangeInterceptor<A, B> resource) {
		List<D> routes = new ArrayList<>();
		this.routeExtractor()
			.apply(route -> {
				route.set(resource);
				routes.add(route);
			})
			.accept(this.router.createRoute(null));
		this.router.addRoutes(routes);
		return this.router;
	}

	@Override
	public final F set(Supplier<ExchangeInterceptor<A, B>> resourceFactory, Consumer<D> routeConfigurer) {
		List<D> routes = new ArrayList<>();
		this.routeExtractor()
			.apply(route -> {
				route.set(resourceFactory.get());
				routes.add(route);
			})
			.accept(this.router.createRoute(null));
		this.router.addRoutes(routes);
		routes.forEach(routeConfigurer);
		return this.router;
	}

	/**
	 * <p>
	 * Returns the path route extractor.
	 * </p>
	 *
	 * @param next the next route extractor
	 *
	 * @return the path route extractor
	 */
	protected final Consumer<D> pathRouteExtractor(Consumer<D> next) {
		return route -> {
			boolean hasPath = false;
			if(this.paths != null && !this.paths.isEmpty()) {
				for(String path : this.paths) {
					D childRoute = this.router.createRoute(route);
					childRoute.setPath(path);
					next.accept(childRoute);
				}
				hasPath = true;
			}
			if(this.pathPatterns != null && !this.pathPatterns.isEmpty()) {
				for(URIPattern pathPattern : this.pathPatterns) {
					D childRoute = this.router.createRoute(route);
					childRoute.setPathPattern(pathPattern);
					next.accept(childRoute);
				}
				hasPath = true;
			}
			if(!hasPath) {
				next.accept(route);
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
	protected final Consumer<D> contentTypeRouteExtractor(Consumer<D> next) {
		return route -> {
			if (this.contentTypes != null && !this.contentTypes.isEmpty()) {
				for (String contentType : this.contentTypes) {
					D childRoute = this.router.createRoute(route);
					childRoute.setContentType(contentType);
					next.accept(childRoute);
				}
			}
			else {
				next.accept(route);
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
	protected final Consumer<D> acceptRouteExtractor(Consumer<D> next) {
		return route -> {
			if (this.accepts != null && !this.accepts.isEmpty()) {
				for (String accept : this.accepts) {
					D childRoute = this.router.createRoute(route);
					childRoute.setAccept(accept);
					next.accept(childRoute);
				}
			}
			else {
				next.accept(route);
			}
		};
	}

	/**
	 * <p>
	 * Returns the accept language route extractor.
	 * </p>
	 *
	 * @param next the next route extractor
	 *
	 * @return the accept language route extractor
	 */
	protected final Consumer<D> languageRouteExtractor(Consumer<D> next) {
		return route -> {
			if (this.languages != null && !this.languages.isEmpty()) {
				for (String language : this.languages) {
					D childRoute = this.router.createRoute(route);
					childRoute.setLanguage(language);
					next.accept(childRoute);
				}
			}
			else {
				next.accept(route);
			}
		};
	}

	/**
	 * <p>
	 * Returns the interceptor route extractor.
	 * </p>
	 *
	 * @return the interceptor route extractor
	 */
	protected Function<Consumer<D>, Consumer<D>> routeExtractor() {
		return ((Function<Consumer<D>, Consumer<D>>)this::pathRouteExtractor)
			.compose(this::contentTypeRouteExtractor)
			.compose(this::acceptRouteExtractor)
			.compose(this::languageRouteExtractor);
	}

	@Override
	public F enable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public F disable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public F remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<D> findRoutes() {
		throw new UnsupportedOperationException();
	}
}
