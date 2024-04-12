
[cors]: https://en.wikipedia.org/wiki/Cross-origin_resource_sharing
[csrf]: https://en.wikipedia.org/wiki/Cross-site_request_forgery
[same_origin_policy]: https://en.wikipedia.org/wiki/Same-origin_policy
[cors_protocol]: https://fetch.spec.whatwg.org/#http-cors-protocol
[csrf_owasp]: https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
[angular]: https://angular.io

[rfc7515]: https://datatracker.ietf.org/doc/html/rfc7515
[rfc7516]: https://datatracker.ietf.org/doc/html/rfc7516
[rfc7517]: https://datatracker.ietf.org/doc/html/rfc7517
[rfc7518]: https://datatracker.ietf.org/doc/html/rfc7518
[rfc7519]: https://datatracker.ietf.org/doc/html/rfc7519
[rfc7616]: https://datatracker.ietf.org/doc/html/rfc7616
[rfc7617]: https://datatracker.ietf.org/doc/html/rfc7617

# Security HTTP

The Inverno *security-http* module extends the security API and the HTTP server API respectively defined in the *security* module and the *http-server* module in order to secure access to an HTTP server or a Web application.

It defines a complete API for authenticating HTTP requests and exposing the resulting security context in the exchange context which can then be used in exchange interceptors and handlers to secure the application.

Base implementations for various HTTP and Web security standards are also provided. The module currently supports the following features:

- HTTP [basic][rfc7617] authentication scheme.
- HTTP [digest][rfc7616] authentication scheme.
- Form based authentication.
- Token based authentication.
- Cross-origin resource sharing support ([CORS][cors]).
- Protection against Cross-site request forgery attack ([CSRF][csrf]).

In order to use the Inverno *security-http* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.security.http;
    ...
}
```

And also declare that dependency in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-security-http</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-security-http:${VERSION_INVERNO_MODS}'
...
```

Let's quickly see how to secure a simple Web application exposing a single hello world service using basic authentication. The application might initially look like:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;

@Bean
@WebController
public class Main {

    @WebRoute( path = "/hello", method = Method.GET)
    public String hello() {
        return "Hello world!";
    }

    public static void main(String[] args) {
        Application.run(new App_web_security.Builder());
    }
}
```

We can run and test the application which should respond with `Hello world!` when requesting http://localhost:8080/hello:

```java
$ mvn inverno:run
...
[INFO] Running project: io.inverno.example.app_hello_security@1.0.0-SNAPSHOT...
 [═══════════════════════════════════════════════ 100 % ══════════════════════════════════════════════] 
10:19:32.797 [main] INFO  io.inverno.core.v1.Application - Inverno is starting...


     ╔════════════════════════════════════════════════════════════════════════════════════════════╗
     ║                      , ~~ ,                                                                ║
     ║                  , '   /\   ' ,                                                            ║
     ║                 , __   \/   __ ,      _                                                    ║
     ║                ,  \_\_\/\/_/_/  ,    | |  ___  _    _  ___   __  ___   ___                 ║
     ║                ,    _\_\/_/_    ,    | | / _ \\ \  / // _ \ / _|/ _ \ / _ \                ║
     ║                ,   __\_/\_\__   ,    | || | | |\ \/ /|  __/| | | | | | |_| |               ║
     ║                 , /_/ /\/\ \_\ ,     |_||_| |_| \__/  \___||_| |_| |_|\___/                ║
     ║                  ,     /\     ,                                                            ║
     ║                    ,   \/   ,                                  -- ${VERSION_INVERNO_CORE} --                 ║
     ║                      ' -- '                                                                ║
     ╠════════════════════════════════════════════════════════════════════════════════════════════╣
     ║ Java runtime        : OpenJDK Runtime Environment                                          ║
     ║ Java version        : 18+36-2087                                                           ║
     ║ Java home           : /home/jkuhn/Devel/jdk/jdk-18                                         ║
     ║                                                                                            ║
     ║ Application module  : io.inverno.example.app_web_security                                  ║
     ║ Application class   : io.inverno.example.app_web_security.Main                             ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ...                                                                                       ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


10:19:32.801 [main] INFO  io.inverno.example.app_web_security.App_web_security - Starting Module io.inverno.example.app_web_security...
10:19:32.801 [main] INFO  io.inverno.mod.boot.Boot - Starting Module io.inverno.mod.boot...
10:19:33.002 [main] INFO  io.inverno.mod.boot.Boot - Module io.inverno.mod.boot started in 200ms
10:19:33.002 [main] INFO  io.inverno.mod.web.server.Web - Starting Module io.inverno.mod.web.server...
10:19:33.002 [main] INFO  io.inverno.mod.http.server.Server - Starting Module io.inverno.mod.http.server...
10:19:33.002 [main] INFO  io.inverno.mod.http.base.Base - Starting Module io.inverno.mod.http.base...
10:19:33.009 [main] INFO  io.inverno.mod.http.base.Base - Module io.inverno.mod.http.base started in 6ms
10:19:33.110 [main] INFO  io.inverno.mod.http.server.internal.HttpServer - HTTP Server (nio) listening on http://0.0.0.0:8080
10:19:33.111 [main] INFO  io.inverno.mod.http.server.Server - Module io.inverno.mod.http.server started in 109ms
10:19:33.111 [main] INFO  io.inverno.mod.web.server.Web - Module io.inverno.mod.web.server started in 109ms
10:19:33.112 [main] INFO  io.inverno.example.app_web_security.App_web_security - Module io.inverno.example.app_web_security started in 312ms
10:19:33.115 [main] INFO  io.inverno.core.v1.Application - Application io.inverno.example.app_web_security started in 375ms
```

```plaintext
$ curl -i http://localhost:8080/hello
HTTP/1.1 200 OK
content-length: 12

Hello world!
```

From there, let's say we want to protect the access to all routes requiring [HTTP basic authentication][rfc7617]. In order to do this we must authenticate basic credentials provided within requests and reject requests (401) on denied authentication in which case a basic authentication challenge must also be sent to the client.

A Web configurer must be created to define a **security interceptor** that will authenticate requests and use a `BasicAuthenticationErrorInterceptor` to intercept unauthorized (401) errors and return the basic authentication challenge to the client.

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.basic.BasicAuthenticationErrorInterceptor;
import io.inverno.mod.security.http.basic.BasicCredentialsExtractor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRouterConfigurer;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import java.util.List;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .interceptors(List.of(SecurityInterceptor.of(                            // 1
                        new BasicCredentialsExtractor(),                                 // 2
                        new UserAuthenticator<>(                                         // 3
                            InMemoryUserRepository
                                .of(List.of(
                                    User.of("jsmith")
                                        .password(new RawPassword("password"))
                                        .build()
                                ))
                                .build(),
                            new LoginCredentialsMatcher<>()
                        )
                    ),
                    AccessControlInterceptor.authenticated()                             // 4
                ));
    }

    @Override
    public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
        errorRouter
            .intercept()
                .error(UnauthorizedException.class)
                .interceptor(new BasicAuthenticationErrorInterceptor<>("inverno-basic")) // 5
            // We must apply interceptors to intercept white labels error routes
            .applyInterceptors();                                                        // 6
    }
}
```

