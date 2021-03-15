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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.ErrorExchangeHandler;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.web.ErrorRoute;
import io.winterframework.mod.web.ErrorRouteManager;
import io.winterframework.mod.web.ErrorRouter;

/**
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 *
 */
class GenericErrorRouteManager implements ErrorRouteManager {

	private final GenericErrorRouter router;
	
	private Set<Class<? extends Throwable>> errors;
	
	private Set<String> produces;
	
	private Set<String> languages;
	
	private ExchangeHandler<ErrorExchange<Throwable>> handler;
	
	public GenericErrorRouteManager(GenericErrorRouter router) {
		this.router = router;
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
			if(this.errors != null && !this.errors.isEmpty()) {
				if(route.getError() == null || !this.errors.contains(route.getError())) {
					return false;
				}
			}
			if(this.produces != null && !this.produces.isEmpty()) {
				if(route.getProduce() == null || !this.produces.contains(route.getProduce())) {
					return false;
				}
			}
			if(this.languages != null && !this.languages.isEmpty()) {
				if(route.getLanguage() == null || !this.languages.contains(route.getLanguage())) {
					return false;
				}
			}
			return true;
		}).collect(Collectors.toSet());
	}

	@Override
	public ErrorRouteManager error(Class<? extends Throwable> error) {
		if(this.errors == null) {
			this.errors = new LinkedHashSet<>();
		}
		this.errors.add(error);
		return this;
	}

	@Override
	public ErrorRouteManager produces(String mediaType) {
		if(this.produces == null) {
			this.produces = new LinkedHashSet<>();
		}
		this.produces.add(mediaType);
		return this;
	}

	@Override
	public ErrorRouteManager language(String language) {
		if(this.languages == null) {
			this.languages = new LinkedHashSet<>();
		}
		this.languages.add(language);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ErrorRouter handler(ExchangeHandler<? super ErrorExchange<Throwable>> handler) {
		Objects.requireNonNull(handler);
		this.handler = (ExchangeHandler<ErrorExchange<Throwable>>) handler;
		this.commit();
		return this.router;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ErrorRouter handler(ErrorExchangeHandler<? extends Throwable> handler) {
		Objects.requireNonNull(handler);
		// This might throw a class cast exception if the handler is not associated with corresponding class types
		this.handler = (ErrorExchangeHandler<Throwable>) handler;
		this.commit();
		return this.router;
	}
	
	private void commit() {
		Consumer<GenericErrorRoute> languagesCommitter = route -> {
			if(this.languages != null && !this.languages.isEmpty()) {
				for(String language : this.languages) {
					route.setLanguage(language);
					route.setHandler(this.handler);
					this.router.setRoute(route);
				}
			}
			else {
				route.setHandler(this.handler);
				this.router.setRoute(route);
			}
		};
		
		Consumer<GenericErrorRoute> producesCommitter = route -> {
			if(this.produces != null && !this.produces.isEmpty()) {
				for(String produce : this.produces) {
					route.setProduce(produce);
					languagesCommitter.accept(route);
				}
			}
			else {
				languagesCommitter.accept(route);
			}
		};
		
		Consumer<GenericErrorRoute> errorsCommitter = route -> {
			if(this.errors != null && !this.errors.isEmpty()) {
				for(Class<? extends Throwable> error : this.errors) {
					route.setError(error);
					producesCommitter.accept(route);
				}
			}
			else {
				producesCommitter.accept(route);
			}
		};
		errorsCommitter.accept(new GenericErrorRoute(this.router));
	}
}
