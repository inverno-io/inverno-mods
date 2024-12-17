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
import io.inverno.mod.http.base.router.AbstractRouteExtractor;
import io.inverno.mod.http.base.router.AbstractRouteManager;
import io.inverno.mod.http.base.router.AbstractRouter;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.PathRoute;

/**
 * <p>
 * Base internal route extractor.
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
abstract class AbstractWebRouteExtractor<A, B, C extends AbstractWebRoute<A, B, C, D, E, F>, D extends AbstractRouteManager<A, B, C, D, E, F>, E extends AbstractRouter<A, B, C, D, E, F>, F extends AbstractWebRouteExtractor<A, B, C, D, E, F>>
	extends
		AbstractRouteExtractor<A, B, C, D, E, F>
	implements
		PathRoute.Extractor<A, C, F>,
		ContentRoute.Extractor<A, C, F>,
		AcceptContentRoute.Extractor<A, C, F>,
		AcceptLanguageRoute.Extractor<A, C, F> {

	private String path;
	private URIPattern pathPattern;
	private String contentType;
	private String accept;
	private String language;

	/**
	 * <p>
	 * Creates an internal route extractor.
	 * </p>
	 *
	 * @param router the internal router
	 */
	protected AbstractWebRouteExtractor(E router) {
		super(router);
	}

	/**
	 * <p>
	 * Creates a Web route extractor.
	 * </p>
	 *
	 * @param parent the parent extractor
	 */
	protected AbstractWebRouteExtractor(F parent) {
		super(parent);
	}

	/**
	 * <p>
	 * Creates a child route extractor.
	 * </p>
	 *
	 * @return a child route extractor
	 */
	protected abstract F createChildExtractor();

	/**
	 * <p>
	 * Returns the extracted route path.
	 * </p>
	 *
	 * @return an absolute path
	 */
	private String getPath() {
		if(this.path != null) {
			return this.path;
		}
		return this.parent != null ? ((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)this.parent).getPath() : null;
	}

	@Override
	public F path(String path) {
		F childExtractor = this.createChildExtractor();
		((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)childExtractor).path = path;
		return childExtractor;
	}

	/**
	 * <p>
	 * Returns the extracted route path pattern.
	 * </p>
	 *
	 * @return an absolute path pattern
	 */
	private URIPattern getPathPattern() {
		if(this.pathPattern != null) {
			return this.pathPattern;
		}
		return this.parent != null ? ((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)this.parent).getPathPattern() : null;
	}

	@Override
	public F pathPattern(URIPattern pathPattern) {
		F childExtractor = this.createChildExtractor();
		((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)childExtractor).pathPattern = pathPattern;
		return childExtractor;
	}

	/**
	 * <p>
	 * Returns the extracted route content type.
	 * </p>
	 *
	 * @return a media type
	 */
	private String getContentType() {
		if(this.contentType != null) {
			return this.contentType;
		}
		return this.parent != null ? ((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)this.parent).getContentType() : null;
	}

	@Override
	public F contentType(String contentType) {
		F childExtractor = this.createChildExtractor();
		((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)childExtractor).contentType = contentType;
		return childExtractor;
	}

	/**
	 * <p>
	 * Returns the extracted media range accepted by the route.
	 * </p>
	 *
	 * @return a media range
	 */
	private String getAccept() {
		if(this.accept != null) {
			return this.accept;
		}
		return this.parent != null ? ((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)this.parent).getAccept() : null;
	}

	@Override
	public F accept(String accept) {
		F childExtractor = this.createChildExtractor();
		((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)childExtractor).accept = accept;
		return childExtractor;
	}

	/**
	 * <p>
	 * Returns the extracted language range accepted by the route.
	 * </p>
	 *
	 * @return a language range
	 */
	private String getLanguage() {
		if(this.language != null) {
			return this.language;
		}
		return this.parent != null ? ((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)this.parent).getLanguage() : null;
	}

	@Override
	public F language(String language) {
		F childExtractor = this.createChildExtractor();
		((AbstractWebRouteExtractor<?, ?, ?, ?, ?, ?>)childExtractor).language = language;
		return childExtractor;
	}

	@Override
	protected void populateRoute(C route) {
		if(this.getPath() != null) {
			route.setPath(this.getPath());
		}
		if(this.getPathPattern() != null) {
			route.setPathPattern(this.getPathPattern());
		}
		route.setContentType(this.getContentType());
		route.setAccept(this.getAccept());
		route.setLanguage(this.getLanguage());
	}
}
