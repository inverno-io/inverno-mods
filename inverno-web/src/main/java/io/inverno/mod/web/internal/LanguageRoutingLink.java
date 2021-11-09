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
package io.inverno.mod.web.internal;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.Headers.AcceptLanguage.LanguageRange;
import io.inverno.mod.http.base.header.Headers.AcceptMatch;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.Route;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible to route an exchange based on the language as
 * defined by {@link AcceptAware}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
class LanguageRoutingLink<A extends ExchangeContext, B extends Exchange<A>, C extends AcceptAware & Route<A, B>> extends RoutingLink<A, B, LanguageRoutingLink<A, B, C>, C> {

	private final HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec;

	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, B, ?, C>> handlers;
	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, B, ?, C>> enabledHandlers;

	/**
	 * <p>
	 * Creates a language routing link.
	 * </p>
	 * 
	 * @param acceptLanguageCodec a accept-language header codec
	 */
	public LanguageRoutingLink(HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec) {
		super(() -> new LanguageRoutingLink<>(acceptLanguageCodec));
		this.acceptLanguageCodec = acceptLanguageCodec;
		this.handlers = new LinkedHashMap<>();
		this.enabledHandlers = Map.of();
	}

	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream()
			.filter(e -> !e.getValue().isDisabled())
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	public LanguageRoutingLink<A, B, C> setRoute(C route) {
		String language = route.getLanguage();
		if (language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			if (languageRange.getLanguageTag().equals("*")) {
				this.nextLink.setRoute(route);
			} 
			else if (this.handlers.containsKey(languageRange)) {
				this.handlers.get(languageRange).setRoute(route);
			} 
			else {
				this.handlers.put(languageRange, this.nextLink.createNextLink().setRoute(route));
			}
			this.handlers = this.handlers.entrySet().stream()
					.sorted(Comparator.comparing(Entry::getKey, Headers.AcceptLanguage.LanguageRange.COMPARATOR))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new));
			this.updateEnabledHandlers();
		} 
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}

	@Override
	public void enableRoute(C route) {
		String language = route.getLanguage();
		if (language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(languageRange);
			if (handler != null) {
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
	public void disableRoute(C route) {
		String language = route.getLanguage();
		if (language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(languageRange);
			if (handler != null) {
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
	public void removeRoute(C route) {
		String language = route.getLanguage();
		if (language != null) {
			Headers.AcceptLanguage.LanguageRange languageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, language).getLanguageRanges().get(0);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(languageRange);
			if (handler != null) {
				handler.removeRoute(route);
				if (!handler.hasRoute()) {
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
	public <F extends RouteExtractor<A, B, C>> void extractRoute(F extractor) {
		if (!(extractor instanceof AcceptAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not language aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((AcceptAwareRouteExtractor<A, B, C, ?>) extractor).language(e.getKey().getLanguageTag()));
		});
		super.extractRoute(extractor);
	}
	
	@Override
	public Mono<Void> defer(B exchange) {
		if (this.enabledHandlers.isEmpty()) {
			return this.nextLink.defer(exchange);
		} 
		else {
			Headers.AcceptLanguage acceptLanguage = Headers.AcceptLanguage
				.merge(exchange.request().headers().<Headers.AcceptLanguage>getAllHeader(Headers.NAME_ACCEPT_LANGUAGE))
				.orElse(Headers.AcceptLanguage.ALL);

			Iterator<AcceptMatch<LanguageRange, Entry<LanguageRange, RoutingLink<A, B, ?, C>>>> acceptLanguageMatchesIterator = acceptLanguage
				.findAllMatch(this.handlers.entrySet(), Entry::getKey).iterator();

			while (acceptLanguageMatchesIterator.hasNext()) {
				AcceptMatch<LanguageRange, Entry<LanguageRange, RoutingLink<A, B, ?, C>>> bestMatch = acceptLanguageMatchesIterator.next();
				if (bestMatch.getSource().getLanguageTag().equals("*")) {
					// First check if the next link can handle the request since this is the default
					try {
						return this.nextLink.defer(exchange);
					} 
					catch (RouteNotFoundException | DisabledRouteException e1) {
						// There's no default handler defined, we can take the best match
						try {
							return bestMatch.getTarget().getValue().defer(exchange);
						} 
						catch (RouteNotFoundException | DisabledRouteException e2) {
							// continue with the next best match
							continue;
						}
					}
				} 
				else {
					try {
						return bestMatch.getTarget().getValue().defer(exchange);
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
			return this.nextLink.defer(exchange);
		}
	}
}
