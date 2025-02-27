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
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Represents mutable outbound HTTP headers.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @version 1.6
 * 
 * @param <A> the outbound headers type
 */
public interface OutboundHeaders<A extends OutboundHeaders<A>> extends InboundHeaders {

	/**
	 * <p>
	 * Determines whether the headers have been sent to the recipient.
	 * </p>
	 * 
	 * <p>
	 * Any attempts to specify new headers once headers have been sent, will result in an {@link IllegalStateException} being thrown.
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
	 * @return the outbound headers
	 */
	A add(CharSequence name, CharSequence value);

	/**
	 * <p>
	 * Encodes and adds a header with the specified name and value
	 * </p>
	 *
	 * @param <T>   the value type
	 * @param name  the header name
	 * @param value the header value
	 *
	 * @return the outbound headers
	 */
	<T> A addParameter(CharSequence name, T value);

	/**
	 * <p>
	 * Encodes and adds a header with the specified name and value
	 * </p>
	 *
	 * @param <T>   the value type
	 * @param name  the header name
	 * @param value the header value
	 * @param type  the value type
	 *
	 * @return the outbound headers
	 */
	default <T> A addParameter(CharSequence name, T value, Class<T> type) {
		return this.addParameter(name, value, (Type)type);
	}

	/**
	 * <p>
	 * Encodes and adds a header with the specified name and value
	 * </p>
	 *
	 * @param <T>   the value type
	 * @param name  the header name
	 * @param value the header value
	 * @param type  the value type
	 *
	 * @return the outbound headers
	 */
	<T> A addParameter(CharSequence name, T value, Type type);

	/**
	 * <p>
	 * Adds the specified headers.
	 * </p>
	 * 
	 * @param headers the headers to add
	 * 
	 * @return the outbound headers
	 */
	default A add(Header... headers) {
		return this.add(List.of(headers));
	}

	/**
	 * <p>
	 * Adds the specified headers.
	 * </p>
	 *
	 * @param headers the headers to add
	 *
	 * @return the outbound headers
	 */
	A add(List<? extends Header> headers);

	/**
	 * <p>
	 * Sets the value of the header with the specified name.
	 * </p>
	 * 
	 * @param name  the header name
	 * @param value the header value
	 * 
	 * @return the outbound headers
	 */
	A set(CharSequence name, CharSequence value);

	/**
	 * <p>
	 * Encodes and sets the value of the header with the specified name.
	 * </p>
	 *
	 * @param <T>   the value type
	 * @param name  the header name
	 * @param value the header value
	 *
	 * @return the outbound headers
	 */
	<T> A setParameter(CharSequence name, T value);

	/**
	 * <p>
	 * Encodes and sets the value of the header with the specified name.
	 * </p>
	 *
	 * @param <T>   the value type
	 * @param name  the header name
	 * @param value the header value
	 * @param type  the value type
	 *
	 * @return the outbound headers
	 */
	default <T> A setParameter(CharSequence name, T value, Class<T> type) {
		return this.setParameter(name, value, (Type)type);
	}

	/**
	 * <p>
	 * Encodes and sets the value of the header with the specified name.
	 * </p>
	 *
	 * @param <T>   the value type
	 * @param name  the header name
	 * @param value the header value
	 * @param type  the value type
	 *
	 * @return the outbound headers
	 */
	<T> A setParameter(CharSequence name, T value, Type type);

	/**
	 * <p>
	 * Sets the specified headers.
	 * </p>
	 * 
	 * @param headers the headers to set
	 * 
	 * @return the outbound headers
	 */
	default A set(Header... headers) {
		return this.set(List.of(headers));
	}

	/**
	 * <p>
	 * Sets the specified headers.
	 * </p>
	 *
	 * @param headers the headers to set
	 *
	 * @return the outbound headers
	 */
	A set(List<? extends Header> headers);

	/**
	 * <p>
	 * Removes the headers with the specified names.
	 * </p>
	 * 
	 * @param names the names of the headers to remove
	 * 
	 * @return the outbound headers
	 */
	default A remove(CharSequence... names) {
		return this.remove(Set.of(names));
	}

	/**
	 * <p>
	 * Removes the headers with the specified names.
	 * </p>
	 *
	 * @param names the names of the headers to remove
	 *
	 * @return the outbound headers
	 */
	A remove(Set<? extends CharSequence> names);
}
