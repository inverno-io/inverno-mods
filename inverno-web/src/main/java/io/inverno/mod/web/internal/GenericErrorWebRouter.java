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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.*;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.internal.header.AcceptLanguageCodec;
import io.inverno.mod.http.base.internal.header.ContentTypeCodec;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.*;
import io.netty.buffer.Unpooled;
import org.apache.commons.text.StringEscapeUtils;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Generic {@link ErrorWebRouter} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.0
 */
@Bean( name = "errorRouter" )
public class GenericErrorWebRouter extends AbstractErrorWebRouter implements @Provide ErrorWebRouter<ExchangeContext> {
	
	private final DataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	
	private final RoutingLink<ExchangeContext, ErrorWebExchange<ExchangeContext>, ?, ErrorWebRoute<ExchangeContext>> firstLink;
	
	/**
	 * <p>
	 * Creates a generic error web router.
	 * </p>
	 * 
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 */
	public GenericErrorWebRouter(DataConversionService dataConversionService, ObjectConverter<String> parameterConverter) {
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		
		ContentTypeCodec contentTypeCodec = new ContentTypeCodec();
		AcceptLanguageCodec acceptLanguageCodec = new AcceptLanguageCodec(false);
		
		this.firstLink = new ThrowableRoutingLink<>();
		this.firstLink
			.connect(new PathRoutingLink<>())
			.connect(new PathPatternRoutingLink<>())
			.connect(new ProducesRoutingLink<>(contentTypeCodec))
			.connect(new LanguageRoutingLink<>(acceptLanguageCodec))
			.connect(new HandlerRoutingLink<>());
	}
	
	@Init
	@SuppressWarnings("unchecked")
	public void init() {
		this.route().produces(MediaTypes.APPLICATION_JSON).handler(this.httpExceptionHandler_json())
			.route().produces(MediaTypes.TEXT_HTML).handler(this.httpExceptionHandler_html())
			.route().handler(this.httpExceptionHandler());
	}
	
	@Override
	void setRoute(ErrorWebRoute<ExchangeContext> route) {
		this.firstLink.setRoute(route);
	}

	@Override
	void enableRoute(ErrorWebRoute<ExchangeContext> route) {
		this.firstLink.enableRoute(route);
	}

	@Override
	void disableRoute(ErrorWebRoute<ExchangeContext> route) {
		this.firstLink.disableRoute(route);
	}

	@Override
	void removeRoute(ErrorWebRoute<ExchangeContext> route) {
		this.firstLink.removeRoute(route);
	}

	@Override
	public ErrorWebInterceptorManager<ExchangeContext, ErrorWebInterceptedRouter<ExchangeContext>> intercept() {
		return new GenericErrorWebInterceptorManager(new GenericErrorWebInterceptedRouter(this), CONTENT_TYPE_CODEC, ACCEPT_LANGUAGE_CODEC);
	}

	@Override
	public ErrorWebRouteManager<ExchangeContext, ErrorWebRouter<ExchangeContext>> route() {
		return new GenericErrorWebRouteManager(this);
	}
	
	@Override
	public Set<ErrorWebRoute<ExchangeContext>> getRoutes() {
		GenericErrorWebRouteExtractor routeExtractor = new GenericErrorWebRouteExtractor(this);
		this.firstLink.extractRoute(routeExtractor);
		return routeExtractor.getRoutes();
	}
	
	@Override
	public Mono<Void> defer(ErrorExchange<ExchangeContext> exchange) {
		if(exchange.response().isHeadersWritten()) {
			throw new IllegalStateException("Headers already written", exchange.getError());
		}
		return this.firstLink.defer(new GenericErrorWebExchange(new GenericWebRequest(exchange.request(), this.parameterConverter), new GenericWebResponse(exchange.response(), this.dataConversionService), exchange.getError(), exchange.context(), exchange::finalizer));
	}
	
	/**
	 * <p>
	 * Implements the ErrorExchangeHandler contract, however this should not be invoked in order to remain reactive. 
	 * </p>
	 */
	@Override
	public void handle(ErrorExchange<ExchangeContext> exchange) throws HttpException {
		this.defer(exchange).block();
	}
	
