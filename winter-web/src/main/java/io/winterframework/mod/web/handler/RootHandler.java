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
import java.io.InputStream;
import java.util.function.Supplier;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Overridable;
import io.winterframework.core.annotation.Wrapper;
import io.winterframework.mod.web.RequestBody;
import io.winterframework.mod.web.RequestHandler;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.Status;
import io.winterframework.mod.web.router.Router;
import io.winterframework.mod.web.router.WebContext;

/**
 * @author jkuhn
 *
 */
@Bean
@Wrapper
@Overridable 
public class RootHandler implements Supplier<RequestHandler<RequestBody, ResponseBody, Void>> {

	private byte[] favicon;
	
	private byte[] getFavicon() {
		if(this.favicon == null) {
			try(InputStream faviconInput = this.getClass().getClassLoader().getResourceAsStream("winter_favicon.svg")) {
				this.favicon = faviconInput.readAllBytes();
			} 
			catch (IOException e) {
				// TODO proper error handling
				e.printStackTrace();
			}
		}
		return this.favicon;
	}
	
	@Override
	public RequestHandler<RequestBody, ResponseBody, Void> get() {
		return Router.web()
			.route().path("/favicon.ico").handler(this.faviconHandler())
			.route().handler(this.defaultHandler());
	}
	
	private RequestHandler<RequestBody, ResponseBody, WebContext> faviconHandler() {
		return (request, response) -> {
			response.headers(headers -> headers.status(Status.OK).contentType("image/svg+xml"))
				.body().raw().data(this.getFavicon());
		};
	}
	
	private RequestHandler<RequestBody, ResponseBody, WebContext> defaultHandler() {
		return (request, response) -> {
			response.headers(headers -> headers.status(Status.OK).contentType("text/html"))
				.body().raw().data("<html><head><title>Winter</title></head><body><h1>Hello</h1></body></html>");
		};
	}
}
