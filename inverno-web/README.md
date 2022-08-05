[inverno-tools-root]: https://github.com/inverno-io/inverno-tools

[webjars]: https://www.webjars.org/
[open-api]: https://www.openapis.org/
[swagger-ui]: https://swagger.io/tools/swagger-ui/
[form-urlencoded]: https://url.spec.whatwg.org/#application/x-www-form-urlencoded
[ndjson]: http://ndjson.org/
[yaml]: https://en.wikipedia.org/wiki/YAML
[server-sent-events]: https://en.wikipedia.org/wiki/Server-sent_events

[rfc-7231-5.1.1]: https://tools.ietf.org/html/rfc7231#section-5.1.1
[rfc-7231-5.3]: https://tools.ietf.org/html/rfc7231#section-5.3
[rfc-7231-5.3.1]: https://tools.ietf.org/html/rfc7231#section-5.3.1
[rfc-7231-5.3.2]: https://tools.ietf.org/html/rfc7231#section-5.3.2
[rfc-7231-5.3.5]: https://tools.ietf.org/html/rfc7231#section-5.3.5
[rfc-7231-7.1.2]: https://tools.ietf.org/html/rfc7231#section-7.1.2
[rfc-6455]: https://datatracker.ietf.org/doc/html/rfc6455
[rfc-6455-1.9]: https://datatracker.ietf.org/doc/html/rfc6455#section-1.9

# Web

The Inverno *web* module provides extended functionalities on top of the *http-server* module for developing high-end Web and RESTfull applications.

It especially provides:

- advanced HTTP request routing and interception
- content negotiation
- automatic message payload conversion
- path parameters
- static handler for serving static resources
- version agnostic [WebJars][webjars] support
- smooth Web/REST controller development
- [OpenAPI][open-api] specifications generation using Web/REST controllers JavaDoc comments
- SwaggerUI integration
- an Inverno compiler plugin providing static validation of the routes and generation of Web server controller configurers

