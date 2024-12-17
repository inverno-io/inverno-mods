[kubernetes]: https://kubernetes.io/
[kubernetes-headless-service]: https://kubernetes.io/docs/concepts/services-networking/service/#headless-services

# Discovery

The Inverno Discovery module specifies a service discovery API for resolving service instances from a service identifier in the form of a URI.

In a distributed architecture a service is typically served by one or more servers to ensure redundancy and horizontal scalability. A service instance, as the name suggest, represents an instance of a service, it is used within an application to process service requests on a remote server. For instance, an HTTP service instance typically exposes exactly one HTTP endpoint used to send HTTP request to an HTTP server hosting the service.

In order to implement the discovery API for resolving specific service instances we need to declare a dependency to the API in the module descriptor:

```java
module io.inverno.example.discovery.impl {
    requires io.inverno.mod.discovery;
}
```

And also declare this dependency in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-discovery</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-discovery:${VERSION_INVERNO_MODS}'
```

The Inverno framework provides several modules implementing the discovery API resolving different services in various ways:

- the *discovery HTTP* module defines the HTTP discovery API and provides an HTTP discovery service using simple DNS resolution.
- the *discovery HTTP Kubernetes* module provides an HTTP discovery service resolving HTTP instances from environment variables defined for Kubernetes services in pods containers.
- the *discovery HTTP meta* module defines HTTP meta service which supports advanced features such as client-side load balancing, request routing, request rewriting... and provides an HTTP discovery service resolving meta HTTP service descriptors from a configuration source.

## Service discovery API

The discovery API defines the `DiscoveryService` interface which is used to resolve a `Service` from a `ServiceId` and a `TrafficPolicy`. A `Service` exposes one or more `ServiceInstance` eventually used to execute a service request. The `TrafficPolicy`, when applicable, can be used by a service to specify specific configuration for connecting to the service instances and/or to specify how instances are selected for executing a particular request. For that purpose, a `TrafficLoadBalancer` is obtained from the traffic policy in order to load balance the set of service instances.

### Service ID

A service is uniquely identified by a `ServiceID` which comes down to a URI respecting the following rules:

- It must be absolute (i.e. it must have a scheme).
- If the URI is hierarchical (i.e. its scheme-specific-part starts with a `/`) it must define an authority component.
- it doesn't define a path component, a query component or a fragment component.

When creating a service ID from a URI, the path, query or fragment components are ignored to only keep the scheme and the scheme-specific-part for opaque URIs and the scheme and the authority for hierarchical URIs.

The scheme designates the type of service, it can be a well known network service protocol such as `http://`, `ftp://`... in which case it is used by both the discovery service for resolving the service and the service itself for creating the service instances. The scheme can also designate a logical service such as `conf://` or `k8s://`, this is more intended to be used by a discovery service resolving logical services which compose one or more network services.

```java
ServiceID httpServiceID1 = ServiceID.of("http://example.org");
ServiceID httpServiceID2 = ServiceID.of("http://example.org/some/path"); // equals to httpServiceID1 since path is ignored

ServiceID confService = ServiceID.of("conf://servicename");
```

Let's consider a sample network service using the sample protocol. A sample service URI can then be defined as a hierarchical URI with the `sample` scheme, an authority a host and a port.

```java
ServiceID httpServiceID1 = ServiceID.of("sample://host:1234");
```

### Service instance and traffic policy

A service typically designates any network service exposed on one or more servers and accepting requests in a particular protocol, but it can also represent a logical construction of one or more such services (e.g. HTTP meta services). When implementing the discovery API for resolving a particular type of service, specific `ServiceInstance` and `TrafficPolicy` shall be defined. From there, multiple `DiscoveryService` implementations can be provided.

In order to implement sample service discovery we also must have a sample client module that we can use to connect to the sample servers where to send requests using the sample protocol. When resolving a sample service, the service instances provided in the resolved service uses that client module to connect to the particular server instance and send requests.

When establishing a connection, the server, and therefore the client, might require some configuration like for instance credentials, certificates, some specific protocol configuration like timeouts or maximum concurrent requests... Again this configuration is specific to the sample service, it can be either resolved in the sample service where instances are created, in the traffic policy specified when resolving the service or both. 

A service can be served by multiple instances resolved inside the service which uses the traffic policy to obtain a `TrafficLoadBalancer` used when executing a request to select one instance to send it to. The traffic policy then also defines how requests should be load balanced among the service instances. The load balancing strategy can be service-agnostic like basic random or round-robin strategies, or specific to a service when it uses specific information only exposed by a specific instance (e.g. load factor, number of active requests...).

