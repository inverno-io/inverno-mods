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

import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.internal.header.ContentTypeCodec;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;

/**
 * @author jkuhn
 *
 */
public class ProducesRoutingLink extends RoutingLink<ProducesRoutingLink> {

	private ContentTypeCodec contentTypeCodec;
	
	private Map<Headers.ContentType, RoutingLink<?>> handlers;
	
	public ProducesRoutingLink(ContentTypeCodec contentTypeCodec) {
		super(() -> new ProducesRoutingLink(contentTypeCodec));
		this.contentTypeCodec = contentTypeCodec;
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ProducesRoutingLink addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route) {
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
	public void handle(Request<RequestBody, BaseContext> request, Response<ResponseBody> response) {
		Optional<Headers.Accept> acceptHeader = Headers.Accept.merge(request.headers().<Headers.Accept>getAll(Headers.ACCEPT));
		if(acceptHeader.isPresent()) {
			acceptHeader
				.flatMap(accept -> accept.findBestMatch(this.handlers.entrySet(), Entry::getKey).map(Entry::getValue))
				.orElseThrow(() -> new RoutingException(406, "Not Acceptable"))
				.handle(request, response);
		}
		else {
			// */*: client accepts anything, we should try the next link and fallback to the first handler (if any) in case no subsequent handler was found
			try {
				// By default we fallback on the next link
				this.nextLink.handle(request, response);
			}
			catch(RoutingException e) {
				// If we have 
				if(e.getStatus() == 404 && !this.handlers.isEmpty()) {
					this.handlers.entrySet().iterator().next().getValue().handle(request, response);
				}
				else {
					throw e;
				}
			}
		}
	}
}
