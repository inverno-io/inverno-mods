package io.inverno.mod.security;

import java.util.Optional;

public interface Authentication {

	static Authentication anonymous() {
		return GenericAuthentication.ANONYMOUS;
	}
	
	static Authentication granted() {
		return GenericAuthentication.GRANTED;
	}

	static Authentication denied() {
		return GenericAuthentication.DENIED;
	}
	
	static Authentication denied(io.inverno.mod.security.SecurityException cause) {
		return new GenericAuthentication(cause);
	}

	boolean isAuthenticated();

	Optional<io.inverno.mod.security.SecurityException> getCause();
	
	// any other id might be interesting here for the identity resolver



}
