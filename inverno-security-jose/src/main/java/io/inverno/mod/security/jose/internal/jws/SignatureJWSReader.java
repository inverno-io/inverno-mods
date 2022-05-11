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
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWSReadException;
import java.lang.reflect.Type;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * JSON JWS signature specific JWS reader used to read signature specific JWS when reading a JSON JWS.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the expected payload type
 */
public class SignatureJWSReader<A> extends GenericJWSReader<A> {

	private final GenericJWSHeader jwsHeader;
	
	private final Mono<GenericJWSPayload<A>> jwsPayload;
	
	/**
	 * <p>
	 * Creates a JSON JWS signature reader.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the expected payload type
	 * @param keys                  the signature specific keys to consider to verify the JWS
	 * @param jwsHeader             the signature JWS header
	 * @param jwsPayload            the JSON JWS payload
	 */
	@SuppressWarnings("exports")
	public SignatureJWSReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, GenericJWSHeader jwsHeader, GenericJWSPayload<A> jwsPayload) {
		super(mapper, dataConversionService, jwkService, type, keys);
		this.jwsHeader = jwsHeader;
		this.jwsPayload = Mono.just(jwsPayload);
	}

	@Override
	protected GenericJWSHeader readJWSHeader(String encodedHeader) throws JWSReadException, JOSEObjectReadException, JOSEProcessingException {
		return this.jwsHeader;
	}

	@Override
	protected Mono<GenericJWSPayload<A>> readJWSPayload(String encodedPayload, Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType, GenericJWSHeader jwsHeader) throws JWSReadException, JOSEObjectReadException, JOSEProcessingException {
		return this.jwsPayload;
	}
}
