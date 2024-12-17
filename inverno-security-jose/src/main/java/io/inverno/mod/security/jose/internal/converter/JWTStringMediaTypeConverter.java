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

import io.inverno.core.annotation.Bean;
import io.inverno.core.annotation.Provide;
import io.inverno.mod.base.converter.ConverterException;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JWT compact String (i.e. {@code application/jwt}) media type converter.
 * </p>
 * 
 * <p>
 * The {@link MediaTypeConverter} implementation is only partial and only covers the JOSE module use cases.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "jwtStringMediaTypeConverter" )
public class JWTStringMediaTypeConverter implements JOSEMediaTypeConverter, @Provide MediaTypeConverter<String> {

	private JWTService jwtService;
	
	/**
	 * <p>
	 * Creates a JWT compact String converter.
	 * </p>
	 */
	public JWTStringMediaTypeConverter() {
	}
	
	@Override
	public void injectJWSService(JWSService jwsService) {
		
	}

	@Override
	public void injectJWEService(JWEService jweService) {
		
	}

	@Override
	public void injectJWTService(JWTService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_JWT);
	}
	
	/**
	 * <p>
	 * Verifies that the specified type is a valid JWT type.
	 * </p>
	 * 
	 * <p>
	 * A valid JWT type is a JOSE object raw type (i.e. {@link JWS} or {@link JWE}) parameterized with a {@link JWTClaimsSet} type.
	 * </p>
	 * 
	 * @param type the JOSE object type
	 * 
	 * @throws ConverterException if the specified type is not a valid JWT type
	 */
	private void checkJWTType(Type type) throws ConverterException {
		if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType)type;
			Type[] typeArguments = parameterizedType.getActualTypeArguments();
			Type rawType = parameterizedType.getRawType();
			if( !(rawType instanceof Class) || !JWS.class.isAssignableFrom((Class<?>)rawType) || !JWE.class.isAssignableFrom((Class<?>)rawType) || typeArguments.length != 1 || !(typeArguments[0] instanceof Class) || !JWTClaimsSet.class.isAssignableFrom((Class<?>)typeArguments[0]) ) {
				throw new ConverterException("Invalid JWT type: " + type.getTypeName());
			}
		}
		else if(!(type instanceof Class) || !JWS.class.isAssignableFrom((Class<?>)type) || !JWE.class.isAssignableFrom((Class<?>)type)) {
			// best effort, class cast exception might be thrown later
			throw new ConverterException("Invalid JWT type: " + type.getTypeName());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		Objects.requireNonNull(value);
		this.checkJWTType(type);
		return (T)Flux.from(this.jwtService.readerFor(value, type).read(value))
			.next()
			.block();
	}
	
	@Override
	public <T> T decode(String value, Type type) throws ConverterException {
		return this.<T>decodeOne(Mono.just(value), type).block();
	}

	@Override
	public <T> Mono<T> decodeOne(Publisher<String> value, Class<T> type) {
		return this.decodeOne(value, (Type)type);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Mono<T> decodeOne(Publisher<String> value, Type type) {
		return Mono.fromRunnable(() -> this.checkJWTType(type))
			.then((Mono<T>)Flux.from(value)
				.reduceWith(StringBuilder::new, StringBuilder::append)
				.map(StringBuilder::toString)
				.flatMapMany(compact -> this.jwtService.readerFor(compact, type).read(compact))
				.next()
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
		if(!(value instanceof JOSEObject)) {
			throw new ConverterException("Invalid JOSE object: " + value.getClass());
		}
		else if( !(((JOSEObject<?, ?>)value).getPayload() instanceof JWTClaimsSet) ) {
			throw new ConverterException("Invalid JWT payload: " + ((JOSEObject<?, ?>)value).getPayload().getClass());
		}
		return ((JOSEObject<?, ?>)value).toCompact();
	}

	@Override
	public <T> String encode(T value, Class<T> type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> String encode(T value, Type type) throws ConverterException {
		return this.encode(value);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value) {
		return value.map(jose -> {
			if(!(jose instanceof JOSEObject)) {
				throw new ConverterException("Invalid JOSE object: " + jose.getClass());
			}
			else if( !(((JOSEObject<?, ?>)jose).getPayload() instanceof JWTClaimsSet) ) {
				throw new ConverterException("Invalid JWT payload: " + ((JOSEObject<?, ?>)jose).getPayload().getClass());
			}
			return ((JOSEObject<?, ?>)jose).toCompact();
		});
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Class<T> type) {
		return this.encodeOne(value);
	}

	@Override
	public <T> Publisher<String> encodeOne(Mono<T> value, Type type) {
		return this.encodeOne(value);
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
