[inverno-example-web-server]: https://github.com/inverno-io/inverno-examples/tree/master/inverno-example-web-server
[inverno-javadoc]: https://inverno.io/docs/release/api/index.html

[kubernetes]: https://kubernetes.io/
[ndjson]: http://ndjson.org/
[form-urlencoded]: https://url.spec.whatwg.org/#application/x-www-form-urlencoded

[rfc-6455]: https://datatracker.ietf.org/doc/html/rfc6455
[rfc-6455-1.9]: https://datatracker.ietf.org/doc/html/rfc6455#section-1.9
[rfc-7578]: https://datatracker.ietf.org/doc/html/rfc7578
[rfc-9110-15.4]: https://datatracker.ietf.org/doc/html/rfc9110#name-redirection-3xx

# Web Client

The Inverno *web-client* module provides extended functionalities on top of the *http-client* module for developing high-end Web and RESTfull clients.

It especially provides:

- HTTP request interception
- automatic message payload conversion
- parameterized path and query parameters
- service discovery integration
- declarative Web/REST clients
- an Inverno compiler plugin for generating the module's Web client bean and statically validating the routes

The *web-client* module combines the HTTP client with service discovery capabilities into a single Web client making it possible to seamlessly send requests to remote servers without having to deal directly with HTTP connections or service resolution. The `HttpClient` is provided by the *http-client* module and standard HTTP discovery services are provided by *discovery-http*, *discovery-http-k8s* and *discovery-http-meta* modules, additional custom discovery services can be created by implementing the `HttpDiscoveryService` interface. The module does not compose these modules, as a result above dependencies must be provided externally when initializing the module, typically by composing all modules into the application module.

The Web client also supports automatic message payload conversion which requires a list of media type converters. The *boot* module provides basic implementations for `application/json`, `application/x-ndjson` and `text/plain` media types. Additional media type converters can also be provided by implementing the `MediaTypeConverter` interface. The `Reactor`, also provided by the *boot* module, is required to create the main caching discovery service wrapping the set of discovery services and which periodically refreshes requested services in an event loop.

In order to use the Inverno *web-client* module, we should then declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_web_client {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.configuration;       // configuration source for HTTP meta discovery service
    requires io.inverno.mod.discovery.http;      // DNS based discovery service
    requires io.inverno.mod.discovery.http.meta; // Configuration based HTTP meta discovery service
    requires io.inverno.mod.http.client;
    requires io.inverno.mod.web.client;
}
```

> You might probably always want to include *discovery-http* and *discovery-http-meta* modules for a regular application. Together, they support basic HTTP schemes `http://`, `https://`, `ws://`, `wss://` as well as the `conf://` scheme for defining HTTP meta services in configuration.

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
            <artifactId>inverno-configuration</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-discovery-http</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-discovery-http-meta</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-http-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-web-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-configuration:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-discovery-http:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-discovery-http-meta:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-http-client:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-web-client:${VERSION_INVERNO_MODS}'
```

Using a `WebClient` a request can be sent to a remote service seamlessly resolved with one of the provided discovery services:

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.client.WebClient;
import reactor.core.publisher.Mono;

@Bean
public class TimeService {

    public record CurrentTime(
        int year,
        int month,
        int day,
        int hour,
        int minute,
        int seconds,
        int milliSeconds,
        String dateTime,
        String date,
        String time,
        String timeZone,
        String dayOfWeek,
        boolean dstActive
    ) {}

    public final WebClient<? extends ExchangeContext> webClient;

    public TimeService(WebClient<? extends ExchangeContext> webClient) {
        this.webClient = webClient;
    }

    public Mono<CurrentTime> getTime(String timeZone) {
        return this.webClient
            .exchange(URI.create("https://timeapi.io/api/time/current/zone?timeZone=" + timeZone))
            .flatMap(WebExchange::response)
            .flatMap(response -> response.body().decoder(CurrentTime.class).one());
    }
}
```

In above example, service `https://timeapi.io` is resolved using the DNS discovery service and cached, subsequent requests therefore won't need to resolve it anymore. The service will be regularly refreshed by the internal caching discovery service so it never gets stale. The resolved service can hold one or more service instances each bound to an HTTP client `Endpoint` and among which requests are load balanced.

## Web Client API

The Web client API extends the HTTP client API and mainly defines the `WebClient` interface which is the entry point for sending HTTP requests or creating intercepted Web clients. Unlike the `HttpClient`, the `WebClient` abstracts connections to remote HTTP servers which are entirely managed internally. A `WebExchange` is obtained from a single URI providing the target service ID and the request path. A `WebClient` implementation typically relies on a caching `HttpDiscoveryService` to seamlessly resolve services from the requested service ID when the Web exchange response `Mono` is subscribed.

```java
Mono<String> responseBody = webClient
    .exchange(URI.create("https://service/path/to/resource"))   // 1
    .flatMap(WebExchange::response)                             // 2
    .flatMapMany(response -> response.body().string().stream()) // 3
    .collect(Collectors.joining());
```

1. Create the `WebExchange` targeting service `https://service` and resource `/path/to/resource`.
2. When subscribing to the response `Mono`, service `https://service` is first resolved using an internal `HttpDiscoveryService`, a service instance is obtained for that particular exchange which is then processed by that instance and a request is eventually sent to the remote server exposing the service.
3. Process the response.

> The HTTP discovery services injected when creating the *web-client* module are used by the Web client to resolve services, as a result the schemes supported by these discovery services determine the kind of URIs that can be specified when creating an exchange. For instance, requests can be sent to an HTTP meta service defined in a configuration source (e.g. `conf://metaService/path/to/resource`) only in the presence of the configuration HTTP meta discovery service.

Intercepted Web clients can be created in order to intercept requests matching specific rules. Interceptors are defined on a `WebClient` instance, resulting in the creation of a `WebClient.Intercepted` instance on which further interceptors can be specified eventually leading to the creation of a chain of intercepted Web clients. When processing an exchange created on an intercepted client, all interceptors defined by that particular client and its ancestors are evaluated.

In the following example, requests to any `https` service whose path matches `/fruit/**` will be intercepted:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .method(Method.GET)
    .uri(uri -> uri.scheme("https").host("{:*}").path("/fruit/**"))
    .interceptor(exchange -> {
        LOGGER.info("Exchange was intercepted!");
        return Mono.just(exchange);
    });

Mono<? extends WebExchange<? extends ExchangeContext>> interceptedGetAppleExchange = interceptedWebClient.exchange(URI.create("https://service/fruit/apple"));
Mono<? extends WebExchange<? extends ExchangeContext>> getTomatoExchange = interceptedWebClient.exchange(URI.create("https://service/vegetable/tomato"));
```

> The Web client API inherits the HTTP client API, please refer to the *http-client* module for detailed usage documentation on core HTTP client features.

### Web exchange

The *web-client* defines the client `WebExchange` composed of a `WebRequest`/`WebResponse` pair in an HTTP communication between a client and a server, extending interfaces `Exchange`, `Request` and `Response` defined in the *http-client* module. A client Web exchange provides additional features such as request/response body encoder/decoder based on the content type and WebSocket inbound/outbound data decoder/encoder based on the negotiated subprotocol.

A `WebExchange` is basically obtained by invoking one of the `exchange()` methods on the `WebClient`, typically with a URI and an HTTP method (`GET` by default):

```java
Mono<? extends WebExchange<? extends ExchangeContext>> getExchange = webClient.exchange(URI.create("https://service/path/to/resource"));
Mono<? extends WebExchange<? extends ExchangeContext>> getExchange = webClient.exchange(Method.POST, URI.create("https://service/path/to/resource"));
```

> Note that a `WebExchange` instance is stateful and as a result can't be used to send a request multiple times, a new instance must be created for each request.

More complex constructs are possible by using a `WebClient.WebExchangeBuilder` which supports parameterized path and allows to append query parameters.

```java
Mono<WebExchange> exchange = webClient
    .exchange("https://service")
    .method(Method.GET)
    .path("/fruit/{name}")
    .pathParameter("name", "apple")
    .queryParameter("debug", true)
    .build();
```

The builder implements `Cloneable` in order to be able to create different exchange from a base builder instance.

```java
WebClient.WebExchangeBuilder<? extends ExchangeContext> getFruitExchangeBuilder = webClient
    .exchange("https://service")
    .method(Method.GET)
    .path("/fruit/{name}");

Mono<? extends WebExchange<? extends ExchangeContext>> getAppleExchange = getFruitExchangeBuilder.clone().pathParameter("name", "apple").build();
Mono<? extends WebExchange<? extends ExchangeContext>> getOrangeExchange = getFruitExchangeBuilder.clone().pathParameter("name", "orange").build();
Mono<? extends WebExchange<? extends ExchangeContext>> getBananaExchange = getFruitExchangeBuilder.clone().pathParameter("name", "banana").build();
```

#### Path parameters

Being able to create parameterized requests can be very useful in order to factorize common requests. There are basically two ways to create requests with parameterized paths.

The first approach, is to build the exchange URI explicitly using a simple `String` or a `URIBuilder` which is probably the most simple and flexible approach as it is not limited to the path, any part of the URI can basically be parameterized that way.

```java
URIBuilder productURIBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN, URIs.Option.PARAMETERIZED)
    .scheme("https")
    .host("service")
    .path("/api/{productFamily}/{productName}");

Mono<? extends WebExchange<? extends ExchangeContext>> getAppleExchange = webClient.exchange(productURIBuilder.build("fruit", "apple"));
Mono<? extends WebExchange<? extends ExchangeContext>> getLeekExchange = webClient.exchange(productURIBuilder.build("vegetable", "leek"));
```

The drawback is that parameter values have to be converted to `String` explicitly which can be sometimes problematic and might leak conversion logic. The `ObjectConverter<String>` has been created for that purpose in order to streamline objects marshalling/unmarshalling logic and especially for converting request parameters (e.g. path, header, cookie or query parameters) in both client and server. A global instance is provided in the *boot* module which is typically injected in the *web-client* module which uses it to convert path parameters creating an exchange using a `WebClient.WebExchangeBuilder`.

Above example can then be rewritten as follows in order to use the module's `ObjectConverter` to convert parameter values:

```java
WebClient.WebExchangeBuilder<? extends ExchangeContext> getProductExchangeBuilder = webClient.exchange("https://service/api/{productFamily}/{productName}");

Mono<? extends WebExchange<? extends ExchangeContext>> getAppleExchange = getProductExchangeBuilder.clone()
    .pathParameter("productFamily", "fruit")
    .pathParameter("productName", "apple")
    .build();

Mono<? extends WebExchange<? extends ExchangeContext>> getLeekExchange = getProductExchangeBuilder.clone()
    .pathParameter("productFamily", "vegetable")
    .pathParameter("productName", "leek")
    .build();
```

In above example, parameter values are strings already so the `ObjectConverter` has little interest, but when considering lists or custom types, it is the way to go. This is especially useful when considering query parameters. The global converter has built-in support for many basic types and enums, it is also extensible and custom serializers/deserializers can be provided.

> The builder is mutable for performance reasons, it implements `Cloneable` which allows to create different exchange from a base builder instance which is simply cloned to build new requests.

#### Query parameters

As for path parameters, query parameters can be parameterized using a `URIBuilder` or a `WebClient.WebExchangeBuilder`.

Using a `URIBuilder`, parameters can be declared in the query component of the URI:

```java
URIBuilder productURIBuilder = URIs.uri(URIs.Option.NORMALIZED, URIs.Option.PATH_PATTERN, URIs.Option.PARAMETERIZED)
    .scheme("https")
    .host("service")
    .path("/api/get-time")
    .query("timezone={timezone}");

