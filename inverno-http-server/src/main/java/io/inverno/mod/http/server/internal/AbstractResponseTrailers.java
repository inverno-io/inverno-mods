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
import io.inverno.mod.http.base.OutboundHeaders;
import io.inverno.mod.http.base.header.HeaderService;
import java.lang.reflect.Type;

/**
 * <p>
 * Base {@link OutboundHeaders} response trailers implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.10
 * 
 * @param <A> the response trailers type
 */
public abstract class AbstractResponseTrailers<A extends AbstractResponseTrailers<A>> implements OutboundHeaders<A> {

	/**
	 * The header service.
	 */
	protected final HeaderService headerService;
	/**
	 * The parameter converter.
	 */
	protected final ObjectConverter<String> parameterConverter;
	
	private boolean written;

	/**
	 * <p>
	 * Creates base response trailers.
	 * </p>
	 * 
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 */
	public AbstractResponseTrailers(HeaderService headerService, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
	}

	@Override
	public <T> A addParameter(CharSequence name, T value) {
		return this.add(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> A addParameter(CharSequence name, T value, Type type) {
		return this.add(name, this.parameterConverter.encode(value, type));
	}

	@Override
	public <T> A setParameter(CharSequence name, T value) {
		return this.set(name, this.parameterConverter.encode(value));
	}

	@Override
	public <T> A setParameter(CharSequence name, T value, Type type) {
		return this.set(name, this.parameterConverter.encode(value, type));
	}

	/**
	 * <p>
	 * Flags the trailers to have been written.
	 * </p>
	 */
	public final void setWritten() {
		this.written = true;
	}

	@Override
	public final boolean isWritten() {
		return this.written;
	}
}
