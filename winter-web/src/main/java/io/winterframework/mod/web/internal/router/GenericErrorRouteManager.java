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
import java.util.List;
import java.util.Objects;

import io.winterframework.mod.web.RequestHandler;
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
		this.route.enable();
		return this.router;
	}

	@Override
	public ErrorRouter disable() {
		this.route.disable();
		return this.router;
	}

	@Override
	public ErrorRouter remove() {
		this.route.remove();
		return this.router;
	}

	@Override
	public List<ErrorRoute> findRoutes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Throwable> ErrorRouteManager error(Class<T> error) throws IllegalArgumentException {
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

	@Override
	public ErrorRouter handler(RequestHandler<Void, ResponseBody, Throwable> handler) {
		Objects.requireNonNull(handler);
		this.route.handler = handler;
		this.router.addRoute(this.route);
		return this.router;
	}
}
