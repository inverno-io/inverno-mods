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
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwa.JWACipher;
import io.inverno.mod.security.jose.jwa.JWACipherException;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwe.JWEZipException;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * JSON JWE recipient specific JWE builder used to build recipient specific JWE when building a JSON JWE.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the payload type
 */
public class RecipientJWEBuilder<A> extends GenericJWEBuilder<A> {

	private final GenericJWEHeader protectedJWEHeader;
	private final GenericJWEHeader recipientJWEHeader;
	private final GenericJWEHeader jweHeader;
	private final Mono<GenericJWEPayload<A>> jwePayload;
	private final byte[] aad;
	private final JWACipher.EncryptedData zipAndEncryptedPayload;
	private final Flux<? extends JWK> cek;
	
	/**
	 * <p>
	 * Creates a JSON JWE recipient builder.
	 * </p>
	 * 
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the payload type
	 * @param keys                  the recipient specific keys to consider to secure the CEK
	 * @param zips                  a list of supported JWE compression algorithms
	 * @param protectedJWEHeader    the JSON JWE protected header
	 * @param recipientJWEHeader    the JSON JWE unprotected header
	 * @param jweHeader             the recipient JWE header
	 * @param jwePayload            the JSON JWE payload
	 * @param aad                   the additional authentication data
	 */
	@SuppressWarnings("exports")
	public RecipientJWEBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, List<JWEZip> zips, GenericJWEHeader protectedJWEHeader, GenericJWEHeader recipientJWEHeader, GenericJWEHeader jweHeader, GenericJWEPayload<A> jwePayload, byte[] aad) {
		this(mapper, dataConversionService, jwkService, type, keys, zips, protectedJWEHeader, recipientJWEHeader, jweHeader, jwePayload, aad, null, null);
	}
	
	/**
	 * <p>
	 * Creates a JSON JWE recipient builder.
	 * </p>
	 * 
	 * @param mapper                 an object mapper
	 * @param dataConversionService  a data conversion service
	 * @param jwkService             a JWK service
	 * @param type                   the payload type
	 * @param keys                   the recipient specific keys to consider to secure the CEK
	 * @param zips                   a list of supported JWE compression algorithms
	 * @param protectedJWEHeader     the JSON JWE protected header
	 * @param recipientJWEHeader     the JSON JWE unprotected header
	 * @param jweHeader              the recipient JWE header
	 * @param jwePayload             the JSON JWE payload
	 * @param aad                    the additional authentication data
	 * @param cek                    the JSON JWE Content Encryption key
	 * @param zipAndEncryptedPayload the encrypted payload
	 */
	@SuppressWarnings("exports")
	public RecipientJWEBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, List<JWEZip> zips, GenericJWEHeader protectedJWEHeader, GenericJWEHeader recipientJWEHeader, GenericJWEHeader jweHeader, GenericJWEPayload<A> jwePayload, byte[] aad, JWK cek, JWACipher.EncryptedData zipAndEncryptedPayload) {
		super(mapper, dataConversionService, jwkService, type, keys, zips);
		this.protectedJWEHeader = protectedJWEHeader;
		this.recipientJWEHeader = recipientJWEHeader;
		this.jweHeader = jweHeader;
		this.jwePayload = Mono.just(jwePayload);
		this.aad = aad;
		this.zipAndEncryptedPayload = zipAndEncryptedPayload;
		this.cek = cek != null ? Flux.just(cek) : null;
	}

	@Override
	public GenericJWEBuilder<A> header(Consumer<GenericJWEHeader> configurer) {
		throw new IllegalStateException();
	}

	@Override
	public GenericJWEBuilder<A> payload(A payload) {
		throw new IllegalStateException();
	}

	@Override
	protected GenericJWEHeader buildJWEHeader() throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		this.checkHeader(this.jweHeader);
		
		this.jweHeader.setEncoded(this.protectedJWEHeader != null ? this.protectedJWEHeader.getEncoded() : "");

		return this.jweHeader;
	}

	@Override
	protected Mono<GenericJWEPayload<A>> buildJWEPayload(Function<A, Mono<String>> overridingPayloadEncoder, String overridingContentType, GenericJWEHeader jweHeader) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return this.jwePayload;
	}

	@Override
	protected Flux<? extends JWK> generateCEK(GenericJWEHeader jweHeader) throws JWKGenerateException {
		if(this.cek != null) {
			return this.cek;
		}
		return super.generateCEK(jweHeader);
	}

	@Override
	protected void amendJWEHeader(GenericJWEHeader header, Map<String, Object> moreHeaderParameters) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		if(moreHeaderParameters != null && this.recipientJWEHeader != null) {
			moreHeaderParameters.forEach((k, v) -> this.recipientJWEHeader.addCustomParameter(k, v));
		}
		super.amendJWEHeader(header, moreHeaderParameters);
	}
	
	@Override
	protected byte[] getAdditionalAuthenticationData(GenericJWEHeader jweHeader) {
		return this.aad;
	}
	
	@Override
	protected JWACipher.EncryptedData zipAndEncryptPayload(GenericJWEHeader jweHeader, GenericJWEPayload<A> jwePayload, JWEZip payloadZip, JWK cek) throws JWKProcessingException, JWEZipException, JWACipherException {
		if(this.zipAndEncryptedPayload != null) {
			return this.zipAndEncryptedPayload;
		}
		return super.zipAndEncryptPayload(jweHeader, jwePayload, payloadZip, cek);
	}
}
