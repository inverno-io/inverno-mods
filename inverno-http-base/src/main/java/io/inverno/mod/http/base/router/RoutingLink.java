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
package io.inverno.mod.http.base.router;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * A routing link is used in an {@link AbstractRouter} to implement a routing chain.
 * </p>
 *
 * <p>
 * A routing chain is composed of multiple links that each matches a particular criteria from a router input, the result is a tree of criteria which allows to efficiently resolve the resources
 * matching a particular input.
 * </p>
 *
 * <p>
 * Resolution is done top to bottom which means an input can't match different branch in the routing tree, each link must determine the next best matching link and so on until we get to the end of the
 * chain, it is not possible to change branch. The advantage of this approach is that for a given input we'll always resolve exactly one resource or no resource which is what is expected from a
 * router.
 * </p>
 *
 * <p>
 * A routing chain can be created using {@link ChainBuilder} as follows:
 * </p>
 *
 * <pre>{@code
 * RoutingLink<SampleResource, SampleInput, SampleRoute, SampleRouteExtractor> firstLink = RoutingLink
 *     .link(ARoutingLink::new)
 *     .link(BRoutingLink::new)
 *     .link(ign -> new CRoutingLink())
 *     .getRoutingChain();
 * }</pre>
 *
 * <p>
 * In above example, the routing chain is composed of {@code ARoutinglink}, {@code BRoutinglink} and {@code CRoutinglink} which are evaluated in that order when resolving resources. Unlike
 * {@code ARoutinglink} and {@code BRoutinglink}, {@code CRoutinglink} is at the end of the chain and as a result the next link factory is ignored in order to terminate the chain.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the route type
 * @param <D> the route extractor type
 */
public abstract class RoutingLink<A, B, C extends Route<A>, D extends RouteExtractor<A, C>> {

	private final Supplier<RoutingLink<A, B, C, D>> nextLinkFactory;

	private final RoutingLink<A, B, C, D> nextLink;

	private boolean terminal = true; // the link has no route at first

	private boolean disabled;

	private A resource;

	private RoutingLink<A, B, C, D> parent;

	/**
	 * <p>
	 * Creates a terminal routing link.
	 * </p>
	 */
	protected RoutingLink() {
		this(EndRoutingLink::new);
	}

	/**
	 * <p>
	 * Creates a routing link chained to a next link.
	 * </p>
	 *
	 * @param nextLinkFactory the next link factory
	 */
	protected RoutingLink(Supplier<RoutingLink<A, B, C, D>> nextLinkFactory) {
		this.nextLinkFactory = nextLinkFactory;

		this.nextLink = this.createLink();
	}

	/**
	 * <p>
	 * Sets the routes in the chain.
	 * </p>
	 *
	 * <p>
	 * When a criteria corresponding to the link is specified in the route, this method sets the route on an existing link defined with the same criteria or creates a new link, otherwise it simply
	 * sets the route on the default next link.
	 * </p>
	 *
	 * @param route the route to set
	 *
	 * @return true if the route has been linked (i.e. a criteria is managed by the linked or links further down the chain), false otherwise
	 */
	private boolean setRoute0(C route) {
		boolean linked = this.canLink(route);
		if(linked) {
			RoutingLink<A, B, C, D> link = this.getOrSetLink(route);
			if(link == null) {
				if(this.nextLink != null) {
					linked = this.nextLink.setRoute0(route);
				}
			}
			else if (!link.setRoute0(route)) {
				// the rest of the chain hasn't consumed anything
				link.resource = route.get(link.resource);
			}
		}
		else if(this.nextLink != null) {
			linked = this.nextLink.setRoute0(route);
		}
		this.terminal = !this.hasLinks();
		this.refreshEnabled();
		return linked;
	}

	/**
	 * <p>
	 * Removes the specified route from the routing chain.
	 * </p>
	 *
	 * @param route the route to remove.
	 *
	 * @return true if the route was unlinked, false otherwise
	 */
	private boolean removeRoute0(C route) {
		if(this.terminal) {
			// EndRoutingLink is always ending so this excludes nextLink being null in the method.
			return false;
		}

		boolean unlinked = this.canLink(route);
		if(unlinked) {
			RoutingLink<A, B, C, D> link = this.getLink(route);
			if(link != null) {
				if(!link.removeRoute0(route)) {
					link.resource = null;
				}
				if(!link.hasLinks() && link.resource == null) {
					this.removeLink(route);
				}
			}
			else {
				this.nextLink.removeRoute0(route);
			}
		}
		else {
			unlinked = this.nextLink.removeRoute0(route);
		}
		this.terminal = !this.hasLinks();
		this.refreshEnabled();
		return unlinked;
	}

