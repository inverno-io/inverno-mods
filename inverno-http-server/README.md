[inverno-javadoc]: https://inverno.io/docs/release/api/index.html

[netty]: https://netty.io/
[form-urlencoded]: https://url.spec.whatwg.org/#application/x-www-form-urlencoded
[rfc-7578]: https://tools.ietf.org/html/rfc7578
[zero-copy]: https://en.wikipedia.org/wiki/Zero-copy
[chunked-transfer-encoding]: https://en.wikipedia.org/wiki/Chunked_transfer_encoding
[epoll]: https://en.wikipedia.org/wiki/Epoll
[kqueue]: https://en.wikipedia.org/wiki/Kqueue
[jdk-providers]: https://docs.oracle.com/en/java/javase/11/security/oracle-providers.html

[rfc-7540-8.1.2.4]: https://tools.ietf.org/html/rfc7540#section-8.1.2.4

# HTTP Server

The Inverno *http-server* module provides fully reactive HTTP/1.x and HTTP/2 server based on [Netty][netty].

It especially supports:

- HTTP/1.x pipelining
- HTTP/2 over cleartext
- HTTP Compression
- TLS
- Interceptors
- Strongly typed contexts
- `application/x-www-form-urlencoded` body decoding
- `multipart/form-data` body decoding
- Server-sent events
- Cookies
- zero-copy file transfer when supported for fast resource transfer
- parameter conversion

The server is fully reactive, based on the reactor pattern and non-blocking sockets which means it requires a limited number of threads to supports thousands of connections with high end performances. This design offers multiple advantages starting with maximizing the usage of resources. It is also easy to scale the server up and down by specifying the number of threads we want to allocate to the server, which ideally corresponds to the number of CPU cores. All this makes it a perfect choice for microservices applications running in containers in the cloud.

> This module lays the foundational service and API for building HTTP servers with more complex and advanced features, that is why you might sometimes find it a little bit low level but that is the price of performance. If you require higher level functionalities like request routing, content negotiation and automatic payload conversion please consider the [web module](#web).

This module requires basic services like a [net service](#net-service) and a [resource service](#resource-service) which are usually provided by the *boot* module, so in order to use the Inverno *http-server* module, we should declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_http {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.http.server;
}
```

The *http-base* module which provides the header service used by the HTTP server is composed as a transitive dependency in the *http-server* module and as a result it doesn't need to be specified here nor provided in an enclosing module.

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
            <artifactId>inverno-http-server</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-http-server:${VERSION_INVERNO_MODS}'
...
```

## HTTP Server exchange API

The module defines classes and interfaces to implement HTTP server exchange handlers used to handle HTTP requests sent by a client to the server.

A server `ExchangeHandler` is defined to handle a server `Exchange` composed of a `Request`, a `Response` and an `ExchangeContext` in a HTTP communication between a client and a server. The API has been designed to be fluent and reactive in order for the request to be *streamed* down to the response.

### Basic exchange handler

The `ReactiveExchangeHandler` is a functional interface defining method `Mono<Void> defer(Exchange<ExchangeContext> exchange);` which is used to handle server exchanges in a reactive way. It is for instance possible to execute non-blocking operations before actually handling the exchange. 

> Authentication is a typical example of a non-blocking operation that might be executed before handling the request.

> Under the hood, the server will first subscribe to the returned `Mono`, when it completes the server then subscribes to the response body data publisher and eventually sends a response to the client.

The `ExchangeHandler` extends the `ReactiveExchangeHandler` with method `void handle(Exchange<ExchangeContext> exchange)` which is more convenient than `defer()` when no non-blocking operation other than the generation of the client response is required.

A basic exchange handler can then be created as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
        .body()
            .raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello, world!", Charsets.DEFAULT)));
};
```

The above code creates an exchange handler sending a `Hello, world!` message in response to any request.

We might also want to send the response in a reactive way in a stream of data in case the entire response payload is not available right away, if it doesn't fit in memory or if we simply want to send a response in multiple parts as soon as they become available (eg. progressive display).

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    Flux<ByteBuf> dataStream = Flux.just(
        Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)),
        Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", world!", Charsets.DEFAULT))
    );

    exchange.response()
        .body().raw().stream(dataStream);
};
```

### Request body

