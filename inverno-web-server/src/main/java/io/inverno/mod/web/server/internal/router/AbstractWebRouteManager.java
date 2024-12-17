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
import io.inverno.mod.http.base.router.AbstractRouteManager;
import io.inverno.mod.http.base.router.AbstractRouter;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.PathRoute;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>
 * Base internal route manager.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the resource type
 * @param <B> the resolver input type
 * @param <C> the internal route type
 * @param <D> the internal route manager type
 * @param <E> the internal router type
 * @param <F> the internal route extractor type
 */
abstract class AbstractWebRouteManager<A, B, C extends AbstractWebRoute<A, B, C, D, E, F>, D extends AbstractWebRouteManager<A, B, C, D, E, F>, E extends AbstractRouter<A, B, C, D, E, F>, F extends AbstractWebRouteExtractor<A, B, C, D, E, F>>
	extends
		AbstractRouteManager<A, B, C, D, E, F>
	implements
		PathRoute.Manager<A, B, C, D, E>,
		ContentRoute.Manager<A, B, C, D, E>,
		AcceptContentRoute.Manager<A, B, C, D, E>,
		AcceptLanguageRoute.Manager<A, B, C, D, E> {

	private Set<String> paths;
	private Set<URIPattern> pathPatterns;
	private Set<String> contentTypes;
	private Set<String> accepts;
	private Set<String> languages;

	/**
	 * <p>
	 * Creates an internal route manager.
	 * </p>
	 *
	 * @param router the internal router
	 */
	protected AbstractWebRouteManager(E router) {
		super(router);
	}

	@Override
	@SuppressWarnings("unchecked")
	public D path(String path) {
		Objects.requireNonNull(path);
		if(this.paths == null) {
			this.paths = new HashSet<>();
		}
		this.paths.add(path);
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D pathPattern(URIPattern pathPattern) {
		Objects.requireNonNull(pathPattern);
		if(this.pathPatterns == null) {
			this.pathPatterns = new HashSet<>();
		}
		this.pathPatterns.add(pathPattern);
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D contentType(String contentType) {
		Objects.requireNonNull(contentType);
		if(this.contentTypes == null) {
			this.contentTypes = new HashSet<>();
		}
		this.contentTypes.add(contentType);
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D accept(String accept) {
		Objects.requireNonNull(accept);
		if(this.accepts == null) {
			this.accepts = new HashSet<>();
		}
		this.accepts.add(accept);
		return (D)this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public D language(String language) {
		Objects.requireNonNull(language);
		if(this.languages == null) {
			this.languages = new HashSet<>();
		}
		this.languages.add(language);
		return (D)this;
	}

	/**
	 * <p>
	 * Determines whether the specified internal route path is matching the route manager path.
	 * </p>
	 *
	 * @param route an internal route
	 *
	 * @return true if the route path is matching, false otherwise
	 */
	protected final boolean matchesPath(C route) {
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

	/**
	 * <p>
	 * Determines whether the specified internal route content type is matching the route manager content type.
	 * </p>
	 *
	 * @param route an internal route
	 *
	 * @return true if the route content type is matching, false otherwise
	 */
	protected final boolean matchesContentType(C route) {
		if(this.contentTypes != null && !this.contentTypes.isEmpty()) {
			return route.getContentType() != null && this.contentTypes.contains(route.getContentType());
		}
		return true;
	}

	/**
	 * <p>
	 * Determines whether the specified internal route accept is matching the route manager accept.
	 * </p>
	 *
	 * @param route an internal route
	 *
	 * @return true if the route accept is matching, false otherwise
	 */
	protected final boolean matchesAccept(C route) {
		if(this.accepts != null && !this.accepts.isEmpty()) {
			return route.getAccept() != null && this.accepts.contains(route.getAccept());
		}
		return true;
	}

	/**
	 * <p>
	 * Determines whether the specified internal route accept language is matching the route manager accept language.
	 * </p>
	 *
	 * @param route an internal route
	 *
	 * @return true if the route accept language is matching, false otherwise
	 */
	protected final boolean matchesLanguage(C route) {
		if(this.languages != null && !this.languages.isEmpty()) {
			return route.getLanguage() != null && this.languages.contains(route.getLanguage());
		}
		return true;
	}

	@Override
	protected Predicate<C> routeMatcher() {
		return ((Predicate<C>)this::matchesPath)
			.and(this::matchesContentType)
			.and(this::matchesAccept)
			.and(this::matchesLanguage);
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
	protected final Consumer<F> pathRouteExtractor(Consumer<F> next) {
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

	/**
	 * <p>
	 * Returns the content type route extractor.
	 * </p>
	 *
	 * @param next the next route extractor
	 *
	 * @return the content type route extractor
	 */
	protected final Consumer<F> contentTypeRouteExtractor(Consumer<F> next) {
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
	 * Returns the accept route extractor.
	 * </p>
	 *
	 * @param next the next route extractor
	 *
	 * @return the accept route extractor
	 */
	protected final Consumer<F> acceptRouteExtractor(Consumer<F> next) {
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
	 * Returns the accept language route extractor.
	 * </p>
	 *
	 * @param next the next route extractor
	 *
	 * @return the accept language route extractor
	 */
	protected final Consumer<F> languageRouteExtractor(Consumer<F> next) {
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
	protected Function<Consumer<F>, Consumer<F>> routeExtractor() {
		return ((Function<Consumer<F>, Consumer<F>>)this::pathRouteExtractor)
			.andThen(this::contentTypeRouteExtractor)
			.andThen(this::acceptRouteExtractor)
			.andThen(this::languageRouteExtractor);
	}
}
