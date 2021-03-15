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
package io.winterframework.mod.http.base.internal.header;

import java.util.Map;
import java.util.Set;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.http.base.header.HeaderBuilder;
import io.winterframework.mod.http.base.header.HeaderCodec;
import io.winterframework.mod.http.base.header.Headers;

/**
 * <p>
 * Set-cookie HTTP {@link HeaderCodec} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see ParameterizedHeaderCodec
 */
@Bean(visibility = Visibility.PRIVATE)
public class SetCookieCodec extends ParameterizedHeaderCodec<SetCookieCodec.SetCookie, SetCookieCodec.SetCookie.Builder> {

	/**
	 * <p>
	 * Creates a set-cookie header codec.
	 * </p>
	 */
	public SetCookieCodec() {
		super(SetCookieCodec.SetCookie.Builder::new, Set.of(Headers.NAME_SET_COOKIE), DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, true, true, true, true, false, false);
	}
	
	@Override
	public String encodeValue(SetCookie headerField) {
		StringBuilder result = new StringBuilder();
		
		result.append(headerField.getName()).append("=").append(headerField.getValue());
		if(headerField.getExpires() != null) {
			result.append("; ").append(Headers.SetCookie.EXPIRES).append("=").append(headerField.getExpires());
		}
		if(headerField.getMaxAge() != null) {
			result.append("; ").append(Headers.SetCookie.MAX_AGE).append("=").append(headerField.getMaxAge());
		}
		if(headerField.getDomain() != null) {
			result.append("; ").append(Headers.SetCookie.DOMAIN).append("=").append(headerField.getDomain());
		}
		if(headerField.getPath() != null) {
			result.append("; ").append(Headers.SetCookie.PATH).append("=").append(headerField.getPath());
		}
		if(headerField.isSecure() != null && headerField.isSecure()) {
			result.append("; ").append(Headers.SetCookie.SECURE);
		}
		if(headerField.isHttpOnly() != null && headerField.isHttpOnly()) {
			result.append("; ").append(Headers.SetCookie.HTTPONLY);
		}
		return result.toString();
	}

	/**
	 * <p>
	 * {@link Headers.SetCookie} header implemetation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
	 * @since 1.0
	 * 
	 * @see ParameterizedHeader
	 */
	public static final class SetCookie extends ParameterizedHeader implements Headers.SetCookie, io.winterframework.mod.http.base.header.SetCookie, io.winterframework.mod.http.base.header.SetCookie.Configurator {
		
		private String name;
		private String value;
		private String expires;
		private Integer maxAge;
		private String domain;
		private String path;
		private Boolean secure;
		private Boolean httpOnly;
		
		/**
		 * <p>Creates an empty set-cookie header.</p>
		 */
		public SetCookie() {
			super(Headers.NAME_SET_COOKIE, null, null, null);
		}
		
		private SetCookie(String headerName, String headerValue, String name, String value, String expires, Integer maxAge, String domain, String path, Boolean secure, Boolean httpOnly, Map<String, String> parameters) {
			super(Headers.NAME_SET_COOKIE, headerValue, null, parameters);
			
			this.name = name;
			this.value = value;
			this.expires = expires;
			this.maxAge = maxAge;
			this.domain = domain;
			this.path = path;
			this.secure = secure;
			this.httpOnly = httpOnly;
		}
		
		@Override
		public String getName() {
			return this.name;
		}
		
		@Override
		public String getValue() {
			return this.value;
		}
		
		@Override
		public String getExpires() {
			return this.expires;
		}

		@Override
		public Integer getMaxAge() {
			return this.maxAge;
		}

		@Override
		public String getDomain() {
			return this.domain;
		}

		@Override
		public String getPath() {
			return this.path;
		}

		@Override
		public Boolean isSecure() {
			return this.secure;
		}

		@Override
		public Boolean isHttpOnly() {
			return this.httpOnly;
		}
		
		@Override
		public Configurator name(String name) {
			this.name = name;
			return null;
		}

		@Override
		public Configurator value(String value) {
			this.value = value;
			return this;
		}

		@Override
		public Configurator maxAge(int maxAge) {
			this.maxAge = maxAge;
			return this;
		}

		@Override
		public Configurator domain(String domain) {
			this.domain = domain;
			return this;
		}

		@Override
		public Configurator path(String path) {
			this.path = path;
			return this;
		}

		@Override
		public Configurator secure(boolean secure) {
			this.secure = secure;
			return this;
		}

		@Override
		public Configurator httpOnly(boolean httpOnly) {
			this.httpOnly = httpOnly;
			return this;
		}

		/**
		 * <p>
		 * Set-Cookie {@link HeaderBuilder} implementation.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
		 * @since 1.0
		 * 
		 * @see ParameterizedHeader.AbstractBuilder
		 */
		public static final class Builder extends ParameterizedHeader.AbstractBuilder<SetCookie, Builder> {

			private String name;
			private String value;
			private String expires;
			private Integer maxAge;
			private String domain;
			private String path;
			private Boolean secure;
			private Boolean httpOnly;
			
			private boolean expectCookiePair = true;
			
			@Override
			public Builder parameter(String name, String value) {
				if(this.expectCookiePair) {
					this.name = name;
					this.value = value;
					this.expectCookiePair = false;
					return this;
				}
				else {
					if(name.equalsIgnoreCase(EXPIRES)) {
						this.expires = value;
					}
					if(name.equalsIgnoreCase(MAX_AGE)) {
						this.maxAge = Integer.parseInt(value);
					}
					if(name.equalsIgnoreCase(DOMAIN)) {
						this.domain = value;
					}
					if(name.equalsIgnoreCase(PATH)) {
						this.path = value;
					}
					if(name.equalsIgnoreCase(SECURE)) {
						this.secure = true;
					}
					if(name.equalsIgnoreCase(HTTPONLY)) {
						this.httpOnly = true;
					}
					return super.parameter(name, value);
				}
			}
			
			@Override
			public SetCookie build() {
				return new SetCookie(this.headerName, this.headerValue, this.name, this.value, this.expires, this.maxAge, this.domain, this.path, this.secure, this.httpOnly, this.parameters);
			}
		}
	}
}
