[inverno-javadoc]: https://inverno.io/docs/release/api/index.html

[netty]: https://netty.io/
[form-urlencoded]: https://url.spec.whatwg.org/#application/x-www-form-urlencoded
[rfc-7578]: https://tools.ietf.org/html/rfc7578
[rfc-6265-section41]: https://tools.ietf.org/html/rfc6265#section-4.1
[rfc-6455]: https://datatracker.ietf.org/doc/html/rfc6455
[rfc-6455-section551]: https://datatracker.ietf.org/doc/html/rfc6455#section-5.5.1
[zero-copy]: https://en.wikipedia.org/wiki/Zero-copy
[chunked-transfer-encoding]: https://en.wikipedia.org/wiki/Chunked_transfer_encoding
[epoll]: https://en.wikipedia.org/wiki/Epoll
[kqueue]: https://en.wikipedia.org/wiki/Kqueue
[jdk-providers]: https://docs.oracle.com/en/java/javase/21/security/oracle-providers.html
[alpn]: https://en.wikipedia.org/wiki/Application-Layer_Protocol_Negotiation

# HTTP Client

The Inverno *http-client* module provides a fully reactive HTTP/1.x and HTTP/2 client based on [Netty][netty].

It especially supports:

- HTTP/1.x pipelining
- HTTP/2 over cleartext
- WebSocket
- HTTP Compression
- TLS
- Interceptors
- Strongly typed contexts
- `application/x-www-form-urlencoded` body encoding
- `multipart/form-data` body encoding
- Cookies
- zero-copy file transfer when supported for fast resource transfer
- parameter conversion

The client is fully reactive, based on the reactor pattern and non-blocking sockets which means it requires a limited number of threads to supports thousands of connections with high end performances. Connections are managed per endpoint (i.e. HTTP server) in dedicated pools. It is then easy to create multiple HTTP clients in an application with specific configurations: pool size, timeouts, allocated I/O threads... 

This module requires basic services like a [net service](#net-service) and a [resource service](#resource-service) which are usually provided by the *boot* module, so in order to use the Inverno *http-client* module, we should declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.app_http_client {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.http.client;
}
```

The *http-base* module which provides base HTTP API and services is composed as a transitive dependency in the *http-client* module and as a result it doesn't need to be specified here nor provided in an enclosing module.

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
            <artifactId>inverno-http-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```java
...
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-http-client:${VERSION_INVERNO_MODS}'
...
```

An HTTP client is basically created from the `HttpClient` service exposed in the module and which is used to create the `Endpoint` connecting to the HTTP server:

```java
package io.inverno.example.app_http_client;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.base.Method;
import java.util.stream.Collectors;

public class Main {

	/**
	 * Connects to example.org
	 */
	@Bean
	public static class Example {
		
		private final Endpoint endpoint; 
		
		public Example(HttpClient httpClient) {
			this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
				.build();
		}
		
		public String get(String path) {
			return this.endpoint
				.request(Method.GET, path)                         // Creates the request 
				.send()                                            // Sends the request
				.flatMapMany(exchange -> exchange                  // Streams the response
					.response()
					.body()
					.string().stream()
				)
				.collect(Collectors.joining())                     // Aggregates the response
				.block();
		}
	}

	public static void main(String[] args) {
		Http_client http_client = Application.with(new Http_client.Builder()).run();
		
		String response = http_client.example().get("/");
		System.out.println(response);
		
		http_client.stop();
	}
}
```

In above example, module *app_http_client* creates the `Example` bean which uses the `HttpClient` to obtain an `Endpoint` to connect to `example.org` in plain HTTP. A request to get the server root is then created from the endpoint and sent to the server, the corresponding response is finally displayed to the standard output and the module is stopped.

```plaintext
13:52:32.850 [main] INFO  io.inverno.core.v1.Application - Inverno is starting...


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
     ║ Java version        : 21.0.1+12-29                                                         ║
     ║ Java home           : /home/jkuhn/Devel/jdk/jdk-21.0.1                                     ║
     ║                                                                                            ║
     ║ Application module  : io.inverno.example.app_http_client                                   ║
     ║ Application version : 1.6.0-SNAPSHOT                                                       ║
     ║ Application class   : io.inverno.example.app_http_client.Main                              ║
     ║                                                                                            ║
     ║ Modules             :                                                                      ║
     ║  ...                                                                                       ║
     ╚════════════════════════════════════════════════════════════════════════════════════════════╝