Request body can be handled in a similar way. The reactive API allows to process the payload of a request as the server receives it and therefore progressively build and send the corresponding response.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().raw().stream(exchange.request().body()
            .map(body -> Flux.from(body.raw().stream()).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(chunk.readableBytes()))))
            .orElse(Flux.just(Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(0))))
        );
};
```

In the above example, if a client sends a payload in the request, the server responds with the number of bytes of each chunk of data it receives or it responds `0` if the request payload is empty. This simple example illustrates how we can process requests as flow of data

### URL Encoded form

HTML form data are sent in the body of a POST request in the form of key/value pairs encoded in [application/x-www-form-urlencoded format][form-urlencoded]. The resulting list of `Parameter` can be obtained as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
            .map(parameter -> Unpooled.copiedBuffer(Unpooled.copiedBuffer("Received parameter " + parameter.getName() + " with value " + parameter.getValue(), Charsets.DEFAULT)))
        );
}
```

In the above example, for each form parameters the server responds with a message describing the parameters it just received. Again this shows that the API is fully reactive and form parameters can be processed as they are decoded.

A more traditional example though would be to obtained the map of parameters grouped by names (because multiple parameters with the same name can be sent):

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
        .collectMultimap(Parameter::getName)
            .map(formParameters -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("User selected options: " + formParameters.get("options").stream().map(Parameter::getValue).collect(Collectors.joining(", ")), Charsets.DEFAULT)))
        );
}
```

> Here we may think that the aggregation of parameters in a map could *block* the I/O thread but this is definitely not true, when a parameter is decoded, the reactive framework is notified and the parameter is stored in a map, after that the I/O thread can be reallocated. When the parameters publisher completes the resulting map is emitted to the mapping function which build the response. During all this process, no thread is ever waiting for anything.

### Multipart form

A [multipart/form-data][rfc-7578] request can be handled in a similar way. Form parts can be obtained as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().multipart().stream())
            .map(part -> Unpooled.copiedBuffer(Unpooled.copiedBuffer("Received part " + part.getName(), Charsets.DEFAULT)))
        );
};
```

In the above example, the server responds with the name of the part it just received. Parts are decoded and can be processed along the way, a part is like a body embedded in the request body with its own headers and payload.

Multipart form data is most commonly used for uploading files over HTTP. Such handler can be implemented as follows using the [resource API](#resource-api) to store uploaded files:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().multipart().stream())                                                                                                                // 1
            .single()                                                                                                                                                                                       // 2
            .flatMapMany(part -> part.getFilename()                                                                                                                                                         // 3
                .map(fileName -> Flux.<ByteBuf, FileResource>using(                                                                                                                                         // 4
                        () -> new FileResource("uploads/" + part.getFilename().get()),                                                                                                                      // 5
                        file -> file.write(part.raw().stream()).map(Flux::from).get()                                                                                                                       // 6
                            .reduce(0, (acc, cur) -> acc + cur) 
                            .map(size -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Uploaded " + fileName + "(" + part.headers().getContentType() + "): " + size + " Bytes\n", Charsets.DEFAULT))), 
                        FileResource::close                                                                                                                                                                 // 7
                    )
                )
                .orElseThrow(() -> new BadRequestException("Not a file part"))                                                                                                                              // 8
            )
        );
};
```

The above code uses multiple elements and deserves a detailed explanation: 

1. get the stream of parts
2. make sure we only have one part in the request for the sake of simplicity
3. map the part to the response stream by starting to determine whether the part is a file part
4. if the part is a file part indeed, map the part to the response stream by creating a Flux with a file resource
5. in this case the resource is the target file where the uploaded file will be stored
6. stream the part's payload to the target file resource and eventually provides the response in the form of a message stating that a file with a given size and media type has been uploaded
7. close the file resource when the publisher completes
8. if the part is not a file part respond with a bad request error

The `Flux.using()` construct is the reactive counterpart of a try-with-resource statement. It is interesting to note that the content of the file is streamed up to the file and it is then never entirely loaded in memory. From there, it is quite easy to stop the upload of a file if a given size threshold is exceeded. We can also imagine how we could create a progress bar in a client UI to show the progression of the upload.

> In the above code we uploaded a file and stored its content on the local file system and during all that process, the I/O thread was never blocked.

### Resource

A [resource](#resource-api) can be sent as a response to a request. When this is possible the server uses low-level ([zero-copy][zero-copy]) API for fast resource transfer.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().resource().value(new FileResource("/path/to/resource"));
};
```