	/**
	 * <p>
	 * Enables the specified route on the routing chain.
	 * </p>
	 *
	 * @param route the route to enable
	 *
	 * @return true if the route was linked, false otherwise
	 */
	private boolean enableRoute0(C route) {
		boolean linked = this.canLink(route);
		if(linked) {
			RoutingLink<A, B, C, D> link = this.getLink(route);
			if(link != null) {
				if(!link.enableRoute0(route)) {
					link.disabled = false;
				}
			}
		}
		else if(this.nextLink != null) {
			linked = this.nextLink.enableRoute0(route);
		}
		this.terminal = !this.hasLinks();
		this.refreshEnabled();
		return linked;
	}

	/**
	 * <p>
	 * Disables the specified route on the routing chain.
	 * </p>
	 *
	 * @param route the route to enable
	 *
	 * @return true if the route was linked, false otherwise
	 */
	private boolean disableRoute0(C route) {
		boolean linked = this.canLink(route);
		if(linked) {
			RoutingLink<A, B, C, D> link = this.getLink(route);
			if(link != null) {
				if(!link.disableRoute0(route)) {
					link.disabled = link.resource != null;
				}
			}
		}
		else if (this.nextLink != null) {
			linked = this.nextLink.disableRoute0(route);
		}
		this.terminal = !this.hasLinks();
		this.refreshEnabled();
		return linked;
	}

	/**
	 * <p>
	 * Determines whether the link has enabled links targeting an actual resource.
	 * </p>
	 *
	 * @return true if the route has active links, false otherwise
	 */
	private boolean hasLinks() {
		for(RoutingLink<A, B, C, D> link : this.getLinks()) {
			if(!link.disabled || !link.terminal) {
				return true;
			}
		}
		return this.nextLink != null && this.nextLink.hasLinks();
	}

	/**
	 * <p>
	 * Creates a link.
	 * </p>
	 *
	 * <p>
	 * A link is created when a route is set in the routing chain and the link can handle the route (i.e. the route defines a criteria handled by the link).
	 * </p>
	 *
	 * @return a new routing link
	 */
	protected final RoutingLink<A, B, C, D> createLink() {
		if(this.nextLinkFactory != null) {
			RoutingLink<A, B, C, D> nextLink = this.nextLinkFactory.get();
			nextLink.parent = this;
			return nextLink;
		}
		return null;
	}

	/**
	 * <p>
	 * Determines whether the link can link the specified route.
	 * </p>
	 *
	 * <p>
	 * A route is linkable by a link when it defines a criteria handled by the link so basically when the link is able to route an input based on that particular criteria (e.g. a path, an HTTP
	 * method...).
	 * </p>
	 *
	 * @param route a route
	 *
	 * @return true if the link can
	 */
	protected abstract boolean canLink(C route);

	/**
	 * <p>
	 * Returns the next link in the routing chain that is matching the specified route.
	 * </p>
	 *
	 * @param route a route
	 *
	 * @return the next routing link in the routing chain or null if there is no link matching the route
	 */
	protected abstract RoutingLink<A, B, C, D> getLink(C route);

	/**
	 * <p>
	 * Returns or sets the next link in the routing chain that is matching the specified route.
	 * </p>
	 *
	 * <p>
	 * If there is no link matching the route, a new link shall be created and set in the routing chain.
	 * </p>
	 *
	 * @param route a route
	 *
	 * @return the next routing link in the routing chain
	 */
	protected abstract RoutingLink<A, B, C, D> getOrSetLink(C route);

	/**
	 * <p>
	 * Returns all links bound to the link.
	 * </p>
	 *
	 * @return the list of links bound to the link
	 */
	protected abstract Collection<RoutingLink<A, B, C, D>> getLinks();

	/**
	 * <p>
	 * Removes the link matching the specified route.
	 * </p>
	 *
	 * @param route a route
	 */
	protected abstract void removeLink(C route);

	/**
	 * <p>
	 * Refreshes enabled links.
	 * </p>
	 *
	 * <p>
	 * This method is invoked after a change in the routing chain in order to optimize the resource resolution process by identifying disabled links as close as possible from the beginning of the
	 * chain.
	 * </p>
	 */
	protected abstract void refreshEnabled();

	/**
	 * <p>
	 * Populates the specified route extractor with the criteria managed by the link.
	 * </p>
	 *
	 * <p>
	 * This basically comes down to extracting the links managed by the link.
	 * </p>
	 *
	 * @param routeExtractor a route extractor
	 */
	protected abstract void extractLinks(D routeExtractor);

