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
package io.winterframework.mod.web.lab.router.base;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;

/**
 * @author jkuhn
 *
 */
public class LanguageRoutingLink extends RoutingLink<LanguageRoutingLink> {

	private AcceptLanguageCodec acceptLanguageCodec;
	
	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<?>> handlers;
	
	public LanguageRoutingLink(AcceptLanguageCodec acceptLanguageCodec) {
		super(() -> new LanguageRoutingLink(acceptLanguageCodec));
		this.acceptLanguageCodec = acceptLanguageCodec;
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public LanguageRoutingLink addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		if(route.getLanguages() != null && !route.getLanguages().isEmpty()) {
			route.getLanguages().stream()
				.map(language -> this.acceptLanguageCodec.decode(Headers.ACCEPT_LANGUAGE, language).getLanguageRanges().get(0)) // TODO what happens if I have no range? if I have more than one?
				.forEach(languageRange -> {
					if(languageRange.getLanguageTag().equals("*")) {
						this.nextLink.addRoute(route);
					}
					else if(this.handlers.containsKey(languageRange)) {
						this.handlers.get(languageRange).addRoute(route);
					}
					else {
						this.handlers.put(languageRange, this.nextLink.createNextLink().addRoute(route));
					}
				});
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, Headers.AcceptLanguage.LanguageRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}
	
	@Override
	public <B extends RoutingLink<B>> B connect(B nextLink) {
		this.handlers.put(this.acceptLanguageCodec.decode(Headers.ACCEPT_LANGUAGE, "*").getLanguageRanges().get(0), nextLink);
		return super.connect(nextLink);
	}

	@Override
	public void handle(Request<RequestBody, BaseContext> request, Response<ResponseBody> response) {
		Optional<Headers.AcceptLanguage> acceptLanguageHeader = Headers.AcceptLanguage.merge(request.headers().<Headers.AcceptLanguage>getAll(Headers.ACCEPT_LANGUAGE));
		if(acceptLanguageHeader.isPresent()) {
			acceptLanguageHeader
				.flatMap(acceptLanguage -> acceptLanguage.findBestMatch(this.handlers.entrySet(), Entry::getKey).map(Entry::getValue))
				.orElseThrow(() -> new RoutingException(406, "Not Acceptable"))  // This should never happen since we the next link is supposed to match '*'
				.handle(request, response);
		}
		else {
			// client accepts anything so we fallback to the next link which is bound to '*' anyway
			this.nextLink.handle(request, response);
		}
	}
}