The media type of the resource is resolved using a [media type service](#media-type-service) and automatically set in the response `content-type` header field. 

> If a specific resource is created as in above example the media type service used is the one defined when creating the resource or a default implementation if none was specified. If the resource is obtained with the resource service provided in the *boot* module the media type service used is the one provided in the *boot* module.

### Server-sent events

[Server-sent events][server-sent-events] provide a way to send server push notifications to a client. It is based on [chunked transfer encoding][chunked-transfer-encoding] over HTTP/1.x and regular streams over HTTP/2. The API provides an easy way to create SSE endpoints.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response().body().sse().from(
        (events, data) -> data.stream(Flux.interval(Duration.ofSeconds(1))
            .map(seq -> events.create(event -> event
                .id(Long.toString(seq))
                .event("seq")
                .comment("Some comment")
                .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Event #" + seq, Charsets.DEFAULT))))
            )
        )
    );
};
```

In the above example, server-sent events are emitted every second and streamed to the response. This is done in a function accepting the server-sent event factory used to create events and the response data producer.

### Error exchange handler

An error exchange handler is a particular exchange handler which is defined to handle server error exchange. In other words it is used by the server to handle exceptions thrown during the processing of a regular exchange in order to send an appropriate response to the client when this is still possible (ie. assuming response headers haven't been sent yet).

```java
ErrorExchangeHandler<Throwable, ErrorExchange<Throwable>> errorHandler = errorExchange -> {
    if(errorExchange.getError() instanceof BadRequestException) {
        errorExchange.response()
            .headers(headers -> headers.status(Status.BAD_REQUEST))
            .body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("client sent an invalid request", Charsets.DEFAULT)));
    }
    else {
        errorExchange.response()
            .headers(headers -> headers.status(Status.INTERNAL_SERVER_ERROR))
            .body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Unknown server error", Charsets.DEFAULT)));
    }
};
```

### Exchange interceptor

An exchange handler can be intercepted using an `ExchangeInterceptor`. An interceptor can be used to preprocess an exchange in order to check preconditions and potentially respond to the client instead of the handler, initialize a context (tracing, metrics...), decorate the exchange...

The `intercept()` method returns a `Mono` which makes it reactive and allows to invoke non-blocking operations before invoking the handler.

An intercepted exchange handler can be created as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {...};

ExchangeInterceptor<ExchangeContext, Exchange<ExchangeContext>> interceptor = exchange -> {
    LOGGER.info("Path: " + exchange.request().getPath());

    return Mono.just(exchange); // exchange is returned unchanged and will be processed by the handler
}

ReactiveExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> interceptedHandler = handler.intercept(interceptor);
```

An interceptor can also fully process an exchange, in which case it must return an empty `Mono` to stop the exchange handling chain.

```java
ExchangeInterceptor<ExchangeContext, Exchange<ExchangeContext>> interceptor = exchange -> {
    // Check some preconditions...
    if(...) {
        exchange.response().headers(headers -> headers.status(Status.BAD_REQUEST)).body().empty();

        return Mono.empty(); // the exchange has been processed by the interceptor and it won't be processed by the handler
    }
    return Mono.just(exchange);
}
```

We can chain interceptors, by invoking `intercept()` method mutliple times:

```java
// exchange handling chain: interceptor3 -> interceptor2 -> interceptor1 -> handler
handler.intercept(interceptor1).intercept(interceptor2).interceptor(3);
```

### Exchange context

A strongly typed context is exposed in the `Exchange`, it allows to store or access data and to provide contextual operations throughout the process of the exchange. The context is created by the server along with the exchange using a user specific `RootExchangeHandler`. It is then possible to *customize* the exchange with a specific strongly types context. 

The advantage of this approach is that the compiler can perform static type checking but also to avoid the usage of an untyped map of attributes which is more performant and allow more control over contextual data. Since the developer is defining the context type, he can implement logic inside.

A context can be used to store security information, tracing information, metrics... For instance, if we combine this with exchange interceptors:

```java
ExchangeHandler<SecurityContext, Exchange<SecurityContext>> handler = exchange -> {
    if(exchange.context().isAuthenticated()) {
        exchange.response()
            .headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
            .body()
                .raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello, world!", Charsets.DEFAULT)));
    }
    else {
        exchange.response()
            .headers(headers -> headers.status(Status.UNAUTHORIZED))
            .body()
                .empty();
    }
};

ExchangeInterceptor<SecurityContext, Exchange<SecurityContext>> securityInterceptor = exchange -> {
    // Authenticate the request
    if(...) {
        exchange.context().setAuthenticated(true);
    }
    return Mono.just(exchange);
}

ReactiveExchangeHandler<SecurityContext, Exchange<SecurityContext>> interceptedHandler = handler.intercept(securityInterceptor);
```