We then need to create a specific `ServiceInstance` and `TrafficPolicy` instances for the sample service:

```java
package io.inverno.example.discovery.sample;

import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.ServiceInstance;
import java.net.InetSocketAddress;
import reactor.core.publisher.Mono;

public class SampleServiceInstance implements ServiceInstance {

    public final SampleClient client;

    public SampleServiceInstance(InetSocketAddress address, SampleTrafficPolicy trafficPolicy) {
        this.client = new SampleClient(address, trafficPolicy.getUsername(), trafficPolicy.getPassword());
    }

    public Mono<SampleResponse> execute(SampleRequest request) {
        return this.client.send(request);
    }

    public Mono<Void> shutdown() {
        return this.client.shutdown();
    }

    public Mono<Void> shutdownGracefully() {
        return this.client.shutdownGracefully();
    }
}
```

In the interest of simplification, the `SampleTrafficPolicy` hereafter always returns a `RandomTrafficLoadBalancer`.

```java
package io.inverno.example.discovery.sample;

import io.inverno.mod.discovery.RandomTrafficLoadBalancer;
import io.inverno.mod.discovery.TrafficPolicy;

public class SampleTrafficPolicy implements TrafficPolicy<SampleServiceInstance, SampleRequest> {

    private final String username;
    private final String password;
    
    public SampleTrafficPolicy(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return this.username;
    }
    
    public String getPassword() {
        return this.password;
    }

    public TrafficLoadBalancer<SampleServiceInstance, SampleRequest> getLoadBalancer(Collection<SampleServiceInstance> instances) throws IllegalArgumentException {
        return new RandomTrafficLoadBalancer<>(instances);
    }
}
```

The discovery API supplies basic traffic load balancer implementations: `RandomTrafficLoadBalancer`, `WeightedRandomTrafficLoadBalancer`, `RoundRobinTrafficLoadBalancer` and `WeightedRoundRobinTrafficLoadBalancer`. Weighted load balancers can load balance `WeightedServiceInstance` defined with different weights specifying the relative share of requests a given instance can handle. For instance considering a service with two weighted instances with respective weights 1 and 2, the second instance shall then receive twice as many requests as the first.

### Service

A `Service` is returned by a `DiscoveryService` when a service has been successfully resolved, it holds the resolved service instances and is responsible for selecting them when executing service requests. To do so, it can rely on the traffic policy providing traffic load balancer and/or the request itself which allows for more complex construct such as content based routing.

For instance a sample service request can be executed as follows:

```java
SampleRequest request = ...

Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> sampleService = ...

SampleResponse response = sampleService.getInstance(request)
    .flatMap(instance -> instance.execute(request))
    .block();
```

In the most common case, a service implementation typically gets a `TrafficLoadBalancer` from the traffic policy and use it to load balance requests among the list of service instances but since the request is passed when resolving an instance, specific implementations can also route a request to a particular matching instance or set of instances based on its content. 

A service can be refreshed in order to update its list of service instances which may change over time and/or to change the traffic policy.

```java
// Refresh if older than 30 minutes
if(sampleService.getLastRefreshed() < System.currentTimeMillis() + 1800000) {
    sampleService = sampleService.refresh(newTrafficPolicy).block();
}

// Refresh and change the traffic policy
TrafficPolicy newTrafficPolicy = ...
sampleService = sampleService.refresh(newTrafficPolicy).block();
```

> In above example, remember that `refresh()` can return an empty `Mono` which basically indicates that the service is gone.

A service must be eventually disposed when it is no longer useful in order to free resources which basically shutdown the service instances.

> A service instance shall never be shutdown directly, the service should always take care of it.

It can be shutdown gracefully or not:

```java
// Graceful shutdown
sampleService.shutdownGracefully().block();

// Hard shutdown
sampleService.shutdown().block();
```

### Discovery service

The main role of a `DiscoveryService` is to locate the servers serving a service identified by a service ID. It must then create and expose corresponding service instances in a `Service`. The way servers are resolved depends on the implementation and the type of service considered. For a basic network service such as an HTTP service, it would most likely simply resolve inet socket addresses using DNS lookups. For a logical service, it could resolve complex service descriptors from a configuration source or a service orchestrator (e.g. Kubernetes). In the end, the discovery service must be able to create service instances and expose them in a service.

A discovery service implements a single `resolve()` that resolves a service identified by a `ServiceId` with a `TrafficPolicy`. Since it can only resolve specific types of services, it must also provide a way to determine whether a service, characterized by its URI scheme, can be resolved. 

A basic discovery service implementation resolving sample services out of a static map might then look like: 

