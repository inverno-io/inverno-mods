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
import java.util.Map.Entry;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.AcceptMatch;
import io.winterframework.mod.web.NotAcceptableException;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.router.AcceptAwareRoute;

/**
 * @author jkuhn
 *
 */
class ProducesRoutingLink<A, B, C extends Exchange<A, B>, D extends AcceptAwareRoute<A, B, C>> extends RoutingLink<A, B, C, ProducesRoutingLink<A, B, C, D>, D> {

	private AcceptCodec acceptCodec;
	
	private ContentTypeCodec contentTypeCodec;
	
	private Map<Headers.ContentType, RoutingLink<A, B, C, ?, D>> handlers;
	private Map<Headers.ContentType, RoutingLink<A, B, C, ?, D>> enabledHandlers;
	
	public ProducesRoutingLink(AcceptCodec acceptCodec, ContentTypeCodec contentTypeCodec) {
		super(() -> new ProducesRoutingLink<>(acceptCodec, contentTypeCodec));
		this.acceptCodec = acceptCodec;
		this.contentTypeCodec = contentTypeCodec;
		this.handlers = new LinkedHashMap<>();
		this.enabledHandlers = Map.of();
	}
	
	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream().filter(e -> !e.getValue().isDisabled()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
	}

	@Override
	public ProducesRoutingLink<A, B, C, D> setRoute(D route) {
		String produce = route.getProduce();
		if(produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.CONTENT_TYPE, produce);
			if(this.handlers.containsKey(contentType)) {
				this.handlers.get(contentType).setRoute(route);
			}
			else {
				this.handlers.put(contentType, this.nextLink.createNextLink().setRoute(route));
			}
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toMediaRange(), Headers.Accept.MediaRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
			this.updateEnabledHandlers();
		}
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}
	
	@Override
	public void enableRoute(D route) {
		String produce = route.getProduce();
		if(produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.CONTENT_TYPE, produce);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(contentType);
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
	public void disableRoute(D route) {
		String produce = route.getProduce();
		if(produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.CONTENT_TYPE, produce);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(contentType);
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
	public void removeRoute(D route) {
		String produce = route.getProduce();
		if(produce != null) {
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.CONTENT_TYPE, produce);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(contentType);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
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
	public <F extends RouteExtractor<A, B, C, D>> void extractRoute(F extractor) {
		super.extractRoute(extractor);
		if(!(extractor instanceof AcceptAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not accept aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((AcceptAwareRouteExtractor<A, B, C, D, ?>)extractor).produces(e.getKey().getMediaType()));
		});
	}
	
	@Override
	public void handle(C exchange) throws WebException {
		if(this.handlers.isEmpty()) {
			this.nextLink.handle(exchange);
		}
		else {
			Headers.Accept accept = Headers.Accept.merge(exchange.request().headers().<Headers.Accept>getAllHeader(Headers.ACCEPT))
				.orElse(this.acceptCodec.decode(Headers.ACCEPT, "*/*"));
			
			if(!this.enabledHandlers.isEmpty()) {
				Iterator<AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, C, ?, D>>>> acceptMatchesIterator = accept
					.findAllMatch(this.enabledHandlers.entrySet(), Entry::getKey)
					.iterator();
				
				while(acceptMatchesIterator.hasNext()) {
					AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, C, ?, D>>> bestMatch = acceptMatchesIterator.next();
					if(bestMatch.getSource().getMediaType().equals("*/*")) {
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
							catch(RouteNotFoundException | DisabledRouteException e2) {
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
						catch(RouteNotFoundException | DisabledRouteException e) {
							// continue with the next best match
							continue;
						}
					}
				}
				// We haven't found any route that can handle the request
				throw new NotAcceptableException(this.handlers.keySet().stream().map(Headers.ContentType::getMediaType).collect(Collectors.toSet()));
			}
			else if(accept.getMediaRanges().stream().anyMatch(mediaRange -> mediaRange.getMediaType().equals("*/*"))) {
				// We delegate to next link only if */* is accepted
				this.nextLink.handle(exchange);
			}
			else {
				throw new NotAcceptableException();
			}
		}
	}
}
