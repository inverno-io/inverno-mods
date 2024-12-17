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
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.router.AcceptLanguageRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching languages accepted in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see AcceptLanguageRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the accept language route type
 * @param <D> the accept language route extractor type
 */
public abstract class AcceptLanguageRoutingLink<A, B, C extends AcceptLanguageRoute<A>, D extends AcceptLanguageRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private static final HeaderCodec<? extends Headers.AcceptLanguage> ACCEPT_LANGUAGE_CODEC = new AcceptLanguageCodec(false);

	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, B, C, D>> links;
	private Map<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal accept language routing link.
	 * </p>
	 */
	public AcceptLanguageRoutingLink() {
		super();
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Creates an accept language routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public AcceptLanguageRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Extracts all accepted languages from the specified input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a list of accepted languages or an empty list
	 */
	protected abstract List<Headers.AcceptLanguage> getAllAcceptLanguageHeaders(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getLanguage() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		Headers.AcceptLanguage.LanguageRange languageRange = ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, route.getLanguage()).getLanguageRanges().getFirst();
		return this.links.get(languageRange);
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		Headers.AcceptLanguage.LanguageRange languageRange = ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, route.getLanguage()).getLanguageRanges().getFirst();
		if(languageRange.getLanguageTag().equals("*")) {
			return null;
		}
		RoutingLink<A, B, C, D> link = this.links.get(languageRange);
		if(link == null) {
			link = this.createLink();
			this.links.put(languageRange, link);
			// We are recreating the map instead of using a TreeMap because we want to preserve insertion order
			this.links = this.links.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(Headers.AcceptLanguage.LanguageRange.COMPARATOR))
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
		Headers.AcceptLanguage.LanguageRange languageRange = ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, route.getLanguage()).getLanguageRanges().getFirst();
		this.links.remove(languageRange);
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> value.extractRoutes(routeExtractor.language(key.getLanguageTag())));
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
		Headers.AcceptLanguage acceptLanguage = Headers.AcceptLanguage.merge(this.getAllAcceptLanguageHeaders(input)).orElse(Headers.AcceptLanguage.ALL);
		Iterator<Headers.AcceptMatch<Headers.AcceptLanguage.LanguageRange, Map.Entry<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, B, C, D>>>> acceptLanguageMatchesIterator = acceptLanguage
			.findAllMatch(this.enabledLinks.entrySet(), Map.Entry::getKey).iterator();

		boolean nextLinkAdded = false;
		while(acceptLanguageMatchesIterator.hasNext()) {
			Headers.AcceptMatch<Headers.AcceptLanguage.LanguageRange, Map.Entry<Headers.AcceptLanguage.LanguageRange, RoutingLink<A, B, C, D>>> match = acceptLanguageMatchesIterator.next();
			if(!nextLinkAdded && match.getSource().getLanguageTag().equals("*")) {
				result.add(null);
				nextLinkAdded = true;
			}
			result.add(match.getTarget().getValue());
		}
		return result;
	}
}
