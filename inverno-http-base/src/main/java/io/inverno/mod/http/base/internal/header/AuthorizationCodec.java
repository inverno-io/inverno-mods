/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.http.base.internal.header;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.BeanSocket;
import io.inverno.mod.http.base.header.HeaderBuilder;
import io.inverno.mod.http.base.header.HeaderCodec;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.base.header.ParameterizedHeader;
import io.inverno.mod.http.base.header.ParameterizedHeaderCodec;
import io.netty.buffer.ByteBuf;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 
 * https://datatracker.ietf.org/doc/html/rfc7235#section-4.2
 *
 * @author jkuhn
 */
@Bean(visibility = Bean.Visibility.PRIVATE)
public class AuthorizationCodec implements HeaderCodec<AuthorizationCodec.Authorization> {

	private static final Set<String> SUPPORTED_HEADER_NAMES = Set.of(Headers.NAME_AUTHORIZATION);
	
	private final Set<String> tokenExpectedSchemes;
	
	private final Map<String, TokenHeaderCodec> tokenCodecs;
	
	private final Map<String, ParameterizedHeaderCodec<Authorization, Authorization.ParametersBuilder>> parametersCodecs;
	
	@BeanSocket
	public AuthorizationCodec() {
		this(Set.of(Headers.Authorization.AUTH_SCHEME_BASIC, Headers.Authorization.AUTH_SCHEME_BEARER, Headers.Authorization.AUTH_SCHEME_NEGOTIATE));
	}
	
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
	
	public static final class Authorization extends ParameterizedHeader implements Headers.Authorization {

		private String authScheme;
		
		private String token;
		
		public Authorization(String headerName, String headerValue, String authScheme, Map<String, String> authParameters) {
			super(headerName, headerValue, authScheme, authParameters);
			
			this.authScheme = authScheme;
		}
		
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
		
		public static final class TokenBuilder implements HeaderBuilder<AuthorizationCodec.Authorization, TokenBuilder> {

			private final String authScheme;
			
			private String name;
			
			private String token;

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
		
		public static final class ParametersBuilder extends ParameterizedHeader.AbstractBuilder<Authorization, ParametersBuilder> {

			private final String authScheme;
			
			public ParametersBuilder(String authScheme) {
				this.authScheme = authScheme;
			}
			
			@Override
			public Authorization build() {
				return new Authorization(this.headerName, this.headerValue, this.authScheme, this.parameters);
			}
		}
	}
	
	private static final class TokenHeaderCodec extends GenericHeaderCodec<AuthorizationCodec.Authorization, Authorization.TokenBuilder> {

		public TokenHeaderCodec(String authScheme) {
			super(() -> new Authorization.TokenBuilder(authScheme), Set.of(Headers.NAME_AUTHORIZATION));
		}
	}
}
