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
package io.inverno.mod.security.jose.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.JsonJOSEObject;
import io.inverno.mod.security.jose.internal.jwe.GenericJsonJWE;
import io.inverno.mod.security.jose.internal.jws.GenericJsonJWS;

/**
 * <p>
 * Base JSON JOSE Object implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GenericJsonJWS
 * @see GenericJsonJWE
 * 
 * @param <A> the payload type
 */
public abstract class AbstractJsonJOSEObject<A> implements JsonJOSEObject<A> {
	
	/**
	 * The object mapper.
	 */
	protected final ObjectMapper mapper;

	/**
	 * <p>
	 * Creates a JSON JOSE object.
	 * </p>
	 * 
	 * @param mapper an object mapper
	 */
	public AbstractJsonJOSEObject(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public String toJson() throws JOSEProcessingException {
		try {
			return this.mapper.writeValueAsString(this);
		} 
		catch (JsonProcessingException e) {
			throw new JOSEProcessingException(e);
		}
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
}
