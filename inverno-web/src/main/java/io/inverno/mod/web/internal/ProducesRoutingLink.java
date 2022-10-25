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
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.Route;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible to route an exchange based on the content types
 * accepted by the client as defined by {@link AcceptAware}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
class ProducesRoutingLink<A extends ExchangeContext, B extends Exchange<A>, C extends AcceptAware & Route<A, B>> extends RoutingLink<A, B, ProducesRoutingLink<A, B, C>, C> {

	private final HeaderCodec<? extends Headers.ContentType> contentTypeCodec;

	private Map<Headers.ContentType, RoutingLink<A, B, ?, C>> handlers;
	private Map<Headers.ContentType, RoutingLink<A, B, ?, C>> enabledHandlers;
	
	/**
	 * <p>
	 * Creates a produces routing link.
	 * </p>
	 * 
	 * @param contentTypeCodec a content type header codec
	 */
	public ProducesRoutingLink(HeaderCodec<? extends Headers.ContentType> contentTypeCodec) {
		super(() -> new ProducesRoutingLink<>(contentTypeCodec));
		this.contentTypeCodec = contentTypeCodec;
		this.handlers = new LinkedHashMap<>();
		this.enabledHandlers = Map.of();
	}

	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream()
			.filter(e -> !e.getValue().isDisabled())
			.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	public ProducesRoutingLink<A, B, C> setRoute(C route) {
		String produce = route.getProduce();
		if (produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, produce);
			if (this.handlers.containsKey(contentType)) {
				this.handlers.get(contentType).setRoute(route);
			} 
			else {
				this.handlers.put(contentType, this.nextLink.createNextLink().setRoute(route));
			}
			this.handlers = this.handlers.entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getKey().toMediaRange(), Headers.Accept.MediaRange.COMPARATOR))
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
		String produce = route.getProduce();
		if (produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, produce);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(contentType);
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
		String produce = route.getProduce();
		if (produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, produce);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(contentType);
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
		String produce = route.getProduce();
		if (produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, produce);
			RoutingLink<A, B, ?, C> handler = this.handlers.get(contentType);
			if (handler != null) {
				handler.removeRoute(route);
				if (!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good
					this.handlers.remove(contentType);
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
		super.extractRoute(extractor);
		if (!(extractor instanceof AcceptAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not accept aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((AcceptAwareRouteExtractor<A, B, C, ?>) extractor).produces(e.getKey().getMediaType()));
		});
	}

	@Override
	public Mono<Void> defer(B exchange) {
		if (this.enabledHandlers.isEmpty()) {
			return this.nextLink.defer(exchange);
		} 
		else {
			Headers.Accept accept = Headers.Accept
				.merge(exchange.request().headers().<Headers.Accept>getAllHeader(Headers.NAME_ACCEPT))
				.orElse(Headers.Accept.ALL);

			Iterator<Headers.AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, ?, C>>>> acceptMatchesIterator = accept
				.findAllMatch(this.enabledHandlers.entrySet(), Entry::getKey).iterator();

			boolean nextLinkInvoked = false;
			while (acceptMatchesIterator.hasNext()) {
				Headers.AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, ?, C>>> bestMatch = acceptMatchesIterator.next();
				if (!nextLinkInvoked && bestMatch.getSource().getMediaType().equals("*/*") && bestMatch.getSource().getParameters().isEmpty()) {
					nextLinkInvoked = true;
					// First check if the next link can handle the request since this is the default
					try {
						return this.nextLink.defer(exchange);
					} 
					catch (RouteNotFoundException | DisabledRouteException e1) {
						// There's no default handler defined, we can take the best match
						try {
							((OutboundResponseHeaders)exchange.response().headers()).set(bestMatch.getTarget().getKey());
							return bestMatch.getTarget().getValue().defer(exchange);
						} 
						catch (RouteNotFoundException | DisabledRouteException e2) {
							// continue with the next best match
							((OutboundResponseHeaders)exchange.response().headers()).remove(Headers.NAME_CONTENT_TYPE);
						}
					}
				} 
				else {
					try {
						((OutboundResponseHeaders)exchange.response().headers()).set(bestMatch.getTarget().getKey());
						return bestMatch.getTarget().getValue().defer(exchange);
					} 
					catch (RouteNotFoundException | DisabledRouteException e) {
						((OutboundResponseHeaders)exchange.response().headers()).remove(Headers.NAME_CONTENT_TYPE);
					}
				}
			}
			// We haven't found any route that can handle the request
			throw new NotAcceptableException(this.handlers.keySet().stream()
					.map(Headers.ContentType::getMediaType)
					.collect(Collectors.toSet())
				);
		}
	}
}