The Web configurer implements `WebInterceptorsConfigurer` in order to configure route interceptors and `ErrorWebRouterConfigurer` in order to configure error route interceptors and apply them to all error routes (including default ones). It declares the `InterceptingSecurityContext` exchange context type which is required by the `SecurityInterceptor` to set the security context. Interceptors are defined to intercept all routes.

In above code, there are several things that deserve further explanation:

1. The `SecurityInterceptor` is the Web counterpart of the `SecurityManager`, it is used to authenticate credentials provided in HTTP requests and create the security context which is then exposed in the exchange context and accessible to exchange interceptors and handlers.
2. In addition to the authenticator and optional identity and access controller resolvers, it requires a credentials extractor used to extract `Credentials` from the request. The `BasicCredentialsExtractor` basically extracts `LoginCredentials` (username/password) from the `authorization` HTTP header of the request.
3. The security interceptor can then use any authenticator that is able to authenticate login credentials such as the `UserAuthenticator`.
4. An access control interceptor is added next in order to limit the access to authenticated users. Just like the security manager, the security interceptor authenticates credentials and creates the security context. But that does not mean authentication was successful, the resulting security context can be anonymous, denied or authenticated.
5. The `BasicAuthenticationErrorInterceptor` intercepts unauthorized (401) errors and set the basic authentication scheme challenge in the `www-authenticate` HTTP header of the response with the `inverno-basic` realm.
6. The Web server provides white labels error routes by default which must be explicitly intercepted since they have been created before on an unintercepted router.

> Having to explicitly apply interceptors on default routes can be a source of errors and misunderstanding but there is unfortunately no other way if we want to make them overridable. A systematic and safe approach to this issue would be to always override default error routes.

We should now receive an unauthorized (401) error with a basic authentication challenge when requesting http://localhost:8080/hello (or any other endpoint) without credentials:

```plaintext
$ curl -i http://127.0.0.1:8080/hello
HTTP/1.1 401 Unauthorized
www-authenticate: basic realm="inverno-basic"
content-length: 0

```

In order to access the service, we must provide valid credentials in the `authorization` HTTP header. Basic authentication scheme specifies that credentials are obtained by encoding in base64 the concatenation of the username, a single colon and the password. In our example credentials for user `jsmith` are then `anNtaXRoOnBhc3N3b3Jk`:

```plaintext
$ curl -i -H 'authorization: basic anNtaXRoOnBhc3N3b3Jk' http://127.0.0.1:8080/hello
HTTP/1.1 200 OK
content-length: 12

Hello world!
```

We can change the `/hello` route handler to respond with a personalized message. This requires to resolve the identity of the user and use it in the handler.

We use a user repository which can provide user's identity, a `UserIdentityResolver` can then be used in the security interceptor to resolve it and make it available in the security context:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.basic.BasicAuthenticationErrorInterceptor;
import io.inverno.mod.security.http.basic.BasicCredentialsExtractor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRouterConfigurer;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import java.util.List;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<PersonIdentity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<PersonIdentity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .interceptors(List.of(SecurityInterceptor.of(
                        new BasicCredentialsExtractor(),
                        new UserAuthenticator<>(
                            InMemoryUserRepository
                                .of(List.of(
                                    User.of("jsmith")
                                        .password(new RawPassword("password"))
                                        .identity(new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"))
                                        .build()
                                ))
                                .build(),
                            new LoginCredentialsMatcher<>()
                        ),
                        new UserIdentityResolver<>()
                    ),
                    AccessControlInterceptor.authenticated()
                ));
    }

    @Override
    public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
        errorRouter
            .intercept()
                .error(UnauthorizedException.class)
                .interceptor(new BasicAuthenticationErrorInterceptor<>("inverno-basic"))
            // We must apply interceptors to intercept white labels error routes
            .applyInterceptors();
    }
}
```

The `PersonIdentity` type is now declared in the `InterceptingSecurityContext` exchange context type and the identity is resolved from user's identity.

We can now inject the exchange security context in the route handler and get the identity to provide the personalized message:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;

@Bean
@WebController
public class Main {

    @WebRoute( path = "/hello", method = Method.GET)
    public String hello(SecurityContext<? extends PersonIdentity, ? extends AccessController> securityContext) {
        return "Hello " + securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are") + "!";
    }
}
```

> Here we injected `io.inverno.mod.security.http.context.SecurityContext` which extends both `io.inverno.mod.security.context.SecurityContext` and `ExchangeContext`. This interface is not mutable and exposes the exact same components as the regular security context, it should be used in application's route interceptors and handlers. On the other hand, the `InterceptingSecurityContext` is mutable and should only be used by security related interceptors and the `SecurityInterceptor` in particular.

User `jsmith` should now receive a personalized message when requesting http://localhost:8080/hello:

```plaintext
$ curl -i -H 'authorization: basic anNtaXRoOnBhc3N3b3Jk' http://127.0.0.1:8080/hello
HTTP/1.1 200 OK
content-length: 11

Hello John!
```

Let's create another endpoint for VIP users responding with an extra polite message. VIP users can be placed into the `vip` group and a `RoleBasedAccessContoller` can be resolved using a `GroupsRoleBasedAccessControllerResolver`.

Let's start by creating the `/vip/hello` route:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;
import reactor.core.publisher.Mono;

@Bean
@WebController
public class Main {

