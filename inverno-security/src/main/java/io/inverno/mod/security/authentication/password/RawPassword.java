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
 * Represents a raw unencoded password.
 * </p>
 * 
 * <p>
 * This password implementation is not secured and <b>MUST NOT</b> be used for password serialization, in particular for password storage. It should only be considered during an authentication process
 * to represent user provided passwords.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class RawPassword extends AbstractPassword<RawPassword, RawPassword.Encoder> {

	/**
	 * The raw (no-op) password encoder.
	 */
	protected static final RawPassword.Encoder RAW_PASSWORD_ENCODER = new RawPassword.Encoder();
	
	/**
	 * <p>
	 * Creates a raw password with the specified value.
	 * </p>
	 * 
	 * @param raw the raw password
	 */
	public RawPassword(String raw) {
		super(raw, RAW_PASSWORD_ENCODER);
	}
	
	/**
	 * <p>
	 * Creates a raw password with the specified value and encoder.
	 * </p>
	 * 
	 * <p>
	 * This constructor is only used when recovering a raw password from an encoder (see {@link RawPassword.Encoder#recover(java.lang.String)}).
	 * </p>
	 * 
	 * @param raw the raw password
	 * @param encoder the password encoder (should always be {@link #RAW_PASSWORD_ENCODER})
	 */
	protected RawPassword(String raw, RawPassword.Encoder encoder) {
		super(raw, encoder);
	}

	@Override
	@JsonIgnore // we don't want to serialize raw password
	public String getValue() {
		return super.getValue();
	}
	
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 47 * hash + Objects.hashCode(this.value);
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
		final RawPassword other = (RawPassword) obj;
		return Objects.equals(this.value, other.value);
	}
	
	/**
	 * <p>
	 * A raw (no-op) password encoder implementation.
	 * </p>
	 * 
	 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
	 * @since 1.5
	 */
	public static class Encoder implements Password.Encoder<RawPassword, RawPassword.Encoder> {

		@Override
		public RawPassword recover(String encoded) throws PasswordException {
			// TODO check that the encoded value is correct (length, format...)
			return new RawPassword(encoded, this);
		}
		
		@Override
		public RawPassword encode(String raw) throws PasswordException {
			return new RawPassword(raw);
		}

		@Override
		public boolean matches(String raw, String encoded) throws PasswordException {
			return raw.equals(encoded);
		}
	}
}
