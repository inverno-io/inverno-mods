[inverno-javadoc]: https://inverno.io/docs/release/api/index.html

[netty]: https://netty.io/
[form-urlencoded]: https://url.spec.whatwg.org/#application/x-www-form-urlencoded
[rfc-7578]: https://tools.ietf.org/html/rfc7578
[zero-copy]: https://en.wikipedia.org/wiki/Zero-copy
[chunked-transfer-encoding]: https://en.wikipedia.org/wiki/Chunked_transfer_encoding
[epoll]: https://en.wikipedia.org/wiki/Epoll
[kqueue]: https://en.wikipedia.org/wiki/Kqueue
[jdk-providers]: https://docs.oracle.com/en/java/javase/21/security/oracle-providers.html
[server-sent-events]: https://en.wikipedia.org/wiki/Server-sent_events
[mTLS]: https://en.wikipedia.org/wiki/Mutual_authentication

[rfc-7540-8.1.2.4]: https://tools.ietf.org/html/rfc7540#section-8.1.2.4
[rfc-6455]: https://datatracker.ietf.org/doc/html/rfc6455
[rfc-6455-5.4]: https://datatracker.ietf.org/doc/html/rfc6455#section-5.4

# HTTP Server

The Inverno *http-server* module provides a fully reactive HTTP/1.x and HTTP/2 server based on [Netty][netty].

It especially supports:

- HTTP/1.x pipelining
- HTTP/2 over cleartext
- WebSocket
- HTTP Compression
- TLS
- Interceptors
- Strongly typed contexts
- `application/x-www-form-urlencoded` body decoding
- `multipart/form-data` body decoding
- Server-sent events
- Cookies
- zero-copy file transfer when supported for fast resource transfer
- parameter conversion

The server is fully reactive, based on the reactor pattern and non-blocking sockets which means it requires a limited number of threads to supports thousands of connections with high end performances. This design offers multiple advantages starting with maximizing the usage of resources. It is also easy to scale the server up and down by specifying the number of threads we want to allocate to the server, which ideally corresponds to the number of CPU cores. All this makes it a perfect choice for microservices applications running in containers in the cloud.