    ...
    @WebRoute( path = "/vip/hello", method = Method.GET)
    public Mono<String> hello_vip(SecurityContext<? extends PersonIdentity, ? extends RoleBasedAccessController> securityContext) {
        return securityContext.getAccessController()
            .orElseThrow(() -> new ForbiddenException())
            .hasRole("vip")
            .map(isVip -> {
                if(!isVip) {
                    throw new ForbiddenException();
                }
                return "Hello my dear friend " + securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are") + "!";
            });
    }
    ...
}
```

> You may have notice that we did not have to change the `/hello` route definition which can still declare `SecurityContext<? extends PersonIdentity, ? extends AccessController>` since it is assignable from the actual context type `SecurityContext<PersonIdentity, RoleBasedAccessController>` declared in the security configurer. Note that a compilation error would have been raised to report inconsistent exchange context types if we had not used upper bound wildcards.

We can now change the Web configurer to resolve the role-based access controller using a `GroupsRoleBasedAccessControllerResolver`.

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.basic.BasicAuthenticationErrorInterceptor;
import io.inverno.mod.security.http.basic.BasicCredentialsExtractor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRouterConfigurer;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import java.util.List;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<PersonIdentity, RoleBasedAccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<PersonIdentity, RoleBasedAccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .interceptors(List.of(SecurityInterceptor.of(
                        new BasicCredentialsExtractor(),
                        new UserAuthenticator<>(
                            InMemoryUserRepository
                                .of(List.of(
                                    User.of("jsmith")
                                        .password(new RawPassword("password"))
                                        .identity(new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"))
                                        .groups("vip")
                                        .build(),
                                    User.of("adoe")
                                        .password(new RawPassword("password"))
                                        .identity(new PersonIdentity("adoe", "Alice", "Doe", "adoe@inverno.io"))
                                        .build()
                                ))
                                .build(),
                            new LoginCredentialsMatcher<>()
                        ),
                        new UserIdentityResolver<>(),
                        new GroupsRoleBasedAccessControllerResolver()
                    ),
                    AccessControlInterceptor.authenticated()
                ));
    }
    ...
}
```

The `RoleBasedAccessController` type is now declared in the `InterceptingSecurityContext` exchange context type, we also added another normal user and a role-based access controller based on users' groups is now resolved.

Accessing route `/hello` and `/vip/hello` with different users should provide the following results:

```plaintext
$ curl -i -H 'authorization: basic anNtaXRoOnBhc3N3b3Jk' http://127.0.0.1:8080/hello
HTTP/1.1 200 OK
content-length: 11

Hello John!

$ curl -i -H 'authorization: basic anNtaXRoOnBhc3N3b3Jk' http://127.0.0.1:8080/vip/hello
HTTP/1.1 200 OK
content-length: 26

Hello my dear friend John!

$ curl -i -H 'authorization: basic YWRvZTpwYXNzd29yZA==' http://127.0.0.1:8080/hello
HTTP/1.1 200 OK
content-length: 12

Hello Alice!

$ curl -i -H 'authorization: basic YWRvZTpwYXNzd29yZA==' http://127.0.0.1:8080/vip/hello
HTTP/1.1 403 Forbidden
content-length: 0

```

Here we have decided to control access inside the `/vip/hello` route handler but we could have also globally restrict access to `/vip/**` routes to VIP users using an `AccessControlInterceptor` in the security configurer:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.basic.BasicAuthenticationErrorInterceptor;
import io.inverno.mod.security.http.basic.BasicCredentialsExtractor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRouterConfigurer;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import java.util.List;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<PersonIdentity, RoleBasedAccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<PersonIdentity, RoleBasedAccessController>, ?> interceptors) {
        interceptors
            ...
            .intercept()
                .path("/vip/**")
                .interceptor(AccessControlInterceptor.verify(securityContext -> securityContext.getAccessController()
                    .orElseThrow(() -> new ForbiddenException())
                    .hasRole("vip")
                ));
    }
    ...
}
```

The `/vip/hello` route handler can then be simplified while still being only accessible by VIP users:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;

@Bean
@WebController
public class Main {

    ...
    @WebRoute( path = "/vip/hello", method = Method.GET)
    public String hello_vip(SecurityContext<? extends PersonIdentity, ? extends RoleBasedAccessController> securityContext) {
        return "Hello my dear friend " + securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are") + "!";
    }
    ...
}
```

> If you followed the *security* module documentation, and you should have, you might have noticed how the `SecurityInterceptor` is similar to the `SecurityManager`, they basically have the same role which is to authenticate a request and provide a security context which, although we had to create an exchange security context, is still the central component used to secure the application. As a result, securing a Web application is no different than securing a regular application and it should therefore be easy to create secured components and libraries that can be integrated in both.

## Security Interceptor

The `SecurityInterceptor` is the main entry point for securing an HTTP server or a Web application, it is the counterpart of the `SecurityManager` for regular applications. Its role is to extract `Credentials` from HTTP requests and just like the `SecurityManager`, to authenticate them and possibly resolve an `Identity` and/or an `AccessController`. It then sets the resulting security context in the exchange. Exchange interceptors and handlers can then access the security context anytime for securing services and resources.

A `SecurityInterceptor` instance is created by composing a `CredentialsExtractor` used to extract `Credentials` from the request in addition to the `Authenticator` and optional `IdentityResolver` and `AccessControllerResolver`. It should be used to intercept request targeting services or resources that must be secured or require identity information.

Although it is completely possible to use it on the global exchange handler in the HTTP server controller, we will focus on securing Web routes in a Web server in the rest of this documentation as it covers more interesting use cases.

As for the `SecurityManager`, the `SecurityInterceptor` basically chains the extraction of credentials, the authentication, the identity resolution and the access controller resolution and sets the resulting `SecurityContext` in the exchange context declared as a `InterceptingSecurityContext`.

A `SecurityInterceptor` is created as follows:

```java
CredentialsExtractor<Credentials> credentialsExtractor = ...
Authenticator<Credentials, Authentication> authenticator = ...
IdentityResolver<Authentication, Identity> identityResolver = ...
AccessControllerResolver<Authentication, AccessController> accessControllerResolver = ...

SecurityInterceptor<Credentials, Identity, AccessController, InterceptingSecurityContext<Identity, AccessController>, Exchange<InterceptingSecurityContext<Identity, AccessController>>> securityInterceptor = SecurityInterceptor.of(credentialsExtractor, authenticator, identityResolver, accessControllerResolver);
```

It can be applied to Web routes just like any other exchange interceptor by defining a Web configurer implementing `WebInterceptorsConfigurer` or `WebRouterConfigurer`. The following example shows how to secure access by applying the security interceptor to all `/vip/**` routes:

```java
package io.inverno.example.app_web_security;

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;

public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<PersonIdentity, AccessController>, ?> interceptors) {
        CredentialsExtractor<Credentials> credentialsExtractor = ...
        Authenticator<Credentials, Authentication> authenticator = ...
        IdentityResolver<Authentication, Identity> identityResolver = ...
        AccessControllerResolver<Authentication, AccessController> accessControllerResolver = ...

        interceptors
            .intercept()
                .path("/vip/**")
                .interceptor(SecurityInterceptor.of(credentialsExtractor, authenticator, identityResolver, accessControllerResolver));
    }
}
```

