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

import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Internal request headers.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
public interface InternalRequestHeaders extends OutboundRequestHeaders {

	/**
	 * <p>
	 * Defines whether headers have been sent to the endpoint.
	 * </p>
	 *
	 * @param written true to indicates the headers have been written, false otherwise
	 */
	void setWritten(boolean written);

	/**
	 * <p>
	 * Returns the value of the header with the specified name as a char sequence.
	 * </p>
	 * 
	 * @param name the header name
	 * 
	 * @return the header value or null if there's no header with the specified name
	 */
	CharSequence getCharSequence(CharSequence name);
	
	/**
	 * <p>
	 * Returns the values of all headers with the specified name as char sequences.
	 * </p>
	 *
	 * @param name a header name
	 *
	 * @return a list of header values or an empty list if there's no header with the specified name
	 */
	List<CharSequence> getAllCharSequence(CharSequence name);

	/**
	 * <p>
	 * Returns all headers.
	 * </p>
	 *
	 * @return a list of header entries or an empty list if there's no header
	 */
	List<Map.Entry<CharSequence, CharSequence>> getAllCharSequence();
}
