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
package io.inverno.mod.discovery.http.meta.internal;

import io.inverno.mod.discovery.AbstractConfigurationService;
import io.inverno.mod.discovery.DiscoveryService;
import io.inverno.mod.discovery.ManageableService;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.meta.HttpMetaServiceDescriptor;
import io.inverno.mod.http.base.router.HeadersRoute;
import io.inverno.mod.http.base.router.QueryParametersRoute;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.HttpClientConfigurationLoader;
import io.inverno.mod.http.client.UnboundExchange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.concurrent.Queues;

/**
 *
 * <p>
 * An HTTP meta service supporting routing, load balancing, request/response rewriting.
 * </p>
 *
 * <p>
 * An HTTP meta service is created from a {@link HttpMetaServiceDescriptor} which basically specifies how requests are transformed, routed and load balanced to destinations themselves pointing to
 * HTTP services. It then provides a way to compose various HTTP service into one while providing advanced features such as routing, load balancing, request/response rewriting...
 * </p>
 *
 * <p>
 * An HTTP meta service is using an {@link HttpMetaServiceRouter} to route a request to a matching {@link HttpMetaServiceRoute}. A route holds one or more {@link HttpMetaServiceRouteDestination} and a
 * destination holds exactly one HTTP service which is eventually used to process requests. The route load balances traffic among its destinations.
 * </p>
 *
 * <p>
 * The traffic policy, which defines HTTP client configuration and load balancing strategy is propagated top to bottom from the service level, the route level, the destination level and to the service
 * hold by the destination.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
public class HttpMetaService extends AbstractConfigurationService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy, HttpMetaServiceDescriptor> {

	private final DiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationDiscoveryService;

	private volatile HttpMetaServiceDescriptor descriptor;
	private volatile HttpMetaServiceRouter router;
	private volatile List<AbstractHttpMetaServiceRoute<?, ?>> routes;

	/**
	 * <p>
	 * Creates an HTTP meta service.
	 * </p>
	 *
	 * @param serviceId                   the service ID
	 * @param descriptor                  the HTTP meta service descriptor
	 * @param destinationDiscoveryService the discovery service used to resolve services in destinations
	 */
	public HttpMetaService(
		ServiceID serviceId,
		Mono<HttpMetaServiceDescriptor> descriptor,
		DiscoveryService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> destinationDiscoveryService
	) {
		super(serviceId, descriptor);
		this.destinationDiscoveryService = destinationDiscoveryService;
	}

	@Override
	public Mono<? extends HttpServiceInstance> getInstance(UnboundExchange<?> serviceRequest) {
		return Mono.defer(() -> {
			if(this.router == null) {
				throw new IllegalStateException("Service is " + this.serviceId + " is gone");
			}
			AbstractHttpMetaServiceRoute<?, ?> route = this.router.resolve(serviceRequest);
			if(route == null) {
				// We couldn't find an instance matching the request, client can decide what to do like throwing an error or responding with a 404
				return Mono.empty();
			}
			return route.getInstance(serviceRequest);
		});
	}

	@Override
	public Mono<HttpMetaService> doRefresh(HttpTrafficPolicy trafficPolicy, HttpMetaServiceDescriptor serviceDescriptor) {
		return Mono.defer(() -> {
			if(!serviceDescriptor.equals(this.descriptor)) {
				HttpClientConfiguration serviceClientConfiguration = trafficPolicy.getConfiguration();
				HttpMetaServiceDescriptor.LoadBalancerDescriptor serviceLoadBalancer = serviceDescriptor.getLoadBalancer();
				Consumer<HttpClientConfigurationLoader.Configurator> serviceConfigurer = serviceDescriptor.getConfiguration() != null ? serviceDescriptor.getConfiguration().getConfigurer() : null;

				return Flux.fromIterable(serviceDescriptor.getRoutes())
					.flatMap(routeDescriptor -> {
						HttpMetaServiceDescriptor.LoadBalancerDescriptor routeLoadBalancer = routeDescriptor.getLoadBalancer() != null ? routeDescriptor.getLoadBalancer() : serviceLoadBalancer;
						Consumer<HttpClientConfigurationLoader.Configurator> routeConfigurer;
						if(routeDescriptor.getConfiguration() != null) {
							routeConfigurer = serviceConfigurer != null ? serviceConfigurer.andThen(routeDescriptor.getConfiguration().getConfigurer()) : routeDescriptor.getConfiguration().getConfigurer();
						}
						else {
							routeConfigurer = serviceConfigurer;
						}

						return Flux.fromIterable(routeDescriptor.getDestinations())
							.flatMap(destinationDescriptor -> {
								HttpMetaServiceDescriptor.LoadBalancerDescriptor destinationLoadBalancer = destinationDescriptor.getLoadBalancer() != null ? destinationDescriptor.getLoadBalancer() : routeLoadBalancer;
								Consumer<HttpClientConfigurationLoader.Configurator> destinationConfigurer;
								if(destinationDescriptor.getConfiguration() != null) {
									destinationConfigurer = routeConfigurer != null ? routeConfigurer.andThen(destinationDescriptor.getConfiguration().getConfigurer()) : destinationDescriptor.getConfiguration().getConfigurer();
								}
								else {
									destinationConfigurer = routeConfigurer;
								}

								UnaryOperator<HttpTrafficPolicy> destinationTrafficPolicyOverride = originalTrafficPolicy -> {
									if(destinationLoadBalancer == null && destinationConfigurer == null) {
										return originalTrafficPolicy;
									}
									HttpTrafficPolicy.Builder trafficPolicyBuilder = HttpTrafficPolicy.builder(trafficPolicy);

									if(destinationLoadBalancer != null) {
										switch(destinationLoadBalancer.getStrategy()) {
											case RANDOM: trafficPolicyBuilder.randomLoadBalancer();
												break;
											case ROUND_ROBIN: trafficPolicyBuilder.roundRobinLoadBalancer();
												break;
											case LEAST_REQUEST: trafficPolicyBuilder.leastRequestLoadBalancer(((HttpMetaServiceDescriptor.LeastRequestLoadBalancerDescriptor)destinationLoadBalancer).getChoiceCount(), ((HttpMetaServiceDescriptor.LeastRequestLoadBalancerDescriptor)destinationLoadBalancer).getBias());
												break;
											case MIN_LOAD_FACTOR: trafficPolicyBuilder.minLoadFactorLoadBalancer(((HttpMetaServiceDescriptor.MinLoadFactorLoadBalancerDescriptor)destinationLoadBalancer).getChoiceCount(), ((HttpMetaServiceDescriptor.MinLoadFactorLoadBalancerDescriptor)destinationLoadBalancer).getBias());
												break;
											default: throw new IllegalStateException("Unsupported load balancing strategy: " + destinationLoadBalancer.getStrategy());
										}
									}

									if(destinationConfigurer != null) {
										trafficPolicyBuilder.configuration(HttpClientConfigurationLoader.load(trafficPolicy.getConfiguration(), destinationConfigurer));
									}
									return trafficPolicyBuilder.build();
								};
								// TODO We need to preserve path here
								// eventually we'll get an Endpoint when creating the exchange we need to make sure path is chrooted to what was define here:
								// conf://service/path/to/resource -> http://host:port/rootPath -> http://ip:port
								// eventually we'll have http://ip:port/rootPath/path/to/resource
								// we can do this in an interceptor

								return this.destinationDiscoveryService.resolve(ServiceID.of(destinationDescriptor.getURI()), destinationTrafficPolicyOverride.apply(trafficPolicy))
									.map(service -> {
										if(service instanceof ManageableService) {
											return new ManageableHttpMetaServiceRouteDestination(destinationDescriptor, destinationTrafficPolicyOverride, (ManageableService<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy>) service);
										}
										else {
											return new HttpMetaServiceRouteDestination(destinationDescriptor, destinationTrafficPolicyOverride, service);
										}
									});
							})
							.collectList()
							.map(destinations -> {
								UnaryOperator<HttpTrafficPolicy> routeTrafficPolicyOverride = originalTrafficPolicy -> {
									if(routeLoadBalancer == null && routeConfigurer == null) {
										return originalTrafficPolicy;
									}
									HttpTrafficPolicy.Builder trafficPolicyBuilder = HttpTrafficPolicy.builder(trafficPolicy);

									if(routeLoadBalancer != null) {
										switch(routeLoadBalancer.getStrategy()) {
											case RANDOM: trafficPolicyBuilder.randomLoadBalancer();
												break;
											case ROUND_ROBIN: trafficPolicyBuilder.roundRobinLoadBalancer();
												break;
											case LEAST_REQUEST: trafficPolicyBuilder.leastRequestLoadBalancer(((HttpMetaServiceDescriptor.LeastRequestLoadBalancerDescriptor)routeLoadBalancer).getChoiceCount(), ((HttpMetaServiceDescriptor.LeastRequestLoadBalancerDescriptor)routeLoadBalancer).getBias());
												break;
											case MIN_LOAD_FACTOR: trafficPolicyBuilder.minLoadFactorLoadBalancer(((HttpMetaServiceDescriptor.MinLoadFactorLoadBalancerDescriptor)routeLoadBalancer).getChoiceCount(), ((HttpMetaServiceDescriptor.MinLoadFactorLoadBalancerDescriptor)routeLoadBalancer).getBias());
												break;
											default: throw new IllegalStateException("Unsupported load balancing strategy: " + routeLoadBalancer.getStrategy());
										}
									}

									if(routeConfigurer != null) {
										trafficPolicyBuilder.configuration(HttpClientConfigurationLoader.load(trafficPolicy.getConfiguration(), routeConfigurer));
									}
									return trafficPolicyBuilder.build();
								};

								List<ManageableHttpMetaServiceRouteDestination> manageableDestinations = new ArrayList<>();
								for(HttpMetaServiceRouteDestination destination : destinations) {
									if(!(destination instanceof ManageableHttpMetaServiceRouteDestination)) {
										return new HttpMetaServiceRoute(routeDescriptor, trafficPolicy, routeTrafficPolicyOverride, destinations);
									}
									manageableDestinations.add((ManageableHttpMetaServiceRouteDestination)destination);
								}
								return new ManagedHttpMetaServiceRoute(routeDescriptor, trafficPolicy, routeTrafficPolicyOverride, manageableDestinations);
							});
					})
					.collectList()
					.map(routes -> {
						HttpMetaServiceRouter newRouter = new HttpMetaServiceRouter();
						for(AbstractHttpMetaServiceRoute<?, ?> route : routes) {
							HttpMetaServiceDescriptor.RouteDescriptor routeDescriptor = route.getDescriptor();
							HttpMetaServiceRouter.RouteManager routeManager = newRouter.route();
							if(routeDescriptor.getAuthorityMatchers() != null) {
								for(HttpMetaServiceDescriptor.ValueMatcher authorityMatcher : routeDescriptor.getAuthorityMatchers()) {
									switch(authorityMatcher.getKind()) {
										case STATIC:
											routeManager.authority(((HttpMetaServiceDescriptor.StaticValueMatcher) authorityMatcher).getValue());
											break;
										case REGEX:
											routeManager.authorityPattern(Pattern.compile(((HttpMetaServiceDescriptor.RegexValueMatcher) authorityMatcher).getRegex()));
											break;
										default: throw new IllegalStateException("Unsupported matcher: " + authorityMatcher.getKind());
									}
								}
							}

							if(routeDescriptor.getPathMatchers() != null) {
								for(HttpMetaServiceDescriptor.PathMatcher pathMatcher : routeDescriptor.getPathMatchers()) {
									routeManager.resolvePath(pathMatcher.getPath(), pathMatcher.isMatchTrailingSlash());
								}
							}

							if(routeDescriptor.getContentTypeMatchers() != null) {
								for(String contentTypeMatcher : routeDescriptor.getContentTypeMatchers()) {
									routeManager.contentType(contentTypeMatcher);
								}
							}

							if(routeDescriptor.getAcceptMatchers() != null) {
								for(String acceptMatcher : routeDescriptor.getAcceptMatchers()) {
									routeManager.accept(acceptMatcher);
								}
							}

							if(routeDescriptor.getLanguageMatchers() != null) {
								for(String languageMatcher : routeDescriptor.getLanguageMatchers()) {
									routeManager.language(languageMatcher);
								}
							}

							if(routeDescriptor.getHeadersMatchers() != null) {
								Map<String, HeadersRoute.HeaderMatcher> headersMatchers = new HashMap<>();
								for(Map.Entry<String, Set<? extends HttpMetaServiceDescriptor.ValueMatcher>> headerMatchers : routeDescriptor.getHeadersMatchers().entrySet()) {
									Set<String> staticValues = new HashSet<>();
									Set<Pattern> patternValues = new HashSet<>();
									for(HttpMetaServiceDescriptor.ValueMatcher headerMatcher : headerMatchers.getValue()) {
										switch(headerMatcher.getKind()) {
											case STATIC:
												staticValues.add(((HttpMetaServiceDescriptor.StaticValueMatcher) headerMatcher).getValue());
												break;
											case REGEX:
												patternValues.add(Pattern.compile(((HttpMetaServiceDescriptor.RegexValueMatcher) headerMatcher).getRegex()));
												break;
											default: throw new IllegalStateException("Unsupported matcher: " + headerMatcher.getKind());
										}
									}
									headersMatchers.put(headerMatchers.getKey(), new HeadersRoute.HeaderMatcher(staticValues, patternValues));
								}
								routeManager.headersMatchers(headersMatchers);
							}

							if(routeDescriptor.getQueryParameterMatchers() != null) {
								Map<String, QueryParametersRoute.ParameterMatcher> queryParametersMatchers = new HashMap<>();
								for(Map.Entry<String, Set<? extends HttpMetaServiceDescriptor.ValueMatcher>> queryParameterMatchers : routeDescriptor.getQueryParameterMatchers().entrySet()) {
									Set<String> staticValues = new HashSet<>();
									Set<Pattern> patternValues = new HashSet<>();
									for(HttpMetaServiceDescriptor.ValueMatcher queryParameterMatcher : queryParameterMatchers.getValue()) {
										switch(queryParameterMatcher.getKind()) {
											case STATIC:
												staticValues.add(((HttpMetaServiceDescriptor.StaticValueMatcher) queryParameterMatcher).getValue());
												break;
											case REGEX:
												patternValues.add(Pattern.compile(((HttpMetaServiceDescriptor.RegexValueMatcher) queryParameterMatcher).getRegex()));
												break;
											default: throw new IllegalStateException("Unsupported matcher: " + queryParameterMatcher.getKind());
										}
									}
									queryParametersMatchers.put(queryParameterMatchers.getKey(), new QueryParametersRoute.ParameterMatcher(staticValues, patternValues));
								}
								routeManager.queryParametersMatchers(queryParametersMatchers);
							}
							routeManager.set(route);
						}

						Mono<Void> previousRoutesGracefulShutdown;
						// We have to synchronize to prevent race conditions, this is not ideal but invoking refresh concurrently would be an issue anyway: don't invoke refresh concurrently
						synchronized(this) {
							previousRoutesGracefulShutdown = this.routes != null ? Flux.fromIterable(this.routes).flatMap(AbstractHttpMetaServiceRoute::shutdownGracefully).then() : null;
							this.descriptor = serviceDescriptor;
							this.router = newRouter;
							this.trafficPolicy = trafficPolicy;
							this.routes = routes;
						}
						if(previousRoutesGracefulShutdown != null) {
							previousRoutesGracefulShutdown.subscribe();
						}
						return this;
					});
			}
			else {
				// TODO
				//  what to do when a service fail to refresh?
				//  - propagate the error
				//  what to do when service return empty (i.e. service is gone)?
				//  -
				//  what to do when no destination is returned?
				return Flux.fromIterable(this.routes)
					.flatMap(route -> route.refresh(trafficPolicy))
					.collectList()
					.map(routes -> {
						// TODO What if we get an empty list?
						// - let's do nothing for now this is related to circuit breaking which requires more thinking
						return this;
					});
			}
		});
	}

	@Override
	public Mono<Void> shutdown() {
		return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.routes).map(AbstractHttpMetaServiceRoute::shutdown))
			.doFirst(() -> {
				this.router = null;
				this.routes = null;
			})
			.then();
	}

	@Override
	public Mono<Void> shutdownGracefully() {
		return Flux.mergeDelayError(Queues.XS_BUFFER_SIZE, Flux.fromIterable(this.routes).map(AbstractHttpMetaServiceRoute::shutdownGracefully))
			.doFirst(() -> {
				this.router = null;
				this.routes = null;
			})
			.then();
	}
}
