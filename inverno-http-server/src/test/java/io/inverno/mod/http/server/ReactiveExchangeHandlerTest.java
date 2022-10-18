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
package io.inverno.mod.http.server;

import io.inverno.mod.http.base.ExchangeContext;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class ReactiveExchangeHandlerTest {
	
	@Test
	@SuppressWarnings("unchecked")
	public void testIntercept() {
		AtomicBoolean handler = new AtomicBoolean(false);
		AtomicBoolean interceptor = new AtomicBoolean(false);
		
		ExchangeInterceptor<ExchangeContext, Exchange<ExchangeContext>> ei = exchange -> {
			interceptor.set(true);
			return Mono.just(exchange);
		};
		
		ReactiveExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> reh = exchange -> {
			handler.set(true);
			return Mono.empty();
		};
		
		ReactiveExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> ireh = reh.intercept(ei);
		
		Exchange<ExchangeContext> ex = (Exchange<ExchangeContext>)Mockito.mock(Exchange.class);
		
		ireh.defer(ex).block();
		
		Assertions.assertTrue(interceptor.get());
		Assertions.assertTrue(handler.get());
		
		interceptor.set(false);
		handler.set(false);
		
		ei = exchange -> {
			interceptor.set(true);
			return Mono.empty();
		};
		
		ireh = reh.intercept(ei);
		
		ireh.defer(ex).block();
		
		Assertions.assertTrue(interceptor.get());
		Assertions.assertFalse(handler.get());
	}
}
