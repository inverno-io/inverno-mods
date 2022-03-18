/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.digest;

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
 *
 * @author jkuhn
 */
public class DigestCredentialsExtractor implements CredentialsExtractor<DigestCredentials> {

	private static final String PARAMETER_ALGORITHM = "algorithm";
	
	private static final String PARAMETER_RESPONSE = "response";
	
	private static final String PARAMETER_USERNAME = "username";
	
	private static final String PARAMETER_USERNAME_EXT = "username*";
	
	private static final String PARAMETER_USERHASH = "userhash";
	
	private static final String PARAMETER_REALM = "realm";
	
	private static final String PARAMETER_URI = "uri";
	
	private static final String PARAMETER_QOP = "qop";
	
	private static final String PARAMETER_CNONCE = "cnonce";
	
	private static final String PARAMETER_NC = "nc";
	
	private static final String PARAMETER_NONCE = "nonce";
	
	@Override
	public Mono<DigestCredentials> extract(Exchange<?> exchange) throws MalformedCredentialsException {
		return Mono.fromSupplier(() -> exchange.request().headers()
			.<Headers.Authorization>getHeader(Headers.NAME_AUTHORIZATION)
			.filter(authorizationHeader -> {
				System.out.println(authorizationHeader.getHeaderValue());
				
				return authorizationHeader.getAuthScheme().equals(Headers.Authorization.AUTH_SCHEME_DIGEST);
			})
			.map(authorizationHeader -> from(authorizationHeader, exchange.request().getMethod()))
			.orElse(null)
		);
	}
	
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
					userhash = Boolean.valueOf(value);
					break;
			}
		}
		return new DigestCredentials(method.name(), uri, realm, username, userhash, algorithm, qop, nc, cnonce, nonce, response);
	}
}
