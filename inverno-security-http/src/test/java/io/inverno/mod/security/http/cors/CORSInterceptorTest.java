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
package io.inverno.mod.security.http.cors;

import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.RequestHeaders;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.ResponseBody;
import io.inverno.mod.http.server.ResponseHeaders;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class CORSInterceptorTest {

	@Test
	public void test() {
		Assertions.assertEquals("Invalid origin: notAnOrigin", Assertions.assertThrows(IllegalArgumentException.class, () -> CORSInterceptor.builder("notAnOrigin").build()).getMessage());
		Assertions.assertEquals("Invalid origin: notAnOrigin", Assertions.assertThrows(IllegalArgumentException.class, () -> CORSInterceptor.builder().allowOrigin("notAnOrigin").build()).getMessage());
		
		Exchange<ExchangeContext> mockExchange = mockExchange();
		
		// No CORS request with wildcard origin
		CORSInterceptor<ExchangeContext, Exchange<ExchangeContext>> interceptor = CORSInterceptor.builder().build();
		Exchange<ExchangeContext> interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(0)).set(Headers.NAME_VARY, Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// No CORS request with static origin
		interceptor = CORSInterceptor.builder("http://localhost").build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(0)).set(Headers.NAME_VARY, Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// No CORS request with dynamic origin
		interceptor = CORSInterceptor.builder().allowOriginPattern("http://(?:localhost|127\\.0\\.0\\.1)").build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_VARY, Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS invalid origin in request header
		Mockito.when(mockExchange.request().headers().get(Headers.NAME_ORIGIN)).thenReturn(Optional.of("notAnOrigin"));
		Assertions.assertEquals("Invalid origin header: notAnOrigin", Assertions.assertThrows(BadRequestException.class, () -> CORSInterceptor.builder("http://localhost").build().intercept(mockExchange).block()).getMessage());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// Rejected CORS request: origins don't match
		Mockito.when(mockExchange.request().headers().get(Headers.NAME_ORIGIN)).thenReturn(Optional.of("http://127.0.0.1"));
		Assertions.assertEquals("Rejected CORS: http://127.0.0.1 is not auhorized to access resources", Assertions.assertThrows(ForbiddenException.class, () -> CORSInterceptor.builder("http://localhost").build().intercept(mockExchange).block()).getMessage());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS request: GET method
		Mockito.when(mockExchange.request().getMethod()).thenReturn(Method.GET);
		interceptor = CORSInterceptor.builder().allowOriginPattern("http://(?:localhost|127\\.0\\.0\\.1)").build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_VARY, Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(2)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS request: GET method + exposed headers
		interceptor = CORSInterceptor.builder().allowOriginPattern("http://localhost").allowOriginPattern("http://127.0.0.1").exposeHeader(Headers.NAME_ACCEPT).exposeHeader(Headers.NAME_CONTENT_TYPE).build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_VARY, Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_EXPOSE_HEADERS, "content-type,accept");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(3)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS request: GET method + allowCredentials
		interceptor = CORSInterceptor.builder().allowOrigin("http://127.0.0.1").allowOriginPattern("http://(?:localhost|192\\.168\\.1\\.1)").allowCredentials(true).build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_VARY, Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(3)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS request: wildcard + GET method
		interceptor = CORSInterceptor.builder().build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_VARY, Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(2)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS-preflight request: OPTIONS method
		Mockito.when(mockExchange.request().headers().get(Headers.NAME_ORIGIN)).thenReturn(Optional.of("http://127.0.0.1:8080"));
		Mockito.when(mockExchange.request().headers().contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD)).thenReturn(true);
		Mockito.when(mockExchange.request().getMethod()).thenReturn(Method.OPTIONS);
		interceptor = CORSInterceptor.builder("http://127.0.0.1:8080").build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertNull(interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS-preflight request: OPTIONS method + allowed methods + maxAge
		interceptor = CORSInterceptor.builder("http://127.0.0.1:8080").allowMethod(Method.POST).maxAge(600).build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertNull(interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_METHODS, "POST");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_MAX_AGE, "600");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(3)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS-preflight request: OPTIONS method + allowed methods + maxAge + access-control-request-headers
		Mockito.when(mockExchange.request().headers().get(Headers.NAME_ACCESS_CONTROL_REQUEST_HEADERS)).thenReturn(Optional.of("some-request-header"));
		interceptor = CORSInterceptor.builder("http://127.0.0.1:8080").allowMethod(Method.POST).maxAge(600).build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertNull(interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_METHODS, "POST");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_HEADERS, "some-request-header");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_VARY, Headers.NAME_ACCESS_CONTROL_REQUEST_HEADERS);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_MAX_AGE, "600");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(5)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS-preflight request: OPTIONS method + allowed methods + allowed headers + maxAge
		interceptor = CORSInterceptor.builder("http://127.0.0.1:8080").allowMethod(Method.POST).allowHeader("some-header").maxAge(600).build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertNull(interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_METHODS, "POST");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_HEADERS, "some-header");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_MAX_AGE, "600");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(4)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS-preflight request: OPTIONS method + allow private network
		Mockito.when(mockExchange.request().headers().get(Headers.NAME_ACCESS_CONTROL_REQUEST_HEADERS)).thenReturn(Optional.empty());
		interceptor = CORSInterceptor.builder("http://127.0.0.1:8080").allowPrivateNetwork(true).build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertNull(interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS-preflight request: OPTIONS method + allow private network + access-control-request-private-network
		Mockito.when(mockExchange.request().headers().get(Headers.NAME_ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK)).thenReturn(Optional.of("true"));
		interceptor = CORSInterceptor.builder("http://127.0.0.1:8080").allowPrivateNetwork(true).build();
		interceptedExchange = interceptor.intercept(mockExchange).	block();
		Assertions.assertNull(interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK, "true");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(2)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
		
		// CORS-preflight request: OPTIONS method + allow private network + invalid access-control-request-private-network
		Mockito.when(mockExchange.request().headers().get(Headers.NAME_ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK)).thenReturn(Optional.of("invalid"));
		interceptor = CORSInterceptor.builder("http://127.0.0.1:8080").allowPrivateNetwork(true).build();
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertNull(interceptedExchange);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).get(Headers.NAME_ORIGIN);
		Mockito.verify(mockExchange.request().headers(), Mockito.times(1)).contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8080");
		Mockito.verify(mockExchange.response().headers(), Mockito.times(1)).set(Mockito.anyString(), Mockito.anyString());
		
		Mockito.clearInvocations(mockExchange.request().headers(), mockExchange.response().headers());
	}
	
	
	@SuppressWarnings("unchecked")
	private static Exchange<ExchangeContext> mockExchange() {
		Exchange<ExchangeContext> mockExchange = (Exchange<ExchangeContext>)Mockito.mock(Exchange.class);
		
		Request mockRequest = Mockito.mock(Request.class);
		RequestHeaders mockRequestHeaders = Mockito.mock(RequestHeaders.class);
		Mockito.when(mockRequest.headers()).thenReturn(mockRequestHeaders);
		Mockito.when(mockExchange.request()).thenReturn(mockRequest);
		
		Response mockResponse = Mockito.mock(Response.class);
		ResponseHeaders mockResponseHeaders = Mockito.mock(ResponseHeaders.class);
		Mockito.when(mockResponseHeaders.status(Mockito.any())).thenReturn(mockResponseHeaders);
		Mockito.when(mockResponse.headers()).thenReturn(mockResponseHeaders);
		Mockito.when(mockResponse.headers(Mockito.any(Consumer.class))).then(invocation -> {
			Consumer<ResponseHeaders> headersConfigurer = invocation.getArgument(0);
			headersConfigurer.accept(mockResponseHeaders);
			return mockResponse;
		});
		
		ResponseBody mockResponseBody = Mockito.mock(ResponseBody.class);
		Mockito.when(mockResponse.body()).thenReturn(mockResponseBody);
		Mockito.when(mockExchange.response()).thenReturn(mockResponse);
		
		return mockExchange;
	}
}
