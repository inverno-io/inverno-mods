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
package io.inverno.mod.security.jose.jwa;

/**
 * <p>
 * A JSON Web Algorithm for Key Management when performing JSON Web Encryption as specified by <a href="https://datatracker.ietf.org/doc/html/rfc7518#section-4">RFC7518 Section 4</a>.
 * </p>
 * 
 * <p>We can differentiate three kinds of Key Management algorithm:</p>
 * 
 * <ul>
 * <li><em>Encrypting</em> where a Content Encryption Key (or CEK) is encrypted.</li>
 * <li><em>Wrapping</em> where a Content Encryption Key (or CEK), which can be derived, is wrapped.</li>
 * <li><em>Direct</em> where no Content Encryption Key (or CEK) is generated and a symmetric key or a derived key is directly used to encrypt content.</li>
 * </ul>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public interface JWAKeyManager extends JWA {
	
}
