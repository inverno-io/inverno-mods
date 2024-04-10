[grpc-protocol]: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md

# gRPC Base

The Inverno *grpc-base* module defines the foundational API for creating gRPC clients and servers. It also provides common gRPC services like the message compressor service.

In order to use the Inverno *grpc-base* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    requires io.inverno.mod.grpc.base;
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
            <artifactId>inverno-grpc-base</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-grpc-base:${VERSION_INVERNO_MODS}'
...
```

The *grpc-base* module is usually provided as a transitive dependency by other gRPC modules, the *grpc-client* and *grpc-server* modules in particular, so it might not be necessary to include it explicitly.

## gRPC base API

The base gRPC base API defines common classes and interfaces for implementing gRPC clients and servers. This includes:

- common gRPC exchange API
- gRPC service name
- gRPC status enumerations
- gRPC exceptions
- gRPC message compressor API
- gRPC metadata

## gRPC message compressor service

The gRPC message compressor service is used to resolve a message compressor matching gRPC message encoding as defined by the [gRPC protocol][[grpc-protocol]].

The `GrpcMessageCompressor` interface defines methods to compress and uncompress raw message data (i.e. `ByteBuf`), a gRPC message compressor can be resolved from the `GrpcMessageCompressorService` as follows:

```java
Base grpcBase = ...
GrpcMessageCompressorService messageCompressorService = grpcBase.messageCompressorService();

// Returns the first matching message compressor from the list of encodings or an empty optional if there is no matching message compressor
Optional<GrpcMessageCompressor> messageCompressor = messageCompressorService.getMessageCompressor("gzip","deflate");

// Returns the list of supported message encodings
Set<String> supportedMessageEncodings = messageCompressorService.getMessageEncodings();
```

The module has four built-in message compressor implementations: `identity`, `gzip`, `deflate` and `snappy`. Additional custom compressors can be injected in the module to extend its capabilities.

For instance, we could create a `BrotliGrpcMessageCompressor` to compress/uncompress `br` encoded messages. It must be injected in the *grpc-base* module either explicitly when creating the module or through dependency injection.

```java
NetService netService = ...
Base grpcBase = new Base.Builder(netService)
    .setMessageCompressors(List.of(new BrotliGrpcMessageCompressor())
    .build();

grpcBase.start();

// Returns brotli message compressor
Optional<GrpcMessageCompressor> messageCompressor = messageCompressorService.getMessageCompressor("br");

grpcBase.stop();
```

The *grpc-base* module is usually composed in other modules and as a result dependency injection should work just fine, so custom compressors simply need to be declared as beans in the enclosing module.

## Configuration

The *grpc-base* module exposes the `GrpcBaseConfiguration` which allows to configure built-in message compressors. That configuration is typically conveyed by the *grpc-client* and *grpc-server* modules which compose the *grpc-base* module.