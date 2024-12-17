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
import io.inverno.mod.http.base.router.PathRoute;
import io.inverno.mod.http.base.router.RoutingLink;
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
 * A {@link RoutingLink} implementation resolving resources by matching the path in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see PathRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the path route type
 * @param <D> the path route extractor type
 */
public abstract class PathRoutingLink<A, B, C extends PathRoute<A>, D extends PathRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private final Map<String, RoutingLink<A, B, C, D>> staticLinks;
	private final Map<URIPattern, RoutingLink<A, B, C, D>> patternLinks;

	private Map<String, RoutingLink<A, B, C, D>> enabledStaticLinks;
	private Map<URIPattern, RoutingLink<A, B, C, D>> enabledPatternLinks;

	/**
	 * <p>
	 * Creates a terminal path routing link.
	 * </p>
	 */
	public PathRoutingLink() {
		super();
		this.staticLinks = this.enabledStaticLinks = new HashMap<>();
		this.patternLinks = this.enabledPatternLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Creates a path routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public PathRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.staticLinks = this.enabledStaticLinks = new HashMap<>();
		this.patternLinks = this.enabledPatternLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Extracts the absolute normalized path from the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return an absolute normalized path
	 */
	protected abstract String getNormalizedPath(B input);

	/**
	 * <p>
	 * Injects the parameters extracted when matching the path using a {@link URIPattern path pattern} in the input.
	 * </p>
	 *
	 * @param input      an input
	 * @param parameters the extracted parameters
	 */
	protected abstract void setPathParameters(B input, Map<String, String> parameters);

	@Override
	protected boolean canLink(C route) {
		return route.getPath() != null || route.getPathPattern() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		String path = route.getPath();
		if(path != null) {
			return this.staticLinks.get(path);
		}
		else {
			return this.patternLinks.get(route.getPathPattern());
		}
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		String path = route.getPath();
		if(path != null) {
			return this.staticLinks.computeIfAbsent(path, ign -> this.createLink());
		}
		else {
			return this.patternLinks.computeIfAbsent(route.getPathPattern(), ign -> this.createLink());
		}
	}

	@Override
	protected Collection<RoutingLink<A, B, C, D>> getLinks() {
		return Stream.concat(this.staticLinks.values().stream(), this.patternLinks.values().stream()).collect(Collectors.toUnmodifiableList());
	}

	@Override
	protected void removeLink(C route) {
		String path = route.getPath();
		if(path != null) {
			this.staticLinks.remove(path);
		}
		else {
			this.patternLinks.remove(route.getPathPattern());
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
		this.staticLinks.forEach((key, value) -> value.extractRoutes(routeExtractor.path(key)));
		this.patternLinks.forEach((key, value) -> value.extractRoutes(routeExtractor.pathPattern(key)));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledStaticLinks.isEmpty() && this.enabledPatternLinks.isEmpty()) {
			return null;
		}
		String normalizedPath = this.getNormalizedPath(input);
		RoutingLink<A, B, C, D> link = this.enabledStaticLinks.get(normalizedPath);
		if(link == null) {
			URIMatcher bestMatchMatcher = null;
			for(Map.Entry<URIPattern, RoutingLink<A, B, C, D>> e : this.enabledPatternLinks.entrySet()) {
				URIMatcher matcher = e.getKey().matcher(normalizedPath);
				if(matcher.matches() && (bestMatchMatcher == null || matcher.compareTo(bestMatchMatcher) > 0)) {
					bestMatchMatcher = matcher;
					link = e.getValue();
				}
			}

			if(link != null) {
				Map<String, String> rawPathParameters = bestMatchMatcher.getParameters();
				if(!rawPathParameters.isEmpty()) {
					this.setPathParameters(input, rawPathParameters);
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
		String normalizedPath = this.getNormalizedPath(input);
		if(normalizedPath != null) {
			RoutingLink<A, B, C, D> staticLink = this.enabledStaticLinks.get(normalizedPath);
			if (staticLink != null) {
				result.add(staticLink);
			}

			for (Map.Entry<URIPattern, RoutingLink<A, B, C, D>> e : this.enabledPatternLinks.entrySet()) {
				if (e.getKey().matcher(normalizedPath).matches()) {
					result.add(e.getValue());
				}
			}
			return result;
		}
		return result;
	}
}
