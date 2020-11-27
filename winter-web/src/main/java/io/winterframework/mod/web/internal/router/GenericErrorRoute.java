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

import java.util.Collections;
import java.util.Set;

import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.ErrorRoute;

/**
 * @author jkuhn
 *
 */
public class GenericErrorRoute implements ErrorRoute {

	protected GenericErrorRouter router;

	protected Set<Class<? extends Throwable>> errors;
	
	protected Set<String> produces;
	
	protected Set<String> languages;
	
	protected RequestHandler<Void, ResponseBody, Throwable> handler;
	
	public GenericErrorRoute(GenericErrorRouter router) {
		this.router = router;
	}

	@Override
	public RequestHandler<Void, ResponseBody, Throwable> getHandler() {
		return this.handler;
	}

	@Override
	public void enable() {
		this.router.enableRoute(this);
	}

	@Override
	public void disable() {
		this.router.disableRoute(this);
	}

	@Override
	public void remove() {
		this.router.removeRoute(this);
	}
	
	@Override
	public Set<Class<? extends Throwable>> getErrors() {
		return this.errors != null ? Collections.unmodifiableSet(this.errors) : Set.of();
	}
	
	public void setErrors(Set<Class<? extends Throwable>> errors) {
		this.errors = errors;
	}
	
	@Override
	public Set<String> getLanguages() {
		return this.languages != null ? Collections.unmodifiableSet(this.languages) : Set.of();
	}
	
	public void setLanguages(Set<String> languages) {
		this.languages = languages;
	}
	
	@Override
	public Set<String> getProduces() {
		return this.produces != null ? Collections.unmodifiableSet(this.produces) : Set.of();
	}
	
	public void setProduces(Set<String> produces) {
		this.produces = produces;
	}
}
