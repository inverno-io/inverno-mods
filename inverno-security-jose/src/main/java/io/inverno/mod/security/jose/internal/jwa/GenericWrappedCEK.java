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

import io.inverno.mod.security.jose.jwa.WrappingJWAKeyManager;
import java.util.Map;

/**
 * <p>
 * Generic {@link WrappingJWAKeyManager.WrappedCEK} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericWrappedCEK implements WrappingJWAKeyManager.WrappedCEK {

	private final byte[] wrappedKey;
	
	private final Map<String, Object> moreHeaderParameters;

	/**
	 * <p>
	 * Creates a generic wrapped CEK.
	 * </p>
	 *
	 * @param wrappedKey the wrapped content encryption key
	 */
	public GenericWrappedCEK(byte[] wrappedKey) {
		this(wrappedKey, null);
	}
	
	/**
	 * <p>
	 * Creates a generic wrapped CEK with custom parameters.
	 * </p>
	 *
	 * @param wrappedKey           the wrapped content encryption key
	 * @param moreHeaderParameters more header parameters map
	 */
	public GenericWrappedCEK(byte[] wrappedKey, Map<String, Object> moreHeaderParameters) {
		this.wrappedKey = wrappedKey;
		this.moreHeaderParameters = moreHeaderParameters;
	}
	
	@Override
	public byte[] getWrappedKey() {
		return this.wrappedKey;
	}

	@Override
	public Map<String, Object> getMoreHeaderParameters() {
		return this.moreHeaderParameters;
	}
}
