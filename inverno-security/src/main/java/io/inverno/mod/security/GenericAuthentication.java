package io.inverno.mod.security;

import java.util.Optional;

class GenericAuthentication implements Authentication {

	public static final GenericAuthentication ANONYMOUS = new GenericAuthentication(false);
	
	public static final GenericAuthentication GRANTED = new GenericAuthentication(true);

	public static final GenericAuthentication DENIED = new GenericAuthentication(new AuthenticationException());

	private final boolean authenticated;
	
	private final Optional<SecurityException> cause;

	public GenericAuthentication(boolean authenticated) {
		this.authenticated = authenticated;
		this.cause = Optional.empty();
	}

	public GenericAuthentication(SecurityException cause) {
		this.authenticated = false;
		this.cause = Optional.ofNullable(cause);
	}
	
	@Override
	public boolean isAuthenticated() {
		return this.authenticated;
	}

	@Override
	public Optional<SecurityException> getCause() {
		return this.cause;
	}
}
