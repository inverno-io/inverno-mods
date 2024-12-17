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

import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.router.MethodRoute;
import io.inverno.mod.http.base.router.RoutingLink;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link RoutingLink} implementation resolving resources by matching the HTTP method in an input.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see MethodRoute
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the method route type
 * @param <D> the method route extractor type
 */
public abstract class MethodRoutingLink<A, B, C extends MethodRoute<A>, D extends MethodRoute.Extractor<A, C, D>> extends RoutingLink<A, B, C, D> {

	private final Map<Method, RoutingLink<A, B, C, D>> links;
	private Map<Method, RoutingLink<A, B, C, D>> enabledLinks;

	/**
	 * <p>
	 * Creates a terminal HTTP method routing link.
	 * </p>
	 */
	public MethodRoutingLink() {
		super();
		this.links = this.enabledLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Creates an HTTP method routing link in a routing chain.
	 * </p>
	 *
	 * @param nextLinkFactory the next routing link factory
	 */
	public MethodRoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		super(nextLinkFactory);
		this.links = this.enabledLinks = new HashMap<>();
	}

	/**
	 * <p>
	 * Extracts the HTTP method from the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return an error
	 */
	protected abstract Method getMethod(B input);

	@Override
	protected boolean canLink(C route) {
		return route.getMethod() != null;
	}

	@Override
	protected RoutingLink<A, B, C, D> getLink(C route) {
		return this.links.get(route.getMethod());
	}

	@Override
	protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
		return this.links.computeIfAbsent(route.getMethod(), ign -> this.createLink());
	}

	@Override
	protected Collection<RoutingLink<A, B, C, D>> getLinks() {
		return this.links.values();
	}

	@Override
	protected void removeLink(C route) {
		this.links.remove(route.getMethod());
	}

	@Override
	protected void refreshEnabled() {
		this.enabledLinks = this.links.entrySet().stream()
			.filter(e -> e.getValue().isEnabled())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	protected void extractLinks(D routeExtractor) {
		this.links.forEach((key, value) -> value.extractRoutes(routeExtractor.method(key)));
	}

	@Override
	protected RoutingLink<A, B, C, D> resolveLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return null;
		}
		RoutingLink<A, B, C, D> link = this.enabledLinks.get(this.getMethod(input));
		if(link == null) {
			return this.currentOrErrorRoutingLink(() -> new MethodNotAllowedException(this.enabledLinks.keySet()));
		}
		return link;
	}

	@Override
	protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
		if(this.enabledLinks.isEmpty()) {
			return List.of();
		}
		Method method = this.getMethod(input);
		if(method != null) {
			RoutingLink<A, B, C, D> link = this.enabledLinks.get(method);
			return link != null ? List.of(link) : List.of();
		}
		return List.of();
	}
}
