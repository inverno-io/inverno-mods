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
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.security.jose.JOSEObjectBuildException;
import io.inverno.mod.security.jose.JOSEObjectReadException;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.internal.jwe.GenericJWEReader;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwe.JWEReadException;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwt.JWTReadException;
import java.lang.reflect.Type;
import java.util.List;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JSON Web Token JWE reader.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWT Claims Set type
 */
class JWTEReader<A> extends GenericJWEReader<A> {

	/**
	 * <p>
	 * Creates a JWT JWE reader.
	 * </p>
	 * 
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the expected JWT Claims Set type
	 * @param keys                  the keys to consider to decrypt the CEK
	 * @param zips                  a list of supported JWE compression algorithms
	 */
	public JWTEReader(ObjectMapper mapper, GenericDataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, List<JWEZip> zips) {
		super(mapper, dataConversionService, jwkService, type, keys, zips);
	}

	@Override
	public Mono<JWE<A>> read(String compact, String contentType) throws JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		return super.read(compact, MediaTypes.APPLICATION_JSON);
	}
	
	@Override
	protected void checkHeader(JWEHeader header) throws JWTReadException, JWEReadException, JWEBuildException, JOSEObjectReadException, JOSEObjectBuildException, JOSEProcessingException {
		super.checkHeader(header);
		
		String joseType = header.getType();
		if(joseType != null && !joseType.equals("JWT")) {
			throw new JWTReadException("Invalid JWT, type is not set to JWT: " + joseType);
		}
		String joseContentType = header.getContentType();
		if(joseContentType != null) {
			throw new JWTReadException("Invalid JWT, content type must not be set: " + joseContentType);
		}
	}
}
