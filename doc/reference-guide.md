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

The advent of cloud computing and highly distributed architecture based on microservices has changed the way applications should be conceived, maintained, executed and operated. While it was perfectly fine to have application started in couple of seconds or even minutes some years ago with long release cycles, today's application must be highly efficient, agile in terms of development and deployment and start in a heart beat.

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

Before we can dig into the various modules provided in the framework, it is important to understand how to setup a modular Inverno project, so please have a look at the [Inverno distribution documentation][inverno-dist-root] which describes in details how to create, build, run, package and distribute a modular Inverno component or application.

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

For instance, an application can embed a HTTP server as follows:

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

## Base

The Inverno *base* module defines the foundational APIs used across all modules, it can be seen as an extension to the *java.base* module. 

In order to use the Inverno *base* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    requires io.inverno.mod.base;
    ...
}
```

The *base* module declares transitive dependencies to reactive APIs which don't need to be re-declared.

We also need to declare that dependency in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-base</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
...
compile 'io.inverno.mod:inverno-base:1.2.0'
...
```

The *base* module is usually provided as a transitive dependency by other modules, mainly the *boot* module, so defining a direct dependency is usually not necessary at least for an application module.

### Converter API

The converter API provides interfaces and classes for building converters, decoders or encoders which are basically used to decode/encode objects of a given type from/to objects of another type.

#### Scope

The `Scope` interface specifies a way to expose different bean instances depending on particular scope.

For instance, let's say we want to use different instances of a `Warehouse` bean based on a particular region, we can define a prototype bean for the `Warehouse` and create the following bean which extends `KeyScope`:

```java
@Bean
public class WarehouseKeyScope extends KeyScope<Warehouse> {

    private final Supplier<Warehouse> storePrototype;
    
    public WarehouseKeyScope(@Lazy Supplier<Warehouse> storePrototype) {
        this.storePrototype = storePrototype;
    }
    
    @Override
    protected Warehouse create() {
        return this.storePrototype.get();
    }
}
```

We can then inject that bean where we need a `Warehouse` instance for a particular region:

```java
@Bean
public class WarehouseService {

    private final KeyScope<Warehouse> warehouse;

    public WarehouseService(KeyScope<Warehouse> warehouse) {
        this.warehouse = warehouse;
    }
    
    public void store(Product product, String region) {
        Warehouse warehouse = this.warehouse.get(region);
        ...
    }
}
```

The base module expose three base `Scope` implementations:

- the `KeyScope` which binds an instance to an arbitrary key
- the `ThreadScope` which binds an instance to the current thread
- the `ReactorScope` which binds an instance to the current reactor's thread. This is very similar to the `ThreadScope` but this throws an `IllegalStateException` when used outside the scope of the reactor (ie. the current thread is not a reactor thread).

> Particular care must be taken when using this technique in order to avoid resource leaks. For instance, when a scoped instance is no longer in use, it should be cleaned explicitly as references can be strongly reachable. The `KeyScope` exposes the `remove()` for this purpose. Also when using prototype bean instance, the destroy method, if any, may not be invoked if the instance is reclaimed before it can be destroyed, as a result you should avoid using such bean instances within scope beans.

#### Basic converter

The `Converter` interface defines a basic converter. It simply extends `Decoder` and `Encoder` interfaces which defines respectively the basic decoder and the basic encoder.

A basic decoder is used to decode an object of a source type to an object of a target type. For instance, we can create a simple string to integer decoder as follows:

```java
public class StringToIntegerDecoder {
    
    @Override
    public <T extends Integer> T decode(String value, Class<T> type) throws ConverterException {
        return (T)Integer.valueOf(value);
    }
    
    @Override
    public <T extends Integer> T decode(String value, Type type) throws ConverterException {
        return (T)Integer.valueOf(value);
    }
}
Decoder<String, Integer> 
```

A basic encoder is used to encode an object of a source type to an object of a target type. For instance, we can create a simple integer to string encoder as follows:

```java
public class IntegerToStringEncoder implements Encoder<Integer, String> {
    
    @Override
    public <T extends Integer> String encode(T value) throws ConverterException {
        return value.toString();
    }
    
    @Override
    public <T extends Integer> String encode(T value, Class<T> type) throws ConverterException {
        return value.toString();
    }
    
    @Override
    public <T extends Integer> String encode(T value, Type type) throws ConverterException {
        return value.toString();
    }
}
```

A string to integer converter can then be created by combining both implementations.

The previous example while not very representative illustrates the basic decoder and encoder API, you should now wonder how to use this properly in an application and what is the fundamental difference between a decoder and an encoder, the answer actually lies in the names. A decoder is meant to *decode* data formatted in a particular way into a representation that can be used in an application whereas an encoder is meant to *encode* an object in an application into data formatted in a particular way. From there, we understand that a converter can be used to read or write raw data (JSON data in an array of bytes for instance) to or from actual usable representations in the form of Java objects but it can also be used as an object mapper to convert from one representation to another (domain object to data transfer object for instance).

A more realistic example would then be a JSON string to object converter:

```java
public class JsonToObjectConverter implements Converter<String, Object> {
    
    private ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public <T> T decode(String value, Class<T> type) throws ConverterException {
        try {
            return this.mapper.readValue(value, type);
        } 
        catch (JsonProcessingException e) {
            throw new ConverterException(e);
        }
    }
    
    @Override
    public <T> T decode(String value, Type type) throws ConverterException {
        ...
    }
    
    @Override
    public <T> String encode(T value) throws ConverterException {
        try {
            return this.mapper.writeValueAsString(value);
        } 
        catch (JsonProcessingException e) {
            throw new ConverterException(e);
        }
    }
    
    @Override
    public <T> String encode(T value, Class<T> type) throws ConverterException {
        ...
    }
    
    @Override
    public <T> String encode(T value, Type type) throws ConverterException {
        ...
    }
}
```

The API provides other interfaces to create converters, decoders and encoders with more capabilities.

#### Splittable decoder and Joinable encoder

A `SplittableDecoder` is a particular decoder which allows to decode an object of a source type into multiple objects of a target type. It specifies methods to decode one source instance into an array, a list or a set of target instances.

In the same way, a `JoinableEncoder` is a particular encoder which allows to encode multiple objects of a source type into one single object of a target type. It specifies methods to encode an array, a list or a set of source instances into a single target instance.

The `StringConverter` is a typical implementation that can decode or encode multiple parameters values.

```java
StringConverter converter = new StringConverter();

// List.of(1, 2, 3)
List<Integer> l = converter.decodeToList("1,2,3", Integer.class); 
// "1,2,3"
String s = converter.encodeList(List.of(1, 2, 3));
```

#### Primitive decoder and encoder

A `PrimitiveDecoder` is fundamentally an object decoder which provides bindings to decode an object of a source type into an object of primitive (boolean, integer...) or common type (string, date, URI...).

In the same way, a `PrimitiveEncoder` is fundamentally an object encoder which provides bindings to encode an object of a primitive or common type to an object of a target type.

The `StringConverter` which is meant to convert parameter values is again a typical use case of primitive decoder and encoder.

```java
StringConverter converter = new StringConverter();

// 123l
long l = converter.decodeLong("123");
// ISO-8601 date: "yyyy-MM-dd"
String s = converter.encode(LocalDate.now());
```

The `SplittablePrimitiveDecoder` and `JoinablePrimitiveEncoder` are primitive decoder and encoder that respectively extends `SplittableDecoder` and `JoinableEncoder`.

#### Object converter

An `ObjectConverter` is a convenient interface for building `Object` converters. It extends `Converter`, `SplittablePrimitiveDecoder` and `JoinablePrimitiveEncoder`.

#### Reactive converter

A `ReactiveConverter` is a particular converter which extends `ReactiveDecoder` and `ReactiveEncoder` for building reactive converters which are particularly useful to convert data from non-blocking I/O channels.

The `ReactiveDecoder` interface defines methods to decode one or many objects of a target type from a stream of objects of a source type. In the same way, the `ReactiveEncoder` interface defines methods to encode one or many objects of a source type into a stream of objects of target type.

The `ByteBufConverter` is a typical use case, it is meant to convert data from non-blocking channels like the request or response payloads in a network server or client, or the content of a resource read asynchronously.

```java
ByteBufConverter converter = new ByteBufConverter(new StringConverter());

Publisher<ByteBuf> dataStream = ... // comes from a request or resource

// On subscription, chunk of data accumulates until a complete response can be emitted
Mono<ZonedDateTime> dateTimeMono = converter.decodeOne(dataStream, ZonedDateTime.class); 

// On subscription, a stream of integer is mapped to a publisher of ByteBuf
Publisher<ByteBuf> integerStream = converter.encodeMany(Flux.just(1,2,3,4));
```

#### Media type converter

A `MediaTypeConverter` is a particular kind of object converter which supports a specific format specified as a [media type][media-type] and converts object from/to raw data in the supported format. A typical example would be a JSON media type converter used to decode/encode raw JSON data.

> The *web* module relies on such converters to respectively decode end encode HTTP request and HTTP response payloads based on the content type specified in the message headers.

#### Composite converter

A `CompositeConverter` is an extensible object converter based on a `CompositeDecoder` and a `CompositeEncoder` which themselves rely on multiple `CompoundDecoder` and `CompoundEncoder` to extend or override respectively the decoding and encoding capabilities of the converter. In practical terms, it is possible to make a converter able to decode or encode any type of object by providing ad hoc compound decoders and encoders.

The `StringCompositeConverter` is a composite converter implementation which uses a default `StringConverter` to convert primitive and common types of objects, it can be extended to convert other types of object.

For instance, let's consider the following `Message` class:

```java
public static class Message {

    private String message;

    // constructor, getter, setter
    ...
}
```

We can create specific compound decoder and encoder to respectively decode and encode a `Message` from/to a string as follows:

```java
public static class MessageDecoder implements CompoundDecoder<String, Message> {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> T decode(String value, Class<T> type) throws ConverterException {
        return (T) new Message(value);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> T decode(String value, Type type) throws ConverterException {
        return (T) new Message(value);
    }
    
    @Override
    public <T extends Message> boolean canDecode(Class<T> type) {
        return Message.class.equals(type);
    }
    
    @Override
    public boolean canDecode(Type type) {
        return Message.class.equals(type);
    }
}
```

```java
public static class MessageEncoder implements CompoundEncoder<Message, String> {
    
    @Override
    public <T extends Message> String encode(T value) throws ConverterException {
        return value.getMessage();
    }
    
    @Override
    public <T extends Message> String encode(T value, Class<T> type) throws ConverterException {
        return value.getMessage();
    }
    
    @Override
    public <T extends Message> String encode(T value, Type type) throws ConverterException {
        return value.getMessage();
    }
    
    @Override
    public <T extends Message> boolean canEncode(Class<T> type) {
        return Message.class.equals(type);
    }
    
    @Override
    public boolean canEncode(Type type) {
        return Message.class.equals(type);
    }
}
```

And inject them into a string composite converter which can then decode/encode `Message` object:

```java
CompoundDecoder<String, Message> messageDecoder = new MessageDecoder();
CompoundEncoder<Message, String> messageEncoder = new MessageEncoder();

StringCompositeConverter converter = new StringCompositeConverter();
converter.setDecoders(List.of(messageDecoder));
converter.setEncoders(List.of(messageEncoder));

Message decodedMessage = converter.decode("this is an encoded message", Message.class);
String encodedMessage = converter.encode(new Message("this is a decoded message"));
```

### Net API

The Net API provides interfaces and classes to manipulate basic network elements such as URIs or to create basic network clients and servers.

#### URIs

A URI follows the standard defined by [RFC 3986][rfc-3986], it is mostly used to identify resources such as file or more specifically a route in a Web server. The JDK provides a standard implementation which is not close to what is required by the *web* module to name just one.

The `URIs` utility class is the main entry point for working on URIs in any ways imaginable. It defines methods to create a blank URI or a URI based on a given path or URI. These methods return a `URIBuilder` instance which is then used to build a URI, a path, a query string or a URI pattern.

A simple URI can then be created as follows:

```java
// http://localhost:8080/path/to/resource?parameter=value
URI uri = URIs.uri()
    .scheme("http")
    .host("localhost")
    .port(8080)
    .path("/path/to/resource")
    .queryParameter("parameter", "value")
    .build();
```

or from an existing URI as follows:

```java
// https://test-server/path/to/resource
URI uri = URIs.uri(URI.create("http://localhost:8080/path/to?parameter=value"))
    .scheme("https")
    .host("test-server")
    .port(null)
    .segment("resource")
    .clearQuery()
    .build();
```

A URI can be normalized by enabling the `URIs.Option.NORMALIZED` option:

```java
// path/to/other
URI uri = URIs.uri("path/to/resource", URIs.Option.NORMALIZED)
    .segment("..")
    .segment("other")
    .build();
```

A parameterized URI can be created by enabling the `URIs.Option#PARAMETERIZED` option and specifying parameters of the form `{[<name>][:<pattern>]}` in the components of the URI. This allows to create URI templates that can be used to generate URIs from a set of parameters.

```java
URIBuilder uriTemplate = URIs.uri(URIs.Option.PARAMETERIZED)
    .scheme("{scheme}")
    .host("{host}")
    .path("/path/to/resource")
    .segment("{id}")
    .queryParameter("format", "{format}");

// http://locahost/path/to/resource/1?format=text
URI uri1 = uriTemplate.build("http", "localhost", "1", "text");

// https://production/path/to/resource/32?format=json
URI uri2 = uriTemplate.build("https", "production", "32", "json");
```

The `URIBuilder` also defines methods to create string representations of the whole URI, the path component or the query component.

```java
URIBuilder uriBuilder = URIs.uri()
    .scheme("http")
    .host("localhost")
    .port(8080)
    .path("/path/to/resource")
    .queryParameter("parameter", ""value);

// http://localhost:8080/path/to/resource?parameter=value
String uri = uriBuilder.buildString();

// path/to/resource
String path = uriBuilder.buildPath();

// parameter=value
String query = uriBuilder.buildQuery();
```

It can also create `URIPattern` to match a given input against the pattern specified by the URI while extracting parameter values when the URI is parameterized.

```java
URIPattern uriPattern = URIs.uri(URIs.Option.PARAMETERIZED)
    .scheme("{scheme}")
    .host("{host}")
    .path("/path/to/resource")
    .segment("{id}")
    .queryParameter("format", "{format}")
    .buildPattern();

URIMatcher matcher = uriPattern.matcher("http://localhost:8080/path/to/resource/1?format=text");
if(matcher.matches()) {
    // scheme=http, host=localhost, id=1, format=text
    Map<String, String> parameters = matcher.getParameters();
    ...
}
```

#### Network service

The `NetService` interface specifies a service for building optimized network clients and servers based on Netty. The *base* module doesn't provide any implementation, a base implementation is provided in the *boot* module.

This service especially defines methods to obtain `EventLoopGroup` instances backed by a root event loop group in order to reuse event loops across different network servers or clients running in the same application.

It also defines methods to create basic network client and server bootstraps.

### Reflection API

The reflection API provides classes and interfaces for building `java.lang.reflect.Type` instances in order to represent parameterized types at runtime which is otherwise not possible due to type erasure. Such `Type` instances are used when decoding data into objects of parameterized types.

The `Types` class is the main entry point for building any kind of Java types.

```java
// java.util.List<? extends java.lang.Comparable<java.lang.String>>
Type type = Types.type(List.class)
    .wildcardType()
        .upperBoundType(Comparable.class)
            .type(String.class).and()
    .and()
    .build();
```

