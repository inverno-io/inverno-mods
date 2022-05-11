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
package io.inverno.mod.security.jose.internal.jws;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.base.resource.ResourceService;
import io.inverno.mod.security.jose.JOSEConfiguration;
import io.inverno.mod.security.jose.JOSEProcessingException;
import io.inverno.mod.security.jose.internal.converter.GenericDataConversionService;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKKeyResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKService;
import io.inverno.mod.security.jose.internal.jwk.GenericJWKURLResolver;
import io.inverno.mod.security.jose.internal.jwk.GenericX509JWKCertPathValidator;
import io.inverno.mod.security.jose.internal.jwk.JWKPKIXParameters;
import io.inverno.mod.security.jose.internal.jwk.NoOpJWKStore;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWK;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.ec.GenericECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWK;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.oct.GenericOCTJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWK;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericEdECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.okp.GenericXECJWKFactory;
import io.inverno.mod.security.jose.internal.jwk.pbes2.GenericPBES2JWKFactory;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWK;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKBuilder;
import io.inverno.mod.security.jose.internal.jwk.rsa.GenericRSAJWKFactory;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwk.JWKService;
import io.inverno.mod.security.jose.jwk.JWKStore;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JWSHeader;
import io.inverno.mod.security.jose.jws.JWSService;
import io.inverno.mod.security.jose.jws.JsonJWS;
import io.inverno.mod.security.jose.jws.JsonJWS.ReadSignature;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class GenericJWSServiceTest {
	
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	private static final ExecutorService WORKER_POOL = Executors.newCachedThreadPool();
	
	@Test
	public void testRFC7515_A1() throws JsonProcessingException {
		JWSService jwsService = jwsService(mapperRFC7515());
		GenericOCTJWKBuilder octJWKBuilder = octJWKBuilder(mapper());
		
		String compact = "eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
		
		Mono<GenericOCTJWK> key = octJWKBuilder
			.keyValue("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
			.build()
			.cache();
		
		Map<String, Object> jwtPayload = new LinkedHashMap<>();
		jwtPayload.put("iss", "joe");
		jwtPayload.put("exp", 1300819380);
		jwtPayload.put("http://example.com/is_root", true);
		
		JWS<Map<String, Object>> jws = jwsService.<Map<String, Object>>builder(Map.class, key)
			.header(header -> header
				.algorithm("HS256")
				.type("JWT")
			)
			.payload(jwtPayload)
			.build(MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, jws.getPayload());
		Assertions.assertEquals("HS256", jws.getHeader().getAlgorithm());
		Assertions.assertEquals("JWT", jws.getHeader().getType());
		Assertions.assertNull(jws.getHeader().getContentType());
		Assertions.assertEquals(key.block(), jws.getHeader().getKey());
		Assertions.assertNull(jws.getHeader().getCritical());
		Assertions.assertNull(jws.getHeader().getCustomParameters());
		Assertions.assertNull(jws.getHeader().getJWK());
		Assertions.assertNull(jws.getHeader().getJWKSetURL());
		Assertions.assertNull(jws.getHeader().getKeyId());
		Assertions.assertNull(jws.getHeader().getX509CertificateChain());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateURL());
		
		String signingInput = jws.toCompact().substring(0, jws.toCompact().lastIndexOf('.'));
		
		Assertions.assertEquals(compact, jws.toCompact());
		Assertions.assertEquals("eyJ0eXAiOiJKV1QiLA0KICJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ", signingInput);
		Assertions.assertEquals("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", jws.getSignature());
		
		jws.getHeader().getKey().signer("HS256").verify(signingInput.getBytes(), jws.getSignature().getBytes());
		
		// read
		JWS<Map<String, Object>> readJWS = jwsService.<Map<String, Object>>reader(Types.type(Map.class).type(String.class).and().type(Object.class).and().build(), key).read(compact, "json").block();
		
		Assertions.assertEquals(jwtPayload, readJWS.getPayload());
		Assertions.assertEquals("HS256", readJWS.getHeader().getAlgorithm());
		Assertions.assertEquals("JWT", readJWS.getHeader().getType());
		Assertions.assertNull(readJWS.getHeader().getContentType());
		Assertions.assertEquals(key.block(), readJWS.getHeader().getKey());
		Assertions.assertNull(readJWS.getHeader().getCritical());
		Assertions.assertNull(readJWS.getHeader().getCustomParameters());
		Assertions.assertNull(readJWS.getHeader().getJWK());
		Assertions.assertNull(readJWS.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWS.getHeader().getKeyId());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateURL());
	}
	
	@Test
	public void testRFC7515_A2() throws JsonProcessingException {
		JWSService jwsService = jwsService(mapperRFC7515());
		GenericRSAJWKBuilder rsaJWKBuilder = rsaJWKBuilder(mapper());
		
		String compact = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw";
		
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
		Mono<GenericRSAJWK> key = rsaJWKBuilder
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
		
		Map<String, Object> jwtPayload = new LinkedHashMap<>();
		jwtPayload.put("iss","joe");
		jwtPayload.put("exp",1300819380);
		jwtPayload.put("http://example.com/is_root",true);
		
		JWS<Map<String, Object>> jws = jwsService.<Map<String, Object>>builder(Map.class, key)
			.header(header -> header
				.algorithm("RS256")
			)
			.payload(jwtPayload)
			.build(MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, jws.getPayload());
		Assertions.assertEquals("RS256", jws.getHeader().getAlgorithm());
		Assertions.assertNull(jws.getHeader().getType());
		Assertions.assertNull(jws.getHeader().getContentType());
		Assertions.assertEquals(key.block(), jws.getHeader().getKey());
		Assertions.assertNull(jws.getHeader().getCritical());
		Assertions.assertNull(jws.getHeader().getCustomParameters());
		Assertions.assertNull(jws.getHeader().getJWK());
		Assertions.assertNull(jws.getHeader().getJWKSetURL());
		Assertions.assertNull(jws.getHeader().getKeyId());
		Assertions.assertNull(jws.getHeader().getX509CertificateChain());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateURL());
		
		String signingInput = jws.toCompact().substring(0, jws.toCompact().lastIndexOf('.'));
		
		Assertions.assertEquals(compact, jws.toCompact());
		Assertions.assertEquals("eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ", signingInput);
		Assertions.assertEquals("cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw", jws.getSignature());
		
		jws.getHeader().getKey().signer("RS256").verify(signingInput.getBytes(), Base64.getUrlDecoder().decode(jws.getSignature().getBytes()));
		
		// read
		JWS<Map<String, Object>> readJWS = jwsService.<Map<String, Object>>reader(Types.type(Map.class).type(String.class).and().type(Object.class).and().build(), key).read(compact, "json").block();
		
		Assertions.assertEquals(jwtPayload, readJWS.getPayload());
		Assertions.assertEquals("RS256", readJWS.getHeader().getAlgorithm());
		Assertions.assertNull(readJWS.getHeader().getType());
		Assertions.assertNull(readJWS.getHeader().getContentType());
		Assertions.assertEquals(key.block(), readJWS.getHeader().getKey());
		Assertions.assertNull(readJWS.getHeader().getCritical());
		Assertions.assertNull(readJWS.getHeader().getCustomParameters());
		Assertions.assertNull(readJWS.getHeader().getJWK());
		Assertions.assertNull(readJWS.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWS.getHeader().getKeyId());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateURL());
	}
	
	@Test
	public void testRFC7515_A3() throws JsonProcessingException {
		JWSService jwsService = jwsService(mapperRFC7515());
		GenericECJWKBuilder ecJWKBuilder = ecJWKBuilder(mapper());
		
		String compact = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q";
		
		Mono<GenericECJWK> key = ecJWKBuilder
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
			.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
			.eccPrivateKey("jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI")
			.build()
			.cache();
		
		Map<String, Object> jwtPayload = new LinkedHashMap<>();
		jwtPayload.put("iss","joe");
		jwtPayload.put("exp",1300819380);
		jwtPayload.put("http://example.com/is_root",true);
		
		JWS<Map<String, Object>> jws = jwsService.<Map<String, Object>>builder(Map.class, key)
			.header(header -> header
				.algorithm("ES256")
			)
			.payload(jwtPayload)
			.build(MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, jws.getPayload());
		Assertions.assertEquals("ES256", jws.getHeader().getAlgorithm());
		Assertions.assertNull(jws.getHeader().getType());
		Assertions.assertNull(jws.getHeader().getContentType());
		Assertions.assertEquals(key.block(), jws.getHeader().getKey());
		Assertions.assertNull(jws.getHeader().getCritical());
		Assertions.assertNull(jws.getHeader().getCustomParameters());
		Assertions.assertNull(jws.getHeader().getJWK());
		Assertions.assertNull(jws.getHeader().getJWKSetURL());
		Assertions.assertNull(jws.getHeader().getKeyId());
		Assertions.assertNull(jws.getHeader().getX509CertificateChain());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateURL());
		
		String signingInput = jws.toCompact().substring(0, jws.toCompact().lastIndexOf('.'));
		
		// ECDSA is non deterministic so we signatures won't match...
		Assertions.assertEquals("eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ", signingInput);
		Assertions.assertNotNull(jws.getSignature());
		
		// ...but they must still be valid
		jws.getHeader().getKey().signer("ES256").verify(signingInput.getBytes(), Base64.getUrlDecoder().decode(jws.getSignature().getBytes()));
		
		// read
		JWS<Map<String, Object>> readJWS = jwsService.<Map<String, Object>>reader(Types.type(Map.class).type(String.class).and().type(Object.class).and().build(), key).read(compact, "json").block();
		
		Assertions.assertEquals(jwtPayload, readJWS.getPayload());
		Assertions.assertEquals("ES256", readJWS.getHeader().getAlgorithm());
		Assertions.assertNull(readJWS.getHeader().getType());
		Assertions.assertNull(readJWS.getHeader().getContentType());
		Assertions.assertEquals(key.block(), readJWS.getHeader().getKey());
		Assertions.assertNull(readJWS.getHeader().getCritical());
		Assertions.assertNull(readJWS.getHeader().getCustomParameters());
		Assertions.assertNull(readJWS.getHeader().getJWK());
		Assertions.assertNull(readJWS.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWS.getHeader().getKeyId());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateURL());
	}
	
	@Test
	public void testRFC7515_A4() throws JsonProcessingException {
		JWSService jwsService = jwsService(mapperRFC7515());
		GenericECJWKBuilder ecJWKBuilder = ecJWKBuilder(mapper());
		
		String compact = "eyJhbGciOiJFUzUxMiJ9.UGF5bG9hZA.AdwMgeerwtHoh-l192l60hp9wAHZFVJbLfD_UxMi70cwnZOYaRI1bKPWROc-mZZqwqT2SI-KGDKB34XO0aw_7XdtAG8GaSwFKdCAPZgoXD2YBJZCPEX3xKpRwcdOO8KpEHwJjyqOgzDO7iKvU8vcnwNrmxYbSW9ERBXukOXolLzeO_Jn";
		Mono<GenericECJWK> key = ecJWKBuilder
			.curve(ECCurve.P_521.getCurve())
			.xCoordinate("AekpBQ8ST8a8VcfVOTNl353vSrDCLLJXmPk06wTjxrrjcBpXp5EOnYG_NjFZ6OvLFV1jSfS9tsz4qUxcWceqwQGk")
			.yCoordinate("ADSmRA43Z1DSNx_RvcLI87cdL07l6jQyyBXMoxVg_l2Th-x3S1WDhjDly79ajL4Kkd0AZMaZmh9ubmf63e3kyMj2")
			.eccPrivateKey("AY5pb7A0UFiB3RELSD64fTLOSV_jazdF7fLYyuTw8lOfRhWg6Y6rUrPAxerEzgdRhajnu0ferB0d53vM9mE15j2C")
			.build()
			.cache();
		
		String payload = "Payload";
		
		JWS<String> jws = jwsService.builder(String.class, key)
			.header(header -> header
				.algorithm("ES512")
			)
			.payload(payload)
			.build(MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertEquals(payload, jws.getPayload());
		Assertions.assertEquals("ES512", jws.getHeader().getAlgorithm());
		Assertions.assertNull(jws.getHeader().getType());
		Assertions.assertNull(jws.getHeader().getContentType());
		Assertions.assertEquals(key.block(), jws.getHeader().getKey());
		Assertions.assertNull(jws.getHeader().getCritical());
		Assertions.assertNull(jws.getHeader().getCustomParameters());
		Assertions.assertNull(jws.getHeader().getJWK());
		Assertions.assertNull(jws.getHeader().getJWKSetURL());
		Assertions.assertNull(jws.getHeader().getKeyId());
		Assertions.assertNull(jws.getHeader().getX509CertificateChain());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateURL());
		
		String signingInput = jws.toCompact().substring(0, jws.toCompact().lastIndexOf('.'));
		
		// ECDSA is non deterministic so we signatures won't match...
		Assertions.assertEquals("eyJhbGciOiJFUzUxMiJ9.UGF5bG9hZA", signingInput);
		Assertions.assertNotNull(jws.getSignature());
		
		// ...but they must still be valid
		jws.getHeader().getKey().signer("ES512").verify(signingInput.getBytes(), Base64.getUrlDecoder().decode(jws.getSignature().getBytes()));
		
		// read
		JWS<String> readJWS = jwsService.reader(String.class, key).read(compact, "text/plain").block();
		
		Assertions.assertEquals(payload, readJWS.getPayload());
		Assertions.assertEquals("ES512", readJWS.getHeader().getAlgorithm());
		Assertions.assertNull(readJWS.getHeader().getType());
		Assertions.assertNull(readJWS.getHeader().getContentType());
		Assertions.assertEquals(key.block(), readJWS.getHeader().getKey());
		Assertions.assertNull(readJWS.getHeader().getCritical());
		Assertions.assertNull(readJWS.getHeader().getCustomParameters());
		Assertions.assertNull(readJWS.getHeader().getJWK());
		Assertions.assertNull(readJWS.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWS.getHeader().getKeyId());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateURL());
	}
	
	@Test
	public void testRFC7515_A5() throws JsonProcessingException {
		JWSService jwsService = jwsService(mapperRFC7515());
		
		String compact = "eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.";
		
		Map<String, Object> jwtPayload = new LinkedHashMap<>();
		jwtPayload.put("iss","joe");
		jwtPayload.put("exp",1300819380);
		jwtPayload.put("http://example.com/is_root",true);
		
		JWS<Map<String, Object>> jws = jwsService.<Map<String, Object>>builder(Map.class, Mono.empty())
			.header(header -> header
				.algorithm("none")
			)
			.payload(jwtPayload)
			.build(MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, jws.getPayload());
		Assertions.assertEquals("none", jws.getHeader().getAlgorithm());
		Assertions.assertNull(jws.getHeader().getType());
		Assertions.assertNull(jws.getHeader().getContentType());
		Assertions.assertNull(jws.getHeader().getKey());
		Assertions.assertNull(jws.getHeader().getCritical());
		Assertions.assertNull(jws.getHeader().getCustomParameters());
		Assertions.assertNull(jws.getHeader().getJWK());
		Assertions.assertNull(jws.getHeader().getJWKSetURL());
		Assertions.assertNull(jws.getHeader().getKeyId());
		Assertions.assertNull(jws.getHeader().getX509CertificateChain());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateURL());
		Assertions.assertNull(jws.getSignature());
		
		String signingInput = jws.toCompact().substring(0, jws.toCompact().lastIndexOf('.'));
		
		Assertions.assertEquals(compact, jws.toCompact());
		Assertions.assertEquals("eyJhbGciOiJub25lIn0.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ", signingInput);
		
		// read
		JWS<Map<String, Object>> readJWS = jwsService.<Map<String, Object>>reader(Types.type(Map.class).type(String.class).and().type(Object.class).and().build(), Mono.empty()).read(compact, "json").block();
		
		Assertions.assertEquals(jwtPayload, readJWS.getPayload());
		Assertions.assertEquals("none", readJWS.getHeader().getAlgorithm());
		Assertions.assertNull(readJWS.getHeader().getType());
		Assertions.assertNull(readJWS.getHeader().getContentType());
		Assertions.assertNull(readJWS.getHeader().getKey());
		Assertions.assertNull(readJWS.getHeader().getCritical());
		Assertions.assertNull(readJWS.getHeader().getCustomParameters());
		Assertions.assertNull(readJWS.getHeader().getJWK());
		Assertions.assertNull(readJWS.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWS.getHeader().getKeyId());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateURL());
		Assertions.assertNull(jws.getSignature());
	}
	
	@Test
	public void testRFC7515_A6() throws JsonProcessingException {
		JWSService jwsService = jwsService(mapperRFC7515());
		ObjectMapper mapper = mapper();
		GenericRSAJWKBuilder rsaJWKBuilder = rsaJWKBuilder(mapper);
		GenericECJWKBuilder ecJWKBuilder = ecJWKBuilder(mapper);
		
		Mono<GenericRSAJWK> key1 = rsaJWKBuilder
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
		
		Mono<GenericECJWK> key2 = ecJWKBuilder
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
			.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
			.eccPrivateKey("jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI")
			.build()
			.cache();
		
		Map<String, Object> jwtPayload = new LinkedHashMap<>();
		jwtPayload.put("iss","joe");
		jwtPayload.put("exp",1300819380);
		jwtPayload.put("http://example.com/is_root",true);
		
		JsonJWS<Map<String, Object>, JsonJWS.BuiltSignature<Map<String, Object>>> builtJsonJWS = jwsService.<Map<String, Object>>jsonBuilder(Map.class)
				.payload(jwtPayload)
				.signature(
					header -> header
						.algorithm("RS256"), 
					header -> header
						.keyId("2010-12-29"), 
					key1
				)
				.signature(
					header -> header
						.algorithm("ES256"), 
					header -> header
						.keyId("e9bc097a-ce51-4036-9562-d2ade882db0d"),
					key2
				)
				.build(MediaTypes.APPLICATION_JSON)
				.block();
		
		JsonJWS<Map<String, Object>, JsonJWS.ReadSignature<Map<String, Object>>> readJsonJWS = jwsService.<Map<String, Object>>jsonReader(Map.class)
			.read(builtJsonJWS.toJson(), MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, readJsonJWS.getPayload());
		
		Assertions.assertEquals(2, readJsonJWS.getSignatures().size());
		
		JWS<Map<String, Object>> sigJWS = readJsonJWS.getSignatures().get(0).readJWS(key1).block();
		Assertions.assertEquals(builtJsonJWS.getSignatures().get(0).getJWS().getSignature(), sigJWS.getSignature());
		Assertions.assertEquals(readJsonJWS.getPayload(), sigJWS.getPayload());
		
		sigJWS = readJsonJWS.getSignatures().get(1).readJWS(key2).block();
		Assertions.assertEquals(builtJsonJWS.getSignatures().get(1).getJWS().getSignature(), sigJWS.getSignature());
		Assertions.assertEquals(readJsonJWS.getPayload(), sigJWS.getPayload());
		
		// RFC7515 A.6.4
		String completeJson = "{\"payload\":\"eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ\",\"signatures\":[{\"protected\":\"eyJhbGciOiJSUzI1NiJ9\",\"header\":{\"kid\":\"2010-12-29\"},\"signature\":\"cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw\"},{\"protected\":\"eyJhbGciOiJFUzI1NiJ9\",\"header\":{\"kid\":\"e9bc097a-ce51-4036-9562-d2ade882db0d\"},\"signature\":\"DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q\"}]}";
		readJsonJWS = jwsService.<Map<String, Object>>jsonReader(Map.class)
			.read(completeJson, MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, readJsonJWS.getPayload());
		
		Assertions.assertEquals(2, readJsonJWS.getSignatures().size());
		
		sigJWS = readJsonJWS.getSignatures().get(0).readJWS(key1).block();
		Assertions.assertEquals(builtJsonJWS.getSignatures().get(0).getJWS().getSignature(), sigJWS.getSignature());
		Assertions.assertEquals(readJsonJWS.getPayload(), sigJWS.getPayload());
		
		sigJWS = readJsonJWS.getSignatures().get(1).readJWS(key2).block();
		Assertions.assertEquals("DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q", sigJWS.getSignature()); 
		Assertions.assertEquals(readJsonJWS.getPayload(), sigJWS.getPayload());
	}
	
	@Test
	public void testRFC7515_A7() throws JsonProcessingException {
		ObjectMapper mapper = mapper();
		JWSService jwsService = jwsService(mapper);
		GenericECJWKBuilder ecJWKBuilder = ecJWKBuilder(mapper);
		
		Mono<GenericECJWK> key2 = ecJWKBuilder
			.curve(ECCurve.P_256.getCurve())
			.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
			.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
			.eccPrivateKey("jpsQnnGQmL-YBIffH1136cspYG6-0iY7X1fCE9-E9LI")
			.build()
			.cache();
		
		Map<String, Object> jwtPayload = new LinkedHashMap<>();
		jwtPayload.put("iss","joe");
		jwtPayload.put("exp",1300819380);
		jwtPayload.put("http://example.com/is_root",true);
		
		JsonJWS<Map<String, Object>, JsonJWS.BuiltSignature<Map<String, Object>>> builtJsonJWS = jwsService.<Map<String, Object>>jsonBuilder(Map.class)
				.payload(jwtPayload)
				.signature(
					header -> header
						.algorithm("ES256"), 
					header -> header
						.keyId("e9bc097a-ce51-4036-9562-d2ade882db0d"),
					key2
				)
				.build(MediaTypes.APPLICATION_JSON)
				.block();
		
		JsonJWS<Map<String, Object>, JsonJWS.ReadSignature<Map<String, Object>>> readJsonJWS = jwsService.<Map<String, Object>>jsonReader(Map.class)
			.read(builtJsonJWS.toJson(), MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, readJsonJWS.getPayload());
		
		Assertions.assertEquals(1, readJsonJWS.getSignatures().size());
		
		JWS<Map<String, Object>> sigJWS = readJsonJWS.getSignatures().get(0).readJWS(key2).block();
		Assertions.assertEquals(builtJsonJWS.getSignatures().get(0).getJWS().getSignature(), sigJWS.getSignature());
		Assertions.assertEquals(readJsonJWS.getPayload(), sigJWS.getPayload());
		
		// RFC7515 A.7
		String completeJson = "{\"payload\":\"eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ\",\"protected\":\"eyJhbGciOiJFUzI1NiJ9\",\"header\":{\"kid\":\"e9bc097a-ce51-4036-9562-d2ade882db0d\"},\"signature\":\"DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q\"}";
		readJsonJWS = jwsService.<Map<String, Object>>jsonReader(Map.class)
			.read(completeJson, MediaTypes.APPLICATION_JSON)
			.block();
		
		Assertions.assertEquals(jwtPayload, readJsonJWS.getPayload());
		
		Assertions.assertEquals(1, readJsonJWS.getSignatures().size());
		
		sigJWS = readJsonJWS.getSignatures().get(0).readJWS(key2).block();
		Assertions.assertEquals("DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q", sigJWS.getSignature()); 
		Assertions.assertEquals(readJsonJWS.getPayload(), sigJWS.getPayload());
	}
	
	@Test
	public void testRFC7797_4() {
		GenericJWSService jwsService = jwsService(mapperRFC7515().disable(SerializationFeature.INDENT_OUTPUT));
		
		String compactB64 = "eyJhbGciOiJIUzI1NiJ9.JC4wMg.5mvfOroL-g7HyqJoozehmsaqmvTYGEq5jTI1gVvoEoQ";
		String compactNoB64 = "eyJhbGciOiJIUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..A5dxf2s96_n5FLueVuW1Z_vh161FwXZC4YLPff6dmDY";

		Mono<? extends OCTJWK> key = jwkService().oct().builder()
			.keyValue("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
			.build()
			.cache();

		JWS<String> jwsB64 = jwsService.builder(String.class, key)
			.header(header -> header
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
			)
			.payload("$.02")
			.build(MediaTypes.TEXT_PLAIN)
			.block();

		Assertions.assertEquals(compactB64, jwsB64.toCompact());

		JWS<String> jwsNoB64 = jwsService.builder(String.class, key)
			.header(header -> header
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.base64EncodePayload(false)
			)
			.payload("$.02")
			.build(MediaTypes.TEXT_PLAIN)
			.block();

		Assertions.assertEquals("Unencoded payloads containing '.' characters must not be used with JWS compact representation", Assertions.assertThrows(JOSEProcessingException.class, () -> jwsNoB64.toCompact()).getMessage());

		Assertions.assertEquals(compactNoB64, jwsNoB64.toDetachedCompact());
	}
	
	@Test
	public void testRFC8037_A4_A5() {
		JWSService jwsService = jwsService(mapperRFC7515());
		GenericEdECJWKBuilder edecJWKBuilder = edecJWKBuilder(mapper());
		
		String compact = "eyJhbGciOiJFZERTQSJ9.RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc.hgyY0il_MGCjP0JzlnLWG1PPOt7-09PGcvMg3AIbQR6dWbhijcNR4ki4iylGjg5BhVsPt9g7sVvpAr_MuM0KAg";
		
		// {
		//   "kty":"OKP","crv":"Ed25519",
		//   "d":"nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A",
		//   "x":"11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo"
		// }
		Mono<GenericEdECJWK> key = edecJWKBuilder
			.curve("Ed25519")
			.publicKey("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")
			.privateKey("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")
			.build()
			.cache();
		
		String payload = "Example of Ed25519 signing";
		
		JWS<String> jws = jwsService.builder(String.class, key)
			.header(header -> header
				.algorithm("EdDSA")
			)
			.payload(payload)
			.build(MediaTypes.TEXT_PLAIN)
			.block();
		
		Assertions.assertEquals(payload, jws.getPayload());
		Assertions.assertEquals("EdDSA", jws.getHeader().getAlgorithm());
		Assertions.assertNull(jws.getHeader().getType());
		Assertions.assertNull(jws.getHeader().getContentType());
		Assertions.assertEquals(key.block(), jws.getHeader().getKey());
		Assertions.assertNull(jws.getHeader().getCritical());
		Assertions.assertNull(jws.getHeader().getCustomParameters());
		Assertions.assertNull(jws.getHeader().getJWK());
		Assertions.assertNull(jws.getHeader().getJWKSetURL());
		Assertions.assertNull(jws.getHeader().getKeyId());
		Assertions.assertNull(jws.getHeader().getX509CertificateChain());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(jws.getHeader().getX509CertificateURL());
		
		String signingInput = jws.toCompact().substring(0, jws.toCompact().lastIndexOf('.'));
		
		// ECDSA is non deterministic so we signatures won't match...
		Assertions.assertEquals("eyJhbGciOiJFZERTQSJ9.RXhhbXBsZSBvZiBFZDI1NTE5IHNpZ25pbmc", signingInput);
		Assertions.assertNotNull(jws.getSignature());
		
		// ...but they must still be valid
		jws.getHeader().getKey().signer("EdDSA").verify(signingInput.getBytes(), Base64.getUrlDecoder().decode(jws.getSignature().getBytes()));
		
		// read
		JWS<String> readJWS = jwsService.reader(String.class, key).read(compact, "text/plain").block();
		
		Assertions.assertEquals(payload, readJWS.getPayload());
		Assertions.assertEquals("EdDSA", readJWS.getHeader().getAlgorithm());
		Assertions.assertNull(readJWS.getHeader().getType());
		Assertions.assertNull(readJWS.getHeader().getContentType());
		Assertions.assertEquals(key.block(), readJWS.getHeader().getKey());
		Assertions.assertNull(readJWS.getHeader().getCritical());
		Assertions.assertNull(readJWS.getHeader().getCustomParameters());
		Assertions.assertNull(readJWS.getHeader().getJWK());
		Assertions.assertNull(readJWS.getHeader().getJWKSetURL());
		Assertions.assertNull(readJWS.getHeader().getKeyId());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateChain());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA1Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateSHA256Thumbprint());
		Assertions.assertNull(readJWS.getHeader().getX509CertificateURL());
	}
	
	@Test
	public void testJoseJson() {
		GenericJWKService jwkService = jwkService();
		GenericJWSService jwsService = jwsService(mapper());
		
		String payload = "Do or do not, there's no try.";
		
		Mono<? extends OCTJWK> key1 = jwkService.oct().generator().algorithm(OCTAlgorithm.HS256.getAlgorithm()).keyId("key1").generate().cache();
		
		JsonJWS<String, JsonJWS.BuiltSignature<String>> jsonJWSPayload = jwsService.jsonBuilder(String.class)
			.payload(payload)
			.signature(
				pheader -> pheader
					.algorithm(OCTAlgorithm.HS256.getAlgorithm()), 
				uheader -> uheader
					.contentType(MediaTypes.TEXT_PLAIN)
					.keyId("gen"),
				key1
			)
			.build()
			.block();
		
		Mono<? extends OCTJWK> key2 = jwkService.oct().generator().algorithm(OCTAlgorithm.HS256.getAlgorithm()).keyId("key2").generate().cache();
		
		JWS<JsonJWS<String, JsonJWS.BuiltSignature<String>>> jws = jwsService.<JsonJWS<String, JsonJWS.BuiltSignature<String>>>builder(JsonJWS.class, key2)
			.header(header -> header
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.keyId("key2")
				.contentType(MediaTypes.APPLICATION_JOSE_JSON)
			)
			.payload(jsonJWSPayload)
			.build()
			.block();
		
		Assertions.assertEquals(jsonJWSPayload, jws.getPayload());
		
		JWS<JsonJWS<String, ReadSignature<String>>> readJWS = jwsService.<JsonJWS<String, JsonJWS.ReadSignature<String>>>reader(JsonJWS.class, key2)
			.read(jws.toCompact())
			.block();
		
		Assertions.assertEquals(jsonJWSPayload.toJson(), readJWS.getPayload().toJson());
		
		Assertions.assertEquals(1, readJWS.getPayload().getSignatures().size());
		
		// Check that signature is valid
		readJWS.getPayload().getSignatures().get(0).readJWS(key1).block();
	}
	
	private static ObjectMapper mapper() {
		return new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
	}
	
	/** 
	 * In order to comply with RFC7515 tests:
	 * - the content type must not be serialized in the JOSE header
	 * - the content type is however required to be able to determine which media type converter to use to encode/decode the payload
	 * - json must be formatted in particular way using a custom PrettyPrinter
	 */
	private static ObjectMapper mapperRFC7515() {
		SimpleModule module = new SimpleModule();
		module.addSerializer(GenericJWSHeader.class, new RFC7515JWSHeaderSerializer());
		module.addSerializer(new RFC7515PayloadSerializer());
		
		return new ObjectMapper()
			.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDefaultPrettyPrinter(new RFC71515PrettyPrinter())
			.registerModule(module);
	}

	private static GenericECJWKBuilder ecJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private static GenericEdECJWKBuilder edecJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericEdECJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	private GenericOCTJWKBuilder octJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		
		return new GenericOCTJWKBuilder(configuration, jwkStore, keyResolver);
	}
	
	private static GenericRSAJWKBuilder rsaJWKBuilder(ObjectMapper mapper) {
		JOSEConfiguration configuration = Mockito.mock(JOSEConfiguration.class);
		ResourceService resourceService = Mockito.mock(ResourceService.class);
		
		JWKStore jwkStore = new NoOpJWKStore();
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, null, mapper);
		urlResolver.setResourceService(resourceService);
		
		return new GenericRSAJWKBuilder(configuration, jwkStore, keyResolver, urlResolver, null);
	}
	
	@SuppressWarnings("unchecked")
	private static GenericDataConversionService dataConversionService(JWKService jwkService, ObjectMapper mapper) {
		MediaTypeConverter<String> textStringConverter = Mockito.mock(MediaTypeConverter.class);
		Mockito.when(textStringConverter.canConvert("text/plain")).thenReturn(true);
		
		Mockito.when(textStringConverter.encode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, String.class));		
		Mockito.when(textStringConverter.encodeOne(Mockito.isA(Mono.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class));
		Mockito.when(textStringConverter.decode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, String.class));
		Mockito.when(textStringConverter.decodeOne(Mockito.isA(Publisher.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class));
		
		MediaTypeConverter<String> jsonStringConverter = Mockito.mock(MediaTypeConverter.class);
		Mockito.when(jsonStringConverter.canConvert("application/json")).thenReturn(true);
		
		Mockito.when(jsonStringConverter.encode(Mockito.any(), Mockito.isA(Type.class))).thenAnswer(ans -> mapper.writeValueAsString(ans.getArgument(0)));
		Mockito.when(jsonStringConverter.encodeOne(Mockito.isA(Mono.class), Mockito.isA(Type.class))).thenAnswer(ans -> ans.getArgument(0, Mono.class).map(obj -> {
			try {
				return mapper.writeValueAsString(obj);
			} 
			catch(JsonProcessingException ex) {
				throw Exceptions.propagate(ex);
			}
		}));
		Mockito.when(jsonStringConverter.decode(Mockito.anyString(), Mockito.isA(Type.class))).thenAnswer(ans -> mapper.readValue((String)ans.getArgument(0), mapper.constructType((Type)ans.getArgument(1))));
		Mockito.when(jsonStringConverter.decodeOne(Mockito.isA(Publisher.class), Mockito.isA(Type.class))).thenAnswer(
			ans -> Flux.from(ans.getArgument(0, Publisher.class))
				.cast(String.class)
				.reduceWith(() -> new StringBuilder(), (acc, v) -> ((StringBuilder)acc).append(v))
				.map(Object::toString)
				.map(obj -> {
					try {
						return mapper.readValue((String)obj, mapper.constructType((Type)ans.getArgument(1)));
					} 
					catch(JsonProcessingException ex) {
						throw Exceptions.propagate(ex);
					}
				})
		);
		
		return new GenericDataConversionService(List.of(textStringConverter, jsonStringConverter), jwkService, mapper);
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
		GenericX509JWKCertPathValidator certPathValidator = new GenericX509JWKCertPathValidator(configuration, pkixParameters, WORKER_POOL);
		GenericJWKKeyResolver keyResolver = new GenericJWKKeyResolver(configuration);
		GenericJWKURLResolver urlResolver = new GenericJWKURLResolver(configuration, certPathValidator, mapper);
		
		GenericECJWKFactory ecJWKFactory = new GenericECJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericRSAJWKFactory rsaJWKFactory = new GenericRSAJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericOCTJWKFactory symetricJWKFactory = new GenericOCTJWKFactory(configuration, jwkStore, keyResolver, mapper);
		GenericEdECJWKFactory edecJWKFactory = new GenericEdECJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericXECJWKFactory xecJWKFactory = new GenericXECJWKFactory(configuration, jwkStore, keyResolver, mapper, urlResolver, certPathValidator);
		GenericPBES2JWKFactory pbes2JWKFactory = new GenericPBES2JWKFactory(configuration, jwkStore, keyResolver, mapper);
		
		return new GenericJWKService(ecJWKFactory , rsaJWKFactory, symetricJWKFactory, edecJWKFactory, xecJWKFactory, pbes2JWKFactory, jwkStore, urlResolver, mapper);
	}
	
	private static PKIXParameters pkixParameters(String cert, Date date) throws CertificateException, InvalidAlgorithmParameterException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		
		X509Certificate certificate = (X509Certificate)cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(cert)));
		PKIXParameters parameters = new PKIXParameters(Set.of(new TrustAnchor(certificate, null)));
		parameters.setRevocationEnabled(false);
		parameters.setDate(date);
			
		return parameters;
	}
	
	private static GenericJWSService jwsService(ObjectMapper mapper) {
		GenericJWKService jwkService = jwkService();
		GenericDataConversionService dataConversionService = dataConversionService(jwkService, mapper);
		
		GenericJWSService jwsService = new GenericJWSService(mapper, dataConversionService, jwkService);
		jwsService.init();
		
		return jwsService;
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
			if(jwsHeader.getAlgorithm() != null) {
				jg.writeStringField("alg", jwsHeader.getAlgorithm());
			}
			if(jwsHeader.getCritical() != null) {
				if(!jwsHeader.isBase64EncodePayload()) {
					jg.writeBooleanField("b64", jwsHeader.isBase64EncodePayload());
				}
				jg.writeArrayFieldStart("crit");
				for(String p : jwsHeader.getCritical()) {
					jg.writeString(p);
				}
				jg.writeEndArray();
			}
			if(jwsHeader.getKeyId()!= null) {
				jg.writeStringField("kid", jwsHeader.getKeyId());
			}
			jg.writeEndObject();
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static class RFC7515PayloadSerializer extends StdSerializer<Map> {

		private static final long serialVersionUID = 1L;
		
		public RFC7515PayloadSerializer() {
			this(null);
		}

		public RFC7515PayloadSerializer(Class<Map> t) {
			super(t);
		}

		@Override
		public Class<Map> handledType() {
			return Map.class;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void serialize(Map jwt, JsonGenerator jg, SerializerProvider sp) throws IOException {
			jg.writeStartObject();
			jwt.forEach((k,v) -> {
				try {
					jg.writeObjectField(k.toString(),v);
				}
				catch(IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			jg.writeEndObject();
		}
	}
}
