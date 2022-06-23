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
package io.inverno.mod.http.server.internal;

import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.ServiceUnavailableException;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link ErrorExchangeHandler} implementation.
 * </p>
 * 
 * <p>
 * This implementation is used by default to handle error exchange.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
public class GenericErrorExchangeHandler implements ExchangeHandler<ExchangeContext, ErrorExchange<ExchangeContext>> {
	
	public static final GenericErrorExchangeHandler INSTANCE = new GenericErrorExchangeHandler();
	
	@Override
	public void handle(ErrorExchange<ExchangeContext> errorExchange) throws HttpException {
		if(errorExchange.response().isHeadersWritten()) {
			throw new IllegalStateException("Headers already written", errorExchange.getError());
		}
		if(errorExchange.getError() instanceof HttpException) {
			HttpException webError = (HttpException)errorExchange.getError();
			if(webError instanceof MethodNotAllowedException) {
				errorExchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)webError).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
			}
			else if(errorExchange.getError() instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)webError).getRetryAfter().ifPresent(retryAfter -> {
					errorExchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(Headers.FORMATTER_RFC_5322_DATE_TIME)));
				});
			}
			errorExchange.response().headers(h -> h.status(webError.getStatusCode())).body().empty();
		}
		else {
			errorExchange.response().headers(h -> h.status(Status.INTERNAL_SERVER_ERROR)).body().empty();
		}
	}
}
