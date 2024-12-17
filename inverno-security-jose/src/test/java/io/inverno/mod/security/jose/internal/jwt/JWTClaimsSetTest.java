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

package io.inverno.mod.security.jose.internal.jwt;

import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTClaimsSetValidator;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 */
public class JWTClaimsSetTest {

	@Test
	public void jwtClaimsSetValidationTest() {
		JWTClaimsSet claims = JWTClaimsSet.of("iss", ZonedDateTime.now().plusSeconds(5).toEpochSecond()).build();
		
		Assertions.assertTrue(claims.isValid());
		
		claims = JWTClaimsSet.of("iss", ZonedDateTime.now().minusSeconds(5).toEpochSecond()).build();
		
		Assertions.assertFalse(claims.isValid());
		
		claims.setValidators(null);
		claims.validate(JWTClaimsSetValidator.expiration(ZonedDateTime.now().minusSeconds(10)));
		
		Assertions.assertTrue(claims.isValid());
		
		claims = JWTClaimsSet.of("iss", ZonedDateTime.now().plusMinutes(30).toEpochSecond()).build();
		claims.validate(JWTClaimsSetValidator.issuer("iss"));
		
		Assertions.assertTrue(claims.isValid());
		
		claims.setValidators(null);
		claims.validate(JWTClaimsSetValidator.issuer("bad"));
		
		Assertions.assertFalse(claims.isValid());
	}
}
