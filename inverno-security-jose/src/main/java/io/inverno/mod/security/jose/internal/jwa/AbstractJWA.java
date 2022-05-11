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
package io.inverno.mod.security.jose.internal.jwa;

import io.inverno.mod.security.jose.jwa.JWA;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import java.util.Set;

/**
 * <p>
 * Base JSON Web Algorithm implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public abstract class AbstractJWA implements JWA {

	/**
	 * <p>
	 * Initializes the JSON Web Algorithm.
	 * </p>
	 * 
	 * @throws JWAProcessingException if there was an error initializing the JWA
	 */
	protected abstract void init() throws JWAProcessingException;
	
	@Override
	public Set<String> getProcessedParameters() {
		return Set.of();
	}
}
