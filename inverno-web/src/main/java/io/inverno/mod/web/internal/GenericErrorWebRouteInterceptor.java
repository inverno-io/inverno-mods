package io.inverno.mod.web.internal;

import io.inverno.mod.base.net.URIMatcher;
import io.inverno.mod.base.net.URIPattern;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.web.ErrorWebExchange;
import io.inverno.mod.web.spi.AcceptAware;
import io.inverno.mod.web.spi.ErrorAware;
import io.inverno.mod.web.spi.PathAware;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;

public class GenericErrorWebRouteInterceptor implements Cloneable, ErrorWebRouteInterceptor {

	private static final Logger LOGGER = LogManager.getLogger(GenericErrorWebRouteInterceptor.class);

	private final HeaderCodec<? extends Headers.ContentType> contentTypeCodec;
	private final HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec;

	private String path;
	private URIPattern pathPattern;

	private String produce;
	private Headers.Accept.MediaRange produceMediaRange;

	private String language;
	private Headers.AcceptLanguage.LanguageRange languageRange;

	private Class<? extends Throwable> error;

	private ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>> interceptor;

	public GenericErrorWebRouteInterceptor(HeaderCodec<? extends Headers.ContentType> contentTypeCodec, HeaderCodec<? extends Headers.AcceptLanguage> acceptLanguageCodec) {
		this.contentTypeCodec = contentTypeCodec;
		this.acceptLanguageCodec = acceptLanguageCodec;
	}

	private GenericErrorWebRouteInterceptor withInterceptor(ExchangeInterceptorWrapper<ExchangeContext, ErrorWebExchange<Throwable>> interceptor) {
		GenericErrorWebRouteInterceptor clone = this.clone();
		clone.interceptor = interceptor;
		return clone;
	}

	public void setError(Class<? extends Throwable> error) {
		this.error = error;
	}

	public void setPath(String path) {
		this.pathPattern = null;
		this.path = path;
	}

	public void setPathPattern(URIPattern pathPattern) {
		this.pathPattern = pathPattern;
		this.path = null;
	}

	public void setProduce(String mediaRange) {
		this.produce = mediaRange;
		this.produceMediaRange = this.contentTypeCodec.decode(Headers.NAME_CONTENT_TYPE, mediaRange).toMediaRange();
	}

	public void setLanguage(String languageRange) {
		this.language = languageRange;
		List<Headers.AcceptLanguage.LanguageRange> languageRanges = this.acceptLanguageCodec.decode(Headers.NAME_ACCEPT_LANGUAGE, languageRange).getLanguageRanges();
		if(languageRanges.size() != 1) {
			throw new IllegalArgumentException("Invalid language range: " + languageRange);
		}
		this.languageRange = languageRanges.get(0);
	}

	public void setInterceptor(ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>> interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public ErrorWebRouteInterceptor matches(ErrorAware errorAware) {
		if(this.error != null) {
			if(errorAware.getError() != null) {
				if(this.error.isAssignableFrom(errorAware.getError())) {
					// B\A == {}
					return this;
				}
				return null;
			}
			else {
				// B\A != {}
				return this.withInterceptor(new FilteredErrorExchangeInterceptorWrapper());
			}
		}
		else {
			// No restrictions
			return this;
		}
	}

	@Override
	public ErrorWebRouteInterceptor matches(PathAware pathAware) {
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
	public ErrorWebRouteInterceptor matchesContentType(AcceptAware acceptAware) {
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
	public ErrorWebRouteInterceptor matchesContentLanguage(AcceptAware acceptAware) {
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
	public ExchangeInterceptor<ExchangeContext, ErrorWebExchange<Throwable>> getInterceptor() {
		return this.interceptor;
	}

	@Override
	protected GenericErrorWebRouteInterceptor clone() {
		try {
			GenericErrorWebRouteInterceptor clone = (GenericErrorWebRouteInterceptor)super.clone();

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
		routeStringBuilder.append("\"error\":\"").append(this.error).append("\",");
		routeStringBuilder.append("\"path\":\"").append(this.path != null ? this.path : this.pathPattern).append("\",");
		routeStringBuilder.append("\"produce\":\"").append(this.produce).append("\",");
		routeStringBuilder.append("\"language\":\"").append(this.language);
		routeStringBuilder.append("}");

		return routeStringBuilder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((error == null) ? 0 : error.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((pathPattern == null) ? 0 : pathPattern.hashCode());
		result = prime * result + ((produce == null) ? 0 : produce.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
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
		GenericErrorWebRouteInterceptor other = (GenericErrorWebRouteInterceptor) obj;
		if (error == null) {
			if (other.error != null)
				return false;
		} else if (!error.equals(other.error))
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
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		return true;
	}

	private class FilteredErrorExchangeInterceptorWrapper extends ExchangeInterceptorWrapper<ExchangeContext, ErrorWebExchange<Throwable>> {

		public FilteredErrorExchangeInterceptorWrapper() {
			super(GenericErrorWebRouteInterceptor.this.interceptor);
		}

		@Override
		public Mono<? extends ErrorWebExchange<Throwable>> intercept(ErrorWebExchange<Throwable> exchange) {
			if(GenericErrorWebRouteInterceptor.this.error.isAssignableFrom(exchange.getError().getClass())) {
				// Interceptor error is assignable from the exchange error type: the exchange must be intercepted
				return GenericErrorWebRouteInterceptor.this.interceptor.intercept(exchange);
			}
			// Interceptor error is not assignable from the exchange error type: the exchange must not be intercepted
			return Mono.just(exchange);
		}
	}

	private class FilteredPathExchangeInterceptorWrapper extends ExchangeInterceptorWrapper<ExchangeContext, ErrorWebExchange<Throwable>> {

		public FilteredPathExchangeInterceptorWrapper() {
			super(GenericErrorWebRouteInterceptor.this.interceptor);
		}

		@Override
		public Mono<? extends ErrorWebExchange<Throwable>> intercept(ErrorWebExchange<Throwable> exchange) {
			String normalizedPath = exchange.request().getPathAbsolute();
			if(GenericErrorWebRouteInterceptor.this.path != null) {
				if(GenericErrorWebRouteInterceptor.this.path.equals(normalizedPath)) {
					// Interceptor path is equals to request path: the exchange must be intercepted
					return GenericErrorWebRouteInterceptor.this.interceptor.intercept(exchange);
				}
				// Interceptor path is not equals to request path: the exchange must not be intercepted
				return Mono.just(exchange);
			}
			else if(GenericErrorWebRouteInterceptor.this.pathPattern != null) {
				URIMatcher matcher = GenericErrorWebRouteInterceptor.this.pathPattern.matcher(normalizedPath);
				if(matcher.matches()) {
					// Interceptor path pattern matches request path: the exchange must be intercepted
					return GenericErrorWebRouteInterceptor.this.interceptor.intercept(exchange);
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
