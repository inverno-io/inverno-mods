/*
 * Copyright 2021 Jeremy KUHN
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

import java.util.List;

import io.winterframework.mod.http.base.HttpException;
import io.winterframework.mod.http.server.Exchange;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.web.Route;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
public class MockRoutingLink<A extends Exchange, B extends Route<A>> extends RoutingLink<A, MockRoutingLink<A, B>, B> {

	private ExchangeHandler<A> handler;
	
	private boolean disabled;
	
	public MockRoutingLink(List<MockRoutingLink<A, B>> linkRegistry) {
		super(() -> {
			MockRoutingLink<A,B> newLink = new MockRoutingLink<>(linkRegistry);
			linkRegistry.add(newLink);
			return newLink;
		});
	}
	
	public MockRoutingLink<A, B> setRoute(B route) {
		this.handler = route.getHandler();
		return this;
	}
	
	@Override
	public <F extends RouteExtractor<A, B>> void extractRoute(F extractor) {
		super.extractRoute(extractor);
		extractor.handler(this.handler, this.disabled);
	}
	
	@Override
	public void enableRoute(B route) {
		if(this.handler != null) {
			this.disabled = false;
		}
	}
	
	@Override
	public void disableRoute(B route) {
		if(this.handler != null) {
			this.disabled = true;
		}
	}
	
	@Override
	public void removeRoute(B route) {
		this.handler = null;
	}
	
	@Override
	public boolean hasRoute() {
		return this.handler != null;
	}
	
	@Override
	public boolean isDisabled() {
		return this.disabled;
	}
	
	public ExchangeHandler<A> getHandler() {
		return handler;
	}
	
	@Override
	public void handle(A exchange) throws HttpException {
		if(this.handler == null) {
			throw new RouteNotFoundException();
		}
		if(this.disabled) {
			throw new DisabledRouteException();
		}
		this.handler.handle(exchange);
	}

}
