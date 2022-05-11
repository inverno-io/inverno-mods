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

import io.inverno.mod.security.jose.JOSEHeader;
import io.inverno.mod.security.jose.JOSEObject;
import io.inverno.mod.security.jose.internal.jwe.GenericJWE;
import io.inverno.mod.security.jose.internal.jws.GenericJWS;
import java.util.Objects;

/**
 * <p>
 * Base JOSE object implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GenericJWS
 * @see GenericJWE
 * 
 * @param <A> the payload type
 * @param <B> the JOSE header type
 * @param <C> the JOSE payload type
 */
public abstract class AbstractJOSEObject<A, B extends JOSEHeader, C extends AbstractJOSEPayload<A>> implements JOSEObject<A, B> {
	
	/**
	 * The JOSE header.
	 */
	protected final B header;
	
	/**
	 * The JOSE payload.
	 */
	protected final C payload;

	/**
	 * <p>
	 * Creates a JOSE object with the specified JOSE header and JOSE payload.
	 * </p>
	 * 
	 * @param header  the JOSE header
	 * @param payload the JOSE payload
	 */
	public AbstractJOSEObject(B header, C payload) {
		this.header = header;
		this.payload = payload;
	}

	@Override
	public B getHeader() {
		return this.header;
	}

	@Override
	public A getPayload() {
		return this.payload.getValue();
	}

	@Override
	public int hashCode() {
		return Objects.hash(header, payload);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractJOSEObject<?, ?, ?> other = (AbstractJOSEObject<?, ?, ?>) obj;
		return Objects.equals(header, other.header) && Objects.equals(payload, other.payload);
	}
}