> This module lays the foundational service and API for building HTTP servers with more complex and advanced features, that is why you might sometimes find it a little bit low level but that is the price of performance. If you require higher level functionalities like request routing, content negotiation and automatic payload conversion please consider the [web server module](#web-server).

This module requires basic services like a [net service](#net-service) and a [resource service](#resource-service) which are usually provided by the *boot* module, so in order to use the Inverno *http-server* module, we should declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_http {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.http.server;
}
```

The *http-base* module which provides base HTTP API and services is composed as a transitive dependency in the *http-server* module and as a result it doesn't need to be specified here nor provided in an enclosing module.

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

```java
...
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-http-server:${VERSION_INVERNO_MODS}'
...
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

The above example starts a HTTP/1.x server using default configuration and a default server controller.

```plaintext
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
     ║                    ,   \/   ,                                  -- ${VERSION_INVERNO_CORE} --                 ║
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
     ║  ...                                                                                       ║
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

The HTTP server uses a **server controller** to handle client request. The module provides a default implementation as overridable bean, a custom server controller can then be injected when creating the *http-server* module.

> this module can also be used to embed a HTTP server in any application, unlike other application frameworks, Inverno core IoC/DI framework is not pervasive and any Inverno modules can be safely used in various contexts and applications.

## Configuration

The first thing we might want to do is to create a configuration in the *app_http* module for easy *http-server* module setup. The HTTP server configuration is actually done in the `BootConfiguration` defined in the *boot* module for low level network configuration and in `HttpServerConfiguration` defined in the *http-server* module for the HTTP server itself. 

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

This should be enough for exposing a configuration in the *app_http* module that let us setup the server: 

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

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties. We can for instance configure low level network settings like TCP keep alive or TCP no delay as well as HTTP related settings like compression or TLS.

> You can also refer to the [configuration module documentation](#configuration-1) to get more details on how configuration works and more especially how you can from here define the HTTP server configuration in command line arguments, property files...

### Logging

The HTTP server can log access and error events at `INFO` and `ERROR` level respectively. They can be disabled by configuring `io.inverno.mod.http.server.Exchange` logger as follows:

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
        <Logger name="io.inverno.mod.http.server.Exchange" additivity="false" level="off"  />

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
        <Logger name="io.inverno.mod.http.server.Exchange" additivity="false" level="info">
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

### Transport

By default, the HTTP server uses the Java NIO transport, but it is possible to use native [epoll][epoll] transport on Linux or [kqueue][kqueue] transport on BSD-like systems for optimized performances. This can be done by adding the corresponding Netty dependencies with the right classifier in the project descriptor:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-transport-classes-epoll</artifactId>
        </dependency>
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
            <artifactId>netty-transport-classes-kqueue</artifactId>
        </dependency>
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
>     requires io.netty.transport.classes.epoll,
>     requires io.netty.transport.epoll.linux.x86_64;
> }
> ```
>
> This approach is fine as long as we are sure the application will run on Linux, but in order to create a properly portable application, we should prefer adding the modules explicitly when running the application:
>
> ```plaintext
> $ java --add-modules io.netty.transport.unix.common,io.netty.transport.classes.epoll,io.netty.transport.epoll.linux.x86_64 ...
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
>                             <vmOptions>--add-modules io.netty.transport.unix.common,io.netty.transport.classes.epoll,io.netty.transport.epoll.linux.x86_64</vmOptions>
>                         </configuration>
>                     </execution>
>                 </executions>
>             </plugin>
>         </plugins>
>     </build>
> </project>
> ```

### HTTP compression

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

`deflate` and `gzip` compression algorithms are supported by default, Zstandard or Brotli support can be added by adding corresponding dependencies to the project, for instance:

```xml
<dependency>
	<groupId>com.aayushatharva.brotli4j</groupId>
	<artifactId>brotli4j</artifactId>
</dependency>
<dependency>
	<groupId>com.aayushatharva.brotli4j</groupId>
	<artifactId>native-linux-x86_64</artifactId>
</dependency>
```

### TLS

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
                        .tls_key_store(URI.create("module://io.inverno.example.app_http/keystore.jks"))
                        .tls_key_alias("selfsigned")
                        .tls_key_store_password("password")
                    )
                )
            )
        ).run();
    }
}
```

The HTTP server can also be configured to support Mutual TLS authentication (mTLS) by specifying a truststore containing the client CA certificate and the client authentication type which must be either `REQUESTED` (the TLS handshake won't fail if the client does not provide authentication) or `REQUIRED` (the TLS exchange will fail if the client does not provide authentication).

```java
public class Main {

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setApp_httpConfiguration(
                App_httpConfigurationLoader.load(configuration -> configuration
                    .http_server(server -> server
                        .server_port(8443)
                        .tls_enabled(true)
                        .tls_key_store(URI.create("module://io.inverno.example.app_http/keystore.jks"))
                        .tls_key_store_password("password")
						.tls_client_auth(HttpServerConfiguration.ClientAuth.REQUESTED)
						.tls_trust_store(URI.create("module://io.inverno.example.app_http/truststore.jks"))
						.tls_trust_store_password("password")
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

## Server Controller

The server controller specifies how exchanges and errors are handled by the server. It also provides the exchange context created and attached to the exchange by the server. 

The `ServerController` interface bascially defines the following methods:

- `Mono<Void> defer(Exchange<ExchangeContext> exchange)` which is used to handle an exchange
- `Mono<Void> defer(ErrorExchange<ExchangeContext> errorExchange)` which is used to handle an error exchange
- `ExchangeContext createContext()` which provides the context attached to an exchange

> Methods `void handle(Exchange<ExchangeContext> exchange)` and `void handle(ErrorExchange<ExchangeContext> errorExchange)` are also defined, they can be more convenient when the handling logic does not have to be reactive. Note that the server will always invoke `defer()` methods which must then be properly implemented.

As stated before, the *http-server* module provides a default `ServerController` implementation which returns `Hello` when a request is made to the root path `/` and (404) not found error otherwise. By default no context is created and `exchange.context()` returns `null`. 

A custom server controller can be injected when creating the *app_http* module. In the following code, a socket bean is defined to inject the custom server controller and starts an HTTP server which responds with `Hello from app_http module!` to any request:

```java
package io.inverno.example.app_http;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ServerController;
import java.util.function.Supplier;

public class Main {

    @Bean
	public static interface Controller extends Supplier<ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>>> {}

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setController(
                exchange -> exchange.response().body().string().value("Hello from app_http module!")
            )
        ).run();
    }
}
```

The `ServerController` interface also exposes static methods to easily create a server controller with custom exchange and error exchange handlers:

```java
package io.inverno.example.app_http;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ServerController;
import java.util.function.Supplier;

public class Main {

    @Bean
	public static interface Controller extends Supplier<ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>>> {}

    public static void main(String[] args) {
        Application.with(new App_http.Builder()
            .setController(
                ServerController.from(
                    exchange -> {
                        exchange.response()
                            .body().string().value("Hello from app_http module!");
                    },
                    errorExchange -> {
                        errorExchange.response()
                            .headers(headers -> headers.status(Status.INTERNAL_SERVER_ERROR))
                            .body().string().value(errorExchange.getError().getMessage());
                    }
                )
            )
        ).run();
    }
}
```

It is also possible to provide a server controller bean in the *app_http* module:

```java
package io.inverno.example.app_http;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ServerController;

@Bean
public class App_httpServerController implements ServerController<App_httpServerController.CustomContext, Exchange<App_httpServerController.CustomContext>, ErrorExchange<App_httpServerController.CustomContext>>{

    @Override
    public void handle(Exchange<CustomContext> exchange) throws HttpException {
        exchange.response().body().string().value("Hello " + exchange.context().getName() + " from app_http module!");
    }

    @Override
    public CustomContext createContext() {
        return new CustomContext();
    }
	
    public static class CustomContext implements ExchangeContext {

        private String name = "anonymous";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
```

This bean is automatically wired to the server controller socket defined by the *http-server* module overriding the default server controller. 

> Note that above implementation still uses the default error handler.

With this approach there is no need for a server controller socket bean and the server can be simply started as before:

```java
package io.inverno.example.app_http;

import io.inverno.core.v1.Application;

public class Main {

    public static void main(String[] args) {
        Application.with(new App_http.Builder()).run();
    }
}
```

```plaintext
2022-07-18 11:12:57,710 INFO  [main] i.i.c.v.Application - Inverno is starting...


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
     ║ Java version        : 17.0.2+8-86                                                          ║
     ║ Java home           : /home/jkuhn/Devel/jdk/jdk-17.0.2                                     ║
     ║                                                                                            ║
     ║ Application module  : io.inverno.example.app_http                                          ║
     ║ Application version : 1.0.0-SNAPSHOT                                                       ║
     ║ Application class   : io.inverno.example.app_http.Main                                     ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ....                                                                                      ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


2022-07-18 11:12:57,713 INFO  [main] i.i.e.a.App_http - Starting Module io.inverno.example.app_http...
2022-07-18 11:12:57,713 INFO  [main] i.i.m.b.Boot - Starting Module io.inverno.mod.boot...
2022-07-18 11:12:57,935 INFO  [main] i.i.m.b.Boot - Module io.inverno.mod.boot started in 221ms
2022-07-18 11:12:57,935 INFO  [main] i.i.m.h.s.Server - Starting Module io.inverno.mod.http.server...
2022-07-18 11:12:57,935 INFO  [main] i.i.m.h.b.Base - Starting Module io.inverno.mod.http.base...
2022-07-18 11:12:57,940 INFO  [main] i.i.m.h.b.Base - Module io.inverno.mod.http.base started in 5ms
2022-07-18 11:12:57,994 INFO  [main] i.i.m.h.s.i.HttpServer - HTTP Server (nio) listening on http://0.0.0.0:8080
2022-07-18 11:12:57,995 INFO  [main] i.i.m.h.s.Server - Module io.inverno.mod.http.server started in 59ms
2022-07-18 11:12:57,995 INFO  [main] i.i.e.a.App_http - Module io.inverno.example.app_http started in 283ms
2022-07-18 11:12:57,998 INFO  [main] i.i.c.v.Application - Application io.inverno.example.app_http started in 333ms
```

Now if we send a request to the server we should get the following response:

```plaintext
$ curl -i http://localhost:8080
HTTP/1.1 200 OK
content-length: 37

Hello anonymous from app_http module!
```

## HTTP Server API

The module defines classes and interfaces to handle HTTP requests sent by a client or errors raised during that process.

As we just saw, a `ServerController` must be provided to handle `Exchange` and `ErrorExchange`. An exchange represents an HTTP communication between a client and a server, it is composed of a `Request`, a `Response` and an `ExchangeContext`. An error exchange is created whenever an error is raised during the normal processing of an exchange and allows to report the error to the client. The API has been designed to be fluent and reactive in order for the request to be *streamed* down to the response.

### Exchange handler

An exchange handler is defined in a server controller and used to handle client-server exchanges. The `ReactiveExchangeHandler` is a functional interface defining method `Mono<Void> defer(Exchange<ExchangeContext> exchange)` which is used to handle server exchanges in a reactive way. It is for instance possible to execute non-blocking operations before actually handling the exchange. 

> Authentication is a typical example of a non-blocking operation that might be executed before handling the request.

> Under the hood, the server first subscribes to the returned `Mono`, when it completes the server then subscribes to the response body data publisher and eventually sends a response to the client.

The `ExchangeHandler` extends the `ReactiveExchangeHandler` with method `void handle(Exchange<ExchangeContext> exchange)` which is more convenient than `defer()` when no non-blocking operation other than the generation of the client response is required.

A basic exchange handler can then be created as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response().body().string().value("Hello, world!");
};
```

The above exchange handler sends a `Hello, world!` message in response to any request.

### Response body

A response body must be sent back to the client in order to terminate the exchange, the API exposes several ways to provide response data and therefore terminate the exchange.

#### Empty

An exchange can be ended with no response body as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response().body().empty();
};
```

#### String

We already saw how to send a single string response but we might also want to send the response in a reactive way as a stream of data in case the entire response payload is not available right away, if it doesn't fit in memory or if we simply want to send a response in multiple parts as soon as they become available (e.g. progressive display).

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response().body().string().stream(Flux.just("Hello", ", world!"));
};
```

#### Raw

Raw data (i.e. bytes) can also be sent in response to a request. As for the string response, the response can be a single byte buffer or a stream of byte buffers:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    Flux<ByteBuf> dataStream = Flux.just(
        Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)),
        Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(", world!", Charsets.DEFAULT))
    );

    exchange.response().body().raw().stream(dataStream);
};
```

> Returned `ByteBuf` are released as soon as they are sent to the client.

#### Resource

A [resource](#resource-api) can be sent in a response body. When possible the server uses low-level ([zero-copy][zero-copy]) API for fast resource transfer.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().resource().value(new FileResource("/path/to/resource"));
};
```

The media type of the resource is resolved using a [media type service](#media-type-service) and automatically set in the response `content-type` header field. 

> If a specific resource is created as in above example the media type service used is the one defined when creating the resource or a default implementation if none was specified. If the resource is obtained with the resource service provided in the *boot* module the media type service used is the one provided in the *boot* module.

#### Server-sent events

[Server-sent events][server-sent-events] provide a way to send server push notifications to a client. It is based on [chunked transfer encoding][chunked-transfer-encoding] over HTTP/1.x and regular streams over HTTP/2. The API provides an easy way to create SSE endpoints.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
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

### Request body

Request body can be handled in a similar way. The reactive API allows to process the payload of a request as the server receives it and therefore progressively build and send the corresponding response.

A request body is however optional as not all HTTP request has a body.

#### String

The request body can be consumed as `CharSequence` as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().string().stream(exchange.request().body()
            .map(body -> Flux.from(body.string().stream()).map(s -> Integer.toString(s.length())))
            .orElse(Flux.just("0"))
        );
};
```

In the above example, if a client sends a payload in the request, the server responds with the number of characters of each string received or it responds `0` if the request payload is empty. As before, request body is processed as a flow of data.

#### Raw

It can also be consumed as raw data as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().raw().stream(exchange.request().body()
            .map(body -> Flux.from(body.raw().stream())
                .map(chunk -> {
                    try {
                        return Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(chunk.readableBytes()));
                    }
                    finally {
                        chunk.release();
                    }
                })
            )
            .orElse(Flux.just(Unpooled.unreleasableBuffer(Unpooled.buffer(4).writeInt(0))))
        );
};
```

In the above example, if a client sends a payload in the request, the server responds with the number of bytes of each chunk of data it receives or it responds `0` if the request payload is empty. This simple example illustrates how we can process requests as flow of data.

> Note that request's `ByteBuf` data must be released when they are consumed in the exchange handler.

#### URL Encoded form

HTML form data are sent in the body of a POST request in the form of key/value pairs encoded in [application/x-www-form-urlencoded format][form-urlencoded]. The resulting list of `Parameter` can be obtained as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().string().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
            .map(parameter -> "Received parameter " + parameter.getName() + " with value " + parameter.getValue())
        );
}
```

