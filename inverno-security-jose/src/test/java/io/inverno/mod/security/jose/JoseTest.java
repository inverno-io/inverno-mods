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
package io.inverno.mod.security.jose;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inverno.core.v1.Application;
import io.inverno.mod.base.converter.MediaTypeConverter;
import io.inverno.mod.base.converter.StringConverter;
import io.inverno.mod.base.reflect.Types;
import io.inverno.mod.base.resource.MediaTypes;
import io.inverno.mod.boot.converter.JacksonStringConverter;
import io.inverno.mod.boot.converter.JsonStringMediaTypeConverter;
import io.inverno.mod.boot.converter.TextStringMediaTypeConverter;
import io.inverno.mod.boot.internal.resource.FileResourceProvider;
import io.inverno.mod.boot.internal.resource.GenericResourceService;
import io.inverno.mod.security.jose.jwa.ECAlgorithm;
import io.inverno.mod.security.jose.jwa.ECCurve;
import io.inverno.mod.security.jose.jwa.EdECAlgorithm;
import io.inverno.mod.security.jose.jwa.NoAlgorithm;
import io.inverno.mod.security.jose.jwa.OCTAlgorithm;
import io.inverno.mod.security.jose.jwa.OKPCurve;
import io.inverno.mod.security.jose.jwa.PBES2Algorithm;
import io.inverno.mod.security.jose.jwa.RSAAlgorithm;
import io.inverno.mod.security.jose.jwa.XECAlgorithm;
import io.inverno.mod.security.jose.jwe.JWE;
import io.inverno.mod.security.jose.jwe.JsonJWE;
import io.inverno.mod.security.jose.jwk.InMemoryJWKStore;
import io.inverno.mod.security.jose.jwk.JWK;
import io.inverno.mod.security.jose.jwk.JWKGenerateException;
import io.inverno.mod.security.jose.jwk.JWKSet;
import io.inverno.mod.security.jose.jwk.ec.ECJWK;
import io.inverno.mod.security.jose.jwk.oct.OCTJWK;
import io.inverno.mod.security.jose.jwk.okp.EdECJWK;
import io.inverno.mod.security.jose.jwk.okp.XECJWK;
import io.inverno.mod.security.jose.jwk.pbes2.PBES2JWK;
import io.inverno.mod.security.jose.jwk.rsa.RSAJWK;
import io.inverno.mod.security.jose.jws.JWS;
import io.inverno.mod.security.jose.jws.JsonJWS;
import io.inverno.mod.security.jose.jwt.JWTClaimsSet;
import java.net.URI;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <p>
 * </p>
 *
 * @author <a href="mailto:jeremy.kuhn@inverno.io">Jeremy Kuhn</a>
 * @since 1.5
 */
public class JoseTest {
	
	static {
		System.setProperty("org.apache.logging.log4j.simplelog.level", "INFO");
		System.setProperty("org.apache.logging.log4j.simplelog.logFile", "system.out");
	}
	
	public static final ObjectMapper MAPPER = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
	
	public static final List<MediaTypeConverter<String>> MEDIA_TYPE_CONVERTERS = List.of(new TextStringMediaTypeConverter(new StringConverter()), new JsonStringMediaTypeConverter(new JacksonStringConverter(MAPPER)));
	
	@Test
	public void testECJWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			ECJWK builtECJWK = jose.jwkService().ec().builder()
				.keyId("ec")
				.curve(ECCurve.P_256.getCurve())
				.xCoordinate("f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU")
				.yCoordinate("x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0")
				.build()
				.block();

			ECJWK readECJWK = jose.jwkService().ec().read(MAPPER.writeValueAsString(builtECJWK)).block();
			Assertions.assertEquals(builtECJWK, readECJWK);

			readECJWK = (ECJWK)Flux.from(jose.jwkService().read(MAPPER.writeValueAsString(builtECJWK))).onErrorResume(e -> Mono.empty()).single().block();
			Assertions.assertEquals(builtECJWK, readECJWK);

			jose.jwkService().ec().generator().generate().block();
			jose.jwkService().ec().generator().curve(ECCurve.P_256.getCurve()).generate().block();
			jose.jwkService().ec().generator().curve(ECCurve.P_384.getCurve()).generate().block();
			jose.jwkService().ec().generator().curve(ECCurve.P_521.getCurve()).generate().block();
			// only works on JDK-15 with -Djdk.sunec.disableNative=false
	//		jose.jwkService().ec().generator().curve(ECCurve.SECP256K1.getCurve()).generate().block();

			jose.jwkService().ec().generator().algorithm(ECAlgorithm.ES256.getAlgorithm()).curve(ECCurve.P_256.getCurve()).generate().block();
			jose.jwkService().ec().generator().algorithm(ECAlgorithm.ES384.getAlgorithm()).curve(ECCurve.P_384.getCurve()).generate().block();
			jose.jwkService().ec().generator().algorithm(ECAlgorithm.ES512.getAlgorithm()).curve(ECCurve.P_521.getCurve()).generate().block();
			// only works on JDK-15 with -Djdk.sunec.disableNative=false
	//		jose.jwkService().ec().generator().algorithm(ECAlgorithm.ES256K.getAlgorithm()).curve(ECCurve.SECP256K1.getCurve()).generate().block();

