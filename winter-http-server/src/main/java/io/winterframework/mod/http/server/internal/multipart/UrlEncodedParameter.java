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
package io.winterframework.mod.http.server.internal.multipart;

import io.winterframework.mod.base.converter.ObjectConverter;
import io.winterframework.mod.http.base.Parameter;
import io.winterframework.mod.http.base.internal.GenericParameter;

/**
 * <p>
 * URL encoded {@link Parameter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class UrlEncodedParameter extends GenericParameter {

	private boolean partial;
	
	private boolean last;
	
	/**
	 * <p>
	 * Creates a URL encoded parameter.
	 * </p>
	 * 
	 * @param parameterConverter a string object converter
	 * @param name               the parameter name
	 * @param value              the parameter value
	 * @param partial            true to indicate a partial parameter, false
	 *                           otherwise
	 * @param last               true if the parameter is the last parameter in the
	 *                           payload
	 */
	public UrlEncodedParameter(ObjectConverter<String> parameterConverter, String name, String value, boolean partial, boolean last) {
		super(name, value, parameterConverter);
		this.partial = partial;
		this.last = last;
	}

	/**
	 * <p>
	 * Determines whether the parameter is complete.
	 * </p>
	 * 
	 * @return true if the parameter is not complete, false otherwise
	 */
	public boolean isPartial() {
		return partial;
	}

	/**
	 * <p>
	 * Indicates whether the parameter is complete.
	 * </p>
	 * 
	 * @param partial true if the parameter is partial, false otherwise
	 */
	public void setPartial(boolean partial) {
		this.partial = partial;
	}

	/**
	 * <p>
	 * Determines whether the parameter is the last parameter in the payload.
	 * </p>
	 * 
	 * @return true if the parameter is the last, false otherwise
	 */
	public boolean isLast() {
		return last;
	}

	/**
	 * <p>
	 * Indicates whether the parameter is the last parameter in the payload.
	 * </p>
	 * 
	 * @param last true if the parameter is the last, false otherwise
	 */
	public void setLast(boolean last) {
		this.last = last;
	}
}