```java
package io.inverno.example.discovery.sample;

import io.invero.mod.discovery.DiscoveryService;
import io.invero.mod.discovery.Service;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

public class SampleDiscoveryService implements DiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

    public static Map<ServiceID, List<InetSocketAddress>> SAMPLE_SERVICE_REGISTRY = Map.of(
        ServiceID.of("sample://svc1"), List.of(InetSocketAddress.createUnresolved("svc1-host1", 123), InetSocketAddress.createUnresolved("svc1-host2", 123), InetSocketAddress.createUnresolved("svc1-host3", 123)),
        ServiceID.of("sample://svc2"), List.of(InetSocketAddress.createUnresolved("svc2-host1", 456), InetSocketAddress.createUnresolved("svc1-host2", 456)),
        ServiceID.of("sample://svc3"), List.of(InetSocketAddress.createUnresolved("svc3-host1", 789))
    );

    @Override
    public Set<String> getSupportedSchemes() {
        return Set.of("sample");
    }

    @Override
    public Mono<? extends Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy>> resolve(ServiceID serviceId, SampleTrafficPolicy trafficPolicy) throws IllegalArgumentException {
        if(!this.supports(serviceId)) {
            throw new IllegalArgumentException("Unsupported scheme: " + serviceId.getScheme());
        }
        return new SampleService(serviceId).refresh(trafficPolicy);
    }

    private static class SampleService implements Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

        private final ServiceID serviceID;
        private final List<SampleServiceInstance> instances;

        private long lastRefreshed;
        private SampleTrafficPolicy trafficPolicy;
        private TrafficLoadBalancer<SampleServiceInstance, SampleRequest> loadBalancer;

        public SampleService(ServiceID serviceID) {
            this.serviceID = serviceID;
            this.instances = new ArrayList<>();
        }

        @Override
        public ServiceID getID() {
            return this.serviceID;
        }

        @Override
        public SampleTrafficPolicy getTrafficPolicy() {
            return this.trafficPolicy;
        }

        @Override
        public Mono<? extends Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy>> refresh(SampleTrafficPolicy trafficPolicy) {
            return Mono.fromSupplier(() -> {
                List<InetSocketAddress> serviceNodes = SampleDiscoveryService.SAMPLE_SERVICE_REGISTRY.get(this.serviceID);
                List<SampleServiceInstance> newServiceInstances = new ArrayList<>();
                if(serviceNodes != null && !serviceNodes.isEmpty()) {
                    for(InetSocketAddress address : serviceNodes) {
                        newServiceInstances.add(new SampleServiceInstance(address, trafficPolicy));
                    }
                }

                Collection<SampleServiceInstance> instancesToShutdown = new ArrayList<>(this.instances);
                synchronized(this) {
                    this.trafficPolicy = trafficPolicy;
                    this.loadBalancer = !newServiceInstances.isEmpty() ? trafficPolicy.getLoadBalancer(newServiceInstances) : null;
                    this.instances.clear();
                    this.instances.addAll(newServiceInstances);
                    this.lastRefreshed = System.currentTimeMillis();
                }

                Flux.fromIterable(instancesToShutdown)
                    .flatMap(SampleServiceInstance::shutdownGracefully)
                    .subscribe();

                return this.loadBalancer != null ? this : null;
            });
        }

        @Override
        public Mono<? extends SampleServiceInstance> getInstance(SampleRequest serviceRequest) {
            return this.loadBalancer != null ? this.loadBalancer.next(serviceRequest) : Mono.empty();
        }

        @Override
        public long getLastRefreshed() {
            return this.lastRefreshed;
        }

        @Override
        public Mono<Void> shutdown() {
            return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.instances).map(SampleServiceInstance::shutdown))
                .doFirst(() -> {
                    this.loadBalancer = null;
                    this.instances.clear();
                    this.lastRefreshed = System.currentTimeMillis();
                })
                .then();
        }

        @Override
        public Mono<Void> shutdownGracefully() {
            return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.instances).map(SampleServiceInstance::shutdownGracefully))
                .doFirst(() -> {
                    this.loadBalancer = null;
                    this.instances.clear();
                    this.lastRefreshed = System.currentTimeMillis();
                })
                .then();
        }
    }
}
```

Above sample discovery service implementation resolves services from a static `Map`:  `sample://svc1` has three instances, `sample://svc2` has two instances and `sample://svc3` has one instance. Resolved services load balanced instances using a traffic load balancer obtained from the traffic policy which, in the case of the sample service, is always a random traffic load balancer.

To summarize, a sample service is resolved, refreshed and used as follows:

