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

import io.inverno.mod.http.base.router.AuthorityRoute;
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
 * A {@link RoutingLink} implementation resolving resources by matching the authority in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see AuthorityRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the authority route type
 * @param <D> the authority route extractor type
 */
public abstract class AuthorityRoutingLink<A, B, C extends AuthorityRoute<A>, D extends AuthorityRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private final Map<String, RoutingLink<A, B, C, D>> staticLinks;
	private final Map<PatternWrapper, RoutingLink<A, B, C, D>> patternLinks;

	private Map<String, RoutingLink<A, B, C, D>> enabledStaticLinks;
	private Map<PatternWrapper, RoutingLink<A, B, C, D>> enabledPatternLinks;

	/**
	 * <p>
	 * Creates a terminal authority routing link.
	 * </p>
	 */
	public AuthorityRoutingLink() {
		super();
		this.staticLinks = enabledStaticLinks = new HashMap<>();
		this.patternLinks = enabledPatternLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Creates an authority routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public AuthorityRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.staticLinks = enabledStaticLinks = new HashMap<>();
		this.patternLinks = enabledPatternLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Extracts the authority from the specified input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return an authority
	 */
	protected abstract String getAuthority(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getAuthority() != null || route.getAuthorityPattern() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		String authority = route.getAuthority();
		if(authority != null) {
			return this.staticLinks.get(authority);
		}
		else {
			return this.patternLinks.get(new PatternWrapper(route.getAuthorityPattern()));
		}
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		String authority = route.getAuthority();
		if(authority != null) {
			return this.staticLinks.computeIfAbsent(authority, ign -> this.createLink());
		}
		else {
			return this.patternLinks.computeIfAbsent(new PatternWrapper(route.getAuthorityPattern()), ign -> this.createLink());
		}
	}

	@Override
	protected Collection<RoutingLink<A, B, C, D>> getLinks() {
		return Stream.concat(this.staticLinks.values().stream(), this.patternLinks.values().stream()).collect(Collectors.toUnmodifiableList());
	}

	@Override
	protected void removeLink(C route) {
		String authority = route.getAuthority();
		if(authority != null) {
			this.staticLinks.remove(authority);
		}
		else {
			this.patternLinks.remove(new PatternWrapper(route.getAuthorityPattern()));
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
		this.staticLinks.forEach((key, value) -> value.extractRoutes(routeExtractor.authority(key)));
		this.patternLinks.forEach((key, value) -> value.extractRoutes(routeExtractor.authorityPattern(key.unwrap())));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.staticLinks.isEmpty() && this.enabledPatternLinks.isEmpty()) {
			return null;
		}
		String authority = this.getAuthority(input);
		RoutingLink<A, B, C, D> link = this.enabledStaticLinks.get(authority);
		if(link == null) {
			for(Map.Entry<PatternWrapper, RoutingLink<A, B, C, D>> e : this.enabledPatternLinks.entrySet()) {
				if(e.getKey().unwrap().matcher(authority).matches()) {
					link = e.getValue();
					break;
				}
			}
		}
		return link;
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.staticLinks.isEmpty() && this.enabledPatternLinks.isEmpty()) {
			return List.of();
		}
		List<RoutingLink<A, B, C, D>> result = new ArrayList<>();
		String authority = this.getAuthority(input);
		if(authority != null) {
			RoutingLink<A, B, C, D> staticLink = this.enabledStaticLinks.get(authority);
			if (staticLink != null) {
				result.add(staticLink);
			}

			for (Map.Entry<PatternWrapper, RoutingLink<A, B, C, D>> e : this.enabledPatternLinks.entrySet()) {
				if (e.getKey().unwrap().matcher(authority).matches()) {
					result.add(e.getValue());
				}
			}
		}
		return result;
	}
}