By combining various implementations of `CredentialsExtractor`, `Authenticator`, `IdentityResolver` and `AccessControllerResolver`, it is possible to implements any kind of HTTP authentication methods (e.g. basic, digest, token...). It is still good to remember that the role of the security interceptor is to authenticate credentials and create a resulting security context which can be anonymous, denied or authenticated, actual access control must be done in subsequent interceptors or within the route handler.

> Since the security interceptor is a regular exchange interceptor, it is possible to define various instances applied to different routes. We can for instance imagine using different security interceptors implementing different authentication methods or targeting different user repositories based on the path, the language... basically any routing criteria exposed by the `WebRouteManager`.

### CredentialsExtractor

A credentials extractor is used in a security interceptor to extract `Credentials` from an HTTP request. The `CredentialsExtractor` interface is a functional interface defining method `extract()`. The following example shows a simple inline implementation that extract `LoginCredentials` from HTTP headers returning no credentials if either username or password is missing:

```java
CredentialsExtractor<LoginCredentials> credentialsExtractor = exchange -> {
    return Mono.fromSupplier(() -> exchange.request().headers().get("username")
        .flatMap(username -> exchange.request().headers().get("password")
            .map(RawPassword::new)
            .map(password -> LoginCredentials.of(username, password))
        )
        .orElse(null)
    );
};
```

When no credentials are returned, the security interceptor creates an anonymous security context.

Mutliple credentials extractor can be chained in order to extract credentials from different location within the request by order of prefrerence. For instance, we can create a credentials extractor to extract `TokenCredentials` from an HTTP header, a cookie, or a query parameter in that order.

```java
CredentialsExtractor<TokenCredentials> headerTokenCredentialsExtractor = exchange -> {
    return Mono.fromSupplier(() -> exchange.request().headers().get("token").map(TokenCredentials::new).orElse(null));
};

CredentialsExtractor<TokenCredentials> cookieTokenCredentialsExtractor = exchange -> {
    return Mono.fromSupplier(() -> exchange.request().headers().cookies().get("token").map(cookie -> new TokenCredentials(cookie.asString())).orElse(null));
};

CredentialsExtractor<TokenCredentials> queryTokenCredentialsExtractor = exchange -> {
    return Mono.fromSupplier(() -> exchange.request().queryParameters().get("token").map(parameter -> new TokenCredentials(parameter.asString())).orElse(null));
};

CredentialsExtractor<TokenCredentials> credentialsExtractor = headerTokenCredentialsExtractor
        .or(cookieTokenCredentialsExtractor)
        .or(queryTokenCredentialsExtractor);
```

### SecurityContext vs HTTP SecurityContext vs InterceptingSecurityContext

The *security-http* module provides `io.inverno.mod.security.http.context.SecurityContext` which extends both `ExchangeContext` and `io.inverno.mod.security.context.SecurityContext` defined in the *security* module. Although the security context semantic remains unchanged, this was necessary to be able to expose it as an exchange context. The `io.inverno.mod.security.http.context.SecurityContext` can be seen as a security exchange context, it must be used to secure HTTP endpoints as it can be accessed from the `Exchange` and injected in Web route handlers.

It also provides the `io.inverno.mod.security.http.context.InterceptingSecurityContext` which extends `io.inverno.mod.security.http.context.SecurityContext` and exposes a single `setSecurityContext()` method. This is a mutable version of the `io.inverno.mod.security.http.context.SecurityContext` which enables security related interceptors or handlers, such as the `SecurityInterceptor`, to set the `io.inverno.mod.security.context.SecurityContext` in the security exchange context.

> In the end, every `ExchangeContext` types should be implemented in the generated global `ExchangeContext` type which will basically implements both `io.inverno.mod.security.http.context.SecurityContext` and `io.inverno.mod.security.http.context.InterceptingSecurityContext`. However making sure `io.inverno.mod.security.http.context.SecurityContext` is used in applicative interceptors and handlers and only allow the `io.inverno.mod.security.http.context.InterceptingSecurityContext` in specific trusted security interceptors and handlers is a good way to control and protect the security context against untrustful modifications.

## Access Control Interceptor

As we just saw, the role of the security interceptor is to authenticate credentials and provides a security context but it does not actually control access. The security context can be anonymous, denied or authenticated, actual access control must then be done in a subsequent interceptors and/or directly in the route handler. An `AccessControlInterceptor` can be applied on secured routes to control access globally.

In the following example, `AccessControlInterceptor.authenticated()` is used to create an interceptor that restricts access to authenticated users.

```java
package io.inverno.example.app_web_security;

import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import java.util.List;

public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<PersonIdentity, AccessController>, ?> interceptors) {
        CredentialsExtractor<Credentials> credentialsExtractor = ...
        Authenticator<Credentials, Authentication> authenticator = ...
        IdentityResolver<Authentication, Identity> identityResolver = ...
        AccessControllerResolver<Authentication, AccessController> accessControllerResolver = ...

        interceptors
            .intercept()
                .path("/vip/**")
                .interceptors(List.of(
                    SecurityInterceptor.of(credentialsExtractor, authenticator, identityResolver, accessControllerResolver),
                    AccessControlInterceptor.authenticated()
                ));
    }
}
```

We can use `AccessControlInterceptor.anonymous()` to restrict access to anonymous users or we can also provide custom access control using `AccessControlInterceptor.verify()` as follows:

```java
package io.inverno.example.app_web_security;

import io.inverno.mod.http.base.ForbiddenException;
import io.inverno.mod.security.accesscontrol.AccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.Authentication;
import io.inverno.mod.security.authentication.Authenticator;
import io.inverno.mod.security.authentication.Credentials;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.CredentialsExtractor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.identity.IdentityResolver;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import java.util.List;

public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, RoleBasedAccessController>> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, RoleBasedAccessController>, ?> interceptors) {
        CredentialsExtractor<Credentials> credentialsExtractor = null;
        Authenticator<Credentials, Authentication> authenticator = null;
        IdentityResolver<Authentication, Identity> identityResolver = null;
        AccessControllerResolver<Authentication, RoleBasedAccessController> accessControllerResolver = null;

        interceptors
            .intercept()
                .path("/vip/**")
                .interceptors(List.of(
                    SecurityInterceptor.of(credentialsExtractor, authenticator, identityResolver, accessControllerResolver),
                    AccessControlInterceptor.verify(securityContext -> securityContext.getAccessController()
                        .orElseThrow(() -> new ForbiddenException())
                        .hasRole("vip")
                    )
                ));
    }
}
```