13:52:32.858 [main] INFO  io.inverno.example.app_http_client.App_http_client - Starting Module io.inverno.example.app_http_client...
13:52:32.859 [main] INFO  io.inverno.mod.boot.Boot - Starting Module io.inverno.mod.boot...
13:52:33.073 [main] INFO  io.inverno.mod.boot.Boot - Module io.inverno.mod.boot started in 214ms
13:52:33.074 [main] INFO  io.inverno.mod.http.client.Client - Starting Module io.inverno.mod.http.client...
13:52:33.074 [main] INFO  io.inverno.mod.http.base.Base - Starting Module io.inverno.mod.http.base...
13:52:33.078 [main] INFO  io.inverno.mod.http.base.Base - Module io.inverno.mod.http.base started in 4ms
13:52:33.085 [main] INFO  io.inverno.mod.http.client.Client - Module io.inverno.mod.http.client started in 11ms
13:52:33.099 [main] INFO  io.inverno.example.app_http_client.App_http_client - Module io.inverno.example.app_http_client started in 247ms
13:52:33.100 [main] INFO  io.inverno.core.v1.Application - Application io.inverno.example.app_http_client started in 289ms
13:52:33.361 [inverno-io-nio-1-1] INFO  io.inverno.mod.http.client.internal.AbstractEndpoint - HTTP/1.1 Client (nio) connected to http://example.org:80
<!doctype html>
<html>
<head>
    <title>Example Domain</title>

    <meta charset="utf-8" />
    <meta http-equiv="Content-type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style type="text/css">
    body {
        background-color: #f0f0f2;
        margin: 0;
        padding: 0;
        font-family: -apple-system, system-ui, BlinkMacSystemFont, "Segoe UI", "Open Sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
        
    }
    div {
        width: 600px;
        margin: 5em auto;
        padding: 2em;
        background-color: #fdfdff;
        border-radius: 0.5em;
        box-shadow: 2px 3px 7px 2px rgba(0,0,0,0.02);
    }
    a:link, a:visited {
        color: #38488f;
        text-decoration: none;
    }
    @media (max-width: 700px) {
        div {
            margin: 0 auto;
            width: auto;
        }
    }
    </style>    
</head>

<body>
<div>
    <h1>Example Domain</h1>
    <p>This domain is for use in illustrative examples in documents. You may use this
    domain in literature without prior coordination or asking for permission.</p>
    <p><a href="https://www.iana.org/domains/example">More information...</a></p>
</div>
</body>
</html>

13:52:33.635 [main] INFO  io.inverno.example.app_http_client.App_http_client - Stopping Module io.inverno.example.app_http_client...
13:52:33.638 [main] INFO  io.inverno.mod.boot.Boot - Stopping Module io.inverno.mod.boot...
13:52:33.639 [main] INFO  io.inverno.mod.boot.Boot - Module io.inverno.mod.boot stopped in 0ms
13:52:33.639 [main] INFO  io.inverno.mod.http.client.Client - Stopping Module io.inverno.mod.http.client...
13:52:33.639 [main] INFO  io.inverno.mod.http.base.Base - Stopping Module io.inverno.mod.http.base...
13:52:33.639 [main] INFO  io.inverno.mod.http.base.Base - Module io.inverno.mod.http.base stopped in 0ms
13:52:33.639 [main] INFO  io.inverno.mod.http.client.Client - Module io.inverno.mod.http.client stopped in 0ms
13:52:33.640 [main] INFO  io.inverno.example.app_http_client.App_http_client - Module io.inverno.example.app_http_client stopped in 5ms
13:52:34.049 [Thread-0] INFO  io.inverno.example.app_http_client.App_http_client - Stopping Module io.inverno.example.app_http_client...
13:52:34.050 [Thread-0] INFO  io.inverno.mod.boot.Boot - Stopping Module io.inverno.mod.boot...
13:52:34.051 [Thread-0] INFO  io.inverno.mod.boot.Boot - Module io.inverno.mod.boot stopped in 0ms
13:52:34.051 [Thread-0] INFO  io.inverno.mod.http.client.Client - Stopping Module io.inverno.mod.http.client...
13:52:34.051 [Thread-0] INFO  io.inverno.mod.http.base.Base - Stopping Module io.inverno.mod.http.base...
13:52:34.052 [Thread-0] INFO  io.inverno.mod.http.base.Base - Module io.inverno.mod.http.base stopped in 0ms
13:52:34.052 [Thread-0] INFO  io.inverno.mod.http.client.Client - Module io.inverno.mod.http.client stopped in 0ms
13:52:34.052 [Thread-0] INFO  io.inverno.example.app_http_client.App_http_client - Module io.inverno.example.app_http_client stopped in 2ms
```

## Configuration

The *http-client* module is configured in the `BootConfiguration` defined in the *boot* module for low level client network configuration and in `HttpClientConfiguration` in the *http-client* module for the HTTP client itself. A specific configuration can be created in the application module to easily override the default configurations:

```java
package io.inverno.example.app_http_client;

import io.inverno.core.annotation.NestedBean;
import io.inverno.mod.boot.BootConfiguration;
import io.inverno.mod.configuration.Configuration;
import io.inverno.mod.http.server.HttpServerConfiguration;

@Configuration
public interface App_http_clientConfiguration {

    @NestedBean
    BootConfiguration boot();

    @NestedBean
    HttpClientConfiguration http_client();
}
```

This should be enough for exposing a configuration in the *app_http_client* module that let us setup the client:

```java
package io.inverno.example.app_http_client;

import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Method;
import java.util.Set;
import java.util.stream.Collectors;

public class Main {

