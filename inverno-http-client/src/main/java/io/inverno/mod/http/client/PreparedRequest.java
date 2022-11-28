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

import io.inverno.mod.http.base.BaseRequest;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.OutboundRequestHeaders;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface PreparedRequest<A extends ExchangeContext, B extends Exchange<A>, C extends PreExchange<A>> extends BaseRequest {

	PreparedRequest<A, B, C> headers(Consumer<OutboundRequestHeaders> headersConfigurer) throws IllegalStateException;
	
	PreparedRequest<A, B, C> authority(String authority);

	PreparedRequest<A, B, C> intercept(ExchangeInterceptor<A, C> interceptor);

	default Mono<B> send() {
		return this.send((A)null, List.of(), null);
	}
	
	default Mono<B> send(List<Object> parameters) {
		return this.send((A)null, Arrays.asList(parameters), null);
	}

	default Mono<B> send(Map<String, ?> parameters) {
		return this.send((A)null, parameters, null);
	}

	default Mono<B> send(Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send((A)null, List.of(), bodyConfigurer);
	}
	
	default Mono<B> send(A context) {
		return this.send(context, List.of(), null);
	}
	
	default Mono<B> send(List<Object> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send((A)null, parameters, bodyConfigurer);
	}

	default Mono<B> send(Map<String, ?> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer) {
		return this.send((A)null, parameters, bodyConfigurer);
	}
	
	default Mono<B> send(A context, List<Object> parameters) {
		return this.send(context, parameters, null);
	}
	
	Mono<B> send(A context, List<Object> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer);
	
	default Mono<B> send(A context, Map<String, ?> parameters) {
		return this.send(context, parameters, null);
	}
	
	Mono<B> send(A context, Map<String, ?> parameters, Consumer<RequestBodyConfigurator> bodyConfigurer);
}