In the above example, for each form parameters the server responds with a message describing the parameters it just received. Again this shows that the API is fully reactive and form parameters can be processed as they are decoded.

A more traditional example though would be to obtained the map of parameters grouped by names (because multiple parameters with the same name can be sent):

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().string().stream(Flux.from(exchange.request().body().get().urlEncoded().stream())
        .collectMultimap(Parameter::getName)
            .map(formParameters -> "User selected options: " + formParameters.get("options").stream().map(Parameter::getValue).collect(Collectors.joining(", ")))
        );
}
```

> Here we may think that the aggregation of parameters in a map could *block* the I/O thread but this is actually not true, when a parameter is decoded, the reactive framework is notified and the parameter is stored in a map, after that the I/O thread can be reallocated. When the parameters publisher completes the resulting map is emitted to the mapping function which build the response. During all this process, no thread is ever waiting for anything.

#### Multipart form

A [multipart/form-data][rfc-7578] request can be handled in a similar way. Form parts can be obtained as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .body().string().stream(Flux.from(exchange.request().body().get().multipart().stream())
            .map(part -> "Received part " + part.getName())
        );
};
```

Multipart form data is most commonly used for uploading files over HTTP. Such handler can be implemented as follows using the [resource API](#resource-api) to store uploaded files:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response().body().string().stream(
        Flux.from(exchange.request().body().get().multipart().stream())                                                           // 1 
            .flatMap(part -> part.getFilename()                                                                                   // 2 
                .map(fileName -> Flux.<CharSequence, FileResource>using(                                                          // 3 
                    () -> new FileResource("uploads/" + part.getFilename().get()),                                                // 4 
                    file -> file.write(part.raw().stream()).map(Flux::from).get()                                                 // 5 
                        .reduce(0, (acc, cur) -> acc + cur)
                        .map(size -> "Uploaded " + fileName + "(" + part.headers().getContentType() + "): " + size + " Bytes\n"),  
                    FileResource::close                                                                                           // 6 
                ))
                .orElseThrow(() -> new BadRequestException("Not a file part"))                                                    // 7 
            )
    );
};
```

The above code uses multiple elements and deserves a detailed explanation: 

1. get the stream of parts
2. map the part to the response stream by starting to determine whether the part is a file part
3. if the part is a file part, map the part to the response stream by creating a Flux with a file resource
4. in this case the resource is the target file where the uploaded file will be stored
5. stream the part's payload to the target file resource and eventually provides the response in the form of a message stating that a file with a given size and media type has been uploaded
6. close the file resource when the publisher completes
7. if the part is not a file part respond with a bad request error

The `Flux.using()` construct is the reactive counterpart of a try-with-resource statement. It is interesting to note that the content of the file is streamed up to the file and it is then never entirely loaded in memory. From there, it is quite easy to stop the upload of a file if a given size threshold is exceeded. We can also imagine how we could create a progress bar in a client UI to show the progression of the upload.

> In the above code we uploaded one or more file and stored their content on the local file system and during all that process, the I/O thread was never blocked.

> Note that since part's `ByteBuf` data are consumed by the target file resource, there is no need to release them in the exchange handler.

### Error exchange handler

An error exchange handler is defined in a server controller and used to handle errors raised during the normal processing of an exchange in the exchange handler. 

It is basically an `ExchangeHandler` of `ErrorExchange`. An error exchange exposes the original error, it is then possible to implement different behaviours based on the type of error:

```java
ExchangeHandler<ExchangeContext, ErrorExchange<ExchangeContext>> errorHandler = errorExchange -> {
    if(errorExchange.getError() instanceof BadRequestException) {
        errorExchange.response().body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("client sent an invalid request", Charsets.DEFAULT)));
    }
    else {
        errorExchange.response().body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Unknown server error", Charsets.DEFAULT)));
    }
};
```

### Exchange interceptor

An exchange handler can be intercepted using an `ExchangeInterceptor`. An interceptor can be used to preprocess an exchange in order to check preconditions and potentially respond to the client instead of the handler, initialize a context (tracing, metrics...), decorate the exchange...

The `intercept()` method returns a `Mono` which makes it reactive and allows to invoke non-blocking operations before invoking the handler.

An intercepted exchange handler can be created as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {...};

ExchangeInterceptor<ExchangeContext, Exchange<ExchangeContext>> interceptor = exchange -> {
    LOGGER.info("Path: " + exchange.request().getPath());

    // exchange is returned unchanged and will be processed by the handler
    return Mono.just(exchange);
};

ReactiveExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> interceptedHandler = handler.intercept(interceptor);
```

