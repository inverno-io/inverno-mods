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
package io.inverno.mod.security.http.cors;

import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeInterceptor;
import io.inverno.mod.http.server.ResponseHeaders;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A security interceptor that implements Cross-origin resource sharing (CORS) as defined by <a href="https://fetch.spec.whatwg.org/#http-cors-protocol">HTTP CORS protocol</a>.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the type of the exchange context
 * @param <B> the type of exchange handled by the handler
 */
public class CORSInterceptor<A extends ExchangeContext, B extends Exchange<A>> implements ExchangeInterceptor<A, B> {

	/**
	 * The default HTTP port.
	 */
	private static final int DEFAULT_HTTP_PORT = 80;
	
	/**
	 * The default HTTPS port.
	 */
	private static final int DEFAULT_HTTPS_PORT = 443;
	
	/**
	 * The default FTP port.
	 */
	private static final int DEFAULT_FTP_PORT = 21;
	
	/**
	 * The set of allowed origins.
	 */
	protected final Set<Origin> allowedOrigins;
	
	/**
	 * The set of allowed origins patterns.
	 */
	protected final Set<Pattern> allowedOriginsPattern;
	
	/**
	 * Flag indicating whether credentials must be allowed.
	 */
	protected final boolean allowCredentials;
	
	/**
	 * The allowed headers.
	 */
	protected final String allowedHeaders;
	
	/**
	 * The allowed methods.
	 */
	protected final String allowedMethods;
	
	/**
	 * The exposed headers.
	 */
	protected final String exposedHeaders;
	
	/**
	 * The max age in seconds for CORS information cache.
	 */
	protected final Integer maxAge;

	/**
	 * Flag indicating whether private netword must be allowed.
	 */
	protected final boolean allowPrivateNetwork;
	
	/**
	 * Flag indicating whether the interceptor is a wildcard interceptor (allow all origins).
	 */
	protected final boolean isWildcard;
	
	/**
	 * Flag indicating whether the interceptor is a static interceptor (allow one static origin).
	 */
	protected final boolean isStatic;

	/**
	 * <p>
	 * Creates a CORS interceptor.
	 * </p>
	 *
	 * @param allowedOrigins        the set of allowed origins
	 * @param allowedOriginsPattern the set of allowed origins patterns
	 * @param allowCredentials      true to allow credentials, false otherwise
	 * @param allowedHeaders        the set of allowed headers
	 * @param allowedMethods        the set of allowed methods
	 * @param exposedHeaders        the set of exposed headers
	 * @param maxAge                the max age
	 * @param allowPrivateNetwork   true to allow private network, false otherwise
	 */
	protected CORSInterceptor(Set<Origin> allowedOrigins, Set<Pattern> allowedOriginsPattern, boolean allowCredentials, Set<String> allowedHeaders, Set<Method> allowedMethods, Set<String> exposedHeaders, Integer maxAge, boolean allowPrivateNetwork) {
		this.allowedOrigins = allowedOrigins != null ? Collections.unmodifiableSet(allowedOrigins) : Set.of();
		this.allowedOriginsPattern = allowedOriginsPattern != null ? Collections.unmodifiableSet(allowedOriginsPattern) : Set.of();
		this.allowCredentials = allowCredentials;
		this.allowedHeaders = allowedHeaders != null ? allowedHeaders.stream().collect(Collectors.joining(",")) : null;
		this.allowedMethods = allowedMethods != null ? allowedMethods.stream().map(Method::toString).collect(Collectors.joining(",")) : null;
		this.exposedHeaders = exposedHeaders != null ? exposedHeaders.stream().collect(Collectors.joining(",")) : null;
		this.maxAge = maxAge;
		this.allowPrivateNetwork = allowPrivateNetwork;
		
		this.isWildcard = this.allowedOrigins.isEmpty() && this.allowedOriginsPattern.isEmpty();
		this.isStatic = this.allowedOriginsPattern.isEmpty() && this.allowedOrigins.size() == 1;
	}
	