> The server relies on a `RootExchangeHandler` which extends `ExchangeHandler` with the `createContext()` method in order to create the context. The server actually uses exactly one root exchange handler and one exchange error handler to handle server requests and errors. Please refer to the [HTTP server](#http-server) section which explains this in details and describes how to setup the HTTP server.

### Misc

The API is fluent and mostly self-describing as a result it should be easy to find out how to do something in particular, even so here are some miscellaneous elements.

#### Request headers

A particular request header can be obtained as follows, if there are multiple headers with the same name, the first one shall be returned:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    ...
    // get the raw value of a header
    String someHeader = exchange.request().headers().get("some-header").orElseThrow(() -> new BadRequestException("Missing some-header"));
    
    // get a header as a parameter that can be converted using the parameter converter
    LocalDateTime someDateTime = exchange.request().headers().getParameter("some-date-time").map(Parameter::asLocalDateTime).orElseThrow(() -> new BadRequestException("Missing some-date-time"));
    
    // get a decoded header using the header service
    CustomHeader customHeader = exchange.request().headers().<CustomHeader>getHeader("custom-header").orElseThrow(() -> new BadRequestException("Missing some-date-time"));
    ...
};
```

All headers with a particular names can be obtained as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    ...
    // get all raw values defined for a given header
    List<String> someHeaderList = exchange.request().headers().getAll("some-header");
    
    // get all headers with a given header as parameters that can be converted using the parameter converter
    LocalDateTime someDateTime = exchange.request().headers().getParameter("some-date-time").map(Parameter::asLocalDateTime).orElseThrow(() -> new BadRequestException("Missing some-date-time"));
    
    // get all headers with a given name decoded using the header service
    CustomHeader customHeader = exchange.request().headers().<CustomHeader>getHeader("custom-header").orElseThrow(() -> new BadRequestException("Missing some-date-time"));
    ...
};
```

Finally we can retrieve all headers as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    ...
    // get all headers with raw values
    List<Map.Entry<String, String>> requestHeaders = exchange.request().headers().getAll();

    // get all headers as parameters that can be converted using the parameter converter
    List<Parameter> requestHeaderParameters = exchange.request().headers().getAllParameter();
    
    // get all headers decoded using the header service
    List<Header> requestDecodedHeaders = exchange.request().headers().getAllHeader();
    ...
};
```

#### Query parameters

Query parameters in the request can be obtained as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    ...
    // get a specific query parameter, if there are multiple parameters with the same name, the first one is returned
    int someInteger = exchange.request().queryParameters().get("some-integer").map(Parameter::asInteger).orElseThrow(() -> new BadRequestException("Missing some-integer"));

    // get all query parameters with a given name
    List<Integer> someIntergers = exchange.request().queryParameters().getAll("some-integer").stream().map(Parameter::asInteger).collect(Collectors.toList());

    // get all query parameters
    Map<String, List<Parameter>> queryParameters = exchange.request().queryParameters().getAll();
    ...
};
```

#### Request cookies

Request cookie can be obtained in a similar way as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    ...
    // get a specific cookie, if there are multiple cookie with the same name, the first one is returned
    int someInteger = exchange.request().cookies().get("some-integer").map(Parameter::asInteger).orElseThrow(() -> new BadRequestException("Missing some-integer"));

    // get all cookies with a given name
    List<Integer> someIntergers = exchange.request().cookies().getAll("some-integer").stream().map(Parameter::asInteger).collect(Collectors.toList());

    // get all cookies
    Map<String, List<CookieParameter>> queryParameters = exchange.request().cookies().getAll();
    ...
};
```

Note that cookies can also be obtained as request headers.

#### Request components

The API also gives access to multiple request related information such as:

- the HTTP method
- the scheme (`http` or `https`)
- the authority part of the requested URI (`host` header in HTTP/1.x and `:authority` pseudo-header in HTTP/2)
- the requested path including query string
- the absolute path which is the normalized requested path without the query string
- the `URIBuilder` corresponding to the requested path to build relative paths
- the query string
- the socket address of the client or last proxy that sent the request

#### Response headers/trailers

Response headers can be added or set fluently using a configurator as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .headers(headers -> headers
            .contentType(MediaTypes.TEXT_PLAIN)
            .set(Headers.NAME_SERVER, "inverno")
            .add("custom-header", "abc")
        )
        .body().raw()...;
};
```

Response trailers can be set in the exact same way:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .trailers(headers -> headers
            .add("some-trailer", "abc")
        )
        .body().raw()...;
};
```

#### Response status

The response status can be set in the response headers following HTTP/2 specification as defined by [RFC 7540 Section 8.1.2.4][rfc-7540-8.1.2.4].

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .headers(headers -> headers.status(Status.OK))
        .body().raw();
};
```

#### Response cookies

