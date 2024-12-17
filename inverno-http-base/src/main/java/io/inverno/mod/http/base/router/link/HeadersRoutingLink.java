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

import io.inverno.mod.http.base.InboundHeaders;
import io.inverno.mod.http.base.router.HeadersRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching headers in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see HeadersRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the error route type
 * @param <D> the error route extractor type
 */
public abstract class HeadersRoutingLink<A, B, C extends HeadersRoute<A>, D extends HeadersRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private static final Comparator<Map<String, HeadersRoute.HeaderMatcher>> MATCHERS_COMPARATOR = (m1, m2) -> m2.size() - m1.size();

	private static final List<String> DEFAULT_HEADER_VALUES = List.of("");

	private Map<Map<String, HeadersRoute.HeaderMatcher>, RoutingLink<A, B, C, D>> links;
	private Map<Map<String, HeadersRoute.HeaderMatcher>, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal headers routing link.
	 * </p>
	 */
	public HeadersRoutingLink() {
		super();
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Creates a headers routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public HeadersRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Extracts the headers from the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return headers
	 */
	protected abstract InboundHeaders getHeaders(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getHeadersMatchers() != null && !route.getHeadersMatchers().isEmpty();
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		return this.links.get(route.getHeadersMatchers());
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		Map<String, HeadersRoute.HeaderMatcher> headerMatchers = route.getHeadersMatchers();
		RoutingLink<A, B, C, D> link = this.links.get(headerMatchers);
		if(link == null) {
			link = this.createLink();
			this.links.put(headerMatchers, link);
			// We are recreating the map instead of using a TreeMap because we want to preserve insertion order
			this.links = this.links.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(MATCHERS_COMPARATOR))
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
		this.links.remove(route.getHeadersMatchers());
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> value.extractRoutes(routeExtractor.headersMatchers(key)));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}
		InboundHeaders headers = this.getHeaders(input);
		for(Map.Entry<Map<String, HeadersRoute.HeaderMatcher>, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
			boolean match = true;
			matchersLoop:
			for(Map.Entry<String, HeadersRoute.HeaderMatcher> headerMatcher : e.getKey().entrySet()) {
				// all matchers must match corresponding header value
				List<String> headerValues = headers.getAll(headerMatcher.getKey());
				if(headerValues.isEmpty()) {
					headerValues = DEFAULT_HEADER_VALUES;
				}

				for(String headerValue : headerValues) {
					if(headerMatcher.getValue().matches(headerValue)) {
						continue matchersLoop;
					}
				}
				match = false;
				break;
			}
			if(match) {
				return e.getValue();
			}
		}
		return null;
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return List.of();
		}
		List<RoutingLink<A, B, C, D>> result = new ArrayList<>();
		InboundHeaders headers = this.getHeaders(input);
		for(Map.Entry<Map<String, HeadersRoute.HeaderMatcher>, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
			boolean match = true;
			matchersLoop:
			for(Map.Entry<String, HeadersRoute.HeaderMatcher> headerMatcher : e.getKey().entrySet()) {
				// all matchers must match corresponding header value
				List<String> headerValues = headers.getAll(headerMatcher.getKey());
				if(headerValues.isEmpty()) {
					headerValues = DEFAULT_HEADER_VALUES;
				}

				for(String headerValue : headerValues) {
					if(headerMatcher.getValue().matches(headerValue)) {
						continue matchersLoop;
					}
				}
				match = false;
				break;
			}
			if(match) {
				result.add(e.getValue());
			}
		}
		return result;
	}
}
