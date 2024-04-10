[inverno-io]: https://www.inverno.io
[inverno-dist-root]: https://github.com/inverno-io/inverno-dist
[inverno-core-root]: https://github.com/inverno-io/inverno-core
[inverno-tools-root]: https://github.com/inverno-io/inverno-tools
[inverno-core-root-doc]: https://github.com/inverno-io/inverno-core/tree/master/doc/reference-guide.md
[inverno-mods-root-doc]: https://github.com/inverno-io/inverno-mods/tree/master/doc/reference-guide.md
[inverno-examples-root]: https://github.com/inverno-io/inverno-examples

[redis]: https://redis.io/
[lettuce]: https://lettuce.io
[vertx-sql-client]: https://github.com/eclipse-vertx/vertx-sql-client
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0
[ldap]: https://en.wikipedia.org/wiki/Lightweight_Directory_Access_Protocol
[grpc]: https://grpc.io/
[grpc-over-http2]: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
[grpc-core-concepts]: https://grpc.io/docs/what-is-grpc/core-concepts/
[protobuf]: https://protobuf.dev/

[rfc7515]: https://datatracker.ietf.org/doc/html/rfc7515
[rfc7516]: https://datatracker.ietf.org/doc/html/rfc7516
[rfc7517]: https://datatracker.ietf.org/doc/html/rfc7517
[rfc7518]: https://datatracker.ietf.org/doc/html/rfc7518
[rfc7519]: https://datatracker.ietf.org/doc/html/rfc7519
[rfc7638]: https://datatracker.ietf.org/doc/html/rfc7638
[rfc7797]: https://datatracker.ietf.org/doc/html/rfc7797
[rfc8037]: https://datatracker.ietf.org/doc/html/rfc8037
[rfc8812]: https://datatracker.ietf.org/doc/html/rfc8812

# Inverno Modules

