package io.inverno.mod.security;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Username/password credentials.
 * </p>
 * 
 * TODO We could include the realm here as well in order to be able to use multiple Credentialsresolver per realm.
 * 
 * @author jkuhn
 */
public class UserCredentials implements Credentials {

	private final String username;

	private final String password;

	public UserCredentials(String username, String password) throws InvalidCredentialsException {
		if(StringUtils.isBlank(username)) {
			throw new InvalidCredentialsException("Username is blank");
		}
		if(StringUtils.isBlank(password)) {
			throw new InvalidCredentialsException("Password is blank");
		}
		this.username = username;
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
}