			Assertions.assertEquals("JWK with curve P-256 does not support algorithm ES512", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().ec().generator().algorithm(ECAlgorithm.ES512.getAlgorithm()).curve(ECCurve.P_256.getCurve()).generate().block()).getMessage());
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testOCTJWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			OCTJWK builtOCTJWK = jose.jwkService().oct().builder()
				.keyValue("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
				.build()
				.block();

			OCTJWK readOCTJWK = jose.jwkService().oct().read(MAPPER.writeValueAsString(builtOCTJWK)).block();
			Assertions.assertEquals(builtOCTJWK, readOCTJWK);

			readOCTJWK = (OCTJWK)Flux.from(jose.jwkService().read(MAPPER.writeValueAsString(builtOCTJWK))).onErrorResume(e -> Mono.empty()).single().block();
			Assertions.assertEquals(builtOCTJWK, readOCTJWK);

			jose.jwkService().oct().generator().generate();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.HS256.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.HS384.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.HS512.getAlgorithm()).generate().block();

			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A128KW.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A192KW.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A256KW.getAlgorithm()).generate().block();

			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A128GCMKW.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A192GCMKW.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm()).generate().block();

			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A192CBC_HS384.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A256CBC_HS512.getAlgorithm()).generate().block();

			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A128GCM.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A192GCM.getAlgorithm()).generate().block();
			jose.jwkService().oct().generator().algorithm(OCTAlgorithm.A256GCM.getAlgorithm()).generate().block();

			Assertions.assertEquals("Key size 20 is inconsistent with algorithm HS256 which requires 32", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().oct().generator().algorithm(OCTAlgorithm.HS256.getAlgorithm()).keySize(20).generate().block()).getMessage());
			Assertions.assertEquals("Key size must be at least 16", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().oct().generator().keySize(2).generate().block()).getMessage());
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testEdECJWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			EdECJWK builtEdECJWK = jose.jwkService().edec().builder()
				.curve(OKPCurve.ED25519.getCurve())
				.publicKey("11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo")
				.privateKey("nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A")
				.build()
				.block();

			EdECJWK readEdECJWK = jose.jwkService().edec().read(MAPPER.writeValueAsString(builtEdECJWK)).block();
			Assertions.assertEquals(builtEdECJWK, readEdECJWK);

			readEdECJWK = (EdECJWK)Flux.from(jose.jwkService().read(MAPPER.writeValueAsString(builtEdECJWK))).onErrorResume(e -> Mono.empty()).single().block();
			Assertions.assertEquals(builtEdECJWK, readEdECJWK);

			jose.jwkService().edec().generator().generate().block();
			jose.jwkService().edec().generator().algorithm(EdECAlgorithm.EDDSA_ED25519.getAlgorithm()).generate().block();
			jose.jwkService().edec().generator().algorithm(EdECAlgorithm.EDDSA_ED448.getAlgorithm()).generate().block();

			jose.jwkService().edec().generator().curve(OKPCurve.ED25519.getCurve()).generate().block();
			jose.jwkService().edec().generator().curve(OKPCurve.ED448.getCurve()).generate().block();

			Assertions.assertEquals("Unsupported algorithm: EdDSA + X25519", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().edec().generator().algorithm(EdECAlgorithm.EDDSA_ED448.getAlgorithm()).curve(OKPCurve.X25519.getCurve()).generate().block()).getMessage());
			Assertions.assertEquals("Unsupported OKP curve: X25519", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().edec().generator().curve(OKPCurve.X25519.getCurve()).generate().block()).getMessage());
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testXECJWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			XECJWK builtXECJWK = jose.jwkService().xec().builder()
				.curve("X25519")
				.publicKey("hSDwCYkwp1R0i33ctD73Wg2_Og0mOBr066SpjqqbTmo")
				.privateKey("dwdtCnMYpX08FsFyUbJmRd9ML4frwJkqsXf7pR25LCo")
				.build()
				.block();

			XECJWK readXECJWK = jose.jwkService().xec().read(MAPPER.writeValueAsString(builtXECJWK)).block();
			Assertions.assertEquals(builtXECJWK, readXECJWK);

			readXECJWK = (XECJWK)Flux.from(jose.jwkService().read(MAPPER.writeValueAsString(builtXECJWK))).onErrorResume(e -> Mono.empty()).single().block();
			Assertions.assertEquals(builtXECJWK, readXECJWK);

			jose.jwkService().xec().generator().generate().block();
			jose.jwkService().xec().generator().algorithm(XECAlgorithm.ECDH_ES.getAlgorithm()).generate().block();
			jose.jwkService().xec().generator().algorithm(XECAlgorithm.ECDH_ES_A128KW.getAlgorithm()).generate().block();
			jose.jwkService().xec().generator().algorithm(XECAlgorithm.ECDH_ES_A192KW.getAlgorithm()).generate().block();
			jose.jwkService().xec().generator().algorithm(XECAlgorithm.ECDH_ES_A256KW.getAlgorithm()).generate().block();

			jose.jwkService().xec().generator().curve(OKPCurve.X25519.getCurve()).generate().block();
			jose.jwkService().xec().generator().curve(OKPCurve.X448.getCurve()).generate().block();

			Assertions.assertEquals("Unsupported OKP curve: Ed25519", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().xec().generator().curve(OKPCurve.ED25519.getCurve()).generate().block()).getMessage());
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testPBES2JWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			PBES2JWK builtPBES2JWK = jose.jwkService().pbes2().builder()
				.password(Base64.getUrlEncoder().withoutPadding().encodeToString("Thus from my lips, by yours, my sin is purged.".getBytes()))
				.build()
				.block();

			PBES2JWK readPBES2JWK = jose.jwkService().pbes2().read(MAPPER.writeValueAsString(builtPBES2JWK)).block();
			Assertions.assertEquals(builtPBES2JWK, readPBES2JWK);

			readPBES2JWK = (PBES2JWK)Flux.from(jose.jwkService().read(MAPPER.writeValueAsString(builtPBES2JWK))).onErrorResume(e -> Mono.empty()).single().block();
			Assertions.assertEquals(builtPBES2JWK, readPBES2JWK);

			jose.jwkService().pbes2().generator().generate().block();
			jose.jwkService().pbes2().generator().algorithm(PBES2Algorithm.PBES2_HS256_A128KW.getAlgorithm()).generate().block();
			jose.jwkService().pbes2().generator().algorithm(PBES2Algorithm.PBES2_HS384_A192KW.getAlgorithm()).generate().block();
			jose.jwkService().pbes2().generator().algorithm(PBES2Algorithm.PBES2_HS512_A256KW.getAlgorithm()).generate().block();

			Assertions.assertEquals("Password length must be at least 32", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().pbes2().generator().algorithm(PBES2Algorithm.PBES2_HS512_A256KW.getAlgorithm()).length(20).generate().block()).getMessage());
			Assertions.assertEquals("Password length must no longer than 128", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().pbes2().generator().length(200).generate().block()).getMessage());
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testRSAJWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			RSAJWK builtRSAJWK = jose.jwkService().rsa().builder()
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

			RSAJWK readRSAJWK = jose.jwkService().rsa().read(MAPPER.writeValueAsString(builtRSAJWK)).block();
			Assertions.assertEquals(builtRSAJWK, readRSAJWK);

			readRSAJWK = (RSAJWK)Flux.from(jose.jwkService().read(MAPPER.writeValueAsString(builtRSAJWK))).onErrorResume(e -> Mono.empty()).single().block();
			Assertions.assertEquals(builtRSAJWK, readRSAJWK);

			jose.jwkService().rsa().generator().generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.PS256.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.PS384.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.PS512.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RS1.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RS256.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RS384.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RS512.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RSA1_5.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RSA_OAEP.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RSA_OAEP_256.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RSA_OAEP_384.getAlgorithm()).generate().block();
			jose.jwkService().rsa().generator().algorithm(RSAAlgorithm.RSA_OAEP_512.getAlgorithm()).generate().block();

			Assertions.assertEquals("Key size must be at least 2048", Assertions.assertThrows(JWKGenerateException.class, () -> jose.jwkService().rsa().generator().keySize(1024).generate().block()).getMessage());
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWS() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Let's get this party started";

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.generate()
				.cache();

			jwk_oct.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWS<String> builtJWS = jose.jwsService().builder(String.class)
				.header(header -> header
					.algorithm(OCTAlgorithm.HS256.getAlgorithm())
					.keyId("oct")
					.contentType(MediaTypes.TEXT_PLAIN)
				)
				.payload(payload)
				.build()
				.block();

			JWS<String> readJWS = jose.jwsService().reader(String.class)
				.read(builtJWS.toCompact())
				.block();

			Assertions.assertEquals(builtJWS, readJWS);
		}
		finally {
			jose.stop();
		}
	}
		
	@Test
	public void testJWE() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "This is the way";

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.generate()
				.cache();

			jwk_oct.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			Mono<? extends RSAJWK> jwk_rsa= jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.RSA1_5.toString())
				.generate()
				.cache();

			JWE<String> builtJWE = jose.jweService().builder(String.class, jwk_rsa)
				.header(header -> header
					.algorithm(RSAAlgorithm.RSA1_5.toString())
					.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			JWE<String> readJWE = jose.jweService().reader(String.class, jwk_rsa)
				.read(builtJWE.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			Assertions.assertEquals(builtJWE, readJWE);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWSJWT() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			JWTClaimsSet jwtPayload = JWTClaimsSet.of("joe", 1300819380).addCustomClaim("http://example.com/is_root", true).build();

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.generate()
				.cache();

			jwk_oct.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWS<JWTClaimsSet> builtJWSJWT = jose.jwtService().jwsBuilder()
				.header(header -> header
					.algorithm(OCTAlgorithm.HS256.getAlgorithm())
					.type("JWT")
					.keyId("oct")
				)
				.payload(jwtPayload)
				.build()
				.block();

			JWS<JWTClaimsSet> readJWSJWT = jose.jwtService().jwsReader()
				.read(builtJWSJWT.toCompact())
				.block();

			Assertions.assertEquals(builtJWSJWT, readJWSJWT);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWEJWT() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			JWTClaimsSet jwtPayload = JWTClaimsSet.of("joe", 1300819380).addCustomClaim("http://example.com/is_root", true).build();

			Mono<? extends RSAJWK> jwk_rsa = jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.RSA1_5.toString())
				.generate()
				.cache();

			JWE<JWTClaimsSet> builtJWEJWT = jose.jwtService().jweBuilder(jwk_rsa)
				.header(header -> header
					.algorithm(RSAAlgorithm.RSA1_5.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
					.type("JWT")
				)
				.payload(jwtPayload)
				.build()
				.block();

			JWE<JWTClaimsSet> readJWEJWT = jose.jwtService().jweReader(jwk_rsa)
				.read(builtJWEJWT.toCompact())
				.block();

			Assertions.assertEquals(builtJWEJWT, readJWEJWT);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWSJWS() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Let's get this party started";

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.generate()
				.cache();

			Mono<? extends ECJWK> jwk_ec = jose.jwkService().ec().generator()
				.keyId("ec")
				.curve(ECCurve.P_256.getCurve())
				.algorithm(ECAlgorithm.ES256.getAlgorithm())
				.generate()
				.cache();

			jwk_ec.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWS<JWS<String>> builtJWSJWS = jose.jwsService().builder(String.class, jwk_oct)
				.header(header -> header
					.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.flatMap(jws -> jose.jwsService().<JWS<String>>builder(Types.type(JWS.class).type(String.class).and().build())
						.header(header -> header
							.keyId("ec")
							.algorithm(ECAlgorithm.ES256.getAlgorithm())
							.contentType(MediaTypes.APPLICATION_JOSE)
						)
						.payload(jws)
						.build()
				)
				.block();

			JWS<JWS<String>> readJWSJWS = jose.jwsService().<JWS<String>>reader(Types.type(JWS.class).type(String.class).and().build())
				.read(
					builtJWSJWS.toCompact(), 
					raw -> jose.jwsService().reader(String.class, jwk_oct)
						.read(raw, MediaTypes.TEXT_PLAIN)
				)
				.block();

			Assertions.assertEquals(builtJWSJWS, readJWSJWS);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWSJsonJWS() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Let's get this party started";

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.generate()
				.cache();

			Mono<? extends RSAJWK> jwk_rsa = jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.PS512.toString())
				.generate()
				.cache();

			Mono<? extends ECJWK> jwk_ec = jose.jwkService().ec().generator()
				.keyId("ec")
				.curve(ECCurve.P_256.getCurve())
				.algorithm(ECAlgorithm.ES256.getAlgorithm())
				.generate()
				.cache();

			jwk_ec.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWS<JsonJWS<String, JsonJWS.BuiltSignature<String>>> builtJWSJsonJWS = jose.jwsService().jsonBuilder(String.class)
				.headers(null, header -> header.contentType(MediaTypes.TEXT_PLAIN))
				.signature(
					header -> header
						.algorithm(OCTAlgorithm.HS256.getAlgorithm()), 
					header -> header
						.keyId("oct"),
					jwk_oct
				)
				.signature(
					header -> header
						.algorithm(RSAAlgorithm.PS512.toString()), 
					header -> header
						.keyId("rsa"),
					jwk_rsa
				)
				.payload(payload)
				.build()
				.flatMap(jsonJWS -> jose.jwsService().<JsonJWS<String, JsonJWS.BuiltSignature<String>>>builder(Types.type(JsonJWS.class).type(String.class).and().type(JsonJWS.BuiltSignature.class).type(String.class).and().and().build())
					.header(header -> header
						.keyId("ec")
						.algorithm(ECAlgorithm.ES256.getAlgorithm())
						.contentType(MediaTypes.APPLICATION_JOSE_JSON)
					)
					.payload(jsonJWS)
					.build()
				)
				.block();

			JWS<JsonJWS<String, JsonJWS.ReadSignature<String>>> readJWSJsonJWS = jose.jwsService().<JsonJWS<String, JsonJWS.ReadSignature<String>>>reader(Types.type(JsonJWS.class).type(String.class).and().type(JsonJWS.ReadSignature.class).type(String.class).and().and().build())
				.read(builtJWSJsonJWS.toCompact())
				.block();

			Assertions.assertEquals(builtJWSJsonJWS.getHeader(), readJWSJsonJWS.getHeader());
			Assertions.assertEquals(builtJWSJsonJWS.getPayload().getPayload(), readJWSJsonJWS.getPayload().getPayload());

			Assertions.assertEquals(2, builtJWSJsonJWS.getPayload().getSignatures().size());
			Assertions.assertEquals(2, readJWSJsonJWS.getPayload().getSignatures().size());

			JWS<String> builtSigJWS = builtJWSJsonJWS.getPayload().getSignatures().get(0).getJWS();
			JWS<String> readSigJWS = readJWSJsonJWS.getPayload().getSignatures().get(0).readJWS(jwk_oct).block();
			Assertions.assertEquals(builtSigJWS, readSigJWS);

			builtSigJWS = builtJWSJsonJWS.getPayload().getSignatures().get(1).getJWS();
			readSigJWS = readJWSJsonJWS.getPayload().getSignatures().get(1).readJWS(jwk_rsa).block();
			Assertions.assertEquals(builtSigJWS, readSigJWS);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWSJWE() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Let's get this party started";

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				.generate()
				.cache();

			jwk_oct.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			Mono<? extends ECJWK> jwk_ec = jose.jwkService().ec().generator()
				.keyId("ec")
				.curve(ECCurve.P_256.getCurve())
				.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
				.generate()
				.cache();

			JWS<JWE<String>> builtJWSJWE = jose.jweService().builder(String.class, jwk_ec)
				.header(header -> header
					.algorithm(ECAlgorithm.ECDH_ES.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128GCM.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.flatMap(jwe -> jose.jwsService().<JWE<String>>builder(Types.type(JWE.class).type(String.class).and().build())
						.header(header -> header
							.keyId("oct")
							.algorithm(OCTAlgorithm.HS256.getAlgorithm())
							.contentType(MediaTypes.APPLICATION_JOSE)
						)
						.payload(jwe)
						.build()
				)
				.block();

			JWS<JWE<String>> readJWSJWE = jose.jwsService().<JWE<String>>reader(Types.type(JWE.class).type(String.class).and().build())
				.read(
					builtJWSJWE.toCompact(), 
					raw -> jose.jweService().reader(String.class, jwk_ec)
						.read(raw, MediaTypes.TEXT_PLAIN)
				)
				.block();

			Assertions.assertEquals(builtJWSJWE, readJWSJWE);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWSJWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			Mono<? extends RSAJWK> jwk_rsa = jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.RS256.toString())
				.generate()
				.cache();

			Mono<? extends EdECJWK> jwk_edec = jose.jwkService().edec().generator()
				.keyId("edec")
				.publicKeyUse(JWK.USE_SIG)
				.algorithm(EdECAlgorithm.EDDSA_ED25519.getAlgorithm())
				.generate()
				.cache();

			jwk_edec.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWS<RSAJWK> builtJWSJWK = jose.jwsService().builder(RSAJWK.class)
				.header(header -> header
					.keyId("edec")
					.algorithm(EdECAlgorithm.EDDSA_ED25519.getAlgorithm())
					.contentType(MediaTypes.APPLICATION_JWK_JSON)
				)
				.payload(jwk_rsa.block())
				.build()
				.block();

			JWS<RSAJWK> readJWSJWK = jose.jwsService().reader(RSAJWK.class)
				.read(builtJWSJWK.toCompact())
				.block();

			Assertions.assertEquals(builtJWSJWK, readJWSJWK);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWSJWKSet() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			Mono<? extends RSAJWK> jwk_rsa = jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.PS512.toString())
				.generate()
				.cache();

			JWKSet jwkSet = new JWKSet(
				jose.jwkService().oct().generator()
					.keyId("oct")
					.algorithm(OCTAlgorithm.A192CBC_HS384.getAlgorithm())
					.generate()
					.block(),
				jose.jwkService().pbes2().generator()
					.keyId("pbes2")
					.algorithm(PBES2Algorithm.PBES2_HS384_A192KW.getAlgorithm())
					.generate()
					.block(),
				jose.jwkService().xec().generator()
					.keyId("xec")
					.algorithm(XECAlgorithm.ECDH_ES.getAlgorithm())
					.generate()
					.block()
					.toPublicJWK()
			);

			JWS<JWKSet> builtJWSJWKSet = jose.jwsService().builder(JWKSet.class, jwk_rsa)
				.header(header -> header
					.keyId("edec")
					.algorithm(RSAAlgorithm.PS512.getAlgorithm())
					.contentType(MediaTypes.APPLICATION_JWK_SET_JSON)
				)
				.payload(jwkSet)
				.build()
				.block();

			JWS<JWKSet> readJWSJWKSet = jose.jwsService().reader(JWKSet.class, jwk_rsa)
				.read(builtJWSJWKSet.toCompact())
				.block();

			Assertions.assertEquals(builtJWSJWKSet, readJWSJWKSet);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWEJWE() throws Exception {
//		A256GCMKW + A192GCM
//		RSA_OAEP_384 + A192CBC_HS384
		
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Just cos' you got the power that don't mean you got the right";

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
				.generate()
				.cache();

			Mono<? extends RSAJWK> jwk_rsa = jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.RSA_OAEP_384.getAlgorithm())
				.generate()
				.cache();

			JWE<JWE<String>> builtJWEJWE = jose.jweService().builder(String.class, jwk_oct)
				.header(header -> header
					.algorithm(OCTAlgorithm.A256GCMKW.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A192GCM.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.flatMap(jwe -> jose.jweService().<JWE<String>>builder(Types.type(JWE.class).type(String.class).and().build(), jwk_rsa)
					.header(header -> header
						.algorithm(RSAAlgorithm.RSA_OAEP_384.getAlgorithm())
						.encryptionAlgorithm(OCTAlgorithm.A192CBC_HS384.getAlgorithm())
					)
					.payload(jwe)
					.build(MediaTypes.APPLICATION_JOSE)
				)
				.block();

			JWE<JWE<String>> readJWEJWE = jose.jweService().<JWE<String>>reader(Types.type(JWE.class).type(String.class).and().build(), jwk_rsa)
				.read(
					builtJWEJWE.toCompact(),
					raw -> jose.jweService().reader(String.class, jwk_oct)
						.read(raw, MediaTypes.TEXT_PLAIN)
				)
				.block();

			Assertions.assertEquals(builtJWEJWE, readJWEJWE);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWEJsonJWE() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Let's get this party started";

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.A128GCMKW.getAlgorithm())
				.generate()
				.cache();

			jwk_oct.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			Mono<? extends RSAJWK> jwk_rsa = jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.RSA1_5.toString())
				.generate()
				.cache();

			jwk_rsa.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			Mono<? extends XECJWK> jwk_xec = jose.jwkService().xec().generator()
				.keyId("xec")
				.curve(OKPCurve.X448.getCurve())
				.algorithm(XECAlgorithm.ECDH_ES_A192KW.getAlgorithm())
				.generate()
				.cache();

			jwk_xec.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWE<JsonJWE<String, JsonJWE.BuiltRecipient<String>>> builtJWEJsonJWE = jose.jweService().jsonBuilder(String.class)
				.headers(
					header -> header
						.encryptionAlgorithm(OCTAlgorithm.A256CBC_HS512.getAlgorithm()), 
					header -> header
						.contentType(MediaTypes.TEXT_PLAIN)
				)
				.recipient(header -> header
					.keyId("oct")
					.algorithm(OCTAlgorithm.A128GCMKW.getAlgorithm())
				)
				.recipient(header -> header
					.keyId("rsa")
					.algorithm(RSAAlgorithm.RSA1_5.getAlgorithm())
				)
				.payload(payload)
				.build()
				.flatMap(jsonJWE -> jose.jweService().<JsonJWE<String, JsonJWE.BuiltRecipient<String>>>builder(Types.type(JsonJWE.class).type(String.class).and().type(JsonJWE.BuiltRecipient.class).type(String.class).and().and().build())
					.header(header -> header
						.keyId("xec")
						.algorithm(XECAlgorithm.ECDH_ES_A192KW.getAlgorithm())
						.encryptionAlgorithm(OCTAlgorithm.A256GCM.getAlgorithm())
						.contentType(MediaTypes.APPLICATION_JOSE_JSON)
					)
					.payload(jsonJWE)
					.build()
				)
				.block();

			JWE<JsonJWE<String, JsonJWE.ReadRecipient<String>>> readJWEJsonJWE = jose.jweService().<JsonJWE<String, JsonJWE.ReadRecipient<String>>>reader(Types.type(JsonJWE.class).type(String.class).and().type(JsonJWE.ReadRecipient.class).type(String.class).and().and().build())
				.read(builtJWEJsonJWE.toCompact())
				.block();

			Assertions.assertEquals(builtJWEJsonJWE.getHeader(), readJWEJsonJWE.getHeader());

			Assertions.assertEquals(builtJWEJsonJWE.getPayload().getProtectedHeader(), readJWEJsonJWE.getPayload().getProtectedHeader());
			Assertions.assertEquals(builtJWEJsonJWE.getPayload().getUnprotectedHeader(), readJWEJsonJWE.getPayload().getUnprotectedHeader());

			Assertions.assertEquals(2, builtJWEJsonJWE.getPayload().getRecipients().size());
			Assertions.assertEquals(2, readJWEJsonJWE.getPayload().getRecipients().size());

			JWE<String> builtRecJWE = builtJWEJsonJWE.getPayload().getRecipients().get(0).getJWE();
			JWE<String> readRecJWE = readJWEJsonJWE.getPayload().getRecipients().get(0).readJWE().block();
			Assertions.assertEquals(builtRecJWE, readRecJWE);

			builtRecJWE = builtJWEJsonJWE.getPayload().getRecipients().get(1).getJWE();
			readRecJWE = readJWEJsonJWE.getPayload().getRecipients().get(1).readJWE().block();
			Assertions.assertEquals(builtRecJWE, readRecJWE);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWEJWS() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Just cos' you got the power that don't mean you got the right";

			Mono<? extends ECJWK> jwk_ec = jose.jwkService().ec().generator()
				.keyId("ec")
				.algorithm(ECAlgorithm.ECDH_ES_A256KW.getAlgorithm())
				.generate()
				.cache();

			jwk_ec.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.HS512.getAlgorithm())
				.generate()
				.cache();

			jwk_oct.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWE<JWS<String>> builtJWEJWS = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("oct")
					.algorithm(OCTAlgorithm.HS512.getAlgorithm())
					.contentType(MediaTypes.TEXT_PLAIN)
				)
				.payload(payload)
				.build()
				.flatMap(jws -> jose.jweService().<JWS<String>>builder(Types.type(JWS.class).type(String.class).and().build())
					.header(header -> header
						.keyId("ec")
						.algorithm(ECAlgorithm.ECDH_ES_A256KW.getAlgorithm())
						.encryptionAlgorithm(OCTAlgorithm.A192CBC_HS384.getAlgorithm())
						.contentType(MediaTypes.APPLICATION_JOSE)
					)
					.payload(jws)
					.build()
				)
				.block();

			JWE<JWS<String>> readJWEJWS = jose.jweService().<JWS<String>>reader(Types.type(JWS.class).type(String.class).and().build())
				.read(
					builtJWEJWS.toCompact(),
					raw -> jose.jwsService().reader(String.class)
						.read(raw)
				)
				.block();

			Assertions.assertEquals(builtJWEJWS, readJWEJWS);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWEJWK() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			Mono<? extends RSAJWK> jwk_rsa = jose.jwkService().rsa().generator()
				.keyId("rsa")
				.algorithm(RSAAlgorithm.RS256.toString())
				.generate()
				.cache();

			Mono<? extends PBES2JWK> jwk_pbes2 = jose.jwkService().pbes2().builder()
				.keyId("pbes2")
				.password(Base64.getUrlEncoder().withoutPadding().encodeToString("Lorem ipsum dolor sit amet, consectetur adipiscing elit".getBytes()))
				.algorithm(PBES2Algorithm.PBES2_HS256_A128KW.getAlgorithm())
				.build()
				.cache();

			jwk_pbes2.doOnNext(JWK::trust).flatMap(jose.jwkService().store()::set).block();

			JWE<RSAJWK> builtJWEJWK = jose.jweService().builder(RSAJWK.class)
				.header(header -> header
					.keyId("pbes2")
					.algorithm(PBES2Algorithm.PBES2_HS256_A128KW.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128CBC_HS256.getAlgorithm())
					.contentType(MediaTypes.APPLICATION_JWK_JSON)
				)
				.payload(jwk_rsa.block())
				.build()
				.block();

			JWE<RSAJWK> readJWEJWK = jose.jweService().reader(RSAJWK.class)
				.read(builtJWEJWK.toCompact())
				.block();

			Assertions.assertEquals(builtJWEJWK, readJWEJWK);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWEJWKSet() throws Exception {
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setJwkStore(new InMemoryJWKStore()));
		try {
			Mono<? extends OCTJWK> jwk_oct = jose.jwkService().oct().generator()
				.keyId("oct")
				.algorithm(OCTAlgorithm.A128GCM.getAlgorithm())
				.generate()
				.cache();

			JWKSet jwkSet = new JWKSet(
				jose.jwkService().oct().generator()
					.keyId("oct")
					.algorithm(OCTAlgorithm.A192CBC_HS384.getAlgorithm())
					.generate()
					.block(),
				jose.jwkService().pbes2().generator()
					.keyId("pbes2")
					.algorithm(PBES2Algorithm.PBES2_HS384_A192KW.getAlgorithm())
					.generate()
					.block(),
				jose.jwkService().xec().generator()
					.keyId("xec")
					.algorithm(XECAlgorithm.ECDH_ES.getAlgorithm())
					.generate()
					.block()
					.toPublicJWK()
			);

			JWE<JWKSet> builtJWEJWKSet = jose.jweService().builder(JWKSet.class, jwk_oct)
				.header(header -> header
					.algorithm(NoAlgorithm.DIR.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128GCM.getAlgorithm())
				)
				.payload(jwkSet)
				.build(MediaTypes.APPLICATION_JWK_SET_JSON)
				.block();

			JWE<JWKSet> readJWSJWKSet = jose.jweService().reader(JWKSet.class, jwk_oct)
				.read(builtJWEJWKSet.toCompact(), MediaTypes.APPLICATION_JWK_SET_JSON)
				.block();

			Assertions.assertEquals(builtJWEJWKSet, readJWSJWKSet);
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWKKeyStore() throws Exception {
		JOSEConfiguration configuration = new JOSEConfiguration() {
			@Override
			public URI key_store() {
				return Path.of("src/test/resources/jks/keystore.jks").toUri();
			}

			@Override
			public String key_store_password() {
				return "password";
			}
		};
		
		GenericResourceService resourceService = new GenericResourceService();
		resourceService.setProviders(List.of(new FileResourceProvider()));
		
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setConfiguration(configuration).setResourceService(resourceService).setJwkStore(new InMemoryJWKStore()));
		try {
			ECJWK jwk_ec = jose.jwkService().ec().read(Map.of("kid", "ec")).block();
			Assertions.assertEquals("ec", jwk_ec.getKeyId());
			Assertions.assertEquals("P-256", jwk_ec.getCurve());
			Assertions.assertEquals("Y4XGmjbqCMzBEvqd1emId89o9BY4m8v0qla90wdOgFU", jwk_ec.getXCoordinate());
			Assertions.assertEquals("E1YlPDekf_PDoHgp3Tcc4kIHVRNZZNwj5nJbg0oORAs", jwk_ec.getYCoordinate());
			Assertions.assertEquals("TAGDgB_CWOKKSrIBeglfHHvq0h92OQvE9U-hsslc74c", jwk_ec.getEccPrivateKey());

			OCTJWK jwk_oct = jose.jwkService().oct().read(Map.of("kid", "oct")).block();
			Assertions.assertEquals("oct", jwk_oct.getKeyId());
			Assertions.assertEquals("UyYAqYJ_C7JQWTMbIbvt8g", jwk_oct.getKeyValue());

			EdECJWK jwk_edec = jose.jwkService().edec().read(Map.of("kid", "edec")).block();
			Assertions.assertEquals("edec", jwk_edec.getKeyId());
			Assertions.assertEquals("Ed25519", jwk_edec.getCurve());
			Assertions.assertEquals("Uguw_mZ4K9vvkpHxu4Wcm7tnGJg8RANt1YmkGm6v8v4", jwk_edec.getPublicKey());
			Assertions.assertEquals("xpHPy9ha5ARGSL4Ob_9xhI2ko0r8zdozQJ1tCHfTOS0", jwk_edec.getPrivateKey());

			XECJWK jwk_xec = jose.jwkService().xec().read(Map.of("kid", "xec")).block();
			Assertions.assertEquals("xec", jwk_xec.getKeyId());
			Assertions.assertEquals("X448", jwk_xec.getCurve());
			Assertions.assertEquals("5ysteKhT2dTy5wU9Sjb2KIS80djtz5XybrGGNbZ6y0N-hbF7Pbs-cc_ce-udmP07UUFWpKLz0eI", jwk_xec.getPublicKey());
			Assertions.assertEquals("oLNkPvCsu3St4uJcRI5XjyNOv2qhV-wAuOlr81s-RASNUQTXMzKSOEslbvvsg6_5GZPus2YJEvg", jwk_xec.getPrivateKey());

			RSAJWK jwk_rsa = jose.jwkService().rsa().read(Map.of("kid", "rsa")).block();
			Assertions.assertEquals("rsa", jwk_rsa.getKeyId());
			Assertions.assertEquals("yiuP-607_c5vnBM5dXwtZtdE8Da_gxGN4DxLrstoXD0fUZ1J6HbeTY0_-zcyR61d06irprt1RJ9nlbTJCs1P_ZffLHYOAVhEjzp0pyPjUCztxXw0MdMpWblA4U0aIS6BN2cdaKojBQY7yZM9NY5G0OWovBWOhvCHLmznxmK54wCdpq2QIpXLT_DTC68brlvCVoij1IdwydQmdioScN47VySUAaqlim3jv4GjSkDZd6ecnwpzbYpCMC7qhR2iFE-PgThgWlZ_iil9Su4qsWmUWjPSkfSgOAO73bUSg-PQAa3Np_AlF4fCihXg2V6FJxTd77KcZ13IQrXs2AiZ7Anm-Q", jwk_rsa.getModulus());
			Assertions.assertEquals("AQAB", jwk_rsa.getPublicExponent());
			Assertions.assertEquals("rvyXqnpeI6fL2OaW5EawMYSASf7JMtQ93emyhD_RO404D3c54nkIn010JKe44GuBe9NRh4ZX0Sa8DMsm_C-LXe9XHu-r3aQd627oS0b32Iya3UVNFBc2gk-jhZ8rz66l72NUBCTHHPExTJ8h6roUN3mg2_M4ozmLeDaRQphvVrfE_xUGoITJt03C0u1ACF_ELoEUhx0DzMFFe6zeHcgYD4okljqRi_NT8Tvl8rQ2PxM5CoEqe9AV35VdJ1mARw4YQ9MiFmBXOrJ-FG2SflVLy5zCnb2hJ36tZFFjDJpKTd2nVXFXFNXa7tIbPx4nqwBiwBtEcNE1OerD06qImbQxdQ", jwk_rsa.getPrivateExponent());
			Assertions.assertEquals("-xO90vtnU0K-DSOeq0YBISnqmebwI8kIG7ZQy_9m7_XBw3v0ootY7K-aMM4ayhqmyYP4BLvNx9LmxV54owWkf_JczhPdJby75JDEB-Ndj8Tl7lwxKhf1xrpNZRS2ScWvyT7FunkP1XPuZ1txd5DMcwgz2GRX7M_wzd0VxMC7GDM", jwk_rsa.getFirstPrimeFactor());
			Assertions.assertEquals("ziJWRWTCcddcT2aoeAAM6cc6ITt7pBiW2cZJhgKrvaIZ_NfC21H9pZq7134DBptVrXR7ju0doXrlIrTJndJ6BDw7XDiElLcKVYm6Zh8IoMh6rETj1pJuyyY8Uu0Ue2zM8SPNZYqFcrfM-sUz2FCbPcbMZC6u6Nej3_jpXPt8CCM", jwk_rsa.getSecondPrimeFactor());
			Assertions.assertEquals("7TJ3UM0FjlktZwhRrAkUpBoPcpoEICqZqGSS7EY7H1OTXHTMZosy1VgIWTc9g9Wt4A72zrIF9FPAhJF9crWv9Ngo6N9HO5GUCJjjOXiJOXuQpaEfKV89aCM1Xts0Y3mJWpwc_M_GL6e4gJiZF5YKMFp9cF9L2YORQUsud5SErJU", jwk_rsa.getFirstFactorExponent());
			Assertions.assertEquals("FVQOMx3q1Jag8YJAujHfJC0-AYRDFcaJjTzsDJaoKHXmVHgKrGC-au0otHJQY9WcttbPlglIwJWTsdSc69yETX3h8nfSmAScaB9ZDwn0_ZXgw4RZQrFiD1kEctwe-2pVhjnbHb-IcUc9SVEwPRiSB4FTUqZV6LevOBRsKPfIbJs", jwk_rsa.getSecondFactorExponent());
			Assertions.assertEquals("FIFvFRw08HO8c0J9SPiO0eiiNlvjyUbbzMbNPDE0jMazqqcx7X_c5go9w8P_Z0YWMbg3ai6YGroytJXr14jjnQUDVw1ThC7bqOQ6ZjaMZzDEJiZ8rHa0EofS2IrQqfPx9rM9Y7X-kBVSYfZAwIMNqlq53Mq_1_UwzkTkBvpuA2I", jwk_rsa.getFirstCoefficient());

			RSAJWK jwk_rsa_pss = jose.jwkService().rsa().read(Map.of("kid", "rsa_pss")).block();
			Assertions.assertEquals("rsa_pss", jwk_rsa_pss.getKeyId());
			Assertions.assertEquals("9NC1kMNercoyoSwx5wzTVlUm2XadzUqAKoz37RB2mpzsGVoqmyixyrLQSLm9nzlEqpuDA5sKxELV1A0C0rCVsOv69lYmMCep11GvwmjVPstGk3oXBn9fh6y4IDf5JrJIfUGlYcIh4BuDjF_tMmmcE0ZlkUq7EqS99PF0IQA7KWsDAFvpa48TjKCxrR6_tdny-WtQYyvBd8f0LTwGcPfZG-aeEVzFd_gPtlxkPGQ1MTCyo3h38jsRJn5Rhg0yEtOeWGf9DEYBHNny2TfgqZlizHHzu2lTDZJfMGgT9p_G2Q59GY3kZZX2LazwDsRXvqdR1gp_5HwOEa1KlmuKPtArQQ", jwk_rsa_pss.getModulus());
			Assertions.assertEquals("AQAB", jwk_rsa_pss.getPublicExponent());
			Assertions.assertEquals("s7GeE6vTiuynTPYLivQ3C19lLKmMGmtct97Q_AjhhYs5IUK1kz3DgmzNxRPQw1ZduHx9JeBffr8wBH2oXM2QklQj2TxSu3XhjFJBGAmqvHSoUQeEbxh_Hi8A12U-U9D4tDfDFIZSJxUK8bZXfHFYRi2dz49y0LRrWacA_lgVFMgv1rvVYJNtlIIrv-QHieXJvDlN3jKdgo3B4C8J2KrXkEB89rtMUGvq89qeoQH0YfRv-6z2cGVrX6pHCuD3orrKmAdfHk5OrtVSVjBbtSL1TzN4QxzNJJEXMjAi-ZjdgOJ5wyKBD6zKFhojwh_3maa-BdeLiTAb523lZNyuTUugAQ", jwk_rsa_pss.getPrivateExponent());
			Assertions.assertEquals("_g-xJTX1dFO6tetzyNkhUvEXJux5hJrerGrM8fs0csKOHORnVtmCcvaZB1Xh-Ae3p42O5OZ9THjhmYxV20Yjxn_YzMJVSQofC3-e_NNVl9OujDNdVzps4FX0vULhG3SdQpCM6pQo4vMxIqo8TuCTtkB6U8ynqM7hDMFaBsIsAqE", jwk_rsa_pss.getFirstPrimeFactor());
			Assertions.assertEquals("9q70hqp2bDfrpPqXTEcLMz6Cg653VmwzCcg4GwSeOyl95lYrW5GD_AgnDxj_uDlOX5hgblF1KKiO7nF6W-5vfCAot26yG6uCJVHVl00uqbWpb9dLHsVngigcHmjWAUxYZMgedSUC17GrY_-RToK4gA6HsU33Bibxzx_GhfIYBKE", jwk_rsa_pss.getSecondPrimeFactor());
			Assertions.assertEquals("QeANvalFELmZIwx_BCgQtPHgX-5W_-QsMqaqp1_MVKlPsfwjM2jIo0h_m2BQbECMBTz2PTHqcUaysF6r8GQ6aIDD1Svac0rVi-S2c7XUbr6rdpzm6fQzQOPoxp4twjG1iQn0D-sEwvvt1KAxbP5cLph_X3UkT-f8gJMt5ay5PqE", jwk_rsa_pss.getFirstFactorExponent());
			Assertions.assertEquals("X73vsgSfCcl6cAHCjxxTwIPWa_1e2_AKrxVCkVntf9DOyINROKz1qPARGM7_ESVMwdWGN8rtyeYB85Gfh-a25lok82zHO-4JCSsF1z9hiQS0ym-o_DxpvB6NK1BNHxvegt8Y0yaWP9j5SEp8vxgFO85n4-z6nyymVVlj18DqiYE", jwk_rsa_pss.getSecondFactorExponent());
			Assertions.assertEquals("7xDSGuCvCn0i6p0cZ6ZmJcR6xazQwP_a2fN2X4Qk6F2RMRM54kXIretY1v_97i1uaNpnm4hXXqMyTPTg-HkScfcbAFB-c0QwrU6D50OI7-JIVYuYz53dgtuHIDi_fxeWdKl3gxm4hUuEE85dXd4FIb2DzjqjFdweYZ6XsMPcMXA", jwk_rsa_pss.getFirstCoefficient());

			String payload = "Burn butcher burn";
			JWS<String> jws_ec = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("ec")
					.algorithm(ECAlgorithm.ES256.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_ec.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWS<String> jws_oct = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("oct")
					.algorithm(OCTAlgorithm.HS256.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_oct.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWS<String> jws_edec = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("edec")
					.algorithm(EdECAlgorithm.EDDSA_ED25519.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_edec.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWE<String> jwe_xec = jose.jweService().builder(String.class)
				.header(header -> header
					.keyId("xec")
					.algorithm(XECAlgorithm.ECDH_ES_A128KW.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128GCM.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jweService().reader(String.class)
				.read(jwe_xec.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWE<String> jwe_rsa = jose.jweService().builder(String.class)
				.header(header -> header
					.keyId("rsa")
					.algorithm(RSAAlgorithm.RSA_OAEP_256.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A256CBC_HS512.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jweService().reader(String.class)
				.read(jwe_rsa.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWS<String> jws_rsa_pss = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("rsa_pss")
					.algorithm(RSAAlgorithm.PS256.getAlgorithm())
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_rsa_pss.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

	//		System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("keys", List.of(jwk_ec, jwk_oct, jwk_edec, jwk_xec, jwk_rsa, jwk_rsa_pss))));
		}
		finally {
			jose.stop();
		}
	}
	
	@Test
	public void testJWKJKU() {
		URI jku = Path.of("src/test/resources/jwkset.json").toUri();
		GenericResourceService resourceService = new GenericResourceService();
		resourceService.setProviders(List.of(new FileResourceProvider()));
		
		JOSEConfiguration configuration = new JOSEConfiguration() {
			@Override
			public URI key_store() {
				return null;
			}

			@Override
			public String key_store_password() {
				return null;
			}

			@Override
			public boolean resolve_jku() {
				return false;
			}
		};
		
		Jose jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setConfiguration(configuration).setResourceService(resourceService).setJwkStore(new InMemoryJWKStore()));
		try {
			List<? extends JWK> jwks = Flux.from(jose.jwkService().read(jku))
				.onErrorContinue((e, o) -> {}).collectList().block();

			Assertions.assertEquals(6, jwks.size());

			ECJWK jwk_ec = (ECJWK)jwks.get(0);
			Assertions.assertEquals("ec", jwk_ec.getKeyId());
			Assertions.assertEquals("P-256", jwk_ec.getCurve());
			Assertions.assertEquals("Y4XGmjbqCMzBEvqd1emId89o9BY4m8v0qla90wdOgFU", jwk_ec.getXCoordinate());
			Assertions.assertEquals("E1YlPDekf_PDoHgp3Tcc4kIHVRNZZNwj5nJbg0oORAs", jwk_ec.getYCoordinate());
			Assertions.assertEquals("TAGDgB_CWOKKSrIBeglfHHvq0h92OQvE9U-hsslc74c", jwk_ec.getEccPrivateKey());

			OCTJWK jwk_oct = (OCTJWK)jwks.get(1);
			Assertions.assertEquals("oct", jwk_oct.getKeyId());
			Assertions.assertEquals("UyYAqYJ_C7JQWTMbIbvt8g", jwk_oct.getKeyValue());

			EdECJWK jwk_edec = (EdECJWK)jwks.get(2);
			Assertions.assertEquals("edec", jwk_edec.getKeyId());
			Assertions.assertEquals("Ed25519", jwk_edec.getCurve());
			Assertions.assertEquals("Uguw_mZ4K9vvkpHxu4Wcm7tnGJg8RANt1YmkGm6v8v4", jwk_edec.getPublicKey());
			Assertions.assertEquals("xpHPy9ha5ARGSL4Ob_9xhI2ko0r8zdozQJ1tCHfTOS0", jwk_edec.getPrivateKey());

			XECJWK jwk_xec = (XECJWK)jwks.get(3);
			Assertions.assertEquals("xec", jwk_xec.getKeyId());
			Assertions.assertEquals("X448", jwk_xec.getCurve());
			Assertions.assertEquals("5ysteKhT2dTy5wU9Sjb2KIS80djtz5XybrGGNbZ6y0N-hbF7Pbs-cc_ce-udmP07UUFWpKLz0eI", jwk_xec.getPublicKey());
			Assertions.assertEquals("oLNkPvCsu3St4uJcRI5XjyNOv2qhV-wAuOlr81s-RASNUQTXMzKSOEslbvvsg6_5GZPus2YJEvg", jwk_xec.getPrivateKey());

			RSAJWK jwk_rsa = (RSAJWK)jwks.get(4);
			Assertions.assertEquals("rsa", jwk_rsa.getKeyId());
			Assertions.assertEquals("yiuP-607_c5vnBM5dXwtZtdE8Da_gxGN4DxLrstoXD0fUZ1J6HbeTY0_-zcyR61d06irprt1RJ9nlbTJCs1P_ZffLHYOAVhEjzp0pyPjUCztxXw0MdMpWblA4U0aIS6BN2cdaKojBQY7yZM9NY5G0OWovBWOhvCHLmznxmK54wCdpq2QIpXLT_DTC68brlvCVoij1IdwydQmdioScN47VySUAaqlim3jv4GjSkDZd6ecnwpzbYpCMC7qhR2iFE-PgThgWlZ_iil9Su4qsWmUWjPSkfSgOAO73bUSg-PQAa3Np_AlF4fCihXg2V6FJxTd77KcZ13IQrXs2AiZ7Anm-Q", jwk_rsa.getModulus());
			Assertions.assertEquals("AQAB", jwk_rsa.getPublicExponent());
			Assertions.assertEquals("rvyXqnpeI6fL2OaW5EawMYSASf7JMtQ93emyhD_RO404D3c54nkIn010JKe44GuBe9NRh4ZX0Sa8DMsm_C-LXe9XHu-r3aQd627oS0b32Iya3UVNFBc2gk-jhZ8rz66l72NUBCTHHPExTJ8h6roUN3mg2_M4ozmLeDaRQphvVrfE_xUGoITJt03C0u1ACF_ELoEUhx0DzMFFe6zeHcgYD4okljqRi_NT8Tvl8rQ2PxM5CoEqe9AV35VdJ1mARw4YQ9MiFmBXOrJ-FG2SflVLy5zCnb2hJ36tZFFjDJpKTd2nVXFXFNXa7tIbPx4nqwBiwBtEcNE1OerD06qImbQxdQ", jwk_rsa.getPrivateExponent());
			Assertions.assertEquals("-xO90vtnU0K-DSOeq0YBISnqmebwI8kIG7ZQy_9m7_XBw3v0ootY7K-aMM4ayhqmyYP4BLvNx9LmxV54owWkf_JczhPdJby75JDEB-Ndj8Tl7lwxKhf1xrpNZRS2ScWvyT7FunkP1XPuZ1txd5DMcwgz2GRX7M_wzd0VxMC7GDM", jwk_rsa.getFirstPrimeFactor());
			Assertions.assertEquals("ziJWRWTCcddcT2aoeAAM6cc6ITt7pBiW2cZJhgKrvaIZ_NfC21H9pZq7134DBptVrXR7ju0doXrlIrTJndJ6BDw7XDiElLcKVYm6Zh8IoMh6rETj1pJuyyY8Uu0Ue2zM8SPNZYqFcrfM-sUz2FCbPcbMZC6u6Nej3_jpXPt8CCM", jwk_rsa.getSecondPrimeFactor());
			Assertions.assertEquals("7TJ3UM0FjlktZwhRrAkUpBoPcpoEICqZqGSS7EY7H1OTXHTMZosy1VgIWTc9g9Wt4A72zrIF9FPAhJF9crWv9Ngo6N9HO5GUCJjjOXiJOXuQpaEfKV89aCM1Xts0Y3mJWpwc_M_GL6e4gJiZF5YKMFp9cF9L2YORQUsud5SErJU", jwk_rsa.getFirstFactorExponent());
			Assertions.assertEquals("FVQOMx3q1Jag8YJAujHfJC0-AYRDFcaJjTzsDJaoKHXmVHgKrGC-au0otHJQY9WcttbPlglIwJWTsdSc69yETX3h8nfSmAScaB9ZDwn0_ZXgw4RZQrFiD1kEctwe-2pVhjnbHb-IcUc9SVEwPRiSB4FTUqZV6LevOBRsKPfIbJs", jwk_rsa.getSecondFactorExponent());
			Assertions.assertEquals("FIFvFRw08HO8c0J9SPiO0eiiNlvjyUbbzMbNPDE0jMazqqcx7X_c5go9w8P_Z0YWMbg3ai6YGroytJXr14jjnQUDVw1ThC7bqOQ6ZjaMZzDEJiZ8rHa0EofS2IrQqfPx9rM9Y7X-kBVSYfZAwIMNqlq53Mq_1_UwzkTkBvpuA2I", jwk_rsa.getFirstCoefficient());

			RSAJWK jwk_rsa_pss = (RSAJWK)jwks.get(5);
			Assertions.assertEquals("rsa_pss", jwk_rsa_pss.getKeyId());
			Assertions.assertEquals("9NC1kMNercoyoSwx5wzTVlUm2XadzUqAKoz37RB2mpzsGVoqmyixyrLQSLm9nzlEqpuDA5sKxELV1A0C0rCVsOv69lYmMCep11GvwmjVPstGk3oXBn9fh6y4IDf5JrJIfUGlYcIh4BuDjF_tMmmcE0ZlkUq7EqS99PF0IQA7KWsDAFvpa48TjKCxrR6_tdny-WtQYyvBd8f0LTwGcPfZG-aeEVzFd_gPtlxkPGQ1MTCyo3h38jsRJn5Rhg0yEtOeWGf9DEYBHNny2TfgqZlizHHzu2lTDZJfMGgT9p_G2Q59GY3kZZX2LazwDsRXvqdR1gp_5HwOEa1KlmuKPtArQQ", jwk_rsa_pss.getModulus());
			Assertions.assertEquals("AQAB", jwk_rsa_pss.getPublicExponent());
			Assertions.assertEquals("s7GeE6vTiuynTPYLivQ3C19lLKmMGmtct97Q_AjhhYs5IUK1kz3DgmzNxRPQw1ZduHx9JeBffr8wBH2oXM2QklQj2TxSu3XhjFJBGAmqvHSoUQeEbxh_Hi8A12U-U9D4tDfDFIZSJxUK8bZXfHFYRi2dz49y0LRrWacA_lgVFMgv1rvVYJNtlIIrv-QHieXJvDlN3jKdgo3B4C8J2KrXkEB89rtMUGvq89qeoQH0YfRv-6z2cGVrX6pHCuD3orrKmAdfHk5OrtVSVjBbtSL1TzN4QxzNJJEXMjAi-ZjdgOJ5wyKBD6zKFhojwh_3maa-BdeLiTAb523lZNyuTUugAQ", jwk_rsa_pss.getPrivateExponent());
			Assertions.assertEquals("_g-xJTX1dFO6tetzyNkhUvEXJux5hJrerGrM8fs0csKOHORnVtmCcvaZB1Xh-Ae3p42O5OZ9THjhmYxV20Yjxn_YzMJVSQofC3-e_NNVl9OujDNdVzps4FX0vULhG3SdQpCM6pQo4vMxIqo8TuCTtkB6U8ynqM7hDMFaBsIsAqE", jwk_rsa_pss.getFirstPrimeFactor());
			Assertions.assertEquals("9q70hqp2bDfrpPqXTEcLMz6Cg653VmwzCcg4GwSeOyl95lYrW5GD_AgnDxj_uDlOX5hgblF1KKiO7nF6W-5vfCAot26yG6uCJVHVl00uqbWpb9dLHsVngigcHmjWAUxYZMgedSUC17GrY_-RToK4gA6HsU33Bibxzx_GhfIYBKE", jwk_rsa_pss.getSecondPrimeFactor());
			Assertions.assertEquals("QeANvalFELmZIwx_BCgQtPHgX-5W_-QsMqaqp1_MVKlPsfwjM2jIo0h_m2BQbECMBTz2PTHqcUaysF6r8GQ6aIDD1Svac0rVi-S2c7XUbr6rdpzm6fQzQOPoxp4twjG1iQn0D-sEwvvt1KAxbP5cLph_X3UkT-f8gJMt5ay5PqE", jwk_rsa_pss.getFirstFactorExponent());
			Assertions.assertEquals("X73vsgSfCcl6cAHCjxxTwIPWa_1e2_AKrxVCkVntf9DOyINROKz1qPARGM7_ESVMwdWGN8rtyeYB85Gfh-a25lok82zHO-4JCSsF1z9hiQS0ym-o_DxpvB6NK1BNHxvegt8Y0yaWP9j5SEp8vxgFO85n4-z6nyymVVlj18DqiYE", jwk_rsa_pss.getSecondFactorExponent());
			Assertions.assertEquals("7xDSGuCvCn0i6p0cZ6ZmJcR6xazQwP_a2fN2X4Qk6F2RMRM54kXIretY1v_97i1uaNpnm4hXXqMyTPTg-HkScfcbAFB-c0QwrU6D50OI7-JIVYuYz53dgtuHIDi_fxeWdKl3gxm4hUuEE85dXd4FIb2DzjqjFdweYZ6XsMPcMXA", jwk_rsa_pss.getFirstCoefficient());
			
			// this fails since the JKU resolution is disabled => no suitable key found
			final Jose localJose = jose;
			Assertions.assertEquals("No suitable key found", Assertions.assertThrows(JOSEObjectBuildException.class, () -> {
				localJose.jwsService().builder(String.class)
					.header(header -> header
						.keyId("ec")
						.algorithm(ECAlgorithm.ES256.getAlgorithm())
						.jwkSetURL(jku)
					)
					.payload("payload")
					.build(MediaTypes.TEXT_PLAIN)
					.block();
			}).getMessage());
		}
		finally {
			jose.stop();
		}
		
		configuration = new JOSEConfiguration() {
			@Override
			public URI key_store() {
				return null;
			}

			@Override
			public String key_store_password() {
				return null;
			}

			@Override
			public boolean resolve_jku() {
				return true;
			}
		};
		
		jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setConfiguration(configuration).setResourceService(resourceService).setJwkStore(new InMemoryJWKStore()));
		try {
			// this fails since the JKU resolution is enabled but location is not trusted => no suitable key found
			// WARN AbstractJOSEObjectBuilder Skipping untrusted key: {"kty":"EC","kid":"ec","alg":"ES256"}
			final Jose localJose = jose;
			Assertions.assertEquals("No suitable key found", Assertions.assertThrows(JOSEObjectBuildException.class, () -> {
				localJose.jwsService().builder(String.class)
					.header(header -> header
						.keyId("ec")
						.algorithm(ECAlgorithm.ES256.getAlgorithm())
						.jwkSetURL(jku)
					)
					.payload("payload")
					.build(MediaTypes.TEXT_PLAIN)
					.block();
			}).getMessage());
		}
		finally {
			jose.stop();
		}
		
		configuration = new JOSEConfiguration() {
			@Override
			public URI key_store() {
				return null;
			}

			@Override
			public String key_store_password() {
				return null;
			}

			@Override
			public boolean resolve_jku() {
				return true;
			}

			@Override
			public Set<URI> trusted_jku() {
				return Set.of(jku);
			}
		};
		
		jose = Application.run(new Jose.Builder(MEDIA_TYPE_CONVERTERS).setConfiguration(configuration).setResourceService(resourceService).setJwkStore(new InMemoryJWKStore()));
		try {
			String payload = "Burn butcher burn";
			JWS<String> jws_ec = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("ec")
					.algorithm(ECAlgorithm.ES256.getAlgorithm())
					.jwkSetURL(jku)
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_ec.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWS<String> jws_oct = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("oct")
					.algorithm(OCTAlgorithm.HS256.getAlgorithm())
					.jwkSetURL(jku)
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_oct.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWS<String> jws_edec = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("edec")
					.algorithm(EdECAlgorithm.EDDSA_ED25519.getAlgorithm())
					.jwkSetURL(jku)
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_edec.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWE<String> jwe_xec = jose.jweService().builder(String.class)
				.header(header -> header
					.keyId("xec")
					.algorithm(XECAlgorithm.ECDH_ES_A128KW.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A128GCM.getAlgorithm())
					.jwkSetURL(jku)
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jweService().reader(String.class)
				.read(jwe_xec.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWE<String> jwe_rsa = jose.jweService().builder(String.class)
				.header(header -> header
					.keyId("rsa")
					.algorithm(RSAAlgorithm.RSA_OAEP_256.getAlgorithm())
					.encryptionAlgorithm(OCTAlgorithm.A256CBC_HS512.getAlgorithm())
					.jwkSetURL(jku)
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jweService().reader(String.class)
				.read(jwe_rsa.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();

			JWS<String> jws_rsa_pss = jose.jwsService().builder(String.class)
				.header(header -> header
					.keyId("rsa_pss")
					.algorithm(RSAAlgorithm.PS256.getAlgorithm())
					.jwkSetURL(jku)
				)
				.payload(payload)
				.build(MediaTypes.TEXT_PLAIN)
				.block();

			jose.jwsService().reader(String.class)
				.read(jws_rsa_pss.toCompact(), MediaTypes.TEXT_PLAIN)
				.block();
		}
		finally {
			jose.stop();
		}
	}
}
