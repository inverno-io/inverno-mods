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
import java.util.stream.Collectors;

import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.AcceptMatch;
import io.winterframework.mod.web.NotAcceptableException;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.router.AcceptAwareRoute;

/**
 * @author jkuhn
 *
 */
class ProducesRoutingLink<A, B, C, D extends AcceptAwareRoute<A, B, C>> extends RoutingLink<A, B, C, ProducesRoutingLink<A, B, C, D>, D> {

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
		if(route.getProduces() != null && !route.getProduces().isEmpty()) {
			route.getProduces().stream().map(produce -> this.contentTypeCodec.decode(Headers.CONTENT_TYPE, produce))
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
	public void handle(Request<A, C> request, Response<B> response) throws WebException {
		if(!this.handlers.isEmpty()) {
			AcceptMatch<Headers.Accept.MediaRange, Entry<Headers.ContentType, RoutingLink<A, B, C, ?, D>>> bestMatch = Headers.Accept.merge(request.headers().<Headers.Accept>getAll(Headers.ACCEPT))
				.orElse(this.acceptCodec.decode(Headers.ACCEPT, "*/*")) // If there's no accept header we fallback to */*
				.findBestMatch(this.handlers.entrySet(), Entry::getKey)
				.orElseThrow(() -> new NotAcceptableException(this.handlers.keySet().stream().map(Headers.ContentType::getMediaType).collect(Collectors.toSet())));
			
			if(bestMatch.getSource().getMediaType().equals("*/*")) {
				// First check if the next link can handle the request since this is the default
				try {
					this.nextLink.handle(request, response);
				}
				catch(NotFoundException e) {
					// There's no default handler defined
					bestMatch.getTarget().getValue().handle(request, response);
				}
			}
			else {
				bestMatch.getTarget().getValue().handle(request, response);
			}
		}
		else {
			this.nextLink.handle(request, response);
		}
		
		/*Optional<Headers.Accept> acceptHeader = Headers.Accept.merge(request.headers().<Headers.Accept>getAll(Headers.ACCEPT));
		if(acceptHeader.isPresent() && !this.handlers.isEmpty()) {
			acceptHeader
				.flatMap(accept -> accept.findBestMatch(this.handlers.entrySet(), Entry::getKey).map(AcceptMatch::getTarget).map(Entry::getValue))
				.orElseThrow(() -> new NotAcceptableException(this.handlers.keySet().stream().map(Headers.ContentType::getMediaType).collect(Collectors.toSet())))
				.handle(request, response);
		}
		else {
			// client accepts anything, we should try the next link and fallback to the first handler (if any) in case no subsequent handler was found
			try {
				// By default we fallback on the next link
				this.nextLink.handle(request, response);
			}
			catch(NotFoundException e) {
				if(!this.handlers.isEmpty()) {
					this.handlers.entrySet().iterator().next().getValue().handle(request, response);
				}
				else {
					throw e;
				}
			}
		}*/
	}
}
