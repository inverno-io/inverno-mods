[rfc-7231-5.3.2]: https://tools.ietf.org/html/rfc7231#section-5.3.2
[rfc-7231-5.3.5]: https://tools.ietf.org/html/rfc7231#section-5.3.5
[rfc-7235-4.2]: https://datatracker.ietf.org/doc/html/rfc7235#section-4.2
[rfc-6266]: https://tools.ietf.org/html/rfc6266
[rfc-7231-5.1.1.5]: https://tools.ietf.org/html/rfc7231#section-5.1.1.5
[rfc-6265-4.2]: https://tools.ietf.org/html/rfc6265#section-4.2
[rfc-6265-4.1]: https://tools.ietf.org/html/rfc6265#section-4.1

# HTTP Base

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

```java
...
compile 'io.inverno.mod:inverno-http-base:${VERSION_INVERNO_MODS}'
...
```

The *http-base* module is usually provided as a transitive dependency by other HTTP modules, the *http-client*, *http-server* or the *web* modules in particular, so it might not be necessary to include it explicitly.

## HTTP base API

The base HTTP base API defines common classes and interfaces for implementing applications or modules using HTTP/1.x or HTTP/2 protocols. This includes:

- common HTTP exchange API
- HTTP methods and status enumerations
- Exception bindings for HTTP errors: `BadRequestException`, `InternalServerErrorException`...
- basic building blocks such as `Parameter` which defines the base interface for any HTTP component that can be represented as a key/value pair (eg. query parameter, header, cookie...)
- Cookie types: `Cookie` and `SetCookie`
- Common HTTP header names (`Headers.NAME_*`) and values (`Headers.VALUE_*`) constants
- Common HTTP header types: `Headers.ContentType`, `Headers.Accept`...
- HTTP header codec API for implementing HTTP header codec used to decode a raw HTTP header in a specific `Header` object
- A HTTP header service used to encode/decode HTTP headers from/to specific `Header` objects

## HTTP header service

The HTTP header service is the main entry point for decoding and encoding HTTP headers.

The `HeaderService` interface defines method to decode/encode `Header` object from/to `String` or `ByteBuf`.

For instance, a `content-type` header can be parsed as follows:

```java
Base httpBase = ...
HeaderService headerService = httpBase.headerService();

Headers.ContentType contentType = headerService.<Headers.ContentType>decode("content-type", "application/xml;charset=utf-8");

// application/xml
String mediaType = contentType.getMediaType();
// utf-8
Charset charset = contentType.getCharset();

```

The *http-base* module provides a default implementation exposed as a bean which relies on a set of `HeaderCodec` objects to support specific headers. Custom header codecs can then be injected in the module to extend its capabilities.

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
- `authorization` as defined by [RFC 7235 Section 4.2][rfc-7235-4.2]
- `content-disposition` as defined by [RFC 6266][rfc-6266]
- `content-type` as defined by [RFC 7231 Section 3.1.1.5][rfc-7231-5.1.1.5]
- `cookie` as defined by [RFC 6265 Section 4.2][rfc-6265-4.2]
- `set-cookie` as defined by [RFC 6265 Section 4.1][rfc-6265-4.1]
