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
package io.winterframework.mod.web.router.internal;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.header.HeaderCodec;
import io.winterframework.mod.web.header.Headers;
import io.winterframework.mod.web.header.Headers.AcceptLanguage.LanguageRange;
import io.winterframework.mod.web.header.Headers.AcceptMatch;
import io.winterframework.mod.web.router.AcceptAwareRoute;
import io.winterframework.mod.web.server.Exchange;

/**
 * @author jkuhn
 *
 */
class LanguageRoutingLink<A extends Exchange, B extends AcceptAwareRoute<A>> extends RoutingLink<A, LanguageRoutingLink<A, B>, B> {

	private final HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec;
	
	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, ?, B>> handlers;
	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, ?, B>> enabledHandlers;
	
	public LanguageRoutingLink(HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec) {
		super(() -> new LanguageRoutingLink<>(acceptLanguageCodec));
		this.acceptLanguageCodec = acceptLanguageCodec;
		this.handlers = new LinkedHashMap<>();
		this.enabledHandlers = Map.of();
	}
	
	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream().filter(e -> !e.getValue().isDisabled()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
	}

	@Override
	public LanguageRoutingLink<A, B> setRoute(B route) {
		String language = route.getLanguage();
		if(language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			if(languageRange.getLanguageTag().equals("*")) {
				this.nextLink.setRoute(route);
			}
			else if(this.handlers.containsKey(languageRange)) {
				this.handlers.get(languageRange).setRoute(route);
			}
			else {
				this.handlers.put(languageRange, this.nextLink.createNextLink().setRoute(route));
			}
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, Headers.AcceptLanguage.LanguageRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
			this.updateEnabledHandlers();
		}
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}
	
	@Override
	public void enableRoute(B route) {
		String language = route.getLanguage();
		if(language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			RoutingLink<A, ?, B> handler = this.handlers.get(languageRange);
			if(handler != null) {
				handler.enableRoute(route);
				this.updateEnabledHandlers();
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.enableRoute(route);
		}
	}
	
	@Override
	public void disableRoute(B route) {
		String language = route.getLanguage();
		if(language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			RoutingLink<A, ?, B> handler = this.handlers.get(languageRange);
			if(handler != null) {
				handler.disableRoute(route);
				this.updateEnabledHandlers();
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.disableRoute(route);
		}
	}
	
	@Override
	public void removeRoute(B route) {
		String language = route.getLanguage();
		if(language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			RoutingLink<A, ?, B> handler = this.handlers.get(languageRange);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good 
					this.handlers.remove(languageRange);
					this.updateEnabledHandlers();
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
	
	@Override
	public boolean isDisabled() {
		return this.handlers.values().stream().allMatch(RoutingLink::isDisabled) && this.nextLink.isDisabled();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <F extends RouteExtractor<A, B>> void extractRoute(F extractor) {
		if(!(extractor instanceof AcceptAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not language aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((AcceptAwareRouteExtractor<A, B, ?>)extractor).language(e.getKey().getLanguageTag()));
		});
		super.extractRoute(extractor);
	}
	
	@Override
	public void handle(A exchange) throws WebException {
		if(this.enabledHandlers.isEmpty()) {
			this.nextLink.handle(exchange);
		}
		else {
			Headers.AcceptLanguage acceptLanguage = Headers.AcceptLanguage.merge(exchange.request().headers().<Headers.AcceptLanguage>getAllHeader(Headers.NAME_ACCEPT_LANGUAGE))
				.orElse(this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, "*"));
			
			Iterator<AcceptMatch<LanguageRange, Entry<LanguageRange, RoutingLink<A, ?, B>>>> acceptLanguageMatchesIterator = acceptLanguage
				.findAllMatch(this.handlers.entrySet(), Entry::getKey)
				.iterator();
			
			while(acceptLanguageMatchesIterator.hasNext()) {
				AcceptMatch<LanguageRange, Entry<LanguageRange, RoutingLink<A, ?, B>>> bestMatch = acceptLanguageMatchesIterator.next();
				if(bestMatch.getSource().getLanguageTag().equals("*")) {
					// First check if the next link can handle the request since this is the default
					try {
						this.nextLink.handle(exchange);
						return;
					}
					catch(RouteNotFoundException | DisabledRouteException e1) {
						// There's no default handler defined, we can take the best match
						try {
							bestMatch.getTarget().getValue().handle(exchange);
							return;
						} 
						catch (RouteNotFoundException | DisabledRouteException e2) {
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
					catch (RouteNotFoundException | DisabledRouteException e) {
						// continue with the next best match
						continue;
					}
				}
			}
			// We havent't found a matching language so we default to the next link which
			// can throw a RouteNorFoundException.
			// User has to explicitly define a default handler
			// Here we can possibly invoke the next link twice but in that case a
			// RouteNotFoundException or DisabledRouteException is thrown so there shouldn't
			// be complex processing involved.
			this.nextLink.handle(exchange);
		}
	}
}
