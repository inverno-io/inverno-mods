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

import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Headers.AcceptMatch;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.UnsupportedMediaTypeException;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.internal.header.AcceptCodec;
import io.winterframework.mod.web.router.ContentAwareRoute;

/**
 * @author jkuhn
 *
 */
class ConsumesRoutingLink<A, B, C, D extends ContentAwareRoute<A, B, C>> extends RoutingLink<A, B, C, ConsumesRoutingLink<A, B, C, D>, D> {// extends RoutingLink<RequestBody, ResponseBody, WebContext, ConsumesRoutingLink, WebRoute<RequestBody, ResponseBody, WebContext>> {

	private AcceptCodec acceptCodec;
	
	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, C, ?, D>> handlers;
	
	public ConsumesRoutingLink(AcceptCodec acceptCodec) {
		super(() -> new ConsumesRoutingLink<>(acceptCodec));
		this.acceptCodec = acceptCodec;
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ConsumesRoutingLink<A, B, C, D> addRoute(D route) {
		// Note if someone defines a route with a GET like method and a consumed media type, consumes will be ignored because such request does not provide content types headers
		if(route.getConsumes() != null && !route.getConsumes().isEmpty()) {
			route.getConsumes().stream()
				.map(consume -> this.acceptCodec.decode(Headers.ACCEPT, consume).getMediaRanges().get(0)) // TODO what happens if I have no range?
				.forEach(mediaRange -> {
					if(this.handlers.containsKey(mediaRange)) {
						this.handlers.get(mediaRange).addRoute(route);
					}
					else {
						this.handlers.put(mediaRange, this.nextLink.createNextLink().addRoute(route));
					}
				});
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, Headers.Accept.MediaRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}
	
	@Override
	public void handle(Request<A, C> request, Response<B> response) throws WebException {
		Optional<Headers.ContentType> contentTypeHeader = request.headers().<Headers.ContentType>get(Headers.CONTENT_TYPE);
		
		Optional<RoutingLink<A, B, C, ?, D>> handler = contentTypeHeader
			.flatMap(contentType -> Headers.Accept.MediaRange.findFirstMatch(contentType, this.handlers.entrySet(), Entry::getKey).map(AcceptMatch::getSource).map(Entry::getValue));
		
		if(handler.isPresent()) {
			handler.get().handle(request, response);
		}
		else if(this.handlers.isEmpty() || !contentTypeHeader.isPresent()) {
			this.nextLink.handle(request, response);
		}
		else {
			throw new UnsupportedMediaTypeException();
		}
	}
}