	/**
	 * <p>
	 * Returns the default HttpException error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<ExchangeContext> httpExceptionHandler() {
		return exchange -> {
			final HttpException error = HttpException.wrap(exchange.getError());
			if(error instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)error).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
			}
			else if(error instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)error).getRetryAfter().ifPresent(retryAfter -> {
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
				});
			}
			exchange.response().headers(h -> h.status(error.getStatusCode())).body().empty();
		};
	}
	
	/**
	 * <p>
	 * Returns the {@code application/json} HttpException error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<ExchangeContext> httpExceptionHandler_json() {
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
				((NotAcceptableException)error).getAcceptableMediaTypes().ifPresent(acceptableMediaTypes -> {
					errorStream.append(",\"accept\":[");
					errorStream.append(acceptableMediaTypes.stream().map(acceptableMediaType -> "\"" + acceptableMediaType + "\"").collect(Collectors.joining(", ")));
					errorStream.append("]");
				});
			}
			else if(error instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)error).getRetryAfter().ifPresent(retryAfter -> {
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
					errorStream.append(",\"retryAfter\":\"").append(retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("\"");
				});
			}

			if(error.getMessage() != null) {
				errorStream.append(",\"message\":\"").append(StringEscapeUtils.escapeJson(error.getMessage())).append("\"");
			}
			errorStream.append("}");

			exchange.response().headers(h -> h.status(error.getStatusCode()).contentType(MediaTypes.APPLICATION_JSON)).body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(errorOut.toByteArray())));
		};
	}
	
	/**
	 * <p>
	 * Returns the whitelabel {@code text/html} HttpException error handler.
	 * </p>
	 * 
	 * @return an error web exchange handler
	 */
	private ErrorWebExchangeHandler<ExchangeContext> httpExceptionHandler_html() {
		return exchange -> {
			final HttpException error = HttpException.wrap(exchange.getError());
			String status = Integer.toString(error.getStatusCode());
			
			ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
			PrintStream errorStream = new PrintStream(errorOut);
			errorStream.append("<html><head><title>").append(status).append(" ").append(error.getStatusReasonPhrase()).append("</title></head><body style=\"font-family: arial,sans-serif;padding:30px;max-width: 1280px;margin: auto;\">");
			errorStream.append("<h1 style=\"font-size: 3em;\"><span style=\"color:red;\">").append(status).append("</span> ").append(error.getStatusReasonPhrase()).append("</h1>");
			
			if(error instanceof MethodNotAllowedException) {
				exchange.response().headers(headers -> headers.add(Headers.NAME_ALLOW, ((MethodNotAllowedException)error).getAllowedMethods().stream().map(Method::toString).collect(Collectors.joining(", "))));
				errorStream.append("<p>");
				errorStream.append("Allowed Methods:");
				errorStream.append("<ul>");
				((MethodNotAllowedException)error).getAllowedMethods().stream().forEach(allowedMethod -> errorStream.append("<li>").append(allowedMethod.toString()).append("</li>"));
				errorStream.append("</ul>");
				errorStream.append("</p>");
			}
			else if(error instanceof NotAcceptableException) {
				((NotAcceptableException)error).getAcceptableMediaTypes().ifPresent(acceptableMediaTypes -> {
					errorStream.append("<p>");
					errorStream.append("Accept:");
					errorStream.append("<ul>");
					acceptableMediaTypes.stream().forEach(acceptableMediaType -> errorStream.append("<li>").append(acceptableMediaType).append("</li>"));
					errorStream.append("</ul>");
					errorStream.append("</p>");
				});
			}
			else if(error instanceof ServiceUnavailableException) {
				((ServiceUnavailableException)error).getRetryAfter().ifPresent(retryAfter -> {
					errorStream.append("<p>");
					errorStream.append("Retry After: ").append(retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME));
					exchange.response().headers(headers -> headers.add(Headers.NAME_RETRY_AFTER, retryAfter.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
					errorStream.append("</p>");
				});
			}
			
			if(error.getMessage() != null) {
				errorStream.append("<p>").append(error.getMessage()).append("</p>");
			}
			
			errorStream.append("<pre style=\"color:#444444;background-color: #F7F7F7;border:1px solid #E7E7E7;border-radius: 3px;box-shadow: rgba(0, 0, 0, 0.1) 2px 2px 10px;padding:20px;overflow:auto;\">");
			error.printStackTrace(new PrintStream(errorStream));
			errorStream.append("</pre>");
			errorStream.append("<footer style=\"text-align:center;color: #444444;\">");
			errorStream.append("<p><small>This is a whitelabel error page, providing a custom error handler is recommended.</small></p>");
			errorStream.append("</footer>");
			errorStream.append("</body>");
			
			exchange.response().headers(headers -> headers.status(error.getStatusCode()).contentType(MediaTypes.TEXT_HTML)).body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(errorOut.toByteArray())));
		};
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> configureInterceptors(ErrorWebInterceptorsConfigurer<? super ExchangeContext> configurer) {
		GenericErrorWebInterceptedRouter interceptedRouter = new GenericErrorWebInterceptedRouter(this);
		if(configurer != null) {
			GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(interceptedRouter);
			configurer.configure(facade);

			return facade.getInterceptedRouter();
		}
		return interceptedRouter;
	}

	@Override
	public ErrorWebInterceptedRouter<ExchangeContext> configureInterceptors(List<ErrorWebInterceptorsConfigurer<? super ExchangeContext>> configurers) {
		GenericErrorWebInterceptedRouter interceptedRouter = new GenericErrorWebInterceptedRouter(this);
		if(configurers != null && !configurers.isEmpty()) {
			GenericErrorWebInterceptableFacade facade = new GenericErrorWebInterceptableFacade(interceptedRouter);
			configurers.forEach(c -> c.configure(facade));
			return facade.getInterceptedRouter();
		}
		return interceptedRouter;
	}

	@Override
	public ErrorWebRouter<ExchangeContext> configureRoutes(ErrorWebRoutesConfigurer<? super ExchangeContext> configurer) {
		if(configurer != null) {
			GenericErrorWebRoutableFacade<ErrorWebRouter<ExchangeContext>> facade = new GenericErrorWebRoutableFacade<>(this);
			configurer.configure(facade);
		}
		return this;
	}
}
