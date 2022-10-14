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

package io.inverno.mod.http.client;

import io.inverno.mod.http.base.ExchangeContext;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface PreparedRequest<A extends ExchangeContext, B extends Exchange<A>> extends BaseRequest {

	@Override
	PreparedRequest<A, B> headers(Consumer<RequestHeaders> headersConfigurer) throws IllegalStateException;

	@Override
	PreparedRequest<A, B> cookies(Consumer<RequestCookies> cookiesConfigurer) throws IllegalStateException;

	@Override
	PreparedRequest<A, B> authority(String authority);
	
	PreparedRequest<A, B> intercept(ExchangeInterceptor<? super A, ? extends B> interceptor);

	default Mono<B> send(Object... parameters) {
		return this.send(Arrays.asList(parameters), (A)null);
	}
	
	default Mono<B> send(List<Object> parameters) {
		return this.send(Arrays.asList(parameters), (A)null);
	}

	default Mono<B> send(Map<String, ?> parameters) {
		return this.send(parameters, (A)null);
	}

	default Mono<B> send(Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send(List.of(), bodyConfigurer, (A)null);
	}
	
	default Mono<B> send(A context) {
		return this.send(List.of(), (A)null);
	}
	
	default Mono<B> send(List<Object> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send(parameters, bodyConfigurer, (A)null);
	}

	default Mono<B> send(Map<String, ?> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send(parameters, bodyConfigurer, (A)null);
	}
	
	Mono<B> send(List<Object> parameters, A context);
	
	Mono<B> send(List<Object> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer, A context);
	
	Mono<B> send(Map<String, ?> parameters, A context);
	
	Mono<B> send(Map<String, ?> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer, A context);
}