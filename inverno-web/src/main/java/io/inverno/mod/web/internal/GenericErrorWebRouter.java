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
package io.inverno.mod.web.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.InternalServerErrorException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.MethodNotAllowedException;
import io.inverno.mod.http.base.NotAcceptableException;
import io.inverno.mod.http.base.ServiceUnavailableException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.ErrorWebExchangeHandler;
import io.inverno.mod.web.ErrorWebRoute;
import io.inverno.mod.web.ErrorWebRouteManager;
import io.inverno.mod.web.ErrorWebRouter;
import io.inverno.mod.web.ErrorWebRouterConfigurer;
import io.netty.buffer.Unpooled;
import org.apache.commons.text.StringEscapeUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link ErrorWebRouter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean( name = "errorRouter" )
public class GenericErrorWebRouter implements @Provide ErrorWebRouter {
	
	private final DataConversionService dataConversionService;
	
	private final RoutingLink<ExchangeContext, ErrorWebExchange<Throwable>, ?, ErrorWebRoute> firstLink;
	
	private ErrorWebRouterConfigurer configurer;
	
	/**
	 * <p>
	 * Creates a generic error web router.
	 * </p>
	 * 
	 * @param dataConversionService the data conversion service
	 */
	public GenericErrorWebRouter(DataConversionService dataConversionService) {
		this.dataConversionService = dataConversionService;
		
		ContentTypeCodec contentTypeCodec = new ContentTypeCodec();
		AcceptLanguageCodec acceptLanguageCodec = new AcceptLanguageCodec(false);
		
		this.firstLink = new ThrowableRoutingLink();
		this.firstLink.connect(new ProducesRoutingLink<>(contentTypeCodec))
			.connect(new LanguageRoutingLink<>(acceptLanguageCodec))
			.connect(new HandlerRoutingLink<>());
	}
	
	@Init
	public void init() {
		this.route().produces(MediaTypes.APPLICATION_JSON).error(HttpException.class).handler(this.httpExceptionHandler_json())
			.route().produces(MediaTypes.APPLICATION_JSON).handler(this.throwableHandler_json())
			.route().produces(MediaTypes.TEXT_HTML).error(HttpException.class).handler(this.httpExceptionHandler_html())
			.route().produces(MediaTypes.TEXT_HTML).handler(this.throwableHandler_html())
			.route().error(HttpException.class).handler(this.httpExceptionHandler())
			.route().handler(this.throwableHandler());
		
		if(this.configurer != null) {
			this.configurer.accept(this);
		}
	}

	/**
	 * <p>
	 * Sets the error web router configurer used to initialize the router.
	 * </p>
	 * 
	 * @param configurer an error web router configurer
	 */
	public void setConfigurer(ErrorWebRouterConfigurer configurer) {
		this.configurer = configurer;
	}
	
	/**
	 * <p>
	 * Sets the specified error web route in the router.
	 * </p>
	 * 
	 * @param route an error web route
	 */
	void setRoute(ErrorWebRoute route) {
		this.firstLink.setRoute(route);
	}
	
	/**
	 * <p>
	 * Enables the specified error web route if it exists.
	 * </p>
	 * 
	 * @param route the error web route to enable
	 */
	void enableRoute(ErrorWebRoute route) {
		this.firstLink.enableRoute(route);
	}
	
	/**
	 * <p>
	 * Disables the specified error web route if it exists.
	 * </p>
	 * 
	 * @param route the error web route to disable
	 */
	void disableRoute(ErrorWebRoute route) {
		this.firstLink.disableRoute(route);
	}

	/**
	 * <p>
	 * Removes the specified error web route if it exists.
	 * </p>
	 * 
	 * @param route the error web route to remove
	 */
	void removeRoute(ErrorWebRoute route) {
		this.firstLink.removeRoute(route);
	}
	
	@Override
	public ErrorWebRouteManager route() {
		return new GenericErrorWebRouteManager(this);
	}
	
	@Override
	public Set<ErrorWebRoute> getRoutes() {
		GenericErrorWebRouteExtractor routeExtractor = new GenericErrorWebRouteExtractor(this);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public Mono<Void> defer(ErrorExchange<Throwable> exchange) {
		if(exchange.response().isHeadersWritten()) {
			throw new IllegalStateException("Headers already written", exchange.getError());
		}
		return this.firstLink.defer(new GenericErrorWebExchange(exchange, new GenericWebResponse(exchange.response(), this.dataConversionService)));
	}
	
	/**
	 * <p>
	 * Implements the ErrorExchangeHandler contract, however this should not be invoked in order to remain reactive. 
	 * </p>
	 */
	@Override
	public void handle(ErrorExchange<Throwable> exchange) throws HttpException {
		this.defer(exchange).block();
	}
	
	/**
	 * <p>
	 * Returns the default HttpException error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<HttpException> httpExceptionHandler() {
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
	
	/**
	 * <p>
	 * Returns the default Throwable error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<Throwable> throwableHandler() {
		return exchange -> {
			this.httpExceptionHandler().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	/**
	 * <p>
	 * Returns the {@code application/json} HttpException error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<HttpException> httpExceptionHandler_json() {
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
				error.append(",\"message\":\"").append(StringEscapeUtils.escapeJson(exchange.getError().getMessage())).append("\"");
			}
			error.append("}");
			
			exchange.response().headers(h -> h.status(exchange.getError().getStatusCode()).contentType(MediaTypes.APPLICATION_JSON)).body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(errorOut.toByteArray())));
		};
	}
	
	/**
	 * <p>
	 * Returns the {@code application/json} Throwable error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<Throwable> throwableHandler_json() {
		return exchange -> {
			this.httpExceptionHandler_json().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	/**
	 * <p>
	 * Returns the whitelabel {@code text/html} HttpException error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<HttpException> httpExceptionHandler_html() {
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
	
	/**
	 * <p>
	 * Returns the whitelabel {@code text/html} Throwable error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<Throwable> throwableHandler_html() {
		return exchange -> {
			this.httpExceptionHandler_html().handle(exchange.mapError(t -> new InternalServerErrorException(t)));
		};
	}
	
	@Bean( name = "errorRouterConfigurer")
	public static interface ConfigurerSocket extends Supplier<ErrorWebRouterConfigurer> {}
}