Mono<? extends WebExchange<? extends ExchangeContext>> getParisTimeExchange = webClient.exchange(productURIBuilder.build("Europe/Paris"));
Mono<? extends WebExchange<? extends ExchangeContext>> getESTTimeExchange = webClient.exchange(productURIBuilder.build("US/Eastern"));
```

Using a `WebClient.WebExchangeBuilder`, query parameters are simply added to the builder:

```java
WebClient.WebExchangeBuilder<? extends ExchangeContext> getProductExchangeBuilder = webClient.exchange("https://service/api/get-times");

Mono<? extends WebExchange<? extends ExchangeContext>> getTimesExchange = getProductExchangeBuilder.clone()
    .queryParameter("timezone", List.of(ZoneId.of("Europe/Paris"), ZoneId.of("US/Eastern")), Types.type(List.class).type(ZoneId.class).and().build())
    .build();
```

The interesting part is that the builder can accept values with complex types because an `ObjectConverter<String>` is used internally to convert them to `String`. In above example, the converter serializes the list of zone IDs into a comma-separated list of serialized Zone IDs (e.g. `Europe/Paris,US/Eastern`) which can be deserialized on the server, most likely using an `ObjectConverter<String>`, into the original list.

> Note that in the context of serialization, specifying the `List<ZoneId>` type when defining the query parameter is not mandatory since the parameter value already conveyed the type, but it can be useful to specify it to optimize serialization or explicitly set the target type from the parameter value's type hierarchy.

#### Request body encoder

The request body can be encoded based on the content type defined in the request headers.

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.client.WebClient;
import reactor.core.publisher.Mono;

@Bean
public static class FruitService {

    public record Fruit(
        String name, 
        String color, 
        String unit, 
        float price, 
        String currency
    ) {}

    public final WebClient<? extends ExchangeContext> webClient;

    public FruitService(WebClient<? extends ExchangeContext> webClient) {
        this.webClient = webClient;
    }

    public Mono<Void> addFruit(Fruit fruit) {
        return this.webClient.exchange(Method.POST, URI.create("https://service/api/fruit"))
            .flatMap(exchange -> {
                exchange.request()
                    .headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
                    .body().encoder().one(fruit);

                return exchange.response();
            })
            .then();
    }
}
```