An interceptor can also end an exchange, in which case it must return an empty `Mono` to stop the exchange handling chain.

```java
ExchangeInterceptor<ExchangeContext, Exchange<ExchangeContext>> interceptor = exchange -> {
    // Check some preconditions...
    if(...) {
        // Do some processing and terminate the exchange
        exchange.response().body().empty();

        // the exchange has been processed by the interceptor and it won't be processed by the handler
        return Mono.empty();
    }
    return Mono.just(exchange);
}
```

Mulitple interceptors can be chained by invoking `intercept()` method mutliple times:

```java
// exchange handling chain: interceptor3 -> interceptor2 -> interceptor1 -> handler
handler.intercept(interceptor1).intercept(interceptor2).intercept(interceptor3);
```

### Exchange context

A strongly typed context is exposed in the `Exchange`, it allows to store or access data and to provide contextual operations throughout the process of the exchange. The server creates the context along with the exchange using the server controller. It is then possible to *customize* the exchange with a specific strongly types context. 

The advantage of this approach is that the compiler can perform static type checking but also to avoid the usage of an untyped map of attributes which is less performant and provides no control over contextual data. Since the developer defines the context type, he can also implement logic inside.

A context can be used to store security information, tracing information, metrics... For instance, if we combine this with exchange interceptors:

