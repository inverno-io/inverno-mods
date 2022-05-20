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
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.DataConversionService;
import io.inverno.mod.security.jose.internal.jwe.GenericJWEBuilder;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEBuildException;
import io.inverno.mod.security.jose.jwe.JWEHeader;
import io.inverno.mod.security.jose.jwe.JWEZip;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwt.JWTBuildException;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import java.lang.reflect.Type;
import java.util.List;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * <p>
 * A JSON Web Token JWE builder.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the JWT Claims Set type
 */
class JWTEBuilder<A extends JWTClaimsSet> extends GenericJWEBuilder<A> {

	/**
	 * <p>
	 * Creates a JWT JWE builder.
	 * </p>
	 *
	 * @param mapper                an object mapper
	 * @param dataConversionService a data conversion service
	 * @param jwkService            a JWK service
	 * @param type                  the JWT Claims Set type
	 * @param keys                  the keys to consider to secure the CEK
	 * @param zips                  a list of supported JWE compression algorithms
	 */
	public JWTEBuilder(ObjectMapper mapper, DataConversionService dataConversionService, JWKService jwkService, Type type, Publisher<? extends JWK> keys, List<JWEZip> zips) {
		super(mapper, dataConversionService, jwkService, type, keys, zips);
	}

	@Override
	public Mono<JWE<A>> build(String contentType) throws JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		return super.build(MediaTypes.APPLICATION_JSON);
	}
	
	@Override
	protected void checkHeader(JWEHeader header) throws JWTBuildException, JWEBuildException, JOSEObjectBuildException, JOSEProcessingException {
		super.checkHeader(header);
		if(header.getContentType() != null) {
			throw new JWTBuildException("Content type is not allowed in JWT header: " + header.getContentType());
		}
		String headerType = header.getType();
		if(headerType != null && !headerType.equals("JWT")) {
			throw new JWTBuildException("Type must be JWT: " + headerType);
		}
	}
}