	...
	public static void main(String[] args) {
		App_http_client http_client = Application.with(new App_http_client.Builder()
			.setApp_http_clientConfiguration(
				App_http_clientConfigurationLoader.load(configuration -> configuration
                    .http_client(client -> client
                        .http_protocol_versions(Set.of(HttpVersion.HTTP_1_1))
                        .pool_max_size(3)
                    )
                    .boot(boot -> boot
                    	.net_client(netClient -> netClient
                    		.connect_timeout(30000)
                    	)
                        .reactor_event_loop_group_size(4)
                    )
                )
			)
		).run();
		...
	}
}
```

In the above code, we have set: 

- the client to connect using HTTP/1.1 protocol only (default includes both HTTP/1.1 and HTTP/2 which is used first when the server supports it)
- the connection pool max size to 3 for the Endpoint (endpoints will use up to 3 connections to send requests to the HTTP server)
- the low level connection timeout to 30 seconds 
- the number of thread allocated to the reactor core IO event loop group to 4

This configuration is basically used to override the default values globally, these settings are applied for all `Endpoint` instances created with the `HttpClient`. For instance, the connection pool size will then be the same for all endpoints. However if we consider an application creating muliple endpoints to connect to multiple HTTP servers, it is usually usefull to apply different configurations per endpoint. Hopefully specific `HttpClientConfiguration` and `NetClientConfiguration` can also be specified when creating an endpoint:

```java
package io.inverno.example.app_http_client;

...

public class Main {

	@Bean
	public static class Example {

		...		
		public Example(HttpClient httpClient, App_http_clientConfiguration baseConfiguration) {
			this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
				.configuration(
					HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
						.pool_max_size(5)
					)
				)
				.build();
		}
		...
	}
	...	
}
```

In above code, the connection pool max size was set to 5 for `example.org` endpoint overriding the `baseConfiguration` that was set in the application module.

Please refer to the [API documentation][inverno-javadoc] to have an exhaustive description of the different configuration properties. We can for instance configure low level network settings like TCP keep alive or TCP no delay as well as HTTP related settings like compression or TLS.

> You can also refer to the [configuration module documentation](#configuration-1) to get more details on how configuration works and more especially how you can from here define the HTTP client configuration in command line arguments, property files...

### Transport

By default, the HTTP client uses the Java NIO transport, but it is possible to use native [epoll][epoll] transport on Linux or [kqueue][kqueue] transport on BSD-like systems for optimized performances. This can be done by adding the corresponding Netty dependencies with the right classifier in the project descriptor:

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


### HTTP protocol versions

The HTTP client supports HTTP/1.1, HTTP/2 and HTTP/2 over cleartext (H2C) protocols, the protocol choosed by the endpoint to connects to an HTTP server is resolved from the configuration. 

The `http_protocol_versions` parameter defines the set of HTTP protocol versions that the client considers when negotiating the protocol with the server. When supported by both client and server, the HTTP/2 protocol should be always prefered over HTTP/1.1. 

When the `tls_enabled` parameter is enabled, protocol negotiation is done through [ALPN][alpn], the client submits the protocols defined in the `http_protocol_versions` parameter, the server then responds with its prefered protocol version and the connection is established or it rejects the connection if it doesn't support any of the protocol versions proposed by the client (e.g. client only provides HTTP/2 whereas the server only supports HTTP/1.1).

The following configuration allows to create HTTP endpoints using secured HTTP/2.0 or HTTP/1.1 connections:

```java
this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
	.configuration(
		HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
			.tls_enabled(true)
			.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0, HttpVersion.HTTP_1_1))
		)
	)
	.build();
)
```

When the `tls_enabled` parameter is disabled and both HTTP/2 and HTTP/1.1 protocols are defined in `http_protocol_versions` parameter, the client will include HTTP/2 over cleartext upgrade headers in the initial HTTP/1.1 request to upgrade the connection to use HTTP/2 protocol when the server supports it.

The following configuration allows to create HTTP endpoints using HTTP/2.0 over cleartext or HTTP/1.1 connections:

```java
this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
	.configuration(
		HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
			.tls_enabled(false)
			.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0, HttpVersion.HTTP_1_1))
		)
	)
	.build();
)
```

> Note that it is possible to only set HTTP/2 protocol with TLS disabled, in which case the client will communicate with the server directly using the HTTP/2 protocol. Although this might be a valid use case no HTTP servers actually supports it, so you should always provide HTTP/1.1 protocol along with HTTP/2 protocol in `http_protocol_versions` when the connection is not secured. Accepted protocol combinations should include: HTTP/2 over TLS, HTTP/1.1 over TLS, HTTP/2 and HTTP/1.1 over TLS, HTTP/2 and HTTP/1.1 over clear text or HTTP/1.1 over clear text.

### HTTP compression

HTTP compression can be activated by configuration for request and/or response. For instance:

```java
this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
	.configuration(
		HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
			.decompression_enabled(true)
            .compression_enabled(true)
		)
	)
	.build();
)
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

TLS can be enabled by configuration to create secured connections as follows:

```java
this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
	.configuration(
		HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
			.tls_enabled(true)
		)
	)
	.build();
)
```

Untrustworthy connections are discarded by default. For instance, connecting to a server using a self-signed certificate will fail which can become an issue on development or testing environments. 

By default, the client relies on JDK's truststore (`$JAVA_HOME/lib/security/cacerts`) but it can also be configured with a specific trust store:

```java
this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
	.configuration(
		HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
			.tls_enabled(true)
			.tls_trust_store(URI.create("file:/path/to/truststore.p12"))
			.tls_trust_store_type("PKCS12")
			.tls_trust_store_password("password")
		)
	)
	.build();
)
```

