[inverno-javadoc]: https://inverno.io/docs/release/api/index.html
[inverno-tools-grpc-protoc-plugin]: https://github.com/inverno-io/inverno-tools/tree/master/inverno-grpc-protoc-plugin

[grpc-protocol]: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
[grpc-core-concepts]: https://grpc.io/docs/what-is-grpc/core-concepts/
[grpc-compression]: https://grpc.io/docs/guides/compression/
[grpc-metadata]: https://grpc.io/docs/guides/metadata/
[grpc-cancellation]: https://grpc.io/docs/guides/cancellation/
[protocol-buffer]: https://protobuf.dev/
[protocol-buffer-compiler]: https://grpc.io/docs/protoc-installation/

# gRPC Client

The Inverno *grpc-client* module allows to creates reactive gRPC clients as described by the [gRPC over HTTP/2][grpc-protocol] protocol on top of the [http-client](#http-client) module.

It provides an API to transform HTTP client exchanges into gRPC exchanges supporting the gRPC protocol. A gRPC exchange basically supports:

- the four kinds of gRPC service methods: unary, client streaming, server streaming and bidirectional streaming as defined by the [gRPC core concepts][grpc-core-concepts].
- [metadata][grpc-metadata] and especially encoding/decoding of protocol buffer binary metadata
- [message compression][grpc-compression] with built-in support for `gzip`, `deflate` and `snappy` message encodings
- [cancellation][grpc-cancellation]

This module requires a [net service](#net-service) which is usually provided by the *boot* module. The *http-client* module, although not required to bootstrap the module, is required to be able to create HTTP/2 clients. In order to use the Inverno *grpc-client* module and invoke a gRPC service method, we should then declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_grpc_client {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.http.client;
    requires io.inverno.mod.grpc.client;
}
```

The *grpc-base* module which provides base gRPC API and services is composed as a transitive dependency in the *grpc-client* module and as a result it doesn't need to be specified here nor provided in an enclosing module.

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
            <artifactId>inverno-http-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-grpc-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-http-client:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-grpc-client:${VERSION_INVERNO_MODS}'
...
```

A gRPC service is basically invoked by converting an HTTP/2 client exchange into a unary, client streaming, server streaming or bidirectional streaming gRPC exchange using the `GrpcClient` bean:

```java
package io.inverno.example.app_grpc_client;

import examples.HelloReply;
import examples.HelloRequest;
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.client.GrpcClient;
import io.inverno.mod.grpc.client.GrpcExchange;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import java.util.Set;

public class Main {

    @Bean
    public static class Greeter {

        private final Endpoint<ExchangeContext> endpoint;

        private final GrpcClient grpcClient;

        public Greeter(GrpcClient grpcClient, HttpClient httpClient) {
            this.grpcClient = grpcClient;
            this.endpoint = httpClient
                .endpoint("localhost", 8080)
                .configuration(HttpClientConfigurationLoader.load(configuration ->
                    configuration.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0))                                 // enable direct HTTP/2
                ))
                .build();                                                                                              // Create the endpoint
        }

        public HelloReply sayHello(HelloRequest request) {
            return this.endpoint
                .exchange()                                                                                            // Create the HTTP client exchange
                .<GrpcExchange.Unary<ExchangeContext, HelloRequest, HelloReply>>map(exchange -> this.grpcClient.unary( // Convert to a unary gRPC exchange
                    exchange,
                    GrpcServiceName.of("helloworld", "Greeter"),                                                       // service name
                    "SayHello",                                                                                        // method name
                    HelloRequest.getDefaultInstance(),                                                                 // request message type
                    HelloReply.getDefaultInstance()                                                                    // response message type
                ))
                .flatMap(grpcExchange -> {
                    grpcExchange.request().value(request);                                                             // Set the request
                    return grpcExchange.response().flatMap(GrpcResponse.Unary::value);                                 // Get the response
                })
                .block();                                                                                              // Get a connection, send the request and receive the response
        }
    }

    public static void main(String[] args) {
        App_grpc_client app_grpc_client = Application.run(new App_grpc_client.Builder());
        try {
            HelloReply response = app_grpc_client.greeter().sayHello(HelloRequest.newBuilder()
                .setName("Bob")
                .build()
            );
            System.out.println("Received: " + response.getMessage());
        }
        finally {
            app_grpc_client.stop();
        }
    }
}
```

In above example, module *app_grpc_client* creates the `Greeter` bean which uses the `HttpClient` to obtain an `Endpoint` to connect to `localhost` in plain HTTP/2. When invoking the `sayHello()` method, an HTTP client exchange is created, it is converted to a unary gRPC exchange which allows to set the request message and to return the response message. The request is sent when the response publisher is subscribed, the gRPC response message is eventually returned and displayed to the standard output before the module is finally stopped.

> Note that the gRPC protocol is built on top of HTTP/2 and as such the underlying connection should be an HTTP/2 connection, trying to convey gRPC messages over an HTTP/1.x connection, although possible in theory, is discouraged as it will break interoperability and might lead to unpredictable behaviours.

```plaintext
2024-04-05 15:02:41,083 INFO  [main] i.i.c.v.Application - Inverno is starting...


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
     ║ Application module  : io.inverno.example.app_grpc_client                                   ║
     ║ Application version : 1.0.0-SNAPSHOT                                                       ║
     ║ Application class   : io.inverno.example.app_grpc_client.Main                              ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ...                                                                                       ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


2024-04-05 15:02:41,095 INFO  [main] i.i.e.a.App_grpc_client - Starting Module io.inverno.example.app_grpc_client...
2024-04-05 15:02:41,095 INFO  [main] i.i.m.b.Boot - Starting Module io.inverno.mod.boot...
2024-04-05 15:02:41,323 INFO  [main] i.i.m.b.Boot - Module io.inverno.mod.boot started in 227ms
2024-04-05 15:02:41,323 INFO  [main] i.i.m.g.c.Client - Starting Module io.inverno.mod.grpc.client...
2024-04-05 15:02:41,324 INFO  [main] i.i.m.g.b.Base - Starting Module io.inverno.mod.grpc.base...
2024-04-05 15:02:41,347 INFO  [main] i.i.m.g.b.Base - Module io.inverno.mod.grpc.base started in 23ms
2024-04-05 15:02:41,349 INFO  [main] i.i.m.g.c.Client - Module io.inverno.mod.grpc.client started in 25ms
2024-04-05 15:02:41,350 INFO  [main] i.i.m.h.c.Client - Starting Module io.inverno.mod.http.client...
2024-04-05 15:02:41,350 INFO  [main] i.i.m.h.b.Base - Starting Module io.inverno.mod.http.base...
2024-04-05 15:02:41,355 INFO  [main] i.i.m.h.b.Base - Module io.inverno.mod.http.base started in 5ms
2024-04-05 15:02:41,362 INFO  [main] i.i.m.h.c.Client - Module io.inverno.mod.http.client started in 11ms
2024-04-05 15:02:41,387 INFO  [main] i.i.e.a.App_grpc_client - Module io.inverno.example.app_grpc_client started in 301ms
2024-04-05 15:02:41,388 INFO  [main] i.i.c.v.Application - Application io.inverno.example.app_grpc_client started in 357ms
2024-04-05 15:02:41,588 INFO  [inverno-io-epoll-1-1] i.i.m.h.c.i.AbstractEndpoint - HTTP/2.0 Client (epoll) connected to http://localhost:8080
Received: Hello Bob
2024-04-05 15:02:41,652 INFO  [main] i.i.e.a.App_grpc_client - Stopping Module io.inverno.example.app_grpc_client...
2024-04-05 15:02:41,655 INFO  [main] i.i.m.b.Boot - Stopping Module io.inverno.mod.boot...
2024-04-05 15:02:41,656 INFO  [main] i.i.m.b.Boot - Module io.inverno.mod.boot stopped in 0ms
2024-04-05 15:02:41,656 INFO  [main] i.i.m.g.c.Client - Stopping Module io.inverno.mod.grpc.client...
2024-04-05 15:02:41,656 INFO  [main] i.i.m.g.b.Base - Stopping Module io.inverno.mod.grpc.base...
2024-04-05 15:02:41,657 INFO  [main] i.i.m.g.b.Base - Module io.inverno.mod.grpc.base stopped in 0ms
2024-04-05 15:02:41,657 INFO  [main] i.i.m.g.c.Client - Module io.inverno.mod.grpc.client stopped in 0ms
2024-04-05 15:02:41,657 INFO  [main] i.i.m.h.c.Client - Stopping Module io.inverno.mod.http.client...
2024-04-05 15:02:41,657 INFO  [main] i.i.m.h.b.Base - Stopping Module io.inverno.mod.http.base...
2024-04-05 15:02:41,657 INFO  [main] i.i.m.h.b.Base - Module io.inverno.mod.http.base stopped in 0ms
2024-04-05 15:02:41,657 INFO  [main] i.i.m.h.c.Client - Module io.inverno.mod.http.client stopped in 0ms
2024-04-05 15:02:41,657 INFO  [main] i.i.e.a.App_grpc_client - Module io.inverno.example.app_grpc_client stopped in 5ms
```

Above example showed a programmatic way to invoke gRPC service methods, gRPC works with [protocol buffer][protocol-buffer] which allows to specify messages and services in an interface description language (proto 3). The [protocol buffer compiler][protocol-buffer-compiler] is used to generate Java classes from `.proto` files, it can be extended with the [Inverno gRPC plugin][inverno-tools-grpc-protoc-plugin] in order to automatically generate Inverno client stubs at build time.

For instance, providing the following protocol buffer service definition:

```protobuf
service Greeter {
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}
```

The plugin generates bean `GreeterGrpcClient` implementing boiler plate code and greetly simplifies the application above application:

```java
package io.inverno.example.app_grpc_client;

import examples.HelloReply;
import examples.HelloRequest;
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.client.GrpcClient;
import io.inverno.mod.grpc.client.GrpcExchange;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import java.util.Set;

public class Main {

    public static void main(String[] args) {
        App_grpc_client app_grpc_client = Application.run(new App_grpc_client.Builder());
        try {
            try(GreeterGrpcClient.Stub<ExchangeContext> stub = app.greeterGrpcClient().createStub("localhost", 8080)) {
                HelloReply response = stub
                    .sayHello(HelloRequest.newBuilder()
                        .setName("Bob")
                        .build()
                    )
                    .block();
                System.out.println("Received: " + response.getMessage());
            }
        }
        finally {
            app_grpc_client.stop();
        }
    }
}
```

> Note that in this example the endpoint is directly created by the `GreeterGrpcClient` and automatically closed at the end of the try-with-resources statement, it uses the `HttpClient` bean which must then be configured to connect with HTTP/2. It is also possible to create the endpoint instance and pass it to the stub instead.

## Configuration

The *grpc-client* module operates on top of the *http-client* module, as a result network configuration and client specific configuration are inherited from the [HTTP client](#http-client) configuration. The *grpc-client* specific configuration basically conveys the *grpc-base* module configuration which configures the built-in message compressors. A specific configuration can be created in the application module to easily override the default configurations:

```java
package io.inverno.example.app_grpc_client;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.grpc.client.GrpcClientConfiguration;
import io.inverno.mod.http.client.HttpClientConfiguration;

@Configuration
public interface App_grpc_clientConfiguration {

    @NestedBean
    GrpcClientConfiguration grpc_client();

    @NestedBean
    HttpClientConfiguration http_client();
}
```

This should be enough for exposing a configuration bean in the *app_grpc_client* module that let us setup the client:

```java
package io.inverno.example.app_grpc_client;

import examples.HelloReply;
import examples.HelloRequest;
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.grpc.base.GrpcServiceName;
import io.inverno.mod.grpc.client.GrpcClient;
import io.inverno.mod.grpc.client.GrpcExchange;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws IOException {
        App_grpc_client app = Application.run(new App_grpc_client.Builder()
            .setApp_grpc_clientConfiguration(
                App_grpc_clientConfigurationLoader.load(configuration -> configuration
                    .grpc_client(grpcClient -> grpcClient
                        .base(base -> base.compression_gzip_compressionLevel(6))
                    )
                    .http_client(httpClient -> httpClient
                        .http_protocol_versions(Set.of(HttpVersion.HTTP_2_0))
                        .request_timeout(600000l)
                    )
                )
            )
        );
        ...
    }
}
```

In above code, we have set:

- the gzip message compression level to 6
- the client to connect using HTTP/2 protocol only (required by gRPC protocol)
- the request timeout to 10 minutes

> It is important to be aware that the HTTP client will terminate requests that exceeds the request timeout (i.e. take longer than the timeout to complete). Increasing the request timeout can then be required if you intend to invoke long running gRPC services like server or bidirectional streaming service methods which might take longer than the default 60 seconds to complete. Implementing reconnection mechanism is also highly recommended for long polling use cases.

The support for native transport or TLS secured connection is provided by the [HTTP client](#http-client) which must be configured accordingly. Although nothing prevents to send gRPC messages over an HTTP/1.x connection, this is discouraged and might as it will break interoperability and might result in unpredictable behaviours, particular care must be taken to ensure the client is properly configured to create HTTP/2 connections.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

> You can also refer to the [configuration module documentation](#configuration-1) to get more details on how configuration works and more especially how you can from here define the client configuration in command line arguments, property files...

## gRPC exchange

The [gRPC protocol][grpc-core-concepts] specifies four kinds of service method that all fit into the exchange paradigm: unary RPC, client streaming RPC, server streaming RPC and bidirectional streaming RPC. The `GrpcClient` allows to convert an HTTP client exchange into a `GrpcExchange` specific to each kind of service method. Protocol buffer being used to encode and decode client and server messages, default message instances are required when creating a gRPC exchange.

A `GrpcExchange` exposes a context, the `GrpcRequest` and the `GrpcResponse`.

### gRPC request

The gRPC client request exposes common request information and allows to specify the request metadata as well as request messages publisher. There are two kinds of gRPC requests: unary and streaming requests which are exposed depending on the kind exchange: unary and server streaming exchanges expose unary requests whereas client streaming and bidirectional streaming exchanges expose streaming requests.

The service name and the request method can be obtained as follows:

```java
endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to gRPC exchange
    .flatMap(grpcExchange -> {
        // <package>.<service>
        GrpcServiceName serviceName = grpcExchange.request().getServiceName();
        // <method>
        String methodName = grpcExchange.request().getMethodName();
        // <package>.<service>/<method>
        String fullMethodName = grpcExchange.request().getFullMethodName();

        return grpcExchange.response();
    })
    ...
```

Standard or custom request metadata including protocol buffer binary data endoded in Base64 can be provided as follows:

```java
endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to gRPC exchange
    .flatMap(grpcExchange -> {
        grpcExchange.request()
            .metadata(metadata -> metadata
                .acceptMessageEncoding(List.of(GrpcHeaders.VALUE_GZIP, GrpcHeaders.VALUE_IDENTITY))
                .messageEncoding(GrpcHeaders.VALUE_GZIP)
                .timeout(Duration.ofSeconds(5))
                .set("custom", "someValue")
                .setBinary("customBinary", SomeMessage.newBuilder().setValue("abc").build()) // -bin suffix is automatically added
            );

        return grpcExchange.response();
    })
```

When considering a unary or server streaming exchange, the request message can be set either synchronously or in a reactive way:

```java
endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to a unary or server streaming gRPC exchange
    .flatMap(grpcExchange -> {
        // set the request message
        grpcExchange.request().value(SingleHelloRequest.newBuilder().setName("Bob").build());

        // set the request message in a reactive way
        grpcExchange.request().value(Mono.fromSupplier(() -> SingleHelloRequest.newBuilder().setName("Bob").build()));

        return grpcExchange.response();
    })
    ...
```

When considering a client streaming or bidirectional streaming exchange, the request messages publisher can be set as follows:

```java
endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to a client streaming or bidirectional streaming gRPC exchange
    .flatMap(grpcExchange -> {
        grpcExchange.request().stream(
            Flux.just("Bob", "Bill", "Jane")
                .map(name -> SingleHelloRequest.newBuilder().setName(name).build())
        );

        return grpcExchange.response();
    })
    ...
```

### gRPC response

The gRPC client response exposes the response metadata and trailers metadata as well as the response messages publisher. As for the request, there are two kinds of gRPC response: unary and streaming responses which are exposed depending on the kind exchange: unary and client streaming exchanges expose a unary responses whereas server streaming and bidirectional streaming exchanges expose streaming responses.

Standard or custom response metadata including protocol buffer binary data encoded in Base64 can be accessed as follows:

```java
endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to gRPC exchange
    .flatMap(grpcExchange ->
        // set the request
        ...
        return grpcExchange.response();
    })
    .map(response -> {
        GrpcInboundResponseMetadata metadata = grpcExchange.response().metadata();
        List<String> acceptMessageEncodings = metadata.getAcceptMessageEncoding();
        Optional<String> messageEncoding = metadata.getMessageEncoding();
        Optional<String> customValue = metadata.get("custom");
        Optional<SomeMessage> customBinaryValue = metadata.getBinary("customBinary", SomeMessage.getDefaultInstance()); // -bin suffix is automatically added

        return response;
    })
    ...
```

Response trailers metadata are available after the complete response have been received and exposes the final gRPC stratus and message:

```java
endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to a unary or client streaming gRPC exchange
    .flatMap(grpcExchange ->
        // set the request
        ...
        return grpcExchange.response();
    })
    .flatMap(response -> {
        return response.value()
            .doOnTerminate(() -> System.out.println("gRPC status: " + response.trailersMetadata().getStatus()));
    })
    .block();
```

> Note that before the response message publisher completes, there are no trailers and `null` shall be returned.

When considering a unary or a client streaming exchange, the response message is exposed as a single publisher:

```java
Reply reply = endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to a unary or client streaming gRPC exchange
    .flatMap(grpcExchange -> {
        // set the request
        ...
        return grpcExchange.response();
    })
    .flatMap(response -> response.value()) // single response message
    .block();
```

When considering a unary or a client streaming exchange, the response messages are exposed in a publisher:

```java
List<Reply> replies = endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to a server streaming or bidirectional streaming gRPC exchange
    .flatMap(grpcExchange -> {
        // set the request
        ...
        return grpcExchange.response();
    })
    .flatMapMany(response -> response.stream()) // multiple response messages
    .collect(Collectors.toList())
    .block();
```

### gRPC Exchange interceptor

A gRPC exchange is backed by an HTTP exchange which can be intercepted just like any HTTP exchange. There's no specific gRPC API for interceptor, it is assumed the HTTP client API is enough to cover all use cases.

### gRPC Exchange context

The context is inherited from the HTTP client, it is used to convey contextual information such as security, tracing... throughout the processing of the exchange and especially within interceptors.

### Unary gRPC exchange

A unary gRPC exchange corresponds to the request/response paradigm where a client sends exactly one message and receives exacly one message from the server.

The following example shows how to invoke a unary service method:

```java
Endpoint<ExchangeContext> endpoint = ...
SingleHelloReply reply = endpoint.exchange()
    .<GrpcExchange.Unary<ExchangeContext, SingleHelloRequest, SingleHelloReply>>map(exhange -> grpcClient.unary(
        exhange,
        GrpcServiceName.of("examples", "HelloService"),
        "SayHello",
        SingleHelloRequest.getDefaultInstance(),
        SingleHelloReply.getDefaultInstance()
    ))
    .flatMap(grpcExchange -> {
        grpcExchange.request().value(SingleHelloRequest.newBuilder().setName("Bob").build());
        return grpcExchange.response();
    })
    .flatMap(GrpcResponse.Unary::value)
    .block();
```

The client sends one `SingleHelloRequest` message and the server sends one `SingleHelloReply` message in response.

### Client streaming gRPC exchange

A client streaming gRPC exchange corresponds to the stream/response paradigm where a client sends a stream of messages and receives exactly one message from the server.

The following example shows how to invoke a client streaming service method:

```java
Endpoint<ExchangeContext> endpoint = ...
GroupHelloReply reply = endpoint.exchange()
    .<GrpcExchange.ClientStreaming<ExchangeContext, SingleHelloRequest, GroupHelloReply>>map(exhange -> grpcClient.clientStreaming(
        exhange,
        GrpcServiceName.of("examples", "HelloService"),
        "SayHelloToEverybody",
        SingleHelloRequest.getDefaultInstance(),
        GroupHelloReply.getDefaultInstance()
    ))
    .flatMap(grpcExchange -> {
        grpcExchange.request().stream(Flux.just("Bob", "Bill", "Jane")
            .map(name -> SingleHelloRequest.newBuilder().setName(name).build())
        );
        return grpcExchange.response();
    })
    .flatMap(GrpcResponse.Unary::value)
    .block();
```

The client sends multiple `SingleHelloRequest` messages and the server sends one `GroupHelloReply` message in response.

In such use case, the server typically aggregates all requests and only sends the response after all of them has been received and processed. All requests can be aggregated before processing the response message or processed on the fly using a reduction operation.

### Server streaming gRPC exchange

A server streaming gRPC exchange corresponds to the request/stream paradigm where a client sends exactly one message and receives a stream of messages from the server.

The following example shows how to invoke a server streaming service method:

```java
Endpoint<ExchangeContext> endpoint = ...
List<SingleHelloReply> replies = endpoint.exchange()
    .<GrpcExchange.ServerStreaming<ExchangeContext, GroupHelloRequest, SingleHelloReply>>map(exhange -> grpcClient.serverStreaming(
        exhange,
        GrpcServiceName.of("examples", "HelloService"),
        "SayHelloToEveryoneInTheGroup",
        GroupHelloRequest.getDefaultInstance(),
        SingleHelloReply.getDefaultInstance()
    ))
    .flatMap(grpcExchange -> {
        grpcExchange.request().value(GroupHelloRequest.newBuilder().addAllNames(List.of("Bob", "Bill", "Jane")).build());
        return grpcExchange.response();
    })
    .flatMapMany(GrpcResponse.Streaming::stream)
    .collect(Collectors.toList())
    .block();
```

The client sends one `GroupHelloRequest` message and the server sends multiple `SingleHelloReply` message in response.

In such use case, the server sends response messages that the client can either process as soon as they are available or aggregate to process them all at once at the end of the call.

### Bidirectional streaming gRPC exchange

A bidirectional streaming gRPC exchange corresponds to the stream/stream paradigm where a client sends a stream of messages and receives a stream of messages from the server.

The following example shows how to invoke a bidirectional streaming service method:

```java
Endpoint<ExchangeContext> endpoint = ...
List<SingleHelloReply> REPLIES = endpoint.exchange()
    .<GrpcExchange.BidirectionalStreaming<ExchangeContext, SingleHelloRequest, SingleHelloReply>>map(exhange -> grpcClient.bidirectionalStreaming(
        exhange,
        GrpcServiceName.of("examples", "HelloService"),
        "sayHelloToEveryone",
        SingleHelloRequest.getDefaultInstance(),
        SingleHelloReply.getDefaultInstance()
    ))
    .flatMap(grpcExchange -> {
        grpcExchange.request().stream(Flux.just("Bob", "Bill", "Jane")
            .map(name -> SingleHelloRequest.newBuilder().setName(name).build())
        );
        return grpcExchange.response();
    })
    .flatMapMany(GrpcResponse.Streaming::stream)
    .collect(Collectors.toList())
    .block();
```

In above example, the client sends multiple `SingleHelloRequest` messages and the server sends multiple `SingleHelloReply` messages in response.

In such use case, both the server and the client can send messages when they are available. Messages sent by the server do not necessarily relates to messages sent by the client, the two streams are not necessarily correlated.

### Cancellation

A client can decide to cancel a gRPC when it is no longer interested in the result of a gRPC call as defined by [gRPC Cancellation][grpc-cancellation].

```java
endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to a server streaming or bidirectional streaming gRPC exchange
    .flatMapMany(grpcExchange -> {
        // set the request
        ...
        return grpcExchange.response()
            .flatMapMany(response -> response.stream())
            .doOnNext(message -> {
                if(...) { // Check cancellation conditions then cancel
                    exchange.cancel();
                }
            });
    })
    .subscribe(...);
```

Cancelling the exchange basically cancels subscriptions to the request and response message publishers and sends a `RST_STREAM` frame to the server with code `CANCEL (0x8)`. When receiving this signal, the server is expected to do the same and also propagate the cancellation to any outgoing gRPC calls.

A gRPC client exchange is also canceled when a `GrpcException` with a `CANCELLED(1)` status is thrown in the request or response streams. But a more elegant way to cancel a gRPC exchange is to simply cancel the response subscription:

```java
Disposable disposable = endpoint.exchange()
    .map(exhange -> grpcClient...) // converts HTTP exchange to a server streaming or bidirectional streaming gRPC exchange
    .flatMapMany(grpcExchange -> {
        // set the request
        ...
        return grpcExchange.response();
    })
    .flatMapMany(GrpcResponse.Streaming::stream)
    .subscribe(...);

disposable.cancel();
```

> The reactive nature of the Inverno framework makes this quite natural, the exchange paradigm implemented in HTTP client and server already considers request and response as data streams that can be cancelled anytime. Considering a server making outgoing HTTP calls chained to an original request, those will be automatically canceled when the original request is canceled and this is especially the case for gRPC.
