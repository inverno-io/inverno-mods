[inverno-core-root]: https://github.com/inverno-io/inverno-core
[inverno-core-root-doc]: https://github.com/inverno-io/inverno-core/tree/master/doc/reference-guide.md
[inverno-dist-root]: https://github.com/inverno-io/inverno-dist
[inverno-tools-root]: https://github.com/inverno-io/inverno-tools

[inverno-javadoc]: https://inverno.io/docs/release/api/index.html
[cprops-grammar]: https://github.com/inverno-io/inverno-mods/tree/master/inverno-configuration/src/main/javacc/configuration_properties.jj
[template-benchmark]: https://github.com/jkuhn1/template-benchmark

[project-reactor-io]: https://projectreactor.io/
[project-reactor-io-doc]: https://projectreactor.io/docs/core/release/reference/
[media-type]: https://en.wikipedia.org/wiki/Media_type
[jdk-files-probeContentType]: https://docs.oracle.com/javase/9/docs/api/java/nio/file/Files.html#probeContentType-java.nio.file.Path-
[jdk-executors-newCachedThreadPool]: https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Executors.html#newCachedThreadPool--
[jdk-providers]: https://docs.oracle.com/en/java/javase/11/security/oracle-providers.html
[ndjson]: http://ndjson.org/
[javacc]: https://javacc.github.io/javacc/
[redis]: https://redis.io/
[netty]: https://netty.io/
[form-urlencoded]: https://url.spec.whatwg.org/#application/x-www-form-urlencoded
[epoll]: https://en.wikipedia.org/wiki/Epoll
[kqueue]: https://en.wikipedia.org/wiki/Kqueue
[webjars]: https://www.webjars.org/
[open-api]: https://www.openapis.org/
[swagger-ui]: https://swagger.io/tools/swagger-ui/
[yaml]: https://en.wikipedia.org/wiki/YAML
[zero-copy]: https://en.wikipedia.org/wiki/Zero-copy
[server-sent-events]: https://www.w3.org/TR/eventsource/
[chunked-transfer-encoding]: https://en.wikipedia.org/wiki/Chunked_transfer_encoding
[xslt]: https://en.wikipedia.org/wiki/XSLT
[erlang]: https://en.wikipedia.org/wiki/Erlang_(programming_language)
[vertx-sql-client]: https://github.com/eclipse-vertx/vertx-sql-client
[vertx-database-doc]: https://vertx.io/docs/#databases

[rfc-3986]: https://tools.ietf.org/html/rfc3986
[rfc-7231-5.1.1.5]: https://tools.ietf.org/html/rfc7231#section-5.1.1.5
[rfc-7231-5.3]: https://tools.ietf.org/html/rfc7231#section-5.3
[rfc-7231-5.3.1]: https://tools.ietf.org/html/rfc7231#section-5.3.1
[rfc-7231-5.3.2]: https://tools.ietf.org/html/rfc7231#section-5.3.2
[rfc-7231-5.3.5]: https://tools.ietf.org/html/rfc7231#section-5.3.5
[rfc-7231-7.1.2]: https://tools.ietf.org/html/rfc7231#section-7.1.2
[rfc-6265-4.1]: https://tools.ietf.org/html/rfc6265#section-4.1
[rfc-6265-4.2]: https://tools.ietf.org/html/rfc6265#section-4.2
[rfc-6266]: https://tools.ietf.org/html/rfc6266
[rfc-7540-8.1.2.4]: https://tools.ietf.org/html/rfc7540#section-8.1.2.4
[rfc-7578]: https://tools.ietf.org/html/rfc7578

# Inverno Modules

## Motivation

Built on top of the [Inverno core IoC/DI framework][inverno-core-root], Inverno modules suite aimed to provide a complete set of features to develop high end production-grade applications.

The advent of cloud computing and highly distributed architecture based on microservices has changed the way applications should be conceived, maintained, executed and operated. While it was perfectly fine to have application started in a couple of seconds or even minutes some years ago with long release cycles, today's application must be highly efficient, agile in terms of development and deployment and start in a heart beat.

The Inverno framework was created to reduce framework overhead at runtime to the minimum, allowing to create applications that start in milliseconds. Inverno modules extend this approach to provide functionalities with low footprint, relying on the compiler when it makes sense to generate human-readable code for easy maintenance and improved performance.

An agile application is naturally modular which is the essence of the Inverno framework, but it must also be highly configurable and customizable in many ways using configuration data distributed in various data stores and that greatly depend on the context such as an execution environment: test, production..., a location: US, Europe, Asia..., a particular customer, a particular user... Advanced configuration capabilities are then essential to build modern applications.

Traditional application servers and frameworks used to be based on inefficient threading models that didn't make fair use of hardware resources which make them bad cloud citizens. Inverno applications are one hundred percent reactive making maximum use of the allocated resources.

The primary goals can be summarized as follows:

- provide a complete set of common features to build any kind of applications
- maintain a high level of performance...
- ...but always choose modularity and maintainability over performance to favor agility
- be explicit and consistent, there's nothing worse than ambiguity and disparateness, the *you have to know*s must be minimal and logical.
- provide advanced configuration and customization features

## Prerequisites

Before we can dig into the various modules provided in the framework, it is important to understand how to set up a modular Inverno project, so please have a look at the [Inverno distribution documentation][inverno-dist-root] which describes in details how to create, build, run, package and distribute a modular Inverno component or application.

Inverno modules are built on top of the Inverno core IoC/DI framework, please refer to the [Inverno core documentation][inverno-core-root-doc] to understand how IoC/DI is working in the framework.

The framework is fully reactive thanks to [Project Reactor Core library][project-reactor-io], it is strongly recommended to also look at [the reference documentation][project-reactor-io-doc].

## Overview 

The basic Inverno application is an Inverno module composing the *boot* module which provides common services. Other Inverno modules can then be added by defining the corresponding dependencies in the module descriptor.

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app {
    requires io.inverno.mod.boot;
    // Other modules...
}
```

Declaring a dependency to the *boot* module automatically includes core IoC/DI modules as well as *base* module, *configuration* module and reactive framework dependencies.

A basic application can then be created as follows:

```java
import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App.Builder()).run();
    }
}
```

Inverno modules are fully integrated which means they have been designed to work together in an Inverno component or application but this doesn't mean it's not possible to embed them independently in any kind of application following the agile principle. For instance, the *configuration* module, can be easily used in any application with limited dependency overhead. More generally, an Inverno module can be created and started very easily in pure Java thanks to the Inverno core IoC/DI framework. 

For instance, an application can embed an HTTP server as follows:

```java
Boot boot = new Boot.Builder().build();
boot.start();

Server httpServer = new Server.Builder(boot.netService(), boot.resourceService())
    .setHttpServerConfiguration(HttpServerConfigurationLoader.load(conf -> conf.server_port(8080)))
    .setRootHandler(
        exchange -> exchange
            .response()
            .body()
            .raw()
            .value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello, world!", Charsets.DEFAULT)))
    )
    .build();

httpServer.start();
...
httpServer.stop();
boot.stop();
```

> Note that as for any Inverno module, dependencies are clearly specified and must be provided when creating a module, in the previous example the HTTP server requires a `NetService` and a `ResourceService` which are normally provided by the boot module but custom implementations can be provided. It is also possible to create an Inverno module composing the *boot* and *http-server* modules to let the framework deal with dependency injection.
