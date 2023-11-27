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
 * Base HTTP header interface defining common HTTP header.
 * </p>  
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see Headers
 * @see HeaderBuilder
 * @see HeaderCodec
 */
public interface Header {

	/**
	 * <p>
	 * Returns the header's name.
	 * </p>
	 * 
	 * <p>
	 * A header name is always in lower case.
	 * </p>
	 * 
	 * @return a header name
	 */
	String getHeaderName();

	/**
	 * <p>
	 * Returns header's raw value.
	 * </p>
	 * 
	 * @return a raw header value
	 */
	String getHeaderValue();
}
