/*
 * Copyright 2024 Jeremy Kuhn
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

import io.inverno.mod.base.concurrent.CommandExecutor;
import io.inverno.mod.base.concurrent.Reactor;
import io.netty.channel.EventLoop;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A discovery service wrapper that caches services by service ID and traffic policy.
 * </p>
 *
 * <p>
 * A service is uniquely identified by its service ID and the traffic policy that was used to resolve it. In order to prevent service instances from leaking, the caching discovery service caches
 * resolved service instances by {@code (serviceId, trafficPolicy)} hash and periodically refreshes those instances to keep them up to date.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 *
 * @param <A> the type of service instance
 * @param <B> the type of service request
 * @param <C> the type of traffic policy
 */
public class CachingDiscoveryService<A extends ServiceInstance, B, C extends TrafficPolicy<A, B>> implements DiscoveryService<A, B, C> {

	/**
	 * The default time to live in milliseconds.
	 */
	public static final long DEFAULT_TIME_TO_lIVE = 30000;

	private static final Logger LOGGER = LogManager.getLogger(CachingDiscoveryService.class);

	private final EventLoop eventLoop;
	private final DiscoveryService<A, B, C> discoveryService;
	private final long timeToLive;

	private final Map<Integer, Mono<? extends Service<A, B, C>>> serviceResolvers;
	private final Set<CachedService> services;
	private final CommandExecutor<Void> commandExecutor;
	private final Mono<Long> refresher;

	private ScheduledFuture<?> refreshFuture;
	private Mono<Void> shutdown;
	private Mono<Void> shutdownGracefully;

	/**
	 * <p>
	 * Creates a caching discovery service wrapping the specified discovery service.
	 * </p>
	 *
	 * @param reactor          the reactor
	 * @param discoveryService the discovery service resolving services
	 */
	public CachingDiscoveryService(Reactor reactor, DiscoveryService<A, B, C> discoveryService) {
		this(reactor, discoveryService, DEFAULT_TIME_TO_lIVE);
	}

	/**
	 * <p>
	 * Creates a caching discovery service wrapping the specified discovery service refreshing services after the specified time to live.
	 * </p>
	 *
	 * @param reactor          the reactor
	 * @param discoveryService the discovery service resolving services
	 * @param timeToLive       the service time to live in milliseconds
	 */
	public CachingDiscoveryService(Reactor reactor, DiscoveryService<A, B, C> discoveryService, long timeToLive) {
		this.eventLoop = reactor.getEventLoop();
		this.discoveryService = discoveryService;
		this.timeToLive = timeToLive;

		this.serviceResolvers = new HashMap<>();
		this.services = new HashSet<>();
		this.commandExecutor = new CommandExecutor<>(null);

		this.refresher = Flux.fromIterable(this.services)
			.flatMap(service -> {
				if(System.currentTimeMillis() - service.getLastRefreshed() > this.timeToLive) {
					// We reuse the latest traffic policy, it is changed on the resolved service that is intercepted in the cached discovery service
					return service.refresh(service.getOriginalTrafficPolicy())
						.onErrorResume(
							ex -> true,
							ex -> {
								// Log the error but let's keep the conf we had so far
								LOGGER.error("Failed to refresh service {}", service.getID(), ex);
								return Mono.empty();
							}
						)
						.then(Mono.empty());
				}
				else {
					return Mono.just(service);
				}
			})
			.map(Service::getLastRefreshed)
			.collect(Collectors.minBy(Comparator.naturalOrder()))
			.mapNotNull(oldestLastRefreshed -> oldestLastRefreshed.map(lastRefreshed -> Math.min(this.timeToLive, System.currentTimeMillis() - lastRefreshed)).orElse(this.timeToLive));
	}

	/**
	 * <p>
	 * Schedules the next service refresh.
	 * </p>
	 *
	 * @param delay the delay in milliseconds
	 */
	private void scheduleRefresh(long delay) {
		if(this.refreshFuture == null && !this.services.isEmpty()) {
			this.refreshFuture = this.eventLoop.schedule(
				() -> {
					this.refreshFuture = null;
					this.refresher.subscribe(
						this::scheduleRefresh,
						ex -> {
							// This should never happen
							LOGGER.error("Failed to refresh services", ex);
							this.scheduleRefresh(this.timeToLive);
						}
					);
				},
				delay,
				TimeUnit.MILLISECONDS
			);
		}
	}

	@Override
	public boolean supports(String scheme) {
		return this.discoveryService.supports(scheme);
	}

	@Override
	public Set<String> getSupportedSchemes() {
		return this.discoveryService.getSupportedSchemes();
	}

	@Override
	public Mono<? extends Service<A, B, C>> resolve(ServiceID serviceId, C trafficPolicy) {
		return Mono.<Mono<? extends Service<A, B, C>>>create(sink -> this.commandExecutor.execute(ign -> sink.success(
			this.serviceResolvers.computeIfAbsent(
				Objects.hash(serviceId, trafficPolicy),
				key -> this.discoveryService.resolve(serviceId, trafficPolicy)
					.map(resolvedService -> new CachedService(trafficPolicy, resolvedService))
					.doOnSuccess(cachedService -> {
						if(cachedService == null) {
							this.serviceResolvers.remove(key);
						}
						else {
							this.services.add(cachedService);
							this.scheduleRefresh(this.timeToLive);
						}
					})
					.share()
			)
		)))
		.flatMap(Function.identity());
	}

