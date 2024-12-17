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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.server.Part;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.internal.multipart.MultipartDecoder;
import java.util.Optional;

/**
 * <p>
 * Base {@link Request} implementation
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the request headers type
 * @param <B> the request body type
 */
public abstract class AbstractRequest<A extends AbstractRequestHeaders, B extends AbstractRequestBody<A>> implements Request {

	/**
	 * The parameter converter.
	 */
	protected final ObjectConverter<String> parameterConverter;
	/**
	 * The application/x-www-form-urlencoded body decoder.
	 */
	protected final MultipartDecoder<Parameter> urlEncodedBodyDecoder;
	/**
	 * The multipart/form-data body decoder
	 */
	protected final MultipartDecoder<Part> multipartBodyDecoder;
	/**
	 * The request headers.
	 */
	protected final A headers;
	
	private URIBuilder pathBuilder;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	
	/**
	 * The request body.
	 */
	protected B body;

	/**
	 * <p>
	 * Creates a base request.
	 * </p>
	 *
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyDecoder the application/x-www-form-urlencoded body decoder
	 * @param multipartBodyDecoder  the multipart/form-data body decoder
	 * @param headers               the request headers
	 */
	public AbstractRequest(ObjectConverter<String> parameterConverter, MultipartDecoder<Parameter> urlEncodedBodyDecoder, MultipartDecoder<Part> multipartBodyDecoder, A headers) {
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyDecoder = urlEncodedBodyDecoder;
		this.multipartBodyDecoder = multipartBodyDecoder;
		this.headers = headers;
	}
	
	/**
	 * <p>
	 * Disposes the request.
	 * </p>
	 * 
	 * <p>
	 * This method simply disposes the request body if any was created/requested.
	 * </p>
	 * 
	 * @param cause an error or null if disposal does not result from an error (e.g. shutdown) 
	 */
	public final void dispose(Throwable cause) {
		if(this.body != null) {
			this.body.dispose(cause);
		}
	}
	
	/**
	 * <p>
	 * Returns or creates the primary path builder created from the request path.
	 * </p>
	 * 
	 * <p>
	 * The primary path builder is used to extract absolute path, query parameters and query string and to create the path builder returned by {@link #getPathBuilder() }.
	 * </p>
	 * 
	 * @return the path builder
	 */
	private URIBuilder getPrimaryPathBuilder() {
		if(this.pathBuilder == null) {
			this.pathBuilder = URIs.uri(this.getPath(), false, URIs.Option.NORMALIZED);
		}
		return this.pathBuilder;
	}
	
	@Override
	public String getPathAbsolute() {
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.getPrimaryPathBuilder().buildRawPath();
		}
		return this.pathAbsolute;
	}

	@Override
	public URIBuilder getPathBuilder() {
		return this.getPrimaryPathBuilder().clone();
	}

	@Override
	public String getQuery() {
		if(this.queryString == null) {
			this.queryString = this.getPrimaryPathBuilder().buildRawQuery();
		}
		return this.queryString;
	}

	@Override
	public QueryParameters queryParameters() {
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.getPrimaryPathBuilder().getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}
	
	@Override
	public A headers() {
		return this.headers;
	}

	@Override
	public abstract Optional<B> body();
	
	/**
	 * <p>
	 * Returns the request body if any was created/requested.
	 * </p>
	 * 
	 * @return the request body or null
	 */
	public B getBody() {
		return this.body;
	}
}
