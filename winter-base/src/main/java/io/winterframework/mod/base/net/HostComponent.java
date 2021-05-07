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

import java.nio.charset.Charset;

/**
 * <p>
 * A URI component representing the host part of an URI as defined by
 * <a href="https://tools.ietf.org/html/rfc3986#section-3.2.2">RFC 3986 Section
 * 3.2.2</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedURIComponent
 */
// TODO validate host value
class HostComponent extends AbstractParameterizedURIComponent {
	
	/**
	 * <p>
	 * Creates a host component with the specified flags, charset and raw value.
	 * </p>
	 * 
	 * @param flags    URI flags
	 * @param charset  a charset
	 * @param rawValue a raw value
	 */
	public HostComponent(URIFlags flags, Charset charset, String rawValue) {
		super(flags, charset, rawValue);
	}
}