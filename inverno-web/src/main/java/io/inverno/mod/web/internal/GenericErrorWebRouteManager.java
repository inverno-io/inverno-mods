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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ErrorExchangeHandler;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.ErrorWebExchangeHandler;
import io.inverno.mod.web.ErrorWebRoute;
import io.inverno.mod.web.ErrorWebRouteManager;
import io.inverno.mod.web.ErrorWebRouter;

/**
 * <p>
 * Generic {@link ErrorWebRouteManager} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericErrorWebRouteManager implements ErrorWebRouteManager {

	private final GenericErrorWebRouter router;
	
	private Set<Class<? extends Throwable>> errors;
	
	private Set<String> produces;
	
	private Set<String> languages;
	
	private ExchangeHandler<ExchangeContext, ErrorWebExchange<Throwable>> handler;
	
	/**
	 * <p>
	 * Creates a generic error web route manager.
	 * </p>
	 * 
	 * @param router the generic error web router
	 */
	public GenericErrorWebRouteManager(GenericErrorWebRouter router) {
		this.router = router;
	}

	@Override
	public ErrorWebRouter enable() {
		this.findRoutes().stream().forEach(route -> route.enable());
		return this.router;
	}

	@Override
	public ErrorWebRouter disable() {
		this.findRoutes().stream().forEach(route -> route.disable());
		return this.router;
	}

	@Override
	public ErrorWebRouter remove() {
		this.findRoutes().stream().forEach(route -> route.remove());
		return this.router;
	}

	@Override
	public Set<ErrorWebRoute> findRoutes() {
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
	public ErrorWebRouteManager error(Class<? extends Throwable> error) {
		if(this.errors == null) {
			this.errors = new LinkedHashSet<>();
		}
		this.errors.add(error);
		return this;
	}

	@Override
	public ErrorWebRouteManager produces(String mediaType) {
		if(this.produces == null) {
			this.produces = new LinkedHashSet<>();
		}
		this.produces.add(mediaType);
		return this;
	}

	@Override
	public ErrorWebRouteManager language(String language) {
		if(this.languages == null) {
			this.languages = new LinkedHashSet<>();
		}
		this.languages.add(language);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ErrorWebRouter handler(ErrorExchangeHandler<? extends Throwable, ? extends ErrorExchange<? extends Throwable>> handler) {
		Objects.requireNonNull(handler);
		this.handler = (ExchangeHandler<ExchangeContext, ErrorWebExchange<Throwable>>) handler;
		this.commit();
		return this.router;
	}
	
	@Override
	public ErrorWebRouter handler(ErrorWebExchangeHandler<Throwable> handler) {
		Objects.requireNonNull(handler);
		this.handler = handler;
		this.commit();
		return this.router;
	}
	
	private void commit() {
		Consumer<GenericErrorWebRoute> languagesCommitter = route -> {
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
		
		Consumer<GenericErrorWebRoute> producesCommitter = route -> {
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
		
		Consumer<GenericErrorWebRoute> errorsCommitter = route -> {
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
		errorsCommitter.accept(new GenericErrorWebRoute(this.router));
	}
}
