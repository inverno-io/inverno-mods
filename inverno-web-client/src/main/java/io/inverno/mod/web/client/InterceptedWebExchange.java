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
import io.inverno.mod.http.client.InterceptedExchange;
import java.util.function.Function;
import reactor.core.publisher.Mono;

/**
 * <p>
 * An intercepted Web exchange extending HTTP client {@link InterceptedExchange}.
 * </p>
 *
 * <p>
 * It especially provides response body encoding based on the response content type as well as configurable error status mapper.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public interface InterceptedWebExchange<A extends ExchangeContext> extends InterceptedExchange<A> {

	/**
	 * <p>
	 * Specifies the error status mapper to convert error responses (i.e. with a {@code 4xx} or {@code 5xx} status) to exceptions.
	 * </p>
	 *
	 * <p>
	 * This mapper is used when a Web exchange has been set to fail on error responses (see {@link WebExchange#failOnErrorStatus(boolean)}) and only when the response status code is greater than 400
	 * included. It must map the error response to an error {@code Mono} when a particular error must be propagated or an empty {@code Mono} if the error should not be propagated.
	 * </p>
	 *
	 * @param errorMapper an error response mapper returning an error {@code Mono} to propagate the error or an empty {@code Mono} to ignore it
	 *
	 * @return the intercepted Web exchange
	 */
	InterceptedWebExchange<A> failOnErrorStatus(Function<WebResponse, Mono<Void>> errorMapper);

	@Override
	InterceptedWebRequest request();
	
	@Override
	InterceptedWebResponse response();
}
