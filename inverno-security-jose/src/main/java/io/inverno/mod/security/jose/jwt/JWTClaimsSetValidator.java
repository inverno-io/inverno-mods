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
package io.inverno.mod.security.jose.jwt;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * <p>
 * A JWT claims set validator used to validate a {@link JWTClaimsSet}.
 * </p>
 *
 * @author <a href="jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
@FunctionalInterface
public interface JWTClaimsSetValidator {

	/**
	 * <p>
	 * Determines wether the specified claims set is valid.
	 * </p>
	 * 
	 * @param claims a JWT claims set
	 * 
	 * @throws InvalidJWTException if the specified claims set was invalid
	 */
	void validate(JWTClaimsSet claims) throws InvalidJWTException;
	
	/**
	 * <p>
	 * Returns a JWT claims set validator that validates that the JWT issuer corresponds to the specified trusted issuer.
	 * </p>
	 * 
	 * @param iss the trusted issuer
	 * 
	 * @return a JWT claims set validator
	 */
	static JWTClaimsSetValidator issuer(String iss) {
		return claims -> {
			if(iss != null && !iss.equals(claims.getIssuer())) {
				throw new InvalidJWTException("Invalid issuer: " + claims.getIssuer());
			}
		};
	}
	
	/**
	 * <p>
	 * Returns a JWT claims set validator that validates that the JWT audience corresponds to trusted audiences.
	 * </p>
	 * 
	 * @param aud a set of trusted audience
	 * 
	 * @return a JWT claims set validator
	 */
	static JWTClaimsSetValidator audience(Set<String> aud) {
		return claims -> {
			if(aud != null && !aud.isEmpty() && !aud.contains(claims.getAudience())) {
				throw new InvalidJWTException("Invalid audience: " + claims.getAudience());
			}
		};
	}
	
	/**
	 * <p>
	 * Returns a JWT claims set validator that validates that the JWT subject corresponds to the specified trusted subject.
	 * </p>
	 * 
	 * @param sub the trusted subject
	 * 
	 * @return a JWT claims set validator
	 */
	static JWTClaimsSetValidator subject(String sub) {
		return claims -> {
			if(sub != null && !sub.equals(claims.getSubject())) {
				throw new InvalidJWTException("Invalid subject: " + claims.getSubject());
			}
		};
	}
	
	/**
	 * <p>
	 * Returns a JWT claims set validator that validates that the JWT expiration time is after current date time.
	 * </p>
	 * 
	 * @return a JWT claims set validator
	 */
	static JWTClaimsSetValidator expiration() {
		return claims -> {
			ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
			ZonedDateTime exp = claims.getExpirationTimeAsDateTime();
			if(exp != null && now.isAfter(exp)) {
				throw new ExpiredJWTException("Token has expired");
			}
		};
	}
	
	/**
	 * <p>
	 * Returns a JWT claims set validator that validates that the JWT expiration time is after the specified expiration time.
	 * </p>
	 * 
	 * @param time the expiration time reference
	 * 
	 * @return a JWT claims set validator
	 */
	static JWTClaimsSetValidator expiration(ZonedDateTime time) {
		return claims -> {
			ZonedDateTime exp = claims.getExpirationTimeAsDateTime();
			if(exp != null && time.isAfter(exp)) {
				throw new ExpiredJWTException("Token has expired");
			}
		};
	}
	
	/**
	 * <p>
	 * Returns a JWT claims set validator that validates that the JWT not before time is after the current date time.
	 * </p>
	 * 
	 * @return a JWT claims set validator
	 */
	static JWTClaimsSetValidator notBefore() {
		return claims -> {
			ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
			ZonedDateTime nbf = claims.getNotBeforeAsDateTime();
			if(nbf != null && now.isBefore(nbf)) {
				throw new InactiveJWTException("Token is not active yet");
			}
		};
	}
	
	/**
	 * <p>
	 * Returns a JWT claims set validator that validates that the JWT not before time is after the specified activation time.
	 * </p>
	 * 
	 * @param time the activtion time reference
	 * 
	 * @return a JWT claims set validator
	 */
	static JWTClaimsSetValidator notBefore(ZonedDateTime time) {
		return claims -> {
			ZonedDateTime nbf = claims.getNotBeforeAsDateTime();
			if(nbf != null && time.isBefore(nbf)) {
				throw new InactiveJWTException("Token is not active yet");
			}
		};
	}
}
