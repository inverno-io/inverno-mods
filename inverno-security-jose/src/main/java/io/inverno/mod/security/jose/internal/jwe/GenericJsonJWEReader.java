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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.AbstractJsonJOSEObjectReader;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEReadException;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwe.JsonJWE;
import io.inverno.mod.security.jose.jwe.JsonJWEReader;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * Generic JSON JWE reader implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the expected payload type
 */
public class GenericJsonJWEReader<A> extends AbstractJsonJOSEObjectReader<A, JsonJWE<A, JsonJWE.ReadRecipient<A>>, GenericJsonJWEReader<A>> implements JsonJWEReader<A, GenericJsonJWEReader<A>> {

	private final List<JWEZip> zips;
	
	/**
	 * <p>
	 * Creates a generic JSON JWE reader.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the expected payload type
	 * @param zips                  a list of supported JWE compression algorithms
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public GenericJsonJWEReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, List<JWEZip> zips) {
		super(mapper, dataConversionService, jwkService, type);
		this.zips = zips;
	}

	@Override
	public Mono<JsonJWE<A, JsonJWE.ReadRecipient<A>>> read(String json, String contentType) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(json, null, contentType);
	}

	@Override
	public Mono<JsonJWE<A, JsonJWE.ReadRecipient<A>>> read(String json, Function<String, Mono<A>> payloadDecoder) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return this.read(json, payloadDecoder, null);
	}
	
	/**
	 * <p>
	 * Reads a JSON JWE representation.
	 * </p>
	 *
	 * @param json                     a JSON JWE representation
	 * @param overridingPayloadDecoder an overriding payload decoder
	 * @param overridingContentType    an overriding payload content type
	 *
	 * @return a single JSON JWE publisher
	 *
	 * @throws JWEReadException         if there was an error reading the JSON JWE
	 * @throws JWEBuildException        if there was an error building the JSON JWE
	 * @throws JOSEObjectReadException  if there was an error reading the JSON JWE
	 * @throws JOSEObjectBuildException if there was an error building the JSON JWE
	 * @throws JOSEProcessingException  if there was a JOSE processing error
	 */
	@SuppressWarnings("unchecked")
	private Mono<JsonJWE<A, JsonJWE.ReadRecipient<A>>> read(String json, Function<String, Mono<A>> overridingPayloadDecoder, String overridingContentType) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		final Map<String, Object> parsedJson;
		try {
			parsedJson = this.mapper.readerForMapOf(Object.class).readValue(json);
		} 
		catch (JsonProcessingException e) {
			throw new JWEReadException("Invalid Json JWE", e);
		}

		final String protectedValue;
		final Map<String, Object> unprotectedValue;
		final String aadValue;
		final String ivValue;
		final String ciphertextValue;
		final String tagValue;
		final List<Object> recipientsValue;
		try {
			protectedValue = (String)parsedJson.get("protected");
			unprotectedValue = (Map<String, Object>)parsedJson.get("unprotected");
			aadValue = (String)parsedJson.get("aad");
			ivValue = (String)parsedJson.get("iv");
			ciphertextValue = (String)parsedJson.get("ciphertext");
			tagValue = (String)parsedJson.get("tag");
			recipientsValue = (List<Object>)parsedJson.get("recipients");
		}
		catch(ClassCastException e) {
			throw new JWEReadException("Invalid Json JWE", e);
		}
		
		JsonJWEHeader protectedJWEHeader = this.readProtectedJWEHeader(protectedValue);
		JsonJWEHeader unprotectedJWEHeader = this.readUnprotectedJWEHeader(unprotectedValue);
		if(protectedJWEHeader != null && unprotectedJWEHeader != null) {
			Set<String> overlappingParameters = new HashSet<>(protectedJWEHeader.getParametersSet());
			overlappingParameters.retainAll(unprotectedJWEHeader.getParametersSet());
			if(!overlappingParameters.isEmpty()) {
				throw new JWEReadException("Protected and unprotected headers must be disjoint: " + String.join(", ", overlappingParameters));
			}
		}
		
