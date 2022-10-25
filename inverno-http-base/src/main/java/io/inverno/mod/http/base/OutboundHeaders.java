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
package io.inverno.mod.http.base;

import io.inverno.mod.http.base.header.Header;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface OutboundHeaders<A extends OutboundHeaders<A>> extends InboundHeaders {

	/**
	 * <p>
	 * Determines whether the headers have been sent to the recipient.
	 * </p>
	 * 
	 * @return true if the headers have been sent, false otherwise
	 */
	boolean isWritten();
	
	/**
	 * <p>
	 * Adds a header with the specified name and value.
	 * </p>
	 * 
	 * @param name  the header name
	 * @param value the header value
	 * 
	 * @return the response headers
	 */
	A add(CharSequence name, CharSequence value);

	/**
	 * <p>
	 * Adds the specified headers.
	 * </p>
	 * 
	 * @param headers the headers to add
	 * 
	 * @return the response headers
	 */
	A add(Header... headers);

	/**
	 * <p>
	 * Sets the value of the header with the specified name.
	 * </p>
	 * 
	 * @param name  the header name
	 * @param value the header value
	 * 
	 * @return the response headers
	 */
	A set(CharSequence name, CharSequence value);

	/**
	 * <p>
	 * Sets the specified headers.
	 * </p>
	 * 
	 * @param headers the headers to set
	 * 
	 * @return the response headers
	 */
	A set(Header... headers);

	/**
	 * <p>
	 * Removes the headers with the specified names.
	 * </p>
	 * 
	 * @param names the names of the headers to remove
	 * 
	 * @return the response headers
	 */
	A remove(CharSequence... names);
}