	/**
	 * <p>
	 * Resolves the link best matching the specified input.
	 * </p>
	 *
	 * <p>
	 * The link is resolved by comparing the criteria coming from the routes to the criteria defined in the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return the matching link or null if no route matching the input was defined in the link
	 */
	protected abstract RoutingLink<A, B, C, D> resolveLink(B input);

	/**
	 * <p>
	 * Resolves all links matching the specified input.
	 * </p>
	 *
	 * <p>
	 * Links are resolved by comparing the criteria coming from the routes to the criteria defined in the input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a list of links sorted from the best matching to the least matching or an empty list if no route matching the input was defined in the link
	 */
	protected abstract List<RoutingLink<A, B, C, D>> resolveAllLink(B input);

	/**
	 * <p>
	 * Creates a routing chain.
	 * </p>
	 *
	 * @param link the first link builder
	 *
	 * @return a chain builder
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the route type
	 * @param <D> the route extractor type
	 */
	public static <A, B, C extends Route<A>, D extends RouteExtractor<A, C>> ChainBuilder<A, B, C, D> link(ChainBuilder<A, B, C, D> link) {
		return link;
	}

	/**
	 * <p>
	 * Sets the specified route in the routing chain.
	 * </p>
	 *
	 * @param route the route to set
	 */
	public final void setRoute(C route) {
		this.setRoute0(route);
	}

	/**
	 * <p>
	 * Removes the specified route from the routing chain.
	 * </p>
	 *
	 * @param route the route to remove
	 */
	public final void removeRoute(C route) {
		this.removeRoute0(route);
	}

	/**
	 * <p>
	 * Enables the specified route in the routing chain.
	 * </p>
	 *
	 * @param route the route to enable
	 */
	public final void enableRoute(C route) {
		this.enableRoute0(route);
	}

	/**
	 * <p>
	 * Disables the specified route in the routing chain.
	 * </p>
	 *
	 * @param route the route to disable
	 */
	public final void disableRoute(C route) {
		this.disableRoute0(route);
	}

	/**
	 * <p>
	 * Determines whether the link is enabled in the routing chain.
	 * </p>
	 *
	 * <p>
	 * A link is enabled when it is not disabled and it has enabled routing links.
	 * </p>
	 *
	 * @return true if the link is enabled, false otherwise
	 */
	public final boolean isEnabled() {
		return !this.disabled || this.hasLinks();
	}

	/**
	 * <p>
	 * Extracts routes defined in the routing chain.
	 * </p>
	 *
	 * @param routeExtractor a route extractor
	 */
	public final void extractRoutes(D routeExtractor) {
		if(this.resource != null) {
			routeExtractor.set(this.resource, this.disabled);
		}
		this.extractLinks(routeExtractor);
		if(this.nextLink != null) {
			this.nextLink.extractRoutes(routeExtractor);
		}
	}

	/**
	 * <p>
	 * Returns the current resource.
	 * </p>
	 *
	 * <p>
	 * The current resource is the resource that would be returned when resolving a resource if this was the final link in the routing chain.
	 * </p>
	 *
	 * @return the current resource
	 */
	private A getCurrentResource() {
		if(!this.disabled && this.resource != null) {
			return this.resource;
		}
		if(this.parent != null) {
			return this.parent.getCurrentResource();
		}
		return null;
	}

	/**
	 * <p>
	 * Resolves the resource best matching the specified input.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a resource or null if no route matching the input was defined in the routing chain
	 */
	public final A resolve(B input) {
		return this.doResolve(input);
	}

	/**
	 * <p>
	 * Resolves the resource.
	 * </p>
	 *
	 * <p>
	 * When the link is terminal (i.e. a resource is defined on the link and there is no resource defined further down the chain) the resource is returned right away, otherwise
	 * {@link #resolve(Object)} is invoked on the next link which is either the result of {@link #resolveLink(Object)} or the default link.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return the best matching resource or null
	 */
	A doResolve(B input) {
		if(this.terminal) {
			return this.disabled ? null : this.resource;
		}

		A result = null;
		RoutingLink<A, B, C, D> link = this.resolveLink(input);
		if(link != null) {
			result = link.resolve(input);
		}
		if(result == null && this.nextLink != null) {
			result = this.nextLink.resolve(input);
		}

		if(result != null) {
			return result;
		}
		return this.disabled ? null : this.resource;
	}

