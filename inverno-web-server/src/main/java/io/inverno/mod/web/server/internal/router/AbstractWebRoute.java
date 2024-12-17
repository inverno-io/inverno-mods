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
import io.inverno.mod.http.base.router.AbstractRoute;
import io.inverno.mod.http.base.router.AbstractRouteExtractor;
import io.inverno.mod.http.base.router.AbstractRouteManager;
import io.inverno.mod.http.base.router.AbstractRouter;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.PathRoute;
import java.util.Objects;

/**
 * <p>
 * Base internal route.
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
abstract class AbstractWebRoute<A, B, C extends AbstractWebRoute<A, B, C, D, E, F>, D extends AbstractRouteManager<A, B, C, D, E, F>, E extends AbstractRouter<A, B, C, D, E, F>, F extends AbstractRouteExtractor<A, B, C, D, E, F>>
	extends
		AbstractRoute<A, B, C, D, E, F>
	implements
		PathRoute<A>,
		ContentRoute<A>,
		AcceptContentRoute<A>,
		AcceptLanguageRoute<A> {

	private String path;
	private URIPattern pathPattern;
	private String contentType;
	private String accept;
	private String language;

	/**
	 * <p>
	 * Creates an internal route.
	 * </p>
	 *
	 * @param router   the internal router
	 * @param resource the resource
	 * @param disabled true to create a disabled route, false otherwise
	 */
	protected AbstractWebRoute(E router, A resource, boolean disabled) {
		super(router, resource, disabled);
	}

	/**
	 * <p>
	 * Sets the route path.
	 * </p>
	 *
	 * @param path an absolute path
	 */
	void setPath(String path) {
		this.path = path;
		this.pathPattern = null;
	}

	@Override
	public String getPath() {
		return this.path;
	}

	/**
	 * <p>
	 * Sets the route path pattern.
	 * </p>
	 *
	 * @param pathPattern an absolute path pattern
	 */
	void setPathPattern(URIPattern pathPattern) {
		this.path = null;
		this.pathPattern = pathPattern;
	}

	@Override
	public URIPattern getPathPattern() {
		return this.pathPattern;
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
	public String getContentType() {
		return this.contentType;
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
	public String getAccept() {
		return this.accept;
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
	public String getLanguage() {
		return this.language;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		AbstractWebRoute<?, ?, ?, ?, ?, ?> that = (AbstractWebRoute<?, ?, ?, ?, ?, ?>) o;
		return Objects.equals(path, that.path) && Objects.equals(pathPattern, that.pathPattern) && Objects.equals(contentType, that.contentType) && Objects.equals(accept, that.accept) && Objects.equals(language, that.language);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), path, pathPattern, contentType, accept, language);
	}
}
