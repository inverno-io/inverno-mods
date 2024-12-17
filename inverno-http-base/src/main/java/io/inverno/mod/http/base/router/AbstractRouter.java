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
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>
 * Base {@link Router} implementation.
 * </p>
 *
 * <p>
 * It is based on a {@link RoutingLink.ChainBuilder routing chain}, implementors must also provide consistent implementations for: {@link AbstractRoute}, {@link AbstractRouteManager} and
 * {@link AbstractRouteExtractor}.
 * </p>
 *
 * <p>
 * Assuming resource resolution is based on criteria {@code A}, {@code B} and {@code C} extracted from a {@code SampleInput}, implementors would provides {@link RoutingLink} implementations:
 * {@code ARoutingLink}, {@code BRoutingLink} and {@code CRoutingLink}, each of them being responsible for resolving the next routing link from their respective criteria. A sample router could then be
 * implemented as follows:
 * </p>
 *
 * <pre>{@code
 * public class SampleRouter extends AbstractRouter<SampleResource, SampleInput, SampleRoute, SampleRouteManager, SampleRouter, SampleRouteExtractor> {
 *
 *     public TestRouter() {
 *         super(RoutingLink
 *             .link(ARoutingLink::new)
 *             .link(BRoutingLink::new)
 *             .link(ign -> new CRoutingLink())
 *         );
 *     }
 *
 *     @Override
 *     protected SampleRoute createRoute(String resource, boolean disabled) {
 *         return new SampleRoute(this, resource, disabled);
 *     }
 *
 *     @Override
 *     protected SampleRouteExtractor createRouteExtractor() {
 *         return new SampleRouteExtractor(this);
 *     }
 *
 *     @Override
 *     protected SampleRouteManager createRouteManager() {
 *         return new SampleRouteManager(this);
 *     }
 * }
 * }</pre>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @see RoutingLink
 *
 * @param <A> the resource type
 * @param <B> the input type
 * @param <C> the route type
 * @param <D> the route manager type
 * @param <E> the router type
 * @param <F> the route extractor type
 */
public abstract class AbstractRouter<A, B, C extends Route<A>, D extends RouteManager<A, B, C, D, E>, E extends Router<A, B, C, D, E>, F extends RouteExtractor<A, C>> implements Router<A, B, C, D, E> {

	private final FirstRoutingLink<A, B, C, F> routingChain;

	/**
	 * <p>
	 * Creates a router.
	 * </p>
	 *
	 * @param chainBuilder the routing chain builder
	 */
	protected AbstractRouter(RoutingLink.ChainBuilder<A, B, C, F> chainBuilder) {
		this(chainBuilder.getRoutingChain());
	}

	/**
	 * <p>
	 * Creates a router.
	 * </p>
	 *
	 * @param firstLink the first link in the routing chain
	 */
	protected AbstractRouter(RoutingLink<A, B, C, F> firstLink) {
		this.routingChain = new FirstRoutingLink<>(firstLink);
	}

	/**
	 * <p>
	 * Creates a route.
	 * </p>
	 *
	 * @param resource the resource targeted by the route
	 * @param disabled true to create a disabled route, false otherwise
	 *
	 * @return a route
	 */
	protected abstract C createRoute(A resource, boolean disabled);

	/**
	 * <p>
	 * Creates a route manager.
	 * </p>
	 *
	 * @return a route manager
	 */
	protected abstract D createRouteManager();

	/**
	 * <p>
	 * Creates a route extractor.
	 * </p>
	 *
	 * @return a route extractor
	 */
	protected abstract F createRouteExtractor();

	/**
	 * <p>
	 * Sets the specified route in the router.
	 * </p>
	 *
	 * @param route the route to set
	 */
	protected void setRoute(C route) {
		this.routingChain.setRoute(route);
	}

	/**
	 * <p>
	 * Removes the specified route from the router.
	 * </p>
	 *
	 * @param route the route to remove
	 */
	protected void removeRoute(C route) {
		this.routingChain.removeRoute(route);
	}

	/**
	 * <p>
	 * Disables the specified route in the router.
	 * </p>
	 *
	 * @param route the route to disable
	 */
	protected void disableRoute(C route) {
		this.routingChain.disableRoute(route);
	}

	/**
	 * <p>
	 * Enables the specified route in the router.
	 * </p>
	 *
	 * @param route the route to enable
	 */
	protected void enableRoute(C route) {
		this.routingChain.enableRoute(route);
	}

	@Override
	public D route() {
		return this.createRouteManager();
	}

	@Override
	@SuppressWarnings("unchecked")
	public E route(Consumer<D> configurer) {
		if(configurer != null) {
			configurer.accept(this.createRouteManager());
		}
		return (E)this;
	}

	@Override
	public final Set<C> getRoutes() {
		F routeExtractor = this.createRouteExtractor();
		this.routingChain.extractRoutes(routeExtractor);
		return routeExtractor.getRoutes();
	}

	@Override
	public A resolve(B input) {
		return this.routingChain.resolve(input);
	}

	@Override
	public Collection<A> resolveAll(B input) {
		return this.routingChain.resolveAll(input);
	}

	/**
	 * <p>
	 * Represents the first link in a routing chain.
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
	private static class FirstRoutingLink<A, B, C extends Route<A>, D extends RouteExtractor<A, C>> extends RoutingLink<A, B, C, D> {

		private final RoutingLink<A, B, C, D> link;

		/**
		 * <p>
		 * Creates a routing link representing the first link in a routing chain.
		 * </p>
		 *
		 * @param link the actual first link
		 */
		public FirstRoutingLink(RoutingLink<A, B, C, D> link) {
			super(null);
			this.link = link;
		}

		/**
		 * <p>
		 * Returns always {@code true}.
		 * </p>
		 *
		 * @return true
		 */
		@Override
		protected boolean canLink(C route) {
			return true;
		}

		@Override
		protected RoutingLink<A, B, C, D> getLink(C route) {
			return this.link;
		}

		@Override
		protected Collection<RoutingLink<A, B, C, D>> getLinks() {
			return List.of(this.link);
		}

		@Override
		protected RoutingLink<A, B, C, D> getOrSetLink(C route) {
			return this.link;
		}

		@Override
		protected void removeLink(C route) {
			this.link.removeRoute(route);
		}

		@Override
		protected void refreshEnabled() {
			this.link.refreshEnabled();
		}

		@Override
		protected void extractLinks(D routeExtractor) {
			this.link.extractRoutes(routeExtractor);
		}

		@Override
		protected RoutingLink<A, B, C, D> resolveLink(B input) {
			return this.link;
		}

		@Override
		protected List<RoutingLink<A, B, C, D>> resolveAllLink(B input) {
			return List.of(this.link);
		}
	}
}
