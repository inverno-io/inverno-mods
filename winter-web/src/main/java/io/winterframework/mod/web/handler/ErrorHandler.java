/*
 * Copyright 2020 Jeremy KUHN
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
package io.winterframework.mod.web.handler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Overridable;
import io.winterframework.core.annotation.Wrapper;
import io.winterframework.mod.commons.resource.MediaTypes;
import io.winterframework.mod.web.ErrorExchange;
import io.winterframework.mod.web.ErrorExchangeHandler;
import io.winterframework.mod.web.ExchangeHandler;
import io.winterframework.mod.web.Headers;
import io.winterframework.mod.web.InternalServerErrorException;
import io.winterframework.mod.web.Method;
import io.winterframework.mod.web.MethodNotAllowedException;
import io.winterframework.mod.web.NotAcceptableException;
import io.winterframework.mod.web.ResponseBody;
import io.winterframework.mod.web.ServiceUnavailableException;
import io.winterframework.mod.web.WebException;
import io.winterframework.mod.web.router.Router;

/**
 * @author jkuhn
 *
 */
@Bean
@Wrapper
@Overridable 
public class ErrorHandler implements Supplier<ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>>> {

	@Override
	public ExchangeHandler<Void, ResponseBody, ErrorExchange<ResponseBody, Throwable>> get() {
		return Router.error()
			.route().produces(MediaTypes.APPLICATION_JSON).error(WebException.class).handler(webExceptionHandler_json())
			.route().produces(MediaTypes.APPLICATION_JSON).handler(this.throwableHandler_json())
			.route().produces(MediaTypes.TEXT_HTML).error(WebException.class).handler(this.webExceptionHandler_html())
			.route().produces(MediaTypes.TEXT_HTML).handler(this.throwableHandler_html())
			.route().error(WebException.class).handler(this.webExceptionHandler())
			.route().handler(this.throwableHandler());
	}
	
