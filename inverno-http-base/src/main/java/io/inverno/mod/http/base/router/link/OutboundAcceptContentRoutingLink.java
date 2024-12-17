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

import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.base.router.AcceptContentRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching the accepted content types in an input.
 * </p>
 *
 * <p>
 * When considering an HTTP server, it allows to select the resource (e.g. handler) that best match the content types accepted in the request.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see AcceptContentRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the outbound accept content route type
 * @param <D> the outbound accept content route extractor type
 */
public abstract class OutboundAcceptContentRoutingLink<A, B, C extends AcceptContentRoute<A>, D extends AcceptContentRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private static final HeaderCodec<? extends Headers.ContentType> CONTENT_TYPE_CODEC = new ContentTypeCodec();

	private final boolean strict;
	private Map<Headers.ContentType, RoutingLink<A, B, C, D>> links;
	private Map<Headers.ContentType, RoutingLink<A, B, C, D>> enabledLinks;

	public OutboundAcceptContentRoutingLink() {
		this(true);
	}

	/**
	 * <p>
	 * Creates a terminal outbound accept content routing link.
	 * </p>
	 *
	 * @param strict true to throw a {@link NotAcceptableException} when no match is found, false otherwise
	 */
	public OutboundAcceptContentRoutingLink(boolean strict) {
		super();
		this.strict = strict;
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Creates a strict outbound accept content routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public OutboundAcceptContentRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		this(nextLinkFactory, true);
	}

	/**
	 * <p>
	 * Creates an outbound accept content routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 * @param strict          true to throw a {@link NotAcceptableException} when no match is found, false otherwise
	 */
	public OutboundAcceptContentRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory, boolean strict) {
		super(nextLinkFactory);
		this.strict = strict;
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
		Headers.ContentType contentType = CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, route.getAccept());
		return this.links.get(contentType);
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		Headers.ContentType contentType = CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, route.getAccept());
		RoutingLink<A, B, C, D> link = this.links.get(contentType);
		if(link == null) {
			link = this.createLink();
			this.links.put(contentType, link);
			// We are recreating the map instead of using a TreeMap because we want to preserve insertion order
			this.links = this.links.entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getKey().toMediaRange(), Headers.Accept.MediaRange.COMPARATOR))
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
		Headers.ContentType contentType = CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, route.getAccept());
		this.links.remove(contentType);
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> {
			StringBuilder contentType = new StringBuilder(key.getMediaType());
			for (Map.Entry<String, String> p : key.getParameters().entrySet()) {
				contentType.append(";").append(p.getKey()).append("=").append(p.getValue());
			}
			value.extractRoutes(routeExtractor.accept(contentType.toString()));
		});
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}

		List<RoutingLink<A, B, C, D>> links = this.resolveAllLink(input);
		if(links.isEmpty()) {
			if(this.strict) {
				throw new NotAcceptableException(this.enabledLinks.keySet().stream()
					.map(Headers.ContentType::getMediaType)
					.collect(Collectors.toSet())
				);
			}
			return null;
		}
		return this.bestMatchingRoutingLink(links);
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return List.of();
		}
		List<RoutingLink<A, B, C, D>> result = new ArrayList<>();
		Headers.Accept accept = Headers.Accept.merge(this.getAllAcceptHeaders(input)).orElse(Headers.Accept.ALL);
		Iterator<Headers.AcceptMatch<Headers.Accept.MediaRange, Map.Entry<Headers.ContentType, RoutingLink<A, B, C, D>>>> acceptMatchesIterator = accept
			.findAllMatch(this.enabledLinks.entrySet(), Map.Entry::getKey).iterator();

		boolean nextLinkAdded = false;
		while(acceptMatchesIterator.hasNext()) {
			Headers.AcceptMatch<Headers.Accept.MediaRange, Map.Entry<Headers.ContentType, RoutingLink<A, B, C, D>>> match = acceptMatchesIterator.next();
			if(!nextLinkAdded && match.getSource().getMediaType().equals("*/*") && match.getSource().getParameters().isEmpty()) {
				result.add(null); // I need to go up... i.e simulate what would happen if I return null here
				nextLinkAdded = true;
			}
			result.add(match.getTarget().getValue());
		}

		if(this.strict && result.isEmpty()) {
			throw new NotAcceptableException(this.enabledLinks.keySet().stream()
				.map(Headers.ContentType::getMediaType)
				.collect(Collectors.toSet())
			);
		}
		return result;
	}
}
