package io.inverno.mod.security;

import java.util.Optional;

public interface SecurityContext {

	static SecurityContext of(Authentication authentication) {
		return new GenericSecurityContext(authentication);
	}

	static SecurityContext of(Authentication authentication, Identity identity) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setIdentity(identity);
		return context;
	}

	static SecurityContext of(Authentication authentication, Authorizations authorizations) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setAuthorizations(authorizations);
		return context;
	}

	static SecurityContext of(Authentication authentication, Identity identity, Authorizations authorizations) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setIdentity(identity);
		context.setAuthorizations(authorizations);
		return context;
	}

	static SecurityContext of(Authentication authentication, Optional<Identity> identity, Optional<Authorizations> authorizations) {
		GenericSecurityContext context = new GenericSecurityContext(authentication);
		context.setIdentity(identity);
		context.setAuthorizations(authorizations);
		return context;
	}

	default boolean isAuthenticated() {
		return false;
	}

	default Authentication getAuthentication() { return Authentication.denied(); }

	default Optional<Identity> getIdentity() {
		return Optional.empty();
	}

	default Optional<Authorizations> getAuthorizations() {
		return Optional.empty();
	}
}