```java
SampleTrafficPolicy trafficPolicy = new SampleTrafficPolicy("user", "password");
SampleDiscoveryService discoveryService = new SampleDiscoveryService();

SampleRequest request = new SampleRequest("request");

SampleResponse response = discoveryService
    .resolve(ServiceID.of("sample://svc1"), trafficPolicy) // 1
    .flatMap(service -> service.getInstance(request))      // 2
    .flatMap(instance -> instance.execute(request))        // 3
    .block();
```

1. Service `sample://svc1` is resolved
2. A service instance among the three instances is selected using the load balancer
3. The request is executed

## Implementation support

Implementing complex service discovery can be a cumbersome, hopefully the API provides some base implementations to help with this task.

### DNS Discovery Service

The `AbstractDnsDiscoveryService` can be used to implement discovery services resolving services and instances using DNS resolution with the `NetService`. Implementors must implement methods `createUnresolvedAddress()` and `createServiceInstance()` for respectfully creating an unresolved inet address from a supported service ID and creating a service instance from a service ID, a traffic policy and a resolved inet address.

Considering a sample service URI, the authority part most certainly specifies an unresolved inet address (e.g. HTTP), a DNS based sample discovery service can then be implemented as follows:

```java
package io.inverno.example.discovery.sample;

import io.inverno.mod.base.net.NetService;

public class SampleDnsDiscoveryService extends AbstractDnsDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

    private static final int DEFAULT_SAMPLE_PORT = 1234;

    public SampleDnsDiscoveryService(NetService netService) {
        super(netService, Set.of("sample"));
    }

    @Override
    protected InetSocketAddress createUnresolvedAddress(ServiceID serviceId) {
        String host = serviceId.getURI().getHost();
        int port = serviceId.getURI().getPort();
        return InetSocketAddress.createUnresolved(host, port == -1 ? DEFAULT_SAMPLE_PORT : port);
    }

    @Override
    protected SampleServiceInstance createServiceInstance(ServiceID serviceId, SampleTrafficPolicy trafficPolicy, InetSocketAddress resolvedAddress) {
        return new SampleServiceInstance(resolvedAddress, trafficPolicy);
    }
}
```

> From a single hostname, A DNS lookup can return one or more IP addresses representing multiple service instances. That's basically what you'll get when resolving a [headless service][kubernetes-headless-service] with multiple replicas in a [Kubernetes][kubernetes] cluster.

### Configuration Discovery Service

The `AbstractConfigurationDiscoveryService` can be used to implement discovery services resolving services described in descriptors stored in a configuration source. Implementors must implement methods `readServiceDescriptor()` and `createService()` for respectfully parsing the service descriptor resolved from the configuration source as a string and creating the service from the service ID and the service descriptor. The `AbstractConfigurationService` should then be used to implement the service.

A configuration service URI for the sample service can be defined as `sample-conf://<service_key>`. A basic service descriptor can be a simple comma separated list of inet socket addresses: `<ip>:<port>`.

A simple configuration based sample discovery service can then be implemented as follows:

```java
package io.inverno.example.discovery.sample;

import java.net.InetSocketAddress;
import java.util.Set;

public class SampleServiceDescriptor {

    private final Set<InetSocketAddress> addresses;

    public SampleServiceDescriptor(Set<InetSocketAddress> addresses) {
        this.addresses = addresses;
    }

    public Set<InetSocketAddress> getAddresses() {
        return addresses;
    }
}
```

```java
package io.inverno.example.discovery.sample;

import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.discovery.AbstractConfigurationDiscoveryService;
import io.inverno.mod.discovery.ServiceID;
import java.util.Arrays;
import java.util.Set;

public class SampleConfigDiscoveryService extends AbstractConfigurationDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy, SampleServiceDescriptor> {

    public SampleConfigDiscoveryService(ConfigurationSource configurationSource) {
        super(Set.of("sample-conf"), "sample.service", configurationSource);
    }

    @Override
    protected SampleServiceDescriptor readServiceDescriptor(String content) throws Exception {
        return new SampleServiceDescriptor(Arrays.stream(content.split(","))
            .map(String::trim)
            .map(addr -> addr.split(":"))
            .map(addr -> new InetSocketAddress(addr[0], Integer.parseInt(addr[1])))
            .collect(Collectors.toSet()));
    }

    @Override
    protected Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> createService(ServiceID serviceId, Mono<SampleServiceDescriptor> serviceDescriptor) {
        return new SampleConfigService(serviceId, serviceDescriptor);
    }
}
```

A sample service descriptor is fetched from the configuration source using the service name specified in the service id prefixed by `sample.service`.

