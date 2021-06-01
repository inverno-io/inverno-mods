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
package io.inverno.mod.http.server.internal;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Overridable;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;

/**
 * <p>
 * The server exchange handler which by default returns {@code Hello} when a
 * request is made to the {@code /} resource.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean
@Wrapper
@Overridable 
public class RootHandler implements Supplier<ExchangeHandler<Exchange>> {

	@Override
	public ExchangeHandler<Exchange> get() {
		return exchange -> {
			if(exchange.request().getPathAbsolute().equalsIgnoreCase("/")) {
				exchange.response().body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)));
			}
			else {
				throw new NotFoundException();
			}
		};
	}
}
