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
package io.inverno.mod.web.server.internal;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptCodec;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.ws.WebSocket;
import io.inverno.mod.http.server.ws.WebSocketExchange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public final class MockExchanges {

	private static final HeaderCodec<? extends Headers.ContentType> CONTENT_TYPE_CODEC = new ContentTypeCodec();
	private static final HeaderCodec<? extends Headers.Accept> ACCEPT_CODEC = new AcceptCodec(true);
	private static final HeaderCodec<? extends Headers.AcceptLanguage> ACCEPT_LANGUAGE_CODEC = new AcceptLanguageCodec(true);

	private MockExchanges() {}

	public static MockExchangeBuilder<ExchangeContext> exchangeBuilder(Method method, String path) {
		return new MockExchangeBuilder<>(method, path, null);
	}

	public static <T extends ExchangeContext> MockExchangeBuilder<T> exchangeBuilder(Method method, String path, T context) {
		return new MockExchangeBuilder<>(method, path, context);
	}

	public static MockErrorExchangeBuilder<ExchangeContext> errorExchangeBuilder(Throwable error, String path) {
		return new MockErrorExchangeBuilder<>(error, path, null);
	}

	public static <T extends ExchangeContext> MockErrorExchangeBuilder<T> errorExchangeBuilder(Throwable error, String path, T context) {
		return new MockErrorExchangeBuilder<>(error, path, context);
	}

	public static class MockExchangeBuilder<A extends ExchangeContext> {

		private final Method method;
		private final String path;
		private final A context;
		private final Map<String, String> headers;

		private List<String> supportedSubProtocols;

		private MockExchangeBuilder(Method method, String path, A context) {
			this.method = method;
			this.path = path;
			this.context = context;
			this.headers = new HashMap<>();
		}

		public MockExchangeBuilder<A> contentType(String contentType) {
			this.headers.put(Headers.NAME_CONTENT_TYPE, contentType);
			return this;
		}

		public MockExchangeBuilder<A> accept(String accept) {
			this.headers.put(Headers.NAME_ACCEPT, accept);
			return this;
		}

		public MockExchangeBuilder<A> acceptLanguage(String acceptLanguage) {
			this.headers.put(Headers.NAME_ACCEPT_LANGUAGE, acceptLanguage);
			return this;
		}

		public MockExchangeBuilder<A> header(String name, String value) {
			this.headers.put(name, value);
			return this;
		}

		public MockExchangeBuilder<A> subprotocol(String subprotocol) {
			this.headers.put(Headers.NAME_SEC_WEBSOCKET_PROTOCOL, subprotocol);
			return this;
		}

		public MockExchangeBuilder<A> supportedSubProtocol(List<String> supportedSubProtocol) {
			this.supportedSubProtocols = supportedSubProtocol;
			return this;
		}

		@SuppressWarnings("unchecked")
		public Exchange<A> build() {
			Exchange<A> exchangeMock = (Exchange<A>)Mockito.mock(Exchange.class);

			Mockito.when(exchangeMock.context()).thenReturn(this.context);

			Request requestMock = Mockito.mock(Request.class);

			Mockito.when(requestMock.getMethod()).thenReturn(this.method);
			Mockito.when(requestMock.getPathAbsolute()).thenReturn(this.path);
			InboundRequestHeaders requestHeadersMock = Mockito.mock(InboundRequestHeaders.class);
			if(this.headers.containsKey(Headers.NAME_CONTENT_TYPE)) {
				Mockito.when(requestHeadersMock.getContentTypeHeader()).thenReturn(CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, this.headers.get(Headers.NAME_CONTENT_TYPE)));
			}
			if(this.headers.containsKey(Headers.NAME_ACCEPT)) {
				Mockito.when(requestHeadersMock.getAllHeader(Headers.NAME_ACCEPT)).thenReturn(List.of(ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, this.headers.get(Headers.NAME_ACCEPT))));
			}
			if(this.headers.containsKey(Headers.NAME_ACCEPT_LANGUAGE)) {
				Mockito.when(requestHeadersMock.getAllHeader(Headers.NAME_ACCEPT_LANGUAGE)).thenReturn(List.of(ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, this.headers.get(Headers.NAME_ACCEPT_LANGUAGE))));
			}
			if(this.headers.containsKey(Headers.NAME_SEC_WEBSOCKET_PROTOCOL)) {
				Mockito.when(requestHeadersMock.getAll(Headers.NAME_SEC_WEBSOCKET_PROTOCOL)).thenReturn(List.of(this.headers.get(Headers.NAME_SEC_WEBSOCKET_PROTOCOL)));
			}

			this.headers.forEach((k,v) -> {
				Mockito.when(requestHeadersMock.get(k)).thenReturn(Optional.of(v));
				Mockito.when(requestHeadersMock.getAll(k)).thenReturn(List.of(v));
			});
			Mockito.when(requestHeadersMock.getAll()).thenReturn(new ArrayList<>(this.headers.entrySet()));

			Mockito.when(requestMock.headers()).thenReturn(requestHeadersMock);
			Mockito.when(exchangeMock.request()).thenReturn(requestMock);

			Response responseMock = Mockito.mock(Response.class);
			Mockito.when(exchangeMock.response()).thenReturn(responseMock);

			WebSocket<ExchangeContext, ? extends WebSocketExchange<ExchangeContext>> webSocket = Mockito.mock(WebSocket.class);
			if(this.supportedSubProtocols != null && !this.supportedSubProtocols.isEmpty()) {
				for(String supportedSubProtocol : this.supportedSubProtocols) {
					Mockito.when(exchangeMock.webSocket(supportedSubProtocol)).thenAnswer(ign -> Optional.of(webSocket));
				}
			}
			else {
				Mockito.when(exchangeMock.webSocket()).thenAnswer(ign -> Optional.of(webSocket));
			}
			return exchangeMock;
		}
	}

	public static class MockErrorExchangeBuilder<A extends ExchangeContext> {

		private final Throwable error;
		private final String path;
		private final A context;
		private final Map<String, String> headers;

		private MockErrorExchangeBuilder(Throwable error, String path, A context) {
			this.error = error;
			this.path = path;
			this.context = context;
			this.headers = new HashMap<>();
		}

		public MockErrorExchangeBuilder<A> contentType(String contentType) {
			this.headers.put(Headers.NAME_CONTENT_TYPE, contentType);
			return this;
		}

		public MockErrorExchangeBuilder<A> accept(String accept) {
			this.headers.put(Headers.NAME_ACCEPT, accept);
			return this;
		}

		public MockErrorExchangeBuilder<A> acceptLanguage(String acceptLanguage) {
			this.headers.put(Headers.NAME_ACCEPT_LANGUAGE, acceptLanguage);
			return this;
		}

		public MockErrorExchangeBuilder<A> header(String name, String value) {
			this.headers.put(name, value);
			return this;
		}

		@SuppressWarnings("unchecked")
		public ErrorExchange<A> build() {
			ErrorExchange<A> exchangeMock = (ErrorExchange<A>)Mockito.mock(ErrorExchange.class);

			Mockito.when(exchangeMock.context()).thenReturn(this.context);
			Mockito.when(exchangeMock.getError()).thenReturn(this.error);

			Request requestMock = Mockito.mock(Request.class);

			Mockito.when(requestMock.getPathAbsolute()).thenReturn(this.path);
			InboundRequestHeaders requestHeadersMock = Mockito.mock(InboundRequestHeaders.class);
			if(this.headers.containsKey(Headers.NAME_CONTENT_TYPE)) {
				Mockito.when(requestHeadersMock.getContentTypeHeader()).thenReturn(CONTENT_TYPE_CODEC.decode(Headers.NAME_CONTENT_TYPE, this.headers.get(Headers.NAME_CONTENT_TYPE)));
			}
			if(this.headers.containsKey(Headers.NAME_ACCEPT)) {
				Mockito.when(requestHeadersMock.getAllHeader(Headers.NAME_ACCEPT)).thenReturn(List.of(ACCEPT_CODEC.decode(Headers.NAME_ACCEPT, this.headers.get(Headers.NAME_ACCEPT))));
			}
			if(this.headers.containsKey(Headers.NAME_ACCEPT_LANGUAGE)) {
				Mockito.when(requestHeadersMock.getAllHeader(Headers.NAME_ACCEPT_LANGUAGE)).thenReturn(List.of(ACCEPT_LANGUAGE_CODEC.decode(Headers.NAME_ACCEPT_LANGUAGE, this.headers.get(Headers.NAME_ACCEPT_LANGUAGE))));
			}

			this.headers.forEach((k,v) -> {
				Mockito.when(requestHeadersMock.get(k)).thenReturn(Optional.of(v));
				Mockito.when(requestHeadersMock.getAll(k)).thenReturn(List.of(v));
			});
			Mockito.when(requestHeadersMock.getAll()).thenReturn(new ArrayList<>(this.headers.entrySet()));

			Mockito.when(requestMock.headers()).thenReturn(requestHeadersMock);
			Mockito.when(exchangeMock.request()).thenReturn(requestMock);

			Response responseMock = Mockito.mock(Response.class);
			Mockito.when(exchangeMock.response()).thenReturn(responseMock);

			return exchangeMock;
		}
	}
}
