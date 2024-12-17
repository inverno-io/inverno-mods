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
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.jwe.JWEReadException;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import java.lang.reflect.Type;
import java.util.List;
import org.reactivestreams.Publisher;

/**
 * <p>
 * JSON JWE recipient specific JWE reader used to read recipient specific JWE when reading a JSON JWE.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the expected payload type
 */
public class RecipientJWEReader<A> extends GenericJWEReader<A> {

	private final GenericJWEHeader jweHeader;
	private final byte[] aad;
	
	/**
	 * <p>
	 * Creates a JSON JWE recipient reader.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  a payload type
	 * @param keys                  the recipient specific keys to consider to decrypt the CEK
	 * @param zips                  a list of supported JWE compression algorithms
	 * @param jweHeader             the recipient JWE header
	 * @param aad                   the additional authentication data
	 */
	@SuppressWarnings("ClassEscapesDefinedScope")
	public RecipientJWEReader(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, List<JWEZip> zips, GenericJWEHeader jweHeader, byte[] aad) {
		super(mapper, dataConversionService, jwkService, type, keys, zips);
		this.jweHeader = jweHeader;
		this.aad = aad;
	}

	@Override
	protected GenericJWEHeader readJWEHeader(String encodedHeader) throws JWEReadException, JOSEObjectReadException, JOSEProcessingException {
		return this.jweHeader;
	}

	@Override
	protected byte[] getAdditionalAuthenticationData(GenericJWEHeader jweHeader) {
		return this.aad;
	}
}
