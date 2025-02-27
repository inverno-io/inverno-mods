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

The objective is to provide a complete consistent set of high-end tools and components for the development of fast and maintainable applications.

## Using a module

Modules can be used in an Inverno module by defining dependencies in the module descriptor. For instance, you can create a Web application module using the *boot* and *web-server* modules:

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

## List of modules

The framework currently provides the following modules.

### io.inverno.mod.base

The Inverno base module defines foundational APIs of the Inverno framework modules:

- Conversion API used to convert objects from/to other objects
- Concurrent API defining the reactive threading model API
- Net API providing URI manipulation as well as low level network client and server utilities
- Reflect API for manipulating parameterized type at runtime
- Resource API to read/write any kind of resources (e.g. file, zip, jar, classpath, module...)

### io.inverno.mod.boot

The Inverno boot module provides base services to an application:

- the reactor which defines the reactive threading model of an application
- a net service used for the implementation of optimized network clients and servers
- a media type service used to determine resource media types
- a resource service used to access resources based on URIs
- a basic set of converters to decode/encode JSON, parameters (string to primitives or common types), media types (text/plain, application/json, application/x-ndjson...)
- a worker thread pool used to execute tasks asynchronously
- a JSON reader/writer

### io.inverno.mod.configuration

The Inverno configuration module defines an application configuration API providing great customization and configuration features to multiple parts of an application (e.g. system configuration, multitenant configuration, user preferences...).

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

### io.inverno.mod.discovery

The Inverno discovery module defines the foundational API for service discovery. At its center, the discovery service is used to resolve specific network services from URI identifiers. Requests are submitted to these services whose role is to assign them to the right service instances based on routing rules and/or load balancing strategies.

The module provides supporting classes to ease the development of discovery service implementation, it especially provides:

- base DNS discovery service implementation for resolving services from a hostname using DNS lookup
- base configuration discovery service implementation for resolving services from a specific service descriptor stored in a configuration source
- caching discovery service wrapper for automatically caching and refreshing resolved services
- composite discovery service which combines multiple discovery services into one
- base traffic load balancer implementations for random and round-robin strategies with support for weighted or non-weighted service instances 

### io.inverno.mod.discovery.http

The Inverno discovery HTTP module specializes the Discovery API for HTTP service resolution by defining a specific HTTP traffic policy and shipping extra *least requests* and *minimum load factor* load balancing strategies. It also provides a DNS HTTP discovery service bean for resolving HTTP services through DNS lookup. 

