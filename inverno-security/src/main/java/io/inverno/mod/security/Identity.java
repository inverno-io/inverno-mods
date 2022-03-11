package io.inverno.mod.security;

public interface Identity {

	static Identity anonymous() {
		return Anonymous.INSTANCE;
	}
}