Response cookies can be set fluently using a configurator as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .cookies(cookies -> cookies
            .addCookie(cookie -> cookie.name("cookie1")
                .httpOnly(true)
                .secure(true)
                .maxAge(3600)
                .value("abc")
            )
            .addCookie(cookie -> cookie.name("cookie2")
                .httpOnly(true)
                .secure(true)
                .maxAge(3600)
                .value("def")
            )
        )
        .body().raw()...;
};
```

Note that cookies can also be set or added as response headers.

## HTTP Server

The HTTP server is started with the *http-server* module which requires a `NetService` and a `ResourceService` usually provided by the *boot* module, so one way to create an application with a HTTP server is to create an Inverno module composing the *boot* and *http-server* modules.

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_http {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.http.server;
}
```

The resulting *app_http* module, thus created, can then be started as an application as follows:

```java
package io.inverno.example.app_http;

import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App_http.Builder()).run();
    }
}
```

The above example starts a HTTP/1.x server using default configuration and default root and error handlers.

```
2021-04-14 09:51:46,329 INFO  [main] i.w.c.v.Application - Inverno is starting...


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
     ║ Application module  : io.inverno.example.app_http                                          ║
     ║ Application version : 1.0.0-SNAPSHOT                                                       ║
     ║ Application class   : io.inverno.example.app_http.Main                                     ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ....                                                                                      ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


2021-04-14 09:53:21,829 INFO  [main] i.w.e.a.App_http - Starting Module io.inverno.example.app_http...
2021-04-14 09:53:21,829 INFO  [main] i.w.m.b.Boot - Starting Module io.inverno.mod.boot...
2021-04-14 09:53:22,025 INFO  [main] i.w.m.b.Boot - Module io.inverno.mod.boot started in 195ms
2021-04-14 09:53:22,025 INFO  [main] i.w.m.h.s.Server - Starting Module io.inverno.mod.http.server...
2021-04-14 09:53:22,025 INFO  [main] i.w.m.h.b.Base - Starting Module io.inverno.mod.http.base...
2021-04-14 09:53:22,029 INFO  [main] i.w.m.h.b.Base - Module io.inverno.mod.http.base started in 3ms
2021-04-14 09:53:22,080 INFO  [main] i.w.m.h.s.i.HttpServer - HTTP Server (nio) listening on http://0.0.0.0:8080
2021-04-14 09:53:22,080 INFO  [main] i.w.m.h.s.Server - Module io.inverno.mod.http.server started in 55ms
2021-04-14 09:53:22,080 INFO  [main] i.w.e.a.App_http - Module io.inverno.example.app_http started in 252ms

```

You should be able to send a request to the server:

```plaintext
$ curl -i http://localhost:8080/
HTTP/1.1 200
content-length: 5

Hello
```

The HTTP server defines two handlers: the **root handler** which handles HTTP requests and the **error handler** which handles errors. The module provides default implementations as overridable beans, custom handlers can then be used instead when creating the *http-server* module.

> this module can also be used to embed a HTTP server in any application, unlike other application frameworks, Inverno core IoC/DI framework is not pervasive and any Inverno modules can be safely used in various contexts and applications.

### Configuration

The first thing we might want to do is to create a configuration in the *app_http* module for easy *http-server* module setup. The HTTP server configuration is actually done in the `BootConfiguration` defined in the *boot* module for low level network configuration and `HttpServerConfiguration` in the *http-server* module configuration for the HTTP server itself. 

The following configuration can then be created in the *app_http* module:

```java
package io.inverno.example.app_http;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.boot.BootConfiguration;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.http.server.HttpServerConfiguration;

@Configuration
public interface App_httpConfiguration {

    @NestedBean
    BootConfiguration boot();

    @NestedBean
    HttpServerConfiguration http_server();
}
```

This should be enough for exposing a configuration in the *app_http* module, that let us setup the server: 

```java
package io.inverno.example.app_http;

import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setApp_httpConfiguration(
                App_httpConfigurationLoader.load(configuration -> configuration
                    .http_server(server -> server
                        .server_port(8081)
                        .h2c_enabled(true)
                    )
                    .boot(boot -> boot
                        .reactor_event_loop_group_size(4)
                    )
                )
            )
        ).run();
    }
}
```

In the above code, we have set the server port to 8081, enabled HTTP/2 over cleartext and set the number of thread allocated to the reactor core IO event loop group to 4.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties. We can for instance configure low level network settings like TCP keep alive or TCP no delay as well as HTTP related settings like compression or TLS.

