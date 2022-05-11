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
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJsonJOSEObjectReader;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jws.JWSBuildException;
import io.inverno.mod.security.jose.jws.JWSReadException;
import io.inverno.mod.security.jose.jws.JsonJWS;
import io.inverno.mod.security.jose.jws.JsonJWSReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON JWS reader implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the expected payload type
 */
public class GenericJsonJWSReader<A> extends AbstractJsonJOSEObjectReader<A, JsonJWS<A, JsonJWS.ReadSignature<A>>, GenericJsonJWSReader<A>> implements JsonJWSReader<A, GenericJsonJWSReader<A>> {
	
	/**
	 * <p>
	 * Creates a generic JSON JWS reader.
	 * </p>
	 * 
	 * @param mapper an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService a JWK service
	 * @param type the expected payload type
	 */
	@SuppressWarnings("exports")
	public GenericJsonJWSReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type) {
		super(mapper, dataConversionService, jwkService, type);
	}

	@Override
	public Mono<JsonJWS<A, JsonJWS.ReadSignature<A>>> read(String json, String contentType) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(json, null, contentType);
	}
	
	@Override
	public Mono<JsonJWS<A, JsonJWS.ReadSignature<A>>> read(String json, Function<String, Mono<A>> payloadDecoder) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(json, payloadDecoder, null);
	}

	/**
	 * <p>
	 * Reads a JSON JWS representation.
	 * </p>
	 *
	 * @param json                     a JSON JWS representation
	 * @param overridingPayloadDecoder an overriding payload decoder
	 * @param overridingContentType    an overriding payload content type
	 *
	 * @return a single JSON JWS publisher
	 *
	 * @throws JWSReadException         if there was an error reading the JSON JWS
	 * @throws JWSBuildException        if there was an error building the JSON JWS
	 * @throws JOSEObjectReadException  if there was an error reading the JSON JWS
	 * @throws JOSEObjectBuildException if there was an error building the JSON JWS
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	@SuppressWarnings("unchecked")
	private Mono<JsonJWS<A, JsonJWS.ReadSignature<A>>> read(String json, Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType) throws JWSReadException, JWSBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		final Map<String, Object> parsedJson;
		try {
			parsedJson = this.mapper.readerForMapOf(Object.class).readValue(json);
		} 
		catch (JsonProcessingException ex) {
			throw new JWSReadException("Error reading Json JWS");
		}
		
		final String payloadValue;
		final List<Object> signaturesValue;
		try {
			payloadValue = (String)parsedJson.get("payload");
			signaturesValue = (List<Object>)parsedJson.get("signatures");
		}
		catch(ClassCastException e) {
			throw new JWSReadException("Invalid JWS", e);
		}
		
		String cty = null;
		Boolean b64 = null;
		final List<GenericJsonJWSReader.SignatureInfo> signatureInfos = new LinkedList<>();
		if(signaturesValue != null) {
			for(Object signatureElement : signaturesValue) {
				GenericJsonJWSReader.SignatureInfo signatureInfo = readJWSSignature(signatureElement);
				
				if(cty == null) {
					cty = signatureInfo.signatureJWSHeader.getContentType();
				}
				else if(signatureInfo.signatureJWSHeader.getContentType() != null && !cty.equals(signatureInfo.signatureJWSHeader.getContentType())) {
					throw new JWSReadException("Inconsistent content type");
				}
				
				if(b64 == null) {
					// signature must either null or true
					b64 = signatureInfo.signatureJWSHeader.isBase64EncodePayload() == null || signatureInfo.signatureJWSHeader.isBase64EncodePayload();
				}
				else if( (!b64 && (signatureInfo.signatureJWSHeader.isBase64EncodePayload() == null || signatureInfo.signatureJWSHeader.isBase64EncodePayload())) || (b64 && signatureInfo.signatureJWSHeader.isBase64EncodePayload() != null && !signatureInfo.signatureJWSHeader.isBase64EncodePayload()) ) {
					throw new JWSReadException("Inconsistent base64 payload encoding");
				}
				
				signatureInfos.add(signatureInfo);
			}
		}
		else {
			// Try flattened syntax
			GenericJsonJWSReader.SignatureInfo signatureInfo = readJWSSignature(parsedJson);
			cty = signatureInfo.signatureJWSHeader.getContentType();
			b64 = signatureInfo.signatureJWSHeader.isBase64EncodePayload();
			signatureInfos.add(signatureInfo);
		}
		
		return this.readJWSPayload(payloadValue, overridingPayloadDecoder, overridingContentType, cty, b64)
			.flatMap(jwsPayload -> Flux.fromIterable(signatureInfos)
				.map(signatureReader -> (JsonJWS.ReadSignature<A>)new GenericJsonJWS.GenericReadSignature<>(
						signatureReader.signatureProtectedJWSHeader, 
						signatureReader.signatureUnprotectedJWSHeader, 
						signatureReader.signature, 
						keys -> new SignatureJWSReader<>(this.mapper, this.dataConversionService, this.jwkService, this.type, keys, signatureReader.signatureJWSHeader, jwsPayload)
							.read(".." + signatureReader.signature)
					)
				)
				.collectList()
				.map(signatures -> new GenericJsonJWS<>(this.mapper, jwsPayload, signatures))
			);
	}
	
	/**
	 * <p>
	 * Reads a JSON JWS recipient.
	 * </p>
	 * 
	 * @param signatureElementValue the extracted JSON JWS signature element
	 * 
	 * @return a read recipient info
	 * 
	 * @throws JWSReadException        if there was an error reading the JSON JWS signature
	 * @throws JOSEObjectReadException if there was an error reading the JSON JWS signature
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	@SuppressWarnings("unchecked")
	protected GenericJsonJWSReader.SignatureInfo readJWSSignature(Object signatureElementValue) throws JWSReadException, JOSEObjectReadException, JOSEProcessingException {
		final Map<String, Object> signatureElement;
		final String protectedValue;
		final Map<String, Object> unprotectedValue;
		final String signatureValue;
		try {
			signatureElement = (Map<String,Object>)signatureElementValue;
			protectedValue = (String)signatureElement.get("protected");
			unprotectedValue = (Map<String, Object>)signatureElement.get("header");
			signatureValue = (String)signatureElement.get("signature");
		}
		catch(ClassCastException e) {
			throw new JWSReadException("Invalid signature element", e);
		}
		
		if(signatureValue == null) {
			throw new JWSReadException("Missing signature field");
		}

		JsonJWSHeader protectedSignatureJWSHeader = null;
		JsonJWSHeader unprotectedSignatureJWSHeader = null;
		JsonJWSHeader signatureJWSHeader;
		try {
			if(unprotectedValue != null) {
				unprotectedSignatureJWSHeader = this.mapper.convertValue(unprotectedValue, JsonJWSHeader.class);
				unprotectedSignatureJWSHeader.setEncoded("");
				signatureJWSHeader = this.mapper.convertValue(unprotectedValue, JsonJWSHeader.class);
				if(protectedValue != null) {
					protectedSignatureJWSHeader = this.mapper.readValue(Base64.getUrlDecoder().decode(protectedValue), JsonJWSHeader.class);
					protectedSignatureJWSHeader.setEncoded(protectedValue);
					
					signatureJWSHeader.startRecordOverlap();
					this.mapper.readerForUpdating(signatureJWSHeader).readValue(Base64.getUrlDecoder().decode(protectedValue));
					
					Set<String> overlappedParameters = signatureJWSHeader.stopRecordOverlap();
					if(overlappedParameters != null && !overlappedParameters.isEmpty()) {
						throw new JWSReadException("Protected and unprotected headers must be disjoint: " + overlappedParameters.stream().collect(Collectors.joining(", ")));
					}
					signatureJWSHeader.setEncoded(protectedValue);
				}
				else {
					signatureJWSHeader = unprotectedSignatureJWSHeader;
				}
			}
			else if(protectedValue != null) {
				protectedSignatureJWSHeader = this.mapper.readValue(Base64.getUrlDecoder().decode(protectedValue), JsonJWSHeader.class);
				protectedSignatureJWSHeader.setEncoded(protectedValue);
				
				signatureJWSHeader = protectedSignatureJWSHeader;
			}
			else {
				throw new JWSReadException("Signature must have one of protected and header fields");
			}
		}
		catch (IOException e) {
			throw new JWSReadException("Error decoding signature header", e);
		}
		return new GenericJsonJWSReader.SignatureInfo(protectedSignatureJWSHeader, unprotectedSignatureJWSHeader, signatureJWSHeader, signatureValue);
	}
	
	/**
	 * <p>
	 * Reads the JSON JWS payload.
	 * </p>
	 * 
	 * @param encodedPayload the Base64URL encoded payload
	 * @param overridingPayloadDecoder an overriding payload decoder
	 * @param overridingContentType    an overriding payload content type
	 * @param cty the common payload content type
	 * @param b64 the comming b64 parameter
	 * 
	 * @return a singe JWS payload publisher
	 * 
	 * @throws JWSReadException        if there was an error reading the JSON JWS payload
	 * @throws JOSEObjectReadException if there was an error reading the JSON JWS payload
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected Mono<GenericJWSPayload<A>> readJWSPayload(String encodedPayload, Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType, String cty, Boolean b64) throws JWSReadException, JOSEObjectReadException, JOSEProcessingException {
		Function<String, Mono<A>> payloadDecoder = this.getPayloadDecoder(overridingPayloadDecoder, overridingContentType, cty);
		return Mono.defer(() -> {
			if(b64 != null && !b64) {
				return payloadDecoder.apply(encodedPayload)
					.map(payload -> {
						GenericJWSPayload<A> jwsPayload = new GenericJWSPayload<>(payload);
						jwsPayload.setRaw(encodedPayload);

						return jwsPayload;
					});
			}
			else {
				return payloadDecoder.apply(new String(Base64.getUrlDecoder().decode(encodedPayload)))
					.map(payload -> {
						GenericJWSPayload<A> jwsPayload = new GenericJWSPayload<>(payload);
						jwsPayload.setEncoded(encodedPayload);
						jwsPayload.setRaw(new String(Base64.getUrlDecoder().decode(encodedPayload)));

						return jwsPayload;
					});
			}
		});
	}
	
	/**
	 * <p>
	 * Read signature info for holding signature specific information.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	protected static class SignatureInfo {
		
		final GenericJWSHeader signatureProtectedJWSHeader;
		final GenericJWSHeader signatureUnprotectedJWSHeader;
		final GenericJWSHeader signatureJWSHeader;
		final String signature;

		/**
		 * <p>
		 * Creates read signature info.
		 * </p>
		 *
		 * @param signatureProtectedJWSHeader   the signature protected JWS header
		 * @param signatureUnprotectedJWSHeader the signature unprotected JWS header
		 * @param jwsHeader                     the signature JWS header
		 * @param signature                     the Base64URL encoded signature
		 */
		public SignatureInfo(GenericJWSHeader signatureProtectedJWSHeader, GenericJWSHeader signatureUnprotectedJWSHeader, GenericJWSHeader jwsHeader, String signature) {
			this.signatureProtectedJWSHeader = signatureProtectedJWSHeader;
			this.signatureUnprotectedJWSHeader = signatureUnprotectedJWSHeader;
			this.signatureJWSHeader = jwsHeader;
			this.signature = signature;
		}
	}
}
