package io.inverno.mod.security;

import java.util.Objects;
import java.util.Optional;

class GenericSecurityContext implements SecurityContext {

	private final Authentication authentication;
	private Optional<Identity> identity;
	private Optional<Authorizations> authorizations;

	public GenericSecurityContext(Authentication authentication) {
		Objects.requireNonNull(authentication);
		this.authentication = authentication;
	}

	public void setIdentity(Identity identity) {
		this.identity = Optional.ofNullable(identity);
	}

	public void setIdentity(Optional<Identity> identity) {
		this.identity = identity;
	}

	public void setAuthorizations(Authorizations authorizations) {
		this.authorizations = Optional.ofNullable(authorizations);
	}

	public void setAuthorizations(Optional<Authorizations> authorizations) {
		this.authorizations = authorizations;
	}

	@Override
	public boolean isAuthenticated() {
		return this.authentication.isAuthenticated();
	}

	@Override
	public Authentication getAuthentication() {
		return this.authentication;
	}

	@Override
	public Optional<Identity> getIdentity() {
		return this.identity;
	}

	@Override
	public Optional<Authorizations> getAuthorizations() {
		return this.authorizations;
	}
}
