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
package io.inverno.mod.security.jose.internal.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKSet;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWK Set JSON String (i.e. {@code application/jwk-set+json}) media type converter.
 * </p>
 * 
 * <p>
 * The {@link MediaTypeConverter} implementation is only partial and only covers the JOSE module use cases.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "jwkSetJsonMediaTypeConverter" )
public class JWKSetJsonMediaTypeConverter implements JOSEMediaTypeConverter, @Provide MediaTypeConverter<String> {

	private final JWKService jwkService;
	private final ObjectMapper mapper;
	
	/**
	 * <p>
	 * Creates a JWK Set JSON object String converter.
	 * </p>
	 * 
	 * @param jwkService the JWK service
	 * @param mapper an object mapper
	 */
	public JWKSetJsonMediaTypeConverter(JWKService jwkService, ObjectMapper mapper) {
		this.jwkService = jwkService;
		this.mapper = mapper;
	}
	
	@Override
	public void injectJWSService(JWSService jwsService) {
		
	}

	@Override
	public void injectJWEService(JWEService jweService) {
		
	}

	@Override
	public void injectJWTService(JWTService jwtService) {
		
	}
	
	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_JWK_SET_JSON);
	}

	@Override
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		return this.decode(value, (Type)type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T decode(String value, Type type) throws ConverterException {
		Objects.requireNonNull(value);
		this.checkJWKSetType(type);
		return (T)Flux.from(this.jwkService.read(value))
			.collectList()
			.map(keys -> new JWKSet(keys.toArray(JWK[]::new)))
			.block();
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Class<T> type) {
		return this.decodeOne(value, (Type)type);
	}

	/**
	 * <p>
	 * Verifies that the specified type is a valid JWK Set type.
	 * </p>
	 * 
	 * @param type a type
	 * 
	 * @throws ConverterException if the specified type is not a valid JWK Set type
	 */
	private void checkJWKSetType(Type type) throws ConverterException {
		if(type instanceof ParameterizedType) {
			Type rawType = ((ParameterizedType)type).getRawType();
			if( !(rawType instanceof Class) || !JWKSet.class.isAssignableFrom((Class<?>)rawType)) {
				throw new ConverterException("Invalid JWK Set type");
			}
		}
		else if(!(type instanceof Class) || !JWKSet.class.isAssignableFrom((Class<?>)type)) {
			throw new ConverterException("Invalid JWK Set type");
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> Mono<T> decodeOne(Publisher<String> value, Type type) {
		return Mono.fromRunnable(() -> this.checkJWKSetType(type))
			.then((Mono<T>)Flux.from(value)
				.reduceWith(StringBuilder::new, StringBuilder::append)
				.map(StringBuilder::toString)
				.flatMapMany(this.jwkService::read)
				.collectList()
				.map(keys -> new JWKSet(keys.toArray(JWK[]::new)))
			);
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Flux<T> decodeMany(Publisher<String> value, Type type) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public <T> String encode(T value) throws ConverterException {
		Objects.requireNonNull(value);
		try {
			if(value instanceof JWKSet) {
				return this.mapper.writeValueAsString(value);
			}
			throw new ConverterException("Invalid JWKSet object: " + value.getClass());
		} 
		catch(JsonProcessingException e) {
			throw new ConverterException(e);
		}
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		return this.encode(value, (Type)type);
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		Objects.requireNonNull(value);
		try {
			if(value instanceof JWKSet) {
				return this.mapper.writerFor(mapper.constructType(type)).writeValueAsString(value);
			}
			throw new ConverterException("Invalid JWKSet object: " + value.getClass());
		} 
		catch(JsonProcessingException e) {
			throw new ConverterException(e);
		}
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value) {
		return value.map(jwkSet -> {
			try {
				if(jwkSet instanceof JWKSet) {
					return this.mapper.writeValueAsString(jwkSet);
				}
				throw new ConverterException("Invalid JWKSet object: " + jwkSet.getClass());
			} 
			catch(JsonProcessingException e) {
				throw new ConverterException(e);
			}
		});
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Class<T> type) {
		return this.encodeOne(value, (Type)type);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Type type) {
		return value.map(jwkSet -> {
			try {
				if(jwkSet instanceof JWKSet) {
					return this.mapper.writerFor(mapper.constructType(type)).writeValueAsString(jwkSet);
				}
				throw new ConverterException("Invalid JWKSet object: " + jwkSet.getClass());
			} 
			catch(JsonProcessingException e) {
				throw new ConverterException(e);
			}
		});
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Class<T> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> Publisher<String> encodeMany(Flux<T> value, Type type) {
		throw new UnsupportedOperationException();
	}
}
