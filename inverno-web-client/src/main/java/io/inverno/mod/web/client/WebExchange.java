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
package io.inverno.mod.web.client;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.web.client.ws.Web2SocketExchange;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An exchange that extends HTTP client {@link Exchange} with features for the Web.
 * </p>
 *
 * <p>
 * It supports request body encoding based on the request content type as well as response body decoding based on the response content type.
 * </p>
 *
 * <p>
 * It gives access to the request URI and exposes a context which used to propagate contextual information throughout the processing of the exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public interface WebExchange<A extends ExchangeContext> extends Exchange<A> {

	/**
	 * <p>
	 * Specifies whether an {@link io.inverno.mod.http.base.HttpException} should be raised when a client or server error response is received.
	 * </p>
	 *
	 * <p>
	 * When set to true, the {@link #response()} returns a Mono that fails when the response has a client ({@code 4xx}) or server ({@code 5xx}) error status code. This is the default behaviour.
	 * </p>
	 *
	 * <p>
	 * It is possible to customize how the response is mapped to the error in an exchange interceptor with {@link InterceptedWebExchange#failOnErrorStatus(Function)}.
	 * </p>
	 *
	 * @param failOnErrorStatus true to raise an HTTP exception on client ({@code 4xx}) or server({@code 5xx}) error, false otherwise
	 *
	 * @return the Web exchange
	 */
	WebExchange<A> failOnErrorStatus(boolean failOnErrorStatus);

	@Override
	WebRequest request();

	@Override
	Mono<WebResponse> response();

	@Override
	default Mono<Web2SocketExchange<A>> webSocket() {
		return this.webSocket(null);
	}

	@Override
	Mono<Web2SocketExchange<A>> webSocket(String subProtocol);

	/**
	 * <p>
	 * A Web exchange configurer.
	 * </p>
	 *
	 * @param <A> the exchange context type
	 */
	@FunctionalInterface
	interface Configurer<A extends ExchangeContext> {

		/**
		 * <p>
		 * Configures the specified exchange.
		 * </p>
		 *
		 * @param exchange the exchange to configure
		 */
		void configure(WebExchange<? extends A> exchange);
	}
}
