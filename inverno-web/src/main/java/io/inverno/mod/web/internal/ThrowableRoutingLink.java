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
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.ErrorWebRoute;
import io.inverno.mod.web.spi.ErrorAware;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible to route an error exchange based on the type of
 * error as defined by {@link ErrorAware}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @param <A> the type of the exchange context
 */
class ThrowableRoutingLink<A extends ExchangeContext> extends RoutingLink<A, ErrorWebExchange<A>, ThrowableRoutingLink<A>, ErrorWebRoute<A>> {

	private Map<Class<? extends Throwable>, RoutingLink<A, ErrorWebExchange<A>, ?, ErrorWebRoute<A>>> handlers;
	
	private static final Comparator<Class<? extends Throwable>> CLASS_COMPARATOR = (t1, t2) -> {
		if(t1.isAssignableFrom(t2)) {
			return 1;
		}
		else if(t2.isAssignableFrom(t1)) {
			return -1;
		}
		else {
			return 0;
		}
	};

	/**
	 * <p>
	 * Creates a throwable routing link.
	 * </p>
	 */
	public ThrowableRoutingLink() {
		super(ThrowableRoutingLink::new);
		this.handlers = new LinkedHashMap<>();
	}

	@Override
	public ThrowableRoutingLink<A> setRoute(ErrorWebRoute<A> route) {
		Class<? extends Throwable> error = route.getError();
		if(error != null) {
			if(this.handlers.containsKey(error)) {
				this.handlers.get(error).setRoute(route);
			}
			else {
				this.handlers.put(error, this.nextLink.createNextLink().setRoute(route));
			}
			this.handlers = this.handlers.entrySet().stream().sorted(Comparator.comparing(Entry::getKey, CLASS_COMPARATOR)).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a,b) -> a, LinkedHashMap::new));
		}
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}
	
	@Override
	public void enableRoute(ErrorWebRoute<A> route) {
		Class<? extends Throwable> error = route.getError();
		if(error != null) {
			RoutingLink<A, ErrorWebExchange<A>, ?, ErrorWebRoute<A>> handler = this.handlers.get(error);
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
	public void disableRoute(ErrorWebRoute<A> route) {
		Class<? extends Throwable> error = route.getError();
		if(error != null) {
			RoutingLink<A, ErrorWebExchange<A>, ?, ErrorWebRoute<A>> handler = this.handlers.get(error);
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
	public void removeRoute(ErrorWebRoute<A> route) {
		Class<? extends Throwable> error = route.getError();
		if(error != null) {
			RoutingLink<A, ErrorWebExchange<A>, ?, ErrorWebRoute<A>> handler = this.handlers.get(error);
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
	
	@Override
	public boolean isDisabled() {
		return this.handlers.values().stream().allMatch(RoutingLink::isDisabled) && this.nextLink.isDisabled();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <F extends RouteExtractor<A, ErrorWebExchange<A>, ErrorWebRoute<A>>> void extractRoute(F extractor) {
		super.extractRoute(extractor);
		if(!(extractor instanceof ErrorAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not error aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((ErrorAwareRouteExtractor<A, ErrorWebExchange<A>, ErrorWebRoute<A>, ?>)extractor).error(e.getKey()));
		});
	}
	
	@Override
	public Mono<Void> defer(ErrorWebExchange<A> exchange) {
		// We take the first match, or we delegate to the next link
		return this.handlers.entrySet().stream()
			.filter(e -> e.getKey().isAssignableFrom(exchange.getError().getClass()))
			.findFirst()
			.map(Entry::getValue)
			.map(handler -> handler.defer(exchange))
			.orElseGet(() -> this.nextLink.defer(exchange));
	}
}