```java
package io.inverno.example.discovery.sample;

import io.inverno.mod.discovery.AbstractConfigurationService;
import io.inverno.mod.discovery.TrafficLoadBalancer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

public class SampleConfigService extends AbstractConfigurationService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy, SampleServiceDescriptor> {

    private final volatile List<SampleServiceInstance> instances;

    private volatile TrafficLoadBalancer<SampleServiceInstance, SampleRequest> loadBalancer;

    public SampleConfigService(ServiceID serviceId, Mono<SampleServiceDescriptor> serviceMetadata) {
        super(serviceId, serviceMetadata);
        this.instances = new ArrayList<>();
    }

    @Override
    protected Mono<? extends Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy>> doRefresh(SampleTrafficPolicy trafficPolicy, SampleServiceDescriptor serviceMetadata) {
        return Mono.fromSupplier(() -> {
            List<SampleServiceInstance> newServiceInstances = serviceMetadata.getAddresses().stream()
                .map(address -> new SampleServiceInstance(address, trafficPolicy))
                .collect(Collectors.toList());

            Collection<SampleServiceInstance> instancesToShutdown = new ArrayList<>(this.instances);
            synchronized(this) {
                this.trafficPolicy = trafficPolicy;
                this.loadBalancer = !newServiceInstances.isEmpty() ? trafficPolicy.getLoadBalancer(newServiceInstances) : null;
                this.instances.clear();
                this.instances.addAll(newServiceInstances);
            }

            Flux.fromIterable(instancesToShutdown)
                .flatMap(SampleServiceInstance::shutdownGracefully)
                .subscribe();

            return this.loadBalancer != null ? this : null;
        });
    }

    @Override
    public Mono<? extends SampleServiceInstance> getInstance(SampleRequest serviceRequest) {
        return this.loadBalancer != null ? this.loadBalancer.next(serviceRequest) : Mono.empty();
    }

    @Override
    public Mono<Void> shutdown() {
        return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.instances).map(SampleServiceInstance::shutdown))
            .doFirst(() -> {
                this.loadBalancer = null;
                this.instances.clear();
            })
            .then();
    }

    @Override
    public Mono<Void> shutdownGracefully() {
        return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.instances).map(SampleServiceInstance::shutdownGracefully))
            .doFirst(() -> {
                this.loadBalancer = null;
                this.instances.clear();
            })
            .then();
    }
}
```

Service `sample-conf://mySuperSampleService` can then be defined with two instances in a configuration source as follows:

```properties
sample.service.mySuperSampleService=1.2.3.4:1234,5.6.7.8:567
```

> Using a service descriptor is very flexible and allows to implement complex behaviours. For instance HTTP meta services support request routing, request rewriting, advanced load balancing... defined in HTTP meta descriptors.

### Composite Discovery Service

A `CompositeDiscoveryService` allows to aggregate multiple discovery services supporting different schemes into one discovery service.

The `SampleDnsDiscoveryService` and the `SampleConfigDiscoveryService` can be aggregated as follows:

```java
package io.inverno.example.discovery.sample;

import io.inverno.mod.discovery.CompositeDiscoveryService;

public class CompositeSampleDiscoveryService extends CompositeDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

    public CompositeSampleDiscoveryService(SampleConfigDiscoveryService configDiscoveryService, SampleDnsDiscoveryService dnsDiscoveryService) throws IllegalArgumentException {
        super(List.of(configDiscoveryService, dnsDiscoveryService));
    }
}
```

### Caching Discovery Service

A `CachingDiscoveryService` allows to wrap a discovery service, cache resolved services and refresh them periodically to avoid stale configurations. Caching services is useful to share and optimize resource usage in an application. It is important to remember that a service holds one or more service instances which in turn holds one or more connections to remote servers, being able to use same service instances in a multithreaded application is therefore crucial.

The `CompositeSampleDiscoveryService` can be cached as follows:

```java
package io.inverno.example.discovery.sample;

import io.inverno.mod.discovery.CachingDiscoveryService;

public class CachingSampleDiscoveryService extends CachingDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

    public CachingSampleDiscoveryService(Reactor reactor, CompositeSampleDiscoveryService sampleDiscoveryService) {
        super(reactor, sampleDiscoveryService);
    }

    public CachingSampleDiscoveryService(Reactor reactor, CompositeSampleDiscoveryService sampleDiscoveryService, long timeToLive) {
        super(reactor, sampleDiscoveryService, timeToLive);
    }
}
```

The `timeToLive` parameter defines the time to live in milliseconds of a resolved service before being refreshed.

The `CachingSampleDiscoveryService` is thread-safe and resolves sample services using DNS lookup (`sample://hostname:port`) or from a configuration source (`sample-conf://mySuperSampleService`), caches them and periodically refreshes them.