		byte[] raw_aad;
		if(aadValue != null) {
			byte[] encodedProtectedHeader = protectedJWEHeader != null ? protectedJWEHeader.getEncoded().getBytes(StandardCharsets.US_ASCII) : new byte[0];
			byte[] encodedAAD = aadValue.getBytes(StandardCharsets.US_ASCII);
			
			raw_aad = new byte[encodedProtectedHeader.length + 1 + encodedAAD.length];
			
			System.arraycopy(encodedProtectedHeader, 0, raw_aad, 0, encodedProtectedHeader.length);
			raw_aad[encodedProtectedHeader.length] = '.';
			System.arraycopy(encodedAAD, 0, raw_aad, encodedProtectedHeader.length + 1, encodedAAD.length);
		}
		else if(protectedJWEHeader != null) {
			raw_aad = protectedJWEHeader.getEncoded().getBytes(StandardCharsets.US_ASCII);
		}
		else {
			raw_aad = new byte[0];
		}
		
		String typ = null;
		String cty = null;
		String enc = null;
		String zip = null;
		final List<GenericJsonJWEReader.RecipientInfo> recipientInfos = new LinkedList<>();
		if(recipientsValue != null) {
			for(Object recipientElement : recipientsValue) {
				GenericJsonJWEReader.RecipientInfo recipientInfo = readJWERecipient(recipientElement, protectedJWEHeader, unprotectedJWEHeader);
				
				if(typ == null) {
					typ = recipientInfo.jweHeader.getType();
				}
				else if(recipientInfo.jweHeader.getType() != null && !typ.equals(recipientInfo.jweHeader.getType())) {
					throw new JWEReadException("Recipients have inconsistent types");
				}
				
				if(cty == null) {
					cty = recipientInfo.jweHeader.getContentType();
				}
				else if(recipientInfo.jweHeader.getContentType() != null && !cty.equals(recipientInfo.jweHeader.getContentType())) {
					throw new JWEReadException("Recipients have inconsistent content types");
				}
				
				if(enc == null) {
					enc = recipientInfo.jweHeader.getEncryptionAlgorithm();
				}
				else if(recipientInfo.jweHeader.getEncryptionAlgorithm() != null && !enc.equals(recipientInfo.jweHeader.getEncryptionAlgorithm())) {
					throw new JWEReadException("Recipients have inconsistent encryption algorithms");
				}
				
				if(zip == null) {
					zip = recipientInfo.jweHeader.getCompressionAlgorithm();
				}
				else if(recipientInfo.jweHeader.getCompressionAlgorithm() != null && !zip.equals(recipientInfo.jweHeader.getCompressionAlgorithm())) {
					throw new JWEReadException("Recipients have inconsistent compression algorithms");
				}
				recipientInfos.add(recipientInfo);
			}
		}
		else {
			// Try flattened syntax
			GenericJsonJWEReader.RecipientInfo recipientInfo = readJWERecipient(parsedJson, protectedJWEHeader, unprotectedJWEHeader);
			typ = recipientInfo.jweHeader.getType();
			cty = recipientInfo.jweHeader.getContentType();
			enc = recipientInfo.jweHeader.getEncryptionAlgorithm();
			zip = recipientInfo.jweHeader.getCompressionAlgorithm();
			recipientInfos.add(recipientInfo);
		}
		
		Function<String, Mono<A>> payloadDecoder = this.getPayloadDecoder(overridingPayloadDecoder, overridingContentType, cty);
		
