/*
 * Copyright 2021 Jeremy Kuhn
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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.web.server.PathParameters;
import io.inverno.mod.web.server.WebRequest;
import io.inverno.mod.web.server.WebRequestBody;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Map;
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
public class GenericWebRequest implements WebRequest {

	private final ServerDataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final Request request;

	private GenericPathParameters pathParameters;
	private Optional<WebRequestBody> webRequestBody;

	/**
	 * <p>
	 * Creates a generic Web request.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 * @param request               the originating request
	 */
	public GenericWebRequest(ServerDataConversionService dataConversionService, ObjectConverter<String> parameterConverter, Request request) {
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		this.request = request;
	}

	@Override
	public String getScheme() {
		return this.request.getScheme();
	}

	@Override
	public SocketAddress getLocalAddress() {
		return this.request.getLocalAddress();
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return this.request.getLocalCertificates();
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.request.getRemoteAddress();
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.request.getRemoteCertificates();
	}

	@Override
	public Method getMethod() {
		return this.request.getMethod();
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

	/**
	 * <p>
	 * Sets the path parameters extracted from the path when resolving the Web route.
	 * </p>
	 *
	 * @param parameters a map of path parameters
	 */
	public void setPathParameters(Map<String, String> parameters) {
		if(this.pathParameters == null) {
			this.pathParameters = new GenericPathParameters(this.parameterConverter, parameters);
		}
		else {
			this.pathParameters.putAll(parameters);
		}
	}

	@Override
	public PathParameters pathParameters() {
		if(this.pathParameters == null) {
			this.pathParameters = new GenericPathParameters(this.parameterConverter);
		}
		return pathParameters;
	}

	@Override
	public String getQuery() {
		return this.request.getQuery();
	}

	@Override
	public QueryParameters queryParameters() {
		return this.request.queryParameters();
	}

	@Override
	public InboundRequestHeaders headers() {
		return this.request.headers();
	}

	@Override
	public Optional<WebRequestBody> body() {
		if(this.webRequestBody == null) {
			if(this.dataConversionService != null) {
				this.webRequestBody = this.request.body().map(requestBody -> new GenericWebRequestBody(this.dataConversionService, this, requestBody));
			}
			else {
				this.webRequestBody = Optional.empty();
			}
		}
		return this.webRequestBody;
	}
}
