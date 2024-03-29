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

/**
 * <p>
 * A header builder is used to build a specific {@link Header} instance.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Header
 * 
 * @param <A> the header type built by the builder
 * @param <B> the header builder type
 */

public interface HeaderBuilder<A extends Header, B extends HeaderBuilder<A, B>> {

	/**
	 * <p>
	 * Sets the specified header name.
	 * </p>
	 * 
	 * @param name a header name
	 * @return the header builder
	 */
	B headerName(String name);
	
	/**
	 * <p>
	 * Sets the specified raw header value.
	 * </p>
	 * 
	 * @param value a raw header value
	 * @return the header builder
	 */
	B headerValue(String value);
	
	/**
	 * <p>
	 * Builds the header.
	 * </p>
	 * 
	 * @return a header instance
	 */
	A build();
}
