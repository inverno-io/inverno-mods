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
package io.inverno.mod.security.jose.internal.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Init;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.JOSEObjectReader;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.internal.jwe.DeflateJWEZip;
import io.inverno.mod.security.jose.jwe.JWEBuilder;
import io.inverno.mod.security.jose.jwe.JWEReader;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWSBuilder;
import io.inverno.mod.security.jose.jws.JWSReader;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTReadException;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.reactivestreams.Publisher;

/**
 * <p>
 * Generic JSON Web Token service implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "jwtService" )
public class GenericJWTService implements @Provide JWTService {

	private final ObjectMapper mapper;
	private final GenericDataConversionService dataConversionService;
	private final JWTDataConversionService jwtDataConversionService;
	private final JWKService jwkService;
	
	private final List<JWEZip> zips;

	/**
	 * <p>
	 * Creates a generic JWT service.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 */
	public GenericJWTService(ObjectMapper mapper, GenericDataConversionService dataConversionService, JWKService jwkService) {
		this.mapper = mapper;
		this.dataConversionService = dataConversionService;
		this.jwtDataConversionService = new JWTDataConversionService(dataConversionService);
		this.jwkService = jwkService;
		this.zips = new LinkedList<>();
	}
	
	/**
	 * <p>
	 * Initializes the JWT service.
	 * </p>
	 */
	@Init
	public void init() {
		this.dataConversionService.injectJWTService(this);
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
	
	/**
	 * <p>
	 * Verifies that the specified type is a valid JWT Claims Set type.
	 * </p>
	 * 
	 * @param type a type
	 * 
	 * @throws JOSEProcessingException if the specified type is not a valid JWT Claims Set type
	 */
	private void checkJWTClaimsSetType(Type type) throws JOSEProcessingException {
		Objects.requireNonNull(type);
		if(type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType) type).getRawType();
			if(!(rawType instanceof Class) || !JWTClaimsSet.class.isAssignableFrom((Class<?>)rawType)) {
				throw new JOSEProcessingException("Invalid JWT Claims Set type: " + type.getTypeName());
			}
		}
		else if(!(type instanceof Class) || !JWTClaimsSet.class.isAssignableFrom((Class<?>)type)) {
			throw new JOSEProcessingException("Invalid JWT Claims Set type: " + type.getTypeName());
		}
	}

	@Override
	public <T extends JWTClaimsSet> JWSBuilder<T, ?, ?> jwsBuilder(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		this.checkJWTClaimsSetType(type);
		return new JWTSBuilder<>(this.mapper, this.jwtDataConversionService, this.jwkService, type, keys);
	}

	@Override
	public <T extends JWTClaimsSet> JWEBuilder<T, ?, ?> jweBuilder(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		this.checkJWTClaimsSetType(type);
		return new JWTEBuilder<>(this.mapper, this.jwtDataConversionService, this.jwkService, type, keys, this.zips);
	}

	@Override
	public <T extends JWTClaimsSet> JWSReader<T, ?> jwsReader(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		this.checkJWTClaimsSetType(type);
		return new JWTSReader<>(this.mapper, this.dataConversionService, this.jwkService, type, keys);
	}

	@Override
	public <T extends JWTClaimsSet> JWEReader<T, ?> jweReader(Type type, Publisher<? extends JWK> keys) throws JOSEProcessingException {
		this.checkJWTClaimsSetType(type);
		return new JWTEReader<>(this.mapper, this.dataConversionService, this.jwkService, type, keys, this.zips);
	}

	@Override
	public <T extends JWTClaimsSet> JOSEObjectReader<T, ?, ? extends JOSEObject<T, ?>, ?> readerFor(String compact, Type type, Publisher<? extends JWK> keys) throws JWTReadException, JOSEProcessingException {
		String[] splitCompact = compact.split("\\.");
		switch(splitCompact.length) {
			case 3:
				// JWS
				return this.jwsReader(type, keys);
			case 5:
				// JWE
				return this.jweReader(type, keys);
			default:
				throw new JWTReadException("Invalid JWT object compact representation");
		}
	}
}
