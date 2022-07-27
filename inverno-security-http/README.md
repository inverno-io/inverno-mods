
[cors]: https://en.wikipedia.org/wiki/Cross-origin_resource_sharing
[csrf]: https://en.wikipedia.org/wiki/Cross-site_request_forgery

[rfc7616]: https://datatracker.ietf.org/doc/html/rfc7616
[rfc7617]: https://datatracker.ietf.org/doc/html/rfc7617

# Security HTTP

The Inverno *security-http* module extends the security API defined in the *security* module and the HTTP server API defined in the *http-server* module in order to secure access to an HTTP server or a Web application.

It defines a complete API for authenticating HTTP requests and exposing the resulting security context in the exchange context which can be used in exchange interceptors and handlers to secure the application. Base implementations for various HTTP and Web security standards are also provided.

It currently provides the following features:

- HTTP [basic][rfc7617] authentication scheme.
- HTTP [digest][rfc7616] authentication scheme.
- Form based authentication.
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

Let's quickly see how to secure a simple Web application using basic authentication exposing a single hello world REST service. The application might initially look like:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;

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
10:19:33.002 [main] INFO  io.inverno.mod.web.Web - Starting Module io.inverno.mod.web...
10:19:33.002 [main] INFO  io.inverno.mod.http.server.Server - Starting Module io.inverno.mod.http.server...
10:19:33.002 [main] INFO  io.inverno.mod.http.base.Base - Starting Module io.inverno.mod.http.base...
10:19:33.009 [main] INFO  io.inverno.mod.http.base.Base - Module io.inverno.mod.http.base started in 6ms
10:19:33.110 [main] INFO  io.inverno.mod.http.server.internal.HttpServer - HTTP Server (nio) listening on http://0.0.0.0:8080
10:19:33.111 [main] INFO  io.inverno.mod.http.server.Server - Module io.inverno.mod.http.server started in 109ms
10:19:33.111 [main] INFO  io.inverno.mod.web.Web - Module io.inverno.mod.web started in 109ms
10:19:33.112 [main] INFO  io.inverno.example.app_web_security.App_web_security - Module io.inverno.example.app_web_security started in 312ms
10:19:33.115 [main] INFO  io.inverno.core.v1.Application - Application io.inverno.example.app_web_security started in 375ms
```

```plaintext
$ curl -i http://localhost:8080/hello
HTTP/1.1 200 OK
content-length: 12

Hello world!
```

From there, let's say we want to protect the access to all route and ask for authentication using [HTTP basic authentication scheme][rfc7617]. In order to do this we must authenticate basic credentials provided within requests and reject requests (401) on denied authentication in which case a basic authentication challenge must also be sent to the client.

We need then to define a Web configurer which defines a **security interceptor** to authenticate requests and the `BasicAuthenticationErrorInterceptor` which intercepts unauthorized (401) errors to return the basic authentication challenge to the client.

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.server.ExchangeContext;
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
import io.inverno.mod.web.ErrorWebRouter;
import io.inverno.mod.web.ErrorWebRouterConfigurer;
import io.inverno.mod.web.WebInterceptable;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import java.util.List;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<Identity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<Identity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .interceptors(List.of(new SecurityInterceptor<>(                         // 1 
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

The Web configurer implements `WebInterceptorsConfigurer` in order to configure route interceptors and `ErrorWebRouterConfigurer` in order to configure error route interceptors and apply them to all error routes (including default routes). It declares the `InterceptingSecurityContext` exchange context type which is required by the `SecurityInterceptor` to set the security context. Interceptors are defined to intercept all routes.

In above code, there are several things that deserves some further explanation:

1. The `SecurityInterceptor` is the Web counterpart of the `SecurityManager`, it is used to authenticate credentials provided in HTTP requests and create the security context which then exposed in the exchange context to make it accessible to exchange interceptors and handlers.
2. In addition to the authenticator and optional identity and access controller resolvers, it requires a credentials extractor used to extract `Credentials` from the request. The `BasicCredentialsExtractor` basically extracts `LoginCredentials` (username/password) from the `authorization` HTTP header of the request.
3. The security interceptor can then use any authenticator that is able to authenticate login credentials such as the `UserAuthenticator`.
4. An access control interceptor is then added in order to limit the access to authenticated users. Just like the security manager, the security interceptor authenticates credentials and creates the security context in the exchange context. But that does not mean authentication was successful, the resulting security context can be anonymous, denied or authenticated. 
5. The `BasicAuthenticationErrorInterceptor` intercepts unauthorized (401) errors and set the basic authentication scheme challenge in the `www-authenticate` HTTP header of the response with the `inverno-basic` realm.
6. The Web server provides white labels error routes by default which must be explicitly intercepted since they have been created before on an unintercepted router.

> Having to explicitly apply interceptors on default routes can be a source of errors and misunderstanding but there is unfortunately no other way if we want to make them overridable. A systematic and safe approach to this issue would be to always override default error routes.

We should now receive an unauthorized (401) error with a basic authentication challenge when requesting http://localhost:8080/hello (or any other endpoint):

```plaintext
$ curl -i http://127.0.0.1:8080/hello
HTTP/1.1 401 Unauthorized
www-authenticate: basic realm="inverno-basic"
content-length: 0

