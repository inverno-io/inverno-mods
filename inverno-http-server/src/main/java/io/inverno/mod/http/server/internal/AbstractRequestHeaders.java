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
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.header.HeaderService;

/**
 * <p>
 * Base {@link InboundRequestHeaders} implementation.
 * </p>
 * 
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 */
public abstract class AbstractRequestHeaders implements InboundRequestHeaders {

	/**
	 * The header service.
	 */
	protected final HeaderService headerService;
	/**
	 * The parameter converter.
	 */
	protected final ObjectConverter<String> parameterConverter;
	
	private GenericRequestCookies requestCookies;

	/**
	 * <p>
	 * Creates base request headers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the paremeter converter
	 */
	public AbstractRequestHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	@Override
	public final GenericRequestCookies cookies() {
		if(this.requestCookies == null) {
			this.requestCookies = new GenericRequestCookies(this, this.parameterConverter);
		}
		return this.requestCookies;
	}
}