```java
ExchangeHandler<SecurityContext, Exchange<SecurityContext>> handler = exchange -> {
    if(exchange.context().isAuthenticated()) {
        exchange.response().body().string().value("Hello, world!");
    }
    else {
        exchange.response().body().empty();
    }
};

ExchangeInterceptor<SecurityContext, Exchange<SecurityContext>> securityInterceptor = exchange -> {
    // Authenticate the request
    if(...) {
        exchange.context().setAuthenticated(true);
    }
    return Mono.just(exchange);
}

ReactiveExchangeHandler<SecurityContext, Exchange<SecurityContext>> interceptedHandler = handler.intercept(securityInterceptor);
```

> The server relies on the `ServerController` in order to create the context. Please refer to the [Server Controller](#server-controller) section which explains this in details and describes how to setup the HTTP server.

### Misc

The API is fluent and mostly self-describing as a result it should be easy to find out how to do something in particular, even so here are some miscellaneous elements

#### Request headers

Request headers can be obtained as string values as follows:

```java
handler = exchange -> {
    // Returns the value of the first occurence of 'some-header' as string or returns null
    String someHeaderValue = exchange.request().headers().get("some-header").orElse(null);
    
    // Returns all 'some-header' values as strings
    List<String> someHeaderValues = exchange.request().headers().getAll("some-header");
    
    // Returns all headers as strings
    List<Map.Entry<String, String>> allHeadersValues = exchange.request().headers().getAll();
};
```

It is also possible to get headers as `Parameter` which allows to easily convert the value using a parameter converter:

```java
handler = exchange -> {
    // Returns the value of the first occurence of 'some-header' as LocalDateTime or returns null
    LocalDateTime someHeaderValue = exchange.request().headers().getParameter("some-header").map(Parameter::asLocalDateTime).orElse(null);
    
    // Returns all 'some-header' values as LocalDateTime
    List<LocalDateTime> someHeaderValues = exchange.request().headers().getAllParameter("some-header").stream().map(Parameter::asLocalDateTime).collect(Collectors.toList());
    
    // Returns all headers as parameters
    List<Parameter> allHeadersParameters = exchange.request().headers().getAllParameter();
};
```

The *http-server* module can also uses the [header service](#http-header-service) provided by the *http-base* module to decode HTTP headers:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    // Returns the decoded 'content-type' header or null
    Headers.ContentType contenType = exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);
    
    String mediaType = contenType.getMediaType();
    Charset charset = contenType.getCharset();
    ...
};
```

> The header service can be extended with custom HTTP `HeaderCodec`. Please refer to [Extending HTTP services](#extending-http-services) and the [http-base module](#http-base) for more information.

#### Query parameters

Query parameters in the request can be obtained as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
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

#### Request cookies

Request cookie can be obtained in a similar way as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    ...
    // get a specific cookie, if there are multiple cookie with the same name, the first one is returned
    int someInteger = exchange.request().headers().cookies().get("some-integer").map(Parameter::asInteger).orElseThrow(() -> new BadRequestException("Missing some-integer"));

    // get all cookies with a given name
    List<Integer> someIntergers = exchange.request().headers().cookies().getAll("some-integer").stream().map(Parameter::asInteger).collect(Collectors.toList());

    // get all cookies
    Map<String, List<CookieParameter>> queryParameters = exchange.request().headers().cookies().getAll();
    ...
};
```

