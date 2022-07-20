[inverno-javadoc]: https://inverno.io/docs/release/api/index.html
[jdk-files-probeContentType]: https://docs.oracle.com/javase/9/docs/api/java/nio/file/Files.html#probeContentType-java.nio.file.Path-
[jdk-executors-newCachedThreadPool]: https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Executors.html#newCachedThreadPool--
[netty]: https://netty.io/
[vertx]: https://vertx.io/
[ndjson]: http://ndjson.org/
[jsr310]: https://jcp.org/en/jsr/detail?id=310
[iso8601]: https://en.wikipedia.org/wiki/ISO_8601

# Boot

The Inverno *boot* module provides basic services to applications including several base implementation for interfaces defined in the *base* module.

The Inverno *boot* module is the basic building block for any application and as such it must be the first module to declare in an application module descriptor.

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app {
    requires io.inverno.mod.boot;
    // Other modules...
}
```

The *boot* module declares transitive dependencies to the core IoC/DI modules as well as *base* and *configuration* modules. They don't need to be re-declared.

This dependency must also be declared in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-boot</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
...
```

## Configuration

The `BootConfiguration` is used to configure the beans exposed in the *boot* module, the `Reactor` and the `NetService` in particular.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

## Reactor

The module provides two `Reactor` implementations: one generic implementation which creates a regular Netty event loop group and a [Vert.x][vertx] core implementation which uses the event loops of a `Vertx` instance. The Vert.x implementation is particularly suited when an Inverno application must integrate Vert.x services such as the PostgreSQL client.

The module exposes one or the other as bean depending on the *boot* module configuration, parameter `reactor_prefer_vertx` must be set to true, and whether or not the Vert.x core module is present on the module path.

## Net service

The module provides a base `NetService` implementation exposed as a bean for building network applications based on [Netty][netty].

## Media type service

The module provides a base `MediaTypeService` implementation based on the JDK (see [Files.probeContentType(Path)][jdk-files-probeContentType]) and exposed as an overridable bean allowing custom implementations to be provided.

## Resource service

The module provides a base `ResourceService` implementation exposed as a bean for accessing resources.

This base implementation supports the following schemes: `file`, `zip`, `jar`, `classpath`, `module`, `http`, `https` and `ftp` and it allows to list resources for `file`, `zip` and `jar` schemes.

When supported, resources are listed from a base URI specifying a path pattern using the following rules:

- `?` matches one character
- `*` matches zero or more characters
- `**` matches zero or more directories in a path

For instance:

```java
ResourceService resourceService = ...

// Return: '/base/test1/a', '/base/test1/a/b', '/base/test2/c'...
Stream<Resource> resources = resourceService.getResources(URI.create("file:/base/test?/**/*"));
```

It is also possible to resolve all resources with a specific name defined in all application modules by specifying '`*`' instead of the module name in a module URI:

```java
ResourceService resourceService = ...

// all resources named '/path/to/resource' in all application modules
Stream<Resource> resources = resourceService.getResources(URI.create("module://*/path/to/resource"));
```

This service can be extended by injecting custom `ResourceProvider` providing resources for a custom URI scheme. For instance, if we create a custom `Resource` and corresponding `ResourceProvider` implementations mapped to URI scheme `custom`, we can extend the resource service so it can create such custom resources.

```java
Boot boot = new Base.Boot()
    .setResourceProviders(List.of(new CustomResourceProvider())
    .build();

boot.start();

Resource customResource = boot.resourceService().get(URI.create("custom:..."));
...

boot.stop();
```

## Converters

The module exposes various `Converter` implementations used across an application to convert parameter values or message payloads.

This includes the following also exposed as beans:

- a parameter converter for converting strings from/to objects, this converter can be extended by injecting specific compound decoders and encoders in the module as described in the [composite converter documentation](#composite-converter).
- a JSON `ByteBuf` converter for converting raw JSON data in `ByteBuf` from/to objects in the application.
- an `application/json` media type converter for converting message payloads from/to JSON.
- an `application/x-ndjson` media type converter for converting message payloads from/to [Newline Delimited JSON][ndjson]
- a `text/plain` media type converter for converting message payloads from/to plain text.

## Worker pool

An Inverno application must be fully reactive, most of the processing is performed in non-blocking I/O threads but sometimes blocking operations might be needed, in such cases, the worker thread pool should be used to execute these blocking operations without impacting the I/O event loop.

The default worker pool bean is a simple [cached Thread pool][jdk-executors-newCachedThreadPool] which can be overridden by providing a different instance to the *boot* module.

## Object mapper

A standard JSON reader/writer based on Jackson `ObjectMapper` is also provided. This instance is used across the application to perform JSON conversion operations, a global configuration can then be applied to that particular instance or it can be overridden when creating the *boot* module.

The global object mapper is configured to use [JSR310][jsr310] for dates which are serialized as timestamps following [ISO 8601][iso8601] representation.
