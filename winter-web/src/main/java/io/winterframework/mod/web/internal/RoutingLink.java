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
package io.winterframework.mod.web.internal;

import java.util.function.Supplier;

import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.web.AbstractRoute;

/**
 * @author jkuhn
 *
 */
abstract class RoutingLink<A extends Exchange, B extends RoutingLink<A, B, C>, C extends AbstractRoute<A>> implements ExchangeHandler<A> {

	private final Supplier<B> linkSupplier;
	
	protected RoutingLink<A, ?, C> nextLink;
	
	public RoutingLink(Supplier<B> linkSupplier) {
		this.linkSupplier = linkSupplier;
	}
	
	public <T extends RoutingLink<A, T, C>> T connect(T nextLink) {
		this.nextLink = nextLink;
		return nextLink;
	}
	
	protected void connectUnbounded(RoutingLink<A, ?, C> nextLink) {
		this.nextLink = nextLink;
	}
	
	protected B createNextLink() {
		B nextLink = this.linkSupplier.get();
		if(this.nextLink != null) {
			nextLink.connectUnbounded(this.nextLink.createNextLink());
		}
		return nextLink;
	}

	public <F extends RouteExtractor<A, C>> void extractRoute(F extractor) {
		if(this.nextLink != null) {
			this.nextLink.extractRoute(extractor);
		}
	}
	
	public abstract B setRoute(C route);

	public abstract void enableRoute(C route);
	
	public abstract void disableRoute(C route);
	
	public abstract void removeRoute(C route);
	
	public abstract boolean hasRoute();
	
	public abstract boolean isDisabled();
}
