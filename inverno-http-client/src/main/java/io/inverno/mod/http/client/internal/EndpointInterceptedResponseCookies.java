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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.OutboundResponseHeaders;
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
 * An {@link OutboundSetCookies} implementation used to specify response cookies in an {@link io.inverno.mod.http.client.ExchangeInterceptor}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.8
 */
public class EndpointInterceptedResponseCookies implements OutboundSetCookies {

	private final HeaderService headerService;
	private final OutboundResponseHeaders responseHeaders;
	private final ObjectConverter<String> parameterConverter;
	
	private Map<String, List<SetCookieParameter>> pairs;
	
	/**
	 * <p>
	 * Creates intercepted response cookies.
	 * </p>
	 *
	 * @param headerService      the header service
	 * @param parameterConverter the parameter converter
	 * @param responseHeaders    the response headers
	 */
	public EndpointInterceptedResponseCookies(HeaderService headerService, ObjectConverter<String> parameterConverter, OutboundResponseHeaders responseHeaders) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.responseHeaders = responseHeaders;
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
					return setCookies.getFirst();
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
