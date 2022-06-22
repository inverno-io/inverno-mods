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
package io.inverno.mod.security.http.form;

import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.security.authentication.InvalidCredentialsException;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A credentials extractor that extracts login credentials provided by a user in a form ({@code application/x-www-form-urlencoded}) submitted in an HTTP {@code POST} request.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class FormCredentialsExtractor implements CredentialsExtractor<LoginCredentials> {

	/**
	 * The default username parameter name.
	 */
	public static final String DEFAULT_PARAMETER_USERNAME = "username";
	
	/**
	 * The default password parameter name.
	 */
	public static final String DEFAULT_PARAMETER_PASSWORD = "password";

	/**
	 * The username parameter name.
	 */
	private final String usernameParameter;
	
	/**
	 * The password parameter name.
	 */
	private final String passwordParameter;
	
	/**
	 * <p>
	 * Creates a form credentials extractor with default username and password parameter names.
	 * </p>
	 */
	public FormCredentialsExtractor() {
		this(DEFAULT_PARAMETER_USERNAME, DEFAULT_PARAMETER_PASSWORD);
	}
	
	/**
	 * <p>
	 * Creates a form credentials extractor with specified username and password parameter names.
	 * </p>
	 * 
	 * @param usernameParameter the username parameter name
	 * @param passwordParameter the password parameter name
	 */
	public FormCredentialsExtractor(String usernameParameter, String passwordParameter) {
		this.usernameParameter = usernameParameter;
		this.passwordParameter = passwordParameter;
	}

	/**
	 * <p>
	 * Returns the username parameter name.
	 * </p>
	 * 
	 * @return the username parameter name
	 */
	public String getUsernameParameter() {
		return usernameParameter;
	}

	/**
	 * <p>
	 * Returns the password parameter name.
	 * </p>
	 * 
	 * @return the password parameter name
	 */
	public String getPasswordParameter() {
		return passwordParameter;
	}
	
	@Override
	public Mono<LoginCredentials> extract(Exchange<?> exchange) throws MalformedCredentialsException {
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
				return LoginCredentials.of(username.getValue(), new RawPassword(password.getValue()));
			});
	}
}
