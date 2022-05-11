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

import io.inverno.mod.security.jose.internal.AbstractJOSEPayload;

/**
 * <p>
 * Generic JWS payload implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the actual payload type
 */
public class GenericJWSPayload<A> extends AbstractJOSEPayload<A> {

	/**
	 * <p>
	 * Creates a JWS payload
	 * </p>
	 * 
	 * @param value the actual payload value
	 */
	public GenericJWSPayload(A value) {
		super(value);
	}
}
