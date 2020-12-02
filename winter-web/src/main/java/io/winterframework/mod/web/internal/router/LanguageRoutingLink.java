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
package io.winterframework.mod.web.internal.router;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.Headers.AcceptLanguage.LanguageRange;
import io.winterframework.mod.web.Headers.AcceptMatch;
import io.winterframework.mod.web.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.web.router.AcceptAwareRoute;

/**
 * @author jkuhn
 *
 */
class LanguageRoutingLink<A, B, C extends Exchange<A, B>, D extends AcceptAwareRoute<A, B, C>> extends RoutingLink<A, B, C, LanguageRoutingLink<A, B, C, D>, D> {

	private AcceptLanguageCodec acceptLanguageCodec;
	
	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, B, C, ?, D>> handlers;
	
	public LanguageRoutingLink(AcceptLanguageCodec acceptLanguageCodec) {
		super(() -> new LanguageRoutingLink<>(acceptLanguageCodec));
		this.acceptLanguageCodec = acceptLanguageCodec;
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public LanguageRoutingLink<A, B, C, D> addRoute(D route) {
		Set<String> languages = route.getLanguages();
		if(languages != null && !languages.isEmpty()) {
			languages.stream()
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
	public void removeRoute(D route) {
		Set<String> languages = route.getLanguages();
		if(languages != null && !languages.isEmpty()) {
			// We can only remove single route
			if(languages.size() > 1) {
				throw new IllegalArgumentException("Multiple accept languages found in route, can only remove a single route");
			}
			String language = languages.iterator().next();
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(languageRange);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good 
					this.handlers.remove(languageRange);
				}
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.removeRoute(route);
		}
	}
	
	@Override
	public boolean hasRoute() {
		return !this.handlers.isEmpty() || this.nextLink.hasRoute();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <F extends RouteExtractor<A, B, C, D>> void extractRoute(F extractor) {
		if(!(extractor instanceof AcceptAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not language aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((AcceptAwareRouteExtractor<A, B, C, D, ?>)extractor).language(e.getKey().getLanguageTag()));
		});
		super.extractRoute(extractor);
	}
	
	@Override
	public void handle(C exchange) throws WebException {
		if(!this.handlers.isEmpty()) {
			Iterator<AcceptMatch<LanguageRange, Entry<LanguageRange, RoutingLink<A, B, C, ?, D>>>> acceptMatchesIterator = Headers.AcceptLanguage.merge(exchange.request().headers().<Headers.AcceptLanguage>getAll(Headers.ACCEPT_LANGUAGE))
				.orElse(this.acceptLanguageCodec.decode(Headers.ACCEPT_LANGUAGE, "*"))
				.findAllMatch(this.handlers.entrySet(), Entry::getKey)
				.iterator();
			
			while(acceptMatchesIterator.hasNext()) {
				AcceptMatch<LanguageRange, Entry<LanguageRange, RoutingLink<A, B, C, ?, D>>> bestMatch = acceptMatchesIterator.next();
				if(bestMatch.getSource().getLanguageTag().equals("*")) {
					// First check if the next link can handle the request since this is the default
					try {
						this.nextLink.handle(exchange);
						return;
					}
					catch(NotFoundException e) {
						// There's no default handler defined, we can take the best match
						try {
							bestMatch.getTarget().getValue().handle(exchange);
							return;
						} 
						catch (NotFoundException e1) {
							// continue with the next best match
							continue;
						}
					}
				}
				else {
					try {
						bestMatch.getTarget().getValue().handle(exchange);
						return;
					} 
					catch (WebException e) {
						// continue with the next best match
						continue;
					}
				}
			}
			this.nextLink.handle(exchange);
			
//			Headers.AcceptLanguage.merge(exchange.request().headers().<Headers.AcceptLanguage>getAll(Headers.ACCEPT_LANGUAGE))
//				.orElse(this.acceptLanguageCodec.decode(Headers.ACCEPT_LANGUAGE, "*"))
//				.findBestMatch(this.handlers.entrySet(), Entry::getKey)
//				.ifPresentOrElse(
//					bestMatch -> {
//						if(bestMatch.getSource().getLanguageTag().equals("*")) {
//							try {
//								this.nextLink.handle(exchange);
//							}
//							catch(NotFoundException e) {
//								// There's no default handler defined
//								bestMatch.getTarget().getValue().handle(exchange);
//							}
//						}
//						else {
//							bestMatch.getTarget().getValue().handle(exchange);
//						}
//					},
//					() -> {
//						this.nextLink.handle(exchange);
//					});
		}
		else {
			this.nextLink.handle(exchange);
		}
	}
}
