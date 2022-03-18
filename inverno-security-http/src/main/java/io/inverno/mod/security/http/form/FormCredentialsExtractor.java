/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.inverno.mod.security.http.form;

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.InvalidCredentialsException;
import io.inverno.mod.security.UserCredentials;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import reactor.core.publisher.Mono;

/**
 *
 * @author jkuhn
 */
public class FormCredentialsExtractor implements CredentialsExtractor<UserCredentials> {

	public static final String DEFAULT_PARAMETER_USERNAME = "username";
	
	public static final String DEFAULT_PARAMETER_PASSWORD = "password";

	private final String usernameParameter;
	
	private final String passwordParameter;
	
	public FormCredentialsExtractor() {
		this(DEFAULT_PARAMETER_USERNAME, DEFAULT_PARAMETER_PASSWORD);
	}
	
	public FormCredentialsExtractor(String usernameParameter, String passwordParameter) {
		this.usernameParameter = usernameParameter;
		this.passwordParameter = passwordParameter;
	}

	public String getUsernameParameter() {
		return usernameParameter;
	}

	public String getPasswordParameter() {
		return passwordParameter;
	}
	
	@Override
	public Mono<UserCredentials> extract(Exchange<?> exchange) throws MalformedCredentialsException {
		return exchange.request().body().get().urlEncoded().collectMap()
			.map(parameterMap -> {
				Parameter username = parameterMap.get(this.usernameParameter);
				if(username == null) {
					throw new InvalidCredentialsException("Missing username");
				}

				Parameter password = parameterMap.get(this.passwordParameter);
				if(password == null) {
					throw new InvalidCredentialsException("Missing password");
				}
				return new UserCredentials(username.getValue(), password.getValue());
			});
	}
}