#### Request components

The API also gives access to multiple request related information such as:

- the HTTP method:

```java
exchange.request().getMethod();
```

- the scheme (`http` or `https`):

```java
exchange.request().getScheme();
```

- the authority part of the requested URI (`host` header in HTTP/1.x and `:authority` pseudo-header in HTTP/2):

```java
exchange.request().getAuthority();
```

- the requested path including query string:

```java
exchange.request().getPath();
```

- the absolute path which is the normalized requested path without the query string:

```java
exchange.request().getAbsolutePath();
```

- the `URIBuilder` corresponding to the requested path to build relative paths:

```java
exchange.request().getPathBuilder().path("path/to/child/resource").build();
```

- the query string:

```java
exchange.request().getQuery();
```

- the socket address of the client or last proxy that sent the request:

```java
exchange.request().getRemoteAddress();
```

- the certificates chain sent by the authenticated client:

```java
exchange.request().getRemoteCertificates();
```

> The server must be configured with [mTLS](#tls) support

#### Response status

The response status can be set in the response headers following HTTP/2 specification as defined by [RFC 7540 Section 8.1.2.4][rfc-7540-8.1.2.4].

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .headers(headers -> headers.status(Status.OK))
        .body().raw();
};
```

#### Response headers/trailers

Response headers can be added or set fluently using a configurator as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
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
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.response()
        .trailers(headers -> headers
            .add("some-trailer", "abc")
        )
        .body().raw()...;
};
```

#### Response cookies

Response cookies can be set fluently using a configurator as follows:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
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

## Web Socket

An HTTP exchange can be upgraded to a WebSocket exchange as defined by [RFC 6455][rfc-6455].

The `webSocket()` method exposed on the `Exchange` allows to upgrade to the WebSocket protocol, it returns an optional `WebSocket` which might be empty if the original exchange does not support the upgrade. This is especially the case when using HTTP/2 for which Websocket upgrade is not supported or if the state of the exchange prevents the upgrade (e.g. error exchange). 

The resulting `WebSocket` allows specifying a `WebSocketExchangeHandler` and a default action in case the WebSocket opening handshake fails (e.g. the client did not provide the correct headers for the upgrade...). A WebSocket exchange handler is used to handle the resulting `WebSocketExchange` which exposes WebSocket inbound and outbound data.

In the following example, the original HTTP `Exchange` is upgraded to a `WebSocketExchange` and all inbound frames are sent back to the client. An internal server error (500) is returned if WebSocket upgrade is not supported and a bad request error (400) is returned if the opening handshake failed:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.webSocket()
        .orElseThrow(() -> new InternalServerErrorException("WebSocket not supported"))
        .handler(webSocketExchange -> {
            webSocketExchange.outbound().frames(factory -> webSocketExchange.inbound().frames());
        })
        .or(() -> {
            throw new BadRequestException("Web socket handshake failed");
        });
};
```

It is possible to specify the supported subprotocols when creating the `WebSocket`, an `UnsupportedProtocolException` shall be raised if the subprotocol negotiation fails (i.e. the client requested a protocol that is not supported by the server)

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    // Indicates that the server supports the 'chat' subprotocol
    exchange.webSocket("chat")
        ...
};
```

### WebSocket exchange

The `WebSocketExchange` also exposes:

- the original HTTP request, 

```java
webSocketExchange.request();
```

- the exchange context:

```java
webSocketExchange.context();
```

- the negotiated subprotocol:

