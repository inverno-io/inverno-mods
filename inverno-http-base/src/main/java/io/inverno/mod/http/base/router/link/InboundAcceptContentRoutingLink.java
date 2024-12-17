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

import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching the accepted content types in an input.
 * </p>
 *
 * <p>
 * When considering an HTTP client, it allows to select all resources (e.g. interceptors) that match the content types accepted in the request.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see AcceptContentRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the inbound accept content route type
 * @param <D> the inbound accept content route extractor type
 */
public abstract class InboundAcceptContentRoutingLink<A, B, C extends AcceptContentRoute<A>, D extends AcceptContentRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private static final Logger LOGGER = LogManager.getLogger(InboundAcceptContentRoutingLink.class);

	private static final HeaderCodec<? extends Headers.Accept> ACCEPT_CODEC = new AcceptCodec(false);

	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, C, D>> links;
	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal inbound accept content routing link.
	 * </p>
	 */
	public InboundAcceptContentRoutingLink() {
		super();
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Creates an inbound accept content routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public InboundAcceptContentRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Extracts all accepted content types from the specified input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a list of accepted content types or an empty list
	 */
	protected abstract List<Headers.Accept> getAllAcceptHeaders(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getAccept() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		Headers.Accept.MediaRange mediaRange = ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, route.getAccept()).getMediaRanges().getFirst();
		return this.links.get(mediaRange);
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		Headers.Accept.MediaRange mediaRange = ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, route.getAccept()).getMediaRanges().getFirst();
		RoutingLink<A, B, C, D> link = this.links.get(mediaRange);
		if(link == null) {
			link = this.createLink();
			this.links.put(mediaRange, link);
			// We are recreating the map instead of using a TreeMap because we want to preserve insertion order
			this.links = this.links.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(Headers.Accept.MediaRange.COMPARATOR))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
		}
		return link;
	}

	@Override
	protected Collection<RoutingLink<A, B, C, D>> getLinks() {
		return this.links.values();
	}

	@Override
	protected void removeLink(C route) {
		Headers.Accept.MediaRange mediaRange = ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, route.getAccept()).getMediaRanges().getFirst();
		this.links.remove(mediaRange);
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> value.extractRoutes(routeExtractor.accept(key.getMediaType())));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}
		List<RoutingLink<A, B, C, D>> links = this.resolveAllLink(input);
		return links.isEmpty() ? null :this.bestMatchingRoutingLink(links);
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return List.of();
		}
		List<RoutingLink<A, B, C, D>> result = new ArrayList<>();
		Headers.Accept
			.merge(this.getAllAcceptHeaders(input))
			.ifPresent(accept -> {
				for(Map.Entry<Headers.Accept.MediaRange, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
					if(matchRouteMediaRange(e.getKey(), accept)) {
						result.add(e.getValue());
					}
				}
			});
		return result;
	}

	/**
	 * <p>
	 * Determines whether the specified route media range matches the specified accept header.
	 * </p>
	 *
	 * @param routeMediaRange a media range
	 * @param accept          an accept header
	 *
	 * @return true if the route media range matches the accept header, false otherwise
	 */
	private static boolean matchRouteMediaRange(Headers.Accept.MediaRange routeMediaRange, Headers.Accept accept) {
		boolean match = false;
		boolean exactMatch = true;
		for(Headers.Accept.MediaRange mediaRange : accept.getMediaRanges()) {
			// We must compare media range here
			if(routeMediaRange.getParameters().isEmpty() || routeMediaRange.getParameters().equals(mediaRange.getParameters())) {
				String routeType = routeMediaRange.getType();
				String routeSubType = routeMediaRange.getSubType();
				String acceptType = mediaRange.getType();
				String acceptSubType = mediaRange.getSubType();

				// A = Set of route media types
				// B = Set of accepted media types
				if(routeType.equals("*")) {
					if(routeSubType.equals("*")) {
						// route: */*, accept: ?/? => B ⊆ A
						match = true;
					}
					else if(routeSubType.equals(acceptSubType)) {
						// route: */x, accept: ?/x => B ⊆ A
						match = true;
					}
					else if(acceptSubType.equals("*")) {
						match = true;
						exactMatch = false;
					}
					else {
						// route: */x, accept: ?/y => B ⊄ A
						exactMatch = false;
					}
				}
				else {
					if(routeSubType.equals("*")) {
						if(acceptType.equals("*")) {
							// route: x/*, accept: */? => B ∩ A ≠ ∅ and B ⊄ A
							match = true;
							exactMatch = false;
						}
						else if(routeType.equals(acceptType)) {
							// route: x/*, accept: x/? => B ⊆ A
							match = true;
						}
						else {
							// route: x/*, accept: y/? => B ⊄ A
							exactMatch = false;
						}
					}
					else {
						if(acceptType.equals("*")) {
							if(acceptSubType.equals("*")) {
								// route: x/y, accept: */* => => B ∩ A ≠ ∅ and B ⊄ A
								match = true;
							}
							else if(routeSubType.equals(acceptSubType)) {
								// route: x/y, accept: */y => B ∩ A ≠ ∅ and B ⊄ A
								match = true;
							}
							// route: x/y, accept: */? => => B ⊄ A
							exactMatch = false;
						}
						else {
							if(routeType.equals(acceptType)) {
								if(acceptSubType.equals("*")) {
									// route: x/y, accept: x/* => B ∩ A ≠ ∅ and B ⊄ A
									match = true;
									exactMatch = false;
								}
								else if(routeSubType.equals(acceptSubType)) {
									// route: x/y, accept: x/y => B = A
									match = true;
								}
								else {
									// route: x/y, accept: x/z => B ∩ A = ∅
									exactMatch = false;
								}
							}
						}
					}
				}
			}
			else {
				// No match
				exactMatch = false;
			}
		}
		if(!exactMatch) {
			LOGGER.warn(() -> "Request accepted media range " + accept.getMediaRanges().stream().map(Headers.Accept.MediaRange::getMediaType).collect(Collectors.joining(", ")) + " is wider than the interceptor route media type " + routeMediaRange.getMediaType());
		}
		return match;
	}
}