## HTTP authentication

By combining `CredentialsExtractor` with `Authenticator`, it is possible to implement various HTTP authentication methods. The security HTTP API provides credentials extractors as well as exchange interceptors and handlers that facilitate the configuration of standard HTTP authentication methods in Web applications.

### HTTP Basic authentication

The [basic HTTP authentication scheme][rfc7617] is, as its name suggests, a basic authentication method on top of HTTP in which credentials are provided in the `authorization` HTTP header in the form `basic Base64(username ":" password)`. Basic authentication can be requested to the client (e.g. a Web browser) by specifying a `www-authenticate` HTTP header in an unauthorized (401) response sent when a protected resource is requested without credentials or with invalid credentials.

A security context implementing HTTP basic authentication is obtained by combining the `BasicCredentialsExtractor` which extracts `LoginCredentials` with a compatible `Authenticator` implementation. The following example uses a basic `PrincipalAuthenticator` with an in-memory login credentials resolver in order to secure `/basic/**` routes:

```java
package io.inverno.example.app_web_security;

...
import io.inverno.mod.security.http.basic.BasicCredentialsExtractor;
...

...
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .path("/basic/**")
                .interceptor(SecurityInterceptor.of(
                    new BasicCredentialsExtractor(),
                    new PrincipalAuthenticator<>(
                        new InMemoryLoginCredentialsResolver(List.of(
                            LoginCredentials.of("john", new MessageDigestPassword.Encoder().encode("password")),
                            LoginCredentials.of("alice", new MessageDigestPassword.Encoder().encode("password")),
                            LoginCredentials.of("bob", new MessageDigestPassword.Encoder().encode("password"))
                        )),
                        new LoginCredentialsMatcher<LoginCredentials, LoginCredentials>()
                    )
                ));
    }
}
```

In order to fully implement HTTP basic authentication scheme as defined by [RFC 7617][rfc7617], we also need to send a basic authentication challenge on unauthorized (401) errors. This can be done by intercepting `UnauthorizedException` on secured routes using a `BasicAuthenticationErrorInterceptor`:

```java
package io.inverno.example.app_web_security;

...
import io.inverno.mod.security.http.basic.BasicAuthenticationErrorInterceptor;
...

...
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    ...
    @Override
    public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
        errorRouter
            .intercept()
                .error(UnauthorizedException.class)
                .path("/basic/**")
                .interceptor(new BasicAuthenticationErrorInterceptor<>("inverno-basic"))
                 // We must apply interceptors to intercept white labels error routes which are already defined
                .applyInterceptors();
    }
}
```

Using above configuration, unauthorized (401) error response corresponding to unauthenticated access will be augmented with a `www-authenticate` HTTP header requesting for basic authentication in the `inverno-basic` realm. In practice, this results in a login prompt being displayed in a Web browser.

The following shows an unauthorized (401) HTTP response with a basic authentication challenge generated by the `BasicAuthenticationErrorInterceptor`:

```plaintext
$ curl -i http://127.0.0.1:8080/basic/hello
HTTP/1.1 401 Unauthorized
www-authenticate: basic realm="inverno-basic"
content-length: 0

```

> Sending a basic authentication challenge to the client has actually nothing to do with authentication, it simply gives indication to the client on what credentials are expected by the server to access a protected resource. If you don't need to strictly abide to the specification or if your HTTP resources will only be consumed by backend applications you might choose not to use the `BasicAuthenticationErrorInterceptor`.

### HTTP Digest authentication

The [HTTP digest access authentication][rfc7616] is a more secured HTTP authentication method in which login credentials (username/password) are sent digested by the client using a nonce previously sent by the server in a `www-authenticate` HTTP header. As for basic authentication, digest credentials are provided in the `authorization` HTTP header. The nonce is built using a secret, the current timestamp and a validity period which allows to expire digest credentials.

A security context implementing HTTP digest authentication is obtained by combining the `DigestCredentialsExtractor` which extracts `DigestCredentials` with a compatible `Authenticator` implementation. The `DigestCredentialsMatcher` can be used within a `PrincipalAuthenticator` or a `UserAuthenticator` to match digest credentials against trusted login credentials (digest credentials basically represent digested login credentials). The following example uses a `UserAuthenticator` with an in-memory user repository and a `DigestCredentialsMatcher` in order to secure `/digest/**` routes:

```java
package io.inverno.example.app_web_security;

...
import io.inverno.mod.security.http.digest.DigestCredentialsExtractor;
import io.inverno.mod.security.http.digest.DigestCredentialsMatcher;
...

...
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .path("/digest/**")
                .interceptor(SecurityInterceptor.of(
                    new DigestCredentialsExtractor(),
                    new UserAuthenticator<>(
                        InMemoryUserRepository
                            .of(List.of(
                                User.of("jsmith")
                                    .password(new RawPassword("password"))
                                    .build(),
                                User.of("adoe")
                                    .password(new RawPassword("password"))
                                    .build()
                            ))
                            .build(),
                        new DigestCredentialsMatcher<>("secret")
                    )
                ));
    }
}
```

As previously mentionned, digest credentials expire at a fixed datetime specified in the nonce, this is checked in the `DigestCredentialsMatcher` which fails authentication with a `ExpiredNonceException` when this happens.

The HTTP digest access authentication is based on a challenge-response mechanism as a result a digest authentication challenge must be generated server-side on an unauthorized access or expired nonce errors and sent to the client prior to authentication. This is done using a `DigestAuthenticationErrorInterceptor` on secured routes to intercept `UnauthorizedException` errors:

```java
package io.inverno.example.app_web_security;

...
import io.inverno.mod.security.http.basic.BasicAuthenticationErrorInterceptor;
...

...
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    ...
    @Override
    public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
        errorRouter
            .intercept()
                .error(UnauthorizedException.class)
                .path("/digest/**")
                .interceptor(new DigestAuthenticationErrorInterceptor<>("inverno-digest", "secret"))
                 // We must apply interceptors to intercept white labels error routes which are already defined
                .applyInterceptors();
    }
}
```

Using above configuration, an unauthorized (401) error response corresponding to unauthenticated access will be augmented with a `www-authenticate` HTTP header containing the digest authentication challenge requesting for digest credentials in the `inverno-digest` realm. The interceptor basically generates a nonce using the specified secret, the nonce validity period (defaults to 300 seconds) and the message digest algorithm (defaults to `MD5`). In practice, this results in a login prompt being displayed in a Web browser.

