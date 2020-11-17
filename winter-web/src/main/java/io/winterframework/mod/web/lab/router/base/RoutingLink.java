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
package io.winterframework.mod.web.lab.router.base;

import java.util.function.Supplier;

import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.lab.router.BaseContext;
import io.winterframework.mod.web.lab.router.BaseRoute;

/**
 * @author jkuhn
 *
 */
public abstract class RoutingLink<A extends RoutingLink<A>> implements RequestHandler<RequestBody, BaseContext, ResponseBody> {

	protected RoutingLink<?> nextLink;
	
	private Supplier<A> linkSupplier;
	
	public RoutingLink(Supplier<A> linkSupplier) {
		this.linkSupplier = linkSupplier;
	}
	
	public <B extends RoutingLink<B>> B connect(B nextLink) {
		this.nextLink = nextLink;
		return nextLink;
	}
	
	protected void connectUnbounded(RoutingLink<?> nextLink) {
		this.nextLink = nextLink;
	}
	
	public abstract A addRoute(BaseRoute<RequestBody, BaseContext, ResponseBody> route);
	
	protected A createNextLink() {
		A nextLink = this.linkSupplier.get();
		if(this.nextLink != null) {
			nextLink.connectUnbounded(this.nextLink.createNextLink());
		}
		return nextLink;
	}
	
	
}
