package io.inverno.mod.security;

import org.apache.commons.lang3.StringUtils;

public class UsernamePasswordCredentials implements Credentials {

	private final String user;

	private final String password;

	public UsernamePasswordCredentials(String user, String password) {
		if(StringUtils.isBlank(user)) {
			throw new IllegalArgumentException("User is blank");
		}
		if(password == null) {
			throw new IllegalArgumentException("Password is null");
		}
		this.user = user;
		this.password = password;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}
}
