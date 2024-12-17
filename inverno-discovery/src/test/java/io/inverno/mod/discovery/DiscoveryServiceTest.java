/*
 * Copyright 2024 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inverno.mod.discovery;

import io.inverno.mod.base.concurrent.Reactor;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.configuration.ConfigurationSource;
import io.inverno.mod.configuration.source.PropertiesConfigurationSource;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.ScheduledFuture;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class DiscoveryServiceTest {

	@Test
	public void testSampleDiscoveryService() {
		SampleDiscoveryService discoveryService = new SampleDiscoveryService();

		SampleTrafficPolicy trafficPolicy = new SampleTrafficPolicy("user", "password");
		SampleRequest request = new SampleRequest("request");

		SampleResponse response = discoveryService
			.resolve(ServiceID.of("sample://svc1"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);
	}

	@Test
	public void testSampleDnsDiscoveryService() throws UnknownHostException {
		NetService netService = Mockito.mock(NetService.class);

		Mockito.when(netService.resolveAll(InetSocketAddress.createUnresolved("svc1", 1234)))
			.thenReturn(Mono.just(List.of(new InetSocketAddress(InetAddress.getByAddress(new byte[]{1, 2, 3, 4}), 1234))));

		SampleDnsDiscoveryService discoveryService = new SampleDnsDiscoveryService(netService);

		SampleTrafficPolicy trafficPolicy = new SampleTrafficPolicy("user", "password");
		SampleRequest request = new SampleRequest("request");

		SampleResponse response = discoveryService
			.resolve(ServiceID.of("sample://svc1"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);

		Mockito.verify(netService).resolveAll(InetSocketAddress.createUnresolved("svc1", 1234));
	}

	@Test
	public void testSampleConfigDiscoveryService() throws UnknownHostException {
		Properties properties = new Properties();
		properties.setProperty("sample.service.svc1", "svc1-host1:1234,svc1-host2:5678");
		properties.setProperty("sample.service.svc2", "svc2-host1:1234");
		PropertiesConfigurationSource configurationSource = new PropertiesConfigurationSource(properties);

		SampleConfigDiscoveryService discoveryService = new SampleConfigDiscoveryService(configurationSource);

		SampleTrafficPolicy trafficPolicy = new SampleTrafficPolicy("user", "password");
		SampleRequest request = new SampleRequest("request");

		SampleResponse response = discoveryService
			.resolve(ServiceID.of("sample-conf://svc1"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);

		response = discoveryService
			.resolve(ServiceID.of("sample-conf://svc2"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);
	}

	@Test
	public void testCompositeSampleDiscoveryService() throws UnknownHostException {
		NetService netService = Mockito.mock(NetService.class);
		Mockito.when(netService.resolveAll(InetSocketAddress.createUnresolved("svc1", 1234)))
			.thenReturn(Mono.just(List.of(new InetSocketAddress(InetAddress.getByAddress(new byte[]{1, 2, 3, 4}), 1234))));

		Properties properties = new Properties();
		properties.setProperty("sample.service.svc1", "svc1-host1:1234,svc1-host2:5678");
		properties.setProperty("sample.service.svc2", "svc2-host1:1234");
		PropertiesConfigurationSource configurationSource = new PropertiesConfigurationSource(properties);

		CompositeSampleDiscoveryService discoveryService = new CompositeSampleDiscoveryService(new SampleConfigDiscoveryService(configurationSource), new SampleDnsDiscoveryService(netService));

		SampleTrafficPolicy trafficPolicy = new SampleTrafficPolicy("user", "password");
		SampleRequest request = new SampleRequest("request");

		SampleResponse response = discoveryService
			.resolve(ServiceID.of("sample-conf://svc1"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);

		response = discoveryService
			.resolve(ServiceID.of("sample://svc1"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);

		response = discoveryService
			.resolve(ServiceID.of("sample-conf://svc2"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);
	}

	@Test
	public void testCachingConfigDiscoveryService() throws UnknownHostException {
		NetService netService = Mockito.mock(NetService.class);
		Mockito.when(netService.resolveAll(InetSocketAddress.createUnresolved("svc1", 1234)))
			.thenReturn(Mono.just(List.of(new InetSocketAddress(InetAddress.getByAddress(new byte[]{1, 2, 3, 4}), 1234))));

		Properties properties = new Properties();
		properties.setProperty("sample.service.svc1", "svc1-host1:1234,svc1-host2:5678");
		properties.setProperty("sample.service.svc2", "svc2-host1:1234");
		PropertiesConfigurationSource configurationSource = new PropertiesConfigurationSource(properties);

		Reactor reactorMock = Mockito.mock(Reactor.class);
		EventLoop eventLoopMock = Mockito.mock(EventLoop.class);
		ScheduledFuture<?> refreshFutureMock = Mockito.mock(ScheduledFuture.class);
		Mockito.when(eventLoopMock.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any())).thenAnswer(ign -> refreshFutureMock);
		Mockito.when(reactorMock.getEventLoop()).thenReturn(eventLoopMock);

		CachingSampleDiscoveryService discoveryService = new CachingSampleDiscoveryService(reactorMock, new CompositeSampleDiscoveryService(new SampleConfigDiscoveryService(configurationSource), new SampleDnsDiscoveryService(netService)));

		SampleTrafficPolicy trafficPolicy = new SampleTrafficPolicy("user", "password");
		SampleRequest request = new SampleRequest("request");

		SampleResponse response = discoveryService
			.resolve(ServiceID.of("sample-conf://svc1"), trafficPolicy)
			.flatMap(service -> service.getInstance(request))
			.flatMap(instance -> instance.execute(request))
			.block();

		Assertions.assertEquals(new SampleResponse("Response to: " + request.getRequest()), response);

		Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> sampleConfSvc1_1 = discoveryService
			.resolve(ServiceID.of("sample-conf://svc1"), trafficPolicy)
			.block();

		Service<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> sampleConfSvc1_2 = discoveryService
			.resolve(ServiceID.of("sample-conf://svc1"), trafficPolicy)
			.block();

		// Service should be cached
		Assertions.assertTrue(sampleConfSvc1_1 == sampleConfSvc1_2);
	}

	public void test() {
		SampleTrafficPolicy trafficPolicy = new SampleTrafficPolicy("user", "password");
		SampleDiscoveryService discoveryService = new SampleDiscoveryService();

		SampleRequest request = new SampleRequest("request");

		SampleResponse response = discoveryService
			.resolve(ServiceID.of("sample://svc1"), trafficPolicy) // 1
			.flatMap(service -> service.getInstance(request))          // 2
			.flatMap(instance -> instance.execute(request))            // 3
			.block();
	}

	private static class SampleDiscoveryService implements DiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

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

	private static class SampleServiceInstance implements ServiceInstance {

		public final SampleClient client;

		public SampleServiceInstance(InetSocketAddress address, SampleTrafficPolicy trafficPolicy) {
			this.client = new SampleClient(address, trafficPolicy.getUsername(), trafficPolicy.getPassword());
		}

		public SampleClient getClient() {
			return client;
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

	private static class SampleTrafficPolicy implements TrafficPolicy<SampleServiceInstance, SampleRequest> {

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
			return new RoundRobinTrafficLoadBalancer<>(instances);
			//return new RandomTrafficLoadBalancer<>(instances);
		}
	}

	/* Sample client */
	private static class SampleClient {

		private final InetSocketAddress address;
		private final String username;
		private final String password;

		public SampleClient(InetSocketAddress address, String username, String password) {
			this.address = address;
			this.username = username;
			this.password = password;
		}

		public InetSocketAddress getAddress() {
			return address;
		}

		public Mono<SampleResponse> send(SampleRequest request) {
			return Mono.just(new SampleResponse("Response to: " + request.getRequest()));
		}

		public Mono<Void> shutdown() {
			return Mono.empty();
		}

		public Mono<Void> shutdownGracefully() {
			return Mono.empty();
		}
	}

	private static class SampleRequest {

		private final String request;

		public SampleRequest(String request) {
			this.request = request;
		}

		public String getRequest() {
			return request;
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;
			SampleRequest that = (SampleRequest) o;
			return Objects.equals(request, that.request);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(request);
		}
	}

	private static class SampleResponse {

		private final String response;

		public SampleResponse(String response) {
			this.response = response;
		}

		public String getResponse() {
			return response;
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) return true;
			if(o == null || getClass() != o.getClass()) return false;
			SampleResponse that = (SampleResponse) o;
			return Objects.equals(response, that.response);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(response);
		}
	}

	private static class SampleDnsDiscoveryService extends AbstractDnsDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

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

	private static class SampleServiceDescriptor {

		private final Set<InetSocketAddress> addresses;

		public SampleServiceDescriptor(Set<InetSocketAddress> addresses) {
			this.addresses = addresses;
		}

		public Set<InetSocketAddress> getAddresses() {
			return addresses;
		}
	}

	private static class SampleConfigDiscoveryService extends AbstractConfigurationDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy, SampleServiceDescriptor> {

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

	private static class SampleConfigService extends AbstractConfigurationService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy, SampleServiceDescriptor> {

		private final List<SampleServiceInstance> instances;

		private TrafficLoadBalancer<SampleServiceInstance, SampleRequest> loadBalancer;

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

	private static class CompositeSampleDiscoveryService extends CompositeDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

		public CompositeSampleDiscoveryService(SampleConfigDiscoveryService configDiscoveryService, SampleDnsDiscoveryService dnsDiscoveryService) throws IllegalArgumentException {
			super(List.of(configDiscoveryService, dnsDiscoveryService));
		}
	}

	private static class CachingSampleDiscoveryService extends CachingDiscoveryService<SampleServiceInstance, SampleRequest, SampleTrafficPolicy> {

		public CachingSampleDiscoveryService(Reactor reactor, CompositeSampleDiscoveryService sampleDiscoveryService) {
			super(reactor, sampleDiscoveryService);
		}

		public CachingSampleDiscoveryService(Reactor reactor, CompositeSampleDiscoveryService sampleDiscoveryService, long timeToLive) {
			super(reactor, sampleDiscoveryService, timeToLive);
		}
	}
}
