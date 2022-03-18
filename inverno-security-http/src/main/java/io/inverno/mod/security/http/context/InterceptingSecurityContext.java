package io.inverno.mod.security.http;

import io.inverno.mod.security.SecurityContext;

public interface InterceptingSecurityContext extends DelegatingSecurityContext {

    void setSecurityContext(SecurityContext securityContext);
}
