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

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.router.QueryParametersRoute;
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
 * A {@link RoutingLink} implementation resolving resources by matching query parameters in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see QueryParametersRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the query parameters route type
 * @param <D> the query parameters route extractor type
 */
public abstract class QueryParametersRoutingLink<A, B, C extends QueryParametersRoute<A>, D extends QueryParametersRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private static final Comparator<Map<String, QueryParametersRoute.ParameterMatcher>> MATCHERS_COMPARATOR = (m1, m2) -> m2.size() - m1.size();

	private static final List<String> DEFAULT_PARAMETER_VALUES = List.of("");

	private Map<Map<String, QueryParametersRoute.ParameterMatcher>, RoutingLink<A, B, C, D>> links;
	private Map<Map<String, QueryParametersRoute.ParameterMatcher>, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal query parameters routing link.
	 * </p>
	 */
	public QueryParametersRoutingLink() {
		super();
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Creates a query parameters routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public QueryParametersRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Extracts the query parameters from the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return query parameters
	 */
	protected abstract QueryParameters getQueryParameters(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getQueryParameterMatchers() != null && !route.getQueryParameterMatchers().isEmpty();
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		return this.links.get(route.getQueryParameterMatchers());
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		Map<String, QueryParametersRoute.ParameterMatcher> parameterMatchers = route.getQueryParameterMatchers();
		RoutingLink<A, B, C, D> link = this.links.get(parameterMatchers);
		if(link == null) {
			link = this.createLink();
			this.links.put(parameterMatchers, link);
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
		this.links.remove(route.getQueryParameterMatchers());
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)); // map must be ordered
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> value.extractRoutes(routeExtractor.queryParametersMatchers(key)));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}
		QueryParameters parameters = this.getQueryParameters(input);
		for(Map.Entry<Map<String, QueryParametersRoute.ParameterMatcher>, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
			boolean match = true;
			matchersLoop:
			for(Map.Entry<String, QueryParametersRoute.ParameterMatcher> parameterMatcher : e.getKey().entrySet()) {
				// all matchers must match corresponding header value
				List<String> parameterValues = parameters.getAll(parameterMatcher.getKey()).stream().map(Parameter::getValue).collect(Collectors.toUnmodifiableList());
				if(parameterValues.isEmpty()) {
					parameterValues = DEFAULT_PARAMETER_VALUES;
				}

				for(String parameterValue : parameterValues) {
					if(parameterMatcher.getValue().matches(parameterValue)) {
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
		QueryParameters parameters = this.getQueryParameters(input);
		for(Map.Entry<Map<String, QueryParametersRoute.ParameterMatcher>, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
			boolean match = true;
			matchersLoop:
			for(Map.Entry<String, QueryParametersRoute.ParameterMatcher> parameterMatcher : e.getKey().entrySet()) {
				// all matchers must match corresponding header value
				List<String> parameterValues = parameters.getAll(parameterMatcher.getKey()).stream().map(Parameter::getValue).collect(Collectors.toUnmodifiableList());
				if(parameterValues.isEmpty()) {
					parameterValues = DEFAULT_PARAMETER_VALUES;
				}

				for(String parameterValue : parameterValues) {
					if(parameterMatcher.getValue().matches(parameterValue)) {
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
