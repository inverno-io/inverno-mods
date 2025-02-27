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

package io.inverno.mod.http.base.internal.header;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.SetCookieParameter;
import io.inverno.mod.http.base.internal.GenericParameter;
import java.time.ZonedDateTime;

/**
 * <p>
 * Generic {@link SetCookieParameter} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 * 
 * @see GenericParameter
 */
public class GenericSetCookieParameter extends GenericParameter implements SetCookieParameter {

	private final Headers.SetCookie setCookieHeader;
	
	/**
	 * <p>
	 * Creates a generic set cookie parameter.
	 * </p>
	 * 
	 * @param setCookieHeader    a set cookie header
	 * @param parameterConverter a string object parameter
	 */
	public GenericSetCookieParameter(Headers.SetCookie setCookieHeader, ObjectConverter<String> parameterConverter) {
		super(setCookieHeader.getName(), setCookieHeader.getValue(), parameterConverter);
		this.setCookieHeader = setCookieHeader;
	}

	@Override
	public ZonedDateTime getExpires() {
		return this.setCookieHeader.getExpires();
	}

	@Override
	public Integer getMaxAge() {
		return this.setCookieHeader.getMaxAge();
	}

	@Override
	public String getDomain() {
		return this.setCookieHeader.getDomain();
	}

	@Override
	public String getPath() {
		return this.setCookieHeader.getPath();
	}

	@Override
	public Boolean isSecure() {
		return this.setCookieHeader.isSecure();
	}

	@Override
	public Boolean isHttpOnly() {
		return this.setCookieHeader.isHttpOnly();
	}

	@Override
	public Boolean isPartitioned() {
		return this.setCookieHeader.isPartitioned();
	}

	@Override
	public Headers.SetCookie.SameSitePolicy getSameSite() {
		return this.setCookieHeader.getSameSite();
	}
}
