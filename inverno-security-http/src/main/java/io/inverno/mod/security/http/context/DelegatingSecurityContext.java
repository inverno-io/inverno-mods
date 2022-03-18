package io.inverno.mod.security.http.context;

import io.inverno.mod.security.Authentication;
import io.inverno.mod.security.Authorizations;
import io.inverno.mod.security.Identity;

import java.util.Optional;

interface DelegatingSecurityContext extends SecurityContext {

	@Override
	public default Authentication getAuthentication() {
		var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getAuthentication();
        }
		return SecurityContext.super.getAuthentication();
	}

    @Override
    default Optional<Identity> getIdentity() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getIdentity();
        }
        return SecurityContext.super.getIdentity();
    }

    @Override
    default Optional<Authorizations> getAuthorizations() {
        var ctx = this.getSecurityContext();
        if(ctx != null) {
            return ctx.getAuthorizations();
        }
        return SecurityContext.super.getAuthorizations();
    }

    io.inverno.mod.security.SecurityContext getSecurityContext();
}
