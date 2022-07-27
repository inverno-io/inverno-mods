[oauth2.1]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-05
[argon2]: https://en.wikipedia.org/wiki/Argon2
[pbkdf2]: https://en.wikipedia.org/wiki/PBKDF2
[bcrypt]: https://en.wikipedia.org/wiki/Bcrypt
[scrypt]: https://en.wikipedia.org/wiki/Scrypt
[rbac]: https://en.wikipedia.org/wiki/Role-based_access_control
[ldap]: https://en.wikipedia.org/wiki/Lightweight_Directory_Access_Protocol
[active_directory]: https://en.wikipedia.org/wiki/Active_Directory
[cors]: https://en.wikipedia.org/wiki/Cross-origin_resource_sharing
[csrf]: https://en.wikipedia.org/wiki/Cross-site_request_forgery
[redis]: http://redis.io/
[nist_digital_identity_guidelines]: https://pages.nist.gov/800-63-3/sp800-63b.html

[rfc7515]: https://datatracker.ietf.org/doc/html/rfc7515
[rfc7516]: https://datatracker.ietf.org/doc/html/rfc7516
[rfc7517]: https://datatracker.ietf.org/doc/html/rfc7517
[rfc7518]: https://datatracker.ietf.org/doc/html/rfc7518
[rfc7519]: https://datatracker.ietf.org/doc/html/rfc7519
[rfc7616]: https://datatracker.ietf.org/doc/html/rfc7616
[rfc7617]: https://datatracker.ietf.org/doc/html/rfc7617

# Security

The Inverno *security* module defines an API for securing access to protected services or resources in an application.

Securing an application is a complex topic which involves multiple concerns such as authentication, identification, access control, cryptography... Over the years, many techniques and specifications were created to address these concerns and protect against always more complex attacks. Defining a generic security API that is consistent with all these aspects is therefore a tedious task.

The Inverno security API has been designed to follow a clear security model with the aim of simplifying security setup inside an application by relying on simple concepts in order to keep things manageable and understandable.

The Inverno security model, which basically defines application security, is based on three main concepts:

- **Authentication** which relates to the authentification of a request made to the application.
- **Identification** which relates to the identification of the entity accessing the application.
- **Access Control** which relates to the control of access to protected services or resources in the application.

The authentication process is about authenticating credentials (e.g. user/password, token...) provided in a request in order to assess whether access to the application is granted to a requesting entity. It is very important to understand that authentication is not about authenticating the entity but really the credentials. The entity represents the originator of a request to the application, it can be external or internal, it can be an application, a device, a proxy or an actual person but as far as the application is concerned, access can only be granted when valid credentials have been authenticated which is more related to the request than the actual entity behind that request. When refering to the *authenticated entity*, we simply refer to that entity behind a request which provided credentials that has been authenticated during the authentication process.

> This is actually an important point so let's take a concrete example to better understand what it means. Let's consider a prepaid card which allows for ten entries to a roller coaster, you can buy one and at the entrance pass it to your friends one after the other so you can all enjoy the ride. When passing the gates, it is the pass that is being authenticated not the person holding that pass.

The identification process is about identifying the authenticated entity accessing the application. This goes beyond authentication whose role is, and we insisted on that, to validate that provided credentials are valid and which does not necessarily give any information about who or what is actually accessing the application.

The access control process is about controlling whether an authenticated entity has the proper clearance (e.g. roles, permissions...) to access specific services or resources within the application.

From these definitions, it is important to notice that although authentication, identification and access control are all related to an entity accessing the application, they are not necessarily related to each others. For instance [OAuth2][oauth2.1] is a perfect example of authentication without identification. Then we can surely conceive multiple cases where we have authentication without access control, for example an opaque token can be authenticated which gives us no information about the roles or permissions of the authenticated entity. To sum up, a requesting entity can be authenticated, then maybe identified and we may be able to control the access to protected services or resources based on other information (e.g. roles, permissions...)

Let's consider a more practical example to illustrate the theory. Let's assume our secured application is actually a secured facility:

- a person can only enter the facility if he authenticates at the entrance by showing proper credentials:
    - it can be a blank badge that gives him access to the facility but does not strongly identify him.
    - it can a badge with identification information which is actually useless to properly identify the person unless he can prove he is the actual owner of the badge (e.g. using biometric information).
    - it can be some kind of ID registered in the facility security system like a driver's license or an ID card. From there he can receive a temporary badge to access the rest of the facility (e.g. a visitor badge). In this case we might have some identification information but not necessarily what is needed to fully use the services offered inside the facility. Let's say the facility is a bank and the person is here to make a withdrawal, once inside the bank the ID card authenticated at the entrance does not give any information about the person's bank account and whether he is actually the owner of that bank account. These might be considered as identification information which require additional identification process.
    - it can be a registered fingerprint or any kind of biometric information which might also provide identification information assuming they are securely stored inside the facility security system.
