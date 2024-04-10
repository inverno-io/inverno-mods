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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundSetCookies;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.SetCookie;
import io.inverno.mod.http.base.header.SetCookieParameter;
import io.inverno.mod.http.base.internal.header.GenericSetCookieParameter;
import io.inverno.mod.http.base.internal.header.SetCookieCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Generic {@link OutboundSetCookies} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericResponseCookies implements OutboundSetCookies {

	private final HeaderService headerService;
	private final InternalResponseHeaders responseHeaders;
	private final ObjectConverter<String> parameterConverter;
	
	private Map<String, List<SetCookieParameter>> pairs;
	
	/**
	 * <p>
	 * Creates response cookies with the specified header service and response headers.
	 * </p>
	 *
	 * @param headerService   the header service
	 * @param responseHeaders the response headers
	 */
	public GenericResponseCookies(HeaderService headerService, InternalResponseHeaders responseHeaders, ObjectConverter<String> parameterConverter) {
		this.headerService = headerService;
		this.responseHeaders = responseHeaders;
		this.parameterConverter = parameterConverter;
	}

	@Override
	public OutboundSetCookies addCookie(Consumer<SetCookie.Configurator> configurer) {
		if(configurer != null) {
			SetCookieCodec.SetCookie setCookie = new SetCookieCodec.SetCookie();
			configurer.accept(setCookie);
			setCookie.setHeaderValue(this.headerService.encodeValue(setCookie));
			this.responseHeaders.add(setCookie);
			if(this.pairs != null) {
				this.pairs.computeIfAbsent(setCookie.getName(), ign -> new ArrayList<>()).add(new GenericSetCookieParameter(setCookie, this.parameterConverter));
			}
		}
		return this;
	}

	@Override
	public boolean contains(String name) {
		return this.getAll().containsKey(name);
	}

	@Override
	public Set<String> getNames() {
		return this.getAll().keySet();
	}

	@Override
	public Optional<SetCookieParameter> get(String name) {
		return Optional.ofNullable(this.getAll().get(name))
			.map(setCookies -> {
				if(!setCookies.isEmpty()) {
					return setCookies.get(0);
				}
				return null;
			});
	}

	@Override
	public List<SetCookieParameter> getAll(String name) {
		List<SetCookieParameter> setCookiePairs = this.pairs.get(name);
		return setCookiePairs != null ? setCookiePairs : List.of();
	}

	@Override
	public Map<String, List<SetCookieParameter>> getAll() {
		if(this.pairs == null) {
			this.pairs = new HashMap<>();
			for(Headers.SetCookie setCookie : this.responseHeaders.<Headers.SetCookie>getAllHeader(Headers.NAME_SET_COOKIE)) {
				this.pairs.computeIfAbsent(setCookie.getName(), ign -> new ArrayList<>()).add(new GenericSetCookieParameter(setCookie, this.parameterConverter));
			}
		}
		return this.pairs;
	}
}
