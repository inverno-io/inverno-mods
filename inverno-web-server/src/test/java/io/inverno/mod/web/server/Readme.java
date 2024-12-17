/*
 * Copyright 2024 Jeremy Kuhn
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
package io.inverno.mod.web.server;

import io.inverno.core.v1.Application;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.HttpAccessLogsInterceptor;
import io.netty.buffer.ByteBuf;
import java.util.List;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class Readme {
	public void moduleInfo() {
		List<MediaTypeConverter<ByteBuf>> mediaTypeConverters = null;
		NetService netService = null;
		Reactor reactor = null;
		ResourceService resourceService = null;

		Application.run(new Server.Builder(mediaTypeConverters, netService, reactor, resourceService)
			.setConfiguration(WebServerConfigurationLoader.load(conf -> conf.http_server(http_conf -> http_conf.server_port(8080))))
		)
		.webServerBoot().webServer()
			.intercept().interceptor(new HttpAccessLogsInterceptor<>())
			.route()
				.path("/ path/to/resource1")
				.method(Method.GET)
				.produce(MediaTypes.APPLICATION_JSON)
				.produce(MediaTypes.TEXT_PLAIN)
				.handler(exchange -> exchange.response().body().encoder().value("Resource 1"))
			.route()
				.path("/path/to/resource2")
				.method(Method.GET)
				.produce(MediaTypes.APPLICATION_JSON)
				.produce(MediaTypes.TEXT_PLAIN)
				.handler(exchange -> exchange.response().body().encoder().value("Resource 2"))
			.interceptError().interceptor(new HttpAccessLogsInterceptor<>())
			.configureErrorRoutes(new WhiteLabelErrorRoutesConfigurer<>());
	}
}
