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
package io.inverno.mod.web.client.internal;

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.ExchangeContext;
import io.inverno.mod.http.client.HttpClient;
import io.inverno.mod.web.base.DataConversionService;
import io.inverno.mod.web.client.WebClient;
import io.inverno.mod.web.client.internal.discovery.WebDiscoveryService;
import java.util.function.Supplier;

/**
 * <p>
 * Generic {@link WebClient.Boot} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.12
 */
@Bean( name = "webClientBoot" )
@Provide(WebClient.Boot.class)
public class GenericWebClientBoot implements WebClient.Boot {

	private final DataConversionService dataConversionService;
	private final ObjectConverter<String> parameterConverter;
	private final HttpClient httpClient;
	private final WebDiscoveryService discoveryService;

	private GenericWebClient<?> rootClient;

	/**
	 * <p>
	 * Creates a generic Web client boot.
	 * </p>
	 *
	 * @param dataConversionService the data conversion service
	 * @param parameterConverter    the parameter converter
	 * @param httpClient            the HTTP client
	 * @param discoveryService      the Web discovery service
	 */
	public GenericWebClientBoot(DataConversionService dataConversionService, ObjectConverter<String> parameterConverter, HttpClient httpClient, WebDiscoveryService discoveryService) {
		this.dataConversionService = dataConversionService;
		this.parameterConverter = parameterConverter;
		this.httpClient = httpClient;
		this.discoveryService = discoveryService;
	}

	@Override
	public <T extends ExchangeContext> WebClient<T> webClient(Supplier<T> contextFactory) throws IllegalStateException {
		if(this.rootClient != null) {
			throw new IllegalStateException("A WebClient has already been initialized");
		}
		GenericWebClient<T> client = new GenericWebClient<>(this.dataConversionService, this.parameterConverter, this.httpClient, this.discoveryService, contextFactory);
		this.rootClient = client;
		return client;
	}
}
