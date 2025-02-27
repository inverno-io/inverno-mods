# Session HTTP

The Inverno *session-http* module extends the session API and the HTTP server API respectively defined in the *session* module and the *http-server* module in order to provide session management in an HTTP server or a Web application. It basically provides a session exchange interceptor that resolves the user session from a request, exposes it in a session exchange context and eventually saves it and injects it in the response when needed: in case of a new session or when the session id was refreshed. The session context allows to interact with the session or create it when it is missing. 

Usage is completely transparent and designed to facilitate the interaction with the session, however it is important to understand how sessions are managed within a session store, when they are persisted and when they expire, please refer to the [*session* module documentation](#session) for a proper overview on session management.

In order to use the Inverno *session-http* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app_session_http {
    requires io.inverno.mod.session.http;
}
```

And also declare that dependency in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-session-http</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-session-http:${VERSION_INVERNO_MODS}'
```

Basic HTTP session support using an in-memory session store, a simple `Map` as session data and a cookie to convey the session id can be added to a Web application as follows by configuring the session interceptor on the Web routes that use sessions:

```java
package io.inverno.test.app_session_http;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.session.BasicSessionStore;
import io.inverno.mod.session.InMemoryBasicSessionStore;
import io.inverno.mod.session.http.CookieSessionIdExtractor;
import io.inverno.mod.session.http.CookieSessionInjector;
import io.inverno.mod.session.http.SessionInterceptor;
import io.inverno.mod.session.http.context.BasicSessionContext;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.server.WebServer;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;
import java.util.HashMap;
import java.util.Map;
import reactor.core.publisher.Mono;

@Bean( visibility = Bean.Visibility.PRIVATE )
@WebController
public class Main {

    @Bean( visibility = Bean.Visibility.PRIVATE )
    public static class WebServerConfigurer implements WebServer.Configurer<BasicSessionContext.Intercepted<Map<String, String>>> {

        private final BasicSessionStore<Map<String, String>> sessionStore;

        public WebServerConfigurer(Reactor reactor) {
            this.sessionStore = InMemoryBasicSessionStore.<Map<String, String>>builder(reactor).build();
        }

        @Override
        public WebServer<BasicSessionContext.Intercepted<Map<String, String>>> configure(WebServer<BasicSessionContext.Intercepted<Map<String, String>>> webServer) {
            return webServer
                .intercept()
                .interceptor(SessionInterceptor.of(
                    new CookieSessionIdExtractor<>(),
                    this.sessionStore,
                    new CookieSessionInjector<>()
                ));
        }
    }

    @WebRoute(path = "/session/{name}", method = Method.PUT, consumes = MediaTypes.TEXT_PLAIN)
    public Mono<Void> setSessionAttribute(@PathParam String name, @Body String value, BasicSessionContext<Map<String, String>> sessionContext) {
        return sessionContext.getSessionData(HashMap::new).doOnNext(data -> data.put(name, value)).then();
    }

    @WebRoute(path = "/session", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    public Mono<Map<String, String>> getSessionAttributes(BasicSessionContext<Map<String, String>> sessionContext) {
        return sessionContext.getSessionData(HashMap::new);
    }

    @WebRoute(path = "/session/{name}", method = Method.GET, produces = MediaTypes.TEXT_PLAIN)
    public Mono<String> getSessionAttribute(@PathParam String name, BasicSessionContext<Map<String, String>> sessionContext) {
        return sessionContext.getSessionData(HashMap::new).map(data -> data.get(name));
    }

    public static void main(String[] args) {
        App_web_session appSession = Application.run(new App_web_session.Builder());
    }
}
```

We can run above application and try to set and get some session attributes:

```plaintext
$ mvn inverno:run
...
2025-02-25 10:59:34,151 INFO  [main] i.i.t.a.App_web_session - Starting Module io.inverno.test.app_web_session...
2025-02-25 10:59:34,151 INFO  [main] i.i.m.b.Boot - Starting Module io.inverno.mod.boot...
2025-02-25 10:59:34,279 INFO  [main] i.i.m.b.Boot - Module io.inverno.mod.boot started in 127ms
2025-02-25 10:59:34,279 INFO  [main] i.i.m.w.s.Server - Starting Module io.inverno.mod.web.server...
2025-02-25 10:59:34,279 INFO  [main] i.i.m.h.s.Server - Starting Module io.inverno.mod.http.server...
2025-02-25 10:59:34,279 INFO  [main] i.i.m.h.b.Base - Starting Module io.inverno.mod.http.base...
2025-02-25 10:59:34,281 INFO  [main] i.i.m.h.b.Base - Module io.inverno.mod.http.base started in 2ms
2025-02-25 10:59:34,285 INFO  [main] i.i.m.w.b.Base - Starting Module io.inverno.mod.web.base...
2025-02-25 10:59:34,285 INFO  [main] i.i.m.h.b.Base - Starting Module io.inverno.mod.http.base...
2025-02-25 10:59:34,285 INFO  [main] i.i.m.h.b.Base - Module io.inverno.mod.http.base started in 0ms
2025-02-25 10:59:34,286 INFO  [main] i.i.m.w.b.Base - Module io.inverno.mod.web.base started in 0ms
2025-02-25 10:59:34,304 INFO  [main] i.i.m.h.s.i.HttpServer - HTTP Server (nio) listening on http://0.0.0.0:8080
2025-02-25 10:59:34,304 INFO  [main] i.i.m.h.s.Server - Module io.inverno.mod.http.server started in 25ms
2025-02-25 10:59:34,304 INFO  [main] i.i.m.w.s.Server - Module io.inverno.mod.web.server started in 25ms
2025-02-25 10:59:34,335 INFO  [main] i.i.t.a.App_web_session - Module io.inverno.test.app_web_session started in 187ms
2025-02-25 10:59:34,335 INFO  [main] i.i.c.v.Application - Application io.inverno.test.app_web_session started in 214ms
```

Session attributes can be added by sending `PUT` requests to `http://localhost:8080/session/{attributeName}:

```plaintext
$ curl -i -X PUT -H 'content-type: text/plain' -d 'someValue' http://localhost:8080/session/someAttribute
HTTP/1.1 200 OK
set-cookie: SESSION-ID=ZDAxYzAxZDAtMDFiMi00MjA5LWJkYjUtMWJmNzkyM2Y2MTI0; Path=/; HttpOnly; SameSite=Lax
content-length: 0
```

Now in order to access the session and its attributes, subsequent requests must convey above session cookie:

```plaintext
$ curl -i -H 'cookie: SESSION-ID=ZDAxYzAxZDAtMDFiMi00MjA5LWJkYjUtMWJmNzkyM2Y2MTI0' http://localhost:8080/session
HTTP/1.1 200 OK
content-type: application/json
content-length: 29

{"someAttribute":"someValue"}

$ curl -i -H 'cookie: SESSION-ID=ZDAxYzAxZDAtMDFiMi00MjA5LWJkYjUtMWJmNzkyM2Y2MTI0' http://localhost:8080/session/someAttribute
HTTP/1.1 200 OK
content-type: text/plain
content-length: 9

someValue
```

The session will eventually expire after a period of inactivity (30 minutes by default when using the `InMemoryBasicSessionStore`), but we can also explicitly invalidate the session with the following Web route:

```java
@WebRoute(path = "/session", method = Method.DELETE)
public Mono<Void> invalidateSession(WebExchange<? extends BasicSessionContext<Map<String, String>>> exchange) {
    if(exchange.context().isSessionPresent()) {
        exchange.response().body().before(exchange.context().getSession().flatMap(Session::invalidate));
    }
    return Mono.empty();
}
```

The client can then invalidate the session explicitly:

```plaintext
$ curl -i -X DELETE -H 'cookie: SESSION-ID=ZDAxYzAxZDAtMDFiMi00MjA5LWJkYjUtMWJmNzkyM2Y2MTI0' http://localhost:8080/session
HTTP/1.1 200 OK
set-cookie: SESSION-ID=; Max-Age=0; Path=/
content-length: 0

$ curl -i -H 'cookie: SESSION-ID=ZDAxYzAxZDAtMDFiMi00MjA5LWJkYjUtMWJmNzkyM2Y2MTI0' http://localhost:8080/session
HTTP/1.1 200 OK
content-type: application/json
set-cookie: SESSION-ID=ZTczZmIyODktYmQyYS00YjRiLTlhYzItYWQwOGRlYzJmMjMz; Path=/; HttpOnly; SameSite=Lax
content-length: 2

{}
```

The invalidated session is removed from the session store and a subsequent request conveying the invalidated session id leads to the creation of a new session.

> You probably noticed that session invalidation was done in the `WebResponseBody#before(Mono)` hook, this is actually mandatory to reflect the session invalidation in the response, namely to provide an empty `set-cookie` header. This cannot actually be done in the response body publisher because when it is subscribed the response headers have already been sent to the client, and it is then not possible to inject the session in the response headers. This is also true for any change that might affect the session id and requires to inject the session in the response like refreshing the session id or, when using JWT sessions, updating expiration settings or stateless session data. 

## Session interceptor

The `SessionInterceptor` must be set on the Web routes that use sessions. Its role is to provide a `SessionContext` to subsequent exchange interceptors and eventually the exchange handler and to save the session at the end of the processing of the exchange.

A `SessionInterceptor` instance is created by composing a `SessionIdExtractor` used to extract the session id from the request, a `SessionStore` used to manage and store sessions and a `SessionInjector` used to inject the session in the response. 

It is important to understand what is actually done and especially when during the processing of the exchange to avoid any misuse.

1. The session interceptor first tries to extract the session id from the request. If any was provided, it then tries to resolve the session from the session store. 
2. If a session exists, it is set in the session context otherwise the session creation `Mono` returned by `SessionStore#create()` is set instead.
3. The session interceptor then sets a `ResponseBody#before(Mono)` hook on the exchange response body whose role is to inject the session, if any and if required, into the response. This is done before consuming the response body publisher in order to be able to set response headers.
4. A `ResponseBody#after(Mono)` hook is finally set on the exchange response body in order to save the session at the end of the exchange processing which is after the response has been completely sent to the client.

This implies several things:

- a session will not be automatically saved in case of errors during the processing of the exchange, as a result it will have to be saved explicitly in an error exchange handler using `Session#save()`.
- a session is injected in the response **before** the response body publisher is subscribed, which means that any changes impacting the session id (e.g. creation of the session, refresh session id...) must happen in the exchange handler itself or in a `ResponseBody#before(Mono)` hook.

> As stated in the *session* module documentation, some session store implementations like in-memory session stores do expose the session data actually stored which can then be updated on-fly but this must be considered as an exception and an explicit call to `Session#save()` is the only guarantee that the complete session will actually be persisted in the session store.

A `SecurityInterceptor` is created as follows:

```java
SessionIdExtractor<SessionContext.Intercepted<SessionData, Session<SessionData>>, Exchange<SessionContext.Intercepted<SessionData, Session<SessionData>>>> sessionIdExtractor = ...
SessionStore<SessionData, Session<SessionData>> sessionStore = ...
SessionInjector<SessionData, Session<SessionData>, SessionContext.Intercepted<SessionData, Session<SessionData>>, Exchange<SessionContext.Intercepted<SessionData, Session<SessionData>>>> sessionInjector = ...

SessionInterceptor<SessionData, Session<SessionData>, SessionContext.Intercepted<SessionData, Session<SessionData>>, Exchange<SessionContext.Intercepted<SessionData, Session<SessionData>>>> sessionInterceptor = SessionInterceptor.of(sessionIdExtractor, sessionStore, sessionInjector);
```

Using generics here allows the compiler to properly check that provided components are consistent with each other. Nonetheless, the API provides `BasicSessionContext` and `JWTSessionContext` interfaces in order to simplify a bit above declarations when using basic session and JWT session respectively. 

```java
SessionIdExtractor<BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> sessionIdExtractor = ...
BasicSessionStore<SessionData> sessionStore = ...
SessionInjector<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> sessionInjector = ...

SessionInterceptor<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> sessionInterceptor = SessionInterceptor.of(sessionIdExtractor, sessionStore, sessionInjector);
```

> In practice, it is not needed to specify explicit type arguments. As you'll see in the next example, the compiler should be able to figure them out implicitly from the session store and the Web configurer declaration.

It has to be applied just like any other exchange interceptor to any Web routes using sessions. This can be done by defining a Web configurer implementing `WebRouteInterceptor.Configurer` or `WebServer.Configurer`. The following example shows how to enable session support for `/session/**` routes:

```java
package io.inverno.test.app_session_http;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.session.Session;
import io.inverno.mod.session.SessionStore;
import io.inverno.mod.session.http.CookieSessionIdExtractor;
import io.inverno.mod.session.http.CookieSessionInjector;
import io.inverno.mod.session.http.SessionInterceptor;
import io.inverno.mod.session.http.context.SessionContext;
import io.inverno.mod.web.server.WebRouteInterceptor;
import java.util.Map;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class WebSessionConfigurer implements WebRouteInterceptor.Configurer<SessionContext.Intercepted<Map<String, String>, Session<Map<String, String>>>> {

    private final SessionStore<Map<String, String>, Session<Map<String, String>>> sessionStore;

    public WebSessionConfigurer(SessionStore<Map<String, String>, Session<Map<String, String>>> sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public WebRouteInterceptor<SessionContext.Intercepted<Map<String, String>, Session<Map<String, String>>>> configure(WebRouteInterceptor<SessionContext.Intercepted<Map<String, String>, Session<Map<String, String>>>> interceptors) {
        return interceptors
            .intercept()
                .path("/session/**")
                .interceptor(SessionInterceptor.of(
                    new CookieSessionIdExtractor<>(),
                    this.sessionStore,
                    new CookieSessionInjector<>()
                ));
    }
}
```

The session is extracted from the request and injected in the response in an HTTP cookie, but any `SessionIdExtractor` and `SessionInjector` implementations could have been used as well.

> Above example shows a general setup using generic `SessionContext`, `SessionStore` and `Session` but `BasicSessionContext`, `BasicSessionStore`, `JWTSessionContext`, `JWTSessionStore` or any other `SessionStore` implementations could have been used. 

### Session id extractor

A session id extractor is used in a `SessionInterceptor` to extract the client session id from an HTTP request. The `SessionIdExtractor` interface is a functional interface defining method `extract(Exchange)`. The following example shows a simple inline implementation that extracts a session id from a query parameter:

```java
SessionIdExtractor<BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> queryParameterSessionIdExtractor = exchange -> Mono.justOrEmpty(
    exchange.request().queryParameters().get("session-id")
        .map(Parameter::asString)
        .orElse(null)
);
```

The API provides the `CookieSessionIdExtractor` that extracts the session id from a session cookie named `SESSION-ID` by default.

Multiple session id extractors can be chained in order to extract the session id from different locations within the request by order of preference. For instance, we can chain a `CookieSessionIdExtractor` to above extractor in order to extract the session id from a query parameter or a session cookie in that order.

```java
SessionIdExtractor<BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> composedSessionIdExtractor = queryParameterSessionIdExtractor.or(new CookieSessionIdExtractor<>());
```

### Session injector

A session injector is used in a `SessionInterceptor` before sending the HTTP response headers to inject or remove the session in the HTTP response. The `SessionInjector` interface defines methods `#inject(Exchange, Session)` and `#remove(Exchange)` for which a default no-op implementation is provided. The following example shows a simple inline implementation that sets the session id in a response header:

```java
SessionInjector<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> sessionInjector = (exchange, session) -> Mono.fromRunnable(() -> 
    exchange.response().headers(headers -> headers.set("session-id", session.getId()))
);
```

The API provides the `CookieSessionInjector` that injects the session id in a session cookie named `SESSION-ID` by default.

> A no-op implementation is provided for `#remove(Exchange)` method for convenience as it is not always easy nor possible to inform a client that a session has been invalidated like in above example. In any case if an invalid or expired session id is conveyed in a request no session will be resolved. It is however good practice to provide a proper implementation whenever possible which is the case for the `CookieSessionInjector`.

Multiple session injectors can be composed in order to inject the session at different locations in the response. For instance, we can chain a `CookieSessionInjector` to above injector in order to inject the session id in a response header and in a response cookie.

```java
SessionInjector<SessionData, Session<SessionData>, BasicSessionContext.Intercepted<SessionData>, Exchange<BasicSessionContext.Intercepted<SessionData>>> composedSessionInjector = headerSessionInjector.compose(new CookieSessionInjector<>());
```

## Session context

The `SessionContext` extends the `ExchangeContext` and provides access to the session during the processing an exchange. It basically exposes method `getSession()` which returns a `Mono` that emits the existing session resolved by the session interceptor or a new session if none were present. The session is therefore only created when needed. Furthermore, method `isSessionPresent()` allows to check whether the session interceptor was able to resolve the session which can be useful when the creation of a session is optional.

It also exposes convenient methods `getSessionData()` and `getSessionData(Supplier)` to access session data directly.

> Please remember that depending on the session store and more precisely on the session data save strategy, session data updates might not be persisted unless `Session#setData()` is invoked explicitly. See [*session* module documentation](#session) to know more about this particular aspect.

The `SessionContext.Intercepted` extends the `SessionContext` by providing `setSessionPresent(boolean)` and `setSession(Mono)` which are used by the session interceptor to populate the session context. This mutable session context shall only be used when configuring session support in a Web configurer.

It is usually a good practice to use an upper bound wildcard when declaring the session context in an exchange inside a Web controller in order to avoid compilation errors.

```java
@WebRoute(path = "/session", method = Method.DELETE)
public Mono<Void> invalidateSession(WebExchange<? extends SessionContext<Map<String, String>>, Session<Map<String, String>>> exchange) {
    ...
}
```

### Basic session context

The `BasicSessionContext` interface extends the generic `SessionContext` interface in order to simplify setup when using basic sessions. Just like the `BasicSessionStore`, it basically fixes the `Session` type so that only the session data type needs to be specified.

Using basic sessions, previous session Web configurer could be rewritten as follows:

```java
package io.inverno.test.app_session_http;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.session.BasicSessionStore;
import io.inverno.mod.session.http.CookieSessionIdExtractor;
import io.inverno.mod.session.http.CookieSessionInjector;
import io.inverno.mod.session.http.SessionInterceptor;
import io.inverno.mod.session.http.context.BasicSessionContext;
import io.inverno.mod.web.server.WebRouteInterceptor;
import java.util.Map;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class WebBasicSessionConfigurer implements WebRouteInterceptor.Configurer<BasicSessionContext.Intercepted<Map<String, String>>> {

    private final BasicSessionStore<Map<String, String>> sessionStore;

    public WebBasicSessionConfigurer(BasicSessionStore<Map<String, String>> sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public WebRouteInterceptor<BasicSessionContext.Intercepted<Map<String, String>>> configure(WebRouteInterceptor<BasicSessionContext.Intercepted<Map<String, String>>> interceptors) {
        return interceptors
            .intercept()
                .path("/session/**")
            .interceptor(SessionInterceptor.of(
                new CookieSessionIdExtractor<>(),
                this.sessionStore,
                new CookieSessionInjector<>()
            ));
    }
}
```

> In the particular case of basic sessions, this actually brings little change apart from less verbosity and the fact that `BasicSessionContext` can then be declared in Web controller (remember that there is only one exchange context type generated by the Inverno Web compiler and which extends all types declared in configurers and controllers). but in the end it is still a regular `Session` object that is exposed to the application. On the other hand, declaring a `JWTSessionContext` is much more interesting as it provides additional features like stateless session data.

### JWT session context

The `JWTSessionContext` interface extends the generic `SessionContext` interface in order to simplify setup when using JWT sessions. It fixes the `Session` type to `JWTSession` so that only the *stateful* and *stateless* session data types have to be declared in type arguments and adds methods `getStatelessSessionData()` and `getStatelessSessionData(Supplier)` for accessing *stateless* session data stored in the JWT session id.

Using JWT sessions, a `JWTSessionStore` must be provided within the application and the session Web configurer should be rewritten as follows:

```java
package io.inverno.test.app_session_http;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.session.http.CookieSessionIdExtractor;
import io.inverno.mod.session.http.CookieSessionInjector;
import io.inverno.mod.session.http.SessionInterceptor;
import io.inverno.mod.session.http.context.jwt.JWTSessionContext;
import io.inverno.mod.session.jwt.JWTSessionStore;
import io.inverno.mod.web.server.WebRouteInterceptor;
import java.util.Map;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class WebJWTSessionConfigurer implements WebRouteInterceptor.Configurer<JWTSessionContext.Intercepted<Void, Map<String, String>>> {

    private final JWTSessionStore<Void, Map<String, String>> sessionStore;

    public WebJWTSessionConfigurer(JWTSessionStore<Void, Map<String, String>> sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public WebRouteInterceptor<JWTSessionContext.Intercepted<Void, Map<String, String>>> configure(WebRouteInterceptor<JWTSessionContext.Intercepted<Void, Map<String, String>>> interceptors) {
        return interceptors
            .intercept()
                .path("/session/**")
                .interceptor(SessionInterceptor.of(
                    new CookieSessionIdExtractor<>(),
                    this.sessionStore,
                    new CookieSessionInjector<>()
                ));
    }
}
```

> Note that in above example, the *stateful* session data type has been declared as `Void` which means that all session data will be stored in the stateless data in the JWT session id on the client side.

In a Web controller, it is then possible to get or set *stateless* session data using the `JWTSessionContext`:

```java
@WebRoute(path = "/hello", method = Method.GET)
public Mono<String> hello(JWTSessionContext<Void, Map<String, String>> sessionContext) {
    return sessionContext.getStatelessSessionData()
        .mapNotNull(statelessData -> "Hello " + statelessData.get("user"))
        .switchIfEmpty(Mono.error(new UnauthorizedException()));
}
```

Particular care must be taken when using JWT session, as any change to session metadata like expiration settings or *stateless* session data will result in a session id refresh which must always be performed before response headers are sent to the client, otherwise there is a real risk for the client to lose the session, the session being moved to a different id in the session store after writing the response headers making it impossible for the session interceptor to inject the new session id into the response.

When such situation is detected, the session interceptor should log the following error:

```plaintext
2025-02-25 16:30:41,059 ERROR [inverno-io-epoll-1-2] i.i.m.s.h.i.GenericSessionInterceptor - Session id has been refreshed after response
```

Expiration setting as well as *Stateless* data updates should then always be done in a `ResponseBody#before(Mono)` hook as follows:

```java
@WebRoute(path = "/login", method = Method.GET)
public Mono<String> login(@HeaderParam String authorization, WebExchange<? extends JWTSessionContext<Void, Map<String, String>>> exchange) {
    String username = ... // authenticate authorization header and extract the username
    exchange.response().body().before(exchange.context()
        .getStatelessSessionData(HashMap::new)
        .doOnNext(statelessData -> statelessData.put("user", username))
        .then()
    );
    return Mono.just("OK");
}
```

In practice, *stateless* data should only be used to store data that barely change during the lifetime of the session such as authentication data in order to limit session id refresh. Explicit session id refresh may still be necessary to prevent replay attacks in which case they must be performed in a `ResponseBody#before(Mono)` hook as in above example.
