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
package io.winterframework.mod.web.internal.router;

import java.util.function.Supplier;

import io.winterframework.mod.web.Request;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.Response;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.Route;

/**
 * @author jkuhn
 *
 */
abstract class RoutingLink<A, B, C, D extends RoutingLink<A, B, C, D, E>, E extends Route<A, B, C>> implements RequestHandler<A, B, C> {

	protected RoutingLink<A, B, C, ?, E> nextLink;
	
	private Supplier<D> linkSupplier;
	
	public RoutingLink(Supplier<D> linkSupplier) {
		this.linkSupplier = linkSupplier;
	}
	
	public <T extends RoutingLink<A, B, C, T, E>> T connect(T nextLink) {
		this.nextLink = nextLink;
		return nextLink;
	}
	
	protected void connectUnbounded(RoutingLink<A, B, C, ?, E> nextLink) {
		this.nextLink = nextLink;
	}
	
	protected D createNextLink() {
		D nextLink = this.linkSupplier.get();
		if(this.nextLink != null) {
			nextLink.connectUnbounded(this.nextLink.createNextLink());
		}
		return nextLink;
	}

	public abstract D addRoute(E route);
	
	@Override
	public abstract void handle(Request<A, C> request, Response<B> response) throws WebException;
}
