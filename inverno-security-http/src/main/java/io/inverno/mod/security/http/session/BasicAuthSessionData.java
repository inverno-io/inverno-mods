package io.inverno.mod.security.http.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inverno.mod.security.authentication.Authentication;

/**
 * <p>
 * A basic {@link AuthSessionData} implementation.
 * </p>
 *
 * <p>
 * This can be used directly as session type when authentication is the only component to store in the session.
 * </p>
 *
 * <p>
 * Although possible, it is not recommended to use to implement application session data types as it would leak the authentication in the session data. Defining a specific application session data
 * type and an authentication type extending it and implementing {@link AuthSessionData} should then be preferred.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.13
 *
 * @param <A> the authentication type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BasicAuthSessionData<A extends Authentication> implements AuthSessionData<A> {

	private A authentication;

	@Override
	@JsonProperty("authentication")
	public A getAuthentication() {
		return this.authentication;
	}

	@Override
	public void setAuthentication(A authentication) {
		this.authentication = authentication;
	}
}
