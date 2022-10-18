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
package io.inverno.mod.web.internal;

import io.inverno.mod.base.net.URIMatcher;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ContentAware;
import io.inverno.mod.web.spi.MethodAware;
import io.inverno.mod.web.spi.PathAware;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic {@link WebRouteInterceptor} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.3
 */
class GenericWebRouteInterceptor implements Cloneable, WebRouteInterceptor<ExchangeContext> {

	private static final Logger LOGGER = LogManager.getLogger(GenericWebRouteInterceptor.class);
	
	private final HeaderCodec<? extends Headers.ContentType> contentTypeCodec;
	private final HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec;
	
	private String path;
	private URIPattern pathPattern;
	
	private Method method;
	
	private String produce;
	private Headers.Accept.MediaRange produceMediaRange;
	
	private String consume;
	private Headers.Accept.MediaRange consumeMediaRange;
	
	private String language;
	private Headers.AcceptLanguage.LanguageRange languageRange;
	
	private ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor;

	/**
	 * <p>
	 * Creates a generic web route interceptor.
	 * </p>
	 *
	 * @param contentTypeCodec    a content type header codec
	 * @param acceptLanguageCodec an accept language header codec
	 */
	public GenericWebRouteInterceptor(HeaderCodec<? extends Headers.ContentType> contentTypeCodec, HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec) {
		this.contentTypeCodec = contentTypeCodec;
		this.acceptLanguageCodec = acceptLanguageCodec;
	}
	
	private GenericWebRouteInterceptor withInterceptor(ExchangeInterceptorWrapper<ExchangeContext, WebExchange<ExchangeContext>> interceptor) {
		GenericWebRouteInterceptor clone = this.clone();
		clone.interceptor = interceptor;
		return clone;
	}

	public void setPath(String path) {
		this.pathPattern = null;
		this.path = path;
	}

	public void setPathPattern(URIPattern pathPattern) {
		this.pathPattern = pathPattern;
		this.path = null;
	}
	
	public void setMethod(Method method) {
		this.method = method;
	}

