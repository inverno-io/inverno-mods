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
package io.inverno.mod.security.http.csrf;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.InboundCookies;
import io.inverno.mod.http.base.InboundRequestHeaders;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.OutboundResponseHeaders;
import io.inverno.mod.http.base.OutboundSetCookies;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.QueryParameters;
import io.inverno.mod.http.base.header.CookieParameter;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.SetCookie;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.Request;
import io.inverno.mod.http.server.RequestBody;
import io.inverno.mod.http.server.Response;
import io.inverno.mod.http.server.ResponseBody;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class CSRFDoubleSubmitCookieInterceptorTest {
	
	@Test
	public void test() {
		final CSRFDoubleSubmitCookieInterceptor<ExchangeContext, Exchange<ExchangeContext>> interceptor = CSRFDoubleSubmitCookieInterceptor.builder().build();
		
		SetCookie.Configurator mockCookieConfigurator = Mockito.mock(SetCookie.Configurator.class);
		Exchange<ExchangeContext> mockExchange = mockExchange(mockCookieConfigurator);
		
		// GET
		Mockito.when(mockExchange.request().getMethod()).thenReturn(Method.GET);
		Exchange<ExchangeContext> interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(1)).cookies(Mockito.any());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).name(CSRFDoubleSubmitCookieInterceptor.DEFAULT_COOKIE_NAME);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).value(Mockito.anyString());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).httpOnly(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).secure(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).sameSite(Headers.SetCookie.SameSitePolicy.STRICT);
		
		Mockito.clearInvocations(mockExchange.response(), mockExchange.response().headers(), mockCookieConfigurator);
		
		// POST
		Mockito.when(mockExchange.request().getMethod()).thenReturn(Method.POST);
		Assertions.assertEquals("Missing CSRF token cookie", Assertions.assertThrows(ForbiddenException.class, () -> interceptor.intercept(mockExchange).block()).getMessage());
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(0)).cookies(Mockito.any());
		
		Mockito.clearInvocations(mockExchange.response(), mockExchange.response().headers(), mockCookieConfigurator);
		
		// POST + CSRF cookie
		CookieParameter mockCSRFCookieParameter = Mockito.mock(CookieParameter.class);
		Mockito.when(mockCSRFCookieParameter.asString()).thenReturn("token");
		Mockito.when(mockExchange.request().headers().cookies().get(CSRFDoubleSubmitCookieInterceptor.DEFAULT_COOKIE_NAME)).thenReturn(Optional.of(mockCSRFCookieParameter));
		Assertions.assertEquals("Missing CSRF token header/parameter", Assertions.assertThrows(ForbiddenException.class, () -> interceptor.intercept(mockExchange).block()).getMessage());
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(0)).cookies(Mockito.any());
		
		// POST + CSRF cookie + non-matching CSRF header
		Mockito.when(mockExchange.request().headers().get(CSRFDoubleSubmitCookieInterceptor.DEFAULT_HEADER_NAME)).thenReturn(Optional.of("invalidToken"));
		Assertions.assertEquals("CSRF token header does not match CSRF token cookie", Assertions.assertThrows(ForbiddenException.class, () -> interceptor.intercept(mockExchange).block()).getMessage());
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(0)).cookies(Mockito.any());
		
		// POST + CSRF cookie + matching CSRF header
		Mockito.when(mockExchange.request().headers().get(CSRFDoubleSubmitCookieInterceptor.DEFAULT_HEADER_NAME)).thenReturn(Optional.of("token"));
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(1)).cookies(Mockito.any());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).name(CSRFDoubleSubmitCookieInterceptor.DEFAULT_COOKIE_NAME);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).value(Mockito.anyString());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).httpOnly(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).secure(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).sameSite(Headers.SetCookie.SameSitePolicy.STRICT);
		
		Mockito.clearInvocations(mockExchange.response(), mockExchange.response().headers(), mockCookieConfigurator);
		
		// POST + CSRF cookie + non-matching CSRF parameter
		Mockito.when(mockExchange.request().headers().get(CSRFDoubleSubmitCookieInterceptor.DEFAULT_HEADER_NAME)).thenReturn(Optional.empty());
		Parameter mockCSRFParameter = Mockito.mock(Parameter.class);
		Mockito.when(mockCSRFParameter.asString()).thenReturn("invalidToken");
		Mockito.when(mockExchange.request().queryParameters().get(CSRFDoubleSubmitCookieInterceptor.DEFAULT_PARAMETER_NAME)).thenReturn(Optional.of(mockCSRFParameter));
		Assertions.assertEquals("CSRF token header does not match CSRF token cookie", Assertions.assertThrows(ForbiddenException.class, () -> interceptor.intercept(mockExchange).block()).getMessage());
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(0)).cookies(Mockito.any());
		
		// POST + CSRF cookie + matching CSRF parameter
		Mockito.when(mockCSRFParameter.asString()).thenReturn("token");
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(1)).cookies(Mockito.any());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).name(CSRFDoubleSubmitCookieInterceptor.DEFAULT_COOKIE_NAME);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).value(Mockito.anyString());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).httpOnly(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).secure(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).sameSite(Headers.SetCookie.SameSitePolicy.STRICT);
		
		Mockito.clearInvocations(mockExchange.response(), mockExchange.response().headers(), mockCookieConfigurator);

		// POST + CSRF cookie + url encoded body
		Mockito.when(mockExchange.request().headers().getContentType()).thenReturn(MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED);
		Mockito.when(mockExchange.request().queryParameters().get(CSRFDoubleSubmitCookieInterceptor.DEFAULT_PARAMETER_NAME)).thenReturn(Optional.empty());
		RequestBody.UrlEncoded mockUrlEncoded = Mockito.mock(RequestBody.UrlEncoded.class);
		
		Mockito.when(mockExchange.request().body().get().urlEncoded()).thenReturn(mockUrlEncoded);
		Mockito.when(mockUrlEncoded.collectMap()).thenReturn(Mono.just(Map.of()));
		Assertions.assertEquals("Missing CSRF token header/parameter", Assertions.assertThrows(ForbiddenException.class, () -> interceptor.intercept(mockExchange).block()).getMessage());
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(0)).cookies(Mockito.any());
		
		// POST + CSRF cookie + non-matching form parameter
		Mockito.when(mockExchange.request().queryParameters().get(CSRFDoubleSubmitCookieInterceptor.DEFAULT_PARAMETER_NAME)).thenReturn(Optional.empty());
		Mockito.when(mockCSRFParameter.asString()).thenReturn("invalidToken");
		Mockito.when(mockUrlEncoded.collectMap()).thenReturn(Mono.just(Map.of(CSRFDoubleSubmitCookieInterceptor.DEFAULT_PARAMETER_NAME, mockCSRFParameter)));
		Assertions.assertEquals("CSRF token header does not match CSRF token cookie", Assertions.assertThrows(ForbiddenException.class, () -> interceptor.intercept(mockExchange).block()).getMessage());
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(0)).cookies(Mockito.any());
		
		// POST + CSRF cookie + matching form parameter
		Mockito.when(mockCSRFParameter.asString()).thenReturn("token");
		interceptedExchange = interceptor.intercept(mockExchange).block();
		Assertions.assertEquals(mockExchange, interceptedExchange);
		Mockito.verify((OutboundResponseHeaders)mockExchange.response().headers(), Mockito.times(1)).cookies(Mockito.any());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).name(CSRFDoubleSubmitCookieInterceptor.DEFAULT_COOKIE_NAME);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).value(Mockito.anyString());
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).httpOnly(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).secure(true);
		Mockito.verify(mockCookieConfigurator, Mockito.times(1)).sameSite(Headers.SetCookie.SameSitePolicy.STRICT);
		
		Mockito.clearInvocations(mockExchange.response(), mockExchange.response().headers(), mockCookieConfigurator);
	}
	
	@SuppressWarnings("unchecked")
	private static Exchange<ExchangeContext> mockExchange(SetCookie.Configurator mockCookieConfigurator) {
		
		Mockito.when(mockCookieConfigurator.name(Mockito.any())).thenReturn(mockCookieConfigurator);
		Mockito.when(mockCookieConfigurator.value(Mockito.any())).thenReturn(mockCookieConfigurator);
		Mockito.when(mockCookieConfigurator.sameSite(Mockito.any())).thenReturn(mockCookieConfigurator);
		
		Exchange<ExchangeContext> mockExchange = (Exchange<ExchangeContext>)Mockito.mock(Exchange.class);
		
		Request mockRequest = Mockito.mock(Request.class);
		InboundRequestHeaders mockRequestHeaders = Mockito.mock(InboundRequestHeaders.class);
		Mockito.when(mockRequest.headers()).thenReturn(mockRequestHeaders);
		InboundCookies mockRequestCookies = Mockito.mock(InboundCookies.class);
		Mockito.when(mockRequestHeaders.cookies()).thenReturn(mockRequestCookies);
		QueryParameters mockQueryParameters = Mockito.mock(QueryParameters.class);
		Mockito.when(mockRequest.queryParameters()).thenReturn(mockQueryParameters);
		RequestBody mockRequestBody = Mockito.mock(RequestBody.class);
		Mockito.when(mockRequest.body()).thenAnswer(invocation -> Optional.of(mockRequestBody));
		Mockito.when(mockExchange.request()).thenReturn(mockRequest);
		
		Response mockResponse = Mockito.mock(Response.class);
		OutboundResponseHeaders mockResponseHeaders = Mockito.mock(OutboundResponseHeaders.class);
		Mockito.when(mockResponseHeaders.status(Mockito.any())).thenReturn(mockResponseHeaders);
		Mockito.when(mockResponse.headers()).thenReturn(mockResponseHeaders);
		Mockito.when(mockResponse.headers(Mockito.any(Consumer.class))).then(invocation -> {
			Consumer<OutboundResponseHeaders> headersConfigurer = invocation.getArgument(0);
			headersConfigurer.accept(mockResponseHeaders);
			return mockResponse;
		});
		
		OutboundSetCookies mockResponseCookies = Mockito.mock(OutboundSetCookies.class);
		Mockito.when(mockResponseHeaders.cookies(Mockito.any(Consumer.class))).then(invocation -> {
			Consumer<OutboundSetCookies> cookiesConfigurer = invocation.getArgument(0);
			cookiesConfigurer.accept(mockResponseCookies);
			return mockResponseHeaders;
		});
		Mockito.when(mockResponseCookies.addCookie(Mockito.any(Consumer.class))).then(invocation -> {
			Consumer<SetCookie.Configurator> cookieConfigurer = invocation.getArgument(0);
			cookieConfigurer.accept(mockCookieConfigurator);
			return mockResponseCookies;
		});
		
		ResponseBody mockResponseBody = Mockito.mock(ResponseBody.class);
		Mockito.when(mockResponse.body()).thenReturn(mockResponseBody);
		Mockito.when(mockExchange.response()).thenReturn(mockResponse);
		
		return mockExchange;
	}
}