The module can be used jointly with the [Web client module](#ioinvernomodwebclient) in order to resolve standard HTTP URIs (i.e. `http://`, `https://`, `ws://` or `wss://`).

### io.inverno.mod.discovery.http.k8s

The Inverno discovery HTTP Kubernetes module provides discovery service beans resolving HTTP services deployed in a Kubernetes cluster. 

It currently exposes an environment based discovery service implementation which resolves service inet socket address (i.e. host and port) from the environment variables defined by Kubernetes for each service in the cluster pods (i.e. `<SERVICE_NAME>_SERVICE_HOST` and `<SERVICE_NAME>_SERVICE_PORT_HTTP`).

The module can be used jointly with the [Web client module](#ioinvernomodwebclient) in order to resolve HTTP services from `k8s-env://<SERVICE_NAME>` URIs in a Kubernetes cluster.

### io.inverno.mod.discovery.http.meta

The Inverno discovery HTTP meta module specifies the HTTP meta service which, as its name suggest, allows to combine several other HTTP services into one by routing requests whose content (path, method, content type, headers, query parameters...) matches specific rules to the corresponding service. It also supports request rewriting, including path, request/response headers and query parameters, as well as cross-service load balancing (i.e. load balance requests among multiple services).

The module exposes a configuration based HTTP meta discovery service which resolves HTTP meta service descriptors from a configuration source. 

The module can be used jointly with the [Web client module](#ioinvernomodwebclient) in order to resolve HTTP services from `conf://<SERVICE_NAME>` URIs.

### io.inverno.mod.grpc.base

The Inverno gRPC base module provides the foundational API as well as common services for developing [gRPC][grpc] clients and servers, such as message compressors (e.g. `gzip`, `deflate`, `snappy`...).

### io.inverno.mod.grpc.client

The Inverno gRPC client module provides a service to convert HTTP client exchange into gRPC client exchanges supporting the [gRPC protocol over HTTP/2][grpc-over-http2].

It supports the following features:

- unary, client streaming, server streaming and bidirectional streaming service methods as defined in [gRPC core concepts][]
- metadata encoding and decoding
- RPC cancellation
- message compression (`gzip`, `deflate`, `snappy`)

[Inverno tools][inverno-tools-root] provide a gRPC [Protocol buffer][protobuf] compiler plugin for generating client stubs for each service definition.

### io.inverno.mod.grpc.server

The Inverno gRPC server module provides a service to convert gRPC server exchange handlers supporting the [gRPC protocol over HTTP/2][grpc-over-http2] into HTTP server exchange handlers that can be injected in the HTTP server controller or Web routes to expose gRPC endpoints.

It supports the following features:

- unary, client streaming, server streaming and bidirectional streaming service methods as defined in [gRPC core concepts][]
- metadata encoding and decoding
- RPC cancellation
- message compression (`gzip`, `deflate`, `snappy`)

[Inverno tools][inverno-tools-root] provide a gRPC [Protocol buffer][protobuf] compiler plugin for generating Web routes configurers for each service definition.

### io.inverno.mod.http.base

The Inverno HTTP base module provides the foundational API as well as common services for HTTP client and server development, such as an extensible HTTP header service used to decode and encode HTTP headers.

It also provides a generic router API used to create routers to best match an input to a resource based on some set of criteria. This API is especially used in the Web server module for routing Web exchange to Web exchange handlers or in the Web client module for resolving interceptors.

### io.inverno.mod.http.client

The Inverno HTTP client module provides a fully reactive HTTP/1.x and HTTP/2 client implementation based on Netty. 

It supports the following features:

- SSL
- HTTP compression/decompression
- HTTP/2 over cleartext upgrade
- URL encoded form data
- Multipart form data
- WebSocket

### io.inverno.mod.http.server

The Inverno HTTP server module provides a fully reactive HTTP/1.x and HTTP/2 server implementation based on Netty. 

It supports the following features:

- SSL
- HTTP compression/decompression
- Server-sent events
- HTTP/2 over cleartext upgrade
- URL encoded form data
- Multipart form data
- WebSocket

### io.inverno.mod.irt

The Inverno Reactive Template module provides a reactive template engine including:

- reactive, near zero-copy rendering
- statically types template generated by the Inverno compiler at compile time
- pipes for data transformation
- functional syntax inspired from XSLT and Erlang on top of the Java language that perfectly embraces reactive principles

### io.inverno.mod.ldap

The Inverno LDAP module specifies a reactive API for querying [LDAP][ldap] servers. It also includes a basic LDAP client implementation based on the JDK. It supports bind and search operations.

### io.inverno.mod.redis

The Inverno Redis client module specifies a reactive API for executing Redis commands on a [Redis][redis] data store. It supports:

- batch queries
- transaction

### io.inverno.mod.redis.lettuce

The Inverno Redis client Lettuce implementation module provides Redis implementation on top of [Lettuce][lettuce] async pool.

It also provides a Redis Client bean backed by a Lettuce client and created using the module's configuration. It can be used as is to send commands to a Redis data store.

### io.inverno.mod.security

The Inverno Security module specifies an API for authenticating request to an application and controlling the access to protected services or resources. It provides:

- User/password authentication against a user repository (in-memory, Redis...).
- Token based authentication.
- Session-based authentication.
- Strong user identification against a user repository (in-memory, Redis...).
- Secured password encoding using message digest, Argon2, Password-Based Key Derivation Function (PBKDF2), BCrypt, SCrypt... 
- Role-based access control.
- Permission-based access control.

### io.inverno.mod.security.http

The Inverno Security HTTP module is an extension to the Inverno Security module that provides a specific API and base implementations for securing applications accessed via HTTP. It provides supports for:

- HTTP basic authentication scheme.
- HTTP digest authentication scheme.
- Form based authentication.
- Cross-origin resource sharing support CORS.
- Protection against Cross-site request forgery attack CSRF.

### io.inverno.mod.security.ldap

The Inverno Security LDAP module is an extension to the Inverno Security module that provides support for authentication and identification against LDAP and Active Directory servers.

### io.inverno.mod.security.jose

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

### io.inverno.mod.session

The Inverno Session module specifies an API for managing session that persist across more than one request to an application. It provides:

- Basic session support with data stored on the application side.
- JWT session support allowing a hybrid approach where stateless data can be stored in a JWT used as session identifier on the client side along with basic session data on the application side. 
- In-memory and Redis session store implementations.

### io.inverno.mod.session.http

The Inverno Session HTTP module is an extension to the Inverno Session module that provides specific API and components to support session in a Web application.

### io.inverno.mod.sql

The Inverno SQL client module specifies a reactive API for executing SQL statements on a RDBMS. It supports:

- prepared statement
- batch execution
- transaction

### io.inverno.mod.sql.vertx

The Inverno SQL client Vert.x implementation module provides SQL Client implementations on top of [Vert.x][vertx-sql-client] pool and pooled client.

It also exposes a pool based Sql Client bean created using the module's configuration that can be used as is to query a RDBMS.

### io.inverno.mod.web.base

The Inverno Web base module provides the foundational API as well as common services for Web client and server development, such as a data conversion service used to create inbound data decoder and outbound data encoder for respectively decoding and encoding HTTP or WebSocket payloads based on their media types. 

It also defines common request parameter binding annotations for creating declarative Web client or Web server routes.

### io.inverno.mod.web.client

The Inverno Web client module provides advanced features on top of the HTTP client module, including:

- path parameters
- exchange interceptors based on path, path pattern, HTTP method, request and response content negotiation including request and response content type and language of the response.
- seamless payload conversion from raw representation (arrays of bytes) to Java objects based on request or response content type as well as WebSocket subprotocol
- seamless parameter (path, cookie, header, query...) conversion from string to Java objects
- a complete set of annotations for creating declarative Web clients

Web clients can be created in a declarative way using annotations which are processed by the Inverno compiler to generate the Web client implementation and expose a corresponding bean in the module. 

### io.inverno.mod.web.server

The Inverno Web server module provides advanced features on top of the HTTP server module, including:

- path parameters
- request routing based on path, path pattern, HTTP method, request and response content negotiation including request and response content type and language of the response.
- exchange interceptors based on path, path pattern, HTTP method, request and response content negotiation including request and response content type and language of the response.
- transparent payload conversion from raw representation (arrays of bytes) to Java objects based on request or response content type as well as WebSocket subprotocol 
- transparent parameter (path, cookie, header, query...) conversion from string to Java objects
- static resource handler to serve static resources from various location based on the resource API
- a complete set of annotations for creating declarative REST controllers

REST controllers can be created in a declarative way using annotations which are processed by the Inverno compiler to generate corresponding Web server routes and register them in the Web server. The compiler also performs some static checks to make sure routes are defined properly and that there are no conflicting routes.

## Building Inverno framework modules

The Inverno framework modules can be built using Maven and JDK 15+ with the following command:

```plaintext
$ mvn install
```

## License

The Inverno Framework is released under version 2.0 of the [Apache License][apache-license].