	public void setProduce(String mediaRange) {
		this.produce = mediaRange;
		this.produceMediaRange = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, mediaRange).toMediaRange();
	}

	public void setConsume(String mediaRange) {
		this.consume = mediaRange;
		this.consumeMediaRange = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, mediaRange).toMediaRange();
	}

	public void setLanguage(String languageRange) {
		this.language = languageRange;
		List<Headers.AcceptLanguage.LanguageRange> languageRanges = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, languageRange).getLanguageRanges();
		if(languageRanges.size() != 1) {
			throw new IllegalArgumentException("Invalid language range: " + languageRange);
		}
		this.languageRange = languageRanges.get(0);
	}

	public void setInterceptor(ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor) {
		this.interceptor = interceptor;
	}
	
	/* 
	 * A: the set of resources intercepted by the interceptor
	 * B: the set of resources served by the route
	 * Match is partial when B is not fully contained in A when B\A != {}
	 */
	@Override
	public WebRouteInterceptor<ExchangeContext> matches(PathAware pathAware) {
		if(this.path != null) {
			if(pathAware.getPath() != null) {
				if(this.path.equals(pathAware.getPath())) {
					// B\A == {}
					return this;
				}
				return null;
			}
			else if(pathAware.getPathPattern() != null) {
				if(pathAware.getPathPattern().matcher(this.path).matches()) {
					// B\A != {}
					return this.withInterceptor(new FilteredPathExchangeInterceptorWrapper());
				}
				return null;
			}
			else {
				// B\A != {}
				return this.withInterceptor(new FilteredPathExchangeInterceptorWrapper());
			}
		}
		else if(this.pathPattern != null) {
			if(pathAware.getPath() != null) {
				if(this.pathPattern.matcher(pathAware.getPath()).matches()) {
					// B\A == {}
					return this;
				}
				return null;
			}
			else if(pathAware.getPathPattern() != null) {
				// This is the trickiest
				// We need to determine whether the path pattern of the interceptor includes the path pattern of the route
				URIPattern.Inclusion includes = this.pathPattern.includes(pathAware.getPathPattern());
				
				switch(includes) {
					case INCLUDED: return this;
					case INDETERMINATE: return this.withInterceptor(new FilteredPathExchangeInterceptorWrapper()); 
					default: return null;
				}
			}
			else {
				// B\A != {}
				return this.withInterceptor(new FilteredPathExchangeInterceptorWrapper());
			}
		}
		else {
			// No restrictions
			return this;
		}
	}

	@Override
	public WebRouteInterceptor<ExchangeContext> matches(MethodAware methodAware) {
		if(this.method != null) {
			if(methodAware.getMethod() != null) {
				if(this.method == methodAware.getMethod()) {
					// B\A == {}
					return this;
				}
				return null;
			}
			else {
				// B\A != {}
				return this.withInterceptor(new FilteredMethodExchangeInterceptorWrapper());
			}
		}
		else {
			// No restrictions
			return this;
		}
	}

	@Override
	public WebRouteInterceptor<ExchangeContext> matches(ContentAware contentAware) {
		if(this.consume != null) {
			if(contentAware.getConsume() != null) {
				// interceptor: consume is a media range: A/B
				// route: consume is a media range: C/D
				Headers.Accept.MediaRange routeMediaRange = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, contentAware.getConsume()).toMediaRange();
				
				// if parameters do not match there's no match
				if(this.consumeMediaRange.getParameters().isEmpty() || this.consumeMediaRange.getParameters().equals(routeMediaRange.getParameters())) {
					// from there we have to compare types and subtypes to determine what to do
					
					String interceptorType = this.consumeMediaRange.getType();
					String interceptorSubType = this.consumeMediaRange.getSubType();
					String routeType = routeMediaRange.getType();
					String routeSubType = routeMediaRange.getSubType();
				
					if(interceptorType.equals("*")) {
						if(interceptorSubType.equals("*")) {
							// interceptor */*
							// route ?/? => B/A == {}
							return this;
						}
						else {
							// interceptor */x
							if(routeType.equals("*")) {
								if(routeSubType.equals("*")) {
									// route */* => B/A != {} && B/A != A
									return this.withInterceptor(new FilteredContentExchangeInterceptorWrapper());
								}
								else {
									// route */x
									if(interceptorSubType.equals(routeSubType)) {
										return this;
									}
									return null;
								}
							}
							else {
								if(routeSubType.equals("*")) {
									// route x/* => B/A != {} && B/A != A
									return this.withInterceptor(new FilteredContentExchangeInterceptorWrapper());
								}
								else {
									// route x/x
									if(interceptorSubType.equals(routeSubType)) {
										return this;
									}
									return null;
								}
							}
						}
					}
					else {
						if(interceptorSubType.equals("*")) {
							// interceptor x/*
							if(routeType.equals("*")) {
								// route */? => B/A != {}
								return this.withInterceptor(new FilteredContentExchangeInterceptorWrapper());
							}
							else {
								// route x/?
								if(interceptorType.equals(routeType)) {
									return this;
								}
								return null;
							}
						}
						else {
							// interceptor x/x
							if(routeType.equals("*")) {
								if(routeSubType.equals("*") || interceptorSubType.equals(routeSubType)) {
									// route */*|*/x => B/A != {}
									return this.withInterceptor(new FilteredContentExchangeInterceptorWrapper());
								}
								return null;
							}
							else {
								if(interceptorType.equals(routeType)) {
									if(routeSubType.equals("*")) {
										// route x/* => B/A != {}
										return this.withInterceptor(new FilteredContentExchangeInterceptorWrapper());
									}
									else {
										// route x/x 
										if(interceptorSubType.equals(routeSubType)) {
											return this;
										}
										return null;
									}
								}
								return null;
							}
						}
					}
				}
				return null;
			}
			else {
				// B\A != {}
				return this.withInterceptor(new FilteredContentExchangeInterceptorWrapper());
			}
		}
		else {
			// No retrictions
			return this;
		}
	}

	@Override
	public WebRouteInterceptor<ExchangeContext> matchesContentType(AcceptAware acceptAware) {
		String routeMediaType = acceptAware.getProduce();
		if(this.produce != null) {
			if(routeMediaType != null) {
				Headers.ContentType routeContentType = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, routeMediaType);
				if(this.produceMediaRange.matches(routeContentType)) {
					return this;
				}
				return null;
			}
			else {
				// B\A != {}
				/* 
				 * Here we actually don't know the content type before the route handler is actually executed, so what to do?
				 * - very defensive: we can break here and say we detected a bad defined route 
				 * - defensive: we can decide not to apply the interceptor and log a warning
				 * - offensive: we can apply the interceptor and log a warning saying that the route is incomplete
				 * we should go for the defensive option: if someone wants to apply inteceptors on routes producing a particular content, it is fair to assume these routes are defined with such information
				 */
				LOGGER.warn("Ignoring interceptor " + this.toString() + " on route " + acceptAware.toString() + ": content type is missing");
				return null;
			}
		}
		else {
			// No restrictions
			return this;
		}
	}

	@Override
	public WebRouteInterceptor<ExchangeContext> matchesContentLanguage(AcceptAware acceptAware) {
		String routeLanguageTag = acceptAware.getLanguage();
		if(this.language != null) {
			if(routeLanguageTag != null) {
				Headers.AcceptLanguage.LanguageRange routeLanguageRange = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, routeLanguageTag).getLanguageRanges().get(0);
				if(this.languageRange.matches(routeLanguageRange)) {
					return this;
				}
				return null;
			}
			else {
				// B\A != {}
				/* 
				 * Here we actually don't know the language tag before the route handler is actually executed, so what to do?
				 * - very defensive: we can break here and say we detected a bad defined route 
				 * - defensive: we can decide not to apply the interceptor and log a warning
				 * - offensive: we can apply the interceptor and log a warning saying that the route is incomplete
				 * we should go for the defensive option: if someone wants to apply inteceptors on routes producing a particular language, it is fair to assume these routes are defined with such information
				 */
				LOGGER.warn("Ignoring interceptor " + this.toString() + " on route " + acceptAware.toString() + ": language is missing");
				return null;
			}
		}
		else {
			// No restrictions
			return this;
		}
	}

	@Override
	public ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> getInterceptor() {
		return this.interceptor;
	}

	@Override
	protected GenericWebRouteInterceptor clone() {
		try {
			GenericWebRouteInterceptor clone = (GenericWebRouteInterceptor)super.clone();

			return clone;
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		StringBuilder routeStringBuilder = new StringBuilder();

		routeStringBuilder.append("{");
		routeStringBuilder.append("\"method\":\"").append(this.method != null ? this.method : null).append("\",");
		routeStringBuilder.append("\"path\":\"").append(this.path != null ? this.path : this.pathPattern).append("\",");
		routeStringBuilder.append("\"consume\":\"").append(this.consume).append("\",");
		routeStringBuilder.append("\"produce\":\"").append(this.produce).append("\",");
		routeStringBuilder.append("\"language\":\"").append(this.language);
		routeStringBuilder.append("}");

		return routeStringBuilder.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((consume == null) ? 0 : consume.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((pathPattern == null) ? 0 : pathPattern.hashCode());
		result = prime * result + ((produce == null) ? 0 : produce.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericWebRouteInterceptor other = (GenericWebRouteInterceptor) obj;
		if (consume == null) {
			if (other.consume != null)
				return false;
		} else if (!consume.equals(other.consume))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (pathPattern == null) {
			if (other.pathPattern != null)
				return false;
		} else if (!pathPattern.equals(other.pathPattern))
			return false;
		if (produce == null) {
			if (other.produce != null)
				return false;
		} else if (!produce.equals(other.produce))
			return false;
		return true;
	}

	private class FilteredMethodExchangeInterceptorWrapper extends ExchangeInterceptorWrapper<ExchangeContext, WebExchange<ExchangeContext>> {

		public FilteredMethodExchangeInterceptorWrapper() {
			super(GenericWebRouteInterceptor.this.interceptor);
		}
		
		@Override
		public Mono<? extends WebExchange<ExchangeContext>> intercept(WebExchange<ExchangeContext> exchange) {
			if(GenericWebRouteInterceptor.this.method == exchange.request().getMethod()) {
				// Interceptor method equals request method: the exchange must be intercepted
				return GenericWebRouteInterceptor.this.interceptor.intercept(exchange);
			}
			// Interceptor method differs from request method: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}
	
	private class FilteredContentExchangeInterceptorWrapper extends ExchangeInterceptorWrapper<ExchangeContext, WebExchange<ExchangeContext>> {

		public FilteredContentExchangeInterceptorWrapper() {
			super(GenericWebRouteInterceptor.this.interceptor);
		}
		
		@Override
		public Mono<? extends WebExchange<ExchangeContext>> intercept(WebExchange<ExchangeContext> exchange) {
			if(exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).map(GenericWebRouteInterceptor.this.consumeMediaRange::matches).orElse(false)) {
				// Interceptor consume media range matches request content type: the exchange must be intercepted
				return GenericWebRouteInterceptor.this.interceptor.intercept(exchange);
			}
			// Interceptor consume media range does not match request content type: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}
	
	private class FilteredPathExchangeInterceptorWrapper extends ExchangeInterceptorWrapper<ExchangeContext, WebExchange<ExchangeContext>> {

		public FilteredPathExchangeInterceptorWrapper() {
			super(GenericWebRouteInterceptor.this.interceptor);
		}

		@Override
		public Mono<? extends WebExchange<ExchangeContext>> intercept(WebExchange<ExchangeContext> exchange) {
			String normalizedPath = exchange.request().getPathAbsolute();
			if(GenericWebRouteInterceptor.this.path != null) {
				if(GenericWebRouteInterceptor.this.path.equals(normalizedPath)) {
					// Interceptor path is equals to request path: the exchange must be intercepted
					return GenericWebRouteInterceptor.this.interceptor.intercept(exchange);
				}
				// Interceptor path is not equals to request path: the exchange must not be intercepted
				return Mono.just(exchange);
			}
			else if(GenericWebRouteInterceptor.this.pathPattern != null) {
				URIMatcher matcher = GenericWebRouteInterceptor.this.pathPattern.matcher(normalizedPath);
				if(matcher.matches()) {
					// Interceptor path pattern matches request path: the exchange must be intercepted
					return GenericWebRouteInterceptor.this.interceptor.intercept(exchange);
				}
				// Interceptor path pattern matches request path: the exchange must not be intercepted
				return Mono.just(exchange);
			}
			else {
				// This should never happen
				throw new IllegalStateException("Filtered path interceptor has no defined path");
			}
		}
	}
}
