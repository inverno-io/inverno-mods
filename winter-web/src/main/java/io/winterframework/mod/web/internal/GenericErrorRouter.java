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
package io.winterframework.mod.web.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.netty.buffer.Unpooled;
import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Init;
import io.winterframework.core.annotation.Provide;
import io.winterframework.mod.base.resource.MediaTypes;
import io.winterframework.mod.http.base.InternalServerErrorException;
import io.winterframework.mod.http.base.Method;
import io.winterframework.mod.http.base.MethodNotAllowedException;
import io.winterframework.mod.http.base.NotAcceptableException;
import io.winterframework.mod.http.base.ServiceUnavailableException;
import io.winterframework.mod.http.base.WebException;
import io.winterframework.mod.http.base.header.Headers;
import io.winterframework.mod.http.base.internal.header.AcceptCodec;
import io.winterframework.mod.http.base.internal.header.AcceptLanguageCodec;
import io.winterframework.mod.http.base.internal.header.ContentTypeCodec;
import io.winterframework.mod.http.server.ErrorExchange;
import io.winterframework.mod.http.server.ErrorExchangeHandler;
import io.winterframework.mod.web.ErrorRoute;
import io.winterframework.mod.web.ErrorRouteManager;
import io.winterframework.mod.web.ErrorRouter;
import io.winterframework.mod.web.ErrorRouterConfigurer;

/**
 * @author jkuhn
 *
 */
@Bean( name = "errorRouter" )
public class GenericErrorRouter implements @Provide ErrorRouter {

	private final RoutingLink<ErrorExchange<Throwable>, ?, ErrorRoute> firstLink;
	
	private ErrorRouterConfigurer configurer;
	
	public GenericErrorRouter() {
		AcceptCodec acceptCodec = new AcceptCodec(false);
		ContentTypeCodec contentTypeCodec = new ContentTypeCodec();
		AcceptLanguageCodec acceptLanguageCodec = new AcceptLanguageCodec(false);
		
		this.firstLink = new ThrowableRoutingLink();
		this.firstLink.connect(new ProducesRoutingLink<>(acceptCodec, contentTypeCodec))
			.connect(new LanguageRoutingLink<>(acceptLanguageCodec))
			.connect(new HandlerRoutingLink<>());
	}
	
	@Init
	public void init() {
		this.route().produces(MediaTypes.APPLICATION_JSON).error(WebException.class).handler(webExceptionHandler_json())
			.route().produces(MediaTypes.APPLICATION_JSON).handler(this.throwableHandler_json())
			.route().produces(MediaTypes.TEXT_HTML).error(WebException.class).handler(this.webExceptionHandler_html())
			.route().produces(MediaTypes.TEXT_HTML).handler(this.throwableHandler_html())
			.route().error(WebException.class).handler(this.webExceptionHandler())
			.route().handler(this.throwableHandler());
		
		if(this.configurer != null) {
			this.configurer.accept(this);
		}
	}

	public void setConfigurer(ErrorRouterConfigurer configurer) {
		this.configurer = configurer;
	}
	
	@Override
	public ErrorRouteManager route() {
		return new GenericErrorRouteManager(this);
	}
	
	void setRoute(ErrorRoute route) {
		this.firstLink.setRoute(route);
	}
	
	void enableRoute(ErrorRoute route) {
		this.firstLink.enableRoute(route);
	}
	
	void disableRoute(ErrorRoute route) {
		this.firstLink.disableRoute(route);
	}

	void removeRoute(ErrorRoute route) {
		this.firstLink.removeRoute(route);
	}
	
	@Override
	public Set<ErrorRoute> getRoutes() {
		GenericErrorRouteExtractor routeExtractor = new GenericErrorRouteExtractor(this);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public void handle(ErrorExchange<Throwable> exchange) throws WebException {
		ErrorRouter.super.handle(exchange);
		exchange.getError().printStackTrace();
		this.firstLink.handle(exchange);
	}
	
	private ErrorExchangeHandler<WebException> webExceptionHandler() {
		return exchange -> {
			if(exchange.getError() instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
			}
			else if(exchange.getError() instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)exchange.getError()).getRetryAfter().ifPresent(retryAfter -> {
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
				});
			}
			exchange.response().headers(h -> h.status(exchange.getError().getStatusCode())).body().empty();
		};
	}
	
	private ErrorExchangeHandler<Throwable> throwableHandler() {
		return exchange -> {
			this.webExceptionHandler().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	private ErrorExchangeHandler<WebException> webExceptionHandler_json() {
		// {"timestamp":"2020-11-20T16:10:33.829+00:00","path":"/tertjer","status":404,"error":"Not Found","message":null,"requestId":"115fe3c6-3"}
		return exchange -> {
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream error = new PrintStream(errorOut);
			
			error.append("{");
			error.append("\"status\":\"").append(Integer.toString(exchange.getError().getStatusCode())).append("\",");
			error.append("\"path\":\"").append(exchange.request().getPath()).append("\",");
			error.append("\"error\":\"").append(exchange.getError().getStatusReasonPhrase()).append("\"");
			
			if(exchange.getError() instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
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
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
					error.append(",\"retryAfter\":\"").append(retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("\"");
				});
			}
			
			if(exchange.getError().getMessage() != null) {
				error.append(",\"message\":\"").append(exchange.getError().getMessage()).append("\"");
			}
			error.append("}");
			
			exchange.response().headers(h -> h.status(exchange.getError().getStatusCode()).contentType(MediaTypes.APPLICATION_JSON)).body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(errorOut.toByteArray())));
		};
	}
	
	private ErrorExchangeHandler<Throwable> throwableHandler_json() {
		return exchange -> {
			this.webExceptionHandler_json().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	private ErrorExchangeHandler<WebException> webExceptionHandler_html() {
		return exchange -> {
			String status = Integer.toString(exchange.getError().getStatusCode());
			
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream error = new PrintStream(errorOut);
			error.append("<html><head><title>").append(status).append(" ").append(exchange.getError().getStatusReasonPhrase()).append("</title></head><body style=\"font-family: arial,sans-serif;padding:30px;max-width: 1280px;margin: auto;\">");
			error.append("<h1 style=\"font-size: 3em;\"><span style=\"color:red;\">").append(status).append("</span> ").append(exchange.getError().getStatusReasonPhrase()).append("</h1>");
			
			if(exchange.getError() instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)exchange.getError()).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
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
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
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
			error.append("<p><small>This is a whitelabel error page, providing a custom error handler is recommended.</small></p>");
			error.append("</footer>");
			error.append("</body>");
			
			exchange.response().headers(headers -> headers.status(exchange.getError().getStatusCode()).contentType(MediaTypes.TEXT_HTML)).body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(errorOut.toByteArray())));
		};
	}
	
	private ErrorExchangeHandler<Throwable> throwableHandler_html() {
		return exchange -> {
			this.webExceptionHandler_html().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	@Bean( name = "ErrorRouterConfigurer")
	public static interface ConfigurerSocket extends Supplier<ErrorRouterConfigurer> {}
}