		return Flux.fromIterable(recipientInfos)
			.map(recipientInfo -> (JsonJWE.ReadRecipient<A>)new GenericJsonJWE.GenericReadRecipient<>(
				recipientInfo.recipientJWEHeader, 
				recipientInfo.encrypted_key, 
				keys -> new RecipientJWEReader<A>(this.mapper, this.dataConversionService, this.jwkService, this.type, keys, this.zips, recipientInfo.jweHeader, raw_aad)
					.read("." + recipientInfo.encrypted_key + "." + ivValue + "." + ciphertextValue + "." + tagValue, payloadDecoder)
				)
			)
			.collectList()
			.map(recipients -> new GenericJsonJWE<>(protectedJWEHeader, unprotectedJWEHeader, ivValue, aadValue, ciphertextValue, tagValue, recipients, this.mapper));
	}
	
	/**
	 * <p>
	 * Reads the JSON JWE protected header.
	 * </p>
	 * 
	 * @param protectedValue the Base64URL encoded protected JWE header
	 * 
	 * @return the protected JWE header
	 * 
	 * @throws JWEReadException if there was an error reading the protected JWE header
	 * @throws JOSEObjectReadException if there was an error reading the protected JWE header
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected JsonJWEHeader readProtectedJWEHeader(String protectedValue) throws JWEReadException, JOSEObjectReadException, JOSEProcessingException {
		if(protectedValue == null) {
			return null;
		}
		try {
			JsonJWEHeader protectedHeader = this.mapper.readValue(Base64.getUrlDecoder().decode(protectedValue), JsonJWEHeader.class);
			protectedHeader.setEncoded(protectedValue);
			return protectedHeader;
		} 
		catch (IOException e) {
			throw new JWEReadException("Failed to decode Json JWE protected header", e);
		}
	}
	
	/**
	 * <p>
	 * Reads the JSON JWE unprotected header.
	 * </p>
	 * 
	 * @param unprotectedValue the unprotected JWE header as map
	 * 
	 * @return the unprotected JWE header
	 * 
	 * @throws JWEReadException if there was an error reading the unprotected JWE header
	 * @throws JOSEObjectReadException if there was an error reading the unprotected JWE header
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	protected JsonJWEHeader readUnprotectedJWEHeader(Map<String, Object> unprotectedValue) throws JWEReadException, JOSEObjectReadException, JOSEProcessingException {
		if(unprotectedValue == null) {
			return null;
		}
		return this.mapper.convertValue(unprotectedValue, JsonJWEHeader.class);
	}
	
	/**
	 * <p>
	 * Reads a JSON JWE recipient.
	 * </p>
	 * 
	 * @param recipientElementValue the extracted JSON JWE recipient element
	 * @param protectedJWEHeader the JSON JWE protected header
	 * @param unprotectedJWEHeader the JSON JWE unprotected header
	 * 
	 * @return a read recipient info
	 * 
	 * @throws JWEReadException        if there was an error reading the JSON JWE recipient
	 * @throws JOSEObjectReadException if there was an error reading the JSON JWE recipient
	 * @throws JOSEProcessingException if there was a JOSE processing error
	 */
	@SuppressWarnings("unchecked")
	protected GenericJsonJWEReader.RecipientInfo readJWERecipient(Object recipientElementValue, JsonJWEHeader protectedJWEHeader, JsonJWEHeader unprotectedJWEHeader) throws JWEReadException, JOSEObjectReadException, JOSEProcessingException {
		final Map<String, Object> recipientElement;
		final Map<String, Object> headerValue;
		final String encrypted_keyValue;
		try {
			recipientElement = (Map<String, Object>)recipientElementValue;
			headerValue = (Map<String, Object>)recipientElement.get("header");
			encrypted_keyValue = (String)recipientElement.get("encrypted_key");
		}
		catch(ClassCastException e) {
			 throw new JWEReadException("Invalid recipient element", e);
		}
		
		if(encrypted_keyValue == null) {
			throw new JWEReadException("Missing encrypted key field");
		}
		
		JsonJWEHeader recipientJWEHeader = headerValue != null ? this.mapper.convertValue(headerValue, JsonJWEHeader.class) : null;
		if(recipientJWEHeader != null) {
			Set<String> overlappingParameters = recipientJWEHeader.getParametersSet();
			overlappingParameters.retainAll(protectedJWEHeader.getParametersSet());
			overlappingParameters.retainAll(unprotectedJWEHeader.getParametersSet());
			if(!overlappingParameters.isEmpty()) {
				throw new JWEReadException("Recipient, protected and unprotected headers must be disjoint: " + String.join(", ", overlappingParameters));
			}
		}
		return new GenericJsonJWEReader.RecipientInfo(recipientJWEHeader, new JsonJWEHeader().merge(protectedJWEHeader).merge(unprotectedJWEHeader).merge(recipientJWEHeader), encrypted_keyValue);
	}
	
	/**
	 * <p>
	 * Read recipient info for holding recipient specific information.
	 * </p>
	 *
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	protected static class RecipientInfo {
		
		private final JsonJWEHeader recipientJWEHeader;
		private final JsonJWEHeader jweHeader;
		private final String encrypted_key;

		/**
		 * <p>
		 * Creates read recipient info.
		 * </p>
		 *
		 * @param recipientJWEHeader the recipient specific JWE header
		 * @param jweHeader          the recipient JWE header
		 * @param encrypted_key      the recipient specific encrypted key
		 */
		public RecipientInfo(JsonJWEHeader recipientJWEHeader, JsonJWEHeader jweHeader, String encrypted_key) {
			this.recipientJWEHeader = recipientJWEHeader;
			this.jweHeader = jweHeader;
			this.encrypted_key = encrypted_key;
		}
	}
}
