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
package io.inverno.mod.security.authentication.password;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

/**
 * <p>
 * Base password implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 * 
 * @param <A> the password type
 * @param <B> the password encoder type
 */
public abstract class AbstractPassword<A extends Password<A, B>, B extends Password.Encoder<A, B>> implements Password<A, B> {

	/**
	 * The encoded password value.
	 */
	@JsonIgnore
	protected final String value;
	
	/**
	 * The password encoder.
	 */
	@JsonIgnore
	protected final B encoder;
	
	/**
	 * <p>
	 * Creates a password with the specified value and encoder.
	 * </p>
	 *
	 * @param encoded the encoded password value
	 * @param encoder the password encoder
	 */
	protected AbstractPassword(String encoded, B encoder) {
		this.value = Objects.requireNonNull(encoded);
		this.encoder = Objects.requireNonNull(encoder);
	}
	
	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public B getEncoder() {
		return this.encoder;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 47 * hash + Objects.hashCode(this.value);
		hash = 47 * hash + Objects.hashCode(this.encoder);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final AbstractPassword<?, ?> other = (AbstractPassword<?, ?>) obj;
		if (!Objects.equals(this.value, other.value)) {
			return false;
		}
		return Objects.equals(this.encoder, other.encoder);
	}
}
