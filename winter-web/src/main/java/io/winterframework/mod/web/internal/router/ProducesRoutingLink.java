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
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.AcceptMatch;
import io.winterframework.mod.web.NotAcceptableException;
import io.winterframework.mod.web.NotFoundException;
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
	
	public ProducesRoutingLink(AcceptCodec acceptCodec, ContentTypeCodec contentTypeCodec) {
		super(() -> new ProducesRoutingLink<>(acceptCodec, contentTypeCodec));
		this.acceptCodec = acceptCodec;
		this.contentTypeCodec = contentTypeCodec;
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ProducesRoutingLink<A, B, C, D> addRoute(D route) {
		Set<String> produces = route.getProduces();
		if(produces != null && !produces.isEmpty()) {
			produces.stream().map(produce -> this.contentTypeCodec.decode(Headers.CONTENT_TYPE, produce))
				.forEach(contentType -> {
					if(this.handlers.containsKey(contentType)) {
						this.handlers.get(contentType).addRoute(route);
					}
					else {
						this.handlers.put(contentType, this.nextLink.createNextLink().addRoute(route));
					}
				});
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toMediaRange(), Headers.Accept.MediaRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}
	
	@Override
	public void removeRoute(D route) {
		Set<String> produces = route.getProduces();
		if(produces != null && !produces.isEmpty()) {
			// We can only remove single route
			if(produces.size() > 1) {
				throw new IllegalArgumentException("Multiple content types found in route, can only remove a single route");
			}
			String produce = produces.iterator().next();
			Headers.ContentType contentType = this.contentTypeCodec.decode(Headers.CONTENT_TYPE, produce);
			RoutingLink<A, B, C, ?, D> handler = this.handlers.get(contentType);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good 
					this.handlers.remove(contentType);
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
		if(!this.handlers.isEmpty()) {
			Iterator<AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, C, ?, D>>>> acceptMatchesIterator = Headers.Accept.merge(exchange.request().headers().<Headers.Accept>getAll(Headers.ACCEPT))
				.orElse(this.acceptCodec.decode(Headers.ACCEPT, "*/*"))
				.findAllMatch(this.handlers.entrySet(), Entry::getKey)
				.iterator();
			
			if(!acceptMatchesIterator.hasNext()) {
				throw new NotAcceptableException(this.handlers.keySet().stream().map(Headers.ContentType::getMediaType).collect(Collectors.toSet()));
			}
			
			while(acceptMatchesIterator.hasNext()) {
				AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, C, ?, D>>> bestMatch = acceptMatchesIterator.next();
				if(bestMatch.getSource().getMediaType().equals("*/*")) {
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
						catch(NotFoundException e2) {
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
					catch(NotFoundException e) {
						// continue with the next best match
						continue;
					}
				}
			}
			throw new NotFoundException();
			
//			AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, C, ?, D>>> bestMatch = Headers.Accept.merge(exchange.request().headers().<Headers.Accept>getAll(Headers.ACCEPT))
//				.orElse(this.acceptCodec.decode(Headers.ACCEPT, "*/*")) // If there's no accept header we fallback to */*
//				.findBestMatch(this.handlers.entrySet(), Entry::getKey)
//				.orElseThrow(() -> new NotAcceptableException(this.handlers.keySet().stream().map(Headers.ContentType::getMediaType).collect(Collectors.toSet())));
//			
//			if(bestMatch.getSource().getMediaType().equals("*/*")) {
//				// First check if the next link can handle the request since this is the default
//				try {
//					this.nextLink.handle(exchange);
//				}
//				catch(NotFoundException e) {
//					// There's no default handler defined
//					bestMatch.getTarget().getValue().handle(exchange);
//				}
//			}
//			else {
//				bestMatch.getTarget().getValue().handle(exchange);
//			}
		}
		else {
			this.nextLink.handle(exchange);
		}
	}
}
