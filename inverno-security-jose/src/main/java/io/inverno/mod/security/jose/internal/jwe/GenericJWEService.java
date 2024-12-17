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
package io.inverno.mod.security.jose.internal.jwe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.jwe.JWEBuilder;
import io.inverno.mod.security.jose.jwe.JWEReader;
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwe.JsonJWEBuilder;
import io.inverno.mod.security.jose.jwe.JsonJWEReader;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Generic {@link JWEService} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "jweService" )
public class GenericJWEService implements @Provide JWEService {
	
	private final ObjectMapper mapper;
	private final GenericDataConversionService dataConversionService;
	private final JWKService jwkService;
	
	private final List<JWEZip> zips;

	/**
	 * <p>
	 * Creates a generic JWE service.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public GenericJWEService(ObjectMapper mapper, GenericDataConversionService dataConversionService, JWKService jwkService) {
		this.mapper = mapper;
		this.dataConversionService = dataConversionService;
		this.jwkService = jwkService;
		this.zips = new LinkedList<>();
	}

	/**
	 * <p>
	 * Initializes the JWE service.
	 * </p>
	 */
	@Init
	public void init() {
		this.dataConversionService.injectJWEService(this);
	}
	
	/**
	 * <p>
	 * Sets the JWE compression algorithm implementations.
	 * </p>
	 * 
	 * @param zips a set of JWE compression algorithm implementations
	 */
	public void setJWEZips(List<JWEZip> zips) {
		this.zips.clear();
		JWEZip deflateZip = null;
		for(JWEZip zip : zips) {
			if(zip instanceof DeflateJWEZip) {
				deflateZip = zip;
			}
			else {
				this.zips.add(zip);
			}
		}
		if(deflateZip != null) {
			this.zips.add(deflateZip);
		}
	}

	@Override
	public <T> JWEBuilder<T, ?, ?> builder(Type type, Publisher<? extends JWK> keys) {
		return new GenericJWEBuilder<>(this.mapper, this.dataConversionService, this.jwkService, type, keys, this.zips);
	}

	@Override
	public <T> JWEReader<T, ?> reader(Type type, Publisher<? extends JWK> keys) {
		return new GenericJWEReader<>(this.mapper, this.dataConversionService, this.jwkService, type, keys, this.zips);
	}
	
	@Override
	public <T> JsonJWEBuilder<T, ?, ?> jsonBuilder(Type type) {
		return new GenericJsonJWEBuilder<>(this.mapper, this.dataConversionService, this.jwkService, type, this.zips);
	}

	@Override
	public <T> JsonJWEReader<T, ?> jsonReader(Type type) {
		return new GenericJsonJWEReader<>(this.mapper, this.dataConversionService, this.jwkService, type, this.zips);
	}
}