> You can also refer to the [configuration module documentation](#configuration-1) to get more details on how configuration works and more especially how you can from here define the HTTP server configuration in command line arguments, property files...

#### Logging

The HTTP server can log access and error events at `INFO` and `ERROR` level respectively. They can be disabled by configuring `io.inverno.mod.http.server.internal.AbstractExchange` logger as follows:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration xmlns="http://logging.apache.org/log4j/2.0/config"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://logging.apache.org/log4j/2.0/config https://raw.githubusercontent.com/apache/logging-log4j2/rel/2.14.0/log4j-core/src/main/resources/Log4j-config.xsd" 
    status="WARN" shutdownHook="disable">

    <Appenders>
        <Console name="LogToConsole" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{DEFAULT} %highlight{%-5level} [%t] %c{1.} - %msg%n%ex"/>
        </Console>
    </Appenders>
    <Loggers>
        <!-- Disable HTTP server access and error logs -->
        <Logger name="io.inverno.mod.http.server.internal.AbstractExchange" additivity="false" level="off"  />

        <Root level="info">
            <AppenderRef ref="LogToConsole"/>
        </Root>
    </Loggers>
</Configuration>
```

We can also create a more *production-like* logging configuration for a standard HTTP server that asynchronously logs access and error events in separate files in a JSON format for easy integration with log processing tools with a rolling strategy.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" name="Website" shutdownHook="disable">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
             <PatternLayout pattern="%d{DEFAULT} %highlight{%-5level} [%t] %c{1.} - %msg%n%ex"/>
        </Console>
        <!-- Error log -->
        <RollingRandomAccessFile name="ErrorRollingFile" fileName="logs/error.log" filePattern="logs/error-%d{yyyy-MM-dd}-%i.log.gz">
            <JsonTemplateLayout/>
            <NoMarkerFilter onMatch="ACCEPT" onMismatch="DENY"/>
            <Policies>
                <TimeBasedTriggeringPolicy />
                <SizeBasedTriggeringPolicy size="10 MB"/>
            </Policies>
            <DefaultRolloverStrategy>
                <Delete basePath="logs" maxDepth="2">
                    <IfFileName glob="error-*.log.gz" />
                    <IfLastModified age="10d" />
                </Delete>
            </DefaultRolloverStrategy>
        </RollingRandomAccessFile>
        <Async name="AsyncErrorRollingFile">
            <AppenderRef ref="ErrorRollingFile"/>
        </Async>
        <!-- Access log -->
        <RollingRandomAccessFile name="AccessRollingFile" fileName="logs/access.log" filePattern="logs/access-%d{yyyy-MM-dd}-%i.log.gz">
            <JsonTemplateLayout/>
            <MarkerFilter marker="HTTP_ACCESS" onMatch="ACCEPT" onMismatch="DENY"/>
            <Policies>
                <TimeBasedTriggeringPolicy />
                <SizeBasedTriggeringPolicy size="10 MB"/>
            </Policies>
            <DefaultRolloverStrategy>
                <Delete basePath="logs" maxDepth="2">
                    <IfFileName glob="access-*.log.gz" />
                    <IfLastModified age="10d" />
                </Delete>
            </DefaultRolloverStrategy>
        </RollingRandomAccessFile>
        <Async name="AsyncAccessRollingFile">
            <AppenderRef ref="AccessRollingFile"/>
        </Async>
    </Appenders>

    <Loggers>
        <Logger name="io.inverno.mod.http.server.internal.AbstractExchange" additivity="false" level="info">
            <AppenderRef ref="AsyncAccessRollingFile" level="info"/>
            <AppenderRef ref="AsyncErrorRollingFile" level="error"/>
        </Logger>

        <Root level="info" additivity="false">
            <AppenderRef ref="Console" level="info" />
            <AppenderRef ref="AsyncErrorRollingFile" level="error"/>
        </Root>
    </Loggers>
</Configuration>
```

> Note that access and error events are logged by the same logger, they are differentiated by markers, `HTTP_ACCESS` and `HTTP_ERROR` respectively.

#### Transport

By default, the HTTP server uses the Java NIO transport, but it is possible to use native [epoll][epoll] transport on Linux or [kqueue][kqueue] transport on BSD-like systems for optimized performances. This can be done by adding the corresponding Netty dependency with the right classifier in the project descriptor:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-transport-native-epoll</artifactId>
            <classifier>linux-x86_64</classifier>
        </dependency>
    </dependencies>
</project>
```

or 

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-transport-native-kqueue</artifactId>
            <classifier>osx-x86_64</classifier>
        </dependency>
    </dependencies>
</project>
```

> When these dependencies are declared on the JVM module path, the corresponding Java modules must be added explicitly when running the application. This is typically the case when the application is run or packaged as an application image using the Inverno Maven plugin.
>
> This can be done by defining the corresponding dependencies in the module descriptor: 
> 
> ```java
> @io.inverno.core.annotation.Module
> module io.inverno.example.app {
>     ...
>     requires io.netty.transport.unix.common;
>     requires io.netty.transport.epoll;
> }
> ```
>
> This approach is fine as long as we are sure the application will run on Linux, but in order to create a properly portable application, we should prefer adding the modules explicitly when running the application:
>
> ```plaintext
> $ java --add-modules io.netty.transport.unix.common,io.netty.transport.epoll ...
> ```
> 
> When building an application image, this can be specified in the Inverno Maven plugin configuration:
>
> ```xml
> <project>
>     <build>
>         <plugins>
>             <plugin>
>                 <groupId>io.inverno.tool</groupId>
>                 <artifactId>inverno-maven-plugin</artifactId>
>                 <executions>
>                     <execution>
>                         <configuration>
>                             <vmOptions>--add-modules io.netty.transport.unix.common,io.netty.transport.epoll</vmOptions>
>                         </configuration>
>                     </execution>
>                 </executions>
>             </plugin>
>         </plugins>
>     </build>
> </project>
> ```

### Root handler

The HTTP server defines a root exchange handler to handle all HTTP requests. By default, it uses a basic `RootExchangeHandler` implementation which returns `Hello` when a request is made to the root path `/` and (404) not found error otherwise. By default no context is created and `exchange.context()` returns `null`.

In order to use our own handler, we must define an exchange handler bean in the *app_http* module:

```java
package io.inverno.example.app_http;

import io.netty.buffer.Unpooled;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;

@Bean
public class CustomHandler implements RootExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> {

    @Override
    public void handle(Exchange<ExchangeContext> exchange) throws HttpException {
        exchange.response()
            .body().raw()
            .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello from app_http module!", Charsets.DEFAULT)));
    }
}
```

This bean will be automatically wired to the root handler socket defined by the *http-server* module overriding the default root handler. If we don't want to provide a handler implementation inside the *app_http* module, we can also define a socket bean for the root handler and provide an instance when creating the *app_http* module. 

```java
package io.inverno.example.app_http;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;

public class Main {

    @Bean
    public static interface RootHandler extends Supplier<RootExchangeHandler<ExchangeContext, Exchange<ExchangeContext>>> {}

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setRootHandler(exchange -> {
                exchange.response()
                    .body().raw()
                    .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello from main!", Charsets.DEFAULT)));
                }
            )
        ).run();
    }
}
```

Note that this socket bean is optional since the root handler socket on the *http-server* module to which it is wired is itself optional.

### Error handler

The HTTP server defines an error exchange handler to handle exceptions thrown when processing HTTP requests when this is still possible, basically when the response headers haven't been sent yet to the client. By default, it uses a basic `ErrorExchangeHandler` implementation which handles standard `HttpException` and responds empty body messages with HTTP error status corresponding to the exception.

This default implementation should be enough for a basic HTTP server but a custom handler should be provided to produce custom error pages or handle specific types of error. This can be done in the exact same way as for the [root handler](#root-handler) by defining an error exchange handler bean:

```java
package io.inverno.example.app_http;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ErrorExchangeHandler;

@Bean
public class CustomErrorHandler implements ErrorExchangeHandler<Throwable, ErrorExchange<Throwable>> {

    @Override
    public void handle(ErrorExchange<Throwable> exchange) throws HttpException {
        if(exchange.getError() instanceof SomeCustomException) {
            ...
        }
        else if(...) {
            ...
        }
        ...
        else {
            ...
        }
    }
}
```

Or by defining a socket bean:

```java
package io.inverno.example.app_http;

import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ErrorExchangeHandler;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;

public class Main {

    @Bean
    public static interface RootHandler extends Supplier<ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>>> {}
    
    @Bean
    public static interface ErrorHandler extends Supplier<ErrorExchangeHandler<Throwable, ErrorExchange<Throwable>>> {}

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setErrorHandler(exchange -> {
                exchange.response()
                    .headers(headers -> headers.status(500))
                    .body().raw()
                    .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Error: " + exchange.getError().getMessage(), Charsets.DEFAULT)));
        })).run();
    }
}
```

### HTTP compression

HTTP compression can be activated by configuration for request and/or response. For instance:

```java
public class Main {

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setApp_httpConfiguration(
                App_httpConfigurationLoader.load(configuration -> configuration
                    .http_server(server -> server
                        .decompression_enabled(true)
                        .compression_enabled(true)
                        .compression_level(6)
                    )
                )
            )
        ).run();
    }
}
```

Now if we send a request which accepts compression to the server, we should now receive a compressed response:

```plaintext
$ curl -i --compressed -H 'accept-encoding: gzip, deflate' http://localhost:8080
HTTP/1.1 200 OK
content-type: text/plain
server: inverno
content-encoding: gzip
content-length: 39

Hello
```

### TLS

In order to activate TLS, we need first to obtain a private key and a certificate stored in a keystore.

A self-signed certificate can be generated using `keytool`, the resulting keystore should be placed in `src/main/resources` to make it available as a module resource:

```plaintext
$ keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
```

Then we need to configure the server to activate TLS using the certificate:

```java
public class Main {

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setApp_httpConfiguration(
                App_httpConfigurationLoader.load(configuration -> configuration
                    .http_server(server -> server
                        .server_port(8443)
                        .tls_enabled(true)
                        .key_store(URI.create("module://io.inverno.example.app_http/keystore.jks"))
                        .key_alias("selfsigned")
                        .key_store_password("password")
                    )
                )
            )
        ).run();
    }
}
```

> When an application using the *http-server* module is packaged as an application image, you'll need to make sure TLS related modules from the JDK are included in the runtime image otherwise TLS might not work. You can refer to the [JDK providers documentation][jdk-providers] in the security developer's guide to find out which modules should be added depending on your needs. Most of the time you'll simply add `jdk.crypto.ec` module in the Inverno Maven plugin configuration:
> 
> ```xml
> <project>
>     <build>
>         <plugins>
>             <plugin>
>                 <groupId>io.inverno.tool</groupId>
>                 <artifactId>inverno-maven-plugin</artifactId>
>                 <executions>
>                     <execution>
>                         <configuration>
>                             <addModules>jdk.crypto.ec</addModules>
>                         </configuration>
>                     </execution>
>                 </executions>
>             </plugin>
>         </plugins>
>     </build>
> </project>
> ```


### Extend HTTP services

The HTTP server relies on a header service and a parameter converter to respectively decode HTTP headers and convert parameter values.

The *http-server* module defines a socket to plug custom `HeaderCodec` instances so the HTTP header service can be extended to decode custom HTTP headers as described in the [HTTP header service documentation](#http-header-service).

It also defines a socket to plug a custom parameter converter which is a basic `StringConverter` by default. Since we created the *app_http* module by composing *boot* and *http-server* modules, the parameter converter provided by the *boot* module should then override the default. This converter is a `StringCompositeConverter` which can be extended by injecting custom `CompoundDecoder` and/or `CompoundEncoder` instances in the *boot* module as described in the [composite converter documentation](#composite-converter).

To sum up, all we have to do to extend these services is to provide `HeaderCodec`, `CompoundDecoder` or `CompoundEncoder` beans in the *app_http* module.

## Wrap-up

If we put all we've just seen together, here is a complete example showing how to create a HTTP/2 server with HTTP compression using custom root and error handlers:

```java
package io.inverno.example.app_http;

import java.net.URI;
import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ErrorExchangeHandler;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeContext;
import io.inverno.mod.http.server.ExchangeHandler;

public class Main {

    @Bean
    public static interface RootHandler extends Supplier<ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>>> {}
    
    @Bean
    public static interface ErrorHandler extends Supplier<ErrorExchangeHandler<Throwable, ErrorExchange<Throwable>>> {}

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setApp_httpConfiguration(
                    App_httpConfigurationLoader.load(configuration -> configuration
                    .http_server(server -> server
                        // HTTP compression
                        .decompression_enabled(true)
                        .compression_enabled(true)
                        // TLS
                        .server_port(8443)
                        .tls_enabled(true)
                        .key_store(URI.create("module://io.inverno.example.app_http/keystore.jks"))
                        .key_alias("selfsigned")
                        .key_store_password("password")
                        // Enable HTTP/2
                        .h2_enabled(true)
                    )
                )
            )
            .setRootHandler(exchange -> {
                exchange.response()
                    .body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello from main!", Charsets.DEFAULT)));
            })
            .setErrorHandler(exchange -> {
                exchange.response()
                   .headers(headers -> headers.status(500))
                   .body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Error: " + exchange.getError().getMessage(), Charsets.DEFAULT)));
            })
        ).run();
    }
}
```

```plaintext
$ curl -i --insecure https://localhost:8443/
HTTP/2 200 
content-length: 16

Hello from main!
```
