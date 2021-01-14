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
package io.winterframework.mod.web.internal.header;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Bean.Visibility;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.internal.server.GenericCookie;

/**
 * @author jkuhn
 *
 */
@Bean(visibility = Visibility.PRIVATE)
public class CookieCodec extends ParameterizedHeaderCodec<CookieCodec.Cookie, CookieCodec.Cookie.Builder> {

	public CookieCodec() {
		super(CookieCodec.Cookie.Builder::new, Set.of(Headers.NAME_COOKIE), DEFAULT_PARAMETER_DELIMITER, DEFAULT_VALUE_DELIMITER, true, true, false, false, false, false);
	}
	
	@Override
	public String encodeValue(Cookie headerField) {
		return headerField.getPairs().values().stream().flatMap(List::stream).map(cookie -> cookie.getName() + "=" + cookie.getValue()).collect(Collectors.joining("; "));
	}
	
	public static final class Cookie extends ParameterizedHeader implements Headers.Cookie {
		
		private Map<String, List<io.winterframework.mod.web.Cookie>> pairs;
		
		private Cookie(String headerName, String headerValue, Map<String, String> parameters, Map<String, List<io.winterframework.mod.web.Cookie>> pairs) {
			super(Headers.NAME_COOKIE, headerValue, null, parameters);
			this.pairs = pairs != null ? Collections.unmodifiableMap(pairs) : Map.of();
		}
		
		@Override
		public Map<String, List<io.winterframework.mod.web.Cookie>> getPairs() {
			return this.pairs;
		}

		public static final class Builder extends ParameterizedHeader.AbstractBuilder<Cookie, Builder> {

			private Map<String, List<io.winterframework.mod.web.Cookie>> pairs;

			@Override
			public Builder parameter(String name, String value) {
				if(this.pairs == null) {
					this.pairs = new HashMap<>();
				}
				if(!this.pairs.containsKey(name)) {
					this.pairs.put(name, new LinkedList<>());
				}
				this.pairs.get(name).add(new GenericCookie(name, value));
				return this;
			}
			
			@Override
			public Cookie build() {
				return new Cookie(this.headerName, this.headerValue, this.parameters, this.pairs);
			}
		}
	}
}
