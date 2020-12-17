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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.AcceptMatch;
import io.winterframework.mod.web.UnsupportedMediaTypeException;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.router.ContentAwareRoute;

/**
 * @author jkuhn
 *
 */
class ConsumesRoutingLink<A, B, C extends Exchange<A, B>, D extends ContentAwareRoute<A, B, C>> extends RoutingLink<A, B, C, ConsumesRoutingLink<A, B, C, D>, D> {

	private AcceptCodec acceptCodec;
	
	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, C, ?, D>> handlers;
	
	public ConsumesRoutingLink(AcceptCodec acceptCodec) {
		super(() -> new ConsumesRoutingLink<>(acceptCodec));
		this.acceptCodec = acceptCodec;
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ConsumesRoutingLink<A, B, C, D> setRoute(D route) {
		// Note if someone defines a route with a GET like method and a consumed media type, consumes will be ignored because such request does not provide content types headers
		String consume = route.getConsume();
		if(consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			if(this.handlers.containsKey(mediaRange)) {
				this.handlers.get(mediaRange).setRoute(route);
			}
			else {
				this.handlers.put(mediaRange, this.nextLink.createNextLink().setRoute(route));
			}
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, Headers.Accept.MediaRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}
	
	@Override
	public void enableRoute(D route) {
		String consume = route.getConsume();
		if(consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(mediaRange);
			if(handler != null) {
				handler.enableRoute(route);
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.enableRoute(route);
		}
	}
	
	@Override
	public void disableRoute(D route) {
		String consume = route.getConsume();
		if(consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(mediaRange);
			if(handler != null) {
				handler.disableRoute(route);
			}
			// route doesn't exist so let's do nothing
		}
		else {
			this.nextLink.disableRoute(route);
		}
	}
	
	@Override
	public void removeRoute(D route) {
		String consume = route.getConsume();
		if(consume != null) {
			Headers.Accept.MediaRange mediaRange = this.acceptCodec.decode(Headers.NAME_ACCEPT, consume).getMediaRanges().get(0);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(mediaRange);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good 
					this.handlers.remove(mediaRange);
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
		if(!(extractor instanceof ContentAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not content aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((ContentAwareRouteExtractor<A, B, C, D, ?>)extractor).consumes(e.getKey().getMediaType()));
		});
		super.extractRoute(extractor);
	}
	
	@Override
	public void handle(C exchange) throws WebException {
		if(this.handlers.isEmpty()) {
			this.nextLink.handle(exchange);
		}
		else {
			Optional<Headers.ContentType> contentTypeHeader = exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE);
			
			Optional<RoutingLink<A, B, C, ?, D>> handler = contentTypeHeader
				.flatMap(contentType -> Headers.Accept.MediaRange.findFirstMatch(contentType, this.handlers.entrySet(), Entry::getKey).map(AcceptMatch::getSource).map(Entry::getValue));
			
			if(handler.isPresent()) {
				handler.get().handle(exchange);
			}
			else if(this.handlers.isEmpty() || !contentTypeHeader.isPresent()) {
				this.nextLink.handle(exchange);
			}
			else {
				throw new UnsupportedMediaTypeException();
			}
		}
	}
}
