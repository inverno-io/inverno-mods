/*
 * Copyright 2022 Jeremy KUHN
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

package io.inverno.mod.http.client;

import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.http.base.ExchangeContext;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface HttpClient {

	default EndpointBuilder endpoint(String host, int port) {
		return this.endpoint(InetSocketAddress.createUnresolved(host, port));
	}
	
	EndpointBuilder endpoint(InetSocketAddress remoteAddress);
	
	interface EndpointBuilder {
		
		default EndpointBuilder localAddress(String host, int port) {
			return localAddress(new InetSocketAddress(host, port));
		}

		EndpointBuilder localAddress(InetSocketAddress localAddress);
		
		EndpointBuilder configuration(Consumer<HttpClientConfigurationLoader.Configurator> configurer);
	
		EndpointBuilder netConfiguration(NetClientConfiguration netConfiguration);
		
		// The Web client should not be exposed using a builder so we won't have to override this method with WebExchange
		default Endpoint<ExchangeContext> build() {
			return this.build(ExchangeContext.class);
		}

		// The context type is the equivalent to the context supplier in the server, it allows to control the context injected in the client: basically we have a "typed" client
		// This should also be checked at runtime
		<T extends ExchangeContext> Endpoint<T> build(Class<T> contextType);
	}
}