	/**
	 * <p>
	 * Returns a CORS interceptor builder for the specified allowed origins.
	 * </p>
	 * 
	 * @param allowedOrigins a list of static allowed origins
	 * 
	 * @return a new CORS interceptor builder
	 */
	public static CORSInterceptor.Builder builder(String... allowedOrigins) {
		CORSInterceptor.Builder builder = new CORSInterceptor.Builder();
		if(allowedOrigins != null) {
			for(String allowedOrigin : allowedOrigins) {
				builder.allowOrigin(allowedOrigin);
			}
		}
		return builder;
	}
	
	@Override
	public Mono<? extends B> intercept(B exchange) {
		// Determine whether this is a CORS request
		Optional<String> originOpt = exchange.request().headers().get(Headers.NAME_ORIGIN);
		if(!originOpt.isPresent()) {
			// Not a CORS request
			
			// https://fetch.spec.whatwg.org/#cors-protocol-and-http-caches
			// However, if `Access-Control-Allow-Origin` is set to * or a static origin for a particular resource, then configure the server to always send `Access-Control-Allow-Origin` in responses 
			// for the resource — for non-CORS requests as well as CORS requests — and do not use `Vary`. 
			if(!this.isWildcard && !this.isStatic) {
				exchange.response().headers(headers -> addVaryHeader(headers, Headers.NAME_ORIGIN));
			}
			else if(this.isWildcard) {
				exchange.response().headers(headers -> headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
			}
			else if(this.isStatic) {
				exchange.response().headers(headers -> headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, this.allowedOrigins.iterator().next().toString()));
			}
			return Mono.just(exchange);
		}
		
		// Is origin allowed?
		String originValue = originOpt.get();
		Origin origin;
		try {
			origin = new Origin(originValue);
		}
		catch(IllegalArgumentException e) {
			throw new BadRequestException("Invalid origin header: " + originOpt.get(), e);
		}
		
		if(this.isSameOrigin(exchange, origin)) {
			// Request is authorized as it comes from the same origin
			return Mono.just(exchange);
		}
		this.checkOrigin(origin);
		
		boolean preflight = exchange.request().getMethod().equals(Method.OPTIONS) && exchange.request().headers().contains(Headers.NAME_ACCESS_CONTROL_REQUEST_METHOD);
		exchange.response().headers(headers -> {
			if(this.allowCredentials) {
				headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
				headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, originValue);
			}
			else if(this.isWildcard) {
				headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, "*");
			}
			else {
				headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_ORIGIN, originValue);
			}
			
			if(preflight) {
				headers.status(Status.NO_CONTENT).contentLength(0);

				if(this.allowedMethods != null) {
					headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_METHODS, this.allowedMethods);
				}

