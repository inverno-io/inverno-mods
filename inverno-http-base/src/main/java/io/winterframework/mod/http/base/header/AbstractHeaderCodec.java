/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.http.base.header;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

/**
 * <p>
 * Base implementation for {@link HeaderCodec}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see HeaderCodec
 * 
 * @param <A> the header type encoded/decoded by the codec
 * @param <B> the header builder type
 */
public abstract class AbstractHeaderCodec<A extends Header, B extends HeaderBuilder<A, B>> implements HeaderCodec<A> {
	
	/**
	 * The header builder supplier.
	 */
	protected Supplier<B> builderSupplier;
	
	/**
	 * The supported header names.
	 */
	protected Set<String> supportedHeaderNames;
	
	/**
	 * <p>
	 * Creates a header codec with the specified builder supplier and supported
	 * header names.
	 * </p>
	 * 
	 * @param builderSupplier      a supplier to create header builder instances
	 *                             when decoding a header
	 * @param supportedHeaderNames the list of header names supported by the codec
	 */
	protected AbstractHeaderCodec(Supplier<B> builderSupplier, Set<String> supportedHeaderNames) {
		this.builderSupplier = builderSupplier;
		this.supportedHeaderNames = supportedHeaderNames != null ? Collections.unmodifiableSet(supportedHeaderNames) : Set.of();
	}
	
	@Override
	public Set<String> getSupportedHeaderNames() {
		return this.supportedHeaderNames;
	}
}
