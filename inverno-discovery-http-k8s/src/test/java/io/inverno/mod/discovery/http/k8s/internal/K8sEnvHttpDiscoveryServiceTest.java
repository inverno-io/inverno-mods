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
package io.inverno.mod.discovery.http.k8s.internal;

import io.inverno.mod.discovery.Service;
import io.inverno.mod.discovery.ServiceID;
import io.inverno.mod.discovery.http.HttpDiscoveryService;
import io.inverno.mod.discovery.http.HttpServiceInstance;
import io.inverno.mod.discovery.http.HttpTrafficPolicy;
import io.inverno.mod.discovery.http.k8s.K8sHttpDiscoveryConfiguration;
import io.inverno.mod.discovery.http.k8s.K8sHttpDiscoveryConfigurationLoader;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.Endpoint;
import io.inverno.mod.http.client.Exchange;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.http.client.InterceptedExchange;
import io.inverno.mod.http.client.UnboundExchange;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class K8sEnvHttpDiscoveryServiceTest {

	@Test
	@SuppressWarnings("unchecked")
	public void test_resolve_test_http() throws UnknownHostException {
		InetSocketAddress test_http_address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{1,2,3,4}), 8080);

		Endpoint<ExchangeContext> endpoint = Mockito.mock(Endpoint.class);
		HttpClient.EndpointBuilder<ExchangeContext, Exchange<ExchangeContext>, InterceptedExchange<ExchangeContext>> endpointBuilder = Mockito.mock(HttpClient.EndpointBuilder.class);
		Mockito.when(endpointBuilder.configuration(Mockito.any())).thenReturn(endpointBuilder);
		Mockito.when(endpointBuilder.netConfiguration(Mockito.any())).thenReturn(endpointBuilder);
		Mockito.when(endpointBuilder.build()).thenReturn(endpoint);

		HttpClient httpClient = Mockito.mock(HttpClient.class);
		Mockito.when(httpClient.endpoint(test_http_address)).thenReturn(endpointBuilder);

		UnboundExchange<ExchangeContext> exchange = Mockito.mock(UnboundExchange.class);
		Mockito.when(exchange.bind(Mockito.any())).thenReturn(exchange);

		HttpDiscoveryService k8sDiscoveryService_preferHTTPS = new K8sEnvHttpDiscoveryService(K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(true)), httpClient);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> httpService = k8sDiscoveryService_preferHTTPS.resolve(ServiceID.of("k8s-env://test-http")).block();
		Mockito.verify(httpClient).endpoint(test_http_address);
		httpService.getInstance(exchange).map(instance -> instance.bind(exchange)).block();
		Mockito.verify(exchange).bind(endpoint);

		Mockito.clearInvocations(httpClient);
		Mockito.clearInvocations(exchange);

		HttpDiscoveryService k8sDiscoveryService_preferHTTP = new K8sEnvHttpDiscoveryService(K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(false)), httpClient);

		httpService = k8sDiscoveryService_preferHTTP.resolve(ServiceID.of("k8s-env://test-http")).block();
		Mockito.verify(httpClient).endpoint(test_http_address);
		httpService.getInstance(exchange).map(instance -> instance.bind(exchange)).block();
		Mockito.verify(exchange).bind(endpoint);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_resolve_test_https() throws UnknownHostException {
		InetSocketAddress test_https_address = new InetSocketAddress(InetAddress.getByAddress(new byte[]{5,6,7,8}), 8443);

		K8sHttpDiscoveryConfiguration configuration = K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(true));

		Endpoint<ExchangeContext> endpoint = Mockito.mock(Endpoint.class);
		HttpClient.EndpointBuilder<ExchangeContext, Exchange<ExchangeContext>, InterceptedExchange<ExchangeContext>> endpointBuilder = Mockito.mock(HttpClient.EndpointBuilder.class);
		Mockito.when(endpointBuilder.configuration(Mockito.any())).thenReturn(endpointBuilder);
		Mockito.when(endpointBuilder.netConfiguration(Mockito.any())).thenReturn(endpointBuilder);
		Mockito.when(endpointBuilder.build()).thenReturn(endpoint);

		HttpClient httpClient = Mockito.mock(HttpClient.class);
		Mockito.when(httpClient.endpoint(test_https_address)).thenReturn(endpointBuilder);

		UnboundExchange<ExchangeContext> exchange = Mockito.mock(UnboundExchange.class);
		Mockito.when(exchange.bind(Mockito.any())).thenReturn(exchange);

		HttpDiscoveryService k8sDiscoveryService_preferHTTPS = new K8sEnvHttpDiscoveryService(K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(true)), httpClient);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> httpService = k8sDiscoveryService_preferHTTPS.resolve(ServiceID.of("k8s-env://test-https")).block();
		Mockito.verify(httpClient).endpoint(test_https_address);
		httpService.getInstance(exchange).map(instance -> instance.bind(exchange)).block();
		Mockito.verify(exchange).bind(endpoint);

		Mockito.clearInvocations(httpClient);
		Mockito.clearInvocations(exchange);

		HttpDiscoveryService k8sDiscoveryService_preferHTTP = new K8sEnvHttpDiscoveryService(K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(false)), httpClient);

		httpService = k8sDiscoveryService_preferHTTP.resolve(ServiceID.of("k8s-env://test-https")).block();
		Mockito.verify(httpClient).endpoint(test_https_address);
		httpService.getInstance(exchange).map(instance -> instance.bind(exchange)).block();
		Mockito.verify(exchange).bind(endpoint);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void test_resolve_test_both() throws UnknownHostException {
		InetSocketAddress test_both_address_http = new InetSocketAddress(InetAddress.getByAddress(new byte[]{9,10,11,12}), 8080);
		InetSocketAddress test_both_address_https = new InetSocketAddress(InetAddress.getByAddress(new byte[]{9,10,11,12}), 8443);

		K8sHttpDiscoveryConfiguration configuration = K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(true));

		Endpoint<ExchangeContext> endpoint_http = Mockito.mock(Endpoint.class);
		HttpClient.EndpointBuilder<ExchangeContext, Exchange<ExchangeContext>, InterceptedExchange<ExchangeContext>> endpoint_httpBuilder = Mockito.mock(HttpClient.EndpointBuilder.class);
		Mockito.when(endpoint_httpBuilder.configuration(Mockito.any())).thenReturn(endpoint_httpBuilder);
		Mockito.when(endpoint_httpBuilder.netConfiguration(Mockito.any())).thenReturn(endpoint_httpBuilder);
		Mockito.when(endpoint_httpBuilder.build()).thenReturn(endpoint_http);

		Endpoint<ExchangeContext> endpoint_https = Mockito.mock(Endpoint.class);
		HttpClient.EndpointBuilder<ExchangeContext, Exchange<ExchangeContext>, InterceptedExchange<ExchangeContext>> endpoint_httpsBuilder = Mockito.mock(HttpClient.EndpointBuilder.class);
		Mockito.when(endpoint_httpsBuilder.configuration(Mockito.any())).thenReturn(endpoint_httpsBuilder);
		Mockito.when(endpoint_httpsBuilder.netConfiguration(Mockito.any())).thenReturn(endpoint_httpsBuilder);
		Mockito.when(endpoint_httpsBuilder.build()).thenReturn(endpoint_https);

		HttpClient httpClient = Mockito.mock(HttpClient.class);
		Mockito.when(httpClient.endpoint(test_both_address_http)).thenReturn(endpoint_httpBuilder);
		Mockito.when(httpClient.endpoint(test_both_address_https)).thenReturn(endpoint_httpsBuilder);

		UnboundExchange<ExchangeContext> exchange = Mockito.mock(UnboundExchange.class);
		Mockito.when(exchange.bind(Mockito.any())).thenReturn(exchange);

		HttpDiscoveryService k8sDiscoveryService_preferHTTPS = new K8sEnvHttpDiscoveryService(K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(true)), httpClient);

		Service<HttpServiceInstance, UnboundExchange<?>, HttpTrafficPolicy> httpService = k8sDiscoveryService_preferHTTPS.resolve(ServiceID.of("k8s-env://test-both")).block();
		Mockito.verify(httpClient).endpoint(test_both_address_https);
		httpService.getInstance(exchange).map(instance -> instance.bind(exchange)).block();
		Mockito.verify(exchange).bind(endpoint_https);

		Mockito.clearInvocations(httpClient);
		Mockito.clearInvocations(exchange);

		HttpDiscoveryService k8sDiscoveryService_preferHTTP = new K8sEnvHttpDiscoveryService(K8sHttpDiscoveryConfigurationLoader.load(conf -> conf.prefer_https(false)), httpClient);

		httpService = k8sDiscoveryService_preferHTTP.resolve(ServiceID.of("k8s-env://test-both")).block();
		Mockito.verify(httpClient).endpoint(test_both_address_http);
		httpService.getInstance(exchange).map(instance -> instance.bind(exchange)).block();
		Mockito.verify(exchange).bind(endpoint_http);
	}
}