[![CI/CD](https://github.com/inverno-io/inverno-mods/actions/workflows/maven.yml/badge.svg)](https://github.com/inverno-io/inverno-mods/actions/workflows/maven.yml)

The [Inverno modules framework][inverno-io] project provides a collection of components for building highly modular and powerful applications on top of the [Inverno IoC/DI framework][inverno-core-root].

While being fully integrated, any of these modules can also be used individually in any application thanks to the high modularity and low footprint offered by the Inverno framework.

The objective is to provide a complete consistent set of high end tools and components for the development of fast and maintainable applications.

## Using a module

Modules can be used in a Inverno module by defining dependencies in the module descriptor. For instance you can create a Web application module using the *boot* and *web-server* modules:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.webApp {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.web.server;
}
```

A simple microservice application can then be created in a few lines of code as follows:

```java
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.web.server.annotation.WebController;
import io.inverno.mod.web.server.annotation.WebRoute;

@Bean
@WebController
public class MainController {

    @WebRoute( path = "/message", produces = MediaTypes.TEXT_PLAIN)
    public String getMessage() {
        return "Hello, world!";
    }

    public static void main(String[] args) {
        Application.with(new WebApp.Builder()).run();
    }
}
```

Please refer to [Inverno distribution][inverno-dist-root] for detailed setup and installation instructions. 

Comprehensive reference documentations are available for [Inverno core][inverno-core-root-doc] and [Inverno modules][inverno-mods-root-doc].

Several example projects showing various features are also available in the [Inverno example project][inverno-examples-root]. They can also be used as templates to start new Inverno application or component projects.

Feel free to report bugs and feature requests in GitHub's issue tracking system if you ran in any issue or wish to see some new functionalities implemented in the framework.

## Available modules

The framework currently provides the following modules.

### inverno-base

The foundational APIs of the Inverno framework modules:

- Conversion API used to convert objects from/to other objects
- Concurrent API defining the reactive threading model API
- Net API providing URI manipulation as well as low level network client and server utilities
- Reflect API for manipulating parameterized type at runtime
- Resource API to read/write any kind of resources (eg. file, zip, jar, classpath, module...)

### inverno-boot

The boot Inverno module provides base services to an application:

- the reactor which defines the reactive threading model of an application
- a net service used for the implementation of optimized network clients and servers
- a media type service used to determine the media type of a resource
- a resource service used to access resources based on URIs
- a basic set of converters to decode/encode JSON, parameters (string to primitives or common types), media types (text/plain, application/json, application/x-ndjson...)
- a worker thread pool used to execute tasks asynchronously
- a JSON reader/writer

### inverno-configuration

Application configuration API providing great customization and configuration features to multiple parts of an application (eg. system configuration, multitenant configuration, user preferences...).

This module also introduces the `.cprops` configuration file format which facilitates the definition of complex parameterized configuration.

In addition, it also provides implementations for multiple configuration sources:

- a command line configuration source used to load configuration from command line arguments
- a map configuration source used to load configuration stored in map in memory
- a system environment configuration source used to load configuration from environment variables
- a system properties configuration source used to load configuration from system properties
- a `.properties` file configuration source used to load configuration stored in a `.properties` file
- a `.cprops` file configuration source used to load configuration stored in a `.cprops` file
- a Redis configuration source used to load/store configuration from/to a Redis data store with supports for configuration versioning
- a composite configuration source used to combine multiple sources with support for smart defaulting
- an application configuration source used to load the system configuration of an application from a set of common configuration sources in a specific order, for instance: command line, system properties, system environment, local `configuration.cprops` file and `configuration.cprops` file resource in the application module

Configurations are defined as simple interfaces in a module which are processed by the Inverno compiler to generate configuration loaders and beans to make them available in an application with no further effort.

### inverno-grpc-base

The Inverno gRPC base module provides the foundational API as well as common services for developing [gRPC][grpc] clients and servers, such as message compressors (e.g. `gzip`, `deflate`, `snappy`...).

### inverno-grpc-client

The Inverno gRPC client module provides a service to convert HTTP client exchange into gRPC client exchanges supporting the [gRPC protocol over HTTP/2][grpc-over-http2].

It supports the following features:

- unary, client streaming, server streaming and bidirectional streaming service methods as defined in [gRPC core concepts][]
- metadata encoding and decoding
- RPC cancellation
- message compression (`gzip`, `deflate`, `snappy`)

[Inverno tools][inverno-tools-root] provide a gRPC [Protocol buffer][protobuf] compiler plugin for generating client stubs for each service definition.

### inverno-grpc-server

The Inverno gRPC server module provides a service to convert gRPC server exchange handlers supporting the [gRPC protocol over HTTP/2][grpc-over-http2] into HTTP server exchange handlers that can be injected in the HTTP server controller or Web routes to expose gRPC endpoints.

It supports the following features:

- unary, client streaming, server streaming and bidirectional streaming service methods as defined in [gRPC core concepts][]
- metadata encoding and decoding
- RPC cancellation
- message compression (`gzip`, `deflate`, `snappy`)

[Inverno tools][inverno-tools-root] provide a gRPC [Protocol buffer][protobuf] compiler plugin for generating Web routes configurers for each service definition.

### inverno-http-base

The Inverno HTTP base module provides the foundational API as well as common services for HTTP client and server development, in particular an extensible HTTP header service used to decode and encode HTTP headers.

### inverno-http-client

The Inverno HTTP client module provides a fully reactive HTTP/1.x and HTTP/2 client implementation based on Netty. 

It supports the following features:

- SSL
- HTTP compression/decompression
- HTTP/2 over cleartext upgrade
- URL encoded form data
- Multipart form data
- WebSocket

### inverno-http-server

The Inverno HTTP server module provides a fully reactive HTTP/1.x and HTTP/2 server implementation based on Netty. 

It supports the following features:

- SSL
- HTTP compression/decompression
- Server-sent events
- HTTP/2 over cleartext upgrade
- URL encoded form data
- Multipart form data
- WebSocket

### inverno-irt

The Inverno Reactive Template module provides a reactive template engine including:

- reactive, near zero-copy rendering
- statically types template generated by the Inverno compiler at compile time
- pipes for data transformation
- functional syntax inspired from XSLT and Erlang on top of the Java language that perfectly embraces reactive principles

### inverno-ldap

The Inverno LDAP module specifies a reactive API for querying [LDAP][ldap] servers. It also includes a basic LDAP client implementation based on the JDK. It supports bind and search operations.

### inverno-redis

The Inverno Redis client module specifies a reactive API for executing Redis commands on a [Redis][redis] data store. It supports:

- batch queries
- transaction

### inverno-redis-lettuce

The Inverno Redis client Lettuce implementation module provides Redis implementation on top of [Lettuce][lettuce] async pool.

It also exposes a Redis Client bean backed by a Lettuce client and created using the module's configuration. It can be used as is to send commands to a Redis data store.

### inverno-security

The Inverno Security module specifies an API for authenticating request to an application and controlling the access to protected services or resources. It provides:

- User/password authentication against a user repository (in-memory, Redis...).
- Token based authentication.
- Strong user identification against a user repository (in-memory, Redis...).
- Secured password encoding using message digest, Argon2, Password-Based Key Derivation Function (PBKDF2), BCrypt, SCrypt... 
- Role-based access control.
- Permission-based access control.

### inverno-security-http

The Inverno Security HTTP module is an extension to the Inverno Security module that provides a specific API and base implementations for securing applications accessed via HTTP. It provides supports for:

- HTTP basic authentication scheme.
- HTTP digest authentication scheme.
- Form based authentication.
- Cross-origin resource sharing support CORS.
- Protection against Cross-site request forgery attack CSRF.

### inverno-security-ldap

The Inverno Security LDAP module is an extension to the Inverno Security module that provides support for authentication and identification against LDAP and Active Directory servers.

### inverno-security-jose

The Inverno Security JOSE module is a complete implementation of JSON Object Signing and Encryption RFCs. It provides:

- a JWK service used to manipulate JSON Web Key as specified by [RFC 7517][rfc7517] and [RFC 7518][rfc7518].
- a JWS service used to create and validate JWS tokens as specified by [RFC 7515][rfc7515].
- a JWE service used to create and decrypt JWE tokens as specified by [RFC 7516][rfc7516].
- a JWT service used to create, validate or decrypt JSON Web Tokens as JWS or JWE as specified by [RFC 7519][rfc7519].
- JWS and JWE compact and JSON representations support.
- JSON Web Key Thumbprint support as specified by [RFC 7638][rfc7638].
- support for JWS Unencoded Payload Option as specified by [RFC 7797][rfc7797].
- CFRG Elliptic Curve Diffie-Hellman (ECDH) and Signatures support as specified by [RFC 8037][rfc8037].
- CBOR Object Signing and Encryption (COSE) as specified by [RFC 8812][rfc8812].

### inverno-sql

The Inverno SQL client module specifies a reactive API for executing SQL statements on a RDBMS. It supports:

- prepared statement
- batch execution
- transaction

### inverno-sql-vertx

The Inverno SQL client Vert.x implementation module provides SQL Client implementations on top of [Vert.x][vertx-sql-client] pool and pooled client.

It also exposes a pool based Sql Client bean created using the module's configuration that can be used as is to query a RDBMS.

### inverno-web-server

The Inverno Web server module provides advanced features on top of the HTTP server module, including:

- request routing based on path, path pattern, HTTP method, request and response content negotiation including request and response content type and language of the response.
- path parameters
- interceptors
- transparent payload conversion based on the content type of the request or the response from raw representation (arrays of bytes) to Java objects 
- transparent parameter (path, cookie, header, query...) conversion from string to Java objects
- static resource handler to serve static resources from various location based on the resource API
- a complete set of annotations for easy REST controller development

REST controllers can be easily defined using annotations which are processed by the Inverno compiler to generate the Web server configuration. The compiler also checks that everything is in order as for example that there are no conflicting routes.

## Building Inverno framework modules

The Inverno framework modules can be built using Maven and JDK 15+ with the following command:

```plaintext
$ mvn install
```

## License

The Inverno Framework is released under version 2.0 of the [Apache License][apache-license].

