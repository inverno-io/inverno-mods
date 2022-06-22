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

import java.security.SecureRandom;
import java.util.Base64;

/**
 * <p>
 * Password utilities class
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
final class PasswordUtils {
	
	/**
	 * Default secure random.
	 */
	public static final SecureRandom DEFAULT_SECURE_RANDOM = new SecureRandom();
	
	/**
	 * Base64 encoder without padding.
	 */
	public static final Base64.Encoder BASE64_NOPAD_ENCODER = Base64.getEncoder().withoutPadding();
	
	/**
	 * Base64URL encoder without padding.
	 */
	public static final Base64.Encoder BASE64_NOPAD_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	
	private PasswordUtils() {}
	
	/**
	 * <p>
	 * Generates a salt of the specified length using the specified secure random.
	 * </p>
	 * 
	 * @param secureRandom a secure random
	 * @param length the initialization vector length
	 * 
	 * @return a salt
	 */
	public static byte[] generateSalt(SecureRandom secureRandom, int length) {
		byte[] salt = new byte[length];
		secureRandom.nextBytes(salt);
		return salt;
	}
}