```java
webSocketExchange.getSubProtocol();
```

- multiple methods for closing the WebSocket:

```java
webSocketExchange.close(WebSocketStatus.NORMAL_CLOSURE);
webSocketExchange.close((short)1000, "Goodbye!");
```

A WebSocket exchange finalizer can be specified to free resources once the WebSocket is closed:

```java
webSocketExchange.finalizer(Mono.fromRunnable(() -> {
    // Release some resources
    ...
}));
```

The WebSocket protocol is bidirectional and allows sending and receiving data on both ends exposed by `inbound()` and `outbound()` methods in the WebSocket exchange.

### Inbound

In a WebSocket exchange, the `Inbound` exposes the stream of frames received by the server from the client. It allows to consume WebSocket frames (text or binary) or messages (text or binary).

The following handler simply logs incoming frames:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.webSocket()
        .orElseThrow(() -> new InternalServerErrorException())
        .handler(webSocketExchange -> {
            Flux.from(webSocketExchange.inbound().frames()).subscribe(frame -> {
                try {
                    LOGGER.info("Received WebSocket frame: kind = " + frame.getKind() + ", final = " + frame.isFinal() + ", size = " + frame.getBinaryData().readableBytes());
                }
                finally {
                    frame.release();
                }
            });
        });
};
```

As for request body `ByteBuf` data, WebSocket frames are reference counted and they must be released where they are consumed. In previous example, inbound frames are consumed in the handler which must release them.

The WebSocket protocol supports fragmentation as defined by [RFC 6455 Section 5.4][rfc-6455-5.4], a WebSocket message can be fragmented into multiple frames, the final frame being flagged as final to indicate the end of the message. The `Inbound` can handle fragmented WebSocket messages and allows to consume corresponding fragmented data in multiple ways.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.webSocket()
        .orElseThrow(() -> new InternalServerErrorException())
        .handler(webSocketExchange -> {
            Flux.from(webSocketExchange.inbound().messages()).subscribe(message -> {
                // The stream of frames composing the message
                Publisher<WebSocketFrame> frames = message.frames();
            
                // The message data as stream of ByteBuf
                Publisher<ByteBuf> binary = message.binary();
                
                // The message data as stream of String
                Publisher<String> text = message.text();
                
                // Aggregate all fragments into a single ByteBuf
                Mono<ByteBuf> reducedBinary = message.reducedBinary();
                
                // Aggregate all fragments into a single String
                Mono<String> reducedText = message.reducedText();
                
                ...
            });
        });
};
```

> Note that the different publishers in previous example are all variants of the frames publisher, as a result they are exclusive and it is only possible to subscribe once to only one of them.

Unlike WebSocket frames, WebSocket messages are not reference counted, however message fragments, which are basically frames, must be released when consumed as WebSocket frames or `ByteBuf`.

Messages can be filtered by type (text or binary) by invoking `WebSocketExchange.Inbound#textMessages()` and `WebSocketExchange.Inbound#binaryMessages()`.

### Outbound

In a WebSocket exchange, the `Outbound` exposes the stream of frames sent by the server to the client. It allows to specify the stream of WebSocket frames (text or binary) or messages (text or binary) to send to the client. WebSocket frames and messages are created using provided factories.

The following handler simply sends three text frames to the client.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.webSocket()
        .orElseThrow(() -> new InternalServerErrorException())
        .handler(webSocketExchange -> {
            webSocketExchange.outbound()
                .frames(factory -> Flux.just("ONE", "TWO", "THREE").map(factory::text));
        });
}
```

Likewise we can send messages to the client, in the following example three Websocket frames are sent to the client per message: the constant `message:`, the actual message content and an empty final frame which marks the end of the message. Frames and messages publisher are exclusive, only one of them can be specified.

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.webSocket()
        .orElseThrow(() -> new InternalServerErrorException())
        .handler(webSocketExchange -> {
            webSocketExchange.outbound()
                .messages(factory -> Flux.just("ONE", "TWO", "THREE").map(content -> factory.text(Flux.just("message: ", content))));
        });
}
```

By default, a close frame is automatically sent when the outbound publisher terminates. This behaviour can be changed by configuration by setting the `ws_close_on_outbound_complete` parameter to ̀`false` or on the `Outbound` itself using the `closeOnComplete()` method:

```java
ExchangeHandler<ExchangeContext, Exchange<ExchangeContext>> handler = exchange -> {
    exchange.webSocket()
        .orElseThrow(() -> new InternalServerErrorException())
        .handler(webSocketExchange -> {
            webSocketExchange.outbound()
                .closeOnComplete(false)
                .frames(factory -> Flux.just("ONE", "TWO", "THREE").map(factory::text));
        });
}
```

After a close frame has been sent, if the inbound publisher has not been subscribed or if it has terminated, the connection is closed right away, otherwise the server waits up to a configured timeout (`ws_inbound_close_frame_timeout` defaults to 60000ms) for the client to respond with a corresponding close frame before closing the connection.

### A simple chat server