```

In order to access the service, we must provide valid credentials in the `authorization` HTTP header. Basic authentication scheme specifies that authorization is obtained by encoding in base64 the concatenation of the username, a single colon and the password which is `anNtaXRoOnBhc3N3b3Jk` for user `jsmith`:

```plaintext
$ curl -i -H 'authorization: basic anNtaXRoOnBhc3N3b3Jk' http://127.0.0.1:8080/hello
HTTP/1.1 200 OK
content-length: 12

Hello world!
```

We can change the `/hello` route handler to respond with a personalized message. This requires to resolve the identity of the user and use it in the handler.

We use a user repository which can provide user's identity that can then be resolved by defining a `UserIdentityResolver` in the security interceptor:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.server.ExchangeContext;
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
import io.inverno.mod.web.ErrorWebRouter;
import io.inverno.mod.web.ErrorWebRouterConfigurer;
import io.inverno.mod.web.WebInterceptable;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import java.util.List;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class SecurityConfigurer implements WebInterceptorsConfigurer<InterceptingSecurityContext<PersonIdentity, AccessController>>, ErrorWebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(WebInterceptable<InterceptingSecurityContext<PersonIdentity, AccessController>, ?> interceptors) {
        interceptors
            .intercept()
                .interceptors(List.of(new SecurityInterceptor<>(
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

The `PersonIdentity` type is now declared in the `InterceptingSecurityContext` exchange context type.

We can now inject the exchange security context in the route handler and get the identity to provide the personalized message:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;

@Bean
@WebController
public class Main {
    
    @WebRoute( path = "/hello", method = Method.GET)
    public String hello(SecurityContext<? extends PersonIdentity, ? extends AccessController> securityContext) {
        return "Hello " + securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are") + "!";
    }
}
```

> Here we injected `io.inverno.mod.security.http.context.SecurityContext` which extends both `io.inverno.mod.security.context.SecurityContext` and `ExchangeContext`. This interface is not mutable and exposes the exact same components as the regular security context, it should be used in application's route interceptors and handlers. On the other hand, the `InterceptingSecurityContext` is mutable and should only be used by security interceptors and the `SecurityInterceptor` in particular.

User `jsmith` should now receive a personalized message when requesting http://localhost:8080/hello:

```plaintext

We should now receive ae when requesting http://localhost:8080/hello (or any other endpoint):

```plaintext
$ curl -i -H 'authorization: basic anNtaXRoOnBhc3N3b3Jk' http://127.0.0.1:8080/hello
HTTP/1.1 200 OK
content-length: 11

Hello John!
```

Let's create another endpoint for VIP users responding with an extra polite message. VIP users can be placed into the `vip` group and a `RoleBasedAccessContoller` can be resolved in order to check this:

Let's start by creating the `/hello_vip` route:

```java
package io.inverno.example.app_web_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.http.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;
import reactor.core.publisher.Mono;

@Bean
@WebController
public class Main {
    
    @WebRoute( path = "/hello", method = Method.GET)
    public String hello(SecurityContext<? extends PersonIdentity, ? extends RoleBasedAccessController> securityContext) {
        return "Hello " + securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are") + "!";
    }
    
    @WebRoute( path = "/hello_vip", method = Method.GET)
    public Mono<String> hello_vip(SecurityContext<? extends PersonIdentity, ? extends RoleBasedAccessController> securityContext) {
        return securityContext.getAccessController()
            .orElseThrow(() -> new UnauthorizedException())
            .hasRole("vip")
            .map(isVip -> {
                if(!isVip) {
                    throw new UnauthorizedException();
                }
                return "Hello my dear friend " + securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are") + "!";
            });
    }
    ...
}
```

We can now change the Web configurer to resolve the access controller 