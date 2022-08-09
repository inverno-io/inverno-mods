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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.internal.converter.JWTStringMediaTypeConverter;
import io.inverno.mod.security.jose.internal.jwe.GenericJWEService;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKService;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKURLResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericX509JWKCertPathValidator;
import io.inverno.mod.security.jose.jwk.JWKPKIXParameters;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.internal.jwk.SwitchableJWKURLResolver;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWK;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKFactory;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWK;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKFactory;
import io.inverno.mod.security.jose.internal.jws.GenericJWSHeader;
import io.inverno.mod.security.jose.internal.jws.GenericJWSService;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JWEService;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSHeader;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jwt.JWTBuildException;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import io.inverno.mod.security.jose.jwt.JWTService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class GenericJWTServiceTest {

	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final ExecutorService WORKER_POOL = Executors.newCachedThreadPool();
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	/* 
	 * In order to comply with RFC7515 tests:
	 * - the content type must not be serialized in the JOSE header
	 * - the content type is however required to be able to determine which media type converter to use to encode/decode the payload
	 * - json must be formatted in particular way using a custom PrettyPrinter
	 */
	static {
		SimpleModule module = new SimpleModule();
		module.addSerializer(GenericJWSHeader.class, new GenericJWTServiceTest.RFC7515JWSHeaderSerializer());
		module.addSerializer(new GenericJWTServiceTest.RFC7515PayloadSerializer());
		
		MAPPER
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDefaultPrettyPrinter(new GenericJWTServiceTest.RFC71515PrettyPrinter())
			.registerModule(module);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC7515_A1() {
		String compact = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

		Mono<? extends OCTJWK> key = jwkService().oct().builder()
			.keyValue("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
			.build()
			.cache();

		JWTClaimsSet jcs = JWTClaimsSet.of("joe", 1300819380).addCustomClaim("http://example.com/is_root", true).build();
		
		JWTService jwtService = joseServices().jwtService;
		
		JWS<JWTClaimsSet> jwts = jwtService.jwsBuilder(key)
			.header(header -> header
					.algorithm("HS256")
					.type("JWT")
			)
			.payload(jcs)
			.build()
			.block();
		
		Assertions.assertEquals(compact, jwts.toCompact());

		Assertions.assertEquals(jcs, jwts.getPayload());
		Assertions.assertEquals("HS256", jwts.getHeader().getAlgorithm());
		Assertions.assertEquals("JWT", jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		jwts = jwtService.jwsReader(key).read(jwts.toCompact()).block();
		
		Assertions.assertEquals(compact, jwts.toCompact());

		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("HS256", jwts.getHeader().getAlgorithm());
		Assertions.assertEquals("JWT", jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		jwts = jwtService.jwsReader(key).read(compact).block();
		
		Assertions.assertEquals(compact, jwts.toCompact());

		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("HS256", jwts.getHeader().getAlgorithm());
		Assertions.assertEquals("JWT", jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC7515_A2() {
		String compact = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw";

		Mono<GenericRSAJWK> key = rsaJWKBuilder()
			.modulus("ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ")
			.publicExponent("AQAB")
			.privateExponent("Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97IjlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYTCBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLhBOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ")
			.firstPrimeFactor("4BzEEOtIpmVdVEZNCqS7baC4crd0pqnRH_5IB3jw3bcxGn6QLvnEtfdUdiYrqBdss1l58BQ3KhooKeQTa9AB0Hw_Py5PJdTJNPY8cQn7ouZ2KKDcmnPGBY5t7yLc1QlQ5xHdwW1VhvKn-nXqhJTBgIPgtldC-KDV5z-y2XDwGUc")
			.secondPrimeFactor("uQPEfgmVtjL0Uyyx88GZFF1fOunH3-7cepKmtH4pxhtCoHqpWmT8YAmZxaewHgHAjLYsp1ZSe7zFYHj7C6ul7TjeLQeZD_YwD66t62wDmpe_HlB-TnBA-njbglfIsRLtXlnDzQkv5dTltRJ11BKBBypeeF6689rjcJIDEz9RWdc")
			.firstFactorExponent("BwKfV3Akq5_MFZDFZCnW-wzl-CCo83WoZvnLQwCTeDv8uzluRSnm71I3QCLdhrqE2e9YkxvuxdBfpT_PI7Yz-FOKnu1R6HsJeDCjn12Sk3vmAktV2zb34MCdy7cpdTh_YVr7tss2u6vneTwrA86rZtu5Mbr1C1XsmvkxHQAdYo0")
			.secondFactorExponent("h_96-mK1R_7glhsum81dZxjTnYynPbZpHziZjeeHcXYsXaaMwkOlODsWa7I9xXDoRwbKgB719rrmI2oKr6N3Do9U0ajaHF-NKJnwgjMd2w9cjz3_-kyNlxAr2v4IKhGNpmM5iIgOS1VZnOZ68m6_pbLBSp3nssTdlqvd0tIiTHU")
			.firstCoefficient("IYd7DHOhrWvxkwPQsRM2tOgrjbcrfvtQJipd-DlcxyVuuM9sQLdgjVk2oy26F0EmpScGLq2MowX7fhd_QJQ3ydy5cY7YIBi87w93IKLEdfnbJtoOPLUW0ITrJReOgo1cq9SbsxYawBgfp_gh6A5603k2-ZQwVK0JKSHuLFkuQ3U")
			.build()
			.cache();

		JWTClaimsSet jcs = JWTClaimsSet.of("joe", 1300819380).addCustomClaim("http://example.com/is_root", true).build();
		
		JWTService jwtService = joseServices().jwtService;
		
		JWS<JWTClaimsSet> jwts = jwtService.jwsBuilder(key)
			.header(header -> header
				.algorithm("RS256")
			)
			.payload(jcs)
			.build()
			.block();
		
		Assertions.assertEquals(jcs, jwts.getPayload());
		Assertions.assertEquals("RS256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		jwts = jwtService.jwsReader(key).read(jwts.toCompact()).block();
		
		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("RS256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		jwts = jwtService.jwsReader(key).read(compact).block();
		
		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("RS256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC7515_A3() {
		String compact = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q";

		Mono<GenericECJWK> key = ecJWKBuilder()
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
			.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
			.eccPrivateKey("jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI")
			.build()
			.cache();

		JWTClaimsSet jcs = JWTClaimsSet.of("joe", 1300819380).addCustomClaim("http://example.com/is_root", true).build();
		
		JWTService jwtService = joseServices().jwtService;
		
		JWS<JWTClaimsSet> jwts = jwtService.jwsBuilder(key)
			.header(header -> header
				.algorithm("ES256")
			)
			.payload(jcs)
			.build()
			.block();
		
		Assertions.assertEquals(jcs, jwts.getPayload());
		Assertions.assertEquals("ES256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		jwts = jwtService.jwsReader(key).read(jwts.toCompact()).block();
		
		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("ES256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		jwts = jwtService.jwsReader(key).read(compact).block();
		
		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("ES256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(key.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC7519_A1() {
		String compact = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0.QR1Owv2ug2WyPBnbQrRARTeEk9kDO2w8qDcjiHnSJflSdv1iNqhWXaKH4MqAkQtMoNfABIPJaZm0HaA415sv3aeuBWnD8J-Ui7Ah6cWafs3ZwwFKDFUUsWHSK-IPKxLGTkND09XyjORj_CHAgOPJ-Sd8ONQRnJvWn_hXV1BNMHzUjPyYwEsRhDhzjAD26imasOTsgruobpYGoQcXUwFDn7moXPRfDE8-NoQX7N7ZYMmpUDkR-Cx9obNGwJQ3nM52YCitxoQVPzjbl7WBuB7AohdBoZOdZ24WlN1lVIeh8v1K4krB8xgKvRU8kgFrEn_a1rZgN5TiysnmzTROF869lQ.AxY8DCtDaGlsbGljb3RoZQ.MKOle7UQrG6nSxTLX6Mqwt0orbHvAKeWnDYvpIAeZ72deHxz3roJDXQyhxx0wKaMHDjUEOKIwrtkHthpqEanSBNYHZgmNOV7sln1Eu9g3J8.fiK51VwhsxJ-siBMR-YFiA";
		String[] splitCompact = compact.split("\\.");
		
		// cek = 32 bytes
		byte[] cek = new byte[]{ (byte)4, (byte)211, (byte)31, (byte)197, (byte)84, (byte)157, (byte)252, (byte)254, (byte)11, (byte)100, (byte)157, (byte)250, (byte)63, (byte)170, (byte)106, (byte)206, (byte)107, (byte)124, (byte)212, (byte)45, (byte)111, (byte)107, (byte)9, (byte)219, (byte)200, (byte)177, (byte)0, (byte)240, (byte)143, (byte)156, (byte)44, (byte)207 };
		// iv = 16 bytes
		byte[] iv = new byte[]{ (byte)3, (byte)22, (byte)60, (byte)12, (byte)43, (byte)67, (byte)104, (byte)105, (byte)108, (byte)108, (byte)105, (byte)99, (byte)111, (byte)116, (byte)104, (byte)101 };
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				if(bytes.length == 32) {
					System.arraycopy(cek, 0, bytes, 0, cek.length);
					return;
				}
				else if(bytes.length == 16) {
					System.arraycopy(iv, 0, bytes, 0, iv.length);
					return;
				}
				super.nextBytes(bytes);
			}
		};
		
		JWTService jwtService = joseServices().jwtService;
		
		//{
		//	"kty": "RSA",
		//	"n": "sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw",
		//	"e": "AQAB",
		//	"d": "VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ",
		//	"p": "9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM",
		//	"q": "uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0",
		//	"dp": "w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs",
		//	"dq": "o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU",
		//	"qi": "eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo"
		//}
		Mono<GenericRSAJWK> key = rsaJWKBuilder()
			.modulus("sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw")
			.publicExponent("AQAB")
			.privateExponent("VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ")
			.firstPrimeFactor("9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM")
			.secondPrimeFactor("uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0")
			.firstFactorExponent("w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs")
			.secondFactorExponent("o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU")
			.firstCoefficient("eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo")
			.build();
		
		JWTClaimsSet jcs = JWTClaimsSet.of("joe", 1300819380).addCustomClaim("http://example.com/is_root", true).build();
		
		Assertions.assertEquals("Content type is not allowed in JWT header: text/plain", Assertions.assertThrows(
				JWTBuildException.class,
				() -> jwtService.jweBuilder(key)
					.secureRandom(secureRandom)
					.header(header -> header
							.algorithm("RSA1_5")
							.encryptionAlgorithm("A128CBC-HS256")
							.contentType("text/plain")
					)
					.payload(jcs)
					.build()
			)
			.getMessage()
		);
		
		Assertions.assertEquals("Type must be JWT: NotJWT", Assertions.assertThrows(
				JWTBuildException.class, 
				() -> jwtService.jweBuilder(key)
					.secureRandom(secureRandom)
					.header(header -> header
						.algorithm("RSA1_5")
						.encryptionAlgorithm("A128CBC-HS256")
						.type("NotJWT")
					)
					.payload(jcs)
					.build()
			)
			.getMessage()
		);
		
		
		// {"alg":"RSA1_5","enc":"A128CBC-HS256"}
		JWE<JWTClaimsSet> jwte = jwtService.jweBuilder(key)
			.secureRandom(secureRandom)
			.header(header -> header
				.algorithm("RSA1_5")
				.encryptionAlgorithm("A128CBC-HS256")
			)
			.payload(jcs)
			.build()
			.block();
		
		String[] jwteSplitCompact = jwte.toCompact().split("\\.");
		
		// RSA1_5 uses random value therefore result is non deterministic, so we can't check equality for eveything
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jwteSplitCompact[0]));
		Assertions.assertEquals("RSA1_5", jwte.getHeader().getAlgorithm());
		Assertions.assertNull(jwte.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwte.getHeader().getContentType());
		Assertions.assertNull(jwte.getHeader().getCritical());
		Assertions.assertNull(jwte.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwte.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwte.getHeader().getJWK());
		Assertions.assertNull(jwte.getHeader().getJWKSetURL());
		Assertions.assertNull(jwte.getHeader().getKey());
		Assertions.assertNull(jwte.getHeader().getKeyId());
		Assertions.assertNull(jwte.getHeader().getType());
		Assertions.assertNull(jwte.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwte.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwte.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwte.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jwteSplitCompact[1]));
		Assertions.assertNotNull(jwte.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jwteSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwte.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertEquals(splitCompact[3], jwteSplitCompact[3]);
		Assertions.assertEquals(jcs, jwte.getPayload());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jwteSplitCompact[4]));
		Assertions.assertNotNull(jwte.getAuthenticationTag());
		
		// Check that we can read what we generate
		Assertions.assertNotNull(jwtService.readerFor(jwte.toCompact(), key).read(jwte.toCompact()).block());
		
		// Read representation from RFC (https://datatracker.ietf.org/doc/html/rfc7516#appendix-A.2.7)
		jwte = jwtService.jweReader(key).read(compact).block();
		
		jwteSplitCompact = jwte.toCompact().split("\\.");
		
		// We must have the same representation
		Assertions.assertEquals(compact, jwte.toCompact());
		
		// 0. JWE protected header
		Assertions.assertTrue(StringUtils.isNotBlank(jwteSplitCompact[0]));
		Assertions.assertEquals("RSA1_5", jwte.getHeader().getAlgorithm());
		Assertions.assertNull(jwte.getHeader().getCompressionAlgorithm());
		Assertions.assertNull(jwte.getHeader().getContentType());
		Assertions.assertNull(jwte.getHeader().getCritical());
		Assertions.assertNull(jwte.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", jwte.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(jwte.getHeader().getJWK());
		Assertions.assertNull(jwte.getHeader().getJWKSetURL());
		Assertions.assertNull(jwte.getHeader().getKey());
		Assertions.assertNull(jwte.getHeader().getKeyId());
		Assertions.assertNull(jwte.getHeader().getType());
		Assertions.assertNull(jwte.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwte.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwte.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwte.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertTrue(StringUtils.isNotBlank(jwteSplitCompact[1]));
		Assertions.assertNotNull(jwte.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertEquals(splitCompact[2], jwteSplitCompact[2]);
		Assertions.assertEquals(splitCompact[2], jwte.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertEquals(splitCompact[3], jwteSplitCompact[3]);
		Assertions.assertEquals(jcs.getIssuer(), jwte.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwte.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwte.getPayload().getCustomClaims());
		Assertions.assertNull(jwte.getPayload().getAudience());
		Assertions.assertNull(jwte.getPayload().getIssuedAt());
		Assertions.assertNull(jwte.getPayload().getJWTId());
		Assertions.assertNull(jwte.getPayload().getNotBefore());
		Assertions.assertNull(jwte.getPayload().getSubject());
		
		// 4. JWE authentication tag
		Assertions.assertTrue(StringUtils.isNotBlank(jwteSplitCompact[4]));
		Assertions.assertNotNull(jwte.getAuthenticationTag());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRFC7519_A2() {
		String compact = "eyJhbGciOiJSU0ExXzUiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiY3R5IjoiSldUIn0.g_hEwksO1Ax8Qn7HoN-BVeBoa8FXe0kpyk_XdcSmxvcM5_P296JXXtoHISr_DD_MqewaQSH4dZOQHoUgKLeFly-9RI11TG-_Ge1bZFazBPwKC5lJ6OLANLMd0QSL4fYEb9ERe-epKYE3xb2jfY1AltHqBO-PM6j23Guj2yDKnFv6WO72tteVzm_2n17SBFvhDuR9a2nHTE67pe0XGBUS_TK7ecA-iVq5COeVdJR4U4VZGGlxRGPLRHvolVLEHx6DYyLpw30Ay9R6d68YCLi9FYTq3hIXPK_-dmPlOUlKvPr1GgJzRoeC9G5qCvdcHWsqJGTO_z3Wfo5zsqwkxruxwA.UmVkbW9uZCBXQSA5ODA1Mg.VwHERHPvCNcHHpTjkoigx3_ExK0Qc71RMEParpatm0X_qpg-w8kozSjfNIPPXiTBBLXR65CIPkFqz4l1Ae9w_uowKiwyi9acgVztAi-pSL8GQSXnaamh9kX1mdh3M_TT-FZGQFQsFhu0Z72gJKGdfGE-OE7hS1zuBD5oEUfk0Dmb0VzWEzpxxiSSBbBAzP10l56pPfAtrjEYw-7ygeMkwBl6Z_mLS6w6xUgKlvW6ULmkV-uLC4FUiyKECK4e3WZYKw1bpgIqGYsw2v_grHjszJZ-_I5uM-9RA8ycX9KqPRp9gc6pXmoU_-27ATs9XCvrZXUtK2902AUzqpeEUJYjWWxSNsS-r1TJ1I-FMJ4XyAiGrfmo9hQPcNBYxPz3GQb28Y5CLSQfNgKSGt0A4isp1hBUXBHAndgtcslt7ZoQJaKe_nNJgNliWtWpJ_ebuOpEl8jdhehdccnRMIwAmU1n7SPkmhIl1HlSOpvcvDfhUN5wuqU955vOBvfkBOh5A11UzBuo2WlgZ6hYi9-e3w29bR0C2-pp3jbqxEDw3iWaf2dc5b-LnR0FEYXvI_tYk5rd_J9N0mg0tQ6RbpxNEMNoA9QWk5lgdPvbh9BaO195abQ.AVO9iT5AV4CzvDJCdhSFlQ";
		String[] splitCompact = compact.split("\\.");
		
		// cek = 32 bytes
		byte[] cek = new byte[]{ (byte)4, (byte)211, (byte)31, (byte)197, (byte)84, (byte)157, (byte)252, (byte)254, (byte)11, (byte)100, (byte)157, (byte)250, (byte)63, (byte)170, (byte)106, (byte)206, (byte)107, (byte)124, (byte)212, (byte)45, (byte)111, (byte)107, (byte)9, (byte)219, (byte)200, (byte)177, (byte)0, (byte)240, (byte)143, (byte)156, (byte)44, (byte)207 };
		// iv = 16 bytes
		byte[] iv = new byte[]{ (byte)3, (byte)22, (byte)60, (byte)12, (byte)43, (byte)67, (byte)104, (byte)105, (byte)108, (byte)108, (byte)105, (byte)99, (byte)111, (byte)116, (byte)104, (byte)101 };
		
		SecureRandom secureRandom = new SecureRandom() {
			@Override
			public void nextBytes(byte[] bytes) {
				if(bytes.length == 32) {
					System.arraycopy(cek, 0, bytes, 0, cek.length);
					return;
				}
				else if(bytes.length == 16) {
					System.arraycopy(iv, 0, bytes, 0, iv.length);
					return;
				}
				super.nextBytes(bytes);
			}
		};
		
		JoseServices joseServices = joseServices();
		
		JWTService jwtService = joseServices.jwtService;
		JWEService jweService = joseServices.jweService;
		// {
		//	"kty":"RSA",
		//	"n":"ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ",
		//	"e":"AQAB",
		//	"d":"Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97IjlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYTCBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLhBOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ",
		//	"p":"4BzEEOtIpmVdVEZNCqS7baC4crd0pqnRH_5IB3jw3bcxGn6QLvnEtfdUdiYrqBdss1l58BQ3KhooKeQTa9AB0Hw_Py5PJdTJNPY8cQn7ouZ2KKDcmnPGBY5t7yLc1QlQ5xHdwW1VhvKn-nXqhJTBgIPgtldC-KDV5z-y2XDwGUc",
		//	"q":"uQPEfgmVtjL0Uyyx88GZFF1fOunH3-7cepKmtH4pxhtCoHqpWmT8YAmZxaewHgHAjLYsp1ZSe7zFYHj7C6ul7TjeLQeZD_YwD66t62wDmpe_HlB-TnBA-njbglfIsRLtXlnDzQkv5dTltRJ11BKBBypeeF6689rjcJIDEz9RWdc",
		//	"dp":"BwKfV3Akq5_MFZDFZCnW-wzl-CCo83WoZvnLQwCTeDv8uzluRSnm71I3QCLdhrqE2e9YkxvuxdBfpT_PI7Yz-FOKnu1R6HsJeDCjn12Sk3vmAktV2zb34MCdy7cpdTh_YVr7tss2u6vneTwrA86rZtu5Mbr1C1XsmvkxHQAdYo0",
		//	"dq":"h_96-mK1R_7glhsum81dZxjTnYynPbZpHziZjeeHcXYsXaaMwkOlODsWa7I9xXDoRwbKgB719rrmI2oKr6N3Do9U0ajaHF-NKJnwgjMd2w9cjz3_-kyNlxAr2v4IKhGNpmM5iIgOS1VZnOZ68m6_pbLBSp3nssTdlqvd0tIiTHU",
		//	"qi":"IYd7DHOhrWvxkwPQsRM2tOgrjbcrfvtQJipd-DlcxyVuuM9sQLdgjVk2oy26F0EmpScGLq2MowX7fhd_QJQ3ydy5cY7YIBi87w93IKLEdfnbJtoOPLUW0ITrJReOgo1cq9SbsxYawBgfp_gh6A5603k2-ZQwVK0JKSHuLFkuQ3U"
		// }
		Mono<GenericRSAJWK> jwtKey = rsaJWKBuilder()
			.modulus("ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ")
			.publicExponent("AQAB")
			.privateExponent("Eq5xpGnNCivDflJsRQBXHx1hdR1k6Ulwe2JZD50LpXyWPEAeP88vLNO97IjlA7_GQ5sLKMgvfTeXZx9SE-7YwVol2NXOoAJe46sui395IW_GO-pWJ1O0BkTGoVEn2bKVRUCgu-GjBVaYLU6f3l9kJfFNS3E0QbVdxzubSu3Mkqzjkn439X0M_V51gfpRLI9JYanrC4D4qAdGcopV_0ZHHzQlBjudU2QvXt4ehNYTCBr6XCLQUShb1juUO1ZdiYoFaFQT5Tw8bGUl_x_jTj3ccPDVZFD9pIuhLhBOneufuBiB4cS98l2SR_RQyGWSeWjnczT0QU91p1DhOVRuOopznQ")
			.firstPrimeFactor("4BzEEOtIpmVdVEZNCqS7baC4crd0pqnRH_5IB3jw3bcxGn6QLvnEtfdUdiYrqBdss1l58BQ3KhooKeQTa9AB0Hw_Py5PJdTJNPY8cQn7ouZ2KKDcmnPGBY5t7yLc1QlQ5xHdwW1VhvKn-nXqhJTBgIPgtldC-KDV5z-y2XDwGUc")
			.secondPrimeFactor("uQPEfgmVtjL0Uyyx88GZFF1fOunH3-7cepKmtH4pxhtCoHqpWmT8YAmZxaewHgHAjLYsp1ZSe7zFYHj7C6ul7TjeLQeZD_YwD66t62wDmpe_HlB-TnBA-njbglfIsRLtXlnDzQkv5dTltRJ11BKBBypeeF6689rjcJIDEz9RWdc")
			.firstFactorExponent("BwKfV3Akq5_MFZDFZCnW-wzl-CCo83WoZvnLQwCTeDv8uzluRSnm71I3QCLdhrqE2e9YkxvuxdBfpT_PI7Yz-FOKnu1R6HsJeDCjn12Sk3vmAktV2zb34MCdy7cpdTh_YVr7tss2u6vneTwrA86rZtu5Mbr1C1XsmvkxHQAdYo0")
			.secondFactorExponent("h_96-mK1R_7glhsum81dZxjTnYynPbZpHziZjeeHcXYsXaaMwkOlODsWa7I9xXDoRwbKgB719rrmI2oKr6N3Do9U0ajaHF-NKJnwgjMd2w9cjz3_-kyNlxAr2v4IKhGNpmM5iIgOS1VZnOZ68m6_pbLBSp3nssTdlqvd0tIiTHU")
			.firstCoefficient("IYd7DHOhrWvxkwPQsRM2tOgrjbcrfvtQJipd-DlcxyVuuM9sQLdgjVk2oy26F0EmpScGLq2MowX7fhd_QJQ3ydy5cY7YIBi87w93IKLEdfnbJtoOPLUW0ITrJReOgo1cq9SbsxYawBgfp_gh6A5603k2-ZQwVK0JKSHuLFkuQ3U")
			.build()
			.cache();
		
		
		//{
		//	"kty": "RSA",
		//	"n": "sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw",
		//	"e": "AQAB",
		//	"d": "VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ",
		//	"p": "9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM",
		//	"q": "uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0",
		//	"dp": "w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs",
		//	"dq": "o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU",
		//	"qi": "eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo"
		//}
		Mono<GenericRSAJWK> jweKey = rsaJWKBuilder()
			.modulus("sXchDaQebHnPiGvyDOAT4saGEUetSyo9MKLOoWFsueri23bOdgWp4Dy1WlUzewbgBHod5pcM9H95GQRV3JDXboIRROSBigeC5yjU1hGzHHyXss8UDprecbAYxknTcQkhslANGRUZmdTOQ5qTRsLAt6BTYuyvVRdhS8exSZEy_c4gs_7svlJJQ4H9_NxsiIoLwAEk7-Q3UXERGYw_75IDrGA84-lA_-Ct4eTlXHBIY2EaV7t7LjJaynVJCpkv4LKjTTAumiGUIuQhrNhZLuF_RJLqHpM2kgWFLU7-VTdL1VbC2tejvcI2BlMkEpk1BzBZI0KQB0GaDWFLN-aEAw3vRw")
			.publicExponent("AQAB")
			.privateExponent("VFCWOqXr8nvZNyaaJLXdnNPXZKRaWCjkU5Q2egQQpTBMwhprMzWzpR8Sxq1OPThh_J6MUD8Z35wky9b8eEO0pwNS8xlh1lOFRRBoNqDIKVOku0aZb-rynq8cxjDTLZQ6Fz7jSjR1Klop-YKaUHc9GsEofQqYruPhzSA-QgajZGPbE_0ZaVDJHfyd7UUBUKunFMScbflYAAOYJqVIVwaYR5zWEEceUjNnTNo_CVSj-VvXLO5VZfCUAVLgW4dpf1SrtZjSt34YLsRarSb127reG_DUwg9Ch-KyvjT1SkHgUWRVGcyly7uvVGRSDwsXypdrNinPA4jlhoNdizK2zF2CWQ")
			.firstPrimeFactor("9gY2w6I6S6L0juEKsbeDAwpd9WMfgqFoeA9vEyEUuk4kLwBKcoe1x4HG68ik918hdDSE9vDQSccA3xXHOAFOPJ8R9EeIAbTi1VwBYnbTp87X-xcPWlEPkrdoUKW60tgs1aNd_Nnc9LEVVPMS390zbFxt8TN_biaBgelNgbC95sM")
			.secondPrimeFactor("uKlCKvKv_ZJMVcdIs5vVSU_6cPtYI1ljWytExV_skstvRSNi9r66jdd9-yBhVfuG4shsp2j7rGnIio901RBeHo6TPKWVVykPu1iYhQXw1jIABfw-MVsN-3bQ76WLdt2SDxsHs7q7zPyUyHXmps7ycZ5c72wGkUwNOjYelmkiNS0")
			.firstFactorExponent("w0kZbV63cVRvVX6yk3C8cMxo2qCM4Y8nsq1lmMSYhG4EcL6FWbX5h9yuvngs4iLEFk6eALoUS4vIWEwcL4txw9LsWH_zKI-hwoReoP77cOdSL4AVcraHawlkpyd2TWjE5evgbhWtOxnZee3cXJBkAi64Ik6jZxbvk-RR3pEhnCs")
			.secondFactorExponent("o_8V14SezckO6CNLKs_btPdFiO9_kC1DsuUTd2LAfIIVeMZ7jn1Gus_Ff7B7IVx3p5KuBGOVF8L-qifLb6nQnLysgHDh132NDioZkhH7mI7hPG-PYE_odApKdnqECHWw0J-F0JWnUd6D2B_1TvF9mXA2Qx-iGYn8OVV1Bsmp6qU")
			.firstCoefficient("eNho5yRBEBxhGBtQRww9QirZsB66TrfFReG_CcteI1aCneT0ELGhYlRlCtUkTRclIfuEPmNsNDPbLoLqqCVznFbvdB7x-Tl-m0l_eFTj2KiqwGqE9PZB9nNTwMVvH3VRRSLWACvPnSiwP8N5Usy-WRXS-V7TbpxIhvepTfE0NNo")
			.build()
			.cache();
		
		JWTClaimsSet jcs = JWTClaimsSet.of("joe", 1300819380).addCustomClaim("http://example.com/is_root", true).build();
		
		JWE<JWS<JWTClaimsSet>> ejwts = jwtService.jwsBuilder(jwtKey)
			.header(header -> header.algorithm("RS256"))
			.payload(jcs)
			.build()
			.flatMap(jwt -> jweService.<JWS<JWTClaimsSet>>builder(jweKey)
				.header(header -> header
					.algorithm("RSA1_5")
					.encryptionAlgorithm("A128CBC-HS256")
					.contentType("JWT")
				)
				.payload(jwt)
				.build()
			)
			.block();
		
		String[] ejwtsSplitCompact = ejwts.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertNotNull(ejwtsSplitCompact[0]);
		Assertions.assertEquals("RSA1_5", ejwts.getHeader().getAlgorithm());
		Assertions.assertNull(ejwts.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("JWT", ejwts.getHeader().getContentType());
		Assertions.assertNull(ejwts.getHeader().getCritical());
		Assertions.assertNull(ejwts.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", ejwts.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(ejwts.getHeader().getJWK());
		Assertions.assertNull(ejwts.getHeader().getJWKSetURL());
		Assertions.assertNull(ejwts.getHeader().getKey());
		Assertions.assertNull(ejwts.getHeader().getKeyId());
		Assertions.assertNull(ejwts.getHeader().getType());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertNotNull(ejwtsSplitCompact[1]);
		Assertions.assertNotNull(ejwts.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertNotNull(ejwtsSplitCompact[2]);
		Assertions.assertNotNull(ejwts.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertNotNull(ejwtsSplitCompact[3]);
		JWS<JWTClaimsSet> jwts = ejwts.getPayload();
		
		Assertions.assertEquals(jcs, jwts.getPayload());
		Assertions.assertEquals("RS256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(jwtKey.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		// 4. JWE authentication tag
		Assertions.assertNotNull(ejwtsSplitCompact[4]);
		Assertions.assertNotNull(ejwts.getAuthenticationTag());
		
		// Check that we can read what we generate
		Type nestedJWTType = Types.type(JWS.class).type(JWTClaimsSet.class).and().build();
		ejwts = jweService.<JWS<JWTClaimsSet>>reader(nestedJWTType, jweKey).read(ejwts.toCompact(), raw -> jwtService.jwsReader(jwtKey).read(raw)).block();
		ejwtsSplitCompact = ejwts.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertNotNull(ejwtsSplitCompact[0]);
		Assertions.assertEquals("RSA1_5", ejwts.getHeader().getAlgorithm());
		Assertions.assertNull(ejwts.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("JWT", ejwts.getHeader().getContentType());
		Assertions.assertNull(ejwts.getHeader().getCritical());
		Assertions.assertNull(ejwts.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", ejwts.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(ejwts.getHeader().getJWK());
		Assertions.assertNull(ejwts.getHeader().getJWKSetURL());
		Assertions.assertNull(ejwts.getHeader().getKey());
		Assertions.assertNull(ejwts.getHeader().getKeyId());
		Assertions.assertNull(ejwts.getHeader().getType());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertNotNull(ejwtsSplitCompact[1]);
		Assertions.assertNotNull(ejwts.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertNotNull(ejwtsSplitCompact[2]);
		Assertions.assertNotNull(ejwts.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertNotNull(ejwtsSplitCompact[3]);
		jwts = ejwts.getPayload();
		
		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("RS256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(jwtKey.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		// 4. JWE authentication tag
		Assertions.assertNotNull(ejwtsSplitCompact[4]);
		Assertions.assertNotNull(ejwts.getAuthenticationTag());
		
		// Read representation from RFC (https://datatracker.ietf.org/doc/html/rfc7519#appendix-A.2)
		ejwts = jweService.<JWS<JWTClaimsSet>>reader(nestedJWTType, jweKey).read(compact, raw -> jwtService.jwsReader(jwtKey).read(raw)).block();
		ejwtsSplitCompact = ejwts.toCompact().split("\\.");
		
		// 0. JWE protected header
		Assertions.assertNotNull(ejwtsSplitCompact[0]);
		Assertions.assertEquals("RSA1_5", ejwts.getHeader().getAlgorithm());
		Assertions.assertNull(ejwts.getHeader().getCompressionAlgorithm());
		Assertions.assertEquals("JWT", ejwts.getHeader().getContentType());
		Assertions.assertNull(ejwts.getHeader().getCritical());
		Assertions.assertNull(ejwts.getHeader().getCustomParameters());
		Assertions.assertEquals("A128CBC-HS256", ejwts.getHeader().getEncryptionAlgorithm());
		Assertions.assertNull(ejwts.getHeader().getJWK());
		Assertions.assertNull(ejwts.getHeader().getJWKSetURL());
		Assertions.assertNull(ejwts.getHeader().getKey());
		Assertions.assertNull(ejwts.getHeader().getKeyId());
		Assertions.assertNull(ejwts.getHeader().getType());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(ejwts.getHeader().getX509CertificateURL());
		
		// 1. JWE encrypted key
		Assertions.assertNotNull(ejwtsSplitCompact[1]);
		Assertions.assertNotNull(ejwts.getEncryptedKey());
		
		// 2. JWE Initialization vector
		Assertions.assertNotNull(ejwtsSplitCompact[2]);
		Assertions.assertNotNull(ejwts.getInitializationVector());
		
		// 3. JWE cipher text
		Assertions.assertNotNull(ejwtsSplitCompact[3]);
		jwts = ejwts.getPayload();
		
		Assertions.assertEquals(jcs.getIssuer(), jwts.getPayload().getIssuer());
		Assertions.assertEquals(jcs.getExpirationTime(), jwts.getPayload().getExpirationTime());
		Assertions.assertEquals(jcs.getCustomClaims(), jwts.getPayload().getCustomClaims());
		Assertions.assertNull(jwts.getPayload().getAudience());
		Assertions.assertNull(jwts.getPayload().getIssuedAt());
		Assertions.assertNull(jwts.getPayload().getJWTId());
		Assertions.assertNull(jwts.getPayload().getNotBefore());
		Assertions.assertNull(jwts.getPayload().getSubject());
		Assertions.assertEquals("RS256", jwts.getHeader().getAlgorithm());
		Assertions.assertNull(jwts.getHeader().getType());
		Assertions.assertEquals(jwtKey.block(), jwts.getHeader().getKey());
		Assertions.assertNull(jwts.getHeader().getCritical());
		Assertions.assertNull(jwts.getHeader().getCustomParameters());
		Assertions.assertNull(jwts.getHeader().getJWK());
		Assertions.assertNull(jwts.getHeader().getJWKSetURL());
		Assertions.assertNull(jwts.getHeader().getKeyId());
		Assertions.assertNull(jwts.getHeader().getX509CertificateChain());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jwts.getHeader().getX509CertificateURL());
		
		// 4. JWE authentication tag
		Assertions.assertNotNull(ejwtsSplitCompact[4]);
		Assertions.assertNotNull(ejwts.getAuthenticationTag());
	}
	
	private static GenericECJWKBuilder ecJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericXECJWKBuilder xecJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericXECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericEdECJWKBuilder edecJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericEdECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private GenericOCTJWKBuilder octJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		
		return new GenericOCTJWKBuilder(configuration, jwkStore, keyResolver);
	}
	
	private GenericPBES2JWKBuilder pbesJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		
		return new GenericPBES2JWKBuilder(configuration, jwkStore, keyResolver);
	}
	
	private static GenericRSAJWKBuilder rsaJWKBuilder() {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(MAPPER);
		urlResolver.setResourceService(resourceService);
		
		return new GenericRSAJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	// jwk, jws, jwe, jwt
	@SuppressWarnings("unchecked")
	private static GenericDataConversionService dataConversionService() {
		MediaTypeConverter<String> textStringConverter = Mockito.mock(MediaTypeConverter.class);
		Mockito.when(textStringConverter.canConvert("text/plain")).thenReturn(true);
		
		Mockito.when(textStringConverter.encode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, String.class));		
		Mockito.when(textStringConverter.encodeOne(Mockito.isA(Mono.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class));
		Mockito.when(textStringConverter.decode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, String.class));
		Mockito.when(textStringConverter.decodeOne(Mockito.isA(Publisher.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class));
		
		MediaTypeConverter<String> jsonStringConverter = Mockito.mock(MediaTypeConverter.class);
		Mockito.when(jsonStringConverter.canConvert("application/json")).thenReturn(true);
		
		Mockito.when(jsonStringConverter.encode(Mockito.any(), Mockito.isA(Type.class))).thenAnswer(ans -> MAPPER.writeValueAsString(ans.getArgument(0)));
		Mockito.when(jsonStringConverter.encodeOne(Mockito.isA(Mono.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class).map(obj -> {
			try {
				return MAPPER.writeValueAsString(obj);
			} 
			catch(JsonProcessingException ex) {
				throw Exceptions.propagate(ex);
			}
		}));
		Mockito.when(jsonStringConverter.encodeOne(Mockito.isA(Mono.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class).map(obj -> {
			try {
				return MAPPER.writeValueAsString(obj);
			} 
			catch(JsonProcessingException ex) {
				throw Exceptions.propagate(ex);
			}
		}));
		Mockito.when(jsonStringConverter.decode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> MAPPER.readValue((String)ans.getArgument(0), MAPPER.constructType((Type)ans.getArgument(1))));
		Mockito.when(jsonStringConverter.decodeOne(Mockito.isA(Publisher.class), Mockito.isA(Type.class))).thenAnswer(
			ans -> Flux.from(ans.getArgument(0, Publisher.class))
				.cast(String.class)
				.reduceWith(() -> new StringBuilder(), (acc, v) -> ((StringBuilder)acc).append(v))
				.map(Object::toString)
				.map(obj -> {
					try {
						return MAPPER.readValue((String)obj, MAPPER.constructType((Type)ans.getArgument(1)));
					} 
					catch(JsonProcessingException ex) {
						throw Exceptions.propagate(ex);
					}
				})
		);
		
		JWTStringMediaTypeConverter jwtConverter = new JWTStringMediaTypeConverter();
		
		return new GenericDataConversionService(List.of(textStringConverter, jsonStringConverter, jwtConverter));
	}
	
	private static GenericJWKService jwkService() {
		return jwkService(null);
	}
	
	private static GenericJWKService jwkService(PKIXParameters pkixParameters) {
		ObjectMapper mapper = new ObjectMapper();
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		JWKStore jwkStore = new NoOpJWKStore();
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
		
		return new GenericJWKService(configuration, ecJWKFactory , rsaJWKFactory, symmetricJWKFactory, edecJWKFactory, xecJWKFactory, pbes2JWKFactory, jwkStore, urlResolver, switchableUrlResolver, mapper);
	}
	
	private static PKIXParameters pkixParameters(String cert, Date date) throws CertificateException, InvalidAlgorithmParameterException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		
		X509Certificate certificate = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(cert)));
		PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(certificate, null)));
		parameters.setRevocationEnabled(false);
		parameters.setDate(date);
			
		return parameters;
	}
	
	private static JoseServices joseServices() {
		GenericJWKService jwkService = jwkService();
		GenericDataConversionService dataConversionService = dataConversionService();
		
		GenericJWSService jwsService = new GenericJWSService(MAPPER, dataConversionService, jwkService);
		GenericJWEService jweService = new GenericJWEService(MAPPER, dataConversionService, jwkService);
		GenericJWTService jwtService = new GenericJWTService(MAPPER, dataConversionService, jwkService);
		
		dataConversionService.injectJWEService(jweService);
		dataConversionService.injectJWSService(jwsService);
		dataConversionService.injectJWTService(jwtService);
		
		return new JoseServices(jwsService, jweService, jwtService);
	}
	
	private static class JoseServices {
	
		final JWSService jwsService;
		final JWEService jweService;
		final JWTService jwtService;

		public JoseServices(JWSService jwsService, JWEService jweService, JWTService jwtService) {
			this.jwsService = jwsService;
			this.jweService = jweService;
			this.jwtService = jwtService;
		}
	}
	
	private static class RFC71515PrettyPrinter extends MinimalPrettyPrinter {

		private static final long serialVersionUID = 1L;

		@Override
		public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
			super.writeObjectEntrySeparator(g);
			g.writeRaw("\r\n ");
		}
	}
	
	private static class RFC7515JWSHeaderSerializer extends StdSerializer<JWSHeader> {

		private static final long serialVersionUID = 1L;

		public RFC7515JWSHeaderSerializer() {
			this(null);
		}

		public RFC7515JWSHeaderSerializer(Class<JWSHeader> t) {
			super(t);
		}

		@Override
		public void serialize(JWSHeader jwsHeader, JsonGenerator jg, SerializerProvider sp) throws IOException {
			jg.writeStartObject();
			if(jwsHeader.getType() != null) {
				jg.writeStringField("typ", jwsHeader.getType());
			}
			jg.writeStringField("alg", jwsHeader.getAlgorithm());
			jg.writeEndObject();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static class RFC7515PayloadSerializer extends StdSerializer<JWTClaimsSet> {

		private static final long serialVersionUID = 1L;
		
		public RFC7515PayloadSerializer() {
			this(null);
		}

		public RFC7515PayloadSerializer(Class<JWTClaimsSet> t) {
			super(t);
		}

		@Override
		public Class<JWTClaimsSet> handledType() {
			return JWTClaimsSet.class;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void serialize(JWTClaimsSet jwt, JsonGenerator jg, SerializerProvider sp) throws IOException {
			jg.writeStartObject();
			
			if(jwt.getIssuer() != null) {
				jg.writeStringField("iss", jwt.getIssuer());
			}
			if(jwt.getExpirationTime() != null) {
				jg.writeNumberField("exp", jwt.getExpirationTime());
			}
			jwt.getCustomClaim("http://example.com/is_root")
				.ifPresent(claim -> {
					try {
						jg.writeBooleanField(claim.getName(), claim.asBoolean());
					}
					catch(IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			
			jg.writeEndObject();
		}
	}
}
