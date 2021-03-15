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
package io.winterframework.mod.http.server.internal;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.http.base.MethodNotAllowedException;
import io.winterframework.mod.http.base.ServiceUnavailableException;
import io.winterframework.mod.http.base.Status;
import io.winterframework.mod.http.base.WebException;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.ExchangeHandler;

/**
 * <p>
 * Generic {@link ErrorHandler} implementation.
 * </p>
 * 
 * <p>
 * This implementation is used by default to handle error exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@winterframework.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericErrorHandler implements ExchangeHandler<ErrorExchange<Throwable>> {

	@Override
	public void handle(ErrorExchange<Throwable> exchange) throws WebException {
		if(exchange.response().isHeadersWritten()) {
			throw new IllegalStateException("Headers already written", exchange.getError());
		}
		if(exchange.getError() instanceof WebException) {
			WebException webError = (WebException)exchange.getError();
			if(webError instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)webError).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
			}
			else if(exchange.getError() instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)webError).getRetryAfter().ifPresent(retryAfter -> {
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
				});
			}
			exchange.response().headers(h -> h.status(webError.getStatusCode())).body().empty();
		}
		else {
			exchange.response().headers(h -> h.status(Status.INTERNAL_SERVER_ERROR)).body().empty();
		}
	}
}
