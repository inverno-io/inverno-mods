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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.InboundResponseHeaders;
import io.inverno.mod.http.base.InboundSetCookies;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import java.util.function.Consumer;

/**
 * <p>
 * Base {@link InboundResponseHeaders} implementation representing the headers received from the connected remote endpoint.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.11
 */
public abstract class AbstractResponseHeaders implements InboundResponseHeaders {

	/**
	 * The parameter converter.
	 */
	protected final ObjectConverter<String> parameterConverter;
	
	private GenericResponseCookies cookies;

	/**
	 * <p>
	 * Creates base HTTP response headers.
	 * </p>
	 * 
	 * @param parameterConverter the parameter converter
	 */
	public AbstractResponseHeaders(ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
	}

	protected abstract void configureInterceptedHeaders(Consumer<OutboundResponseHeaders> headersConfigurer);
	
	@Override
	public InboundSetCookies cookies() {
		if(this.cookies == null) {
			this.cookies = new GenericResponseCookies(this.parameterConverter, this);
		}
		return this.cookies;
	}
}
