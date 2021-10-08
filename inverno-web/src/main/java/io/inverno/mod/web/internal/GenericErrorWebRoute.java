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

import io.inverno.mod.http.server.ErrorExchangeHandler;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ReactiveExchangeHandler;
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.ErrorWebRoute;

/**
 * <p>
 * Generic {@link ErrorWebRoute} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericErrorWebRoute implements ErrorWebRoute {

	private final GenericErrorWebRouter router;
	
	private boolean disabled;

	private Class<? extends Throwable> error;
	
	private String produce;
	
	private String language;
	
	private ErrorExchangeHandler<Throwable, ErrorWebExchange<Throwable>> handler;
	
	/**
	 * <p>
	 * Creates a generic error web route in the specified generic error web router.
	 * </p>
	 * 
	 * @param router a generic error web router
	 */
	public GenericErrorWebRoute(GenericErrorWebRouter router) {
		this.router = router;
	}

	@Override
	public void enable() {
		this.router.enableRoute(this);
		this.disabled = false;
	}

	@Override
	public void disable() {
		this.router.disableRoute(this);
		this.disabled = true;
	}
	
	@Override
	public boolean isDisabled() {
		return this.disabled;
	}
	
	/**
	 * <p>
	 * Disables/Enables the error web route.
	 * </p>
	 * 
	 * @param disabled true to disable the route, false otherwise
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public void remove() {
		this.router.removeRoute(this);
	}
	
	@Override
	public ReactiveExchangeHandler<ExchangeContext ,ErrorWebExchange<Throwable>> getHandler() {
		return this.handler;
	}
	
	/**
	 * <p>
	 * Sets the route exchange handler as defined by
	 * {@link ErrorWebRoute#getHandler()}.
	 * </p>
	 * 
	 * @param handler an exchange handler
	 */
	public void setHandler(ErrorExchangeHandler<Throwable, ErrorWebExchange<Throwable>> handler) {
		this.handler = handler;
	}
	
	@Override
	public Class<? extends Throwable> getError() {
		return this.error;
	}
	
	/**
	 * <p>
	 * Sets the route error type as defined by {@link ErrorWebRoute#getError()}
	 * </p>
	 * 
	 * @param error an error type
	 */
	public void setError(Class<? extends Throwable> error) {
		this.error = error;
	}
	
	@Override
	public String getLanguage() {
		return this.language;
	}
	
	/**
	 * <p>
	 * Sets the route language as defined by {@link ErrorWebRoute#getLanguage()}
	 * </p>
	 * 
	 * @param language a language tag
	 */
	public void setLanguage(String language) {
		this.language = language;
	}
	
	@Override
	public String getProduce() {
		return this.produce;
	}
	
	/**
	 * <p>
	 * Sets the route produced media type as defined by
	 * {@link ErrorWebRoute#getProduce()}
	 * </p>
	 * 
	 * @param mediaType a media type
	 */
	public void setProduce(String mediaType) {
		this.produce = mediaType;
	}
}
