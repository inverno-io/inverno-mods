[server-sent-events]: https://en.wikipedia.org/wiki/Server-sent_events

# Web Base

The Inverno *web-base* module defines the foundational API for creating Web clients and servers. It also provides common services like a data conversion service based on the converter API.

This module requires a list of `MediaTypeConverter` to be able to convert payloads using the data conversion service. Common converters are provided in the *boot* module. In order to use the Inverno *web-base* module, we then need to define the following dependencies in the module descriptor:

```java
module io.inverno.example.app {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.web.base;
}
```

And also declare that dependency in the build descriptor:

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
            <artifactId>inverno-web-base</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-web-base:${VERSION_INVERNO_MODS}'
```

The *web-base* module is used internally by the *web-client* and *web-server* modules which provide it as a transitive dependency, so it might not be necessary to include it explicitly, and it is not meant to be used out of the context of the Web client and the Web server.

## Web base API

The Web base API defines common annotations in `io.inverno.mod.web.base.annotation` package to create declarative Web clients and Web server controllers. These allow to create declarative Web clients and servers for requesting remote Web resources or exposing Web routes in plain Java. Annotations are used to bind HTTP request and response attributes (path, body, headers, cookies, query parameters...) to methods arguments and return type. The Inverno Web compiler is then used to generate the actual implementations and beans, in case of a Web client a bean is created in the module allowing to send requests using plain Java, in case of a Web server, the routes defined in annotated controller bean are registered in the Web server. Please refer to *web-client* and *web-server* modules respective documentation for detailed descriptions.

## Web data conversion service

The module also provides a `dataConversionService` bean used by both *web-client* and *web-server* modules to convert payloads: request and response bodies as well as WebSocket messages or [Server Sent Event][server-sent-events] data. It basically allows to obtained encoders or decoders from media types (e.g. `application/json`) assuming a supporting raw `MediaTypeConverter<ByteBuf>` have been injected in the module.