The following shows an unauthorized (401) HTTP response with a digest authentication challenge generated by the `DigestAuthenticationErrorInterceptor`:

```plaintext
$ curl -i http://localhost:8080/digest/hello
HTTP/1.1 401 Unauthorized
www-authenticate: digest realm="inverno-digest",qop="auth",nonce="ODg2OTk2MzI3NjcwMzAwOjAyZmIxNWY0ZTAyMTA0NzMzMzdjYmU4YmY4NWRhOGI4",algorithm=MD5
content-length: 0

```

### Token based authentication

Token based authentication is a simple authentiation method based on the authentication of a token which was usually previously issued to the client by the server.

A token must be ideally difficult to forge and easy to validate which is why cryptographic methods are often used to generate secured token but solution based on random numbers stored in a trusted data store (like a session store) can also be considered.

A security context implementing token based authentication can be obtained by combining a `TokenCredentials` extractor with a compatible `Authenticator` implementation. The following example uses a `CookieTokenCredentialsExtractor` to extract `TokenCredentials` from a specific cookie and a simplistic highly unsecure authenticator which validates tokens against an hardcoded list of authorized tokens:

```java
package io.inverno.example.app_web_security;

...
import io.inverno.mod.security.http.token.CookieTokenCredentialsExtractor;
...

...
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .path("/token/**")
                .interceptor(SecurityInterceptor.of(
                    new CookieTokenCredentialsExtractor(),
                    credentials -> Mono.fromSupplier(() -> {
                        if(Set.of("token1", "token2", "token3").contains(credentials.getToken())) {
                            return Authentication.granted();
                        }
                        return Authentication.denied();
                    })
                ));
    }
}
```

> As already mentionned, a proper token must be ideally hard to forge and using cryptographic solution such as [JWS][rfc7515], [JWE][rfc7516] or [JWT][rfc7519] are highly recommended.

### Form based login

Form based login is meant to be used to log physical users in an application using a login page in a Web browser. This is slightly more complex than a basic authentication as it usually involves the use of multiple authentication methods.

The login flow is started when a user tries to access a protected resource (1) in a Web browser without credentials or with invalid credentials, an unauthorized (401) is then raised and the user is redirected to the login page prompting for credentials (2), usually a username/password pair. The user then fills the input fields and submits the form (3) to the login action whose role is to authenticate the credentials and generate temporary credentials, usually token credentials, sent back to the client, usually in a cookie, in a found (302) response (4). The client is then redirected to the page initially requested which is now accessed with valid token credentials (5). The user can then access the protected page (6).

<img class="shadow mb-4" src="doc/img/form_based_flow.png" alt="Form based login flow"/>

Form based login then requires two authentication methods: one to authenticate credentials provided by the user to a login action which should generate the actual credentials that are authenticated by the second method to grant access to protected resources.

Let's start by configuring Web routes to the login page and the login action.

The API provides the `FormLoginPageHandler` which renders a white label login page containing the login form using an Inverno reactive template. The actual login action URI can be configured when creating the handler (defaults to `/login`). The login form sends three parameters: `username`, `password` and `redirect_uri`.

```java
package io.inverno.example.app_web_security;

...

public class SecurityConfigurer implements WebRoutesConfigurer<SecurityContext<Identity, AccessController>> {

    @Override
    public void configure(WebRoutable<SecurityContext<Identity, AccessController>, ?> routes) {
        routes
            .route()
                .method(Method.GET)
                .path("/login")
                .produces(MediaTypes.TEXT_HTML)
                .handler(new FormLoginPageHandler<>("/login"));
    }
}
```

> The login page is not different than a standard route and a custom login page can be easily used instead of the white label login page.

The `LoginActionHandler` is a route handler that must be targeted by the login form to authentiate the user credentials. It relies on a `CredentialsExtractor` to extract credentials from the login request and a compatible `Authenticator` to authenticate them. Finally, it uses a `LoginSuccessHandler` and a `LoginFailureHandler` to determine what to do in case of successful or failed authentication. If no `LoginSuccessHandler` is defined, a blank response is returned on successful authentication. If no `LoginFailureHandler` is defined, a unauthorized (401) error is returned on failed authentication.

In the following example, we decided to generate a [JWS][rfc7515] on successful authentication which requires to inject a `JWKService` to generate a JSON Web Key and a `JWSService` to create JWS tokens.