				if(this.allowedHeaders != null) {
					headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_HEADERS, this.allowedHeaders);
				}
				else {
					exchange.request().headers().get(Headers.NAME_ACCESS_CONTROL_REQUEST_HEADERS).ifPresent(value -> {
						addVaryHeader(headers, Headers.NAME_ACCESS_CONTROL_REQUEST_HEADERS);
						headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_HEADERS, value);
					});
				}

				if(this.maxAge != null) {
					headers.set(Headers.NAME_ACCESS_CONTROL_MAX_AGE, this.maxAge.toString());
				}

				if(this.allowPrivateNetwork) {
					exchange.request().headers().get(Headers.NAME_ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK).ifPresent(value -> {
						if(Boolean.valueOf(value)) {
							headers.set(Headers.NAME_ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK, "true");
						}
					});
				}
			}
			else {
				addVaryHeader(headers, Headers.NAME_ORIGIN);
				if(this.exposedHeaders != null) {
					headers.set(Headers.NAME_ACCESS_CONTROL_EXPOSE_HEADERS, this.exposedHeaders);
				}
			}
		});
		
		if(preflight) {
			exchange.response().body().empty();
			return Mono.empty();
		}
		else {
			return Mono.just(exchange);
		}
	}
	
	/**
	 * <p>
	 * Adds the {@code vary} header to the response.
	 * </p>
	 * 
	 * @param responseHeaders the response headers
	 * @param headerName the name of the varying header
	 */
	private static void addVaryHeader(ResponseHeaders responseHeaders, String headerName) {
		responseHeaders.get(Headers.NAME_VARY).ifPresentOrElse(
			vary -> {
				responseHeaders.set(Headers.NAME_VARY, vary + "," + headerName);
			},
			() -> {
				responseHeaders.set(Headers.NAME_VARY, headerName);
			}
		);
	}
	
	/**
	 * <p>
	 * Determines whether the request was issued from the same origin.
	 * </p>
	 * 
	 * @param exchange the exchange
	 * @param origin the target origin
	 * 
	 * @return true if the origin is the same, false otherwise or if it could not be determined
	 */
	protected boolean isSameOrigin(B exchange, Origin origin) {
		// Check whether request was issued from the same origin
		String scheme = exchange.request().getScheme();
		String authority = exchange.request().getAuthority();
		if(scheme != null && authority != null) {
			Origin requestOrigin;
			String[] splitAuthority = authority.split(":");
			switch(splitAuthority.length) {
				case 1: {
					// no port
					requestOrigin = new Origin(scheme, splitAuthority[0], null);
					break;
				}
				case 2: {
					// port
					requestOrigin = new Origin(scheme, splitAuthority[0], Integer.parseInt(splitAuthority[1]));
					break;
				}
				default: {
					// https://www.rfc-editor.org/rfc/rfc9110.html#name-host-and-authority 
					throw new BadRequestException("Invalid authority");
				}
			}
			return requestOrigin.equals(origin);
		}
		// We could not determine whether the request was issued from the same origin
		return false;
	}
	
	/**
	 * <p>
	 * Determines whether the origin is a valid origin.
	 * </p>
	 * 
	 * @param origin the origin to check

	 * @throws ForbiddenException if the origin is not authorized
	 */
	protected void checkOrigin(Origin origin) throws BadRequestException, ForbiddenException {
		// Check whether origin is allowed
		if(this.isWildcard) {
			// *
			return;
		}
		
		for(Pattern matcher : this.allowedOriginsPattern) {
			if(matcher.matcher(origin.toString()).matches()) {
				return;
			}
		}
		
		for(Origin allowedOrigin : this.allowedOrigins) {
			if(allowedOrigin.equals(origin)) {
				return;
			}
		}
		
		throw new ForbiddenException("Rejected CORS: " + origin.toString() + " is not auhorized to access resources");
	}
	
	
	/**
	 * <p>
	 * A CORS interceptor builder.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Builder {
		
		/**
		 * The set of allowed origins.
		 */
		protected Set<Origin> allowedOrigins;
		
		/**
		 * The set of allowed origins patterns.
		 */
		protected Set<Pattern> allowedOriginsPattern;
	
		/**
		 * The allow credentials flag.
		 */
		protected boolean allowCredentials;

		/**
		 * The set of allowed headers.
		 */
		protected Set<String> allowedHeaders;

		/**
		 * The set of allowed methods.
		 */
		protected Set<Method> allowedMethods;

		/**
		 * The set of exposed headers.
		 */
		protected Set<String> exposedHeaders;

		/**
		 * The max age in seconds.
		 */
		protected Integer maxAge;
		
		/**
		 * The allow private network flag.
		 */
		protected boolean allowPrivateNetwork;
		
		/**
		 * <p>
		 * Creates a CORS interceptor builder.
		 * </p>
		 */
		protected Builder() {}
		
		/**
		 * <p>
		 * Specifies an allowed origin.
		 * </p>
		 * 
		 * <p>
		 * {@code *} overrides previous and further values and allow all origins.
		 * </p>
		 * 
		 * @param allowedOrigin the origin to allow
		 * 
		 * @return this builder
		 * 
		 * @throws IllegalArgumentException if the specified origin is invalid
		 */
		public CORSInterceptor.Builder allowOrigin(String allowedOrigin) throws IllegalArgumentException {
			if(StringUtils.isNotBlank(allowedOrigin)) {
				if(allowedOrigin.equals("*")) {
					// Set wildcard by emptying allowed origin sets
					this.allowedOrigins = Set.of();
					this.allowedOriginsPattern = Set.of();
					return this;
				}
				
				if(this.allowedOrigins == null) {
					this.allowedOrigins = new HashSet<>();
				}
				else if(this.allowedOrigins.isEmpty()) {
					// Ignore since a wildcard has been previously set
					return this;
				}
				this.allowedOrigins.add(new Origin(allowedOrigin));
			}
			return this;
		}
		
		/**
		 * <p>
		 * Specifies an allowed origin pattern.
		 * </p>
		 * 
		 * <p>
		 * {@code .*} regex overrides previous and further values and allow all origins.
		 * </p>
		 * 
		 * @param allowedOriginRegex the origin pattern to allow
		 * 
		 * @return this builder
		 * 
		 * @throws IllegalArgumentException if the specified origin regex is invalid
		 */
		public CORSInterceptor.Builder allowOriginPattern(String allowedOriginRegex) {
			if(StringUtils.isNotBlank(allowedOriginRegex)) {
				if(allowedOriginRegex.equals(".*")) {
					// Set wildcard by emptying allowed origin sets
					this.allowedOrigins = Set.of();
					this.allowedOriginsPattern = Set.of();
					return this;
				}
				
				if(this.allowedOriginsPattern == null) {
					this.allowedOriginsPattern = new HashSet<>();
				}
				else if(this.allowedOriginsPattern.isEmpty()) {
					// Ignore since a wildcard has been previously set
					return this;
				}
				this.allowedOriginsPattern.add(Pattern.compile(allowedOriginRegex));
			}
			return this;
		}
		
		/**
		 * <p>
		 * Sepcifies the allow credentials flag.
		 * </p>
		 * 
		 * @param allowCredentials true to allow credentials, false otherwise
		 * 
		 * @return this builder
		 */
		public CORSInterceptor.Builder allowCredentials(boolean allowCredentials) {
			this.allowCredentials = allowCredentials;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies an allowed header.
		 * </p>
		 * 
		 * @param allowedHeader the header to allow
		 * 
		 * @return this builder
		 */
		public CORSInterceptor.Builder allowHeader(String allowedHeader) {
			if(StringUtils.isNotBlank(allowedHeader)) {
				if(this.allowedHeaders == null) {
					this.allowedHeaders = new HashSet<>();
				}
				this.allowedHeaders.add(allowedHeader);
			}
			return this;
		}
		
		/**
		 * <p>
		 * Specifies an allowed method.
		 * </p>
		 * 
		 * @param allowedMethod the method to allow
		 * 
		 * @return this builder
		 */
		public CORSInterceptor.Builder allowMethod(Method allowedMethod) {
			if(allowedMethod != null) {
				if(this.allowedMethods == null) {
					this.allowedMethods = new HashSet<>();
				}
				this.allowedMethods.add(allowedMethod);
			}
			return this;
		}
		
		/**
		 * <p>
		 * Specifies a header to expose.
		 * </p>
		 * 
		 * @param exposedHeader the header to expose
		 * 
		 * @return this builder
		 */
		public CORSInterceptor.Builder exposeHeader(String exposedHeader) {
			if(StringUtils.isNotBlank(exposedHeader)) {
				if(this.exposedHeaders == null) {
					this.exposedHeaders = new HashSet<>();
				}
				this.exposedHeaders.add(exposedHeader);
			}
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the max age for CORS information cache.
		 * </p>
		 * 
		 * @param maxAge the max age in seconds
		 * 
		 * @return this builder
		 */
		public CORSInterceptor.Builder maxAge(int maxAge) {
			this.maxAge = maxAge;
			return this;
		}
		
		/**
		 * <p>
		 * Specifies the allow private network flag.
		 * </p>
		 * 
		 * @param allowPrivateNetwork true to allow private network, false otherwise
		 * 
		 * @return this builder
		 */
		public CORSInterceptor.Builder allowPrivateNetwork(boolean allowPrivateNetwork) {
			this.allowPrivateNetwork = allowPrivateNetwork;
			return this;
		}
		
		/**
		 * <p>
		 * Builds and returns a CORS interceptor.
		 * </p>
		 *
		 * @param <A> the type of the exchange context
		 * @param <B> the type of exchange handled by the handler
		 *
		 * @return a new CORS interceptor
		 */
		public <A extends ExchangeContext, B extends Exchange<A>> CORSInterceptor<A, B> build() {
			return new CORSInterceptor<>(this.allowedOrigins, this.allowedOriginsPattern, this.allowCredentials, this.allowedHeaders, this.allowedMethods, this.exposedHeaders, this.maxAge, this.allowPrivateNetwork);
		}
	}
	
	/**
	 * <p>
	 * Represents an origin composed of a scheme, a host and a port.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	protected static class Origin {
		
		/**
		 * The scheme.
		 */
		protected final String scheme;
		
		/**
		 * The host.
		 */
		protected final String host;
		
		/**
		 * The port.
		 */
		protected final int port;
		
		/**
		 * The origin value ({@code [scheme]://[host]:[port]}).
		 */
		protected final String value;
		
		/**
		 * <p>
		 * Creates an origin by parsing the specified origin string.
		 * </p>
		 * 
		 * @param origin the origin
		 * 
		 * @throws IllegalArgumentException if the specified origin is invalid
		 */
		protected Origin(String origin) throws IllegalArgumentException {
			URI originURI = URI.create(origin);
			
			String originURIScheme = originURI.getScheme();
			String originURIHost = originURI.getHost();
			
			if(originURIScheme == null || originURIHost == null) {
				throw new IllegalArgumentException("Invalid origin: " + origin);
			}
			
			this.scheme = originURIScheme.toLowerCase();
			this.host = originURIHost.toLowerCase();
			StringBuilder valueBuilder = new StringBuilder();
			valueBuilder.append(this.scheme).append("://").append(this.host);
			if(originURI.getPort() == -1) {
				this.port = getDefaultPort(this.scheme);
			}
			else {
				this.port = originURI.getPort();
				valueBuilder.append(":").append(this.port);
			}
			this.value = valueBuilder.toString();
		}
		
		/**
		 * <p>
		 * Creates an origin with the specified scheme, host and port.
		 * </p>
		 *
		 * @param scheme a scheme
		 * @param host   a host
		 * @param port   a port
		 *
		 * @throws IllegalArgumentException if the specified parameters are invalid
		 */
		protected Origin(String scheme, String host, Integer port) throws IllegalArgumentException {
			this.scheme = scheme;
			this.host = host;
			StringBuilder valueBuilder = new StringBuilder();
			valueBuilder.append(this.scheme).append("://").append(this.host);
			if(port == null || port < 0) {
				this.port = getDefaultPort(scheme);
			}
			else {
				this.port = port;
				valueBuilder.append(":").append(this.port);
			}
			this.value = valueBuilder.toString();
		}
		
		/**
		 * <p>
		 * Returns the default port from the specified scheme.
		 * </p>
		 * 
		 * @param scheme a scheme
		 * 
		 * @return the default port corresponding to the scheme or -1
		 */
		public static Integer getDefaultPort(String scheme) {
			switch(scheme.toLowerCase()) {
				case "http": return DEFAULT_HTTP_PORT;
				case "https": return DEFAULT_HTTPS_PORT;
				case "ftp": return DEFAULT_FTP_PORT;
				default:
					return -1;
			}
		}
		
		@Override
		public String toString() {
			return this.value;
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 59 * hash + Objects.hashCode(this.scheme);
			hash = 59 * hash + Objects.hashCode(this.host);
			hash = 59 * hash + this.port;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Origin other = (Origin) obj;
			if (this.port != other.port) {
				return false;
			}
			if (!Objects.equals(this.scheme, other.scheme)) {
				return false;
			}
			return Objects.equals(this.host, other.host);
		}
	}
}