It is also possible to trust all certificates which can be convenient in a testing environment:

```java
this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
	.configuration(
		HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
			.tls_enabled(true)
			.tls_trust_all(true)
		)
	)
	.build();
)
```

> A custom `TrustManagerFactory` can also be set in `tls_trust_manager_factory` configuration parameter to fully customize how certificates are verified.

The HTTP client can also be configured to support Mutual TLS authentication (mTLS) by specifying a keystore containing the client certificate and private key (which should then be trusted by the server for the authentication to succeed):


```java
this.endpoint = httpClient.endpoint("example.org", 80) // Creates the endpoint
    .configuration(
        HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
            .tls_enabled(true)
            .tls_key_store(URI.create("module://io.inverno.example.app_http/keystore.jks"))
            .tls_key_store_password("password")
            .tls_trust_store(URI.create("file:/path/to/truststore.p12"))
			.tls_trust_store_password("password")
		)
	)
	.build();
)
```

> When an application using the *http-client* module is packaged as an application image, you'll need to make sure TLS related modules from the JDK are included in the runtime image otherwise TLS might not work. You can refer to the [JDK providers documentation][jdk-providers] in the security developer's guide to find out which modules should be added depending on your needs. Most of the time you'll simply add `jdk.crypto.ec` module in the Inverno Maven plugin configuration:
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

## Endpoint

The `Endpoint` represents the terminal end in an HTTP communication from a client to a server. From the client perspective this is basically a bridge to an HTTP server. It is responsible to establish and manage the connections to a single HTTP server on which HTTP requests are sent by the application.

An application can create many endpoint to connect to many HTTP servers. An `Endpoint` instance is obtained from the `HttpClient` service by specifying the host and the port of the HTTP server:

```java
httpClient.endpoint("example.org", 80) 
	.build();
```

Specific `HttpClientConfiguration` and/or an `NetClientConfiguration` can also be provided when building the endpoint. By default, global modules configurations are applied if none are specified.

```java
HttpClient httpClient = ...
Endpoint endpoint = httpClient.endpoint("example.org", 80) 
	.configuration(
		HttpClientConfigurationLoader.load(baseConfiguration.http_client(), configuration -> configuration
			.tls_enabled(true)
			.http_protocol_versions(Set.of(HttpVersion.HTTP_2_0))
			.request_timeout(30000l)
		)
	)
	.build();
```

An HTTP request can be created and sent fluently from the `Endpoint` instance:

```java
Endpoint endpoint = ...
Flux<String> responseBody = endpoint
	.request(Method.GET, "/")         // 1 
	.send()                           // 2
	.flatMapMany(exchange -> exchange // 3
		.response()
		.body()
		.string().stream()
	);
```

1. Create a `GET` request to get server's root path.
2. Send the request to the server.
3. When the client receives a response from the server the `Exchange` is emitted, exposing both the request and the response. 

> HTTP client and server APIs are built on top of *http-base*  module API and as a result client and server `Exchange` API are very alike and used in a similar way.

It is also possible to create *unbound* HTTP requests from the `HttpClient` in order to send a single request to multiple endpoints:

```java
Endpoint endpoint1 = ...
Endpoint endpoint2 = ...

HttpClient.Request<ExchangeContext, Exchange<ExchangeContext>, InterceptableExchange<ExchangeContext>> request = client.request(Method.GET, "/");

endpoint1.send(request);
endpoint2.send(request);
```

> Note that in case a body publisher is provided in the request, it will be subscribed multiple times (one per actual HTTP request). 

### Request

The `Request` allows to specify headers, cookies and the request body.

#### Request headers

Request headers can be added or set fluently using a configurator as follows:

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/")
	.headers(headers -> headers
		.contentType(MediaTypes.APPLICATION_JSON)
		.add("SomeHeader", "SomeValue")
	);
```

#### Request cookies

Request cookies can be set fluently using a configurator as follows:

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/")
	.headers(headers -> headers
		.cookies(cookies -> cookies
			.addCookie("cookie", "12345")
		)
	);
```

#### Query parameters

Query parameters must be provided in the request target when creating the request, they are later exposed in the exchange as convertible parameters:

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/some/path/id?some-integer=123&some-string=abc")
	.send()
	.map(exchange -> {
		...
		// get a specific query parameter, if there are multiple parameters with the same name, the first one is returned
		Integer someInteger = exchange.request().queryParameters().get("some-integer").map(Parameter::asInteger).orElse(null);

		// get all query parameters with a given name
		List<Integer> someIntergers = exchange.request().queryParameters().getAll("some-integer").stream().map(Parameter::asInteger).collect(Collectors.toList());

		// get all query parameters
		Map<String, List<Parameter>> queryParameters = exchange.request().queryParameters().getAll();
		...
	})
	...
```

> A request can't be parameterized, the request target path specified when creating the request is the one included in the request sent to the server. It is however easy to get around this using a `URIBuilder`:
>
> ```java
> Map<String, ?> values = null;
> endpoint
> 	.request(
> 		Method.GET, 
> 		URIs.uri(
> 			"/some/path/{id}?p1={p1}", 
> 			URIs.Option.NORMALIZED, URIs.Option.PARAMETERIZED
> 		)
> 		.buildString(values)
> 	)
> 	...
> ```

#### Request body

The request body can also be specified fluently before sending the request. Since the HTTP client is fully reactive, the body must be specified as a publisher which is subscribed only when the request is sent to the server.

##### String

A simple string can be set in the request body as follows:

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/some/path")
	.body(body -> body.string().value("Hello world!"))
	.send()
	...
```

