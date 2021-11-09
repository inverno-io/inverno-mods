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
package io.inverno.mod.base.net;

/**
 * <p>
 * Represents a common part in a URI component like a URI parameter or a static part.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
interface URIComponentPart {
	
	/**
	 * <p>
	 * Return true if this represents a static part.
	 * </p>
	 * 
	 * @return true if this is a static part, false otherwise
	 */
	boolean isStatic();
	
	/**
	 * <p>
	 * Return true if this represents a question mark part as described in {@link URIs.Option#PATH_PATTERN}.
	 * </p>
	 * 
	 * @return true if this is a question mark part, false otherwise
	 */
	boolean isQuestionMark();
	
	/**
	 * <p>
	 * Return true if this represents a wildcard part as described in {@link URIs.Option#PATH_PATTERN}.
	 * </p>
	 * 
	 * @return true if this is a wildcard part, false otherwise
	 */
	boolean isWildcard();
	
	/**
	 * <p>
	 * Return true if this represents a custom part defined with a custom pattern.
	 * </p>
	 * 
	 * @return true if this is a custom part, false otherwise
	 */
	boolean isCustom();
	
	/**
	 * <p>
	 * Returns the raw value of the part.
	 * </p>
	 * 
	 * @return a value
	 */
	String getValue();
}
