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

import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.router.ErrorRoute;

/**
 * @author jkuhn
 *
 */
public class GenericErrorRoute implements ErrorRoute {

	private GenericErrorRouter router;
	
	private boolean disabled;

	private Class<? extends Throwable> error;
	
	private String produce;
	
	private String language;
	
	private ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> handler;
	
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
	
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public void remove() {
		this.router.removeRoute(this);
	}
	
	public void setHandler(ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> handler) {
		this.handler = handler;
	}
	
	@Override
	public Class<? extends Throwable> getError() {
		return this.error;
	}
	
	public void setError(Class<? extends Throwable> error) {
		this.error = error;
	}
	
	@Override
	public String getLanguage() {
		return this.language;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}
	
	@Override
	public String getProduce() {
		return this.produce;
	}
	
	public void setProduce(String produce) {
		this.produce = produce;
	}
	
	@Override
	public ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> getHandler() {
		return this.handler;
	}
}
