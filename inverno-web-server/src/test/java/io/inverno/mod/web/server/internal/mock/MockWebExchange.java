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
package io.inverno.mod.web.server.internal.mock;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.web.server.Web2SocketExchange;
import io.inverno.mod.web.server.WebExchange;
import java.util.Optional;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 *
 */
public class MockWebExchange implements WebExchange<ExchangeContext> {

	private final HttpVersion protocol;
	private final MockWebRequest mockRequest;
	private final MockWebResponse mockResponse;
	private final ExchangeContext context;
	
	public MockWebExchange(HttpVersion protocol, MockWebRequest mockRequest, MockWebResponse mockResponse, ExchangeContext context) {
		this.protocol = protocol;
		this.mockRequest = mockRequest;
		this.mockResponse = mockResponse;
		this.context = context;
	}
	
	public static MockExchangeBuilder from(String path) {
		return new MockExchangeBuilder().path(path);
	}
	
	public static MockExchangeBuilder from(String path, Method method) {
		return new MockExchangeBuilder().method(method).path(path);
	}

	@Override
	public HttpVersion getProtocol() {
		return this.protocol;
	}
	
	@Override
	public ExchangeContext context() {
		return this.context;
	}
	
	@Override
	public MockWebRequest request() {
		return this.mockRequest;
	}

	@Override
	public MockWebResponse response() {
		return this.mockResponse;
	}

	@Override
	public Optional<? extends WebSocket<ExchangeContext, Web2SocketExchange<ExchangeContext>>> webSocket(String... subProtocols) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void reset(long code) {
	}

	@Override
	public Optional<Throwable> getCancelCause() {
		return Optional.empty();
	}
}