> Please refer to the [*security-jose* module documentation](#json-object-signing-and-encryption) to learn how to create and validate [JWS][rfc7515], [JWE][rfc7516] or [JWT][rfc7519].

```java
package io.inverno.example.app_web_security;

...

public class SecurityConfigurer implements WebRoutesConfigurer<SecurityContext<Identity, AccessController>> {

    private final Mono<? extends OCTJWK> jwsKey;
    private final JWSService jwsService;

    public SecurityConfigurer(JWKService jwkService, JWSService jwsService) {
        this.jwsKey = jwkService.oct().generator()
            .algorithm(OCTAlgorithm.HS256.getAlgorithm())
            .generate()
            .cache();
        this.jwsService = jwsService;
    }

    @Override
    public void configure(WebRoutable<SecurityContext<Identity, AccessController>, ?> routes) {
        routes
            ...
            .route()
                .method(Method.POST)
                .path("/login")
                .handler(new LoginActionHandler<>(                                                                 // 1
                    new FormCredentialsExtractor(),                                                                // 2
                    new PrincipalAuthenticator<>(                                                                  // 3
                        new InMemoryLoginCredentialsResolver(List.of(
                            LoginCredentials.of("john", new MessageDigestPassword.Encoder().encode("password")),
                            LoginCredentials.of("alice", new MessageDigestPassword.Encoder().encode("password")),
                            LoginCredentials.of("bob", new MessageDigestPassword.Encoder().encode("password"))
                        )),
                        new LoginCredentialsMatcher<LoginCredentials, LoginCredentials>()
                    )
                    .failOnDenied()                                                                                // 4
                    .flatMap(authentication -> this.jwsService.builder(PrincipalAuthentication.class, this.jwsKey) // 5
                        .header(header -> header
                            .algorithm(OCTAlgorithm.HS256.getAlgorithm())
                        )
                        .payload(authentication)
                        .build(MediaTypes.APPLICATION_JSON)
                        .map(JWSAuthentication::new)
                    ),
                    LoginSuccessHandler.of(
                        new CookieTokenLoginSuccessHandler<>("/form"),                                             // 6
                        new RedirectLoginSuccessHandler<>()                                                        // 7
                    ),
                    new RedirectLoginFailureHandler<>("/login")                                                    // 8
                ));
    }
}
```

1. The `LoginActionHandler` is used to handle `POST` request submitted in the login form.
2. The `FormCredentialsExtractor` is used to extract user credentials submitted in the login form as `LoginCredentials`, the actual username and password form parameter names can be set in the credentials extractor (defaults to `username` and `password`).
3. A simple `PrincipalAuthenticator` is then used to authenticate the credentials.
4. The authentication shall fail if the principal authenticator returns a denied authentication.
5. The resulting `PrincipalAuthentication` is then wrapped into a `JWSAuthentication`. We don't have to check whether the authentication is authenticated before creating the JWS token since we used `failOnDenied()`.
6. The `CookieTokenLoginSuccessHandler` is used to set the compact representation of the JWS token in a response cookie. The cookie name and the cookie path can be set when creating the login success handler (defaults to `AUTH-TOKEN` and `/`).
7. The `RedirectLoginSuccessHandler` is then chained to redirect the user to the the page initially requested.
8. The `RedirectLoginFailureHandler` is used to redirect the user to the login page in case of failed authentication, the actual authentication error is specified in a query parameter (defaults to `error`) so it can be displayed in the login form.

The login page and the login action handler are all set, we can now move on and configure a token based authentication to secure `/form/**` routes and restrict access to authenticated users. Since the login action handler sets a JWS token in a cookie we need to use a `CookieTokenCredentialsExtractor` to extract the `TokenCredentials` and a `JWSAuthenticator` to validate the JWS. The JWS actually wraps the original `PrincipalAuthentication` we can then unwrap it in order to restore the original authentication.

```java
package io.inverno.example.app_web_security;

...

public class SecurityConfigurer implements WebRoutesConfigurer<SecurityContext<Identity, AccessController>>, WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>> {

    ...
    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .path("/form/**")
                .interceptors(List.of(
                    SecurityInterceptor.of(
                        new CookieTokenCredentialsExtractor(),
                        new JWSAuthenticator<>(this.jwsService, PrincipalAuthentication.class, this.jwsKey)
                            .failOnDenied()
                            .map(jwsAuthentication -> jwsAuthentication.getJws().getPayload())
                    ),
                    AccessControlInterceptor.authenticated()
                ));
    }
}
```

In above code, the JWS authenticator uses the JWS service to parse and validate the JWS token. A denied `JWSAuthentication` with an `InvalidCredentialsException` cause is returned on invalid tokens.

> Using a JWS token allows to restore the original authentication which can be very useful for resolving identity and/or access controller using regular authentication types (e.g. `PrincipalAuthentication`, `UserAuthentication`...).

Accessing a protected resource with no token or an invalid token results in an `UnauthorizedException` error since the access is retricted to authenticated users. the client should then be redirected to the login page. This can be done applying the `FormAuthenticationErrorInterceptor` on `UnauthorizedException` errors on `/form/**` routes.

```java
package io.inverno.example.app_web_security;

...

public class SecurityConfigurer implements WebRoutesConfigurer<SecurityContext<Identity, AccessController>>, WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    ...
    @Override
    public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
        errorRouter
            .intercept()
                .error(UnauthorizedException.class)
                .path("/form/**")
                .interceptor(new FormAuthenticationErrorInterceptor<>("/login"))
            // We must apply interceptors to intercept white labels error routes which are already defined
            .applyInterceptors();
    }
}
```

Finally, a `/logout` route can also be defined using a `LogoutActionHandler` which uses an `AuthenticationReleaser` to release the security context and a `LogoutSuccessHandler` to handle successful logout and respond to the client. In the following example, a `CookieTokenLogoutSuccessHandler` is used to delete the token cookie and a `RedirectLogoutSuccessHandler` is used to redirect the user after a successful logout to the root of the server (`/`).

```java
package io.inverno.example.app_web_security;

...

public class SecurityConfigurer implements WebRoutesConfigurer<SecurityContext<Identity, AccessController>>, WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    ...
    @Override
    public void configure(WebRoutable<SecurityContext<Identity, AccessController>, ?> routes) {
        routes
            ...
            .route()
                .method(Method.GET)
                .path("/logout")
                .handler(new LogoutActionHandler<>(
                    authentication -> Mono.empty(),
                    LogoutSuccessHandler.of(
                        new CookieTokenLogoutSuccessHandler<>("/form"),
                        new RedirectLogoutSuccessHandler<>()
                    )
                ));
    }
    ...
}
```

Here is the complete code of the `SecurityConfigurer` used to configure form login flow:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.InMemoryLoginCredentialsResolver;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.PrincipalAuthentication;
import io.inverno.mod.security.authentication.PrincipalAuthenticator;
import io.inverno.mod.security.authentication.password.MessageDigestPassword;
import io.inverno.mod.security.http.AccessControlInterceptor;
import io.inverno.mod.security.http.SecurityInterceptor;
import io.inverno.mod.security.http.context.InterceptingSecurityContext;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.http.form.FormAuthenticationErrorInterceptor;
import io.inverno.mod.security.http.form.FormCredentialsExtractor;
import io.inverno.mod.security.http.form.FormLoginPageHandler;
import io.inverno.mod.security.http.form.RedirectLoginFailureHandler;
import io.inverno.mod.security.http.form.RedirectLoginSuccessHandler;
import io.inverno.mod.security.http.form.RedirectLogoutSuccessHandler;
import io.inverno.mod.security.http.login.LoginActionHandler;
import io.inverno.mod.security.http.login.LoginSuccessHandler;
import io.inverno.mod.security.http.login.LogoutActionHandler;
import io.inverno.mod.security.http.login.LogoutSuccessHandler;
import io.inverno.mod.security.http.token.CookieTokenCredentialsExtractor;
import io.inverno.mod.security.http.token.CookieTokenLoginSuccessHandler;
import io.inverno.mod.security.http.token.CookieTokenLogoutSuccessHandler;
import io.inverno.mod.security.identity.Identity;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jws.JWSAuthentication;
import io.inverno.mod.security.jose.jws.JWSAuthenticator;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRouterConfigurer;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;
import io.inverno.mod.web.server.WebRoutable;
import io.inverno.mod.web.server.WebRoutesConfigurer;
import java.util.List;
import reactor.core.publisher.Mono;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebRoutesConfigurer<SecurityContext<Identity, AccessController>>, WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    private final Mono<? extends OCTJWK> jwsKey;
    private final JWSService jwsService;

    public SecurityConfigurer(JWKService jwkService, JWSService jwsService) {
        this.jwsKey = jwkService.oct().generator()
            .algorithm(OCTAlgorithm.HS256.getAlgorithm())
            .generate()
            .cache();
        this.jwsService = jwsService;
    }

    @Override
    public void configure(WebRoutable<SecurityContext<Identity, AccessController>, ?> routes) {
        routes
            .route()
                .method(Method.GET)
                .path("/login")
                .produces(MediaTypes.TEXT_HTML)
                .handler(new FormLoginPageHandler<>("/login"))
            .route()
                .method(Method.POST)
                .path("/login")
                .handler(new LoginActionHandler<>(
                    new FormCredentialsExtractor(),
                    new PrincipalAuthenticator<>(
                        new InMemoryLoginCredentialsResolver(List.of(
                            LoginCredentials.of("john", new MessageDigestPassword.Encoder().encode("password")),
                            LoginCredentials.of("alice", new MessageDigestPassword.Encoder().encode("password")),
                            LoginCredentials.of("bob", new MessageDigestPassword.Encoder().encode("password"))
                        )),
                        new LoginCredentialsMatcher<LoginCredentials, LoginCredentials>()
                    )
                    .failOnDenied()
                    .flatMap(authentication -> this.jwsService.builder(PrincipalAuthentication.class, this.jwsKey)
                        .header(header -> header
                            .algorithm(OCTAlgorithm.HS256.getAlgorithm())
                        )
                        .payload(authentication)
                        .build(MediaTypes.APPLICATION_JSON)
                        .map(JWSAuthentication::new)
                    ),
                    LoginSuccessHandler.of(
                        new CookieTokenLoginSuccessHandler<>("/form"),
                        new RedirectLoginSuccessHandler<>()
                    ),
                    new RedirectLoginFailureHandler<>("/login")
                ))
            .route()
                .method(Method.GET)
                .path("/form/logout")
                .handler(new LogoutActionHandler<>(
                    authentication -> Mono.empty(),
                    LogoutSuccessHandler.of(
                        new CookieTokenLogoutSuccessHandler<>("/form"),
                        new RedirectLogoutSuccessHandler<>()
                    )
                ));
    }

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .path("/form/**")
                .interceptors(List.of(
                    SecurityInterceptor.of(
                        new CookieTokenCredentialsExtractor(),
                        new JWSAuthenticator<>(this.jwsService, PrincipalAuthentication.class, this.jwsKey)
                            .failOnDenied()
                            .map(jwsAuthentication -> jwsAuthentication.getJws().getPayload())
                    ),
                    AccessControlInterceptor.authenticated()
                ));
    }

    @Override
    public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
        errorRouter
            .intercept()
                .error(UnauthorizedException.class)
                .path("/form/**")
                .interceptor(new FormAuthenticationErrorInterceptor<>("/login"))
            // We must apply interceptors to intercept white labels error routes which are already defined
            .applyInterceptors();
    }
}
```

After defining routes `/form/hello` and `/`, we can run the application and test the login flow by accessing `http://localhost:8080/form/hello` which should redirect the Web browser to the white label login page:

