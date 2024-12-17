# Discovery HTTP Meta services

The Inverno Discovery HTTP meta module defines HTTP meta services which support advanced features such as:

- client-side load balancing: requests are load balanced across multiple destinations using various strategies: round-robin, random, least requests, minimum load factor...
- request rewriting: path can be changed with parameter support, headers can be added, updated or removed in both request and response...
- content based routing: request can be routed to a specific destination based on matching path, method, headers, query parameters...
- service composition: as its name suggest a *meta* service allows to compose multiple services into one service.  
- specific client configuration: HTTP client configurations can be provided per service, per route and/or per destination.

The module provides an HTTP meta discovery service implementation which resolves HTTP meta service descriptors from a configuration source.

This module requires a `ConfigurationSource` where to look for JSON HTTP meta service descriptors, an `ObjectMapper` to parse descriptors and a set of `HttpDiscoveryService` to resolve services composed in the HTTP meta services. A default `ObjectMapper` is provided. However, in a regular Inverno application the one provided in the *boot* module shall be injected. HTTP discovery services can be provided by including dependencies to *discovery-http*, *discovery-http-k8s* or any module providing `HttpDiscoveryService` beans. In order to use the Inverno *discovery-http-meta* module, a basic setup would then require to define the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.discovery.http.k8s {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.discovery.http;
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
            <artifactId>inverno-discovery-http</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-discovery-http:${VERSION_INVERNO_MODS}'
```

Before looking into the details, let's consider simple use cases to better understand the purpose of HTTP meta services.

At its simplest, an HTTP meta service can be used to abstract the service location from the application code which can then simply reference a key to the actual destination stored in a configuration source for instance:

```properties
io.inverno.mod.discovery.http.meta.service.someService = "http://someService"
```

Inside the application we can then just resolve the service using `conf://someService` service ID which will eventually resolve whatever service ID is specified in the configuration.

```java
HttpClient httpClient = ...;
HttpDiscoveryService httpMetaDiscoveryService = ...;
Mono<? extends Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>> service = httpMetaDiscoveryService.resolve(ServiceID.of("conf://someService").cache();

Mono<Strin> responseBody = httpClient.exchange(Method.GET, "/path/to/resource")
    .flatMap(exchange -> service.getInstance(exchange).map(instance -> instance.bind(exchange)))
    .flatMap(Exchange::response)
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining());
```

> The cached service instance could also be refreshed on configuration change. A `CachingDiscoveryService`, which caches and periodically refreshes the service, can also be used.

Now let's say we have a REST API in version `v1` running in some cluster (Docker, Kubernetes or bare metal), a new version `v2` is available, and we would like to gradually shift the traffic from `v1` to `v2`. The HTTP meta service descriptor can now be written as follows to control the traffic shares between `v1` and `v2` destinations with 80% of the requests routed to `someService-v1` and the remaining 20% to `someService-v2`: 

```json
[
    {"uri": "http://someService-v1", "weight": 80},
    {"uri": "http://someService-v2", "weight": 20}
]
```

The resolved service now returns `someService-v1` instances 80% of the time and `someService-v2` instances the rest of the time. The descriptor in the configuration can be updated and the service refreshed (explicitly, periodically, on update...) making it possible to gradually shift the traffic. 

Now let's consider another REST API exposing two resources: `http://service/fruit` and `http://service/vegetable` that we want to refactor in two distinct services: `http://fruit` and `http://vegetable`. This would normally require to refactor any client consuming the original service. Using an HTTP meta service, it is possible to create a service that can route traffic to the right destinations keeping the original API. In that particular case, routing is based on the request path which also have to be rewritten to match the new APIs:

```json
{
    "routes": [
        {
            "path": [
                {"path":"/fruit/**"}
            ],
            "transformRequest": {
                "translatePath":{
                    "/fruit/{path:**}": "/{path:**}"
                }
            },
            "destinations": [
                {"uri": "http://fruit/"}
            ]
        },
        {
            "path": [
                {"path":"/vegetable/**"}
            ],
            "transformRequest": {
                "translatePath":{
                    "/vegetable/{path:**}": "/{path:**}"
                }
            },
            "destinations": [
                {"uri": "http://vegetable"}
            ]
        }
    ]
}
```

A request to `conf://service/fruit/apple` is then routed to `http://fruit/apple` and a request to `conf://service/vegetable/cucumber` to `http://vegetable/cucumber`.

In above examples, destination services (i.e. `http://...`) are resolved using the standard DNS based discovery service initially injected in the *discovery-http-meta* module.

## HTTP meta service descriptor

The `HttpMetaServiceDescriptor` is used to define an HTTP meta service, typically using JSON notation, in a configuration property or any other source supported in an HTTP discovery service implementation.

> The *discovery-http-meta* currently provides a configuration based implementation, but it would be entirely possible to create implementations reading service descriptors from a [Zookeeper](https://zookeeper.apache.org/) cluster or a Kubernetes control plane for instance.

An HTTP meta service is composed of one or more routes targeting one or more destinations. When resolving a service instance, an HTTP meta service selects the route matching the request based on criteria specified in the route before returning a service instance from one of the route's destinations.

The traffic policy can be overridden at the top level. In the following descriptor the traffic policy provided when resolving the service is overridden, including the HTTP client configuration and the traffic load balancer:

```json
{
    "configuration": {
        "http_protocol_versions": ["HTTP_2_0"],
        "user_agent": "Discovery example client"
    },
    "loadBalancer": {
        "strategy": "LEAST_REQUEST",
        "choiceCount": 3,
        "bias": 2
    },
    "routes": [...]
}
```

> The fact that the traffic policy in the descriptor overrides the one provided programmatically might appear counterintuitive, but it is actually a logical choice considering that the traffic policy can also be overridden at route and destination level. If the provided traffic policy were to override the descriptor, it would apply to all routes and destinations policies which is actually less flexible. A good way to look at this is to consider the provided traffic policy to be meant to override the default HTTP client configuration and provide a *preferred* load balancing strategy that can both be overridden in the HTTP meta service descriptor.

### Routes

An HTTP meta service route basically defines a list of rules and a list of destinations. An HTTP meta service can define one or more routes with different rules that are used to match requests based on their content (e.g. method, path, query parameters, headers...) in order to route them to the right destinations.

When resolving a service instance for a given request, routes are evaluated in sequence in the order in which they appear in the service descriptor. A request is matching a route when it matches all rules defined in the route, in which case, a service instance resolved from the list of destinations is returned, otherwise no service instance is returned. 

For instance, considering the following HTTP meta service descriptor, requests matching `/service1/**` are routed to `http://service1` destination, requests to `/service2/**` are routed to `http://service2` destination and all other requests are routed to `http://default`:

```json
{
    "routes": [
        {
            "path": [
                {"path":"/service1/**"}
            ],
            "destinations": [
                {"uri":"http://service1"}
            ]
        },
        {
            "path": [
                {"path":"/service2/**"}
            ],
            "destinations": [
                {"uri":"http://service2"}
            ]
        },
        {
            "destinations": [
                {"uri":"http://default"}
            ]
        }
    ]
}
```

A route with no rules is the default route, it matches any request and as a result it should be defined last.

How destination service instances are selected in a route depends on the route's traffic policy and especially the load balancing strategy. The route traffic policy is obtained by overriding the initial traffic policy with the configuration and load balancer configuration provided at the service level and then at the route level.

In the following descriptor, the traffic policy is overridden globally and in the first route. All service routes are set to use a random load balancing strategy when resolving destination service instances except the first route which uses the least request strategy. The first route is also configured to create HTTP client endpoints with up to 10 connections:

```json
{
    "loadBalancer": {
        "strategy": "RANDOM"
    },
    "routes": [
        {
            "configuration": {
                "http_protocol_versions": ["HTTP_2_0"],
                "pool_max_size": 10
            },
            "loadBalancer": {
                "strategy": "LEAST_REQUEST"
            },
            "path": [
                {"path":"/service1/**"}
            ],
            "destinations":[
                {"uri":"http://service1"}
            ]
        },
        ...
    ]
}
```

> Traffic policies are propagated and overridden top to bottom up to the destinations which use them to resolve the services that process requests.

#### Routing rules

A route can be defined with rules used to match requests based on their content. Different kind of rules can be defined in the route descriptor to match different parts of the request. A request must match all the rules defined in the route to be routed to that route's destinations. A route with no rules matches all request, it should normally appear last in the list of routes.

##### Authority rule

An authority rule is used to match a request authority (`Host` header in HTTP/1.x or `:authority` pseudo header in HTTP/2). It is defined using one or more value matchers allowing to define exact matching values or regular expressions. A request matches an authority rule when it matches any of the value matchers:

```json
{
    "routes": [
        {
            "authority": [
                {"value": "service-1"},
                {"value": "service-2"}
            ],
            "destinations": [
                {"uri": "http://service-1-or-2"}
            ]
        },
        {
            "authority": [
                {"regex": "service-.*"}
            ],
            "destinations": [
                {"uri": "http://service-others"}
            ]
        },
        ...
    ]
}
```

##### Path rule

A path rule is used to match the requested path. It is defined with one or more path matchers which can be defined as static values or path pattern values (see `URIPattern` in *base* module) with or without matching trailing slash. A request matches a path rule when it matches any of the path matchers:

```json
{
    "routes": [
        {
            "path": [
                {"path": "/service/fruit/**"},
                {"path": "/service/vegetable/**"}
            ],
            "destinations": [
                {"uri": "http://service-fruit-or-vegetable"}
            ]
        },
        {
            "path": [
                {"path": "/service/**"}
            ],
            "destinations": [
                {"uri": "http://service-others"}
            ]
        },
        ...
    ]
}
```

In above descriptor, requests to `/service/fruit/apple` or `/service/vergetable/carrot` are routed to `http://service-fruit-or-vegetable/service/fruit/apple` or  `http://service-fruit-or-vegetable/service/vegetable/carrot` respectively.

> Path can be rewritten at route or destination level using a request transformer.

#### Method rule

A method rule is used to match the request method. It is defined as a list of HTTP methods. A request matches a method rule when it matches any of the methods:

```json
{
    "routes": [
        {
            "method": [ "POST", "PUT" ],
            "destinations": [
                {"uri": "http://service-post-or-put"}
            ]
        },
        {
            "method": [ "GET" ],
            "destinations": [
                {"uri": "http://service-get"}
            ]
        },
        ...
    ]
}
```

#### Content type rule

A content type rule is used to match the request content type (defined in `content-type` header). It is defined as a list of media ranges (e.g. `application/json`, `*/xml`...). A request matches a content type rule when its content type matches any of the media ranges:

```json
{
    "routes": [
        {
            "contentType": [ "application/json" ],
            "destinations":[
                {"uri": "http://service-json"}
            ]
        },
        {
            "contentType": [ "*/xml" ],
            "destinations":[
                {"uri": "http://service-xml"}
            ]
        },
        ...
    ]
}
```

#### Accept rule

An accept rule is used to match the request accepted media types (defined in `accept` header). It is defined as a list of media types (e.g. `application/json`). A request matches an accept rule when it accepts one of the media types:

```json
{
    "routes": [
        {
            "accept": [ "application/json" ],
            "destinations":[
                {"uri": "http://service-json"}
            ]
        },
        {
            "accept": [ "application/xml", "text/xml" ],
            "destinations":[
                {"uri": "http://service-xml"}
            ]
        },
        ...
    ]
}
```

#### Language rule

A language rule is used to match the request accepted languages (defined in `language` header). It is defined as a list of language tags (e.g. `en`, `fr-FR`...). A request matches a language rule when it accepts any of the language tags:

```json
{
    "routes": [
        {
            "language": [ "en-US", "en-GB" ],
            "destinations":[
                {"uri": "http://service-en"}
            ]
        },
        {
            "accept": [ "fr" ],
            "destinations":[
                {"uri": "http://service-fr"}
            ]
        },
        ...
    ]
}
```

#### Headers rule

A headers rule is used to match request headers. It is defined as a map of header value matchers. A request matches a header rule when its headers match all entries in the map, that is to say when every headers match any of their corresponding value matchers: 

```json
{
    "routes": [
        {
            "headers": {
                "version": [
                    {"value": "v1"},
                    {"value": "v2"}
                ],
                "environment": [
                    {"value": "prod"}
                ]
            },
            "destinations":[
                {"uri": "http://service-v1-or-v2-and-env-prod"}
            ]
        },
        {
            "headers": {
                "version": [
                    {"regex": "v[3-7]"}
                ],
                "environment": [
                    {"value": "dev"}
                ]
            },
            "destinations":[
                {"uri": "http://service-v3to7-and-env-dev"}
            ]
        },
        ...
    ]
}
```

#### Query parameters rule

A query parameters rule is used to match request query parameters (`?key=value`). It is defined as a map of parameter value matchers. A request matches a query parameter rule when its query parameters match all entries in the map, that is to say when every parameters match any of their corresponding value matchers:

```json
{
    "routes": [
        {
            "queryParameters": {
                "zone": [
                    {"value": "us-east"}
                ],
                "organization": [
                    {"value": "org-1"},
                    {"value": "org-2"}
                ]
            },
            "destinations":[
                {"uri": "http://service-us-east-and-org1-or-org2"}
            ]
        },
        {
            "queryParameters": {
                "zone": [
                    {"value": "eu-west"}
                ],
                "organization": [
                    {"regex": "org-[a-z]*"}
                ]
            },
            "destinations":[
                {"uri": "http://service-eu-west-and-alphabetic-org"}
            ]
        },
        ...
    ]
}
```

### Destinations

A route destination specifies a service where requests matching a route are routed. It essentially comes down to a URI which conveys the targeted service ID URI, a root path and optional query parameters.

For instance, with the following descriptor, a request to `/resource?id=123` with header `organization: org-1` is routed to service `http://service` and its path gets translated to `/path/to/resource?id=123&organization=org-1`:

```json
{
    "routes": [
        {
            "headers": {
                "organization": [ {"value": "org-1"} ]
            },
            "destinations": [
                {
                    "uri": "http://service/path/to?organization=org-1"
                }
            ]
        },
        ...
    ]
}
```

As for services and routes, the traffic policy can be overridden at destination level, the resulting traffic policy is used when resolving the destination service:

```json
{
    "routes": [
        {
            "destinations":[
                {
                    "configuration": {
                        "pool_max_size": 5
                    },
                    "loadBalancer": {
                        "strategy": "LEAST_REQUEST"
                    },
                    "uri": "http://service"
                }
            ]
        },
        ...
    ]
}
```

At least one destination must be defined in a route, this is the usual case where requests are routed to a single service and load balanced among the service instances. There are however cases where multiple destinations can be specified.

For instance, multiple destinations can be defined with different weights in order to gradually shift the traffic to a newer version of the service:

```json
{
    "routes": [
        {
            "destinations": [
                { "uri": "http://service-v1", "weight": 80 },
                { "uri": "http://service-v2", "weight": 20 }
            ]
        }
    ]
}
```

In the same way, A/B testing can be achieved by routing a small part of the traffic to an experimental destination:

```json
{
    "routes": [
        {
            "destinations": [
                { "uri": "http://service", "weight": 90 },
                { "uri": "http://service-test", "weight": 10 }
            ]
        }
    ]
}
```

If a service is deployed on bare metal servers with different capacities, it is possible to assign more traffic to the node that can handle it:

```json
{
    "routes": [
        {
            "destinations": [
                { "uri": "http://big-server", "weight": 3 },
                { "uri": "http://medium-server", "weight": 2 },
                { "uri": "http://small-server", "weight": 1 }
            ]
        }
    ]
}
```

Traffic is load balanced among destinations service instances using the load balancing strategy specified at the route level if and only if all resolved destination services are manageable implementing `ManageableService`. Destination services in an HTTP meta service are resolved using one or more HTTP discovery services which basically determines the kind of services that can be specified in a destination URI (i.e. the supported schemes), resolved services may or may not implement `ManageableService` which exposes the underlying service instances. When they do, an HTTP meta service is then able to *manage* service instances for them and handle load balancing across service instances from multiple destinations, otherwise traffic can only be load balanced among destination services which limits the choice of load balancing strategy. In the presence of an unmanaged service, a route can only use `RANDOM` or `ROUND_ROBIN` load balancing strategies. `MIN_LOAD_FACTOR` or `LEAST_REQUEST` cannot be used because they require access to the service instances to get the load factor or the active request count. Trying to apply an unsupported strategy in the route will result in an `IllegalStateException` being raised. It is however still possible to set up any strategy at destination level.

In the following example, service `conf://unmanaged-service/` is unmanaged, as a result the route can only rely on `RANDOM` or `ROUND_ROBIN` load balancing strategies but the destination itself is set to load balance its requests among its service instances using the `LEAST_REQUEST`:


```json
{
    "routes": [
        {
            "loadBalancer": {
                "strategy": "RANDOM"
            },
            "destinations": [
                {
                    "uri": "http://managed-service", 
                    "weight": 60
                },
                {
                    "loadBalancer": {
                        "strategy": "LEAST_REQUEST"
                    },
                    "uri": "conf://unmanaged-service", 
                    "weight": 40
                }
            ]
        }
    ]
}
```

In above example, 60% of the requests are routed to `http://managed-service` service and the remaining 40% to `conf://unmanaged-service` using `RANDOM` load balancing strategy. Each of these services then internally load balance their respective shares of requests. The `http://managed-service` service inherits the traffic policy from the route so it is implicitly set to use the `RANDOM` load balancing strategy whereas the `conf://unmanaged-service` is explicitly set to use `LEAST_REQUEST` load balancing strategy.

> HTTP meta services are a typical example of unmanageable services since they do not create or use service instances directly, and they are unable to expose service instances by nature.

### Transformers

Transformers can be defined in the HTTP meta service descriptor at route and/or destination level in order to modify the path, the authority or the headers of a request before it is actually sent to an endpoint as well as modifying headers from a received response.

#### Path translation

The path of a request processed in an HTTP meta service can be translated by defining a request transformer. It relies on the `URIPattern` and has the ability to capture parameters in the original path and reference them in the translated path.

In the following example, path matching `/api/{version}/{resource}/{path:**}` are translated to `/{resource}/{version}/{path:**}`:

```json
{
    "routes": [
        {
            "transformRequest": {
                "translatePath":{
                    "/api/{version}/{resource}/{path:**}": "/{resource}/{version}/{path:**}"
                }
            },
            "destinations": [
                { "uri": "http://service" }
            ]
        },
        ...
    ]
}
```

Only paths matching the source pattern are translated. In above example, path `/api/v1/fruits/apple/gala` which is matching `/api/{version}/{resource}/{path:**}` is translated to `fruits/v1/apple/gala` but path `/v1/vegetables/zucchini` is left untouched. When used in conjunction with route path matching rules, not all requests matching a route a path rule will be translated if the transformer source path pattern does not exactly match the rule path pattern (i.e. if the set of paths matched by the route is larger than the set of paths matched by the transformer).

Multiple path translations can be specified in a transformer, they are evaluated in sequence until the request path matches a source pattern.

> Note that the destination URI can also modify the request path, but it only provides a root path and can't be used to rewrite the path.

#### Authority

The request authority, `host` header in HTTP/1.x and `:authority` pseudo-header in HTTP/2, can be set explicitly in a request transformer in order to override the original authority:

```json
{
    "routes": [
        {
            "transformRequest": {
                "authority": "authority.com"
            },
            "destinations": [
                { "uri": "http://service" }
            ]
        },
        ...
    ]
}
```

#### Headers

Request headers can be added, set or removed as follows:

```json
{
    "routes": [
        {
            "transformRequest": {
                "addHeaders": {
                    "some-header": "1234"
                },
                "removeHeaders": [ "variant" ]
            },
            "destinations": [
                {
                    "uri": "http://service-A",
                    "transformRequest": {
                        "setHeaders": {
                            "variant": "a"
                        }
                    },
                    "weight": 80
                },
                {
                    "uri": "http://service-B",
                    "transformRequest": {
                        "setHeaders": {
                            "variant": "b"
                        }
                    },
                    "weight": 20
                }
            ]
        },
        ...
    ]
}
```

Above example illustrates how requests can be flagged per destination for A/B testing monitoring. Header `variant` is removed from the original request at route level and then set again at destination level.

In a similar way, headers can be added, set or removed in a response by defining a response transformer:

```json
{
    "routes": [
        {
            "transformResponse": {
                "setHeaders": {
                    "route": "A"
                }
            },
            "destinations": [
                { 
                    "uri": "http://service",
                    "transformResponse": {
                        "addHeaders": {
                            "destination": "1"
                        }
                    }
                }
            ]
        },
        ...
    ]
}
```

## Configuration HTTP meta discovery service

The *discovery-http-meta* module exposes the `configurationHttpMetaDiscoveryService` bean that resolves HTTP meta services in a configuration source. It supports the `conf://` scheme, an HTTP meta service ID URI conveys the service name used to resolve the descriptor from the configuration source. The configuration property name is obtained by appending the service name to a configuration key prefix used to avoid name collisions, it is defined in `HttpMetaDiscoveryConfiguration#meta_service_configuration_key_prefix()` and defaults to `io.inverno.mod.discovery.http.meta.service`. For instance, the descriptor for service `conf://myService` shall be defined in `io.inverno.mod.discovery.http.meta.service.myService` configuration property in the `ConfigurationSource` injected into the module.

The `configurationHttpMetaDiscoveryService` bean creates a `CompositeDiscoveryService` with the set of HTTP discovery services injected in the module and its own instance to resolve destination services. Any service scheme supported by one of the injected discovery service, with the addition of `conf://`, can then be used in destination URIs.

> Since the configuration discovery service is also included, it is possible to reference HTTP meta services from other HTTP meta services which can be handy in various situations.

The following code shows how the configuration HTTP meta discovery service can be used to resolve and consume a meta service whose descriptor is defined in a configuration source:

```java
package io.inverno.example.discovery.http.k8s.sample;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

@Bean
public class SomeService {

    private final HttpClient httpClient;
    private final HttpDiscoveryService k8sHttpDiscoveryService;
    private final HttpTrafficPolicy httpTrafficPolicy;

    private final HttpClient httpClient;
    private final HttpDiscoveryService configurationHttpMetaDiscoveryService;
    private final HttpTrafficPolicy httpTrafficPolicy;

    public SomeService(HttpClient httpClient, HttpDiscoveryService configurationHttpMetaDiscoveryService) {
        this.httpClient = httpClient;
        this.configurationHttpMetaDiscoveryService = configurationHttpMetaDiscoveryService;
        this.httpTrafficPolicy = HttpTrafficPolicy.builder().build();
    }

    public Mono<String> execute() {
        return this.httpClient.exchange(Method.GET, "/path/to/resource")
            .flatMap(exchange -> this.configurationHttpMetaDiscoveryService.resolve(ServiceID.of("conf://myService"))
                .flatMap(service -> service.getInstance(exchange))
                .map(instance -> instance.bind(exchange))
            )
            .flatMap(Exchange::response)
            .flatMapMany(response -> response.body().string().stream())
            .collect(Collectors.joining());
    }
}
```

Assuming the service is running on port `80` on nodes `http://192.168.1.10`, `http://192.168.1.11` and `http://192.168.1.12`, configuration can then be defined as:

```properties
io.inverno.mod.discovery.http.meta.service.myService = """
{
    "routes": [
        {
            "loadBalancer": {
                "strategy": "ROUND_ROBIN"
            },
            "destinations":[
                { "uri": "http://192.168.1.10", "weight": 80 },
                { "uri": "http://192.168.1.11", "weight": 10 },
                { "uri": "http://192.168.1.12", "weight": 10 }
           ]
        }
    ]
}
"""
```

> In above example, the service is resolved on each request which is not suitable for a real-life application. Several options exist to make it more robust: cache the service using reactor API (e.g. using `cache()` methods) or define a global `CachingDiscoveryService` which also regularly refreshes services. The *web-client* module has been designed to silently take care of these aspects, so unless you need explicit control, it is recommended to use it as a replacement of the HTTP client.