When invoking the `encoder()` method, a [media type converter](#media-type-converter) corresponding to the request content type is selected to encode the payload. The `content-type` header MUST be specified in the request, otherwise an `IllegalStateException` reporting an empty media type is thrown. If there is no converter corresponding to the media type, a `MissingConverterException` is thrown.

> It is not required to explicitly specify the type of the object to encode as it is implicitly conveyed with the instance. It might however be interesting to specify it to optimize serialization process or, when using inheritance, to control the serialized representation. Parameterized Types can be built at runtime using the [reflection API](#reflection-api).

The encoder is fully reactive, a single object is encoded by invoking method `one()` with an actual instance or a `Mono<T>` and multiple objects can be encoded by invoking method `many()` with an `Iterable<T>` or a `Flux<T>`. Sending multiple objects in a stream is particularly suited for streaming payload to the server and limit resource usage since the complete message doesn't have to be loaded into the memory all at once.

For instance many elements could be streamed to the server as follows:

```java
public Mono<Void> addFruits(Flux<Fruit> fruits) {
    return this.webClient.exchange(Method.POST, URI.create("https://service/api/fruit"))
        .flatMap(exchange -> {
            exchange.request()
                .headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
                .body().encoder().many(fruits);

            return exchange.response();
        })
        .then();
}
```

The part body in a multipart form data request can also be encoded in a similar way based on the content type defined in the part headers:

```java
public Mono<Void> addFruitsMultipart(Flux<Fruit> fruits) {
    return this.webClient.exchange(Method.POST, URI.create("https://service/api/fruit"))
        .flatMap(exchange -> {
            exchange.request()
                .body().multipart().from((factory, data) -> data.stream(
                    fruits.map(fruit -> factory.encoded(
                        part -> part
                            .name(fruit.getName())
                            .headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
                            .value(fruit),
                        Fruit.class
                    )
                )));

            return exchange.response();
        })
        .then();
}
```

#### Response body decoder

Conversely, the response body can be decoded based on the content type defined in the response headers.

```java
public Mono<Fruit> getFruit(String name) {
    return this.webClient.exchange(URI.create("https://service/api/fruit/" + name))
        .flatMap(exchange -> {
            exchange.request().headers(headers -> headers.accept(MediaTypes.APPLICATION_JSON));
            return exchange.response();
        })
        .flatMap(response -> response.body().decoder(Fruit.class).one());
}
```

When invoking the `decoder()` method, a [media type converter](#media-type-converter) corresponding to the response content type is selected to decode the payload. The `content-type` header MUST be present in the response, otherwise an `IllegalStateException` is thrown indicating that a response with an empty media type was received. If there is no converter corresponding to the media type, a `MissingConverterException` is thrown.

A decoder is obtained by specifying the type of the object to decode in the `decoder()` method, the type can be a `Class<T>` or a `java.lang.reflect.Type` which allows to decode parameterized types at runtime bypassing type erasure. Parameterized Types can be built at runtime using the [reflection API](#reflection-api).

A single object is decoded by invoking method `one()` and multiple objects can be decoded by invoking method `many()`, both are reactive, when returning multiple objects the client decodes and streams the results as they are received from the server which means the first element can be processed before the last one has been actually received.

```java
public Flux<Fruit> listFruits() {
    return this.webClient.exchange(URI.create("https://service/api/fruit"))
        .flatMap(exchange -> {
            exchange.request().headers(headers -> headers.accept(MediaTypes.APPLICATION_JSON));
            return exchange.response();
        })
        .flatMapMany(response -> response.body().decoder(Fruit.class).many());
}
```

### WebSocket exchange

A WebSocket exchange is obtained by upgrading the Web exchange using the `webSocket()` method. The resulting `Web2SocketExchange` allows to seamlessly convert WebSocket inbound and outbound messages based on the subprotocol negotiated during the opening handshake.

As for request and response payloads, a [media type converter](#media-type-converter) corresponding to the subprotocol is selected to decode/encode inbound and outbound messages. If there is no converter corresponding to the subprotocol, a `WebSocketException` is thrown indicating that no converter was found matching the subprotocol.

The subprotocol must then correspond to a valid media type. Unlike request and response payloads which expect strict media type representation, compact `application/` media type representation can be specified as subprotocol. In practice, it is possible to open a WebSocket connection with subprotocol `json` to select the `application/json` media type converter.

> As defined by [RFC 6455][rfc-6455], a WebSocket subprotocol is not a media type and is registered separately. However, using media type is very handy in this case as it allows to reuse the data conversion facility. Using compact `application/` media type representation mitigates this specification violation as it is then possible to specify a valid subprotocol while still being able to select a media type converter. Let's consider the registered subprotocol `v2.bookings.example.net` (taken from [RFC 6455 Section 1.9][rfc-6455-1.9]), we can then create a media type converter for `application/v2.bookings.example.net` that will be selected when opening connection using that particular subprotocol.

The following example shows how to open a WebSocket to a simple chat server sending and receiving text messages formated in JSON:

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.client.WebClient;
import reactor.core.publisher.Flux;

@Bean
public static class ChatClient {

    public record Message(
        String nickname,
        String message
    ) {}

    public final WebClient<? extends ExchangeContext> webClient;

    public ChatClient(WebClient<? extends ExchangeContext> webClient) {
        this.webClient = webClient;
    }

    public Flux<Message> join(Flux<Message> inbound) {
        return this.webClient.exchange(URI.create("https://service/chat"))
            .flatMap(exchange -> exchange.webSocket("json"))
            .flatMapMany(wsExchange -> {
                wsExchange.outbound().encodeTextMessages(inbound, Message.class);
                return wsExchange.inbound().decodeTextMessages(Message.class);
            });
    }
}
```

### Web route interceptor

A Web route interceptor specifies an `ExchangeInterceptor` and the rules a Web exchange created on an intercepted Web client must match to be intercepted by that interceptor. A Web route Interceptor is defined on the `WebClient` using a `WebRouteInterceptorManager` resulting in the creation of a `WebClient.Intercepted` instance on which further Web route interceptors can be defined resulting in the creation of a tree of intercepted Web clients each of which inherits interceptors from its ancestors.

The `WebRouteInterceptor` interface, implemented by the `WebClient`, defines a fluent API for the definition of Web interceptors. The following is an example of the definition of a Web route interceptor that is applied to exchange matching `POST` method and producing `application/json` request:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .method(Method.POST)
    .produce(MediaTypes.APPLICATION_JSON)
    .interceptor(exchange -> {
        LOGGER.info("Intercepted!");
        return Mono.just(exchange);
    });
```

The resulting intercepted Web client can then be used to send requests eventually intercepted when they match the rules defined in above Web interceptor route.

```java 
Mono<? extends WebExchange<? extends ExchangeContext>> interceptedExchange = interceptedWebClient
    .exchange(Method.POST, URI.create("https://service/path/to/resource"))
    .doOnNext(exchange -> exchange.request()
        .headers(headers -> headers.contentType(MediaTypes.APPLICATION_JSON))
        .body().string().value("{\"message\":\"Hello world!\"}")
    );

Mono<? extends WebExchange<? extends ExchangeContext>> notInterceptedExchange = interceptedWebClient
    .exchange(Method.GET, URI.create("https://service/path/to/resource"));
```

Defining another Web route interceptor on the intercepted Web client will result in the creation of a child intercepted Web client which inherits interceptors from its parent.

```java
WebClient.Intercepted<? extends ExchangeContext> childInterceptedWebClient = interceptedWebClient.intercept()
    .method(Method.GET)
    .interceptor(exchange -> {
        LOGGER.info("Intercepted GET request!");
        return Mono.just(exchange);
    });
```

All `GET` exchanges will be intercepted by above interceptor when created with the `childInterceptedWebClient`, but `GET` exchanges creates with the parent `interceptedWebClient` still won't be intercepted.

```java
Mono<? extends WebExchange<? extends ExchangeContext>> interceptedGetExchange = childInterceptedWebClient
    .exchange(Method.GET, URI.create("https://service/path/to/resource"));

Mono<? extends WebExchange<? extends ExchangeContext>> stillNotInterceptedGetExchange = interceptedWebClient
    .exchange(Method.GET, URI.create("https://service/path/to/resource"));
```

> Arranging interceptors in chains of Web clients is particularly useful in a multimodule application where the `WebClient` bean is injected in component modules which must be able to define their own interceptors in complete isolation without impacting other modules. For instance, global interceptors can be defined in an application module and the resulting intercepted Web client injected into multiple submodules which can then define their own interceptors on top in perfect isolation.

Multiple Web interceptor routes can be created at once using a `WebRouteInterceptor.Configurer`. For instance previous interceptors could have been defined at once in a single intercepted Web client as follows:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.configureInterceptors(interceptors -> interceptors
    .intercept()
    .method(Method.POST)
    .produce(MediaTypes.APPLICATION_JSON)
    .interceptor(exchange -> {
        LOGGER.info("Intercepted!");
        return Mono.just(exchange);
    })
    .intercept()
    .method(Method.GET)
    .interceptor(exchange -> {
        LOGGER.info("Intercepted GET request!");
        return Mono.just(exchange);
    })
);
```

> It is important to return the final `WebRouteInterceptor` in the configurer in order to include all defined interceptors in the resulting intercepted Web client. For instance, considering the following example, the last interceptor won't be included:
> 
> ```java
> webClient.configureInterceptors(interceptors -> {
>     WebRouteInterceptor<ExchangeContext> interceptor = interceptors.intercept()
>         .interceptor(exchange -> {
>             LOGGER.info("I'm included");
>             return Mono.just(exchange);
>         });
> 
>     interceptor.intercept()
>         .method(Method.GET)
>         .interceptor(exchange -> {
>             LOGGER.info("I'm just ignored");
>             return Mono.just(exchange);
>         });
> 
>     return interceptor;
> });
> ```

The same exchange interceptor can be defined for multiple routes at once by defining multiple routing rules, the following example actually results in 4 individual routes being defined (`GET+JSON`, `GET+XML`, `POST+JSON` and `POST+XML`):

```java
WebClient.Intercepted<? extends ExchangeContext> childInterceptedWebClient = interceptedWebClient.intercept()
    .method(Method.GET)
    .method(Method.POST)
    .produce(MediaTypes.APPLICATION_JSON)
    .produce(MediaTypes.APPLICATION_XML)
    .interceptor(exchange -> {
        LOGGER.info("Intercepted GET request!");
        return Mono.just(exchange);
    });
```

The following rules can be used to defined Web route interceptors: URI, method, consume, produce and language. They are evaluated in that order by the intercepted Web client when processing an exchange.

#### URI routing rule

The URI routing rule matches the exchange URI, namely scheme, authority and path components. It is defined using a `WebRouteInterceptorManager.UriConfigurator`. The Web client implementation relies on the `URIMatcher` to match request URIs, it supports parameterized values, allowing regular expressions and path patterns (i.e. `?`, `*` and `**`) to be defined in URI components values.

The following example shows how to define a URI routing rule matching `https` URIs targeting host `service` on default port and whose path is matching `/api/fruits/**`:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .uri(uri -> uri
        .scheme("https")
        .host("service")
        .path("/api/fruit/**")
    )
    .interceptor(exchange -> {
        ...
    });
```

A regular expression can be defined in order to match both schemes `http` and `https`:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .uri(uri -> uri
        .scheme("{:(http|https)}")
        .host("service")
        .path("/api/fruit/**")
    )
    .interceptor(exchange -> Mono.just(exchange));
```

> Note that defining two Web interceptor routes, one matching `http` scheme and the other one `https` with the same exchange interceptor would produce the same effect.

Pattern `*` can be used in the host component to match any host:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .uri(uri -> uri
        .scheme("{:(http|https)}")
        .host("{:*}")
        .path("/api/fruit/**")
    )
    .interceptor(exchange -> {
        ...
    });
```

Unlike the path component, patterns must be defined in an unnamed parameter using `{:...}` notation in scheme, host, port or authority components. The `**` is also specific to the path component and can't be used elsewhere.

The URI routing rule strictly matches the request URI, this concretely means that `https://service:443/api/fruit/apple` is not matched by above rules which do not define the port component even if `443` is implicit here. Multiple rules have to be defined to obtain that behaviour.

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .uri(uri -> uri
        .scheme("{:(http|https)}")
        .host("{:*}")
        .path("/api/fruit/**")
    )
    .uri(uri -> uri
    .scheme("{:(http|https)}")
        .host("{:*}")
        .port(443)
        .path("/api/fruit/**")
    )
    .interceptor(exchange -> {
        ...
    });
```

When defined, the authority component overrides the host and port components and vice versa:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .uri(uri -> uri
        .scheme("http")
        .host("localhost")
        .port(8080)
        .authority("127.0.0.1:8080") // overrides localhost and 8080
    )
    .interceptor(exchange -> {
        ...
    });
```

#### Method routing rule

The method routing rule matches exchanges sent with a particular HTTP method.

In order to handle all `GET` exchanges, we can do:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient.intercept()
    .method(Method.GET)
    .interceptor(exchange -> {
        ...
    });
```

#### Consume routing rule

The consume routing rule matches exchanges based on the request accepted content types specified in the `accept` header. It is defined by specifying a media range (e.g. `application/*`), an exchange interceptor is executed whenever an exchange request accepted media ranges matches that media range.

In the following example, all exchanges accepting `application/json` media types are intercepted, including exchanges with accept headers containing `application/json` but also `application/*`, `*/json` or `*/*` and excluding those with accept headers containing `application/xml` for instance:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
    .consume(MediaTypes.APPLICATION_JSON) // matches application/json, application/*, */json and */*
    .interceptor(exchange -> Mono.just(exchange));
```

A media range is specified using wildcards making it possible to intercept exchange accepting for instance `application` type with any subtype:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
    .consume("application/*") // matches application/json, application/xml
    .interceptor(exchange -> Mono.just(exchange));
```

> It is important to remember that exchanges are intercepted before a request is actually sent to the server, the response is not known when interceptor routes are resolved, it is not possible to anticipate the actual response content type. Behaviour here is then actually similar to the language routing rules and how the `accept-language` header is working.

#### Produce routing rule

The produce routing rule matches an exchange based on the request content type provided in the `content-type` header. As for the consume routing rule, it is defined by specifying a media range (e.g. `application/*`), an exchange interceptor is executed whenever an exchange request content type matches that media range.

In the following example, all exchanges producing `application/json` are intercepted:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
    .produce(MediaTypes.APPLICATION_JSON) // matches application/json only
    .interceptor(exchange -> Mono.just(exchange));
```

A media range is specified using wildcards making it possible to intercept exchange producing `application` type with any subtype:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
    .produce("application/*") // matches application/json, application/xml...
    .interceptor(exchange -> Mono.just(exchange));
```

> Note that unlike the Web server which selects the best matching route when routing a request to the exchange handler, the Web client will execute every route interceptor matching an exchange request content type will be executed. 

#### Language routing rule

Tha language routing rule matches exchanges based on the request accepted languages specified in the `accept-language` header. It is defined by specifying a language range (e.g. `*` or `fr-FR`), an exchange interceptor is executed whenever an exchange request accepted languages matches that language range.

In the following example, all exchanges accepting `fr-FR` language are intercepted, including `fr`

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
    .language("fr-FR") // matches fr-FR, fr or *
    .interceptor(exchange -> Mono.just(exchange));
```

A wildcard can also be specified to match any language (which is basically equivalent to not specifying any language rule):

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = this.webClient.intercept()
    .language("*") // matches any language
    .interceptor(exchange -> Mono.just(exchange));
```

## Web Client

The *web-client* implements the Web client API using *http-client* and *discovery-http* modules which are not composed inside the module, as a result it doesn't instantiate these modules and required dependencies are defined instead for injecting the `Reactor`, the `HttpClient` and a list of `HttpDiscoveryService` which are used to create exchanges and resolve HTTP services eventually used to process them. Internally the list of HTTP discovery services is wrapped in a `CompositeDiscoveryService` itself wrapped in a `CachingDiscoveryService` which allows services to be resolved once for a whole application and regularly refreshed in an event loop obtained from the `Reactor` to avoid stale services. 

This makes the *web-client* module configurable and extensible by providing different sets of HTTP discovery services which may include custom implementations, it is possible to precisely select the kind of services that can be requested to the Web client: `http://`, `conf://`, `k8s-env`...

For instance the following application module descriptor includes the basic DNS based HTTP discovery service exposed in *discovery-http* (`http://`, `https://`, `ws://` and `wss://`), the configuration based HTTP meta discovery service (`conf://`) exposed in *discovery-htt-meta* module and the environment variables based [Kubernetes][kubernetes] discovery service (`k8s-env://`) exposed in *discovery-http-k8s* module:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_web_client {
    requires io.inverno.mod.boot;                // provides Reactor
    requires io.inverno.mod.configuration;       // configuration source for HTTP meta discovery service
    requires io.inverno.mod.http.client;         // provides HttpClient
    requires io.inverno.mod.discovery.http;      // DNS based discovery service
    requires io.inverno.mod.discovery.http.meta; // Configuration based HTTP meta discovery service
    requires io.inverno.mod.discovery.http.k8s;  // Kubernetes discovery service
    requires io.inverno.mod.web.client;

    exports io.inverno.example.app_web_client;
}
```

We can create a simple `TimeService` in the *app_web_client* module for querying a time zone API:

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.client.WebClient;
import java.net.URI;
import reactor.core.publisher.Mono;

@Bean
public class TimeService {

    public record CurrentTime(
        int year,
        int month,
        int day,
        int hour,
        int minute,
        int seconds,
        int milliSeconds,
        String dateTime,
        String date,
        String time,
        String timeZone,
        String dayOfWeek,
        boolean dstActive
    ) {}

    public final WebClient<? extends ExchangeContext> webClient;

    public TimeService(WebClient<? extends ExchangeContext> webClient) {
        this.webClient = webClient;
    }

    public Mono<CurrentTime> getTime(String timeZone) {
        return this.webClient
            .exchange(URI.create("https://timeapi.io/api/time/current/zone?timeZone=" + timeZone))
            .flatMap(exchange -> {
                exchange.request()
                    .headers(headers -> headers
                        .accept(MediaTypes.APPLICATION_JSON)
                    );
                return exchange.response();
            })
            .flatMap(response -> response.body().decoder(CurrentTime.class).one());
    }
}
```

The *app_web_client* module can be started as an application and the time service invoked as follows:

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.source.BootstrapConfigurationSource;
import java.io.IOException;
import java.util.function.Supplier;

public class Main {

    @Bean
    public interface ServiceConfigurationSource extends Supplier<ConfigurationSource> {}

    public static void main(String[] args) throws IOException {
        App_web_client webClientApp = Application.run(new App_web_client.Builder(new BootstrapConfigurationSource(Main.class.getModule(), args)));
        try {
            System.out.println(webClientApp.timeService().getTime("america/los_angeles").block());
        }
        finally {
            webClientApp.stop();
        }
    }
}
```

The `serviceConfigurationSource` socket bean must be defined to provide a `ConfigurationSource` into the module which is required by the `configurationHttpMetaDiscoveryService` provided by the *discovery-http-meta* and injected into the *web-client* module which uses a configuration source for resolving `conf://` services.

> We injected a `BootstrapConfigurationSource` which is usually used to configure modules settings. As a result HTTP meta service descriptors can be defined along other properties in configuration files, environment variables, system properties or even directly in the command line. But we could also have created dedicated sources for HTTP services and application configuration. 

In above example, the application sends a `GET` query to `https://timeapi.io/api/time/current/zone?timeZone=america/los_angeles` and outputs the `TimeService.CurrentTime` describing the current time at the requested timezone:

```plaintext
INFO Application Inverno is starting...


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
     ║ Java version        : 21.0.2+13-58                                                         ║
     ║ Java home           : /home/jkuhn/Devel/jdk/jdk-21.0.2                                     ║
     ║                                                                                            ║
     ║ Application module  : io.inverno.example.app_web_client                                    ║
     ║ Application version : 1.0.0-SNAPSHOT                                                       ║
     ║ Application class   : io.inverno.example.app_web_client.Main                               ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ...                                                                                       ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


INFO App_web_client Starting Module io.inverno.example.app_web_client...
INFO Boot Starting Module io.inverno.mod.boot...
INFO Boot Module io.inverno.mod.boot started in 293ms
INFO Http Starting Module io.inverno.mod.discovery.http...
INFO Client Starting Module io.inverno.mod.http.client...
INFO Base Starting Module io.inverno.mod.http.base...
INFO Base Module io.inverno.mod.http.base started in 4ms
INFO Client Module io.inverno.mod.http.client started in 12ms
INFO Http Module io.inverno.mod.discovery.http started in 13ms
INFO K8s Starting Module io.inverno.mod.discovery.http.k8s...
INFO K8s Module io.inverno.mod.discovery.http.k8s started in 1ms
INFO Meta Starting Module io.inverno.mod.discovery.http.meta...
INFO Meta Module io.inverno.mod.discovery.http.meta started in 67ms
INFO Client Starting Module io.inverno.mod.web.client...
INFO Base Starting Module io.inverno.mod.web.base...
INFO Base Starting Module io.inverno.mod.http.base...
INFO Base Module io.inverno.mod.http.base started in 0ms
INFO Base Module io.inverno.mod.web.base started in 1ms
INFO Client Module io.inverno.mod.web.client started in 16ms
INFO App_web_client Module io.inverno.example.app_web_client started in 411ms
INFO Application Application io.inverno.example.app_web_client started in 483ms
INFO AbstractEndpoint HTTP/1.1 Client (nio) connected to https://timeapi.io:443
CurrentTime[year=2024, month=12, day=2, hour=1, minute=4, seconds=51, milliSeconds=493, dateTime=2024-12-02T01:04:51.4933778, date=12/02/2024, time=01:04, timeZone=America/Los_Angeles, dayOfWeek=Monday, dstActive=false]
INFO App_web_client Stopping Module io.inverno.example.app_web_client...
INFO Boot Stopping Module io.inverno.mod.boot...
INFO Boot Module io.inverno.mod.boot stopped in 0ms
INFO Http Stopping Module io.inverno.mod.discovery.http...
INFO Http Module io.inverno.mod.discovery.http stopped in 0ms
INFO K8s Stopping Module io.inverno.mod.discovery.http.k8s...
INFO K8s Module io.inverno.mod.discovery.http.k8s stopped in 0ms
INFO Meta Stopping Module io.inverno.mod.discovery.http.meta...
INFO Meta Module io.inverno.mod.discovery.http.meta stopped in 0ms
INFO Client Stopping Module io.inverno.mod.http.client...
INFO Base Stopping Module io.inverno.mod.http.base...
INFO Base Module io.inverno.mod.http.base stopped in 0ms
INFO Client Module io.inverno.mod.http.client stopped in 0ms
INFO Client Stopping Module io.inverno.mod.web.client...
INFO Base Stopping Module io.inverno.mod.web.base...
INFO Base Stopping Module io.inverno.mod.http.base...
INFO Base Module io.inverno.mod.http.base stopped in 0ms
INFO Base Module io.inverno.mod.web.base stopped in 0ms
INFO Client Module io.inverno.mod.web.client stopped in 0ms
INFO App_web_client Module io.inverno.example.app_web_client stopped in 7ms
```

### Configuration

The Web client configuration is specified in the *web-client* module configuration `WebClientConfiguration` which includes, among other things, the *http-client* module configuration `HttpClientConfiguration` and the `NetClientConfiguration` for low level client network configuration which override the default configurations defined in the *http-client* and *boot* modules.

These configurations can be exposed by creating the following in the *app_web_client*: 

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.boot.BootConfiguration;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.web.client.WebClientConfiguration;

@Configuration
public interface App_web_clientConfiguration {
    @NestedBean
    WebClientConfiguration web_client();
}
```

Web client configuration allows to configure the internal discovery service and the base load balancing strategy in particular, it also contains an HTTP client configuration and a Net client configuration that supersedes the configurations provided in the HTTP client module.

> When using the `WebClient`, HTTP client and Net client configurations must be specified in the `WebClientConfiguration`, they are eventually used to create the `HttpTrafficPolicy` used to resolve services and which supersedes any HTTP client module configuration. As result, defining them explicitly in the module only affect the `HttpClient` when it is used directly to create `Endpoint` instances but will have no effect on the Web client HTTP configuration. 

The boot configuration allows to configure low level net client configuration, the HTTP client configuration allows to configure the default HTTP client configuration and the 

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.source.BootstrapConfigurationSource;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import java.io.IOException;
import java.util.function.Supplier;

public class Main {

    @Bean
    public interface ServiceConfigurationSource extends Supplier<ConfigurationSource> {}

    public static void main(String[] args) throws IOException {
        App_web_client webClientApp = Application.run(new App_web_client.Builder(new BootstrapConfigurationSource(Main.class.getModule(), args))
            .setApp_web_clientConfiguration(App_web_clientConfigurationLoader.load(configuration -> configuration
                .web_client(web_client -> web_client
                    .discovery_service_ttl(20000)
                    .load_balancing_strategy(HttpTrafficPolicy.LoadBalancingStrategy.ROUND_ROBIN)
                    .http_client(http_client -> http_client.pool_max_size(3))
                    .net_client(BootNetClientConfigurationLoader.load(net_client -> net_client.connect_timeout(30000)))
                )
            ))
        );
        ...
    }
}
```

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties.

> You can also refer to the [configuration module documentation](#configuration-1) to get more details on how configuration works and more especially how you can from here define the server configuration in command line arguments, property files...

### Initializing the Web client

The *web-client* module does not expose the `WebClient` bean directly, instead it exposes a `WebClient.Boot` bean with the sole purpose of initializing the *root* `WebClient` bean with an exchange context factory used to create the application context. The Web client is context aware, a strongly typed `ExchangeContext` is attached to the exchange and propagated during processing, it is typically initialized with the `WebExchange` when initiating a request and then used and/or enriched within interceptors. The exchange context type is unique and global within an application and more precisely within the scope of a *web-client* module instance which is usually instantiated within the application module composing the module.

In order to set up the `WebClient` bean in a module, we need then to consider two use cases: the case of a module composing and then starting the *web-client* module which requires the exchange context factory to initialize and expose the *root* `WebClient`, and then the case of a component module which simply uses the `WebClient` without starting the *web-client* module and then defines a `WebClient` socket with some specific context type. 

In the first case, a wrapper bean is needed to create the *root* `WebClient` bean from the `WebClient.Boot` bean and the exchange context factory. The following example shows how to create the *root* `WebClient` from the *boot* Web client:

> Note that the *root* `WebClient` can be created only once by invoking the `webClient()` on the `WebClient.Boot` instance, any subsequent call to that method will result in an `IllegalStateException`.

In the second case, the module can simply define a regular `WebClient` module socket specifying the context type required by the interceptors and services in the module.

The Inverno Web compiler plugin is taking care of generating these beans. The global exchange context type is computed by aggregating all types declared in `WebClient` sockets within the module including bean sockets or module sockets used to inject the Web client or in `WebRouteInterceptor.Configurer` beans used to configure Web client route interceptors.

Let's consider an application defining two services: `FrontOfficeService` used to query front office services and `BackOfficeService` service to query back office services. Each of these services requires a specific context: `FrontOfficeContext` and `BackOfficeContext`.

The front office context can be defined as follows:

```java
package io.inverno.example.app_web_client;

import io.inverno.mod.http.base.ExchangeContext;

public interface FrontOfficeContext extends ExchangeContext {

    void setMarket(String market);

    String getMarket();
}
```

As for the front office service, it requires a `WebClient` with a `FrontOfficeContext` which MUST be defined using an upper bound wildcard. The injected instance is intercepted in the constructor to set the context in all requests issued within the service.

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebExchange;
import java.net.URI;
import reactor.core.publisher.Mono;

@Bean
public class FrontOfficeService {

    private final WebClient<? extends FrontOfficeContext> webClient;

    public FrontOfficeService(WebClient<? extends FrontOfficeContext> webClient) {
        this.webClient = webClient.intercept()
            .interceptor(exchange -> {
                exchange.request().headers(headers -> headers.set("fe-market", exchange.context().getMarket()));
                return Mono.just(exchange);
            });
    }

    public Mono<Void> doSomeStuff() {
        return this.webClient
            .exchange(Method.POST, URI.create("conf://frontOffice/"))
            .doOnNext(exchange -> exchange.context().setMarket(market))
            .flatMap(WebExchange::response)
            .then();
    }
}
```

The back office context and service can be created in a similar way:

```java
package io.inverno.example.app_web_client;

public interface BackOfficeContext {

    void setRiskPolicy(String policy);

    String getRiskPolicy();
}
```

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebExchange;
import java.net.URI;
import reactor.core.publisher.Mono;

@Bean
public class BackOfficeService {

    private final WebClient<? extends BackOfficeContext> webClient;

    public BackOfficeService(WebClient<? extends BackOfficeContext> webClient) {
        this.webClient = webClient.intercept()
            .interceptor(exchange -> {
                exchange.request().headers(headers -> headers.set("be-risk-policy", exchange.context().getRiskPolicy()));
                return Mono.just(exchange);
            });
    }

    public Mono<Void> doSomeStuff() {
        return this.webClient
            .exchange(Method.POST, URI.create("conf://backOffice/"))
            .doOnNext(exchange -> exchange.context().setRiskPolicy("some-hardcoded-sample-policy"))
            .flatMap(WebExchange::response)
            .then();
    }
}
```

The Inverno compiler plugin basically generates class `<MODULE_CLASS>_WebClient` containing the aggregated `Context` type extending all required types which MUST then all be defined as interfaces extending `ExchangeContext`. For the first case, when the *root* `WebClient` is initialized, a concrete `ContextImpl` implementation is also generated to be able to provide the context factory to the boot Web client:

- Getter and setter methods (i.e. `T get*()` and `void set*(T value)` methods) are implemented in order be able to set and get data on the context.
- Other methods with no default implementation gets a blank implementation (i.e. no-op).

When compiling above module, the Inverno Web compiler plugin generates `App_web_client_WebClient` private wrapper bean which:

- defines the `App_web_client_WebClient.Context` interface aggregating module's exchange context types
- defines the `App_web_client_WebClient.ContextImpl` class implementing the context interface
- initializes the *root* `WebClient` using the aggregated context implementation and the Web route interceptor configurers defined in the module

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Wrapper;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebRouteInterceptor;
import java.lang.String;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.processing.Generated;

@Wrapper @Bean( name = "webClient", visibility = Bean.Visibility.PRIVATE )
@Generated(value="io.inverno.mod.web.compiler.internal.client.WebClientCompilerPlugin", date = "2024-12-02T15:53:08.284683043+01:00[Europe/Paris]")
public final class App_web_client_WebClient implements Supplier<WebClient<App_web_client_WebClient.Context>> {

    private final WebClient.Boot webClientBoot;
    private WebClient<App_web_client_WebClient.Context> webClient;

    private List<WebRouteInterceptor.Configurer<? super App_web_client_WebClient.Context>> interceptorsConfigurers;

    public App_web_client_WebClient(WebClient.Boot webClientBoot) {
        this.webClientBoot = webClientBoot;
    }

    @Init
    public void init() {
        this.webClient = this.webClientBoot.webClient(App_web_client_WebClient.ContextImpl::new);
        this.webClient = this.webClient
            .configureInterceptors(this.interceptorsConfigurers);
    }

    @Override
    public WebClient<App_web_client_WebClient.Context> get() {
        return this.webClient;
    }

    public void setInterceptorsConfigurers(List<WebRouteInterceptor.Configurer<? super App_web_client_WebClient.Context>> interceptorsConfigurers) {
        this.interceptorsConfigurers = interceptorsConfigurers;
    }

    public interface Context extends BackOfficeContext, FrontOfficeContext, ExchangeContext {}

    private static class ContextImpl implements App_web_client_WebClient.Context {
            private String riskPolicy;
            private String market;

            @Override
            public void setRiskPolicy(String riskPolicy) {
                this.riskPolicy = riskPolicy;
            }

            @Override
            public String getMarket() {
                return this.market;
            }

            @Override
            public void setMarket(String market) {
                this.market = market;
            }

            @Override
            public String getRiskPolicy() {
                return this.riskPolicy;
            }
    }
}
```

The plugin only generates a wrapper bean containing the global exchange context implementation when a module composes and starts the *web-client* module. Now let's imagine that the `BackOfficeService` is moved to module *back_office* declared with `@Module(excludes = "io.inverno.mod.web.client")` and composed in the application module. When compiling that module, the plugin then generates a mutator socket used to inject the `WebClient` dependency with the proper context type:

```java
package io.inverno.example.back_office;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Mutator;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.WebRouteInterceptor;
import java.util.List;
import java.util.function.Function;
import javax.annotation.processing.Generated;

@Mutator(required = true) @Bean( name = "webClient" )
@Generated(value="io.inverno.mod.web.compiler.internal.client.WebClientCompilerPlugin", date = "2024-12-02T16:01:45.356889748+01:00[Europe/Paris]")
public final class Back_Office_WebClient implements Function<WebClient<? extends Back_Office_WebClient.Context>, WebClient<Back_Office_WebClient.Context>> {

    private List<WebRouteInterceptor.Configurer<? super Back_Office_WebClient.Context>> interceptorsConfigurers;

    @Override
    @SuppressWarnings("unchecked")
    public WebClient<Back_Office_WebClient.Context> apply(WebClient<? extends Back_Office_WebClient.Context> webClient) {
        return ((WebClient<Back_Office_WebClient.Context>)webClient)
            .configureInterceptors(this.interceptorsConfigurers);
    }

    public void setInterceptorsConfigurers(List<WebRouteInterceptor.Configurer<? super Back_Office_WebClient.Context>> interceptorsConfigurers) {
        this.interceptorsConfigurers = interceptorsConfigurers;
    }

    public interface Context extends BackOfficeContext, ExchangeContext {}
}
```

> A simple socket with the aggregated context type would have been enough to declare the `WebClient` dependency. However, the mutator socket allows to intercept the injected instance before it is made available for dependency injection within the module, it is then possible to configure common interceptors. Please refer to the [Configuring interceptors](#configuring-interceptors) section to learn how interceptors are defined in a Web client application.

When composing that module in the *app_web_client* module, the Web compiler plugin will aggregate `Back_Office_WebClient.Context` into the global `App_web_client.Context` making it possible to inject the resulting `WebClient` bean into the *back_office* module requiring `WebClient<? extends Back_Office_WebClient.Context>`. It is important to notice that `WebClient` instances are always provided with a context type extending the required type and because of type erasure upper bound wildcards MUST always be used when defining `WebClient` socket.

Using such generated context guarantees that the context eventually created by the `WebClient` complies with what is expected within the module and component modules. This allows to safely compose multiple Web client modules in an application, developed by separate teams and defining different context types.

This doesn't come without limitations. For instance, contexts must be defined as interfaces since multiple inheritance is not supported in Java. If you try to use a class, a compilation error will be raised.

Another limitation comes from the fact that it might be difficult to define a Web client or an interceptor that uses many context types, programmatically the only way to achieve this is to create an intermediary type that extends the required context types. Although this is acceptable, it is not ideal semantically speaking. Hopefully this issue can be mitigated when defining Web client routes in a declarative way, a [Declarative Web client](#declarative-web-client) allows to specify context type using intersection types on the route method (e.g. `<T extends FrontOfficeContext & BackOfficeContext>`).

Finally, the Inverno Web compiler plugin only generates concrete implementations for getter and setter methods which might seem simplistic but actual logic can still be provided using default implementations in the context interface. For example, considering a simplistic security context used to set a bearer token in an interceptor, a method to encode the token in base64 can be exposed as follows:

```java
package io.inverno.example.app_web_client;

import io.inverno.mod.http.base.ExchangeContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public interface SecurityContext extends ExchangeContext {

    void setToken(String token);

    String getToken();

    default String tokenInBase64() {
        return Base64.getUrlEncoder().encodeToString(this.getToken().getBytes(StandardCharsets.UTF_8));
    }
}
```

Particular care must be taken when declaring context types with generics (e.g. `Context<A>`), we must always make sure that for a given erased type (e.g. `Context`) there is one type that is assignable to all others which will then be retained during the context type generation. This basically follows Java language specification which prevents from implementing the same interface twice with different arguments as a result the generated context can only implement one which must obviously be assignable to all others. A compilation error shall be reported if inconsistent exchange context types have been defined.

> In order to avoid any misuse and realize the benefits of the context generation, it is important to understand the purpose of the exchange context and why we choose to have it strongly typed.
>
> The exchange context is mainly used to propagate and expose contextual information during the processing of an exchange in interceptors or during the creation of the exchange, it is not necessarily meant to expose any logic.
> 
> A strongly typed context has many advantages over an untyped map:
>
> - static checking can be performed by the compiler,
> - a handler or an interceptor have guarantees over the information exposed in the context (`ClassCastException` are basically impossible),
> - as we just saw it is also possible to expose some logic using default interface methods,
> - actual services can be exposed right away in the context without having to use error-prone string keys or explicit cast.
>
> The generation of the context by the Inverno Web compiler plugin is here to reduce the complexity induced by strong typing as long as above rules are respected.

### Configuring interceptors

The Web client API allows to define interceptors globally with specific sets of rules exchanges must match for the corresponding interceptors to be executed. They can then all be defined on the `WebClient` bean when setting up the instance, resulting in all matching exchanges initiated within the module to be intercepted.

As we just saw, the Inverno Web compiler plugin is generating beans for initializing the `WebClient` in a module whether the *web-client* module is composed and started or not. The generated bean defines a socket for injecting the list of `WebRouteInterceptor.Configurer` beans defined in the module. They are applied when initializing the Web client to create a `WebClient.Intercepted` instance eventually provided in the module.

For instance, the following configurer can be created to intercept exchanges targeting a specific service in order to set a corresponding bearer token for authentication:

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.Headers;
import io.inverno.mod.web.client.WebRouteInterceptor;
import reactor.core.publisher.Mono;

@Bean
public class SecurityWebRouteInterceptorConfigurer implements WebRouteInterceptor.Configurer<ExchangeContext> {

    private static final String FRONT_OFFICE_API_KEY = "abcdef123456";

    private static final String BACK_OFFICE_API_KEY = "ghijkl78910";

    @Override
    public WebRouteInterceptor<ExchangeContext> configure(WebRouteInterceptor<ExchangeContext> interceptors) {
        return interceptors
            .intercept()
            .uri(uri -> uri.scheme("conf").host("frontOffice").path("/**"))
            .interceptor(exchange -> {
                exchange.request().headers(headers -> headers.set(Headers.NAME_AUTHORIZATION, "Bearer " + FRONT_OFFICE_API_KEY));
                return Mono.just(exchange);
            })
            .intercept()
            .uri(uri -> uri.scheme("conf").host("backOffice").path("/**"))
            .interceptor(exchange -> {
                exchange.request().headers(headers -> headers.set(Headers.NAME_AUTHORIZATION, "Bearer " + BACK_OFFICE_API_KEY));
                return Mono.just(exchange);
            });
    }
}
```

After compiling the module, any request sent to `conf://frontOffice` or `conf://backOffice` is now intercepted and the corresponding API key set in the `authorization` header.

In a multi-module application, interceptors defined in a composite module also apply to exchanges created in its component modules. Since interceptors are defined in a chain of intercepted Web clients which are initialized once in the generated Web client initialization bean, interceptors defined in a module are scoped to that module and its component modules. Considering a component module, its interceptors are not applied, not even evaluated, on exchanges created in the parent composite module or sibling component modules.

### Service discovery

The *web-client* module integrates a `CachingDiscoveryService` whose role is to resolve services from the request URI when processing an exchange. This service is composing the list of `HttpDiscoveryService` injected in the module which basically determines the kind of URIs and therefore services that can be resolved and requested by the Web client. For instance, in order to be able to request a `conf://` URIs, the configuration HTTP meta discovery service provided by the *discovery-http-meta* module and which supports `conf://` scheme must be injected in the module. It is then possible to control and extend how services are resolved by the Web client while abstracting service discovery and therefore also HTTP connectivity. In the end, the Web client allows to request any HTTP service endpoint seamlessly resolved and cached, without having to manipulate HTTP connections either. When the `WebClient` fails to resolve a service, a `ServiceNotFoundException` is thrown.

To better understand what is done behind the scene, let's consider service `conf://frontOffice` configured as follows in a `configuration.cprops` file:

```properties
io.inverno.mod.discovery.http.meta.service.frontOffice = "http://localhost:8080"
```

Here is how to send a request to the `conf://frontOffice` service using the `WebClient`:

```java
Mono<String> responseBody = webClient.exchange(URI.create("conf://frontOffice/path/to/resource"))
    .flatMap(WebExchange::response)
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining());
```

Using the configuration meta `HttpDiscoveryService` as follows, service is resolved and a connection created on every request since the discovery service is not caching services:

And here is how the same request would be sent using the `HttpClient` configuration meta `HttpDiscoveryService`:

```java
URI requestURI = URI.create("conf://frontOffice/path/to/resource");
Mono<String> responseBody = discoveryService.resolve(ServiceID.of(requestURI))
    .flatMap(service -> httpClient.exchange(ServiceID.getRequestTarget(requestURI))
            .flatMap(exchange -> service.getInstance(exchange)
                .map(serviceInstance -> serviceInstance.bind(exchange))
            )
            .flatMap(Exchange::response)
    )
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining());
```

> Please refer to service discovery modules documentation for a detailed description of service discovery.

Now using the `HttpClient` only, an endpoint and therefore a connection is created on every request and the service inet socket address is also hardcoded. When the service is deployed on multiple nodes, connections and load balancing must then be handled manually.

Finally, here is how the same request would be sent using the `HttpClient` only:

```java
Mono<String> responseBody = httpClient
    .endpoint("localhost", 8080).build()
    .exchange("/path/to/resource")
    .flatMap(Exchange::response)
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining());
```

The Web client makes things simpler and more flexible by taking care of service discovery, HTTP connections and caching. Cached services are also regularly refreshed to prevent stale states, the frequency at which they are refreshed and which defaults to 30000 milliseconds, can be controlled by configuration by setting `discovery_service_ttl` Web client configuration property:

```java
App_web_client webClientApp = Application.run(new App_web_client.Builder(new BootstrapConfigurationSource(Main.class.getModule(), args))
    .setApp_web_clientConfiguration(App_web_clientConfigurationLoader.load(configuration -> configuration
        .web_client(web_client -> web_client
            .discovery_service_ttl(20000)
        )
    ))
);
```

### Fail on error status

The Web client is configured to raise exception by default when receiving a response with status `4xx` or `5xx`. The default error response mapper simply convert the error response status to the corresponding `HttpException` using `HttpException.fromStatus()` method.

```java
String responseBody = webClient.exchange(URI.create("http://service/not_found"))
    .flatMap(WebExchange::response)
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining())
    .block(); // throws NotFoundException
```

Custom error response mapper can be specified on the exchange in an interceptor as follows:

```java
WebClient.Intercepted<? extends ExchangeContext> interceptedWebClient = webClient
    .intercept()
    .interceptor(exchange -> {
        exchange.failOnErrorStatus(response -> Mono.error(new CustomException()));
        return Mono.just(exchange);
    });

String responseBody = interceptedWebClient
    .exchange(URI.create("http://service/error"))
    .flatMap(WebExchange::response)
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining())
    .block(); // throws CustomException on error responses

```

If you prefer to handle the error response explicitly, the error mapping can also be disabled on the exchange: 

```java
String responseBody = webClient.exchange(URI.create("http://service/error"))
    .doOnNext(exchange -> exchange.failOnErrorStatus(false)) // do not automatically fail on 4xx or 5xx
    .flatMap(WebExchange::response)
    .flatMapMany(response -> {
        switch(response.headers().getStatus().getCategory()) {
            case CLIENT_ERROR:
            case SERVER_ERROR: {
                throw new CustomException();
            }
            default: return response.body().string().stream();
        }
    })
    .collect(Collectors.joining())
    .block(); // throws CustomException on error responses
```

### Follow redirect

The Web client does not automatically [follow redirect][rfc-9110-15.4] responses (i.e. `3xx` response status), but it is actually quite easy to implement as follows:

```java
Mono<String> responseBody = webClient.exchange(URI.create("http://service/path/to/resource"))
    .flatMap(WebExchange::response)
    .flatMap(response -> {
        if(response.headers().getStatus().getCategory() == Status.Category.REDIRECTION) {
            return response.headers().get(Headers.NAME_LOCATION)
                .map(location -> webClient.exchange(URI.create(location)).flatMap(WebExchange::response))
                .orElseThrow(() -> new IllegalStateException("Missing location header in redirect"));
        }
        return Mono.just(response);
    })
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining());
```

> Above example is a simple `GET` request, things can get more complex considering `POST` request with a body or when headers must be copied, updated or replaced from the original request which is why there is no built-in support to follow redirect responses. Besides service discovery is probably more suited for changing service locations in a microservices application, an HTTP meta service especially support advanced routing of requests as well as path rewriting.

### Retry on error

Retrying a request after receiving an error response can easily be implemented using the reactor API as follows:

```java
Mono<String> responseBody = webClient.exchange(URI.create("http://service/path/to/resource"))
    .flatMap(WebExchange::response)
    .retry(2) // retries 2 times when receiving errors
    .flatMapMany(response ->  response.body().string().stream())
    .collect(Collectors.joining());
```

Using a fixed delay between attempts:

```java
Mono<String> responseBody = webClient.exchange(URI.create("http://service/path/to/resource"))
    .flatMap(WebExchange::response)
    .retryWhen(Retry.fixedDelay(10, Duration.ofMillis(500))) // retries 2 times with a fixed delay of 500ms between each attempt
    .flatMapMany(response ->  response.body().string().stream())
    .collect(Collectors.joining());
```

Using an exponential backoff strategy:

```java
Mono<String> responseBody = webClient.exchange(URI.create("http://service/path/to/resource"))
    .flatMap(WebExchange::response)
    .retryWhen(Retry.backoff(10, Duration.ofMillis(100))) // retries 10 times using an exponential backoff strategy with a minimum duration of 100ms
    .flatMapMany(response ->  response.body().string().stream())
    .collect(Collectors.joining());
```

## Declarative Web Client

The [Web client API](#web-client-api) provides a *programmatic* way of initiating and sending HTTP requests, but it also comes with a set of annotations for defining Web client routes in a declarative way for consuming resources exposed by a single service.

A **Web client** is a simple interface annotated with `@WebClient` and defining methods annotated with `@WebRoute` or `@WebSocketRoute` which describe how to request HTTP or WebSocket resources. These interfaces are scanned at compile time by the Inverno Web compiler plugin in order to generate concrete beans using the `WebClient` bean to create the actual requests for the declared Web route methods.

> Implementations are nested in the module's `WebClient` initializing bean class also generated by the Inverno Web compiler.

For instance, in order to create a client for consuming a book service exposing basic CRUD operations, we have to define a `Book` model in a dedicated `*.dto` package:

```java
package io.inverno.example.app_web_client.dto;

public class Book {

    private String isbn;
    private String title;
    private String author;
    private int pages;

    // Constructor, getters, setters, hashcode, equals...
}
```

Now we can define the `BookClient` interface as follows:

```java
package io.inverno.example.app_web_client;

import io.inverno.example.app_web_client.dto.Book;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.client.annotation.WebClient;
import io.inverno.mod.web.client.annotation.WebRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebClient( uri = "conf://bookService/book" )                                                // 1
public interface BookClient {

    @WebRoute(method = Method.POST, produces = MediaTypes.APPLICATION_JSON)                  // 2
    Mono<Void> create(@Body Book book) throws BadRequestException;                           // 3

    @WebRoute(path = "/{isbn}", method = Method.PUT, produces = MediaTypes.APPLICATION_JSON)
    Mono<Void> update(@PathParam String isbn, @Body Book book) throws NotFoundException;

    @WebRoute(method = Method.GET, consumes = MediaTypes.APPLICATION_JSON)
    Flux<Book> list();

    @WebRoute(path = "/{isbn}", method = Method.GET, consumes = MediaTypes.APPLICATION_JSON)
    Mono<Book> get(@PathParam String isbn);

    @WebRoute(path = "/{isbn}", method = Method.DELETE)
    Mono<Void> delete(@PathParam String isbn);
}
```

1. A Web client must be an interface, the actual implementation is generated by the Web compiler plugin resulting in a bean being provided in the module. It must be annotated with `@WebClient` annotation. The mandatory `uri` parameter specifies the base URI which must specify the service ID and the root service path and therefore must be absolute (i.e. it must have a scheme). The other annotation parameters, `name` and `visibility`, allows to specify the name and the visibility of the generated bean in the module.
2. The `@WebRoute` annotation on a method indicates which service resource is targeted by the method and basically how the Web compiler plugin should create the corresponding exchange. It basically specifies the resource path relative to the base URI, the method, the consumed media types, the produced media type and the accepted language tags.
3. Request Parameters and body are specified as method parameters annotated with `@CookieParam`, `@FormParam`, `@HeaderParam`, `@PathParam`, `@QueryParam` and `@Body` annotations. The return type represents the response body necessarily declared in a reactive type (i.e. `Mono<T>`, `Flux<T>` or `Publisher<T>`).

A `WebExchange.Configurer` can also be declared as method parameter to be able to customize the `WebExchange` and the exchange context in particular when invoking the method. The return type must be reactive, the actual request being always sent when the return publisher is subscribed. The return type usually represents the response body, but it is also possible to return a `WebExchange` or `WebResponse` publisher for specific use cases requiring complete control over the exchange or response.

The package containing the `Book` DTO must be exported to `com.fasterxml.jackson.databind` module in the *app_web_client* module descriptor in order for the `ObjectMapper` to be allowed to instantiate objects and populate them from parsed JSON trees using reflection API.

```java
module io.inverno.example.app_web_client {
    exports io.inverno.example.app_web_client.dto to com.fasterxml.jackson.databind;
}
```

> Using a dedicated package for DTOs allows to limit and control the access to the module classes, if you're not familiar with the Java modular system and used to Java 8<, you might find this a bit distressing but if you want to better structure and secure your applications, this is the way.

The `conf://bookService` HTTP meta service must be configured in `configuration.cprops` for the Web client to be able to resolve the book service:

```properties
io.inverno.mod.discovery.http.meta.service.bookService = "http://127.0.0.1:8080"
```

> The book service basically consumes the book resource exposed in the [Web server example application][inverno-example-web-server]. 

After compiling the module, a `BookClient` bean should have been generated and can now be used to request book resources.

```java
package io.inverno.example.app_web_client;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.example.app_web_client.dto.Book;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.source.BootstrapConfigurationSource;
import java.io.IOException;
import java.util.function.Supplier;

public class Main {

    @Bean
    public interface ServiceConfigurationSource extends Supplier<ConfigurationSource> {}

    public static void main(String[] args) throws IOException {
        App_web_client webClientApp = Application.run(new App_web_client.Builder(new BootstrapConfigurationSource(Main.class.getModule(), args)));
        try {
            webClientApp.bookClient().create(new Book("978-0132143011", "Distributed Systems: Concepts and Design", "George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair", 1080)).block();
            System.out.println(webClientApp.bookClient().get("978-0132143011").block());
            webClientApp.bookClient().delete("978-0132143011").block();
            System.out.println(webClientApp.bookClient().list().collectList().block().size() + " books in store");
        }
        finally {
            webClientApp.stop();
        }
    }
}
```

Running above application should output:

```plaintext
...
INFO Application Application io.inverno.example.app_web_client started in 686ms
INFO AbstractEndpoint HTTP/1.1 Client (nio) connected to http://127.0.0.1:8080
Book{isbn='978-0132143011', title='Distributed Systems: Concepts and Design', author='George Coulouris, Jean Dollimore, Tim Kindberg, Gordon Blair', pages=1080}
0 books in store
...
```

> Notice that only one connection has been created to make four requests in a row which demonstrates that the resolved service is cached and that connections are pooled.

Web client can be defined using a hierarchy of interfaces with support for generics as long as the *leaves*, annotated with the `@WebClient` are not generic types. This allows to factorize the definition of resources. For instance, a generic `CRUDClient` interface can be created in order to define common CRUD operations, the `BookClient` can then simply extends that interface as follows:

```java
package io.inverno.example.app_web_client;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.BadRequestException;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.base.NotFoundException;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.base.annotation.PathParam;
import io.inverno.mod.web.client.annotation.WebRoute;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CRUDClient<T> {

    @WebRoute(method = Method.POST, produces = MediaTypes.APPLICATION_JSON)
    Mono<Void> create(@Body T resource) throws BadRequestException;

    @WebRoute(path = "/{id}", method = Method.PUT, produces = MediaTypes.APPLICATION_JSON)
    Mono<Void> update(@PathParam String id, @Body T resource) throws NotFoundException;

    @WebRoute(method = Method.GET, consumes = MediaTypes.APPLICATION_JSON)
    Flux<T> list();

    @WebRoute(path = "/{id}", method = Method.GET, consumes = MediaTypes.APPLICATION_JSON)
    Mono<T> get(@PathParam String id);

    @WebRoute(path = "/{id}", method = Method.DELETE)
    Mono<Void> delete(@PathParam String id);
}
```

```java
package io.inverno.example.app_web_client;

import io.inverno.example.app_web_client.dto.Book;
import io.inverno.mod.web.client.annotation.WebClient;

@WebClient( uri = "conf://bookService/book" )
public interface BookClient extends CRUDClient<Book> {
    
}
```

### Web client route

A Web client route basically describes the exchange to create in order to request a particular REST endpoint and what to do with the response. This is essentially characterized by:

- An input, the HTTP request characterized by the following components: path, method, query parameters, headers, cookies, path parameters, request body.
- A regular output which is a successful HTTP response (2xx or 3xx), basically a response body and more precisely: status, headers and body.
- A set of error outputs, unsuccessful HTTP responses (4xx or 5xx) basically resulting in an `HttpException` to be thrown but more precisely: status, headers and body.

Web client routes are defined as methods in a Web client which match this definition: the input is defined in method parameters, the output is defined by the return type of the method and finally the exceptions thrown by the method define the error outputs.

It then remains to bind the Web route semantic to the method, this is done using various annotations on the method and its parameters.

The request is sent when the response publisher returned in the Web client route method is subscribed.

#### Request attributes

Basic request attributes are specified in a single `@WebRoute` annotation on a Web client method. It allows to define the path to the resource relative to the base URI specified in the `@WebClient` annotation, the request method, the request content type, the accepted media ranges and the accepted language tags.

For instance, the following example shows how to declare a `POST` request to `http://service/api/v1/resource` endpoint with an `application/json` body and accepting `application/json` content type and `en-US` language in response:

```java
package io.inverno.example.app_web_client;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.client.annotation.WebClient;
import io.inverno.mod.web.client.annotation.WebRoute;
import reactor.core.publisher.Mono;

@WebClient( uri = "http://service/api/v1" )
public interface ApiClientV1 {

    @WebRoute( path = "/resource", method = Method.POST, produces = MediaTypes.APPLICATION_JSON, consumes = MediaTypes.APPLICATION_JSON, language = "en-US" )
    Mono<Void> createResource(@Body ResourceV1 resource);
}
```

The Web compiler plugin more or less translates above `@WebRoute` annotation into the following:

```java
webClient.exchange(Method.POST, URI.create("http://service/api/v1/resource"))
    .doOnNext(exchange -> exchange.request()
        .headers(headers -> headers
            .contentType("application/json")
            .set(Headers.NAME_ACCEPT, "application/json")
            .set(Headers.NAME_ACCEPT_LANGUAGE, "en-US")
        )
        body().<ResourceV1>encoder(ResourceV1.class).value(resource)
    )
    ...
```

#### Parameter bindings

Web client route method parameters are bound to the various elements of the request using `@*Param` annotations defined in the Web client API.

This parameters can be of any type, as long as the parameter converter injected into the *web-client* module can convert it, otherwise a `ConverterException` is thrown. The default parameter converter provided in the *boot* module is able to convert primitive and common types including arrays and collections. Please refer to the [HTTP client documentation](#extending-http-services) to learn how to extend the parameter converter to convert custom types.

In the following example, the value or values of query parameter `isbns` is encoded to an array of strings:

```java
@WebRoute( path = "/book/byIsbn", consumes = MediaTypes.APPLICATION_JSON )
Flux<Book> getBooksByIsbn(@QueryParam String[] isbns);
```

The `isbns` query parameter will be added to target when sending the request: `/book/byIsbn?isbns=978-0132143011,978-0132143012,978-0132143013`.

Method overloading should be used to define optional parameters. In the following example, query parameter `limit` is optional, it is possible to invoke `getBooks()` method with or without it: 

```java
// /book
@WebRoute( path = "/book", consumes = MediaTypes.APPLICATION_JSON )
Flux<Book> getBooks();

// /book?limit=123
@WebRoute( path = "/book", consumes = MediaTypes.APPLICATION_JSON )
Flux<Book> getBooks(@QueryParam int limit);
```

##### Query parameter

Query parameters are declared using the `@QueryParam` annotation as follows:

```java
@WebRoute( path = { "/book/byIsbn" } )
Flux<T> getBooksByIsbn(@QueryParam String[] isbns);
```

The name of the method parameter defines the name of the query parameter to set in the request query component.

##### Path parameter

Path parameters are declared using the `@PathParam` annotation as follows:

```java
@WebRoute( path = "/{id}" )
Mono<T> get(@PathParam String id);
```

Note that the name of the method parameter must match the name of the path parameter defined in the path attribute of the `@WebRoute` annotation.

##### Cookie parameter

It is possible to bind cookie values as well using the `@cookieParam` annotation as follows:

```java
@WebRoute( path = "/" )
Mono<T> get(@CookieParam String session);
```

The name of the method parameter defines the name of the cookie to set in the request `cookie` header.

##### Header parameter

Header fields can also be bound using the `@HeaderParam` annotation as follows:

```java
@WebRoute( path = "/" )
Flux<T> list(@HeaderParam Format format);
```

In previous example, the `Format` type is an enumeration indicating how book references must be returned (e.g. `SHORT`, `FULL`...), the name of the method parameter defines the name of the header to set in the request headers. 

##### Form parameter

Form parameters are bound using the `@FormParam` annotation as follows:

```java
@WebRoute(method = Method.POST, produces = MediaTypes.APPLICATION_X_WWW_FORM_URLENCODED)
Mono<Void> createAuthor(
    @FormParam String forename,
    @FormParam String surname,
    @FormParam LocalDate birthdate,
    @FormParam String nationality);
```

Form parameters are sent in a request body following `application/x-www-form-urlencoded` format as defined by [living standard][form-urlencoded]. The name of the method parameter defines the name of the form parameter in the request body. 

Above example result in the following request body being sent:

```plaintext
forename=Leslie,middlename=B.,surname=Lamport,birthdate=19410207,nationality=US
```

Form parameters are encoded in the request body and as such, `@FormParam` annotation can't be used together with `@Body` or `@PartParam` when defining Web client route method parameters.

##### Part parameter

Part parameters in a multipart request are bound using the `@PartParam` annotation as follows:

```java
@WebRoute( path = "/post_multipart", method = Method.POST, produces = MediaTypes.MULTIPART_FORM_DATA )
Mono<Void> post_multipart(@PartParam String part, @PartParam(filename = "resourceFile") Resource resource, @PartParam(contentType = MediaTypes.APPLICATION_JSON) Message message);
```

Part parameters are sent in a `multipart/form-data` encoded body as defined by [RFC 7578][rfc-7578]. The name of the method parameter defines the name of the part in the request body. A part body is encoded using a media type converter just like a regular request body, the media type converter being selected based on media type specified in the `contentType` attribute. A `Resource` can also be specified in order to upload a file content, the part's filename defaults to the resource name, it can be overridden by specifying in the `filename` attribute. 

Above example result in the following request body being sent:

```plaintext
--------------------------e9632fafa520176d
content-disposition: form-data;name="part"

Some part
--------------------------e9632fafa520176d
content-length: 20
content-type: text/plain
content-disposition: form-data;name="resource";filename="resourceFile"

This is an example!

--------------------------e9632fafa520176d
content-type: application/json
content-disposition: form-data;name="message"

{"message":"Hello world!"}
--------------------------e9632fafa520176d--
```

Part parameters are encoded in the request body and as such, `@PartParam` annotation can't be used together with `@Body` or `@FormParam` when defining Web client route method parameters.

#### Request body

The request body is bound to a route method parameter using the `@Body` annotation, it is automatically converted when sending the request based on the media type defined in the `produce` attribute of the `@WebRoute` annotation which is also eventually set in the `content-type` header of the request. The body method parameter can be of any type as long as there is a converter defined for the specified media type that can convert it.

In the following example, the request body is bound to parameter `book` of type `Book`, the book instance passed to the method is converted to `application/json` when sending the request:

```java
@WebRoute( method = Method.POST, produces = MediaTypes.APPLICATION_JSON )
Mono<Void> create(@Body Book book) throws BadRequestException;
```

The request body can also be specified in a reactive way using a `Mono<T>`, a `Flux<T>` or more broadly a `Publisher<T>`, previous example can be made fully reactive as follows:

```java
@WebRoute( method = Method.POST, produces = MediaTypes.APPLICATION_JSON )
Mono<Void> create(@Body Mono<Book> book) throws BadRequestException;
```

This basically allows to stream request data to the remote endpoint and send consistent objects as soon as they are ready to be sent resulting in reduced memory usage on the client and overall better response time because the server can also start processing data earlier.

```java
@WebRoute( method = Method.POST, consumes = MediaTypes.APPLICATION_X_NDJSON )
Mono<Void> create(@Body Flux<Book> book) throws BadRequestException;
```

> Using the `application/json` media type converter, objects are sent a JSON array. Using the `application/x-ndjson` media type converter, objects are separated by a new line character.

The `@Body` annotation conflicts with the `@FormParam` or `@PartParam` annotations which are all used to specify the request body, as a result they are mutually exclusive and only one can be specified in a Web client route method.

#### Response body

The response body is specified by the return type of the route method, it MUST be defined in a reactive way in a `Mono<T>`, a `Flux<T>` or more broadly a `Publisher<T>`, the request being sent when the publisher returned by the Web client route method is subscribed. The response body is automatically converted using the media type converter corresponding to the media type specified in the response `content-type` header.

```java
@WebRoute( path = "/{isbn}", method = Method.GET, consumes = MediaTypes.APPLICATION_JSON )
Mono<Book> get(@PathParam String isbn);
```

A response can be processed as a stream when the media type converter supports it. For instance, the `application/x-ndjson` converter can emit converted objects each time a new line is encountered as defined by [the ndjon format][ndjson]. This allows to process content as soon as possible without having to wait for the entire payload to be received resulting in reduced resource consumption.

```java
@WebRoute( method = Method.GET, consumes = MediaTypes.APPLICATION_X_NDJSON )
Flux<Book> list();
```

> The `application/json` converter can also be used for streaming elements received in a JSON array.

#### Exposing the Web exchange

Fully declarative Web clients should cover most usage but some specific use cases might still require to have full access to the exchange and or the response. One typical such use case is when there's a need to initialize the exchange context. This can obviously be achieved using the `WebClient` programmatically, but it can also be done in a declarative Web client.

The `WebExchange` can be exposed in a Web client route method in a `WebExchange.Configurer` method parameter:

```java
@WebRoute( method = Method.POST, produces = MediaTypes.APPLICATION_JSON )
Mono<Void> create(@Body Book book, WebExchange.Configurer<ApiContext> exchange) throws BadRequestException;
```

It is then possible to customize it when invoking the method:

```java
webClientApp.bookClient()
    .create(new Book(), exchange -> {
        exchange.request().headers(headers -> headers.set("some-header", "some value"));
        exchange.context().setApiKey("123456789");
    })
    ...
```

Another way to expose the `WebExchange` is to return it in the Web client route method.

```java
@WebRoute( method = Method.POST, produces = MediaTypes.APPLICATION_JSON )
Mono<WebExchange<? extends ApiContext>> create(@Body Book book) throws BadRequestException;
```

The resulting method implementation simply initializes the exchange and returns it, the response publisher must then be explicitly subscribed to send the request.

```java
webClientApp.bookClient()
    .create(new Book()))
    .flatMap(exchange -> {
        exchange.request().headers(headers -> headers.set("some-header", "some value"));
        exchange.context().setApiKey("123456789");
        
        return exchange.response();
    })
    ...
```

Context types declared in Web client route methods are aggregated by the Inverno Web compiler plugin in a unique exchange context type for the module but unlike `WebRouteInterceptor.Configurer`, it is possible to specify intersection types using a type variable when multiple context type are required.

```java
@WebRoute( method = Method.POST, produces = MediaTypes.APPLICATION_JSON )
<T extends TracingContext & SecurityContext> Mono<Void> create(@Body Book book, WebExchange.Configurer<T> exchange) throws BadRequestException;
```

or

```java
@WebRoute( method = Method.POST, produces = MediaTypes.APPLICATION_JSON )
<T extends TracingContext & SecurityContext> Mono<WebExchange<T>> create(@Body Book book) throws BadRequestException;
```

#### Exposing the Web response

A Web client route method can return the `WebResponse` instead of the response body type to give access to the full response. This is particularly useful when there is a need to access the actual response status or headers which are otherwise ignored.

In the following example, the request is sent when the returned `WebResponse` publisher is subscribed:

```java
@WebRoute( method = Method.POST, produces = MediaTypes.APPLICATION_JSON )
Mono<WebResponse> createOrUpdate(@Body Book book) throws NotFoundException;
```

It is then possible to do something useful with the response status and headers:

```java
String result = webClientApp.apiClientV1().createOrUpdate(new Book(...))
    .map(response -> {
        if(response.headers().getStatus() == Status.CREATED) {
            return "A book was created at " + response.headers().get("date").orElse(ZonedDateTime.now().toString());
        }
        else {
            return "A book was updated at " + response.headers().get("date").orElse(ZonedDateTime.now().toString());
        }
    })
    .block();
```

### WebSocket client route

A WebSocket client route is declared using the `@WebSocketRoute` annotation with some differences in semantic and bindings compared to a Web client route. A WebSocket exchange is essentially defined by an inbound stream of messages and an outbound stream of messages.

WebSocket client routes are defined as methods in a Web Client with the following rules:

- The WebSocket `BaseWeb2SocketExchange.Inbound` may be exposed in the method's return type as `Mono<BaseWeb2SocketExchange.Inbound>`.
- The WebSocket inbound may also be specified as method's return type as a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>`.
- The WebSocket `BaseWeb2SocketExchange.Outbound` may be exposed in a method parameter using a `Consumer<BaseWeb2SocketExchange.Outbound>`.
- The WebSocket outbound may also be specified as method parameter as `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>` which closes the WebSocket on terminate by default.
- The `Web2SocketExchange` may be exposed in a method parameter using a `Web2SocketExchange.Configurer`.
- The `Web2SocketExchange` may also be exposed in the method's return type as `Mono<Web2SocketExchange<T extends ExchangeContext>>`.
- The `WebExchange` may be exposed in a method parameter using a `WebExchange.Configurer` just like for regular Web client routes.
- Any of `@PathParam`, `@QueryParam`, `@HeaderParam` or `@CookieParam` may be specified as method parameter just like for regular Web client routes.

The WebSocket connection is opened when the inbound publisher returned by the WebSocket client route method is subscribed.

#### WebSocket request attributes

A WebSocket is opened by sending an initial HTTP request to upgrade the protocol. The upgrading request basic attributes are specified in a single `@WebSocketRoute` annotation on a Web client method. It allows to define the path to the WebSocket endpoint relative to the base URI specified in the `@WebClient` annotation, the WebSocket subprotocol, the WebSocket message kind (`TEXT` or `BINARY`) and the accepted language tags.

A basic WebSocket client route connecting to `ws://service/api/v1/chat`, consuming and producing JSON text messages can be declared as follows:

```java
package io.inverno.example.app_web_client;

import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.web.base.annotation.Body;
import io.inverno.mod.web.client.annotation.WebClient;
import io.inverno.mod.web.client.annotation.WebRoute;
import reactor.core.publisher.Mono;

@WebClient( uri = "ws://service/api/v1" )
public interface WebSocketClient {

    @WebSocketRoute( path = "/chat", subprotocol = "json", messageType = WebSocketMessage.Kind.TEXT )
    Flux<Message> chat(Flux<Message> outbound);
}
```

The Web compiler plugin more or less translates above `@WebSocketRoute` annotation into the following:

```java
webClient.exchange(Method.GET, URI.create("ws://service/api/v1/chat"))
    .flatMap(exchange -> exchange.webSocket("json"))
    .doOnNext(wsExchange -> wsExchange.outbound().encodeTextMessages(outbound, Message.class))
    ...
```

The upgrading request can also be configured using regular Web client route parameter bindings such as `@PathParam`, `@QueryParam`, `@HeaderParam` or `@CookieParam`.

```java
@WebSocketRoute( path = "/chat/{room}", subprotocol = "json")
Flux<Message> chatRoom(@PathParam String room, @QueryParam String nickname, Flux<Message> outbound);
```

The upgrading Web exchange can also be exposed using a `WebExchange.Configurer` just like for a regular Web Client route:

```java
@WebSocketRoute( path = "/chat", subprotocol = "json")
Flux<Message> chat(Flux<Message> outbound, WebExchange.Configurer<ApiContext> exchange);
```

#### WebSocket outbound

The WebSocket outbound can be specified as a method parameter as a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>` where `<T>` can be basically a `ByteBuf`, a `String` or any types that can be converted using a media type converter matching the subprotocol.

For instance, a raw message outbound publisher can be declared as follows:

```java
@WebSocketRoute( path = "/raw-messages", messageType = WebSocketMessage.Kind.BINARY)
Flux<ByteBuf> rawMessages(Flux<ByteBuf> outbound);
```

The individual frames composing WebSocket outbound messages can also be sent as follows:

```java
@WebSocketRoute( path = "/raw-messages", messageType = WebSocketMessage.Kind.BINARY)
Flux<ByteBuf> rawMessageFrames(Flux<Flux<ByteBuf>> outbound);
```

When a subprotocol is provided, messages are automatically encoded (and decoded) using a converter matching the subprotocol:

```java
@WebSocketRoute( path = "/chat", subprotocol = "json")
Flux<Message> chat(Flux<Message> outbound);
```

> Note that the subprotocol is normally negotiated with the server during the opening handshake, if the server does not support the requested subprotocol, no subprotocol is included in the handshake response but that doesn't prevent the WebSocket to be opened, it is then up to the WebSocket client to decide whether the connection must be closed which is the HTTP client normal behaviour.

The `BaseWeb2SocketExchange.Outbound` can also be exposed directly in a method parameter using a `Consumer<BaseWeb2SocketExchange.Outbound>` as follows:

```java
@WebSocketRoute( path = "/raw", subprotocol = "json")
Flux<Message> chat2(Consumer<BaseWeb2SocketExchange.Outbound> outbound);
```

It is then possible to explicitly set the outbound:

```java
webClientApp.apiClientV1().chat2(outbound -> outbound
    .closeOnComplete(false)
    .encodeTextMessages(Flux.just("Message 1", "Message 2", "Message 3").map(Message::new), Message.class)
)
...
```

By default, the WebSocket is automatically closed when the outbound publisher terminates, this behaviour is controlled by the `closeOnComplete` attribute in the `@WebSocketRoute` annotation:

```java
@WebSocketRoute(path = "/events", subprotocol = "json", closeOnComplete = false)
Flux<Event> events(Mono<LoginCredentials> outbound);
```

When set to `false`, the WebSocket must be eventually closed explicitly, this can be done by exposing the `Web2SocketExchange` and invoking the `close()` method, but it is also possible to do it by cancelling the subscription to the inbound publisher:

```java
Disposable subscription = webClientApp.apiClientV1().events(Mono.just(new LoginCredentials("user", "password"))).subscribe(event -> {
    // Do something useful with the events...
    ...
});

// Eventually close the WebSocket 
subscription.dispose();
```

#### WebSocket inbound

The WebSocket inbound can be specified in the method's return type as a `Mono<T>`, a `Flux<T>` or more broadly as a `Publisher<T>` where `<T>` can be basically a `ByteBuf`, a `String` or any types that can be converted using a media type converter matching the negotiated subprotocol.

For instance, a `String` message inbound publisher can be declared as follows:

```java
@WebSocketRoute( path = "/string-messages")
Flux<String> stringMessages(Flux<String> outbound);
```

The individual frames composing inbound WebSocket messages can also be received as follows:

```java
@WebSocketRoute( path = "/string-messages")
Flux<Flux<String>> stringMessageFrames(Flux<String> outbound);
```

As for the outbound, inbound messages are automatically decoded using a converter matching the subprotocol:

```java
@WebSocketRoute( path = "/chat", subprotocol = "json")
Flux<Message> chat(Flux<Message> outbound);
```

The `BaseWeb2SocketExchange.Inbound` can also be returned by the method as follows:

```java
@WebSocketRoute( path = "/chat", subprotocol = "json")
Mono<BaseWeb2SocketExchange.Inbound> chat(Flux<Message> outbound);
```

This gives access to the WebSocket inbound, allowing to consume messages explicitly:

```java
webClientApp.apiClientV1().chat3(Flux.just("Message 1", "Message 2", "Message 3").map(Message::new))
    .flatMapMany(inbound -> inbound.decodeTextMessages(Message.class))
    ...
```

#### Exposing the WebSocket exchange

As for declarative Web client routes, most WebSocket client use case should be covered by fully declarative WebSocket client. Specific use cases can still be handled programmatically using the `Web2SocketExchange` which can be conveniently exposed in a WebSocket client route method in a `Web2SocketExchange.configurer` method parameter:

```java
@WebSocketRoute( path = "/chat/{room}", subprotocol = "json")
Flux<Message> chat(Web2SocketExchange.Configurer<ApiContext> wsExchange);
```

This basically allows to initialize the exchange context or set the WebSocket outbound explicitly:

```java
webClientApp.webSocketClient().chat(wsExchange -> {
    wsExchange.context().setApiKey("123456789");
    wsExchange.outbound().encodeTextMessages(Flux.just("Message 1", "Message 2", "Message 3").map(Message::new), Message.class);
});
```

Another way to expose the `Web2SocketExchange` is to return it in the WebSocket client route method:

```java
@WebSocketRoute( path = "/chat", subprotocol = "json")
Mono<Web2SocketExchange<? extends ApiContext>> chat(Flux<Message> outbound);
```

The resulting method implementation then simply returns the exchange instead of the WebSocket inbound which must then be explicitly subscribed to open the WebSocket connection:

```java
webClientApp.webSocketClient().chat4(Flux.just("Message 1", "Message 2", "Message 3").map(Message::new))
    .flatMapMany(wsExchange -> {
        wsExchange.context().setApiKey("123456789");
        return wsExchange.inbound().decodeTextMessages(Message.class);
    });
```

As for regular Web client route, it is possible to specify an exchange context type that will be aggregated by the Inverno Web compiler plugin in a unique exchange context type for the module. Defining a type variable on the route method allows to specify intersection types as well:

```java
@WebSocketRoute( path = "/chat/{room}", subprotocol = "json")
<T extends TracingContext & SecurityContext> Flux<Message> chat(Web2SocketExchange.Configurer<T> wsExchange);
```

or

```java
@WebSocketRoute( path = "/chat", subprotocol = "json")
<T extends TracingContext & SecurityContext> Mono<Web2SocketExchange<T>> chat(Flux<Message> outbound);
```