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
import io.inverno.mod.security.authentication.UserCredentials;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.MalformedCredentialsException;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
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
