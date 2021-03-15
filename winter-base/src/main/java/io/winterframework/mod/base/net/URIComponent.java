/*
 * Copyright 2021 Jeremy KUHN
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
package io.winterframework.mod.base.net;

import java.util.List;

/**
 * <p>
 * A URI component is a part of a URI as defined by
 * <a href="https://tools.ietf.org/html/rfc3986#section-3">RFC 3986 Section
 * 3</a>.
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
interface URIComponent {

	/**
	 * <p>
	 * Returns the raw representation of the component.
	 * </p>
	 * 
	 * @return the component raw value
	 */
	String getRawValue();

	/**
	 * <p>
	 * Returns the value of the component.
	 * </p>
	 * 
	 * <p>
	 * Note that the resulting value is percent encoded as defined by
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2.1">RFC 3986 Section
	 * 2.1</a>.
	 * </p>
	 * 
	 * @return the component value
	 */
	String getValue();

	/**
	 * <p>
	 * Returns the regex matching the component.
	 * </p>
	 * 
	 * @return the component regex
	 */
	String getPattern();

	/**
	 * <p>
	 * Returns the names of the named groups matched in the component's regex.
	 * </p>
	 * 
	 * @return a list of group names or an empty list
	 */
	List<String> getPatternGroupNames();

	/**
	 * <p>
	 * Determines whether the component is empty (ie. has an empty value).
	 * </p>
	 * 
	 * @return true if the component's value is empty, false otherwise
	 */
	boolean isPresent();
}