<img class="shadow mb-4" src="doc/img/login_page.png" alt="Login page"/>

After filling valid login credentials in the login form, we should be redirected to the protected resource which is now accessible.

<img class="shadow mb-4" src="doc/img/form_hello.png" alt="Form hello"/>

> We described a basic form login flow but it can be extended to match more complex or specific security requirements.
> 
> For instance, two-factors authentication could be implemented quite easily by providing a custom login form that would include a second authentication factor in addition to the login credentials and a specific login credentials authenticator that would check that factor as well, it is even possible to use the standard `UserAuthenticator` and just chain another authenticator to validate the second factor.

## Cross-origin resource sharing (CORS)

Cross-origin resource sharing is a mechanism that allows for cross-domain requests where a resource is requested in a Web browser from a page in another domain. Cross-domain requests are usually forbidden by Web browsers following the [same-origin policy][same_origin_policy], CORS defines a protocol that allows the Web browser to communicate with the server and determine whether a cross-origin request can be authorized.

The `CORSInterceptor` can be used to configure the CORS policy, it can be applied to routes that might be accessed from different domain than the server or globally to apply the policy to all routes.

Assuming the HTTP server runs locally on port `8080`, the following example shows how to authorize all requests from `http://127.0.0.1:9090`:

```java
package io.inverno.example.app_web_security;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.security.http.cors.CORSInterceptor;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;

public class SecurityConfigurer implements WebInterceptorsConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<ExchangeContext, ?> interceptors) {
        interceptors
            .intercept()
                .interceptor(CORSInterceptor.builder("http://127.0.0.1:9090").build());
    }
}
```

The `CORSInterceptor` fully supports the CORS protocol, it allows to define allowed origins (static or using a pattern), methods, headers with max age allowing credentials or private network. Please refer to the [HTTP CORS protocol specification][cors_protocol] for further details in order to create more complex configuration

## Cross-site request forgery protection (CSRF)

Cross-site request forgery attack consists for an attacker to make the Web browser of a victim perform unwanted action on a trusted Web site when the user is authenticated. This is made possible by the use of cookies holding authentication credentials and which are automatically included in the requests by the Web browser. As far as the server is concerned, it can not make the difference between a legitimate and a malicious request as long as it contains valid credentials.

The `CSRFDoubleSubmitCookieInterceptor` can be used to protect against CSRF attacks, it implements the double submit cookie method advised by [OWASP][csrf_owasp].

The following example shows how to configure the Web server in order to prevent CSRF attacks:

```java
package io.inverno.example.app_web_security;

import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.security.http.csrf.CSRFDoubleSubmitCookieInterceptor;
import io.inverno.mod.web.server.WebInterceptable;
import io.inverno.mod.web.server.WebInterceptorsConfigurer;

public class SecurityConfigurer implements WebInterceptorsConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<ExchangeContext, ?> interceptors) {
        interceptors
            .intercept()
                .interceptor(CSRFDoubleSubmitCookieInterceptor.builder().httpOnly(false).build());;
    }
}
```

The name of the reference cookie token is set to `XSRF-TOKEN`, on a `POST`, `PUT`, `PATCH` or `DELETE` request, the interceptor tries to compare its value to a header (`X-CSRF-TOKEN` by default) or, if missing, to a query parameter (`_csrf_token` by default). If the two values are matching, which basically means the client was able to read the cookie, the request can be safely authorized otherwise a forbidden (403) error shall be return to the client.

> When using the `CSRFDoubleSubmitCookieInterceptor` with a Web application developped with [Angular][angular] or other any other framework that support double submit cookie, the `httpOnly` flag of the reference cookie must be set to `false`.
