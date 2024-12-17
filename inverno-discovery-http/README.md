[kubernetes-headless-service]: https://kubernetes.io/docs/concepts/services-networking/service/#headless-services

# Discovery HTTP

The Inverno Discovery HTTP module extends the Service Discovery API for HTTP services and provides a DNS based HTTP discovery service bean.

This module requires the `NetService` and the `HttpClient` which are respectively provided by the *boot* module and the *http-client* module, so in order to use the Inverno *discovery-http* module, we need to declare the following dependencies in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.discovery.http {
    requires io.inverno.mod.boot;
    requires io.inverno.mod.http.client;
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
            <artifactId>inverno-http-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-boot:${VERSION_INVERNO_MODS}'
compile 'io.inverno.mod:inverno-http-client:${VERSION_INVERNO_MODS}'
```

## HTTP Service discovery API

The HTTP discovery API extends the discovery API for resolving HTTP services. It basically defines specific `HttpTrafficPolicy`, `HttpDiscoveryService` and `HttpServiceInstance`.

An HTTP service instance is backed by an HTTP client `Endpoint` pointing to an HTTP server exposing the service and where HTTP requests are sent.

### HTTP traffic policy

The `HttpTrafficPolicy` provides optional HTTP client and network configuration which are used when building the HTTP client endpoints. When specified, they override the default configurations provided in the *http-client* module.

It also exposes the `TrafficLoadBalancer.Factory` creating the specific HTTP load balancer used to load balance requests among HTTP service instances.

An HTTP client `Endpoint` exposes two indicators describing its current load: the number of active requests and the load factor which is the ratio between the number of active requests and the defined endpoint capacity. 

The `LeastRequestTrafficLoadBalancer` is a weighted load balancer that uses the active requests indicator to load balance requests to HTTP service instances with the least active requests. When an instance is requested, the resolved HTTP service selects a random set of instances (2 by default), calculates a score for each of them using the following formula and returns the instance with the highest score:

$$score = \frac{weight}{(activeRequests + 1)^{bias}}$$

`weight` is the weight of the instance, `activeRequests` is the number of active requests and `bias` (1 by default) is used to increase the importance of active requests: the greater it is, the more instances with lower active requests count are selected.  

The `MinLoadFactorTrafficLoadBalancer` is a weighted load balancer that uses the load factor indicator to load balance requests to HTTP service instances with the smallest load factor. When an instance is requested, the resolved HTTP service selects a random set of instances (2 by default), calculates a score for each of them using the following formula and returns the instance with the highest score:

$$score = weight\times{}(1 - loadFactor)^{bias}$$

`weight` is the weight of the instance, `loadFactor` is the endpoint load factor and `bias` (1 by default) is used to increase the importance of the load factor: the greater it is, the more instances with lower load factor are selected.

An `HttpTrafficPolicy` overriding HTTP client configuration and creating custom least request load balancer can be built as follows:

```java
HttpTrafficPolicy trafficPolicy = HttpTrafficPolicy.builder()
    .configuration(HttpClientConfigurationLoader.load(configuration -> configuration
        .pool_max_size(3) // Overrides HTTP client configuration
    ))
    .leastRequestLoadBalancer(3, 2) // choose up to 3 instances and set bias to 2
    .build();
```

### HTTP Service instance

The `HttpServiceInstance` exposes the HTTP client endpoint pointing to an HTTP server exposing the service. The `Endpoint` instance is eventually used to process service requests.

> Note that endpoints returned by service instances must not be shut down directly, service instances are managed in an enclosing service which must be used to shut down both service instances and HTTP client endpoints, trying to shut down a service instance endpoint will result in an error.

### HTTP Discovery service

The `HttpDiscoveryService` is using an `HttpTrafficPolicy` for resolving HTTP services pointing to one or more `HttpServiceInstance`.

An HTTP service is resolved and a request processed as follows:

```java
HttpClient httpClient = null;
HttpDiscoveryService discoveryService = null;

String responseBody = discoveryService.resolve(ServiceID.of("http://hostname:8080"), trafficPolicy)
    .flatMap(service -> httpClient.exchange(Method.GET, "/path/to/resource")
        .flatMap(exchange -> service.getInstance(exchange)
            .map(instance -> instance.bind(exchange))
        )
    )
    .flatMap(Exchange::response)
    .flatMapMany(response -> response.body().string().stream())
    .collect(Collectors.joining())
    .block();
```

## DNS HTTP Discovery service

The *http-discovery-http* module exposes the `dnsHttpDiscoveryService` bean that uses the DNS resolution methods provided in the `NetService` in order to resolve the unresolved inet socket address (i.e. `HOSTNAME+PORT`) deduced from a service ID URL (i.e. `http(s)://<HOSTNAME>:<PORT>`) and obtain the set of resolved inet socket addresses (i.e `IP+PORT`) of the servers exposing the service.

> Multiple IP addresses can be associated to a single hostname, this is especially the case with [Kubernetes headless services][kubernetes-headless-service] with multiple replicas.

The DNS discovery service supports `http://`, `https://`, `ws://` and `wss://` schemes, resolved HTTP client endpoints are automatically configured with TLS in the presence of a secured protocol (i.e. `https://` or `wss://`) overriding the configuration provided in both the HTTP client and HTTP traffic policy.

The following code shows how the DNS discovery could be used in a service bean to resolve an API service and consume a REST resource:

```java
package io.inverno.example.discovery.http.sample;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Destroy;
import io.inverno.core.annotation.Init;
import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.http.base.Method;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.UnboundExchange;
import io.inverno.mod.http.client.HttpClient;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;

@Bean
public class SomeService {

    private final HttpClient httpClient;
    private final HttpDiscoveryService dnsHttpDiscoveryService;
    private final HttpTrafficPolicy httpTrafficPolicy;

    private Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> apiService;

    public SomeService(HttpClient httpClient, HttpDiscoveryService dnsHttpDiscoveryService) {
        this.httpClient = httpClient;
        this.dnsHttpDiscoveryService = dnsHttpDiscoveryService;

        this.httpTrafficPolicy = HttpTrafficPolicy.builder().build();
    }

    @Init
    public void init() {
        this.apiService = this.dnsHttpDiscoveryService
            .resolve(ServiceID.of("https://api.example.org"), this.httpTrafficPolicy)
            .block();
    }

    @Destroy
    public void destroy() {
        this.apiService.shutdown().block();
    }

    public Mono<String> get(String id) {
        return httpClient.exchange(Method.GET, "/v1/some/service/" + id)
            .flatMap(exchange -> this.apiService.getInstance(exchange)
                .map(instance -> instance.bind(exchange))
            )
            .flatMap(Exchange::response)
            .flatMapMany(response -> response.body().string().stream())
            .collect(Collectors.joining());
    }
}
```

> Above example is a showcase, not ideally suited for an actual application as it is blocking during the initialization process when resolving the service which is also never refreshed. You might probably prefer using a `CachingDiscoveryService` that caches and silently refreshes services or use the *web-client* module which provides higher level features such as automatic content encoding and manages all these aspects.