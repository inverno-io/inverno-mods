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

import io.inverno.mod.security.jose.jwa.DirectJWAKeyManager;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import java.util.Map;

/**
 * <p>
 * Generic {@link DirectJWAKeyManager.DirectCEK} implementation.
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericDirectCEK implements DirectJWAKeyManager.DirectCEK {

	private final OCTJWK encryptionKey;
	
	private final Map<String, Object> moreHeaderParameters;

	/**
	 * <p>
	 * Creates a generic direct CEK.
	 * </p>
	 * 
	 * @param encryptionKey the content encryption key
	 */
	public GenericDirectCEK(OCTJWK encryptionKey) {
		this(encryptionKey, null);
	}
	
	/**
	 * <p>
	 * Creates a generic direct CEK with custom parameters.
	 * </p>
	 *
	 * @param encryptionKey        the content encryption key
	 * @param moreHeaderParameters more header parameters map
	 */
	public GenericDirectCEK(OCTJWK encryptionKey, Map<String, Object> moreHeaderParameters) {
		this.encryptionKey = encryptionKey;
		this.moreHeaderParameters = moreHeaderParameters;
	}
	
	@Override
	public OCTJWK getEncryptionKey() {
		return this.encryptionKey;
	}

	@Override
	public Map<String, Object> getMoreHeaderParameters() {
		return this.moreHeaderParameters;
	}
}
