/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.http.base.router.link;

import io.inverno.mod.http.base.router.RoutingLink;
import io.inverno.mod.http.base.router.WebSocketSubprotocolRoute;
import io.inverno.mod.http.base.ws.UnsupportedProtocolException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching the WebSocket subprotocols accepted in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see WebSocketSubprotocolRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the WebSocket subprotocol route type
 * @param <D> the WebSocket subprotocol route extractor type
 */
public abstract class WebSocketSubprotocolRoutingLink<A, B, C extends WebSocketSubprotocolRoute<A>, D extends WebSocketSubprotocolRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private final Map<String, RoutingLink<A, B, C, D>> links;

	private Map<String, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal WebSocket subprotocol routing link.
	 * </p>
	 */
	public WebSocketSubprotocolRoutingLink() {
		super();
		this.links = this.enabledLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Creates a WebSocket subprotocol routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public WebSocketSubprotocolRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Extracts the accepted WebSocket subprotocols from the specified input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a list of accepted WebSocket subprotocols or an empty list
	 */
	protected abstract List<String> getSubprotocols(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getSubprotocol() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		return this.links.get(route.getSubprotocol());
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		return this.links.computeIfAbsent(route.getSubprotocol(), ign -> this.createLink());
	}

	@Override
	protected Collection<RoutingLink<A, B, C, D>> getLinks() {
		return this.links.values();
	}

	@Override
	protected void removeLink(C route) {
		this.links.remove(route.getSubprotocol());
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> value.extractRoutes(routeExtractor.subprotocol(key)));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}

		for(String subprotocol: this.getSubprotocols(input)) {
			RoutingLink<A, B, C, D> link = this.enabledLinks.get(subprotocol);
			if(link != null) {
				return link;
			}
		}
		return this.currentOrErrorRoutingLink(() -> new UnsupportedProtocolException(this.enabledLinks.keySet()));
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return List.of();
		}

		List<RoutingLink<A, B, C, D>> result = new ArrayList<>();
		for(String subprotocol: this.getSubprotocols(input)) {
			RoutingLink<A, B, C, D> link = this.enabledLinks.get(subprotocol);
			if(link != null) {
				result.add(link);
			}
		}
		return result;
	}
}
