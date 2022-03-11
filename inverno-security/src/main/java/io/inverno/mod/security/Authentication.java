package io.inverno.mod.security;

public interface Authentication {

	static Authentication granted() {
		return GenericAuthentication.GRANTED;
	}

	static Authentication denied() {
		return GenericAuthentication.DENIED;
	}

	boolean isAuthenticated();

	// any other id might be interesting here for the identity resolver



}