	/**
	 * <p>
	 * Resolves all resources matching the specified input sorted from best matching to least matching.
	 * </p>
	 *
	 * @param input an input
	 *
	 * @return a list of matching resources
	 */
	public final Collection<A> resolveAll(B input) {
		if(this.terminal) {
			return this.resource != null ? List.of(this.resource) : List.of();
		}

		LinkedHashSet<A> result = new LinkedHashSet<>();
		boolean nextLinkResolved = false;
		for(RoutingLink<A, B, C, D> link : this.resolveAllLink(input)) {
			if(link == null) {
				result.addAll(this.nextLink.resolveAll(input));
				nextLinkResolved = true;
			}
			else {
				result.addAll(link.resolveAll(input));
			}
		}
		if(!nextLinkResolved && this.nextLink != null) {
			result.addAll(this.nextLink.resolveAll(input));
		}
		if(!this.disabled && this.resource != null) {
			result.add(this.resource);
		}
		return result;
	}

	/**
	 * <p>
	 * Returns a routing link wrapper that returns either the current resource (see {@link #getCurrentResource()}) or throw an exception if no resource has been defined.
	 * </p>
	 *
	 * <p>
	 * This is typically used by {@link RoutingLink} implementors in order to throw errors instead of returning no resources.
	 * </p>
	 *
	 * @param exceptionSupplier the exception supplier
	 *
	 * @see io.inverno.mod.http.base.router.link.MethodRoutingLink
	 *
	 * @return a routing link wrapper
	 */
	protected final RoutingLink<A, B, C, D> currentOrErrorRoutingLink(Supplier<? extends Throwable> exceptionSupplier) {
		return new CurrentOrErrorRoutingLink(exceptionSupplier);
	}

	/**
	 * <p>
	 * Returns a routing link wrapper that returns the best matching link from a list of matching links.
	 * </p>
	 *
	 * <p>
	 * This is typically used by {@link RoutingLink} implementors when it is not possible to resolve the best matching resource from a single link and {@link #resolve(Object)} must be invoked
	 * successively on the element of a list of matching links, ordered from the best matching to the least matching, until a resource is resolved.
	 * </p>
	 *
	 * <p>
	 * {@code null} can be specified in the list of links to identify the default link.
	 * </p>
	 *
	 * @param links a list of routing links ordered from the best matching to the least matching
	 *
	 * @see io.inverno.mod.http.base.router.link.AcceptLanguageRoutingLink
	 *
	 * @return a routing link wrapper
	 */
	protected final RoutingLink<A, B, C, D> bestMatchingRoutingLink(List<RoutingLink<A, B, C, D>> links) {
		return new BestMatchingRoutingLink(links);
	}

	/**
	 * <p>
	 * A routing chain builder.
	 * </p>
	 *
	 * <p>
	 * Assuming we have defined 3 routing link: {@code ARoutingLink}, {@code BRoutingLink} and {@code CRoutingLink}, a routing chain can be created as follows:
	 * </p>
	 *
	 * <pre>{@code
	 * RoutingLink
	 *     .link(ARoutingLink::new)
	 *     .link(BRoutingLink::new)
	 *     .link(ign -> new CRoutingLink())
	 * }</pre>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the route type
	 * @param <D> the route extractor type
	 */
	@FunctionalInterface
	public interface ChainBuilder<A, B, C extends Route<A>, D extends RouteExtractor<A, C>> extends Function<Supplier<RoutingLink<A, B, C, D>>, RoutingLink<A, B, C, D>>  {

		/**
		 * <p>
		 * Links the specified next link factory to the chain.
		 * </p>
		 *
		 * @param nextLinkFactory the next link factory
		 *
		 * @return the resulting routing chain
		 */
		default ChainBuilder<A, B, C, D> link(ChainBuilder<A, B, C, D> nextLinkFactory) {
			return next -> this.apply(() -> nextLinkFactory.apply(next));
		}

		/**
		 * <p>
		 * Returns the routing chain.
		 * </p>
		 *
		 * <p>
		 * The routing chain is basically the first routing link in the chain.
		 * </p>
		 *
		 * @return the first routing link
		 */
		default RoutingLink<A, B, C, D> getRoutingChain() {
			return this.apply(null);
		}
	}

	/**
	 * <p>
	 * Represents the final link in a routing chain.
	 * </p>
	 *
	 * <p>
	 * This link terminates the chain, it is used to hold the resource at the end of the chain, no link can exist after.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @param <A> the resource type
	 * @param <B> the input type
	 * @param <C> the route type
	 * @param <D> the route extractor type
	 */
	private static class EndRoutingLink<A, B, C extends Route<A>, D extends RouteExtractor<A, C>> extends RoutingLink<A, B, C, D> {

		/**
		 * <p>
		 * Creates the
		 * </p>
		 */
		public EndRoutingLink() {
			super(null);
		}

