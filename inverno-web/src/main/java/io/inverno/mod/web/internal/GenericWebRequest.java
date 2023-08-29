/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.web.WebRequest;
import io.inverno.mod.web.WebRequestBody;
import java.net.SocketAddress;
import java.util.Optional;

/**
 * <p>
 * Generic {@link WebRequest} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see WebRequestBody
 */
class GenericWebRequest implements WebRequest {

	private final Request request;
	
	private final DataConversionService dataConversionService;
	
	private final ObjectConverter<String> parameterConverter;
	
	private GenericPathParameters pathParameters;
	
	private Optional<WebRequestBody> webRequestBody;
	
	/**
	 * <p>
	 * Creates a generic web request with the specified underlying request and parameter value converter.
	 * </p>
	 * 
	 * <p>
	 * Since no data conversion service is specified, the request body is assumed to be empty. This is particularly the case in an error exchange.
	 * </p>
	 * 
	 * @param request               the underlying request
	 * @param parameterConverter    a string object converter
	 */
	public GenericWebRequest(Request request, ObjectConverter<String> parameterConverter) {
		this(request, parameterConverter, null);
	}
	
	/**
	 * <p>
	 * Creates a generic web request with the specified underlying request, data conversion service and parameter value converter.
	 * </p>
	 *
	 * @param request               the underlying request
	 * @param parameterConverter    a string object converter
	 * @param dataConversionService the data conversion service
	 */
	public GenericWebRequest(Request request, ObjectConverter<String> parameterConverter, DataConversionService dataConversionService) {
		this.request = request;
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
	}

	@Override
	public InboundRequestHeaders headers() {
		return this.request.headers();
	}

	@Override
	public QueryParameters queryParameters() {
		return this.request.queryParameters();
	}
	
	@Override
	public GenericPathParameters pathParameters() {
		if(this.pathParameters == null) {
			this.pathParameters = new GenericPathParameters(this.parameterConverter);
		}
		return this.pathParameters;
	}

	@Override
	@Deprecated
	public InboundCookies cookies() {
		return this.request.cookies();
	}
	
	@Override
	public Method getMethod() {
		return this.request.getMethod();
	}

	@Override
	public String getScheme() {
		return this.request.getScheme();
	}

	@Override
	public String getAuthority() {
		return this.request.getAuthority();
	}

	@Override
	public String getPath() {
		return this.request.getPath();
	}

	@Override
	public String getPathAbsolute() {
		return this.request.getPathAbsolute();
	}
	
	@Override
	public URIBuilder getPathBuilder() {
		return this.request.getPathBuilder();
	}

	@Override
	public String getQuery() {
		return this.request.getQuery();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.request.getRemoteAddress();
	}

	@Override
	public Optional<WebRequestBody> body() {
		if(this.webRequestBody == null) {
			if(this.dataConversionService != null) {
				this.webRequestBody = this.request.body().map(requestBody -> new GenericWebRequestBody(this, requestBody, this.dataConversionService));
			}
			else {
				this.webRequestBody = Optional.empty();
			}
		}
		return this.webRequestBody;
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.request.getLocalAddress();
	}
}
