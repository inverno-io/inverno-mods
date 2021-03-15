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

import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.ExchangeHandler;
import io.winterframework.mod.web.ErrorRoute;

/**
 * <p>
 * Generic {@link ErrorRoute} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
class GenericErrorRoute implements ErrorRoute {

	private final GenericErrorRouter router;
	
	private boolean disabled;

	private Class<? extends Throwable> error;
	
	private String produce;
	
	private String language;
	
	private ExchangeHandler<ErrorExchange<Throwable>> handler;
	
	/**
	 * <p>
	 * Creates a generic error route in the specified generic error router.
	 * </p>
	 * 
	 * @param router a generic error router
	 */
	public GenericErrorRoute(GenericErrorRouter router) {
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
	 * Disables/Enables the error route.
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
	
	/**
	 * <p>
	 * Sets the route exchange handler as defined by
	 * {@link ErrorRoute#getHandler()}.
	 * </p>
	 * 
	 * @param handler an exchange handler
	 */
	public void setHandler(ExchangeHandler<ErrorExchange<Throwable>> handler) {
		this.handler = handler;
	}
	
	@Override
	public Class<? extends Throwable> getError() {
		return this.error;
	}
	
	/**
	 * <p>
	 * Sets the route error type as defined by {@link ErrorRoute#getError()}
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
	 * Sets the route language as defined by {@link ErrorRoute#getLanguage()}
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
	 * {@link ErrorRoute#getProduce()}
	 * </p>
	 * 
	 * @param mediaType a media type
	 */
	public void setProduce(String mediaType) {
		this.produce = mediaType;
	}
	
	@Override
	public ExchangeHandler<ErrorExchange<Throwable>> getHandler() {
		return this.handler;
	}
}