The body can also be specified in a reactive way as a publisher of `CharSequence`:

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/some/path")
	.body(body -> body.string().stream(Flux.just("Hello", " ", "world", "!")))
	.send()
	...
```

##### Raw

Raw data (i.e. bytes) can also be sent in the request. As for the string request body, it can be a single byte buffer or a stream of byte buffers:

The body can be specified as a publisher of `ByteBuf`:

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/some/path")
	.body(body -> body.raw().stream(
		Flux.just(
			Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello", Charsets.DEFAULT)),
			Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(" world!", Charsets.DEFAULT))
		)
	))
	.send()
	...
```

> Provided `ByteBuf` are released when they are sent to the server.

##### Resource

A [resource](#resource-api) can be sent in a request body. When possible the client uses low-level ([zero-copy][zero-copy]) API for fast resource transfer.

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/some/path")
	.body(body -> body.resource().value(new FileResource("/path/to/resource")))
	.send()
	...
```

The media type of the resource is resolved using a [media type service](#media-type-service) and automatically set in the request `content-type` header field. 

> If a specific resource is created as in above example the media type service used is the one defined when creating the resource or a default implementation if none was specified. If the resource is obtained with the resource service provided in the *boot* module the media type service used is the one provided in the *boot* module.

##### URL encoded form

HTML form data of the form of key/value pairs encoded in [application/x-www-form-urlencoded format][form-urlencoded] can be sent in the request body of a POST as follows:

```java
Endpoint endpoint = ...
endpoint
	.request(Method.GET, "/some/path")
	// param1=1234&param2=abc
	.body(body -> body.urlEncoded()
		.from((factory, data) -> data.stream(
			Flux.just(
				factory.create("param1", 1234), 
				factory.create("param2", "abc")
			)
		))
	)
	.send()
	...
```

When setting a URL encoded body, the request `content-type` header is automatically set to `application/x-www-form-urlencoded`. Parameter values are automatically converted to string using the `ParameterConverter` set in the *http-client* module and obviously percent-encoded to comply with [application/x-www-form-urlencoded format][form-urlencoded].

##### Multipart form

When the request body must be split into multiple parts of different types or more basically when there is a need to upload one or more files along with other form data, a [Multipart/form-data][rfc-7578] request body can be specified as follows:

```java
endpoint
	.request(Method.POST, "/some/path")
	.body(body -> body.multipart().from((factory, output) -> output.stream(Flux.just(
		factory.string(part -> part
			.name("param1")
			.headers(headers -> headers
				.contentType(MediaTypes.TEXT_PLAIN)
			)
			.value("1234")
		),
		factory.string(part -> part
			.name("param2")
			.headers(headers -> headers
				.contentType(MediaTypes.APPLICATION_JSON)
			)
			.value("{\"value\":123}")
		),
		factory.resource(part -> part
			.name("file")
			.value(new FileResource("/path/to/resource"))
		)
	))))
	.send()
```

The media type of a resource part is resolved using a [media type service](#media-type-service) and automatically set in the part `content-type` header field. If no explicit `filename` parameter has been specified in the part, it is also automatically set to the filename of the resource.

### Response

The `Response` is made available in the `Exchange` after the request has been sent to the server and response headers has been received, it exposes headers, cookies and the response body.

#### Response headers/trailers

Response headers can be obtained as string values as follows:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.map(exchange -> {
		// Returns the value of the first occurence of 'some-header' as string or returns null
		String someHeaderValue = exchange.response().headers().get("some-header").orElse(null);

		// Returns all 'some-header' values as strings
		List<String> someHeaderValues = exchange.response().headers().getAll("some-header");

		// Returns all headers as strings
		List<Map.Entry<String, String>> allHeadersValues = exchange.response().headers().getAll();
		
		...
	});
```

It is also possible to get headers as `Parameter` which allows to easily convert the value using a parameter converter:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.map(exchange -> {
		// Returns the value of the first occurence of 'some-header' as LocalDateTime or returns null
		LocalDateTime someHeaderValue = exchange.response().headers().getParameter("some-header").map(Parameter::asLocalDateTime).orElse(null);

		// Returns all 'some-header' values as LocalDateTime
		List<LocalDateTime> someHeaderValues = exchange.response().headers().getAllParameter("some-header").stream().map(Parameter::asLocalDateTime).collect(Collectors.toList());

		// Returns all headers as parameters
		List<Parameter> allHeadersParameters = exchange.response().headers().getAllParameter();
		
		...
	});
```

The *http-client* module can also uses the [header service](#http-header-service) provided by the *http-base* module to decode HTTP headers:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.map(exchange -> {
		// Returns the decoded 'content-type' header or null
		Headers.ContentType contenType = exchange.request().headers().<Headers.ContentType>getHeader(Headers.NAME_CONTENT_TYPE).orElse(null);

		String mediaType = contenType.getMediaType();
		Charset charset = contenType.getCharset();

		...
	});
```

> The header service can be extended with custom HTTP `HeaderCodec`. Please refer to [Extending HTTP services](#extending-http-services) and the [http-base module](#http-base) for more information.

#### Response status

The response status is exposed as part of the headers:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.map(exchange -> {
		if(exchange.response().headers().getStatus().getCode() >= 400) {
			// Report Error
			...
		}
		...		
	});
```

#### Response cookies

Response cookies can be obtained as convertible `Parameter` from the headers as follows:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.map(exchange -> {
		// Returns the value of the first occurence of 'some-cookie' as LocalDateTime or returns null
		LocalDateTime someCookieValue = exchange.response().headers().cookies().get("some-cookie").map(Parameter::asLocalDateTime).orElse(null);

		// Returns all 'some-cookie' values as LocalDateTime
		List<LocalDateTime> someCookieValues = exchange.response().headers().cookies().getAll("some-cookie").stream().map(Parameter::asLocalDateTime).collect(Collectors.toList());

		// Returns all cookies as set-cookie parameters
		Map<String, List<SetCookieParameter>> allCookieParameters = exchange.response().headers().cookies().getAll();
		
		...
	});
```

`SetCookieParameter` extends both `SetCookie` and `Parameter` so as to also expose `set-cookie` header attributes as defined by [RFC 6265 Section 4.1][rfc-6265-section41]:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.map(exchange -> {
		SetCookieParameter someCookie = exchange.response().headers().cookies().get("some-cookie").orElse(null);

		ZonedDateTime expires = someCookie.getExpires();
		int maxAge = someCookie.getMaxAge();
		String domain = someCookie.getDomain();
		String path = someCookie.getPath();
		boolean isSecure = someCookie.isSecure();
		boolean isHttp = someCookie.isHttpOnly();
		Headers.SetCookie.SameSitePolicy sameSitePolicy = someCookie.getSameSite();
				
		...
	});
```

#### Response body

The response body is exposed in a reactive way which allows to process it while it is being received by the client. 

##### string

The response body can be consumed as `CharSequence` as follows:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.flatMapMany(exchange -> exchange.response().body().string().stream())
	.subscribe(chunk -> {
		// Do something usefull with the payload
		...
	});
```

In above example, response body string publisher is streamed from the exchange, the resulting flow of data (server can decide to respond with multiple chunk of data) is then subscribed and the payload processed as it is received.

##### raw

The response body can also be consumed as raw data as follows:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.flatMapMany(exchange -> exchange.response().body().raw().stream())
	.subscribe((ByteBuf chunk) -> {
		try {
			// Do something usefull with the payload
			...
		}
		finally {
			chunk.release();
		}
	});
```

When response payload is consumed as a flow of `ByteBuf`, it is the responsability of the subscriber to release chunks in order to avoid memory leaks.

### Exchange interceptor

The request can be intercepted using an `ExchangeInterceptor`. This can be used to preprocess a request before it is sent or the response before the exchange is emitted in the publisher returned by the `send()` method. For instance, it is then possible to add some security headers to the request, initialize some context (tracing, metrics...) or decorate the request and response bodies.

The `intercept()` method returns a `Mono` which makes it reactive and allows to invoke non-blocking operations before the request is actually sent.

The following code shows how to intercept a request in order to log request and response bodies:

```java
endpoint
	.request(Method.POST, "/some/path")
	.intercept(exchange -> {
		final StringBuilder requestBodyBuilder = new StringBuilder();
		exchange.request().body().ifPresent(body -> body.transform(data -> Flux.from(data)
			.doOnNext(buf -> requestBodyBuilder.append(buf.toString(Charsets.UTF_8)))
			.doOnComplete(() -> System.out.println("Request Body: \n" + requestBodyBuilder.toString()))
		));

		final StringBuilder responseBodyBuilder = new StringBuilder();
		exchange.response().body().transform(data -> Flux.from(data)
			.doOnNext(buf -> responseBodyBuilder.append(buf.toString(Charsets.UTF_8)))
			.doOnComplete(() -> System.out.println("Response Body: \n" + responseBodyBuilder.toString()))
		);

		return Mono.just(exchange);
	})
	.body(body -> body.string().value("This is an example"))
	.send()
	...
```

An interceptor also allows to abort sending the actual HTTP request by returning an empty publisher in which case the current interceptable exchange is emitted.

The following example shows how to abort the HTTP request and return a response programmatically:

```java
endpoint
	.request(Method.GET, "/some/path")
	.intercept(exchange -> {
		exchange.response()
			.headers(headers -> headers
				.status(Status.OK)
			)
			.body()
				.string().value("You have been intercepted");
		
		return Mono.empty();
	})
	.send()
	...
```

Mulitple interceptors can be chained by invoking `intercept()` method mutliple times:

```java
endpoint
	.request(Method.GET, "/some/path")
	.intercept(interceptor1)
	.intercept(interceptor2)
	.intercept(interceptor3)
	.send()
	...
```

### Exchange context

A strongly typed context is exposed in the `Exchange`, it allows to store or access data and to provide contextual operations throughout the process of the exchange. Such context can be provided when creating the request.

For instance, it is possible to propagate a security context from an `ExchangeInterceptor`:

```java
SecurityContext context = ...
endpoint
	.request(Method.GET, path, context)
	.intercept(exchange -> {
		exchange.request().headers(headers -> headers
			.add(Headers.NAME_AUTHORIZATION, "Bearer " + exchange.context().getToken())
		);
		return Mono.just(exchange);
	})
	.send()
	...
```

> Above code is a simple example, in an actual application interceptors would probably be defined in a separate component and applied to multiple requests.

The advantage of using strongly types context is that the compiler can perform static type checking but also to avoid the usage of an untyped map of attributes which is less performant and provides no control over contextual data. Since the developer defines the context type, it can also expose specific logic.

### Connection pool

The *http-client* module provides an `Endpoint` implementation based on a connection pool which allows it to be resilient, stale connections being automatically recreated, and to scale up and down automatically based on application workload.

An HTTP connection is only created when a request is sent on an `Endpoint` and no connection is currently available to process that request. This is especially the case when there is no active connection in the pool (e.g. when the endpoint has just been created) are full (i.e. max concurrent requests threshold has been reached). The pooled endpoint can create up to a certain amount of connections before buffering requests also up to a certain threshold above which a `ConnectionPoolException` is finally thrown. When the workload decreases, the endpoint parks unnecessary connections and eventually close them when they time out, this allows to put them back in the active pool in case new requests are issued.

The endpoint connection pool behaviour is controlled by configuration with the following parameters:

- **pool_max_size**: the maximum number of connections that can exist in a pool (defaults to 2).
- **pool_clean_period**: the frequency at which the pool parks unecessary connections or closes timed out connection (defaults to 1000ms).
- **pool_buffer_size**: the size of the request buffer used to buffer requests when all connections in the pool are full (defaults to `null` for no limit).
- **pool_keep_alive_timeout**: how much time the pool needs to keep a parked connection in the pool before actually closing it (defaults to 60000ms).
- **pool_connect_timeout**: how much time to wait for the pool to return a connection when a request is sent before raising a `ConnectionTimeoutException`.
- **http1_max_concurrent_requests** and **http2_max_concurrent_streams**: the maximum number of concurrent requests a connection can handle (pipelining for HTTP/1.1 connections and concurrent streams for HTTP/2 connections).

> Note that HTTP client configuration **pool_connect_timeout** parameter should not be confused with Net configuration **connect_timeout** parameter which specify the connection timeout at network level (i.e. used when the client tries to open the socket to the server).

At any moment, the endpoint will try its best to optimize connection usage by distributing request to the least loaded connection and only create connection when necessary. It also has the ability to reinstate parked connections which are still active if the workload demands it.

### Error handling

Http client errors such as connection errors, timeout errors or any Http client related errors are raised by the exchange `Mono` returned by the `send()` method, they can then be handled as for any error on a reactive stream:

```java 
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.flatMapMany(exchange -> exchange.response().body().string().stream())
	.collect(Collectors.joining())
	.subscribe(
		body -> {
		
		},
		error -> LOGGER.error("There was an error requesting the server", error)
	);
```

Being reactive allows interesting constructs, such as retries on error:

```java
endpoint
	.request(Method.GET, "/some/path")
	.send()
	.retry(5)
	...
```

## WebSocket

The *http-client* module allows to open WebSocket connections as defined by [RFC 6455][rfc-6455]. The `webSocketRequest()` method on the `Endpoint` is used to create a specific HTTP request sent to upgrade an HTTP/1.1 connection to the WebSocket protocol.

A WebSocket connection can then be created as follows:

```java
endpoint
	.webSocketRequest("/some/path/ws")
	.send()
	.subscribe(webSocketExchange -> {
		// write to outbound and read from inbound...
		...
	});
```

In case the server does not support or accept the upgrade a `WebSocketClientHandshakeException` is raised in the publisher returned in the `send()` method otherwise a `WebSocketExchange` is emitted exposing inbound and outbound frames or messages.

> Note that a WebSocket connection lives outside of the connection pool managed by the endpoint and as such doesn't count in pool's capacity. It is closed as soon as the WebSocket is closed either by the client or the server.

As for a regular HTTP request, headers and cookies can be specified as follows:

```java
endpoint
	.webSocketRequest("/some/path/ws")
	.headers(headers -> headers
		.add("some-header", "value")
		.cookies(cookies -> cookies.addCookie("some-cookie", 123))
	)
	.send()
	.subscribe(webSocketExchange -> {
		...
	});
```

The WebSocket request can also specify the subprotocol to use by both client and server to communicate:

```java
endpoint
	.webSocketRequest("/some/path/ws")
	.subProtocol("xml")
	.send()
	.subscribe(webSocketExchange -> {
		...
	});
```

The upgrading HTTP request basically contains the requested subprotocol which must be supported by the server for the connection to be established. The WebSocket handshake eventually fails if the server doesn't support it and a `WebSocketClientHandshakeException` shall be raised.

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

In a WebSocket exchange, the `Inbound` exposes the stream of frames received by the client from the server. It allows to consume WebSocket frames (text or binary) or messages (text or binary).

The following example shows how to logs every incoming frames:

```java
endpoint
	.webSocketRequest("/some/path/ws")
	.send()
	.flatMapMany(wsExchange -> wsExchange.inbound().frames())
	.subscribe(frame -> {
		try {
			LOGGER.info("Received WebSocket frame: kind = " + frame.getKind() + ", final = " + frame.isFinal() + ", size = " + frame.getBinaryData().readableBytes());					
		}
		finally {
			frame.release();
		}
	});
```

As for response body `ByteBuf` data, WebSocket frames are reference counted and they must be released where they are consumed. In previous example, inbound frames are consumed in the subscriber which must then release them.

The WebSocket protocol supports fragmentation as defined by [RFC 6455 Section 5.4][rfc-6455-5.4], a WebSocket message can be fragmented into multiple frames, the final frame being flagged as final to indicate the end of the message. The `Inbound` can handle fragmented WebSocket messages and allows to consume corresponding fragmented data in multiple ways.

```java
endpoint
	.webSocketRequest("/some/path/ws")
	.send()
	.flatMapMany(wsExchange -> wsExchange.inbound().messages())
	.flatMap(message -> {
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
```

> Note that the different publishers in previous example are all variants of the frames publisher, as a result they are exclusive and it is only possible to subscribe once to only one of them.

Unlike WebSocket frames, WebSocket messages are not reference counted, however message fragments, which are basically frames, must be released when consumed as WebSocket frames or `ByteBuf`.

Messages can be filtered by type (text or binary) by invoking `WebSocketExchange.Inbound#textMessages()` and `WebSocketExchange.Inbound#binaryMessages()`.

### Outbound

In a WebSocket exchange, the `Outbound` exposes the stream of frames sent by the client to the server. It allows to specify the stream of WebSocket frames (text or binary) or messages (text or binary) to send. WebSocket frames and messages are created using provided factories.

In the following example, a sink is used to create the frame publisher specified in the WebSocket outbound:

```java
Sinks.Many<String> framesSink = Sinks.many().unicast().onBackpressureBuffer();
endpoint
	.webSocketRequest("/some/path/ws")
	.send()
	.flatMapMany(wsExchange -> {
		wsExchange.outbound()
			.frames(factory -> framesSink.asFlux().map(factory::text));
		
		return wsExchange.inbound().frames();
	})
	.subscribe(frame -> {
		try {
			LOGGER.info("Received WebSocket frame: kind = " + frame.getKind() + ", final = " + frame.isFinal() + ", size = " + frame.getBinaryData().readableBytes());					
		}
		finally {
			frame.release();
		}
	});

framesSink.tryEmitNext("test1");
framesSink.tryEmitNext("test2");
framesSink.tryEmitNext("test3");
framesSink.tryEmitComplete();
```

Likewise, a message publisher can be specfied to send messages composed of multiple frames. Frames and messages publisher are exclusive, only one of them can be specified.

```java
Sinks.Many<List<String>> messagesSink = Sinks.many().unicast().onBackpressureBuffer();
endpoint
	.webSocketRequest("/some/path/ws")
	.send()
	.flatMapMany(wsExchange -> {
		wsExchange.outbound().closeOnComplete(true)
			.messages(factory -> messagesSink.asFlux().map(Flux::fromIterable).map(factory::text));
		
		return wsExchange.inbound().messages();
	})
	.flatMap(WebSocketMessage::reducedText)
	.subscribe(message -> {
		LOGGER.info("Received WebSocket message: {}", message );
	});

messagesSink.tryEmitNext(List.of("One frame"));
messagesSink.tryEmitNext(List.of("Multiple ", "frames"));
messagesSink.tryEmitComplete();
```

By default, a close frame is automatically sent when the outbound publisher terminates. This behaviour can be changed by configuration by setting the `ws_close_on_outbound_complete` parameter to ̀`false` or on the `Outbound` itself using the `closeOnComplete()` method:

```java
Sinks.Many<String> framesSink = Sinks.many().unicast().onBackpressureBuffer();
endpoint
	.webSocketRequest("/some/path/ws")
	.send()
	.flatMapMany(wsExchange -> {
		wsExchange.outbound()
			.closeOnComplete(false)
			.frames(factory -> framesSink.asFlux().map(factory::text));
		
		return wsExchange.inbound().frames();
	})
	...
```

After a close frame has been sent, if the inbound publisher has not been subscribed or if it has terminated, the connection is closed right away, otherwise the client waits up to a configured timeout (`ws_inbound_close_frame_timeout` defaults to 60000ms) for the server to respond with a corresponding close frame before closing the connection. This allows the server to properly close the WebSocket as defined by [RFC 6455 Section 5.5.1][rfc-6455-section551].

## Extending HTTP services

The *http-client* module also defines a socket to plug a custom parameter converter which is a basic `StringConverter` by default. Since we created the *app_http_client* module by composing *boot* and *http-client* modules, the parameter converter provided by the *boot* module should then override the default. This converter is a `StringCompositeConverter` which can be extended by injecting custom `CompoundDecoder` and/or `CompoundEncoder` instances in the *boot* module as described in the [composite converter documentation](#composite-converter).

The `HeaderService` provided by the *http-basic* module composed in the *http-client* module can also be extended by injecting custom `HeaderCodec` instances used to encode/decode custom HTTP headers.

In practice, all we have to do to extend these services is to provide `HeaderCodec`, `CompoundDecoder` or `CompoundEncoder` beans in the *app_http_client* module.


