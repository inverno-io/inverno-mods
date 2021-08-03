[inverno-io]: https://www.inverno.io
[inverno-dist-root]: https://github.com/inverno-io/inverno-dist
[inverno-core-root]: https://github.com/inverno-io/inverno-core
[inverno-core-root-doc]: https://github.com/inverno-io/inverno-core/tree/master/doc/reference-guide.md
[inverno-mods-root-doc]: https://github.com/inverno-io/inverno-mods/tree/master/doc/reference-guide.md
[inverno-examples-root]: https://github.com/inverno-io/inverno-examples

[apache-license]: https://www.apache.org/licenses/LICENSE-2.0

# Inverno Modules

The [Inverno modules framework][inverno-io] project provides a collection of components for building highly modular and powerful applications on top of the [Inverno IoC/DI framework][inverno-core-root].

While being fully integrated, any of these modules can also be used individually in any application thanks to the high modularity and low footprint offered by the Inverno framework.

The objective is to provide a complete consistent set of high end tools and components for the development of fast and maintainable applications.

## Using a module

Modules can be used in a Inverno module by defining dependencies in the module descriptor. For instance you can create a Web application module using the *boot* and *web* modules:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.webApp {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.web;
}
```

A simple microservice application can then be created in a few lines of code as follows:

```java
import io.inverno.core.annotation.Bean;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.web.annotation.WebController;
import io.inverno.mod.web.annotation.WebRoute;

@Bean
@WebController
public class MainController {

    @WebRoute( path = "/message", produces = MediaTypes.TEXT_PLAIN)
    public String getMessage() {
        return "Hello, world!";
    }

    public static void main(String[] args) {
        Application.with(new WebApp.Builder()).run();
    }
}
```

Please refer to [Inverno distribution][inverno-dist-root] for detailed setup and installation instructions. 

Comprehensive reference documentations are available for [Inverno core][inverno-core-root-doc] and [Inverno modules][inverno-mods-root-doc].

Several example projects showing various features are also available in the [Inverno example project][inverno-examples-root]. They can also be used as templates to start new Inverno application or component projects.

Feel free to report bugs and feature requests in GitHub's issue tracking system if you ran in any issue or wish to see some new functionalities implemented in the framework.

## Available modules

The framework currently provides the following modules.

### inverno-base

The foundational APIs of the Inverno framework modules:

- Conversion API used to convert objects from/to other objects
- Concurrent API defining the reactive threading model API
- Net API providing URI manipulation as well as low level network client and server utilities
- Reflect API for manipulating parameterized type at runtime
- Resource API to read/write any kind of resources (eg. file, zip, jar, classpath, module...)

### inverno-boot

The boot Inverno module provides base services to an application:

- the reactor which defines the reactive threading model of an application
- a net service used for the implementation of optimized network clients and servers
- a media type service used to determine the media type of a resource
- a resource service used to access resources based on URIs
- a basic set of converters to decode/encode JSON, parameters (string to primitives or common types), media types (text/plain, application/json, application/x-ndjson...)
- a worker thread pool used to execute tasks asynchronously
- a JSON reader/writer

### inverno-configuration

Application configuration API providing great customization and configuration features to multiple parts of an application (eg. system configuration, multitenant configuration, user preferences...).

This module also introduces the `.cprops` configuration file format which facilitates the definition of complex parameterized configuration.

In addition, it also provides implementations for multiple configuration sources:

- a command line configuration source used to load configuration from command line arguments
- a map configuration source used to load configuration stored in map in memory
- a system environment configuration source used to load configuration from environment variables
- a system properties configuration source used to load configuration from system properties
- a `.properties` file configuration source used to load configuration stored in a `.properties` file
- a `.cprops` file configuration source used to load configuration stored in a `.cprops` file
- a Redis configuration source used to load/store configuration from/to a Redis data store with supports for configuration versioning
- a composite configuration source used to combine multiple sources with support for smart defaulting
- an application configuration source used to load the system configuration of an application from a set of common configuration sources in a specific order, for instance: command line, system properties, system environment, local `configuration.cprops` file and `configuration.cprops` file resource in the application module

Configurations are defined as simple interfaces in a module which are processed by the Inverno compiler to generate configuration loaders and beans to make them available in an application with no further effort.

### inverno-http-base

The Inverno HTTP base module provides the foundational API as well as common services for HTTP client and server development, in particular an extensible HTTP header service used to decode and encode HTTP headers.

### inverno-http-server

The Inverno HTTP server module provides a fully reactive HTTP/1.x and HTTP/2 server implementation based on Netty. 

It supports the following features:

- SSL
- HTTP compression/decompression
- Server-sent events
- HTTP/2 over cleartext upgrade
- URL encoded form data
- Multipart form data

### inverno-web

The Inverno Web module provides advanced features on top of the HTTP server module, including:

- request routing based on path, path pattern, HTTP method, request and response content negotiation including request and response content type and language of the response.
- path parameters
- transparent payload conversion based on the content type of the request or the response from raw representation (arrays of bytes) to Java objects 
- transparent parameter (path, cookie, header, query...) conversion from string to Java objects
- static resource handler to serve static resources from various location based on the resource API
- a complete set of annotations for easy REST controller development

REST controllers can be easily defined using annotations which are processed by the Inverno compiler to generate the Web server configuration. The compiler also checks that everything is in order as for example that there are no conflicting routes.

### inverno-irt

The Inverno Reactive Template module provides a reactive template engine including:

- reactive, near zero-copy rendering
- statically types template generated by the Inverno compiler at compile time
- pipes for data transformation
- functional syntax inspired from XSLT and Erlang on top of the Java language that perfectly embraces reactive principles

## Building Inverno framework modules

The Inverno framework modules can be built using Maven and Java<=9 with the following command:

```plaintext
$ mvn install
```

## License

The Inverno Framework is released under version 2.0 of the [Apache License][apache-license].

