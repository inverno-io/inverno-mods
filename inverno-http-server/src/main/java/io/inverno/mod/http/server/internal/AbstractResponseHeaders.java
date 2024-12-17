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
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.OutboundSetCookies;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Header;
import io.inverno.mod.http.base.header.HeaderService;
import java.lang.reflect.Type;
import java.util.function.Consumer;

/**
 * <p>
 * Base {@link OutboundResponseHeaders} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 * 
 * @param <A> the response headers type
 */
public abstract class AbstractResponseHeaders<A extends AbstractResponseHeaders<A>> implements OutboundResponseHeaders {

	/**
	 * The header service.
	 */
	protected final HeaderService headerService;
	/**
	 * The parameter converter.
	 */
	protected final ObjectConverter<String> parameterConverter;
	
	private boolean written;
	private GenericResponseCookies cookies;

	/**
	 * <p>
	 * Creates base response headers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public AbstractResponseHeaders(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}
	
	/**
	 * <p>
	 * Flags the headers to have been written.
	 * </p>
	 */
	public final void setWritten() {
		this.written = true;
	}
	
	@Override
	public final boolean isWritten() {
		return this.written;
	}

	@Override
	public abstract A status(Status status);

	@Override
	public abstract A status(int status);
	
	@Override
	public abstract A contentType(String contentType);

	@Override
	public abstract A contentLength(long contentLength);
	
	@Override
	@SuppressWarnings("unchecked")
	public final A cookies(Consumer<OutboundSetCookies> cookiesConfigurer) {
		if(cookiesConfigurer != null) {
			cookiesConfigurer.accept(this.cookies());
		}
		return (A)this;
	}

	@Override
	public final GenericResponseCookies cookies() {
		if(this.cookies == null) {
			this.cookies = new GenericResponseCookies(this.headerService, this, this.parameterConverter);
		}
		return this.cookies;
	}
	
	@Override
	public abstract A add(CharSequence name, CharSequence value);

	@Override
	public <T> A addParameter(CharSequence name, T value) {
		return this.add(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> OutboundResponseHeaders addParameter(CharSequence name, T value, Type type) {
		return this.add(name, this.parameterConverter.encode(value, type));
	}

	@Override
	public abstract A add(Header... headers);

	@Override
	public abstract A set(CharSequence name, CharSequence value);

	@Override
	public <T> A setParameter(CharSequence name, T value) {
		return this.set(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> OutboundResponseHeaders setParameter(CharSequence name, T value, Type type) {
		return this.set(name, this.parameterConverter.encode(value, type));
	}

	@Override
	public abstract A set(Header... headers);

	@Override
	public abstract A remove(CharSequence... names);
}
