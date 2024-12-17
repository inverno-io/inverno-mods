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
package io.inverno.mod.web.server;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.ServiceUnavailableException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeHandler;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.stream.Collectors;
import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * Configures white label error routes handling common HTTP errors and responding with {@code application/json}, {@code text/html} or {@code text/plain}.
 * </p>
 *
 * <p>
 * This provides a simple and convenient way to handle errors in a Web server on an environment where exposing stacktrace is not a security concern. It is however recommended to provide proper error
 * handlers that comply with security policies.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the exchange context type
 */
public class WhiteLabelErrorRoutesConfigurer<A extends ExchangeContext> implements ErrorWebRouter.Configurer<A> {

	@Override
	public void configure(ErrorWebRouter<A> routes) {
		routes
			.routeError().produce(MediaTypes.APPLICATION_JSON).handler(this.httpExceptionHandler_json())
			.routeError().produce(MediaTypes.TEXT_HTML).handler(this.httpExceptionHandler_html())
			.routeError().produce(MediaTypes.TEXT_PLAIN).handler(this.httpExceptionHandler_text())
			.routeError().handler(this.httpExceptionHandler());
	}

	/**
	 * <p>
	 * Returns the default {@link HttpException} error handler.
	 * </p>
	 *
	 * @return an error Web exchange handler
	 */
	private ExchangeHandler<A, ErrorWebExchange<A>> httpExceptionHandler() {
		return exchange -> {
			final HttpException error = HttpException.wrap(exchange.getError());
			if(error instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)error).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
			}
			else if(error instanceof ServiceUnavailableException) {
				if(((ServiceUnavailableException)error).getRetryAfter() != null) {
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, ((ServiceUnavailableException)error).getRetryAfter().format(Headers.FORMATTER_RFC_5322_DATE_TIME)));
				}
			}

			exchange.response()
				.headers(h -> h.status(error.getStatusCode()))
				.body().empty();
		};
	}

	/**
	 * <p>
	 * Returns the {@code text/plain} {@link HttpException} error handler.
	 * </p>
	 *
	 * @return an error Web exchange handler
	 */
	private ExchangeHandler<A, ErrorWebExchange<A>> httpExceptionHandler_text() {
		return exchange -> {
			final HttpException error = HttpException.wrap(exchange.getError());
			if(error instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)error).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
			}
			else if(error instanceof ServiceUnavailableException) {
				if(((ServiceUnavailableException)error).getRetryAfter() != null) {
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, ((ServiceUnavailableException)error).getRetryAfter().format(Headers.FORMATTER_RFC_5322_DATE_TIME)));
				}
			}

			exchange.response()
				.headers(h -> h.status(error.getStatusCode()).contentType(MediaTypes.TEXT_PLAIN))
				.body().string().value(error.getMessage());
		};
	}

	/**
	 * <p>
	 * Returns the {@code application/json} {@link HttpException} error handler.
	 * </p>
	 *
	 * @return an error Web exchange handler
	 */
	private ExchangeHandler<A, ErrorWebExchange<A>> httpExceptionHandler_json() {
		return exchange -> {
			final HttpException error = HttpException.wrap(exchange.getError());

			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream errorStream = new PrintStream(errorOut);

			errorStream.append("{");
			errorStream.append("\"status\":\"").append(Integer.toString(error.getStatusCode())).append("\",");
			errorStream.append("\"path\":\"").append(exchange.request().getPath()).append("\",");
			errorStream.append("\"error\":\"").append(error.getStatusReasonPhrase()).append("\"");

			if(error instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)error).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
				errorStream.append(",\"allowedMethods\":[");
				errorStream.append(((MethodNotAllowedException)error).getAllowedMethods().stream().map(method -> "\"" + method + "\"").collect(Collectors.joining(", ")));
				errorStream.append("]");
			}
			else if(error instanceof NotAcceptableException) {
				if(((NotAcceptableException)error).getAcceptableMediaTypes() != null) {
					errorStream.append(",\"accept\":[");
					errorStream.append(((NotAcceptableException)error).getAcceptableMediaTypes().stream().map(acceptableMediaType -> "\"" + acceptableMediaType + "\"").collect(Collectors.joining(", ")));
					errorStream.append("]");
				}
			}
			else if(error instanceof ServiceUnavailableException) {
				if(((ServiceUnavailableException)error).getRetryAfter() != null) {
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, ((ServiceUnavailableException)error).getRetryAfter().format(Headers.FORMATTER_RFC_5322_DATE_TIME)));
					errorStream.append(",\"retryAfter\":\"").append(((ServiceUnavailableException)error).getRetryAfter().format(Headers.FORMATTER_RFC_5322_DATE_TIME)).append("\"");
				}
			}

			if(error.getMessage() != null) {
				errorStream.append(",\"message\":\"").append(StringEscapeUtils.escapeJson(error.getMessage())).append("\"");
			}
			errorStream.append("}");

			exchange.response()
				.headers(h -> h.status(error.getStatusCode()).contentType(MediaTypes.APPLICATION_JSON))
				.body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(errorOut.toByteArray())));
		};
	}

	/**
	 * <p>
	 * Returns the whitelabel {@code text/html} {@link HttpException} error handler.
	 * </p>
	 *
	 * @return an error Web exchange handler
	 */
	private ExchangeHandler<A, ErrorWebExchange<A>> httpExceptionHandler_html() {
		return exchange -> {
			final HttpException error = HttpException.wrap(exchange.getError());
			String status = Integer.toString(error.getStatusCode());

			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream errorStream = new PrintStream(errorOut);

			errorStream.append("<!DOCTYPE html>");
			errorStream.append("<html>");
			errorStream.append("<head>");
			errorStream.append("<title>").append(status).append(" ").append(error.getStatusReasonPhrase()).append("</title>");
			errorStream.append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
			errorStream.append("<style>");
			errorStream.append("body {font-family: system-ui,-apple-system,\"Segoe UI\",Roboto,\"Helvetica Neue\",Arial,\"Noto Sans\",\"Liberation Sans\",sans-serif,\"Apple Color Emoji\",\"Segoe UI Emoji\",\"Segoe UI Symbol\",\"Noto Color Emoji\";margin: 0px;display: flex;justify-content: center;flex-direction: column;align-items: center;}");
			errorStream.append("footer {text-align: center;color: #6C757D;margin: 1rem;}");
			errorStream.append(".title {font-size: 3em;}");
			errorStream.append(".code {color: #DC3545;}");
			errorStream.append(".stacktrace {color: rgb(33, 37, 41);border: 1px solid #DEE2E6;background-color: #F8F9FA;box-shadow: 0 .5rem 1rem rgba(0,0,0,.15);padding: 1rem;overflow: auto;border-radius: 0.25rem;max-width: 80vw;}");
			errorStream.append("</style>");
			errorStream.append("</head>");
			errorStream.append("<body>");
			errorStream.append("<div>");
			errorStream.append("<h1 class=\"title\"><span class=\"code\">").append(status).append("</span> ").append(error.getStatusReasonPhrase()).append("</h1>");

			if(error instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)error).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
				errorStream.append("<p>");
				errorStream.append("Allowed Methods:");
				errorStream.append("<ul>");
				((MethodNotAllowedException)error).getAllowedMethods().forEach(allowedMethod -> errorStream.append("<li>").append(allowedMethod.toString()).append("</li>"));
				errorStream.append("</ul>");
				errorStream.append("</p>");
			}
			else if(error instanceof NotAcceptableException) {
				if(((NotAcceptableException)error).getAcceptableMediaTypes() != null) {
					errorStream.append("<p>");
					errorStream.append("Accept:");
					errorStream.append("<ul>");
					((NotAcceptableException)error).getAcceptableMediaTypes().forEach(acceptableMediaType -> errorStream.append("<li>").append(acceptableMediaType).append("</li>"));
					errorStream.append("</ul>");
					errorStream.append("</p>");
				}
			}
			else if(error instanceof ServiceUnavailableException) {
				if(((ServiceUnavailableException)error).getRetryAfter() != null) {
					errorStream.append("<p>");
					errorStream.append("Retry After: ").append(((ServiceUnavailableException)error).getRetryAfter().format(Headers.FORMATTER_RFC_5322_DATE_TIME));
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, ((ServiceUnavailableException)error).getRetryAfter().format(Headers.FORMATTER_RFC_5322_DATE_TIME)));
					errorStream.append("</p>");
				}
			}

			if(error.getMessage() != null) {
				errorStream.append("<p>").append(error.getMessage()).append("</p>");
			}

			errorStream.append("<pre class=\"stacktrace\">");
			error.printStackTrace(new PrintStream(errorStream));
			errorStream.append("</pre>");
			errorStream.append("</div>");
			errorStream.append("<footer>");
			errorStream.append("<p><small>This is a whitelabel error page, providing a custom error handler is recommended.</small></p>");
			errorStream.append("</footer>");
			errorStream.append("</body>");
			errorStream.append("</html>");

			exchange.response()
				.headers(headers -> headers.status(error.getStatusCode()).contentType(MediaTypes.TEXT_HTML))
				.body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(errorOut.toByteArray())));
		};
	}
}
