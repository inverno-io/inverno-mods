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
package io.winterframework.mod.web.lab.router.base;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import io.winterframework.mod.web.HeaderService;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;

/**
 * @author jkuhn
 *
 */
public class ConsumesRoutingLink extends RoutingLink<ConsumesRoutingLink> {

	private HeaderService headerService;
	
	private Map<Headers.MediaRange, RoutingLink<?>> handlers;
	
	public ConsumesRoutingLink(HeaderService headerService) {
		super(() -> new ConsumesRoutingLink(headerService));
		this.headerService = headerService;
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ConsumesRoutingLink addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
		// Note if someone defines a route with a GET like method and a consumed media type, consumes will be ignored because such request does not provide content types headers
		
		// I must parse the media range to get parameters 
		
		if(route.getConsumes() != null && !route.getConsumes().isEmpty()) {
			route.getConsumes().stream()
				.map(consume -> this.headerService.<Headers.Accept>decode(Headers.ACCEPT, consume).getMediaRanges().get(0)) // TODO what happens if I have no range? 
				.forEach(mediaRange -> {
					if(this.handlers.containsKey(mediaRange)) {
						this.handlers.get(mediaRange).addRoute(route);
					}
					else {
						this.handlers.put(mediaRange, this.nextLink.createNextLink().addRoute(route));
					}
				});
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, Headers.MediaRange.COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}
	
	@Override
	public void handle(Request<RequestBody, BaseContext> request, Response<ResponseBody> response) {
		Optional<Headers.ContentType> contentTypeHeader = request.headers().<Headers.ContentType>get(Headers.CONTENT_TYPE);
		
		Optional<RoutingLink<?>> handler = contentTypeHeader
			.flatMap(contentType -> Headers.MediaRange.findFirstMatch(contentType, this.handlers.entrySet(), Entry::getKey).map(Entry::getValue));
		
		if(handler.isPresent()) {
			handler.get().handle(request, response);
		}
		else if(this.handlers.isEmpty() || !contentTypeHeader.isPresent()) {
			this.nextLink.handle(request, response);
		}
		else {
			throw new RoutingException(415, "Unsupported Media Type");
		}
	}
}
