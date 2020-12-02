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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.ErrorExchangeHandler;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.ErrorRoute;
import io.winterframework.mod.web.router.ErrorRouteManager;
import io.winterframework.mod.web.router.ErrorRouter;

/**
 * @author jkuhn
 *
 */
class GenericErrorRouteManager implements ErrorRouteManager {

	private GenericErrorRouter router;
	
	private GenericErrorRoute route;
	
	public GenericErrorRouteManager(GenericErrorRouter router) {
		this.router = router;
		this.route = new GenericErrorRoute(router);
	}

	@Override
	public ErrorRouter enable() {
		this.findRoutes().stream().forEach(route -> route.enable());
		return this.router;
	}

	@Override
	public ErrorRouter disable() {
		this.findRoutes().stream().forEach(route -> route.disable());
		return this.router;
	}

	@Override
	public ErrorRouter remove() {
		this.findRoutes().stream().forEach(route -> route.remove());
		return this.router;
	}

	@Override
	public Set<ErrorRoute> findRoutes() {
		// TODO Implement filtering in the route extractor
		return this.router.getRoutes().stream().filter(route -> {
			// We want all routes that share the same criteria as the one defined in this route manager
			if(this.route.errors != null && !this.route.errors.isEmpty()) {
				if(route.getErrors() == null || route.getErrors().isEmpty() || !this.route.errors.containsAll(route.getErrors())) {
					return false;
				}
			}
			if(this.route.produces != null && !this.route.produces.isEmpty()) {
				if(route.getProduces() == null || route.getProduces().isEmpty() || !this.route.produces.containsAll(route.getProduces())) {
					return false;
				}
			}
			if(this.route.languages != null && !this.route.languages.isEmpty()) {
				if(route.getLanguages() == null || route.getLanguages().isEmpty() || !this.route.languages.containsAll(route.getLanguages())) {
					return false;
				}
			}
			return true;
		}).collect(Collectors.toSet());
	}

	@Override
	public ErrorRouteManager error(Class<? extends Throwable> error) throws IllegalArgumentException {
		if(this.route.errors == null) {
			this.route.errors = new LinkedHashSet<>();
		}
		this.route.errors.add(error);
		return this;
	}

	@Override
	public ErrorRouteManager produces(String mediaType) {
		if(this.route.produces == null) {
			this.route.produces = new LinkedHashSet<>();
		}
		this.route.produces.add(mediaType);
		return this;
	}

	@Override
	public ErrorRouteManager language(String language) {
		if(this.route.languages == null) {
			this.route.languages = new LinkedHashSet<>();
		}
		this.route.languages.add(language);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ErrorRouter handler(ExchangeHandler<? super Void, ? super ResponseBody, ? super ErrorExchange<ResponseBody, Throwable>> handler) {
		Objects.requireNonNull(handler);
		this.route.handler = (ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>>) handler;
		this.router.addRoute(this.route);
		return this.router;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ErrorRouter handler(ErrorExchangeHandler<ResponseBody, ? extends Throwable> handler) {
		Objects.requireNonNull(handler);
		// This might throw a class cast exception if the handler is not associated with corresponding class types
		this.route.handler = (ErrorExchangeHandler<ResponseBody, Throwable>) handler;
		this.router.addRoute(this.route);
		return this.router;
	}
}
