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

```groovy
compile 'io.inverno.mod:inverno-http-base:${VERSION_INVERNO_MODS}'
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

## HTTP router API

The HTTP router API defines building blocks for the development of advanced routers that can be used in various situations: in a Web server to route an exchange to an exchange handler based on criteria extracted from the request, in A Web client to resolve the exchange interceptors to invoke before sending the request...

The `AbstractRouter` class defines the base `Router` implementation, it is based on a routing chain used to resolve the resource or resources (e.g. handler, interceptor...) that are best matching an input. It is composed of multiple `RoutingLink`, each link being responsible for resolving the next link in the chain based on a specific criteria extracted from the input.

Considering an HTTP server, a request needs to be routed to a specific handler based on the request path, method, content type, accepted content types, accepted languages... The following routing chain could then be used to implement such router:

```java
var routingChain = RoutingLink
    .<ExchangeHandler<ExchangeContext>, Exchange<ExchangeContext>, SampleRoute, SampleRouteExtractor>link(next -> new PathRoutingLink<>(next) {...})
    .link(next -> new MethodRoutingLink<>(next) {...})
    .link(next -> new ContentRoutingLink<>(next) {...})
    .link(next -> new OutboundAcceptContentRoutingLink<>(next) {...})
    .link(next -> new AcceptLanguageRoutingLink<>(next) {...})
    .getRoutingChain();
```

In above example, `ExchangeHandler<ExchangeContext>` resources are stored in the routing chain and the expected type of input is `Exchange<ExchangeContext>`. When resolving a handler from an exchange, routing links are invoked top to bottom: the path routing is invoked first to return the next best matching link from the path specified in the input exchange, if no matching route was defined (i.e. there is no route in the routing chain that matches the path) the default next link is returned (matching all paths), the next link, the method routing link in above routing chain, is then invoked until we either reach a terminal link (no resource was defined past that link) or the end of chain and the resource if any was defined is then returned. This approach ensures that a single best matching resource is returned. 

The routing chain accepts `SampleRoute` which must implement `PathRoute`, `MethodRoute`, `ContentRoute`, `AcceptContentRoute` and `AcceptLanguageContentRoute` which are respectively required by `PathRoutingLink`, `MethodRoutingLink`, `ContentRoutingLink`, `OutboundAcceptContentRoutingLink` and `AcceptLanguageRoutingLink`. Routes can be extracted from the chain using a `SampleRouteExtractor`

When implementing an `AbstractRouter`, a routing chain must be set in the constructor and `AbstractRoute`, `AbstractRouteManager` and `AbstractRouteExtractor` implementation must be provided.

```java
public class SampleRouter extends AbstractRouter<ExchangeHandler<ExchangeContext>, Exchange<ExchangeContext>, SampleRoute, SampleRouteManager, SampleRouter, SampleRouteExtractor> { 
    
    public TestRouter() {
        super(RoutingLink
            .<ExchangeHandler<ExchangeContext>, Exchange<ExchangeContext>, SampleRoute, SampleRouteExtractor>link(next -> new PathRoutingLink<>(next) {...})
            .link(next -> new MethodRoutingLink<>(next) {...})
            .link(next -> new ContentRoutingLink<>(next) {...})
            .link(next -> new OutboundAcceptContentRoutingLink<>(next) {...})
            .link(next -> new AcceptLanguageRoutingLink<>(next) {...})
        );
    }
    
    ....
}
```

The API provides several base `RoutingLink` implementations that can be used to create HTTP routers:

- `AcceptLanguageRoutingLink` for resolving resources based on input accepted languages
- `AuthorityRoutingLink` for resolving resources based on input authority
- `ContentRoutingLink` for resolving resources based on input content type
- `ErrorRoutingLink` for resolving resources based on input error
- `HeadersRoutingLink` for resolving resources based on input headers
- `InboundAcceptContentRoutingLink` for resolving resources based on input accepted content (for Web client)
- `OutboundAcceptContentRoutingLink` for resolving resources based on input accepted content (for Web server)
- `PathRoutingLink` for resolving resources based on input path
- `QueryParametersRoutingLink` for resolving resources based on input query parameters
- `URIRoutingLink` for resolving resources based on input URI
- `WebSocketSubprotocolRoutingLink` for resolving resources based on input accepted WebSocket subprotocols

> This API is currently used in multiple modules which requires routing capabilities: in the *web-server* module for routing Web exchanges to Web exchange handlers or error Web exchange to error Web exchange handlers, in the *web-client* module for resolving the interceptors to apply to a Web exchange before sending the request to the server, in the *discovery-http* module for resolving the endpoints where to send a request.
> 
> Although it can be considered as internal, it is generic enough to have value on its own whenever there's a need to best match inputs to resources based on some set of criteria.

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



