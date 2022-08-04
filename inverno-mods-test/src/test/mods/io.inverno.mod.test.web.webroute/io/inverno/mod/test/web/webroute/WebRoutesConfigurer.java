/*
 * Copyright 2022 Jeremy KUHN
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
package io.inverno.mod.test.web.webroute;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.base.resource.FileResource;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.StaticHandler;
import io.inverno.mod.web.WebRoutable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Visibility.PRIVATE )
public class WebRoutesConfigurer implements io.inverno.mod.web.WebRoutesConfigurer<ExchangeContext> {

	@Override
	public void configure(WebRoutable<ExchangeContext, ?> routes) {
		routes
			.route()
				.path("/static/{path:.*}", true)
				.method(Method.GET)
				.handler(new StaticHandler<>(new FileResource("web-root/")))
			.route()
				.path("/upload")
				.method(Method.POST)
				.handler(exchange -> {
					exchange.response()
				        .body().raw().stream(Flux.from(exchange.request().body().get().multipart().stream())
				            .flatMap(part -> part.getFilename()
				                .map(fileName -> Flux.<ByteBuf, FileResource>using(
				                        () -> new FileResource("uploads/" + part.getFilename().get()),
				                        file -> file.write(part.raw().stream()).map(Flux::from).get()
				                            .reduce(0, (acc, cur) -> acc + cur)
				                            .map(size -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Uploaded " + fileName + "(" + part.headers().getContentType() + "): " + size + " Bytes\n", Charsets.DEFAULT))), 
				                        FileResource::close
				                    )
				                )
				                .orElseThrow(() -> new BadRequestException("Not a file part"))
				            )
				        );
				});
	}

}
