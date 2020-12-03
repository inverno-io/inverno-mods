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
package io.winterframework.mod.web.handler;

import java.io.IOException;
import java.net.URI;
import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Overridable;
import io.winterframework.core.annotation.Wrapper;
import io.winterframework.mod.commons.resource.ClasspathResource;
import io.winterframework.mod.commons.resource.Resource;
import io.winterframework.mod.commons.resource.ResourceException;
import io.winterframework.mod.web.Exchange;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.NotFoundException;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.Status;
import io.winterframework.mod.web.router.Router;

/**
 * @author jkuhn
 *
 */
@Bean
@Wrapper
@Overridable 
public class RootHandler implements Supplier<ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>>> {

	private static final URI FAVICON_URI = URI.create("classpath:/winter_favicon.svg");
	
	@Override
	public ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> get() {
		return Router.web()
			.route().path("/favicon.ico").handler(this.faviconHandler())
			.route().handler(this.defaultHandler());
	}
	
	private ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> faviconHandler() {
		return exchange -> {
			try(Resource favicon = new ClasspathResource(FAVICON_URI)) {
				Boolean exists = favicon.exists();
				if(exists == null || exists) {
					exchange.response()
						.headers(headers -> headers.status(Status.OK).contentType("image/svg+xml"))
						.body().resource().data(favicon);
				}
				else {
					throw new NotFoundException();
				}
			} 
			catch (IllegalArgumentException | ResourceException | IOException e) {
				throw new NotFoundException();
			}
		};
	}
	
	private ExchangeHandler<RequestBody, ResponseBody, Exchange<RequestBody, ResponseBody>> defaultHandler() {
		return exchange -> {
			exchange.response()
				.headers(headers -> headers.status(Status.OK).contentType("text/html"))
				.body().raw().data("<html><head><title>Winter</title></head><body><h1>Hello</h1></body></html>");
		};
	}
}
