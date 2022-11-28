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
package io.inverno.mod.http.client.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.RequestBodyConfigurator;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface HttpConnection {
	
	boolean isTls();
	
	HttpVersion getProtocol();
	
	Long getMaxConcurrentRequests();
	
	void setHandler(HttpConnection.Handler handler);

	default <A extends ExchangeContext> Mono<Exchange<A>> send(
			A exchangeContext,
			Method method, 
			String authority, 
			List<Map.Entry<String, String>> headers, 
			String path) {
		return this.send(exchangeContext, method, authority, headers, path, null, null, null);
	}
	
	default <A extends ExchangeContext> Mono<Exchange<A>> send(
			A exchangeContext,
			Method method, 
			String authority, 
			List<Map.Entry<String, String>> headers, 
			String path, 
			Consumer<RequestBodyConfigurator> requestBodyConfigurer) {
		return this.send(exchangeContext, method, authority, headers, path, requestBodyConfigurer, null, null);
	}
	
	<A extends ExchangeContext> Mono<Exchange<A>> send(
			A exchangeContext,
			Method method, 
			String authority, 
			List<Map.Entry<String, String>> headers, 
			String path, 
			Consumer<RequestBodyConfigurator> requestBodyConfigurer,
			Function<Publisher<ByteBuf>, Publisher<ByteBuf>> requestBodyTransformer,
			Function<Publisher<ByteBuf>, Publisher<ByteBuf>> responseBodyTransformer);
	
	// TODO shutdown gracefully
	Mono<Void> close();
	
	interface Handler {
		
		void onUpgrade(HttpConnection upgradedConnection);
		
		void onSettingsChange(long maxConcurrentRequests);
		
		void onExchangeTerminate(Exchange<?> exchange);

		void onError(Throwable t);

		void onClose();
	}
}
