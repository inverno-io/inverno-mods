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

import io.winterframework.mod.base.net.URIs.Option;

/**
 * <p>
 * URI flags providing bindings to the options specified when creating a URI
 * builder.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see URIs.Option
 * @see URIBuilder
 */
class URIFlags {

	private boolean normalized;

	private boolean parameterized;

	/**
	 * <p>
	 * Creates URI flags from the specified list of options.
	 * </p>
	 * 
	 * @param options a list of options
	 */
	public URIFlags(URIs.Option... options) {
		for (URIs.Option option : options) {
			switch (option) {
			case NORMALIZED:
				this.normalized = true;
				break;
			case PARAMETERIZED:
				this.parameterized = true;
				break;
			default:
				throw new IllegalArgumentException("Unsupported option: " + option);
			}
		}
	}

	/**
	 * <p>
	 * Returns when the {@link Option#NORMALIZED} was specified.
	 * 
	 * @return true if the normalized option is enabled, false otherwise
	 */
	public boolean isNormalized() {
		return normalized;
	}

	/**
	 * <p>
	 * Returns when the {@link Option#PARAMETERIZED} was specified.
	 * 
	 * @return true if the parameterized option is enabled, false otherwise
	 */
	public boolean isParameterized() {
		return parameterized;
	}
}
