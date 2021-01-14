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
package io.winterframework.mod.web.server.handler;

import java.net.URI;
import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Overridable;
import io.winterframework.core.annotation.Wrapper;
import io.winterframework.mod.base.resource.Resource;
import io.winterframework.mod.base.resource.ResourceService;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.server.Exchange;
import io.winterframework.mod.web.server.ExchangeHandler;

/**
 * @author jkuhn
 *
 */
@Bean
@Wrapper
@Overridable 
public class RootHandler implements Supplier<ExchangeHandler<Exchange>> {

	private ResourceService resourceService;
	
	public RootHandler(ResourceService resourceService) {
		this.resourceService = resourceService;
	}
	
	@Override
	public ExchangeHandler<Exchange> get() {
		return exchange -> {
			if(exchange.request().headers().getPath().equalsIgnoreCase("/favicon.ico")) {
				try(Resource favicon = this.resourceService.get(URI.create("classpath:/winter_favicon.svg"))) {
					exchange.response().body().resource().data(favicon);
				} 
				catch (Exception e) {
					throw new NotFoundException();
				}
			}
			else {
				throw new NotFoundException();
			}
		};
	}
}