The reflection API is particularly useful to specify a parameterized type to an [object converter](#object-converter). For instance, let's imagine we have a `ByteBuf` we want to decode to a `List<String>`, we can do:

```java
ByteBuf input = ...;
ObjectConverter<ByteBuf> converter = ...;

Type listOfStringType = Types.type(List.class)
    .type(String.class).and()
    .build();
List<String> decode = converter.<List<String>>decode(input, listOfStringType);
```

### Resource API

The resource API provides classes and interfaces for accessing resources of different kinds and locations (file, zip, jar, classpath, module...) in a consistent way using a unique `Resource` interface.

A resource can be created directly using the implementation corresponding to the kind of resource. For instance, in order to access a resource on the class path, you need to choose the `ClasspathResource` implementation:

```java
ClasspathResource resource = new ClasspathResource(URI.create("classpath:/path/to/resource"));
```

A resource is identified by a URI whose scheme specifies the kind of resources. The *base* module provides several implementations with a corresponding scheme.

<table>
<tr>
<th>Type</th>
<th>URI</th>
<th>Implementation</th>
</tr>
<tr>
<td><code>file</code></td>
<td><code>file:/path/to/resource</code></td>
<td><code>FileResource</code></td>
</tr>
<tr>
<td><code>zip</code></td>
<td><code>zip:/path/to/zip!/path/to/resource</code></td>
<td><code>ZipResource</code></td>
</tr>
<tr>
<td><code>jar</code></td>
<td><code>jar:/path/to/jar!/path/to/resource</code></td>
<td><code>JarResource</code></td>
</tr>
<tr>
<td><code>url</code></td>
<td><code>http|https|ftp://host/path/to/resource</code></td>
<td><code>URLResource</code></td>
</tr>
<tr>
<td><code>classpath</code></td>
<td><code>classpath:/path/to/resource</code></td>
<td><code>ClasspathResource</code></td>
</tr>
<tr>
<td><code>module</code></td>
<td><code>module://[MODULE_NAME]/path/to/resource</code></td>
<td><code>ModuleResource</code></td>
</tr>
</table>

The `ResourceService` interface specifies a service which provides a unified access to resources based only on the resource URI. The *base* module doesn't provide any implementation, a base implementation is provided in the *boot* module.

A typical use case is to get a resource from a URI without knowing the actual kind of the resource.

```java
ResourceService resourceService = ...

Resource resource = resourceService.getResource(URI.create("classpath:/path/to/resource"));
```

The resource service can also be used to list resources at a given location. Nonetheless this actually depends on the implementation and the kind of resource, although it is clearly possible to list resources from a file location, it might not be supported to list resources from a class path or URL location.

The *boot* module [implementation](#resource-service) supports for instance the listing of resources that match a specific path pattern:

```java
ResourceService resourceService = ...

Stream<Resource> resources = resourceService.getResources(URI.create("file:/path/to/resources/**/*"));
```

The `MediaTypeService` interface specifies a service used to determine the media type of a resource based on its extension, name, path or URI. As for the resource service, a base implementation is provided in the *boot* module.

```java
MediaTypeService mediaTypeService = ...

// image/png
String mediaType = mediaTypeService.getForExtension("png");
```

## Boot

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

```groovy
...
compile 'io.inverno.mod:inverno-boot:1.2.0'
...
```

### Configuration

The `BootConfiguration` is used to configure the beans exposed in the *boot* module, the `Reactor` and the `NetService` in particular.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

### Reactor

The module provides two `Reactor` implementations: one generic implementation which creates a regular Netty event loop group and a Vert.x core implementation which uses the event loops of a `Vertx` instance. The Vert.x implementation is particularly suited when an Inverno application must integrate Vert.x services such as the PostgreSQL client.

The module exposes one or the other as bean depending on the *boot* module configuration, parameter `reactor_prefer_vertx` must be set to true, and whether or not the Vert.x core module is present on the module path.

### Net service

The module provides a base `NetService` implementation exposed as a bean for building network applications based on Netty.

### Media type service

The module provides a base `MediaTypeService` implementation based on the JDK (see [Files.probeContentType(Path)][jdk-files-probeContentType]) and exposed as an overridable bean allowing custom implementations to be provided.

### Resource service

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

### Converters

The module exposes various `Converter` implementations used across an application to convert parameter values or message payloads.

This includes the following also exposed as beans:

- a parameter converter for converting strings from/to objects, this converter can be extended by injecting specific compound decoders and encoders in the module as described in the [composite converter documentation](#composite-converter).
- a JSON `ByteBuf` converter for converting raw JSON data in `ByteBuf` from/to objects in the application.
- an `application/json` media type converter for converting message payloads from/to JSON.
- an `application/x-ndjson` media type converter for converting message payloads from/to [Newline Delimited JSON][ndjson]
- a `text/plain` media type converter for converting message payloads from/to plain text.

### Worker pool

An Inverno application must be fully reactive, most of the processing is performed in non-blocking I/O threads but sometimes blocking operations might be needed, in such cases, the worker thread pool should be used to execute these blocking operations without impacting the I/O event loop.

The default worker pool bean is a simple [cached Thread pool][jdk-executors-newCachedThreadPool] which can be overridden by providing a different instance to the *boot* module.

### Object mapper

A standard JSON reader/writer based on Jackson `ObjectMapper` is also provided. This instance is used across the application to perform JSON conversion operations, a global configuration can then be applied to that particular instance or it can be overridden when creating the *boot* module.

## Configuration

The Inverno *configuration* module defines a unified configuration API for building agile highly configurable applications.

Configuration is one of the most important aspect of an application and sadly one of the most neglected. There are very few decent configuration frameworks and most of the time they relate to one part of the issue. It is important to approach configuration by considering it as a whole and not as something that can be solved by a property file here and a database there. Besides, it must be the first issue to tackle during the design phase as it will impact all aspects of the application. For instance, we can imagine an application where configuration is defined in simple property files, a complete configuration would probably be needed for each environment where the application is deployed, maintenance would be probably problematic even more when we know that configuration properties can be added, modified or removed over time.

In its most basic form, a configuration is not more than a set of properties associating a value to a key. It would be naive to think that this is enough to build an agile and customizable application, but in the end, a property should still be considered as the basic building block for configurations.

Now, the first thing to notice is that any part of an application can potentially be configurable, from a server IP address to a color of a button in a user interface, there are multiple forms of configuration with different expectations that must coexist in an application. For instance, some parts of the configuration are purely static and do not change during the operation of an application, this is the case of a bootstrap configuration which mostly relates to the operating environment (eg. a server port). Some other parts, on the other hand, are more dynamic and can change during the operation of an application, this is the case of tenant specific configuration or even user preferences. 

Following this, we can see that a configuration greatly depends on the context in which it is loaded. The definition of a configuration, which is basically a list of property names, is dictated by the application, so when the application is running, this definition should be fixed but the context is not. For instance, the bootstrap configuration is different from one operating environment to another, user preferences are not the same from one user to another...

We can summarize this as follows:

- a configuration is a set of configuration properties.
- the configuration of an application is actually composed of multiple configurations with their own specificities.
- the definition of a configuration is bound to the application as a result the only way to change it is to change the application.
- a configuration depends on a particular context which must be considered when setting or getting configuration properties. 

The configuration API has been created to address previous points, giving a maximum flexibility to precisely design how an application should be configured.

In order to use the Inverno *configuration* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.configuration;
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
            <artifactId>inverno-configuration</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
...
compile 'io.inverno.mod:inverno-configuration:1.2.0'
...
```

### Configuration source

A configuration source can be any data store that holds configuration data, the API abstracts configuration data sources to provide a unified access to configuration data through the `ConfigurationSource` interface. Specific implementations should be considered depending on the type of configuration: a bootstrap configuration is most likely to be static and stored in configuration files or environment variables whereas a tenant specific configuration is most likely to be stored in a distributed data store. However this is not a universal rule, depending on the needs we can very well consider any kind of configuration source for any kind of configuration. The configuration source abstracts these concerns from the the rest of the application.

The `ConfigurationSource` is the main entry point for accessing configuration properties, it shall be used every time there's a need to retrieve configuration properties. It defines only one method for creating a `ConfigurationQuery` instance eventually executed in order to retrieve one or more configuration properties.

For instance, property `server.uri` can be retrieved as follows:

```java
ConfigurationSource<?, ?, ?> source = ...

source.get("server.url")                        // 1
    .execute()                                  // 2
    .single()                                   // 3
    .map(queryResult -> queryResult
        .getResult()                            // 4
        .flatMap(property -> property.asURI())  // 5
        .orElse(URI.create("http://localhost")) // 6
    )
    .subscribe(serverURI -> ...);               // 7
```

In the preceding example:

1. creates a configuration query to retrieve the `server.url` property
2. executes the query, the API is reactive so nothing will happen until a subscription is actually made on the resulting `Flux` of `ConfigurationQueryResult`
3. transforms the `Flux` to a `Mono` since we expect a single result
4. gets the resulting configuration property, a query result is always returned even if the property does not exist in the source therefore `getResult()` returns an `Optional` that lets you decide what to do if the property is missing
5. converts the property value to URI if present, a property can be defined in a source with a null value which explains why the property value is also an `Optional` and why we need to use `flatMap()`
6. returns the actual value if it exists or the specified default value
7. we subscribe to the `Mono` which actually runs the query in the source and returns the property value or the default value if the property value is null or not defined in the source

This seems to be a lot of steps to simply retrieve one property value, but if you look closely you'll understand that each of them is actually necessary:

- we want to be able to retrieve multiple properties and/or create more complex queries in a batch so `.execute()` is required to mark the end of a batch of queries
- we want to be reactive so `.single().map()` and `subscribe()` are required
- we want to have access to the configuration query key at the origin of a property for troubleshooting as a result the a query result must expose `getQueryKey()` and `getResult()` methods
- we want to be able to convert a property value and provide different behaviors when a property does not exist in a source or when it does exist but with a null value, as a result `.flatMap(property -> property.asURI()).orElse(URI.create("http://localhost"))` is required

As we said earlier, a configuration depends on the context: a given property might have different values when considering different contexts. The configuration API defines a configuration property with a name, a value and a set of parameters specifying the context for which the property is defined. Such configuration property is referred to as a **parameterized configuration property**.

> Some configuration source implementations do not support parameterized configuration property, they simply ignore parameters specified in queries and return the value associated to the property name. This is especially the case of environment variables which don't allow to specify property parameters.

In order to retrieve a property in a particular context we can then parameterized the configuration query as follows:

```java
source.get("server.url")
    .withParameters("environment", "production", "zone", "us")
    .execute()
    ...
```

In the preceding example, we query the source for property `server.url` defined for the production environment in zone US. To state the obvious, both the list of parameters and their values can be determined at runtime using actual contextual values. This is what makes parameterized properties so powerful as it is suitable for a wide range of use cases. This is all the more true when using a configuration source which supports some kind of defaulting such as the [Composite Configuration source](#composite-configuration-source).

> Whether the exact or nearest value matching the query is returned by a configuration source is implementation dependent but since the *configuration* module provides the [Composite Configuration source](#composite-configuration-source) which can wrap any configuration source to add support for defaulting, it is a good practice to implement configuration sources that only support exact matching of a configuration query key (ie. including name and parameters).

As said before the API let's you fluently query multiple properties in a batch and map the results in a configuration object.

```java
source
    .get("server.port", "db.url", "db.user", "db.password").withParameters("environment", "production", "zone", "us")
    .and()
    .get("db.schema").withParameters("environment", "production", "zone", "us", "tenant", "someCompany")
    .execute()
    .collectMap(queryResult -> queryResult.getQueryKey().getName(), queryResult -> queryResult.getResult())
    .map(properties -> {
        ApplicationConfiguration config = new ApplicationConfiguration();
        
        properties.get("server.port").flatMap(property -> property.asInteger()).ifPresent(config::setServerPort);
        properties.get("db.url").flatMap(property -> property.asURL()).ifPresent(config::setDbURL);
        properties.get("db.user").flatMap(property -> property.asString()).ifPresent(config::setDbUser);
        String dbPassword = properties.get("db.password").flatMap(property -> property.asString()).ifPresent(config::setDbPassword);
        String dbSchema = properties.get("db.schema").flatMap(property -> property.asString()).ifPresent(config::setDbSchema);
        
        return config;
    })
    .subscribe(config -> {
        ...
    });
```

The beauty of being reactive is that it comes with a lot of cool features such as the ability to re-execute a query or caching the result. A `Flux` or a `Mono` executes on subscriptions, which means we can create a complex query to retrieve a whole configuration, keep the resulting Reactive Streams `Publisher` and subscribe to it when needed. A Reactive Stream publisher can also cache configuration results.

```java
Mono<ApplicationConfiguration> configurationLoader = ... // see previous example

// Query the source on each subscriptions
configurationLoader.subscribe(config -> {
    ...
});

// Cache the configuration for five minutes
Mono<ApplicationConfiguration> cachedConfigurationLoader = configurationLoader.cache(Duration.ofMinutes(5));

// Query the source on first subscriptions, further subscriptions within a window of 5 minutes will get the cached configuration
cachedConfigurationLoader.subscribe(config -> {
    ...
});
```

> Although publisher caching is a cool feature, it might not be ideal for complex caching use cases and more solid solution should be considered.

A configuration source relies on a `SplittablePrimitiveDecoder` to decode property values. Configuration source implementations usually provide a default decoder but it is possible to inject custom decoders to decode particular configuration values. The expected decoder implementation depends on the configuration source implementation but most of the time a string to object decoder is expected.

```java
SplittablePrimitiveDecoder<String> customDecoder = ...

PropertyFileConfigurationSource source = new PropertyFileConfigurationSource(new ClasspathResource(URI.create("classpath:/path/to/configuration")), customDecoder)
```

#### Map configuration source

The map configuration is the most basic configuration source implementation. It exposes configuration properties stored in a map in memory. It doesn't support parameterized properties, regardless of the parameters specified in a query, only the property name is considered when resolving a value.

```java
MapConfigurationSource source = new MapConfigurationSource(Map.of("server.url", new URL("http://localhost")));
...
```

This implementation can be used for testing purpose in order to provide a mock configuration source.

#### System environment configuration source

The system environment configuration source exposes system environment variables as configuration properties. As for the map configuration source, this implementation doesn't support parameterized properties.

```plaintext
$ export SERVER_URL=http://localhost
```

```java
SystemEnvironmentConfigurationSource source = new SystemEnvironmentConfigurationSource();
...
```

This implementation can be used to bootstrap an application using system environment variables.

#### System properties configuration source

The system properties configuration source exposes system properties as configuration properties. As for the two previous implementations, it doesn't support parameterized properties.

```plaintext
$ java -Dserver.url=http://localhost ...
```

```java
SystemPropertiesConfigurationSource source = new SystemPropertiesConfigurationSource();
...
```

This implementation can be used to bootstrap an application using system properties.

#### Command line configuration source

The command line configuration source exposes configuration properties specified as command line arguments of the application. This implementation supports parameterized properties.

Configuration properties must be specified as application arguments using the following syntax: `--property[parameter_1=value_1...parameter_n=value_n]=value` where property and parameter names are valid Java identifiers and property and parameter values are Java primitives such as integer, boolean, string... A complete description of the syntax can be found in the [API documentation][inverno-javadoc].

For instance the following are valid configuration properties specified as command line arguments:

```plaintext
$ java ... Main \
--web.server_port=8080 \
--web.server_port[profile="ssl"]=8443 \
--db.url[env="dev"]="jdbc:oracle:thin:@dev.db.server:1521:sid" \
--db.url[env="prod",zone="eu"]="jdbc:oracle:thin:@prod_eu.db.server:1521:sid" \
--db.url[env="prod",zone="us"]="jdbc:oracle:thin:@prod_us.db.server:1521:sid"
```

```java
public static void main(String[] args) {
    CommandLineConfigurationSource source = new CommandLineConfigurationSource(args);
    ...
}
...
```

#### `.properties` file configuration source

The `.properties` file configuration source exposes configuration properties specified in a `.properties` file. This implementation supports parameterized properties.

Configuration properties can be specified in a property file using a syntax similar to the command line configuration source for the property key. Some characters must be escaped with respect to the `.properties` file format. Property values don't need to follow Java's notation for strings since they are considered as strings by design.

```properties
web.server_port=8080 
web.server_port[profile\="ssl"]=8443
db.url[env\="dev"]=jdbc:oracle:thin:@dev.db.server:1521:sid
db.url[env\="prod",zone\="eu"]=jdbc:oracle:thin:@prod_eu.db.server:1521:sid
db.url[env\="prod",zone\="us"]=jdbc:oracle:thin:@prod_us.db.server:1521:sid
```

```java
PropertyFileConfigurationSource source = new PropertyFileConfigurationSource(new ClasspathResource(URI.create("classpath:/path/to/file")));
...
```

#### `.cprops` file configuration source

The `.cprops` file configuration source exposes configuratio properties specified in a `.cprops` file. This implementation supports parameterized properties.

The `.cprops` file format has been introduced to facilitate the definition and reading of parameterized properties. In particular it allows to regroup the definition of properties with common parameters into sections and many more.

For instance:

```properties
server.port=8080
db.url=jdbc:oracle:thin:@localhost:1521:sid
db.user=user
db.password=password
log.level=ERROR
application.greeting.message="""
 === Welcome! === 

     This is      
    a formated    
     message.     

 ================
"""

[ environment="test" ] {
    db.url=jdbc:oracle:thin:@test:1521:sid
    db.user=user_test
    db.password=password_test
}

[ environment="production" ] {
    db.url=jdbc:oracle:thin:@production:1521:sid
    db.user=user_production
    db.password=password_production
    
    [ zone="US" ] {
        db.url=jdbc:oracle:thin:@production.us:1521:sid
    }
    
    [ zone="EU" ] {
        db.url=jdbc:oracle:thin:@production.eu:1521:sid
    }
    
    [ zone="EU", node="node1" ] {
        log.level=DEBUG
    }
}
```

A complete [JavaCC][javacc] [grammar][cprops-grammar] is available in the source of the configuration module.

```java
CPropsFileConfigurationSource source = new CPropsFileConfigurationSource(new ClasspathResource(URI.create("classpath:/path/to/file")));
...
```

#### Bootstrap configuration source

The bootstrap configuration source is a [composite configuration source](#composite-configuration-source) preset with configuration sources typically used when bootstrapping an application.

This implementation resolves configuration properties from the following sources in that order, from the highest priority to the lowest:

- command line
- system properties
- system environment variables
- the `configuration.cprops` file in `./conf/` or `${inverno.conf.path}/` directories if one exists (if the first one exists the second one is ignored)
- the `configuration.cprops` file in `${java.home}/conf/` directory if it exists
- the `configuration.cprops` file in the application module if it exists

This source is typically created in a `main` method to load the bootstrap configuration on startup.

```java
public class Application {

    public static void main(String[] args) {
        BootstrapConfigurationSource source = new BootstrapConfigurationSource(Application.class.getModule(), args);
        
        // Load configuration
        ApplicationConfiguration configuration = ConfigurationLoader
            .withConfiguration(ApplicationConfiguration.class)
            .withSource(source)
            .load()
            .block();

        // Start the application with the configuration
        ...
    }
}
```

#### Redis configuration source

The [Redis][redis] configuration source exposes configuration properties stored in a Redis data store. This implementation supports parameterized properties and it is also configurable which means it can be used to set configuration properties in the data store at runtime.

It also provides a simple but effective versioning system which allows to set multiple properties and activate or revert them atomically. A global revision keeps track of the whole data store but it is also possible to version a particular branch in the tree of properties.

The following example shows how to set configuration properties for the `dev` and `prod` environment and activates them globally or independently.

```java
RedisClient redisClient = ...
RedisConfigurationSource source = new RedisConfigurationSource(redisClient);

source
    .set("db.url", "jdbc:oracle:thin:@dev.db.server:1521:sid").withParameters("environment", "dev").and()
    .set("db.url", "jdbc:oracle:thin:@prod_eu.db.server:1521:sid").withParameters("environment", "prod", "zone", "eu").and()
    .set("db.url", "jdbc:oracle:thin:@prod_us.db.server:1521:sid").withParameters("environment", "prod", "zone", "us")
    .execute()
    .blockLast();
    
// Activate working revision globally
source.activate().block();

// Activate working revision for dev environment and prod environment independently
source.activate("environment", "dev").block();
source.activate("environment", "prod").block();
```

It is also possible to fallback to a particular revision by specifying it in the `activate()` method:

```java
// Activate revision 2 globally
source.activate(2).block();
```

This implementation is particularly suitable to load tenant specific configuration in a multi-tenant application, or user preferences... basically any kind of configuration that can and will be dynamically changed at runtime.

> Parameterized properties and versioning per branch are two simple yet powerful features but it is important to be picky here otherwise there is a real risk of messing things up. You should thoughtfully decide when a configuration branch can be versioned, for instance the versioned sets of properties must be disjointed (if this is not obvious, think again), this is actually checked in the Redis configuration source and an exception will be thrown if you try to do things like this, basically trying to version the same property twice.

#### Composite Configuration source

The composite configuration source is a configuration source implementation with two particular features: first it allows to compose multiple configuration sources into one configuration source and then it supports defaulting strategies to determine the best matching value for a given configuration query key.

The property returned for a configuration query key then depends on two factors: the order in which configuration sources were defined in the composite configuration source, from the highest priority to the lowest, and then how close is a property from the configuration query key. 

The `CompositeConfigurationSource` resolves a configuration property by querying its sources in sequence from the highest priority to the lowest. It relies on a `CompositeConfigurationStrategy` to determine at each round which queries to execute and retain the best matching property from the results. The best matching property is the property whose key is the closest to the original configuration query key according to a metric implemented in the strategy. The algorithm stops when an exact match is found or when there's no more configuration source to query.

The `DefaultCompositeConfigurationStrategy` defines the default strategy implementation. It determines the best matching property for a given original query by prioritizing query parameters from left to right: the best matching property is the one matching the most continuous parameters from right to left. In practice, if we consider query key `property[p1=v1,...pn=vn]`, it supersedes key `property[p2=v2,...pn=vn]` which supersedes key `property[p3=v3,...pn=vn]`... which supersedes key `property[]`. As a result, an original query with `n` parameters results in `n+1` queries being actually executed if no property was retained in previous rounds and `n-p` queries if a property with p parameters (p<n) was retained in previous rounds. The order into which parameters are specified in the original query is then significant: `property[p1=v1,p2=v2] != property[p2=v2,p1=v1]`.

When defining configuration parameters, we should then order them from the most specific to the most general when querying a composite source. For example, the `node` parameter which is more specific than the `zone` parameter should come first then the `zone` parameter which is more specific than the `environment` parameter should come next and finally the `environment` parameter which is the most general should come last.

For instance, we can consider two parameterized configuration sources: `source1` and `source2`.

`source1` holds the following properties:

- `server.url[]=null`
- `server.url[zone="US", environment="production"]="https://prod.us"`
- `server.url[zone="EU"]="https://default.eu"`

`source2` holds the following properties:

- `server.url[]="https://default"`
- `server.url[environment="test"]="https://test"`
- `server.url[environment="production"]="https://prod"`

We can compose them in a composite configuration source as follows:

```java
ConfigurationSource<?, ?, ?> source1 = ...
ConfigurationSource<?, ?, ?> source2 = ...

CompositeConfigurationSource source = new CompositeConfigurationSource(List.of(source1, source2));

source                                                         // 1
    .get("server.url")
    .withParameters("zone", "US", "environment", "production")
    .execute()
    ...

source                                                         // 2
    .get("server.url")
    .withParameters("environment", "test")
    .execute()
    ...

source                                                         // 3
    .get("server.url")
    .execute()
    ...


source                                                         // 4
    .get("server.url")
    .withParameters("zone", "EU", "environment", "production")
    .execute()...

source                                                         // 5
    .get("server.url")
    .withParameters("environment", "production", "zone", "EU")
    .execute()
    ...
    
```

In the example above:

1. `server.url[environment="production",zone="US"]` is exactly defined in `source1` => `https://prod.us` defined in `source1` is returned
2. `server.url[environment="test"]` is not defined in `source1` but exactly defined in `source2` => `https://test`  defined in `source2` is returned
3. Although `server.url[]` is defined in both `source1` and `source2`, `source1` has the highest priority and therefore => `null` is returned
4. There is no exact match for `server.url[zone="EU", environment="production"]` in both `source1` and `source2`, the priority is given to the parameters from left to right, the property matching `server.url[environment="production"]` shall be returned => `https://prod` defined in `source2` is returned
5. Here we've simply changed the order of the parameters in the previous query, again the priority is given to parameters from left to right, since there is no match for `server.url[environment="production", zone="EU"]`, `server.url[zone="EU"]` is considered => `https://default.eu` defined in `source1` is returned

As you can see, the order into which parameters are specified in a query is significant and different results might be returned.

When considering multiple configuration sources, properties can be defined with the exact same key in two different sources, the source with the highest priority wins. In the last example we've been able to set the value of `server.url[]` to `null` in `source1`, however `null` is itself a value with a different meaning than a missing property, the `unset` value can be used in such situation to *unset* a property defined in a source with a lower priority.

For instance, considering previous example, we could have defined `server.url[]=unset` instead of `server.url[]=null` in `source1`, the query would then have returned an empty query result indicating an undefined property.

#### Configurable configuration source

A configurable configuration source is a particular configuration source which supports configuration properties updates. The [Redis configuration source](#redis-configuration-source) is an example of configurable configuration source.

The `ConfigurableConfigurationSource` interface is the main entry point for updating configuration properties, it shall be used every time there's a need to retrieve or set configuration properties.

It extends the `ConfigurationSource` with one method for creating a `ConfigurationUpdate` instance eventually executed in order to set one or more configuration properties in the configuration source.

For instance, a parameterized property `server.port` can be set in a configuration source as follows:

```java
ConfigurableConfigurationSource<?, ?, ?, ?, ?, ?> source = null;

source.set("server.port", 8080)
    .withParameters("environment", "production", "zone", "us")
    .execute()
    .single()
    .subscribe(
        updateResult -> {
            try {
                updateResult.check();
                // Update succeeded
                ...
            }
            catch(ConfigurationSourceException e) {
                // Update failed
                ...
            }
        }
    );
```

A configurable configuration source relies on a `JoinablePrimitiveEncoder` to encode property values. Implementations usually provide a default encoder but it is possible to inject custom encoders to encode particular configuration values. The expected encoder implementation depends on the configuration source implementation but most of the time an object to string encoder is expected.

```java
RedisClient redisClient = ...
JoinablePrimitiveEncoder<String> customEncoder = ...
SplittablePrimitiveDecoder<String> customDecoder = ...

RedisConfigurationSource source = new RedisConfigurationSource(redisClient, customEncoder, customDecoder)
```

### Configuration loader

The API offers a great flexibility but as we've seen it might require some efforts to load a configuration in a usable explicit Java bean. Hopefully, this has been anticipated and the configuration module provides a configuration loader to smoothly load configuration objects in the application.

The `ConfigurationLoader` interface is the main entry point for loading configuration objects from a configuration source. It can be used in two different ways, either dynamically using Java reflection or statically using the Inverno compiler.

#### Dynamic loader

A dynamic loader can be created by invoking static method `ConfigurationLoader#withConfiguration()` which accepts a single `Class` argument specifying the type of the configuration that must be loaded. 

A valid configuration type must be an interface defining configuration properties as non-void no-argument methods whose names correspond to the configuration properties to retrieve and to map to the resulting configuration object, default values can be specified in default methods.

For instance the following interface represents a valid configuration type which can be loaded by a configuration loader:

```java
public interface AppConfiguration {

    // query property 'server_host'
    String server_host();

    // query property 'server_port'
    default int server_port() {
        return 8080;
    }
}
```

It can be loaded at runtime as follows:

```java
ConfigurationSource<?, ?, ?> source = ...

ConfigurationLoader
    .withConfiguration(AppConfiguration.class)
    .withSource(source)
    .withParameters("environment", "production")
    .load()
    .map(configuration -> startServer(configuration.server_host(), configuration.server_port()))
    .subscribe();
```

In the above example, the configuration source is queried for properties `server_host[environment="production"]` and `server_port[environment="production"]`.

The dynamic loader also supports nested configurations when the return type of a method is an interface representing a valid configuration type.

```java
public interface ServerConfiguration {

    // query property 'server_host'
    String server_host();
    
    // query property 'server_port'
    default int server_port() {
        return 8080;
    }
}
```

```java
public interface AppConfiguration {
    
    // Prefix child property names with 'server_configuration'
    ServerConfiguration server_configuration();
}
```

In the above example, the configuration source is queried for properties `server_configuration.server_host[environment="production"]` and `server_configuration.server_port[environment="production"]`.

It is also possible to load a configuration by invoking static method `ConfigurationLoader#withConfigurator()` which allows to load any type of configuration (not only interface) by relying on a configurator and a mapping function. 

A configurator defines configuration properties as void single argument methods whose names correspond to the configuration properties to retrieve and inject into a configurator instance using a dynamic configurer `Consumer<Configurator>`. The mapping function is finally applied to that configurer to actually create the resulting configuration object.

For instance, previous example could have been implemented as follows:

```java
public class AppConfiguration {
    
    private String server_host;
    private String server_port = 8080;
    
    // query property 'server_host'
    public void server_host(String server_host) {
        this.server_host = server_host;
    }
    
    // query property 'server_port'
    public void server_port(int server_port) {
        this.server_port = server_port;
    }
    
    public String server_host() {
        return server_host;
    }
    
    public int server_port() {
        return server_port;
    }
}
```

```java
ConfigurationSource<?, ?, ?> source = ...

ConfigurationLoader
    .withConfigurator(AppConfiguration.class, configurer -> {
        AppConfiguration configuration = new AppConfiguration();
        configurer.apply(configuration);
        return configuration;
    })
    .withSource(source)
    .withParameters("environment", "production")
    .load()
    .map(configuration -> startServer(configuration.server_host(), configuration.server_port()))
    .subscribe();
```

#### Static loader

Dynamic loading is fine but it relies on Java reflection which induces extra processing at runtime and might cause unexpected runtime errors due to the lack of static checking. This is all the more true as most of the time configuration definitions are known at compile time. For these reasons, it is better to create adhoc configuration loader implementations. Fortunately, the configuration Inverno compiler plugin can generate these for us.

In order to create a configuration bean in an Inverno module, we simply need to create an interface for our configuration as specified above and annotates it with `@Configuration`, this will tell the configuration Inverno compiler plugin to generate a corresponding configuration loader implementation as well as a module bean making our configuration directly available inside our module.

```java
@Configuration
public interface AppConfiguration {

    // query property 'server_host'
    String server_host();

    // query property 'server_port'
    int server_port();
}
```

The preceding code will result in the generation of class `AppConfigurationLoader` which can then be used to load configuration at runtime without resorting to reflection.

```java
ConfigurationSource<?, ?, ?> source = ...

new AppConfigurationLoader()
    .withSource(source)
    .withParameters("environment", "production")
    .load()
    .map(configuration -> startServer(configuration.server_host(), configuration.server_port()))
    .subscribe();
```

A configuration can also be obtained *manually* as follows:

```java
AppConfiguration defaultConfiguration = AppConfigurationLoader.load(configurator -> configurator.server_host("0.0.0.0"));

AppConfiguration customConfiguration = AppConfigurationLoader.load(configurator -> configurator.server_host("0.0.0.0"));
```

By default, the generated loader also defines an overridable module bean which loads the configuration in the module. This bean defines three optional sockets:

- **configurationSource** indicates the configuration source to query when initializing the configuration bean
- **parameters** indicates the parameters to consider when querying the source
- **configurer** provides a way to overrides default values

If no configuration source is present, a default configuration is created, otherwise the configuration source is queried with the parameters, the resulting configuration is then *patched* with the configurer if present. The bean is overridable by default which means we can inject our own implementation if we feel like it.

It is possible to disable the activation of the configuration bean or make it non overridable in the `@Configuration` interface:

```java
@Configuration(generateBean = false, overridable = false)
public interface AppConfiguration {
    ...
}
```

Finally, nested beans can be specified in a configuration which is convenient when a module is composing multiple modules and we wish to aggregate all configurations into one single representation in the composite module. 

For instance, we can have the following configuration defined in a component module:

```java
@Configuration
public interface ComponentModuleConfiguration {
    ...
}
```

and the following configuration defined in the composite module:

```java
@Configuration
public interface CompositeModuleConfiguration {

    @NestedBean
    ComponentModuleConfiguration component_module_configuration();
}
```

In the preceding example, we basically indicate to the Inverno framework that the `ComponentModuleConfiguration` defined in the `CompositeModuleConfiguration` must be injected into the component module instance.

## HTTP Base

The Inverno *http-base* module defines the foundational API for creating HTTP clients and servers. It also provides common HTTP services such as the header service.

In order to use the Inverno *http-base* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    requires io.inverno.mod.http.base;
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
            <artifactId>inverno-http-base</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
...
compile 'io.inverno.mod:inverno-http-base:1.2.0'
...
```

The *http-base* module is usually provided as a transitive dependency by other HTTP modules, the *http-server* module or the *web* module in particular, so this might not be necessary.

### HTTP base API

The base HTTP API defines common classes and interfaces for implementing applications or modules using HTTP/1.x or HTTP/2 protocols. This includes:

- HTTP methods and status enumerations
- Exception bindings for HTTP errors: `BadRequestException`, `InternalServerErrorException`...
- basic building blocks such as `Parameter` which defines the base interface for any HTTP component that can be represented as a key/value pair (eg. query parameter, header, cookie...)
- Cookie types: `Cookie` and `SetCookie`
- Common HTTP header names (`Headers.NAME_*`) and values (`Headers.VALUE_*`) constants
- Common HTTP header types: `Headers.ContentType`, `Headers.Accept`...
- HTTP header codec API for implementing HTTP header codec used to decode a raw HTTP header in a specific `Header` object
- A HTTP header service used to encode/decode HTTP headers from/to specific `Header` objects

### HTTP header service

The HTTP header service is the main entry point for decoding and encoding HTTP headers.

The `HeaderService` interface defines method to decode/encode `Header` object from/to `String` or `ByteBuf`.

For instance, a `content-type` header can be parsed as follows:

```java
HeaderService headerService = ...

Headers.ContentType contentType = headerService.<Headers.ContentType>decode("content-type", "application/xml;charset=utf-8");

// application/xml
String mediaType = contentType.getMediaType();
// utf-8
Charset charset = contentType.getCharset();

```

The *http-base* module provides a default implementation exposed as a bean which relies on a set of `HeaderCodec` to support specific headers. Custom header codecs can then be injected in the module to extend its capabilities.

For instance, we can create an `ApplicationContextHeaderCodec` codec in order for the header service to decode custom `application-context` headers to  `ApplicationContextHeader` instances. The codec must be injected in the *http-base* module either explicitly when creating the module or through dependency injection.

```java
Base httpBase = new Base.Builder()
    .setHeaderCodecs(List.of(new ApplicationContextHeaderCodec())
    .build();

httpBase.start();

ApplicationContextHeaderCodec decodedHeader = httpBase.headerService().<ApplicationContextHeaderCodec>.decode("...")
...

httpBase.stop();
```

Most of the time the *http-base* module is composed in a composite module and as a result dependency injection should work just fine, so we simply need to declare the codec as a bean in the module composing the *http-base* module to extend the header service.

By default, the *http-base* module provides codecs for the following headers:

- `accept` as defined by [RFC 7231 Section 5.3.2][rfc-7231-5.3.2]
- `accept-language` as defined by [RFC 7231 Section 5.3.5][rfc-7231-5.3.5]
- `content-disposition` as defined by [RFC 6266][rfc-6266]
- `content-type` as defined by [RFC 7231 Section 3.1.1.5][rfc-7231-5.1.1.5]
- `cookie` as defined by [RFC 6265 Section 4.2][rfc-6265-4.2]
- `set-cookie` as defined by [RFC 6265 Section 4.1][rfc-6265-4.1]

## HTTP Server

The Inverno *http-server* module provides fully reactive HTTP/1.x and HTTP/2 server based on [Netty][netty].

It especially supports:

- HTTP/1.x pipelining
- HTTP/2 over cleartext
- HTTP Compression
- TLS
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

```groovy
...
compile 'io.inverno.mod:inverno-boot:1.2.0'
compile 'io.inverno.mod:inverno-http-server:1.2.0'
...
```

### HTTP Server exchange API

The module defines classes and interfaces to implement HTTP server exchange handlers used to handle HTTP requests sent by a client to the server.

A server `ExchangeHandler` is defined to handle a server `Exchange` composed of the `Request` and `Response` pair in a HTTP communication between a client and a server. The API has been designed to be fluent and reactive in order for the request to be *streamed* down to the response.

#### Basic exchange

The `ExchangeHandler` is a functional interface, a basic exchange handler can then be created as follows:

```java
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .headers(headers -> headers.contentType(MediaTypes.TEXT_PLAIN))
        .body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello, world!", Charsets.DEFAULT)));
};
```

The above code creates an exchange handler sending a `Hello, world!` message in response to any request.

We might also want to send the response in a reactive way in a stream of data in case the entire response payload is not available right away, if it doesn't fit in memory or if we simply want to send a response in multiple parts as soon as they become available (eg. progressive display).

```java
ExchangeHandler<Exchange> handler = exchange -> {
    Flux<ByteBuf> dataStream = Flux.just(
        Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)),
        Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", world!", Charsets.DEFAULT))
    );

    exchange.response()
        .body().raw().stream(dataStream);
};
```

#### Request body

Request body can be handled in a similar way. The reactive API allows to process the payload of a request as the server receives it and therefore progressively build and send the corresponding response.

```java
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .body().raw().stream(exchange.request().body()
            .map(body -> Flux.from(body.raw().stream()).map(chunk -> Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(chunk.readableBytes()))))
            .orElse(Flux.just(Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(0))))
        );
};
```

In the above example, if a client sends a payload in the request, the server responds with the number of bytes of each chunk of data it receives or it responds `0` if the request payload is empty. This simple example illustrates how we can process requests as flow of data

#### URL Encoded form

HTML form data are sent in the body of a POST request in the form of key/value pairs encoded in [application/x-www-form-urlencoded format][form-urlencoded]. The resulting list of `Parameter` can be obtained as follows:

```java
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
            .map(parameter -> Unpooled.copiedBuffer(Unpooled.copiedBuffer("Received parameter " + parameter.getName() + " with value " + parameter.getValue(), Charsets.DEFAULT)))
        );
}
```

In the above example, for each form parameters the server responds with a message describing the parameters it just received. Again this shows that the API is fully reactive and form parameters can be processed as they are decoded.

A more traditional example though would be to obtained the map of parameters grouped by names (because multiple parameters with the same name can be sent):

```java
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
        .collectMultimap(Parameter::getName)
            .map(formParameters -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("User selected options: " + formParameters.get("options").stream().map(Parameter::getValue).collect(Collectors.joining(", ")), Charsets.DEFAULT)))
        );
}
```

> Here we may think that the aggregation of parameters in a map could *block* the I/O thread but this is definitely not true, when a parameter is decoded, the reactive framework is notified and the parameter is stored in a map, after that the I/O thread can be reallocated. When the parameters publisher completes the resulting map is emitted to the mapping function which build the response. During all this process, no thread is ever waiting for anything.

#### Multipart form

A [multipart/form-data][rfc-7578] request can be handled in a similar way. Form parts can be obtained as follows:

```java
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().multipart().stream())
            .map(part -> Unpooled.copiedBuffer(Unpooled.copiedBuffer("Received part " + part.getName(), Charsets.DEFAULT)))
        );
};
```

In the above example, the server responds with the name of the part it just received. Parts are decoded and can be processed along the way, a part is like a body embedded in the request body with its own headers and payload.

Multipart form data is most commonly used for uploading files over HTTP. Such handler can be implemented as follows using the [resource API](#resource-api) to store uploaded files:

```java
ExchangeHandler<Exchange> handler = exchange -> {
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

#### Resource

A [resource](#resource-api) can be sent as a response to a request. When this is possible the server uses low-level ([zero-copy][zero-copy]) API for fast resource transfer.

```java
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .body().resource().value(new FileResource("/path/to/resource"));
};
```

The media type of the resource is resolved using a [media type service](#media-type-service) and automatically set in the response `content-type` header field. 

> If a specific resource is created as in above example the media type service used is the one defined when creating the resource or a default implementation if none was specified. If the resource is obtained with the resource service provided in the *boot* module the media type service used is the one provided in the *boot* module.

#### Server-sent events

[Server-sent events][server-sent-events] provide a way to send server push notifications to a client. It is based on [chunked transfer encoding][chunked-transfer-encoding] over HTTP/1.x and regular streams over HTTP/2. The API provides an easy way to create SSE endpoints.

```java
ExchangeHandler<Exchange> handler = exchange -> {
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

#### Error exchange handler

An error exchange handler is a particular exchange handler which is defined to handle server error exchange. In other words it is used by the server to handle exceptions thrown during the processing of a regular exchange in order to send an appropriate response to the client when this is still possible (ie. assuming response headers haven't been sent yet).

```java
ErrorExchangeHandler<Throwable> errorHandler = errorExchange -> {
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

#### Misc

The API is fluent and mostly self-describing as a result it should be easy to find out how to do something in particular, even so here are some miscellaneous elements.

##### Request headers

A particular request header can be obtained as follows, if there are multiple headers with the same name, the first one shall be returned:

```java
ExchangeHandler<Exchange> handler = exchange -> {
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
ExchangeHandler<Exchange> handler = exchange -> {
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
ExchangeHandler<Exchange> handler = exchange -> {
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

##### Query parameters

Query parameters in the request can be obtained as follows:

```java
ExchangeHandler<Exchange> handler = exchange -> {
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

##### Request cookies

Request cookie can be obtained in a similar way as follows:

```java
ExchangeHandler<Exchange> handler = exchange -> {
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

##### Request components

The API also gives access to multiple request related information such as:

- the HTTP method
- the scheme (`http` or `https`)
- the authority part of the requested URI (`host` header in HTTP/1.x and `:authority` pseudo-header in HTTP/2)
- the requested path including query string
- the absolute path which is the normalized requested path without the query string
- the `URIBuilder` corresponding to the requested path to build relative paths
- the query string
- the socket address of the client or last proxy that sent the request

##### Response headers/trailers

Response headers can be added or set fluently using a configurator as follows:

```java
ExchangeHandler<Exchange> handler = exchange -> {
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
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .trailers(headers -> headers
            .add("some-trailer", "abc")
        )
        .body().raw()...;
};
```

##### Response status

The response status can be set in the response headers following HTTP/2 specification as defined by [RFC 7540 Section 8.1.2.4][rfc-7540-8.1.2.4].

```java
ExchangeHandler<Exchange> handler = exchange -> {
    exchange.response()
        .headers(headers -> headers.status(Status.OK))
        .body().raw();
};
```

##### Response cookies

Response cookies can be set fluently using a configurator as follows:

```java
ExchangeHandler<Exchange> handler = exchange -> {
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

### HTTP Server

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
     ║                    ,   \/   ,                                  -- 1.3.0 --                 ║
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

#### Configuration

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

Please refer to the [API documentation](javadoc) to have an exhaustive description of the different configuration properties. We can for instance configure low level network settings like TCP keep alive or TCP no delay as well as HTTP related settings like compression or TLS.

> You can also refer to the [configuration module documentation](#configuration-1) to get more details on how configuration works and more especially how you can from here define the HTTP server configuration in command line arguments, property files...

##### Logging

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

##### Transport

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

#### Root handler

The HTTP server defines a root exchange handler to handle all HTTP requests. By default, it uses a basic handler implementation which returns `Hello` when a request is made to the root path `/` and return (404) not found errors otherwise. 

In order to use our own handler, we must define an exchange handler bean in the *app_http* module:

```java
package io.inverno.example.app_http;

import io.netty.buffer.Unpooled;
import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;

@Bean
public class CustomHandler implements ExchangeHandler<Exchange> {

    @Override
    public void handle(Exchange exchange) throws HttpException {
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
import io.inverno.mod.http.server.ExchangeHandler;

public class Main {

    @Bean
    public static interface Handler extends Supplier<ExchangeHandler<Exchange>> {}

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setHandler(exchange -> {
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

#### Error handler

The HTTP server defines an error exchange handler to handle exceptions thrown when processing HTTP requests when this is still possible, basically when the response headers haven't been sent yet to the client. By default, it uses a basic error handler implementation which handles standard `HttpException` and responds empty body messages with HTTP error status corresponding to the exception.

This default implementation should be enough for a basic HTTP server but a custom handler can be provided to produce custom error pages for specific types of error. This can be done in the exact same way as the [root handler](#root-handler) by defining an error exchange handler bean:

```java
package io.inverno.example.app_http;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.ExchangeHandler;

@Bean
public class CustomErrorHandler implements ExchangeHandler<ErrorExchange<Throwable>> {

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
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;

public class Main {

    @Bean
    public static interface Handler extends Supplier<ExchangeHandler<Exchange>> {}
    
    @Bean
    public static interface ErrorHandler extends Supplier<ErrorExchangeHandler<Throwable>> {}

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

#### HTTP compression

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

#### TLS configuration

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


#### Extend HTTP services

The HTTP server relies on a header service and a parameter converter to respectively decode HTTP headers and convert parameter values.

The *http-server* module defines a socket to plug custom `HeaderCodec` instances so the HTTP header service can be extended to decode custom HTTP headers as described in the [HTTP header service documentation](#http-header-service).

It also defines a socket to plug a custom parameter converter which is a basic `StringConverter` by default. Since we created the *app_http* module by composing *boot* and *http-server* modules, the parameter converter provided by the *boot* module should then override the default. This converter is a `StringCompositeConverter` which can be extended by injecting custom `CompoundDecoder` and/or `CompoundEncoder` instances in the *boot* module as described in the [composite converter documentation](#composite-converter).

To sum up, all we have to do to extend these services is to provide `HeaderCodec`, `CompoundDecoder` or `CompoundEncoder` beans in the *app_http* module.

### Wrap-up

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
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ExchangeHandler;

public class Main {

    @Bean
    public static interface Handler extends Supplier<ExchangeHandler<Exchange>> {}
    
    @Bean
    public static interface ErrorHandler extends Supplier<ErrorExchangeHandler<Throwable>> {}

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
            .setHandler(exchange -> {
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

## Web

The Inverno *web* module provides extended functionalities on top of the *http-server* module for developing Web and RESTfull applications.

It especially provides:

- HTTP request routing
- content negotiation
- automatic message payload conversion
- path parameters
- static handler for serving static resources
- version agnostic [WebJars][webjars] support
- easy Web/REST controller development
- [OpenAPI][open-api] specifications generation using Web controllers JavaDoc comments
- SwaggerUI integration
- an Inverno compiler plugin providing static validation of the routes and generation of Web router configurers

The *web* module composes the *http-server* module and therefore starts a HTTP server. Just like the *http-server* module, it requires a net service and a resource service as well as a list of [media type converters](#media-type-converter) for message payload conversion. Basic implementations of these services are provided by the *boot* module which provides `application/json`, `application/x-ndjson` and `text/plain` media type converters. Additional media type converters can surely be provided by implementing the `MediaTypeConverter` interface.

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

```groovy
...
compile 'io.inverno.mod:inverno-boot:1.2.0'
compile 'io.inverno.mod:inverno-web:1.2.0'
...
```

### Routing

The *web* module also defines an API for routing HTTP requests to the right handlers. 

A **router** is a standard server exchange handler as defined by the *http-server* module API which can be used as root handler or error handler in the HTTP server, its role is to route an exchange to a handler based on a set of rules applied to the exchange.

A **route** specifies the rules that an exchange must matched to be routed to a particular handler.

A **route manager** is used to manage the routes in a router or, more explicitly, to list, create, enable or disable routes in a router.

> The module defines a high level routing API with `Router`, `Route`, `RouteManager` and `RouterConfigurer` that can be used as a base to implement custom routing implementations in addition to the provided Web and error routing implementations. Nevertheless, it is more of a guideline, one can choose a totally different approach to implement routing, in the end the HTTP server expects a `ExchangeHandler<ByteBuf>` what is done inside is completely opaque, the Web API only shows one way to do it.

#### Web routing

A `WebRouter` is used to route a `WebExchange` to the right `WebExchangeHandler`, it implements `ExchangeHandler<ByteBuf>` and it is typically used as root handler in the HTTP server.

##### Web Server exchange

The *web* module API extends the [server exchange API](#http-server-exchange-api) defined in the *http-server* module and defines the server `WebExchangeHandler` to handle a server `WebExchange` composed of the `WebRequest` and `WebResponse` pair in a HTTP communication between a client and a server. These interfaces respectively extends the `ExchangeHandler`, `Exchange`, `Request` and `Response` interfaces which are defined in the *http-server* module. A Web exchange handler is typically attached to one or more Web routes defined in a `WebRouter`.

###### Exchange attributes

Attributes can be set on a `WebExchange` to propagate contextual information such as a security or functional context in a chain of Web exchange handlers:

```java
WebExchangeHandler<WebExchange> handler = exchange -> {
    exchange.<SecurityContext>getAttribute("security_context")
        .filter(SecurityContext::isAuthenticated)
        .ifPresentOrElse(
            securityContext -> {
                ...
            },
            () -> exchange.response().headers(headers -> headers.status(Status.UNAUTHORIZED)).body().empty()
        );
};

WebExchangeHandler<WebExchange> securityInterceptor = exchange -> {
    SecurityContext securityContext = ...;
    exchange.setAttribute("security_context", securityContext);
    
    handler.handle(exchange);
};
```

###### Path parameters

Path parameters are exposed in the `WebRequest`, they are extracted from the requested path by the [Web router](#web-router) when the handler is attached to a route matching a parameterized path as defined in a [URI builder](#uris).

For instance, if the handler is attached to a route matching `/book/{id}`, the `id` path parameter can be retrieved as follows:

```java
WebExchangeHandler<WebExchange> handler = exchange -> {
    exchange.request().pathParameters().get("id")
        .ifPresentOrElse(
            id -> {
                ...
            },
            () -> exchange.response().headers(headers -> headers.status(Status.NOT_FOUND)).body().empty()
        );
};
```

###### Request body decoder

The request body can be decoded based on the content type defined in the request headers.

```java
WebExchangeHandler<WebExchange> handler = exchange -> {
    Mono<Result> storeBook = exchange.request().body().get()
        .decoder(Book.class)
        .one()
        .map(book -> storeBook(book));
    exchange.response().body()
        .raw().stream(storeBook.map(result -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(result.getMessage(), Charsets.DEFAULT))));
};
```

When invoking the `decoder()` method, a [media type converter](#media-type-converter) corresponding to the request content type is selected to decode the payload. The `content-type` header MUST be specified in the request, otherwise (400) bad request error is returned indicating an empty media type. If there is no converter corresponding to the media type, an (415) unsupported media type error is returned indicating that no decoder was found matching the content type.

A decoder is obtained by specifying the type of the object to decode in the `decoder()` method, the type can be a `Class<T>` or a `java.lang.reflect.Type` which allows to decode parameterized types at runtime bypassing type erasure. Parameterized Types can be built at runtime using the [reflection API](#reflection-api).

As you can see in the above example the decoder is fully reactive, a request payload can be decoded in a single object by invoking method `one()` on the decoder which returns a `Mono<T>` publisher or in a stream of objects by invoking method `many()` on the decoder which returns a `Flux<T>` publisher. 

Decoding multiple payload objects is indicated when a client streams content to the server. For instance, it can send a request with `application/x-ndjson` content type in order to send multiple messages in a single request. Since everything is reactive the server doesn't have to wait for the full request and it can process a message as soon as it is received. What is remarkable is that the code is largely unchanged.

```java
WebExchangeHandler<WebExchange> handler = exchange -> {
    Flux<Result> storeBook = exchange.request().body().get()
        .decoder(Book.class)
        .many()
        .map(book -> storeBook(book));
    exchange.response().body()
        .raw().stream(storeBook.map(result -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(result.getMessage(), Charsets.DEFAULT))));
};
```

Conversion of a multipart form data request body is also supported, the payload of each part being decoded independently based on the content type of the part. For instance we can upload multiple books in multiple files in a `multipart/form-data` request and decode them on the fly as follows:

```java
WebExchangeHandler<WebExchange> handler = exchange -> {
    exchange.response()
        .body().raw().stream(Flux.from(exchange.request().body().get().multipart().stream())                          // 1
            .flatMap(part -> part.decoder(Book.class).one())                                                          // 2
            .map(book -> storeBook(book))                                                                             // 3
            .map(result -> Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(result.getMessage(), Charsets.DEFAULT))) // 4
        );
};
```

In the previous example:

1. A stream of files is received in a `multipart/form-data` request (note that we assume all parts are file parts).
2. Each part is decoded to a `Book` object, the media type must be specified in the `content-type` header field of the part.
3. The book object so obtained is processed.
4. The result for each upload is returned to the client.

All this process is done in a reactive way, the first chunk of response can be sent before all parts have been processed.

###### Response body encoder

As for the request body, the response body can be encoded based on the content type defined in the response headers. Considering previous example we can do the following:

```java
WebExchangeHandler<WebExchange> handler = exchange -> {
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
WebExchangeHandler<WebExchange> handler = exchange -> {
    Flux<SearchResult> searchResults = ...;
    exchange.response()
        .headers(headers -> headers.contentType(MediaTypes.APPLICATION_X_NDJSON))
        .body()
            .encoder(SearchResult.class)
            .many(searchResults);
};
```

##### Web router

A `WebRouter` routes a Web server exchange to an exchange handler attached to a Web route by matching it against a combination of routing rules specified in the route. A Web route can combine the following routing rules which are matched in that order: the path, method and content type of the request, the media ranges and language ranges accepted by the client. For instance, the Web router matches an exchange against the path routing rule first, then the method routing rule... Multiples routes can then match a given exchange but only one will be retained to actually process the exchange which is the one matching the highest routing rules.

If a route doesn't define a particular routing rule, the routing rule is simply ignored and matches all exchanges. For instance, if a route doesn't define any method routing rule, exchanges are matched regardless of the method.

The following is an example of a Web route which matches all exchanges, this is the simplest route that can be defined on a router:

```java
router
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

1. A new `WebRouteManager` instance is obtained from the Web router to configure a `WebRoute`
2. We only define the handler of the route as a result any exchange might be routed to that particular route unless a more specific route matching the exchange exists.

> A `WebRouter` is exposed in the *web* module and wired to the *http-server* module to override the HTTP server's root handler, it can be configured in a `WebRouterConfigurer` as defined in the [Web Server documentation](#web-server).

An exchange handler can be attached to multiple routes at once by providing multiple routing rules to the route manager, the following example actually results in 8 individual routes being created in the Web router:

```java
router
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

The Web routes defined in a Web router can be queried as they are defined by invoking the `findRoutes()` method instead of the `handler()` method on the route manager. The following example select all routes matching `GET` method:

```java
Set<WebRoute<WebExchange>> routes = router
    .route()
        .method(Method.GET)
        .findRoutes();
```

It is also possible to enable, disable or remove a set of routes in a similar way:

```java
// Disables all GET routes
router
    .route()
        .method(Method.GET)
        .disable();

// Enables all GET routes
router
    .route()
        .method(Method.GET)
        .enable();

// remove all GET routes
router
    .route()
        .method(Method.GET)
        .remove();
```

Individual routes can also be enabled, disabled or removed as follows:

```java
// Disables all GET routes producing 'application/json'
router
    .route()
        .method(Method.GET)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::disable);

// Enables all GET routes producing 'application/json'
router
    .route()
        .method(Method.GET)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::enable);

// Removes all GET routes producing 'application/json'
router
    .route()
        .method(Method.GET)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::remove);
```

###### Path routing rule

The path routing rule matches exchanges whose request targets a specific path or a path that matches against a particular pattern. The path or path pattern of a routing rule must be absolute (ie. start with `/`).

We can for instance define a route to handle all requests to `/bar/foo` as follows:

```java
router
    .route()
        .path("/foo/bar")
        .handler(exchange -> {
            ...
        });
```

The route in the preceding example specifies an exact match for the exchange request path, it is also possible to make the route match the path with or without a trailing slash as follows:

```java
router
    .route()
        .path("/foo/bar", true)
        .handler(exchange -> {
            ...
        });
```

A path pattern following the [parameterized URIs notation](#uris) can also be specified to create a routing rule matching multiple paths. This also allows to specify [path parameters](#path-parameters) that can be retrieved from the `WebExchange`.

In the following example, the route will match exchanges whose request path is `/book/1`, `/book/abc`... and store the extracted parameter value in path parameter `id`:

```java
router
    .route()
        .path("/book/{id}")
        .handler(exchange -> {
            exchange.request().pathParameters().get("id")...
        });
```

A parameter is matched against a regular expression set to `[^/]*` by default which is why previous route does not match `/book/a/b`. Parameterized URIs allow to specify the pattern matched by a particular path parameter using `{[<name>][:<pattern>]}` notation, we can then put some constraints on path parameters value. For instance, we can make sure the `id` parameter is a number between 1 and 999:

```java
router
    .route()
        .path("/book/{id:[1-9][0-9]{0,2}}")
        .handler(exchange -> {
            ...
        });
```

If we just want to match a particular path without extracting path parameters, we can omit the parameter name and simply write:

```java
router
    .route()
        .path("/book/{}")
        .handler(exchange -> {
            ...
        });
```

###### Method routing rule

The method routing rule matches exchanges that have been sent with a particular HTTP method.

In order to handle all `GET` exchanges, we can do:

```java
router
    .route()
        .method(Method.GET)
        .handler(exchange -> {
            ...
        });
```

###### Consume routing rule

The consume routing rule matches exchanges whose request body content type matches a particular media range as defined by [RFC 7231 Section 5.3.2][rfc-7231-5.3.2].

For instance, in order to match all exchanges with a `application/json` request payload, we can do:

```java
router
    .route()
        .method(Method.POST)
        .consumes(MediaTypes.APPLICATION_JSON)
        .handler(exchange -> {
            ...
        });
```

We can also specify a media range to match, for example, all exchanges with a `*/json` request payload:

```java
router
    .route()
        .method(Method.POST)
        .consumes("*/json")
        .handler(exchange -> {
            ...
        });
```

The two previous routes are different and as a result they can be both defined in the router, a content negotiation algorithm is used to determine which route should process a particular exchange as defined in [RFC 7231 Section 5.3][rfc-7231-5.3].

Routes are sorted by consumed media ranges as follows:

- quality value is compared first as defined by [RFC7231 Section 5.3.1][rfc-7231-5.3.1], the default quality value is 1.
- type and subtype wildcards are considered after: `a/b` > `a/*` > `*/b` > `*/*`
- parameters are considered last, the most precise media range which is the one with the most parameters with values gets the highest priority (eg. `application/json;p1=a;p2=2` > `application/json;p1=b` > `application/json;p1`)

The Web router then selects the first route whose media range matches the request `content-type` header field.

If we consider previous routes, an exchange with a `application/json` request payload will be matched by the first route while an exchange with a `text/json` request will be matched by the second route.

A media range can also be parameterized which allows for interesting setup such as:

```java
router
    .route()
        .path("/document")
        .method(Method.POST)
        .consumes("application/json;version=1")
        .handler(exchange -> {
            ...
        });
    
router
    .route()
        .path("/document")
        .method(Method.POST)
        .consumes("application/json;version=2")
        .handler(exchange -> {
            ...
        });

router
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

> As described before, if no route is defined with a consume routing rule, exchanges are matched regardless of the request content type, content negotiation is then eventually delegated to the handler which must be able to process the payload whatever the format.

###### Produce routing rule

The produce routing rule matches exchanges based on the acceptable media ranges supplied by the client in the `accept` header field of the request as defined by [RFC 7231 Section 5.3.2][rfc-7231-5.3.2].

A HTTP client (eg. Web browser) typically sends a `accept` header to indicate the server which response media types are acceptable in the response. The Web router determines the best matching route based on the media types produced by the routes matching previous routing rules.

We can for instance define the following routes:

```java
router
    .route()
        .path("/doc")
        .produces(MediaTypes.APPLICATION_JSON)
        .handler(exchange -> {
            ...
        });
    
router
    .route()
        .path("/doc")
        .produces(MediaTypes.TEXT_XML)
        .handler(exchange -> {
            ...
        });

```

Now let's consider the following `accept` request header field:

```http
accept: application/json, application/xml;q=0.9, */xml;q=0.8
```

This field basically tells the server that the client wants to receive first a `application/json` response payload, if not available a `application/xml` response payload and if not available any `*/xml` response payload.

The content negotiation algorithm is similar as the one described in the [consume routing rule](#consume-routing-rule), it is simply reversed in the sense that it is the acceptable media ranges defined in the `accept` header field that are sorted and the route producing the media type matching the media range with the highest priority is selected.

Considering previous routes, a request with previous `accept` header field is then matched by the first route. 

A request with the following `accept` header field is matched by the second route:

```http
accept: application/xml;q=0.9, */xml;q=0.8
```

The exchange is also matched by the second route with the following `accept` header field:

```http
accept: application/json;q=0.5, text/xml;q=1.0
```

If there is no route producing a media type that matches any of the acceptable media ranges, then a (406) not acceptable error is returned.

> As described before, if no route is defined with a produce routing rule, exchanges are matched regardless of the acceptable media ranges, content negotiation is then eventually delegated to the handler which becomes responsible to return an acceptable response to the client. 

###### Language routing rule

Finally, the language routing rule matches exchanges based on the acceptable languages supplied by client in the `accept-language` header field of the request as defined by [RFC 7231 Section 5.3.5][rfc-7231-5.3.5].

A HTTP client (eg. Web browser) typically sends a `accept-language` header to indicate the server which languages are acceptable for the response. The Web router determines the best matching route based on the language tags produced by the routes matching previous routing rules.

We can defines the following routes to return a particular resource in English or in French:

```java
router
    .route()
        .path("/doc")
        .language("en-US")
        .handler(exchange -> {
            ...
        });

router
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

The Web router then selects the route whose produced language tag matches the language range with the highest priority.

As for the produce routing rule, if there is no route defined with a language tag that matches any of the acceptable language ranges, then a (406) not acceptable error is returned. However, unlike the produce routing rule, a default route can be defined to handle such unmatched exchanges.

For instance, we can add the following default route to the router:

```java
router
    .route()
        .path("/doc")
        .handler(exchange -> {
            ...
        });
```

A request with the following `accept-language` header field is then matched by the default route:

```http
accept-language: it-IT
```

#### Error routing

An `ErrorRouter` is used to route an `ErrorWebExchange` to the right `ErrorWebExchangeHandler` when an exception is thrown during the normal processing of an exchange, it implements `ExchangeHandler<ErrorExchange<Throwable>>` and it is typically used as error handler in the HTTP server.

##### Error web exchange

The *web* module API extends the [server exchange API](#http-server-exchange-api) defined in the *http-server* module and defines the server `ErrorWebExchangeHandler` to handle a server `ErrorWebExchange`. These interfaces respectively extends the `ExchangeHandler` and `Exchange` interfaces which are defined in the *http-server* module. An error Web exchange handler is typically attached to one or more error Web routes defined in an `ErrorWebRouter`.

The `ErrorWebExchange` provides a response body encoder which can be used to encode error response body based on the content type specified in the response headers. The usage is exactly the same as for the Web server exchange [response body encoder](#response-body-encoder).

```java
errorRouter
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

##### Error router

An `ErrorWebRouter` routes an error exchange to an error exchange handler attached to an `ErrorWebRoute` by matching it against a combination of routing rules specified in the route. An error route can combine the following routing rules which are matched in that order: the type of the error, the media ranges and language ranges accepted by the client. For instance, the error router matches an error exchange against the error type routing rule first, then the produce routing rule... Multiples routes can then match a given exchange but only one will be retained to actually process the exchange which is the one matching the highest routing rules.

If a route doesn't define a particular routing rule, the routing rule is simply ignored and matches all exchanges. For instance, if a route doesn't define any error type routing rule, it matches error exchanges regardless of the error.

The following is an example of an error route which matches all error exchanges and as a result handles all types of error:

```java
errorRouter
    .route()
        .handler(errorExchange -> 
            errorExchange.response()
                .headers(headers -> 
                    headers
                        .status(Status.INTERNAL_SERVER_ERROR)
                        .contentType(MediaTypes.TEXT_PLAIN)
                )
                .body()
                .empty()
        );
```

> An `ErrorWebRouter` is exposed in the *web* module and wired to the *http-server* module to override the HTTP server's error handler. It defines error routes to *whitelabel* error handlers for standard `HTTPexception` as defined by [HTTP base API](#http-base-api) and producing `application/json` or `text/html` payloads. It can be configured to override these routes or defines others using an `ErrorWebRouterConfigurer` as defined in the [Web Server documentation](#web-server).

The creation, activation, deactivation or removal of routes in an error router is done in the exact same way as for the [Web router](#web-router).

In the following example, we define an error route to handle `SomeCustomException` producing `text/html` response in English for the clients that accept it:

```java
errorRouter
    .route()
        .error(SomeCustomException.class)
        .produces(MediaTypes.TEXT_HTML)
        .language("en-US")
        .handler(errorExchange -> 
            ...
        );
```

Error routes can be queried as follows:

```java
Set<ErrorWebRoute> errorRoutes = errorRouter
    .route()
        .error(SomeCustomException.class)
        .findRoutes();
```

They can be enabled, disabled or removed as follows:

```java
// Disables all SomeCustomException routes
errorRouter
    .route()
        .error(SomeCustomException.class)
        .disable();

// Enables all SomeCustomException routes
errorRouter
    .route()
        .error(SomeCustomException.class)
        .enable();

// remove all SomeCustomException routes
errorRouter
    .route()
        .error(SomeCustomException.class)
        .remove();
```

Individual error routes are enabled, disabled or removed as follows:

```java
// Disables all SomeCustomException routes producing 'application/json'
router
    .route()
        .error(SomeCustomException.class)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::disable);

// Enables all SomeCustomException routes producing 'application/json'
router
    .route()
        .error(SomeCustomException.class)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::enable);

// Removes all SomeCustomException routes producing 'application/json'
router
    .route()
        .error(SomeCustomException.class)
        .findRoutes()
        .stream()
        .filter(route -> route.getProduce().equals(MediaTypes.APPLICATION_JSON))
        .forEach(WebRoute::remove);
```

###### Error type routing rule

The error type routing rule matches error exchanges whose error is of a particular type.

For instance, in order to handle all error exchanges whose error is an instance of `SomeCustomException`, we can do:

```java
errorRouter
    .route()
        .method(SomeCustomException.class)
        .handler(exchange -> {
            ...
        });
```

###### Produce routing rule

The produce routing rule, when applied to an error route behaves exactly the same as for a [Web route](#produce-routing-rule). It allows to define error handlers that produce responses of different types based on the set of media range accepted by the client.

This is particularly useful to returned specific error responses to a particular client in a particular context. For instance, a backend application might want to receive errors in a parseable format like `application/json` whereas a Web browser might want to receive errors in a human readable format like `text/html`.

###### Language routing rule

The language routing rule, when applied to an error route behaves exactly the same as for a [Web route](#language-routing-rule). It allows to define error handlers that produce responses with different languages based on the set of language range accepted by the client fallbacking to the default route when content negotiation did not give any match.

### Static handler

The `StaticHandler` is a specific `WebExchangeHandler<WebExchange>` implementation that can be used to define routes for serving static resources resolved with the [Resource API](#resource-api).

For instance, we can create a route to serve files stored in a `web-root` directory as follows:

```java
router
    .route()
        .path("/static/{path:.*}")                                 // 1
        .handler(new StaticHandler(new FileResource("web-root/"))) // 2
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

### Web Server

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
     ║                    ,   \/   ,                                  -- 1.3.0 --                 ║
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

The Web router doesn't define any routes by default so if we hit the server, a (404) not found error is returned showing the error router in action:

```plaintext
$ curl -i -H 'accept: application/json' http://locahost:8080
HTTP/1.1 404 Not Found
content-type: application/json
content-length: 47

{"status":"404","path":"/","error":"Not Found"}
```

If you open `http://locahost:8080` in a Web browser, you should see the following (404) whitelabel error page:

<img class="shadow mb-4" src="img/404_whitelabel.png" alt="HTTP 404 whitelabel error page"/>

#### Configuration

The Web server configuration is done in the the *web* module configuration `WebConfiguration` which includes the *http-server* module configuration `HttpServerConfiguration`. As for the *http-server* module, the net service configuration can also be considered to set low level network configuration in the *boot* module. 

The following configuration can then be created in the *app_http* module:

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

The Web server can then be configured. For instance, we can enable HTTP/2 over cleartext, TLS, HTTP compression... as described in the [http-server module documentation](#http-server-1).

For instance:

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

#### Configuring the Web router

As explained before, a [Web router](#web-router) is used to route a request to the right handler based on a set of rules defined in a route. The *web* module provides a `WebRouter` bean which is wired to the *http-server* module to override the default root handler and handle all incoming requests to the server. Unlike the root handler in the *http-server* module, this bean is not overridable but it can be configured in order to define Web routes for the *app_web* module.

This can be done by defining a `WebRouterConfigurer` bean in the *app_web* module. A Web router configurer is basically a `Consumer<WebRouter>` invoked after the initialization of the Web router and more precisely after the default configuration has been applied.

Using what we've learned from the [Web routing documentation](#web-routing), routes can then be defined as follows:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.web.WebExchange;
import io.inverno.mod.web.WebRouter;
import io.inverno.mod.web.WebRouterConfigurer;

@Bean
public class App_webWebRouterConfigurer implements WebRouterConfigurer<WebExchange> {

    @Override
    public void accept(WebRouter<WebExchange> router) {
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
                );
    }
}

```

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

#### Configuring the error router

The *web* module also provides an error router bean wired to the *http-server* module to override the default error handler and handle all errors thrown when processing an exchange. As a reminder, an [error router](#error-router) is used to route an error exchange to the right error handler based on a set of rules defined in an error route.

As for the Web router, the error router can't be overridden, it is rather configured by defining an `ErrorWebRouterConfigurer` bean invoked after the initialization of the error router and more precisely after the default configuration has been applied which means default error routes remain although they can be overridden in the configurer.

The error router implemented in the *web* module provides default error routes for handling base `HttpException`, whitelabel error pages are returned in particular when a client request `text/html` responses. More generally, `Throwable` errors are also handled by default, returning a generic (500) internal server error.

Now let's assume, we have created a route which might throw a particular custom exception for which we want to return a particular response. For instance, we might have defined the following route in the `App_webWebRouterConfigurer`:

```java
@Bean
public class App_webWebRouterConfigurer implements WebRouterConfigurer<WebExchange> {

    @Override
    public void accept(WebRouter<WebExchange> router) {
        router
            ...
            .route()
                .path("custom_exception")
                .handler(exchange -> {
                    throw new SomeCustomException();
                });
    }
}
```

> Note that an exchange handler is defined to throw checked `HttpException` only which actually makes sense since a Http error is eventually what will be returned to the client. In our example, the `SomeCustomException` is then either unchecked or extends `HttpException`.

Now using what we've learned from the [error routing documentation](#error-routing), we can define an error route to handle that particular exception as follows:

```java
package io.inverno.example.app_web;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Status;
import io.inverno.mod.web.ErrorWebRouter;
import io.inverno.mod.web.ErrorWebRouterConfigurer;

@Bean
public class App_webErrorWebRouterConfigurer implements ErrorWebRouterConfigurer {

    @Override
    public void accept(ErrorWebRouter errorRouter) {
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
                );
    }
}
```

Now if we hit `http://locahost:8080/custom_exception` we should receive a custom error:

```plaintext
$ curl -i http://locahost:8080/custom_exception
HTTP/1.1 400 Bad Request
content-type: text/plain
content-length: 29

A custom exception was raised
```

#### WebJars

The Web server can also be configured to automatically serve static resources from [WebJars dependencies][webjars] using a version agnostic path: `/webjars/{webjar_module}/{path:.*}` where `{webjar_module}` corresponds to the *modularized* name of the WebJar minus `org.webjars`. For example the location of the Swagger UI WebJar would be `/webjars/swagger.ui/`.

This feature can be activated with the following configuration:

```java
package io.inverno.example.app_web;

import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App_web.Builder()
                .setApp_webConfiguration(
                        App_webConfigurationLoader.load(configuration -> configuration
                            .web(web -> web
                                .enable_webjars(true)
                            )
                        )
                    )
            ).run();
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

The Swagger UI should now be accessible at `http://locahost:8080/webjars/swagger.ui/`.

Sadly WebJars are not modular JARs, they are not even named modules which causes several issues when dependencies are specified on the module path. That's why when an application is run or packaged using [Inverno tools][inverno-tools-root], such dependencies and WebJars in particular are *modularized*. A WebJar such as `swagger-ui` is modularized into `org.webjars.swagger.ui` module which explains why it is referred by its module name: `swagger.ui` in the WebJars resource path (the `org.webjars` part is omitted since the context is known).

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

> It might also be possible to define the dependency in the module descriptor, unfortunately since WebJars modules are unnamed, they are named against the name of the JAR file which is greatly unstable and can't be trusted, so previous approach is by far the safest. If you need to create a WebJar you should make it a named module with the `Automatic-Module-Name` attribute sets to `org.webjars.{webjar_module}` in the manifest file and with resources located under `META-INF/resources/webjars/{webjar_module}/{webjar_version}/`.

Note that when the application is run with non-modular WebJars specified on the class path, they can be accessed without any particular configuration as part of the UNNAMED module using the same path notation.

#### OpenAPI specification

The Web server can also be configured to expose [OpenAPI specifications][open-api] defined in `/META-INF/inverno/web/openapi.yml` resource in application modules.

This feature can be activated with the following configuration:

```java
package io.inverno.example.app_web;

import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App_web.Builder()
                .setApp_webConfiguration(
                        App_webConfigurationLoader.load(configuration -> configuration
                            .web(web -> web
                                .enable_open_api(true)
                            )
                        )
                    )
            ).run();
    }
}
```

When the server starts, it will scan for OpenAPI specifications files `/META-INF/inverno/web/openapi.yml` in the application modules and configure the following routes:

- `/open-api` returning the list of available OpenAPI specifications in `application/json`
- `/open-api/{moduleName}` returning the OpenAPI specifications defined for the specified module name or (404) not found error if there is no OpenAPI specification defined in the module or no module with that name.

If the server is also configured to serve [WebJars](#webjars), previous routes can also be used to display OpenAPI specifications in a [Swagger UI][swagger-ui] assuming the Swagger UI WebJar dependency is present:

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

> OpenAPI specifications are usually automatically generated by the Web Inverno compiler plugin for routes defined in a [Web controller](#web-controller) but you can provide manual or generated specifications using the tool of your choice, as long as it is not conflicting with the Web compiler plugin.

### Web Controller

The [Web router](#web-router) and [Web server](#web-server) documentations describe a *programmatic* way of defining the Web routes of a Web server but the *web* module API also provides a set of annotations for defining Web routes in a more declarative way. 

A **Web controller** is a regular module bean annotated with `@WebController` which defines methods annotated with `@WebRoute` describing Web routes. These beans are scanned at compile time by the Web Inverno compiler plugin in order to generate a single `WebRouterConfigurer` bean configuring the routes in the Web router.

For instance, we can create a book resource with basic CRUD operations, to do so we must define a `Book` model in a dedicated `*.dto` package, we'll see later why this matters:

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

@Bean                                                                                          // 1
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

1. A Web controller must be a module bean because it will be wired into the generated Web router configurer and used to invoke the right handler method attached to a Web route. Besides it is anyway convenient for implementation, a repository could be wired into the `BookResource` bean for instance.
2. The `@WebController` annotation tells the Web compiler plugin to process the bean as a Web controller. The controller root path can also be specified in the annotation, if not specified it defaults to `/` which is the root path of the Web server.
3. The `@WebRoute` annotation on a method tells the Web compiler plugin to define a route whose handler invokes that method. The set of routing rules (ie. path, method, consume, produce, language) describing the route are specified in the annotation.
4. Parameters and request body are specified as method parameters annotated with `@CookieParam`, `@FormParam`, `@HeaderParam`, `@PathParam`, `@QueryParam` and `@Body` annotations.

Some other contextual objects like the underlying `WebExchange` can also be injected in the Web controller method.

Assuming we have provided proper implementations to create, update, list, get and delete a book in a data store, we can compile the module. A new module bean `io.inverno.example.app_web.WebRouterConfigurer` implementing `WebRouterConfigurer` should have been generated in `target/generated-sources/annotations`. It basically configures the routes corresponding to the Web controller's annotated methods in the Web router. This class uses the APIs described before and it is perfectly readable and debuggable and above all it eliminates the overhead of resolving Web controllers or Web routes at runtime.

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
$ curl -i http://locahost:8080/book
HTTP/1.1 200 OK
content-type: application/json
content-length: 2

[]
```
```plaintext
$ curl -i -X POST -H 'content-type: application/json' -d '{"isbn":"978-0132143011","title":"Distributed Systems: Concepts and Design","author":"George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair","pages":1080}' http://locahost:8080/book
HTTP/1.1 200 OK
content-length: 0

```
```plaintext
$ curl -i http://locahost:8080/book
HTTP/1.1 200 OK
content-type: application/json
content-length: 163

[{"isbn":"978-0132143011","title":"Distributed Systems: Concepts and Design","author":"George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair","pages":1080}]
```
```plaintext
$ curl -i http://locahost:8080/book/978-0132143011
HTTP/1.1 200 OK
content-type: application/json
content-length: 161

{"isbn":"978-0132143011","title":"Distributed Systems: Concepts and Design","author":"George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair","pages":1080}
```

> If you have carefully followed the *web* module documentation, you should have noticed that we have previously created a Web router configurer bean in the *app_web* module which is indeed conflicting with the generated Web router configurer resulting in the following self-explanatory compilation error:
> 
> ```plaintext
> [ERROR] /home/jkuhn/Devel/git/frmk/io.inverno.example.app_web/src/main/java/module-info.java:[4,1] Multiple beans matching socket io.inverno.mod.web:webRouterConfigurer were found
>  - io.inverno.example.app_web:app_webWebRouterConfigurer of type io.inverno.example.app_web.App_webWebRouterConfigurer
>  - io.inverno.example.app_web:webRouterConfigurer of type io.inverno.example.app_web.WebRouterConfigurer
>  
>  Consider specifying an explicit wiring in module io.inverno.example.app_web (eg. @io.inverno.core.annotation.Wire(beans="io.inverno.example.app_web:app_webWebRouterConfigurer", into="io.inverno.mod.web:webRouterConfigurer") )
> ```
> In order to resolve that conflict, you can remove the first router configurer or define an explicit wire in the module definition:
> 
> ```java
> @io.inverno.core.annotation.Module
> @io.inverno.core.annotation.Wire(beans="io.inverno.example.app_web:webRouterConfigurer", into="io.inverno.mod.web:webRouterConfigurer")
> module io.inverno.example.app_web {
>   ...
> }
> ```
>
> Such situation can also occur when you are composing multiple modules defining Web controller beans and thus exposing multiple Web router configurers in the module. Hopefully it is safe to resolve these conflicts by wiring the Web router configurer of the module composing the *web* module as it aggregates all Web router configurers annotated with `@WebRoutes`. Please look at [Composite Web module documentation](#composite-web-module) for further details.

It is possible to separate the API from the implementation by defining the Web controller and the Web routes in an interface implemented in a module bean. For instance,

```java
package io.inverno.example.app_web;

import java.util.Set;

import io.inverno.example.app_web.dto.Book;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;

@WebController(path = "/book")
public interface BookResource {

    @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
    void create(@Body Book book);
    
    @WebRoute(path = "/{isbn}", method = Method.PUT, consumes = MediaTypes.APPLICATION_JSON)
    void update(@PathParam String isbn, @Body Book book);
    
    @WebRoute(method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    Set<Book> list();
    
    @WebRoute(path = "/{isbn}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
    Book get(@PathParam String isbn);
    
    @WebRoute(path = "/{isbn}", method = Method.DELETE)
    void delete(@PathParam String isbn);
}
```

This provides better modularity allowing to define the API in a dedicated module which can later be used to implement various server and/or client implementations in different modules. Another advantage is that it allows to split a Web controller interface into multiple interfaces.

Generics are also supported, we can for instance create a generic `CRUD<T>` interface as follows:

```java
package io.inverno.example.app_web;

import java.util.Set;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.annotation.Body;
import io.inverno.mod.web.annotation.PathParam;
import io.inverno.mod.web.annotation.WebRoute;

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

And then create specific resources sharing the same interface:

```java
package io.inverno.example.app_web;

import java.util.Set;

import io.inverno.example.app_web.dto.Book;
import io.inverno.mod.web.annotation.WebController;

@WebController(path = "/book")
public interface BookResource extends CRUD<Book> {

    void create(Book book);
    
    void update(String id, Book book);
    
    Set<Book> list();
    
    Book get(String id);
    
    void delete(String id);
}
```

The book resource as we defined it doesn't seem very reactive, this statement is both true and untrue: the API and the Web server are fully reactive, as a result Web routes declared in the book resource Web controller are configured using a reactive API in the Web router configurer, nonetheless the methods in the Web controller are not reactive.

Luckily, we can transform previous interfaces to make them fully reactive:

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

There is one remaining thing to do to make the book resource a proper REST resource. When creating a book we must return a 201 Created HTTP code with a `location` header as defined by [RFC7231 Section 7.1.2][rfc-7231-7.1.2]. This can be done by injecting the `WebExchange` in the `create()` method:

```java
public interface CRUD<T> {

    @WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON, produces = MediaTypes.APPLICATION_JSON)
    Mono<Void> create(@Body Mono<T> resource, WebExchange exchange);
    ...
}
```

We can then do the following in the book resource implementation class:

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
    public Mono<Void> create(Mono<Book> book, WebExchange exchange) {
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

#### Web route

So far, we have described a concrete Web controller use case which should already give a good idea of what can be done with the *web* module. Now, let's examine in details how a Web route is declared in a Web controller.

A Web route or HTTP endpoint or REST endpoint... in short an HTTP request/response exchange is essentially defined by:

- an input, basically an HTTP request characterized by the following components: path, method, query parameters, headers, cookies, path parameters, request body
- a normal output, basically a successful HTTP response and more especially: status (2xx or 3xx), headers, response body
- a set of error outputs, basically unsuccessful HTTP responses and more especially: status (4xx or 5xx), headers, response body

Web routes are defined as methods in a Web controller which match this definition: the Web route input is defined in the parameters of the method, the Web route normal output is defined by the return type of the method and finally the exceptions thrown by the method define the Web route error outputs.

It then remains to bind the Web route semantic to the method, this is done within various annotations on the method and its parameters.

##### Routing rules

Routing rules, as defined in the [Web router documentation](#web-router), are specified in a single `@WebRoute` annotation on a Web controller method. It allows to define the paths, the methods, the consumed media ranges, the produced media types and the produced languages of the Web routes that route a matching request to the handler implemented in the method.

For instance, we can define multiple paths and/or multiple produced media types in order to expose a resource at different locations in different formats:

```java
@WebRoute( path = { "/book/current", "/book/v1" }, produces = { MediaTypes.APPLICATION_JSON, MediaTypes.APPLICATION_XML } )
Flux<T> list();
```

> Note that this exactly corresponds to the *web* module API

The `matchTrailingSlash` parameter can be used to indicate that the defined path should be matched taking the trailing slash into account or not.

##### Parameter bindings

As stated above, a `@WebRoute` annotated method must be bound to the Web exchange. In particular, method parameters are bound to the various elements of the request using `@*Param` annotations defined in the *web* module API.

Such parameters can be of any type, as long as the parameter converter plugged into the *web* module can convert it, otherwise a `ConverterException` is thrown. The default parameter converter provided in the *boot* module is able to convert primitive and common types including arrays and collections. Please refer to the [HTTP server documentation](#extend-http-services) to learn how to extend the parameter converter to convert custom types.

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

###### Query parameter

Query parameters are declared using the `@QueryParam` annotation as follows:

```java
@WebRoute( path = { "/book/byIsbn" }, produces = { MediaTypes.APPLICATION_JSON } )
Flux<T> getBooksByIsbn(@QueryParam String[] isbns);
```

Note that the name of the method parameter actually specifies the name of the query parameter.

> This contrasts with other RESTful API, such as JAX-RS, which requires to specify the parameter name, again, in the annotation. Since the Web Inverno compiler plugin works at compile time, it has access to actual method parameter names defined in the source.

###### Path parameter

Path parameters are declared using the `@PathParam` annotation as follows:

```java
@WebRoute(path = "/{id}", method = Method.GET, produces = MediaTypes.APPLICATION_JSON)
Mono<T> get(@PathParam String id);
```

Note that the name of the method parameter must match the name of the path parameter of the route path defined in the `@WebRoute` annotation.

###### Cookie parameter

It is possible to bind cookie values as well using the `@cookieParam` annotation as follows:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
Mono<Void> create(@CookieParam String book_store, @Body Mono<T> book, WebExchange exchange);
```

In previous example, the route must be queried with a `book_store` cookie:

```plaintext
$ curl -i -X POST -H 'cookie: book_store=store1' -H 'content-type: application/json' -d '...' http://locahost:8080/book
...
```

###### Header parameter

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

###### Form parameter

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

Form parameters are sent in a request body using `application/x-www-form-urlencoded` format as defined by [living standard][form-urlencoded]. They can be sent using a HTML form submitted to the server resulting in the following request body:

```http
forename=Leslie,middlename=B.,surname=Lamport,birthdate=19410207,nationality=US
```

Previous route can then be queried as follows:

```plaintext
$ curl -i -X POST -H 'content-type:application/x-www-form-urlencoded' -d 'forename=Leslie,middlename=B.,surname=Lamport,birthdate=19410207,nationality=US' http://locahost:8080/author
```

Form parameters results from the parsing of the request body and as such, `@FormParam` annotations can't be used together with `@Body` on route method parameters.

##### Contextual parameters

A contextual parameter is directly related to the context into which an exchange is processed in the route method, it can be injected in the route method by specifying a method parameter of a supported contextual parameter type.

###### Exchange

Currently the only supported contextual parameter is the exchange which can be provided by specifying a method parameter of a type assignable from `WebExchange`.

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
Mono<Void> create(@Body Mono<T> book, WebExchange exchange);
```

> The exchange gives a full access to the underlying request and response. Although it allows to manipulate the request and response bodies, this might conflict with the generated Web route and as a result the exchange should only be used to specify a specific response status, response cookies or headers...

##### Request body

The request body can be bound to a route method parameter using the `@Body` annotation. Request body is automatically converted based on the media type declared in the `content-type` header field of the request as described in the [Web server exchange documentation](#request-body-decoder). The body parameter method can then be of any type as long as there is a media type converter for the media type specified in the request that can convert it.

In the following example, the request body is bound to parameter `book` of type `Book`, it is then converted from `application/json` into a `Book` instance:

```java
@WebRoute(method = Method.POST, consumes = MediaTypes.APPLICATION_JSON)
void create(@Body Book book);
```

Unlike parameters, the request body can be provided in a non-blocking/reactive way, the previous example can then be rewritten using a `Mono<T>`, a `Flux<T>` or more broadly a `Publisher<T>` as body parameter type as follows:

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

The `@Body` annotation can't be used together with the `@FormParam` annotation on route method parameters because the request body can only be consumed once.

###### Multipart form data

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

> It is not possible to bind particular parts to a route method parameter. This design choice has been motivated by performance and resource consumption considerations. Indeed, this would require to consume and store the entire request body in memory before invoking the method. As a result, multipart data must still be handled *manually* using the *web* module API.

##### Response body

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
Stream<Book> list();
```

In the preceding example, as soon as a book is retrieved from a data store it can be sent to the client which can then process responses as soon as possible reducing the latency and resource consumption on both client and server. The response content type is `application/x-ndjson`, so each book is encoded in JSON before a newline delimiter to let the client detects partial responses as defined by [the ndjon format][ndjson].

###### Server-sent events

Server-sent events can be streamed in the response body when declared together with a server-sent event factory route method parameter. A server-sent event factory can be bound to a route method parameter using the `@SseEventFactory` annotation.

In the following example, we declare a basic server-sent events Web route producing events with a `String` message:

```java
@WebRoute(path = "/event", method = Method.GET)
Publisher<WebResponseBody.SseEncoder.Event<String>> getBookEvents(@SseEventFactory WebResponseBody.SseEncoder.EventFactory<String> events);
```

Server-sent event return type can be any of `Mono<WebResponseBody.SseEncoder.Event<T>>` if only one event is expected, `Flux<WebResponseBody.SseEncoder.Event<T>>` if multiple events are expected and more broadly `Publisher<WebResponseBody.SseEncoder.Event<T>>`.

By default, the media type of a server-sent event message is `text/plain` but it can be encoded using a specific media type converter as well by specifying a media type in the `@SseEventFactory` annotation. 

We can rewrite previous [SSE example](#server-sent-events) with a message of a custom type as follows:

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

#### Composite Web module

The Web Inverno compiler plugin generates a single Web router configurer bean aggregating all route definitions specified in Web controllers beans in the module. When the module composes the *web* module, this bean is then plugged in the *web* module to configure the Web server router. 

Now if the module doesn't compose the *web* module, the Web router configurer bean is simply exposed by the module waiting for the module to be composed within other modules until a top module eventually composes the *web* module. 

This raises two issues: 

- First if multiple modules exposing web router configurers are composed together with the *web* module, we'll end up with dependency injection conflicts since multiple web router configurer beans can be wired to the *web* module. Selecting one of them with a `@Wire` annotation doesn't really solve the problem since we expect all routes to be configured in the Web server router.
- Then if such module is composed in another module defining other Web controllers, we still need to expose one Web router configurer providing all route definitions to a top module composing the *web* module.

In order to solve these issues, the Web Inverno compiler plugin aggregates all Web router configurer beans annotated with `@WebRoutes` into the generated Web router configurer of the module so that it can be used to configure all defined routes. This includes Web router configurer exposed in component modules as well as user defined Web router configurer beans within the module.

A generated Web router configurer is always annotated with a `@WebRoutes` annotation specifying the Web routes it configures. For instance, the Web router configurer generated for the module defining the book Web controller looks like:

```java
@WebRoutes({
    @WebRoute(path = { "/book/{id}" }, method = { Method.GET }, produces = { "application/json" }),
    @WebRoute(path = { "/book" }, method = { Method.POST }, consumes = { "application/json" }),
    @WebRoute(path = { "/book/{id}" }, method = { Method.PUT }, consumes = { "application/json" }),
    @WebRoute(path = { "/book" }, method = { Method.GET }, produces = { "application/json" }),
    @WebRoute(path = { "/book/{id}" }, method = { Method.DELETE })
})
@Bean
public final class WebRouterConfigurer implements io.inverno.mod.web.WebRouterConfigurer<WebExchange> {
    ...
}
```

This information is used by the compiler plugin to statically check that there is no conflicting routes when generating the Web router configurer.

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

The first thing to notice is that the *web* module is excluded from *admin* and *book* modules since we don't want to start a Web server in these modules, we only need the Web API to define Web controllers and generate Web router configurer beans. As a consequence, the *boot* module which provides converters and net service required to create and start the *web* module is also not required but the `io.inverno.core` module is still required. Finally we must export packages containing the generated module classes to the *app* module so it can compose them.

The *admin* and *book* modules should compile just fine resulting in two Web router configurer beans being generated and exposed in each module. But the compilation of *app* module should raise some dependency injection errors since multiple Web router configurer beans exist whereas only one can be wired to the *web* module. There are actually three Web configurer beans, how so? There are those exposed by the *admin* and *book* modules and one Web router configurer bean generated in the *app* module and aggregating the previous two. In order to solve the conflict, we should then define the following explicit wire in the *app* module:

```java
@io.inverno.core.annotation.Module
@io.inverno.core.annotation.Wire(beans="io.inverno.example.web_modular.app:webRouterConfigurer", into="io.inverno.mod.web:webRouterConfigurer")
module io.inverno.example.web_modular.app {
    ...
}
```

> One could rightfully argue that this explicit wiring is useless and cumbersome, but it is consistent with the IoC/DI core framework principles. Keeping things simple and explicit limits possible side effects induced by the fact that what's happening with *automatic* conflict resolution is often specific and might not be obvious. This is all the more true when such behavior is manually overridden.

The same principles applies if multiple modules like *admin* or *book* are cascaded into one another: the Web router configurer beans at a given level are aggregated in the Web router configurer bean in the next level.

#### Automatic OpenAPI specifications

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

The Web compiler plugin generates, per module, one Open API specification and one Web router configurer bean aggregating all routes from all Web controllers. As a result the general API documentation corresponds to the general documentation of the module and defined in the module descriptor JavaDoc comment.

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

These specifications can also be exposed in the Web server as described in the [Web server documentation](#web-server).

If we build and run the [modular book application](#composite-web-module) and access `http://locahost:8080/open-api` in a Web browser we should see a Swagger UI loaded with the Open API specifications of the *admin* and *book* modules:

<img class="shadow mb-4" src="img/swaggerUI_root.png" alt="General Swagger UI"/>

It is also possible to target a single specification by specifying the module name in the URI, for instance `http://locahost:8080/open-api/io.inverno.example.web_modular.book`:

<img class="shadow mb-4" src="img/swaggerUI_module.png" alt="Module Swagger UI"/>

Finally, Open API specifications formatted in [YAML][yaml] can be retrieved as follows:

```plaintext
$ curl http://locahost:8080/open-api/io.inverno.example.web_modular.admin

openapi: 3.0.3
info:
    title: 'io.inverno.example.web_modular.admin'
    version: ''
...
```

## Reactive Template

The Inverno *reactive template* module provides a template engine for efficient reactive data rendering.

Data are basically rendered by templates which are regrouped in template sets and applied based on the type of data to render. A template set is statically typed and generated by an Inverno compiler plugin which compiles `.irt` template set source files along with the Java sources of a module.

The template sets classes thus obtained support reactive rendering, data are rendered as a flow of events for efficient usage of resources. For instance, the complete set of data doesn't have to be available or loaded into memory, the rendering being reactive the output can be generated incrementally by processing each piece of data individually one after the other when they become available. Since the rendering process never blocks it is also possible to lazily load data when/if they need to be rendered.

The syntax of `.irt` template set is inspired from functional language such as [XSLT][xslt] and [Erlang][erlang] which are particularly suited for reactive rendering. Since a template is a generated Java class, the Java language is also widely used in a template source file, especially for the dynamic parts of a template.

In terms of raw performance, Inverno templates processing is faster than most Java template engines by an order of magnitude and with lower memory usage. The following [benchmark project][template-benchmark] compares performances of various template engines rendering a list of stock items into an HTML document as a String. 

<img class="shadow mb-4" src="img/template_benchmarks.png" alt="Template Benchmarks"/>

> Please keep in mind that outcomes might be different considering different scenarios, especially reactive rendering which might appear slower but addresses different concerns such as stream processing and optimized usage of resources.

In order to use the Inverno *reactive template* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.irt;
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
            <artifactId>inverno-irt</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
...
compile 'io.inverno.mod:inverno-irt:1.2.0'
...
```

### Creates an `.irt` template

A template can be created along with other Java source files in the source directory of an Inverno module. At compile time, the Inverno reactive template compiler plugin will scan the module source folder for `.irt` files and compiles them to generate template set classes that can be used in your module to render data.

The following `Simple.irt` template set is a simple example containing one template that renders a `Message` object as String:

```plaintext
package io.inverno.example.app_irt.templates;

import io.inverno.example.app_irt.model.Message;

option modes = {"STRING"};
option charset = "utf-8";

(Message message) -> {The message is: {@message.message}}
```

```java
package io.inverno.example.app_irt.model;

public class Message {

	private final String message;
	
	private final boolean important;
	
	public Message(String message, boolean important) {
		this.message = message;
		this.important = important;
	}

	public String getMessage() {
		return message;
	}
	
	public boolean isImportant() {
		return important;
	}
}
```

As for any Java source file, the preceding template source must be created in the same package as the one it declares in a module source folder. The name of the template corresponds to the name of the file.

After compiling the module, a new Java class `Simple.java` should have been created in the generated source folder in package `io.inverno.example.app_irt.templates`.

A `Message` object can then be rendered as follows:

```java
CompletableFuture<String> rendered = Simple.string().render(new Message("Hello, world!"));
System.out.println(rendered.get()); // The message is: Hello, world!
```

### `.irt` syntax

#### Package and imports

An `.irt` template always starts with the declaration of the Java package containing the template, followed by the list of imported Java types or static methods used within the template. This is exactly the same as any Java source file.

```plaintext
package io.inverno.example.app_irt.templates;

import io.inverno.example.app_irt.model.Message;
...
```

#### Includes

Then you can specify external template sets to include in the template set using the `include` keyword. This allows to include templates from an external template set in a template set using the same precedence. For instance, in the following example, template set `io.inverno.example.app_irt.templates.Misc` is included in the template set which means that its templates can be applied in the including template.

```plaintext
include io.inverno.example.app_irt.templates.Misc;
```

> Note that this can lead to conflicts when two included templates defines a template using the same signature (same name and same input parameters), such conflict can be resolved by explicitly overriding the conflicting template in the including template set.

#### Options

Rendering options are specified after that using the `option` keyword. You can for instance declare the charset to use for rendering which defaults to `utf-8` if not specified: 

```plaintext
option charset = "utf-8";
```

or the template rendering modes supported by the generated template set. There are five template rendering modes, you can choose to specify one or more modes depending on your needs:

- **STRING** to expose methods to render data in a `String`, this is the default behavior
- **BYTEBUF** to expose methods to render data in a `ByteBuf`
- **STREAM** to expose methods to render data in an `OutputStream`
- **PUBLISHER_STRING** to expose methods to render data in a `Publisher<String>`
- **PUBLISHER_BYTEBUF** to expose methods to render data in a `Publisher<ByteBuf>`

The last two modes are particularly suitable for reactive rendering.

```plaintext
option modes = {"STRING", "STREAM", "PUBLISHER_STRING"};
```

#### Templates

Templates are specified last. A template is a function that defines how a particular input must be rendered, a template can have a name in which case it is referred as a named template. In a template set, there can't be two template with the same signature (ie. defining the same input parameters) unless they have different names.

A template is declared as follows:

```plaintext
(Message message) -> {...}
```

A named template is declared as follows:

```plaintext
name(Message message) -> {...}
```

A template can be defined with zero or more parameters. No parameter templates can be useful to create static templates such as headers or footers, they are usually named:

```plaintext
header() -> {...}
```

The body of a template is a combination of static content and statements which define how the template input should be rendered. Template statements are specified within braces `{...}` which must be escaped within static content using `\`.

A template can also be specified without a body in order to create aliases or resolve conflicts.

For instance the following template defines alias `apple` for template `fruit` (assuming `Apple` is an instance of `Fruit`):

```plaintext
apple(Apple fruit) -> this::fruit

fruit(Fruit fruit) -> {...}
```

And the following template resolves a conflict induced by the inclusion of template set `Include1` and `Include2` which both defines template `conflicting` with the same input parameters:

```plaintext
...
include io.inverno.example.app_irt.templates.Include1;
include io.inverno.example.app_irt.templates.Include2;
...

conflicting(String input) -> Include1
```

#### Static content

Static contents are specified directly in the template body and are rendered as is:

```plaintext
(...) -> {
This is a static content, braces: \{ and \} must be escaped.
}
```

#### Comment

The syntax supports two kinds of comments which can be either outside or inside the body of a template.

Outside the body of a template, comments are regular Java comments:

```plaintext
/*
 * This a comment to explain that the following import is commented
 */
// import java.util.List; 
```

Inside the body of a template, comments are statements starting with `{%` and ending with `}`:

```plaintext
(Message message) -> {
Hello {% this is a comment} World.
}
```

#### Value of

A value can be rendered directly in a synchronous way within a statement starting with `{@` and ending with `}` as follows:

```plaintext
(Message message) -> {
The message is: {@message.message}
}
```

In the preceding example, we used a syntactic sugar to navigate into the message object hierarchy and access the `message` properties but it is also possible to evaluate a raw Java expression specified between `(` and `)` to get the same result:

```plaintext
(Message message) -> {
The message is: {@(message.getMessage())}
}
```

It is then possible to evaluate any Java expression:

```plaintext
(Message message) -> {
The message is: {@(5+8)}
}
```

> Note that this can be dangerous when you can't trust the source of a template set.

#### If

An if statement can be used to render different contents based on one or more conditions. An if statement starts with `{@if` and ends with `}`, it contains one or more branches separated by `;` defining a condition and a corresponding body, a default branch with an empty condition can be specified last. Each condition is specified as a raw Java if expression between `(` and `)`:

```plaintext
(Message message, String lang) -> {
	{@if
		(lang.equals("fr")) -> {
			Le message est: {@message.message}
		};
		(lang.equals("de")) -> {
			Die Nachricht ist: {@message.message}
		};
		() -> {
			The message is: {@message.message}
		}
	}
}
```

#### Apply template

Templates can be applied on data using an apply template statement starting with `{` and ending with `}`. The template to apply is selected among the ones available in the template set based on the type of data to render following Java's rules for function overloading. 

As for the value of statement, it is possible to use a syntactic sugar notation or a raw Java expression between `(` and `)` to select the data on which a template should be applied. A template set provides a default template for object which simply renders the `toString()` representation of the input. Considering previous examples, the content of a message object can then also be rendered as follows:

```plaintext
(Message message) -> {
The message is: {message.message}
}
```

Unlike the value of statement which renders data synchronously, applying a template can be an asynchronous operation depending on the type of the data to render. Indeed when the data to render is an an array, an `Iterable`, a `Stream` or a `Publisher`, the template is applied on each element and in the case of the `Publisher` this is a reactive, non-blocking and therefore asynchronous operation.

For instance, a list of messages can be rendered synchronously as follows:

```plaintext
(List<Message> messages) -> {
Messages are: 
{messages}
}

(Message message) -> {{@message.message}
}
```

resulting in:

```plaintext
Messages are:
message 1
message 2
message 3
message 4
message 5
...
```

Now if we consider a `Publisher`, a message is rendered to the output when it is emitted by the publisher following reactive principles.

As you can see the apply template statement is extremely powerful, it is used to render data based on their types which facilitates composition but it can also be used as a for loop statement to render a list of elements.

By default, an apply template statement will select the unnamed template within the template set matching the type of the data to render, but it is also possible to select a named templates as follows:

```plaintext
(List<Message> messages) -> {
Messages are: 
{messages;bullet}
}

(Message message) -> {{@message.message}
}

bullet(Message message) -> {* {@message.message}
}
```

resulting in:

```plaintext
Messages are:
* message 1
* message 2
* message 3
* message 4
* message 5
...
```

Extra parameters can also be passed to a template in which case we have to explicitly specify the inputs:

```plaintext
(List<Message> messages) -> {
Messages are: 
{messages; message -> bullet(message, "-")}
}

bullet(Message message, String marker) -> {{@marker} {@message.message}
}
```

resulting in:

```plaintext
Messages are:
- message 1
- message 2
- message 3
- message 4
- message 5
...
```

It is also possible to specify guard expressions as raw Java expressions and choose to apply different templates based on certain conditions. For instance, let's say we want to render important messages in a specific way, we can do as follows:

```plaintext
(List<Message> messages) -> {
Messages are: 
{messages;(message) -> important(message) when (message.isImportant());(message)}
}

(Message message) -> {{@message.message}
}

important(Message message) -> {**{@message.message}**
}

```

In the previous example, the `important` template is applied when a message is important and the unnamed template is applied otherwise. Assuming message 3 is important, this will result in::

```plaintext
Messages are:
- message 1
- message 2
- **message 3**
- message 4
- message 5
...
```

The index of an item in a list is made available when selecting the target template. For instance, a numbered list of messages can be rendered as follows:

```plaintext
(List<Message> messages) -> {
Messages are: 
{messages;(index, message) -> (index, message)}
}

(long index, Message message) -> {{@index}. {@message.message}
}
```

resulting in:

```plaintext
Messages are:
0. message 1
1. message 2
2. message 3
3. message 4
4. message 5
...
```

A no-arg named template can be applied by omitting the data part in the statement:

```plaintext
(Message message) -> {
	{;header}
	The message is: {@message.message}
	{;footer}
}

header() -> {==== HEADER ====
}

footer() -> {==== FOOTER ====
}
```

resulting in:

```plaintext
==== HEADER ====
The message is: {@message.message}
==== FOOTER ====
```

### Pipes

A pipe can be used to transform data before they are rendered or before a template is applied, as a result they can be specified in value of and apply template statements. In practice, a pipe is a simple function that accepts a data and transform it into another data. Pipes can be chained to sequentially apply multiple transformations.

A pipe can be specified as a lambda expression and applied using a `|` in a value of or apply template statement as follows:

```plaintext
(Message message) -> {
The message is: {@message.message|((String content) -> content.toUpperCase())}
}
```

Lambdas are handy when there's a need for very specific pipes, however the recommended way to create pipes is to define them in Java as static methods returning the `Pipe` implementation in order to keep the template readable. Above pipe can be defined in a Java class as follows:

```java
package io.inverno.example.app_irt.pipes; 

import io.inverno.mod.irt.Pipe;

public final class SamplePipes {

	public static Pipe<String, String> uppercase() {
		return String::toUpperCase;
	}
}
```

We can then statically import that class in the template set and simplify above example:


```plaintext
import static io.inverno.example.app_irt.pipes.SamplePipes.*;

(Message message) -> {
The message is: {@message.message|uppercase}
}
```

Several built-in pipes are provided in the module in the `Pipes`, `SteamPipes` and `PublisherPipes` classes. The `Pipes` class provides pipes used to tranform simple data object before rendering such as strings, dates and numbers. The `StreamPipes` and `PublisherPipes` provide pipes used to transformed streams and publishers typically in an apply template statement.

For instance the following example sort a list of items and map them to their datetime before applying templates:

```plaintext
import static io.inverno.mod.irt.Pipes.*;
import static io.inverno.mod.irt.PublisherPipes.*;

import java.time.format.DateTimeFormatter;

(Publisher<Item> items) -> {
	{items|sort|map(Item::getDateTime)}
}

(ZonedDateTime datetime) -> {
	{@datetime|dateTime(DateTimeFormatter.ISO_DATE_TIME)}
}
```

### Modes

Template set classes are generated by the Inverno reactive template compiler plugin. Depending on the modes specified in the template set options, the resulting class will expose different `render()` methods with different outputs.

The **STRING** mode is the default resulting in the generation of `render()` methods that return a `CompletableFuture<String>` which completes once the input data has been fully rendered into the resulting String. For instance, assuming we have created a `Simple.irt` template set containing a template to render `Message` object, we can render a `Message` to a String as follows:

```java
String result = Simple.string().render(new Message("some important message", true)).get();
```

The rendering process start as soon as the `render()` method is invoked, the `get()` operation on the resulting `CompletableFuture` waits until the message has been fully rendered. In this particular example, the whole process is synchronous since the input data is available from the start but keep in mind that this might not always be the case especially when `Publisher` objects are rendered in the process.

The **BYTEBUF** has a similar behavior except that data are rendered in a **ByteBuf**:

```java
ByteBuf result = Simple.bytebuf().render(new Message("some important message", true)).get();
```

It is possible to provide the `ByteBuf` instance into which data should be rendered by defining a factory:

```java
ByteBuf result = Simple.bytebuf(() -> Unpooled.unreleasableBuffer(Unpooled.buffer())).render(new Message("some important message", true)).get();
```

The **STREAM** mode is used to render data in an `OutputStream`:

```java
ByteArrayOutputStream result = Simple.stream(() -> new ByteArrayOutputStream()).render(new Message("some important message", true)).get();
```

Finally the **PUBLISHER_STRING** and **PUBLISHER_BYTEBUF** modes are used to generate fully reactive rendering methods which return `Publisher<String>` and `Publisher<ByteBuf>` respectively. Unlike for previous modes, the rendering process starts when a subscription is made on the returned `Publisher` which can emits partial rendering result whenever a partial data is rendered.

```java
String result = Flux.from(Simple.publisherString().render(new Message("some important message", true))).collect(Collectors.joining()).block();
```

If you consider small data set and require very high performance, you should prefer non-reactive modes. If your concern is more about resources, considering a large amount of data that you do not want to load into memory at once, you should prefer reactive modes with a slight decrease in performance.

## SQL Client

The Inverno SQL client module specifies a reactive API for executing SQL statement on a RDBMS.

This module only exposes the API and a proper implementation module must be considered to obtain `SqlClient` instances.

In order to use the Inverno *SQL client* module, we need to declare a dependency to the API and at least one implementation in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.sql; // this is actually optional since implementations should already define a transitive dependency
    requires io.inverno.mod.sql.vertx; // Vert.x implementation
    ...
}
```

And also declare these dependencies in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-sql</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-sql-vertx</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
...
compile 'io.inverno.mod:inverno-sql:1.2.0'
compile 'io.inverno.mod:inverno-sql-vertx:1.2.0'
...
```

### SQL client API

The Sql client API defines the `SqlClient` interface which provides reactive methods to execute SQL statements on a RDBMS.

#### Query and update

The `SqlClient` extends the `SqlOperations` interface which defines methods for common RDBMS operations such as query or update in addition to the more general statements and prepared statements.

We can query a database as follows:

```java
SqlClient client = ...

Flux<Person> persons = Flux.from(
    client.query("SELECT * FROM person")
)
.map(row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate"))); // Map the resulting rows

persons.subscribe(...); // The query is executed on subscribe following reactive principles
```

Prepared queries are also supported:

```java
Publisher<Row> results = client.query("SELECT * FROM person WHERE name = $1", "John");
```

A row mapping function can be specified directly in the query as well

```java
Publisher<Person> results = client.query(
    "SELECT * FROM person WHERE name = $1", 
    row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate")), 
    "Smith"
);
```

A single result can also be queried as follows:

```java
Mono<Person> person = client.queryForObject( // only consider the first row in the results
    "SELECT * FROM person WHERE name = $1", 
    row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate")),
    "Smith"
);
```

> The two previous examples are actually optimizations of the first one which enable implementations to optimize the query, resulting in faster execution.

The database can be updated as follows:

```java
client.update(
    "UPDATE person SET birthdate = $1 WHERE id = $2", 
    LocalDate.of(1970, 1, 1), 123
);
```

It can also be updated in a batch as follows:

```java
client.batchUpdate(
    "UPDATE person SET birthdate = $1 WHERE id = $2", 
    List.of(
        new Object[]{ LocalDate.of(1970, 1, 1), 123 },
        new Object[]{ LocalDate.of(1980, 1, 1), 456 },
        new Object[]{ LocalDate.of(1990, 1, 1), 789 }
    )
);
```

> Note that all these operations use prepared statements which protect against SQL injection attacks.

#### Statements

The `SqlClient` also defines methods to create more general statements and prepared statements.

A static statement can be created and executed as follows:

```java
SqlClient client = ...

Publisher<SqlResult> results = client.statement("SELECT * FROM person").execute();

results.subscribe(...); // The statement is executed on subscribe following reactive principles
```

The execution of a statement returns `SqlResult` for each SQL operations in the statement in a publisher.

The `SqlResult` exposes row metadata and depending on the operation type either the number of rows affected by the operation (`UPDATE` or `DELETE`) or the resulting rows (`SELECT`).

Following preceding example:

```java
Flux<Person> persons = Flux.from(client.statement("SELECT * FROM person").execute())
    .single() // Make sure we have only one result
    .flatMapMany(SqlResult::rows)
    .map(row -> new Person(row.getString("firstname"), row.getString("name"), row.getLocalDate("birthdate")))

persons.subscribe(...);
```

Queries can also be fluently appended to a statement as follows:

```java
Publisher<SqlResult> results = client
    .statement("SELECT * FROM person")
    .and("SELECT * FROM city")
    .and("SELECT * FROM country")
    .execute();
```

Unlike prepared statements, static statements are not pre-compiled and do not protect against SQL injection attacks which is why prepared statements should be preferred when there is a need for performance, dynamic or user provided queries.

A prepared statement can be created and executed as follows:

```java
SqlClient client = ...

Publisher<SqlResult> results = client.preparedStatement("SELECT * FROM person WHERE name = $1")
    .bind("Smith") // bind the query argument
    .execute();

results.subscribe(...); // The statement is executed on subscribe following reactive principles
```

As for a static statement, a prepared statement returns `SqlResult` for each SQL operations in the statement, however it is not possible to specify multiple operation in a prepared statement. But it is possible to transform it into a batch which will result in multiple operations and therefore multiple `SqlResult`.

In order to create a batch statement, we must bind multiple query arguments as follows:

```java
Publisher<SqlResult> results = client.preparedStatement("SELECT * FROM person WHERE name = $1")
    .bind("Smith")         // first query
    .and().bind("Cooper")  // second query
    .and().bind("Johnson") // third query
    .execute();

long resultCount = Flux.from(results).count().block(); // returns 3 since we have created a batch statement with three queries
```

#### Transactions

The API provides two ways to execute statement in a transaction which can be managed explicitly or implicitly.

We can choose to manage transaction explicitly by obtaining a `TransactionalSqlOperations` which expose `commit()` and `rollback()` methods that we must invoke explicitly to close the transaction:

In the following example we perform a common `SELECT/UPDATE` operation within a transaction:

```java
SqlClient client = ...

final float debit = 42.00f;
final int accountId = 1;

Mono<Integer> affectedRows = Mono.usingWhen(
    client.transaction(), 
    tops -> tops
        .queryForObject("SELECT balance FROM account WHERE id = $1", row -> row.getFloat(0), accountId)
        .flatMap(balance -> ops
            .update("UPDATE account SET balance = $1 WHERE id = $2", balance - debit, accountId)
            .doOnNext(rowCount -> {
                if(balance - debit < 0) {
                    throw new IllegalStateException();
                }
            })
        )
    ,
    tops -> {                                // Complete
        // extra processing before commit
        // ...
        
        return tops.commit();
    },
    (tops, ex) -> {                          // Error
        // extra processing before roll back
        // ...
        
        return tops.rollback();
    }, 
    tops -> {                                // Cancel
        // extra processing before commit
        // ...
        
        return tops.rollback();
    }
);

affectedRows.subscribe(...); // on subscribe, a transaction is created, the closure method is invoked and the transaction is explicitly committed or rolled back when the publisher terminates.
```

The following example does the same but with implicit transaction management:

```java
SqlClient client = ...

final float debit = 42.00f;
final int accountId = 1;

Publisher<Integer> affectedRows = client.transaction(ops -> ops
    .queryForObject("SELECT balance FROM account WHERE id = $1", row -> row.getFloat(0), accountId)
    .flatMap(balance -> ops
        .update("UPDATE account SET balance = $1 WHERE id = $2", balance - debit, accountId)
        .doOnNext(rowCount -> {
            if(balance - debit < 0) {
                throw new IllegalStateException();
            }
        })
    )
);

affectedRows.subscribe(...); // same as before but the transaction is implicitly committed or rolled back
```

> Note that transactions might not be supported by all implementations, for instance the Vert.x pooled client implementation does not support transactions and an `UnsupportedOperationException` will be thrown if you try to create a transaction.

#### Connections

Some `SqlClient` implementations backed by a connection pool for instance can be used to execute multiple SQL statements on a single connection released once the resulting publisher terminates (either closed or returned to the pool).

For instance we can execute multiple statements on a single connection as follows:

```java
SqlClient client = ...

final int postId = 1;

client.connection(ops -> ops
    .queryForObject("SELECT likes FROM posts WHERE id = $1", row -> row.getInteger(0), postId)
    .flatMap(likes -> ops.update("UPDATE posts SET likes = $1 WHERE id = $2", likes + 1, postId))
);
```

### Vert.x SQL Client implementation

The Inverno Vert.x SQL client module is an implementation of the SQL client API on top of the [Vert.x Reactive SQL client][vertx-sql-client].

It provides multiple `SqlClient` implementations that wraps Vert.x SQL pooled client, pool or connection and exposes a `SqlCLient` bean created from the module's configuration and backed by a Vert.x pool. It can be used to execute SQL statements in an application.

In order to use the Inverno *Vert.x SQL client* module, we need to declare a dependency in the module descriptor:

```java
module io.inverno.example.app {
    ...
    requires io.inverno.mod.sql.vertx;
    ...
}
```

And also declare this dependency as well as a dependency to the Vert.x implementation corresponding to the RDBMS we are targeting in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-sql-vertx</artifactId>
        </dependency>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-pg-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
...
compile 'io.inverno.mod:inverno-sql-vertx:1.2.0'
compile 'io.vertx:vertx-pg-client:4.1.2'
...
```

#### Configuration

The `VertxSqlClientConfiguration` is used to create and configure the SQL client bean exposed by the module.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

#### Sql Client bean

The module exposes a `SqlClient` bean which is backed by a Vert.x pool. It is created using the configuration and especially the `db_uri` property whose scheme indicates the RDBMS system and therefore the Vert.x pool implementation to use.

For instance, the following configuration can be used to connect to a PostgreSQL database:

```plaintext
db_uri="postgres://user:password@localhost:5432/sample_db"
```

> If you want to connect to a particular RDBMS, don't forget to add a dependency to the corresponding Vert.x SQL client implementation. Vert.x currently supports DB2, MSSQL, MySQL and PostgreSQL.

The pool can be configured as well: 

```plaintext
pool_maxSize=20
```

Please refer to the [Vert.x database documentation][vertx-database-doc] to get the options supported for each RDBMS implementations.

The Vert.x SQL client requires a `Vertx` instance which is provided in the Inverno application reactor when using a `VertxReactor`, otherwise a dedicated `Vertx` instance is created. In any case, this instance can be overridden by providing a custom one to the module.

#### Vert.x wrappers

Depending on our needs, we can also choose to create a custom `SqlClient` using one the Vert.x SQL client wrappers provided by the module.

The `ConnectionSqlClient` wraps a Vert.x SQL connection, you can use to transform a single connection obtained via a Vert.x connection factory into a reactive `SqlClient`.

The `PooledClientSqlClient` wraps a Vert.x pooled SQL client that supports pipelining of queries on a single configuration for optimized performances. This implementation doesn't support transactions.

```java
SqlClient client = new PooledClientSqlClient(PgPool.client(...));
```

Finally, the `PoolSqlClient` wraps a Vert.x SQL pool. This is a common implementation supporting transactions and result streaming, it is used to create the module's SQL client bean.

```java
SqlClient client = new PoolSqlClient(PgPool.pool(...));
```