The *web* module composes the *http-server* module and therefore starts a HTTP server. Just like the *http-server* module, it requires a net service and a resource service as well as a list of [media type converters](#media-type-converter) for message payload conversion. Basic implementations of these services are provided by the *boot* module which provides `application/json`, `application/x-ndjson` and `text/plain` media type converters. Additional media type converters can also be provided by implementing the `MediaTypeConverter` interface.

In order to use the Inverno *web* module, we should declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_web {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.web;
}
```

We also need to declare these dependencies in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-boot</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-web:${VERSION_INVERNO_MODS}'
...
```

## Web Routing API

The *web* module defines an API for routing HTTP requests to the right handlers. 

A **router** is a server exchange handler as defined by the *http-server* module API which can be used to handle exchanges or error exchanges in the server controller of the HTTP server, its role is to route an exchange to an handler based on a set of rules applied to the exchange.

A **route** specifies the rules that an exchange must matched to be routed to a particular handler. A **route interceptor** specifies the rules that a route must match to be intercepted by a particular exchange interceptor.

A **route manager** is used to manage the routes in a router or, more explicitly, to list, create, enable or disable routes in a router. An **interceptor manager** is used to configure the route interceptors in an intercepted router.

> The module defines a high level SPI in `io.inverno.mod.spi` package that can be used as a base to implement custom routing implementations in addition to the provided Web routing implementations. Nevertheless, it is more of a guideline, one can choose a totally different approach to implement routing, in the end the HTTP server expects a `ServerController` with an `ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>>` to handle exchange and an `ExchangeHandler<ExchangeContext, ErrorExchange<ExchangeContext>>` to handle errors, what is done inside these handlers is completely opaque, the SPI only shows one way to do it.

A `WebRouter` is used to route a `WebExchange` to the right `ExchangeHandler`, it extends `ExchangeHandler` and it is typically used as the exchange handler in a the server controller of the HTTP server.

An `ErrorRouter` is used to route an `ErrorWebExchange` to the right `ExchangeHandler` when an exception is thrown during the normal processing of an exchange, it extends `ExchangeHandler` and it is typically used as the error exchange handler in a the server controller of the HTTP server.

### Web exchange

The *web* module API extends the [server exchange API](#http-server-api) defined in the *http-server* module. It defines the server `WebExchange` composed of a `WebRequest`/`WebResponse` pair in a HTTP communication between a client and a server. These interfaces respectively extends the `Exchange`, `Request` and `Response` interfaces defined in the *http-server* module. A web exchange handler (i.e. `ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>`) is typically attached to one or more Web routes defined in a `WebRouter`.

The Web exchange provides additional functionnalities on top of the exchange including support for path parameters, request/response body decoder/encoder based on the content type, WebSocket inbound/outbound data decoder/encoder based on the subprotocol.

#### Path parameters

Path parameters are exposed in the `WebRequest`, they are extracted from the requested path by the [Web router](#web-router) when the handler is attached to a route matching a parameterized path as defined in a [URI builder](#uris).

For instance, if the handler is attached to a route matching `/book/{id}`, the `id` path parameter can be retrieved as follows:

```java
ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
    exchange.request().pathParameters().get("id")
        .ifPresentOrElse(
            id -> {
                ...
            },
            () -> exchange.response().headers(headers -> headers.status(Status.NOT_FOUND)).body().empty()
        );
};
```

#### Request body decoder

The request body can be decoded based on the content type defined in the request headers.

```java
ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
    Mono<Result> storeBook = exchange.request().body().get()
        .decoder(Book.class)
        .one()
        .map(book -> storeBook(book));
    exchange.response().body()
        .string().stream(storeBook.map(result -> result.getMessage()));
};
```

When invoking the `decoder()` method, a [media type converter](#media-type-converter) corresponding to the request content type is selected to decode the payload. The `content-type` header MUST be specified in the request, otherwise (400) bad request error is returned indicating an empty media type. If there is no converter corresponding to the media type, a (415) unsupported media type error is returned indicating that no decoder was found matching the content type.

A decoder is obtained by specifying the type of the object to decode in the `decoder()` method, the type can be a `Class<T>` or a `java.lang.reflect.Type` which allows to decode parameterized types at runtime bypassing type erasure. Parameterized Types can be built at runtime using the [reflection API](#reflection-api).

As you can see in the above example the decoder is fully reactive, a request payload can be decoded in a single object by invoking method `one()` on the decoder which returns a `Mono<T>` publisher or in a stream of objects by invoking method `many()` on the decoder which returns a `Flux<T>` publisher. 

Decoding multiple payload objects is indicated when a client streams content to the server. For instance, it can send a request with `application/x-ndjson` content type in order to send multiple messages in a single request. Since everything is reactive the server doesn't have to wait for the full request and it can process a message as soon as it is received. What is remarkable is that the code is widely unchanged.

```java
ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
    Flux<Result> storeBook = exchange.request().body().get()
        .decoder(Book.class)
        .many()
        .map(book -> storeBook(book));
    exchange.response().body()
        .string().stream(storeBook.map(result -> result.getMessage()));
};
```

Conversion of a multipart form data request body is also supported, the payload of each part being decoded independently based on the content type of the part. For instance we can upload multiple books in multiple files in a `multipart/form-data` request and decode them on the fly as follows:

```java
ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().string().stream(Flux.from(exchange.request().body().get().multipart().stream()) // 1 
            .flatMap(part -> part.decoder(Book.class).one())                                    // 2 
            .map(book -> storeBook(book))                                                       // 3 
            .map(result -> result.getMessage())                                                 // 4
        );
};
```

In the previous example:

1. A stream of files is received in a `multipart/form-data` request (note that we assume all parts are file parts).
2. Each part is decoded to a `Book` object, the media type must be specified in the `content-type` header field of the part.
3. The book object so obtained is processed.
4. The result for each upload is returned to the client.

All this process is done in a reactive way, the first chunk of response can be sent before all parts have been processed.

#### Response body encoder

As for the request body, the response body can be encoded based on the content type defined in the response headers. Considering previous example we can do the following:

```java
ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
    Mono<Result> storeBook = exchange.request().body().get()
        .decoder(Book.class)
        .one()
        .map(book -> storeBook(book));
    exchange.response()
        .headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
        .body()
            .encoder(Result.class)
            .one(storeBook);
};
```

When invoking the `encoder()` method, a [media type converter](#media-type-converter) corresponding to the response content type is selected to encode the payload. The `content-type` header MUST be specified in the response, otherwise a (500) internal server error is returned indicating an empty media type. If there is no converter corresponding to the media type, a (500) internal server error is returned indicating that no encoder was found matching the content type.

A single object is encoded by invoking method `one()` on the encoder or multiple objects can be encoded by invoking method `many()` on the encoder. Returning multiple objects in a stream is particularly suitable to implement progressive display in a Web application, for example to display search results as soon as some are available.

```java
ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
    Flux<SearchResult> searchResults = ...;
    exchange.response()
        .headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_NDJSON))
        .body()
            .encoder(SearchResult.class)
            .many(searchResults);
};
```

#### WebSocket message decoder/encoder

A Web exchange can be upgraded to a Web WebSocket exchange. The `Web2SocketExchange` thus created extends `WebSocketExchange` and allows to respectively decode/encode WebSocket inbound and outbound messages based on the subprotocol negotiated during the opening handshake. 

As for request and response payloads, a [media type converter](#media-type-converter) corresponding to the subprotocol is selected to decode/encode inbound and outbound messages. If there is no converter corresponding to the subprotocol, a `WebSocketException` is thrown resulting in a (500) internal server error returned to the client indicating that no converter was found matching the subprotocol.

The subprotocol must then correspond to a valid media type. Unlike request and response payloads which expect strict media type representation, compact `application/` media type representation can be specified as subprotocol. In practice, it is possible to open a WebSocket connection with subprotocol `json` to select the `application/json` media type converter.

> As defined by [RFC 6455][rfc-6455], a WebSocket subprotocol is not a media type and is registered separately, however using media type is very handy in this case as it allows to reuse the data conversion facility. Supporting compact `application/` media type representation allows to mitigate this specification violation as it is then possible to specify a valid subprotocol while still being able to select a media type converter. Let's consider the registered subprotocol `v2.bookings.example.net` (taken from [RFC 6455 Section 1.9][rfc-6455-1.9]), we can then create a media type converter for `application/v2.bookings.example.net` that will be selected when receiving connection for that particular subprotocol.

The following example is a variant of the [simple chat server](#a-simple-chat-server) which shows how JSON messages can be automatically decoded and encoded:

```java
ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>> handler = exchange -> {
    exchange.webSocket("json")
        .orElseThrow(() -> new InternalServerErrorException("WebSocket not supported"))
        .handler(webSocketExchange -> {
            Flux.from(webSocketExchange.inbound().decodeTextMessages(Message.class)).subscribe(message -> this.chatSink.tryEmitNext(message));
            webSocketExchange.outbound().encodeTextMessages(this.chatSink.asFlux());
        })
        .or(() -> exchange.response()
            .body().string().value("Web socket handshake failed")
        );
};
```

### Web route

A Web route specifies the routing rules and the exchange handler that shall be invoked to handle a matching exchange. It can combine the following routing rules which are matched in that order: the path, method and content type of the request, the media ranges and language ranges accepted by the client. For instance, a Web exchange is matched against the path routing rule first, then the method routing rule... Multiples routes can then match a given exchange but only one will be retained to actually process the exchange which is the one matching the highest routing rules.

If a route doesn't define a particular routing rule, the routing rule is simply ignored and matches all exchanges. For instance, if a route doesn't define any method routing rule, exchanges are matched regardless of the method.

The `WebRoutable` interface defines a fluent API for the definition of Web routes. The following is an example of the definition of a Web route which matches all exchanges, this is the simplest route that can be defined:

```java
routable
    .route()                                                   // 1
        .handler(exchange -> {                                 // 2
            exchange.response()
                .headers(headers -> 
                    headers.contentType(MediaTypes.TEXT_PLAIN)
                )
                .body()
                .encoder()
                .value("Hello, world!");
        });
```

1. A new `WebRouteManager` instance is obtained to configure a `WebRoute`
2. We only define the handler of the route as a result any exchange might be routed to that particular route unless a more specific route matching the exchange exists.

An exchange handler can be attached to multiple routes at once by providing multiple routing rules to the route manager, the following example actually results in 8 individual routes being defined:

```java
routable
    .route()
        .path("/doc")
        .path("/document")
        .method(Method.GET)
        .method(Method.POST)
        .consumes(MediaTypes.APPLICATION_JSON)
        .consumes(MediaTypes.APPLICATION_XML)
        .handler(exchange -> {
            ...
        });
```

The Web routable also allows to select all routes that matches the rules defined in a Web route manager using the `findRoutes()` method. The following example select all routes matching `GET` method:

```java
Set<WebRoute<ExchangeContext>> routes = router
    .route()
        .method(Method.GET)
        .findRoutes();
```

It is also possible to enable, disable or remove a set of routes in a similar way:

```java
// Disables all GET routes
routable
    .route()
        .method(Method.GET)
        .disable();

// Enables all GET routes
routable
    .route()
        .method(Method.GET)
        .enable();

// remove all GET routes
routable
    .route()
        .method(Method.GET)
        .remove();
```

Individual routes can be enabled, disabled or removed as follows:

```java
// Disables all GET routes producing 'application/json'
routable
    .route()
        .method(Method.GET)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::disable);

// Enables all GET routes producing 'application/json'
routable
    .route()
        .method(Method.GET)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::enable);

// Removes all GET routes producing 'application/json'
routable
    .route()
        .method(Method.GET)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::remove);
```

Routes can also be configured as blocks in reusable `WebRoutesConfigurer` by invoking `configureRoutes()` methods:

```java
WebRoutesConfigurer<ExchangeContext> public_routes_configurer = routable -> {
    routable
        .route()
        ...
};

WebRoutesConfigurer<ExchangeContext> private_routes_configurer = routable -> {
    routable
        .route()
        ...
};

routable
    .configureRoutes(public_routes_configurer)
    .configureRoutes(private_routes_configurer)
    .route()
    ...
```

#### Path routing rule

The path routing rule matches exchanges whose request targets a specific path or a path that matches against a particular pattern. The path or path pattern of a routing rule must be absolute (ie. start with `/`).

We can for instance define a route to handle all requests to `/bar/foo` as follows:

```java
routable
    .route()
        .path("/foo/bar")
        .handler(exchange -> {
            ...
        });
```

The route in the preceding example specifies an exact match for the exchange request path, it is also possible to make the route match the path with or without a trailing slash as follows:

```java
routable
    .route()
        .path("/foo/bar", true)
        .handler(exchange -> {
            ...
        });
```

A path pattern following the parameterized or path pattern [URIs notation](#uris) can also be specified to create a routing rule matching multiple paths. This also allows to specify [path parameters](#path-parameters) that can be retrieved from the `WebExchange`.

In the following example, the route will match all exchanges whose request path is `/book/1`, `/book/abc`... and store the extracted parameter value in path parameter `id`:

```java
routable
    .route()
        .path("/book/{id}")
        .handler(exchange -> {
            exchange.request().pathParameters().get("id")...
        });
```

A parameter is matched against a regular expression set to `[^/]*` by default which is why previous route does not match `/book/a/b`. Parameterized URIs allow to specify the pattern matched by a particular path parameter using `{[<name>][:<pattern>]}` notation, we can then put some constraints on path parameters value. For instance, we can make sure the `id` parameter is a number between 1 and 999:

```java
routable
    .route()
        .path("/book/{id:[1-9][0-9]{0,2}}")
        .handler(exchange -> {
            ...
        });
```

If we just want to match a particular path without extracting path parameters, we can omit the parameter name and simply write:

```java
routable
    .route()
        .path("/book/{}")
        .handler(exchange -> {
            ...
        });
```

#### Method routing rule

The method routing rule matches exchanges that have been sent with a particular HTTP method.

In order to handle all `GET` exchanges, we can do:

```java
routable
    .route()
        .method(Method.GET)
        .handler(exchange -> {
            ...
        });
```

#### Consume routing rule

The consume routing rule matches exchanges whose request body content type matches a particular media range as defined by [RFC 7231 Section 5.3.2][rfc-7231-5.3.2].

For instance, in order to match all exchanges with an `application/json` request payload, we can do:

```java
routable
    .route()
        .method(Method.POST)
        .consumes(MediaTypes.APPLICATION_JSON)
        .handler(exchange -> {
            ...
        });
```

We can also specify a media range to match, for example, all exchanges with a `*/json` request payload:

```java
routable
    .route()
        .method(Method.POST)
        .consumes("*/json")
        .handler(exchange -> {
            ...
        });
```

The two previous routes are different and as a result they can be both defined, a content negotiation algorithm is used to determine which route should process a particular exchange as defined in [RFC 7231 Section 5.3][rfc-7231-5.3].

Routes are sorted by consumed media ranges as follows:

- quality value is compared first as defined by [RFC7231 Section 5.3.1][rfc-7231-5.3.1], the default quality value is 1.
- type and subtype wildcards are considered after: `a/b` > `a/*` > `*/b` > `*/*`
- parameters are considered last, the most precise media range which is the one with the most parameters with matching values gets the highest priority (eg. `application/json;p1=a;p2=2` > `application/json;p1=b` > `application/json;p1`)

The first route whose media range matches the request's `content-type` header field is selected.

If we consider previous routes, an exchange with an `application/json` request payload will be matched by the first route while an exchange with a `text/json` request will be matched by the second route.

A media range can also be parameterized which allows for interesting setup such as:

```java
routable
    .route()
        .path("/document")
        .method(Method.POST)
        .consumes("application/json;version=1")
        .handler(exchange -> {
            ...
        })
    .route()
        .path("/document")
        .method(Method.POST)
        .consumes("application/json;version=2")
        .handler(exchange -> {
            ...
        })
    .route()
        .path("/document")
        .method(Method.POST)
        .consumes("application/json")
        .handler(exchange -> {
            ...
        });
```

In the above example, an exchange with a `application/json;version=1` request payload is matched by the first route, `application/json;version=2` request payload is matched by the second route and any other `application/json` request payload is matched by the third route.

If there is no route matching the content type of a request of an exchange matched by previous routing rules, a (415) unsupported media type error is returned.

> As described before, if no route is defined with a consume routing rule, exchanges are matched regardless of the request content type, content negotiation is then eventually delegated to the handler which must be able to process the payload whatever the content type.

#### Produce routing rule

The produce routing rule matches exchanges based on the acceptable media ranges supplied by the client in the `accept` header field of the request as defined by [RFC 7231 Section 5.3.2][rfc-7231-5.3.2].

A HTTP client (eg. Web browser) typically sends an `accept` header to indicate the server which response media types are acceptable in the response. The best matching route is determined based on the media types produced by the routes matching previous routing rules.

We can for instance define the following routes:

```java
routable
    .route()
        .path("/doc")
        .produces(MediaTypes.APPLICATION_JSON)
        .handler(exchange -> {
            ...
        }) 
    .route()
        .path("/doc")
        .produces(MediaTypes.TEXT_XML)
        .handler(exchange -> {
            ...
        });
```

Now let's consider the following `accept` request header field:

```plaintext
accept: application/json, application/xml;q=0.9, */xml;q=0.8
```

This field basically tells the server that the client wants to receive first an `application/json` response payload, if not available an `application/xml` response payload and if not available any `*/xml` response payload.

The content negotiation algorithm is similar as the one described in the [consume routing rule](#consume-routing-rule), it is simply reversed in the sense that it is the acceptable media ranges defined in the `accept` header field that are sorted and the route producing the media type matching the media range with the highest priority is selected.

Considering previous routes, a request with previous `accept` header field is then matched by the first route. 

A request with the following `accept` header field is matched by the second route:

```plaintext
accept: application/xml;q=0.9, */xml;q=0.8
```

The exchange is also matched by the second route with the following `accept` header field:

```plaintext
accept: application/json;q=0.5, text/xml;q=1.0
```

If there is no route producing a media type that matches any of the acceptable media ranges, then a (406) not acceptable error is returned.

> As described before, if no route is defined with a produce routing rule, exchanges are matched regardless of the acceptable media ranges, content negotiation is then eventually delegated to the handler which becomes responsible to return an acceptable response to the client. 

#### Language routing rule

The language routing rule matches exchanges based on the acceptable languages supplied by client in the `accept-language` header field of the request as defined by [RFC 7231 Section 5.3.5][rfc-7231-5.3.5].

A HTTP client (eg. Web browser) typically sends a `accept-language` header to indicate the server which languages are acceptable for the response. The best matching route is determined based on the language tags produced by the routes matching previous routing rules.

We can defines the following routes to return a particular resource in English or in French:

```java
routable
    .route()
        .path("/doc")
        .language("en-US")
        .handler(exchange -> {
            ...
        });

routable
    .route()
        .path("/doc")
        .language("fr-FR")
        .handler(exchange -> {
            ...
        });
```

The content negotiation is similar to the one described in the [produce routing rule](#produce-routing-rule) but using language ranges and language types instead of media ranges and media types. Acceptable language ranges are sorted as follows:

- quality value is compared first as defined by [RFC 7231 Section 5.3.1][rfc-7231-5.3.1], the default quality value is 1.
- primary and secondary language tags and wildcards are considered after: `fr-FR` > `fr` > `*`

The route whose produced language tag matches the language range with the highest priority is selected.

As for the produce routing rule, if there is no route defined with a language tag that matches any of the acceptable language ranges, then a (406) not acceptable error is returned. However, unlike the produce routing rule, a default route can be defined to handle such unmatched exchanges.

For instance, we can add the following default route to the router:

```java
routable
    .route()
        .path("/doc")
        .handler(exchange -> {
            ...
        });
```

A request with the following `accept-language` header field is then matched by the default route:

```plaintext
accept-language: it-IT
```

### WebSocket route

The `WebRoutable` interface also exposes `webSocketRoute()` which returns a `WebSocketRouteManager` which allows defining WebSocket routes. A WebSocket route specifies the routing rules and the WebSocket exchange handler that shall be invoked after upgrading a matching exchange to a WebSocket exchange. it can combine the following routing rules which are matched in that order: the path of the request, the language ranges accepted by the client and the supported subprotocol. Unlike a regular Web route, a WebSocket exchange does not support method, consume and produce routing rules, this difference can be explain by the fact that a WebSocket upgrade request is always a `GET` request and that consumed and produced media types have just no meaning in the context of a WebSocket.

When an exchange matches a WebSocket route, the Web router automatically handle the upgrade and setup the WebSocket exchange handler specified in the route. If the WebSocket upgrade is not supported, a `WebSocketException` is thrown resulting in a (500) internal server error returned to the client.

A WebSocket endpoint can then be easily defined as follows:

```java
routable
    .webSocketRoute()
        .path("/ws")
        .subprotocol("json")
        .handler(webSocketExchange -> {
            webSocketExchange.outbound().messages(factory -> webSocketExchange.inbound().messages());
        });
```

`WebSocketRoute` extends `WebRoute`, as a result, just like Web routes, WebSocket routes matching particular rules can be selected, enabled, disabled or removed:

```java
// Disables all WebSocket routes supporting subprotocol 'json'
routable
    .webSocketRoute()
        .subprotocol("json")
        .findRoutes()
        .stream()
        .forEach(WebSocketRoute::disable);

// Enables all routes (including WebSocket routes) with path matching '/ws'
routable
    .route()
        .path("/ws")
        .enable();
```

#### Subprotocol routing rule

The produce routing rule matches exchanges based on the supported subprotocols supplied by the client in the `sec-websocket-version` header field of the request as defined by [RFC 6455][rfc-6455].

A HTTP client (eg. Web browser) wishing to open a WebSocket connection typically sends a `sec-websocket-version` header to indicate the server which subprotocols it supports by order of preference. The best matching route is determined based on the subprotocol supported by the routes matching previous routing rules.

We can then define the following WebSocket routes that handle different subprotocols:

```java
routable
    .webSocketRoute()
        .path("/ws")
        .subprotocol("json")
        .handler(webSocketExchange -> {
            ...
        })
    .webSocketRoute()
        .path("/ws")
        .subprotocol("xml")
        .handler(webSocketExchange -> {
            ...
        })
    .webSocketRoute()
        .path("/ws")
        .handler(webSocketExchange -> {
            ...
        });
```

Let's consider a request with the following `sec-websocket-version` header field:

```plaintext
sec-websocket-version: xml, json
```

This field basically tells the server that the client wants to open a WebSocket connection using the `xml` subprotocol and if not supported the `json` subprotocol. As a result the request is matched by the second route in above example.

If there is no route supporting any of the subprotocols provided by the client, an `UnsupportedProtocolException` is thrown resulting in a (500) internal server error returned to the client. The last route in above example is therefore not a default route, it is only matched when the client open a WebSocket connection with no subprotocol.

### Web route interceptor

A Web route interceptor specifies the rules and the exchange interceptor that shall be applied to a matching route. It can combine the same rules as for the definition of a route: the path and method of the route, media range matching the content consumed by the route, media range and language range matching the media type and language produced by the route.

Multiple web exchange interceptors (i.e. `ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>>`) can be applied to one or more web routes.

The `WebInterceptable` interface defines a fluent API similar to the `WebRoutable` for the definition of Web interceptors. The following is an example of the definition of a Web route interceptor that is applied to routes matching `GET` methods and consuming `application/json` payloads:

```java
interceptable.
    .intercept()
        .method(Method.GET)
        .consumes(MediaTypes.APPLICATION_JSON)
        .interceptor(exchange -> {
            LOGGER.info("Intercepted!");
            return Mono.just(exchange);
        });
```

As for an exchange handler, an exchange interceptor can be applied to multiple routes at once by providing multiple rules to the route interceptor manager, the following example is used to apply a route interceptor to `/doc` and `/document` routes consuming `application/json` or `application/xml` payloads:

```java
interceptable
    .intercept()
        .path("/doc")
        .path("/document")
        .consumes(MediaTypes.APPLICATION_JSON)
        .consumes(MediaTypes.APPLICATION_XML)
        .interceptor(exchange -> {
            ...
        });
```

Multiple interceptors can be applied to a route at once using the `interceptors()` methods. The following example is equivalent as applying `interceptor1` then `interceptor2` on all routes matching `/some_path` (i.e. `interceptor2` is then invoked before `interceptor1`):

```java
ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor1 = ...;
ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> interceptor2 = ...;

interceptable
    .intercept()
        .path("/some_path")
        .interceptors(List.of(interceptor1, interceptor2));
```

The list of exchange interceptors applied to a route can be obtained from a `WebRoute` instance:

```java
// Use a WebRoutable to find a WebRoute
WebRoute<ExchangeContext> route = ...

List<? extends ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> routeInterceptors = route.getInterceptors();
```

In a similar way, it is possible to explicitly set exchange interceptors on a specific `WebRoute` instance:

```java
Set<WebRoute<ExchangeContext>> routes = router.getRoutes();

ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> serverHeaderInterceptor = exchange -> {
    exchange.response()
        .headers(headers -> headers.set(Headers.NAME_SERVER, "Inverno Web Server");

    return Mono.just(exchange);
};

ExchangeInterceptor<ExchangeContext, WebExchange<ExchangeContext>> securityInterceptor = exchange -> {...};

routes.stream().forEach(route -> route.setInterceptors(List.of(serverHeaderInterceptor, securityInterceptor));
```

Route interceptors can also be configured as blocks in reusable `WebInterceptorsConfigurer` by invoking `configureInterceptors()` methods:

```java
WebInterceptorsConfigurer<ExchangeContext> public_interceptors_configurer = interceptable -> {
    interceptable
        .intercept()
        ...
};

WebInterceptorsConfigurer<ExchangeContext> private_interceptors_configurer = interceptable -> {
    interceptable
        .intercept()
        ...
};

interceptable
    .configureInterceptors(public_interceptors_configurer)
    .configureInterceptors(private_interceptors_configurer)
    .intercept()
    ...
```

The definition of an interceptor is very similar to the definition of a route, however there are some peculiarities. For instance, a route can only produce one particular type of content in one particular language that are matched by a route interceptor with matching media and language ranges.

For performance reasons, route interceptor's rules should not be evaluated each time an exchange is processed but once when a route is defined. Unfortunately, this is not always possible and sometimes some rules have to be evaluated when processing the exchange. This happens when the difference between the set of exhanges matched by a route and the set of exchanges matched by a route interceptor is not empty which basically means that the route matches more exchanges than the route interceptor.

In these situations, the actual exchange interceptor is wrapped in order to evaluate the problematic rule on each exchange. A typical example is when a route defines a path pattern (eg. `/path/*.jsp`) that matches the specific path of a route interceptor (eg. `/path/private.jsp`), the exchange interceptor must only be invoked on an exchange that matches the route interceptor's path. This can also happens with method and consumes rules.

> Path patterns are actually very tricky to match *offline*, the `WebInterceptedRouter` implementation uses the `URIPattern#includes()` to determine whether a given URIs set is included into another, when this couldn't be determine with certainty, the exchange interceptor is wrapped. Please refer to the [URIs](#uris) documentation for more information.

> Particular care must be taken when listing the exchange interceptor attached to a route as these are the actual interceptors and not the wrappers. If you set interceptors explicitly on a `WebRoute` instance, they will be invoked whenever the route is invoked.

When a route interceptor is defined with specific produce and language rules, it can only be applied on routes that actually specify matching produce and language rules. Since there is no way to determine which content type and language will be produced by an exchange handler, it is not possible to determine whether an exchange interceptor should be invoked prior to the exchange handler unless specified explicitly on the route. In such case, a warning is logged to indicate that the interceptor is ignored for the route due to missing produce or language rules on the route.

### Web router

The `WebRouter` extends both `WebRoutable` and `WebInterceptable` interfaces. As such routes and route interceptors are defined in the `WebRouter` bean exposed in the *web* module and used in the web server controller to handle web exchange. This internal web server controller is wired to the *http-server* module to override the default HTTP server controller. 

In addition to `configureRoutes()` and `configureInterceptors()` methods defined by `WebRoutable` and `WebInterceptable`, the `WebRouter` interface provides `configure()` methods that accepts `WebRouterConfigurer` to fluently apply blocks of configuration.

```java
WebRouter<ExchangeContext> router = ...

WebRouterConfigurer<ExchangeContext> configurer = ...
List<WebRouterConfigurer<ExchangeContext>> configurers = ...

router
    .configure(configurers)
    .configure(configurer)
    .route()
        .handler(exchange -> ...)
```

> Please refer to the [Web Server documentation](#web-server) to see in details how to properly configure Web server routes and interceptors.

Route interceptors are only applied to routes defined on a `WebInterceptedRouter` which is obtained by defining one or more route interceptor on the web router. The following example shows how it works:

```java
router
    .route()
        .path("/public")
        .handler(exchange -> {
            ...
        })
    .intercept()
        .interceptor(exchange -> {
            ...
        })
    .route()
        .path("/private")
        .handler(exchange -> {
            ...
        })
    .getRouter()
    .route()
        .path("/static/**")
        .handler(new StaticHandler<>(resourceService.getResource(URI.create("file:/path/to/web-root/"))));
```

In the preceding example, only `/private` route is intercepted, both `/public` and `/static/**` routes are not intercepted since they are defined on the original Web router which is not intercepted. Note the call to `getRouter()` method which returns the original Web router instance and basically *rollbacks* the interceptors configuration.

A Web intercepted router can also be used to apply interceptors to all routes previously defined in a Web router.

```java
router
    .intercept()
        .method(Method.GET)
        .interceptor(exchange -> {...})
    .applyInterceptors()
```

In the previous example, all `GET` routes previsously defined in the Web router will be intercepted.

> The Web router bean specifies default Web routes and error Web routes created when the router is initialized and therefore not intercepted. You must keep in mind that they exist and if you wish to intercept them, you'll have to explicitly invoke `applyInterceptors()`.

### Error web exchange

The *web* module API extends the [server exchange API](#http-server-api) defined in the *http-server* module. It defines the server `WebExchange` composed of a `WebRequest`/`WebResponse` pair in a HTTP communication between a client and a server. These interfaces respectively extends the `Exchange`, `Request` and `Response` interfaces defined in the *http-server* module. A web exchange handler (i.e. `ExchangeHandler<ExchangeContext, WebExchange<ExchangeContext>>`) is typically attached to one or more Web routes defined in a `WebRouter`.

The Error Web exchange provides additional functionnalities on top of the exchange such as path parameters and response body encoding based on the content type.

As the `WebExchange`, the `ErrorWebExchange` exposes a `WebResponse` which supports automatic response payload encoding based on the content type specified in the response headers. The usage is exactly the same as for the Web server exchange [response body encoder](#response-body-encoder).

The following error Web route matches `IllegalArgumentException` errors for client accepting `application/json` media type in the response:

```java
ExchangeHandler<ExchangeContext, ErrorWebExchange<ExchangeContext>> errorHandler = errorExchange -> {
    errorExchange.response()
        .headers(headers -> headers.status(Status.INTERNAL_SERVER_ERROR))
        .body()
        .encoder(Message.class)
        .value(new Message(errorExchange.getError().getMessage()));
};
```

### Error Web route

An Error Web route specifies the routing rules and the error exchange handler that shall be invoked to handle a matching error exchange. Similar to a [Web route](#web-route), it can combine the following routing rules which are matched in that order: the type of error, the path of the request, the media ranges and language ranges accepted by the client.

The `ErrorWebRoutable` interface defines a fluent API for the definition of Error Web routes. The following is an example of the definition of an Error Web route which matches `IllegalArgumentException` errors for client accepting `application/json` media type:

```java
errorRoutable
    .route()
        .error(IllegalArgumentException.class)
        .produces(MediaTypes.APPLICATION_JSON)
        .handler(errorExchange -> 
            errorExchange.response()
                .body()
                .encoder(Message.class)
                .value(new Message("IllegalArgumentException"))
        );
```

As with a Web routable, the Error Web routable allows to select routes matching specific rules defined in an `ErrorWebRouteManager` and enable, disable or remove specific routes.

The following example disable all routes matching `SomeCustomException` error type:

```java
errorRoutable
    .route()
        .error(SomeCustomException.class)
        .disable();
```

#### Error type routing rule

The error type routing rule matches error exchanges whose error is of a particular type.

For instance, in order to handle all error exchanges whose error is an instance of `SomeCustomException`, we can do:

```java
errorRoutable
    .route()
        .error(SomeCustomException.class)
        .handler(exchange -> {
            ...
        });
```

#### Produce routing rule

The produce routing rule, when applied to an error route behaves exactly the same as for a [Web route](#produce-routing-rule). It allows to define error handlers that produce responses of different types based on the set of media range accepted by the client.

This is particularly useful to returned specific error responses to a particular client in a particular context. For instance, a backend application might want to receive errors in a parseable format like `application/json` whereas a Web browser might want to receive errors in a human readable format like `text/html`.

#### Language routing rule

The language routing rule, when applied to an error route behaves exactly the same as for a [Web route](#language-routing-rule). It allows to define error handlers that produce responses with different languages based on the set of language range accepted by the client fallbacking to the default route when content negotiation did not give any match.

### Error Web route interceptor

Error Web routes can be intercepted in a similar way as for [Web route](#web-route-interceptor) by combining the same rules as for the definition of an Error Web route.

Multiple Error Web exchange interceptors (i.e. `ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>`) can be applied to one or more Error Web routes.

The `ErrorWebInterceptable` interface defines a fluent API similar to the `ErrorWebRoutable` for the definition of Error Web interceptors. The following is an example of the definition of an Error Web route interceptor for intercepting Error Web exchange with `SomeCustomException` errors and `/some_path` path:

̀```java
errorInterceptable
    .intercept()
        .path("/some_path")
        .error(SomeCustomException.class)
        .interceptor(errorExchange -> {
            ...
        });
̀```

As for `WebRoute`, the `ErrorWebRoute` allows to list the Error interceptors applied to an Error route and explicitly set interceptors:

```java
// Use an ErrorWebRoutable to find an ErrorWebRoute
ErrorWebRoute<ExchangeContext> errorRoute = ...

List<ExchangeInterceptor<ExchangeContext, ErrorWebExchange<ExchangeContext>>> errorRouteInterceptors = new ArrayList<>(errorRoute.getInterceptors());
errorRouteInterceptors.add(errorExchange -> {
    ...
});

errorRoute.setInterceptors(errorRouteInterceptors);
```

The `ErrorWebInterceptable` offers the same features as the `WebInterceptable` and allows configuring error interceptors as blocks in reusable `ErrorWebInterceptorsConfigurer` by invoking `configureInterceptors()` methods:

```java
ErrorWebInterceptorsConfigurer<ExchangeContext> public_error_interceptors_configurer = errInterceptable -> {
    errInterceptable
        .intercept()
        ...
};

ErrorWebInterceptorsConfigurer<ExchangeContext> private_error_interceptors_configurer = errInterceptable -> {
    errInterceptable
        .intercept()
        ...
};

errorInterceptable
    .configureInterceptors(public_error_interceptors_configurer)
    .configureInterceptors(private_error_interceptors_configurer)
    .intercept()
    ...
```

### Error Web router

The `ErrorWebRouter` extends both `ErrorWebRoutable` and `ErrorWebInterceptable` interfaces. As such Error routes and Error route interceptors are defined in the `ErrorWebRouter` bean exposed in the *web* module and used in the web server controller to handle Error Web exchange. This internal web server controller is wired to the *http-server* module to override the default HTTP server controller. 

Just like the `WebRouter` interface, the `ErrorWebRouter` exposes the `configure()` method which accepts `ErrorWebRouterConfigurer` to fluently apply blocks of configuration. The same configuration rules as for the [Web router](#web-router) applies:

```java
ErrorWebRouter<ExchangeContext> errorRouter = ...

ErrorWebRouterConfigurer<ExchangeContext> configurer = ...
List<ErrorWebRouterConfigurer<ExchangeContext>> configurers = ...

router
    .configure(configurers)
    .configure(configurer)
    .intercept()
        .interceptor(errorExchange -> {
            ...
        })
    .applyInterceptors() // Apply interceptor to previously defined Error routes
    .route()
        .path("/intercepted")
        .handler(exchange -> {
            ...
        })
    .getRouter()
    .route()
        .path("/not_intercepted")
        .handler(exchange -> {
            ...
        });
```

> Please refer to the [Web Server documentation](#web-server) to see in details how to properly configure Web server error routes and interceptors.

## Web Server

The *web* module composes the *http-server* module and as a result it requires a `NetService` and a `ResourceService`. A set of [media type converters](#media-type-converter) is also required for message payload conversion. All these services are provided by the *boot* module, so one way to create an application with a Web server is to create an Inverno module composing *boot* and *web* modules.

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_web {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.web;
}
```

The resulting *app_web* module, thus created, can then be started as an application as follows:

```java
package io.inverno.example.app_web;

import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App_web.Builder()).run();
    }
}
```

The above example starts a Web server using default configuration which is a HTTP/1.x server with a Web router as root handler and an error router as error handler.

```plaintext
2021-04-14 11:00:18,308 INFO  [main] i.w.c.v.Application - Inverno is starting...


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
     ║ Java version        : 16+36-2231                                                           ║
     ║ Java home           : /home/jkuhn/Devel/jdk/jdk-16                                         ║
     ║                                                                                            ║
     ║ Application module  : io.inverno.example.app_web                                           ║
     ║ Application version : 1.0.0-SNAPSHOT                                                       ║
     ║ Application class   : io.inverno.example.app_web.Main                                      ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ...                                                                                       ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


2021-04-14 11:00:18,313 INFO  [main] i.w.e.a.App_web - Starting Module io.inverno.example.app_web...
2021-04-14 11:00:18,313 INFO  [main] i.w.m.b.Boot - Starting Module io.inverno.mod.boot...
2021-04-14 11:00:18,494 INFO  [main] i.w.m.b.Boot - Module io.inverno.mod.boot started in 181ms
2021-04-14 11:00:18,494 INFO  [main] i.w.m.w.Web - Starting Module io.inverno.mod.web...
2021-04-14 11:00:18,495 INFO  [main] i.w.m.h.s.Server - Starting Module io.inverno.mod.http.server...
2021-04-14 11:00:18,495 INFO  [main] i.w.m.h.b.Base - Starting Module io.inverno.mod.http.base...
2021-04-14 11:00:18,499 INFO  [main] i.w.m.h.b.Base - Module io.inverno.mod.http.base started in 4ms
2021-04-14 11:00:18,570 INFO  [main] i.w.m.h.s.i.HttpServer - HTTP Server (nio) listening on http://0.0.0.0:8080
2021-04-14 11:00:18,570 INFO  [main] i.w.m.h.s.Server - Module io.inverno.mod.http.server started in 75ms
2021-04-14 11:00:18,571 INFO  [main] i.w.m.w.Web - Module io.inverno.mod.web started in 76ms
2021-04-14 11:00:18,571 INFO  [main] i.w.e.a.App_web - Module io.inverno.example.app_web started in 259ms
```

The Web router doesn't define any routes by default so if we hit the server, a (404) not found error is returned showing the default error Web router in action:

```plaintext
$ curl -i -H 'accept: application/json' http://locahost:8080
HTTP/1.1 404 Not Found
content-type: application/json
content-length: 47

{"status":"404","path":"/","error":"Not Found"}
```

Now if you open `http://locahost:8080` in a Web browser, you should see the following (404) whitelabel error page:

<img class="shadow mb-4" src="doc/img/404_whitelabel.png" alt="HTTP 404 whitelabel error page"/>

### Configuration

The Web server configuration is done in the the *web* module configuration `WebConfiguration` which includes the *http-server* module configuration `HttpServerConfiguration`. As for the *http-server* module, the net service configuration can also be considered to set low level network configuration in the *boot* module. 

Let's create the following configuration in the *app_web* module:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.boot.BootConfiguration;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.web.WebConfiguration;

@Configuration
public interface App_webConfiguration {

    @NestedBean
    BootConfiguration boot();

    @NestedBean
    WebConfiguration web();
}
```

The Web server can then be configured. For instance, we can enable HTTP/2 over cleartext, TLS, HTTP compression... as described in the [http-server module documentation](#http-server).

```java
package io.inverno.example.app_web;

import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App_web.Builder()
            .setApp_webConfiguration(
                    App_webConfigurationLoader.load(configuration -> configuration
                        .web(web -> web
                            .http_server(server -> server
                                .server_port(8081)
                                .h2c_enabled(true)
                                .server_event_loop_group_size(4)
                            )
                        )
                    )
                )
        ).run();
    }
}
```

### Configuring the Web server controller

As explained before, the module specifies a `ServerController` bean as defined by the [*http-server* module](#http-server) and wired to the HTTP server overriding the default server controller. It is composed of the Web router and the Error Web router beans which respectively route exchanges and error exchanges to the right handlers.

The Web server controller bean is private, its Web router and Error Web router are configured by defining a single `WebServerControllerConfigurer` bean. The `WebServerControllerConfigurer` interface extends both `WebRouterConfigurer` and `ErrorWebRouterConfigurer` and specifies a `createContext()` method used to initialize the exchange context as specified in [http-server module documentation](#exchange-context). The Web server controller configurer is responsible for configuring routes in the Web server. It is invoked after default routes have been initiliazed but it doesn't replace them, they can however be overridden by defining routes matching the same rules.

#### Web configurers

In a complex application with many route definitions sometimes dispatched into multiple modules and using complex interceptor setup, having a single configuration might not always be ideal and we should prefer defining multiple consistent configurers later aggregated into one Web server controller configurer bean. Following [Web routing API documentation](#web-routing-api), we know routes and interceptors can be configured using a combination of `WebRoutesConfigurer`, `WebInterceptorsConfigurer`, `WebRouterConfigurer`, `ErrorWebRoutesConfigurer`, `ErrorWebInterceptorsConfigurer` or `ErrorWebRouterConfigurer` beans. At compile time, the Inverno Web compiler plugin will then automatically generates a `WebServerControllerConfigurer` bean that aggregates all these beans into one single configuration. This way we don't have to create a Web server controller configurer bean and we can compose with above configurers which offer more flexibility, particularly in relation to the exchange context.

For instance, the Web router and the error Web router can be configured into separate configurer beans in the *app_web* module as follows:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class App_webWebRouterConfigurer implements WebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(WebRouter<ExchangeContext> router) {
        router
            .route()
                .path("/hello")
                .produces(MediaTypes.TEXT_PLAIN)
                .language("en-US")
                .handler(exchange -> exchange
                    .response()
                        .body()
                        .encoder(String.class)
                        .value("Hello!")
                )
            .route()
                .path("/hello")
                .produces(MediaTypes.TEXT_PLAIN)
                .language("fr-FR")
                .handler(exchange -> exchange
                    .response()
                        .body()
                        .encoder(String.class)
                        .value("Bonjour!")
                )
            .route()
                .path("/custom_exception")
                .handler(exchange -> {
                    throw new SomeCustomException();
                });
    }
}
```

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.UnauthorizedException;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.ErrorWebRouter;
import io.inverno.mod.web.ErrorWebRouterConfigurer;
import reactor.core.publisher.Mono;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class App_webErrorWebRouterConfigurer implements ErrorWebRouterConfigurer<ExchangeContext> {

    @Override
    public void configure(ErrorWebRouter<ExchangeContext> errorRouter) {
        errorRouter
            .route()
                .error(SomeCustomException.class)
                .handler(errorExchange -> errorExchange
                    .response()
                    .headers(headers -> headers
                        .status(Status.BAD_REQUEST)
                        .contentType(MediaTypes.TEXT_PLAIN)
                    )
                    .body()
                    .encoder()
                    .value("A custom exception was raised")
                )
            .intercept()
                .error(UnauthorizedException.class)
                .interceptor(errorExchange -> {
                    errorExchange.response().headers(headers -> headers.add(Headers.NAME_WWW_AUTHENTICATE, "basic realm=inverno"));
                    return Mono.just(errorExchange);
                })
            // We must apply interceptors to intercept error routes defined by default in the web server module
            .applyInterceptors();
    }
}
```

After compilation, class `App_web_WebServerContollerConfigurer` aggregating the two configurer beans should have been generated and the corresponding bean wired into the Web server module.

Now we can test the application:

```plaintext
$ curl -i http://locahost:8080/
HTTP/1.1 404 Not Found
content-length: 0
```

```plaintext
$ curl -i http://locahost:8080/hello
HTTP/1.1 200 OK
content-type: text/plain
content-length: 6

Hello!
```

```plaintext
$ curl -i -H 'accept-language: fr' http://locahost:8080/hello
HTTP/1.1 200 OK
content-type: text/plain
content-length: 8

Bonjour!
```

```plaintext
$ curl -i -H 'accept: application/json' http://locahost:8080/hello
HTTP/1.1 406 Not Acceptable
content-type: application/json
content-length: 81

{"status":"406","path":"/hello","error":"Not Acceptable","accept":["text/plain"]}
```

```plaintext
$ curl -i http://locahost:8080/custom_exception
HTTP/1.1 400 Bad Request
content-type: text/plain
content-length: 29

A custom exception was raised
```

> Since Web configurers are all defined as interfaces, you can easily centralize configuration by implementing one or more configurers. For instance, previous configurers could have been defined in one single bean implementing `WebRouterConfigurer<ExchangeContext>` and `ErrorWebRouterConfigurer<ExchangeContext>`.

> Note that it is still possible to use a custom `WebServerControllerConfigurer` bean instead of the one generated by the Inverno Web compiler plugin. This basically requires to explicitly wire the custom bean into the *web* module using a `@Wire` annotation (otherwise compilation will fail indicating a dependency injection conflict as two beans can then be wired to the Web server controller configurer socket). This can be justified when there are specific needs regarding the exchange context. It is however recommended to use the generated configurer which greatly simplifies configuration.

When defining Web configurer beans, it is important to make them private inside the module in order to avoid side effects when composing the module as they may interfere with the generated server controller configurer, which already aggregates module's Web configurer beans, resulting in routes being configured twice. Compilation warnings shall be raised when a Web configurer is defined as a public bean.

Web configurers are applied by the generated Web server controller configurer in the following order starting by `WebInterceptorsConfigurer` beans, then `WebRouterConfigurer` beans and finally `WebRoutesConfigurer` beans. This basically means that the interceptors defined in `WebInterceptorsConfigurer` beans in the module will be applied to all routes defined in the module including throse provided in component modules. Although it is possible to define multiple `WebInterceptorsConfigurer` beans, it is recommended to have only one because the order in which they are injected in the Web server controller configurer is not guaranteed which might be problematic under certain circumstances.

#### Exchange context

The exchange context is global to all routes and interceptors, and basically specific to any application as it directly depends on what is expected by the routes and interceptors. Considering a complex application, this can quickly become very tricky. A safe approach would be to define a single global context type for the whole application and use it in all routes and interceptors definitions. Unfortunately we might have to include routes provided by third party modules that can't possibly use that context type. Besides, we might not want to expose the whole context to every routes and interceptors. The exchange context is unique and therefore necessarily global but ideally it should be possible to define different context types corresponding to the routes being defined. For instance, a secured route might require some kind of security context unlike a public route.

The exchange context is provided by the Web server controller which basically delegates to the `createContext()` method of the Web server controller configurer. Since it is generated by the Inverno Web compiler plugin, the plugin must also generate the global context based on the routes and interceptors definitions aggregated in the generated `WebServerControllerConfigurer` bean.

> The fact that the *web* module only accepts one Web server controller configurer guarantees that there will be only one context provider.

Let's consider the case of an application which defines routes and interceptors that can use different exchange context depending on their functional area. For instance, we can imagine an application exposing front office and back office services using `FrontOfficeContext` and `BackOfficeContext` respectively.

Front office routes are then defined to handle exchanges exposing the `FrontOfficeContext` and back office routes, that may be specified in a completely different module, are defined to handle exchanges exposing the `BackOfficeContext`.

Let's start by defining these contexts and see how the global context is generated by the Inverno Web compiler plugin. 

Exchange contexts must be defined as interfaces extending `ExchangeContext`:

```java
package io.inverno.example.app_web.test;

import io.inverno.mod.http.server.ExchangeContext;

public interface FrontOfficeContext extends ExchangeContext {
    
    void setMarket(String market);
    
    String getMarket();
}
```

```java
package io.inverno.example.app_web.test;

import io.inverno.mod.http.server.ExchangeContext;

public interface BackOfficeContext extends ExchangeContext {

    void setVar(double var);
    
    double getVar();
}
```

Then we can define different beans to configure front office and back office routers:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.WebInterceptable;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import io.inverno.mod.web.WebRoutable;
import io.inverno.mod.web.WebRoutesConfigurer;
import reactor.core.publisher.Mono;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class FrontOfficeRouterConfigurer implements WebRoutesConfigurer<FrontOfficeContext>, WebInterceptorsConfigurer<FrontOfficeContext>  {

    @Override
    public void configure(WebRoutable<FrontOfficeContext, ?> routes) {
        routes
            .route()
                .path("/frontOffice")
                .method(Method.GET)
                .handler(exchange -> {
                    exchange.response()
                        .headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))	
                        .body().string().value("I've done some stuff on market: " + exchange.context().getMarket());
                });
    }

    @Override
    public void configure(WebInterceptable<FrontOfficeContext, ?> interceptors) {
        interceptors
            .intercept()
                .path("/frontOffice/**")
                .interceptor(exchange -> {
                    // Resolve the market from the request, session or else
                    exchange.context().setMarket("market");
                    return Mono.just(exchange);
                });
    }
}
```

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.WebInterceptable;
import io.inverno.mod.web.WebInterceptorsConfigurer;
import io.inverno.mod.web.WebRoutable;
import io.inverno.mod.web.WebRoutesConfigurer;
import reactor.core.publisher.Mono;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class BackOfficeRouterConfigurer implements WebRoutesConfigurer<BackOfficeContext>, WebInterceptorsConfigurer<BackOfficeContext> {
    
    @Override
    public void configure(WebRoutable<BackOfficeContext, ?> routes) {
        routes
            .route()
                .path("/backOffice")
                .method(Method.GET)
                .handler(exchange -> {
                    exchange.response()
                        .headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))	
                        .body().string().value("VaR is: " + exchange.context().getVar());
                });
    }

    @Override
    public void configure(WebInterceptable<BackOfficeContext, ?> interceptors) {
        interceptors
            .intercept()
                .path("/backOffice/**")
                .interceptor(exchange -> {
                    // Resolve the VaR from the request, session or else
                    exchange.context().setVar(1234.5678);				
                    return Mono.just(exchange);
                });
    }
}
```

Now if we compile the module, the Inverno Web compiler plugin generates interface `App_web_WebServerContollerConfigurer.Context` inside the generated `App_web_WebServerContollerConfigurer` which extends all context types encountered while aggregating the configurer beans. It will also implement method `createContext()` in order to return a concrete implementation of the context: 

- Getter and setter methods (i.e. `T get*()` and `void set*(T value)` methods) are implemented in order be able to set and get data on the context as shown in above examples.
- Other methods with no default implementation gets a blank implementation (i.e. no-op).

If we open the generated `App_web_WebServerContollerConfigurer` we should see:

```java
...
@Override
public Context createContext() {
    return new Context() {
        private String market;
        private double var;

        @Override
        public String getMarket() {
            return this.market;
        }

        @Override
        public void setMarket(String market) {
            this.market = market;
        }

        @Override
        public double getVar() {
            return this.var;
        }

        @Override
        public void setVar(double var) {
            this.var = var;
        }
    };
}

public static interface Context extends BackOfficeContext, FrontOfficeContext, ExchangeContext {}
...
```

Using such generated context guarantees that the context created by the Web server controller complies with what is expected by route handlers and interceptors. This allows to safely compose mutliple Web modules in an application, developped by separate teams and using different context types.

This doesn't come without limitations. For instance, exchange context must be defined as interfaces since multiple inheritance is not supported in Java. If you try to use a class, a compilation error will be raised.

Another limitation comes from the fact that it might difficult to define a route that uses many context types, using configurers the only way to achieve this is to create an intermediary interface that extends the required context types. Although this is acceptable, it is not ideal semantically speaking. Hopefully this issue can be mitigated, at least for route definition, when routes are defined in a declarative way in a [Web controller](#web-controller) which allows to specify context type using intersection types on the route method (e.g. `<T extends FrontOfficeContext & BackOfficeContext>`).

Finally, the Inverno Web compiler plugin only generates concrete implementations for getter and setter methods which might seem simplistic but actual logic can still be provided using default implementations in the context interface. For example, role based access control can be implemented in a security context as follows:

```java
package io.inverno.example.app_web;

import io.inverno.mod.http.server.ExchangeContext;
import java.util.Set;

public interface SecurityContext extends ExchangeContext {

    void setRoles(Set<String> roles);
    
    Set<String> getRoles();
    
    default boolean hasRole(String role) {
        return this.getRoles().contains(role);
    }
}
```

Exposing `setRoles()` methods to actual services which should only be concerned by controlling access might not be ideal. There are two concerns to consider here: first resolving the roles of the authenticated user and set them into the context which is the responsability of a security interceptor and then controlling the access to a secured service or resource which is the responsability of a service or another security interceptor. Since we can compose multiple configurers using multiple context types automatically aggregated into one server controller configurer we can easily solve that issue by splitting previous security context:

```java
package io.inverno.example.app_web;

import io.inverno.mod.http.server.ExchangeContext;
import java.util.Set;

public interface SecurityContext extends ExchangeContext {

    Set<String> getRoles();
    
    default boolean hasRole(String role) {
        return this.getRoles().contains(role);
    }
}
```

```java
package io.inverno.example.app_web;

import java.util.Set;

public interface ConfigurableSecurityContext extends SecurityContext {

    void setRoles(Set<String> roles);
}
```

Particular care must be taken when declaring context types with generics (e.g. `Context<A>`), we must always make sure that for a given erased type (e.g. `Context`) there is one type that is assignable to all others which will then be retained during the context type generation. This basically follows Java language specification which prevents from implementing the same interface twice with different arguments as a result the generated context can only implement one which must obviously be assignable to all others. A compilation error shall be reported if inconsistent exchange context types have been defined.

> In order to avoid any misuse and realize the benefits of the context generation, it is important to understand the purpose of the exchange context and why we choose to have it strongly typed. 
>
> The exchange context is mainly used to propagate contextual information across the routing chain composed by interceptors and the exchange handler, it is not necessarily meant to expose any logic. 
>
> Unlike many other frameworks which use untyped map, the exchange context is strongly typed which has many advantages: 
> 
> - static checking can be performed by the compiler,
> - an handler or an interceptor have guarantees over the information exposed in the context (`ClassCastException` are basically impossible),
> - as we just saw it is also possible to expose some logic using default interface methods,
> - actual services can be exposed right away in the context without having to use error prone string keys or explicit cast.
>
> The generation of the context by the Inverno Web compiler plugin is here to reduce the complexity induced by strong typing as long as above rules are respected.

### Static handler

The `StaticHandler` is a built-in exchange handler that can be used to define routes for serving static resources resolved with the [Resource API](#resource-api).

For instance, we can create a route to serve files stored in a `web-root` directory as follows:

```java
router
    .route()
        .path("/static/{path:.*}")                                   // 1
        .handler(new StaticHandler<>(new FileResource("web-root/"))) // 2
```

1. The path must be parameterized with a `path` parameter which can include `/`, for the static handler to be able to determine the relative path of the resource in the `web-root` directory
2. The base resource is defined directly as a `FileResource`, although it is also possible to use a `ResourceService` to be more flexible in terms of the kind of resource

The static handler relies on the resource abstraction to resolve resources, as a result, these can be located on the file system, on the class path, on the module path...

The static handler also looks for a welcome page when a directory resource is requested. For instance considering the following `web-root` directory:

```plaintext
web-root/
├── index.html
└── snowflake.svg
```

A request to `http://127.0.0.1/static/` would return the `index.html` file.

### 100-continue interceptor

The `ContinueInterceptor` class which can be used to automatically handles `100-continue` as defined by [RFC 7231 Section 5.1.1][rfc-7231-5.1.1].

```java
router
    .intercept()
        .interceptor(new ContinueInterceptor())
    .route()
    ...
```

> Note that in order to comply with RFC 7231, an HTTP server must respond with a (100) status to a request with a 100-continue expectation. The `ContinueInterceptor` allows to automatize this, otherwise it must be done explicitly:
>
> ```java
> ...
> if(exchange.request().headers().contains(Headers.NAME_EXPECT, Headers.VALUE_100_CONTINUE)) {
>     exchange.response().sendContinue();
> }
> ...
> ```

### WebJars

The `WebJarsRoutesConfigurer` is a `WebRoutesConfigurer` implementation used to configure routes to WebJars static resources available on the module path or class path. Paths to the resources are version agnostic: `/webjars/{webjar_module}/{path:.*}` where `{webjar_module}` corresponds to the *modularized* name of the WebJar minus `org.webjars`. For example the location of the Swagger UI WebJar would be `/webjars/swagger.ui/`.

The `WebJarsRoutesConfigurer` requires a `ResourceService` to resolve WebJars resources. WebJars routes can be configured as follows:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.WebJarsRoutesConfigurer;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;

@Bean
public class App_webWebRouterConfigurer implements WebRouterConfigurer<ExchangeContext> {

    private final ResourceService resourceService;

    public App_webWebRouterConfigurer(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void accept(WebRouter<ExchangeContext> router) {
        router
            .configureRoutes(new WebJarsRoutesConfigurer<>(this.resourceService))
            ...
    }
}
```

Then we can declare WebJars dependencies such as the Swagger UI in the build descriptor:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>org.webjars</groupId>
            <artifactId>swagger-ui</artifactId>
        </dependency>
    </dependencies>
</project>
```

The Swagger UI should be accessible at `http://locahost:8080/webjars/swagger.ui/`.

Sadly WebJars are rarely modular JARs, they are not even named modules which causes several issues when dependencies are specified on the module path. That's why when an application is run or packaged using [Inverno tools][inverno-tools-root], such dependencies and WebJars in particular are *modularized*. A WebJar such as `swagger-ui` is modularized into `org.webjars.swagger.ui` module which explains why it is referred to by its module name: `swagger.ui` in the WebJars resource path (the `org.webjars` part is omitted since the context is known).

When running a fully modular Inverno application, *modularized* WebJars modules must be added explicitly to the JVM using the `--add-modules` option, otherwise they are not resolved when the JVM starts. For instance:

```plaintext
$ java --add-modules org.webjars.swagger.ui ...
```

Hopefully, the Inverno Maven plugin adds unnamed modules by default when running or packaging an application, so you shouldn't have to worry about it. The following command automatically adds the unnamed modules when running the JVM:

```plaintext
$ mvn inverno:run
```

This can be disabled in order to manually control which modules should be added:

```plaintext
$ mvn inverno:run -Dinverno.exec.addUnnamedModules=false -Dinverno.exec.vmOptions="--add-modules org.webjars.swagger.ui"
```

> It might also be possible to define the dependency in the module descriptor, unfortunately since WebJars modules are unnamed, they are named after the name of the JAR file which is greatly unstable and can't be trusted, so previous approach is by far the safest. If you need to create a WebJar you should make it a named module with the `Automatic-Module-Name` attribute sets to `org.webjars.{webjar_module}` in the manifest file and with resources located under `META-INF/resources/webjars/{webjar_module}/{webjar_version}/`.

Note that when the application is run with non-modular WebJars specified on the class path, they can be accessed without any particular configuration as part of the UNNAMED module using the same path notation.

### OpenAPI specification

The `OpenApiRoutesConfigurer` is a `WebRoutesConfigurer` implementation used to configure routes to [OpenAPI specifications][open-api] defined in `/META-INF/inverno/web/openapi.yml` resources in application modules.

OpenAPI routes can be configured on the Web router as follows:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.web.OpenApiRoutesConfigurer;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;

@Bean
public class App_webWebRouterConfigurer implements WebRouterConfigurer<ExchangeContext> {

    private final ResourceService resourceService;

    public App_webWebRouterConfigurer(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public void accept(WebRouter<ExchangeContext> router) {
        router
            .configureRoutes(new OpenApiRoutesConfigurer<>(this.resourceService))
            ...
    }
}
```

The configurer will scan for OpenAPI specifications files `/META-INF/inverno/web/openapi.yml` in the application modules and configure the following routes:

- `/open-api` returning the list of available OpenAPI specifications in `application/json`
- `/open-api/{moduleName}` returning the OpenAPI specifications defined for the specified module name or (404) not found error if there is no OpenAPI specification defined in the module or no module with that name.

By default the configurer also configures these routes to display OpenAPI specifications in a [Swagger UI][swagger-ui] when accessed from a Web browser (ie. with `accept: text/html`) assuming the Swagger UI WebJar dependency is present:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>org.webjars</groupId>
            <artifactId>swagger-ui</artifactId>
        </dependency>
    </dependencies>
</project>
```

Swagger UI support can be disabled from the `OpenApiRoutesConfigurer` constructor:

```java
router
    .configureRoutes(new OpenApiRoutesConfigurer<>(this.resourceService, false))
    ...
```

> OpenAPI specifications are usually automatically generated by the Web Inverno compiler plugin for routes defined in a [Web controller](#web-controller) but you can provide manual or generated specifications using the tool of your choice, as long as it is not conflicting with the Web compiler plugin.

## Web Controller

The [Web routing API](#web-routing-api) provides a *programmatic* way of defining the Web routes of a Web server but it also provides a set of annotations for defining Web routes in a more declarative way. 

A **Web controller** is a regular module bean annotated with `@WebController` which defines methods annotated with `@WebRoute` describing Web routes. These beans are scanned at compile time by the Inverno Web compiler plugin in order to include corresponding *programmatic* configuration in the generated Web server controller configurer.

For instance, in order to create a book resource with basic CRUD operations, we can start by defining a `Book` model in a dedicated `*.dto` package (we'll see later why this matters):

```java
package io.inverno.example.app_web.dto;

public class Book {

    private String isbn;
    private String title;
    private String author;
    private int pages;
    
    // Constructor, getters, setters, hashcode, equals...
}
```

Now we can define a `BookResource` Web controller as follows:

```java
package io.inverno.example.app_web;

import java.util.Set;

import io.inverno.core.annotation.Bean;
import io.inverno.example.app_web.dto.Book;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;

@Bean( visibility = Bean.Visibility.PRIVATE )                                                  // 1
@WebController( path = "/book" )                                                               // 2
public class BookResource {

    @WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_JSON )                  // 3
    public void create(@Body Book book) {                                                      // 4
        ...
    }
    
    @WebRoute( path = "/{isbn}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON )
    public void update(@PathParam String isbn, @Body Book book) {
        ...
    }
    
    @WebRoute( method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
    public Set<Book> list() {
        ...
    }
    
    @WebRoute( path = "/{isbn}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
    public Book get(@PathParam String isbn) {
        ...
    }
    
    @WebRoute( path = "/{isbn}", method = Method.DELETE )
    public void delete(@PathParam String isbn) {
        ...
    }
}
```

Implementations details have been omitted for clarity, here is what's important:

1. A Web controller must be a module bean because it will be wired into the generated Web router configurer and used to invoke the right handler method attached to a Web route. Besides this is convenient for implementation as it allows a repository to be wired into the `BookResource` bean for instance.
2. The `@WebController` annotation tells the Web compiler plugin to process the bean as a Web controller. The controller root path can also be specified in this annotation, if not specified it defaults to `/` which is the root path of the Web server.
3. The `@WebRoute` annotation on a method tells the Web compiler plugin to define a route whose handler should invoke that method. The set of routing rules (ie. path, method, consume, produce, language) describing the route can all be specified in the annotation.
4. Request Parameters and body are specified as method parameters annotated with `@CookieParam`, `@FormParam`, `@HeaderParam`, `@PathParam`, `@QueryParam` and `@Body` annotations.

Some other contextual objects like the underlying `WebExchange` or the exchange context can also be injected in the Web controller method.

Assuming we have provided proper implementations to create, update, list, get and delete a book in a data store, we can compile the module. The generated Web server controller configurer bean should configure the routes corresponding to the Web controller's annotated methods in the Web router. The generated class uses the same APIs described before, it is perfectly readable and debuggable and above all it eliminates the overhead of resolving Web controllers or Web routes at runtime.

Now let's go back to the `Book` DTO, we said earlier that it must be created in a dedicated package, the reason is actually quite simple. Since above routes consume and produce `application/json` payloads, the `application/json` media type converter will be invoked to convert `Book` objects from/to JSON data. This converter uses an `ObjectMapper` object from module `com.fasterxml.jackson.databind` which uses reflection to instantiate the objects and populate them from a parsed JSON tree. Unfortunately or hopefully the Java modular system prevents unauthorized reflective access and as a result the `ObjectMapper` can't access the `Book` class unless we explicitly export the package containing DTOs to module `com.fasterxml.jackson.databind` in the module descriptor as follows:

```java
module io.inverno.example.app_web {
    ...    
    exports io.inverno.example.app_web.dto to com.fasterxml.jackson.databind;
}
```

Using a dedicated package for DTOs allows then to limit and control the access to the module classes.

> If you're not familiar with the Java modular system and used to Java 8<, you might find this a bit distressing but if you want to better structure and secure your applications, this is the way. 

We can now run the application and test the book resource:

```plaintext
$ curl -i http://localhost:8080/book
HTTP/1.1 200 OK
content-type: application/json
content-length: 2

[]
```
```plaintext
$ curl -i -X POST -H 'content-type: application/json' -d '{"isbn":"978-0132143011","title":"Distributed Systems: Concepts and Design","author":"George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair","pages":1080}' http://localhost:8080/book
HTTP/1.1 200 OK
content-length: 0

```
```plaintext
$ curl -i http://localhost:8080/book
HTTP/1.1 200 OK
content-type: application/json
content-length: 163

[{"isbn":"978-0132143011","title":"Distributed Systems: Concepts and Design","author":"George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair","pages":1080}]
```
```plaintext
$ curl -i http://localhost:8080/book/978-0132143011
HTTP/1.1 200 OK
content-type: application/json
content-length: 161

{"isbn":"978-0132143011","title":"Distributed Systems: Concepts and Design","author":"George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair","pages":1080}
```

It is possible to separate the API from the implementation by defining the Web controller and the Web routes in an interface implemented in a module bean. For instance:

```java
package io.inverno.example.app_web;

import io.inverno.example.app_web.dto.Book;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;
import java.util.Set;

@WebController( path = "/book" )
public interface BookResource {

    @WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_JSON )
    void create(@Body Book book);
    
    @WebRoute( path = "/{isbn}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON )
    void update(@PathParam String isbn, @Body Book book);
    
    @WebRoute( method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
    Set<Book> list();
    
    @WebRoute( path = "/{isbn}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON )
    Book get(@PathParam String isbn);
    
    @WebRoute( path = "/{isbn}", method = Method.DELETE )
    void delete(@PathParam String isbn);
}
```

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.example.app_web.dto.Book;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Bean( visibility = Bean.Visibility.PRIVATE )
public class BookResourceImpl implements BookResource {

    @Override
    public void create(@Body Book book) {
        ...
    }
    
    @Override
    public void update(@PathParam String isbn, @Body Book book) {
        ...
    }
    
    @Override
    public Set<Book> list() {
        ...
    }
    
    @Override
    public Book get(@PathParam String isbn) {
        ...
    }
    
    @Override
    public void delete(@PathParam String isbn) {
        ...
    }
}
```

This provides better modularity and allows defining the API in a dedicated module which can later be used to implement various server and/or client implementations in different modules. Another advantage is that it allows to split a Web controller interface into multiple interfaces.

Generics are also supported, we can for instance create the following generic `CRUD<T>` interface:

```java
package io.inverno.example.app_web;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.WebRoute;
import java.util.Set;

public interface CRUD<T> {

    @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
    void create(@Body T resource);
    
    @WebRoute(path = "/{id}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON)
    void update(@PathParam String id, @Body T resource);
    
    @WebRoute(method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    Set<T> list();
    
    @WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    T get(@PathParam String id);
    
    @WebRoute(path = "/{id}", method = Method.DELETE)
    void delete(@PathParam String id);
}
```

And then create multiple specific resources using that interface:

```java
package io.inverno.example.app_web;

import io.inverno.example.app_web.dto.Book;
import io.inverno.mod.web.annotation.WebController;

@WebController(path = "/book")
public interface BookResource extends CRUD<Book> {

}
```

The book resource as we defined it doesn't seem very reactive, this statement is both true and untrue: the API and the Web server are fully reactive, as a result Web routes declared in the book resource Web controller are configured using a reactive API in the generated Web server controller configurer, nonetheless the methods in the Web controller are not reactive.

Luckily, we can easily transform previous interface and make it fully reactive:

```java
package io.inverno.example.app_web;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.WebRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CRUD<T> {

    @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
    Mono<Void> create(@Body Mono<T> resource);
    
    @WebRoute(path = "/{id}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON)
    Mono<Void> update(@PathParam String id, @Body Mono<T> resource);
    
    @WebRoute(method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    Flux<T> list();
    
    @WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    Mono<T> get(@PathParam String id);
    
    @WebRoute(path = "/{id}", method = Method.DELETE)
    Mono<Void> delete(@PathParam String id);
}
```

There is one remaining thing to do to make the book resource a proper REST resource. When creating a book we must return a 201 Created HTTP code with a `location` header as defined by [RFC7231 Section 7.1.2][rfc-7231-7.1.2]. This can be done by injecting the `WebExchange` directly in the `create()` method:

```java
public interface CRUD<T> {

    @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON)
    Mono<Void> create(@Body Mono<T> resource, WebExchange<?> exchange);
    ...
}
```

We can then do the following in the book resource implementation to set the status and `location` header:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.example.app_web.dto.Book;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.web.WebExchange;
import reactor.core.publisher.Mono;

@Bean
public class BookResourceImpl implements BookResource {

    @Override
    public Mono<Void> create(Mono<Book> book, WebExchange<?> exchange) {
        ...
        exchange.response().headers(headers -> headers
            .status(Status.CREATED)
            .add(Headers.NAME_LOCATION, exchange.request().getPathBuilder().segment(b.getIsbn()).buildPath())
        );
        ...
    }
    ...
}
```

Now if we run the application and create a book resource we should get the following:

```plaintext
$ curl -i -X POST -H 'content-type: application/json' -d '{"isbn":"978-0132143011","title":"Distributed Systems: Concepts and Design","author":"George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair","pages":1080}' http://locahost:8080/book
HTTP/1.1 201 Created
content-type: application/json
location: /book/978-0132143012
content-length: 0
```

Declarative routes are configured last in the generated Web server controller configurer which means they override any route prevously defined in a Web configurer but above all they are intercepted by the interceptors defined in `WebInterceptorsConfigurer` beans in the module.

### Declarative Web route

So far, we have described a concrete Web controller use case which should already give a good idea on how to configure route in a declarative way. Now, let's examine in details how a Web route is declared in a Web controller.

A Web route or HTTP endpoint or REST endpoint... in short an HTTP request/response exchange is essentially defined by:

- An input, basically an HTTP request characterized by the following components: path, method, query parameters, headers, cookies, path parameters, request body.
- A normal output, basically a successful HTTP response and more precisely: a status (2xx or 3xx), headers and a response body.
- A set of error outputs, basically unsuccessful HTTP responses and more precisely: a status (4xx or 5xx), headers and a response body.

Web routes are defined as methods in a Web controller which match this definition: the Web route input is defined as method parameters, the Web route normal output is defined by the return type of the method and finally the exceptions thrown by the method define the Web route error outputs.

It then remains to bind the Web route semantic to the method, this is done using various annotations on the method and its parameters.

#### Routing rules

Routing rules, as defined in the [Web routing API](#web-route), are specified in a single `@WebRoute` annotation on a Web controller method. It allows to define paths, methods, consumed media ranges, produced media types and produced languages of the Web routes that route a matching request to the handler implemented by the method.

For instance, we can define multiple paths and/or multiple produced media types in order to expose a resource at different locations in various formats:

```java
@WebRoute( path = { "/book/current", "/book/v1" }, produces = { MediaTypes.APPLICATION_JSON, MediaTypes.APPLICATION_XML } )
Flux<T> list();
```

The `matchTrailingSlash` parameter can be used to indicate that the defined paths should be matched taking the trailing slash into account or not.

> Note that this exactly corresponds to the [Web routing API](#web-routing-api).

#### Parameter bindings

As stated above, a `@WebRoute` annotated method must be bound to a Web exchange. In particular, method parameters are bound to the various elements of the request using `@*Param` annotations defined in the Web routing API.

Such parameters can be of any type, as long as the parameter converter plugged into the *web* module can convert it, otherwise a `ConverterException` is thrown. The default parameter converter provided in the *boot* module is able to convert primitive and common types including arrays and collections. Please refer to the [HTTP server documentation](#extending-http-services) to learn how to extend the parameter converter to convert custom types.

In the following example, the value or values of query parameter `isbns` is converted to an array of strings:

```java
@WebRoute( path = { "/book/byIsbn" }, produces = { MediaTypes.APPLICATION_JSON } )
Flux<T> getBooksByIsbn(@QueryParam String[] isbns);
```

If the above route is queried with `/book/byIsbn?isbns=978-0132143011,978-0132143012,978-0132143013&isbns=978-0132143014` the `isbns` parameter is then: `["978-0132143011", "978-0132143012", "978-0132143013", "978-0132143014"]`.

A parameter defined like this is required by default and a `MissingRequiredParameterException` is thrown if one or more parameters are missing from the request but it can be declared as optional by defining it as an `Optional<T>`:

In the following example, query parameter `limit` is optional and no exception will be thrown if it is missing from the request:

```java
@WebRoute( path = { "/book" }, produces = { MediaTypes.APPLICATION_JSON } )
Flux<T> getBooks(@QueryParam Optional<Integer> limit);
```

##### Query parameter

Query parameters are declared using the `@QueryParam` annotation as follows:

```java
@WebRoute( path = { "/book/byIsbn" }, produces = { MediaTypes.APPLICATION_JSON } )
Flux<T> getBooksByIsbn(@QueryParam String[] isbns);
```

Note that the name of the method parameter actually defines the name of the query parameter.

> This contrasts with other RESTful API, such as JAX-RS, which requires to specify the parameter name, again, in the annotation. Since the Inverno Web compiler plugin works at compile time, it has access to actual method parameter names defined in the source.

##### Path parameter

Path parameters are declared using the `@PathParam` annotation as follows:

```java
@WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
Mono<T> get(@PathParam String id);
```

Note that the name of the method parameter must match the name of the path parameter of the route path defined in the `@WebRoute` annotation.

##### Cookie parameter

It is possible to bind cookie values as well using the `@cookieParam` annotation as follows:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
Mono<Void> create(@CookieParam String book_store, @Body Mono<T> book, WebExchange exchange);
```

In previous example, the route must be queried with a `book_store` cookie which is not declared as optional:

```plaintext
$ curl -i -X POST -H 'cookie: book_store=store1' -H 'content-type: application/json' -d '...' http://locahost:8080/book
...
```

##### Header parameter

Header field can also be bound using the `@HeaderParam` annotation as follows:

```java
@WebRoute(method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
Flux<T> list(@HeaderParam Optional<Format> format);
```

In previous example, the `Format` type is an enumeration indicating how book references must be returned (eg. `SHORT`, `FULL`...), a `format` header may or may not be added to the request since it is declared as optional:

```plaintext
$ curl -i -H 'format: SHORT' http://locahost:8080/book
...
```

##### Form parameter

Form parameters are bound using the `@FormParam` annotation as follows:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
Mono<Void> createAuthor(
    @FormParam String forename, 
    @FormParam Optional<String> middlename, 
    @FormParam String surname, 
    @FormParam LocalDate birthdate, 
    @FormParam Optional<LocalDate> deathdate, 
    @FormParam String nationality);
```

Form parameters are sent in a request body following `application/x-www-form-urlencoded` format as defined by [living standard][form-urlencoded]. They can be sent using a HTML form submitted to the server resulting in the following request body:

```plaintext
forename=Leslie,middlename=B.,surname=Lamport,birthdate=19410207,nationality=US
```

Previous route can then be queried as follows:

```plaintext
$ curl -i -X POST -H 'content-type:application/x-www-form-urlencoded' -d 'forename=Leslie,middlename=B.,surname=Lamport,birthdate=19410207,nationality=US' http://locahost:8080/author
```

Form parameters results from the parsing of the request body and as such, `@FormParam` annotations can't be used together with `@Body` on route method parameters.

#### Contextual parameters

A contextual parameter is directly related to the context into which an exchange is processed in the route method, it can be injected in the route method by specifying a method parameter of a supported contextual parameter type.

##### WebExchange

The underlying Web exchange can be injected by specifying a method parameter of a type assignable from `WebExchange`.

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
Mono<Void> create(@Body Mono<T> resource, WebExchange<?> exchange) throws BadRequestException;
```

> The exchange gives full access to the underlying request and response. Although it allows to manipulate the request and response bodies, this might conflict with the generated Web route and as a result the exchange should only be used to access request parameters, headers, cookies... or specify a specific response status, response cookies or headers...

The Web exchange also gives access to the exchange context, if a route handler requires a particular context type, it can be specified as a type parameter as follows:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
Mono<Void> create(@Body Mono<T> resource, WebExchange<SecurityContext> exchange) throws BadRequestException;
```

Context types declared in a declarative Web route are aggregated in the Web server controller configurer by the Inverno Web compiler plugin in the same way as for Web server [configurers](#configuring-the-web-server-controller). However declarative Web routes make it possible to use interaction types when multiple context types are expected using a type variable which brings more flexibility.

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
<E extends TracingContext & SecurityContext> Mono<Void> create(@Body Mono<T> resource, WebExchange<E> exchange) throws BadRequestException;
```

When declaring generic context types, we must make sure they are all consistent (i.e. there is one type that is assignable to all others). When declaring a route using generic context type, it is then good practice to use upper bound wildcards as follows:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
Mono<Void> create(@Body Mono<T> resource, WebExchange<SecurityContext<? extends PersonIdentity, ? extends AccessController>> exchange) throws BadRequestException;
```

Previous code basically means that the route requires a `SecurityContext` with any `PersonIdentity` types and any `AccessContoller` types. This is quite different than if we defined it as `SecurityContext<PersonIdentity, AccessController>`, in the first case we can assign `SecurityContext<PersonIdentity, RoleBasedAccessController>` whereas in the second case we can only assign `SecurityContext<PersonIdentity, RoleBasedAccessController>`. Using upper bound wildcards then provides greater flexibility and more integration options: routes basically don't have to be defined using the same context type definition.

##### Exchange context

The exchange context can also be injected directly by specifying a method parameter of a type assignable from `ExchangeContext`.

```java
@WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
Mono<T> get(@PathParam String id, WebContext webContext);
```

As for the Web exchange, it is possible to specify intersection types using a type variable:

```java
@WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
<E extends WebContext & InterceptorContext> Mono<T> get(@PathParam String id, E context);
```

As before, context types declared in a declarative Web route are aggregated in the Web server controller configurer by the Inverno Web compiler plugin.

#### Request body

The request body can be bound to a route method parameter using the `@Body` annotation. Request body is automatically converted based on the media type declared in the `content-type` header field of the request as described in the [Web server exchange documentation](#request-body-decoder). The body parameter method can then be of any type as long as there is a media type converter for the media type specified in the request that can convert it.

In the following example, the request body is bound to parameter `book` of type `Book`, it is then converted from `application/json` into a `Book` instance:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
void create(@Body Book book);
```

Unlike parameters, the request body can be specified in a reactive way, the previous example can then be rewritten using a `Mono<T>`, a `Flux<T>` or more broadly a `Publisher<T>` as body parameter type as follows:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
Mono<Void> create(@Body Mono<Book> book);
```

A stream of objects can be processed when the media type converter supports it. For instance, the `application/x-ndjson` converter can emit converted objects each time a new line is encountered, this allows to process content without having to wait for the entire message resulting in better response time and reduced memory consumption. 

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_X_NDJSON)
Mono<Void> create(@Body Flux<Book> book);
```

> The `application/json` also supports such streaming capability by emitting converted objects while parsing a JSON array.

The `@Body` annotation can not be used together with the `@FormParam` annotation on route method parameters because the request body can only be consumed once.

##### Multipart form data

Multipart form data request body can be bound by defining a body parameter of type `Mono<WebPart>` if one part is expected, `Flux<WebPart>` if multiple parts are expected or more broadly of type `Publisher<WebPart>`.

We can then rewrite the example described in [Web server exchange documentation](#request-body-decoder) as follows:

```java
@WebRoute( path = "/bulk", method = Method.POST, consumes = MediaTypes.MULTIPART_FORM_DATA)
Flux<Result> createBulk(@Body Flux<WebPart> parts) {
    return parts
        .flatMap(part -> part.decoder(Book.class).one())
        .map(book -> storeBook(book));
}
```

> It is not possible to bind particular parts to a route method parameter. This design choice has been motivated by performance and resource consumption considerations. Indeed, this would require to consume and store the entire request body in memory before invoking the method. As a result, multipart data must still be handled *manually* using and processed in sequence (i.e. a part must be fully consumed before we can consume the next one).

#### Response body

The response body is specified by the return type of the route method.

```java
@WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
Book get(@PathParam String id);
```

As for the request body, the response body can be reactive if specified as a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>`:

```java
@WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
Mono<Book> get(@PathParam String id);
```

Depending on the media type converter, partial responses can be sent to the client as soon as they are complete. For instance a stream of responses can be sent to a client as follows:

```java
@WebRoute(path = "/", method = Method.GET, produces = MediaTypes.APPLICATION_X_NDJSON)
Publisher<Book> list();
```

In the preceding example, as soon as a book is retrieved from a data store it can be sent to the client which can then process responses as soon as possible reducing the latency and resource consumption on both client and server. The response content type is `application/x-ndjson`, so each book is encoded in JSON before a newline delimiter to let the client detects partial responses as defined by [the ndjon format][ndjson].

##### Server-sent events

[Server-sent events][server-sent-events] can be streamed in the response body when declared together with a server-sent event factory route method parameter. A server-sent event factory can be bound to a route method parameter using the `@SseEventFactory` annotation.

In the following example, we declare a basic server-sent events Web route producing events with a `String` message:

```java
@WebRoute(path = "/event", method = Method.GET)
Publisher<WebResponseBody.SseEncoder.Event<String>> getBookEvents(@SseEventFactory WebResponseBody.SseEncoder.EventFactory<String> events);
```

Server-sent event return type can be any of `Mono<WebResponseBody.SseEncoder.Event<T>>` if only one event is expected, `Flux<WebResponseBody.SseEncoder.Event<T>>` if multiple events are expected or more broadly `Publisher<WebResponseBody.SseEncoder.Event<T>>`.

By default, the media type of a server-sent event message is `text/plain` but it can be encoded using a specific media type converter as well by specifying a media type in the `@SseEventFactory` annotation. 

We can rewrite previous example with messages of a custom type serialized in JSON as follows:

```java
@WebRoute(path = "/event", method = Method.GET)
public Publisher<WebResponseBody.SseEncoder.Event<BookEvent>> getBookEvents(@SseEventFactory(MediaTypes.APPLICATION_JSON) WebResponseBody.SseEncoder.EventFactory<BookEvent> events) {
    return Flux.interval(Duration.ofSeconds(1))
        .map(seq -> events.create(
                event -> event
                    .id(Long.toString(seq))
                    .event("bookEvent")
                    .value(new BookEvent("some book event"))
            )
        );
}
```

### Declarative WebSocket route

Just like Web route, a WebSocket route can be declared using the `@WebSocketRoute` annotation with slightly different semantic and bindings. A WebSocket exchange is essentially defined by an inbound stream of messages and an outbound stream of messages.

WebSocket routes are defined as methods in a Web controller with the following rules: 

- The WebSocket `Web2SocketExchange.Inbound` may be injected as method parameter.
- The WebSocket inbound may be injected as method parameter as a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>`. When defined that way, the `Web2SocketExchange.Inbound` can not be injected as method parameter.
- The WebSocket `Web2SocketExchange.Outbound` may be injected as method parameter and if so the method must be `void`.
- The WebSocket WebSocket outbound may be specified as method's return type as a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>` which closes the WebSocket when it terminates. When defined that way, the `Web2SocketExchange.Outbound` can not be injected as method parameter.
- The `Web2SocketExchange` may always be injected as method parameter.
- The exchange context may always be injected as method parameter just like for regular Web routes.

#### Routing rules

WebSocket routing rules, as defined in the [Web routing API](#websocket-route), are specified in a single `@WebSocketRoute` annotation on a Web controller method. It allows to define paths, produced languages, supported subprotocols and the message type consumed and produced by the WebSocket routes that route a matching request to the handler implemented by the method.

A basic WebSocket route consuming and producing JSON text messages can be declared as follows:

```java
@WebSocketRoute( path = "/chat", subprotocol = { "json" } )
Flux<Message> chat(Flux<Message> inbound);
```

> Note that this exactly corresponds to the [Web routing API](#web-routing-api).

#### Contextual parameters

The `Web2SocketExchange` and the exchange context can be injected in the WebSocket route handler method just as for a regular [Web route](#contextual-parameters).

```java
@WebSocketRoute( path = "/chat", subprotocol = { "json" } )
Flux<Message> chat(Flux<Message> inbound, Web2SocketExchange<? extends ExchangeContext> webSocketExchange);
```

```java
@WebSocketRoute( path = "/chat", subprotocol = { "json" } )
<E extends SecurityContext & ChatContext> Flux<Message> chat(Flux<Message> inbound, E context);
```

#### WebSocket inbound

The WebSocket inbound can be specified as method parameter in two ways, either by injecting the `Web2SocketExchange.Inbound` or by injecting a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>`.

When specified as `Web2SocketExchange.Inbound` parameter, inbound frames or messages can be consumed as defined in the [Web Routing API documentation](#websocket-route):

```java
@WebSocketRoute( path = "/ws" )
public void webSocket(Web2SocketExchange.Inbound inbound) {
    Flux.from(inbound.messages()).flatMap(WebSocketMessage::reducedText).subscribe(LOGGER::info);
}
```

When specified as a `Publisher<T>` parameter, `<T>` can be basically a `ByteBuf`, a `String` or any types that can be converted using a converter matching the negotiated subprotocol.

For instance, raw inbound messages can be consumed as follows:

```java
@WebSocketRoute( path = "/ws" )
public void webSocket(Flux<ByteBuf> inbound) {
    inbound.subscribe(message -> {
        try {
            LOGGER.info(message.toString(Charsets.DEFAULT));
        }
        finally {
            // ByteBuf must be released where they are consumed
            message.release();
        }
    });
}
```

It is also possible to consume raw frame data composing inbound messages as follows:

```java
@WebSocketRoute( path = "/ws" )
public void webSocket(Flux<Flux<ByteBuf>> inbound) {

    inbound
        .doOnNext(message -> LOGGER.info("Message start"))
        .flatMap(message -> message.doOnComplete(() -> LOGGER.info("Message end")))
        .subscribe(message -> {
            try {
                LOGGER.info(message.toString(Charsets.DEFAULT));
            }
            finally {
                // ByteBuf must be released where they are consumed
                message.release();
            }
        });
}
```

Finally, inbound messages can also be automatically decoded using a converter matching the subprotocol negotiated during the opening handshake:

```java
@WebSocketRoute( path = "/ws", subprotocol = { "json" } )
public void webSocket(Flux<Message> inbound) {
    inbound.subscribe(message -> {
        LOGGER.info(message.getNickname() + ": " + message.getMessage());
    });
}
```

#### WebSocket outbound

The WebSocket outbound can be specified in two ways, either as method parameter by injecting the `Web2SocketExchange.Outbound` or as method's return type as a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>`.

When specified as `Web2SocketExchange.Outbound`, outbound frames or messages can be provided as defined in the [Web Routing API documentation](#websocket-route):

```java
@WebSocketRoute( path = "/ws" )
public void webSocket(Web2SocketExchange.Outbound outbound) {
    outbound.messages(factory -> Flux.interval(Duration.ofSeconds(1)).map(ign -> factory.text(ZonedDateTime.now().toString())));
}
```

When specified as method's return type as a `Publisher<T>`, `<T>` can be basically a `ByteBuf`, a `String` or any types that can be converted using a converter matching the negotiated subprotocol.

For instance, `String` outbound messages can be provided as follows:

```java
@WebSocketRoute( path = "/ws" )
public Flux<String> webSocket() {
    return Flux.just("messge 1", "message 2", "message 3");
}
```

It is also possible to produce fragmented raw messages as follows:

```java
@WebSocketRoute( path = "/ws" )
public Flux<Flux<ByteBuf>> webSocket() {
    return Flux.just(
        Flux.just(
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("message", Charsets.DEFAULT)), 
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(" 1", Charsets.DEFAULT))
        ), 
        Flux.just(
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("message ", Charsets.DEFAULT)), 
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(" 2", Charsets.DEFAULT))
        ), 
        Flux.just(
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("message ", Charsets.DEFAULT)), 
            Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(" 3", Charsets.DEFAULT))
        )
    );
}
```

Finally, outbound messages can be automatically encoded using a converter matching the subprotocol negotiated during the opening handshake:

```java
@WebSocketRoute( path = "/ws", subprotocol = { "json" } )
public Flux<Message> webSocket() {
    return Flux.just(
        new Message("john", "message 1"),
        new Message("bob", "message 2"),
        new Message("alice", "message 3")
    );
}
```

<br/>

> Putting it all together, the [simple chat server](#a-simple-chat-server) can be simply implemented as follows:
> 
> ```java
> package io.inverno.example.app_web_websocket;
> 
> import io.inverno.core.annotation.Bean;
> import io.inverno.core.annotation.Destroy;
> import io.inverno.core.annotation.Init;
> import io.inverno.example.app_web_websocket.dto.Message;
> import io.inverno.mod.web.annotation.WebController;
> import io.inverno.mod.web.annotation.WebSocketRoute;
> import reactor.core.publisher.Flux;
> import reactor.core.publisher.Sinks;
> 
> @Bean
> @WebController
> public class App_web_websocketWebController {
> 
>     private Sinks.Many<Message> chatSink;
> 
>     @Init
>     public void init() {
>         this.chatSink = Sinks.many().multicast().onBackpressureBuffer(16, false);
>     }
> 
>     @Destroy
>     public void destroy() {
>         this.chatSink.tryEmitComplete();
>     }
> 
>     @WebSocketRoute(path = "/ws", subprotocol = "json")
>     public Flux<Message> ws2(Flux<Message> inbound) {
>         inbound.subscribe(message -> this.chatSink.tryEmitNext(message));
>         return this.chatSink.asFlux();
>     }
> }
> ```

### Composite Web module

The Web Inverno compiler plugin generates a single Web server controller configurer bean aggregating all route definitions and context types specified in Web configurers or Web controllers beans in the module. When a module composes the *web* module, this bean is then wired to the *web* module to configure the Web server controller. 

Now when a module doesn't compose the *web* module, the Web router configurer bean is simply exposed by the module waiting for the module to be composed within other modules until a top module eventually composes the *web* module. 

This raises two issues: 

- First if multiple Web modules are composed together with the *web* module, dependency injection conflicts will be reported since multiple Web server controller configurer beans can be wired to the *web* module.
- Then if such module is composed in another module defining other Web controllers, we still need to expose one Web router configurer providing all route definitions to a top module composing the *web* module.

Hopefully, the `WebServerControllerConfigurer` interface extends `WebRouterConfigurer` and `ErrorWebRouterConfigurer` which are automatically aggregated in a generated Web server controller configurer bean by the Inverno Web compiler plugin. Then all we have to do to compose Web modules is to explicitly wire the top `WebServerControllerConfigurer` bean to the *web* module.

A generated Web server controller configurer is always annotated with a `@WebRoutes` annotation specifying the Web routes it configures. For instance, the configurer generated for the module defining the book Web controller looks like:

```java
@WebRoutes({
	@WebRoute(path = { "/book/{id}" }, method = { Method.GET }, produces = { "application/json" }),
	@WebRoute(path = { "/book" }, method = { Method.POST }, consumes = { "application/json" }),
	@WebRoute(path = { "/book/{id}" }, method = { Method.PUT }, consumes = { "application/json" }),
	@WebRoute(path = { "/book" }, method = { Method.GET }, produces = { "application/json" }),
	@WebRoute(path = { "/book/{id}" }, method = { Method.DELETE })
})
@Bean( name = "webServerContollerConfigurer" )
@Generated(value="io.inverno.mod.web.compiler.internal.WebServerControllerConfigurerCompilerPlugin", date = "2022-07-20T14:10:14.100988902+02:00[Europe/Paris]")
public final class App_web_WebServerContollerConfigurer implements WebServerControllerConfigurer<App_web_WebServerContollerConfigurer.Context> {
    ...
}
```

These information are used by the compiler plugin to statically check that there is no conflicting routes when generating the Web server controller configurer. It is a good practice to explicitly define the `@WebRoutes` annotation when defining routes programmatically in a Web configurer, otherwise the compiler can not determine conflict as it does not know the actual routes configured.

Now let's imagine we have created a modular Web application with a *book* module defining the book Web controller, an *admin* module defining some admin Web controllers and a top *app* module composing these modules together with the *web* module.

The module descriptors for each of these modules should look like:

```java
@io.inverno.core.annotation.Module( excludes = { "io.inverno.mod.web" } )
module io.inverno.example.web_modular.admin {
    requires io.inverno.core;
    requires io.inverno.mod.web;

    exports io.inverno.example.web_modular.admin to io.inverno.example.web_modular.app;
}
```
```java
@io.inverno.core.annotation.Module( excludes = { "io.inverno.mod.web" } )
module io.inverno.example.web_modular.book {
    requires io.inverno.core;
    requires io.inverno.mod.web;
    
    exports io.inverno.example.web_modular.book to io.inverno.example.web_modular.app;
    exports io.inverno.example.web_modular.book.dto to com.fasterxml.jackson.databind;
}
```
```java
@io.inverno.core.annotation.Module
module io.inverno.example.web_modular.app {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.web;
    
    requires io.inverno.example.web_modular.admin;
    requires io.inverno.example.web_modular.book;
}
```

The first thing to notice is that the *web* module is excluded from *admin* and *book* modules since we don't want to start a Web server in these modules, we only need the Web routing API to define Web controllers and generate Web server controller configurer beans. As a consequence, the *boot* module which provides converters and net service required to create and start the *web* module is also not required but the `io.inverno.core` module is still required. Finally we must export packages containing the generated module classes to the *app* module so it can compose them.

The *admin* and *book* modules should compile just fine resulting in two Web server controller configurer beans being generated and exposed in each module. But the compilation of *app* module should raise some dependency injection errors since multiple Web server controller configurer beans exist whereas only one can be wired to the *web* module. There are actually three Web server controller configurer beans, how so? There are those exposed by the *admin* and *book* modules and one generated in the *app* module and aggregating the previous two. In order to solve the conflict, we should then define the following explicit wire in the *app* module:

```java
@io.inverno.core.annotation.Module
@io.inverno.core.annotation.Wire(beans="io.inverno.example.web_modular.app:webServerContollerConfigurer", into="io.inverno.mod.web:controllerConfigurer")
module io.inverno.example.web_modular.app {
    ...
}
```

> One could rightfully argue that this explicit wiring is useless and cumbersome, but it is consistent with the IoC/DI core framework principles. Keeping things simple and explicit limits possible side effects induced by the fact that what's happening with *automatic* conflict resolution is often specific and might not be obvious. This is all the more true when such behavior is manually overridden.

The same principles applies if multiple modules like *admin* or *book* are cascaded into one another: Web server controller configurer beans at a given level are aggregated in the Web server controller configurer bean in the next level.

### Automatic OpenAPI specifications

Besides facilitating the development of REST and Web resources in general, Web controllers also simplify documentation. The Web Inverno compiler plugin can be setup to generate [Open API][open-api] specifications from the Web controller classes defined in a module and their JavaDoc comments. 

> Writing JavaDoc comments is something natural when developing in the Java language, with this approach, a REST API can be documented just as you document a Java class or method, documentation is written once and can be used in both Java and other languages and technologies using the generated Open API specification.

In order to activate this feature the `inverno.web.generateOpenApiDefinition` annotation processor option must be enabled when compiling a Web module. This can be done on the command line: `java -Ainverno.web.generateOpenApiDefinition=true ...` or in the Maven compiler plugin configuration in the build descriptor:

```java
<project>
    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <compilerArgs combine.children="append">
                            <arg>-Ainverno.web.generateOpenApiDefinition=true</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

The compiler then generates an Open API specification in `META-INF/inverno/web/openapi.yml` for any module defining one or more Web controllers.

The previous [book resource](#web-controller) could then be documented as follows:

```java
/**
 * The book resource.
 */
@Bean
@WebController(path = "/book")
public class BookResource {
    
    /**
     * Creates a book resource.
     * 
     * @param book a book
     * @param exchange the web exchange
     * 
     * @return the book resource has been successfully created
     * @throws BadRequestException A book with the same ISBN reference already exist
     */
    @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
    public Mono<Void> create(@Body Mono<Book> book, WebExchange exchange) throws BadRequestException { ... }
    
    /**
     * Updates a book resource.
     * 
     * @param isbn the reference of the book resource to update
     * @param book the updated book resource
     * 
     * @return the book resource has been successfully updated
     * @throws NotFoundException if the specified reference does not exist
     */
    @WebRoute(path = "/{isbn}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON)
    public Mono<Void> update(@PathParam String isbn, @Body Mono<Book> book) throws NotFoundException { ... }
    
    /**
     * Returns the list of book resources.
     * 
     * @return a list of book resources
     */
    @WebRoute(method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    public Flux<Book> list();
    
    /**
     * Returns the book resource identified by the specified ISBN.
     * 
     * @param isbn an ISBN
     * 
     * @return the requested book resource
     * @throws NotFoundException if the specified reference does not exist
     */
    @WebRoute(path = "/{isbn}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    public Mono<Book> get(@PathParam String isbn) throws NotFoundException { ... }
    
    /**
     * Deletes the book resource identified by the specified ISBN.
     * 
     * @param isbn an ISBN
     * 
     * @return the book resource has been successfully deleted
     * @throws NotFoundException if the specified reference does not exist
     */
    @WebRoute(path = "/{isbn}", method = Method.DELETE)
    public Mono<Void> delete(@PathParam String isbn) { ... }
}
```

Note that just like the `javadoc` tool, the Web compiler plugin takes inheritance into account when resolving JavaDoc comments and as a result, it is possible to define JavaDoc comments in an interface and enrich or override them in the implementation classes.

By default, the normal HTTP status code responded by a route is assumed to be `200` but it is possible to specify a custom status code using the `@inverno.web.status` tag. For instance the book creation route which actually responds with a `201` status should be documented as follows:

```java
public class BookResource {

    /**
     * Creates a book resource.
     * 
     * @param book a book
     * @param exchange the web exchange
     * 
     * @return {@inverno.web.status 201} the book resource has been successfully created
     * @throws BadRequestException A book with the same ISBN reference already exist
     */
    @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
    public Mono<Void> create(@Body Mono<Book> book, WebExchange exchange) throws BadRequestException { ... }
    
    ...
}
```

> Multiple `@return` statements can be specified if multiple response statuses are expected, however this might raise issues during the generation of the JavaDoc, you can bypass this by disabling the linter with `-Xdoclint:none` option.

This tag can also be used to specify error status code in `@throws` statements, but this is usually not necessary since the Web compiler plugin automatically detects status code for regular `HTTPException` such as `BadRequestException` (400) or `NotFoundException` (404).

The Web compiler plugin generates, per module, one Open API specification and one Web server controller configurer bean aggregating all routes from all Web controllers and Web configurers. As a result the general API documentation corresponds to the general documentation of the module which is defined in the module descriptor JavaDoc comment.

For instance, we can describe the API exposed by the *book* module in the module descriptor including the API version which should normally match the module version:

```java
/**
 * This is a sample Book API which demonstrates Inverno Web module capabilities.
 * 
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * 
 * @version 1.2.3
 */
@io.inverno.core.annotation.Module( excludes = { "io.inverno.mod.web" } )
module io.inverno.example.web_modular.book {
    requires io.inverno.core;
    requires io.inverno.mod.web;
    
    exports io.inverno.example.web_modular.book to io.inverno.example.web_modular.app;
    exports io.inverno.example.web_modular.book.dto to com.fasterxml.jackson.databind;
}
```

These specifications can also be exposed in the Web server using the `OpenApiRoutesConfigurer` as described in the [Web server documentation](#openapi-specification).

If we build and run the [modular book application](#composite-web-module) and access `http://locahost:8080/open-api` in a Web browser we should see a Swagger UI loaded with the Open API specifications of the *admin* and *book* modules:

<img class="shadow mb-4" src="doc/img/swaggerUI_root.png" alt="General Swagger UI"/>

It is also possible to target a single specification by specifying the module name in the URI, for instance `http://locahost:8080/open-api/io.inverno.example.web_modular.book`:

<img class="shadow mb-4" src="doc/img/swaggerUI_module.png" alt="Module Swagger UI"/>

Finally, Open API specifications formatted in [YAML][yaml] can be retrieved as follows:

```plaintext
$ curl http://locahost:8080/open-api/io.inverno.example.web_modular.admin

openapi: 3.0.3
info:
    title: 'io.inverno.example.web_modular.admin'
    version: ''
...
```
