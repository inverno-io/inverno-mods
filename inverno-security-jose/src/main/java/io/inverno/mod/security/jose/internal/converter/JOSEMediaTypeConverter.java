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

import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jwt.JWTService;

/**
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public interface JOSEMediaTypeConverter extends MediaTypeConverter<String> {

	/**
	 * <p>
	 * Injects the JWE service into the data conversion service.
	 * </p>
	 * 
	 * <p>
	 * The JWE service also depends on the data conversion service, so we can't rely on IoC/DI to inject it since it would introduce a dependency cycle. As a result, it has to be done explicitly when
	 * the JWE service instance is initialized.
	 * </p>
	 * 
	 * @param jweService the JWE service
	 */
	void injectJWEService(JWEService jweService);
	
	/**
	 * <p>
	 * Injects the JWS service into the data conversion service.
	 * </p>
	 * 
	 * <p>
	 * The JWS service also depends on the data conversion service, so we can't rely on IoC/DI to inject it since it would introduce a dependency cycle. As a result, it has to be done explicitly when
	 * the JWS service instance is initialized.
	 * </p>
	 * 
	 * @param jwsService the JWS service
	 */
	void injectJWSService(JWSService jwsService);
	
	/**
	 * <p>
	 * Injects the JWT service into the data conversion service.
	 * </p>
	 * 
	 * <p>
	 * The JWT service also depends on the data conversion service, so we can't rely on IoC/DI to inject it since it would introduce a dependency cycle. As a result, it has to be done explicitly when
	 * the JWT service instance is initialized.
	 * </p>
	 * 
	 * @param jwtService the JWT service
	 */
	void injectJWTService(JWTService jwtService);
}
