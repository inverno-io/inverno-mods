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

package io.inverno.mod.http.client.internal;

import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.base.net.NetClientConfiguration;
import io.inverno.mod.base.net.NetService;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import java.net.InetSocketAddress;
import reactor.core.publisher.Mono;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class SingleEndpoint<A extends ExchangeContext> extends AbstractEndpoint<A> {

	private Mono<HttpConnection> connection;
	
	public SingleEndpoint(
			NetService netService, 
			SslContextProvider sslContextProvider,
			EndpointChannelConfigurer channelConfigurer,
			InetSocketAddress localAddress,
			InetSocketAddress remoteAddress, 
			HttpClientConfiguration configuration, 
			NetClientConfiguration netConfiguration,
			Class<A> contextType, 
			HeaderService headerService, 
			ObjectConverter<String> parameterConverter) {
		super(netService, sslContextProvider, channelConfigurer, localAddress, remoteAddress, configuration, netConfiguration, contextType, headerService, parameterConverter);
		this.connection = this.createConnection().cache();
	}

	@Override
	public Mono<HttpConnection> connection() {
		return this.connection;
	}

	@Override
	public Mono<Void> close() {
		if(this.connection != null) {
			return this.connection.flatMap(HttpConnection::close);
		}
		return Mono.empty();
	}
}