Using the reactive API, a simple chat server can be implemented quite easily. The following exchange handler uses a sink to broadcast the frames received to every connected clients:

```java
package io.inverno.example.app_http_websocket;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.PathResource;
import io.inverno.mod.base.resource.Resource;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.HttpException;
import io.inverno.mod.http.base.ws.WebSocketFrame;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ServerController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Bean
public class ChatServerController implements ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>> {

    private Sinks.Many<WebSocketFrame> chatSink;

    @Init
    public void init() {
        this.chatSink = Sinks.many().multicast().onBackpressureBuffer(16, false);                                                  // 0 
    }

    @Destroy
    public void destroy() {
        this.chatSink.tryEmitComplete();
    }

    @Override
    public void handle(Exchange<ExchangeContext> exchange) throws HttpException {
        exchange.webSocket().ifPresentOrElse(
            websocket -> websocket
                .handler(webSocketExchange -> {
                    Flux.from(webSocketExchange.inbound().frames())                                                                // 1 
                        .subscribe(frame -> {                                                                                      // 2 
                            try {
                                this.chatSink.tryEmitNext(frame);                                                                  // 3 
                            }
                            finally {
                                frame.release();                                                                                   // 4 
                            }
                        });
                    webSocketExchange.outbound().frames(factory -> this.chatSink.asFlux().map(WebSocketFrame::retainedDuplicate)); // 5 
                })
                .or(() -> exchange.response()
                    .body().string().value("Web socket handshake failed")
                ),
            () -> exchange.response()
                .body().string().value("WebSocket not supported")
        );
    }
}
```

0. Create a multicast chat sink with autocancel set to false to broadcast inbound frames to all connected clients.
1. When receiving a new connection, get the inbound frames stream.
2. Subscribe to the inbound frames stream.
3. For each frame received, broadcast the frame using the chat sink.
4. Release the inbound frame.
5. Set the WebSocket outbound using the chat sink: on each frame, retain and duplicate.

As stated before, WebSocket frames are reference counted and inbound WebSocket frames must be released since the handler is the one consuming them. Furthermore for each connected client, the frame must be duplicated, since it is written multiple times, and retained to increment the reference counter, since it must stay in memory until it has been sent to all connected clients.

> This chat server could have been implemented more simply without bothering with reference counting by emitting string data instead of frames in the chat sink. But this would actually be far less optimal as it would involve memory copy. In above solution, the incoming data is never copied into memory, there is only one `ByteBuf` written to all connected client. As always, it is important to find the right balance between performance, simplicity and readability.

## Extending HTTP services

The *http-server* module also defines a socket to plug a custom parameter converter which is a basic `StringConverter` by default. Since we created the *app_http* module by composing *boot* and *http-server* modules, the parameter converter provided by the *boot* module should then override the default. This converter is a `StringCompositeConverter` which can be extended by injecting custom `CompoundDecoder` and/or `CompoundEncoder` instances in the *boot* module as described in the [composite converter documentation](#composite-converter).

The `HeaderService` provided by the *http-basic* module composed in the *http-server* module can also be extended by injecting custom `HeaderCodec` instances used to encode/decode custom HTTP headers.

In practice, all we have to do to extend these services is to provide `HeaderCodec`, `CompoundDecoder` or `CompoundEncoder` beans in the *app_http* module.

## Wrap-up

If we put all we've just seen together, here is a complete example showing how to create a HTTP/2 server with HTTP compression using a custom server controller:

```java
package io.inverno.example.app_http;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.Charsets;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.server.ErrorExchange;
import io.inverno.mod.http.server.Exchange;
import io.inverno.mod.http.server.ServerController;
import io.netty.buffer.Unpooled;
import java.net.URI;
import java.util.function.Supplier;

public class Main {

    @Bean
    public static interface Controller extends Supplier<ServerController<ExchangeContext, Exchange<ExchangeContext>, ErrorExchange<ExchangeContext>>> {}

    public static void main(String[] args) {
        // Starts the server
        Application.run(new App_http.Builder()
            // Setups the server
            .setApp_httpConfiguration(
                    App_httpConfigurationLoader.load(configuration -> configuration
                    .http_server(server -> server
                        // HTTP compression
                        .decompression_enabled(true)
                        .compression_enabled(true)
                        // TLS
                        .server_port(8443)
                        .tls_enabled(true)
                        .tls_key_store(URI.create("module:/keystore.jks"))
                        .tls_key_store_password("password")
                        // Enable HTTP/2
                        .h2_enabled(true)
                    )
                )
            )
            // Sets the server controller
            .setController(ServerController.from(
                exchange -> {
                    exchange.response()
                        .body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello from main!", Charsets.DEFAULT)));
                },
                errorExchange -> {
                    errorExchange.response()
                        .headers(headers -> headers.status(500))
                        .body().raw().value(Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Error: " + errorExchange.getError().getMessage(), Charsets.DEFAULT)));
                }
            ))
        );
    }
}
```

```plaintext
$ curl -i --insecure https://localhost:8443/
HTTP/2 200 
content-length: 16

Hello from main!
```
