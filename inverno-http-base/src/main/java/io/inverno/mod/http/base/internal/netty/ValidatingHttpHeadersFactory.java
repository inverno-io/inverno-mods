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
package io.inverno.mod.http.base.internal.netty;

import io.inverno.mod.http.base.internal.header.HeadersValidator;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeadersFactory;

/**
 * <p>
 * An {@link HttpHeadersFactory} implementation for creating validating HTTP headers.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.9
 * 
 * @see HeadersValidator
 */
public class ValidatingHttpHeadersFactory implements HttpHeadersFactory {
	
	/**
	 * Default validating headers factory.
	 */
	public static final HttpHeadersFactory VALIDATING_HEADERS_FACTORY = new DefaultValidatingHttpHeadersFactory();
	
	/**
	 * Default non-validating headers factory.
	 */
	public static final HttpHeadersFactory NON_VALIDATING_HEADERS_FACTORY = new DefaultNonValidatingHttpHeadersFactory();
	
	/**
	 * The headers validator or null for creating non-validating headers.
	 */
	private final HeadersValidator headersValidator;
	
	/**
	 * <p>
	 * Creates a validating headers factory.
	 * </p>
	 * 
	 * @param headersValidator a headers validator or null
	 */
	public ValidatingHttpHeadersFactory(HeadersValidator headersValidator) {
		this.headersValidator = headersValidator;
	}

	@Override
	public HttpHeaders newHeaders() {
		return new LinkedHttpHeaders(this.headersValidator);
	}

	@Override
	public HttpHeaders newEmptyHeaders() {
		return new LinkedHttpHeaders(this.headersValidator);
	}
	
	/**
	 * <p>
	 * Validating HTTP headers factory.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	private static class DefaultValidatingHttpHeadersFactory implements HttpHeadersFactory {

		@Override
		public HttpHeaders newHeaders() {
			return new LinkedHttpHeaders(HeadersValidator.DEFAULT_HTTP1X_HEADERS_VALIDATOR);
		}

		@Override
		public HttpHeaders newEmptyHeaders() {
			return new LinkedHttpHeaders(HeadersValidator.DEFAULT_HTTP1X_HEADERS_VALIDATOR);
		}
	}
	
	/**
	 * <p>
	 * Non-validating HTTP headers factory.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.9
	 */
	private static class DefaultNonValidatingHttpHeadersFactory implements HttpHeadersFactory {

		@Override
		public HttpHeaders newHeaders() {
			return new LinkedHttpHeaders(null);
		}

		@Override
		public HttpHeaders newEmptyHeaders() {
			return new LinkedHttpHeaders(null);
		}
	}
}