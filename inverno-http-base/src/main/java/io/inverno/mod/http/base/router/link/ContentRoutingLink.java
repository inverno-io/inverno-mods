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

import io.inverno.mod.http.base.UnsupportedMediaTypeException;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.router.ContentRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching the content type in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see ContentRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the content route type
 * @param <D> the content route extractor type
 */
public abstract class ContentRoutingLink<A, B, C extends ContentRoute<A>, D extends ContentRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private static final HeaderCodec<? extends Headers.Accept> ACCEPT_CODEC = new AcceptCodec(false);

	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, C, D>> links;
	private Map<Headers.Accept.MediaRange, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal content routing link.
	 * </p>
	 */
	public ContentRoutingLink() {
		super();
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Creates a content routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public ContentRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Extracts the content type from the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a content type
	 */
	protected abstract Headers.ContentType getContentTypeHeader(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getContentType() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		Headers.Accept.MediaRange mediaRange = ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, route.getContentType()).getMediaRanges().getFirst();
		return this.links.get(mediaRange);
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		Headers.Accept.MediaRange mediaRange = ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, route.getContentType()).getMediaRanges().getFirst();
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
		Headers.Accept.MediaRange mediaRange = ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, route.getContentType()).getMediaRanges().getFirst();
		this.links.remove(mediaRange);
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> {
			StringBuilder contentType = new StringBuilder(key.getMediaType());
			for (Map.Entry<String, String> p : key.getParameters().entrySet()) {
				contentType.append(";").append(p.getKey()).append("=").append(p.getValue());
			}
			value.extractRoutes(routeExtractor.contentType(contentType.toString()));
		});
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}
		Headers.ContentType contentType = this.getContentTypeHeader(input);

		if(contentType == null) {
			return null;
		}

		return Headers.Accept.MediaRange.findFirstMatch(contentType, this.enabledLinks.entrySet(), Map.Entry::getKey)
			.map(match -> match.getSource().getValue())
			.orElseThrow(UnsupportedMediaTypeException::new);
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return List.of();
		}
		List<RoutingLink<A, B, C, D>> result = new ArrayList<>();
		Headers.ContentType contentType = this.getContentTypeHeader(input);
		if(contentType == null) {
			result.addAll(this.enabledLinks.values());
		}
		else {
			for(Map.Entry<Headers.Accept.MediaRange, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
				if(e.getKey().matches(contentType)) {
					result.add(e.getValue());
				}
			}
		}
		return result;
	}
}