	private ErrorExchangeHandler<ResponseBody, WebException> webExceptionHandler() {
		return exchange -> {
			if(exchange.getError() instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.ALLOW, ((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
			}
			else if(exchange.getError() instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)exchange.getError()).getRetryAfter().ifPresent(retryAfter -> {
					exchange.response().headers(headers -> headers.add(Headers.RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
				});
			}
			exchange.response().headers(h -> h.status(exchange.getError().getStatusCode())).body().empty();
		};
	}
	
	private ErrorExchangeHandler<ResponseBody, Throwable> throwableHandler() {
		return exchange -> {
			this.webExceptionHandler().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	private ErrorExchangeHandler<ResponseBody, WebException> webExceptionHandler_json() {
		// {"timestamp":"2020-11-20T16:10:33.829+00:00","path":"/tertjer","status":404,"error":"Not Found","message":null,"requestId":"115fe3c6-3"}
		return exchange -> {
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream error = new PrintStream(errorOut);
			
			error.append("{");
			error.append("\"status\":\"").append(Integer.toString(exchange.getError().getStatusCode())).append("\",");
			error.append("\"path\":\"").append(exchange.request().headers().getPath()).append("\",");
			error.append("\"error\":\"").append(exchange.getError().getStatusReasonPhrase()).append("\"");
			
			if(exchange.getError() instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.ALLOW, ((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
				error.append(",\"allowedMethods\":[");
				error.append(((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().map(method -> "\"" + method + "\"").collect(Collectors.joining(", ")));
				error.append("]");
			}
			else if(exchange.getError() instanceof NotAcceptableException) {
				((NotAcceptableException)exchange.getError()).getAcceptableMediaTypes().ifPresent(acceptableMediaTypes -> {
					error.append(",\"accept\":[");
					error.append(acceptableMediaTypes.stream().map(acceptableMediaType -> "\"" + acceptableMediaType + "\"").collect(Collectors.joining(", ")));
					error.append("]");
				});
			}
			else if(exchange.getError() instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)exchange.getError()).getRetryAfter().ifPresent(retryAfter -> {
					exchange.response().headers(headers -> headers.add(Headers.RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
					error.append(",\"retryAfter\":\"").append(retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("\"");
				});
			}
			
			if(exchange.getError().getMessage() != null) {
				error.append(",\"message\":\"").append(exchange.getError().getMessage()).append("\"");
			}
			error.append("}");
			
			exchange.response().headers(h -> h.status(exchange.getError().getStatusCode()).contentType(MediaTypes.APPLICATION_JSON)).body().raw().data(errorOut.toByteArray());
		};
	}
	
	private ErrorExchangeHandler<ResponseBody, Throwable> throwableHandler_json() {
		return exchange -> {
			this.webExceptionHandler_json().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	private ErrorExchangeHandler<ResponseBody, WebException> webExceptionHandler_html() {
		return exchange -> {
			String status = Integer.toString(exchange.getError().getStatusCode());
			
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream error = new PrintStream(errorOut);
			error.append("<html><head><title>").append(status).append(" ").append(exchange.getError().getStatusReasonPhrase()).append("</title></head><body style=\"font-family: arial,sans-serif;padding:30px;max-width: 1280px;margin: auto;\">");
			error.append("<h1 style=\"font-size: 3em;\"><span style=\"color:red;\">").append(status).append("</span> ").append(exchange.getError().getStatusReasonPhrase()).append("</h1>");
			
			if(exchange.getError() instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.ALLOW, ((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
				error.append("<p>");
				error.append("Allowed Methods:");
				error.append("<ul>");
				((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().forEach(allowedMethod -> error.append("<li>").append(allowedMethod.toString()).append("</li>"));
				error.append("</ul>");
				error.append("</p>");
			}
			else if(exchange.getError() instanceof NotAcceptableException) {
				((NotAcceptableException)exchange.getError()).getAcceptableMediaTypes().ifPresent(acceptableMediaTypes -> {
					error.append("<p>");
					error.append("Accept:");
					error.append("<ul>");
					acceptableMediaTypes.stream().forEach(acceptableMediaType -> error.append("<li>").append(acceptableMediaType).append("</li>"));
					error.append("</ul>");
					error.append("</p>");
				});
			}
			else if(exchange.getError() instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)exchange.getError()).getRetryAfter().ifPresent(retryAfter -> {
					error.append("<p>");
					error.append("Retry After: ").append(retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME));
					exchange.response().headers(headers -> headers.add(Headers.RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
					error.append("</p>");
				});
			}
			
			if(exchange.getError().getMessage() != null) {
				error.append("<p>").append(exchange.getError().getMessage()).append("</p>");
			}
			
			error.append("<pre style=\"color:#444444;background-color: #F7F7F7;border:1px solid #E7E7E7;border-radius: 3px;box-shadow: rgba(0, 0, 0, 0.1) 2px 2px 10px;padding:20px;overflow:auto;\">");
			exchange.getError().printStackTrace(new PrintStream(error));
			error.append("</pre>");
			error.append("<footer style=\"text-align:center;color: #444444;\">");
			error.append("<p><small>This is a whitelabel error page, providing a specific error handler is recommended.</small></p>");
			error.append("</footer>");
			error.append("</body>");
			
			exchange.response().headers(headers -> headers.status(exchange.getError().getStatusCode()).contentType(MediaTypes.TEXT_HTML)).body().raw().data(errorOut.toByteArray());
		};
	}
	
	private ErrorExchangeHandler<ResponseBody, Throwable> throwableHandler_html() {
		return exchange -> {
			this.webExceptionHandler_html().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	/*@Override
	public RequestHandler<Void, ResponseBody, Throwable> get() {
		return Router.error()
			.route().produces(MediaTypes.APPLICATION_JSON).error(WebException.class).handler(this.webExceptionHandler_json())
			.route().produces(MediaTypes.APPLICATION_JSON).handler(this.throwableHandler_json())
			.route().produces(MediaTypes.TEXT_HTML).error(WebException.class).handler(this.webExceptionHandler_html())
			.route().produces(MediaTypes.TEXT_HTML).handler(this.throwableHandler_html());
	}
	
	private RequestHandler<Void, ResponseBody, Throwable> throwableHandler_html() {
		return (request, response) -> {
			this.webExceptionHandler_html().handle(request.mapContext(t -> new InternalServerErrorException(t)), response);
		};
	}
	
	private RequestHandler<Void, ResponseBody, Throwable> throwableHandler_json() {
		return (request, response) -> {
			this.webExceptionHandler_json().handle(request.mapContext(t -> new InternalServerErrorException(t)), response);
		};
	}
	
	private RequestHandler<Void, ResponseBody, WebException> webExceptionHandler_html() {
		return (request, response) -> {
			String status = Integer.toString(request.context().getStatusCode());
			
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream error = new PrintStream(errorOut);
			error.append("<html><head><title>").append(status).append(" ").append(request.context().getStatusReasonPhrase()).append("</title></head><body style=\"font-family: arial,sans-serif;padding:30px;max-width: 1280px;margin: auto;\">");
			error.append("<h1 style=\"font-size: 3em;\"><span style=\"color:red;\">").append(status).append("</span> ").append(request.context().getStatusReasonPhrase()).append("</h1>");
			
			if(request.context() instanceof MethodNotAllowedException) {
				response.headers(headers -> headers.add(Headers.ALLOW, ((MethodNotAllowedException)request.context()).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
				error.append("<p>");
				error.append("Allowed Methods:");
				error.append("<ul>");
				((MethodNotAllowedException)request.context()).getAllowedMethods().stream().forEach(allowedMethod -> error.append("<li>").append(allowedMethod.toString()).append("</li>"));
				error.append("</ul>");
				error.append("</p>");
			}
			else if(request.context() instanceof NotAcceptableException) {
				((NotAcceptableException)request.context()).getAcceptableMediaTypes().ifPresent(acceptableMediaTypes -> {
					error.append("<p>");
					error.append("Acceptable Content Types:");
					error.append("<ul>");
					acceptableMediaTypes.stream().forEach(acceptableMediaType -> error.append("<li>").append(acceptableMediaType).append("</li>"));
					error.append("</ul>");
					error.append("</p>");
				});
			}
			else if(request.context() instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)request.context()).getRetryAfter().ifPresent(retryAfter -> {
					error.append("<p>");
					error.append("Retry After: ").append(retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME));
					response.headers(headers -> headers.add(Headers.RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
					error.append("</p>");
				});
			}
			
			if(request.context().getMessage() != null) {
				error.append("<p>").append(request.context().getMessage()).append("</p>");
			}
			
			error.append("<pre style=\"color:#444444;background-color: #F7F7F7;border:1px solid #E7E7E7;border-radius: 3px;box-shadow: rgba(0, 0, 0, 0.1) 2px 2px 10px;padding:20px;overflow:auto;\">");
			request.context().printStackTrace(new PrintStream(error));
			error.append("</pre>");
			error.append("<footer style=\"text-align:center;color: #444444;\">");
			error.append("<p><small>This is a whitelabel error page, providing a specific error handler is recommended.</small></p>");
			error.append("</footer>");
			error.append("</body>");
			
			response.headers(headers -> headers.status(request.context().getStatusCode()).contentType(MediaTypes.TEXT_HTML)).body().raw().data(errorOut.toByteArray());
		};
	}
	
	private RequestHandler<Void, ResponseBody, WebException> webExceptionHandler_json() {
		// {"timestamp":"2020-11-20T16:10:33.829+00:00","path":"/tertjer","status":404,"error":"Not Found","message":null,"requestId":"115fe3c6-3"}
		return (request, response) -> {
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream error = new PrintStream(errorOut);
			
			error.append("{");
			error.append("\"status\":\"").append(Integer.toString(request.context().getStatusCode())).append("\",");
			error.append("\"path\":\"").append(request.headers().getPath()).append("\",");
			error.append("\"error\":\"").append(request.context().getStatusReasonPhrase()).append("\",");
			error.append("\"message\":\"").append(request.context().getMessage()).append("\",");
			error.append("}");
			
			response.headers(h -> h.status(request.context().getStatusCode()).contentType(MediaTypes.APPLICATION_JSON)).body().raw().data(errorOut.toByteArray());
		};
	}*/
}
