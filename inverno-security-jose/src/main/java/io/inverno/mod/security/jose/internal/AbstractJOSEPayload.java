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

import io.inverno.mod.security.jose.internal.jwe.GenericJWEPayload;
import io.inverno.mod.security.jose.internal.jws.GenericJWSPayload;
import java.util.Objects;

/**
 * <p>
 * Base JOSE payload which holds the actual JOSE object payload, the serialized raw representation and the Base64URL encoded representation without padding.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @see GenericJWSPayload
 * @see GenericJWEPayload
 * 
 * @param <A> the payload type
 */
public abstract class AbstractJOSEPayload<A> {
	
	/**
	 * The payload value.
	 */
	protected final A value;
	
	/**
	 * The raw representation of the payload.
	 */
	protected String raw;
	
	/**
	 * The Base64URL encoded payload without padding.
	 */
	protected String encoded;

	/**
	 * <p>
	 * Creates a JOSE payload.
	 * </p>
	 * 
	 * @param value the actual payload
	 */
	public AbstractJOSEPayload(A value) {
		this.value = value;
	}

	/**
	 * <p>
	 * Returns the actual payload
	 * </p>
	 * 
	 * @return the payload value
	 */
	public A getValue() {
		return value;
	}
	
	/**
	 * <p>
	 * Sets the serialized raw representation.
	 * </p>
	 * 
	 * @param raw the serialized payload
	 */
	public void setRaw(String raw) {
		this.raw = raw;
	}
	
	/**
	 * <p>
	 * Returns the serialized raw representation.
	 * </p>
	 * 
	 * @return the serialiazed payload
	 */
	public String getRaw() {
		return raw;
	}

	/**
	 * <p>
	 * Sets the Base64URL encoded representation.
	 * </p>
	 * 
	 * @param encoded the Base64URL payload without padding
	 */
	public void setEncoded(String encoded) {
		this.encoded = encoded;
	}
	
	/**
	 * <p>
	 * Returns the Base64URL encoded representation.
	 * </p>
	 * 
	 * @return the Base64URL payload without padding 
	 */
	public String getEncoded() {
		return encoded;
	}

	@Override
	public int hashCode() {
		return Objects.hash(encoded, raw, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractJOSEPayload<?> other = (AbstractJOSEPayload<?>) obj;
		return Objects.equals(encoded, other.encoded) && Objects.equals(raw, other.raw)
				&& Objects.equals(value, other.value);
	}
}
