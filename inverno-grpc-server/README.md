[inverno-javadoc]: https://inverno.io/docs/release/api/index.html
[inverno-tools-grpc-protoc-plugin]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-grpc-protoc-plugin

[grpc-protocol]: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
[grpc-core-concepts]: https://grpc.io/docs/what-is-grpc/core-concepts/
[grpc-compression]: https://grpc.io/docs/guides/compression/
[grpc-metadata]: https://grpc.io/docs/guides/metadata/
[grpc-cancellation]: https://grpc.io/docs/guides/cancellation/
[grpc-timeout]: https://grpc.io/docs/guides/deadlines/
[grpc-http-mapping]: https://github.com/grpc/grpc/blob/master/doc/http-grpc-status-mapping.md
[protocol-buffer]: https://protobuf.dev/
[protocol-buffer-compiler]: https://grpc.io/docs/protoc-installation/

[grpc-java-helloworld-example]: https://github.com/grpc/grpc-java/tree/master/examples/src/main/java/io/grpc/examples/helloworld

# gRPC Server

The Inverno *grpc-server* module allows to create reactive gRPC servers as described by the [gRPC over HTTP/2][grpc-protocol] protocol on top of the [http-server](#http-server) module.

It provides an API to create HTTP exchange handlers supporting the gRPC protocol. A gRPC exchange basically supports:

- the four kinds of gRPC service methods: unary, client streaming, server streaming and bidirectional streaming as defined by the [gRPC core concepts][grpc-core-concepts].
- [metadata][grpc-metadata] and especially encoding/decoding of Base64 protocol buffer binary metadata
- [message compression][grpc-compression] with built-in support for `gzip`, `deflate` and `snappy` message encodings
- [cancellation][grpc-cancellation]

This module requires a [net service](#net-service) which is usually provided by the *boot* module. One of *http-server* module or the *web-server* module (which compose the *http-server* module), although not required to bootstrap the module, is required to be able to expose HTTP/2 endpoints. In order to use the Inverno *grpc-server* module and expose a gRPC service endpoint, we should then declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_grpc_server {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.web.server;
    requires io.inverno.mod.grpc.server;
}
```

The *grpc-base* module which provides base gRPC API and services is composed as a transitive dependency in the *grpc-server* module and as a result it doesn't need to be specified here nor provided in an enclosing module.

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
            <artifactId>inverno-web-server</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-grpc-server</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-web-server:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-grpc-server:${VERSION_INVERNO_MODS}'
```

A gRPC service is basically exposed by using the `GrpcServer` bean to convert a unary, client streaming, server streaming or bidirectional streaming gRPC exchange handler into an HTTP server exchange handler which can then be injected either in the HTTP server controller or more conveniently in a Web route:

```java
package io.inverno.example.app_grpc_server;

import examples.HelloReply;
import examples.HelloRequest;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.server.WebRouter;
import io.inverno.mod.web.server.WebRoutesConfigurer;

@Bean( visibility = Visibility.PRIVATE )
public class GreeterRouteConfigurer implements WebRoutesConfigurer<ExchangeContext> {

    private final GrpcServer grpcServer;

    public GreeterRouteConfigurer(GrpcServer grpcServer) {
        this.grpcServer = grpcServer;
    }

    @Override
    public void configure(WebRoutable<ExchangeContext, ?> routes) {
        routes
            .route()
                .path(GrpcServiceName.of("helloworld", "Greeter").methodPath("SayHello"))             // /helloworld.Greeter/SayHello
                .method(Method.POST)
                .consumes(MediaTypes.APPLICATION_GRPC)
                .consumes(MediaTypes.APPLICATION_GRPC_PROTO)
                .handler(this.grpcServer.unary(                                                       // Create a unary exchange handler
                    HelloRequest.getDefaultInstance(),
                    HelloReply.getDefaultInstance(),
                    (GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> { // Handle the gRPC exchange
                        grpcExchange.response().value(grpcExchange.request().value()
                            .map(helloRequest -> HelloReply.newBuilder()
                                .setMessage("Hello " + helloRequest.getName())
                                .build()
                            )
                        );
                    }
                ));
    }
}
```

In above example, bean `greeterRouteConfigurer` is created in module *app_grpc_client*, the Web routes configurer is automatically registered to the Web server by the Inverno Web compiler to expose `helloworld.Greeter/SayHello` gRPC endpoint. When receiving a `HelloRequest` message the server simply responds with a corresponding `HelloReply` message.

> Note that the gRPC protocol is built on top of HTTP/2 and as such the underlying connection should be an HTTP/2 connection, trying to convey gRPC messages over an HTTP/1.x connection, although possible in theory, is discouraged as it will break interoperability and might lead to unpredictable behaviours.

```plaintext
2024-04-08 11:52:41,131 INFO  [main] i.i.c.v.Application - Inverno is starting...


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
     ║ Java version        : 22+36-snap                                                           ║
     ║ Java home           : /snap/openjdk/1735/jdk                                               ║
     ║                                                                                            ║
     ║ Application module  : io.inverno.example.app_grpc_server                                   ║
     ║ Application version : 1.0.0-SNAPSHOT                                                       ║
     ║ Application class   : io.inverno.example.app_grpc_server.Main                              ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ...                                                                                       ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


2024-04-08 11:52:41,141 INFO  [main] i.i.e.a.App_grpc_server - Starting Module io.inverno.example.app_grpc_server...
2024-04-08 11:52:41,142 INFO  [main] i.i.m.b.Boot - Starting Module io.inverno.mod.boot...
2024-04-08 11:52:41,357 INFO  [main] i.i.m.b.Boot - Module io.inverno.mod.boot started in 215ms
2024-04-08 11:52:41,358 INFO  [main] i.i.m.g.s.Server - Starting Module io.inverno.mod.grpc.server...
2024-04-08 11:52:41,358 INFO  [main] i.i.m.g.b.Base - Starting Module io.inverno.mod.grpc.base...
2024-04-08 11:52:41,474 INFO  [main] i.i.m.g.b.Base - Module io.inverno.mod.grpc.base started in 116ms
2024-04-08 11:52:41,477 INFO  [main] i.i.m.g.s.Server - Module io.inverno.mod.grpc.server started in 119ms
2024-04-08 11:52:41,477 INFO  [main] i.i.m.w.s.Server - Starting Module io.inverno.mod.web.server...
2024-04-08 11:52:41,477 INFO  [main] i.i.m.h.s.Server - Starting Module io.inverno.mod.http.server...
2024-04-08 11:52:41,478 INFO  [main] i.i.m.h.b.Base - Starting Module io.inverno.mod.http.base...
2024-04-08 11:52:41,483 INFO  [main] i.i.m.h.b.Base - Module io.inverno.mod.http.base started in 5ms
2024-04-08 11:52:41,633 INFO  [main] i.i.m.h.s.i.HttpServer - HTTP Server (epoll) listening on http://0.0.0.0:8080
2024-04-08 11:52:41,633 INFO  [main] i.i.m.h.s.Server - Module io.inverno.mod.http.server started in 155ms
2024-04-08 11:52:41,633 INFO  [main] i.i.m.w.s.Server - Module io.inverno.mod.web.server started in 156ms
2024-04-08 11:52:41,634 INFO  [main] i.i.e.a.App_grpc_server - Module io.inverno.example.app_grpc_server started in 500ms
2024-04-08 11:52:41,634 INFO  [main] i.i.c.v.Application - Application io.inverno.example.app_grpc_server started in 559ms
```

The `Greeter` service can be invoked using an [Inverno gRPC client](#grpc-client) but since this service is gRPC official HelloWorld example, we can also use [gRPC-java HelloWorld example][grpc-java-helloworld-example]:

```plaintext
$ $GRPC_JAVA_HOME/examples/build/install/examples/bin/hello-world-client Bob localhost:8080
avr. 08, 2024 11:58:42 AM io.grpc.examples.helloworld.HelloWorldClient greet
INFOS: Will try to greet Bob ...
avr. 08, 2024 11:58:42 AM io.grpc.examples.helloworld.HelloWorldClient greet
INFOS: Greeting: Hello Bob
$
```

Above example showed a programmatic way to expose gRPC service methods, gRPC works with [protocol buffer][protocol-buffer] which allows to specify messages and services in an interface description language (proto 3). The [protocol buffer compiler][protocol-buffer-compiler] is used to generate Java classes from `.proto` files, it can be extended with the [Inverno gRPC plugin][inverno-tools-grpc-protoc-plugin] in order to automatically generate Inverno Web routes configurers for gRPC services at build time.

For instance, providing the following protocol buffer service definition:

```protobuf
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}
```

The plugin generates abstract class `GreeterGrpcRouteConfigurer` implementing boilerplate code, we can then simply extend that class to implement the service logic:

```java
package io.inverno.example.app_grpc_server;

import examples.GreeterGrpcRouteConfigurer;
import examples.HelloReply;
import examples.HelloRequest;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import reactor.core.publisher.Mono;

@Bean( visibility = Visibility.PRIVATE )
public class GreeterController extends GreeterGrpcRouteConfigurer<ExchangeContext> {

    public GreeterController(GrpcServer grpcServer) {
        super(grpcServer);
    }

    @Override
    public Mono<HelloReply> sayHello(HelloRequest request) {
        return Mono.just(HelloReply.newBuilder().setMessage("Hello " + request.getName()).build());
    }
}
```

Above example simply handles `HelloRequest` message to produce corresponding `HelloReply` message, but it is also possible to access the `GrpcExchange` to access request metadata or set response metadata:

```java
package io.inverno.example.app_grpc_server;

import examples.GreeterGrpcRouteConfigurer;
import examples.HelloReply;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.grpc.server.GrpcExchange;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import java.util.Optional;

@Bean( visibility = Visibility.PRIVATE )
public class GreeterController extends GreeterGrpcRouteConfigurer<ExchangeContext> {

    public GreeterController(GrpcServer grpcServer) {
        super(grpcServer);
    }

    @Override
    public void sayHello(GrpcExchange.Unary<ExchangeContext, examples.HelloRequest, examples.HelloReply> grpcExchange) {
        Optional<String> requestMetadata = grpcExchange.request().metadata().get("SomeRequestMetadata");
        grpcExchange.response()
            .metadata(metadata -> metadata.set("SomeResponseMetadata", "someValue"))
            .value(grpcExchange.request().value()
                .map(helloRequest -> HelloReply.newBuilder().setMessage("Hello " + helloRequest.getName()).build())
            );
    }
}
```

> Note that the `GreeterController` class must be created as a `@Bean` in order for the service routes to be registered the Web server.

## Configuration

The *grpc-server* module operates on top of the *http-server* or *web-server* modules, as a result network configuration and server specific configuration are inherited from the [HTTP server](#http-server) or [Web server][#web-server] configuration. The *grpc-server* specific configuration basically conveys the *grpc-base* module configuration which configures the built-in message compressors. A specific configuration can be created in the application module to easily override the default configurations:

```java
package io.inverno.example.app_grpc_server;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.grpc.server.GrpcServerConfiguration;
import io.inverno.mod.web.server.WebServerConfiguration;

@Configuration
public interface App_grpc_serverConfiguration {

    @NestedBean
    GrpcServerConfiguration grpc_server();

    @NestedBean
    WebServerConfiguration web_server();
}
```

The Web server can then be configured. For instance, we can enable direct HTTP/2 over cleartext which is required to expose gRPC endpoints, configure the server port or the built-in gRPC message compressors:

```java
package io.inverno.example.app_grpc_server;

import io.inverno.core.v1.Application;
import io.inverno.mod.configuration.source.BootstrapConfigurationSource;

public class Main {

    public static void main(String[] args) throws IOException {
        Application.run(new App_grpc_server.Builder()
            .setApp_grpc_serverConfiguration(App_grpc_serverConfigurationLoader.load(configuration -> configuration
                .grpc_server(grpcServer -> grpcServer
                    .base(base -> base.compression_gzip_compressionLevel(6))
                )
                .web_server(webServer -> webServer
                    .http_server(httpServer -> httpServer
                        .server_port(8081)
                        .h2_enabled(true)
                    )
                )
            ))
        );
    }
}
```

The support for native transport or TLS secured connection is provided by the [HTTP server](#http-server) which must be configured accordingly. Although nothing prevents to send gRPC messages over an HTTP/1.x connection, this is discouraged as it will break interoperability and might result in unpredictable behaviours, particular care must be taken to ensure the server is properly configured to accept HTTP/2 connections.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

> You can also refer to the [configuration module documentation](#configuration-1) to get more details on how configuration works and more especially how you can from here define the server configuration in command line arguments, property files...

## gRPC exchange

The [gRPC protocol][grpc-core-concepts] specifies four kinds of service method that all fit into the exchange paradigm: unary RPC, client streaming RPC, server streaming RPC and bidirectional streaming RPC. The `GrpcServer` allows to convert a gRPC exchange handler handling one of those into a regular HTTP exchange handler that can be used to handle exchanges in the HTTP server controller or in a Web route. Protocol buffer being used to encode and decode client and server messages, default message instances are required when creating a gRPC exchange handler.

A `GrpcExchange` exposes a context, the `GrpcRequest` and the `GrpcResponse`.

### gRPC request

The gRPC server request exposes common request information, the request metadata as well as the request messages publisher. There are two kinds of gRPC requests: unary and streaming requests which are exposed depending on the kind of exchange: unary and server streaming exchanges expose unary requests whereas client streaming and bidirectional streaming exchanges expose streaming requests.

The service name and the request method can be obtained as follows:

```java
(GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
    // <package>.<service>
    GrpcServiceName serviceName = grpcExchange.request().getServiceName();
    // <method>
    String methodName = grpcExchange.request().getMethodName();
    // <package>.<service>/<method>
    String fullMethodName = grpcExchange.request().getFullMethodName();

    ...
}
```

Standard or custom request metadata including protocol buffer binary data encoded in Base64 can be accessed as follows:

```java
(GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
    GrpcInboundRequestMetadata metadata = grpcExchange.request().metadata();
    List<String> acceptMessageEncodings = metadata.getAcceptMessageEncoding();
    Optional<String> messageEncoding = metadata.getMessageEncoding();
    Optional<Duration> timeout = grpcExchange.request().metadata().getTimeout();
    Optional<String> customValue = metadata.get("custom");
    Optional<SomeMessage> customBinaryValue = metadata.getBinary("customBinary", SomeMessage.getDefaultInstance()); // -bin suffix is automatically added

    ...
}
```

When considering a unary or a server streaming exchange, the request message is exposed as a single publisher:

```java
(GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
    Mono<HelloRequest> value = grpcExchange.request().value(); // single request message
    ...
}

(GrpcExchange.ServerStreaming<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
    Mono<HelloRequest> value = grpcExchange.request().value(); // single request message
    ...
}
```

When considering a client streaming or a bidirectional streaming exchange, the request messages are exposed in a publisher:

```java
(GrpcExchange.ClientStreaming<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
    Publisher<HelloRequest> stream = grpcExchange.request().stream(); // multiple request message
    ...
}

(GrpcExchange.BidirectionalStreaming<ExchangeContext, HelloRequest, HelloReply> grpcExchange) -> {
    Publisher<HelloRequest> stream = grpcExchange.request().stream(); // multiple request message
    ...
}
```

### gRPC Exchange interceptor

A gRPC exchange is backed by an HTTP server exchange which can be intercepted just like any HTTP exchange. There's no specific gRPC API for interceptor, it is assumed the HTTP server or Web server API are enough to cover all use cases.

### gRPC Exchange context

The context is inherited from the HTTP server or the Web server, it is used to convey contextual information such as security, tracing... throughout the processing of the exchange and especially within interceptors.

### Unary gRPC exchange

A unary gRPC exchange corresponds to the request/response paradigm where a server receives exactly one message from the client and responds with exactly one message.

The following example shows how to expose a unary service method:

```java
...
@Override
public final void configure(WebRoutable<ExchangeContext, ?> routes) {
    routes
        .route()
            .path(SERVICE_NAME.methodPath("SayHello"))
            .method(Method.POST)
            .consumes(MediaTypes.APPLICATION_GRPC)
            .consumes(MediaTypes.APPLICATION_GRPC_PROTO)
            .handler(this.grpcServer.unary(
                SingleHelloRequest.getDefaultInstance(),
                SingleHelloReply.getDefaultInstance(),
                (GrpcExchange.Unary<ExchangeContext, SingleHelloRequest, SingleHelloReply> grpcExchange) -> {
                    grpcExchange.response().value(grpcExchange.request().value()
                        .map(request -> SingleHelloReply.newBuilder().setMessage("Hello " + request.getName()).build())
                    );
                }
            ));
}
...
```

The server receives one `SingleHelloRequest` message and sends one `SingleHelloReply` message in response.

### Client streaming gRPC exchange

A client streaming gRPC exchange corresponds to the stream/response paradigm where a server receives a stream of messages from the client and responds with exactly one message.

The following example shows how to expose a client streaming service method:

```java
...
@Override
public final void configure(WebRoutable<ExchangeContext, ?> routes) {
    routes
        .route()
            .path(SERVICE_NAME.methodPath("SayHelloToEverybody"))
            .method(Method.POST)
            .consumes(MediaTypes.APPLICATION_GRPC)
            .consumes(MediaTypes.APPLICATION_GRPC_PROTO)
            .handler(this.grpcServer.clientStreaming(
                SingleHelloRequest.getDefaultInstance(),
                GroupHelloReply.getDefaultInstance(),
                (GrpcExchange.ClientStreaming<ExchangeContext, SingleHelloRequest, GroupHelloReply> grpcExchange) -> {
                    grpcExchange.response().value(Flux.from(grpcExchange.request().stream())
                        .map(SingleHelloRequest::getName)
                        .collectList()
                        .map(names -> GroupHelloReply.newBuilder().setMessage("Hello ").addAllNames(names).build())
                    );
                }
            ));
}
...
```

The client receives multiple `SingleHelloRequest` messages and sends one `GroupHelloReply` message in response.

In such use case, the server typically aggregates all requests and only sends the response after all of them has been received and processed. Requests can be aggregated before processing or processed on the fly using a reduction operation.

For instance, above example could be rewritten using a reduction operation like this:

```java
...
@Override
public final void configure(WebRoutable<ExchangeContext, ?> routes) {
    routes
        .route()
            .path(SERVICE_NAME.methodPath("SayHelloToEverybody"))
            .method(Method.POST)
            .consumes(MediaTypes.APPLICATION_GRPC)
            .consumes(MediaTypes.APPLICATION_GRPC_PROTO)
            .handler(this.grpcServer.clientStreaming(
                SingleHelloRequest.getDefaultInstance(),
                GroupHelloReply.getDefaultInstance(),
                (GrpcExchange.ClientStreaming<ExchangeContext, SingleHelloRequest, GroupHelloReply> grpcExchange) -> {
                    grpcExchange.response().value(Flux.from(grpcExchange.request().stream())
                        .reduceWith(
                            () -> GroupHelloReply.newBuilder().setMessage("Hello "),
                            (reply, request) -> reply.addNames(request.getName())
                        )
                        .map(GroupHelloReply.Builder::build)
                    );
                }
            ));
}
...
```

### Server streaming gRPC exchange

A server streaming gRPC exchange corresponds to the request/stream paradigm where a server receives exactly one message and responds with a stream of messages.

The following example shows how to expose a server streaming service method:

```java
...
@Override
public final void configure(WebRoutable<ExchangeContext, ?> routes) {
    routes
        .route()
            .path(SERVICE_NAME.methodPath("SayHelloToEveryoneInTheGroup"))
            .method(Method.POST)
            .consumes(MediaTypes.APPLICATION_GRPC)
            .consumes(MediaTypes.APPLICATION_GRPC_PROTO)
            .handler(this.grpcServer.serverStreaming(
                GroupHelloRequest.getDefaultInstance(),
                SingleHelloReply.getDefaultInstance(),
                (GrpcExchange.ServerStreaming<ExchangeContext, GroupHelloRequest, SingleHelloReply> grpcExchange) -> {
                    grpcExchange.response().stream(grpcExchange.request().value()
                        .flatMapMany(request -> Flux.fromIterable(request.getNamesList())
                            .map(name -> SingleHelloReply.newBuilder().setMessage("Hello " + name).build())
                        )
                    );
                }
            ));
}
...
```

The server receives one `GroupHelloRequest` message and sends multiple `SingleHelloReply` message in response.

In such use case, the server sends response messages that the client can either process as soon as they are available or aggregate to process them all at once at the end of the call.

### Bidirectional streaming gRPC exchange

A bidirectional streaming gRPC exchange corresponds to the stream/stream paradigm where a server receives a stream of messages and responds with a stream of messages.

The following example shows how to expose a bidirectional streaming service method:

```java
...
@Override
public final void configure(WebRoutable<ExchangeContext, ?> routes) {
    routes
        .route()
            .path(SERVICE_NAME.methodPath("SayHelloToEveryoneInTheGroups"))
            .method(Method.POST)
            .consumes(MediaTypes.APPLICATION_GRPC)
            .consumes(MediaTypes.APPLICATION_GRPC_PROTO)
            .handler(this.grpcServer.bidirectionalStreaming(
                GroupHelloRequest.getDefaultInstance(),
                SingleHelloReply.getDefaultInstance(),
                (GrpcExchange.BidirectionalStreaming<ExchangeContext, GroupHelloRequest, SingleHelloReply> grpcExchange) -> {
                    grpcExchange.response().stream(Flux.from(grpcExchange.request().stream())
                        .map(GroupHelloRequest::getNamesList)
                        .flatMap(Flux::fromIterable)
                        .map(name -> SingleHelloReply.newBuilder().setMessage("Hello " + name).build())
                    );
                }
            ));
}
...
```

In above example, the server receives multiple `GroupHelloRequest` messages and sends multiple `SingleHelloReply` messages in response.

In such use case, both the server and the client can send messages when they are available. Messages sent by the server do not necessarily relates to messages sent by the client, the two streams are not necessarily correlated.

### Cancellation

A server can decide to cancel a gRPC when it can no longer produce result to the client or if it estimates the client won't be interested anymore as defined by [gRPC Cancellation][grpc-cancellation]. This is typically the case when it exceeds the timeout specified by the client in the gRPC request.

Considering a server streaming exchange, [gRPC request timeout][grpc-timeout] can be implemented as follows:

```java
(GrpcExchange.ServerStreaming<A, GroupHelloRequest, SingleHelloReply> grpcExchange) -> {
    Duration timeout = grpcExchange.request().metadata().getTimeout().orElse(Duration.ofSeconds(20)); // Get the grpc-timeout, defaults to 20 seconds when missing
    grpcExchange.response().stream(Flux.interval(Duration.ofSeconds(1))                               // A long-running stream that will eventually time out
        .map(index -> SingleHelloReply.newBuilder().setMessage("Hello " + index).build())
        .doOnCancel(() -> grpcExchange.cancel())                                                      // Cancel the exchange on cancel subscription
        .take(timeout)                                                                                // Cancel subscription when the request timeout is exceeded
    );
}
...
```

Cancelling the exchange basically cancels subscriptions to the request and response message publishers and sends a `RST_STREAM` frame to the client with code `CANCEL (0x8)`.

A gRPC server exchange is also canceled when a `GrpcException` with a `CANCELLED(1)` status is thrown in the response stream.

### gRPC error handler

The `GrpcServer` also provides an HTTP exchange error handler that can be injected in the HTTP server or Web server in order to map [HTTP errors to gRPC status code][grpc-http-mapping].

In a gRPC exchange the expected HTTP code returned by a server should always be `OK(200)` even in case of errors which should then be reported in the `grpc-status` response trailer. The gRPC server exchange handler catches most of the errors that can be thrown when it is invoked by the server or when processing request and response messages publishers. However, it can't catch errors thrown outside the scope of the handler, like in interceptors for example. These errors are normally handled by the Web server error exchange handlers.

The gRPC error handler must be used to circumvent that issue, it can be injected in the HTTP server controller or in an error web route.

```java
package io.inverno.example.app_grpc_server;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Bean.Visibility;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.grpc.server.GrpcServer;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.server.ErrorWebRouter;
import io.inverno.mod.web.server.ErrorWebRoutesConfigurer;

@Bean(visibility = Visibility.PRIVATE)
public class App_grpc_serverErrorWebRoutesConfigurer implements ErrorWebRoutesConfigurer<ExchangeContext> {

    private final GrpcServer grpcServer;

    public App_grpc_serverErrorWebRoutesConfigurer(GrpcServer grpcServer) {
        this.grpcServer = grpcServer;
    }

    @Override
    public void configure(ErrorWebRoutable<ExchangeContext, ?> errorRoutes) {
        errorRoutes
            .route()
            .consumes(MediaTypes.APPLICATION_GRPC)
            .consumes(MediaTypes.APPLICATION_GRPC_JSON)
            .consumes(MediaTypes.APPLICATION_GRPC_PROTO)
            .handler(this.grpcServer.errorHandler());
    }
}
```

In case of errors, it basically makes sure the HTTP response status is `OK(200)`, maps the HTTP error to a gRPC status code as define by [HTTP to gRPC Status Code Mapping][grpc-http-mapping] and sets in the response trailers.
