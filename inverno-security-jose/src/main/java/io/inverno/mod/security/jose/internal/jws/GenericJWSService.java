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
package io.inverno.mod.security.jose.internal.jws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWSBuilder;
import io.inverno.mod.security.jose.jws.JWSReader;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jws.JsonJWSBuilder;
import io.inverno.mod.security.jose.jws.JsonJWSReader;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Generic {@link JWSService} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "jwsService" )
public class GenericJWSService implements @Provide JWSService {

	private final ObjectMapper mapper;
	private final GenericDataConversionService dataConversionService;
	private final JWKService jwkService;

	/**
	 * <p>
	 * Creates a generic JWS service.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 */
	@SuppressWarnings("exports")
	public GenericJWSService(ObjectMapper mapper, GenericDataConversionService dataConversionService, JWKService jwkService) {
		this.mapper = mapper;
		this.dataConversionService = dataConversionService;
		this.jwkService = jwkService;
	}

	/**
	 * <p>
	 * Initializes the JWS service.
	 * </p>
	 */
	@Init
	public void init() {
		this.dataConversionService.injectJWSService(this);
	}

	@Override
	public <T> JWSBuilder<T, ?, ?> builder(Type type, Publisher<? extends JWK> keys) {
		return new GenericJWSBuilder<>(this.mapper, this.dataConversionService, this.jwkService, type, keys);
	}

	@Override
	public <T> JWSReader<T, ?> reader(Type type, Publisher<? extends JWK> keys) {
		return new GenericJWSReader<>(this.mapper, this.dataConversionService, this.jwkService, type, keys);
	}

	@Override
	public <T> JsonJWSBuilder<T, ?, ?> jsonBuilder(Type type) {
		return new GenericJsonJWSBuilder<>(this.mapper, this.dataConversionService, this.jwkService, type);
	}

	@Override
	public <T> JsonJWSReader<T, ?> jsonReader(Type type) {
		return new GenericJsonJWSReader<>(this.mapper, this.dataConversionService, this.jwkService, type);
	}
}
