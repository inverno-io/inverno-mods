/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.URIBuilder;
import io.inverno.mod.base.net.URIs;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.internal.GenericQueryParameters;
import io.inverno.mod.http.client.Part;
import java.util.function.Consumer;
import io.inverno.mod.http.client.Request;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import java.net.SocketAddress;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * <p>
 * The {@link Request} implementation exposed in the {@link EndpointExchange} to populate the request.
 * </p>
 * 
 * <p>
 * Once the request has been sent, it becomes a proxy to the actual request. At this point the request becomes immutable.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @version 1.8
 */
public class EndpointRequest implements Request  {

	private final ObjectConverter<String> parameterConverter;
	private final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	private final MultipartEncoder<Part<?>> multipartBodyEncoder; 
	private final Part.Factory partFactory;
	private final EndpointRequestHeaders requestHeaders;
	
	private Method method;
	private String authority;
	private String path;
	private URIBuilder primaryPathBuilder;
	private String pathAbsolute;
	private String queryString;
	private GenericQueryParameters queryParameters;
	private Optional<EndpointRequestBody> requestBody;
	
	private HttpConnectionRequest connectedRequest;

	/**
	 * <p>
	 * Creates an endpoint request.
	 * </p>
	 *
	 * @param headerService         the header service
	 * @param parameterConverter    the parameter converter
	 * @param urlEncodedBodyEncoder the URL encoded body encoder
	 * @param multipartBodyEncoder  the multipart body encoder
	 * @param partFactory           the part factory
	 * @param method                the HTTP method
	 * @param path                  the path
	 */
	public EndpointRequest(
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter, 
			MultipartEncoder<Parameter> urlEncodedBodyEncoder,
			MultipartEncoder<Part<?>> multipartBodyEncoder, 
			Part.Factory partFactory,
			Method method, 
			String path) {
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
		this.method = method;
		this.path(path);
		this.requestHeaders = new EndpointRequestHeaders(headerService, parameterConverter);
	}
	
	/**
	 * <p>
	 * Validates that the request has not been sent yet and that we can perform mutable operations.
	 * </p>
	 * 
	 * @throws IllegalArgumentException if the request has been sent
	 */
	private void checkNotConnected() throws IllegalArgumentException {
		if(this.connectedRequest != null) {
			throw new IllegalStateException("Request already sent");
		}
	}
	
	/**
	 * <p>
	 * Injects either the request sent to the endpoint or this request or the interceptable request when the request was intercepted (i.e. interceptor returned an empty publisher).
	 * </p>
	 * 
	 * @param connectedRequest the retained request
	 */
	public void setConnectedRequest(HttpConnectionRequest connectedRequest) {
		this.connectedRequest = connectedRequest;
	}
	
	@Override
	public SocketAddress getLocalAddress() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getLocalAddress() : null;
	}

	@Override
	public Optional<Certificate[]> getLocalCertificates() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getLocalCertificates(): null;
	}

	@Override
	public SocketAddress getRemoteAddress() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getRemoteAddress(): null;
	}

	@Override
	public Optional<Certificate[]> getRemoteCertificates() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getRemoteCertificates(): null;
	}
	
	@Override
	public String getScheme() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getScheme() : null;
	}

	@Override
	public Request method(Method method) throws IllegalStateException {
		this.checkNotConnected();
		this.method = method != null ? method : Method.GET;
		return this;
	}
	
	@Override
	public Method getMethod() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getMethod() : this.method;
	}
	
	@Override
	public Request authority(String authority) throws IllegalStateException {
		this.checkNotConnected();
		this.authority = authority;
		return this;
	}
	
	@Override
	public String getAuthority() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getAuthority() : this.authority;
	}
	
	@Override
	public final Request path(String path) throws IllegalStateException {
		this.checkNotConnected();
		this.path = path;
		// TODO make sure this is a path with no scheme or authority
		this.primaryPathBuilder = URIs.uri(path, false, URIs.Option.NORMALIZED);
		this.pathAbsolute = null;
		return this;
	}
	
	@Override
	public String getPath() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getPath() : this.path;
	}

	@Override
	public String getPathAbsolute() {
		if(this.connectedRequest != null && this.connectedRequest != this) {
			return this.connectedRequest.getPathAbsolute();
		}
		if(this.pathAbsolute == null) {
			this.pathAbsolute = this.primaryPathBuilder.buildRawString();
		}
		return this.pathAbsolute;
	}

	@Override
	public URIBuilder getPathBuilder() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.getPathBuilder() : this.primaryPathBuilder.clone();
	}

	@Override
	public String getQuery() {
		if(this.connectedRequest != null && this.connectedRequest != this) {
			return this.connectedRequest.getQuery();
		}
		if(this.queryString == null) {
			this.queryString = this.primaryPathBuilder.buildRawQuery();
		}
		return this.queryString;
	}

	@Override
	public QueryParameters queryParameters() {
		if(this.connectedRequest != null && this.connectedRequest != this) {
			return this.connectedRequest.queryParameters();
		}
		if(this.queryParameters == null) {
			this.queryParameters = new GenericQueryParameters(this.primaryPathBuilder.getQueryParameters(), this.parameterConverter);
		}
		return this.queryParameters;
	}
	
	@Override
	public boolean isHeadersWritten() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.isHeadersWritten() : false;
	}

	@Override
	public Request headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException {
		this.checkNotConnected();
		if(headersConfigurer != null) {
			headersConfigurer.accept(this.requestHeaders);
		}
		return this;
	}
	
	@Override
	public InboundRequestHeaders headers() {
		return this.connectedRequest != null && this.connectedRequest != this ? this.connectedRequest.headers() : this.requestHeaders;
	}

	public EndpointRequestHeaders getHeaders() {
		return this.requestHeaders;
	}
	
	@Override
	public Optional<EndpointRequestBody> body() throws IllegalStateException {
		this.checkNotConnected();
		if(this.requestBody == null) {
			switch(this.getMethod()) {
				case POST:
				case PUT:
				case PATCH:
				case DELETE: {
					this.requestBody = Optional.of(new EndpointRequestBody(this.requestHeaders, this.parameterConverter, this.urlEncodedBodyEncoder, this.multipartBodyEncoder, this.partFactory));
					break;
				}
				default: {
					this.requestBody = Optional.empty();
				}
			}
		}
		return this.requestBody;
	}
	
	/**
	 * <p>
	 * Returns the endpoint request body.
	 * </p>
	 * 
	 * @return the request body or null if none was specified or if the method doesn't allow any body.
	 */
	public EndpointRequestBody getBody() {
		if(this.requestBody != null && this.requestBody.isPresent()) {
			return (EndpointRequestBody)this.requestBody.get();
		}
		return null;
	}

	/**
	 * <p>
	 * Determines whether the request was sent
	 * </p>
	 * 
	 * @return true when the request was sent, false otherwise
	 */
	public boolean isSent() {
		return this.connectedRequest != null && this.connectedRequest != this;
	}
}