- the person can then enter the facility and access areas or use services inside:
    - there can be unsecured services, like a coffee machine in the lobby which anybody within the facility can use.
    - there can be restricted areas or services that require proper clearance to access. The person must then re-authenticate using the same credentials he used to enter the facility or using temporary credentials received at the entrance (e.g. visitor badge). Access control must then be performed and requires to have the person's clearances securely stored in the facility security system or inside the temporary credentials in which case they should ideally be signed and encrypted to guarantee both integrity (we don't want to let him forge his own clearances) and privacy (we don't want to let him know how access control works in the system).
    - there can be services that require further identification information which can be already available following the person's authentication or which require some additional verification. For instance, the facility can be a casino, anybody can access the restaurant area but the casino area is restricted to adults over 18.
- finally when leaving the facility, the person must return any temporary credentials he receveived (e.g. visitor badge in exchange from his ID card) or we can just let him go if those credentials have an expiration time and/or can be revoked anytime when we don't want him to use the facility anymore. 

An Inverno application is secured by composing authentication with identity and access controller inside a **Security Context** that implements application security requirements. 

The *security* module defines the core security API and several extensions modules provide specific security features:

- the *security-http* module provides exchange interceptors and handlers to secure Web applications.
- the *security-jose* module provides services to manipulate JSON Object Signing and Encryption token as specified by [RFC 7515][rfc7515], [RFC 7516][rfc7516], [RFC 7517][rfc7517], [RFC 7518][rfc7518] and [RFC 7519][rfc7519].
- the *security-ldap* module provides authenticators and identity resolvers to authenticate and identify an entity against an [LDAP][ldap] server or an [Active Directory][active_directory] server.

The complete security API including extension modules currently supports:

- User/password authentication against a user repository (in-memory, Redis...).
- Token based authentication.
- Strong user identification against a user repository (in-memory, Redis...).
- Secured password encoding using message digest, [Argon2][argon2], [Password-Based Key Derivation Function][pbkdf2], [BCrypt][bcrypt], [SCrypt][scrypt]... 
- [Role-based access control][rbac].
- Permission-based access control.
- JSON Object Signing and Encryption (provided in the *security-jose* module).
- LDAP/Active Directory authentication and identification (provided in the *security-ldap* module).
- HTTP [basic][rfc7617] authentication scheme (provided in the *security-http* module).
- HTTP [digest][rfc7616] authentication scheme (provided in the *security-http* module).
- Form based authentication (provided in the *security-http* module).
- Cross-origin resource sharing support ([CORS][cors]) (provided in the *security-http* module).
- Protection against Cross-site request forgery attack ([CSRF][csrf]) (provided in the *security-http* module).

In order to use the Inverno *security* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.security;
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
            <artifactId>inverno-security</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-security:${VERSION_INVERNO_MODS}'
...
```

Before looking into details of the security API, let's see how to secure a simple standalone application composed of a single `HelloService` bean exposing `sayHello()` method. Initially the application might look like:

```java
package io.inverno.example.app_hello_security;

import io.inverno.core.annotation.Bean;

@Bean
public class HelloService {
    
    public void sayHello() {
        StringBuilder message = new StringBuilder();
        message.append("Hello world!");
        System.out.println(message.toString());
    }
}
```

```java
package io.inverno.example.app_hello_security;

import io.inverno.core.v1.Application;

public class Main {
    
    public static void main(String[] args) {
        Application.run(new App_hello_security.Builder()).helloService().sayHello();
    }
}
```

Running the application would return the following output:

```plaintext
$ mvn inverno:run
...
[INFO] Running project: io.inverno.example.app_hello_security@1.0.0-SNAPSHOT...
 [═══════════════════════════════════════════════ 100 % ══════════════════════════════════════════════] 
15:59:29.395 [main] INFO  io.inverno.core.v1.Application - Inverno is starting...


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
     ║ Java version        : 17.0.2+8-86                                                          ║
     ║ Java home           : /home/jkuhn/Devel/jdk/jdk-17.0.2                                     ║
     ║                                                                                            ║
     ║ Application module  : io.inverno.example.app_hello_security                                ║
     ║ Application class   : io.inverno.example.app_hello_security.Main                           ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ...                                                                                       ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


15:59:29.400 [main] INFO  io.inverno.example.app_hello_security.App_hello_security - Starting Module io.inverno.example.app_hello_security...
15:59:29.402 [main] INFO  io.inverno.example.app_hello_security.App_hello_security - Module io.inverno.example.app_hello_security started in 3ms
15:59:29.405 [main] INFO  io.inverno.core.v1.Application - Application io.inverno.example.app_hello_security started in 23ms
Hello world!
15:59:29.411 [Thread-0] INFO  io.inverno.example.app_hello_security.App_hello_security - Stopping Module io.inverno.example.app_hello_security...
```

We want to protect the whole application so basically exit the application if the user could not be authenticated using login/password credentials specified on the command line.

In order to authenticate a user against an in-memory repository, we must create a **security manager** as follows:

```java
package io.inverno.example.app_hello_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.security.SecurityManager;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.Identity;
import java.util.List;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
        
    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("Usage: hello <user> <password>");
            return;
        }
        
        // The security manager uses a user authenticator with an in-memory user repository and a Login credentials (i.e. login/pasword) matcher
        SecurityManager<LoginCredentials, Identity, AccessController> securityManager = SecurityManager.of(
            new UserAuthenticator<>(
                InMemoryUserRepository
                    .of(List.of(
                        User.of("jsmith")
                            .password(new RawPassword("password"))
                            .build()
                    ))
                    .build(), 
                new LoginCredentialsMatcher<>()
            )
        );
        
        securityManager.authenticate(LoginCredentials.of(args[0], new RawPassword(args[1])))
            .subscribe(securityContext -> {
                if(securityContext.isAuthenticated()) {
                    LOGGER.info("User has been authenticated");
                    Application.run(new App_hello_security.Builder()).helloService().sayHello();
                }
                else {
                    securityContext.getAuthentication().getCause().ifPresentOrElse(
                        error -> LOGGER.error("Failed to authenticate user", error),
                        () -> LOGGER.error("Unauthorized anonymous access")
                    );
                }
            });
    }
}
```

Now if we run the application with valid or invalid credentials we should get the following outputs:

```plaintext
$ mvn inverno:run -Dinverno.run.arguments="jsmith password"
16:08:24.078 [main] INFO  io.inverno.example.app_hello_security.Main - User has been authenticated
16:08:24.090 [main] INFO  io.inverno.core.v1.Application - Inverno is starting...
...
16:08:24.108 [main] INFO  io.inverno.example.app_hello_security.App_hello_security - Starting Module io.inverno.example.app_hello_security...
16:08:24.111 [main] INFO  io.inverno.example.app_hello_security.App_hello_security - Module io.inverno.example.app_hello_security started in 4ms
16:08:24.115 [main] INFO  io.inverno.core.v1.Application - Application io.inverno.example.app_hello_security started in 21ms
Hello world!
16:08:24.116 [Thread-0] INFO  io.inverno.example.app_hello_security.App_hello_security - Stopping Module io.inverno.example.app_hello_security...
```

```plaintext
$ mvn inverno:run -Dinverno.run.arguments="jsmith invalid"
...
16:08:49.442 [main] ERROR io.inverno.example.app_hello_security.Main - Failed to authenticate user
io.inverno.mod.security.authentication.InvalidCredentialsException: Invalid credentials
	at io.inverno.mod.security.authentication.AbstractPrincipalAuthenticator.lambda$authenticate$1(AbstractPrincipalAuthenticator.java:74) ~[io.inverno.mod.security-1.5.0-SNAPSHOT.jar:?]
	at reactor.core.publisher.MonoErrorSupplied.subscribe(MonoErrorSupplied.java:55) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.Mono.subscribe(Mono.java:4400) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.FluxSwitchIfEmpty$SwitchIfEmptySubscriber.onComplete(FluxSwitchIfEmpty.java:82) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onComplete(FluxMapFuseable.java:150) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.FluxFilterFuseable$FilterFuseableSubscriber.onComplete(FluxFilterFuseable.java:171) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.Operators$MonoSubscriber.complete(Operators.java:1817) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.MonoSupplier.subscribe(MonoSupplier.java:62) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.Mono.subscribe(Mono.java:4400) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.Mono.subscribeWith(Mono.java:4515) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.Mono.subscribe(Mono.java:4371) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.Mono.subscribe(Mono.java:4307) [reactor.core-3.4.14.jar:?]
	at reactor.core.publisher.Mono.subscribe(Mono.java:4279) [reactor.core-3.4.14.jar:?]
	at io.inverno.example.app_hello_security.Main.main(Main.java:71) [classes/:?]
...
```

We can change the `HelloService` in order to display a personalized greeting message to the authenticated user. This requires to resolve the identity of the user and inject the security context into the `HelloService`. 

The identity of the user can be stored in the user repository and resolved using a `UserIdentityResolver` in the security manager as follows:

```java
package io.inverno.example.app_hello_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.security.SecurityManager;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import java.util.List;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    
    @Bean
    public static interface App_hello_securitySecurityContext extends Supplier<SecurityContext<PersonIdentity, AccessController>> {}
    
    public static void main(String[] args) {
        ...
        // The security manager now uses a user identity resolver to resolve the identity of the authenticated user
        SecurityManager<LoginCredentials, PersonIdentity, AccessController> securityManager = SecurityManager.of(
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
        );
        
        // The security context is now injected in the App_hello_security module
        securityManager.authenticate(LoginCredentials.of(args[0], new RawPassword(args[1])))
            .subscribe(securityContext -> {
                if(securityContext.isAuthenticated()) {
                    LOGGER.info("User has been authenticated");
                    Application.run(new App_hello_security.Builder(securityContext)).helloService().sayHello();
                }
                else {
                    securityContext.getAuthentication().getCause().ifPresentOrElse(
                        error -> LOGGER.error("Failed to authenticate user", error),
                        () -> LOGGER.error("Unauthorized anonymous access")
                    );
                }
            });
    }
}
```

In above code, we also declared a socket bean in order to inject the `SecurityContext` in the module and eventually in the `HelloService` bean:

```java
package io.inverno.example.app_hello_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.accesscontrol.AccessController;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;

