[media-type]: https://en.wikipedia.org/wiki/Media_type
[rfc-3986]: https://tools.ietf.org/html/rfc3986

# Base

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

```java
...
compile 'io.inverno.mod:inverno-base:${VERSION_INVERNO_MODS}'
...
```

The *base* module is usually provided as a transitive dependency by other modules, mainly the *boot* module, so defining a direct dependency is usually not necessary at least for an application module.

## Converter API

The converter API provides interfaces and classes for building converters, decoders or encoders which are basically used to decode/encode objects of a given type from/to objects of another type.

### Scope

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

### Basic converter

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

### Splittable decoder and Joinable encoder

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

### Primitive decoder and encoder

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

### Object converter

An `ObjectConverter` is a convenient interface for building `Object` converters. It extends `Converter`, `SplittablePrimitiveDecoder` and `JoinablePrimitiveEncoder`.

### Reactive converter

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

### Media type converter

A `MediaTypeConverter` is a particular kind of object converter which supports a specific format specified as a [media type][media-type] and converts object from/to raw data in the supported format. A typical example would be a JSON media type converter used to decode/encode raw JSON data.

> The *web* module relies on such converters to respectively decode end encode HTTP request and HTTP response payloads based on the content type specified in the message headers.

### Composite converter

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

## Net API

The Net API provides interfaces and classes to manipulate basic network elements such as URIs or to create basic network clients and servers.

### URIs

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

Path patterns are also supported by enabling the `URIs.Option#PATH_PATTERN` option and allows to create URI patterns with question marks or wildcards.

```java
// Matches all .java files under /src path
URIPattern uriPattern = URIs.uri("/src/**/*.java", URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PATH_PATTERN)
    .buildPathPattern();

// Matches test.jsp, tast.jsp, t1st.jsp...
uriPattern = URIs.uri("/t?st.java", URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PATH_PATTERN)
    .buildPathPattern();
```

> Note that the Path pattern option is not compatible with `ORIGIN` form request target, as a result the URI must be created using the `ABSOLUTE` request target form.

It is possible to determine whether a path pattern is included into another. A path pattern is said to be included into another path pattern if and only if the set of URIs matched by this pattern is included in the set of URIs matched by the other pattern.

̀```java
URIPattern pathPattern1 = URIs.uri("/src/**", URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PATH_PATTERN)
    .buildPathPattern();

URIPattern pathPattern2 = URIs.uri("/src/java/**/*.java", URIs.RequestTargetForm.ABSOLUTE, URIs.Option.PATH_PATTERN)
    .buildPathPattern();

URIPattern.Inclusion inclusion = uriPattern1.includes(uriPattern2); // returns URIPattern.Inclusion.INCLUDED
̀```

The proposed implementation is not exact which is why the `includes()` method returns `INCLUDED` when inclusion could be determined with certainty, `DISJOINT` when exclusion could be determined with certainty and `INDETERMINATE` when inclusion could not be determined with certainty.

> Note that inclusion can only be determined when considering path patterns, ie. created using `buildPathPattern()` method and containing only a path component. The `includes()` method will always return `INDETERMINATE` for any other type of URI patterns.

### Network service

The `NetService` interface specifies a service for building optimized network clients and servers based on Netty. The *base* module doesn't provide any implementation, a base implementation is provided in the *boot* module.

This service especially defines methods to obtain `EventLoopGroup` instances backed by a root event loop group in order to reuse event loops across different network servers or clients running in the same application.

It also defines methods to create basic network client and server bootstraps.

## Reflection API

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

## Resource API

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

A resource content can be read using a `ReadableByteChannel` as follows:

```java
try (Resource resource = new FileResource("/path/to/file")) {
    String content = resource.openReadableByteChannel()
        .map(channel -> {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ByteBuffer buffer = ByteBuffer.allocate(256);
                while (channel.read(buffer) > 0) {
                    out.write(buffer.array(), 0, buffer.position());
                    buffer.clear();
                }
                return new String(out.toByteArray(), Charsets.UTF_8);
            }
            finally {
                channel.close();
            }
        })
        .orElseThrow(() -> new IllegalStateException("Resource is not readable"));
}
```

It can also be read in a reactive way:

```java
try(Resource resource = new FileResource("/path/to/resource")) {
    String content = Flux.from(resource.read())
        .map(chunk -> {
            try {
                return chunk.toString(Charsets.UTF_8);
            }
            finally {
                chunk.release();
            }
        })
        .collect(Collectors.joining())
        .block();
}
```

In a similar way, content can be written to a resource using a `WritableByteChannel` as follows:

```java
try (Resource resource = new FileResource("/path/to/file")) {
    resource.openWritableByteChannel()
        .ifPresentOrElse(
            channel -> {
                try {
                    ByteBuffer buffer = ByteBuffer.wrap("Hello world".getBytes(Charsets.UTF_8));
                    channel.write(buffer);
                }
                finally {
                    channel.close();
                }
            },
            () -> {
                throw new IllegalStateException("Resource is not writable");
            }
        );
}
```

Data can also be written in a reactive way:

```java
try (Resource resource = new FileResource("/path/to/resource")) {
    int nbBytes = Flux.from(resource.write(Flux.just(Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("Hello world".getBytes(Charsets.UTF_8))))))
        .collect(Collectors.summingInt(i -> i))
        .block();
    System.out.println(nbBytes + " bytes written");
}
```

The `MediaTypeService` interface specifies a service used to determine the media type of a resource based on its extension, name, path or URI. As for the resource service, a base implementation is provided in the *boot* module.

```java
MediaTypeService mediaTypeService = ...

// image/png
String mediaType = mediaTypeService.getForExtension("png");
```
