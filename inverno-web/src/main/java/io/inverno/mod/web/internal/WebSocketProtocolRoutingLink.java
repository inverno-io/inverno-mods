/*
 * Copyright 2022 Jeremy KUHN
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

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ws.UnsupportedProtocolException;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebSocketProtocolAware;
import io.inverno.mod.web.spi.Route;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A routing link responsible to route an WebSocket upgrade exchange based on the WebSocket subprotocols requested by the client as defined by {@link WebSocketProtocolAware}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 *
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the route
 * @param <C> the route type
 */
class WebSocketProtocolRoutingLink<A extends ExchangeContext, B extends WebExchange<A>, C extends WebSocketProtocolAware & Route<A, B>> extends RoutingLink<A, B, WebSocketProtocolRoutingLink<A, B, C>, C> {

	private final Map<String, RoutingLink<A, B, ?, C>> handlers;
	private Map<String, RoutingLink<A, B, ?, C>> enabledHandlers;
	
	/**
	 * <p>
	 * Creates a WebSocket protocol routing link.
	 * </p>
	 */
	public WebSocketProtocolRoutingLink() {
		super(WebSocketProtocolRoutingLink::new);
		this.handlers = new HashMap<>();
		this.enabledHandlers = Map.of();
	}
	
	private void updateEnabledHandlers() {
		this.enabledHandlers = this.handlers.entrySet().stream()
			.filter(e -> !e.getValue().isDisabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public WebSocketProtocolRoutingLink<A, B, C> setRoute(C route) {
		String subprotocol = ((WebSocketProtocolAware)route).getSubProtocol();
		if (subprotocol != null) {
			if (this.handlers.containsKey(subprotocol)) {
				this.handlers.get(subprotocol).setRoute(route);
			}
			else {
				this.handlers.put(subprotocol, this.nextLink.createNextLink().setRoute(route));
			}
			this.updateEnabledHandlers();
		}
		else {
			this.nextLink.setRoute(route);
		}
		return this;
	}

	@Override
	public void enableRoute(C route) {
		String subprotocol = ((WebSocketProtocolAware)route).getSubProtocol();
		if (subprotocol != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(subprotocol);
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
		String subprotocol = ((WebSocketProtocolAware)route).getSubProtocol();
		if (subprotocol != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(subprotocol);
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
		String subprotocol = ((WebSocketProtocolAware)route).getSubProtocol();
		if (subprotocol != null) {
			RoutingLink<A, B, ?, C> handler = this.handlers.get(subprotocol);
			if (handler != null) {
				handler.removeRoute(route);
				if (!handler.hasRoute()) {
					// The link has no more routes, we can remove it for good
					this.handlers.remove(subprotocol);
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

	@Override
	public <F extends RouteExtractor<A, B, C>> void extractRoute(F extractor) {
		if (!(extractor instanceof WebSocketProtocolAwareRouteExtractor)) {
			throw new IllegalArgumentException("Route extractor is not WebSocket protocol aware");
		}
		this.handlers.entrySet().stream().forEach(e -> {
			e.getValue().extractRoute(((WebSocketProtocolAwareRouteExtractor<A, B, C, ?>) extractor).subprotocol(e.getKey()));
		});
		super.extractRoute(extractor);
	}

	@Override
	public Mono<Void> defer(B exchange) throws HttpException {
		List<String> wsProtocolHeaderValues = exchange.request().headers().getAll(Headers.NAME_SEC_WEBSOCKET_PROTOCOL);
		if(wsProtocolHeaderValues.isEmpty()) {
			// no protocol specified in the request
			return this.nextLink.defer(exchange);
		}
		else if(!this.enabledHandlers.isEmpty()) {
			return wsProtocolHeaderValues.stream()
				.flatMap(value -> Arrays.stream(value.split(",")))
				.map(subprotocol -> {
					RoutingLink<A, B, ?, C> handler = this.enabledHandlers.get(subprotocol.trim());
					if(handler == null) {
						// Try with a normalized media type
						handler = this.enabledHandlers.get(MediaTypes.normalizeApplicationMediaType(subprotocol.trim()));
					}
					return handler;
				})
				.filter(Objects::nonNull)
				.findFirst()
				.orElseThrow(() -> new UnsupportedProtocolException(this.enabledHandlers.keySet())) // report that specified subprotocols are not supported
				.defer(exchange);
		}
		else {
			// We don't have any enabled handler
			throw new NotFoundException();
		}
	}
}
