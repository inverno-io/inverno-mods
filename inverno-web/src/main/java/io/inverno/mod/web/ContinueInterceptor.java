/*
 * Copyright 2021 Jeremy KUHN
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
package io.inverno.mod.web;

import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeContext;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A web exchange interceptor that sends an interim 100 Continue response to the client on requests that contain {@code expect: 100-continue} HTTP header as defined by
 * <a href="https://tools.ietf.org/html/rfc7231#section-5.1.1">RFC 7231 Section 5.1.1</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
public class ContinueInterceptor implements WebExchangeInterceptor<ExchangeContext> {

	@Override
	public Mono<? extends WebExchange<ExchangeContext>> intercept(WebExchange<ExchangeContext> exchange) {
		if(exchange.request().headers().contains(Headers.NAME_EXPECT, Headers.VALUE_100_CONTINUE)) {
			exchange.response().sendContinue();
		}
		return Mono.just(exchange);
	}
}
