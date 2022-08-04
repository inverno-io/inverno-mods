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
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JOSE object compact String (i.e. {@code application/jose}) media type converter.
 * </p>
 * 
 * <p>
 * The {@link MediaTypeConverter} implementation is only partial and only covers the JOSE module use cases.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@Bean( name = "joseStringMediaTypeConverter" )
public class JOSEStringMediaTypeConverter implements JOSEMediaTypeConverter, @Provide MediaTypeConverter<String> {

	private JWSService jwsService;
	private JWEService jweService;
	
	/**
	 * <p>
	 * Creates a JOSE object compact String converter.
	 * </p>
	 */
	public JOSEStringMediaTypeConverter() {
	}

	@Override
	public void injectJWSService(JWSService jwsService) {
		this.jwsService = jwsService;
	}

	@Override
	public void injectJWEService(JWEService jweService) {
		this.jweService = jweService;
	}

	@Override
	public void injectJWTService(JWTService jwtService) {
		
	}
	
	@Override
	public boolean canConvert(String mediaType) {
		return mediaType.equalsIgnoreCase(MediaTypes.APPLICATION_JOSE);
	}

	@Override
	public <T> T decode(String value, Class<T> type) throws ConverterException {
		return this.decode(value, (Type)type);
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
		return Flux.from(value)
			.reduceWith(() -> new StringBuilder(), (acc, v) -> acc.append(v))
			.map(StringBuilder::toString)
			.flatMap(compact -> {
				String[] splitCompact = compact.split("\\.");
				switch(splitCompact.length) {
					case 3:
						// JWS
						if(this.jwsService != null) {
							return (Mono<T>)this.jwsService.<T>reader(this.getJosePayloadType(type, JWS.class)).read(compact);
						}
						else {
							throw new ConverterException("Missing JWS service, unable to decode JWS");
						}
					case 5:
						// JWE
						if(this.jweService != null) {
							return (Mono<T>)this.jweService.<T>reader(this.getJosePayloadType(type, JWS.class)).read(compact);
						}
						else {
							throw new ConverterException("Missing JWE service, unable to decode JWE");
						}
					default:
						throw new ConverterException("Invalid JOSE object compact representation");
				}
			});
	}
	
	/**
	 * <p>
	 * Resolves the actual payload type from the specified type and returns it.
	 * </p>
	 *
	 * @param type        the JOSE object type
	 * @param joseRawType the JOSE object raw type
	 *
	 * @return the actual payload type
	 *
	 * @throws ConverterException if the specified type in not a valid JOSE object type
	 */
	private Type getJosePayloadType(Type type, Class<?> joseRawType) throws ConverterException {
		if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType)type;
			Type rawType = parameterizedType.getRawType();
			if(rawType instanceof Class && joseRawType.isAssignableFrom((Class<?>)rawType)) {
				Type[] typeArguments = parameterizedType.getActualTypeArguments();
				if(typeArguments.length == 0) {
					return Object.class;
				}
				else {
					return typeArguments[0];
				}
			}
			else {
				throw new ConverterException("Invalid Jose object type");
			}
		}
		else if(type instanceof Class && joseRawType.isAssignableFrom((Class<?>)type)) {
			// can't determine payload type from a class at runtime
			return Object.class;
		}
		else {
			throw new ConverterException("Invalid Jose object type");
		}
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
		if(value instanceof JOSEObject) {
			return ((JOSEObject<? ,?>)value).toCompact();
		}
		throw new ConverterException("Invalid JOSE object: " + value.getClass());
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
			if(jose instanceof JOSEObject) {
				return ((JOSEObject<?, ?>)jose).toCompact();
			}
			throw new ConverterException("Invalid JOSE object: " + jose.getClass());
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
