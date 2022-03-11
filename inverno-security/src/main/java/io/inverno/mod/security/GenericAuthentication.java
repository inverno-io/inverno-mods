package io.inverno.mod.security;

class GenericAuthentication implements Authentication {

	public static final GenericAuthentication GRANTED = new GenericAuthentication(true);

	public static final GenericAuthentication DENIED = new GenericAuthentication(false);

	private final boolean authenticated;

	public GenericAuthentication(boolean authenticated) {
		this.authenticated = authenticated;
	}

	@Override
	public boolean isAuthenticated() {
		return this.authenticated;
	}
}
