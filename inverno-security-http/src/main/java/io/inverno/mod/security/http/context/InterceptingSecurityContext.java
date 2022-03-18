package io.inverno.mod.security.http.context;

import io.inverno.mod.security.SecurityContext;

public interface InterceptingSecurityContext extends DelegatingSecurityContext {

    void setSecurityContext(SecurityContext securityContext);
}
