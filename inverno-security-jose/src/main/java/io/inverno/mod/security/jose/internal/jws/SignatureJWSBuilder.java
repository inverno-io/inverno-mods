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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.JOSEUtils;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWSBuildException;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * JSON JWS Signature specific JWS builder used to build signature specific JWS when building a JSON JWS.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class SignatureJWSBuilder<A> extends GenericJWSBuilder<A> {

	private final GenericJWSHeader signatureProtectedJWSHeader;
	private final GenericJWSHeader jwsHeader;
	private final Mono<GenericJWSPayload<A>> jwsPayload;
	
	/**
	 * <p>
	 * Creates a JSON JWS signature builder.
	 * </p>
	 *
	 * @param mapper                      an object mapper
	 * @param dataConversionService       a data conversion service
	 * @param jwkService                  a JWK service
	 * @param type                        the payload type
	 * @param keys                        the signature specific keys to consider to sign the JWS
	 * @param signatureProtectedJWSHeader the signature specific protected JWSheader
	 * @param jwsHeader                   the signaTure JWS header
	 * @param jwsPayload                  the JSON JWS payload
	 */
	@SuppressWarnings("exports")
	public SignatureJWSBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type,	Publisher<? extends JWK> keys, GenericJWSHeader signatureProtectedJWSHeader, GenericJWSHeader jwsHeader, GenericJWSPayload<A> jwsPayload) {
		super(mapper, dataConversionService, jwkService, type, keys);
		
		this.jwsHeader = jwsHeader;
		this.signatureProtectedJWSHeader = signatureProtectedJWSHeader;
		this.jwsPayload = Mono.just(jwsPayload);
	}

	@Override
	public GenericJWSBuilder<A> header(Consumer<GenericJWSHeader> configurer) {
		throw new IllegalStateException();
	}

	@Override
	public GenericJWSBuilder<A> payload(A payload) {
		throw new IllegalStateException();
	}
	
	@Override
	protected GenericJWSHeader buildJWSHeader() throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		this.checkHeader(this.jwsHeader);
		
		try {
			String encodedProtectedHeader = JOSEUtils.BASE64_NOPAD_URL_ENCODER.encodeToString(this.mapper.writeValueAsBytes(this.signatureProtectedJWSHeader));
			this.signatureProtectedJWSHeader.setEncoded(encodedProtectedHeader);
			this.jwsHeader.setEncoded(encodedProtectedHeader);
		} 
		catch(JsonProcessingException e) {
			throw new JWSBuildException("Failed to encode JWS protected header", e);
		}

		return this.jwsHeader;
	}

	@Override
	protected Mono<GenericJWSPayload<A>> buildJWSPayload(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, GenericJWSHeader jwsHeader) throws JWSBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.jwsPayload;
	}
}
