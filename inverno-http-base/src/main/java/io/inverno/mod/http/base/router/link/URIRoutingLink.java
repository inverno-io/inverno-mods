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

import io.inverno.mod.base.net.URIMatcher;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.router.RoutingLink;
import io.inverno.mod.http.base.router.URIRoute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching the URI in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see URIRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the URI route type
 * @param <D> the URI route extractor type
 */
public abstract class URIRoutingLink<A, B, C extends URIRoute<A>, D extends URIRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private final Map<String, RoutingLink<A, B, C, D>> staticLinks;
	private final Map<URIPattern, RoutingLink<A, B, C, D>> patternLinks;

	private Map<String, RoutingLink<A, B, C, D>> enabledStaticLinks;
	private Map<URIPattern, RoutingLink<A, B, C, D>> enabledPatternLinks;

	/**
	 * <p>
	 * Creates a terminal URI routing link.
	 * </p>
	 */
	public URIRoutingLink() {
		super();
		this.staticLinks = this.enabledStaticLinks = new HashMap<>();
		this.patternLinks = this.enabledPatternLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Creates a URI routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public URIRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.staticLinks = this.enabledStaticLinks = new HashMap<>();
		this.patternLinks = this.enabledPatternLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Extracts the normalized URI from the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return an normalized URI
	 */
	protected abstract String getNormalizedURI(B input);

	/**
	 * <p>
	 * Injects the parameters extracted when matching the URI using a {@link URIPattern URI pattern} in the input.
	 * </p>
	 *
	 * @param input      an input
	 * @param parameters the extracted parameters
	 */
	protected abstract void setURIParameters(B input, Map<String, String> parameters);

	@Override
	protected boolean canLink(C route) {
		return route.getURI() != null || route.getURIPattern() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		String uri = route.getURI();
		if(uri != null) {
			return this.staticLinks.get(uri);
		}
		else {
			return this.patternLinks.get(route.getURIPattern());
		}
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		String uri = route.getURI();
		if(uri != null) {
			return this.staticLinks.computeIfAbsent(uri, ign -> this.createLink());
		}
		else {
			return this.patternLinks.computeIfAbsent(route.getURIPattern(), ign -> this.createLink());
		}
	}

	@Override
	protected Collection<RoutingLink<A, B, C, D>> getLinks() {
		return Stream.concat(this.staticLinks.values().stream(), this.patternLinks.values().stream()).collect(Collectors.toUnmodifiableList());
	}

	@Override
	protected void removeLink(C route) {
		String uri = route.getURI();
		if(uri != null) {
			this.staticLinks.remove(uri);
		}
		else {
			this.patternLinks.remove(route.getURIPattern());
		}
	}

	@Override
	protected void refreshEnabled() {
		this.enabledStaticLinks = this.staticLinks.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		this.enabledPatternLinks = this.patternLinks.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.staticLinks.forEach((key, value) -> value.extractRoutes(routeExtractor.uri(key)));
		this.patternLinks.forEach((key, value) -> value.extractRoutes(routeExtractor.uriPattern(key)));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledStaticLinks.isEmpty() && this.enabledPatternLinks.isEmpty()) {
			return null;
		}
		String normalizedURI = this.getNormalizedURI(input);
		RoutingLink<A, B, C, D> link = this.enabledStaticLinks.get(normalizedURI);
		if(link == null) {
			URIMatcher bestMatchMatcher = null;
			for(Map.Entry<URIPattern, RoutingLink<A, B, C, D>> e : this.enabledPatternLinks.entrySet()) {
				URIMatcher matcher = e.getKey().matcher(normalizedURI);
				if (matcher.matches() && (bestMatchMatcher == null || matcher.compareTo(bestMatchMatcher) > 0)) {
					bestMatchMatcher = matcher;
					link = e.getValue();
				}
			}

			if(link != null) {
				Map<String, String> rawPathParameters = bestMatchMatcher.getParameters();
				if(!rawPathParameters.isEmpty()) {
					this.setURIParameters(input, rawPathParameters);
				}
			}
		}
		return link;
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.enabledStaticLinks.isEmpty() && this.enabledPatternLinks.isEmpty()) {
			return List.of();
		}
		List<RoutingLink<A, B, C, D>> result = new ArrayList<>();
		String normalizedURI = this.getNormalizedURI(input);
		if(normalizedURI != null) {
			RoutingLink<A, B, C, D> staticLink = this.enabledStaticLinks.get(normalizedURI);
			if (staticLink != null) {
				result.add(staticLink);
			}

			for (Map.Entry<URIPattern, RoutingLink<A, B, C, D>> e : this.enabledPatternLinks.entrySet()) {
				if (e.getKey().matcher(normalizedURI).matches()) {
					result.add(e.getValue());
				}
			}
		}
		return result;
	}
}
