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
package io.winterframework.mod.web.router.internal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.winterframework.mod.web.router.ErrorRoute;
import io.winterframework.mod.web.server.ErrorExchange;
import io.winterframework.mod.web.server.ExchangeHandler;

/**
 * @author jkuhn
 *
 */
public class GenericErrorRouteExtractor implements ErrorRouteExtractor {

	private GenericErrorRouter router;
	
	private GenericErrorRouteExtractor parent;
	
	private Set<ErrorRoute> routes;
	
	private Class<? extends Throwable> error;
	
	private String produce;
	
	private String language;
	
	public GenericErrorRouteExtractor(GenericErrorRouter router) {
		this.router = router;
	}

	private GenericErrorRouteExtractor(GenericErrorRouteExtractor parent) {
		this.parent = parent;
	}
	
	private GenericErrorRouter getRouter() {
		if(this.parent != null) {
			return this.parent.getRouter();
		}
		else {
			return this.router;
		}
	}
	
	private Class<? extends Throwable> getError() {
		if(this.error != null) {
			return this.error;
		}
		else if(parent != null) {
			return this.parent.getError();
		}
		return null;
	}
	
	private String getProduce() {
		if(this.produce != null) {
			return this.produce;
		}
		else if(parent != null) {
			return this.parent.getProduce();
		}
		return null;
	}
	
	private String getLanguage() {
		if(this.language != null) {
			return this.language;
		}
		else if(parent != null) {
			return this.parent.getLanguage();
		}
		return null;
	}
	
	private void addRoute(ErrorRoute route) {
		if(this.parent != null) {
			this.parent.addRoute(route);
		}
		else {
			if(this.routes == null) {
				this.routes = new HashSet<>();
			}
			this.routes.add(route);
		}
	}
	
	@Override
	public Set<ErrorRoute> getRoutes() {
		if(this.parent != null) {
			return this.parent.getRoutes();			
		}
		else {
			return Collections.unmodifiableSet(this.routes);
		}
	}
	
	@Override
	public ErrorRouteExtractor error(Class<? extends Throwable> error) {
		GenericErrorRouteExtractor childExtractor = new GenericErrorRouteExtractor(this);
		childExtractor.error = error;
		return childExtractor;
	}
	
	@Override
	public ErrorRouteExtractor produces(String mediaType) {
		GenericErrorRouteExtractor childExtractor = new GenericErrorRouteExtractor(this);
		childExtractor.produce = mediaType;
		return childExtractor;
	}

	@Override
	public ErrorRouteExtractor language(String language) {
		GenericErrorRouteExtractor childExtractor = new GenericErrorRouteExtractor(this);
		childExtractor.language = language;
		return childExtractor;
	}

	@Override
	public void handler(ExchangeHandler<ErrorExchange<Throwable>> handler, boolean disabled) {
		if(handler != null) {
			GenericErrorRoute route = new GenericErrorRoute(this.getRouter());
			route.setDisabled(disabled);
	
			Class<? extends Throwable> error = this.getError();
			String produce = this.getProduce();
			String language = this.getLanguage();
			
			if(error != null) {
				route.setError(error);
			}
			if(produce != null) {
				route.setProduce(produce);
			}
			if(language != null) {
				route.setLanguage(language);
			}
			route.setHandler(handler);
			this.addRoute(route);
		}
	}	
}