	/**
	 * <p>
	 * Shutdowns the discovery service.
	 * </p>
	 *
	 * <p>
	 * This basically shutdowns all cached services and free resources.
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the service is shutdown
	 */
	public synchronized Mono<Void> shutdown() {
		if(this.shutdown == null) {
			this.shutdown = Mono
				.fromRunnable(() -> {
					if(this.refreshFuture != null) {
						this.refreshFuture.cancel(false);
						this.refreshFuture = null;
					}
				})
				.then(Flux.fromIterable(this.services).flatMap(cachedService -> cachedService.unwrap().shutdown()).then())
				.doFinally(ign -> {
					this.serviceResolvers.clear();
					this.services.clear();
				})
				.share();
		}
		return this.shutdown;
	}

	/**
	 * <p>
	 * Gracefully shutdowns the discovery service.
	 * </p>
	 *
	 * <p>
	 * This basically gracefully shutdowns all cached services and free resources.
	 * </p>
	 *
	 * @return a {@code Mono} which completes once the service is shutdown
	 */
	public synchronized Mono<Void> shutdownGracefully() {
		if(this.shutdownGracefully == null) {
			this.shutdownGracefully = Mono
				.fromRunnable(() -> {
					if(this.refreshFuture != null) {
						this.refreshFuture.cancel(false);
						this.refreshFuture = null;
					}
				})
				.then(Flux.fromIterable(this.services).flatMap(cachedService -> cachedService.unwrap().shutdownGracefully()).then())
				.doFinally(ign -> {
					this.serviceResolvers.clear();
					this.services.clear();
				})
				.share();
		}
		return this.shutdownGracefully;
	}

	/**
	 * <p>
	 * A cached service wrapper.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.12
	 */
	private class CachedService implements Service<A, B, C> {

		private final C originalTrafficPolicy;

		private Service<A, B, C> service;

		/**
		 * <p>
		 * Creates a cached service wrapper for the specified service.
		 * </p>
		 *
		 * @param originalService a service
		 */
		public CachedService(C originalTrafficPolicy, Service<A, B, C> originalService) {
			this.originalTrafficPolicy = originalTrafficPolicy;
			this.service = originalService;
		}

		public C getOriginalTrafficPolicy() {
			return this.originalTrafficPolicy;
		}

		/**
		 * <p>
		 * Returns the wrapped service.
		 * </p>
		 *
		 * @return the wrapped service
		 */
		public Service<A, B, C> unwrap() {
			return this.service;
		}

		@Override
		public ServiceID getID() {
			return this.service.getID();
		}

		@Override
		public C getTrafficPolicy() {
			return this.service.getTrafficPolicy();
		}

		@Override
		public Mono<? extends A> getInstance(B serviceRequest) {
			return this.service.getInstance(serviceRequest);
		}

		@Override
		public Mono<? extends CachedService> refresh(C trafficPolicy) {
			if(!this.originalTrafficPolicy.equals(trafficPolicy)) {
				// a service is cached based on serviceId + traffic policy, so if traffic policy is changing, we'll have to resolve another service because we can't impact others who are still using
				// the original traffic policy
				// There is a slight chance that an already cached instance in which case service must be refreshed explicitly
				return Mono.defer(() -> {
					long t0 = System.currentTimeMillis();
					return CachingDiscoveryService.this.resolve(this.service.getID(), trafficPolicy)
						.flatMap(service -> {
							if(service.getLastRefreshed() < t0) {
								// trigger immediate refresh
								return ((CachedService) service).refresh(trafficPolicy);
							}
							return Mono.just((CachedService)service);
						});
				});
			}
			else {
				return this.service.refresh(trafficPolicy)
					.switchIfEmpty(Mono.create(sink -> CachingDiscoveryService.this.commandExecutor.execute(ign -> {
						CachingDiscoveryService.this.serviceResolvers.remove(this.hashCode());
						CachingDiscoveryService.this.services.remove(this);
						this.shutdownGracefully().subscribe();
						sink.success();
					})))
					.flatMap(service -> Mono.create(sink -> CachingDiscoveryService.this.commandExecutor.execute(ign -> {
						this.service = service;
						sink.success(this);
					})));
			}
		}

		@Override
		public long getLastRefreshed() {
			return this.service.getLastRefreshed();
		}

		@Override
		public Mono<Void> shutdown() {
			return Mono.error(() -> new IllegalStateException("A cached service can only be shutdown by the caching discovery service"));
		}

		@Override
		public Mono<Void> shutdownGracefully() {
			return Mono.error(() -> new IllegalStateException("A cached service can only be shutdown by the caching discovery service"));
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CachedService that = (CachedService) o;
			return Objects.equals(service.getID(), that.service.getID()) && Objects.equals(originalTrafficPolicy, that.originalTrafficPolicy);
		}

		@Override
		public int hashCode() {
			return Objects.hash(service.getID(), originalTrafficPolicy);
		}
	}

}
