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
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.PathRoute;
import io.inverno.mod.http.base.router.Route;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Base internal interceptor route.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 * @param <B> the exchange type
 * @param <C> the internal interceptor route type
 */
abstract class AbstractWebRouteInterceptorRoute<A extends ExchangeContext, B extends Exchange<A>, C extends AbstractWebRouteInterceptorRoute<A, B, C>>
	implements
		Route<ExchangeInterceptor<A, B>>,
		PathRoute<ExchangeInterceptor<A, B>>,
		ContentRoute<ExchangeInterceptor<A, B>>,
		AcceptContentRoute<ExchangeInterceptor<A, B>>,
		AcceptLanguageRoute<ExchangeInterceptor<A, B>> {

	/**
	 * Content type header codec.
	 */
	protected static final HeaderCodec<? extends Headers.ContentType> CONTENT_TYPE_CODEC = new ContentTypeCodec();
	/**
	 * Accept language header codec.
	 */
	protected static final HeaderCodec<? extends Headers.AcceptLanguage> ACCEPT_LANGUAGE_CODEC = new AcceptLanguageCodec(false);

	/**
	 * The parent route.
	 */
	protected final C parent;

	private String path;
	private URIPattern pathPattern;
	private String contentType;
	private Headers.Accept.MediaRange contentTypeRange;
	private String accept;
	private Headers.Accept.MediaRange acceptRange;
	private String language;
	private Headers.AcceptLanguage.LanguageRange languageRange;

	private ExchangeInterceptor<A, B> interceptor;

	/**
	 * <p>
	 * Creates an internal interceptor route.
	 * </p>
	 *
	 * @param parent the parent route
	 */
	protected AbstractWebRouteInterceptorRoute(C parent) {
		this.parent = parent;
	}

	@Override
	public String getPath() {
		if(this.path != null) {
			return this.path;
		}
		return this.parent != null ? this.parent.getPath() : null;
	}

	/**
	 * <p>
	 * Sets the route path.
	 * </p>
	 *
	 * @param path an absolute path
	 */
	public void setPath(String path) {
		this.path = path;
		this.pathPattern = null;
	}

	@Override
	public URIPattern getPathPattern() {
		if(this.pathPattern != null) {
			return this.pathPattern;
		}
		return this.parent != null ? this.parent.getPathPattern() : null;
	}

	/**
	 * <p>
	 * Sets the route path pattern.
	 * </p>
	 *
	 * @param pathPattern an absolute path pattern
	 */
	public void setPathPattern(URIPattern pathPattern) {
		this.path = null;
		this.pathPattern = pathPattern;
	}

	@Override
	public String getContentType() {
		if(this.contentType != null) {
			return this.contentType;
		}
		return this.parent != null ? this.parent.getContentType() : null;
	}

	/**
	 * <p>
	 * Returns the route content type range.
	 * </p>
	 *
	 * @return a media range
	 */
	public Headers.Accept.MediaRange getContentTypeRange() {
		if(this.contentTypeRange != null) {
			return this.contentTypeRange;
		}
		return this.parent != null ? this.parent.getContentTypeRange() : null;
	}

	/**
	 * <p>
	 * Sets the route content type range.
	 * </p>
	 *
	 * @param contentType a media range
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
		this.contentTypeRange = CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, contentType).toMediaRange();
	}

	@Override
	public String getAccept() {
		if(this.accept != null) {
			return this.accept;
		}
		return this.parent != null ? this.parent.getAccept() : null;
	}

	/**
	 * <p>
	 * Returns the route accepted media range.
	 * </p>
	 *
	 * @return a media range
	 */
	public Headers.Accept.MediaRange getAcceptRange() {
		if(this.acceptRange != null) {
			return this.acceptRange;
		}
		return this.parent != null ? this.parent.getAcceptRange() : null;
	}

	/**
	 * <p>
	 * Sets the route accepted media range.
	 * </p>
	 *
	 * @param accept a media range
	 */
	public void setAccept(String accept) {
		this.accept = accept;
		this.acceptRange = CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, accept).toMediaRange();
	}

	@Override
	public String getLanguage() {
		if(this.language != null) {
			return this.language;
		}
		return this.parent != null ? this.parent.getLanguage() : null;
	}

	/**
	 * <p>
	 * Returns the route accepted language range.
	 * </p>
	 *
	 * @return a language range
	 */
	public Headers.AcceptLanguage.LanguageRange getLanguageRange() {
		if(this.languageRange != null) {
			return this.languageRange;
		}
		return this.parent != null ? this.parent.getLanguageRange() : null;
	}

	/**
	 * <p>
	 * Sets the route accepted language range.
	 * </p>
	 *
	 * @param language a language range
	 */
	public void setLanguage(String language) {
		this.language = language;
		List<Headers.AcceptLanguage.LanguageRange> languageRanges = ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges();
		if(languageRanges.size() != 1) {
			throw new IllegalArgumentException("Invalid language range: " + language);
		}
		this.languageRange = languageRanges.getFirst();
	}

	@Override
	public ExchangeInterceptor<A, B> get() {
		return this.interceptor;
	}

	/**
	 * <p>
	 * Sets the exchange interceptor.
	 * </p>
	 *
	 * @param interceptor an exchange interceptor
	 */
	public void set(ExchangeInterceptor<A, B> interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public void enable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDisabled() {
		return false;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractWebRouteInterceptorRoute<?, ?, ?> that = (AbstractWebRouteInterceptorRoute<?, ?, ?>) o;
		return Objects.equals(parent, that.parent) && Objects.equals(path, that.path) && Objects.equals(pathPattern, that.pathPattern) && Objects.equals(contentType, that.contentType) && Objects.equals(accept, that.accept) && Objects.equals(language, that.language) && Objects.equals(this.get(), that.get());
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent, path, pathPattern, contentType, accept, language);
	}

	@Override
	public String toString() {
		StringBuilder routeStringBuilder = new StringBuilder();

		routeStringBuilder.append("{");
		routeStringBuilder.append("\"path\":\"").append(this.getPath() != null ? this.getPath() : this.getPathPattern()).append("\",");
		routeStringBuilder.append("\"consume\":\"").append(this.getContentType()).append("\",");
		routeStringBuilder.append("\"produce\":\"").append(this.getAccept()).append("\",");
		routeStringBuilder.append("\"language\":\"").append(this.getLanguage());
		routeStringBuilder.append("}");

		return routeStringBuilder.toString();
	}
}