@Bean
public class HelloService {
    
    private final SecurityContext<PersonIdentity, AccessController> securityContext;
    
    public HelloService(SecurityContext<PersonIdentity, AccessController> securityContext) {
        this.securityContext = securityContext;
    }
    
    public void sayHello() {
        StringBuilder message = new StringBuilder();
        message.append("Hello ").append(this.securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are")).append("!");
        System.out.println(message.toString());
    }
}
```

If we run the application, we should now get a personalized greeting message using the user identity:

```plaintext
$ mvn inverno:run -Dinverno.run.arguments="jsmith password"
...
Hello John!
```

> A `PersonIdentity` has been attached to the user in the repository but the repository may also contain users with no defined identity which is why `SecurityContext#identity()` returns an `Optional`.

Now let's say we want some priviledged users to be greeted with an extra polite message. We can assign roles to users in the repository and resolve a `RoleBasedAccessContoller` to check priviledges in the `HelloService`:

```java
package io.inverno.example.app_hello_security;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.security.SecurityManager;
import io.inverno.mod.security.accesscontrol.GroupsRoleBasedAccessControllerResolver;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.authentication.LoginCredentials;
import io.inverno.mod.security.authentication.LoginCredentialsMatcher;
import io.inverno.mod.security.authentication.password.RawPassword;
import io.inverno.mod.security.authentication.user.InMemoryUserRepository;
import io.inverno.mod.security.authentication.user.User;
import io.inverno.mod.security.authentication.user.UserAuthenticator;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import io.inverno.mod.security.identity.UserIdentityResolver;
import java.util.List;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    
    @Bean
    public static interface App_hello_securitySecurityContext extends Supplier<SecurityContext<PersonIdentity, RoleBasedAccessController>> {}
    
    public static void main(String[] args) {
        ...
        // The security manager now uses a groups RBAC Resolver to resolve the RBAC access controler of the authenticated user
        SecurityManager<LoginCredentials, PersonIdentity, RoleBasedAccessController> securityManager = SecurityManager.of(
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
        );
        ...
    }
}
```

```java
package io.inverno.example.app_hello_security;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.security.accesscontrol.RoleBasedAccessController;
import io.inverno.mod.security.context.SecurityContext;
import io.inverno.mod.security.identity.PersonIdentity;
import reactor.core.publisher.Mono;

@Bean
public class HelloService {
    
    private final SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext;
    
    public HelloService(SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext) {
        this.securityContext = securityContext;
    }
    
    public void sayHello() {
        this.securityContext.getAccessController()
            .map(rbac -> rbac.hasRole("vip"))
            .orElse(Mono.just(false))
            .subscribe(isVip -> {
                StringBuilder message = new StringBuilder();
                if(isVip) {
                    message.append("Hello my dear friend ").append(this.securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are")).append("!");
                }
                else {
                    message.append("Hello ").append(this.securityContext.getIdentity().map(PersonIdentity::getFirstName).orElse("whoever you are")).append("!");
                }
                System.out.println(message.toString());
            });
    }
}
```

We can now run the application using `jsmith` and `adoe` credentials and see the results:

```plaintext
$ mvn clean inverno:run -Dinverno.run.arguments="jsmith password"
...
Hello my dear friend John!
```

```plaintext
$ mvn clean inverno:run -Dinverno.run.arguments="adoe password"
...
Hello Alice!
```

> As for the identity, we can not assume that an access controller is present in the security context which only proves that an entity has been authenticated.

## Security Manager

Now let's take a closer look at the API starting by the `SecurityManager` which is the main entry point to secure an application.

> Note that when securing a Web application, the role of the `SecurityManager` is actually handled by a `SecurityInterceptor` intercepting secured Web route and populating the exchange context with the security context to make it accessible to route handlers and interceptors. Please refere to the *security-http* module documentation for detailed information.

The security manager is used to authenticate a set of credentials and create a security context exposing the actual authentication result and the authenticated entity's identity and access controller if any. A `SecurityManager` instance is created by composing an `Authenticator` with optional `IdentityResolver` and `AccessControllerResolver` which are respectively used to resolve the `Identity` and the `AccessController` of the authenticated entity based on the `Authentication` object resulting from the authentication of input `Credentials` by the `Authenticator`.

The `SecurityManager` interface bascially chains the authentication, the identity resolution and the access controller resolution in a single method `authenticate()` returning the resulting `SecurityContext`.

```java
Authenticator<Credentials, Authentication> authenticator = ...
IdentityResolver<Authentication, Identity> identityResolver = ...
AccessControllerResolver<Authentication, AccessController> accessControllerResolver = ...

// Create a security manager with authentication only
SecurityManager<Credentials, Identity, AccessController> securityManager = SecurityManager.of(authenticator);

// Create a security manager with identity resolution
SecurityManager<Credentials, Identity, AccessController> securityManager = SecurityManager.of(authenticator, identityResolver);

// Create a security manager with access control resolution
SecurityManager<Credentials, Identity, AccessController> securityManager = SecurityManager.of(authenticator, accessControllerResolver);

// Create a security manager with both identification and access control resolution
SecurityManager<Credentials, Identity, AccessController> securityManager = SecurityManager.of(authenticator, identityResolver, accessControllerResolver);
```

> Note how generics are used to specify what `Credentials` can be authenticated, what `Authentication` object are returned by the authenticator and used by identity and access controller resolvers to resolve specific `Identity` and `AccessController` objects. This basically allows the compiler to check that the security manager is created with consistent `Authenticator`, `IdentityResolver` and `AccessControllerResolver`. 

A security context can then be obtained by authenticating appropriate credentials as defined by the selected authenticator.

```java
SecurityManager<LoginCredentials, PersonIdentity, RoleBasedAccessController> securityManager = ...

SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = securityManager.authenticate(LoginCredentials.of("user", new RawPassword("password"))).block();
```

A security manager shall always return a security context even in case of security errors. For instance it returns:

- an **anonymous** security context when authenticating `null` credentials. An anonymous security context only expose an unauthenticated `Authentication` object with no cause.
- an **denied** security context when authentication or identity or access controller resolutions failed with error.
- an **granted** security context when authentication and identity and access controller resolutions were successful.

The following shows a proper way to handle a security context:

```java
if(securityContext.isAuthenticated()) {
    // Sucessful authentication
    ...
}
else if(securityContext.isAnonymous()) {
    // Anonymous access
    ...
}
else {
    // Failed authentication
    ...
}
```

### Credentials

Credentials must be provided to the application to get access to protected services or resources inside the application. In practice, `Credentials` must be authenticated by the `Authenticator` of a `SecurityManager` which eventually creates the application's `SecurityContext` used accross the application to determine whether the authenticated entity can invoke services or access resources.

There are many forms of credentials which depend on the actual authentication process. The most common is a username/password pair, but we can also think about tokens, an X.509 certificates... The security API exposes several basic type of credentials.

#### TokenCredentials

Token credentials are composed of a single token usually easy to authenticate, temporary, revocable or renewable. They are typically obtained by an entity following other stronger authentication processes using sensitive credentials (e.g. username/password with or without multi-factor authentication...) in order to avoid exposing these sensitive data or to use a cheaper authentication process each time the application is accessed by the authenticated entity.

The `TokenCredentials` class is a basic token credentials implementation exposing an opaque token.

#### PrincipalCredentials

Principal credentials represents generic credentials for a principal entity identified using a username. The `PrincipalCredentials` interface is actually a base type which is simply exposing the username, it does not presume of any particular authentication method (e.g. password, multi-factor, biometric...).

#### LoginCredentials

Login credentials are specific principal credentials with a password which is used to authenticate a principal entity identified by a username.

The `LoginCredentials` interface extends `PrincipalCredentials` and simply exposes a `Password` in addition to the username. Login credentials can be created with a username and a password as follows:

```java
LoginCredentials loginCredentials = LoginCredentials.of("jsmith", new RawPassword("password"));
```

> Login credentials provided by a user in a login form for instance usually contain a raw password in clear text, however it is completely possible to define them using an encoded password and therefore secure the password all the way to the authenticator.

### Password

Password can be used in an authentication based on a shared secret, namely the password. The API defines the `Password` interface which is used to represent a password and allows it to be stored in a secured encoded form, for instance in a user repository. It can also be used to match a password provided in a password based credentials, for instance when authenticating `LoginCredentials` against other password based credentials resolved from a secured repository.

The `Password` interface exposes an encoded password value, the actual `Password.Encoder` that was used to encode the password and `matches()` methods used to match a raw password or another `Password` instance.

A simple message digest password can be created from a raw password value as follows:

```java
// password -> bta60AntIvI9YWRfsFFSRBocTW-4xSzmI...
MessageDigestPassword password = new MessageDigestPassword.Encoder().encode("password");
```

or from an encoded value as follows:

```java
MessageDigestPassword password = new MessageDigestPassword("bta60AntIvI9YWRfsFFSRBocTW-4xSzmI...", new MessageDigestPassword.Encoder());
```

Using the password instance, it is then possible to match a provided raw password value:

```java
if(password.matches("password")) {
    // passwords match
    ...
}
```

In order to properly match passwords, it is important to use the same encoder as the one that was used to encode the password. Password encoders can be configured in various ways to reach a proper level of protection. As a result, when encoding password, it is important to always use constant encoder's settings to be able to recover the exact same password instance from a given encoded password. On way to do that is to hardcode these settings in the application but then they shall never be changed or all passwords must be renewed. Another more reliable way would be to store encoder's settings next the encoded password. This can be done by serializing the password as JSON.

```java
ObjectMapper mapper = new ObjectMapper();

MessageDigestPassword password = new MessageDigestPassword.Encoder("SHA-512", "secret".getBytes(), 16).encode("password");

// {"@c":".MessageDigestPassword","value":"R3IF7VY7Trxh4slRRVF4Yk0_JNIcaAtUZ...","encoder":{"@c":".MessageDigestPassword$Encoder","algorithm":"SHA-512","secret":"c2VjcmV0","saltLength":16}}
String serializedPassword = mapper.writeValueAsString(password);

// Returns a MessageDigestPassword instance
Password<?, ?> readPassword = mapper.readValue(jsonPassword, Password.class);
```

The API currently provides the following `Password` implementations:

- `Argon2Password` which uses [Argon2][argon2] key derivation function.
- `BCryptPassword` which uses [Bcrypt][bcrypt] hashing function.
- `MessageDigestPassword` which uses a `MessageDigest` with salt.
- `PBKDF2Password` which uses [Password-Based Key Derivation Function 2][pbkdf2].
- `SCryptPassword` which uses [Scrypt][scrypt] hashing function.
- The `RawPassword` implementation does not encode passwords, it is typically used to represent in-memory and volatile passwords submitted to a running application for authentication. They are usually matched against stored and secured password credentials. A `RawPassword` instance can't be serialized as JSON as other password implementations, it shall not be stored or communicated under any circumstances.

### Authenticator

In a security manager, an authenticator is responsible for authenticating `Credentials` and returning a resulting `Authentication` which represents a proof that credentials have been authenticated.

The `Authenticator` interface is a functional interface defining one `authenticate()` method. It is then easy to create *inline* authenticator implementations for testing purposes or else. A simplistic authenticator for authenticating login credentials (i.e. username/password) can be created as follows:

```java
Authenticator<LoginCredentials, Authentication> authenticator = credentials -> Mono.fromSupplier(() -> {
    if(credentials.getUsername().equals("user") && credentials.getPassword().equals("password")) {
        return Authentication.authenticated();
    }
    return Authentication.denied();
});
```

An authenticator might not always be able to authenticate provided credentials, this basically means that the authenticator is unable to determine whether specified credentials are valid because it does not manage or understand them. For instance, we can imagine defining different authenticators targeting different user realms or authentication systems, credentials could only be authenticated by the authenticator targeting the same realm or authentication system. 

In such situations, an authenticator can decide to return an empty `Mono` instead of returning a denied authentication or throwing an `AuthenticationException` which would terminate the authentication process. This would allow other authenticators to try to authenticate the credentials. 

Multiple authenticators can be chained using the `or()` operator. In the following example, `authenticator1` is implemented in such a way that it only tries to authenticate users it knows, returning an empty `Mono` for those it doesn't know in order to delegate authentication to `authenticator2` which is terminal and always returns an `Authentication` instance:

```java
Authenticator<LoginCredentials, Authentication> authenticator1 = credentials -> Mono.fromSupplier(() -> {
    if(credentials.getUsername().equals("user1")) {
        if(credentials.getPassword().matches("password")) {
            return Authentication.granted();
        }
        // Claim the credentials and terminate the chain
        return Authentication.denied();
    }
    // Delegate to next authenticator in the chain
    return null;
});

Authenticator<LoginCredentials, Authentication> authenticator2 = credentials -> Mono.fromSupplier(() -> {
    if (credentials.getUsername().equals("user2") && credentials.getPassword().matches("password")) {
        return Authentication.granted();
    }
    return Authentication.denied();
});

Authenticator<LoginCredentials, Authentication> compositeAuthenticator = authenticator1.or(authenticator2);

// A granted authentication is returned by authenticator1
compositeAuthenticator.authenticate(LoginCredentials.of("user1", new RawPassword("password")));

// A denied authentication is returned by authenticator2 which claimed the credentials
compositeAuthenticator.authenticate(LoginCredentials.of("user1", new RawPassword("invalid")));

// A granted authentication is returned by authenticator2
compositeAuthenticator.authenticate(LoginCredentials.of("user2", new RawPassword("password")));

// A denied authentication is returned by authenticator2 which is terminal
compositeAuthenticator.authenticate(LoginCredentials.of("user2", new RawPassword("invalid")));

// A denied authentication is returned by authenticator2 which is terminal
compositeAuthenticator.authenticate(LoginCredentials.of("unknown", new RawPassword("password")));
```

> This approach might be very usefull when there is a need to authenticate credentials against multiple authentication systems. However you must be aware that some authenticator might not be *chainable* since, as `authenticator2` they can be implemented to claim all credentials peventing further authenticator to be invoked. Let's consider a `LoginCredentials` authenticator, it could rightfully consider that any username/password pair that it is unable to validate should be denied.

It is also possible to transform the resulting authentication which can be useful to adapt it for further processing (e.g. identity resolver, access controller resolver, login forms...). In the following example, we transform the authentication returned by a login credentials authenticator into a `TokenAuthentication`:

```java
Authenticator<LoginCredentials, Authentication> authenticator = ...

authenticator.map(authentication -> {
    final String token = UUID.randomUUID().toString();
    return new TokenAuthentication() {
        @Override
        public String getToken() {
            return token;
        }

        @Override
        public boolean isAuthenticated() {
            return authentication.isAuthenticated();
        }

        @Override
        public Optional<SecurityException> getCause() {
            return authentication.getCause();
        }
    };
});
```

A proper authentication implementation shall always return an authentication whether authentication succeeds or fails, however there might be use cases where we simply want to fail and propagate the authentication error. This can be desirable when handling denied authentications is not required and must be delegated to a higher level typically the security manager.

Considering previous example, we can make sure only authenticated authentication will be transformed by using the `failOnDenied()` operator which can be invoked to avoid having to handle denied authentications when transforming the authentication output:

```java
Authenticator<LoginCredentials, Authentication> authenticator = ...

authenticator
    // Fail when an denied authentication is returned and propagate the underlying SecurityException
    .failOnDenied()
    // Only transform successful authentication
    .map(authentication -> {
        final String token = UUID.randomUUID().toString();
        return new TokenAuthentication() {
            @Override
            public String getToken() {
                return token;
            }

            @Override
            public boolean isAuthenticated() {
                return authentication.isAuthenticated();
            }

            @Override
            public Optional<SecurityException> getCause() {
                return authentication.getCause();
            }
        };
    });
```

It is also possible to fail on both denied or anonymous authentications using the `failOnDeniedAndAnonymous()` operator.

> The API was designed to provide the most flexibility to the application which can decide how denied or anonymous authentications should be handled, unauthenticated authentications actually exist to still be able to create a security context and do things inside the application from an unauthenticated authentication. You should however takes particular care when transforming authentication instances using `map()` or `flatMap()` operators, remember that an authentication represents proof that credentials were authenticated and as a result always make sure the authentication state is taken into account all the way. In previous example, we could have quite easily ignored the authentication in the mapper and always returned an authenticated authentication. Using `failOnDenied()` or `failOnDeniedAndAnonymous()` can prevent you form doing such mistakes.

The API provides several base implementations that facilitate the authentication setup in an application.

> Please refer to *security-jose* and *security-ldap* modules documentations for JOSE tokens authenticators (i.e. JWS, JWE, JWT), LDAP and Active Directory authenticators.

#### PrincipalAuthenticator

The principal authenticator is a generic authenticator for `PrincipalCredential` which returns `PrincipalAuthentication`. Authentication is done by matching provided credentials against trusted credentials using a `CredentialsMatcher`. Trusted credentials are resolved by username using a `CredentialsResolver`. A `PrincipalAuthenticator` is then created with a `CredentialsResolver` and a `CredentialsMatcher` as follows:

```java
// Resolves trusted credentials by username (e.g. from a trusted store...)
CredentialsResolver<LoginCredentials> credentialsResolver = ...

// Matches provided credentials against trusted credentials
CredentialsMatcher<LoginCredentials, LoginCredentials> credentialsMatcher = ...

PrincipalAuthenticator<LoginCredentials, LoginCredentials> authenticator = new PrincipalAuthenticator<>(credentialsResolver, credentialsMatcher);

authenticator.authenticate(LoginCredentials.of("user", new RawPassword("password")));
```

A principal authenticator is terminal by default and terminates the authentication by returning a denied authentication on `AuthenticationException` due to unresolvable credentials (`CredentialsNotFoundException`) or unmatched credentials (`InvalidCredentialsException`). A principal authenticator can be made non-terminal in order to chain other authenticators:

```java
PrincipalAuthenticator<LoginCredentials, LoginCredentials> authenticator = new PrincipalAuthenticator<>(credentialsResolver, credentialsMatcher);

LoginCredentials invalidCredentials = LoginCredentials.of("user", new RawPassword("invalid"));

// Returns a denied authentication
PrincipalAuthentication authentication = authenticator.authenticate(invalidCredentials).block();

// Returns null
PrincipalAuthentication authentication = authenticator.authenticate(invalidCredentials).block();
```

#### UserAuthenticator

The user authenticator extends the principal authenticator, it is used to authenticate actual users. As for the `PrincipalAuthenticator`, the `UserAuthenticator` authenticates `PrincipalCredentials`, but it matches them against trusted `User` credentials instead of generic credentials. A user is a specific kind of credentials to represent actual users with `Identity` and groups. The resulting authentication is a `UserAuthentication` which exposes the `Identity` and the set of groups of the authenticated entity. A user is typically used to represent credentials for a physical person accessing the application.

Since the `User` interface exposes both identity and groups, the `UserAuthenticator` can actually authenticate and resolve data required to resolve the user's `Identity` and `AccessController` at once. In a security manager, it can be associated with a `UserIdentityResolver` which extracts the identity from the authentication and a `GroupsRoleBasedAccessControllerResolver` which uses the groups from the authentication as roles to create a `RoleBasedAccessController`.

```java
// Resolves system users by username (e.g. from a user repository...)
CredentialsResolver<User<PersonIdentity>> credentialsResolver = ...

// Matches provided credentials against trusted users which are also LoginCredentials
CredentialsMatcher<LoginCredentials, LoginCredentials> credentialsMatcher = ...

UserAuthenticator<LoginCredentials, PersonIdentity, User<PersonIdentity>> authenticator = new UserAuthenticator<>(credentialsResolver, credentialsMatcher);

UserAuthentication<PersonIdentity> authentication = authenticator.authenticate(LoginCredentials.of("user", new RawPassword("password"))).block();

// first name, last name, email...
PersonIdentity identity = authentication.getIdentity();

// user belongs to groups sales, admin...
Set<String> groups = authentication.getGroups();
```

As for the [principal authenticator](#principalauthenticator), a user authenticator is terminal by default but can be made non-terminal by setting the `terminal` flag to `false`.

### Credentials resolver

A credentials resolver is usually used within `Authenticator` implementations for resolving trusted credentials based on some id provided with the credentials in order to match them against trusted credentials. Both `PrincipalAuthenticator` and `UserAuthenticator` uses this technique to authenticate `LoginCredentials` identified by the username.

The `CredentialsResolver` interface is a functional interface defining one `resolveCredentials()` method. A simplistic implementation can then be created as follows:

```java
CredentialsResolver<LoginCredentials> credentialsResolver = username -> Mono.fromSupplier(() -> {
    switch(username) {
        case "user1": return LoginCredentials.of("user1", new BCryptPassword.Encoder().encode("password1"));
        case "user2": return LoginCredentials.of("user2", new BCryptPassword.Encoder().encode("password2"));
        default: return null;
    }
});

// Returns user1's trusted credentials
LoginCredentials user1Credentials = credentialsResolver.resolveCredentials("user1").block();

// Returns null
LoginCredentials user3Credentials = credentialsResolver.resolveCredentials("user3").block();
```

The API provides several implementations that facilitate the authentication setup in an application.

#### InMemoryLoginCredentialsResolver

An in-memory login credentials resolver can be used to create dynamic and volatile `LoginCredentials` resolvers which are particularly suited for testing and prototyping. The `InMemoryLoginCredentialsResolver` basically looks for `LoginCredentials` stored in a `ConcurrentHashMap` and allows to add or remove credentials as needed.

```java
InMemoryLoginCredentialsResolver inMemoryLoginCredentialsResolver = new InMemoryLoginCredentialsResolver(List.of(LoginCredentials.of("user1", new RawPassword("password"))));
inMemoryLoginCredentialsResolver.put("user2", new RawPassword("password"));
inMemoryLoginCredentialsResolver.remove("user1");
```

#### UserRepository

A user repository is a user credentials resolver that provides CRUD operations to a data store in order to securely store and manage application users.

```java
UserRepository<PersonIdentity, User<PersonIdentity>> userRepository = null;
		
// Create a user with identity and groups
userRepository.createUser(new User<>("jsmith", new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io"), new RawPassword("password"), "group1", "group2"));

// Update user email
userRepository.getUser("jsmith")
    .doOnNext(user -> user.getIdentity().setEmail("jsmith1@inverno.io"))
    .map(userRepository::updateUser)
    .block();

// Password change requires current credentials
userRepository.changePassword(LoginCredentials.of("jsmith", new RawPassword("password")), "newPassword");

// Delete user
userRepository.deleteUser("jsmith").block();
```

A proper `UserRepository` implementation shall rely on a `PasswordPolicy` and a `PasswordEncoder` to respectively control the level of protection offered by passwords and securely store them in the datastore.

The `PasswordPolicy` interface defines the `verify()` method which evaluates the strength of a password in a login credentials against some rules. A `PasswordPolicy.PasswordStrength` provides qualitative and quantitative marks used to evaluate the password strength, it is returned when the password follows the policy and included in a `PasswordPolicyException` thrown when the password does not follow the policy.

The `SimplePasswordPolicy` is a simple implementation that allows to control password's minimum and maximum length:

```java 
PasswordPolicy<LoginCredentials, SimplePasswordPolicy.SimplePasswordStrength> passwordPolicy = new SimplePasswordPolicy<>(4, 8);
		
// Throws a PasswordPolicyException since 'newPassword' is too long (> 8)
SimplePasswordPolicy.SimplePasswordStrength passwordStrength = passwordPolicy.verify(LoginCredentials.of("jsmith", new RawPassword("password")), "newPassword");

// Returns the strength of the password
SimplePasswordPolicy.SimplePasswordStrength passwordStrength = passwordPolicy.verify(LoginCredentials.of("jsmith", new RawPassword("password")), "newPassword");

// WEAK, MEDIUM, STRONG...
passwordStrength.getQualifier();

// 10, 42, 100... The higher the better
passwordStrength.getScore();
```

> Please consider [NIST Digital Identity Guidelines Section 5.1.1.2][nist_digital_identity_guidelines] if you need to create more elaborate implementations.

The `PasswordEncoder` was covered previously in this documentation, it is used to evenly encode passwords before they are stored in the repository.

The API currently provides two `UserRepository` implementations:

- the `InMemoryUserRepository` which stores users in a `ConcurrentHashMap`.
- the `RedisUserRepository` which stores users in a [Redis][redis] datastore.

By default, they both use a default `SimplePasswordPolicy` as password policy and a `PBKDF2Password.Encoder` as password encoder. Custom password policy and encoder can be specified as follows:

```java
// Required to access Redis datastore
RedisClient<String, String> redisClient = null;

// Required to serialize/deserialize users to/from JSON strings
ObjectMapper mapper = null;

// Use BCrypt hashing function and enforce passwords between 10 and 20 characters
UserRepository<PersonIdentity, User<PersonIdentity>> redisUserRepository = new RedisUserRepository<>(redisClient, mapper, new BCryptPassword.Encoder(8, 32), new SimplePasswordPolicy<>(10,20) );
```

> A `UserRepository` can be typically exposed in a REST interface consumed by an admin UI in order to manage application's users.

### Credentials matcher

A credentials matcher is usually used in conjunction with a credentials resolver within `Authenticator` implementations to match credentials against trusted credentials resolved using the credentials resolver. Both `PrincipalAuthenticator` and `UserAuthenticator` uses this technique to authenticate `LoginCredentials` identified by the username.

The `CredentialsMatcher` interface is a functional interface defining one `matches()` method which must be reflexive, symetric and transitive. A simplistic implementation can then be created as follows:

```java
CredentialsMatcher<LoginCredentials, LoginCredentials> credentialsMatcher = (credentials, trustedCredentials) -> {
    return credentials.getPassword().matches(trustedCredentials.getPassword());
};
```

#### LoginCredentialsMatcher

The API provides `LoginCredentialsMatcher` implementation which basically check that usernames are equal and that passwords are matching.

```java
// Match user provided login credentials against trusted user credentials
CredentialsMatcher<LoginCredentials, User<PersonIdentity>> credentialsMatcher = new LoginCredentialsMatcher();
```

### Identity resolver

In a security manager, an identity resolver is responsible for resolving the `Identity` of an authenticated entity based on the `Authentication` returned by an `Authenticator`.

The `IdentityResolver` interface is a functional interface defining one `resolveIdentity()` method which makes it easy to create inline implementations:

```java
IdentityResolver<PrincipalAuthentication, PersonIdentity> identityResolver = authentication -> {
    // The authentication is a proof of authentication, we can assume valid credentials have been provided
    String authenticatedUsername = authentication.getUsername();
    
    // Retrieve user identity from a reactive data source using the authenticated username
    Mono<PersonIdentity> identity = ...
    
    return identity;
};
```

A security manager may or may not use an identity manager depending on what is needed by the application. Identity resolution is also not exclusive to the identity resolver, there might be cases where identity information can actually be resolved during the authentication process, these information can then be exposed in an specific authentication and used in an identity resolver to create the actual identity exposed in the security context.

> We can also think of various use cases where the identity can not or should not be resolved during the authentication process. For instance, in token based authentication, a token can be authenticated using cryptographic techniques (e.g. signature) without requiring to communicate with an external system which might have provided identity information, identity can then be resolved next by the identity resolver if the application needs it. Again, it is important to understand that authentication and identity are not necessarily correlated, the `LDAPIdentityResolver` provided in the *security-ldap* module is a good example that can be used after another authenticator than the `LDAPAuthenticator`.

#### UserIdentityResolver

The `UserAuthenticator` is a good example of identity information resolved during authentication. The identity is resolved with trusted credentials used for authentication in order to save resources. However a security manager still requires an identity resolver in order to expose the identity in the security context. In this particular case, the `UserIdentityResolver` can be used to simply extract the identity from the `UserAuthentication` and returns it to the security manager.

```java
// Simply returns the identity resolved during authentication
IdentityResolver<UserAuthentication<PersonIdentity>, PersonIdentity> identityResolver = new UserIdentityResolver<UserAuthentication<PersonIdentity>, PersonIdentity>();
```

### AccessController resolver

In a security manager, an access controller resolver is responsible for resolving the authorizations granted to the authenticated entity based on the `Authentication` returned by an `Authenticator` in order to control its access to protected services and resources using an `AccessController`.

The `AccessControllerResolver` interface is a functional interface defining method `resolveAccessController()`, a simple inline implementation can be created as follows:

```java
AccessControllerResolver<PrincipalAuthentication, RoleBasedAccessController> accessControllerResolver = authentication -> {
    // The authentication is a proof of authentication, we can assume valid credentials have been provided
    String authenticatedUsername = authentication.getUsername();
    
    // Retrieve the role of the authenticated entity from a reactive data source using the authenticated username
    Mono<Set<String>> roles = ...
    
    return roles.map(RoleBasedAccessController::of);
};
```

As for the [identity resolver](#identity-resolver), a security manager may or may not use an access controller resolver depending on application's needs. As for identity resolution, access control information (e.g. roles, permissions...) can be resolved during authentication. For instance, the `UserAuthenticator` resolves user's groups along with trusted credentials used for authentication. These information can then be passed in the authentication and used within the access controller resolver to create the `AccessController` used to control the access to protected service and resources for the authenticated entity.

#### GroupsRoleBasedAccessControllerResolver

The `GroupsRoleBasedAccessControllerResolver` uses the set of groups exposed in a `GroupAwareAuthentication` (e.g. `UserAuthentication`) to create a [role-based access controller](#rolebasedaccesscontroller).

```java
AccessControllerResolver<GroupAwareAuthentication, RoleBasedAccessController> accessControllerResolver = new GroupsRoleBasedAccessControllerResolver();
```

#### ConfigurationSourcePermissionBasedAccessControllerResolver
 
The `ConfigurationSourcePermissionBasedAccessControllerResolver` creates a [permission-based access controller](#permissionbasedaccesscontroller) for the authenticated entity identified by a username. The resulting access controller is backed by a [configuration source](#configuration-source) which defines permissions by username.

```java
// The configuration source defining permissions by user
ConfigurationSource<?,?,?> configurationSource = null;

ConfigurationSourcePermissionBasedAccessControllerResolver accessControllerResolver = new ConfigurationSourcePermissionBasedAccessControllerResolver(configurationSource);
```

## Security Context

The security context is the central component used to secure an application. It is obtained from a [security manager](#security-manager) after credentials authentication. It is composed of the following sub-components:

- an `Authentication` which results from the authentication of credentials and proves that there was an authentication.
- an `Identity` which provides information about the identity of the authenticated entity.
- an `AccessController` which provides services to determine whether the authenticated entity has the right to access protected services or resources within the application.

These basically correspond to the three main concepts composing the Inverno security model as decribed in the [introduction](#security) of the *security* module.

A `SecurityContext` instance should be distributed in the application anywhere there is a need to protect services and resources (i.e. authentication and access control) or a need for information about the authenticated entity (i.e. identification). It is usually obtained from a security manager but it is also possible to create a security context from previous components as follows:

```java
Authentication authentication = Authentication.granted();
PersonIdentity identity = new PersonIdentity("jsmith", "John", "Smith", "jsmith@inverno.io");
RoleBasedAccessController accessController = RoleBasedAccessController.of("reader", "writer");

SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = SecurityContext.of(authentication, identity, accessController);
```

This construct can be useful for testing but it is important to remember that the API specifies that an authentication must represent the proof that credentials were authenticated which basically guarantees that the security context can be trusted. As a result, the security manager should always be prefered to create the security context.

### Authentication

An authentication results from an authentication process and represents the proof that [credentials](#credentials) were authenticated, typically by an [authenticator](#authenticator). In other words, it guarantees that the entity accessing the application has provided credentials and that they have been authenticated successfully or not.

An `Authentication` is always present in a security context but this does not means credential have been successfully authenticated, it simply means that there was an authentication. It can then takes three forms:

- **anonymous** which corresponds to an authentication which is not authenticated with no cause of error and indicates that authentication was bypassed and application is accessed anonymously.
- **denied** which corresponds to an authentication which is not authenticated with a cause of error (e.g invalid credentials...) and indicates a failed authentication.
- **granted** which corresponds to an authenticated authentication and indicates a successful authentication.

From there, it is up to the application to authorize anonymous access and decide what to do in case of denied access. The following example shows how to fully handle authentication in a security context:

```java
SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = ...

if(securityContext.getAuthentication().isAuthenticated()) {
    // Application is accessed by an authenticated entity: 
    // - use access controller to secure services and resources
    // - use identity to get information about the authenticated entity
    ...
}
else if(securityContext.getAuthentication().isAnonymous()) {
    // Application is accessed anonymously: we can grant partial access or deny access
    ...
}
else {
    // Authentication failed: we should deny access and report the error
    LOGGER.error(securityContext.getAuthentication().getCause().get());
    ...
}
```

By extension, a security context can be anonymous, denied or granted as described in the [security manager](#security-manager). A denied or anonymous security context always returns empty identity and access controller. Previous code can then be rewritten as follows:

```java
SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = ...

if(securityContext.isAuthenticated()) {
    // Application is accessed by an authenticated entity: 
    // - use access controller to secure services and resources
    // - use identity to get information about the authenticated entity
    ...
}
else if(securityContext.isAnonymous()) {
    // Application is accessed anonymously: we can grant partial access or deny access
    ...
}
else {
    // Authentication failed: we should deny access and report the error
    LOGGER.error(securityContext.getAuthentication().getCause().get());
    ...
}
```

> You might have notice that, unlike identity and access controller types, the authentication type is not defined as formal parameter in the `SecurityContext` interface. The authentication type is important in the security manager which uses specific identity and access controller resolvers for which the actual authentication type is important, however it is no longer useful in the security context which only needs to determine whether authentication is anonymous, denied or granted.

### Identity

The identity exposes information that identifies that authenticated entity, it is resolved by the security manager using an [identity resolver](#identity-resolver).

A security context may or may not expose an identity depending on several elements such as whether identity is required by the application or whether an identity can be resolved based on the credentials provided to the security manager. In any case, the application must be prepared to handle security context with no identity. 

```java
SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = ...

securityContext.getIdentity().ifPresentOrElse(
    identity -> {
        // Send an email to the authenticated user
        String email = identity.getEmail();
        ...
    }, 
    () -> {
        LOGGER.warn("Unable to send email: missing identity");
        ...
    }
);
```

### Access Controller

The access controller provides services used to determine whether access to protected service or resource should be granted to the authenticated entity, it is resolved by the security manager using an [access controller resolver](#accesscontroller-resolver).

As for the identity, the application should not assume that a security context exposes an access controller for an authenticated entity and it must be prepared to deal with a missing access controller.

```java
SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = ...

Mono<String> protectedReactiveService = securityContext.getAccessController()
    .map(accessController2 -> accessController2
        .hasRole("reader")
        .map(hasRole -> {
            if(!hasRole) {
                throw new ForbiddenException();
            }
            // User is authorized: do something useful
            return "User is a reader";
        })
    )
    .orElseThrow(() -> new InternalServerErrorException("Missing access controller"));
```

The API provides `AccessController` implementations to get [role-based access control][rbac] or permission-based access control.

#### RoleBasedAccessController

A role-based access controller defines services used to determine whether an authenticated entity has a particular set of roles. [Role-based access control][rbac] is used to protect access to services or resources based on the roles that were assigned to the authenticated user.

A `RoleBasedAccessController` is ideally obtained from an authentication by a security manager using a specific access controller resolver, but a simple instance can also be created from a collection of roles as follows:

```java
RoleBasedAccessController accessController = RoleBasedAccessController.of("reader", "writer");
```

This construct can be useful for `AccessControllerResolver` implementations and testing purposes.

The `RoleBasedAccessController` interface basically defines three methods: `hasRole()` used to determine whether the autheticated entity has a specific role, `hasAnyRole()` used to determine whether the authenticated entity has any of the roles in a set of roles and `hasAllRole()` used to determine whether the authenticated entity has all the roles in a set of roles.

```java
SecurityContext<PersonIdentity, RoleBasedAccessController> securityContext = ...

securityContext.getAccessController()
    .ifPresent(accessController2 -> {
        // Returns true if the authenticated user has role 'reader'
        Mono<Boolean> canRead = accessController2.hasRole("reader");
        
        // Returns true if the authenticated user has any of the roles: 'writer', 'admin'
        Mono<Boolean> canWrite = accessController2.hasAnyRole("writer", "admin");
        
        // Returns true if the authenticated user has all of the roles: 'reader', 'writer'
        Mono<Boolean> canReadAndWrite = accessController2.hasAllRoles("reader", "writer");
    });
```

> These methods are reactive to support implementations using non-blocking operations.

#### PermissionBasedAccessController

A permission-based access controller defines services used to determine whether an authenticated has the required permissions to access a protected service or resource. Access to services or resources is then controlled based on the permissions granted to the authenticated user for a particular context. Permissions are evaluated in a context defined by a set of parameters, such permissions are referred as **parameterized permissions**.

The `PermissionBasedAccessController` interface basically defines three kind of methods: `hasPermission()` used to determine whether the authenticated user has a particular permission in a particular context, `hasAnyPermission()` used to determine whether the authenticated entity has any of the permissions in a set of permissions in a particular context and `hasAllPermissions()` used to determine whether the authenticated entity has all the permissions in a set of permissions in a particular context.

```java
SecurityContext<PersonIdentity, PermissionBasedAccessController> securityContext = null;

securityContext.getAccessController()
    .ifPresent(accessController -> {
        // Returns true if the authenticated user has permission read
        Mono<Boolean> canRead = accessController.hasPermission("read");
    
        // Returns true if the authenticated user has permission read on 'contract' documents
        Mono<Boolean> canReadContracts = accessController.hasPermission("read", PermissionBasedAccessController.Parameter.of("documentType", "contract"));
        
        // Returns true if the authenticated user has permission 'manage' or 'admin'
        Mono<Boolean> canManagePrinter = accessController.hasAnyPermission(Set.of("manage", "admin"));
        
        // Returns true if the authenticated user has permission can manage printer 'lp1200'
        Mono<Boolean> canManagePrinterLP1200 = accessController.hasAnyPermission(Set.of("manage", "admin"), PermissionBasedAccessController.Parameter.of("printer", "lp1200"));
        
        // Returns true if the authenticated user can book and modify 'AF' flights from 'Orly' airport
        Mono<Boolean> canBookAndModify = accessController.hasAllPermissions(Set.of("book", "modify"), PermissionBasedAccessController.Parameter.of("company", "AF"), PermissionBasedAccessController.Parameter.of("origin", "ORY"));
    });
```

Parameterized permissions are very powerful and offer the most flexibility to control access to protected services and resources by taking the operational context into account. They are very similar to parameterized configuration properties as described in the *configuration* module. It is then no surprise than the API provides the `ConfigurationSourcePermissionBasedAccessController` implementation which is backed by a `ConfigurationSource` to resolve permissions as configuration properties defined as follows:

- the property name can be either a username or a role name prefixed with a role prefix to differentiate them from users (defaults is `ROLE_`)
- the property parameters are the permissions parameters defining the context into which permissions are defined
- the property value is a comma separated list of permissions defined using the following rules:
    - `permission` to indicates a granted permission
    - `!permission` to indicates that a permission must not be granted
    - `*` to indicate that all permissions are granted

The configuration source can be configured to use various defaulting strategies depending on the needs, it is however common to use a `DefaultingStrategy.wildcard()` strategy as it is more adapt than the `DefaultingStrategy.lookup()` strategy in that particular context.

Considering the following permissions defined in a `CPropsFileConfigurationSource`:

```plaintext
[ domain = "printer" ] {
	# jsmith has role 'user' and therefore permission to query to any printer in the printer domain
	ROLE_user="query"
	ROLE_admin="*"
}

[ domain = "printer", printer = "lp1200" ] {
	# jsmith has permission to query and print to printer lp1200
	jsmith="query,print"
}

[ printer="epsoncolor" ] {
	# jsmith has permission to manage printer epsoncolor across all domains
	# when querying with (domain=printer,printer=epsoncolor) the permission is actually 'query' because domain parameter has the highest priority
	jsmith="manage"
	ROLE_user="query,print"
}

[ domain = "printer", printer = "XP-4100" ] {
	# jsmith has all permission on printer XP-4100
	jsmith="*"
}

[ domain = "printer", printer = "HL-L6400DW" ] {
	ROLE_user="query,print"
}

[ domain = "printer", printer = "C400V_DN" ] {
	jsmith="*,!manage"
}
```

We can then control permissions for user `jsmith` as follows:

```java
CPropsFileConfigurationSource src = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/permissions.cprops")))
    .withDefaultingStrategy(DefaultingStrategy.wildcard());

PermissionBasedAccessController pbac = new ConfigurationSourcePermissionBasedAccessController(src, "jsmith", Set.of("user"));

// true: 'jsmith' has role 'user' for which permission query is granted in domain 'printer'
pbac.hasPermission("query", "domain", "printer").block();

// true: 'jsmith' has role 'user' for which permission query is granted in domain 'printer'
pbac.hasPermission("query", "domain", "printer", "printer", "TM-C3500").block();

// false: 'jsmith' only have permission query in domain 'printer'
pbac.hasPermission("query").block();

// true: 'jsmith' has all permissions on printer 'XP-4100' in domain 'printer'
pbac.hasPermission("manage", "domain", "printer", "printer", "XP-4100").block();

// true: 'jsmith' has all permissions but 'manage' permission on printer 'C400V-DN' in domain 'printer'
pbac.hasPermission("print", "domain", "printer", "printer", "C400V-DN").block();

// false: 'jsmith' has all permissions but 'manage' permission on printer 'C400V-DN' in domain 'printer'
pbac.hasPermission("manage", "domain", "printer", "printer", "C400V-DN").block();
```

It is important to remember that when using a defaulting strategy, the order into which parameters are specified in the query can impact results. For instance, the wildcard strategy gives priority to the permission defined with the most parameters and in case of conflict to parameters defined from left to right in the query.

> *With great power comes great responsability.* As you can imagine, this particular permission-based access controller implementation is quite complex and requires rigor to be used properly. The more parameters are considered, the more difficult it is to define permissions. This might also have an impact on performances, especially when a defaulting strategy is used (wildcard defaulting may require `2^n` queries on the configuration source where `n` is the number of parameter). As a guideline, you should try to consider limited number of parameters (ideally two and not more than three) and consider caching permissions.

> As of now, the impact on performances that might be introduced by the `ConfigurationSourcePermissionBasedAccessController` is still unclear due to limited real-life feedbacks which is why no big decision was taken yet to provide caching solutions. Possible solutions include using multiple dedicated Redis replicas when using a `RedisConfigurationSource` or caching the complete list of permissions by user in an in-memory configuration source.
