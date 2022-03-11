package io.inverno.mod.security.http;

import io.inverno.mod.security.Authorizations;
import io.inverno.mod.security.Identity;

import java.util.Optional;

public interface DelegatingSecurityContext extends SecurityContext {

    @Override
    default boolean isAuthenticated() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            boolean auth = ctx.isAuthenticated();
            return ctx.isAuthenticated();
        }
        return false;
    }

    @Override
    default Optional<Identity> getIdentity() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getIdentity();
        }
        return Optional.empty();
    }

    @Override
    default Optional<Authorizations> getAuthorizations() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getAuthorizations();
        }
        return Optional.empty();
    }

    io.inverno.mod.security.SecurityContext getSecurityContext();
}
