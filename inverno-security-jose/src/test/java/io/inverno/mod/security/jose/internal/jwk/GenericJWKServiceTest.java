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
package io.inverno.mod.security.jose.internal.jwk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKFactory;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKFactory;
import io.inverno.mod.security.jose.internal.jws.GenericJWSHeader;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwa.JWAProcessingException;
import io.inverno.mod.security.jose.jwa.JWASignatureException;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwk.InMemoryJWKStore;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKPKIXParameters;
import io.inverno.mod.security.jose.jwk.JWKProcessingException;
import io.inverno.mod.security.jose.jwk.JWKReadException;
import io.inverno.mod.security.jose.jwk.JWKResolveException;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import io.inverno.mod.security.jose.jwk.okp.OKPJWK;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import java.io.ByteArrayInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericJWKServiceTest {
	
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final ExecutorService WORKER_POOL = Executors.newCachedThreadPool();
	
	@Test
	public void testRFC7517_A1() {
		JWKService jwkService = jwkService();
		
		String jwkStr = "{\n" +
			"	\"keys\":[\n" +
			"		{\n" +
			"			\"kty\":\"EC\",\n" +
			"			\"crv\":\"P-256\",\n" +
			"			\"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\n" +
			"			\"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\n" +
			"			\"use\":\"enc\",\n" +
			"			\"kid\":\"1\"\n" +
			"		},\n" +
			"		{\n" +
			"			\"kty\":\"RSA\",\n" +
			"			\"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\",\n" +
			"			\"e\":\"AQAB\",\n" +
			"			\"alg\":\"RS256\",\n" +
			"			\"kid\":\"2011-04-29\"\n" +
			"		}\n" +
			"	]\n" +
			"}";
		
		List<? extends JWK> jwks = Flux.from(jwkService.read(jwkStr)).collectList().block();
		
		Assertions.assertEquals(2, jwks.size());
		
		Assertions.assertInstanceOf(ECJWK.class, jwks.get(0));
		ECJWK ecJWK = (ECJWK)jwks.get(0);
		Assertions.assertEquals(ECJWK.KEY_TYPE, ecJWK.getKeyType());
		Assertions.assertEquals(ECCurve.P_256.getCurve(), ecJWK.getCurve());
		Assertions.assertEquals("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4", ecJWK.getXCoordinate());
		Assertions.assertEquals("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM", ecJWK.getYCoordinate());
		Assertions.assertEquals(JWK.USE_ENC, ecJWK.getPublicKeyUse());
		Assertions.assertEquals("1", ecJWK.getKeyId());
		Assertions.assertNull(ecJWK.getX509CertificateChain());
		Assertions.assertNull(ecJWK.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(ecJWK.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(ecJWK.getX509CertificateURL());
		Assertions.assertNull(ecJWK.getAlgorithm());
		Assertions.assertNull(ecJWK.getKeyOperations());
		
		Assertions.assertThrows(JWKProcessingException.class, () -> ecJWK.signer(ECAlgorithm.ES256.getAlgorithm()), "JWK is not to be used for signing data");
		Assertions.assertThrows(JWKProcessingException.class, () -> ecJWK.signer(ECAlgorithm.ES256.getAlgorithm()), "JWK is not to be used for verifying signature");
		// TODO encrypter/decrypter
		
		Assertions.assertInstanceOf(RSAJWK.class, jwks.get(1));
		RSAJWK rsaJWK = (RSAJWK)jwks.get(1);
		Assertions.assertEquals(RSAJWK.KEY_TYPE, rsaJWK.getKeyType());
		Assertions.assertEquals("0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw", rsaJWK.getModulus());
		Assertions.assertEquals("AQAB", rsaJWK.getPublicExponent());
		Assertions.assertEquals(RSAAlgorithm.RS256.getAlgorithm(), rsaJWK.getAlgorithm());
		Assertions.assertEquals("2011-04-29", rsaJWK.getKeyId());
		Assertions.assertNull(rsaJWK.getX509CertificateChain());
		Assertions.assertNull(rsaJWK.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(rsaJWK.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(rsaJWK.getX509CertificateURL());
		Assertions.assertNull(rsaJWK.getKeyOperations());
		Assertions.assertNull(rsaJWK.getPrivateExponent());
		Assertions.assertNull(rsaJWK.getFirstPrimeFactor());
		Assertions.assertNull(rsaJWK.getSecondPrimeFactor());
		Assertions.assertNull(rsaJWK.getFirstFactorExponent());
		Assertions.assertNull(rsaJWK.getSecondFactorExponent());
		Assertions.assertNull(rsaJWK.getFirstCoefficient());
		Assertions.assertNull(rsaJWK.getOtherPrimesInfo());
		
		Assertions.assertThrows(JWASignatureException.class, () -> rsaJWK.signer(RSAAlgorithm.RS256.getAlgorithm()).sign(new byte[0]), "JWK is missing RSA private exponent");
		Assertions.assertNotNull(rsaJWK.signer(RSAAlgorithm.RS256.getAlgorithm()));
		
		Assertions.assertThrows(JWKProcessingException.class, () -> rsaJWK.signer(RSAAlgorithm.RS512.getAlgorithm()), "JWK algorithm RS256 does not match RS512");
		Assertions.assertThrows(JWKProcessingException.class, () -> rsaJWK.signer(RSAAlgorithm.RS512.getAlgorithm()), "JWK algorithm RS256 does not match RS512");
	}
	
	@Test
	public void testRFC7517_A2() {
		JWKService jwkService = jwkService();
		
		String jwkStr = "{\n" +
			"	\"keys\": [\n" +
			"		{\n" +
			"			\"kty\": \"EC\",\n" +
			"			\"crv\": \"P-256\",\n" +
			"			\"x\": \"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\",\n" +
			"			\"y\": \"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\",\n" +
			"			\"d\": \"870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE\",\n" +
			"			\"use\": \"enc\",\n" +
			"			\"kid\": \"1\"\n" +
			"		},\n" +
			"		{\n" +
			"			\"kty\": \"RSA\",\n" +
			"			\"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\",\n" +
			"			\"e\": \"AQAB\",\n" +
			"			\"d\": \"X4cTteJY_gn4FYPsXB8rdXix5vwsg1FLN5E3EaG6RJoVH-HLLKD9M7dx5oo7GURknchnrRweUkC7hT5fJLM0WbFAKNLWY2vv7B6NqXSzUvxT0_YSfqijwp3RTzlBaCxWp4doFk5N2o8Gy_nHNKroADIkJ46pRUohsXywbReAdYaMwFs9tv8d_cPVY3i07a3t8MN6TNwm0dSawm9v47UiCl3Sk5ZiG7xojPLu4sbg1U2jx4IBTNBznbJSzFHK66jT8bgkuqsk0GjskDJk19Z4qwjwbsnn4j2WBii3RL-Us2lGVkY8fkFzme1z0HbIkfz0Y6mqnOYtqc0X4jfcKoAC8Q\",\n" +
			"			\"p\": \"83i-7IvMGXoMXCskv73TKr8637FiO7Z27zv8oj6pbWUQyLPQBQxtPVnwD20R-60eTDmD2ujnMt5PoqMrm8RfmNhVWDtjjMmCMjOpSXicFHj7XOuVIYQyqVWlWEh6dN36GVZYk93N8Bc9vY41xy8B9RzzOGVQzXvNEvn7O0nVbfs\",\n" +
			"			\"q\": \"3dfOR9cuYq-0S-mkFLzgItgMEfFzB2q3hWehMuG0oCuqnb3vobLyumqjVZQO1dIrdwgTnCdpYzBcOfW5r370AFXjiWft_NGEiovonizhKpo9VVS78TzFgxkIdrecRezsZ-1kYd_s1qDbxtkDEgfAITAG9LUnADun4vIcb6yelxk\",\n" +
			"			\"dp\": \"G4sPXkc6Ya9y8oJW9_ILj4xuppu0lzi_H7VTkS8xj5SdX3coE0oimYwxIi2emTAue0UOa5dpgFGyBJ4c8tQ2VF402XRugKDTP8akYhFo5tAA77Qe_NmtuYZc3C3m3I24G2GvR5sSDxUyAN2zq8Lfn9EUms6rY3Ob8YeiKkTiBj0\",\n" +
			"			\"dq\": \"s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6huUUvMfBcMpn8lqeW6vzznYY5SSQF7pMdC_agI3nG8Ibp1BUb0JUiraRNqUfLhcQb_d9GF4Dh7e74WbRsobRonujTYN1xCaP6TO61jvWrX-L18txXw494Q_cgk\",\n" +
			"			\"qi\": \"GyM_p6JrXySiz1toFgKbWV-JdI3jQ4ypu9rbMWx3rQJBfmt0FoYzgUIZEVFEcOqwemRN81zoDAaa-Bk0KWNGDjJHZDdDmFhW3AN7lI-puxk_mHZGJ11rxyR8O55XLSe3SPmRfKwZI6yU24ZxvQKFYItdldUKGzO6Ia6zTKhAVRU\",\n" +
			"			\"alg\": \"RS256\",\n" +
			"			\"kid\": \"2011-04-29\"\n" +
			"		}\n" +
			"	]\n" +
			"}";
		
		List<? extends JWK> jwks = Flux.from(jwkService.read(jwkStr)).collectList().block();
		
		Assertions.assertEquals(2, jwks.size());
		
		Assertions.assertInstanceOf(ECJWK.class, jwks.get(0));
		ECJWK ecJWK = (ECJWK)jwks.get(0);
		Assertions.assertEquals(ECJWK.KEY_TYPE, ecJWK.getKeyType());
		Assertions.assertEquals(ECCurve.P_256.getCurve(), ecJWK.getCurve());
		Assertions.assertEquals("MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4", ecJWK.getXCoordinate());
		Assertions.assertEquals("4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM", ecJWK.getYCoordinate());
		Assertions.assertEquals("870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE", ecJWK.getEccPrivateKey());
		Assertions.assertEquals(JWK.USE_ENC, ecJWK.getPublicKeyUse());
		Assertions.assertEquals("1", ecJWK.getKeyId());
		Assertions.assertNull(ecJWK.getX509CertificateChain());
		Assertions.assertNull(ecJWK.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(ecJWK.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(ecJWK.getX509CertificateURL());
		Assertions.assertNull(ecJWK.getAlgorithm());
		Assertions.assertNull(ecJWK.getKeyOperations());
		
		Assertions.assertThrows(JWKProcessingException.class, () -> ecJWK.signer(ECAlgorithm.ES256.getAlgorithm()), "JWK is not to be used for signing data");
		Assertions.assertThrows(JWKProcessingException.class, () -> ecJWK.signer(ECAlgorithm.ES256.getAlgorithm()), "JWK is not to be used for verifying signature");
		// TODO encrypter/decrypter
		
		Assertions.assertInstanceOf(RSAJWK.class, jwks.get(1));
		RSAJWK rsaJWK = (RSAJWK)jwks.get(1);
		Assertions.assertEquals(RSAJWK.KEY_TYPE, rsaJWK.getKeyType());
		Assertions.assertEquals("0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw", rsaJWK.getModulus());
		Assertions.assertEquals("AQAB", rsaJWK.getPublicExponent());
		Assertions.assertEquals("X4cTteJY_gn4FYPsXB8rdXix5vwsg1FLN5E3EaG6RJoVH-HLLKD9M7dx5oo7GURknchnrRweUkC7hT5fJLM0WbFAKNLWY2vv7B6NqXSzUvxT0_YSfqijwp3RTzlBaCxWp4doFk5N2o8Gy_nHNKroADIkJ46pRUohsXywbReAdYaMwFs9tv8d_cPVY3i07a3t8MN6TNwm0dSawm9v47UiCl3Sk5ZiG7xojPLu4sbg1U2jx4IBTNBznbJSzFHK66jT8bgkuqsk0GjskDJk19Z4qwjwbsnn4j2WBii3RL-Us2lGVkY8fkFzme1z0HbIkfz0Y6mqnOYtqc0X4jfcKoAC8Q", rsaJWK.getPrivateExponent());
		Assertions.assertEquals("83i-7IvMGXoMXCskv73TKr8637FiO7Z27zv8oj6pbWUQyLPQBQxtPVnwD20R-60eTDmD2ujnMt5PoqMrm8RfmNhVWDtjjMmCMjOpSXicFHj7XOuVIYQyqVWlWEh6dN36GVZYk93N8Bc9vY41xy8B9RzzOGVQzXvNEvn7O0nVbfs", rsaJWK.getFirstPrimeFactor());
		Assertions.assertEquals("3dfOR9cuYq-0S-mkFLzgItgMEfFzB2q3hWehMuG0oCuqnb3vobLyumqjVZQO1dIrdwgTnCdpYzBcOfW5r370AFXjiWft_NGEiovonizhKpo9VVS78TzFgxkIdrecRezsZ-1kYd_s1qDbxtkDEgfAITAG9LUnADun4vIcb6yelxk", rsaJWK.getSecondPrimeFactor());
		Assertions.assertEquals("G4sPXkc6Ya9y8oJW9_ILj4xuppu0lzi_H7VTkS8xj5SdX3coE0oimYwxIi2emTAue0UOa5dpgFGyBJ4c8tQ2VF402XRugKDTP8akYhFo5tAA77Qe_NmtuYZc3C3m3I24G2GvR5sSDxUyAN2zq8Lfn9EUms6rY3Ob8YeiKkTiBj0", rsaJWK.getFirstFactorExponent());
		Assertions.assertEquals("s9lAH9fggBsoFR8Oac2R_E2gw282rT2kGOAhvIllETE1efrA6huUUvMfBcMpn8lqeW6vzznYY5SSQF7pMdC_agI3nG8Ibp1BUb0JUiraRNqUfLhcQb_d9GF4Dh7e74WbRsobRonujTYN1xCaP6TO61jvWrX-L18txXw494Q_cgk", rsaJWK.getSecondFactorExponent());
		Assertions.assertEquals("GyM_p6JrXySiz1toFgKbWV-JdI3jQ4ypu9rbMWx3rQJBfmt0FoYzgUIZEVFEcOqwemRN81zoDAaa-Bk0KWNGDjJHZDdDmFhW3AN7lI-puxk_mHZGJ11rxyR8O55XLSe3SPmRfKwZI6yU24ZxvQKFYItdldUKGzO6Ia6zTKhAVRU", rsaJWK.getFirstCoefficient());
		Assertions.assertEquals(RSAAlgorithm.RS256.getAlgorithm(), rsaJWK.getAlgorithm());
		Assertions.assertEquals("2011-04-29", rsaJWK.getKeyId());
		Assertions.assertNull(rsaJWK.getX509CertificateChain());
		Assertions.assertNull(rsaJWK.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(rsaJWK.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(rsaJWK.getX509CertificateURL());
		Assertions.assertNull(rsaJWK.getKeyOperations());
		Assertions.assertNull(rsaJWK.getOtherPrimesInfo());
		
		Assertions.assertNotNull(rsaJWK.signer(RSAAlgorithm.RS256.getAlgorithm()));
		Assertions.assertNotNull(rsaJWK.signer(RSAAlgorithm.RS256.getAlgorithm()));
		
		Assertions.assertThrows(JWKProcessingException.class, () -> rsaJWK.signer(RSAAlgorithm.RS512.getAlgorithm()), "JWK algorithm RS256 does not match RS512");
		Assertions.assertThrows(JWKProcessingException.class, () -> rsaJWK.signer(RSAAlgorithm.RS512.getAlgorithm()), "JWK algorithm RS256 does not match RS512");
	}
	
	@Test
	public void testRFC7517_A3() {
		JWKService jwkService = jwkService();
		
		String jwkStr = "{\n" +
			"	\"keys\": [\n" +
			"		{\n" +
			"			\"kty\": \"oct\",\n" +
			"			\"alg\": \"A128KW\",\n" +
			"			\"k\": \"GawgguFyGrWKav7AX4VKUg\"\n" +
			"		},\n" +
			"		{\n" +
			"			\"kty\": \"oct\",\n" +
			"			\"k\": \"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow\",\n" +
			"			\"kid\": \"HMAC key used in JWS spec Appendix A.1 example\"\n" +
			"		}\n" +
			"	]\n" +
			"}";
		
		// We must ignore errors here: since no algorithm is specified for the second key, the PBES2JWKFactory is invoked (key type is "oct") and since there's no "p" entry in the input the process fails 
		List<? extends JWK> jwks = Flux.from(jwkService.read(jwkStr)).onErrorResume(ign -> Mono.empty()).collectList().block();
		
		Assertions.assertEquals(2, jwks.size());
		
		Assertions.assertInstanceOf(OCTJWK.class, jwks.get(0));
		OCTJWK symmetricJWK1 = (OCTJWK)jwks.get(0);
		Assertions.assertEquals(OCTJWK.KEY_TYPE, symmetricJWK1.getKeyType());
		Assertions.assertEquals(OCTAlgorithm.A128KW.getAlgorithm(), symmetricJWK1.getAlgorithm());
		Assertions.assertEquals("GawgguFyGrWKav7AX4VKUg", symmetricJWK1.getKeyValue());
		Assertions.assertNull(symmetricJWK1.getKeyId());
		Assertions.assertNull(symmetricJWK1.getKeyOperations());
		Assertions.assertNull(symmetricJWK1.getPublicKeyUse());
		
		Assertions.assertThrows(JWAProcessingException.class, () -> symmetricJWK1.signer(OCTAlgorithm.A128KW.getAlgorithm()), "Not a signature algorithm A128KW");
		Assertions.assertThrows(JWAProcessingException.class, () -> symmetricJWK1.signer(OCTAlgorithm.A128KW.getAlgorithm()), "Not a signature algorithm A128KW");
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> symmetricJWK1.signer(RSAAlgorithm.RS512.getAlgorithm()), "Unknown OCT algorithm RS512");
		Assertions.assertThrows(IllegalArgumentException.class, () -> symmetricJWK1.signer(RSAAlgorithm.RS512.getAlgorithm()), "Unknown OCT algorithm RS512");
		// TODO encrypter/decrypter...
		
		Assertions.assertInstanceOf(OCTJWK.class, jwks.get(0));
		OCTJWK symmetricJWK2 = (OCTJWK)jwks.get(1);
		Assertions.assertEquals(OCTJWK.KEY_TYPE, symmetricJWK2.getKeyType());
		Assertions.assertEquals("HMAC key used in JWS spec Appendix A.1 example", symmetricJWK2.getKeyId());
		Assertions.assertEquals("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow", symmetricJWK2.getKeyValue());
		Assertions.assertNull(symmetricJWK2.getAlgorithm());
		Assertions.assertNull(symmetricJWK2.getKeyOperations());
		Assertions.assertNull(symmetricJWK2.getPublicKeyUse());
		
		Assertions.assertNotNull(symmetricJWK2.signer(OCTAlgorithm.HS256.getAlgorithm()));
		Assertions.assertNotNull(symmetricJWK2.signer(OCTAlgorithm.HS256.getAlgorithm()));
		
		Assertions.assertNotNull(symmetricJWK2.signer(OCTAlgorithm.HS512.getAlgorithm()));
		Assertions.assertNotNull(symmetricJWK2.signer(OCTAlgorithm.HS512.getAlgorithm()));
	}
	
	@Test
	public void testRFC7517_B() throws CertificateException, InvalidAlgorithmParameterException {
		JWKService jwkService = jwkService(pkixParameters("MIIDQjCCAiqgAwIBAgIGATz/FuLiMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDAeFw0xMzAyMjEyMzI5MTVaFw0xODA4MTQyMjI5MTVaMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL64zn8/QnHYMeZ0LncoXaEde1fiLm1jHjmQsF/449IYALM9if6amFtPDy2yvz3YlRij66s5gyLCyO7ANuVRJx1NbgizcAblIgjtdf/u3WG7K+IiZhtELto/A7Fck9Ws6SQvzRvOE8uSirYbgmj6He4iO8NCyvaK0jIQRMMGQwsU1quGmFgHIXPLfnpnfajr1rVTAwtgV5LEZ4Iel+W1GC8ugMhyr4/p1MtcIM42EA8BzE6ZQqC7VPqPvEjZ2dbZkaBhPbiZAS3YeYBRDWm1p1OZtWamT3cEvqqPpnjL1XyW+oyVVkaZdklLQp2Btgt9qr21m42f4wTw+Xrp6rCKNb0CAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAh8zGlfSlcI0o3rYDPBB07aXNswb4ECNIKG0CETTUxmXl9KUL+9gGlqCz5iWLOgWsnrcKcY0vXPG9J1r9AqBNTqNgHq2G03X09266X5CpOe1zFo+Owb1zxtp3PehFdfQJ610CDLEaS9V9Rqp17hCyybEpOGVwe8fnk+fbEL2Bo3UPGrpsHzUoaGpDftmWssZkhpBJKVMJyf/RuP2SmmaIzmnw9JiSlYhzo4tpzd5rFXhjRbg4zW9C+2qok+2+qDM1iJ684gPHMIY8aLWrdgQTxkumGmTqgawR+N5MDtdPTEQ0XfIBc2cJEUyMTY5MPvACWpkA6SdS4xSvdXK3IVfOWA==", Date.from(Instant.parse("2017-01-01T00:00:00.00Z"))));
		
		String jwkStr = "{\n" +
			"	\"kty\": \"RSA\",\n" +
			"	\"use\": \"sig\",\n" +
			"	\"kid\": \"1b94c\",\n" +
			"	\"n\": \"vrjOfz9Ccdgx5nQudyhdoR17V-IubWMeOZCwX_jj0hgAsz2J_pqYW08PLbK_PdiVGKPrqzmDIsLI7sA25VEnHU1uCLNwBuUiCO11_-7dYbsr4iJmG0Qu2j8DsVyT1azpJC_NG84Ty5KKthuCaPod7iI7w0LK9orSMhBEwwZDCxTWq4aYWAchc8t-emd9qOvWtVMDC2BXksRngh6X5bUYLy6AyHKvj-nUy1wgzjYQDwHMTplCoLtU-o-8SNnZ1tmRoGE9uJkBLdh5gFENabWnU5m1ZqZPdwS-qo-meMvVfJb6jJVWRpl2SUtCnYG2C32qvbWbjZ_jBPD5eunqsIo1vQ\",\n" +
			"	\"e\": \"AQAB\",\n" +
			"	\"x5c\": [\n" +
			"		\"MIIDQjCCAiqgAwIBAgIGATz/FuLiMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDAeFw0xMzAyMjEyMzI5MTVaFw0xODA4MTQyMjI5MTVaMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL64zn8/QnHYMeZ0LncoXaEde1fiLm1jHjmQsF/449IYALM9if6amFtPDy2yvz3YlRij66s5gyLCyO7ANuVRJx1NbgizcAblIgjtdf/u3WG7K+IiZhtELto/A7Fck9Ws6SQvzRvOE8uSirYbgmj6He4iO8NCyvaK0jIQRMMGQwsU1quGmFgHIXPLfnpnfajr1rVTAwtgV5LEZ4Iel+W1GC8ugMhyr4/p1MtcIM42EA8BzE6ZQqC7VPqPvEjZ2dbZkaBhPbiZAS3YeYBRDWm1p1OZtWamT3cEvqqPpnjL1XyW+oyVVkaZdklLQp2Btgt9qr21m42f4wTw+Xrp6rCKNb0CAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAh8zGlfSlcI0o3rYDPBB07aXNswb4ECNIKG0CETTUxmXl9KUL+9gGlqCz5iWLOgWsnrcKcY0vXPG9J1r9AqBNTqNgHq2G03X09266X5CpOe1zFo+Owb1zxtp3PehFdfQJ610CDLEaS9V9Rqp17hCyybEpOGVwe8fnk+fbEL2Bo3UPGrpsHzUoaGpDftmWssZkhpBJKVMJyf/RuP2SmmaIzmnw9JiSlYhzo4tpzd5rFXhjRbg4zW9C+2qok+2+qDM1iJ684gPHMIY8aLWrdgQTxkumGmTqgawR+N5MDtdPTEQ0XfIBc2cJEUyMTY5MPvACWpkA6SdS4xSvdXK3IVfOWA==\"\n" +
			"	]\n" +
			"}";
		
		JWK jwk = Mono.from(jwkService.read(jwkStr)).block();
		
		Assertions.assertInstanceOf(RSAJWK.class, jwk);
		RSAJWK rsaJWK = (RSAJWK)jwk;
		Assertions.assertEquals(RSAJWK.KEY_TYPE, rsaJWK.getKeyType());
		Assertions.assertEquals(RSAJWK.USE_SIG, rsaJWK.getPublicKeyUse());
		Assertions.assertEquals("1b94c", rsaJWK.getKeyId());
		Assertions.assertEquals("vrjOfz9Ccdgx5nQudyhdoR17V-IubWMeOZCwX_jj0hgAsz2J_pqYW08PLbK_PdiVGKPrqzmDIsLI7sA25VEnHU1uCLNwBuUiCO11_-7dYbsr4iJmG0Qu2j8DsVyT1azpJC_NG84Ty5KKthuCaPod7iI7w0LK9orSMhBEwwZDCxTWq4aYWAchc8t-emd9qOvWtVMDC2BXksRngh6X5bUYLy6AyHKvj-nUy1wgzjYQDwHMTplCoLtU-o-8SNnZ1tmRoGE9uJkBLdh5gFENabWnU5m1ZqZPdwS-qo-meMvVfJb6jJVWRpl2SUtCnYG2C32qvbWbjZ_jBPD5eunqsIo1vQ", rsaJWK.getModulus());
		Assertions.assertEquals("AQAB", rsaJWK.getPublicExponent());
		Assertions.assertArrayEquals(new String[]{"MIIDQjCCAiqgAwIBAgIGATz/FuLiMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDAeFw0xMzAyMjEyMzI5MTVaFw0xODA4MTQyMjI5MTVaMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL64zn8/QnHYMeZ0LncoXaEde1fiLm1jHjmQsF/449IYALM9if6amFtPDy2yvz3YlRij66s5gyLCyO7ANuVRJx1NbgizcAblIgjtdf/u3WG7K+IiZhtELto/A7Fck9Ws6SQvzRvOE8uSirYbgmj6He4iO8NCyvaK0jIQRMMGQwsU1quGmFgHIXPLfnpnfajr1rVTAwtgV5LEZ4Iel+W1GC8ugMhyr4/p1MtcIM42EA8BzE6ZQqC7VPqPvEjZ2dbZkaBhPbiZAS3YeYBRDWm1p1OZtWamT3cEvqqPpnjL1XyW+oyVVkaZdklLQp2Btgt9qr21m42f4wTw+Xrp6rCKNb0CAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAh8zGlfSlcI0o3rYDPBB07aXNswb4ECNIKG0CETTUxmXl9KUL+9gGlqCz5iWLOgWsnrcKcY0vXPG9J1r9AqBNTqNgHq2G03X09266X5CpOe1zFo+Owb1zxtp3PehFdfQJ610CDLEaS9V9Rqp17hCyybEpOGVwe8fnk+fbEL2Bo3UPGrpsHzUoaGpDftmWssZkhpBJKVMJyf/RuP2SmmaIzmnw9JiSlYhzo4tpzd5rFXhjRbg4zW9C+2qok+2+qDM1iJ684gPHMIY8aLWrdgQTxkumGmTqgawR+N5MDtdPTEQ0XfIBc2cJEUyMTY5MPvACWpkA6SdS4xSvdXK3IVfOWA=="}, rsaJWK.getX509CertificateChain());
		
		Assertions.assertThrows(JWASignatureException.class, () -> rsaJWK.signer(RSAAlgorithm.RS256.getAlgorithm()).sign(new byte[0]), "JWK is missing RSA private exponent");
		Assertions.assertNotNull(rsaJWK.signer(RSAAlgorithm.RS256.getAlgorithm()));
		
		Assertions.assertThrows(JWASignatureException.class, () -> rsaJWK.signer(RSAAlgorithm.RS512.getAlgorithm()).sign(new byte[0]), "JWK is missing RSA private exponent");
		Assertions.assertNotNull(rsaJWK.signer(RSAAlgorithm.RS512.getAlgorithm()));
	}
	
	@Test
	public void testRFC7638_31() {
		String jwkThumbprint = "NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs";
		JWKService jwkService = jwkService();
		
		String jwkStr = "{" +
			"\"kty\": \"RSA\"," +
			"\"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\"," +
			"\"e\": \"AQAB\"," +
			"\"alg\": \"RS256\"," +
			"\"kid\": \"2011-04-29\"" +
			"}";
		
		List<? extends JWK> jwks = Flux.from(jwkService.read(jwkStr))
			.collectList()
			.block();
		
		Assertions.assertEquals(1, jwks.size());
		
		Assertions.assertEquals(jwkThumbprint, jwks.get(0).toJWKThumbprint());
	}
	
	@Test
	public void testRFC7517_C1() {
		JWKService jwkService = jwkService();
		
		String jwkStr = "{\n" +
			"\"kty\":\"RSA\",\n" +
			"\"kid\":\"juliet@capulet.lit\",\n" +
			"\"use\":\"enc\",\n" +
			"\"n\":\"t6Q8PWSi1dkJj9hTP8hNYFlvadM7DflW9mWepOJhJ66w7nyoK1gPNqFMSQRyO125Gp-TEkodhWr0iujjHVx7BcV0llS4w5ACGgPrcAd6ZcSR0-Iqom-QFcNP8Sjg086MwoqQU_LYywlAGZ21WSdS_PERyGFiNnj3QQlO8Yns5jCtLCRwLHL0Pb1fEv45AuRIuUfVcPySBWYnDyGxvjYGDSM-AqWS9zIQ2ZilgT-GqUmipg0XOC0Cc20rgLe2ymLHjpHciCKVAbY5-L32-lSeZO-Os6U15_aXrk9Gw8cPUaX1_I8sLGuSiVdt3C_Fn2PZ3Z8i744FPFGGcG1qs2Wz-Q\",\n" +
			"\"e\":\"AQAB\",\n" +
			"\"d\":\"GRtbIQmhOZtyszfgKdg4u_N-R_mZGU_9k7JQ_jn1DnfTuMdSNprTeaSTyWfSNkuaAwnOEbIQVy1IQbWVV25NY3ybc_IhUJtfri7bAXYEReWaCl3hdlPKXy9UvqPYGR0kIXTQRqns-dVJ7jahlI7LyckrpTmrM8dWBo4_PMaenNnPiQgO0xnuToxutRZJfJvG4Ox4ka3GORQd9CsCZ2vsUDmsXOfUENOyMqADC6p1M3h33tsurY15k9qMSpG9OX_IJAXmxzAh_tWiZOwk2K4yxH9tS3Lq1yX8C1EWmeRDkK2ahecG85-oLKQt5VEpWHKmjOi_gJSdSgqcN96X52esAQ\",\n" +
			"\"p\":\"2rnSOV4hKSN8sS4CgcQHFbs08XboFDqKum3sc4h3GRxrTmQdl1ZK9uw-PIHfQP0FkxXVrx-WE-ZEbrqivH_2iCLUS7wAl6XvARt1KkIaUxPPSYB9yk31s0Q8UK96E3_OrADAYtAJs-M3JxCLfNgqh56HDnETTQhH3rCT5T3yJws\",\n" +
			"\"q\":\"1u_RiFDP7LBYh3N4GXLT9OpSKYP0uQZyiaZwBtOCBNJgQxaj10RWjsZu0c6Iedis4S7B_coSKB0Kj9PaPaBzg-IySRvvcQuPamQu66riMhjVtG6TlV8CLCYKrYl52ziqK0E_ym2QnkwsUX7eYTB7LbAHRK9GqocDE5B0f808I4s\",\n" +
			"\"dp\":\"KkMTWqBUefVwZ2_Dbj1pPQqyHSHjj90L5x_MOzqYAJMcLMZtbUtwKqvVDq3tbEo3ZIcohbDtt6SbfmWzggabpQxNxuBpoOOf_a_HgMXK_lhqigI4y_kqS1wY52IwjUn5rgRrJ-yYo1h41KR-vz2pYhEAeYrhttWtxVqLCRViD6c\",\n" +
			"\"dq\":\"AvfS0-gRxvn0bwJoMSnFxYcK1WnuEjQFluMGfwGitQBWtfZ1Er7t1xDkbN9GQTB9yqpDoYaN06H7CFtrkxhJIBQaj6nkF5KKS3TQtQ5qCzkOkmxIe3KRbBymXxkb5qwUpX5ELD5xFc6FeiafWYY63TmmEAu_lRFCOJ3xDea-ots\",\n" +
			"\"qi\":\"lSQi-w9CpyUReMErP1RsBLk7wNtOvs5EQpPqmuMvqW57NBUczScEoPwmUqqabu9V0-Py4dQ57_bapoKRu1R90bvuFnU63SHWEFglZQvJDMeAvmj4sm-Fp0oYu_neotgQ0hzbI5gry7ajdYy9-2lNx_76aBZoOUu9HCJ-UsfSOI8\"\n" +
			"}";

		JWK jwk = Mono.from(jwkService.read(jwkStr)).block();
		
		Assertions.assertInstanceOf(RSAJWK.class, jwk);
		RSAJWK rsaJWK = (RSAJWK)jwk;
		Assertions.assertEquals(RSAJWK.KEY_TYPE, rsaJWK.getKeyType());
		Assertions.assertEquals("juliet@capulet.lit", rsaJWK.getKeyId());
		Assertions.assertEquals(JWK.USE_ENC, rsaJWK.getPublicKeyUse());
		Assertions.assertEquals("t6Q8PWSi1dkJj9hTP8hNYFlvadM7DflW9mWepOJhJ66w7nyoK1gPNqFMSQRyO125Gp-TEkodhWr0iujjHVx7BcV0llS4w5ACGgPrcAd6ZcSR0-Iqom-QFcNP8Sjg086MwoqQU_LYywlAGZ21WSdS_PERyGFiNnj3QQlO8Yns5jCtLCRwLHL0Pb1fEv45AuRIuUfVcPySBWYnDyGxvjYGDSM-AqWS9zIQ2ZilgT-GqUmipg0XOC0Cc20rgLe2ymLHjpHciCKVAbY5-L32-lSeZO-Os6U15_aXrk9Gw8cPUaX1_I8sLGuSiVdt3C_Fn2PZ3Z8i744FPFGGcG1qs2Wz-Q", rsaJWK.getModulus());
		Assertions.assertEquals("AQAB", rsaJWK.getPublicExponent());
		Assertions.assertEquals("GRtbIQmhOZtyszfgKdg4u_N-R_mZGU_9k7JQ_jn1DnfTuMdSNprTeaSTyWfSNkuaAwnOEbIQVy1IQbWVV25NY3ybc_IhUJtfri7bAXYEReWaCl3hdlPKXy9UvqPYGR0kIXTQRqns-dVJ7jahlI7LyckrpTmrM8dWBo4_PMaenNnPiQgO0xnuToxutRZJfJvG4Ox4ka3GORQd9CsCZ2vsUDmsXOfUENOyMqADC6p1M3h33tsurY15k9qMSpG9OX_IJAXmxzAh_tWiZOwk2K4yxH9tS3Lq1yX8C1EWmeRDkK2ahecG85-oLKQt5VEpWHKmjOi_gJSdSgqcN96X52esAQ", rsaJWK.getPrivateExponent());
		Assertions.assertEquals("2rnSOV4hKSN8sS4CgcQHFbs08XboFDqKum3sc4h3GRxrTmQdl1ZK9uw-PIHfQP0FkxXVrx-WE-ZEbrqivH_2iCLUS7wAl6XvARt1KkIaUxPPSYB9yk31s0Q8UK96E3_OrADAYtAJs-M3JxCLfNgqh56HDnETTQhH3rCT5T3yJws", rsaJWK.getFirstPrimeFactor());
		Assertions.assertEquals("1u_RiFDP7LBYh3N4GXLT9OpSKYP0uQZyiaZwBtOCBNJgQxaj10RWjsZu0c6Iedis4S7B_coSKB0Kj9PaPaBzg-IySRvvcQuPamQu66riMhjVtG6TlV8CLCYKrYl52ziqK0E_ym2QnkwsUX7eYTB7LbAHRK9GqocDE5B0f808I4s", rsaJWK.getSecondPrimeFactor());
		Assertions.assertEquals("KkMTWqBUefVwZ2_Dbj1pPQqyHSHjj90L5x_MOzqYAJMcLMZtbUtwKqvVDq3tbEo3ZIcohbDtt6SbfmWzggabpQxNxuBpoOOf_a_HgMXK_lhqigI4y_kqS1wY52IwjUn5rgRrJ-yYo1h41KR-vz2pYhEAeYrhttWtxVqLCRViD6c", rsaJWK.getFirstFactorExponent());
		Assertions.assertEquals("AvfS0-gRxvn0bwJoMSnFxYcK1WnuEjQFluMGfwGitQBWtfZ1Er7t1xDkbN9GQTB9yqpDoYaN06H7CFtrkxhJIBQaj6nkF5KKS3TQtQ5qCzkOkmxIe3KRbBymXxkb5qwUpX5ELD5xFc6FeiafWYY63TmmEAu_lRFCOJ3xDea-ots", rsaJWK.getSecondFactorExponent());
		Assertions.assertEquals("lSQi-w9CpyUReMErP1RsBLk7wNtOvs5EQpPqmuMvqW57NBUczScEoPwmUqqabu9V0-Py4dQ57_bapoKRu1R90bvuFnU63SHWEFglZQvJDMeAvmj4sm-Fp0oYu_neotgQ0hzbI5gry7ajdYy9-2lNx_76aBZoOUu9HCJ-UsfSOI8", rsaJWK.getFirstCoefficient());
		Assertions.assertNull(rsaJWK.getX509CertificateChain());
		Assertions.assertNull(rsaJWK.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(rsaJWK.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(rsaJWK.getX509CertificateURL());
		Assertions.assertNull(rsaJWK.getAlgorithm());
		Assertions.assertNull(rsaJWK.getKeyOperations());
		Assertions.assertNull(rsaJWK.getOtherPrimesInfo());
		
		Assertions.assertThrows(JWKProcessingException.class, () -> jwk.signer(RSAAlgorithm.RS256.getAlgorithm()), "JWK is not to be used for signing data");
		Assertions.assertThrows(JWKProcessingException.class, () -> jwk.signer(RSAAlgorithm.RS256.getAlgorithm()), "JWK is not to be used for verifying signature");
		
		// TODO encrypter/decrypter
	}
	
	@Test
	public void testRFC8037_A1() {
		JWKService jwkService = jwkService();
		
		String jwkStr = "{\n" +
			"	\"kty\": \"OKP\",\n" +
			"	\"crv\": \"Ed25519\",\n" +
			"	\"d\": \"nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A\",\n" +
			"	\"x\": \"11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo\"\n" +
			"}";
		
		
		List<? extends JWK> jwks = Flux.from(jwkService.read(jwkStr))
			.onErrorResume(e -> Mono.empty()) // GenericXECJWKFactory will fail since it doesn't support Ed25519 curve
			.collectList()
			.block();
		
		Assertions.assertEquals(1, jwks.size());
		
		Assertions.assertInstanceOf(EdECJWK.class, jwks.get(0));
		EdECJWK okpJWK = (EdECJWK)jwks.get(0);
		Assertions.assertEquals(OKPJWK.KEY_TYPE, okpJWK.getKeyType());
		Assertions.assertEquals(OKPCurve.ED25519.getCurve(), okpJWK.getCurve());
		Assertions.assertEquals("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo", okpJWK.getPublicKey());
		Assertions.assertEquals("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A", okpJWK.getPrivateKey());
		Assertions.assertNull(okpJWK.getPublicKeyUse());
		Assertions.assertNull(okpJWK.getKeyId());
		Assertions.assertNull(okpJWK.getX509CertificateChain());
		Assertions.assertNull(okpJWK.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(okpJWK.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(okpJWK.getX509CertificateURL());
		Assertions.assertNull(okpJWK.getAlgorithm());
		Assertions.assertNull(okpJWK.getKeyOperations());
		
		Assertions.assertNotNull(okpJWK.signer(EdECAlgorithm.EDDSA_ED25519.getAlgorithm()));
	}
	
	@Test
	public void testRFC8037_A2() {
		JWKService jwkService = jwkService();
		
		String jwkStr = "{\n" +
			"	\"kty\": \"OKP\",\n" +
			"	\"crv\": \"Ed25519\",\n" +
			"	\"x\": \"11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo\"\n" +
			"}";
		
		List<? extends JWK> jwks = Flux.from(jwkService.read(jwkStr))
			.onErrorResume(e -> Mono.empty()) // GenericXECJWKFactory will be invoked because it does support OKP but it will fail since it doesn't support Ed25519 curve
			.collectList()
			.block();
		
		Assertions.assertEquals(1, jwks.size());
		
		Assertions.assertInstanceOf(EdECJWK.class, jwks.get(0));
		EdECJWK okpJWK = (EdECJWK)jwks.get(0);
		Assertions.assertEquals(OKPJWK.KEY_TYPE, okpJWK.getKeyType());
		Assertions.assertEquals(OKPCurve.ED25519.getCurve(), okpJWK.getCurve());
		Assertions.assertEquals("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo", okpJWK.getPublicKey());
		Assertions.assertNull(okpJWK.getPrivateKey());
		Assertions.assertNull(okpJWK.getPublicKeyUse());
		Assertions.assertNull(okpJWK.getKeyId());
		Assertions.assertNull(okpJWK.getX509CertificateChain());
		Assertions.assertNull(okpJWK.getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(okpJWK.getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(okpJWK.getX509CertificateURL());
		Assertions.assertNull(okpJWK.getAlgorithm());
		Assertions.assertNull(okpJWK.getKeyOperations());
		
		Assertions.assertNotNull(okpJWK.signer(EdECAlgorithm.EDDSA_ED25519.getAlgorithm()));
		Assertions.assertThrows(JWASignatureException.class, () -> okpJWK.signer(EdECAlgorithm.EDDSA_ED25519.getAlgorithm()).sign(new byte[0]), "JWK is missing OKP private key");
	}
	
	@Test
	public void testRFC8037_A3() {
		String jwkThumbprint = "kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k";
		JWKService jwkService = jwkService();
		
		String jwkStr = "{\"crv\":\"Ed25519\",\"kty\":\"OKP\",\"x\":\"11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo\"}";
		
		List<? extends JWK> jwks = Flux.from(jwkService.read(jwkStr))
			.onErrorResume(e -> Mono.empty()) // GenericXECJWKFactory will be invoked because it does support OKP but it will fail since it doesn't support Ed25519 curve
			.collectList()
			.block();
		
		Assertions.assertEquals(jwkThumbprint, jwks.get(0).toJWKThumbprint());
	}
	
	@Test
	public void testJWKStore() {
		GenericJWKService jwkService = jwkService();
		
		ECJWK ecJWKK = jwkService.ec().builder()
			.keyId("ec")
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
			.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
			.build()
			.block();
		jwkService.store().set(ecJWKK).block();
		
		JWK jwk = Mono.from(jwkService.read("{\"kty\":\"EC\",\"kid\":\"ec\"}"))
			.block();
		Assertions.assertEquals(ecJWKK, jwk);
		
		jwk = Mono.from(jwkService.read("{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU\",\"y\":\"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0\"}"))
			.block();
		Assertions.assertEquals(ecJWKK, jwk);
		
		JWKReadException ecException = Assertions.assertThrows(JWKReadException.class, () -> Mono.from(jwkService.read("{\"kty\":\"EC\",\"kid\":\"ec\",\"crv\":\"P-384\"}")).block());
		Assertions.assertEquals("JWK parameters does not match stored JWK", Assertions.assertInstanceOf(JWKResolveException.class, ecException.getSuppressed()[0]).getMessage());
		
		OCTJWK octJWK = jwkService.oct().builder()
			.keyId("oct")
			.keyValue("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
			.build()
			.block();
		jwkService.store().set(octJWK).block();
		
		jwk = Mono.from(jwkService.read(new GenericJWSHeader().keyId("oct")))
			.block();
		Assertions.assertEquals(octJWK, jwk);
		
		jwk = Mono.from(jwkService.read("{\"kty\":\"oct\",\"k\":\"AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow\"}"))
			.block();
		Assertions.assertEquals(octJWK, jwk);
		
		JWKReadException octException = Assertions.assertThrows(JWKReadException.class, () -> Mono.from(jwkService.read("{\"kty\":\"oct\",\"kid\":\"oct\",\"k\":\"abc\"}")).block());
		Assertions.assertEquals("JWK parameters does not match stored JWK", Assertions.assertInstanceOf(JWKResolveException.class, octException.getSuppressed()[0]).getMessage());
		Assertions.assertEquals("Stored JWK is not of expected type: " + PBES2JWK.class, Assertions.assertInstanceOf(JWKResolveException.class, octException.getSuppressed()[1]).getMessage());
		
		EdECJWK edecJWK = jwkService.edec().builder()
			.keyId("edec")
			.curve(OKPCurve.ED25519.getCurve())
			.publicKey("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")
			.privateKey("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")
			.build()
			.block();
		jwkService.store().set(edecJWK).block();
		
		jwk = Mono.from(jwkService.read(new GenericJWSHeader().keyId("edec")))
			.block();
		Assertions.assertEquals(edecJWK, jwk);
		
		jwk = Mono.from(jwkService.read("{\"kty\":\"OKP\",\"crv\":\"Ed25519\",\"x\":\"11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo\"}"))
			.block();
		Assertions.assertEquals(edecJWK, jwk);
		
		JWKReadException edecException = Assertions.assertThrows(JWKReadException.class, () -> Mono.from(jwkService.read("{\"kty\":\"OKP\",\"kid\":\"edec\",\"x\":\"abc\"}")).block());
		Assertions.assertEquals("JWK parameters does not match stored JWK", Assertions.assertInstanceOf(JWKResolveException.class, edecException.getSuppressed()[0]).getMessage());
		Assertions.assertEquals("JWK parameters does not match stored JWK", Assertions.assertInstanceOf(JWKResolveException.class, edecException.getSuppressed()[1]).getMessage());
		
		XECJWK xecJWK = jwkService.xec().builder()
			.keyId("xec")
			.curve("X25519")
			.publicKey("hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo")
			.privateKey("dwdtCnMYpX08FsFyUbJmRd9ML4frwJkqsXf7pR25LCo")
			.build()
			.block();
		jwkService.store().set(xecJWK).block();
		
		jwk = Mono.from(jwkService.read("{\"kty\":\"OKP\",\"kid\":\"xec\"}"))
			.block();
		Assertions.assertEquals(xecJWK, jwk);
		
		jwk = Mono.from(jwkService.read("{\"kty\":\"OKP\",\"crv\":\"X25519\",\"x\":\"hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo\"}"))
			.block();
		Assertions.assertEquals(xecJWK, jwk);
		
		JWKReadException xecException = Assertions.assertThrows(JWKReadException.class, () -> Mono.from(jwkService.read("{\"kty\":\"OKP\",\"kid\":\"xec\",\"crv\":\"X448\"}")).block());
		Assertions.assertEquals("JWK parameters does not match stored JWK", Assertions.assertInstanceOf(JWKResolveException.class, xecException.getSuppressed()[0]).getMessage());
		Assertions.assertEquals("JWK parameters does not match stored JWK", Assertions.assertInstanceOf(JWKResolveException.class, edecException.getSuppressed()[1]).getMessage());
		
		RSAJWK rsaJWK = jwkService.rsa().builder()
			.keyId("rsa")
			.modulus("ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ")
			.publicExponent("AQAB")
			.privateExponent("Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97IjlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYTCBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLhBOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ")
			.firstPrimeFactor("4BzEEOtIpmVdVEZNCqS7baC4crd0pqnRH_5IB3jw3bcxGn6QLvnEtfdUdiYrqBdss1l58BQ3KhooKeQTa9AB0Hw_Py5PJdTJNPY8cQn7ouZ2KKDcmnPGBY5t7yLc1QlQ5xHdwW1VhvKn-nXqhJTBgIPgtldC-KDV5z-y2XDwGUc")
			.secondPrimeFactor("uQPEfgmVtjL0Uyyx88GZFF1fOunH3-7cepKmtH4pxhtCoHqpWmT8YAmZxaewHgHAjLYsp1ZSe7zFYHj7C6ul7TjeLQeZD_YwD66t62wDmpe_HlB-TnBA-njbglfIsRLtXlnDzQkv5dTltRJ11BKBBypeeF6689rjcJIDEz9RWdc")
			.firstFactorExponent("BwKfV3Akq5_MFZDFZCnW-wzl-CCo83WoZvnLQwCTeDv8uzluRSnm71I3QCLdhrqE2e9YkxvuxdBfpT_PI7Yz-FOKnu1R6HsJeDCjn12Sk3vmAktV2zb34MCdy7cpdTh_YVr7tss2u6vneTwrA86rZtu5Mbr1C1XsmvkxHQAdYo0")
			.secondFactorExponent("h_96-mK1R_7glhsum81dZxjTnYynPbZpHziZjeeHcXYsXaaMwkOlODsWa7I9xXDoRwbKgB719rrmI2oKr6N3Do9U0ajaHF-NKJnwgjMd2w9cjz3_-kyNlxAr2v4IKhGNpmM5iIgOS1VZnOZ68m6_pbLBSp3nssTdlqvd0tIiTHU")
			.firstCoefficient("IYd7DHOhrWvxkwPQsRM2tOgrjbcrfvtQJipd-DlcxyVuuM9sQLdgjVk2oy26F0EmpScGLq2MowX7fhd_QJQ3ydy5cY7YIBi87w93IKLEdfnbJtoOPLUW0ITrJReOgo1cq9SbsxYawBgfp_gh6A5603k2-ZQwVK0JKSHuLFkuQ3U")
			.build()
			.block();
		jwkService.store().set(rsaJWK).block();
		
		jwk = Mono.from(jwkService.read(new GenericJWSHeader().keyId("rsa")))
			.block();
		Assertions.assertEquals(rsaJWK, jwk);
		
		jwk = Mono.from(jwkService.read("{\"kty\":\"RSA\",\"e\":\"AQAB\",\"n\":\"ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ\"}"))
			.block();
		Assertions.assertEquals(rsaJWK, jwk);
		
		JWKReadException rsaException = Assertions.assertThrows(JWKReadException.class, () -> Mono.from(jwkService.read("{\"kty\":\"RSA\",\"kid\":\"rsa\",\"n\":\"abc\"}")).block());
		Assertions.assertEquals("JWK parameters does not match stored JWK", Assertions.assertInstanceOf(JWKResolveException.class, rsaException.getSuppressed()[0]).getMessage());
	}
	
	private static GenericJWKService jwkService() {
		return jwkService(null);
	}
	
	private static GenericJWKService jwkService(PKIXParameters pkixParameters) {
		ObjectMapper mapper = new ObjectMapper();
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		JWKStore jwkStore = new InMemoryJWKStore();
		if(pkixParameters == null) {
			pkixParameters = new JWKPKIXParameters().get();
		}
		GenericX509JWKCertPathValidator certPathValidator = new GenericX509JWKCertPathValidator(pkixParameters, WORKER_POOL);
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(mapper);
		SwitchableJWKURLResolver switchableUrlResolver = new SwitchableJWKURLResolver(configuration, urlResolver);
		
		GenericECJWKFactory ecJWKFactory = new GenericECJWKFactory(configuration, jwkStore, keyResolver, mapper, switchableUrlResolver, certPathValidator);
		GenericRSAJWKFactory rsaJWKFactory = new GenericRSAJWKFactory(configuration, jwkStore, keyResolver, mapper, switchableUrlResolver, certPathValidator);
		GenericOCTJWKFactory symmetricJWKFactory = new GenericOCTJWKFactory(configuration, jwkStore, keyResolver, mapper);
		GenericEdECJWKFactory edecJWKFactory = new GenericEdECJWKFactory(configuration, jwkStore, keyResolver, mapper, switchableUrlResolver, certPathValidator);
		GenericXECJWKFactory xecJWKFactory = new GenericXECJWKFactory(configuration, jwkStore, keyResolver, mapper, switchableUrlResolver, certPathValidator);
		GenericPBES2JWKFactory pbes2JWKFactory = new GenericPBES2JWKFactory(configuration, jwkStore, keyResolver, mapper);
		
		return new GenericJWKService(configuration, ecJWKFactory, rsaJWKFactory, symmetricJWKFactory, edecJWKFactory, xecJWKFactory, pbes2JWKFactory, jwkStore, urlResolver, switchableUrlResolver, mapper);
	}
	
	private static PKIXParameters pkixParameters(String cert, Date date) throws CertificateException, InvalidAlgorithmParameterException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		
		X509Certificate certificate = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(cert)));
		PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(certificate, null)));
		parameters.setRevocationEnabled(false);
		parameters.setDate(date);
			
		return parameters;
	}
}
