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

import io.inverno.mod.http.base.router.ErrorRoute;
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
 * A {@link RoutingLink} implementation resolving resources by matching the error in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see ErrorRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the error route type
 * @param <D> the error route extractor type
 */
public abstract class ErrorRoutingLink<A, B, C extends ErrorRoute<A>, D extends ErrorRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private static final Comparator<Class<? extends Throwable>> CLASS_COMPARATOR = (t1, t2) -> {
		if(t1.isAssignableFrom(t2)) {
			return 1;
		}
		else if(t2.isAssignableFrom(t1)) {
			return -1;
		}
		else {
			return 0;
		}
	};

	private Map<Class<? extends Throwable>, RoutingLink<A, B, C, D>> links;
	private Map<Class<? extends Throwable>, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal error routing link.
	 * </p>
	 */
	public ErrorRoutingLink() {
		super();
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Creates an error routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public ErrorRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new LinkedHashMap<>();
	}

	/**
	 * <p>
	 * Extracts the error from the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return an error
	 */
	protected abstract Throwable getError(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getErrorType() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		return this.links.get(route.getErrorType());
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		Class<? extends Throwable> error = route.getErrorType();
		RoutingLink<A, B, C, D> link = this.links.get(error);
		if(link == null) {
			link = this.createLink();
			this.links.put(error, link);
			// We are recreating the map instead of using a TreeMap because we want to preserve insertion order
			this.links = this.links.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(CLASS_COMPARATOR))
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
		this.links.remove(route.getErrorType());
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> value.extractRoutes(routeExtractor.errorType(key)));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}
		Throwable error = this.getError(input);
		if(error != null) {
			Class<? extends Throwable> errorClass = error.getClass();
			for (Map.Entry<Class<? extends Throwable>, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
				if (e.getKey().isAssignableFrom(errorClass)) {
					return e.getValue();
				}
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
		Throwable error = this.getError(input);
		if(error != null) {
			Class<? extends Throwable> errorClass = error.getClass();
			for (Map.Entry<Class<? extends Throwable>, RoutingLink<A, B, C, D>> e : this.enabledLinks.entrySet()) {
				if (e.getKey().isAssignableFrom(errorClass)) {
					result.add(e.getValue());
				}
			}
		}
		return result;
	}
}
