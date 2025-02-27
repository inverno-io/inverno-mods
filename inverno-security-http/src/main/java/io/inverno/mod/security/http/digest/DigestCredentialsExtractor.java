/*
 * Copyright 2022 Jeremy Kuhn
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
package io.inverno.mod.security.http.digest;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials extractor that extracts {@code digest} credentials as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class DigestCredentialsExtractor<A extends ExchangeContext, B extends Exchange<A>> implements CredentialsExtractor<DigestCredentials, A, B> {

	/**
	 * The {@code algorithm} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_ALGORITHM = "algorithm";
	
	/**
	 * The {@code response} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_RESPONSE = "response";
	
	/**
	 * The {@code username} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_USERNAME = "username";
	
	/**
	 * The {@code username*} (extended username) parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_USERNAME_EXT = "username*";
	
	/**
	 * The {@code userhash} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_USERHASH = "userhash";
	
	/**
	 * The {@code realm} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_REALM = "realm";
	
	/**
	 * The {@code uri} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_URI = "uri";
	
	/**
	 * The {@code qop} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_QOP = "qop";
	
	/**
	 * The {@code cnonce} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_CNONCE = "cnonce";
	
	/**
	 * The {@code nc} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_NC = "nc";
	
	/**
	 * The {@code nonce} parameter name as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7616#section-3.4">RFC 7616 Section 3.4</a>.
	 */
	private static final String PARAMETER_NONCE = "nonce";
	
	@Override
	public Mono<DigestCredentials> extract(B exchange) throws MalformedCredentialsException {
		return Mono.fromSupplier(() -> exchange.request().headers()
			.<Headers.Authorization>getHeader(Headers.NAME_AUTHORIZATION)
			.filter(authorizationHeader -> authorizationHeader.getAuthScheme().equals(Headers.Authorization.AUTH_SCHEME_DIGEST))
			.map(authorizationHeader -> from(authorizationHeader, exchange.request().getMethod()))
			.orElse(null)
		);
	}
	
	/**
	 * <p>
	 * Creates digest credentials from the specified authorization header and HTTP method.
	 * </p>
	 * 
	 * @param authorizationHeader the authorization header
	 * @param method the HTTP method
	 * 
	 * @return digest credentials
	 * 
	 * @throws MalformedCredentialsException if digest credentials parameters are incorrect
	 */
	private static DigestCredentials from(Headers.Authorization authorizationHeader, Method method) throws MalformedCredentialsException {
		String algorithm = null;
		String response = null;
		String username = null;
		String realm = null;
		String uri = null;
		String qop = null;
		String cnonce = null;
		String nc = null;
		String nonce = null;
		boolean userhash = false;
		
		for(Map.Entry<String, String> p : authorizationHeader.getParameters().entrySet()) {
			String name = p.getKey();
			String value = p.getValue();
			
			switch(name) {
				case PARAMETER_ALGORITHM: // SHA2-256, SHA-512-256, MD5
					algorithm = value;
					break;
				case PARAMETER_RESPONSE:
					response = value;
					break;
				case PARAMETER_USERNAME: {
					if(username != null) {
						throw new MalformedCredentialsException("You can't specify both username and username*");
					}
					username = StringEscapeUtils.unescapeJava(value);
					break;
				}
				case PARAMETER_USERNAME_EXT: {
					if(username != null) {
						throw new MalformedCredentialsException("You can't specify both username and username*");
					}
					// ext-value = charset "'" [ language ] "'" value-chars
					String[] splitValue = value.split("'");
					if(splitValue.length != 3) {
						throw new MalformedCredentialsException("Invalid extended username*: " + value + ", expected: charset \"'\" [ language ] \"'\" value-chars");
					}
					
					String charset = splitValue[0];
					String language = splitValue[1];
					String pctValue = splitValue[2];
					
					try {
						username = URLDecoder.decode(pctValue, charset);
					} 
					catch (UnsupportedEncodingException ex) {
						throw new MalformedCredentialsException("Invalid charset in extended username*: " + charset);
					}
					break;
				}
				case PARAMETER_REALM:
					realm = StringEscapeUtils.unescapeJava(value);
					break;
				case PARAMETER_URI:
					uri = value;
					break;
				case PARAMETER_QOP:
					// supporting auth-int would require parsing the message body to be able to hash it
					qop = value;
					break;
				case PARAMETER_CNONCE:
					cnonce = value;
					break;
				case PARAMETER_NC:
					nc = value;
					break;
				case PARAMETER_NONCE:
					nonce = value;
					break;
				case PARAMETER_USERHASH:
					userhash = Boolean.parseBoolean(value);
					break;
			}
		}
		return new DigestCredentials(method.name(), uri, realm, username, userhash, algorithm, qop, nc, cnonce, nonce, response);
	}
}
