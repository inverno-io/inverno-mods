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

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.spi.ContentAware;
import io.inverno.mod.web.spi.Route;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible to route an exchange based on the content type of the request as defined by {@link ContentAware}.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
class ConsumesRoutingLink<A extends ExchangeContext, B extends Exchange<A>, C extends ContentAware & Route<A, B>> extends RoutingLink<A, B, ConsumesRoutingLink<A, B, C>, C> {

	private final HeaderCodec<? extends Headers.Accept> acceptCodec;

	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, ?, C>> handlers;
	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, ?, C>> enabledHandlers;

	/**
	 * <p>
	 * creates a consumes routing link.
	 * </p>
	 * 
	 * @param acceptCodec an accept header codec
	 */
	public ConsumesRoutingLink(HeaderCodec<? extends Headers.Accept> acceptCodec) {
		super(() -> new ConsumesRoutingLink<>(acceptCodec));
		this.acceptCodec = acceptCodec;
		this.handlers = new LinkedHashMap<>();
		this.enabledHandlers = Map.of();
	}
	
	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream()
			.filter(e -> !e.getValue().isDisabled())
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	public ConsumesRoutingLink<A, B, C> setRoute(C route) {
		// Note if someone defines a route with a GET like method and a consumed media
		// type, consumes will be ignored because such request does not provide content
		// types headers
		String consume = route.getConsume();
		if (consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			if(this.handlers.containsKey(mediaRange)) {
				this.handlers.get(mediaRange).setRoute(route);
			}
			else {
				this.handlers.put(mediaRange, this.nextLink.createNextLink().setRoute(route));
			}
			this.handlers = this.handlers.entrySet().stream()
				.sorted(Comparator.comparing(Entry::getKey, Headers.Accept.MediaRange.COMPARATOR))
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
		String consume = route.getConsume();
		if(consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(mediaRange);
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
	public void disableRoute(C route) {
		String consume = route.getConsume();
		if(consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(mediaRange);
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
	public void removeRoute(C route) {
		String consume = route.getConsume();
		if(consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(mediaRange);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good
					this.handlers.remove(mediaRange);
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
		if(!(extractor instanceof ContentAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not content aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((ContentAwareRouteExtractor<A, B, C, ?>) extractor).consumes(e.getKey().getMediaType()));
		});
		super.extractRoute(extractor);
	}

	@Override
	public Mono<Void> defer(B exchange) {
		if(this.enabledHandlers.isEmpty()) {
			return this.nextLink.defer(exchange);
		} 
		else {
			Optional<Headers.ContentType> contentTypeHeader = exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE);

			Optional<RoutingLink<A, B, ?, C>> handler = contentTypeHeader
				.flatMap(contentType -> Headers.Accept.MediaRange
					.findFirstMatch(contentType, this.enabledHandlers.entrySet(), Entry::getKey)
					.map(Headers.AcceptMatch::getSource)
					.map(Entry::getValue)
				);

			if(handler.isPresent()) {
				return handler.get().defer(exchange);
			} 
			else if(!contentTypeHeader.isPresent()) {
				return this.nextLink.defer(exchange);
			}
			else {
				throw new UnsupportedMediaTypeException();
			}
		}
	}
}
