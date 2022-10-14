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

package io.inverno.mod.http.client.internal.http1x;

import io.inverno.core.annotation.Bean;
import io.inverno.mod.base.converter.ObjectConverter;
import io.inverno.mod.http.base.HttpVersion;
import io.inverno.mod.http.base.Parameter;
import io.inverno.mod.http.base.header.HeaderService;
import io.inverno.mod.http.client.HttpClientConfiguration;
import io.inverno.mod.http.client.Part;
import io.inverno.mod.http.client.internal.EndpointChannelConfigurer;
import io.inverno.mod.http.client.internal.HttpConnectionFactory;
import io.inverno.mod.http.client.internal.multipart.MultipartEncoder;
import java.util.Set;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
@Bean( visibility = Bean.Visibility.PRIVATE )
public class Http1xConnectionFactory implements HttpConnectionFactory<Http1xConnection> {

	private final HeaderService headerService;
	private final ObjectConverter<String> parameterConverter;
	private final MultipartEncoder<Parameter> urlEncodedBodyEncoder;
	private final MultipartEncoder<Part<?>> multipartBodyEncoder;
	private final Part.Factory partFactory;

	public Http1xConnectionFactory(HeaderService headerService, ObjectConverter<String> parameterConverter, MultipartEncoder<Parameter> urlEncodedBodyEncoder, MultipartEncoder<Part<?>> multipartBodyEncoder, Part.Factory partFactory) {
		this.headerService = headerService;
		this.parameterConverter = parameterConverter;
		this.urlEncodedBodyEncoder = urlEncodedBodyEncoder;
		this.multipartBodyEncoder = multipartBodyEncoder;
		this.partFactory = partFactory;
	}

	@Override
	public Http1xConnection get(HttpClientConfiguration configuration, HttpVersion httpVersion, EndpointChannelConfigurer configurer) {
		if(HttpVersion.HTTP_2_0.equals(httpVersion)) {
			return new Http1xUpgradingConnection(configuration, HttpVersion.HTTP_1_1, this.headerService, this.parameterConverter, this.urlEncodedBodyEncoder, this.multipartBodyEncoder, this.partFactory, configurer);
		}
		else {
			return new Http1xConnection(configuration, httpVersion, this.headerService, this.parameterConverter, this.urlEncodedBodyEncoder, this.multipartBodyEncoder, this.partFactory);
		}
	}
}
