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
package io.winterframework.mod.web.internal.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.winterframework.mod.http.base.header.SetCookie;
import io.winterframework.mod.http.server.ResponseCookies;

/**
 * @author jkuhn
 *
 */
public class MockResponseCookies implements ResponseCookies {

	private final Map<String, MockSetCookie> responseCookies;
	
	public MockResponseCookies() {
		this.responseCookies = new HashMap<>();
	}
	
	public Map<String, MockSetCookie> getCookies() {
		return responseCookies;
	}
	
	@Override
	public ResponseCookies addCookie(Consumer<SetCookie.Configurator> configurer) {
		MockSetCookie setCookie = new MockSetCookie();
		configurer.accept(setCookie);
		this.responseCookies.put(setCookie.getName(), setCookie);
		return this;
	}

	public static class MockSetCookie implements SetCookie, SetCookie.Configurator {

		private String name;
		private String value;
		private Integer maxAge;
		private String domain;
		private String path;
		private Boolean secure;
		private Boolean httpOnly;
		
		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public String getValue() {
			return this.value;
		}

		@Override
		public Configurator name(String name) {
			this.name = name;
			return this;
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
	}
}
