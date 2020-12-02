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
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.ErrorRoute;

/**
 * @author jkuhn
 *
 */
public class ThrowableRoutingLink extends RoutingLink<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>, ThrowableRoutingLink, ErrorRoute> {

	private Map<Class<? extends Throwable>, RoutingLink<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>, ?, ErrorRoute>> handlers;
	
	private static final Comparator<Class<? extends Throwable>> CLASS_COMPARATOR = (t1, t2) -> {
		if(t1.isAssignableFrom(t2)) {
			return -1;
		}
		else if(t2.isAssignableFrom(t1)) {
			return 1;
		}
		else {
			return 0;
		}
	};
	
	public ThrowableRoutingLink() {
		super(ThrowableRoutingLink::new);
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ThrowableRoutingLink addRoute(ErrorRoute route) {
		Set<Class<? extends Throwable>> errors = route.getErrors();
		if(errors != null && !errors.isEmpty()) {
			errors.stream()
				.forEach(error -> {
					if(this.handlers.containsKey(error)) {
						this.handlers.get(error).addRoute(route);
					}
					else {
						this.handlers.put(error, this.nextLink.createNextLink().addRoute(route));
					}
				});
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, CLASS_COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.addRoute(route);
		}
		return this;
	}
	
	@Override
	public void removeRoute(ErrorRoute route) {
		Set<Class<? extends Throwable>> errors = route.getErrors();
		if(errors != null && !errors.isEmpty()) {
			// We can only remove single route
			if(errors.size() > 1) {
				throw new IllegalArgumentException("Multiple errors found in route, can only remove a single route");
			}
			Class<? extends Throwable> error = errors.iterator().next();
			RoutingLink<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>, ?, ErrorRoute> handler = this.handlers.get(error);
			if(handler != null) {
				handler.removeRoute(route);
				if(!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good 
					this.handlers.remove(error);
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
	public <F extends RouteExtractor<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>, ErrorRoute>> void extractRoute(F extractor) {
		super.extractRoute(extractor);
		if(!(extractor instanceof ErrorAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not error aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((ErrorAwareRouteExtractor<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>, ErrorRoute, ?>)extractor).error(e.getKey()));
		});
	}
	
	@Override
	public void handle(ErrorExchange<ResponseBody, Throwable> exchange) throws WebException {
		// We take the first match, or we delegate to the next link
		this.handlers.entrySet().stream()
			.filter(e -> e.getKey().isAssignableFrom(exchange.getError().getClass()))
			.findFirst()
			.map(Entry::getValue)
			.ifPresentOrElse(
				handler -> handler.handle(exchange),
				() -> this.nextLink.handle(exchange)
			);
		
	}
}