		@Override
		protected boolean canLink(C route) {
			return false;
		}

		@Override
		protected RoutingLink<A, B, C, D> getLink(C route) {
			return null;
		}

		@Override
		protected Collection<RoutingLink<A, B, C, D>> getLinks() {
			return List.of();
		}

		@Override
		protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
			return null;
		}

		@Override
		protected void removeLink(C route) {

		}

		@Override
		protected void refreshEnabled() {

		}

		@Override
		protected void extractLinks(D routeExtractor) {
		}

		@Override
		protected RoutingLink<A, B, C, D> resolveLink(B input) {
			return null;
		}

		@Override
		protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
			return List.of();
		}
	}

	/**
	 * <p>
	 * Routing link wrapper that throws an exception when no resource could be resolved.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 *
	 * @see io.inverno.mod.http.base.router.link.MethodRoutingLink
	 */
	private class CurrentOrErrorRoutingLink extends RoutingLink<A, B, C, D> {

		private final Supplier<? extends Throwable> exceptionSupplier;

		/**
		 * <p>
		 * Creates a routing link wrapper that throws an exception when no resource could be resolved.
		 * </p>
		 *
		 * @param exceptionSupplier the exception supplier
		 */
		public CurrentOrErrorRoutingLink(Supplier<? extends Throwable> exceptionSupplier) {
			super(null);
			this.exceptionSupplier = exceptionSupplier;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @return the best matching resource
		 *
		 * @throws RuntimeException if no resource could be resolved
		 */
		@Override
		A doResolve(B input) throws RuntimeException {
			A result = RoutingLink.this.nextLink != null ? RoutingLink.this.nextLink.resolve(input) : null;
			if(result == null) {
				// Do we have a current resource defined up to that link and onward
				result = RoutingLink.this.getCurrentResource();
			}
			if(result == null) {
				Throwable error = exceptionSupplier.get();
				if(error instanceof RuntimeException) {
					throw (RuntimeException)error;
				}
				throw new RuntimeException(error);
			}
			return result;
		}

		@Override
		protected boolean canLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected RoutingLink<A, B, C, D> getLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected Collection<RoutingLink<A, B, C, D>> getLinks() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void removeLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void refreshEnabled() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void extractLinks(D routeExtractor) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected RoutingLink<A, B, C, D> resolveLink(B input) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * <p>
	 * Routing link wrapper that iterate over a list of matching links to resolve the best matching resource.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class BestMatchingRoutingLink extends RoutingLink<A, B, C, D> {

		private final List<RoutingLink<A, B, C, D>> resolvedLinks;

		/**
		 * <p>
		 * Creates a routing link wrapper that iterate over a list of matching links to resolve the best matching resource.
		 * </p>
		 *
		 * <p>
		 * {@code null} when specified in the specified list of links indicates the default link.
		 * </p>
		 *
		 * @param resolvedLinks a list of matching links ordered from the best matching to the least matching
		 */
		public BestMatchingRoutingLink(List<RoutingLink<A, B, C, D>> resolvedLinks) {
			super(null);
			this.resolvedLinks = resolvedLinks;
		}

		/**
		 * <p>
		 * Iterates over the list of matching link and invoke {@link #resolve(Object)} until a resource is found.
		 * </p>
		 */
		@Override
		A doResolve(B input) {
			A result = null;
			boolean nextLinkResolved = false;
			for(RoutingLink<A, B, C, D> matchingLink : this.resolvedLinks) {
				if(matchingLink == null) {
					// Try next link
					result = RoutingLink.this.nextLink != null ? RoutingLink.this.nextLink.resolve(input) : null;
					nextLinkResolved = true;
					if(result == null) {
						// Do we have a current resource defined up to that link and onward
						result = RoutingLink.this.getCurrentResource();
					}
				}
				else {
					result = matchingLink.resolve(input);
				}
				if(result != null) {
					break;
				}
			}
			if(!nextLinkResolved && result == null && RoutingLink.this.nextLink != null) {
				result = RoutingLink.this.nextLink.resolve(input);
			}

			if(result != null) {
				return result;
			}
			return RoutingLink.this.disabled ? null : RoutingLink.this.resource;
		}

		@Override
		protected boolean canLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected RoutingLink<A, B, C, D> getLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected Collection<RoutingLink<A, B, C, D>> getLinks() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void removeLink(C route) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void refreshEnabled() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void extractLinks(D routeExtractor) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected RoutingLink<A, B, C, D> resolveLink(B input) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
			throw new UnsupportedOperationException();
		}
	}
}
