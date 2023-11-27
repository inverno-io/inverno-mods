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
package io.inverno.mod.http.base.internal.header;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.BeanSocket;
import io.inverno.mod.http.base.header.HeaderBuilder;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Authorization HTTP {@link HeaderCodec} implementation.
 * </p>
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class AuthorizationCodec implements HeaderCodec<AuthorizationCodec.Authorization> {

	private static final Set<String> SUPPORTED_HEADER_NAMES = Set.of(Headers.NAME_AUTHORIZATION);
	
	private final Set<String> tokenExpectedSchemes;
	
	private final Map<String, TokenHeaderCodec> tokenCodecs;
	
	private final Map<String, ParameterizedHeaderCodec<Authorization, Authorization.ParametersBuilder>> parametersCodecs;
	
	/**
	 * <p>
	 * Creates an authorization header codec.
	 * </p>
	 */
	@BeanSocket
	public AuthorizationCodec() {
		this(Set.of(Headers.Authorization.AUTH_SCHEME_BASIC, Headers.Authorization.AUTH_SCHEME_BEARER, Headers.Authorization.AUTH_SCHEME_NEGOTIATE));
	}
	
	/**
	 * <p>
	 * Creates an authorization header codec with the specified expected schemes.
	 * </p>
	 * 
	 * @param tokenExpectedSchemes a set of authorization schemes
	 */
	public AuthorizationCodec(Set<String> tokenExpectedSchemes) {
		this.tokenExpectedSchemes = tokenExpectedSchemes;
		
		this.tokenCodecs = new HashMap<>();
		this.parametersCodecs = new HashMap<>();
	}

	@Override
	public Authorization decode(String name, String value) {
		int spaceIndex = value.indexOf(' ');
		
		if(spaceIndex < 0) {
			throw new MalformedHeaderException("Invalid authorization header");
		}
		
		String authScheme = value.substring(0, spaceIndex).toLowerCase();
		if(this.tokenCodecs.containsKey(authScheme) || this.tokenExpectedSchemes.contains(authScheme)) {
			return this.tokenCodecs.computeIfAbsent(authScheme, scheme -> new TokenHeaderCodec(scheme)).decode(name, value.substring(spaceIndex));
		}
		else {
			return this.parametersCodecs.computeIfAbsent(authScheme, scheme -> new ParameterizedHeaderCodec<>(() -> new Authorization.ParametersBuilder(scheme), SUPPORTED_HEADER_NAMES, ' ', ',', ',', true, true, false, false, true, false)).decode(name, value.substring(spaceIndex));
		}
	}

	@Override
	public Authorization decode(String name, ByteBuf buffer, Charset charset) {
		
		// Read until we found space
		// What happen if we hit LF or CR
		int readerIndex = buffer.readerIndex();

		String authScheme = null;
		
		while(buffer.isReadable()) {
			byte nextByte = buffer.readByte();
			
			if(Character.isWhitespace(nextByte)) {
				authScheme = buffer.slice(readerIndex, buffer.readerIndex() - readerIndex + 1).toString(charset).toLowerCase();
				break;
			}
			else if(!HeaderService.isTokenCharacter((char)nextByte)) {
				throw new MalformedHeaderException("Malformed authentication scheme");
			}
		}
		if(authScheme == null) {
			buffer.readerIndex(readerIndex);
			return null;
		}
		
		if(this.tokenCodecs.containsKey(authScheme) || this.tokenExpectedSchemes.contains(authScheme)) {
			return this.tokenCodecs.computeIfAbsent(authScheme, scheme -> new TokenHeaderCodec(scheme)).decode(name, buffer, charset);
		}
		else {
			return this.parametersCodecs.computeIfAbsent(authScheme, scheme -> new ParameterizedHeaderCodec<>(() -> new Authorization.ParametersBuilder(scheme), SUPPORTED_HEADER_NAMES, ' ', ',', ',', true, true, false, false, true, false)).decode(name, buffer, charset);
		}
	}

	@Override
	public String encode(Authorization header) {
		StringBuilder result = new StringBuilder();
		result.append(header.getHeaderName()).append(": ").append(this.encodeValue(header));
		return result.toString();
	}

	@Override
	public String encodeValue(Authorization header) {
		StringBuilder result = new StringBuilder(header.getAuthScheme());
		if(this.tokenExpectedSchemes.contains(header.getAuthScheme())) {
			result.append(" ").append(header.getToken());
		}
		else {
			result.append(this.parametersCodecs.computeIfAbsent(header.getAuthScheme(), scheme -> new ParameterizedHeaderCodec<>(() -> new Authorization.ParametersBuilder(scheme), SUPPORTED_HEADER_NAMES, ' ', ',', ',', true, true, false, false, true, false)).encodeValue(header));
		}
		return result.toString();
	}

	@Override
	public Set<String> getSupportedHeaderNames() {
		return SUPPORTED_HEADER_NAMES;
	}
	
	/**
	 * <p>
	 * {@link Headers.Authorization} header implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 * 
	 * @see ParameterizedHeader
	 */
	public static final class Authorization extends ParameterizedHeader implements Headers.Authorization {

		private final String authScheme;
		
		private String token;
		
		/**
		 * <p>
		 * Creates an authorization header.
		 * </p>
		 * 
		 * @param headerValue    the header value
		 * @param authScheme     the authorization scheme
		 * @param authParameters the authorization header parameters
		 */
		public Authorization(String headerValue, String authScheme, Map<String, String> authParameters) {
			super(Headers.NAME_AUTHORIZATION, headerValue, authScheme, authParameters);
			
			this.authScheme = authScheme;
		}
		
		/**
		 * <p>
		 * Creates an authorization header.
		 * </p>
		 * 
		 * @param headerName  the header name
		 * @param headerValue the header value
		 * @param authScheme  the authorization scheme
		 * @param token       the token
		 */
		public Authorization(String headerName, String headerValue, String authScheme, String token) {
			super(headerName, headerValue, authScheme, Map.of());
			
			this.authScheme = authScheme;
			this.token = token;
		}
		
		@Override
		public String getAuthScheme() {
			return this.authScheme;
		}

		@Override
		public String getToken() {
			return this.token;
		}
		
		/**
		 * <p>
		 * An authorization token builder.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.5
		 */
		public static final class TokenBuilder implements HeaderBuilder<AuthorizationCodec.Authorization, TokenBuilder> {

			private final String authScheme;
			
			private String name;
			
			private String token;

			/**
			 * <p>
			 * Creates a token builder.
			 * </p>
			 * 
			 * @param authScheme the authorization scheme
			 */
			public TokenBuilder(String authScheme) {
				this.authScheme = authScheme;
			}
			
			@Override
			public TokenBuilder headerName(String name) {
				this.name = name;
				return this;
			}

			@Override
			public TokenBuilder headerValue(String value) {
				this.token = value;
				return this;
			}

			@Override
			public Authorization build() {
				return new Authorization(this.name, this.token, this.authScheme, this.token);
			}
		}
		
		/**
		 * <p>
		 * An authorization parameters builder.
		 * </p>
		 * 
		 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
		 * @since 1.5
		 */
		public static final class ParametersBuilder extends ParameterizedHeader.AbstractBuilder<Authorization, ParametersBuilder> {

			private final String authScheme;
			
			/**
			 * <p>
			 * Creates an authorization parameters builder.
			 * </p>
			 * 
			 * @param authScheme the authorization scheme
			 */
			public ParametersBuilder(String authScheme) {
				this.authScheme = authScheme;
			}
			
			@Override
			public Authorization build() {
				return new Authorization(this.headerValue, this.authScheme, this.parameters);
			}
		}
	}
	
	/**
	 * <p>
	 * Token header codec {@link HeaderCodec} implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	private static final class TokenHeaderCodec extends GenericHeaderCodec<AuthorizationCodec.Authorization, Authorization.TokenBuilder> {

		/**
		 * <p>
		 * Creates a token header codec.
		 * </p>
		 * 
		 * @param authScheme the authorization scheme
		 */
		public TokenHeaderCodec(String authScheme) {
			super(() -> new Authorization.TokenBuilder(authScheme), Set.of(Headers.NAME_AUTHORIZATION));
		}
	}
}
