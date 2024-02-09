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
package io.inverno.mod.web.server.internal.mock;

import io.inverno.mod.http.base.OutboundSetCookies;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.SetCookie;
import io.inverno.mod.http.base.header.SetCookieParameter;
import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MockResponseCookies implements OutboundSetCookies {

	private final Map<String, MockSetCookie> responseCookies;
	
	public MockResponseCookies() {
		this.responseCookies = new HashMap<>();
	}
	
	public Map<String, MockSetCookie> getCookies() {
		return responseCookies;
	}

	@Override
	public OutboundSetCookies addCookie(Consumer<SetCookie.Configurator> configurer) {
		MockSetCookie setCookie = new MockSetCookie();
		configurer.accept(setCookie);
		this.responseCookies.put(setCookie.getName(), setCookie);
		return this;
	}

	@Override
	public boolean contains(String name) {
		return this.responseCookies.containsKey(name);
	}

	@Override
	public Set<String> getNames() {
		return this.responseCookies.keySet();
	}

	@Override
	public Optional<SetCookieParameter> get(String name) {
		return Optional.ofNullable(this.responseCookies.get(name));
	}

	@Override
	public List<SetCookieParameter> getAll(String name) {
		return this.responseCookies.containsKey(name) ? List.of(this.responseCookies.get(name)) : List.of();
	}

	@Override
	public Map<String, List<SetCookieParameter>> getAll() {
		return this.responseCookies.values().stream().map(mockSetCookie -> (SetCookieParameter)mockSetCookie).collect(Collectors.groupingBy(SetCookieParameter::getName));
	}

	public static class MockSetCookie implements SetCookieParameter, SetCookie.Configurator {

		private String name;
		private String value;
		private ZonedDateTime expires;
		private Integer maxAge;
		private String domain;
		private String path;
		private Boolean secure;
		private Boolean httpOnly;
		private Headers.SetCookie.SameSitePolicy sameSite;
		
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
		public Configurator expires(ZonedDateTime expires) {
			this.expires = expires;
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
		public Configurator sameSite(Headers.SetCookie.SameSitePolicy sameSite) {
			this.sameSite = sameSite;
			return this;
		}
		
		@Override
		public ZonedDateTime getExpires() {
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
		public Headers.SetCookie.SameSitePolicy getSameSite() {
			return this.sameSite;
		}

		@Override
		public <T> T as(Class<T> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T as(Type type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] asArrayOf(Class<T> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> T[] asArrayOf(Type type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> List<T> asListOf(Class<T> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> List<T> asListOf(Type type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Set<T> asSetOf(Class<T> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> Set<T> asSetOf(Type type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Byte asByte() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Short asShort() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Integer asInteger() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Long asLong() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Float asFloat() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Double asDouble() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Character asCharacter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String asString() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Boolean asBoolean() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BigInteger asBigInteger() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BigDecimal asBigDecimal() {
			throw new UnsupportedOperationException();
		}

		@Override
		public LocalDate asLocalDate() {
			throw new UnsupportedOperationException();
		}

		@Override
		public LocalDateTime asLocalDateTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public ZonedDateTime asZonedDateTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Currency asCurrency() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Locale asLocale() {
			throw new UnsupportedOperationException();
		}

		@Override
		public File asFile() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Path asPath() {
			throw new UnsupportedOperationException();
		}

		@Override
		public URI asURI() {
			throw new UnsupportedOperationException();
		}

		@Override
		public URL asURL() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Pattern asPattern() {
			throw new UnsupportedOperationException();
		}

		@Override
		public InetAddress asInetAddress() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class<?> asClass() {
			throw new UnsupportedOperationException();
		}
	}
}
