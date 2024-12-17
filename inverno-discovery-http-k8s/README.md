[kubernetes]: https://kubernetes.io/
[kubernetes-service]: https://kubernetes.io/docs/concepts/services-networking/service
[kubernetes-service-env-variables]: https://kubernetes.io/docs/concepts/services-networking/service/#environment-variables
[iptables]: https://www.netfilter.org/projects/iptables/index.html

# Discovery HTTP Kubernetes

The Inverno Discovery HTTP Kubernetes module provides HTTP discovery services for resolving HTTP services running in a Kubernetes cluster.

> This module is still work-in-progress, it currently only provides a discovery service based on [Kubernetes service environment variables][kubernetes-service-env-variables], future developments include providing a discovery service using the Kubernetes API. 

This module requires the `HttpClient` which is provided by the *http-client* module, so in order to use the Inverno *discovery-http* module, we need to declare the following dependency in the module descriptor:

```java
@io.inverno.core.annotation.Module
module io.inverno.example.discovery.http.k8s {
    requires io.inverno.mod.http.client;
}
```

We also need to declare this dependency in the build descriptor:

Using Maven:

```xml
<project>
    <dependencies>
        <dependency>
            <groupId>io.inverno.mod</groupId>
            <artifactId>inverno-http-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

Using Gradle:

```groovy
compile 'io.inverno.mod:inverno-http-client:${VERSION_INVERNO_MODS}'
```

## Kubernetes environment variables discovery service

The module exposes the `k8sEnvHttpDiscoveryService` bean that resolves Kubernetes services from [environment variables][kubernetes-service-env-variables] set by Kubernetes in service containers (pods) for each service.

An HTTP service can be deployed on a Kubernetes cluster. A deployment descriptor is used to define the application image, configuration as well as the number of replicas resulting in one or more pods being started in the cluster. A service descriptor defines how the application is exposed in the cluster typically using a `clusterIP`.

The following descriptor show how a simple HTTP service can be deployed using three replicas:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: http-testserver
  labels:
    app: http-testserver
    service: http-testserver
spec:
  ports:
    - name: http
      port: 8080
      appProtocol: http
  selector:
    app: http-testserver
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: http-testserver
  labels:
    app: http-testserver
spec:
  replicas: 3
  selector:
    matchLabels:
      app: http-testserver
  template:
    metadata:
      labels:
        app: http-testserver
    spec:
      containers:
        - name: http-testserver
          image: http-testserver:1.0.0
          ports:
            - containerPort: 8080
---
```

Kubernetes defines [environment variables][kubernetes-service-env-variables] and [iptables][iptables] rules in any pod started after deploying above configuration These variables basically expose the cluster IP and port of the service and the iptables rules are used to load balance connections between the service replicas.

For a given service, Kubernetes creates a variable exposing the cluster IP: `<SERVICE_NAME>_SERVICE_HOST` and one variable per port or application protocol: `<SERVICE_NAME>_SERVICE_PORT_<APPLICATION_PROTOCOL>`. Considering above configuration, the following variables should be defined:

```properties
HTTP_TESTSERVER_SERVICE_HOST=10.244.0.50
HTTP_TESTSERVER_SERVICE_PORT_HTTP=8080
```

The Kubernetes environment discovery service supports `k8s-env://` scheme, resolved HTTP client endpoints are automatically configured with TLS in the presence of an `*_PORT_HTTPS` variable overriding the configuration provided in both the HTTP client and HTTP traffic policy. HTTPS is preferred by default when a service exposes both HTTP and HTTPS ports, this behaviour can be changed by configuration in `K8sHttpDiscoveryConfiguration`.

The following code shows how the Kubernetes discovery service can be used to resolve and consume a service:

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

    public SomeService(HttpClient httpClient, HttpDiscoveryService k8sHttpDiscoveryService) {
        this.httpClient = httpClient;
        this.k8sHttpDiscoveryService = k8sHttpDiscoveryService;
        this.httpTrafficPolicy = HttpTrafficPolicy.builder().build();
    }

    public Mono<String> execute() {
        return this.httpClient.exchange(Method.GET, "/path/to/resource")
            .flatMap(exchange -> this.k8sHttpDiscoveryService.resolve(ServiceID.of("http://some-service"))
                .flatMap(service -> service.getInstance(exchange))
                .map(instance -> instance.bind(exchange))
            )
            .flatMap(Exchange::response)
            .flatMapMany(response -> response.body().string().stream())
            .collect(Collectors.joining());
    }
}
```

> In above example, the service is resolved on each request which is not suitable for a real-life application. Several options exist to make it more robust: cache the service using reactor API (e.g. using `cache()` methods) or define a global `CachingDiscoveryService` which also regularly refreshes services. The *web-client* module has been designed to silently take care of these aspects, so unless you need explicit control, it is recommended to use it as a replacement of the HTTP client.