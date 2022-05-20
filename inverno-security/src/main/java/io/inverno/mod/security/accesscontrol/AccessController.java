package io.inverno.mod.security;

public interface Authorizations {

    static Authorizations blank() {
        return BlankAuthorizations.INSTANCE;
    }
}
