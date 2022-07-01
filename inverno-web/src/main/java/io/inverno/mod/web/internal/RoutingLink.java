/*
 * Copyright 2020 Jeremy KUHN
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
package io.inverno.mod.web.internal;

import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.spi.Route;
import io.inverno.mod.web.spi.Router;
import java.util.function.Supplier;

/**
 * <p>
 * Represents link in a routing chain.
 * </p>
 * 
 * <p>
 * A {@link Router router} can be implemented around a routing chain
 * through which an exchange is routed to the right handler based on a set of
 * criteria. The chain is composed of multiple routing links, each responsible
 * to route the exchange based on a particular set of criteria. The routes in a
 * router are then exploded in the chain.
 * </p>
 * 
 * <p>
 * The order of the routing link in the chain is then relevant to route an
 * exchange, they must ordered from the most important to the least important.
 * </p>
 * 
 * <p>
 * A routing link is itself an exchange handler which is invoked to handle an
 * exchange. If a link in a routing chain can't route an exchange further, it
 * must throw a {@link RouteNotFoundException}.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 * 
 * @see GenericErrorWebRouter
 * @see GenericWebRouter
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the link
 * @param <C> the routing link type
 * @param <D> the route type
 */
abstract class RoutingLink<A extends ExchangeContext, B extends Exchange<A>, C extends RoutingLink<A, B, C, D>, D extends Route<A, B>> implements ReactiveExchangeHandler<A, B> {

	private final Supplier<C> linkSupplier;

	/**
	 * The next link in the chain
	 */
	protected RoutingLink<A, B, ?, D> nextLink;

	/**
	 * <p>
	 * Creates a routing link.
	 * </p>
	 * 
	 * @param linkSupplier a link factory
	 */
	public RoutingLink(Supplier<C> linkSupplier) {
		this.linkSupplier = linkSupplier;
	}

	/**
	 * <p>
	 * Connects the link to the specified link.
	 * </p>
	 * 
	 * @param <T>      the type of the next link
	 * @param nextLink the next link
	 * 
	 * @return the next link
	 */
	public <T extends RoutingLink<A, B, T, D>> T connect(T nextLink) {
		this.nextLink = nextLink;
		return nextLink;
	}

	/**
	 * <p>
	 * Connects the link to a generic link.
	 * </p>
	 * 
	 * @param nextLink the next link
	 */
	protected void connectUnbounded(RoutingLink<A, B, ?, D> nextLink) {
		this.nextLink = nextLink;
	}

	/**
	 * <p>
	 * Creates a next link connected to the link.
	 * </p>
	 * 
	 * @return the next link
	 */
	protected C createNextLink() {
		C nextLink = this.linkSupplier.get();
		if(this.nextLink != null) {
			nextLink.connectUnbounded(this.nextLink.createNextLink());
		}
		return nextLink;
	}

	/**
	 * <p>
	 * Visits the chain using the specified route extractor.
	 * </p>
	 * 
	 * @param <F>       the route extractor type
	 * @param extractor a route extractor
	 */
	public <F extends RouteExtractor<A, B, D>> void extractRoute(F extractor) {
		if(this.nextLink != null) {
			this.nextLink.extractRoute(extractor);
		}
	}

	/**
	 * <p>
	 * Sets the specified route in the chain.
	 * </p>
	 * 
	 * @param route a route
	 * 
	 * @return the routing link
	 */
	public abstract C setRoute(D route);

	/**
	 * <p>
	 * Enables the specified route in the chain
	 * </p>
	 * 
	 * @param route a route
	 */
	public abstract void enableRoute(D route);

	/**
	 * <p>
	 * Disables the specified route in the chain
	 * </p>
	 * 
	 * @param route a route
	 */
	public abstract void disableRoute(D route);

	/**
	 * <p>
	 * Removes the specified route from the chain
	 * </p>
	 * 
	 * @param route a route
	 */
	public abstract void removeRoute(D route);

	/**
	 * <p>
	 * Determines whether the chain has routes.
	 * </p>
	 * 
	 * @return true if there are routes, false otherwise
	 */
	public abstract boolean hasRoute();

	/**
	 * <p>
	 * Determines whether the chain is disabled.
	 * </p>
	 * 
	 * <p>
	 * A chain is considered disabled when all routes are disabled.
	 * </p>
	 * 
	 * @return true if the chain is disabled, false otherwise
	 */
	public abstract boolean isDisabled();
}
