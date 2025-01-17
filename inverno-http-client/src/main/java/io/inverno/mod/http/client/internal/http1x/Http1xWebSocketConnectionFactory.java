/*
 * Copyright 2023 Jeremy KUHN
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
package io.inverno.mod.http.client.internal.http1x;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.internal.WebSocketConnectionFactory;

/**
 * <p>
 * HTTP/1.x {@link WebSocketConnectionFactory} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.6
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class Http1xWebSocketConnectionFactory implements WebSocketConnectionFactory<Http1xWebSocketConnection> {

	private final ObjectConverter<String> parameterConverter;

	/**
	 * <p>
	 * Creates an HTTP/1.x WebSocket connection factory.
	 * </p>
	 * 
	 * @param parameterConverter the parameter converter
	 */
	public Http1xWebSocketConnectionFactory(ObjectConverter<String> parameterConverter) {
		this.parameterConverter = parameterConverter;
	}
	
	@Override
	public Http1xWebSocketConnection create(HttpClientConfiguration configuration, HttpVersion httpVersion) {
		return new Http1xWebSocketConnection(configuration, this.parameterConverter);
	}
}